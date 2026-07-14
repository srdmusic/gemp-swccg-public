package com.gempukku.swccgo.ai.models.common.decision;

import com.gempukku.swccgo.ai.models.common.objective.ObjectiveFacts;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * response constraints, exact measurement names, nonblank metadata, range checks)
 * and Handoffs/CODEX_B2_TYPE_HARDENING_GATE_FA0F254AC_2026-07-13.md items 1-4
 * (explicit builder turn, route inside the checked evidence, obligation/source-fact
 * cross-validation, candidate shape tied to the snapshot rows).
 */
public class DecisionSnapshotTest {

    private static final String PRODUCER = "test-producer";

    private static ObjectiveFacts objectiveFacts() {
        return ObjectiveFacts.unknown("decision snapshot fixture has no objective");
    }

    private static FactValue<Set<DecisionFacts.ObligationFlag>> knownObligations() {
        return FactValue.known(Set.of(DecisionFacts.ObligationFlag.NO_PASS),
                PRODUCER, "decision.params[noPass,min]");
    }

    /** Evidence whose CandidateShape claims exactly {@code candidateCount} action AND
     *  card candidates — snapshot tests pass the row count they actually build
     *  (B2 type-hardening gate item 4: shape and rows must agree). */
    private static DecisionFacts.RouteSelectionEvidence evidence(int candidateCount) {
        return new DecisionFacts.RouteSelectionEvidence(
                AwaitingDecisionType.CARD_ACTION_CHOICE,
                Phase.DEPLOY,
                "Optional responses",
                knownObligations(),
                new DecisionFacts.CandidateShape(candidateCount, candidateCount),
                DecisionFacts.DecisionRoute.CARD_ACTION_CHOICE);
    }

    private static DecisionFacts.RouteSelectionEvidence evidence() {
        return evidence(3);
    }

    /** Fully-populated valid builder EXCEPT turn — the omitted-turn probe needs a
     *  builder that never touched it (B2 type-hardening gate item 1). */
    private static DecisionFacts.Builder factsBuilderWithoutTurn(int candidateCount) {
        return DecisionFacts.builder()
                .decisionId("42")
                .decisionType(AwaitingDecisionType.CARD_ACTION_CHOICE)
                .decisionText("Optional responses")
                .phase(Phase.DEPLOY)
                .window("Optional responses")
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
                .routeSelectionEvidence(evidence(candidateCount));
    }

    /** Fully-populated valid builder; individual tests override one field to probe validation. */
    private static DecisionFacts.Builder factsBuilder(int candidateCount) {
        return factsBuilderWithoutTurn(candidateCount).turn(3);
    }

    private static DecisionFacts.Builder factsBuilder() {
        return factsBuilder(3);
    }

    private static DecisionFacts decisionFacts(int candidateCount) {
        return factsBuilder(candidateCount).build();
    }

    private static DecisionFacts decisionFacts() {
        return decisionFacts(3);
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

    /** Minimal verbatim raw-decision component (trace-V2 gate P0-1) consistent with
     *  the {@code actionCount} rows the snapshot helpers build. */
    private static DecisionSnapshot.RawDecision rawDecision(int actionCount) {
        Map<String, List<String>> params = new LinkedHashMap<>();
        List<String> ids = new ArrayList<>();
        List<String> cardIds = new ArrayList<>();
        for (int i = 0; i < actionCount; i++) {
            ids.add("action-" + i);
            cardIds.add("card-" + i);
        }
        params.put("actionId", ids);
        params.put("cardId", cardIds);
        return new DecisionSnapshot.RawDecision(
                DecisionSnapshot.RawDecision.Source.ENGINE_PARAMETERS, params);
    }

    private static DecisionSnapshot snapshot(int actionCount) {
        List<ActionFacts> actions = new ArrayList<>();
        for (int i = 0; i < actionCount; i++) {
            actions.add(actionFacts(i));
        }
        // B2 type-hardening gate item 4: the evidence's CandidateShape now states the
        // ACTUAL row count (this helper used to claim 3/3 regardless — inconsistent
        // evidence the snapshot now rejects).
        return new DecisionSnapshot(
                decisionFacts(actionCount),
                actions,
                new DecisionSnapshot.ServiceFacts(FactValue.known(3, PRODUCER, "ForceReserveService.maintenanceObligation")),
                objectiveFacts(),
                rawDecision(actionCount),
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
        // Version 4 adds the immutable ObjectiveFacts view shared by every consumer.
        assertEquals(4, snap.snapshotVersion());
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
                        unknown, new DecisionFacts.CandidateShape(3, 3),
                        DecisionFacts.DecisionRoute.CARD_ACTION_CHOICE))
                .build();
        assertTrue(facts.obligationFlags().isUnknown());
        assertEquals("decision carried no parameter block", facts.obligationFlags().unknownReason());
    }

