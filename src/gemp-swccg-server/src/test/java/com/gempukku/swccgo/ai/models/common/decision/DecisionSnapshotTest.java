package com.gempukku.swccgo.ai.models.common.decision;

import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Pure construction tests for {@link DecisionFacts}, {@link ActionFacts} and
 * {@link DecisionSnapshot} — Batch-2 typed-facts increment 1, no production consumer.
 * Contract: Handoffs/CODEX_RANDO_FACTS_ASSESSMENTS_CONTRACT_2026-07-13.md
 * ("Minimal shared model": immutable, ordered by original ordinal, versioned).
 */
public class DecisionSnapshotTest {

    private static final String PRODUCER = "test-producer";

    private static DecisionFacts decisionFacts() {
        return DecisionFacts.builder()
                .decisionId("42")
                .decisionType("CARD_ACTION_CHOICE")
                .decisionText("Optional responses")
                .phase(Phase.DEPLOY)
                .window("Optional responses")
                .turn(3)
                .currentPlayer("asdf")
                .side(Side.DARK)
                .obligationFlags(Set.of("REQUIRED_TRIGGER"))
                .noPass(false)
                .minimum(0)
                .maximum(1)
                .blockedResponses(Set.of("17"))
                .forceAvailable(FactValue.known(6, PRODUCER, "gameState.forcePile"))
                .lifeForce(FactValue.known(31, PRODUCER, "gameState.lifeForce"))
                .handSize(FactValue.known(8, PRODUCER, "gameState.hand"))
                .reserveDeckSize(FactValue.known(20, PRODUCER, "gameState.reserveDeck"))
                .objectiveIdentity(FactValue.known("200_20", PRODUCER, "objective.blueprintId"))
                .objectiveFlipped(FactValue.known(false, PRODUCER, "objective.flipState"))
                .selectedRoute("DEPLOY_CARD_ACTION")
                .routeSelectionEvidence("decisionType=CARD_ACTION_CHOICE phase=DEPLOY candidates=actionIds")
                .build();
    }

    private static ActionFacts actionFacts(int ordinal) {
        return ActionFacts.builder()
                .ordinal(ordinal)
                .actionId("action-" + ordinal)
                .actionText("Deploy Probe Droid")
                .cardId("card-" + ordinal)
                .blueprintId("7_178")
                .testingText("Probe Droid")
                .selectable(true)
                .resolvedAction(FactValue.known("action-" + ordinal, PRODUCER, "decision.actionIds[" + ordinal + "]"))
                .resolvedCard(FactValue.known("card-" + ordinal, PRODUCER, "gameState.findCardById"))
                .resolvedSource(FactValue.known("hand", PRODUCER, "card.zone"))
                .resolvedDestination(FactValue.unknown(PRODUCER, "action.destination", "destination prompt not yet opened"))
                .cost(FactValue.known(2.0f, PRODUCER, "blueprint.deployCost"))
                .power(FactValue.known(3.0f, PRODUCER, "blueprint.power"))
                .ability(FactValue.known(1.0f, PRODUCER, "blueprint.ability"))
                .friendlyPresenceCount(FactValue.known(0, PRODUCER, "destination.friendlyCharacters"))
                .opposingPresenceCount(FactValue.known(2, PRODUCER, "destination.opposingCharacters"))
                .icons(FactValue.known(Set.of(Icon.DROID, Icon.PRESENCE), PRODUCER, "blueprint.icons"))
                .weaponBonus(FactValue.known(0.0f, PRODUCER, "FormationSafety.weaponBonusOf"))
                .build();
    }

    private static DecisionSnapshot snapshot(int actionCount) {
        List<ActionFacts> actions = new ArrayList<>();
        for (int i = 0; i < actionCount; i++) {
            actions.add(actionFacts(i));
        }
        return new DecisionSnapshot(
                decisionFacts(),
                actions,
                new DecisionSnapshot.ServiceFacts(FactValue.known(3, PRODUCER, "ForceReserveService.maintenanceObligation")),
                DecisionSnapshot.CURRENT_VERSION);
    }

    // ── construction happy path ──

