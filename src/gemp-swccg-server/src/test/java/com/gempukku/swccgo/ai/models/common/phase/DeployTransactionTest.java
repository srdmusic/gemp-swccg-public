package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.ForceObligationVector;
import com.gempukku.swccgo.common.DeployDestinationRef;
import com.gempukku.swccgo.common.DeployPhysicalCardRef;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Zone;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

/** Exact physical identity and accepted-response lifecycle contract. */
public class DeployTransactionTest {
    private static final DeployPhysicalCardRef SOURCE =
            new DeployPhysicalCardRef(1002, 102);
    private static final DeployPhysicalCardRef BUDDY =
            new DeployPhysicalCardRef(4002, 402);
    private static final DeployDestinationRef.Card DESTINATION =
            new DeployDestinationRef.Card(new DeployPhysicalCardRef(3001, 301));
    private static final ForceObligationVector OBLIGATIONS =
            new ForceObligationVector(6, 2, 1, true, false,
                    true, true, 1, false);

    @Test
    public void forcedDestinationIsBoundAtParentAcceptance() {
        DeployTransaction transaction = snapshot(
                List.of(DESTINATION), List.of(BUDDY)).parentAccepted("1");

        assertEquals(DeployTransaction.Stage.PARENT_PENDING, transaction.stage());
        assertEquals(DESTINATION, transaction.selectedDestination());
        assertEquals(OBLIGATIONS, transaction.forceObligations());
        assertEquals(List.of(DeployTransaction.Stage.SNAPSHOT,
                DeployTransaction.Stage.PARENT_PENDING), transaction.history());
    }

    @Test
    public void childSequenceCommitsAndCompletesExactlyOnce() {
        DeployDestinationRef.Card second = new DeployDestinationRef.Card(
                new DeployPhysicalCardRef(3002, 302));
        DeployTransaction transaction = snapshot(
                List.of(DESTINATION, second), List.of(BUDDY))
                .parentAccepted("1")
                .destinationAccepted(second, "302")
                .buddyAccepted(BUDDY, "402")
                .committed()
                .completed();

        assertEquals(DeployTransaction.Stage.COMPLETED, transaction.stage());
        assertEquals(second, transaction.selectedDestination());
        assertEquals(BUDDY, transaction.selectedBuddy());
        assertEquals(List.of("1", "302", "402"), transaction.acceptedWires());
        assertThrows(IllegalStateException.class, transaction::completed);
    }

    @Test
    public void duplicateBlueprintCopiesCannotAdvanceEachOther() {
        DeployPhysicalCardRef otherCopy = new DeployPhysicalCardRef(4003, 402);
        DeployTransaction transaction = snapshot(
                List.of(DESTINATION), List.of(BUDDY)).parentAccepted("1");

        assertNotEquals(BUDDY, otherCopy);
        assertThrows(IllegalArgumentException.class,
                () -> transaction.buddyAccepted(otherCopy, "402"));
    }

    @Test
    public void unexpectedDestinationAndExplicitTerminationFailClosed() {
        DeployTransaction transaction = snapshot(
                List.of(DESTINATION), List.of()).parentAccepted("1");
        DeployDestinationRef.Card drift = new DeployDestinationRef.Card(
                new DeployPhysicalCardRef(3999, 399));

        assertThrows(IllegalArgumentException.class,
                () -> transaction.destinationAccepted(drift, "399"));
        DeployTransaction terminated = transaction.terminated("engine rejection");
        assertEquals("engine rejection", terminated.terminalReason());
        assertEquals(DeployTransaction.Stage.PARENT_PENDING, terminated.stage());
    }

    @Test
    public void deferredPullFormationBindsOnceWithoutReplacingAnEstablishedAssessment() {
        DeployTransaction.Key key = new DeployTransaction.Key(
                1L, 123, 5, Phase.DEPLOY, DeployTestFixtures.PLAYER,
                "PULL-90001", DeployTestFixtures.DECISION_ID);
        DeployFormationAssessment unknown = DeployFormationAssessment.unknown(
                SOURCE, List.of(), "destination child has not arrived");
        DeployTransaction deferred = DeployTransaction.snapshot(
                key, SOURCE, Zone.RESERVE_DECK, DeployTestFixtures.PARENT_ORDINAL,
                "PULL-90001", List.of(), List.of(), unknown,
                OBLIGATIONS, 4f).parentAccepted("temp0");
        DeployFormationAssessment accepted = new DeployFormationAssessment(
                DeployFormationAssessment.Verdict.SAFE_SOLO,
                SOURCE, List.of(DESTINATION), SOURCE, null,
                "deferred destination is safe");

        DeployTransaction bound = deferred.formationAccepted(accepted);

        assertSame(accepted, bound.formation());
        assertEquals(List.of(DESTINATION), bound.orderedDestinations());
        assertSame(bound, bound.formationAccepted(accepted));
        assertThrows(IllegalStateException.class,
                () -> bound.formationAccepted(new DeployFormationAssessment(
                        DeployFormationAssessment.Verdict.TARGETED_RESCUE,
                        SOURCE, List.of(DESTINATION), SOURCE, null,
                        "replacement must be rejected")));
    }

    private static DeployTransaction snapshot(
            List<DeployDestinationRef> destinations,
            List<DeployPhysicalCardRef> buddies) {
        DeployTransaction.Key key = new DeployTransaction.Key(
                1L, 123, 5, Phase.DEPLOY, DeployTestFixtures.PLAYER,
                DeployTestFixtures.ATTEMPT, DeployTestFixtures.DECISION_ID);
        DeployFormationAssessment formation = buddies.isEmpty()
                ? new DeployFormationAssessment(
                        DeployFormationAssessment.Verdict.SAFE_SOLO,
                        SOURCE, destinations, SOURCE, null, "safe solo")
                : new DeployFormationAssessment(
                        DeployFormationAssessment.Verdict.SAFE_SEQUENCE,
                        SOURCE, destinations, SOURCE, BUDDY, "safe sequence");
        return DeployTransaction.snapshot(
                key, SOURCE, Zone.HAND, DeployTestFixtures.PARENT_ORDINAL,
                "1", destinations, buddies, formation, OBLIGATIONS, 4f);
    }
}
