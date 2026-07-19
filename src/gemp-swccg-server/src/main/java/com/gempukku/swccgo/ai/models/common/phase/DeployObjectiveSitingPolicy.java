package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Shared pure owner of objective-specific DEPLOY-2 destination scores. */
public final class DeployObjectiveSitingPolicy {
    private DeployObjectiveSitingPolicy() {
    }

    public static PolicyResult evaluate(Facts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();

        if (!facts.undercoverSpy() && facts.objectiveAnalyzed()
                && facts.objectiveRelevant()) {
            operations.add(add(facts.actionId(), "V22-objective-location",
                    TraceOutputKind.BANDED, facts.objectiveLocationBonus(),
                    "OBJECTIVE LOCATION - deploy here helps flip!"));
        }

        if (facts.objectiveAnalyzed() && facts.myLord() && facts.senator()) {
            if (facts.galacticSenateDestination()) {
                operations.add(add(facts.actionId(), "V88-CS",
                        TraceOutputKind.BANDED, 1500.0f,
                        "V88 MY LORD: senator → Galactic Senate (flip target + weapon destiny -6 protection)"));
            } else {
                operations.add(add(facts.actionId(), "V88-CS",
                        TraceOutputKind.VETO, -2000.0f,
                        "V88 MY LORD: senator not at Galactic Senate — wrong site!"));
            }
        }

        if (facts.textNamesDestination()) {
            if (facts.textRejectsDestination()) {
                operations.add(add(facts.actionId(), "V88-text-named",
                        TraceOutputKind.BANDED, -500.0f,
                        "V88 TEXT-NAMED SITE: character text says NOT at '"
                                + facts.bareSiteTitle() + "' — wrong site"));
            } else if (!facts.textNamedDestinationDoomed()) {
                operations.add(add(facts.actionId(), "V88-text-named",
                        TraceOutputKind.BANDED, 500.0f,
                        "V88 TEXT-NAMED SITE: character text mentions '"
                                + facts.bareSiteTitle() + "' — home-site bonus"));
            }
        }

        if (facts.galacticSenateDestination() && facts.character()
                && !facts.senator()
                && facts.opponentPower() <= facts.friendlySenatorPower()) {
            operations.add(add(facts.actionId(), "V99-CS",
                    TraceOutputKind.VETO, -1500.0f,
                    String.format("V99 SENATE GUARD: non-senator → Galactic Senate (opp %.0f <= my senator %.0f) — wasted, deploy elsewhere",
                            facts.opponentPower(), facts.friendlySenatorPower())));
        }

        return new PolicyResult("DEPLOY_OBJECTIVE_SITING_POLICY", operations);
    }

    public static PolicyResult scoreCloudCityArmy(CloudCityArmyFacts facts) {
        Objects.requireNonNull(facts, "facts");
        return single("DEPLOY_CLOUD_CITY_ARMY_POLICY",
                add(facts.actionId(), "V51-CC-ARMY", TraceOutputKind.BANDED,
                        500.0f, String.format(
                                "V51 CC ARMY: Deploy to %s pre-flip — build Cloud City army!",
                                facts.locationTitle())));
    }

    public static PolicyResult scoreObjectiveFirst(ObjectiveFirstFacts facts) {
        Objects.requireNonNull(facts, "facts");
        return single("DEPLOY_OBJECTIVE_FIRST_POLICY",
                add(facts.actionId(), "V51-OBJ-FIRST", TraceOutputKind.BANDED,
                        300.0f, String.format(
                                "V51 OBJ FIRST: Deploy to %s — objective-relevant location pre-flip!",
                                facts.locationTitle())));
    }

    public static PolicyResult scoreKeyCharacter(KeyCharacterFacts facts) {
        Objects.requireNonNull(facts, "facts");
        return single("DEPLOY_KEY_CHARACTER_POLICY",
                add(facts.actionId(), "V67ak", TraceOutputKind.BANDED,
                        800.0f, String.format(
                                "V67ak KEY CHARACTER: %s is named in objective/epic-event text — deploy first to enable flip!",
                                facts.cardTitle())));
    }

    public static CloudCityEngineEvaluation evaluateCloudCityEngine(
            CloudCityEngineFacts facts) {
        Objects.requireNonNull(facts, "facts");
        PolicyOperation operation;
        CloudCityEngineOutcome outcome;
        if (!facts.occupyBespin()) {
            operation = add(facts.actionId(), "V22.7", TraceOutputKind.VETO,
                    -800.0f, "V22.7 BLOCKED: " + facts.cardTitle()
                            + " will SELF-CANCEL — we don't occupy Bespin system!");
            outcome = CloudCityEngineOutcome.BLOCKED;
        } else if (!facts.effectAlreadyOnTable()) {
            operation = add(facts.actionId(), "V24", TraceOutputKind.BANDED,
                    300.0f, "V24 TDIGWATT ENGINE: Deploy " + facts.cardTitle()
                            + " NOW — enables objective damage engine!");
            outcome = CloudCityEngineOutcome.ENGINE_PRIORITY;
        } else {
            operation = add(facts.actionId(), "V22.7", TraceOutputKind.BANDED,
                    50.0f, "V22.7: We occupy Bespin — safe to deploy "
                            + facts.cardTitle());
            outcome = CloudCityEngineOutcome.SAFE;
        }
        return new CloudCityEngineEvaluation(
                single("DEPLOY_CLOUD_CITY_ENGINE_POLICY", operation), outcome);
    }