    @Test
    public void constructionHappyPath() {
        DecisionSnapshot snap = snapshot(3);
        assertEquals("42", snap.decisionFacts().decisionId());
        assertEquals("CARD_ACTION_CHOICE", snap.decisionFacts().decisionType());
        assertEquals(Phase.DEPLOY, snap.decisionFacts().phase());
        assertEquals(3, snap.decisionFacts().turn());
        assertEquals("asdf", snap.decisionFacts().currentPlayer());
        assertEquals(Side.DARK, snap.decisionFacts().side());
        assertEquals("DEPLOY_CARD_ACTION", snap.decisionFacts().selectedRoute());
        assertEquals(3, snap.actionFacts().size());
        assertEquals("Deploy Probe Droid", snap.actionFacts().get(1).actionText());
        assertEquals(Integer.valueOf(3), snap.serviceFacts().forceObligations().value());
    }

    @Test
    public void snapshotVersionIsPresent() {
        DecisionSnapshot snap = snapshot(1);
        assertEquals(DecisionSnapshot.CURRENT_VERSION, snap.snapshotVersion());
        assertEquals(1, snap.snapshotVersion());
    }

    @Test
    public void equalSnapshotsFromEqualInputsAreEqual() {
        // Gate prerequisite: Rando and ChosenOne must receive EQUAL snapshots for the same frozen input.
        assertEquals(snapshot(2), snapshot(2));
        assertEquals(snapshot(2).hashCode(), snapshot(2).hashCode());
    }

    // ── unknown handling flows through the composite types ──

    @Test
    public void unknownFactInsideActionPreservesProducerProvenanceAndReason() {
        ActionFacts action = snapshot(1).actionFacts().get(0);
        FactValue<String> dest = action.resolvedDestination();
        assertTrue(dest.isUnknown());
        assertEquals(PRODUCER, dest.producerId());
        assertEquals("action.destination", dest.provenance());
        assertEquals("destination prompt not yet opened", dest.unknownReason());
    }

    @Test
    public void knownFalseObjectiveFlipIsDistinctFromUnknown() {
        FactValue<Boolean> flip = snapshot(1).decisionFacts().objectiveFlipped();
        assertTrue(flip.isKnown());
        assertEquals(Boolean.FALSE, flip.value());
    }

    // ── ordinal ordering preserved, never sorted or rebuilt ──

    @Test
    public void ordinalOrderingIsPreserved() {
        DecisionSnapshot snap = snapshot(4);
        for (int i = 0; i < 4; i++) {
            assertEquals(i, snap.actionFacts().get(i).ordinal());
            assertEquals("action-" + i, snap.actionFacts().get(i).actionId());
        }
    }

    @Test
    public void misorderedOrdinalsAreRejected() {
        List<ActionFacts> reversed = List.of(actionFacts(1), actionFacts(0));
        try {
            new DecisionSnapshot(decisionFacts(), reversed,
                    new DecisionSnapshot.ServiceFacts(FactValue.known(0, PRODUCER, "k")),
                    DecisionSnapshot.CURRENT_VERSION);
            fail("snapshot must reject action facts not ordered by original ordinal");
        } catch (IllegalArgumentException expected) {
            // expected: candidate order is never sorted or rebuilt
        }
    }

    // ── runtime immutability: collections are unmodifiable, later source mutation has no effect ──

