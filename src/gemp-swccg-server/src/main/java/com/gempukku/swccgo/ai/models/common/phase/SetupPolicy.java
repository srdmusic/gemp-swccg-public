package com.gempukku.swccgo.ai.models.common.phase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Shared SETUP scoring tree. Evaluator adapters retain card lookup, candidate
 * order, action construction, exception handling, and terminal returns.
 */
public final class SetupPolicy {
    private static final Set<String> EARLY_BANNED_EFFECTS = Set.of(
            "no escape",
            "no escape (v)",
            "coarse and rough and irritating",
            "tentacle");

    private static final Pattern FORCE_GENERATION_INCREASE = Pattern.compile(
            "force\\s+generation\\s*(?:is|are|of|by)?\\s*[+]?\\s*[1-9]");

    public enum Branch {
        EARLY_BANNED,
        EARLY_ALLOWED,
        INTERRUPT_EPIC,
        INTERRUPT_THREE_EFFECTS,
        INTERRUPT_TWO_EFFECTS,
        INTERRUPT_EFFECTS,
        INTERRUPT_DEPLOY,
        INTERRUPT_GENERIC,
        LOCATION_MENTIONED,
        LOCATION_OBJECTIVE,
        LOCATION_CC_EXTERIOR,
        LOCATION_CC_INTERIOR,
        LOCATION_RESERVE,
        LOCATION_FORCE_GENERATION,
        LOCATION_EPIC,
        LOCATION_FUNERAL_PYRE,
        LOCATION_BATTLEGROUND,
        LOCATION_NON_BATTLEGROUND,
        LOCATION_SITH_NON_PALACE,
        LOCATION_SITH_PALACE,
        LOCATION_SITH_NON_BATTLEGROUND,
        EFFECT_BANNED,
        EFFECT_DUPLICATE,
        EFFECT_PREFERRED,
        EFFECT_SHADOW_COLLECTIVE,
        EFFECT_IWTM,
        EFFECT_BATTLE_STARTER,
        EFFECT_FORCE_GENERATION,
        EFFECT_ROTS_SYNERGY,
        EFFECT_SKYWALKER,
        EFFECT_HUNT_DOWN_REQUIRED,
        EFFECT_HUNT_DOWN_OTHER,
        EFFECT_OBJECTIVE_LOCATION,
        EFFECT_OBJECTIVE_REQUIRED_CARD,
        EFFECT_LOCATION_PULL,
        EFFECT_RESERVE_ACCESS,
        EFFECT_FORCE_ECONOMY,
        EFFECT_EPIC,
        RESERVE_SKYWALKER,
        RESERVE_SHADOW_COLLECTIVE,
        SAGA_CORRECT,
        SAGA_WRONG,
        SAGA_DEFAULT
    }

    public record Contribution(Branch branch, String reason, float delta) {
    }

    public record StartingCandidate(String actionId, String title) {
    }

    public record EarlyBanCandidate(
            String actionId, boolean banned, float score, String reason) {
    }

    public record EarlyBanEvaluation(
            boolean terminalDecision, List<EarlyBanCandidate> candidates) {
    }

    public record BattlegroundEvaluation(
            Contribution contribution,
            boolean battleground,
            String battlegroundReason) {
    }

    public record StartingEffectEvaluation(
            List<Contribution> contributions, boolean terminalCandidate) {
    }

    public record SagaEvaluation(
            boolean sagaChoice, Contribution contribution) {
    }

    public record SagaSelection(
            boolean sagaChoice, int index, String reason) {
    }

    private SetupPolicy() {
    }

    public static boolean isSetupTurn(int turnNumber) {
        return turnNumber <= 0;
    }

    public static EarlyBanEvaluation earlyStartingEffectBan(
            List<StartingCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return new EarlyBanEvaluation(false, List.of());
        }

        boolean hasBanned = false;
        List<Boolean> banned = new ArrayList<>(candidates.size());
        for (StartingCandidate candidate : candidates) {
            boolean candidateBanned = isEarlyBannedTitle(candidate.title());
            banned.add(candidateBanned);
            hasBanned |= candidateBanned;
        }
        if (!hasBanned) {
            return new EarlyBanEvaluation(false, List.of());
        }

