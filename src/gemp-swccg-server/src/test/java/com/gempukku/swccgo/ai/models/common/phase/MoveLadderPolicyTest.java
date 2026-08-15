package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MoveLadderPolicyTest {
    @Test
    public void rankClaimsPreserveBandsAndR2StrengthGate() {
        assertEquals(4, MoveLadderPolicy.claimR4(1));
        assertEquals(4, MoveLadderPolicy.claimR3(4));
        assertEquals(3, MoveLadderPolicy.claimR3(1));

        MoveLadderPolicy.RankTwoClaim weak =
                MoveLadderPolicy.claimR2(1, false, 199.0f, 1.0f, true);
        MoveLadderPolicy.RankTwoClaim fine =
                MoveLadderPolicy.claimR2(1, false, 200.0f, 0.0f, true);
        MoveLadderPolicy.RankTwoClaim drain =
                MoveLadderPolicy.claimR2(3, false, -500.0f, 2.0f, false);

        assertFalse(weak.accepted());
        assertEquals(1, weak.rank());
        assertFalse(weak.battleSeekingClaim());
        assertFloat(200.0f, weak.requiredFine());
        assertFloat(2.0f, weak.requiredDrainDelta());
        assertTrue(fine.accepted());
        assertEquals(2, fine.rank());
        assertTrue(fine.battleSeekingClaim());
        assertTrue(drain.accepted());
        assertEquals(3, drain.rank());
        assertFalse(drain.battleSeekingClaim());
    }

    @Test
    public void r2IsIntentionallyTacticalAndMayBeOverridden() {
        MoveLadderPolicy.BandIntegrity bands =
                MoveLadderPolicy.bandIntegrity();

        assertFloat(-2350.0f, bands.r2Floor());
        assertFloat(1920.0f, bands.r1Ceiling());
        assertFloat(-4270.0f, bands.margin());
        assertTrue(bands.inverted());
        assertFloat(1000.0f, bands.rankR2Score());
        assertFloat(2800.0f, bands.fineClamp());
        assertFloat(550.0f, bands.actionTextCrossNegative());
        assertFloat(1670.0f, bands.r1FineCeiling());
        assertFloat(250.0f, bands.actionTextCrossPositive());
    }

    @Test
    public void hardAndWrongDirectionVetoOrderRemainsTerminal() {
        MoveLadderPolicy.Finalization hard =
                MoveLadderPolicy.finalizeAction(
                        state(4, true, "hard", false, null,
                                false, true, true, "wrong", true),
                        300.0f);
        MoveLadderPolicy.Finalization wrong =
                MoveLadderPolicy.finalizeAction(
                        state(2, false, null, false, null,
                                true, false, true, "wrong", true),
                        300.0f);

        assertEquals(1, hard.steps().size());
        assertStep(hard.steps().get(0),
                MoveLadderPolicy.StepKind.HARD_VETO,
                "LADDER VETO: hard", -100000.0f);
        assertEquals(1, wrong.steps().size());
        assertStep(wrong.steps().get(0),
                MoveLadderPolicy.StepKind.WRONG_DIRECTION_VETO,
                "LADDER VETO: wrong", -100000.0f);
    }

    @Test
    public void mandatoryTransitSuppressesWrongDirectionThenKeepsR4() {
        List<MoveLadderPolicy.Step> steps =
                MoveLadderPolicy.finalizeAction(
                        state(4, false, null, false, null,
                                false, true, true, "wrong", true),
                        50.0f)
                        .steps();

        assertEquals(2, steps.size());
        assertStep(steps.get(0),
                MoveLadderPolicy.StepKind.WRONG_DIRECTION_SUPPRESSED,
                "V38.3 wrong-direction suppressed (R4 mandatory transit)",
                0.0f);
        assertStep(steps.get(1),
                MoveLadderPolicy.StepKind.RANK_BASE,
                "LADDER: R4 MANDATORY TRANSIT base", 20000.0f);
    }

    @Test
    public void canWinVetoRemainsLimitedToBattleSeekingR2() {
        List<MoveLadderPolicy.Step> vetoed =
                MoveLadderPolicy.finalizeAction(
                        state(2, false, null, true, "cannot win",
                                true, false, false, null, true),
                        -800.0f)
                        .steps();
        List<MoveLadderPolicy.Step> retainedR2 =
                MoveLadderPolicy.finalizeAction(
                        state(2, false, null, true, "cannot win",
                                false, false, false, null, true),
                        -800.0f)
                        .steps();
        List<MoveLadderPolicy.Step> retainedR3 =
                MoveLadderPolicy.finalizeAction(
                        state(3, false, null, true, "cannot win",
                                true, false, false, null, true),
                        -800.0f)
                        .steps();

        assertEquals(1, vetoed.size());
        assertStep(vetoed.get(0), MoveLadderPolicy.StepKind.CAN_WIN_VETO,
                "LADDER VETO: cannot win", -100000.0f);
        assertEquals(MoveLadderPolicy.StepKind.CAN_WIN_RETAINED,
                retainedR2.get(0).kind());
        assertStep(retainedR2.get(1), MoveLadderPolicy.StepKind.RANK_BASE,
                "LADDER: R2 DOCTRINE base", 1000.0f);
        assertEquals(MoveLadderPolicy.StepKind.CAN_WIN_RETAINED,
                retainedR3.get(0).kind());
        assertStep(retainedR3.get(1), MoveLadderPolicy.StepKind.RANK_BASE,
                "LADDER: R3 SURVIVAL base", 12000.0f);
    }

    @Test
    public void clampAndDemotionPreserveRankSpecificOutcomes() {
        List<MoveLadderPolicy.Step> positiveR2 =
                MoveLadderPolicy.finalizeAction(
                        state(2, false, null, false, null,
                                true, false, false, null, true),
                        3000.0f)
                        .steps();
        List<MoveLadderPolicy.Step> negativeR2 =
                MoveLadderPolicy.finalizeAction(
                        state(2, false, null, false, null,
                                true, false, false, null, true),
                        -3000.0f)
                        .steps();
        List<MoveLadderPolicy.Step> negativeR3 =
                MoveLadderPolicy.finalizeAction(
                        state(3, false, null, false, null,
                                false, false, false, null, true),
                        -3000.0f)
                        .steps();
        List<MoveLadderPolicy.Step> negativeR4 =
                MoveLadderPolicy.finalizeAction(
                        state(4, false, null, false, null,
                                false, true, false, null, true),
                        -3000.0f)
                        .steps();

        assertStep(positiveR2.get(0),
                MoveLadderPolicy.StepKind.POSITIVE_CLAMP,
                "LADDER CLAMP: fines +3000 clamped to +2800", -200.0f);
        assertStep(positiveR2.get(1),
                MoveLadderPolicy.StepKind.RANK_BASE,
                "LADDER: R2 DOCTRINE base", 1000.0f);

        assertEquals(2, negativeR2.size());
        assertStep(negativeR2.get(0),
                MoveLadderPolicy.StepKind.NEGATIVE_CLAMP,
                "LADDER CLAMP: fines -3000 clamped to -2800", 200.0f);
        assertEquals(MoveLadderPolicy.StepKind.DEMOTE,
                negativeR2.get(1).kind());
        assertEquals(2, negativeR2.get(1).rankBefore());
        assertEquals(1, negativeR2.get(1).rankAfter());

        assertEquals(MoveLadderPolicy.StepKind.DEMOTE,
                negativeR3.get(1).kind());
        assertStep(negativeR3.get(2),
                MoveLadderPolicy.StepKind.RANK_BASE,
                "LADDER: R2 DOCTRINE base", 1000.0f);
        assertEquals(MoveLadderPolicy.StepKind.NEGATIVE_CLAMP,
                negativeR4.get(0).kind());
        assertStep(negativeR4.get(1),
                MoveLadderPolicy.StepKind.RANK_BASE,
                "LADDER: R4 MANDATORY TRANSIT base", 20000.0f);
    }

    @Test
    public void defaultPenaltyOnlyAppliesToUnclaimedRankedMoves() {
        List<MoveLadderPolicy.Step> rankedMove =
                MoveLadderPolicy.finalizeAction(
                        state(1, false, null, false, null,
                                false, false, false, null, true),
                        20.0f)
                        .steps();
        List<MoveLadderPolicy.Step> unrankedAction =
                MoveLadderPolicy.finalizeAction(
                        state(1, false, null, false, null,
                                false, false, false, null, false),
                        20.0f)
                        .steps();

        assertEquals(1, rankedMove.size());
        assertStep(rankedMove.get(0),
                MoveLadderPolicy.StepKind.DEFAULT_PENALTY,
                "No strategic reason to move", -50.0f);
        assertTrue(unrankedAction.isEmpty());
    }

    private static MoveLadderPolicy.State state(
            int rank,
            boolean hardVeto,
            String hardVetoReason,
            boolean canWinVeto,
            String canWinVetoReason,
            boolean battleSeekingClaim,
            boolean mandatoryTransit,
            boolean wrongDirectionVeto,
            String wrongDirectionVetoReason,
            boolean rankMoveRan) {
        return new MoveLadderPolicy.State(
                rank,
                hardVeto,
                hardVetoReason,
                canWinVeto,
                canWinVetoReason,
                battleSeekingClaim,
                mandatoryTransit,
                wrongDirectionVeto,
                wrongDirectionVetoReason,
                rankMoveRan);
    }

    private static void assertStep(
            MoveLadderPolicy.Step step,
            MoveLadderPolicy.StepKind kind,
            String reasoning,
            float delta) {
        assertEquals(kind, step.kind());
        assertEquals(reasoning, step.reasoning());
        assertFloat(delta, step.delta());
    }

    private static void assertFloat(float expected, float actual) {
        assertEquals(expected, actual, 0.001f);
    }
}
