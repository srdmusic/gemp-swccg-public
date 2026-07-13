package com.gempukku.swccgo.ai.models.chosenone;

import com.gempukku.swccgo.ai.models.common.trace.TraceCorrection;
import com.gempukku.swccgo.ai.models.common.trace.TraceSession;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;

import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Decision Safety Module
 *
 * Provides guaranteed decision responses to ensure the bot NEVER hangs.
 * This is the "last line of defense" - if all evaluators and fallbacks fail,
 * this module ensures we still send a valid response to the server.
 *
 * Design Philosophy:
 * - EVERY decision must get a response - NEVER return without posting a decision
 * - A bad decision is better than no decision (game continues vs hangs)
 * - If noPass=true OR min>=1, we MUST return a valid choice (not empty string)
 * - Log everything for debugging, but never fail silently
 *
 * Ported from Python decision_safety.py
 */
public class DecisionSafety {
    private static final Logger LOG = RandoLogger.getSafetyLogger();
    private static final Random RANDOM = new Random();

    // Known decision types we can handle
    public static final Set<String> KNOWN_TYPES = Set.of(
        "MULTIPLE_CHOICE",
        "CARD_SELECTION",
        "CARD_ACTION_CHOICE",
        "ACTION_CHOICE",
        "INTEGER",
        "ARBITRARY_CARDS"
    );

    /**
     * A guaranteed safe decision response.
     */
    public static class SafetyDecision {
        public final String decisionId;
        public final String value;
        public final String reason;
        public final boolean wasEmergency;

        public SafetyDecision(String decisionId, String value, String reason, boolean wasEmergency) {
            this.decisionId = decisionId;
            this.value = value;
            this.reason = reason;
            this.wasEmergency = wasEmergency;
        }
    }

