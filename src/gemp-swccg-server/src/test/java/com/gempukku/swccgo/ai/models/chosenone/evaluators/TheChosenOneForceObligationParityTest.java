package com.gempukku.swccgo.ai.models.chosenone.evaluators;

import com.gempukku.swccgo.ai.models.chosenone.strategy.DeployStrategy;
import com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan;
import com.gempukku.swccgo.ai.models.common.decision.FactValue;
import com.gempukku.swccgo.ai.models.common.phase.DeployAssessment;
import com.gempukku.swccgo.ai.models.common.phase.DeployFacts;
import com.gempukku.swccgo.ai.models.common.phase.DeployFormationAssessment;
import com.gempukku.swccgo.ai.models.common.phase.DeployRoute;
import com.gempukku.swccgo.ai.models.common.strategy.ForceObligationVector;
import com.gempukku.swccgo.common.DeployDestinationRef;
import com.gempukku.swccgo.common.DeployPhysicalCardRef;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Zone;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertSame;

public class TheChosenOneForceObligationParityTest {

    @Test
    public void parentChildPassAndMoveShareOneVector() {
        ForceObligationVector obligations = new ForceObligationVector(
                3, 2, 1, true, true, true, true, 2, true);
        DeployFacts facts = parentFacts();
        DeployFormationAssessment formation = DeployFormationAssessment.unknown(
                facts.parentCandidates().get(0).sourceCard(),
                facts.parentCandidates().get(0).orderedDestinations(),
                "fixture formation");
        DeployAssessment parent = new DeployAssessment(
                DeployRoute.DEPLOY_PARENT, formation, obligations, null);
        DeployAssessment child = new DeployAssessment(
                DeployRoute.DEPLOY_DESTINATION, formation, obligations, null);
        DecisionContext context = new DecisionContext(
                null, "p1", "CARD_ACTION_CHOICE", "deploy", "7", Phase.DEPLOY);
        context.setDeployTransaction(
                facts, parent, obligations,
                new DeploymentPlan(DeployStrategy.REINFORCE, "fixture"));

        assertSame(obligations, parent.forceObligations());
        assertSame(obligations, child.forceObligations());
        assertSame(obligations, context.getForceObligations());
        assertSame(obligations, PassEvaluator.forceObligations(context));
        assertSame(obligations, MoveEvaluator.forceObligations(context));
    }

    private static DeployFacts parentFacts() {
        DeployPhysicalCardRef source = new DeployPhysicalCardRef(101, 201);
        DeployDestinationRef destination = new DeployDestinationRef.Card(
                new DeployPhysicalCardRef(102, 202));
        DeployFacts.ParentCandidate candidate = new DeployFacts.ParentCandidate(
                0, "0", "attempt-7", source, Zone.HAND, true,
                List.of(destination), List.of(), null,
                FactValue.known(2f, "fixture", "deploy cost"));
        return new DeployFacts(
                "7", 1, "p1", Phase.DEPLOY, DeployRoute.DEPLOY_PARENT,
                List.of(candidate), null, null, null, null, null,
                List.of(), List.of(), List.of(), null, false, List.of(),
                FactValue.unknown("fixture", "opponent drain", "not consumed"));
    }
}
