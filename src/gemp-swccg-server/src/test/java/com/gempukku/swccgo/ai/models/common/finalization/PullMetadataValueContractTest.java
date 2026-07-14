package com.gempukku.swccgo.ai.models.common.finalization;

import com.gempukku.swccgo.ai.models.common.phase.PullRoute;
import com.gempukku.swccgo.ai.models.common.phase.PullRouteInput;
import com.gempukku.swccgo.ai.models.common.phase.PullRouteResolver;
import com.gempukku.swccgo.common.DecisionActionSemantic;
import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.PullDeployRef;
import com.gempukku.swccgo.common.PullPhysicalCardRef;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.logic.actions.SystemQueueAction;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import com.gempukku.swccgo.logic.timing.SnapshotData;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

/** Value and snapshot contracts for engine-owned PULL metadata. */
public class PullMetadataValueContractTest {
    private static final long TRANSACTION_ID = 9001L;

    @Test
    public void conflictingSemanticMarksFailClosedAndSnapshotKeepsTheConflict() {
        SystemQueueAction original = new SystemQueueAction();
        original.setDecisionActionSemantic(DecisionActionSemantic.PULL_DEPLOY_FROM_PILE);
        original.setDecisionActionSemantic(DecisionActionSemantic.PULL_TAKE_INTO_HAND_FROM_PILE);
        original.setAcceptedPullTransactionId(TRANSACTION_ID);

        assertEquals(DecisionActionSemantic.UNKNOWN, original.getDecisionActionSemantic());

        SystemQueueAction snapshot = new SnapshotData().getDataForSnapshot(original);
        assertNotSame(original, snapshot);
        assertEquals(DecisionActionSemantic.UNKNOWN, snapshot.getDecisionActionSemantic());
        assertEquals(Long.valueOf(TRANSACTION_ID), snapshot.getAcceptedPullTransactionId());

        snapshot.setDecisionActionSemantic(DecisionActionSemantic.PULL_DEPLOY_FROM_PILE);
        assertEquals(DecisionActionSemantic.UNKNOWN, snapshot.getDecisionActionSemantic());
    }

    @Test
    public void pullDeployRefPreservesSelectedCopyOrderAndForcedDestination() {
        PullPhysicalCardRef source = new PullPhysicalCardRef(1001, 101);
        PullPhysicalCardRef selectedDuplicate = new PullPhysicalCardRef(2002, 902);
        List<PullPhysicalCardRef> mutableDestinations = new ArrayList<>();
        PullDeployRef base = new PullDeployRef(
                TRANSACTION_ID, 73, 2, "asdf", source,
                GameTextActionId.A_CUNNING_WARRIOR__DEPLOY_CARD,
                Zone.RESERVE_DECK, "asdf", selectedDuplicate,
                mutableDestinations, null);

        mutableDestinations.add(new PullPhysicalCardRef(3999, 399));
        assertEquals(TRANSACTION_ID, base.transactionId());
        assertEquals(selectedDuplicate, base.selectedCard());
        assertEquals(List.of(), base.orderedDestinationCards());

        PullPhysicalCardRef second = new PullPhysicalCardRef(3002, 302);
        PullPhysicalCardRef first = new PullPhysicalCardRef(3001, 301);
        PullDeployRef ordered = base.withDestinations(List.of(second, first), false);
        assertEquals(TRANSACTION_ID, ordered.transactionId());
        assertEquals(List.of(second, first), ordered.orderedDestinationCards());
        assertNull(ordered.forcedDestinationCard());
        assertThrows(UnsupportedOperationException.class,
                () -> ordered.orderedDestinationCards().add(new PullPhysicalCardRef(3003, 303)));

        PullDeployRef forced = base.withDestinations(List.of(first), true);
        assertEquals(TRANSACTION_ID, forced.transactionId());
        assertEquals(List.of(first), forced.orderedDestinationCards());
        assertEquals(first, forced.forcedDestinationCard());

        SystemQueueAction action = new SystemQueueAction();
        action.setPullDeployRef(forced);
        SystemQueueAction snapshot = new SnapshotData().getDataForSnapshot(action);
        assertSame(forced, snapshot.getPullDeployRef());
        assertEquals(TRANSACTION_ID, snapshot.getPullDeployRef().transactionId());
    }

    @Test
    public void missingTransactionIdFailsClosedAsLegacyUnowned() {
        assertEquals(PullRoute.PULL_DEPLOY_CHILD,
                PullRouteResolver.resolve(pileChildInput(List.of(String.valueOf(TRANSACTION_ID)))));
        assertEquals(PullRoute.LEGACY_UNOWNED,
                PullRouteResolver.resolve(pileChildInput(null)));
    }

    private static PullRouteInput pileChildInput(List<String> transactionIds) {
        return new PullRouteInput(
                74,
                AwaitingDecisionType.ARBITRARY_CARDS,
                List.of(DecisionOrigin.PULL_DEPLOY_CHILD.name()),
                null,
                null,
                List.of("temp0"),
                List.of("1"),
                List.of("1"),
                List.of("73"),
                List.of("2"),
                transactionIds,
                List.of("asdf"),
                List.of("101"),
                List.of("1001"),
                List.of(GameTextActionId.A_CUNNING_WARRIOR__DEPLOY_CARD.name()),
                List.of(Zone.RESERVE_DECK.name()),
                List.of("asdf"),
                List.of("902"),
                List.of("2002"),
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
