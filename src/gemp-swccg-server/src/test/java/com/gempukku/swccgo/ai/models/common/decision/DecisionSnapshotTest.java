package com.gempukku.swccgo.ai.models.common.decision;

import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Pure construction tests for {@link DecisionFacts}, {@link ActionFacts} and
 * {@link DecisionSnapshot} — Batch-2 typed-facts increment 1, no production consumer.
 * Contract: Handoffs/CODEX_RANDO_FACTS_ASSESSMENTS_CONTRACT_2026-07-13.md
 * ("Minimal shared model": immutable, ordered by original ordinal, versioned).
 * Gate deltas covered: Handoffs/CODEX_B2_INCREMENT1_GATE_E4E0AA213_2026-07-13.md
 * (engine decision-type enum, typed obligations/route/refs, UNKNOWN-capable
 * response constraints, exact measurement names, nonblank metadata, range checks).
 */
public class DecisionSnapshotTest {

    private static final String PRODUCER = "test-producer";

    private static FactValue<Set<DecisionFacts.ObligationFlag>> knownObligations() {
        return FactValue.known(Set.of(DecisionFacts.ObligationFlag.NO_PASS),
                PRODUCER, "decision.params[noPass,min]");
    }

    private static DecisionFacts.RouteSelectionEvidence evidence() {
        return new DecisionFacts.RouteSelectionEvidence(
                AwaitingDecisionType.CARD_ACTION_CHOICE,
                Phase.DEPLOY,
                "Optional responses",
                knownObligations(),
                new DecisionFacts.CandidateShape(3, 3));
    }

    /** Fully-populated valid builder; individual tests override one field to probe validation. */
    private static DecisionFacts.Builder factsBuilder() {
        return DecisionFacts.builder()
                .decisionId("42")
                .decisionType(AwaitingDecisionType.CARD_ACTION_CHOICE)
                .decisionText("Optional responses")
                .phase(Phase.DEPLOY)
                .window("Optional responses")
                .turn(3)
                .currentPlayer("asdf")
                .side(Side.DARK)
                .obligationFlags(knownObligations())
                .noPass(FactValue.known(true, PRODUCER, "decision.params[noPass]"))
                .minimum(FactValue.known(0, PRODUCER, "decision.params[min]"))
                .maximum(FactValue.known(1, PRODUCER, "decision.params[max]"))
                .blockedResponses(Set.of("17"))
                .forcePileSize(FactValue.known(6, PRODUCER, "GameState.forcePile.size"))
                .lifeForceCardCount(FactValue.known(31, PRODUCER, "GameState.getPlayerLifeForce"))
                .handSize(FactValue.known(8, PRODUCER, "GameState.hand.size"))
                .reserveDeckSize(FactValue.known(20, PRODUCER, "GameState.getReserveDeckSize"))
                .objectiveIdentity(FactValue.known("200_20", PRODUCER, "objective.blueprintId"))
                .objectiveFlipped(FactValue.known(false, PRODUCER, "objective.flipState"))
                .selectedRoute(DecisionFacts.DecisionRoute.CARD_ACTION_CHOICE)
                .routeSelectionEvidence(evidence());
    }

    private static DecisionFacts decisionFacts() {
        return factsBuilder().build();
    }

