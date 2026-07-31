package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.common.CardCategory;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ForceLossPolicyTest {

    private static final String ACTION_ID = "card-1";

    @Test
    public void v153HealthyAndSurvivalMatricesKeepExactZoneScoresAndReasons() {
        List<ZoneCase> cases = List.of(
                new ZoneCase(ForceLossFacts.ZoneBand.HAND, "HAND",
                        CardCategory.CHARACTER, false, 100.0f, 700.0f),
                new ZoneCase(ForceLossFacts.ZoneBand.HAND, "HAND",
                        CardCategory.STARSHIP, false, 500.0f, 750.0f),
                new ZoneCase(ForceLossFacts.ZoneBand.HAND, "HAND",
                        CardCategory.VEHICLE, false, 500.0f, 750.0f),
                new ZoneCase(ForceLossFacts.ZoneBand.HAND, "HAND",
                        CardCategory.EFFECT, false, 600.0f, 850.0f),
                new ZoneCase(ForceLossFacts.ZoneBand.HAND, "HAND",
                        CardCategory.CHARACTER, true, 1000.0f, 1000.0f),
                new ZoneCase(ForceLossFacts.ZoneBand.USED, "USED_PILE",
                        CardCategory.CHARACTER, false, 800.0f, 400.0f),
                new ZoneCase(ForceLossFacts.ZoneBand.RESERVE, "RESERVE_DECK",
                        CardCategory.CHARACTER, false, 400.0f, 300.0f),
                new ZoneCase(ForceLossFacts.ZoneBand.FORCE_PILE, "FORCE_PILE",
                        CardCategory.CHARACTER, false, 50.0f, 50.0f),
                new ZoneCase(ForceLossFacts.ZoneBand.OTHER, "",
                        CardCategory.CHARACTER, false, 100.0f, 100.0f));

        for (ZoneCase zoneCase : cases) {
            assertZone(zoneCase, 4, zoneCase.healthyScore(), true);
            assertZone(zoneCase, 3, zoneCase.survivalScore(), false);
        }
    }

    @Test
    public void thresholdEdgesStayAtLifeFourReserveTenAndHandFour() {
        ForceLossFacts.CandidateFacts reserve = candidate("Reserve Card",
                ForceLossFacts.ZoneBand.RESERVE, "RESERVE_DECK",
                CardCategory.CHARACTER, false, false, false, false, false);
        assertOperations(score(ForceLossPolicy.Route.STANDALONE,
                        decision(5, 10, 3, 0, 2, false), reserve, none()),
                op("V153-zone", 300.0f,
                        "V153 ZONE (RESERVE_DECK, lifeForce=3, protectChars=false)"));
        assertOperations(score(ForceLossPolicy.Route.STANDALONE,
                        decision(5, 10, 4, 0, 2, false), reserve, none()),
                op("V153-zone", 400.0f,
                        "V153 ZONE (RESERVE_DECK, lifeForce=4, protectChars=true)"),
                op("V153-thin-reserve", -335.0f,
                        "V153 THIN RESERVE (deck=10): demote reserve below hand chars to preserve destiny"));
        assertOperations(score(ForceLossPolicy.Route.STANDALONE,
                        decision(5, 11, 4, 0, 2, false), reserve, none()),
                op("V153-zone", 400.0f,
                        "V153 ZONE (RESERVE_DECK, lifeForce=4, protectChars=true)"));

        ForceLossFacts.CandidateFacts hand = candidate("Hand Card",
                ForceLossFacts.ZoneBand.HAND, "HAND", CardCategory.EFFECT,
                false, false, false, false, false);
        assertOperations(score(ForceLossPolicy.Route.STANDALONE,
                        decision(4, 20, 9, 0, 2, false), hand, none()),
                op("V153-zone", 600.0f,
                        "V153 ZONE (HAND, lifeForce=9, protectChars=true)"));
        assertOperations(score(ForceLossPolicy.Route.STANDALONE,
                        decision(4, 20, 10, 0, 2, false), hand, none()),
                op("V153-zone", 600.0f,
                        "V153 ZONE (HAND, lifeForce=10, protectChars=true)"),
                op("V153-hand-floor", -700.0f,
                        "V153 HAND FLOOR: only 4 in hand (life force 10>=10) — keep >=4, lose from piles instead -700",
                        TraceOutputKind.BANDED));
        assertOperations(score(ForceLossPolicy.Route.STANDALONE,
                        decision(5, 20, 10, 0, 2, false), hand, none()),
                op("V153-zone", 600.0f,
                        "V153 ZONE (HAND, lifeForce=10, protectChars=true)"));
    }

    @Test
    public void v109AndV175KeepOrderAndTurnFourBoundary() {
        ForceLossFacts.CandidateFacts senatorInterrupt = candidate(
                "Senator's Battle Trick", ForceLossFacts.ZoneBand.HAND, "HAND",
                CardCategory.INTERRUPT, false, true, true, false, false);
        ForceLossPolicy.ObjectiveFlags myLord =
                new ForceLossPolicy.ObjectiveFlags(true, false, false, false);

        assertOperations(score(ForceLossPolicy.Route.STANDALONE,
                        decision(5, 20, 8, 0, 3, false), senatorInterrupt, myLord),
                op("V109", -300.0f,
                        "V109 MY LORD: PROTECT senator 'Senator's Battle Trick' — never discard/lose senators in this deck -300"),
                op("V153-zone", 600.0f,
                        "V153 ZONE (HAND, lifeForce=8, protectChars=true)"));
        assertOperations(score(ForceLossPolicy.Route.STANDALONE,
                        decision(5, 20, 8, 0, 4, false), senatorInterrupt, myLord),
                op("V109", -300.0f,
                        "V109 MY LORD: PROTECT senator 'Senator's Battle Trick' — never discard/lose senators in this deck -300"),
                op("V153-zone", 600.0f,
                        "V153 ZONE (HAND, lifeForce=8, protectChars=true)"),
                op("V175", -450.0f,
                        "V175 PROTECT BATTLE INTERRUPT: 'Senator's Battle Trick' is an in-battle trick — lose it near-last, like a character"));
    }

    @Test
    public void v178ProtectsAnyWeaponWithAnyWielderFromTurnFour() {
        ForceLossFacts.CandidateFacts weapon = candidate("Unmatched Blaster",
                ForceLossFacts.ZoneBand.HAND, "HAND", CardCategory.WEAPON,
                false, false, false, true, false);

        assertOperations(score(ForceLossPolicy.Route.STANDALONE,
                        decision(5, 20, 8, 0, 3, false), weapon, none()),
                op("V153-zone", 600.0f,
                        "V153 ZONE (HAND, lifeForce=8, protectChars=true)"));
        assertOperations(score(ForceLossPolicy.Route.STANDALONE,
                        decision(5, 20, 8, 0, 4, false), weapon, none()),
                op("V153-zone", 600.0f,
                        "V153 ZONE (HAND, lifeForce=8, protectChars=true)"),
                op("V178-loss", -450.0f,
                        "V178 PROTECT WEAPON: 'Unmatched Blaster' — we have a wielder; lose it near-last, like a character"));
    }

    @Test
    public void v28KeepsForcePileThreeFourBoundaryAndStandaloneScope() {
        ForceLossFacts.CandidateFacts forcePile = candidate("Pile Card",
                ForceLossFacts.ZoneBand.FORCE_PILE, "FORCE_PILE",
                CardCategory.EFFECT, false, false, false, false, false);

        assertOperations(score(ForceLossPolicy.Route.STANDALONE,
                        decision(5, 20, 8, 3, 2, true), forcePile, none()),
                op("V153-zone", 50.0f,
                        "V153 ZONE (FORCE_PILE, lifeForce=8, protectChars=true)"),
                op("V28-DTF", -400.0f,
                        "V28 DTF FORCE PILE PROTECT: Force pile=3, DTF active — lose from reserve instead!"));
        assertOperations(score(ForceLossPolicy.Route.STANDALONE,
                        decision(5, 20, 8, 4, 2, true), forcePile, none()),
                op("V153-zone", 50.0f,
                        "V153 ZONE (FORCE_PILE, lifeForce=8, protectChars=true)"),
                op("V28-DTF", -200.0f,
                        "V28 DTF FORCE PILE PROTECT: Force pile=4, DTF active — lose from reserve instead!"));
        assertOperations(score(ForceLossPolicy.Route.COMBINED_BATTLE,
                        decision(5, 20, 8, 3, 2, true), forcePile, none()),
                op("V153-zone", 50.0f,
                        "V153 ZONE (FORCE_PILE, lifeForce=8, protectChars=true)"));
    }

    @Test
    public void duplicateSuppressesPriorityButNotObjectiveOrLightsaberProtection() {
        ForceLossFacts.CandidateFacts priority = candidate("Houjix",
                ForceLossFacts.ZoneBand.HAND, "HAND", CardCategory.INTERRUPT,
                false, false, false, false, true);
        assertOperations(score(ForceLossPolicy.Route.STANDALONE,
                        decision(5, 20, 8, 0, 2, false), priority, none()),
                op("V153-zone", 600.0f,
                        "V153 ZONE (HAND, lifeForce=8, protectChars=true)"),
                op("V153-priority", -100.0f,
                        "V153 PRIORITY CARD: protect 'Houjix' (hand/used) -100"));

        ForceLossFacts.CandidateFacts duplicate = candidate("Vader's Lightsaber",
                ForceLossFacts.ZoneBand.HAND, "HAND", CardCategory.WEAPON,
                true, false, false, false, true);
        ForceLossPolicy.ObjectiveFlags huntRequired =
                new ForceLossPolicy.ObjectiveFlags(false, true, true, true);
        PolicyResult duplicateResult = score(ForceLossPolicy.Route.STANDALONE,
                decision(5, 20, 8, 0, 2, false), duplicate, huntRequired);
        assertOperations(duplicateResult,
                op("V153-zone", 1000.0f,
                        "V153 ZONE (HAND, lifeForce=8, protectChars=true)"),
                op("V21-objective", -9999.0f,
                        "OBJECTIVE CRITICAL IN HAND - NEVER LOSE!", TraceOutputKind.VETO),
                op("V25", -500.0f,
                        "V25 HUNT DOWN: PROTECT LIGHTSABER IN HAND!"));
        assertFalse(duplicateResult.operations().stream()
                .anyMatch(operation -> operation.ruleArmId().id().equals("V153-priority")));
    }

    @Test
    public void objectiveAndV25KeepRouteSpecificScopesMagnitudesAndCombinedOrder() {
        ForceLossFacts.CandidateFacts usedLightsaber = candidate("Vader's Lightsaber",
                ForceLossFacts.ZoneBand.USED, "USED_PILE", CardCategory.WEAPON,
                false, false, false, false, true);
        ForceLossPolicy.ObjectiveFlags huntRequired =
                new ForceLossPolicy.ObjectiveFlags(false, true, true, true);

        assertOperations(score(ForceLossPolicy.Route.STANDALONE,
                        decision(5, 20, 8, 0, 2, false), usedLightsaber, huntRequired),
                op("V153-zone", 800.0f,
                        "V153 ZONE (USED_PILE, lifeForce=8, protectChars=true)"),
                op("V153-priority", -100.0f,
                        "V153 PRIORITY CARD: protect 'Vader's Lightsaber' (hand/used) -100"),
                op("V21-objective", -9999.0f,
                        "OBJECTIVE CRITICAL - NEVER LOSE!", TraceOutputKind.VETO));

        assertOperations(score(ForceLossPolicy.Route.COMBINED_BATTLE,
                        decision(5, 20, 8, 0, 2, false), usedLightsaber, huntRequired),
                op("V25", -400.0f,
                        "V25 HUNT DOWN: PROTECT LIGHTSABER from loss!"),
                op("V153-zone", 800.0f,
                        "V153 ZONE (USED_PILE, lifeForce=8, protectChars=true)"),
                op("V21-objective", -9999.0f,
                        "OBJECTIVE CRITICAL - NEVER LOSE!", TraceOutputKind.VETO),
                op("V153-priority", -100.0f,
                        "V153 PRIORITY CARD: protect 'Vader's Lightsaber' -100"));

        ForceLossFacts.CandidateFacts reserve = candidate("Objective Pull",
                ForceLossFacts.ZoneBand.RESERVE, "RESERVE_DECK", CardCategory.EFFECT,
                false, false, false, false, false);
        ForceLossPolicy.ObjectiveFlags pullable =
                new ForceLossPolicy.ObjectiveFlags(false, false, false, true);
        assertOperations(score(ForceLossPolicy.Route.STANDALONE,
                        decision(5, 20, 8, 0, 2, false), reserve, pullable),
                op("V153-zone", 400.0f,
                        "V153 ZONE (RESERVE_DECK, lifeForce=8, protectChars=true)"));
        assertOperations(score(ForceLossPolicy.Route.COMBINED_BATTLE,
                        decision(5, 20, 8, 0, 2, false), reserve, pullable),
                op("V153-zone", 400.0f,
                        "V153 ZONE (RESERVE_DECK, lifeForce=8, protectChars=true)"),
                op("V21-objective", -9999.0f,
                        "OBJECTIVE PULLABLE - NEVER LOSE!", TraceOutputKind.VETO));
    }

    @Test
    public void combinedRouteOmitsStandaloneV109V175AndV178Arms() {
        ForceLossFacts.CandidateFacts weaponSenator = candidate("Senator's Blaster",
                ForceLossFacts.ZoneBand.HAND, "HAND", CardCategory.WEAPON,
                false, true, true, true, false);
        ForceLossPolicy.ObjectiveFlags myLord =
                new ForceLossPolicy.ObjectiveFlags(true, false, false, false);

        assertOperations(score(ForceLossPolicy.Route.COMBINED_BATTLE,
                        decision(5, 20, 8, 0, 4, false), weaponSenator, myLord),
                op("V153-zone", 600.0f,
                        "V153 ZONE (HAND, lifeForce=8, protectChars=true)"));
    }

    @Test
    public void oneResultNeverRepeatsAnActionRuleContributionKey() {
        ForceLossFacts.CandidateFacts richCandidate = candidate(
                "Vader's Lightsaber", ForceLossFacts.ZoneBand.HAND, "HAND",
                CardCategory.WEAPON, false, true, false, true, true);
        ForceLossPolicy.ObjectiveFlags flags =
                new ForceLossPolicy.ObjectiveFlags(true, true, true, true);
        PolicyResult result = score(ForceLossPolicy.Route.STANDALONE,
                decision(4, 10, 10, 3, 4, true), richCandidate, flags);

        Set<String> keys = new HashSet<>();
        for (PolicyOperation operation : result.operations()) {
            assertTrue(keys.add(operation.actionId() + ":"
                    + operation.ruleArmId().id()));
        }
        assertEquals(result.operations().size(), keys.size());
    }

    @Test
    public void actionTextChoiceMatrixKeepsExactOrderedAdditiveScores() {
        assertChoice(ForceLossPolicy.ActionTextChoice.PLACE_IN_LOST_PILE,
                -50.0f, "Avoid losing cards");
        assertChoice(ForceLossPolicy.ActionTextChoice.MAINTENANCE_PAY,
                400.0f, "V74 MAINTENANCE PAY: keep the card alive!");
        assertChoice(ForceLossPolicy.ActionTextChoice.MAINTENANCE_OUT_OF_PLAY,
                -800.0f,
                "V74 MAINTENANCE SACRIFICE: place out of play is PERMANENT loss!");
        assertChoice(ForceLossPolicy.ActionTextChoice.MAINTENANCE_USED_PILE,
                -200.0f,
                "V74 MAINTENANCE USED-PILE: lose card to used pile, keep blueprint");
        assertChoice(ForceLossPolicy.ActionTextChoice.MAINTENANCE_SACRIFICE,
                -800.0f, "V74 MAINTENANCE SACRIFICE: avoid");
        assertChoice(ForceLossPolicy.ActionTextChoice.GENERIC_USE_UPKEEP,
                150.0f, "V22.3 MAINTENANCE: Pay upkeep cost!");
        assertChoice(ForceLossPolicy.ActionTextChoice.GENERIC_USE,
                -20.0f,
                "'Use Force' action — prefer not to use force unnecessarily");
        assertChoice(ForceLossPolicy.ActionTextChoice.GENERIC_LOSE,
                -30.0f, "'Lose Force' action — avoid losing force");
        assertChoice(ForceLossPolicy.ActionTextChoice.GENERIC_SACRIFICE,
                -150.0f,
                "V22.3: Avoid sacrificing cards — prefer alternatives");
    }

    @Test
    public void unknownLossCategoryMatrixAndV25OrderStayExact() {
        assertUnknown(CardCategory.EFFECT,
                "FORCE-LOSS-unknown-effect-interrupt", 25.0f,
                "Effect/Interrupt - OK to lose");
        assertUnknown(CardCategory.INTERRUPT,
                "FORCE-LOSS-unknown-effect-interrupt", 25.0f,
                "Effect/Interrupt - OK to lose");
        assertUnknown(CardCategory.CHARACTER,
                "FORCE-LOSS-unknown-character", -15.0f,
                "Character - avoid losing");
        assertUnknown(CardCategory.STARSHIP,
                "FORCE-LOSS-unknown-starship", -15.0f,
                "Starship - avoid losing");
        assertUnknown(CardCategory.VEHICLE,
                "FORCE-LOSS-unknown-vehicle", -10.0f,
                "Vehicle - avoid losing");
        assertUnknown(CardCategory.LOCATION,
                "FORCE-LOSS-unknown-location", -20.0f,
                "Location - avoid losing");
        assertOperations(ForceLossPolicy.scoreUnknownLoss(
                ACTION_ID, CardCategory.WEAPON, false));
        assertOperations(ForceLossPolicy.scoreUnknownLoss(
                ACTION_ID, null, false));

        assertOperations(ForceLossPolicy.scoreUnknownLoss(
                        ACTION_ID, CardCategory.WEAPON, true),
                op("V25-unknown-loss", -300.0f,
                        "V25 HUNT DOWN: PROTECT LIGHTSABER from loss!"));
    }

    @Test
    public void unknownLossRetainsOnlyTheSelectedShieldRoutePackage() {
        assertOperations(
                ForceLossPolicy.scoreUnknownObjectiveRetention(
                    ACTION_ID, true),
                op("HOTH.SHIELD.PACKAGE_RETAIN", -9999.0f,
                    "HOTH SHIELD PACKAGE: retain the selected host, pilot, and Cannon",
                    TraceOutputKind.VETO));
        assertTrue(ForceLossPolicy
                .scoreUnknownObjectiveRetention(
                    ACTION_ID, false)
                .operations().isEmpty());
    }

    private static void assertZone(ZoneCase zoneCase, int lifeForce,
                                   float expectedScore, boolean protectChars) {
        ForceLossFacts.CandidateFacts candidate = candidate("Zone Card",
                zoneCase.zoneBand(), zoneCase.zoneName(), zoneCase.category(),
                zoneCase.duplicate(), false, false, false, false);
        String renderedZone = zoneCase.zoneName().isEmpty() ? "?" : zoneCase.zoneName();
        assertOperations(score(ForceLossPolicy.Route.STANDALONE,
                        decision(5, 20, lifeForce, 0, 2, false), candidate, none()),
                op("V153-zone", expectedScore,
                        "V153 ZONE (" + renderedZone + ", lifeForce=" + lifeForce
                                + ", protectChars=" + protectChars + ")"));
    }

    private static void assertChoice(ForceLossPolicy.ActionTextChoice choice,
                                     float score, String reason) {
        PolicyResult result = ForceLossPolicy.scoreActionTextChoice(ACTION_ID, choice);
        assertEquals(1, result.operations().size());
        PolicyOperation operation = result.operations().get(0);
        assertBits(score, operation.delta());
        assertEquals(reason, operation.reason());
        assertEquals(TraceDomainId.FORCE_LOSS_PAYMENT, operation.domainId());
        assertEquals(TraceOutputKind.ORDERING, operation.outputKind());
        assertEquals(PolicyOperationKind.ADD, operation.kind());
    }

    private static void assertUnknown(CardCategory category, String ruleId,
                                      float score, String reason) {
        PolicyResult result = ForceLossPolicy.scoreUnknownLoss(
                ACTION_ID, category, false);
        assertOperations(result, op(ruleId, score, reason));
    }

    private static PolicyResult score(ForceLossPolicy.Route route,
                                      ForceLossFacts.DecisionFacts decision,
                                      ForceLossFacts.CandidateFacts candidate,
                                      ForceLossPolicy.ObjectiveFlags objective) {
        return ForceLossPolicy.score(ACTION_ID, route, decision, candidate, objective);
    }

    private static ForceLossFacts.DecisionFacts decision(
            int handSize, int reserveDeckSize, int lifeForce, int forcePileSize,
            int turnNumber, boolean drawTheirFireActive) {
        return new ForceLossFacts.DecisionFacts(handSize, reserveDeckSize, lifeForce,
                forcePileSize, turnNumber, drawTheirFireActive);
    }

    private static ForceLossFacts.CandidateFacts candidate(
            String title, ForceLossFacts.ZoneBand zoneBand, String zoneName,
            CardCategory category, boolean duplicate, boolean senator,
            boolean battleInterrupt, boolean hasWielder, boolean priorityCard) {
        return new ForceLossFacts.CandidateFacts(title, zoneName, zoneBand,
                category, duplicate, senator, battleInterrupt, hasWielder,
                priorityCard);
    }

    private static ForceLossPolicy.ObjectiveFlags none() {
        return ForceLossPolicy.ObjectiveFlags.none();
    }

    private static Expected op(String ruleId, float delta, String reason) {
        return op(ruleId, delta, reason, TraceOutputKind.ORDERING);
    }

    private static Expected op(String ruleId, float delta, String reason,
                               TraceOutputKind outputKind) {
        return new Expected(ruleId, delta, reason, outputKind);
    }

    private static void assertOperations(PolicyResult result, Expected... expected) {
        assertEquals("FORCE_LOSS_POLICY", result.producerId());
        assertEquals(expected.length, result.operations().size());
        for (int i = 0; i < expected.length; i++) {
            PolicyOperation operation = result.operations().get(i);
            Expected expectedOperation = expected[i];
            assertEquals(expectedOperation.ruleId(), operation.ruleArmId().id());
            assertBits(expectedOperation.delta(), operation.delta());
            assertEquals(expectedOperation.reason(), operation.reason());
            assertEquals(expectedOperation.outputKind(), operation.outputKind());
            assertEquals(TraceDomainId.FORCE_LOSS_PAYMENT, operation.domainId());
            assertEquals(PolicyOperationKind.ADD, operation.kind());
            assertEquals(ACTION_ID, operation.actionId());
        }
    }

    private static void assertBits(float expected, float actual) {
        assertEquals(Float.floatToRawIntBits(expected), Float.floatToRawIntBits(actual));
    }

    private record ZoneCase(ForceLossFacts.ZoneBand zoneBand, String zoneName,
                            CardCategory category, boolean duplicate,
                            float healthyScore, float survivalScore) {
    }

    private record Expected(String ruleId, float delta, String reason,
                            TraceOutputKind outputKind) {
    }
}