    public static PolicyResult scoreGherant(GherantFacts facts) {
        Objects.requireNonNull(facts, "facts");
        return single("DEPLOY_GHERANT_POLICY",
                add(facts.actionId(), "V24.1", TraceOutputKind.BANDED,
                        150.0f,
                        "V24.1 GHERANT: Deploys an Executor site — free location + force generation!"));
    }

    public static LandoLobotEvaluation evaluateLandoLobot(
            LandoLobotFacts facts) {
        Objects.requireNonNull(facts, "facts");
        PolicyOperation operation = null;
        LandoLobotOutcome outcome = LandoLobotOutcome.NONE;
        if (facts.landoDeploy()) {
            if (facts.backupPresent()) {
                operation = add(facts.actionId(), "V29.2-LANDO",
                        TraceOutputKind.BANDED, 200.0f,
                        "V29.2 LANDO: Key piece + backup present — safe to deploy!");
                outcome = LandoLobotOutcome.LANDO_SAFE;
            } else {
                operation = add(facts.actionId(), "V47-LANDO",
                        TraceOutputKind.VETO, -9999.0f,
                        "V47 LANDO SOLO BLOCK: No friendlies at CC — Lando dies alone!");
                outcome = LandoLobotOutcome.LANDO_BLOCKED;
            }
        } else if (facts.lobotDeploy()) {
            if (facts.backupPresent()) {
                operation = add(facts.actionId(), "V29.2-LOBOT",
                        TraceOutputKind.BANDED, 150.0f,
                        "V29.2 LOBOT: Helps flip TDIGWATT + backup present!");
                outcome = LandoLobotOutcome.LOBOT_SAFE;
            } else {
                operation = add(facts.actionId(), "V47-LOBOT",
                        TraceOutputKind.VETO, -9999.0f,
                        "V47 LOBOT SOLO BLOCK: No friendlies at CC — Lobot dies alone!");
                outcome = LandoLobotOutcome.LOBOT_BLOCKED;
            }
        }
        PolicyResult result = operation == null
                ? new PolicyResult("DEPLOY_LANDO_LOBOT_POLICY", List.of())
                : single("DEPLOY_LANDO_LOBOT_POLICY", operation);
        return new LandoLobotEvaluation(result, outcome);
    }

    public static FlipSitingEvaluation evaluateFlipSiting(
            FlipSitingFacts facts) {
        Objects.requireNonNull(facts, "facts");
        PolicyOperation operation = null;
        FlipSitingOutcome outcome = FlipSitingOutcome.NONE;

        if (!facts.flipped()) {
            if (facts.unoccupiedObjectiveLocations() > 0
                    && facts.deploysToUnoccupiedObjectiveLocation()) {
                float score = 250.0f;
                if (facts.huntDown() && facts.turnNumber() <= 3) {
                    score = facts.inquisitor() ? 1000.0f : 800.0f;
                } else if (facts.huntDown()) {
                    score = 500.0f;
                }
                String earlySuffix = facts.huntDown() && facts.turnNumber() <= 3
                        ? " — EARLY DEFENSE CRITICAL" : "";
                operation = add(facts.actionId(), "V36", TraceOutputKind.BANDED,
                        score, String.format(
                                "V36 DEFEND TERRITORY: Deploy to unoccupied obj location! (%d/%d occupied%s)",
                                facts.occupiedObjectiveLocations(),
                                facts.occupiedObjectiveLocations()
                                        + facts.unoccupiedObjectiveLocations(),
                                earlySuffix));
                outcome = FlipSitingOutcome.PREFLIP_DEFEND;
            } else if (facts.unoccupiedObjectiveLocations() > 0) {
                operation = add(facts.actionId(), "V31-PREFLIP",
                        TraceOutputKind.BANDED, -50.0f, String.format(
                                "V31 PRE-FLIP: %d obj locations still unoccupied — spread out instead of stacking!",
                                facts.unoccupiedObjectiveLocations()));
                outcome = FlipSitingOutcome.PREFLIP_SPREAD;
            }
        } else if (facts.deploysToHoldLocation()) {
            operation = add(facts.actionId(), "V31-POSTFLIP",
                    TraceOutputKind.BANDED, 200.0f,
                    "V31 POST-FLIP: Reinforce key hold location!");
            outcome = FlipSitingOutcome.POSTFLIP_HOLD;
        } else if (facts.deploysToAnyObjectiveLocation()
                && facts.occupiedObjectiveLocations() > 2) {
            operation = add(facts.actionId(), "V40-POSTFLIP",
                    TraceOutputKind.BANDED, 0.0f,
                    "V40 POST-FLIP: Deploying to 3rd obj loc (neutral)");
            outcome = FlipSitingOutcome.POSTFLIP_THIRD_NEUTRAL;
        }

        PolicyResult result = operation == null
                ? new PolicyResult("DEPLOY_FLIP_SITING_POLICY", List.of())
                : single("DEPLOY_FLIP_SITING_POLICY", operation);
        return new FlipSitingEvaluation(result, outcome);
    }