    private static ActionFacts actionFacts(int ordinal) {
        return ActionFacts.builder()
                .ordinal(ordinal)
                .actionId("action-" + ordinal)
                .actionText("Deploy Probe Droid")
                .cardId("card-" + ordinal)
                .blueprintId("7_178")
                .testingText("Probe Droid")
                .selectable(FactValue.known(true, PRODUCER, "decision.params[selectable][" + ordinal + "]"))
                .resolvedAction(FactValue.known(new ActionFacts.ActionRef("action-" + ordinal),
                        PRODUCER, "decision.actionIds[" + ordinal + "]"))
                .resolvedCard(FactValue.known(new ActionFacts.CardRef(100 + ordinal),
                        PRODUCER, "GameState.findCardById"))
                .resolvedSource(FactValue.known(new ActionFacts.SourceRef.FromZone(Zone.HAND),
                        PRODUCER, "PhysicalCard.getZone"))
                .resolvedDestination(FactValue.unknown(PRODUCER, "action.destination",
                        "destination prompt not yet opened"))
                .cost(FactValue.known(2.0f, PRODUCER, "blueprint.deployCost"))
                .basePower(FactValue.known(3.0f, PRODUCER, "blueprint.power"))
                .ability(FactValue.known(1.0f, PRODUCER, "blueprint.ability"))
                .friendlyNonUndercoverCharacterCount(FactValue.known(0,
                        PRODUCER, "FormationSafety.countFriendlyNonUndercoverCharacters"))
                .opposingNonUndercoverCharacterCount(FactValue.known(2,
                        PRODUCER, "FormationSafety.countFriendlyNonUndercoverCharacters(opponent)"))
                .icons(FactValue.known(Set.of(Icon.DROID, Icon.PRESENCE), PRODUCER, "blueprint.icons"))
                .weaponBonus(FactValue.known(0.0f, PRODUCER, "FormationSafety.weaponBonusOf"))
                .build();
    }

    /** ActionFacts with every FactValue explicitly UNKNOWN (each with a real reason). */
    private static ActionFacts allUnknownAction(int ordinal) {
        return ActionFacts.builder()
                .ordinal(ordinal)
                .selectable(FactValue.unknown(PRODUCER, "k", "selectable array absent"))
                .resolvedAction(FactValue.unknown(PRODUCER, "k", "n/a"))
                .resolvedCard(FactValue.unknown(PRODUCER, "k", "n/a"))
                .resolvedSource(FactValue.unknown(PRODUCER, "k", "n/a"))
                .resolvedDestination(FactValue.unknown(PRODUCER, "k", "n/a"))
                .cost(FactValue.unknown(PRODUCER, "k", "n/a"))
                .basePower(FactValue.unknown(PRODUCER, "k", "n/a"))
                .ability(FactValue.unknown(PRODUCER, "k", "n/a"))
                .friendlyNonUndercoverCharacterCount(FactValue.unknown(PRODUCER, "k", "n/a"))
                .opposingNonUndercoverCharacterCount(FactValue.unknown(PRODUCER, "k", "n/a"))
                .icons(FactValue.unknown(PRODUCER, "k", "n/a"))
                .weaponBonus(FactValue.unknown(PRODUCER, "k", "n/a"))
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
        assertEquals(AwaitingDecisionType.CARD_ACTION_CHOICE, snap.decisionFacts().decisionType());
        assertEquals(Phase.DEPLOY, snap.decisionFacts().phase());
        assertEquals(3, snap.decisionFacts().turn());
        assertEquals("asdf", snap.decisionFacts().currentPlayer());
        assertEquals(Side.DARK, snap.decisionFacts().side());
        assertEquals(DecisionFacts.DecisionRoute.CARD_ACTION_CHOICE, snap.decisionFacts().selectedRoute());
        assertEquals(3, snap.actionFacts().size());
        assertEquals("Deploy Probe Droid", snap.actionFacts().get(1).actionText());
        assertEquals(Integer.valueOf(3), snap.serviceFacts().forceObligations().value());
    }

    @Test
    public void snapshotVersionIsPresent() {
        DecisionSnapshot snap = snapshot(1);
        assertEquals(DecisionSnapshot.CURRENT_VERSION, snap.snapshotVersion());
        assertEquals(2, snap.snapshotVersion());
    }

    @Test
    public void equalSnapshotsFromEqualInputsAreEqual() {
        // NOTE (B2 gate boundary, CODEX_B2_INCREMENT1_GATE_E4E0AA213_2026-07-13.md):
        // this test proves RECORD EQUALITY ONLY. It is NOT the Rando/ChosenOne
        // builder parity gate. That gate stays open until increment 2's shadow
        // builders independently build the SAME frozen raw decision through both
        // bot entry paths and produce byte-identical serialized snapshots with
        // unchanged candidate order. Do not cite this test as parity evidence.
        assertEquals(snapshot(2), snapshot(2));
        assertEquals(snapshot(2).hashCode(), snapshot(2).hashCode());
    }

