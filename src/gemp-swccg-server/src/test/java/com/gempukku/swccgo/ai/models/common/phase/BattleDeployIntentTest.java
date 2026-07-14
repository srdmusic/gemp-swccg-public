package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.ForceObligationVector;
import com.gempukku.swccgo.common.DeployDestinationRef;
import com.gempukku.swccgo.common.DeployPhysicalCardRef;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Zone;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/** Accepted DEPLOY intent is exact, optional, target-bound evidence only. */
public class BattleDeployIntentTest {

    @Test
    public void completedOverpowerCarriesExactTargetIntoSameTurn() {
        DeployTransaction transaction = completed(
                DeployFormationAssessment.Verdict.OVERPOWER_OPPORTUNITY);

        BattleDeployIntent intent = BattleDeployIntent.from(
                transaction, 7, "bot");

        assertEquals(BattleDeployIntent.Kind.OVERPOWER, intent.kind());
        assertEquals(Integer.valueOf(220), intent.targetCardId());
        assertEquals(intent, intent.forTarget(220));
        assertEquals(BattleDeployIntent.none(), intent.forTarget(221));
    }

    @Test
    public void completedRescueIsDistinctAndNeverLeaksAcrossTurnOrPlayer() {
        DeployTransaction transaction = completed(
                DeployFormationAssessment.Verdict.TARGETED_RESCUE);

        assertEquals(BattleDeployIntent.Kind.TARGETED_RESCUE,
                BattleDeployIntent.from(transaction, 7, "bot").kind());
        assertEquals(BattleDeployIntent.none(),
                BattleDeployIntent.from(transaction, 8, "bot"));
        assertEquals(BattleDeployIntent.none(),
                BattleDeployIntent.from(transaction, 7, "other"));
    }

    @Test
    public void ordinarySafeDeployCarriesNoBattleIntent() {
        assertEquals(BattleDeployIntent.none(),
                BattleDeployIntent.from(completed(
                        DeployFormationAssessment.Verdict.SAFE_SOLO),
                        7, "bot"));
    }

    private static DeployTransaction completed(
            DeployFormationAssessment.Verdict verdict) {
        DeployPhysicalCardRef source = new DeployPhysicalCardRef(10, 110);
        DeployPhysicalCardRef target = new DeployPhysicalCardRef(20, 220);
        DeployDestinationRef destination = new DeployDestinationRef.Card(target);
        DeployFormationAssessment formation = new DeployFormationAssessment(
                verdict, source, List.of(destination),
                List.of(destination), List.of(), source, null,
                "fixture accepted intent");
        ForceObligationVector obligations = new ForceObligationVector(
                0, 0, 0, false, false, false,
                false, 0, false);
        DeployTransaction.Key key = new DeployTransaction.Key(
                1L, 2, 7, Phase.DEPLOY, "bot", "attempt-1", 99);
        return DeployTransaction.snapshot(
                key, source, Zone.HAND, 0, "0",
                List.of(destination), List.of(), formation,
                obligations, 2f)
                .parentAccepted("0")
                .committed()
                .completed();
    }
}
