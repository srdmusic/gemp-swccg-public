package com.gempukku.swccgo.ai.models.curator;

import com.gempukku.swccgo.ai.SwccgAiController;
import com.gempukku.swccgo.ai.models.rando.RandoCalAi;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CuratorAi — the self-play "curator" controller (INCREMENT 2: Q8 brain).
 *
 * Plays a side in a bot-vs-bot game against Rando. Uses Rando's own logic as an
 * ADVISOR (HAS-A RandoCalAi) and OVERRIDES Rando's pick when the local reasoning
 * model (DeepSeek-R1 70B Q8 via Ollama) judges the suggested move will backfire.
 * Every decision + override is logged as JSONL for Claude (overseer) to review.
 *
 * SAFETY — the loop-bug guard: an override is only ever taken when the curator's
 * reconstructed option set provably matches the engine's (Rando's own pick must
 * appear in it). If it doesn't, or the model fails / times out / returns garbage,
 * the curator falls back to Rando's pick. The engine never receives an
 * unvalidated response string.
 *
 * CONSULT FILTER: the model is consulted only on CONSEQUENTIAL decisions
 * (deploy / battle / move / force-loss / forfeit) with a real choice. Trivial
 * decisions (auto-pass activate/draw/control, single-option) pass straight
 * through to Rando, keeping games tractable (Q8 is ~seconds per call).
 *
 * Config via env: CURATOR_OLLAMA_URL (default http://127.0.0.1:11434),
 * CURATOR_MODEL (default deepseek-r1:70b-llama-distill-q8_0),
 * CURATOR_USE_MODEL (default true — set false for passthrough).
 */
public class CuratorAi implements SwccgAiController {

    private static final Logger LOG = LogManager.getLogger(CuratorAi.class);

    private static final String DECISION_LOG_PATH = "/opt/gemp-swccg/logs/curator_decisions.jsonl";

    // Default to host.docker.internal: the GEMP server runs in a Docker container,
    // so 127.0.0.1 would be the container itself, not the Mac host where Ollama runs.
    // host.docker.internal is Docker Desktop's host alias (verified reachable + has the
    // Q8 model). Override with CURATOR_OLLAMA_URL for non-Docker / remote-PC setups.
    private static final String OLLAMA_URL = envOr("CURATOR_OLLAMA_URL", "http://host.docker.internal:11434");
    private static final String MODEL = envOr("CURATOR_MODEL", "deepseek-r1:70b-llama-distill-q8_0");
    private static final boolean USE_MODEL = !"false".equalsIgnoreCase(envOr("CURATOR_USE_MODEL", "true"));
    // Option C: cap the consult. With pivotal-only consults (few per game) we can
    // afford a longer per-consult ceiling, but cap num_predict so the model can't
    // run away on a giant <think> chain. ~300s ceiling fits a bounded reasoning pass
    // on the q8 70B; on timeout we fall back to Rando (safe).
    private static final int CONSULT_TIMEOUT_S = 300;

    private static final Pattern CHOICE_RE = Pattern.compile("CHOICE:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern REASON_RE = Pattern.compile("REASON:\\s*(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern THINK_RE = Pattern.compile("<think>.*?</think>", Pattern.DOTALL);

    private final RandoCalAi rando = new RandoCalAi();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private final Gson gson = new Gson();

    private String playerId;
    private String deckName;

    public CuratorAi() {
        LOG.warn("🧠 CuratorAi created (increment 2: Q8 brain — model={}, useModel={}, url={})",
                MODEL, USE_MODEL, OLLAMA_URL);
    }

    @Override
    public String decide(String playerId, AwaitingDecision decision, GameState gameState) {
        this.playerId = playerId;

        // Advisor: what would Rando do here? (this is also a valid engine response)
        String randoPick = rando.decide(playerId, decision, gameState);

        String chosen = randoPick;
        boolean overrode = false;
        String reason = "passthrough";

        try {
            if (USE_MODEL && shouldConsult(decision, gameState)) {
                Consult c = consultModel(decision, gameState, randoPick);
                if (c == null) {
                    reason = "consult failed/unavailable — followed Rando";
                } else if (c.chosenResponse == null) {
                    reason = "model agreed with Rando" + (c.reason != null ? " (" + c.reason + ")" : "");
                } else if (c.chosenResponse.equals(randoPick)) {
                    reason = "model picked Rando's option" + (c.reason != null ? " (" + c.reason + ")" : "");
                } else {
                    chosen = c.chosenResponse;
                    overrode = true;
                    reason = "OVERRIDE: " + (c.reason != null ? c.reason : "model preferred a different option");
                }
            } else {
                reason = USE_MODEL ? "passthrough (trivial decision — not consulted)" : "passthrough (model disabled)";
            }
        } catch (Exception e) {
            // Any failure → safe fallback to Rando. Never break the game.
            chosen = randoPick;
            overrode = false;
            reason = "exception — followed Rando: " + e.getMessage();
            LOG.debug("CuratorAi consult error: {}", e.getMessage());
        }

        logDecision(decision, gameState, randoPick, chosen, overrode, reason);
        return chosen;
    }

    // ── Consult filter: consequential decisions only ────────────────────────
    private boolean shouldConsult(AwaitingDecision decision, GameState gameState) {
        if (decision == null) return false;
        String text = decision.getText() != null ? decision.getText().toLowerCase(java.util.Locale.ROOT) : "";

        // === OPTION A (Steve): consult only on PIVOTAL decisions ===
        // The Q8 70B is slow (~minutes per consult on this hardware), so consulting
        // every deploy-or-pass made games take hours and most consults timed out.
        // Restrict to the few highest-impact, loss-critical decisions per game:
        //   1. Battle-damage resolution — forfeit / force-loss (where games are won/lost;
        //      Steve's #1 complaint area).
        //   2. Whether/how to initiate or resolve a BATTLE (attack tactics).
        // Routine deploy-or-pass / move-or-pass pass straight through to Rando. This
        // cuts consults from dozens to a handful per game, keeping games tractable.

        // 1. Battle-damage resolution (forfeit / force-loss) — always pivotal.
        if (text.contains("forfeit") || text.contains("force to lose")
                || text.contains("battle damage")) {
            return true;
        }

        // 2. Battle-phase action decisions with a real choice (initiate/continue a battle).
        int opts = optionCount(decision);
        if (opts < 2) return false;
        String phase = (gameState != null && gameState.getCurrentPhase() != null)
                ? gameState.getCurrentPhase().name() : "";
        if ("BATTLE".equals(phase)) return true;
        // Also catch an explicit "initiate battle" choice regardless of phase label.
        if (text.contains("initiate") && text.contains("battle")) return true;

        return false;  // deploy/move/activate routine decisions → passthrough to Rando
    }

    // ── Model consult ───────────────────────────────────────────────────────
    private static final class Consult {
        final String chosenResponse;   // null = agreed with Rando / no override
        final String reason;
        Consult(String chosenResponse, String reason) { this.chosenResponse = chosenResponse; this.reason = reason; }
    }

    private Consult consultModel(AwaitingDecision decision, GameState gameState, String randoPick) {
        // Build the option set (label + engine response string), parallel lists.
        List<String> labels = new ArrayList<>();
        List<String> responses = new ArrayList<>();
        buildOptions(decision, gameState, labels, responses);

        // PASS option: most consequential decisions are "Choose X action or Pass",
        // where the engine's pass response is the EMPTY string. Rando very often
        // passes (randoPick==""), but "" isn't in actionId[], so without this the
        // safety guard below always bails and the model is never consulted. When
        // Rando's pick is empty, "" is a proven-valid response (Rando just used it),
        // so add an explicit Pass option. The model can then choose act-vs-pass.
        String effRando = (randoPick == null) ? "" : randoPick;
        if (effRando.isEmpty() && !responses.contains("")) {
            responses.add("");
            labels.add("Pass / do nothing (take no action)");
        }

        if (responses.size() < 2) return null;  // nothing meaningful to choose

        // SAFETY GUARD: only proceed if Rando's pick is in our reconstructed set.
        // If it isn't, our option→response mapping doesn't match the engine's
        // format and an override could send an invalid response (loop bug). Bail.
        int randoIdx = responses.indexOf(effRando);
        if (randoIdx < 0) {
            LOG.debug("CuratorAi: randoPick '{}' not in reconstructed options — skip consult (mapping unsafe)", randoPick);
            return null;
        }

        String prompt = buildPrompt(decision, gameState, labels, randoIdx);
        String content = callOllama(prompt);
        if (content == null) return null;

        // Strip reasoning block, parse CHOICE + REASON.
        String clean = THINK_RE.matcher(content).replaceAll("").trim();
        Matcher cm = CHOICE_RE.matcher(clean);
        if (!cm.find()) return null;
        int idx;
        try { idx = Integer.parseInt(cm.group(1)); } catch (NumberFormatException e) { return null; }
        if (idx < 0 || idx >= responses.size()) return null;

        String reason = null;
        Matcher rm = REASON_RE.matcher(clean);
        if (rm.find()) reason = rm.group(1).trim();

        String picked = responses.get(idx);
        // null chosenResponse signals "agree with Rando"; only set when it differs.
        // Compare against effRando (handles the empty/pass case consistently).
        return new Consult(picked.equals(effRando) ? null : picked, reason);
    }

    private void buildOptions(AwaitingDecision decision, GameState gameState,
                              List<String> labels, List<String> responses) {
        Map<String, String[]> params = decision.getDecisionParameters();
        if (params == null) return;
        String dtype = decision.getDecisionType() != null ? decision.getDecisionType().name() : "";

        if ("CARD_ACTION_CHOICE".equals(dtype)) {
            String[] actionIds = params.get("actionId");
            String[] actionTexts = params.get("actionText");
            if (actionIds == null) return;
            for (int i = 0; i < actionIds.length; i++) {
                responses.add(actionIds[i]);
                labels.add(actionTexts != null && i < actionTexts.length && actionTexts[i] != null
                        ? actionTexts[i] : ("action " + actionIds[i]));
            }
        } else {
            // CARD_SELECTION / ARBITRARY_CARDS etc.: response = cardId, label = title
            String[] cardIds = params.get("cardId");
            if (cardIds == null) return;
            for (String cid : cardIds) {
                responses.add(cid);
                labels.add(titleOf(cid, gameState));
            }
        }
    }

    private String titleOf(String cardId, GameState gameState) {
        try {
            PhysicalCard c = gameState.findCardById(Integer.parseInt(cardId));
            if (c != null && c.getTitle() != null) return c.getTitle();
        } catch (Exception ignore) { /* fall through */ }
        return "card " + cardId;
    }

    private String buildPrompt(AwaitingDecision decision, GameState gameState,
                               List<String> labels, int randoIdx) {
        StringBuilder sb = new StringBuilder();
        String phase = (gameState != null && gameState.getCurrentPhase() != null)
                ? gameState.getCurrentPhase().name() : "?";
        sb.append("Phase: ").append(phase).append("\n");
        sb.append("Decision: ").append(decision.getText() != null ? decision.getText() : "").append("\n");
        sb.append(stateSummary(gameState)).append("\n");
        sb.append("Options:\n");
        for (int i = 0; i < labels.size(); i++) {
            sb.append(i).append(": ").append(labels.get(i));
            if (i == randoIdx) sb.append("   <- Rando (reference engine) recommends this");
            sb.append("\n");
        }
        sb.append("\nPick the option most likely to WIN. Rando is usually right; deviate only with a clear reason.\n");
        sb.append("Reply EXACTLY: 'CHOICE: <number>' then optionally 'REASON: <one line>'.");
        return sb.toString();
    }

    private String stateSummary(GameState gameState) {
        if (gameState == null || playerId == null) return "";
        try {
            String opp = gameState.getOpponent(playerId);
            int myLf = gameState.getReserveDeckSize(playerId)
                    + gameState.getUsedPile(playerId).size() + gameState.getForcePileSize(playerId);
            int oppLf = gameState.getReserveDeckSize(opp)
                    + gameState.getUsedPile(opp).size() + gameState.getForcePileSize(opp);
            int hand = gameState.getHand(playerId).size();
            return "Your life force: " + myLf + ", Opponent life force: " + oppLf + ", Your hand: " + hand;
        } catch (Exception e) {
            return "";
        }
    }

    private String callOllama(String prompt) {
        try {
            JsonObject sys = new JsonObject();
            sys.addProperty("role", "system");
            sys.addProperty("content", "You are a Star Wars CCG (SWCCG) expert choosing moves for a bot "
                    + "in a competitive game. Choose the option most likely to win. Decide QUICKLY with "
                    + "MINIMAL deliberation — keep any reasoning to a few sentences, then immediately output "
                    + "the CHOICE line. Do not over-think; a fast good choice beats a slow perfect one.");
            JsonObject usr = new JsonObject();
            usr.addProperty("role", "user");
            usr.addProperty("content", prompt);
            JsonArray messages = new JsonArray();
            messages.add(sys);
            messages.add(usr);

            JsonObject body = new JsonObject();
            body.addProperty("model", MODEL);
            body.add("messages", messages);
            body.addProperty("stream", false);
            JsonObject opts = new JsonObject();
            opts.addProperty("temperature", 0.3);
            opts.addProperty("num_predict", 1200);  // Option C: cap thinking so a consult can't run away
            body.add("options", opts);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_URL + "/v1/chat/completions"))
                    .timeout(Duration.ofSeconds(CONSULT_TIMEOUT_S))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                LOG.debug("CuratorAi: Ollama HTTP {}", resp.statusCode());
                return null;
            }
            JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
            JsonArray choices = json.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) return null;
            return choices.get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString();
        } catch (Exception e) {
            LOG.debug("CuratorAi: Ollama call failed: {}", e.getMessage());
            return null;
        }
    }

    private int optionCount(AwaitingDecision decision) {
        try {
            Map<String, String[]> params = decision.getDecisionParameters();
            if (params == null) return 0;
            String[] a = params.get("actionId");
            if (a != null) return a.length;
            String[] c = params.get("cardId");
            if (c != null) return c.length;
        } catch (Exception ignore) { }
        return 0;
    }

    // ── Decision log ─────────────────────────────────────────────────────────
    private void logDecision(AwaitingDecision decision, GameState gameState,
                             String randoPick, String chosen, boolean overrode, String reason) {
        try {
            String decisionType = (decision != null && decision.getDecisionType() != null)
                    ? decision.getDecisionType().name() : "UNKNOWN";
            String text = (decision != null && decision.getText() != null) ? decision.getText() : "";
            String phase = (gameState != null && gameState.getCurrentPhase() != null)
                    ? gameState.getCurrentPhase().name() : "";
            int oc = optionCount(decision);

            String line = "{"
                    + "\"player\":" + jsonStr(playerId)
                    + ",\"deck\":" + jsonStr(deckName)
                    + ",\"phase\":" + jsonStr(phase)
                    + ",\"decisionType\":" + jsonStr(decisionType)
                    + ",\"text\":" + jsonStr(truncate(text, 300))
                    + ",\"options\":" + oc
                    + ",\"randoPick\":" + jsonStr(randoPick == null ? "(pass)" : randoPick)
                    + ",\"chosen\":" + jsonStr(chosen == null ? "(pass)" : chosen)
                    + ",\"overrode\":" + overrode
                    + ",\"reason\":" + jsonStr(reason)
                    + "}";

            synchronized (CuratorAi.class) {
                try (FileWriter fw = new FileWriter(DECISION_LOG_PATH, true)) {
                    fw.write(line);
                    fw.write("\n");
                }
            }
            if (overrode) {
                LOG.warn("🧠 CURATOR OVERRIDE [{}/{}]: chose '{}' over Rando '{}' — {}",
                        phase, decisionType, chosen, randoPick, reason);
            }
        } catch (IOException e) {
            LOG.debug("CuratorAi: could not append decision log: {}", e.getMessage());
        } catch (Exception e) {
            LOG.debug("CuratorAi: decision-log error: {}", e.getMessage());
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static String jsonStr(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    private static String envOr(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isEmpty()) ? def : v;
    }

    // ── Forward the rest of the controller contract to the advisor ──────────

    @Override
    public void setGame(SwccgGame game) {
        rando.setGame(game);
    }

    @Override
    public void setDeckName(String deckName) {
        this.deckName = deckName;
        rando.setDeckName(deckName);
    }

    @Override
    public String getChatMessage() {
        return rando.getChatMessage();
    }
}