    // ── gate item 1: real engine/typed enums, not strings ──

    @Test
    public void decisionTypeIsTheEngineEnum() {
        AwaitingDecisionType type = decisionFacts().decisionType();
        assertEquals(AwaitingDecisionType.CARD_ACTION_CHOICE, type);
        // The component type is the engine's own enum; a free string cannot get in.
        assertEquals("CARD_ACTION_CHOICE", type.name());
    }

    @Test
    public void selectedRouteIsATypedEnum() {
        assertEquals(DecisionFacts.DecisionRoute.CARD_ACTION_CHOICE, decisionFacts().selectedRoute());
    }

    @Test
    public void obligationFlagsAreTypedAndUnknownCapable() {
        // Known set of typed flags.
        DecisionFacts known = decisionFacts();
        assertTrue(known.obligationFlags().isKnown());
        assertTrue(known.obligationFlags().value().contains(DecisionFacts.ObligationFlag.NO_PASS));

        // Absent decision parameters = UNKNOWN obligations with a reason. This is
        // NOT the same fact as a known-empty obligation set.
        FactValue<Set<DecisionFacts.ObligationFlag>> unknown =
                FactValue.unknown(PRODUCER, "decision.params", "decision carried no parameter block");
        FactValue<Set<DecisionFacts.ObligationFlag>> knownEmpty =
                FactValue.known(Set.of(), PRODUCER, "decision.params");
        assertNotEquals(unknown, knownEmpty);
        DecisionFacts facts = factsBuilder()
                .obligationFlags(unknown)
                .routeSelectionEvidence(new DecisionFacts.RouteSelectionEvidence(
                        AwaitingDecisionType.CARD_ACTION_CHOICE, Phase.DEPLOY, "Optional responses",
                        unknown, new DecisionFacts.CandidateShape(3, 3)))
                .build();
        assertTrue(facts.obligationFlags().isUnknown());
        assertEquals("decision carried no parameter block", facts.obligationFlags().unknownReason());
    }

    // ── gate item 3: response constraints are UNKNOWN-capable, never fabricated defaults ──

    @Test
    public void responseConstraintsPreserveKnownFalseAndKnownZero() {
        DecisionFacts facts = factsBuilder()
                .noPass(FactValue.known(false, PRODUCER, "decision.params[noPass]"))
                .minimum(FactValue.known(0, PRODUCER, "decision.params[min]"))
                .maximum(FactValue.known(0, PRODUCER, "decision.params[max]"))
                .build();
        assertTrue(facts.noPass().isKnown());
        assertEquals(Boolean.FALSE, facts.noPass().value());
        assertEquals(Integer.valueOf(0), facts.minimum().value());
        assertEquals(Integer.valueOf(0), facts.maximum().value());
    }

    @Test
    public void absentResponseConstraintsAreUnknownWithReasonNotDefaults() {
        DecisionFacts facts = factsBuilder()
                .noPass(FactValue.unknown(PRODUCER, "decision.params[noPass]", "parameter absent"))
                .minimum(FactValue.unknown(PRODUCER, "decision.params[min]", "parameter absent"))
                .maximum(FactValue.unknown(PRODUCER, "decision.params[max]", "parameter absent"))
                .build();
        assertTrue(facts.noPass().isUnknown());
        assertTrue(facts.minimum().isUnknown());
        assertTrue(facts.maximum().isUnknown());
        assertEquals("parameter absent", facts.noPass().unknownReason());
        // value() on the absent constraint throws instead of yielding a fabricated default.
        try {
            facts.minimum().value();
            fail("absent minimum must not yield a value");
        } catch (IllegalStateException expected) {
            // expected
        }
    }

