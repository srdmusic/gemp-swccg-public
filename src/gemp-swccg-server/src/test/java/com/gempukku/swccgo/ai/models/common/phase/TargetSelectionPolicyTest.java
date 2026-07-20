package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyContributionLedger;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TargetSelectionPolicyTest {

    @Test
    public void initialScoreCarriesExactTaggedBaseWithoutReasonOperation() {
        TargetSelectionPolicy.InitialScore initial =
                TargetSelectionPolicy.initialScore("731");

        assertEquals("731", initial.actionId());
        assertRaw(50.0f, initial.score());
        assertEquals("Target 731", initial.displayText());
        assertEquals("TARGET-base", initial.ruleArmId().id());
        assertEquals(TraceDomainId.BATTLE_WEAPONS, initial.domainId());
        assertEquals(TraceOutputKind.BANDED, initial.outputKind());
    }

    @Test
    public void ownershipMatrixRetainsExactOperationsAndSuppression() {
        assertOperation(only(ownership(TargetSelectionFacts.Intent.BENEFICIAL,
                        TargetSelectionFacts.Ownership.OWN)),
                "TARGET-beneficial-own", TraceOutputKind.BANDED, 50.0f,
                "Beneficial effect on our card");
        assertOperation(only(ownership(TargetSelectionFacts.Intent.BENEFICIAL,
                        TargetSelectionFacts.Ownership.OPPONENT)),
                "TARGET-beneficial-opponent", TraceOutputKind.VETO, -200.0f,
                "Don't buff opponent's card!");
        assertOperation(only(ownership(TargetSelectionFacts.Intent.HARMFUL,
                        TargetSelectionFacts.Ownership.OPPONENT)),
                "TARGET-harmful-opponent", TraceOutputKind.BANDED, 50.0f,
                "Target opponent's card");
        assertTrue(ownership(TargetSelectionFacts.Intent.HARMFUL,
                TargetSelectionFacts.Ownership.OWN).operations().isEmpty());
    }

    @Test
    public void undercoverOnlyScoresHarmfulOpponentSpy() {
        PolicyResult scored = TargetSelectionPolicy.scoreUndercover(
                new TargetSelectionFacts.UndercoverFacts("target",
                        TargetSelectionFacts.Intent.HARMFUL,
                        TargetSelectionFacts.Ownership.OPPONENT, true));
        assertOperation(only(scored), "V51-kill-spy",
                TraceOutputKind.ORDERING, 500.0f,
                "V51 KILL SPY: Target is an undercover spy — eliminate it!");

        assertTrue(undercover(TargetSelectionFacts.Intent.HARMFUL,
                TargetSelectionFacts.Ownership.OPPONENT, false).operations().isEmpty());
        assertTrue(undercover(TargetSelectionFacts.Intent.HARMFUL,
                TargetSelectionFacts.Ownership.OWN, true).operations().isEmpty());
        assertTrue(undercover(TargetSelectionFacts.Intent.BENEFICIAL,
                TargetSelectionFacts.Ownership.OPPONENT, true).operations().isEmpty());
    }

    @Test
    public void valueMatrixRetainsExactOrderAndBattleSuppression() {
        PolicyResult beneficial = value(TargetSelectionFacts.Intent.BENEFICIAL,
                TargetSelectionFacts.Ownership.OWN, true, true, true);
        assertOperations(beneficial,
                expected("TARGET-beneficial-high-power", TraceOutputKind.ORDERING,
                        30.0f, "High-power target for buff"),
                expected("TARGET-beneficial-unique", TraceOutputKind.ORDERING,
                        20.0f, "Unique target for buff"));

        PolicyResult harmfulOutside = value(TargetSelectionFacts.Intent.HARMFUL,
                TargetSelectionFacts.Ownership.OPPONENT, false, true, true);
        assertOperations(harmfulOutside,
                expected("TARGET-outside-battle-high-power", TraceOutputKind.ORDERING,
                        30.0f, "High-power target"),
                expected("TARGET-harmful-unique", TraceOutputKind.ORDERING,
                        20.0f, "Unique target"));

        PolicyResult harmfulBattle = value(TargetSelectionFacts.Intent.HARMFUL,
                TargetSelectionFacts.Ownership.OPPONENT, true, true, true);
        assertOperations(harmfulBattle,
                expected("TARGET-harmful-unique", TraceOutputKind.ORDERING,
                        20.0f, "Unique target"));

        assertTrue(value(TargetSelectionFacts.Intent.BENEFICIAL,
                TargetSelectionFacts.Ownership.OPPONENT, false, true, true)
                .operations().isEmpty());
        assertTrue(value(TargetSelectionFacts.Intent.HARMFUL,
                TargetSelectionFacts.Ownership.OWN, false, true, true)
                .operations().isEmpty());
    }

    @Test
    public void compositeLedgerRetainsCrossOwnerOrderAndProducerIdentity() {
        PolicyContributionLedger ledger = new PolicyContributionLedger("target-decision");
        ledger.register(ownership(TargetSelectionFacts.Intent.HARMFUL,
                TargetSelectionFacts.Ownership.OPPONENT));
        ledger.register(BattleWeaponsPolicy.scoreTarget(
                new BattleWeaponsFacts.TargetFacts("target", "Padme Naberrie", true,
                        BattleWeaponsFacts.DestinyAssessment.unavailable(),
                        false, false, false, false)));
        ledger.register(undercover(TargetSelectionFacts.Intent.HARMFUL,
                TargetSelectionFacts.Ownership.OPPONENT, true));
        ledger.register(BattleWeaponsPolicy.scoreTarget(
                new BattleWeaponsFacts.TargetFacts("target", "Padme Naberrie", false,
                        BattleWeaponsFacts.DestinyAssessment.available(3.0f, 6.0f),
                        true, false, false, false)));
        ledger.register(value(TargetSelectionFacts.Intent.HARMFUL,
                TargetSelectionFacts.Ownership.OPPONENT, true, true, true));

        List<PolicyOperation> operations = ledger.orderedOperations();
        assertEquals(List.of(
                        "TARGET-harmful-opponent",
                        "V51-already-hit",
                        "V51-kill-spy",
                        "V36-easy-hit",
                        "V36-priority-game-text-canceler",
                        "TARGET-harmful-unique"),
                operations.stream().map(operation -> operation.ruleArmId().id()).toList());
        assertEquals(List.of(50.0f, -500.0f, 500.0f, 200.0f, 300.0f, 20.0f),
                operations.stream().map(PolicyOperation::delta).toList());
        assertEquals("TARGET_SELECTION_POLICY",
                ledger.producerFor("target", operations.get(0).ruleArmId()));
        assertEquals("BATTLE_WEAPONS_TARGET_POLICY",
                ledger.producerFor("target", operations.get(1).ruleArmId()));
        assertEquals("TARGET_SELECTION_POLICY",
                ledger.producerFor("target", operations.get(2).ruleArmId()));
        assertEquals("BATTLE_WEAPONS_TARGET_POLICY",
                ledger.producerFor("target", operations.get(3).ruleArmId()));
        assertEquals("TARGET_SELECTION_POLICY",
                ledger.producerFor("target", operations.get(5).ruleArmId()));
    }

    @Test
    public void boundaryTotalsRemainExactIncludingLegacyContradictions() {
        assertRaw(50.0f, 50.0f);
        assertRaw(100.0f, total(50.0f,
                ownership(TargetSelectionFacts.Intent.BENEFICIAL,
                        TargetSelectionFacts.Ownership.OWN)));
        assertRaw(150.0f, total(50.0f,
                ownership(TargetSelectionFacts.Intent.BENEFICIAL,
                        TargetSelectionFacts.Ownership.OWN),
                value(TargetSelectionFacts.Intent.BENEFICIAL,
                        TargetSelectionFacts.Ownership.OWN, false, true, true)));
        assertRaw(-150.0f, total(50.0f,
                ownership(TargetSelectionFacts.Intent.BENEFICIAL,
                        TargetSelectionFacts.Ownership.OPPONENT)));
        assertRaw(100.0f, harmfulOpponentTotal(false, false, false, false));
        assertRaw(150.0f, harmfulOpponentTotal(false, false, true, true));
        assertRaw(600.0f, harmfulOpponentTotal(false, true, false, false));
        assertRaw(650.0f, harmfulOpponentTotal(false, true, true, true));
        assertRaw(-400.0f, harmfulOpponentTotal(true, false, false, false));
        assertRaw(-350.0f, harmfulOpponentTotal(true, false, true, true));
        assertRaw(100.0f, harmfulOpponentTotal(true, true, false, false));
        assertRaw(150.0f, harmfulOpponentTotal(true, true, true, true));

        PolicyResult self = BattleWeaponsPolicy.scoreTarget(
                new BattleWeaponsFacts.TargetFacts("target", "Own target", false,
                        BattleWeaponsFacts.DestinyAssessment.unavailable(),
                        false, false, false, true));
        assertRaw(-9949.0f, total(50.0f, self));
    }

    @Test
    public void factRecordsRejectMissingRequiredIdentity() {
        assertThrows(NullPointerException.class,
                () -> TargetSelectionPolicy.initialScore(null));
        assertThrows(IllegalArgumentException.class,
                () -> TargetSelectionPolicy.initialScore(" "));
        assertThrows(NullPointerException.class,
                () -> new TargetSelectionFacts.OwnershipFacts(
                        "target", null, TargetSelectionFacts.Ownership.OWN));
        assertThrows(NullPointerException.class,
                () -> new TargetSelectionFacts.ValueFacts(
                        "target", TargetSelectionFacts.Intent.HARMFUL,
                        null, false, false, false));
    }

    private static float harmfulOpponentTotal(boolean hit, boolean spy,
                                               boolean highPower, boolean unique) {
        PolicyResult hitResult = BattleWeaponsPolicy.scoreTarget(
                new BattleWeaponsFacts.TargetFacts("target", "Target", hit,
                        BattleWeaponsFacts.DestinyAssessment.unavailable(),
                        false, false, false, false));
        return total(50.0f,
                ownership(TargetSelectionFacts.Intent.HARMFUL,
                        TargetSelectionFacts.Ownership.OPPONENT),
                hitResult,
                undercover(TargetSelectionFacts.Intent.HARMFUL,
                        TargetSelectionFacts.Ownership.OPPONENT, spy),
                value(TargetSelectionFacts.Intent.HARMFUL,
                        TargetSelectionFacts.Ownership.OPPONENT,
                        false, highPower, unique));
    }

    private static PolicyResult ownership(TargetSelectionFacts.Intent intent,
                                          TargetSelectionFacts.Ownership ownership) {
        return TargetSelectionPolicy.scoreOwnership(
                new TargetSelectionFacts.OwnershipFacts("target", intent, ownership));
    }

    private static PolicyResult undercover(TargetSelectionFacts.Intent intent,
                                           TargetSelectionFacts.Ownership ownership,
                                           boolean undercover) {
        return TargetSelectionPolicy.scoreUndercover(
                new TargetSelectionFacts.UndercoverFacts(
                        "target", intent, ownership, undercover));
    }

    private static PolicyResult value(TargetSelectionFacts.Intent intent,
                                      TargetSelectionFacts.Ownership ownership,
                                      boolean battleMode,
                                      boolean highPower,
                                      boolean unique) {
        return TargetSelectionPolicy.scoreValue(
                new TargetSelectionFacts.ValueFacts(
                        "target", intent, ownership, battleMode, highPower, unique));
    }

    private static PolicyOperation only(PolicyResult result) {
        assertEquals("TARGET_SELECTION_POLICY", result.producerId());
        assertEquals(1, result.operations().size());
        return result.operations().get(0);
    }

    private static void assertOperations(PolicyResult result, Expected... expected) {
        assertEquals("TARGET_SELECTION_POLICY", result.producerId());
        assertEquals(expected.length, result.operations().size());
        for (int i = 0; i < expected.length; i++) {
            Expected item = expected[i];
            assertOperation(result.operations().get(i), item.ruleId(), item.kind(),
                    item.delta(), item.reason());
        }
    }

    private static void assertOperation(PolicyOperation operation,
                                        String ruleId,
                                        TraceOutputKind outputKind,
                                        float delta,
                                        String reason) {
        assertEquals("target", operation.actionId());
        assertEquals(ruleId, operation.ruleArmId().id());
        assertEquals(TraceDomainId.BATTLE_WEAPONS, operation.domainId());
        assertEquals(outputKind, operation.outputKind());
        assertEquals(PolicyOperationKind.ADD, operation.kind());
        assertRaw(delta, operation.delta());
        assertEquals(reason, operation.reason());
    }

    private static Expected expected(String ruleId, TraceOutputKind kind,
                                     float delta, String reason) {
        return new Expected(ruleId, kind, delta, reason);
    }

    private static float total(float initial, PolicyResult... results) {
        float total = initial;
        for (PolicyResult result : results) {
            for (PolicyOperation operation : result.operations()) {
                total += operation.delta();
            }
        }
        return total;
    }

    private static void assertRaw(float expected, float actual) {
        assertEquals(Float.floatToRawIntBits(expected), Float.floatToRawIntBits(actual));
    }

    private record Expected(String ruleId, TraceOutputKind kind,
                            float delta, String reason) {
    }
}
