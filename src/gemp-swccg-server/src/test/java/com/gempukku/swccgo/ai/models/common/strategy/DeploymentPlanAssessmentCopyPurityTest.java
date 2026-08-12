package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.PersistentResponsePolicy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class DeploymentPlanAssessmentCopyPurityTest {

    @Test
    public void randoAssessmentCopyDeepCopiesInstructions() {
        com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan plan =
            new com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan(
                com.gempukku.swccgo.ai.models.rando.strategy.DeployStrategy.REINFORCE, "base plan");
        com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction original =
            new com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction(
                "bp-1", "Stormtrooper", "loc-1", "Docking Bay", 1, "reinforce");
        original.setVerifiedCrewPackage(true);
        plan.addInstruction(original);

        com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan copy = plan.assessmentCopy();
        com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction copied =
            copy.getInstructions().get(0);

        assertNotSame(plan.getInstructions(), copy.getInstructions());
        assertNotSame(original, copied);
        assertTrue(copied.isVerifiedCrewPackage());

        copied.setVerifiedCrewPackage(false);
        copied.setCardPermanentCardId(999);
        copied.setCardCurrentCardId(888);
        copied.setCardName("MUTATED");
        copied.setTargetLocationId("MUTATED-LOC");
        copy.setWaitingForPlannedCards(true);
        copy.setForceAllowExtras(true);
        copy.getHoldBackCards().add("copy-only");

        assertNull(original.getCardPermanentCardId());
        assertNull(original.getCardCurrentCardId());
        assertEquals("Stormtrooper", original.getCardName());
        assertEquals("loc-1", original.getTargetLocationId());
        assertTrue(original.isVerifiedCrewPackage());
        assertFalse(plan.isWaitingForPlannedCards());
        assertFalse(plan.isForceAllowExtras());
        assertFalse(plan.getHoldBackCards().contains("copy-only"));
        assertEquals("bp-1", copied.getCardBlueprintId());
        assertEquals(1, copied.getPriority());
    }

    @Test
    public void chosenOneAssessmentCopyDeepCopiesInstructions() {
        com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan plan =
            new com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan(
                com.gempukku.swccgo.ai.models.chosenone.strategy.DeployStrategy.REINFORCE, "base plan");
        com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentInstruction original =
            new com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentInstruction(
                "bp-1", "Stormtrooper", "loc-1", "Docking Bay", 1, "reinforce");
        original.setVerifiedCrewPackage(true);
        plan.addInstruction(original);

        com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan copy = plan.assessmentCopy();
        com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentInstruction copied =
            copy.getInstructions().get(0);

        assertNotSame(plan.getInstructions(), copy.getInstructions());
        assertNotSame(original, copied);
        assertTrue(copied.isVerifiedCrewPackage());

        copied.setVerifiedCrewPackage(false);
        copied.setCardPermanentCardId(999);
        copied.setCardCurrentCardId(888);
        copied.setCardName("MUTATED");
        copied.setTargetLocationId("MUTATED-LOC");
        copy.setWaitingForPlannedCards(true);
        copy.setForceAllowExtras(true);
        copy.getHoldBackCards().add("copy-only");

        assertNull(original.getCardPermanentCardId());
        assertNull(original.getCardCurrentCardId());
        assertEquals("Stormtrooper", original.getCardName());
        assertEquals("loc-1", original.getTargetLocationId());
        assertTrue(original.isVerifiedCrewPackage());
        assertFalse(plan.isWaitingForPlannedCards());
        assertFalse(plan.isForceAllowExtras());
        assertFalse(plan.getHoldBackCards().contains("copy-only"));
        assertEquals("bp-1", copied.getCardBlueprintId());
        assertEquals(1, copied.getPriority());
    }

    @Test
    public void bothPlansAdvanceAndCopyRemainingResponseWaveMembership() {
        PersistentResponsePolicy.LocationKey location =
                new PersistentResponsePolicy.LocationKey(117,
                        "Carbonite Chamber");
        PersistentResponsePolicy.Obligation obligation =
                new PersistentResponsePolicy.Obligation(
                        new PersistentResponsePolicy.CandidateKey(
                                "carbonite-response"),
                        PersistentResponsePolicy.CandidateKind.RESPONSE_TARGET,
                        List.of(
                                new PersistentResponsePolicy
                                        .DeployActionKey(10, 20),
                                new PersistentResponsePolicy
                                        .DeployActionKey(11, 21)),
                        location, location,
                        PersistentResponsePolicy.TargetRole.PERSISTENT_DAMAGE,
                        PersistentResponsePolicy.Mode.CONTEST,
                        PersistentResponsePolicy.PERSISTENT_RESPONSE_BONUS,
                        0, "selected-executable-response");

        com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan rando =
                new com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan(
                        com.gempukku.swccgo.ai.models.rando.strategy.DeployStrategy.REINFORCE,
                        "persistent response");
        rando.setPersistentResponseObligation(obligation);
        var randoA =
                new com.gempukku.swccgo.ai.models.rando.strategy
                        .DeploymentInstruction(
                        "bp-a", "A", "117", "Carbonite", 1, "wave");
        randoA.setCardPermanentCardId(10);
        randoA.setCardCurrentCardId(20);
        var randoB =
                new com.gempukku.swccgo.ai.models.rando.strategy
                        .DeploymentInstruction(
                        "bp-b", "B", "117", "Carbonite", 2, "wave");
        randoB.setCardPermanentCardId(11);
        randoB.setCardCurrentCardId(21);
        rando.addInstruction(randoA);
        rando.addInstruction(randoB);
        com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan chosen =
                new com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan(
                        com.gempukku.swccgo.ai.models.chosenone.strategy.DeployStrategy.REINFORCE,
                        "persistent response");
        chosen.setPersistentResponseObligation(obligation);
        var chosenA =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                        .DeploymentInstruction(
                        "bp-a", "A", "117", "Carbonite", 1, "wave");
        chosenA.setCardPermanentCardId(10);
        chosenA.setCardCurrentCardId(20);
        var chosenB =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                        .DeploymentInstruction(
                        "bp-b", "B", "117", "Carbonite", 2, "wave");
        chosenB.setCardPermanentCardId(11);
        chosenB.setCardCurrentCardId(21);
        chosen.addInstruction(chosenA);
        chosen.addInstruction(chosenB);

        assertEquals(obligation,
                rando.assessmentCopy().getPersistentResponseObligation());
        assertEquals(obligation,
                chosen.assessmentCopy().getPersistentResponseObligation());

        var staleRando = rando.assessmentCopy();
        var staleChosen = chosen.assessmentCopy();
        staleRando.recordUnavailablePlannedCard(10, 20, "bp-a");
        staleChosen.recordUnavailablePlannedCard(10, 20, "bp-a");
        assertNull(staleRando.getPersistentResponseObligation());
        assertNull(staleChosen.getPersistentResponseObligation());
        assertEquals(1, staleRando.getInstructions().size());
        assertEquals(1, staleChosen.getInstructions().size());
        assertEquals(Integer.valueOf(11), staleRando.getInstructions()
                .get(0).getCardPermanentCardId());
        assertEquals(Integer.valueOf(11), staleChosen.getInstructions()
                .get(0).getCardPermanentCardId());

        rando.recordDeployment(10, 20, "bp-a");
        chosen.recordDeployment(10, 20, "bp-a");
        List<PersistentResponsePolicy.DeployActionKey> remaining =
                List.of(new PersistentResponsePolicy.DeployActionKey(
                        11, 21));
        assertEquals(remaining, rando.getPersistentResponseObligation()
                .responseActions());
        assertEquals(remaining, chosen.getPersistentResponseObligation()
                .responseActions());
        assertEquals(remaining, rando.assessmentCopy()
                .getPersistentResponseObligation().responseActions());
        assertEquals(remaining, chosen.assessmentCopy()
                .getPersistentResponseObligation().responseActions());
        assertFalse(PersistentResponsePolicy
                .matchesSelectedResponseAction(
                        rando.getPersistentResponseObligation(),
                        10, 20, 117));
        assertTrue(PersistentResponsePolicy
                .matchesSelectedResponseAction(
                        rando.getPersistentResponseObligation(),
                        11, 21, 117));

        rando.recordDeployment(11, 21, "bp-b");
        chosen.recordDeployment(11, 21, "bp-b");
        assertNull(rando.getPersistentResponseObligation());
        assertNull(chosen.getPersistentResponseObligation());
    }
}