    @Test
    public void unsetResponseConstraintFactsAreRejectedAtBuild() {
        // The builder has NO default for noPass/minimum/maximum: forgetting them
        // fails construction instead of fabricating noPass=false/min=0/max=0.
        try {
            factsBuilder().noPass(null).build();
            fail("unset noPass fact must be rejected");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void malformedResponseRangesAreRejected() {
        try {
            factsBuilder()
                    .minimum(FactValue.known(2, PRODUCER, "decision.params[min]"))
                    .maximum(FactValue.known(1, PRODUCER, "decision.params[max]"))
                    .build();
            fail("maximum < minimum must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            factsBuilder().minimum(FactValue.known(-1, PRODUCER, "decision.params[min]")).build();
            fail("negative known minimum must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            factsBuilder().maximum(FactValue.known(-2, PRODUCER, "decision.params[max]")).build();
            fail("negative known maximum must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void selectableIsUnknownCapableNotDefaultedFalse() {
        ActionFacts action = allUnknownAction(0);
        assertTrue(action.selectable().isUnknown());
        assertEquals("selectable array absent", action.selectable().unknownReason());
        // Known false stays a first-class, distinct fact.
        assertNotEquals(action.selectable(),
                FactValue.known(false, PRODUCER, "k"));
    }

    // ── gate item 2: structured route evidence, machine-checkable ──

    @Test
    public void routeEvidenceMustMatchTheDecisionsOwnFields() {
        try {
            factsBuilder().routeSelectionEvidence(new DecisionFacts.RouteSelectionEvidence(
                    AwaitingDecisionType.MULTIPLE_CHOICE, Phase.DEPLOY, "Optional responses",
                    knownObligations(), new DecisionFacts.CandidateShape(3, 3))).build();
            fail("evidence with a different decisionType must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            factsBuilder().routeSelectionEvidence(new DecisionFacts.RouteSelectionEvidence(
                    AwaitingDecisionType.CARD_ACTION_CHOICE, Phase.BATTLE, "Optional responses",
                    knownObligations(), new DecisionFacts.CandidateShape(3, 3))).build();
            fail("evidence with a different phase must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            factsBuilder().routeSelectionEvidence(new DecisionFacts.RouteSelectionEvidence(
                    AwaitingDecisionType.CARD_ACTION_CHOICE, Phase.DEPLOY, "Optional responses",
                    FactValue.unknown(PRODUCER, "decision.params", "params absent"),
                    new DecisionFacts.CandidateShape(3, 3))).build();
            fail("evidence with different obligations must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void routeEvidenceDerivesHumanReadableTextFromTypedFields() {
        String text = evidence().describe();
        assertTrue(text.contains("decisionType=CARD_ACTION_CHOICE"));
        assertTrue(text.contains("phase=" + Phase.DEPLOY));
        assertTrue(text.contains("window=Optional responses"));
        assertTrue(text.contains("NO_PASS"));
        assertTrue(text.contains("actions=3"));
        assertTrue(text.contains("cards=3"));
    }

    @Test
    public void negativeCandidateShapeCountsAreRejected() {
        try {
            new DecisionFacts.CandidateShape(-1, 0);
            fail("negative action candidate count must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            new DecisionFacts.CandidateShape(0, -1);
            fail("negative card candidate count must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    // ── gate item 1: distinct typed references, not four interchangeable strings ──

    @Test
    public void refTypesAreDistinctAndValidateTheirIdForm() {
        // Compile-time guarantee: FactValue<ActionRef> is not assignable to
        // FactValue<CardRef> (and so on) — that cannot be demonstrated in a
        // running test, so we assert the value-level distinctness here.
        ActionFacts.ActionRef action = new ActionFacts.ActionRef("5");
        ActionFacts.CardRef card = new ActionFacts.CardRef(5);
        ActionFacts.SourceRef source = new ActionFacts.SourceRef.FromCard(card);
        ActionFacts.DestinationRef destination = new ActionFacts.DestinationRef.ToCard(card);
        assertNotEquals((Object) action, (Object) card);
        assertNotEquals((Object) source, (Object) destination);
        assertNotEquals((Object) new ActionFacts.SourceRef.FromZone(Zone.HAND), (Object) source);

        try {
            new ActionFacts.ActionRef("  ");
            fail("blank action id must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            new ActionFacts.CardRef(0);
            fail("non-positive card id must be rejected (engine card ids start at 1)");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            new ActionFacts.SourceRef.FromZone(null);
            fail("null zone must be rejected");
        } catch (NullPointerException expected) {
            // expected
        }
        try {
            new ActionFacts.DestinationRef.ToCard(null);
            fail("null card must be rejected");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    // ── gate item 5: power components stay separate and UNKNOWN-capable ──

    @Test
    public void failedPowerComponentResolutionStaysUnknownNeverZero() {
        ActionFacts action = allUnknownAction(0);
        assertTrue(action.basePower().isUnknown());
        assertTrue(action.weaponBonus().isUnknown());
        // The component must not silently read as 0: value() throws.
        try {
            action.weaponBonus().value();
            fail("UNKNOWN weaponBonus must never be read as a known zero");
        } catch (IllegalStateException expected) {
            // expected
        }
        assertFalse(FactValue.known(0.0f, PRODUCER, "k").equals(action.weaponBonus()));
    }

    // ── unknown handling flows through the composite types ──

    @Test
    public void unknownFactInsideActionPreservesProducerProvenanceAndReason() {
        ActionFacts action = snapshot(1).actionFacts().get(0);
        FactValue<ActionFacts.DestinationRef> dest = action.resolvedDestination();
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
        Set<DecisionFacts.ObligationFlag> mutableFlags = new HashSet<>();
        mutableFlags.add(DecisionFacts.ObligationFlag.NO_PASS);
        FactValue<Set<DecisionFacts.ObligationFlag>> flags =
                FactValue.known(mutableFlags, PRODUCER, "decision.params");
        DecisionFacts facts = factsBuilder()
                .blockedResponses(blocked)
                .obligationFlags(flags)
                .routeSelectionEvidence(new DecisionFacts.RouteSelectionEvidence(
                        AwaitingDecisionType.CARD_ACTION_CHOICE, Phase.DEPLOY, "Optional responses",
                        flags, new DecisionFacts.CandidateShape(3, 3)))
                .build();
        try {
            facts.blockedResponses().add("18");
            fail("blockedResponses must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
        try {
            facts.obligationFlags().value().add(DecisionFacts.ObligationFlag.MANDATORY_SELECTION);
            fail("known obligation set must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
        blocked.add("19"); // mutate the sources AFTER construction
        mutableFlags.add(DecisionFacts.ObligationFlag.MANDATORY_SELECTION);
        assertEquals(1, facts.blockedResponses().size());
        assertEquals(1, facts.obligationFlags().value().size());
    }

    @Test
    public void knownIconSetIsUnmodifiableEvenWhenBuiltFromMutableSet() {
        Set<Icon> mutableIcons = new HashSet<>();
        mutableIcons.add(Icon.PILOT);
        ActionFacts action = ActionFacts.builder()
                .ordinal(0)
                .selectable(FactValue.unknown(PRODUCER, "k", "n/a"))
                .resolvedAction(FactValue.unknown(PRODUCER, "k", "n/a"))
                .resolvedCard(FactValue.unknown(PRODUCER, "k", "n/a"))
                .resolvedSource(FactValue.unknown(PRODUCER, "k", "n/a"))
                .resolvedDestination(FactValue.unknown(PRODUCER, "k", "n/a"))
                .cost(FactValue.unknown(PRODUCER, "k", "n/a"))
                .basePower(FactValue.unknown(PRODUCER, "k", "n/a"))
                .ability(FactValue.unknown(PRODUCER, "k", "n/a"))
                .friendlyNonUndercoverCharacterCount(FactValue.unknown(PRODUCER, "k", "n/a"))
                .opposingNonUndercoverCharacterCount(FactValue.unknown(PRODUCER, "k", "n/a"))
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
            allUnknownAction(-1);
            fail("negative ordinal must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    // ── gate item 6: nonblank metadata + range checks ──

    @Test
    public void blankIdentityFieldsAreRejected() {
        try {
            factsBuilder().decisionId("  ").build();
            fail("blank decisionId must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            factsBuilder().currentPlayer("").build();
            fail("blank currentPlayer must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            // window is optional (null allowed) but must be nonblank when present;
            // evidence must mirror it, so probe the evidence record directly too.
            new DecisionFacts.RouteSelectionEvidence(
                    AwaitingDecisionType.CARD_ACTION_CHOICE, Phase.DEPLOY, " ",
                    knownObligations(), new DecisionFacts.CandidateShape(3, 3));
            fail("blank window must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            factsBuilder().blockedResponses(Set.of("")).build();
            fail("blank blocked-response id must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void blankRawCandidateIdsAreRejected() {
        try {
            ActionFacts.builder().ordinal(0).actionId(" ").build();
            fail("blank raw actionId must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void negativeKnownCountsAreRejected() {
        try {
            factsBuilder().forcePileSize(FactValue.known(-1, PRODUCER, "k")).build();
            fail("negative forcePileSize must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            factsBuilder().lifeForceCardCount(FactValue.known(-3, PRODUCER, "k")).build();
            fail("negative lifeForceCardCount must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            factsBuilder().handSize(FactValue.known(-1, PRODUCER, "k")).build();
            fail("negative handSize must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            factsBuilder().reserveDeckSize(FactValue.known(-1, PRODUCER, "k")).build();
            fail("negative reserveDeckSize must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            allUnknownActionBuilderWithCounts(-1, 0);
            fail("negative friendly character count must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            allUnknownActionBuilderWithCounts(0, -2);
            fail("negative opposing character count must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            new DecisionSnapshot.ServiceFacts(FactValue.known(-1, PRODUCER, "k"));
            fail("negative forceObligations must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    private static void allUnknownActionBuilderWithCounts(int friendly, int opposing) {
        ActionFacts.builder()
                .ordinal(0)
                .selectable(FactValue.unknown(PRODUCER, "k", "n/a"))
                .resolvedAction(FactValue.unknown(PRODUCER, "k", "n/a"))
                .resolvedCard(FactValue.unknown(PRODUCER, "k", "n/a"))
                .resolvedSource(FactValue.unknown(PRODUCER, "k", "n/a"))
                .resolvedDestination(FactValue.unknown(PRODUCER, "k", "n/a"))
                .cost(FactValue.unknown(PRODUCER, "k", "n/a"))
                .basePower(FactValue.unknown(PRODUCER, "k", "n/a"))
                .ability(FactValue.unknown(PRODUCER, "k", "n/a"))
                .friendlyNonUndercoverCharacterCount(FactValue.known(friendly, PRODUCER, "k"))
                .opposingNonUndercoverCharacterCount(FactValue.known(opposing, PRODUCER, "k"))
                .icons(FactValue.unknown(PRODUCER, "k", "n/a"))
                .weaponBonus(FactValue.unknown(PRODUCER, "k", "n/a"))
                .build();
    }

    @Test
    public void negativeTurnIsRejected() {
        try {
            factsBuilder().turn(-1).build();
            fail("negative turn must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void nonPositiveSnapshotVersionIsRejected() {
        for (int version : new int[]{0, -1}) {
            try {
                new DecisionSnapshot(decisionFacts(), List.of(actionFacts(0)),
                        new DecisionSnapshot.ServiceFacts(FactValue.known(0, PRODUCER, "k")),
                        version);
                fail("snapshotVersion " + version + " must be rejected");
            } catch (IllegalArgumentException expected) {
                // expected
            }
        }
    }
}
