package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Pure DEPLOY-2 formation and destination scoring over adapter-produced facts. */
public final class DeployFormationSitingPolicy {

    private static final String PRODUCER_ID = "DEPLOY_FORMATION_SITING_POLICY";
    private DeployFormationSitingPolicy() {
    }

    /** Emits the live formation operations in legacy source order. */
    public static PolicyResult evaluate(String actionId,
                                        CharacterFormationFacts formation) {
        Objects.requireNonNull(actionId, "actionId");
        Objects.requireNonNull(formation, "formation");

        List<PolicyOperation> operations = new ArrayList<>(3);
        addCommittedReinforcement(operations, actionId, formation);
        addBuddyTopology(operations, actionId, formation);
        addSoloVulnerability(operations, actionId, formation);
        return new PolicyResult(PRODUCER_ID, operations);
    }

    public static PolicyResult evaluateCommittedReinforcement(
            String actionId, CharacterFormationFacts formation) {
        Objects.requireNonNull(actionId, "actionId");
        Objects.requireNonNull(formation, "formation");

        List<PolicyOperation> operations = new ArrayList<>(1);
        addCommittedReinforcement(operations, actionId, formation);
        return new PolicyResult(PRODUCER_ID, operations);
    }

    public static PolicyResult evaluateBuddyTopology(
            String actionId, CharacterFormationFacts formation) {
        Objects.requireNonNull(actionId, "actionId");
        Objects.requireNonNull(formation, "formation");

        List<PolicyOperation> operations = new ArrayList<>(2);
        addBuddyTopology(operations, actionId, formation);
        addSoloVulnerability(operations, actionId, formation);
        return new PolicyResult(PRODUCER_ID, operations);
    }

    public static LegacySoloEvaluation evaluateLegacySolo(LegacySoloFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);
        LegacySoloOutcome outcome = LegacySoloOutcome.NONE;

        if (facts.eligible() && facts.wouldBeSolo()) {
            if (facts.objectiveFlipDeploy()) {
                if (facts.escapeRoute()) {
                    outcome = LegacySoloOutcome.OBJECTIVE_WITH_ESCAPE;
                    addSoloFormation(operations, facts.actionId(), "V29-obj-flip", 50.0f,
                            String.format(
                                    "V29 OBJ-FLIP: %s solo at '%s' to help flip objective — escape route exists!",
                                    facts.cardName(), facts.destinationTitle()));
                } else {
                    outcome = LegacySoloOutcome.OBJECTIVE_NO_ESCAPE;
                    addSoloFormation(operations, facts.actionId(), "V29-obj-flip", -150.0f,
                            String.format(
                                    "V29 OBJ-FLIP: %s solo at '%s' for flip but NO escape route — risky!",
                                    facts.cardName(), facts.destinationTitle()));
                }
            } else if (facts.stagingDeploy()) {
                outcome = LegacySoloOutcome.STAGING;
                addSoloFormation(operations, facts.actionId(), "V38-staging", -80.0f,
                        String.format(
                                "V38 STAGING: %s to non-battleground — move to buddy up next turn",
                                facts.cardName()));
            } else {
                outcome = LegacySoloOutcome.CAUTION;
                addSoloFormation(operations, facts.actionId(), "V38-solo-caution", -150.0f,
                        String.format(
                                "V38 SOLO CAUTION: %s (power %d) solo — vulnerable but acceptable",
                                facts.cardName(), facts.cardPower()));
            }
        }

