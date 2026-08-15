package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgCardBlueprintLibrary;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextType;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BhbmForceDripUrgencyFactsReaderTest {
    private static final String DARK = "dark";
    private static final String LIGHT = "light";
    private static final SwccgCardBlueprintLibrary CARDS =
            new SwccgCardBlueprintLibrary();

    @Test
    public void captiveVaderPairCompletesThreeRolesInOneSnapshot() {
        Fixture fixture = new Fixture();
        PhysicalCard emperor = fixture.add(
                "9_109", 10, DARK, fixture.throne);
        PhysicalCard vader = fixture.add(
                "1_168", 11, DARK, fixture.origin);
        PhysicalCard luke = fixture.add(
                "9_24", 12, LIGHT, fixture.origin);
        fixture.persona(emperor, Persona.SIDIOUS);
        fixture.persona(vader, Persona.VADER);
        fixture.persona(luke, Persona.LUKE);
        fixture.capture(vader, luke);

        CaptureObjectivePolicy.BhbmForceDripUrgencyFacts facts =
                fixture.read(
                    "move-vader", vader, vader,
                    fixture.throne,
                    BhbmForceDripUrgencyFactsReader
                        .CandidateMechanism.LANDSPEED);

        assertTrue(facts.factsKnown());
        assertTrue(facts.candidateWillBePresentAtThroneRoom());
        assertTrue(facts.stableBackCurrentlyHeld());
        assertTrue(facts.preservesStableBackState());
        assertEquals(1, facts.duelTrioPieceCountBefore());
        assertEquals(3, facts.duelTrioPieceCountAfter());
        assertEquals(300.0f, only(
                CaptureObjectivePolicy
                    .scoreBhbmForceDripUrgency(facts))
                    .delta(), 0.0f);
    }

    @Test
    public void retargetCountsRoleBooleansAndIgnoresLukeCopies() {
        Fixture fixture = new Fixture();
        PhysicalCard vader = fixture.add(
                "1_168", 20, DARK, fixture.origin);
        PhysicalCard leia = fixture.add(
                "1_17", 21, LIGHT, fixture.origin);
        PhysicalCard lukeOne = fixture.add(
                "9_24", 22, LIGHT, fixture.throne);
        PhysicalCard lukeTwo = fixture.add(
                "1_19", 23, LIGHT, fixture.throne);
        fixture.persona(vader, Persona.VADER);
        fixture.persona(leia, Persona.LEIA);
        fixture.persona(lukeOne, Persona.LUKE);
        fixture.persona(lukeTwo, Persona.LUKE);
        fixture.capture(vader, leia);
        when(fixture.modifiers.hasGameTextModification(
                fixture.gameState, fixture.objective,
                ModifyGameTextType
                    .BRING_HIM_BEFORE_ME__TARGETS_LEIA_INSTEAD_OF_LUKE))
                .thenReturn(true);
        when(fixture.modifiers.hasGameTextModification(
                fixture.gameState, fixture.objective,
                ModifyGameTextType
                    .BRING_HIM_BEFORE_ME__TARGETS_KANAN_INSTEAD_OF_LUKE))
                .thenReturn(true);

        CaptureObjectivePolicy.BhbmForceDripUrgencyFacts facts =
                fixture.read(
                    "retarget", vader, vader,
                    fixture.throne,
                    BhbmForceDripUrgencyFactsReader
                        .CandidateMechanism.LANDSPEED);

        assertEquals(0, facts.duelTrioPieceCountBefore());
        assertEquals(2, facts.duelTrioPieceCountAfter());
        assertTrue(CaptureObjectivePolicy
                .scoreBhbmForceDripUrgency(facts)
                .operations().isEmpty());
    }

    @Test
    public void lightOwnedCard10SourceCannotClaimDarkProgress() {
        Fixture fixture = new Fixture();
        PhysicalCard emperor = fixture.add(
                "9_109", 30, DARK, fixture.throne);
        PhysicalCard vader = fixture.add(
                "1_168", 31, DARK, fixture.origin);
        PhysicalCard luke = fixture.add(
                "9_24", 32, LIGHT, fixture.origin);
        PhysicalCard card10Source = fixture.add(
                "10_10", 33, LIGHT, fixture.origin);
        fixture.persona(emperor, Persona.SIDIOUS);
        fixture.persona(vader, Persona.VADER);
        fixture.persona(luke, Persona.LUKE);
        fixture.capture(vader, luke);

        CaptureObjectivePolicy.BhbmForceDripUrgencyFacts facts =
                fixture.read(
                    "card10", card10Source, vader,
                    fixture.throne,
                    BhbmForceDripUrgencyFactsReader
                        .CandidateMechanism.LANDSPEED);

        assertFalse(facts.actingPlayerOwnsActionSource());
        assertEquals(3, facts.duelTrioPieceCountAfter());
        assertTrue(CaptureObjectivePolicy
                .scoreBhbmForceDripUrgency(facts)
                .operations().isEmpty());
    }

    @Test
    public void thereIsAnotherForceLimitCancelsOtherwiseValidUrgency() {
        Fixture fixture = new Fixture();
        PhysicalCard emperor = fixture.add(
                "9_109", 40, DARK, fixture.throne);
        PhysicalCard vader = fixture.add(
                "1_168", 41, DARK, fixture.origin);
        PhysicalCard luke = fixture.add(
                "9_24", 42, LIGHT, fixture.origin);
        fixture.persona(emperor, Persona.SIDIOUS);
        fixture.persona(vader, Persona.VADER);
        fixture.persona(luke, Persona.LUKE);
        fixture.capture(vader, luke);
        when(fixture.modifiers.getForceToLoseFromCardLimit(
                fixture.gameState, DARK,
                fixture.objective)).thenReturn(0.0f);

        CaptureObjectivePolicy.BhbmForceDripUrgencyFacts facts =
                fixture.read(
                    "cancelled", vader, vader,
                    fixture.throne,
                    BhbmForceDripUrgencyFactsReader
                        .CandidateMechanism.LANDSPEED);

        assertFalse(facts.forceDripActive());
        assertTrue(CaptureObjectivePolicy
                .scoreBhbmForceDripUrgency(facts)
                .operations().isEmpty());

        when(fixture.modifiers.getForceToLoseFromCardLimit(
                fixture.gameState, DARK,
                fixture.objective)).thenReturn(2.0f);
        CaptureObjectivePolicy.BhbmForceDripUrgencyFacts limited =
                fixture.read(
                    "limited-to-two", vader, vader,
                    fixture.throne,
                    BhbmForceDripUrgencyFactsReader
                        .CandidateMechanism.LANDSPEED);
        assertTrue(limited.forceDripActive());
        assertEquals(300.0f, only(
                CaptureObjectivePolicy
                    .scoreBhbmForceDripUrgency(limited))
                    .delta(), 0.0f);
    }

    @Test
    public void deployAboardRealStarshipHullDoesNotSatisfyPresentAtThroneRoom() {
        Fixture fixture = new Fixture();
        PhysicalCard vader = fixture.add(
                "1_168", 50, DARK, fixture.throne);
        PhysicalCard luke = fixture.add(
                "9_24", 51, LIGHT, fixture.throne);
        PhysicalCard emperor = fixture.addInactive(
                "9_109", 52, DARK);
        PhysicalCard hull = fixture.add(
                "1_299", 53, DARK, fixture.throne);
        fixture.persona(vader, Persona.VADER);
        fixture.persona(luke, Persona.LUKE);
        fixture.persona(emperor, Persona.SIDIOUS);
        fixture.capture(vader, luke);
        assertEquals(
                CardCategory.STARSHIP,
                hull.getBlueprint()
                    .getCardCategory());

        CaptureObjectivePolicy.BhbmForceDripUrgencyFacts facts =
                fixture.read(
                    "deploy-emperor", emperor, emperor,
                    hull,
                    BhbmForceDripUrgencyFactsReader
                        .CandidateMechanism.DEPLOY);

        assertFalse(facts.candidateWillBePresentAtThroneRoom());
        assertEquals(2, facts.duelTrioPieceCountBefore());
        assertEquals(2, facts.duelTrioPieceCountAfter());
        assertTrue(CaptureObjectivePolicy
                .scoreBhbmForceDripUrgency(facts)
                .operations().isEmpty());
    }

    @Test
    public void unknownMovementMechanismFailsClosedOnPreservation() {
        Fixture fixture = new Fixture();
        PhysicalCard emperor = fixture.add(
                "9_109", 60, DARK, fixture.throne);
        PhysicalCard vader = fixture.add(
                "1_168", 61, DARK, fixture.origin);
        PhysicalCard luke = fixture.add(
                "9_24", 62, LIGHT, fixture.origin);
        fixture.persona(emperor, Persona.SIDIOUS);
        fixture.persona(vader, Persona.VADER);
        fixture.persona(luke, Persona.LUKE);
        fixture.capture(vader, luke);

        CaptureObjectivePolicy.BhbmForceDripUrgencyFacts facts =
                fixture.read(
                    "unknown-move", vader, vader,
                    fixture.throne,
                    BhbmForceDripUrgencyFactsReader
                        .CandidateMechanism.OTHER);

        assertFalse(facts.preservesStableBackState());
        assertTrue(CaptureObjectivePolicy
                .scoreBhbmForceDripUrgency(facts)
                .operations().isEmpty());
    }

    @Test
    public void nonBhbmAnalyzerFailsClosed() {
        Fixture fixture = new Fixture();
        PhysicalCard vader = fixture.add(
                "1_168", 70, DARK, fixture.origin);
        fixture.persona(vader, Persona.VADER);
        when(fixture.analyzer.getObjectiveBlueprintId())
                .thenReturn("9_61");

        CaptureObjectivePolicy.BhbmForceDripUrgencyFacts facts =
                fixture.read(
                    "tigih", vader, vader,
                    fixture.throne,
                    BhbmForceDripUrgencyFactsReader
                        .CandidateMechanism.LANDSPEED);

        assertFalse(facts.factsKnown());
        assertTrue(CaptureObjectivePolicy
                .scoreBhbmForceDripUrgency(facts)
                .operations().isEmpty());
    }

    @Test
    public void frontSidePhysicalObjectiveFailsClosed() {
        Fixture fixture = new Fixture();
        PhysicalCard vader = fixture.add(
                "1_168", 75, DARK, fixture.origin);
        fixture.persona(vader, Persona.VADER);
        when(fixture.objective.isFlipped())
                .thenReturn(false);

        CaptureObjectivePolicy.BhbmForceDripUrgencyFacts facts =
                fixture.read(
                    "front", vader, vader,
                    fixture.throne,
                    BhbmForceDripUrgencyFactsReader
                        .CandidateMechanism.LANDSPEED);

        assertFalse(facts.factsKnown());
        assertFalse(facts.forceDripActive());
        assertTrue(CaptureObjectivePolicy
                .scoreBhbmForceDripUrgency(facts)
                .operations().isEmpty());
    }

    @Test
    public void frozenCaptiveCannotCompleteTheDuelTrio() {
        Fixture fixture = new Fixture();
        PhysicalCard emperor = fixture.add(
                "9_109", 80, DARK, fixture.throne);
        PhysicalCard vader = fixture.add(
                "1_168", 81, DARK, fixture.origin);
        PhysicalCard luke = fixture.add(
                "9_24", 82, LIGHT, fixture.origin);
        fixture.persona(emperor, Persona.SIDIOUS);
        fixture.persona(vader, Persona.VADER);
        fixture.persona(luke, Persona.LUKE);
        fixture.capture(vader, luke);
        when(luke.isFrozen()).thenReturn(true);

        CaptureObjectivePolicy.BhbmForceDripUrgencyFacts facts =
                fixture.read(
                    "frozen", vader, vader,
                    fixture.throne,
                    BhbmForceDripUrgencyFactsReader
                        .CandidateMechanism.LANDSPEED);

        assertTrue(facts.stableBackCurrentlyHeld());
        assertEquals(1, facts.duelTrioPieceCountBefore());
        assertEquals(2, facts.duelTrioPieceCountAfter());
        assertTrue(CaptureObjectivePolicy
                .scoreBhbmForceDripUrgency(facts)
                .operations().isEmpty());
    }

    @Test
    public void excludedCaptiveCannotCompleteTheDuelTrio() {
        Fixture fixture = new Fixture();
        PhysicalCard emperor = fixture.add(
                "9_109", 90, DARK, fixture.throne);
        PhysicalCard vader = fixture.add(
                "1_168", 91, DARK, fixture.origin);
        PhysicalCard luke = fixture.add(
                "9_24", 92, LIGHT, fixture.origin);
        fixture.persona(emperor, Persona.SIDIOUS);
        fixture.persona(vader, Persona.VADER);
        fixture.persona(luke, Persona.LUKE);
        fixture.capture(vader, luke);
        fixture.exclude(luke);

        CaptureObjectivePolicy.BhbmForceDripUrgencyFacts facts =
                fixture.read(
                    "excluded", vader, vader,
                    fixture.throne,
                    BhbmForceDripUrgencyFactsReader
                        .CandidateMechanism.LANDSPEED);

        assertTrue(facts.stableBackCurrentlyHeld());
        assertEquals(1, facts.duelTrioPieceCountBefore());
        assertEquals(2, facts.duelTrioPieceCountAfter());
        assertTrue(CaptureObjectivePolicy
                .scoreBhbmForceDripUrgency(facts)
                .operations().isEmpty());
    }

    private static PolicyOperation only(
            PolicyResult result) {
        assertEquals(1, result.operations().size());
        return result.operations().get(0);
    }

    private static final class Fixture {
        private final GameState gameState =
                mock(GameState.class);
        private final SwccgGame game =
                mock(SwccgGame.class);
        private final ModifiersQuerying modifiers =
                mock(ModifiersQuerying.class);
        private final ObjectiveAnalyzer analyzer =
                mock(ObjectiveAnalyzer.class);
        private final List<PhysicalCard> cards =
                new ArrayList<>();
        private final Set<PhysicalCard> active =
                Collections.newSetFromMap(
                    new IdentityHashMap<>());
        private final Set<PhysicalCard> excluded =
                Collections.newSetFromMap(
                    new IdentityHashMap<>());
        private final Map<Integer, PhysicalCard> byId =
                new LinkedHashMap<>();
        private final Map<PhysicalCard, PhysicalCard> locations =
                new IdentityHashMap<>();
        private final Map<PhysicalCard, Set<Persona>> personas =
                new IdentityHashMap<>();
        private final PhysicalCard objective;
        private final PhysicalCard throne;
        private final PhysicalCard origin;

        private Fixture() {
            objective = add(
                    "9_151", 1, DARK, null);
            when(objective.isFlipped())
                    .thenReturn(true);
            throne = add(
                    "9_147", 2, DARK, null);
            origin = add(
                    "213_56", 3, DARK, null);

            when(game.getGameState())
                    .thenReturn(gameState);
            when(game.getModifiersQuerying())
                    .thenReturn(modifiers);
            when(gameState.getAllPermanentCards())
                    .thenReturn(cards);
            when(gameState.findCardByPermanentId(
                    anyInt()))
                    .thenAnswer(invocation ->
                        byId.get(invocation.getArgument(
                            0, Integer.class)));
            when(gameState.isCardInPlayActive(
                    any(PhysicalCard.class),
                    anyBoolean(), anyBoolean(),
                    anyBoolean(), anyBoolean(),
                    anyBoolean(), anyBoolean(),
                    anyBoolean(), anyBoolean()))
                    .thenAnswer(invocation -> {
                        PhysicalCard card =
                                invocation.getArgument(0);
                        boolean includeExcluded =
                                invocation.getArgument(
                                    1, Boolean.class);
                        boolean includeCaptives =
                                invocation.getArgument(
                                    3, Boolean.class);
                        return active.contains(card)
                                && (!excluded.contains(card)
                                    || includeExcluded)
                                && (!card.isCaptive()
                                    || includeCaptives);
                    });
            when(modifiers.getLocationThatCardIsPresentAt(
                    any(GameState.class),
                    any(PhysicalCard.class)))
                    .thenAnswer(invocation -> {
                        PhysicalCard card =
                                invocation.getArgument(1);
                        if (card == throne || card == origin) {
                            return card;
                        }
                        return locations.get(card);
                    });
            when(modifiers.isPresentWith(
                    any(GameState.class),
                    any(PhysicalCard.class),
                    any(PhysicalCard.class)))
                    .thenAnswer(invocation -> {
                        PhysicalCard first =
                                invocation.getArgument(1);
                        PhysicalCard second =
                                invocation.getArgument(2);
                        PhysicalCard firstLocation =
                                locations.get(first);
                        return firstLocation != null
                                && firstLocation
                                    == locations.get(second);
                    });
            when(modifiers.hasPersona(
                    any(GameState.class),
                    any(PhysicalCard.class),
                    any(Persona.class)))
                    .thenAnswer(invocation ->
                        personas.getOrDefault(
                            invocation.getArgument(1),
                            Set.of())
                            .contains(
                                invocation.getArgument(2)));
            when(modifiers.canBeTargetedBy(
                    any(GameState.class),
                    any(PhysicalCard.class),
                    any(PhysicalCard.class),
                    any()))
                    .thenReturn(true);
            when(modifiers.getForceToLoseFromCardLimit(
                    gameState, DARK,
                    objective)).thenReturn(1.0f);
            when(analyzer.isFlipped())
                    .thenReturn(true);
            when(analyzer.isFlipAgeKnown())
                    .thenReturn(true);
            when(analyzer.getTurnsObservedSinceFlip())
                    .thenReturn(4);
            when(analyzer.getObjectiveBlueprintId())
                    .thenReturn("9_151");
        }

        private PhysicalCard add(
                String blueprintId,
                int id,
                String owner,
                PhysicalCard location) {
            PhysicalCard card =
                    physical(
                        blueprintId, id, owner);
            cards.add(card);
            active.add(card);
            byId.put(id, card);
            if (location != null) {
                locations.put(card, location);
            }
            return card;
        }

        private PhysicalCard addInactive(
                String blueprintId,
                int id,
                String owner) {
            PhysicalCard card =
                    physical(
                        blueprintId, id, owner);
            cards.add(card);
            byId.put(id, card);
            return card;
        }

        private void persona(
                PhysicalCard card,
                Persona persona) {
            personas.computeIfAbsent(
                    card,
                    ignored -> EnumSet.noneOf(
                        Persona.class))
                    .add(persona);
        }

        private void capture(
                PhysicalCard escort,
                PhysicalCard captive) {
            when(captive.isCaptive())
                    .thenReturn(true);
            when(captive.getEscort())
                    .thenReturn(escort);
        }

        private void exclude(
                PhysicalCard card) {
            excluded.add(card);
        }

        private CaptureObjectivePolicy.BhbmForceDripUrgencyFacts read(
                String actionId,
                PhysicalCard actionSource,
                PhysicalCard candidate,
                PhysicalCard destination,
                BhbmForceDripUrgencyFactsReader.CandidateMechanism
                        mechanism) {
            return BhbmForceDripUrgencyFactsReader.read(
                    actionId, game, DARK,
                    analyzer, actionSource,
                    candidate, destination,
                    true, mechanism);
        }

        private static PhysicalCard physical(
                String blueprintId,
                int id,
                String owner) {
            PhysicalCard card =
                    mock(PhysicalCard.class);
            SwccgCardBlueprint blueprint =
                    CARDS.getSwccgoCardBlueprint(
                        blueprintId);
            when(card.getOwner())
                    .thenReturn(owner);
            when(card.getZone())
                    .thenReturn(Zone.AT_LOCATION);
            when(card.getBlueprint())
                    .thenReturn(blueprint);
            when(card.getBlueprintId(true))
                    .thenReturn(blueprintId);
            when(card.getBlueprintId(false))
                    .thenReturn(blueprintId);
            when(card.getTitle())
                    .thenReturn(blueprint.getTitle());
            when(card.getTitles())
                    .thenReturn(List.of(
                        blueprint.getTitle()));
            when(card.getPermanentCardId())
                    .thenReturn(id);
            when(card.getCardId())
                    .thenReturn(id);
            when(card.getAdditionalCardIds())
                    .thenReturn(List.of());
            when(card.isCaptive())
                    .thenReturn(false);
            when(card.isFrozen())
                    .thenReturn(false);
            return card;
        }
    }
}