        List<EarlyBanCandidate> results = new ArrayList<>(candidates.size());
        for (int i = 0; i < candidates.size(); i++) {
            StartingCandidate candidate = candidates.get(i);
            boolean candidateBanned = banned.get(i);
            results.add(new EarlyBanCandidate(
                    candidate.actionId(),
                    candidateBanned,
                    candidateBanned ? -500.0f : 100.0f,
                    candidateBanned
                            ? "BANNED as starting effect"
                            : "OK as starting effect"));
        }
        return new EarlyBanEvaluation(true, List.copyOf(results));
    }

    public static Contribution startingInterrupt(String gameText) {
        String lower = lower(gameText);
        if (lower.contains("force is strong in my family")
                || lower.contains("force is strong")
                || lower.contains("epic")) {
            return new Contribution(
                    Branch.INTERRUPT_EPIC,
                    "V43 EPIC EVENT: Deploys saga Epic Event — MUST USE THIS!",
                    1500.0f);
        }

        String startingClause = lower;
        int startingIndex = lower.indexOf("starting:");
        if (startingIndex >= 0) {
            startingClause = lower.substring(startingIndex);
        }
        boolean deploysEffects = startingClause.contains("deploy")
                && startingClause.contains("effect");
        if (deploysEffects) {
            if (startingClause.contains("three effects")
                    || startingClause.contains("3 effects")) {
                return new Contribution(
                        Branch.INTERRUPT_THREE_EFFECTS,
                        "V43 EFFECT-DEPLOYER: STARTING clause deploys Effects",
                        300.0f);
            }
            if (startingClause.contains("two effects")
                    || startingClause.contains("2 effects")) {
                return new Contribution(
                        Branch.INTERRUPT_TWO_EFFECTS,
                        "V43 EFFECT-DEPLOYER: STARTING clause deploys Effects",
                        250.0f);
            }
            return new Contribution(
                    Branch.INTERRUPT_EFFECTS,
                    "V43 EFFECT-DEPLOYER: STARTING clause deploys Effects",
                    200.0f);
        }
        if (startingClause.contains("deploy")) {
            return new Contribution(
                    Branch.INTERRUPT_DEPLOY,
                    "V43: STARTING clause deploys something",
                    100.0f);
        }
        return new Contribution(
                Branch.INTERRUPT_GENERIC,
                "V43: Generic starting interrupt — no Epic Event",
                0.0f);
    }

    public static Contribution startingLocationMention(
            String title, String decisionText) {
        if (title != null && lower(decisionText).contains(lower(title))) {
            return new Contribution(
                    Branch.LOCATION_MENTIONED,
                    "V22 MENTIONED IN STARTING INTERRUPT",
                    50.0f);
        }
        return null;
    }

    public static Contribution startingLocationObjective(
            String title, boolean objectiveRelevant, float objectiveBonus) {
        if (title != null && objectiveRelevant) {
            return new Contribution(
                    Branch.LOCATION_OBJECTIVE,
                    "V22 OBJECTIVE STARTING LOCATION: " + title,
                    objectiveBonus);
        }
        return null;
    }

    public static Contribution startingLocationCloudCity(
            String title, boolean exterior, boolean interior) {
        String titleLower = lower(title);
        if (titleLower.contains("cloud city")) {
            if (exterior && !interior) {
                return new Contribution(
                        Branch.LOCATION_CC_EXTERIOR,
                        "V24.10 EXTERIOR CC STARTING LOCATION: Only way to deploy — I'm Sorry can't pull this!",
                        500.0f);
            } else if (interior) {
                return new Contribution(
                        Branch.LOCATION_CC_INTERIOR,
                        "V24.10 INTERIOR CC: Slip Sliding or I'm Sorry will pull this — save starting slot for exterior!",
                        -500.0f);
            }
        }
        return null;
    }

    public static List<Contribution> startingLocationText(
            String title, String allLocationText) {
        if (title == null) {
            return List.of();
        }

        List<Contribution> contributions = new ArrayList<>();
        String titleLower = lower(title);
        String allTextLower = lower(allLocationText);

        if (!allTextLower.isEmpty()) {
            if (allTextLower.contains("reserve")) {
                contributions.add(new Contribution(
                        Branch.LOCATION_RESERVE,
                        "V22 RESERVE PULL: starting location pulls from reserve deck",
                        75.0f));
            }
            if (allTextLower.contains("force generation")
                    || allTextLower.contains("force icon")
                    || allTextLower.contains("adds one to")) {
                contributions.add(new Contribution(
                        Branch.LOCATION_FORCE_GENERATION,
                        "V22 FORCE GEN: starting location boosts force",
                        25.0f));
            }
            if (allTextLower.contains("epic")) {
                contributions.add(new Contribution(
                        Branch.LOCATION_EPIC,
                        "V29.14 EPIC EVENT: game text mentions 'epic' — critical starting location!",
                        1000.0f));
            }
        }
        if (titleLower.contains("funeral pyre")) {
            contributions.add(new Contribution(
                    Branch.LOCATION_FUNERAL_PYRE,
                    "V29.14 FUNERAL PYRE: critical starting location for Luke Saga!",
                    1000.0f));
        }

        return List.copyOf(contributions);
    }

    public static BattlegroundEvaluation startingLocationBattleground(
            String title,
            String baseGameText,
            boolean lightForceIcon,
            boolean darkForceIcon) {
        String titleLower = lower(title);
        String baseTextLower = lower(baseGameText);

        boolean battleground = false;
        String battlegroundReason = null;
        if (baseTextLower.contains("battleground")) {
            battleground = true;
            battlegroundReason = "game text contains 'battleground'";
        } else if (titleLower.contains("battleground")) {
            battleground = true;
            battlegroundReason = "title contains 'battleground'";
        } else if (lightForceIcon && darkForceIcon) {
            battleground = true;
            battlegroundReason = "site has both LIGHT and DARK force icons";
        }

        Contribution contribution;
        if (battleground) {
            contribution = new Contribution(
                    Branch.LOCATION_BATTLEGROUND,
                    "V67o BATTLEGROUND STARTING LOC: " + title
                            + " is a battleground (" + battlegroundReason
                            + ") — drains and battles from turn 1!",
                    300.0f);
        } else {
            contribution = new Contribution(
                    Branch.LOCATION_NON_BATTLEGROUND,
                    "V67o NON-BATTLEGROUND STARTING LOC: " + title
                            + " — no force drains/battles possible here, prefer battleground!",
                    -150.0f);
        }

        return new BattlegroundEvaluation(
                contribution, battleground, battlegroundReason);
    }

    public static Contribution startingLocationSith(
            String title,
            boolean battleground,
            boolean sithStartingEffectPresent) {
        if (sithStartingEffectPresent) {
            String titleLower = lower(title);
            boolean palace = titleLower.contains("palace");
            if (battleground && !palace) {
                return new Contribution(
                        Branch.LOCATION_SITH_NON_PALACE,
                        "V67q SITH START (RotS/RevotS): " + title
                                + " is non-Palace battleground — starting Effect WILL trigger here!",
                        600.0f);
            } else if (palace) {
                return new Contribution(
                        Branch.LOCATION_SITH_PALACE,
                        "V67q SITH START PALACE: " + title
                                + " is a Palace — RotS/RevotS Effect WON'T trigger here, avoid!",
                        -350.0f);
            } else {
                return new Contribution(
                        Branch.LOCATION_SITH_NON_BATTLEGROUND,
                        "V67q SITH START NON-BG: " + title
                                + " is not a battleground — RotS/RevotS Effect cannot trigger!",
                        -300.0f);
            }
        }
        return null;
    }

    public static StartingEffectEvaluation startingEffectBan(String title) {
        if (title == null) {
            return new StartingEffectEvaluation(List.of(), false);
        }
        String titleLower = lower(title);
        if (titleLower.contains("no escape")
                || titleLower.contains("coarse and rough")) {
            return new StartingEffectEvaluation(
                    List.of(new Contribution(
                            Branch.EFFECT_BANNED,
                            "V22 STARTING BAN: " + title + " banned!",
                            -600.0f)),
                    true);
        }
        return new StartingEffectEvaluation(List.of(), false);
    }

    public static List<Contribution> startingEffectIdentity(
            String title,
            boolean copyCountKnown,
            int copyCount,
            boolean iwtmPreferred) {
        if (title == null) {
            return List.of();
        }
        List<Contribution> contributions = new ArrayList<>();
        String titleLower = lower(title);

        if (copyCountKnown && copyCount > 1) {
            contributions.add(new Contribution(
                    Branch.EFFECT_DUPLICATE,
                    "V187 DUPLICATE: Rando has " + copyCount
                            + " copies of '" + title
                            + "' — prefer a singleton starting effect",
                    -300.0f));
        }
        if (titleLower.contains("endor shield")
                || titleLower.contains("alert my star destroyer")
                || titleLower.contains("silence is golden")) {
            contributions.add(new Contribution(
                    Branch.EFFECT_PREFERRED,
                    "V22 PREFERRED STARTING EFFECT: " + title,
                    200.0f));
        }
        if (isShadowCollectivePayoff(titleLower)) {
            contributions.add(new Contribution(
                    Branch.EFFECT_SHADOW_COLLECTIVE,
                    "V22 PREFERRED STARTING EFFECT (Shadow Collective payoff): "
                            + title,
                    500.0f));
        }
        if (iwtmPreferred) {
            contributions.add(new Contribution(
                    Branch.EFFECT_IWTM,
                    "V186 PREFERRED STARTING EFFECT (I Want That Map): "
                            + title,
                    1000.0f));
        }
        return List.copyOf(contributions);
    }

    public static List<Contribution> startingEffectText(
            String title, String gameText, boolean rotsOnTable) {
        if (title == null) {
            return List.of();
        }
        List<Contribution> contributions = new ArrayList<>();
        String titleLower = lower(title);
        String gameTextLower = gameText == null ? null : lower(gameText);
        if (gameTextLower != null) {
            if (gameTextLower.contains("initiate battles for free")
                    || titleLower.contains("first strike")) {
                contributions.add(new Contribution(
                        Branch.EFFECT_BATTLE_STARTER,
                        "V126a STARTING EFFECT: free battle initiation / drain gate — strong tempo",
                        500.0f));
            }
            if (FORCE_GENERATION_INCREASE.matcher(gameTextLower).find()
                    || (gameTextLower.contains("force generation")
                    && gameTextLower.contains("+"))) {
                contributions.add(new Contribution(
                        Branch.EFFECT_FORCE_GENERATION,
                        "V126b STARTING EFFECT: increases Force generation — compounds every turn",
                        400.0f));
            }
            if (isRotsPairingCandidate(gameTextLower)
                    && rotsOnTable) {
                contributions.add(new Contribution(
                        Branch.EFFECT_ROTS_SYNERGY,
                        "V126c STARTING EFFECT: pairs with Revenge of the Sith ([Episode I] Dark Jedi lock)",
                        600.0f));
            }
        }
        return List.copyOf(contributions);
    }

    public static List<Contribution> startingEffectDeck(
            String title, boolean huntDown) {
        if (title == null) {
            return List.of();
        }
        List<Contribution> contributions = new ArrayList<>();
        String titleLower = lower(title);
        if (titleLower.contains("cunning warrior")
                || titleLower.contains("good friend")) {
            contributions.add(new Contribution(
                    Branch.EFFECT_SKYWALKER,
                    "V80 SKYWALKER STARTING EFFECT: " + title
                            + " — required for Rey/Luke Saga deck!",
                    1000.0f));
        }

        if (huntDown) {
            if (titleLower.contains("there are many hunting you now")
                    || titleLower.contains("i am your father")
                    || titleLower.contains("crush the rebellion")) {
                contributions.add(new Contribution(
                        Branch.EFFECT_HUNT_DOWN_REQUIRED,
                        "V25 HUNT DOWN STARTING EFFECT: " + title
                                + " — REQUIRED!",
                        500.0f));
            } else {
                contributions.add(new Contribution(
                        Branch.EFFECT_HUNT_DOWN_OTHER,
                        "V25 HUNT DOWN: Not a required starting effect for Hunt Down",
                        -300.0f));
            }
        }
        return List.copyOf(contributions);
    }

    public static List<Contribution> startingEffectObjective(
            String gameText,
            boolean objectiveAnalyzed,
            List<String> objectiveLocationFragments,
            List<String> requiredCardsOnTable) {
        String gameTextLower = gameText == null ? null : lower(gameText);
        if (!objectiveAnalyzed || gameTextLower == null) {
            return List.of();
        }
        List<String> locationFragments = objectiveLocationFragments == null
                ? List.of() : objectiveLocationFragments;
        List<String> requiredCards = requiredCardsOnTable == null
                ? List.of() : requiredCardsOnTable;
        List<Contribution> contributions = new ArrayList<>();
        for (String fragment : locationFragments) {
                if (gameTextLower.contains(fragment)) {
                    contributions.add(new Contribution(
                            Branch.EFFECT_OBJECTIVE_LOCATION,
                            "V22 OBJECTIVE-SYNERGY STARTING EFFECT: references '"
                                    + fragment + "'",
                            250.0f));
                    break;
                }
        }
        for (String required : requiredCards) {
                if (gameTextLower.contains(required)) {
                    contributions.add(new Contribution(
                            Branch.EFFECT_OBJECTIVE_REQUIRED_CARD,
                            "V22 OBJECTIVE-SYNERGY: pulls required card '"
                                    + required + "'",
                            200.0f));
                    break;
                }
        }
        if (gameTextLower.contains("deploy")
                    && (gameTextLower.contains("location")
                    || gameTextLower.contains("site")
                    || gameTextLower.contains("system"))) {
                contributions.add(new Contribution(
                        Branch.EFFECT_LOCATION_PULL,
                        "V22 LOCATION-PULLING EFFECT: deploys locations",
                        100.0f));
        }
        if (gameTextLower.contains("reserve")) {
                contributions.add(new Contribution(
                        Branch.EFFECT_RESERVE_ACCESS,
                        "V22 RESERVE DECK ACCESS: can pull from reserve",
                        50.0f));
        }
        if (gameTextLower.contains("force generation")
                    || gameTextLower.contains("activate")
                    || gameTextLower.contains("force icon")
                    || gameTextLower.contains("adds one to")) {
                contributions.add(new Contribution(
                        Branch.EFFECT_FORCE_ECONOMY,
                        "V22 FORCE GENERATION: boosts early force economy",
                        25.0f));
        }
        if (gameTextLower.contains("epic")
                    || gameTextLower.contains("force is strong in my family")
                    || gameTextLower.contains("force is strong")) {
                contributions.add(new Contribution(
                        Branch.EFFECT_EPIC,
                        "V43 EPIC: starting card deploys Epic Event — critical for deck strategy!",
                        1500.0f));
        }
        return List.copyOf(contributions);
    }

    public static List<Contribution> reserveStartingEffect(
            String title, boolean setupTurn) {
        String titleLower = lower(title);
        List<Contribution> contributions = new ArrayList<>();
        if (titleLower.contains("cunning warrior")
                || titleLower.contains("good friend")) {
            contributions.add(new Contribution(
                    Branch.RESERVE_SKYWALKER,
                    "V80 SKYWALKER STARTING EFFECT: " + title
                            + " — required for Rey/Luke Saga deck!",
                    1000.0f));
        }
        if (setupTurn && isShadowCollectivePayoff(titleLower)) {
            contributions.add(new Contribution(
                    Branch.RESERVE_SHADOW_COLLECTIVE,
                    "V22 PREFERRED STARTING EFFECT (Shadow Collective payoff): "
                            + title,
                    500.0f));
        }
        return List.copyOf(contributions);
    }

    public static SagaEvaluation sagaChoice(
            String deckName, String actionText) {
        String actionLower = lower(actionText);
        boolean sagaChoice = actionLower.contains("i have it")
                || actionLower.contains("my father has it")
                || actionLower.contains("you have that power");
        if (!sagaChoice) {
            return new SagaEvaluation(false, null);
        }

        String deckLower = lower(deckName);
        boolean standaloneIHaveIt = actionLower.contains("i have it")
                && !actionLower.contains("my father has it");
        boolean correct = (deckLower.contains("luke") && standaloneIHaveIt)
                || (deckLower.contains("anakin")
                    && actionLower.contains("my father has it"))
                || (deckLower.contains("rey")
                    && actionLower.contains("you have that power"));
        if (correct) {
            return new SagaEvaluation(
                    true,
                    new Contribution(
                            Branch.SAGA_CORRECT,
                            "V29.15 EPIC EVENT: Correct saga choice for '"
                                    + deckName + "' deck!",
                            1000.0f));
        }
        if (!deckLower.isEmpty()) {
            return new SagaEvaluation(
                    true,
                    new Contribution(
                            Branch.SAGA_WRONG,
                            "V29.15 EPIC EVENT: Wrong saga choice for '"
                                    + deckName + "' deck",
                            -500.0f));
        }
        if (standaloneIHaveIt) {
            return new SagaEvaluation(
                    true,
                    new Contribution(
                            Branch.SAGA_DEFAULT,
                            "V29.15 EPIC EVENT: Default to 'I Have It' (no deck name)",
                            500.0f));
        }
        return new SagaEvaluation(true, null);
    }

    public static SagaSelection chooseSaga(
            String deckName, String[] results) {
        return chooseSaga(deckName, results, 0, 0, 0);
    }

    /**
     * V61 amended in place (2026-07-27, Steve): the deck's actual persona
     * counts are the PRIMARY signal — a deck that runs 4x Rey / 3x Luke /
     * 2x Anakin wants 'You Have That Power, Too' regardless of what the deck
     * is named. Replay rgfogqxrh4uat4bo: the deck name arrived null AND the
     * name-based law would have picked Luke anyway; Rando chose 'I Have It'
     * and Rey's boost was lost. Strict argmax over the three persona counts
     * wins; ties and all-zero counts fall through to the original deck-name
     * chain, then the Luke default.
     */
    public static SagaSelection chooseSaga(
            String deckName, String[] results,
            int lukeCount, int anakinCount, int reyCount) {
        if (results == null || results.length == 0) {
            return new SagaSelection(false, -1, null);
        }

        boolean sagaChoice = false;
        int luke = -1;
        int anakin = -1;
        int rey = -1;
        for (int i = 0; i < results.length; i++) {
            String resultLower = lower(results[i]);
            if (resultLower.contains("i have it")
                    || resultLower.contains("my father has it")
                    || resultLower.contains("you have that power")) {
                sagaChoice = true;
            }
            if (resultLower.contains("my father has it")) {
                anakin = i;
            } else if (resultLower.contains("you have that power")) {
                rey = i;
            } else if (resultLower.contains("i have it")) {
                luke = i;
            }
        }
        if (!sagaChoice) {
            return new SagaSelection(false, -1, null);
        }

        int best = Math.max(lukeCount, Math.max(anakinCount, reyCount));
        if (best > 0) {
            boolean lukeBest = lukeCount == best;
            boolean anakinBest = anakinCount == best;
            boolean reyBest = reyCount == best;
            boolean unique = (lukeBest ? 1 : 0) + (anakinBest ? 1 : 0)
                    + (reyBest ? 1 : 0) == 1;
            String counts = "counts L=" + lukeCount + " A=" + anakinCount
                    + " R=" + reyCount;
            if (unique && reyBest && rey >= 0) {
                return new SagaSelection(true, rey,
                        counts + " → Rey deck → 'You Have That Power, Too'");
            }
            if (unique && lukeBest && luke >= 0) {
                return new SagaSelection(true, luke,
                        counts + " → Luke deck → 'I Have It'");
            }
            if (unique && anakinBest && anakin >= 0) {
                return new SagaSelection(true, anakin,
                        counts + " → Anakin deck → 'My Father Has It'");
            }
            // tie, or the winning persona's option missing → name chain below
        }

        String deckLower = lower(deckName);
        if (deckLower.contains("luke") && luke >= 0) {
            return new SagaSelection(
                    true, luke, "Luke deck → 'I Have It'");
        }
        if (deckLower.contains("anakin") && anakin >= 0) {
            return new SagaSelection(
                    true, anakin, "Anakin deck → 'My Father Has It'");
        }
        if (deckLower.contains("rey") && rey >= 0) {
            return new SagaSelection(
                    true, rey, "Rey deck → 'You Have That Power, Too'");
        }
        if (luke >= 0) {
            return new SagaSelection(
                    true, luke, "Default (no deck match) → 'I Have It'");
        }
        return new SagaSelection(true, -1, null);
    }

    public static boolean isRotsPairingCandidate(String gameText) {
        String lower = lower(gameText);
        return (lower.contains("[episode i]") || lower.contains("episode i"))
                && lower.contains("dark jedi");
    }

    private static boolean isEarlyBannedTitle(String title) {
        String lower = lower(title);
        for (String banned : EARLY_BANNED_EFFECTS) {
            if (lower.contains(banned)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isShadowCollectivePayoff(String titleLower) {
        return titleLower.contains("you'll be dead")
                || titleLower.contains("inconsequential losses");
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