    @Test
    public void actionListIsUnmodifiable() {
        DecisionSnapshot snap = snapshot(2);
        try {
            snap.actionFacts().add(actionFacts(2));
            fail("actionFacts list must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
        try {
            snap.actionFacts().remove(0);
            fail("actionFacts list must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test
    public void snapshotIsDetachedFromCallerSuppliedList() {
        List<ActionFacts> source = new ArrayList<>();
        source.add(actionFacts(0));
        DecisionSnapshot snap = new DecisionSnapshot(decisionFacts(), source,
                new DecisionSnapshot.ServiceFacts(FactValue.known(0, PRODUCER, "k")),
                DecisionSnapshot.CURRENT_VERSION);
        source.add(actionFacts(9)); // mutate the source AFTER construction
        assertEquals(1, snap.actionFacts().size());
    }

    @Test
    public void decisionFactsSetsAreUnmodifiableAndDetached() {
        Set<String> blocked = new HashSet<>();
        blocked.add("17");
        DecisionFacts facts = DecisionFacts.builder()
                .decisionId("1").decisionType("CARD_ACTION_CHOICE").decisionText("t")
                .turn(1).currentPlayer("asdf").side(Side.LIGHT)
                .blockedResponses(blocked)
                .forceAvailable(FactValue.known(0, PRODUCER, "k"))
                .lifeForce(FactValue.known(0, PRODUCER, "k"))
                .handSize(FactValue.known(0, PRODUCER, "k"))
                .reserveDeckSize(FactValue.known(0, PRODUCER, "k"))
                .objectiveIdentity(FactValue.unknown(PRODUCER, "k", "no objective on table"))
                .objectiveFlipped(FactValue.unknown(PRODUCER, "k", "no objective on table"))
                .selectedRoute("r").routeSelectionEvidence("e")
                .build();
        try {
            facts.blockedResponses().add("18");
            fail("blockedResponses must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
        try {
            facts.obligationFlags().add("X");
            fail("obligationFlags must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
        blocked.add("19"); // mutate the source AFTER construction
        assertEquals(1, facts.blockedResponses().size());
    }

    @Test
    public void knownIconSetIsUnmodifiableEvenWhenBuiltFromMutableSet() {
        Set<Icon> mutableIcons = new HashSet<>();
        mutableIcons.add(Icon.PILOT);
        ActionFacts action = ActionFacts.builder()
                .ordinal(0)
                .resolvedAction(FactValue.unknown(PRODUCER, "k", "n/a"))
                .resolvedCard(FactValue.unknown(PRODUCER, "k", "n/a"))
                .resolvedSource(FactValue.unknown(PRODUCER, "k", "n/a"))
                .resolvedDestination(FactValue.unknown(PRODUCER, "k", "n/a"))
                .cost(FactValue.unknown(PRODUCER, "k", "n/a"))
                .power(FactValue.unknown(PRODUCER, "k", "n/a"))
                .ability(FactValue.unknown(PRODUCER, "k", "n/a"))
                .friendlyPresenceCount(FactValue.unknown(PRODUCER, "k", "n/a"))
                .opposingPresenceCount(FactValue.unknown(PRODUCER, "k", "n/a"))
                .icons(FactValue.known(mutableIcons, PRODUCER, "blueprint.icons"))
                .weaponBonus(FactValue.unknown(PRODUCER, "k", "n/a"))
                .build();
        try {
            action.icons().value().add(Icon.WARRIOR);
            fail("known icon set must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
        mutableIcons.add(Icon.WARRIOR); // mutate the source AFTER construction
        assertEquals(1, action.icons().value().size());
    }

    // ── required facts are explicit: absent FactValue fields are rejected, not defaulted ──

    @Test
    public void missingRequiredFactIsRejectedNotDefaulted() {
        try {
            ActionFacts.builder().ordinal(0).build();
            fail("ActionFacts without its FactValue fields must be rejected — no silent defaults");
        } catch (NullPointerException expected) {
            // expected: unknown must be declared explicitly with producer/provenance/reason
        }
    }

    @Test
    public void negativeOrdinalIsRejected() {
        try {
            actionFactsWithOrdinal(-1);
            fail("negative ordinal must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    private static ActionFacts actionFactsWithOrdinal(int ordinal) {
        return ActionFacts.builder()
                .ordinal(ordinal)
                .resolvedAction(FactValue.unknown(PRODUCER, "k", "n/a"))
                .resolvedCard(FactValue.unknown(PRODUCER, "k", "n/a"))
                .resolvedSource(FactValue.unknown(PRODUCER, "k", "n/a"))
                .resolvedDestination(FactValue.unknown(PRODUCER, "k", "n/a"))
                .cost(FactValue.unknown(PRODUCER, "k", "n/a"))
                .power(FactValue.unknown(PRODUCER, "k", "n/a"))
                .ability(FactValue.unknown(PRODUCER, "k", "n/a"))
                .friendlyPresenceCount(FactValue.unknown(PRODUCER, "k", "n/a"))
                .opposingPresenceCount(FactValue.unknown(PRODUCER, "k", "n/a"))
                .icons(FactValue.unknown(PRODUCER, "k", "n/a"))
                .weaponBonus(FactValue.unknown(PRODUCER, "k", "n/a"))
                .build();
    }
}