    /**
     * Check if we MUST choose something (cannot pass).
     * True if there's only one valid option, or decision indicates required choice.
     */
    public static boolean mustChoose(AwaitingDecision decision) {
        Map<String, String[]> params = decision.getDecisionParameters();

        // V148 (Steve, 2026-05-28): a decision that explicitly offers Done/Cancel
        // (and allows zero selections, min==0) is NEVER must-choose, even if the
        // engine set noPass=true. The "Done to cancel" button = select zero cards;
        // noPass=true refers to the phase-level pass, not this in-selection cancel.
        // Without this, an empty (cancel) response from the evaluator gets force-
        // corrected back into a random pick — defeating V148's deploy-abort logic.
        if (params != null) {
            String[] minArr = params.get("min");
            int minVal = 0;
            if (minArr != null && minArr.length > 0) {
                try { minVal = Integer.parseInt(minArr[0]); } catch (NumberFormatException ignore) { }
            }
            String text = decision.getText();
            if (minVal == 0 && text != null) {
                String clean = text.replaceAll("<div[^>]*>.*?</div>", "")
                                   .replaceAll("<[^>]+>", "").toLowerCase(Locale.ROOT);
                if (clean.contains("done") || clean.contains("cancel")
                        || clean.contains("if desired") || clean.contains("optional")) {
                    return false;  // genuinely cancellable — empty response is valid
                }
            }
        }

        // Otherwise the noPass parameter is the authoritative source
        if (params != null) {
            String[] noPassArr = params.get("noPass");
            if (noPassArr != null && noPassArr.length > 0) {
                return Boolean.parseBoolean(noPassArr[0]);
            }
        }

        // For most GEMP decisions, we check the available options
        String[] results = params != null ? params.get("results") : null;
        if (results != null && results.length == 1) {
            return true;  // Only one option means we must choose it
        }

        // Check for typical required decision indicators in the PROMPT text
        // IMPORTANT: Strip out card names (in HTML divs) to avoid false positives
        // e.g., "Playing •We Must Accelerate Our Plans" shouldn't trigger on "must"
        String text = decision.getText();
        if (text != null) {
            // Remove HTML card hints which contain card names
            String cleanText = text.replaceAll("<div[^>]*>.*?</div>", "")
                                   .replaceAll("<[^>]+>", "");
            String lowerText = cleanText.toLowerCase(Locale.ROOT);

            // Only match "must" at the start of a decision prompt like "You must choose..."
            // or explicit "required" indicators
            if (lowerText.startsWith("you must") ||
                lowerText.startsWith("must ") ||
                lowerText.contains(" must choose") ||
                lowerText.contains("required")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if we CAN pass (return empty string or done).
     */
    public static boolean canPass(AwaitingDecision decision) {
        String text = decision.getText();
        if (text != null) {
            String lowerText = text.toLowerCase(Locale.ROOT);
            return lowerText.contains("may") ||
                   lowerText.contains("optional") ||
                   lowerText.contains("done") ||
                   lowerText.contains("cancel");
        }
        return true;  // Default to allowing pass
    }

    /**
     * CRITICAL SAFETY METHOD - ensures response is valid before sending.
     *
     * If response is invalid (empty when must choose), this will force
     * a valid response using random selection.
     *
     * @return array of [correctedResponse, reasonIfCorrected]
     */
    public static String[] ensureValidResponse(AwaitingDecision decision, String response,
                                                String[] availableOptions) {
        boolean mustMakeChoice = mustChoose(decision);

        // If response is empty but we MUST choose, force a selection
        if ((response == null || response.isEmpty()) && mustMakeChoice) {
            if (availableOptions != null && availableOptions.length > 0) {
                String forced = availableOptions[RANDOM.nextInt(availableOptions.length)];
                String reason = "SAFETY FORCED: Empty response but must choose. Picked random: " + forced;
                LOG.error(reason);
                // TRACE ORACLE V2 (2026-07-13): DecisionSafety owns the correction record —
                // typed reason + before/after. Observation only; the correction itself is
                // unchanged legacy behavior (recorded AFTER the RNG draw, no extra draws).
                if (TraceSession.isActive()) {
                    TraceSession.recordCorrection(TraceCorrection.Kind.SAFETY_FORCED_CHOICE,
                        response, forced, reason);
                }
                return new String[]{forced, reason};
            } else {
                // Absolute last resort
                LOG.error("SAFETY CRITICAL: Must choose but no options available!");
                // TRACE ORACLE V2 (2026-07-13): typed correction record, observation only.
                if (TraceSession.isActive()) {
                    TraceSession.recordCorrection(TraceCorrection.Kind.SAFETY_CRITICAL_NO_OPTIONS,
                        response, "0", "SAFETY CRITICAL: No options, guessing '0'");
                }
                return new String[]{"0", "SAFETY CRITICAL: No options, guessing '0'"};
            }
        }

        // === SAFETY LAYER 2b (2026-07-10): SELECTABLE-CLAMP for card selections ===
        // Root cause of the 2026-07-10 setup HANG (replay 2jg1sj0l3qrlgy6a): an evaluator answered an
        // ARBITRARY_CARDS decision with a card whose selectable=false ("Any Methods Necessary" iterative
        // prison+bounty-hunter combination; the V22.7 matching/starship routing sent it to the pilot
        // evaluator, which ignores the selectable[] array and picked temp33). The engine throws
        // DecisionResultInvalidException and SwccgGameMediator.maybeLetAiPlay SWALLOWS it for AI players
        // without re-invoking the AI -> the decision sits pending forever (silent, CPU-idle hang).
        // Fix at the response boundary (same philosophy as the 2026-05-31 MULTI-SELECT fix upstream):
        // drop every answer token that is not a known cardId or is not selectable; if the survivors fall
        // below what the decision requires, rebuild as all preselected ids + the FIRST selectable
        // non-preselected id (ONE new card per round when returnAnyChange=true — matches the engine's
        // iterative TakeCardCombinationIntoHandFromPileEffect flow and how a human clicks; filling
        // several at once can build an impossible combination and re-hang). Cap at max.
        if (response != null && !response.isEmpty() && decision != null
                && decision.getDecisionType() != null) {
            String typeName = decision.getDecisionType().name();
            if ("ARBITRARY_CARDS".equals(typeName) || "CARD_SELECTION".equals(typeName)) {
                Map<String, String[]> p = decision.getDecisionParameters();
                String[] ids = p != null ? p.get("cardId") : null;
                String[] selectable = p != null ? p.get("selectable") : null;
                String[] preselected = p != null ? p.get("preselected") : null;
                if (ids != null && selectable != null && ids.length == selectable.length) {
                    Map<String, Integer> idx = new HashMap<>();
                    for (int i = 0; i < ids.length; i++) idx.put(ids[i], i);
                    int min = parseIntParam(p.get("min"), 0);
                    int max = parseIntParam(p.get("max"), ids.length);
                    boolean returnAnyChange = "true".equalsIgnoreCase(firstOrNull(p.get("returnAnyChange")));
                    List<String> original = new ArrayList<>();
                    for (String tok : response.split(",")) {
                        tok = tok.trim();
                        if (!tok.isEmpty()) original.add(tok);
                    }
                    LinkedHashSet<String> kept = new LinkedHashSet<>();
                    for (String tok : original) {
                        Integer i = idx.get(tok);
                        if (i == null) continue;  // unknown card id
                        boolean sel = "true".equalsIgnoreCase(selectable[i]);
                        boolean pre = preselected != null && i < preselected.length
                                && "true".equalsIgnoreCase(preselected[i]);
                        if (sel || pre) kept.add(tok);
                    }
                    int required = Math.max(min, mustMakeChoice ? 1 : 0);
                    if (kept.size() < required) {
                        // Rebuild: every preselected id first, then top up with selectable ids —
                        // ONE new id when returnAnyChange (iterative combination), else up to min.
                        kept.clear();
                        if (preselected != null)
                            for (int i = 0; i < ids.length && i < preselected.length; i++)
                                if ("true".equalsIgnoreCase(preselected[i])) kept.add(ids[i]);
                        int newAllowed = returnAnyChange ? 1 : Math.max(required - kept.size(), 1);
                        for (int i = 0; i < ids.length && newAllowed > 0 && kept.size() < Math.max(max, 1); i++) {
                            boolean pre = preselected != null && i < preselected.length
                                    && "true".equalsIgnoreCase(preselected[i]);
                            if (!pre && "true".equalsIgnoreCase(selectable[i]) && kept.add(ids[i])) newAllowed--;
                        }
                    }
                    while (kept.size() > max && !kept.isEmpty()) {
                        String last = null;
                        for (String s : kept) last = s;
                        kept.remove(last);
                    }
                    List<String> keptList = new ArrayList<>(kept);
                    if (!keptList.equals(original)) {
                        String fixed = String.join(",", keptList);
                        String reason = "SAFETY CLAMP: '" + response
                                + "' had non-selectable/unknown card ids → '" + fixed + "' (" + typeName + ")";
                        LOG.error(reason);
                        // TRACE ORACLE V2 (2026-07-13): typed correction record, observation only.
                        if (TraceSession.isActive()) {
                            TraceSession.recordCorrection(TraceCorrection.Kind.SELECTABLE_CLAMP,
                                response, fixed, reason);
                        }
                        return new String[]{fixed, reason};
                    }
                }
            }
        }

        // Response is valid
        return new String[]{response, ""};
    }

    private static int parseIntParam(String[] vals, int dflt) {
        if (vals == null || vals.length == 0) return dflt;
        try { return Integer.parseInt(vals[0]); } catch (NumberFormatException e) { return dflt; }
    }

    private static String firstOrNull(String[] vals) {
        return (vals != null && vals.length > 0) ? vals[0] : null;
    }

    /**
     * Get an emergency response for any decision.
     *
     * This is the LAST resort - called when all other handlers fail.
     * It will ALWAYS return a valid response.
     */
    public static SafetyDecision getEmergencyResponse(AwaitingDecision decision,
                                                       String[] actionIds,
                                                       String[] cardIds) {
        String decisionType = decision.getDecisionType().name();
        String decisionId = String.valueOf(decision.getAwaitingDecisionId());
        String decisionText = decision.getText() != null ? decision.getText() : "";
        boolean mustMakeChoice = mustChoose(decision);

        LOG.warn("EMERGENCY RESPONSE for {}: '{}'",
            decisionType, truncate(decisionText, 50));
        LOG.warn("   mustChoose={}, actions={}, cards={}",
            mustMakeChoice,
            actionIds != null ? actionIds.length : 0,
            cardIds != null ? cardIds.length : 0);

        String responseValue = "";
        String reason = "";

        // Handle each decision type
        switch (decisionType) {
            case "INTEGER":
                // For INTEGER, use 0 (safer - preserves resources)
                responseValue = "0";
                reason = "Emergency: INTEGER decision, using 0";
                break;

            case "MULTIPLE_CHOICE":
                // For yes/no questions, try to be conservative
                String textLower = decisionText.toLowerCase(Locale.ROOT);
                if (textLower.contains("concede") || textLower.contains("forfeit") ||
                    textLower.contains("surrender")) {
                    responseValue = "1";  // Usually "No" is option 1
                    reason = "Emergency: Detected concede/forfeit, choosing No";
                } else {
                    responseValue = "0";  // Default to first option
                    reason = "Emergency: MULTIPLE_CHOICE, choosing first option";
                }
                break;

            case "CARD_ACTION_CHOICE":
            case "ACTION_CHOICE":
                if (actionIds != null && actionIds.length > 0) {
                    responseValue = actionIds[RANDOM.nextInt(actionIds.length)];
                    reason = "Emergency: Choosing random action (" + responseValue + ")";
                } else if (!mustMakeChoice) {
                    responseValue = "";
                    reason = "Emergency: No actions, passing allowed";
                } else {
                    responseValue = "";
                    reason = "Emergency: No actions but must choose - will likely fail";
                }
                break;

            case "CARD_SELECTION":
            case "ARBITRARY_CARDS":
                if (cardIds != null && cardIds.length > 0) {
                    responseValue = cardIds[RANDOM.nextInt(cardIds.length)];
                    reason = "Emergency: Selecting random card (" + responseValue + ")";
                } else if (!mustMakeChoice) {
                    responseValue = "";
                    reason = "Emergency: No cards, passing allowed";
                } else {
                    responseValue = "";
                    reason = "Emergency: No cards but must choose - will likely fail";
                }
                break;

            default:
                // Unknown decision type
                LOG.error("UNKNOWN DECISION TYPE: {}", decisionType);
                if (actionIds != null && actionIds.length > 0) {
                    responseValue = actionIds[RANDOM.nextInt(actionIds.length)];
                    reason = "Emergency: Unknown type, picking random action";
                } else if (cardIds != null && cardIds.length > 0) {
                    responseValue = cardIds[RANDOM.nextInt(cardIds.length)];
                    reason = "Emergency: Unknown type, picking random card";
                } else {
                    responseValue = "0";
                    reason = "Emergency: Unknown type '" + decisionType + "', guessing '0'";
                }
        }

        // FINAL SAFETY CHECK - if we must choose and response is empty, force a pick
        if (mustMakeChoice && (responseValue == null || responseValue.isEmpty())) {
            String[] allOptions = actionIds != null && actionIds.length > 0 ? actionIds : cardIds;
            if (allOptions != null && allOptions.length > 0) {
                responseValue = allOptions[RANDOM.nextInt(allOptions.length)];
                reason += " -> SAFETY OVERRIDE: forced random pick (" + responseValue + ")";
                LOG.error("SAFETY OVERRIDE: Must choose but had empty response, forcing: {}", responseValue);
            }
        }

        LOG.warn("   -> Response: '{}' ({})", responseValue, reason);

        return new SafetyDecision(decisionId, responseValue, reason, true);
    }

    /**
     * Validate that a response is likely valid for the given decision.
     *
     * @return array of [isValid, warningMessage]
     */
    public static Object[] validateResponse(AwaitingDecision decision, String response,
                                            String[] actionIds, String[] cardIds) {
        String decisionType = decision.getDecisionType().name();
        boolean mustMakeChoice = mustChoose(decision);

        // Validate based on type
        if ((response == null || response.isEmpty()) && mustMakeChoice) {
            return new Object[]{false, "Empty response but must choose - might fail"};
        }

        if ("CARD_ACTION_CHOICE".equals(decisionType) || "ACTION_CHOICE".equals(decisionType)) {
            if (response != null && !response.isEmpty() && actionIds != null) {
                boolean found = false;
                for (String aid : actionIds) {
                    if (aid.equals(response)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return new Object[]{false, "Response '" + response + "' not in action_ids"};
                }
            }
        }

        if ("CARD_SELECTION".equals(decisionType)) {
            if (response != null && !response.isEmpty() && cardIds != null) {
                boolean found = false;
                for (String cid : cardIds) {
                    if (cid.equals(response)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return new Object[]{false, "Response '" + response + "' not in card_ids"};
                }
            }
        }

        return new Object[]{true, ""};
    }

    /**
     * Get a safe "pass" value for the decision, if passing is allowed.
     *
     * @return null if passing is not allowed, empty string if allowed
     */
    public static String getSafePassValue(AwaitingDecision decision) {
        if (canPass(decision) && !mustChoose(decision)) {
            return "";
        }
        return null;
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