        return new LegacySoloEvaluation(new PolicyResult(PRODUCER_ID, operations), outcome);
    }

    public static StrongReinforcementEvaluation evaluateStrongReinforcement(
            StrongReinforcementFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);
        StrongReinforcementOutcome outcome = StrongReinforcementOutcome.NONE;

        if (facts.eligible() && facts.vaderHere()) {
            outcome = StrongReinforcementOutcome.VADER;
            addDeploySiting(operations, facts.actionId(), "V38-reinforce-vader", 400.0f,
                    String.format(
                            "V38 REINFORCE VADER: Deploy %s with Vader — buddy rotation!",
                            facts.cardName()));
        } else if (facts.eligible() && facts.strongAllyHere()) {
            boolean reachesBuddyThreshold = facts.allyAbility() + facts.deployingAbility()
                    >= facts.buddyAbilityThreshold();
            outcome = reachesBuddyThreshold
                    ? StrongReinforcementOutcome.ALLY_REACHES_THRESHOLD
                    : StrongReinforcementOutcome.ALLY;
            addDeploySiting(operations, facts.actionId(), "V38-reinforce-ally",
                    reachesBuddyThreshold ? 300.0f : 200.0f,
                    String.format(
                            "V38 REINFORCE ALLY: Deploy %s to strong ally (ability %.0f + %.0f = %.0f)",
                            facts.cardName(), facts.allyAbility(), facts.deployingAbility(),
                            facts.allyAbility() + facts.deployingAbility()));
        }

        return new StrongReinforcementEvaluation(
                new PolicyResult(PRODUCER_ID, operations), outcome);
    }

    public static BuddySeekEvaluation evaluateBuddySeek(BuddySeekFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);
        BuddySeekOutcome outcome = BuddySeekOutcome.NONE;

        if (facts.eligible() && facts.vulnerableSoloAlly()) {
            if (!facts.battleground()) {
                outcome = BuddySeekOutcome.NON_BATTLEGROUND_SKIP;
            } else {
                outcome = BuddySeekOutcome.PROTECT;
                addSoloFormation(operations, facts.actionId(), "V29-buddy-seek", 200.0f,
                        String.format(
                                "V29 BUDDY-SEEK: Deploy to protect vulnerable %s (power %d) at %s!",
                                facts.soloAllyName(), facts.soloAllyPower(),
                                facts.destinationTitle()));
            }
        }

        return new BuddySeekEvaluation(new PolicyResult(PRODUCER_ID, operations), outcome);
    }

    public static HuntGroupingEvaluation evaluateHuntGrouping(HuntGroupingFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);
        HuntGroupingOutcome outcome = HuntGroupingOutcome.NONE;

        if (facts.eligible() && !facts.deployingVader()) {
            if (facts.deploysToVaderLocation()) {
                if (facts.opponentPowerAtVaderLocation() > 0.0f) {
                    outcome = HuntGroupingOutcome.GROUP_AND_ENGAGE;
                    float score = 350.0f + (facts.cardPower() >= 5 ? 50.0f : 0.0f);
                    addDeploySiting(operations, facts.actionId(), "V35.1-hunt-group", score,
                            String.format(
                                    "V35.1 HUNT GROUP+ENGAGE: Deploy %s WITH Vader at %s — opponents here (power %.0f)!",
                                    facts.cardName(), facts.vaderLocationTitle(),
                                    facts.opponentPowerAtVaderLocation()));
                } else {
                    outcome = HuntGroupingOutcome.GROUP_EMPTY;
                    addDeploySiting(operations, facts.actionId(), "V35.1-hunt-group", 50.0f,
                            String.format(
                                    "V35.1 HUNT GROUP (EMPTY): Deploy %s with Vader at %s — but NO opponents here!",
                                    facts.cardName(), facts.vaderLocationTitle()));
                }
            } else if (!facts.objectiveRelevantElsewhere()) {
                outcome = HuntGroupingOutcome.SCATTER_NEUTRAL;
                addDeploySiting(operations, facts.actionId(), "V40-hunt-scatter", 0.0f,
                        String.format(
                                "V40 HUNT SCATTER: %s deploying away from Vader at %s (neutral)",
                                facts.cardName(), facts.vaderLocationTitle()));
            }
        }

        return new HuntGroupingEvaluation(new PolicyResult(PRODUCER_ID, operations), outcome);
    }

    public static PolicyResult scoreHighDrainSite(HighDrainSiteFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);
        if (facts.opponentIcons() >= 2) {
            addDeploySiting(operations, facts.actionId(), "V40-high-drain", 200.0f,
                    String.format(
                            "V40 HIGH DRAIN: %s has %d opponent force icons — high drain potential!",
                            facts.destinationTitle(), facts.opponentIcons()));
        }
        return new PolicyResult(PRODUCER_ID, operations);
    }

    public static PolicyResult scoreGoodDrainSite(GoodDrainSiteFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);
        if (facts.gameTextKnown() && !facts.drainReduction()) {
            addDeploySiting(operations, facts.actionId(), "V40-good-drain", 100.0f,
                    String.format(
                            "V40 GOOD DRAIN SITE: %s has no drain reduction in game text!",
                            facts.destinationTitle()));
        }
        return new PolicyResult(PRODUCER_ID, operations);
    }

    public static PositiveFormationEvaluation evaluatePositiveFormation(
            PositiveFormationFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(3);
        List<PositiveFormationOutcome> outcomes = new ArrayList<>(3);

        if (facts.friendlyCount() > 0 && facts.ourDrain() >= 2.0f) {
            outcomes.add(PositiveFormationOutcome.FORTIFY_BATTLEGROUND);
            addDeploySiting(operations, facts.actionId(), "V51-fortify", 500.0f,
                    String.format(
                            "V51 FORTIFY BATTLEGROUND: Joining %d friendlies at %s (our drain %.0f) — this is THE fight!",
                            facts.friendlyCount(), facts.destinationTitle(), facts.ourDrain()));
        } else if (facts.friendlyCount() == 0 && facts.ourDrain() >= 2.0f) {
            outcomes.add(PositiveFormationOutcome.ESTABLISH_BATTLEGROUND);
            addDeploySiting(operations, facts.actionId(), "V51-establish", 400.0f,
                    String.format(
                            "V51 ESTABLISH BATTLEGROUND: First deploy to %s (our drain %.0f) — start the army!",
                            facts.destinationTitle(), facts.ourDrain()));
        } else if (facts.friendlyCount() > 0) {
            outcomes.add(PositiveFormationOutcome.REINFORCE);
            addDeploySiting(operations, facts.actionId(), "V51-reinforce", 300.0f,
                    String.format("V51 REINFORCE: Joining %d friendlies at %s!",
                            facts.friendlyCount(), facts.destinationTitle()));
        }

        float totalAbility = facts.friendlyAbility() + facts.deployingAbility();
        if (facts.friendlyCount() > 0 && facts.friendlyAbility() < 4.0f
                && totalAbility >= 4.0f) {
            outcomes.add(PositiveFormationOutcome.BUDDY_DESTINY);
            addDeploySiting(operations, facts.actionId(), "V51-buddy-destiny", 400.0f,
                    String.format(
                            "V51 BUDDY DESTINY: Ability %.0f → %.0f (>= 4) at %s — battle destiny ENABLED!",
                            facts.friendlyAbility(), totalAbility, facts.destinationTitle()));
        } else if (facts.friendlyCount() > 0 && totalAbility >= 7.0f) {
            outcomes.add(PositiveFormationOutcome.BUDDY_FULL);
            addDeploySiting(operations, facts.actionId(), "V51-buddy-full", 500.0f,
                    String.format(
                            "V51 BUDDY FULL: Ability total %.0f >= 7 at %s — full buddy system!",
                            totalAbility, facts.destinationTitle()));
        } else if (facts.friendlyCount() > 0 && totalAbility >= 4.0f) {
            outcomes.add(PositiveFormationOutcome.BUDDY_REINFORCE);
            addDeploySiting(operations, facts.actionId(), "V51-buddy-reinforce", 200.0f,
                    String.format(
                            "V51 BUDDY REINFORCE: Ability %.0f → %.0f at %s — building toward 7!",
                            facts.friendlyAbility(), totalAbility, facts.destinationTitle()));
        }

        String cardLower = facts.cardName().toLowerCase(Locale.ROOT);
        boolean armed = cardLower.contains("lightsaber") || cardLower.contains("blaster")
                || cardLower.contains("with lightsaber") || cardLower.contains("with blaster");
        if ((facts.ourDrain() >= 2.0f || facts.friendlyCount() > 0) && armed) {
            outcomes.add(PositiveFormationOutcome.ARMED);
            addDeploySiting(operations, facts.actionId(), "V51-armed", 150.0f,
                    String.format("V51 ARMED: %s brings a weapon to %s — ready for battle!",
                            facts.cardName(), facts.destinationTitle()));
        }

        return new PositiveFormationEvaluation(
                new PolicyResult(PRODUCER_ID, operations), outcomes);
    }

    public static AbilityThresholdEvaluation evaluateAbilityThreshold(
            AbilityThresholdFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);
        AbilityThresholdOutcome outcome = AbilityThresholdOutcome.NONE;
        float totalAfterDeploy = facts.currentFriendlyAbility()
                + facts.deployingAbility();

        if (totalAfterDeploy >= 4.0f) {
            if (facts.friendlyCharacterCount() > 0
                    && facts.currentFriendlyAbility() < 4.0f) {
                outcome = AbilityThresholdOutcome.FIXES_DEFICIT;
                addDeploySiting(operations, facts.actionId(),
                        "V32-ability-fix", 150.0f, String.format(
                                "V32 ABILITY FIX: Deploy brings ability from %.0f to %.0f (>= 4) at %s!",
                                facts.currentFriendlyAbility(), totalAfterDeploy,
                                facts.destinationTitle()));
            }
        } else if (facts.friendlyCharacterCount() == 0) {
            if (facts.followUpAvailable()) {
                outcome = AbilityThresholdOutcome.SOLO_WITH_FOLLOW_UP;
                addDeploySiting(operations, facts.actionId(),
                        "V40-ability-solo-follow-up", 0.0f, String.format(
                                "V40 ABILITY: Solo ability %.0f < 4 at %s — follow-up in hand, deploy freely",
                                facts.deployingAbility(), facts.destinationTitle()));
            } else {
                outcome = AbilityThresholdOutcome.SOLO_NO_FOLLOW_UP;
                addDeploySiting(operations, facts.actionId(),
                        "V40-ability-solo", 0.0f, String.format(
                                "V40 ABILITY: Solo deploy with ability %.0f < 4 at %s — deploy anyway",
                                facts.deployingAbility(), facts.destinationTitle()));
            }
        } else {
            outcome = AbilityThresholdOutcome.SHARED_BELOW_THRESHOLD;
            addDeploySiting(operations, facts.actionId(),
                    "V40-ability-shared", 0.0f, String.format(
                            "V40 ABILITY: Total ability %.0f still < 4 at %s after deploy (neutral)",
                            totalAfterDeploy, facts.destinationTitle()));
        }

        return new AbilityThresholdEvaluation(
                new PolicyResult(PRODUCER_ID, operations), outcome);
    }

    public static BuddyAbilityEvaluation evaluateBuddyAbility(
            BuddyAbilityFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);
        BuddyAbilityOutcome outcome = BuddyAbilityOutcome.NONE;

        if (!facts.battleground()) {
            if (facts.friendlyPresent()) {
                outcome = BuddyAbilityOutcome.NON_BATTLEGROUND_STACK;
                addDeploySiting(operations, facts.actionId(),
                        "V67ag-non-bg-stack", -300.0f, String.format(
                                "V67ag NON-BG STACK PENALTY: %s already has %s — additional character at non-BG can't battle, deploys to a battleground instead!",
                                facts.destinationTitle(),
                                facts.existingFriendlyTitle()));
            } else {
                outcome = BuddyAbilityOutcome.NON_BATTLEGROUND_SKIP;
            }
        } else if (facts.currentFriendlyAbility() < facts.buddyThreshold()) {
            float totalAfterDeploy = facts.currentFriendlyAbility()
                    + facts.deployingAbility();
            if (totalAfterDeploy >= facts.buddyThreshold()) {
                outcome = BuddyAbilityOutcome.REACHES_THRESHOLD;
                addDeploySiting(operations, facts.actionId(),
                        "V33-buddy-fix", 150.0f, String.format(
                                "V33 BUDDY FIX: Deploy brings ability from %.0f to %.0f (>= %d) at %s!",
                                facts.currentFriendlyAbility(), totalAfterDeploy,
                                facts.buddyThreshold(), facts.destinationTitle()));
            } else if (facts.currentFriendlyAbility() > 0.0f) {
                outcome = BuddyAbilityOutcome.REINFORCES;
                addDeploySiting(operations, facts.actionId(),
                        "V33-buddy-bonus", 100.0f, String.format(
                                "V33 BUDDY BONUS: Reinforcing ability at %s (%.0f → %.0f, target %d)",
                                facts.destinationTitle(), facts.currentFriendlyAbility(),
                                totalAfterDeploy, facts.buddyThreshold()));
            }
        }

        return new BuddyAbilityEvaluation(
                new PolicyResult(PRODUCER_ID, operations), outcome);
    }

    public enum LegacySoloOutcome {
        NONE,
        OBJECTIVE_WITH_ESCAPE,
        OBJECTIVE_NO_ESCAPE,
        STAGING,
        CAUTION
    }

    public enum StrongReinforcementOutcome {
        NONE,
        VADER,
        ALLY,
        ALLY_REACHES_THRESHOLD
    }

    public enum BuddySeekOutcome {
        NONE,
        NON_BATTLEGROUND_SKIP,
        PROTECT
    }

    public enum HuntGroupingOutcome {
        NONE,
        GROUP_AND_ENGAGE,
        GROUP_EMPTY,
        SCATTER_NEUTRAL
    }

    public enum PositiveFormationOutcome {
        FORTIFY_BATTLEGROUND,
        ESTABLISH_BATTLEGROUND,
        REINFORCE,
        BUDDY_DESTINY,
        BUDDY_FULL,
        BUDDY_REINFORCE,
        ARMED
    }

    public enum AbilityThresholdOutcome {
        NONE,
        FIXES_DEFICIT,
        SOLO_NO_FOLLOW_UP,
        SOLO_WITH_FOLLOW_UP,
        SHARED_BELOW_THRESHOLD
    }

    public enum BuddyAbilityOutcome {
        NONE,
        NON_BATTLEGROUND_STACK,
        NON_BATTLEGROUND_SKIP,
        REACHES_THRESHOLD,
        REINFORCES
    }

    public record LegacySoloEvaluation(PolicyResult result, LegacySoloOutcome outcome) {
    }

    public record StrongReinforcementEvaluation(
            PolicyResult result, StrongReinforcementOutcome outcome) {
    }

    public record BuddySeekEvaluation(PolicyResult result, BuddySeekOutcome outcome) {
    }

    public record HuntGroupingEvaluation(PolicyResult result, HuntGroupingOutcome outcome) {
    }

    public record PositiveFormationEvaluation(
            PolicyResult result, List<PositiveFormationOutcome> outcomes) {
        public PositiveFormationEvaluation {
            outcomes = List.copyOf(outcomes);
        }
    }

    public record AbilityThresholdEvaluation(
            PolicyResult result, AbilityThresholdOutcome outcome) {
    }

    public record BuddyAbilityEvaluation(
            PolicyResult result, BuddyAbilityOutcome outcome) {
    }

    public record LegacySoloFacts(String actionId, String cardName, int cardPower,
                                  String destinationTitle, boolean eligible,
                                  boolean wouldBeSolo, boolean objectiveFlipDeploy,
                                  boolean escapeRoute, boolean stagingDeploy) {
        public LegacySoloFacts {
            Objects.requireNonNull(actionId, "actionId");
            cardName = cardName == null ? "" : cardName;
            destinationTitle = destinationTitle == null ? "?" : destinationTitle;
        }
    }

    public record StrongReinforcementFacts(
            String actionId, String cardName, boolean eligible,
            boolean vaderHere, boolean strongAllyHere, float allyAbility,
            float deployingAbility, float buddyAbilityThreshold) {
        public StrongReinforcementFacts {
            Objects.requireNonNull(actionId, "actionId");
            cardName = cardName == null ? "" : cardName;
        }
    }

    public record BuddySeekFacts(
            String actionId, boolean eligible,
            boolean vulnerableSoloAlly, boolean battleground,
            String soloAllyName, int soloAllyPower, String destinationTitle) {
        public BuddySeekFacts {
            Objects.requireNonNull(actionId, "actionId");
            soloAllyName = soloAllyName == null ? "" : soloAllyName;
            destinationTitle = destinationTitle == null ? "" : destinationTitle;
        }
    }

    public record HuntGroupingFacts(
            String actionId, boolean eligible, String cardName, int cardPower,
            String vaderLocationTitle, boolean deploysToVaderLocation,
            boolean deployingVader, float opponentPowerAtVaderLocation,
            boolean objectiveRelevantElsewhere) {
        public HuntGroupingFacts {
            Objects.requireNonNull(actionId, "actionId");
            cardName = cardName == null ? "" : cardName;
            vaderLocationTitle = vaderLocationTitle == null ? "" : vaderLocationTitle;
        }
    }

    public record HighDrainSiteFacts(
            String actionId, String destinationTitle, int opponentIcons) {
        public HighDrainSiteFacts {
            Objects.requireNonNull(actionId, "actionId");
            destinationTitle = destinationTitle == null ? "" : destinationTitle;
        }
    }

    public record GoodDrainSiteFacts(
            String actionId, String destinationTitle,
            boolean gameTextKnown, boolean drainReduction) {
        public GoodDrainSiteFacts {
            Objects.requireNonNull(actionId, "actionId");
            destinationTitle = destinationTitle == null ? "" : destinationTitle;
        }
    }

    public record PositiveFormationFacts(
            String actionId, String cardName, String destinationTitle,
            int friendlyCount, float friendlyAbility,
            float deployingAbility, float ourDrain) {
        public PositiveFormationFacts {
            Objects.requireNonNull(actionId, "actionId");
            cardName = cardName == null ? "" : cardName;
            destinationTitle = destinationTitle == null ? "" : destinationTitle;
        }
    }

    public record AbilityThresholdFacts(
            String actionId, String destinationTitle,
            float currentFriendlyAbility, int friendlyCharacterCount,
            float deployingAbility, boolean followUpAvailable) {
        public AbilityThresholdFacts {
            Objects.requireNonNull(actionId, "actionId");
            destinationTitle = destinationTitle == null ? "" : destinationTitle;
        }
    }

    public record BuddyAbilityFacts(
            String actionId, String destinationTitle,
            boolean battleground, boolean friendlyPresent,
            String existingFriendlyTitle, float currentFriendlyAbility,
            float deployingAbility, int buddyThreshold) {
        public BuddyAbilityFacts {
            Objects.requireNonNull(actionId, "actionId");
            destinationTitle = destinationTitle == null ? "" : destinationTitle;
        }
    }

    private static void addCommittedReinforcement(
            List<PolicyOperation> operations, String actionId,
            CharacterFormationFacts facts) {
        if (!facts.eligible()) {
            return;
        }

        float deficit = facts.opponentPower() - facts.ourPower();
        boolean outgunned = deficit >= 4.0f && deficit <= 5.0f;
        if (facts.ourPower() > 0.0f
                && facts.friendlyCharactersHere() >= 1
                && outgunned
                && !facts.committedFormationCanEscape()) {
            addDeploySiting(operations, actionId, "V67bn", 800.0f,
                    String.format(
                            "V67bn REINFORCE OUTGUNNED (Braveheart): %d friendly char(s) at %s (our %d vs opp %d, deficit %d) \u2014 NO ESCAPE, DEPLOY HERE to minimize overflow!",
                            facts.friendlyCharactersHere(), facts.destinationTitle(),
                            (int) facts.ourPower(), (int) facts.opponentPower(),
                            (int) deficit));
        }
    }

    private static void addBuddyTopology(List<PolicyOperation> operations,
                                         String actionId,
                                         CharacterFormationFacts facts) {
        if (!facts.eligible()) {
            return;
        }

        if (facts.ourLocation()) {
            addSoloFormation(operations, actionId, "V29.5-buddy", 40.0f,
                    "V29.5 BUDDY: Own location \u2014 home field advantage");
        } else if (facts.opponentLocation()) {
            if (facts.emptyTable()) {
                addSoloFormation(operations, actionId, "V29.5-buddy", -20.0f,
                        "V29.6 BUDDY: Opponent's location but empty table \u2014 must deploy somewhere!");
            } else if (facts.friendlyCharactersHere() == 0
                    && facts.opponentCharactersHere() == 0) {
                addSoloFormation(operations, actionId, "V29.5-buddy", -150.0f,
                        "V29.5 BUDDY: Opponent's location, deploying ALONE \u2014 risky!");
            } else if (facts.friendlyCharactersHere() == 0
                    && facts.opponentCharactersHere() > 0) {
                addSoloFormation(operations, actionId, "V29.5-buddy", -100.0f,
                        "V29.5 BUDDY: Opponent's location with enemies, NO friendlies \u2014 dangerous!");
            } else if (facts.friendlyCharactersHere() > 0) {
                addSoloFormation(operations, actionId, "V29.5-buddy", 10.0f,
                        "V29.5 BUDDY: Opponent's location but friendlies present");
            }
        }
    }

    private static void addSoloVulnerability(
            List<PolicyOperation> operations, String actionId,
            CharacterFormationFacts facts) {
        if (facts.eligible()
                && facts.friendlyCharactersHere() == 0
                && !facts.emptyTable()
                && facts.deployingAbility() >= 3.0f) {
            addSoloFormation(operations, actionId, "V113", -300.0f,
                    String.format(
                            "V113 SOLO VULNERABILITY: %s (ability %.0f) alone at %s \u2014 opponent can overwhelm next turn!",
                            facts.deployingCardName(), facts.deployingAbility(),
                            facts.destinationTitle()));
        }
    }

    /** Facts for V67bn/V67bu, V29.5 buddy topology, and V113. */
    public record CharacterFormationFacts(
            boolean eligible,
            String destinationTitle,
            boolean ourLocation,
            boolean opponentLocation,
            int friendlyCharactersHere,
            int opponentCharactersHere,
            boolean emptyTable,
            String deployingCardName,
            float deployingAbility,
            float ourPower,
            float opponentPower,
            boolean committedFormationCanEscape) {
        public CharacterFormationFacts {
            destinationTitle = destinationTitle == null ? "" : destinationTitle;
            deployingCardName = deployingCardName == null ? "" : deployingCardName;
        }
    }

    private static void addDeploySiting(List<PolicyOperation> operations,
                                        String actionId, String ruleId,
                                        float delta, String reason) {
        add(operations, actionId, ruleId, TraceDomainId.DEPLOY_SITING,
                delta, reason);
    }

    private static void addSoloFormation(List<PolicyOperation> operations,
                                         String actionId, String ruleId,
                                         float delta, String reason) {
        add(operations, actionId, ruleId, TraceDomainId.SOLO_FORMATION,
                delta, reason);
    }

    private static void add(List<PolicyOperation> operations,
                            String actionId, String ruleId,
                            TraceDomainId domainId, float delta,
                            String reason) {
        operations.add(PolicyOperation.add(actionId, TraceRuleId.of(ruleId),
                domainId, TraceOutputKind.BANDED, delta, reason));
    }
}