    public record Facts(String actionId, boolean undercoverSpy,
                        boolean objectiveAnalyzed, boolean objectiveRelevant,
                        float objectiveLocationBonus, boolean myLord,
                        boolean senator, boolean character,
                        boolean galacticSenateDestination,
                        boolean textNamesDestination,
                        boolean textRejectsDestination,
                        boolean textNamedDestinationDoomed,
                        String bareSiteTitle, float opponentPower,
                        float friendlySenatorPower) {
        public Facts {
            Objects.requireNonNull(actionId, "actionId");
            bareSiteTitle = bareSiteTitle == null ? "" : bareSiteTitle;
        }
    }

    public record CloudCityArmyFacts(String actionId, String locationTitle) {
        public CloudCityArmyFacts {
            Objects.requireNonNull(actionId, "actionId");
            locationTitle = locationTitle == null ? "" : locationTitle;
        }
    }

    public record ObjectiveFirstFacts(String actionId, String locationTitle) {
        public ObjectiveFirstFacts {
            Objects.requireNonNull(actionId, "actionId");
            locationTitle = locationTitle == null ? "" : locationTitle;
        }
    }

    public record KeyCharacterFacts(String actionId, String cardTitle) {
        public KeyCharacterFacts {
            Objects.requireNonNull(actionId, "actionId");
            cardTitle = cardTitle == null ? "" : cardTitle;
        }
    }

    public record CloudCityEngineFacts(String actionId, String cardTitle,
                                       boolean occupyBespin,
                                       boolean effectAlreadyOnTable) {
        public CloudCityEngineFacts {
            Objects.requireNonNull(actionId, "actionId");
            cardTitle = cardTitle == null ? "" : cardTitle;
        }
    }

    public record CloudCityEngineEvaluation(PolicyResult result,
                                            CloudCityEngineOutcome outcome) {
        public CloudCityEngineEvaluation {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(outcome, "outcome");
        }
    }

    public enum CloudCityEngineOutcome {
        BLOCKED,
        ENGINE_PRIORITY,
        SAFE
    }

    public record GherantFacts(String actionId) {
        public GherantFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record LandoLobotFacts(String actionId, boolean landoDeploy,
                                  boolean lobotDeploy,
                                  boolean backupPresent) {
        public LandoLobotFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record LandoLobotEvaluation(PolicyResult result,
                                       LandoLobotOutcome outcome) {
        public LandoLobotEvaluation {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(outcome, "outcome");
        }
    }

    public enum LandoLobotOutcome {
        NONE,
        LANDO_SAFE,
        LANDO_BLOCKED,
        LOBOT_SAFE,
        LOBOT_BLOCKED
    }

    public record FlipSitingFacts(String actionId, boolean flipped,
                                  int turnNumber, boolean huntDown,
                                  boolean inquisitor,
                                  int occupiedObjectiveLocations,
                                  int unoccupiedObjectiveLocations,
                                  boolean deploysToUnoccupiedObjectiveLocation,
                                  boolean deploysToHoldLocation,
                                  boolean deploysToAnyObjectiveLocation) {
        public FlipSitingFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record FlipSitingEvaluation(PolicyResult result,
                                       FlipSitingOutcome outcome) {
        public FlipSitingEvaluation {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(outcome, "outcome");
        }
    }

    public enum FlipSitingOutcome {
        NONE,
        PREFLIP_DEFEND,
        PREFLIP_SPREAD,
        POSTFLIP_HOLD,
        POSTFLIP_THIRD_NEUTRAL
    }

    private static PolicyResult single(String owner,
                                       PolicyOperation operation) {
        return new PolicyResult(owner, List.of(operation));
    }

    private static PolicyOperation add(String actionId, String ruleId,
                                       TraceOutputKind outputKind,
                                       float delta, String reason) {
        return PolicyOperation.add(actionId, TraceRuleId.of(ruleId),
                TraceDomainId.DEPLOY_SITING, outputKind, delta, reason);
    }
}
