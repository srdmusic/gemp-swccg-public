package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BattleWeaponsPolicyTest {

    @Test
    public void forcePushModesRetainLegacyAdditiveScoresAndReasons() {
        PolicyOperation battle = only(BattleWeaponsPolicy.scoreActionText(actionText(
                "force-push-battle", BattleWeaponsFacts.ForcePushMode.BATTLE_EXCLUSION,
                BattleWeaponsFacts.FireMode.NONE, BattleWeaponsFacts.ThrowMode.NONE,
                BattleWeaponsFacts.RedrawFacts.none())));
        PolicyOperation exchange = only(BattleWeaponsPolicy.scoreActionText(actionText(
                "force-push-exchange", BattleWeaponsFacts.ForcePushMode.FORCE_PILE_EXCHANGE,
                BattleWeaponsFacts.FireMode.NONE, BattleWeaponsFacts.ThrowMode.NONE,
                BattleWeaponsFacts.RedrawFacts.none())));

        assertOperation(battle, "force-push-battle", "V29-force-push-battle",
                TraceOutputKind.BANDED, 80.0f,
                "V29 FORCE PUSH: Battle exclusion — remove threat! Good use.");
        assertOperation(exchange, "force-push-exchange", "V67u-force-push-exchange",
                TraceOutputKind.VETO, -500.0f,
                "V67u FORCE PUSH BLOCK: Exchange w/ Force Pile is WASTE — those cards come to hand on draw anyway. NEVER play during draw phase!");
    }

    @Test
    public void battleFirePrecedesThrowAtExactRawScores() {
        PolicyResult result = BattleWeaponsPolicy.scoreActionText(actionText(
                "weapon-window", BattleWeaponsFacts.ForcePushMode.NONE,
                BattleWeaponsFacts.FireMode.VALID_TARGET_IN_BATTLE,
                BattleWeaponsFacts.ThrowMode.IN_BATTLE,
                BattleWeaponsFacts.RedrawFacts.none()));

        assertOrdered(result.operations(), "V29.12-fire-battle", "V29.12-throw-battle");
        assertRawFloat(300.0f, result.operations().get(0).delta());
        assertRawFloat(200.0f, result.operations().get(1).delta());
        assertEquals("V29.12 FIRE WEAPON: Fire FIRST in battle — hit target before throwing!",
                result.operations().get(0).reason());
        assertEquals("V29.12 LIGHTSABER THROW: Add destiny to attrition — do AFTER firing!",
                result.operations().get(1).reason());
        assertPolicyShape(result);
    }

    @Test
    public void fireFallbacksAndOutsideThrowRetainLegacyRawScores() {
        PolicyResult noTarget = BattleWeaponsPolicy.scoreActionText(actionText(
                "no-target", BattleWeaponsFacts.ForcePushMode.NONE,
                BattleWeaponsFacts.FireMode.NO_VALID_TARGET,
                BattleWeaponsFacts.ThrowMode.NONE,
                BattleWeaponsFacts.RedrawFacts.none()));
        PolicyResult outside = BattleWeaponsPolicy.scoreActionText(actionText(
                "outside", BattleWeaponsFacts.ForcePushMode.NONE,
                BattleWeaponsFacts.FireMode.VALID_TARGET_OUTSIDE_BATTLE,
                BattleWeaponsFacts.ThrowMode.OUTSIDE_BATTLE,
                BattleWeaponsFacts.RedrawFacts.none()));

        assertRawFloat(-30.0f, only(noTarget).delta());
        assertEquals("All targets already HIT - save weapon",
                noTarget.operations().get(0).reason());
        assertOrdered(outside.operations(),
                "BATTLE-fire-valid-target", "V29.10-throw-outside-battle");
        assertRawFloat(50.0f, outside.operations().get(0).delta());
        assertRawFloat(150.0f, outside.operations().get(1).delta());
        assertEquals(List.of(
                        "Firing weapons at valid targets",
                        "V29.10 LIGHTSABER THROW: Throw lightsaber to add destiny to attrition!"),
                outside.operations().stream().map(PolicyOperation::reason).toList());
        assertPolicyShape(noTarget);
        assertPolicyShape(outside);
    }

    @Test
    public void redrawArmsRetainThresholdAverageAndFailureScores() {
        PolicyOperation low = redraw(BattleWeaponsFacts.RedrawFacts.known(2.0f, 3.2d));
        PolicyOperation high = redraw(BattleWeaponsFacts.RedrawFacts.known(3.0f, 3.2d));
        PolicyOperation goodAverage = redraw(BattleWeaponsFacts.RedrawFacts.unknown(3.5d));
        PolicyOperation lowAverage = redraw(BattleWeaponsFacts.RedrawFacts.unknown(3.49d));
        PolicyOperation failed = redraw(BattleWeaponsFacts.RedrawFacts.readFailed());

        assertRawFloat(100.0f, low.delta());
        assertRawFloat(-300.0f, high.delta());
        assertRawFloat(30.0f, goodAverage.delta());
        assertRawFloat(-50.0f, lowAverage.delta());
        assertRawFloat(30.0f, failed.delta());
        assertEquals("V37 REDRAW: Current destiny 2 is LOW (avg 3.2) — try for better!",
                low.reason());
        assertEquals("V37 DON'T REDRAW: Current destiny 3 is GOOD (avg 3.2) — keep it!",
                high.reason());
        assertEquals("Redraw destiny — good average in reserve", goodAverage.reason());
        assertEquals("Redraw destiny — risky, low average in reserve", lowAverage.reason());
        assertEquals("Redraw destiny", failed.reason());
    }

    @Test
    public void battleEvaluatorGenericContributionsStayInSourceOrder() {
        BattleWeaponsFacts.BattleEvaluatorFacts facts =
                new BattleWeaponsFacts.BattleEvaluatorFacts(
                        "battle-action", true, true, true,
                        BattleWeaponsFacts.CancelBattleFacts.opponentInitiated(4.0f, 10.0f),
                        true, true);

        PolicyResult result = BattleWeaponsPolicy.scoreBattleEvaluator(facts);

        assertOrdered(result.operations(),
                "BATTLE-fire-base",
                "BATTLE-fire-character",
                "BATTLE-fire-unique",
                "BATTLE-cancel-losing",
                "BATTLE-fire-during-battle",
                "BATTLE-draw-destiny");
        assertRawDeltas(result.operations(), 40.0f, 10.0f, 20.0f, 60.0f, 50.0f, 30.0f);
        assertEquals(List.of(
                        "Fire weapon",
                        "Target character",
                        "Target unique card",
                        "Cancel losing battle (4 vs 10)",
                        "Fire weapons during battle",
                        "Draw battle destiny"),
                result.operations().stream().map(PolicyOperation::reason).toList());
        assertPolicyShape(result);
    }

    @Test
    public void cancelBattleTiersPreserveStrictNegativeFiveBoundary() {
        PolicyOperation own = cancel(BattleWeaponsFacts.CancelBattleFacts.ownInitiated());
        PolicyOperation losing = cancel(
                BattleWeaponsFacts.CancelBattleFacts.opponentInitiated(4.0f, 10.0f));
        PolicyOperation boundary = cancel(
                BattleWeaponsFacts.CancelBattleFacts.opponentInitiated(5.0f, 10.0f));
        PolicyOperation even = cancel(
                BattleWeaponsFacts.CancelBattleFacts.opponentInitiated(10.0f, 10.0f));

        assertRawFloat(-150.0f, own.delta());
        assertRawFloat(60.0f, losing.delta());
        assertRawFloat(20.0f, boundary.delta());
        assertRawFloat(-60.0f, even.delta());
        assertEquals("DO NOT cancel our own battle! Waste of interrupt.", own.reason());
        assertEquals("Cancel unfavorable battle (5 vs 10)", boundary.reason());
        assertEquals("Don't cancel - we're not losing (10 vs 10)", even.reason());
    }

    @Test
    public void destinyBandsRetainExactThresholdScores() {
        PolicyOperation easy = destinyBand(2.0f, 5.0f);
        PolicyOperation marginal = destinyBand(5.0f, 5.0f);
        PolicyOperation miss = destinyBand(5.1f, 5.0f);

        assertEquals("V36-easy-hit", easy.ruleArmId().id());
        assertEquals("V36-marginal-hit", marginal.ruleArmId().id());
        assertEquals("V36-likely-miss", miss.ruleArmId().id());
        assertRawFloat(200.0f, easy.delta());
        assertRawFloat(50.0f, marginal.delta());
        assertRawFloat(-150.0f, miss.delta());
        assertEquals("V36 EASY HIT: Target defense 2, expected destiny 5.0 — HIGH hit chance!",
                easy.reason());
        assertEquals("V36 MARGINAL HIT: Target defense 5, expected destiny 5.0 — might hit",
                marginal.reason());
        assertEquals("V36 LIKELY MISS: Target defense 5, expected destiny 5.0 — probably won't hit!",
                miss.reason());
    }

    @Test
    public void v383SelfTargetIsFinalAfterV51V36AndPriorityOperations() {
        String exactTargetId = "adapter-target::physical-731";
        BattleWeaponsFacts.TargetFacts facts = new BattleWeaponsFacts.TargetFacts(
                exactTargetId,
                "Adapter Supplied Target",
                true,
                BattleWeaponsFacts.DestinyAssessment.available(2.0f, 6.0f),
                true,
                true,
                true,
                true);

        PolicyResult result = BattleWeaponsPolicy.scoreTarget(facts);

        assertOrdered(result.operations(),
                "V51-already-hit",
                "V36-easy-hit",
                "V36-priority-game-text-canceler",
                "V36-priority-battle-destiny-adder",
                "V36-hunt-jedi-padawan",
                "V38.3-self-target");
        assertRawDeltas(result.operations(), -500.0f, 200.0f, 300.0f, 100.0f, 80.0f, -9999.0f);
        assertTrue(result.operations().stream()
                .allMatch(operation -> exactTargetId.equals(operation.actionId())));
        assertEquals(List.of(
                        "V51 ALREADY HIT: Target already hit — don't waste weapon!",
                        "V36 EASY HIT: Adapter Supplied Target defense 2, expected destiny 6.0 — HIGH hit chance!",
                        "V36 PRIORITY: Padme cancels Vader's game text — REMOVE HER!",
                        "V36 PRIORITY: Battle destiny adder — dangerous!",
                        "V36 HUNT: Jedi/Padawan target — Hunt Down bonus!",
                        "V38.3 SELF-TARGET: NEVER target own card with harmful effect!"),
                result.operations().stream().map(PolicyOperation::reason).toList());

        PolicyOperation last = result.operations().get(result.operations().size() - 1);
        assertEquals(TraceOutputKind.VETO, last.outputKind());
        assertEquals(PolicyOperationKind.ADD, last.kind());
        assertEquals("V38.3 SELF-TARGET: NEVER target own card with harmful effect!",
                last.reason());
        assertPolicyShape(result);
    }

    private static BattleWeaponsFacts.ActionTextFacts actionText(
            String actionId,
            BattleWeaponsFacts.ForcePushMode forcePush,
            BattleWeaponsFacts.FireMode fire,
            BattleWeaponsFacts.ThrowMode throwMode,
            BattleWeaponsFacts.RedrawFacts redraw) {
        return new BattleWeaponsFacts.ActionTextFacts(
                actionId, forcePush, fire, throwMode, redraw);
    }

    private static PolicyOperation redraw(BattleWeaponsFacts.RedrawFacts redraw) {
        return only(BattleWeaponsPolicy.scoreActionText(actionText(
                "redraw", BattleWeaponsFacts.ForcePushMode.NONE,
                BattleWeaponsFacts.FireMode.NONE, BattleWeaponsFacts.ThrowMode.NONE,
                redraw)));
    }

    private static PolicyOperation cancel(BattleWeaponsFacts.CancelBattleFacts cancel) {
        return only(BattleWeaponsPolicy.scoreBattleEvaluator(
                new BattleWeaponsFacts.BattleEvaluatorFacts(
                        "cancel", false, false, false, cancel, false, false)));
    }

    private static PolicyOperation destinyBand(float defense, float expectedDestiny) {
        return only(BattleWeaponsPolicy.scoreTarget(new BattleWeaponsFacts.TargetFacts(
                "target", "Target", false,
                BattleWeaponsFacts.DestinyAssessment.available(defense, expectedDestiny),
                false, false, false, false)));
    }

    private static PolicyOperation only(PolicyResult result) {
        assertEquals(1, result.operations().size());
        assertPolicyShape(result);
        return result.operations().get(0);
    }

    private static void assertOperation(PolicyOperation operation,
                                        String actionId,
                                        String ruleId,
                                        TraceOutputKind outputKind,
                                        float delta,
                                        String reason) {
        assertEquals(actionId, operation.actionId());
        assertEquals(ruleId, operation.ruleArmId().id());
        assertEquals(TraceDomainId.BATTLE_WEAPONS, operation.domainId());
        assertEquals(outputKind, operation.outputKind());
        assertEquals(PolicyOperationKind.ADD, operation.kind());
        assertRawFloat(delta, operation.delta());
        assertEquals(reason, operation.reason());
    }

    private static void assertOrdered(List<PolicyOperation> operations, String... ruleIds) {
        assertEquals(ruleIds.length, operations.size());
        for (int i = 0; i < ruleIds.length; i++) {
            assertEquals(ruleIds[i], operations.get(i).ruleArmId().id());
        }
    }

    private static void assertRawDeltas(List<PolicyOperation> operations, float... deltas) {
        assertEquals(deltas.length, operations.size());
        for (int i = 0; i < deltas.length; i++) {
            assertRawFloat(deltas[i], operations.get(i).delta());
        }
    }

    private static void assertRawFloat(float expected, float actual) {
        assertEquals(Float.floatToRawIntBits(expected), Float.floatToRawIntBits(actual));
    }

    private static void assertPolicyShape(PolicyResult result) {
        Set<String> ruleIds = new HashSet<>();
        for (PolicyOperation operation : result.operations()) {
            assertEquals(TraceDomainId.BATTLE_WEAPONS, operation.domainId());
            assertEquals(PolicyOperationKind.ADD, operation.kind());
            assertTrue(ruleIds.add(operation.ruleArmId().id()));
        }
    }
}