    // ── gate item 3: response constraints are UNKNOWN-capable, never fabricated defaults ──

    @Test
    public void responseConstraintsPreserveKnownFalseAndKnownZero() {
        // Known noPass=false requires a flag set WITHOUT NO_PASS (B2 type-hardening
        // gate item 3 — the old fixture paired noPass=false with a NO_PASS flag,
        // a contradiction the record now rejects).
        FactValue<Set<DecisionFacts.ObligationFlag>> noObligations =
                FactValue.known(Set.of(), PRODUCER, "decision.params[noPass,min]");
        DecisionFacts facts = factsBuilder()
                .obligationFlags(noObligations)
                .routeSelectionEvidence(new DecisionFacts.RouteSelectionEvidence(
                        AwaitingDecisionType.CARD_ACTION_CHOICE, Phase.DEPLOY, "Optional responses",
                        noObligations, new DecisionFacts.CandidateShape(3, 3),
                        DecisionFacts.DecisionRoute.CARD_ACTION_CHOICE))
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

    // ── B2 type-hardening gate item 3: derived obligation flags may not contradict
    //    their KNOWN source facts; UNKNOWN on either side imposes no constraint ──

    @Test
    public void noPassObligationContradictionIsRejected() {
        // factsBuilder carries flags {NO_PASS}; known noPass=false contradicts it.
        try {
            factsBuilder()
                    .noPass(FactValue.known(false, PRODUCER, "decision.params[noPass]"))
                    .build();
            fail("NO_PASS flag with known noPass=false must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        // The mirror contradiction: known noPass=true with a flag set omitting NO_PASS.
        FactValue<Set<DecisionFacts.ObligationFlag>> noFlags =
                FactValue.known(Set.of(), PRODUCER, "decision.params[noPass,min]");
        try {
            factsBuilder()
                    .obligationFlags(noFlags)
                    .routeSelectionEvidence(new DecisionFacts.RouteSelectionEvidence(
                            AwaitingDecisionType.CARD_ACTION_CHOICE, Phase.DEPLOY, "Optional responses",
                            noFlags, new DecisionFacts.CandidateShape(3, 3),
                            DecisionFacts.DecisionRoute.CARD_ACTION_CHOICE))
                    .build(); // noPass stays known(true) from factsBuilder
            fail("known noPass=true without the NO_PASS flag must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void mandatorySelectionObligationContradictionIsRejected() {
        // MANDATORY_SELECTION flagged while the source fact says minimum=0.
        FactValue<Set<DecisionFacts.ObligationFlag>> mandatory = FactValue.known(
                Set.of(DecisionFacts.ObligationFlag.NO_PASS,
                        DecisionFacts.ObligationFlag.MANDATORY_SELECTION),
                PRODUCER, "decision.params[noPass,min]");
        try {
            factsBuilder()
                    .obligationFlags(mandatory)
                    .routeSelectionEvidence(new DecisionFacts.RouteSelectionEvidence(
                            AwaitingDecisionType.CARD_ACTION_CHOICE, Phase.DEPLOY, "Optional responses",
                            mandatory, new DecisionFacts.CandidateShape(3, 3),
                            DecisionFacts.DecisionRoute.CARD_ACTION_CHOICE))
                    .build(); // minimum stays known(0) from factsBuilder
            fail("MANDATORY_SELECTION with known minimum=0 must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        // The mirror contradiction: known minimum>0 without the flag
        // (maximum raised too so the range check cannot fire first).
        try {
            factsBuilder()
                    .minimum(FactValue.known(2, PRODUCER, "decision.params[min]"))
                    .maximum(FactValue.known(3, PRODUCER, "decision.params[max]"))
                    .build(); // flags stay {NO_PASS} only from factsBuilder
            fail("known minimum=2 without MANDATORY_SELECTION must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void partiallyUnknownObligationInputsImposeNoConstraint() {
        // KNOWN flags + UNKNOWN source facts: lawful, no cross-check possible.
        DecisionFacts knownFlagsUnknownSources = factsBuilder()
                .noPass(FactValue.unknown(PRODUCER, "decision.params[noPass]", "parameter absent"))
                .minimum(FactValue.unknown(PRODUCER, "decision.params[min]", "parameter absent"))
                .build();
        assertTrue(knownFlagsUnknownSources.obligationFlags().isKnown());
        assertTrue(knownFlagsUnknownSources.noPass().isUnknown());

        // UNKNOWN flags + KNOWN source facts: also lawful (this is the trace shadow
        // builder's shape when only part of the derivation input arrived).
        FactValue<Set<DecisionFacts.ObligationFlag>> unknownFlags =
                FactValue.unknown(PRODUCER, "decision.params[noPass,min]", "derivation input absent");
        DecisionFacts unknownFlagsKnownSources = factsBuilder()
                .obligationFlags(unknownFlags)
                .routeSelectionEvidence(new DecisionFacts.RouteSelectionEvidence(
                        AwaitingDecisionType.CARD_ACTION_CHOICE, Phase.DEPLOY, "Optional responses",
                        unknownFlags, new DecisionFacts.CandidateShape(3, 3),
                        DecisionFacts.DecisionRoute.CARD_ACTION_CHOICE))
                .build(); // noPass known(true), minimum known(0) from factsBuilder
        assertTrue(unknownFlagsKnownSources.obligationFlags().isUnknown());
        assertTrue(unknownFlagsKnownSources.noPass().isKnown());
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
                    knownObligations(), new DecisionFacts.CandidateShape(3, 3),
                    DecisionFacts.DecisionRoute.CARD_ACTION_CHOICE)).build();
            fail("evidence with a different decisionType must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            factsBuilder().routeSelectionEvidence(new DecisionFacts.RouteSelectionEvidence(
                    AwaitingDecisionType.CARD_ACTION_CHOICE, Phase.BATTLE, "Optional responses",
                    knownObligations(), new DecisionFacts.CandidateShape(3, 3),
                    DecisionFacts.DecisionRoute.CARD_ACTION_CHOICE)).build();
            fail("evidence with a different phase must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            factsBuilder().routeSelectionEvidence(new DecisionFacts.RouteSelectionEvidence(
                    AwaitingDecisionType.CARD_ACTION_CHOICE, Phase.DEPLOY, "Optional responses",
                    FactValue.unknown(PRODUCER, "decision.params", "params absent"),
                    new DecisionFacts.CandidateShape(3, 3),
                    DecisionFacts.DecisionRoute.CARD_ACTION_CHOICE)).build();
            fail("evidence with different obligations must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    /** B2 type-hardening gate item 2: the route is PART of the evidence, and a
     *  route/evidence disagreement is a construction error. A CARD_ACTION_CHOICE
     *  decision claiming DecisionRoute.INTEGER no longer passes. */
    @Test
    public void routeEvidenceMustCarryTheMatchingSelectedRoute() {
        try {
            factsBuilder().routeSelectionEvidence(new DecisionFacts.RouteSelectionEvidence(
                    AwaitingDecisionType.CARD_ACTION_CHOICE, Phase.DEPLOY, "Optional responses",
                    knownObligations(), new DecisionFacts.CandidateShape(3, 3),
                    DecisionFacts.DecisionRoute.INTEGER)).build();
            fail("evidence whose route disagrees with selectedRoute must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            new DecisionFacts.RouteSelectionEvidence(
                    AwaitingDecisionType.CARD_ACTION_CHOICE, Phase.DEPLOY, "Optional responses",
                    knownObligations(), new DecisionFacts.CandidateShape(3, 3), null);
            fail("evidence without a route must be rejected");
        } catch (NullPointerException expected) {
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
        assertTrue(text.contains("route=CARD_ACTION_CHOICE"));
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
            new DecisionSnapshot(decisionFacts(2), reversed,
                    new DecisionSnapshot.ServiceFacts(FactValue.known(0, PRODUCER, "k")),
                    objectiveFacts(),
                    rawDecision(2), DecisionSnapshot.CURRENT_VERSION);
            fail("snapshot must reject action facts not ordered by original ordinal");
        } catch (IllegalArgumentException expected) {
            // expected: candidate order is never sorted or rebuilt
        }
    }

    // ── B2 type-hardening gate item 4: evidence CandidateShape tied to the actual rows ──

    @Test
    public void candidateShapeInconsistentWithActionRowsIsRejected() {
        // Evidence claims 3 action + 3 card candidates; the snapshot carries 1 row.
        // (This was the old snapshot(1) helper's inconsistency, now a hard error.)
        try {
            new DecisionSnapshot(decisionFacts(3), List.of(actionFacts(0)),
                    new DecisionSnapshot.ServiceFacts(FactValue.known(0, PRODUCER, "k")),
                    objectiveFacts(),
                    rawDecision(3), DecisionSnapshot.CURRENT_VERSION);
            fail("evidence claiming more candidates than the snapshot has rows must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        // A row carrying a raw actionId/cardId at an ordinal beyond the claimed count.
        try {
            new DecisionSnapshot(decisionFacts(2),
                    List.of(actionFacts(0), actionFacts(1), actionFacts(2)),
                    new DecisionSnapshot.ServiceFacts(FactValue.known(0, PRODUCER, "k")),
                    objectiveFacts(),
                    rawDecision(2), DecisionSnapshot.CURRENT_VERSION);
            fail("a row with a raw id beyond the claimed candidate count must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        // Rows MAY exceed the claimed counts when the extra rows carry no raw ids:
        // mismatched parallel raw arrays are retained (ghost rows), never padded.
        DecisionSnapshot ghostRow = new DecisionSnapshot(decisionFacts(2),
                List.of(actionFacts(0), actionFacts(1), allUnknownAction(2)),
                new DecisionSnapshot.ServiceFacts(FactValue.known(0, PRODUCER, "k")),
                objectiveFacts(),
                rawDecision(2), DecisionSnapshot.CURRENT_VERSION);
        assertEquals(3, ghostRow.actionFacts().size());
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
        DecisionSnapshot snap = new DecisionSnapshot(decisionFacts(1), source,
                new DecisionSnapshot.ServiceFacts(FactValue.known(0, PRODUCER, "k")),
                objectiveFacts(),
                rawDecision(1), DecisionSnapshot.CURRENT_VERSION);
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
                        flags, new DecisionFacts.CandidateShape(3, 3),
                        DecisionFacts.DecisionRoute.CARD_ACTION_CHOICE))
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
                    knownObligations(), new DecisionFacts.CandidateShape(3, 3),
                    DecisionFacts.DecisionRoute.CARD_ACTION_CHOICE);
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

    /** B2 type-hardening gate item 1: a builder that never set turn must FAIL
     *  construction — a primitive default would fabricate turn 0, a KNOWN
     *  pre-game turn the engine never reported. */
    @Test
    public void omittedTurnFailsConstructionInsteadOfFabricatingPregameZero() {
        try {
            factsBuilderWithoutTurn(3).build();
            fail("unset builder turn must be rejected, not defaulted to 0");
        } catch (NullPointerException expected) {
            // expected: turn is an engine fact — no silent default
        }
        // turn 0 stays valid when SUPPLIED explicitly (pre-game setup decisions).
        assertEquals(0, factsBuilderWithoutTurn(3).turn(0).build().turn());
    }

    @Test
    public void nonPositiveSnapshotVersionIsRejected() {
        for (int version : new int[]{0, -1}) {
            try {
                new DecisionSnapshot(decisionFacts(1), List.of(actionFacts(0)),
                        new DecisionSnapshot.ServiceFacts(FactValue.known(0, PRODUCER, "k")),
                        objectiveFacts(),
                        rawDecision(1), version);
                fail("snapshotVersion " + version + " must be rejected");
            } catch (IllegalArgumentException expected) {
                // expected
            }
        }
    }

    // ── trace-V2 gate P0-1 (CODEX_TRACE_V2_GATE_97D2CB65A_2026-07-13.md): the verbatim
    //    RawDecision component — presence, present-empty vs absent, blanks preserved ──

    @Test
    public void rawDecisionPreservesPresenceEmptinessAndBlanksVerbatim() {
        Map<String, List<String>> params = new LinkedHashMap<>();
        params.put("actionId", Arrays.asList("5", "", "7"));      // blank id stays blank
        params.put("results", Arrays.asList("Yes", "No"));        // results kept as their OWN array
        params.put("preselected", List.of());                     // PRESENT-EMPTY array
        params.put("noPass", List.of("true"));                    // scalar presence
        DecisionSnapshot.RawDecision raw = new DecisionSnapshot.RawDecision(
                DecisionSnapshot.RawDecision.Source.ENGINE_PARAMETERS, params);

        assertEquals(DecisionSnapshot.RawDecision.Source.ENGINE_PARAMETERS, raw.source());
        // verbatim values, including the blank entry
        assertEquals(Arrays.asList("5", "", "7"), raw.values("actionId"));
        assertEquals(Arrays.asList("Yes", "No"), raw.values("results"));
        // present-empty is fully distinct from absent
        assertTrue(raw.has("preselected"));
        assertEquals(List.of(), raw.values("preselected"));
        assertFalse("absent key must not read as present-empty", raw.has("defaultIndex"));
        assertEquals(null, raw.values("defaultIndex"));
        // scalar presence
        assertTrue(raw.has("noPass"));
        assertEquals(List.of("true"), raw.values("noPass"));
    }

    @Test
    public void rawDecisionIsDeeplyImmutableAndDetached() {
        Map<String, List<String>> params = new LinkedHashMap<>();
        List<String> ids = new ArrayList<>(Arrays.asList("5", "7"));
        params.put("actionId", ids);
        DecisionSnapshot.RawDecision raw = new DecisionSnapshot.RawDecision(
                DecisionSnapshot.RawDecision.Source.ENGINE_PARAMETERS, params);

        // mutate the caller-owned sources AFTER construction — the record must not move
        ids.add("tampered");
        params.put("cardId", List.of("temp1"));
        assertEquals(Arrays.asList("5", "7"), raw.values("actionId"));
        assertFalse(raw.has("cardId"));

        // and the exposed collections are unmodifiable
        try {
            raw.parameters().put("x", List.of());
            fail("parameters map must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // required
        }
        try {
            raw.values("actionId").add("Z");
            fail("value lists must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // required
        }
    }

    @Test
    public void rawDecisionRejectsMalformedConstruction() {
        try {
            new DecisionSnapshot.RawDecision(null,
                    Map.of("actionId", List.of("5")));
            fail("null source must be rejected");
        } catch (NullPointerException expected) {
            // required
        }
        try {
            new DecisionSnapshot.RawDecision(
                    DecisionSnapshot.RawDecision.Source.ENGINE_PARAMETERS, null);
            fail("null parameter map must be rejected");
        } catch (NullPointerException expected) {
            // required
        }
        Map<String, List<String>> blankKey = new LinkedHashMap<>();
        blankKey.put("  ", List.of("x"));
        try {
            new DecisionSnapshot.RawDecision(
                    DecisionSnapshot.RawDecision.Source.ENGINE_PARAMETERS, blankKey);
            fail("blank parameter key must be rejected");
        } catch (IllegalArgumentException expected) {
            // required
        }
    }

    @Test
    public void snapshotRequiresARawDecision() {
        try {
            new DecisionSnapshot(decisionFacts(1), List.of(actionFacts(0)),
                    new DecisionSnapshot.ServiceFacts(FactValue.known(0, PRODUCER, "k")),
                    objectiveFacts(),
                    null, DecisionSnapshot.CURRENT_VERSION);
            fail("snapshot without the raw-decision component must be rejected");
        } catch (NullPointerException expected) {
            // required: normalized rows never REPLACE the raw evidence
        }
    }
}
