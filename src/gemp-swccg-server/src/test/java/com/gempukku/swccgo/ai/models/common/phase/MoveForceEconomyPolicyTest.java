package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MoveForceEconomyPolicyTest {
    @Test
    public void reserveRequiresAnActualFutureObligation() {
        assertEquals(MoveForceEconomyPolicy.Mode.NONE,
                reserve(0, false, false, true).mode());
    }

    @Test
    public void reservePreservesHardCriticalAndMildBoundaries() {
        assertOperation(reserve(1, true, false, false),
                MoveForceEconomyPolicy.Mode.HARD_RESERVE,
                "V29-move-reserve", TraceDomainId.MOVE, -100.0f);
        assertOperation(reserve(1, true, false, true),
                MoveForceEconomyPolicy.Mode.HARD_RESERVE,
                "V29-move-reserve", TraceDomainId.MOVE, -150.0f);
        assertOperation(reserve(2, true, false, false),
                MoveForceEconomyPolicy.Mode.LOW_RESERVE,
                "V29-move-reserve", TraceDomainId.MOVE, -60.0f);
        assertEquals(MoveForceEconomyPolicy.Mode.NONE,
                reserve(3, true, false, false).mode());
    }

    @Test
    public void twoReserveObligationsPreserveEqualityBoundaries() {
        assertOperation(reserve(2, true, true, false),
                MoveForceEconomyPolicy.Mode.HARD_RESERVE,
                "V29-move-reserve", TraceDomainId.MOVE, -100.0f);
        assertOperation(reserve(3, true, true, false),
                MoveForceEconomyPolicy.Mode.LOW_RESERVE,
                "V29-move-reserve", TraceDomainId.MOVE, -60.0f);
        assertEquals(MoveForceEconomyPolicy.Mode.NONE,
                reserve(4, true, true, false).mode());
    }

    @Test
    public void maintenancePreservesCostPlusOneBoundary() {
        assertOperation(MoveForceEconomyPolicy.maintenance("a", 2, 3),
                MoveForceEconomyPolicy.Mode.MAINTENANCE_CONSERVE,
                "V27-maintenance-move", TraceDomainId.FORCE_BUDGET, -80.0f);
        assertEquals(MoveForceEconomyPolicy.Mode.NONE,
                MoveForceEconomyPolicy.maintenance("a", 2, 4).mode());
        assertEquals(MoveForceEconomyPolicy.Mode.NONE,
                MoveForceEconomyPolicy.maintenance("a", 0, 0).mode());
    }

    @Test
    public void policyOperationsAreBandedAndKeepExactReasonText() {
        PolicyOperation reserve = reserve(1, true, false, false)
                .result().operations().get(0);
        assertEquals(TraceOutputKind.BANDED, reserve.outputKind());
        assertEquals(
                "V29 FORCE RESERVE: Only 1 Force, need 1 (DTF=true, grabber=false) — save Force!",
                reserve.reason());

        PolicyOperation maintenance = MoveForceEconomyPolicy
                .maintenance("a", 2, 3).result().operations().get(0);
        assertEquals(TraceOutputKind.BANDED, maintenance.outputKind());
        assertEquals(
                "V27 MAINTENANCE: Need 2 Force for upkeep, only 3 left — DON'T waste Force moving!",
                maintenance.reason());
    }

    @Test
    public void policyDeltasMatchLegacyFormulasAcrossBoundaries() {
        for (int forcePile = 0; forcePile <= 6; forcePile++) {
            for (boolean dtf : new boolean[]{false, true}) {
                for (boolean grabber : new boolean[]{false, true}) {
                    for (boolean critical : new boolean[]{false, true}) {
                        MoveForceEconomyPolicy.Evaluation evaluation =
                                reserve(forcePile, dtf, grabber, critical);
                        assertEquals(Float.floatToRawIntBits(legacyReserveDelta(
                                        forcePile, dtf, grabber, critical)),
                                Float.floatToRawIntBits(deltaOrZero(evaluation)));
                    }
                }
            }
        }

        for (int maintenanceCost = 0; maintenanceCost <= 5; maintenanceCost++) {
            for (int forcePile = 0; forcePile <= 7; forcePile++) {
                MoveForceEconomyPolicy.Evaluation evaluation =
                        MoveForceEconomyPolicy.maintenance(
                                "a", maintenanceCost, forcePile);
                float legacy = maintenanceCost > 0
                        && forcePile <= maintenanceCost + 1 ? -80.0f : 0.0f;
                assertEquals(Float.floatToRawIntBits(legacy),
                        Float.floatToRawIntBits(deltaOrZero(evaluation)));
            }
        }
    }

    private static MoveForceEconomyPolicy.Evaluation reserve(
            int forcePile, boolean dtf, boolean grabber, boolean critical) {
        return MoveForceEconomyPolicy.reserve(
                "a", forcePile, dtf, grabber, critical);
    }

    private static void assertOperation(
            MoveForceEconomyPolicy.Evaluation evaluation,
            MoveForceEconomyPolicy.Mode mode,
            String ruleId, TraceDomainId domainId, float delta) {
        assertEquals(mode, evaluation.mode());
        assertEquals(1, evaluation.result().operations().size());
        PolicyOperation operation = evaluation.result().operations().get(0);
        assertEquals(ruleId, operation.ruleArmId().id());
        assertEquals(domainId, operation.domainId());
        assertEquals(Float.floatToRawIntBits(delta),
                Float.floatToRawIntBits(operation.delta()));
    }

    private static float legacyReserveDelta(
            int forcePile, boolean dtf, boolean grabber, boolean critical) {
        int reserveNeeded = (dtf ? 1 : 0) + (grabber ? 1 : 0);
        if (reserveNeeded > 0 && forcePile <= reserveNeeded) {
            return critical ? -150.0f : -100.0f;
        }
        if (reserveNeeded > 0 && forcePile <= reserveNeeded + 1) {
            return -60.0f;
        }
        return 0.0f;
    }

    private static float deltaOrZero(
            MoveForceEconomyPolicy.Evaluation evaluation) {
        return evaluation.result().operations().isEmpty()
                ? 0.0f : evaluation.result().operations().get(0).delta();
    }
}
