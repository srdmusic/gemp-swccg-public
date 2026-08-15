package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.TargetingReason;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.PhysicalCardVisitor;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgCardBlueprintLibrary;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.game.state.actions.GameTextActionState;
import com.gempukku.swccgo.game.state.actions.PlayCardState;
import com.gempukku.swccgo.logic.actions.GameTextAction;
import com.gempukku.swccgo.logic.actions.PlayCardAction;
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
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CaptureMovementMechanismFactsReaderTest {
    private static final String DARK = "dark";
    private static final String LIGHT = "light";
    private static final SwccgCardBlueprintLibrary CARDS =
            new SwccgCardBlueprintLibrary();

    @Test
    public void sourceTypedClassifierRejectsTextImpostors() {
        PhysicalCard castle = card(
                "209_50", 1, DARK, Zone.LOCATIONS);
        PhysicalCard wrongSource = card(
                "1_290", 2, DARK, Zone.LOCATIONS);
        PhysicalCard transport = card(
                "1_243", 3, DARK, Zone.HAND);
        PhysicalCard lightTransport = card(
                "1_97", 4, LIGHT, Zone.HAND);
        PhysicalCard vader = card(
                "1_168", 5, DARK, Zone.AT_LOCATION);
        PhysicalCard rise = card(
                "9_140", 6, DARK, Zone.SIDE_OF_TABLE);
        PhysicalCard machination = card(
                "222_16", 7, DARK, Zone.SIDE_OF_TABLE);

        assertEquals(
                CaptureMovementMechanismFactsReader
                    .Mechanism.VADERS_CASTLE,
                CaptureMovementMechanismFactsReader
                    .classifyParent(
                        castle,
                        "Move from here to other battleground site"));
        assertEquals(
                CaptureMovementMechanismFactsReader
                    .Mechanism.UNKNOWN,
                CaptureMovementMechanismFactsReader
                    .classifyParent(
                        wrongSource,
                        "Move from here to other battleground site"));
        assertEquals(
                CaptureMovementMechanismFactsReader
                    .Mechanism.UNKNOWN,
                CaptureMovementMechanismFactsReader
                    .classifyParent(
                        castle,
                        "Move from here to other battleground site now"));
        assertEquals(
                CaptureMovementMechanismFactsReader
                    .Mechanism.TRANSPORT,
                CaptureMovementMechanismFactsReader
                    .classifyParent(
                        transport,
                        "'Transport' characters"));
        assertEquals(
                CaptureMovementMechanismFactsReader
                    .Mechanism.UNKNOWN,
                CaptureMovementMechanismFactsReader
                    .classifyParent(
                        wrongSource,
                        "'Transport' characters"));
        assertEquals(
                CaptureMovementMechanismFactsReader
                    .Mechanism.TRANSPORT,
                CaptureMovementMechanismFactsReader
                    .classifyParent(
                        lightTransport,
                        "'Transport' characters"));
        assertEquals(
                CaptureMovementMechanismFactsReader
                    .Mechanism.DISEMBARK,
                CaptureMovementMechanismFactsReader
                    .classifyParent(vader, "Disembark"));
        assertEquals(
                CaptureMovementMechanismFactsReader
                    .Mechanism.DOCKING_BAY_TRANSIT,
                CaptureMovementMechanismFactsReader
                    .classifyParent(
                        wrongSource, "Docking bay transit"));
        assertEquals(
                CaptureMovementMechanismFactsReader
                    .Mechanism.RISE_RECALL,
                CaptureMovementMechanismFactsReader
                    .classifyParent(
                        rise, "Take Vader into hand"));
        assertEquals(
                CaptureMovementMechanismFactsReader
                    .Mechanism.RISE_RELOCATE,
                CaptureMovementMechanismFactsReader
                    .classifyParent(
                        rise,
                        "Relocate Vader to Death Star II: Docking Bay"));
        assertEquals(
                CaptureMovementMechanismFactsReader
                    .Mechanism.MACHINATION_RELOCATE,
                CaptureMovementMechanismFactsReader
                    .classifyParent(
                        machination,
                        "Relocate Vader to site"));
        assertEquals(
                CaptureMovementMechanismFactsReader
                    .Mechanism.UNKNOWN,
                CaptureMovementMechanismFactsReader
                    .classifyParent(
                        vader, "Disembark and capture"));
        assertEquals(
                CaptureMovementMechanismFactsReader
                    .Mechanism.UNKNOWN,
                CaptureMovementMechanismFactsReader
                    .classifyParent(
                        wrongSource,
                        "Relocate Vader to site"));
    }

    @Test
    public void onlyTheExactLandspeedParentWinsCaptureCredit() {
        Fixture fixture = new Fixture(false);
        PhysicalCard origin = fixture.location(
                "1_290", 118, DARK);
        PhysicalCard captureSite = fixture.location(
                "1_291", 119, DARK);
        PhysicalCard vader = fixture.active(
                "1_168", 120, DARK, origin);
        PhysicalCard luke = fixture.active(
                "1_19", 121, LIGHT, captureSite);
        fixture.persona(vader, Persona.VADER);
        fixture.persona(luke, Persona.LUKE);
        fixture.allowCaptureTarget(luke);
        fixture.allowLandspeedMove(
                vader, captureSite, 1.0f);
        assertTrue(CaptureObjectiveFacts
                .hasLegalImmediateCaptureMoveDestination(
                    fixture.game, DARK,
                    fixture.commonAnalyzer(false), vader));

        var randoAnalyzer = mock(
                com.gempukku.swccgo.ai.models.rando
                    .strategy.ObjectiveAnalyzer.class);
        stubAnalyzer(randoAnalyzer, false);
        var randoContext =
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.DecisionContext(
                        fixture.gameState, DARK,
                        "ACTION_CHOICE", "Choose action",
                        "landspeed-exact-rando", Phase.MOVE);
        randoContext.setGame(fixture.game);
        randoContext.setSide(Side.DARK);
        randoContext.setObjectiveAnalyzer(randoAnalyzer);
        randoContext.setActionIds(
                List.of("impostor", "exact", ""));
        randoContext.setActionTexts(List.of(
                "Move using landspeed now",
                "Move using landspeed",
                "Pass"));
        randoContext.setCardIds(List.of(
                String.valueOf(vader.getCardId()),
                String.valueOf(vader.getCardId()), ""));
        randoContext.setNoPass(false);

        var chosenAnalyzer = mock(
                com.gempukku.swccgo.ai.models.chosenone
                    .strategy.ObjectiveAnalyzer.class);
        stubAnalyzer(chosenAnalyzer, false);
        var chosenContext =
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.DecisionContext(
                        fixture.gameState, DARK,
                        "ACTION_CHOICE", "Choose action",
                        "landspeed-exact-chosen", Phase.MOVE);
        chosenContext.setGame(fixture.game);
        chosenContext.setSide(Side.DARK);
        chosenContext.setObjectiveAnalyzer(chosenAnalyzer);
        chosenContext.setActionIds(
                List.of("impostor", "exact", ""));
        chosenContext.setActionTexts(List.of(
                "Move using landspeed now",
                "Move using landspeed",
                "Pass"));
        chosenContext.setCardIds(List.of(
                String.valueOf(vader.getCardId()),
                String.valueOf(vader.getCardId()), ""));
        chosenContext.setNoPass(false);

        var randoMoveActions =
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.MoveEvaluator()
                    .evaluate(randoContext);
        var chosenMoveActions =
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.MoveEvaluator()
                    .evaluate(chosenContext);
        var randoImpostor = randoMoveActions.stream()
                .filter(action ->
                    "impostor".equals(action.getActionId()))
                .findFirst().orElseThrow();
        var chosenImpostor = chosenMoveActions.stream()
                .filter(action ->
                    "impostor".equals(action.getActionId()))
                .findFirst().orElseThrow();
        var randoExact = randoMoveActions.stream()
                .filter(action ->
                    "exact".equals(action.getActionId()))
                .findFirst().orElseThrow();
        var chosenExact = chosenMoveActions.stream()
                .filter(action ->
                    "exact".equals(action.getActionId()))
                .findFirst().orElseThrow();
        assertFalse(randoImpostor.getReasoningString()
                .contains("CAPTURE ROUTE"));
        assertFalse(chosenImpostor.getReasoningString()
                .contains("CAPTURE ROUTE"));
        assertTrue(randoExact.getReasoningString()
                .contains("BHBM CAPTURE ROUTE"));
        assertTrue(chosenExact.getReasoningString()
                .contains("BHBM CAPTURE ROUTE"));
        com.gempukku.swccgo.ai.models.common.trace.DecisionTrace
                randoTrace;
        assertTrue(
                com.gempukku.swccgo.ai.models.common.trace
                    .TraceSession.open(
                        "rando", "landspeed-r4-rando",
                        "ACTION_CHOICE", "Choose action",
                        randoContext.getActionIds(), null,
                        List.of("focused evaluator fixture"),
                        false));
        try {
            new com.gempukku.swccgo.ai.models.rando
                .evaluators.MoveEvaluator()
                .evaluate(randoContext);
        } finally {
            randoTrace =
                    com.gempukku.swccgo.ai.models.common.trace
                        .TraceSession.close();
        }
        com.gempukku.swccgo.ai.models.common.trace.DecisionTrace
                chosenTrace;
        assertTrue(
                com.gempukku.swccgo.ai.models.common.trace
                    .TraceSession.open(
                        "chosenone", "landspeed-r4-chosen",
                        "ACTION_CHOICE", "Choose action",
                        chosenContext.getActionIds(), null,
                        List.of("focused evaluator fixture"),
                        false));
        try {
            new com.gempukku.swccgo.ai.models.chosenone
                .evaluators.MoveEvaluator()
                .evaluate(chosenContext);
        } finally {
            chosenTrace =
                    com.gempukku.swccgo.ai.models.common.trace
                        .TraceSession.close();
        }
        assertTrue(randoTrace != null);
        assertTrue(chosenTrace != null);
        assertEquals(1, randoTrace.getOperations().stream()
                .filter(operation ->
                    "exact".equals(operation.getActionId())
                        && operation.getDomainId()
                            == com.gempukku.swccgo.ai.models.common.trace
                                .TraceDomainId.OBJECTIVE_INTENT
                        && operation.getDeltaBits() != null
                        && Float.intBitsToFloat(
                            operation.getDeltaBits())
                            == 300.0f)
                .count());
        assertEquals(1, chosenTrace.getOperations().stream()
                .filter(operation ->
                    "exact".equals(operation.getActionId())
                        && operation.getDomainId()
                            == com.gempukku.swccgo.ai.models.common.trace
                                .TraceDomainId.OBJECTIVE_INTENT
                        && operation.getDeltaBits() != null
                        && Float.intBitsToFloat(
                            operation.getDeltaBits())
                            == 300.0f)
                .count());
        assertTrue(randoTrace.getOperations().stream()
                .noneMatch(operation ->
                    operation.getDomainId()
                            == com.gempukku.swccgo.ai.models.common.trace
                                .TraceDomainId.OBJECTIVE_INTENT
                        && operation.getDeltaBits() != null
                        && Float.intBitsToFloat(
                            operation.getDeltaBits())
                            > 300.0f));
        assertTrue(chosenTrace.getOperations().stream()
                .noneMatch(operation ->
                    operation.getDomainId()
                            == com.gempukku.swccgo.ai.models.common.trace
                                .TraceDomainId.OBJECTIVE_INTENT
                        && operation.getDeltaBits() != null
                        && Float.intBitsToFloat(
                            operation.getDeltaBits())
                            > 300.0f));
        assertEquals(1, randoExact.getReasoning().stream()
                .filter(reason ->
                    reason.contains("BHBM CAPTURE ROUTE"))
                .count());
        assertEquals(1, chosenExact.getReasoning().stream()
                .filter(reason ->
                    reason.contains("BHBM CAPTURE ROUTE"))
                .count());
        assertEquals("Non-objective tactics may change the total score",
                randoExact.getScore(), chosenExact.getScore(), 0.0f);

        var randoWinner =
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.CombinedEvaluator()
                    .evaluateDecision(randoContext);
        var chosenWinner =
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.CombinedEvaluator()
                    .evaluateDecision(chosenContext);
        assertEquals("exact", randoWinner.getActionId());
        assertEquals("exact", chosenWinner.getActionId());
        assertTrue(randoWinner.getReasoningString()
                .contains("BHBM CAPTURE ROUTE"));
        assertTrue(chosenWinner.getReasoningString()
                .contains("BHBM CAPTURE ROUTE"));
    }

    @Test
    public void unsafeLandspeedCaptureRouteCannotOutvoteTheCombinedHold() {
        Fixture fixture = new Fixture(false);
        PhysicalCard origin = fixture.location(
                "1_290", 122, DARK);
        PhysicalCard captureSite = fixture.location(
                "1_291", 123, DARK);
        PhysicalCard vader = fixture.active(
                "1_168", 124, DARK, origin);
        PhysicalCard luke = fixture.active(
                "1_19", 125, LIGHT, captureSite);
        fixture.persona(vader, Persona.VADER);
        fixture.persona(luke, Persona.LUKE);
        fixture.allowCaptureTarget(luke);
        fixture.allowLandspeedMove(
                vader, captureSite, 1.0f);
        when(fixture.modifiers.getTotalPowerAtLocation(
                fixture.gameState, captureSite, LIGHT,
                false, false)).thenReturn(30.0f);
        when(fixture.modifiers.getTotalPowerAtLocation(
                fixture.gameState, captureSite, DARK,
                false, false)).thenReturn(0.0f);

        var assessment =
                CaptureMovementMechanismFactsReader.assess(
                    fixture.game, DARK,
                    fixture.commonAnalyzer(false),
                    CaptureMovementMechanismFactsReader
                        .Mechanism.LANDSPEED,
                    vader, "Move using landspeed");
        assertTrue(assessment.routes().stream().anyMatch(
                route -> route.mover() == vader
                    && route.destination() == captureSite
                    && route.guaranteesImmediateCapture()
                    && route.formationBlocked()));
        assertFalse(assessment.hasAdmissibleCaptureRoute());

        var randoAnalyzer = mock(
                com.gempukku.swccgo.ai.models.rando
                    .strategy.ObjectiveAnalyzer.class);
        stubAnalyzer(randoAnalyzer, false);
        var randoContext =
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.DecisionContext(
                        fixture.gameState, DARK,
                        "ACTION_CHOICE", "Choose action",
                        "unsafe-landspeed-rando",
                        Phase.MOVE);
        randoContext.setGame(fixture.game);
        randoContext.setSide(Side.DARK);
        randoContext.setObjectiveAnalyzer(randoAnalyzer);
        randoContext.setActionIds(
                List.of("landspeed", ""));
        randoContext.setActionTexts(
                List.of("Move using landspeed", "Pass"));
        randoContext.setCardIds(List.of(
                String.valueOf(vader.getCardId()), ""));
        randoContext.setNoPass(false);

        var chosenAnalyzer = mock(
                com.gempukku.swccgo.ai.models.chosenone
                    .strategy.ObjectiveAnalyzer.class);
        stubAnalyzer(chosenAnalyzer, false);
        var chosenContext =
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.DecisionContext(
                        fixture.gameState, DARK,
                        "ACTION_CHOICE", "Choose action",
                        "unsafe-landspeed-chosen",
                        Phase.MOVE);
        chosenContext.setGame(fixture.game);
        chosenContext.setSide(Side.DARK);
        chosenContext.setObjectiveAnalyzer(chosenAnalyzer);
        chosenContext.setActionIds(
                List.of("landspeed", ""));
        chosenContext.setActionTexts(
                List.of("Move using landspeed", "Pass"));
        chosenContext.setCardIds(List.of(
                String.valueOf(vader.getCardId()), ""));
        chosenContext.setNoPass(false);

        var randoMove =
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.MoveEvaluator()
                    .evaluate(randoContext).stream()
                    .filter(action ->
                        "landspeed".equals(action.getActionId()))
                    .findFirst().orElseThrow();
        var chosenMove =
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.MoveEvaluator()
                    .evaluate(chosenContext).stream()
                    .filter(action ->
                        "landspeed".equals(action.getActionId()))
                    .findFirst().orElseThrow();
        assertTrue(randoMove.getScore() < 12000.0f);
        assertTrue(chosenMove.getScore() < 12000.0f);
        assertFalse(randoMove.getReasoningString()
                .contains("BHBM CAPTURE ROUTE"));
        assertFalse(chosenMove.getReasoningString()
                .contains("BHBM CAPTURE ROUTE"));

        var randoConstraint =
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.CaptureMovementEvaluator()
                    .evaluate(randoContext).stream()
                    .filter(action ->
                        "landspeed".equals(action.getActionId()))
                    .findFirst().orElseThrow();
        var chosenConstraint =
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.CaptureMovementEvaluator()
                    .evaluate(chosenContext).stream()
                    .filter(action ->
                        "landspeed".equals(action.getActionId()))
                    .findFirst().orElseThrow();
        assertTrue(randoConstraint.isHardVetoed());
        assertTrue(chosenConstraint.isHardVetoed());

        assertEquals("",
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.CombinedEvaluator()
                    .evaluateDecision(randoContext)
                    .getActionId());
        assertEquals("",
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.CombinedEvaluator()
                    .evaluateDecision(chosenContext)
                    .getActionId());
    }

    @Test
    public void mixedSafeAndUnsafeDestinationsDoNotCreateAFalseParentClaim() {
        Fixture fixture = new Fixture(false);
        PhysicalCard safeSite = fixture.location(
                "1_297", 126, DARK);
        PhysicalCard origin = fixture.location(
                "1_290", 127, DARK);
        PhysicalCard captureSite = fixture.location(
                "1_291", 128, DARK);
        PhysicalCard vader = fixture.active(
                "1_168", 129, DARK, origin);
        PhysicalCard luke = fixture.active(
                "1_19", 130, LIGHT, captureSite);
        fixture.persona(vader, Persona.VADER);
        fixture.persona(luke, Persona.LUKE);
        fixture.allowCaptureTarget(luke);
        fixture.allowLandspeedMove(
                vader, safeSite, 1.0f);
        fixture.allowLandspeedMove(
                vader, captureSite, 1.0f);
        when(fixture.modifiers.getTotalPowerAtLocation(
                fixture.gameState, captureSite, LIGHT,
                false, false)).thenReturn(30.0f);
        when(fixture.modifiers.getTotalPowerAtLocation(
                fixture.gameState, captureSite, DARK,
                false, false)).thenReturn(0.0f);

        ObjectiveAnalyzer common =
                fixture.commonAnalyzer(false);
        assertTrue(CaptureObjectiveFacts
                .hasLegalImmediateCaptureMoveDestination(
                    fixture.game, DARK, common, vader));
        assertFalse(CaptureObjectiveFacts
                .hasFormationSafeLegalImmediateCaptureMoveDestination(
                    fixture.game, DARK, common, vader));
        assertEquals(0,
                CaptureObjectiveFacts
                    .nextCaptureMoveForceReserve(
                        fixture.game, DARK, common, null));

        var assessment =
                CaptureMovementMechanismFactsReader.assess(
                    fixture.game, DARK, common,
                    CaptureMovementMechanismFactsReader
                        .Mechanism.LANDSPEED,
                    vader, "Move using landspeed");
        var unsafeRoute = assessment.routes().stream()
                .filter(route ->
                    route.destination() == captureSite)
                .findFirst().orElseThrow();
        var safeRoute = assessment.routes().stream()
                .filter(route ->
                    route.destination() == safeSite)
                .findFirst().orElseThrow();
        assertTrue(unsafeRoute.guaranteesImmediateCapture());
        assertTrue(unsafeRoute.formationBlocked());
        assertTrue(safeRoute.admissible());
        assertFalse(safeRoute.guaranteesImmediateCapture());
        assertFalse(safeRoute.advancesCaptureApproach());
        assertFalse(assessment.hasAdmissibleCaptureRoute());
        assertFalse(assessment.hasAdmissibleApproachRoute());

        var randoAnalyzer = mock(
                com.gempukku.swccgo.ai.models.rando
                    .strategy.ObjectiveAnalyzer.class);
        stubAnalyzer(randoAnalyzer, false);
        var randoParent =
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.DecisionContext(
                        fixture.gameState, DARK,
                        "ACTION_CHOICE", "Choose action",
                        "mixed-landspeed-parent-rando",
                        Phase.MOVE);
        randoParent.setGame(fixture.game);
        randoParent.setSide(Side.DARK);
        randoParent.setObjectiveAnalyzer(randoAnalyzer);
        randoParent.setActionIds(
                List.of("landspeed", ""));
        randoParent.setActionTexts(
                List.of("Move using landspeed", "Pass"));
        randoParent.setCardIds(List.of(
                String.valueOf(vader.getCardId()), ""));
        randoParent.setNoPass(false);

        var chosenAnalyzer = mock(
                com.gempukku.swccgo.ai.models.chosenone
                    .strategy.ObjectiveAnalyzer.class);
        stubAnalyzer(chosenAnalyzer, false);
        var chosenParent =
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.DecisionContext(
                        fixture.gameState, DARK,
                        "ACTION_CHOICE", "Choose action",
                        "mixed-landspeed-parent-chosen",
                        Phase.MOVE);
        chosenParent.setGame(fixture.game);
        chosenParent.setSide(Side.DARK);
        chosenParent.setObjectiveAnalyzer(chosenAnalyzer);
        chosenParent.setActionIds(
                List.of("landspeed", ""));
        chosenParent.setActionTexts(
                List.of("Move using landspeed", "Pass"));
        chosenParent.setCardIds(List.of(
                String.valueOf(vader.getCardId()), ""));
        chosenParent.setNoPass(false);

        var randoMove =
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.MoveEvaluator()
                    .evaluate(randoParent).stream()
                    .filter(action ->
                        "landspeed".equals(action.getActionId()))
                    .findFirst().orElseThrow();
        var chosenMove =
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.MoveEvaluator()
                    .evaluate(chosenParent).stream()
                    .filter(action ->
                        "landspeed".equals(action.getActionId()))
                    .findFirst().orElseThrow();
        assertFalse(randoMove.getReasoningString()
                .contains("BHBM CAPTURE ROUTE"));
        assertFalse(chosenMove.getReasoningString()
                .contains("BHBM CAPTURE ROUTE"));
        assertEquals("",
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.CombinedEvaluator()
                    .evaluateDecision(randoParent)
                    .getActionId());
        assertEquals("",
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.CombinedEvaluator()
                    .evaluateDecision(chosenParent)
                    .getActionId());

        String childPrompt =
                "Choose where to move "
                    + com.gempukku.swccgo.logic.GameUtils
                        .getCardLink(vader)
                    + " using landspeed";
        var randoChild = randoSelection(
                fixture, randoAnalyzer, childPrompt,
                "mixed-landspeed-child-rando",
                List.of(captureSite, safeSite));
        randoChild.setExtra(
                MovePhysicalCardResolver
                    .MOVER_CARD_ID_EXTRA,
                String.valueOf(vader.getCardId()));
        var chosenChild = chosenSelection(
                fixture, chosenAnalyzer, childPrompt,
                "mixed-landspeed-child-chosen",
                List.of(captureSite, safeSite));
        chosenChild.setExtra(
                MovePhysicalCardResolver
                    .MOVER_CARD_ID_EXTRA,
                String.valueOf(vader.getCardId()));

        var randoChildActions =
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.CaptureMovementEvaluator()
                    .evaluate(randoChild);
        var chosenChildActions =
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.CaptureMovementEvaluator()
                    .evaluate(chosenChild);
        var randoUnsafe = randoChildActions.stream()
                .filter(action -> String.valueOf(
                    captureSite.getCardId())
                    .equals(action.getActionId()))
                .findFirst().orElseThrow();
        var chosenUnsafe = chosenChildActions.stream()
                .filter(action -> String.valueOf(
                    captureSite.getCardId())
                    .equals(action.getActionId()))
                .findFirst().orElseThrow();
        var randoSafe = randoChildActions.stream()
                .filter(action -> String.valueOf(
                    safeSite.getCardId())
                    .equals(action.getActionId()))
                .findFirst().orElseThrow();
        var chosenSafe = chosenChildActions.stream()
                .filter(action -> String.valueOf(
                    safeSite.getCardId())
                    .equals(action.getActionId()))
                .findFirst().orElseThrow();
        assertTrue(randoUnsafe.isHardVetoed());
        assertTrue(chosenUnsafe.isHardVetoed());
        assertFalse(randoSafe.isHardVetoed());
        assertFalse(chosenSafe.isHardVetoed());
        assertFalse(randoSafe.getReasoningString()
                .contains("CAPTURE ROUTE"));
        assertFalse(chosenSafe.getReasoningString()
                .contains("CAPTURE ROUTE"));

        assertEquals(
                String.valueOf(safeSite.getCardId()),
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.CombinedEvaluator()
                    .evaluateDecision(randoChild)
                    .getActionId());
        assertEquals(
                String.valueOf(safeSite.getCardId()),
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.CombinedEvaluator()
                    .evaluateDecision(chosenChild)
                    .getActionId());
    }

    @Test
    public void embarkRequiresThePostMovePresencePlaceForCapture() {
        Fixture fixture = new Fixture(false);
        PhysicalCard site = fixture.location(
                "1_290", 131, DARK);
        PhysicalCard vader = fixture.active(
                "1_168", 132, DARK, site);
        PhysicalCard luke = fixture.active(
                "1_19", 133, LIGHT, site);
        PhysicalCard enclosed = fixture.active(
                "3_154", 134, DARK, site);
        PhysicalCard open = fixture.active(
                "6_173", 135, DARK, site);
        fixture.persona(vader, Persona.VADER);
        fixture.persona(luke, Persona.LUKE);
        fixture.allowCaptureTarget(luke);
        fixture.allowEmbark(vader, enclosed);
        fixture.allowEmbark(vader, open);
        assertTrue(fixture.modifiers.hasKeyword(
                fixture.gameState, enclosed,
                Keyword.ENCLOSED));

        var assessment =
                CaptureMovementMechanismFactsReader.assess(
                    fixture.game, DARK,
                    fixture.commonAnalyzer(false),
                    CaptureMovementMechanismFactsReader
                        .Mechanism.EMBARK,
                    vader, "Embark");
        var enclosedRoute = assessment.routes().stream()
                .filter(route ->
                    route.destination() == enclosed)
                .findFirst().orElseThrow();
        var openRoute = assessment.routes().stream()
                .filter(route ->
                    route.destination() == open)
                .findFirst().orElseThrow();

        assertTrue(enclosedRoute.admissible());
        assertFalse(
                "Vader aboard an enclosed carrier is not present "
                    + "with outside Luke",
                enclosedRoute.guaranteesImmediateCapture());
        assertFalse(enclosedRoute.advancesCaptureApproach());
        assertTrue(openRoute.admissible());
        assertTrue(
                "Vader aboard an open vehicle remains present "
                    + "at its outer site",
                openRoute.guaranteesImmediateCapture());
    }

    @Test
    public void landspeedPursuesAnOpenCarrierWithVaderAboard() {
        Fixture fixture = new Fixture(false);
        PhysicalCard origin = fixture.location(
                "1_290", 150, DARK);
        PhysicalCard captureSite = fixture.location(
                "1_291", 151, DARK);
        PhysicalCard openCarrier = fixture.active(
                "6_173", 152, DARK, origin);
        PhysicalCard enclosedCarrier = fixture.active(
                "3_154", 153, DARK, origin);
        PhysicalCard openVader = fixture.cardAndAdd(
                "1_168", 154, DARK,
                Zone.AT_LOCATION, true);
        PhysicalCard enclosedVader = fixture.cardAndAdd(
                "1_168", 155, DARK,
                Zone.AT_LOCATION, true);
        fixture.aboard(
                openVader, openCarrier, origin,
                origin, origin);
        fixture.aboard(
                enclosedVader, enclosedCarrier,
                origin, enclosedCarrier, null);
        when(fixture.gameState.getAboardCards(
                openCarrier, false))
                .thenReturn(List.of(openVader));
        when(fixture.gameState.getAboardCards(
                enclosedCarrier, false))
                .thenReturn(List.of(enclosedVader));
        PhysicalCard luke = fixture.active(
                "1_19", 156, LIGHT, captureSite);
        fixture.persona(openVader, Persona.VADER);
        fixture.persona(enclosedVader, Persona.VADER);
        fixture.persona(luke, Persona.LUKE);
        fixture.allowCaptureTarget(luke);
        fixture.allowLandspeedMove(
                openCarrier, captureSite, 1.0f);
        fixture.allowLandspeedMove(
                enclosedCarrier, captureSite, 1.0f);
        when(fixture.modifiers.isPiloted(
                fixture.gameState,
                openCarrier, false)).thenReturn(true);
        when(fixture.modifiers.isPiloted(
                fixture.gameState,
                enclosedCarrier, false)).thenReturn(true);
        when(fixture.modifiers.isAboard(
                fixture.gameState, openVader,
                openCarrier, false, true))
                .thenReturn(true);
        when(fixture.modifiers.isAboard(
                fixture.gameState, enclosedVader,
                enclosedCarrier, false, true))
                .thenReturn(true);

        var openAssessment =
                CaptureMovementMechanismFactsReader.assess(
                    fixture.game, DARK,
                    fixture.commonAnalyzer(false),
                    CaptureMovementMechanismFactsReader
                        .Mechanism.LANDSPEED,
                    openCarrier, "Move using landspeed");
        var openRoute = openAssessment.routes().stream()
                .filter(route ->
                    route.destination() == captureSite)
                .findFirst().orElseThrow();

        assertTrue(openRoute.objectiveRelevantMover());
        assertTrue(openRoute.admissible());
        assertTrue(openRoute.guaranteesImmediateCapture());
        assertTrue(openAssessment.hasAdmissibleCaptureRoute());
        assertEquals(1,
                CaptureObjectiveFacts
                    .nextCaptureMoveForceReserve(
                        fixture.game, DARK,
                        fixture.commonAnalyzer(false),
                        null));

        var enclosedAssessment =
                CaptureMovementMechanismFactsReader.assess(
                    fixture.game, DARK,
                    fixture.commonAnalyzer(false),
                    CaptureMovementMechanismFactsReader
                        .Mechanism.LANDSPEED,
                    enclosedCarrier, "Move using landspeed");
        var enclosedRoute = enclosedAssessment.routes().stream()
                .filter(route ->
                    route.destination() == captureSite)
                .findFirst().orElseThrow();

        assertFalse(enclosedRoute.objectiveRelevantMover());
        assertFalse(enclosedRoute.guaranteesImmediateCapture());
        assertFalse(enclosedAssessment
                .hasAdmissibleCaptureRoute());
    }

    @Test
    public void shuttleCannotBorrowPresenceFromADistinctShipAtSameSystem() {
        Fixture fixture = new Fixture(false);
        PhysicalCard system = fixture.location(
                "1_289", 136, DARK);
        PhysicalCard exteriorSite = fixture.location(
                "1_291", 137, DARK);
        PhysicalCard executor = fixture.active(
                "4_167", 138, DARK, system);
        PhysicalCard otherShip = fixture.active(
                "1_301", 139, DARK, system);
        PhysicalCard vader = fixture.active(
                "1_168", 140, DARK, exteriorSite);
        PhysicalCard luke = fixture.cardAndAdd(
                "1_19", 141, LIGHT,
                Zone.AT_LOCATION, true);
        fixture.aboard(
                luke, otherShip, system, otherShip, null);
        fixture.persona(vader, Persona.VADER);
        fixture.persona(luke, Persona.LUKE);
        fixture.allowCaptureTarget(luke);
        fixture.allowPassengerDestination(
                vader, executor);
        when(fixture.modifiers.isRelatedLocations(
                fixture.gameState, exteriorSite, system))
            .thenReturn(true);
        assertTrue(com.gempukku.swccgo.filters.Filters
                .hasAvailablePassengerCapacity(vader)
                .accepts(
                    fixture.gameState,
                    fixture.modifiers, executor));
        assertTrue(com.gempukku.swccgo.filters.Filters
                .canShuttleTo(
                    DARK, vader, false, 0.0f)
                .accepts(
                    fixture.gameState,
                    fixture.modifiers, executor));

        var assessment =
                CaptureMovementMechanismFactsReader.assess(
                    fixture.game, DARK,
                    fixture.commonAnalyzer(false),
                    CaptureMovementMechanismFactsReader
                        .Mechanism.SHUTTLE,
                    vader, "Shuttle");
        var route = assessment.routes().stream()
                .filter(candidate ->
                    candidate.destination() == executor)
                .findFirst().orElseThrow();

        assertFalse(
                "Vader entering Executor is not present with Luke "
                    + "aboard a different ship at the same system",
                route.guaranteesImmediateCapture());
    }

    @Test
    public void disembarkToOutsideLukeRemainsImmediateCapture() {
        Fixture fixture = new Fixture(false);
        PhysicalCard site = fixture.location(
                "1_291", 142, DARK);
        PhysicalCard carrier = fixture.active(
                "3_154", 143, DARK, site);
        PhysicalCard vader = fixture.cardAndAdd(
                "1_168", 144, DARK,
                Zone.AT_LOCATION, true);
        fixture.aboard(
                vader, carrier, site, carrier, null);
        PhysicalCard luke = fixture.active(
                "1_19", 145, LIGHT, site);
        fixture.persona(vader, Persona.VADER);
        fixture.persona(luke, Persona.LUKE);
        fixture.allowCaptureTarget(luke);

        var assessment =
                CaptureMovementMechanismFactsReader.assess(
                    fixture.game, DARK,
                    fixture.commonAnalyzer(false),
                    CaptureMovementMechanismFactsReader
                        .Mechanism.DISEMBARK,
                    vader, "Disembark");
        var route = assessment.routes().stream()
                .filter(candidate ->
                    candidate.destination() == site)
                .findFirst().orElseThrow();

        assertTrue(route.admissible());
        assertTrue(route.guaranteesImmediateCapture());
    }

    @Test
    public void hostileSameSiteDisembarkCaptureRouteIsFormationBlocked() {
        Fixture fixture = new Fixture(false);
        PhysicalCard site = fixture.location(
                "1_291", 146, DARK);
        PhysicalCard carrier = fixture.active(
                "3_154", 147, DARK, site);
        PhysicalCard vader = fixture.cardAndAdd(
                "1_168", 148, DARK,
                Zone.AT_LOCATION, true);
        fixture.aboard(
                vader, carrier, site, carrier, null);
        PhysicalCard luke = fixture.active(
                "1_19", 149, LIGHT, site);
        fixture.persona(vader, Persona.VADER);
        fixture.persona(luke, Persona.LUKE);
        fixture.allowCaptureTarget(luke);
        when(fixture.modifiers.getTotalPowerAtLocation(
                fixture.gameState, site, LIGHT,
                false, false)).thenReturn(30.0f);
        when(fixture.modifiers.getTotalPowerAtLocation(
                fixture.gameState, site, DARK,
                false, false)).thenReturn(0.0f);

        var assessment =
                CaptureMovementMechanismFactsReader.assess(
                    fixture.game, DARK,
                    fixture.commonAnalyzer(false),
                    CaptureMovementMechanismFactsReader
                        .Mechanism.DISEMBARK,
                    vader, "Disembark");
        var route = assessment.routes().stream()
                .filter(candidate ->
                    candidate.destination() == site)
                .findFirst().orElseThrow();

        assertTrue(route.guaranteesImmediateCapture());
        assertTrue(route.formationBlocked());
        assertFalse(assessment.hasAdmissibleCaptureRoute());
    }

    @Test
    public void exactChildPromptsCoverEverySpecialMechanism() {
        assertEquals(
                CaptureMovementMechanismFactsReader
                    .ChoiceStep.DESTINATION,
                CaptureMovementMechanismFactsReader
                    .classifyChild(
                        CaptureMovementMechanismFactsReader
                            .Mechanism.DOCKING_BAY_TRANSIT,
                        "Choose docking bay to transit to"));
        assertEquals(
                CaptureMovementMechanismFactsReader
                    .ChoiceStep.MOVER,
                CaptureMovementMechanismFactsReader
                    .classifyChild(
                        CaptureMovementMechanismFactsReader
                            .Mechanism.VADERS_CASTLE,
                        "Choose card to move to <div>site</div>"));
        assertEquals(
                CaptureMovementMechanismFactsReader
                    .ChoiceStep.MOVER,
                CaptureMovementMechanismFactsReader
                    .classifyChild(
                        CaptureMovementMechanismFactsReader
                            .Mechanism.RISE_RECALL,
                        "Choose Vader to take into hand"));
        assertEquals(
                CaptureMovementMechanismFactsReader
                    .ChoiceStep.DESTINATION,
                CaptureMovementMechanismFactsReader
                    .classifyChild(
                        CaptureMovementMechanismFactsReader
                            .Mechanism.MACHINATION_RELOCATE,
                        "Choose site to relocate Vader to"));
        assertEquals(
                CaptureMovementMechanismFactsReader
                    .ChoiceStep.ORIGIN,
                CaptureMovementMechanismFactsReader
                    .classifyChild(
                        CaptureMovementMechanismFactsReader
                            .Mechanism.TRANSPORT,
                        "Choose site to 'transport' from"));
        assertEquals(
                CaptureMovementMechanismFactsReader
                    .ChoiceStep.UNKNOWN,
                CaptureMovementMechanismFactsReader
                    .classifyChild(
                        CaptureMovementMechanismFactsReader
                            .Mechanism.RISE_RECALL,
                        "Choose Vader"));
    }

    @Test
    public void castleCaptureRouteWinsBothCombinedEvaluators() {
        Fixture fixture = new Fixture(false);
        PhysicalCard castle = fixture.location(
                "209_50", 10, DARK);
        PhysicalCard captureSite = fixture.location(
                "1_290", 11, DARK);
        PhysicalCard vader = fixture.active(
                "1_168", 20, DARK, castle);
        PhysicalCard luke = fixture.active(
                "1_19", 21, LIGHT, captureSite);
        fixture.persona(vader, Persona.VADER);
        fixture.persona(luke, Persona.LUKE);
        fixture.allowCaptureTarget(luke);
        fixture.allowLocationTextMove(
                vader, castle, captureSite);

        ObjectiveAnalyzer common =
                fixture.commonAnalyzer(false);
        var assessment =
                CaptureMovementMechanismFactsReader.assess(
                    fixture.game, DARK, common,
                    CaptureMovementMechanismFactsReader
                        .Mechanism.VADERS_CASTLE,
                    castle,
                    "Move from here to other battleground site");
        assertTrue(assessment.factsKnown());
        assertTrue(assessment.hasAdmissibleCaptureRoute());
        assertTrue(assessment.routes().stream().anyMatch(
                route -> route.mover() == vader
                    && route.destination() == captureSite
                    && route.guaranteesImmediateCapture()
                    && route.formationSafe()));

        var randoContext =
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.DecisionContext(
                        fixture.gameState, DARK,
                        "ACTION_CHOICE", "Choose action",
                        "castle-rando", Phase.MOVE);
        randoContext.setGame(fixture.game);
        randoContext.setSide(Side.DARK);
        var randoAnalyzer = mock(
                com.gempukku.swccgo.ai.models.rando
                    .strategy.ObjectiveAnalyzer.class);
        stubAnalyzer(randoAnalyzer, false);
        randoContext.setObjectiveAnalyzer(randoAnalyzer);
        configureCastleDecision(
                randoContext, castle);
        var randoCastle =
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.CaptureMovementEvaluator()
                    .evaluate(randoContext).stream()
                    .filter(action ->
                        "castle".equals(action.getActionId()))
                    .findFirst().orElseThrow();
        var randoWinner =
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.CombinedEvaluator()
                    .evaluateDecision(randoContext);

        var chosenContext =
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.DecisionContext(
                        fixture.gameState, DARK,
                        "ACTION_CHOICE", "Choose action",
                        "castle-chosen", Phase.MOVE);
        chosenContext.setGame(fixture.game);
        chosenContext.setSide(Side.DARK);
        var chosenAnalyzer = mock(
                com.gempukku.swccgo.ai.models.chosenone
                    .strategy.ObjectiveAnalyzer.class);
        stubAnalyzer(chosenAnalyzer, false);
        chosenContext.setObjectiveAnalyzer(chosenAnalyzer);
        configureCastleDecision(
                chosenContext, castle);
        var chosenCastle =
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.CaptureMovementEvaluator()
                    .evaluate(chosenContext).stream()
                    .filter(action ->
                        "castle".equals(action.getActionId()))
                    .findFirst().orElseThrow();
        var chosenWinner =
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.CombinedEvaluator()
                    .evaluateDecision(chosenContext);

        assertEquals(300.0f, randoCastle.getScore(), 0.0f);
        assertEquals(300.0f, chosenCastle.getScore(), 0.0f);
        assertEquals(1, randoCastle.getReasoning().stream()
                .filter(reason ->
                    reason.contains("BHBM CAPTURE ROUTE"))
                .count());
        assertEquals(1, chosenCastle.getReasoning().stream()
                .filter(reason ->
                    reason.contains("BHBM CAPTURE ROUTE"))
                .count());
        assertEquals("castle", randoWinner.getActionId());
        assertEquals("castle", chosenWinner.getActionId());
        assertTrue(randoWinner.getScore() > 8.0f);
        assertEquals(
                randoWinner.getScore(),
                chosenWinner.getScore(), 0.0f);
    }

    @Test
    public void castleApproachRouteCarriesBoundedPreferenceWithoutPretendingItIsLandspeed() {
        Fixture fixture = new Fixture(false);
        PhysicalCard castle = fixture.location(
                "209_50", 112, DARK);
        PhysicalCard halfway = fixture.location(
                "1_295", 113, DARK);
        PhysicalCard captureSite = fixture.location(
                "1_290", 114, DARK);
        PhysicalCard vader = fixture.active(
                "1_168", 115, DARK, castle);
        PhysicalCard decoyMover = fixture.active(
                "1_169", 116, DARK, castle);
        PhysicalCard luke = fixture.active(
                "1_19", 117, LIGHT, captureSite);
        fixture.persona(vader, Persona.VADER);
        fixture.persona(luke, Persona.LUKE);
        fixture.allowCaptureTarget(luke);
        fixture.allowLocationTextMove(
                vader, castle, halfway);
        when(fixture.modifiers.isBattleground(
                fixture.gameState, captureSite, null))
            .thenReturn(false);
        when(fixture.modifiers.getSitesBetween(
                fixture.gameState, castle, captureSite))
            .thenReturn(List.of(halfway));
        when(fixture.modifiers.getLandspeedRequired(
                fixture.gameState, vader, halfway))
            .thenReturn(null);

        assertFalse(CaptureObjectiveFacts
                .advancesCaptureApproachByLandspeed(
                    fixture.game, DARK,
                    fixture.commonAnalyzer(false),
                    vader, halfway));
        assertTrue(CaptureObjectiveFacts
                .advancesCaptureApproachAt(
                    fixture.game, DARK,
                    fixture.commonAnalyzer(false),
                    vader, castle, halfway));
        var assessment =
                CaptureMovementMechanismFactsReader.assess(
                    fixture.game, DARK,
                    fixture.commonAnalyzer(false),
                    CaptureMovementMechanismFactsReader
                        .Mechanism.VADERS_CASTLE,
                    castle,
                    "Move from here to other battleground site");
        assertFalse(assessment.hasAdmissibleCaptureRoute());
        assertTrue(assessment.hasAdmissibleApproachRoute());
        assertTrue(assessment.routes().stream().anyMatch(
                route -> route.mover() == vader
                    && route.destination() == halfway
                    && route.advancesCaptureApproach()
                    && route.admissible()));

        var randoAnalyzer = mock(
                com.gempukku.swccgo.ai.models.rando
                    .strategy.ObjectiveAnalyzer.class);
        stubAnalyzer(randoAnalyzer, false);
        var chosenAnalyzer = mock(
                com.gempukku.swccgo.ai.models.chosenone
                    .strategy.ObjectiveAnalyzer.class);
        stubAnalyzer(chosenAnalyzer, false);
        var rando = new com.gempukku.swccgo.ai.models.rando
                .evaluators.CombinedEvaluator();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone
                .evaluators.CombinedEvaluator();

        var randoParent =
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.DecisionContext(
                        fixture.gameState, DARK,
                        "ACTION_CHOICE", "Choose action",
                        "castle-approach-parent-rando",
                        Phase.MOVE);
        randoParent.setGame(fixture.game);
        randoParent.setSide(Side.DARK);
        randoParent.setObjectiveAnalyzer(randoAnalyzer);
        configureCastleDecision(randoParent, castle);
        var chosenParent =
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.DecisionContext(
                        fixture.gameState, DARK,
                        "ACTION_CHOICE", "Choose action",
                        "castle-approach-parent-chosen",
                        Phase.MOVE);
        chosenParent.setGame(fixture.game);
        chosenParent.setSide(Side.DARK);
        chosenParent.setObjectiveAnalyzer(chosenAnalyzer);
        configureCastleDecision(chosenParent, castle);
        var randoParentWinner =
                rando.evaluateDecision(randoParent);
        var chosenParentWinner =
                chosen.evaluateDecision(chosenParent);
        assertEquals("castle",
                randoParentWinner.getActionId());
        assertEquals("castle",
                chosenParentWinner.getActionId());
        assertTrue(randoParentWinner.getReasoningString()
                .contains("ACTOR_ROUTE_START"));
        assertTrue(chosenParentWinner.getReasoningString()
                .contains("ACTOR_ROUTE_START"));

        fixture.liveGameTextAction(
                castle,
                "Move from here to other battleground site");
        var randoOrigin = randoSelection(
                fixture, randoAnalyzer,
                "Choose card to move from",
                "castle-approach-origin-rando",
                List.of(captureSite, castle));
        var chosenOrigin = chosenSelection(
                fixture, chosenAnalyzer,
                "Choose card to move from",
                "castle-approach-origin-chosen",
                List.of(captureSite, castle));
        var randoOriginPreference =
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.CaptureMovementEvaluator()
                    .evaluate(randoOrigin).stream()
                    .filter(action -> String.valueOf(castle.getCardId())
                        .equals(action.getActionId()))
                    .findFirst().orElseThrow();
        var chosenOriginPreference =
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.CaptureMovementEvaluator()
                    .evaluate(chosenOrigin).stream()
                    .filter(action -> String.valueOf(castle.getCardId())
                        .equals(action.getActionId()))
                    .findFirst().orElseThrow();
        assertEquals(300.0f,
                randoOriginPreference.getScore(), 0.0f);
        assertEquals(randoOriginPreference.getScore(),
                chosenOriginPreference.getScore(), 0.0f);
        assertTrue(randoOriginPreference.getReasoningString()
                .contains("ACTOR_ROUTE_DESTINATION"));

        var randoDestination = randoSelection(
                fixture, randoAnalyzer,
                "Choose card to move to",
                "castle-approach-destination-rando",
                List.of(captureSite, halfway));
        var chosenDestination = chosenSelection(
                fixture, chosenAnalyzer,
                "Choose card to move to",
                "castle-approach-destination-chosen",
                List.of(captureSite, halfway));
        randoDestination.setExtra(
                CaptureMovementMechanismFactsReader
                    .SELECTED_ORIGIN_CARD_ID_EXTRA,
                castle.getCardId());
        chosenDestination.setExtra(
                CaptureMovementMechanismFactsReader
                    .SELECTED_ORIGIN_CARD_ID_EXTRA,
                castle.getCardId());
        var randoHalfwayPreference =
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.CaptureMovementEvaluator()
                    .evaluate(randoDestination).stream()
                    .filter(action -> String.valueOf(halfway.getCardId())
                        .equals(action.getActionId()))
                    .findFirst().orElseThrow();
        var chosenHalfwayPreference =
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.CaptureMovementEvaluator()
                    .evaluate(chosenDestination).stream()
                    .filter(action -> String.valueOf(halfway.getCardId())
                        .equals(action.getActionId()))
                    .findFirst().orElseThrow();
        assertEquals(300.0f,
                randoHalfwayPreference.getScore(), 0.0f);
        assertEquals(randoHalfwayPreference.getScore(),
                chosenHalfwayPreference.getScore(), 0.0f);
        assertTrue(randoHalfwayPreference.getReasoningString()
                .contains("ACTOR_ROUTE_DESTINATION"));
        assertTrue(chosenHalfwayPreference.getReasoningString()
                .contains("ACTOR_ROUTE_DESTINATION"));
        var randoDestinationWinner =
                rando.evaluateDecision(randoDestination);
        var chosenDestinationWinner =
                chosen.evaluateDecision(chosenDestination);
        assertEquals(
                String.valueOf(captureSite.getCardId()),
                randoDestinationWinner.getActionId());
        assertEquals(
                String.valueOf(captureSite.getCardId()),
                chosenDestinationWinner.getActionId());

        String moverPrompt =
                "Choose card to move to "
                    + com.gempukku.swccgo.logic.GameUtils
                        .getCardLink(halfway);
        var randoMover = randoSelection(
                fixture, randoAnalyzer,
                moverPrompt,
                "castle-approach-mover-rando",
                List.of(decoyMover, vader));
        var chosenMover = chosenSelection(
                fixture, chosenAnalyzer,
                moverPrompt,
                "castle-approach-mover-chosen",
                List.of(decoyMover, vader));
        assertEquals(
                String.valueOf(vader.getCardId()),
                rando.evaluateDecision(randoMover)
                    .getActionId());
        assertEquals(
                String.valueOf(vader.getCardId()),
                chosen.evaluateDecision(chosenMover)
                    .getActionId());
    }

    @Test
    public void castleRouteUsesTheActionsRealOneForceBaseCost() {
        Fixture fixture = new Fixture(false);
        PhysicalCard castle = fixture.location(
                "209_50", 22, DARK);
        PhysicalCard captureSite = fixture.location(
                "1_290", 23, DARK);
        PhysicalCard vader = fixture.active(
                "1_168", 24, DARK, castle);
        PhysicalCard luke = fixture.active(
                "1_19", 25, LIGHT, captureSite);
        fixture.persona(vader, Persona.VADER);
        fixture.persona(luke, Persona.LUKE);
        fixture.allowCaptureTarget(luke);
        fixture.allowLocationTextMove(
                vader, castle, captureSite);
        when(fixture.modifiers.getForceAvailableToUse(
                fixture.gameState, DARK)).thenReturn(0);
        when(fixture.modifiers.getMoveUsingLocationTextCost(
                any(GameState.class),
                any(PhysicalCard.class),
                any(PhysicalCard.class),
                any(PhysicalCard.class),
                anyFloat(), anyFloat()))
                .thenAnswer(invocation ->
                    invocation.getArgument(4));

        assertTrue(
                com.gempukku.swccgo.filters.Filters
                    .canMoveToUsingLocationText(
                        vader, false, 0.0f, 0.0f)
                    .accepts(
                        fixture.gameState,
                        fixture.modifiers, captureSite));

        var assessment =
                CaptureMovementMechanismFactsReader.assess(
                    fixture.game, DARK,
                    fixture.commonAnalyzer(false),
                    CaptureMovementMechanismFactsReader
                        .Mechanism.VADERS_CASTLE,
                    castle,
                    "Move from here to other battleground site");

        assertTrue(assessment.factsKnown());
        assertTrue(assessment.routes().isEmpty());
        assertFalse(assessment.hasAdmissibleCaptureRoute());
    }

    @Test
    public void castleInboundRejectsANonBattlegroundOrigin() {
        Fixture fixture = new Fixture(false);
        PhysicalCard castle = fixture.location(
                "209_50", 108, DARK);
        PhysicalCard interiorOrigin = fixture.location(
                "1_290", 109, DARK);
        PhysicalCard vader = fixture.active(
                "1_168", 110, DARK, interiorOrigin);
        PhysicalCard luke = fixture.active(
                "1_19", 111, LIGHT, castle);
        fixture.persona(vader, Persona.VADER);
        fixture.persona(luke, Persona.LUKE);
        fixture.allowCaptureTarget(luke);
        fixture.allowLocationTextMove(
                vader, interiorOrigin, castle);
        when(fixture.modifiers.isBattleground(
                fixture.gameState, interiorOrigin, null))
            .thenReturn(false);

        var assessment =
                CaptureMovementMechanismFactsReader.assess(
                    fixture.game, DARK,
                    fixture.commonAnalyzer(false),
                    CaptureMovementMechanismFactsReader
                        .Mechanism.VADERS_CASTLE,
                    castle,
                    "Move from other battleground site to here");

        assertTrue(assessment.factsKnown());
        assertTrue(assessment.routes().isEmpty());
        assertFalse(assessment.hasAdmissibleCaptureRoute());
    }

    @Test
    public void castleOriginDestinationAndMoverChainWinsBothBots() {
        Fixture fixture = new Fixture(false);
        PhysicalCard castle = fixture.location(
                "209_50", 72, DARK);
        PhysicalCard captureSite = fixture.location(
                "1_290", 73, DARK);
        PhysicalCard decoySite = fixture.location(
                "1_291", 74, DARK);
        PhysicalCard vader = fixture.active(
                "1_168", 75, DARK, castle);
        PhysicalCard decoyMover = fixture.active(
                "1_169", 76, DARK, castle);
        PhysicalCard luke = fixture.active(
                "1_19", 77, LIGHT, captureSite);
        fixture.persona(vader, Persona.VADER);
        fixture.persona(luke, Persona.LUKE);
        fixture.allowCaptureTarget(luke);
        fixture.allowLocationTextMove(
                vader, castle, captureSite);
        fixture.allowLocationTextMove(
                vader, castle, decoySite);
        fixture.liveGameTextAction(
                castle,
                "Move from here to other battleground site");

        var randoAnalyzer = mock(
                com.gempukku.swccgo.ai.models.rando
                    .strategy.ObjectiveAnalyzer.class);
        stubAnalyzer(randoAnalyzer, false);
        var chosenAnalyzer = mock(
                com.gempukku.swccgo.ai.models.chosenone
                    .strategy.ObjectiveAnalyzer.class);
        stubAnalyzer(chosenAnalyzer, false);
        var rando = new com.gempukku.swccgo.ai.models.rando
                .evaluators.CombinedEvaluator();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone
                .evaluators.CombinedEvaluator();

        var randoOrigin = randoSelection(
                fixture, randoAnalyzer,
                "Choose card to move from",
                "castle-origin-rando",
                List.of(decoySite, castle));
        var chosenOrigin = chosenSelection(
                fixture, chosenAnalyzer,
                "Choose card to move from",
                "castle-origin-chosen",
                List.of(decoySite, castle));
        assertEquals(
                String.valueOf(castle.getCardId()),
                rando.evaluateDecision(randoOrigin)
                    .getActionId());
        assertEquals(
                String.valueOf(castle.getCardId()),
                chosen.evaluateDecision(chosenOrigin)
                    .getActionId());

        var randoDestination = randoSelection(
                fixture, randoAnalyzer,
                "Choose card to move to",
                "castle-destination-rando",
                List.of(decoySite, captureSite));
        var chosenDestination = chosenSelection(
                fixture, chosenAnalyzer,
                "Choose card to move to",
                "castle-destination-chosen",
                List.of(decoySite, captureSite));
        assertEquals(
                String.valueOf(captureSite.getCardId()),
                rando.evaluateDecision(randoDestination)
                    .getActionId());
        assertEquals(
                String.valueOf(captureSite.getCardId()),
                chosen.evaluateDecision(chosenDestination)
                    .getActionId());

        String moverPrompt =
                "Choose card to move to "
                    + com.gempukku.swccgo.logic.GameUtils
                        .getCardLink(captureSite);
        var randoMover = randoSelection(
                fixture, randoAnalyzer,
                moverPrompt, "castle-mover-rando",
                List.of(decoyMover, vader));
        var chosenMover = chosenSelection(
                fixture, chosenAnalyzer,
                moverPrompt, "castle-mover-chosen",
                List.of(decoyMover, vader));
        assertEquals(
                String.valueOf(vader.getCardId()),
                rando.evaluateDecision(randoMover)
                    .getActionId());
        assertEquals(
                String.valueOf(vader.getCardId()),
                chosen.evaluateDecision(chosenMover)
                    .getActionId());
    }

    @Test
    public void stableBackRecallIsARealHardStopFact() {
        Fixture fixture = new Fixture(true);
        PhysicalCard site = fixture.location(
                "1_290", 30, DARK);
        PhysicalCard vader = fixture.active(
                "1_168", 31, DARK, site);
        PhysicalCard luke = fixture.active(
                "1_19", 32, LIGHT, site);
        PhysicalCard rise = fixture.cardAndAdd(
                "9_140", 33, DARK, Zone.HAND, false);
        fixture.persona(vader, Persona.VADER);
        fixture.persona(luke, Persona.LUKE);
        fixture.control(site);

        var assessment =
                CaptureMovementMechanismFactsReader.assess(
                    fixture.game, DARK,
                    fixture.commonAnalyzer(true),
                    CaptureMovementMechanismFactsReader
                        .Mechanism.RISE_RECALL,
                    rise, "Take Vader into hand");

        assertTrue(assessment.factsKnown());
        assertEquals(1, assessment.routes().size());
        assertTrue(assessment.routes().get(0)
                .breaksStableBack());
        assertFalse(assessment.routes().get(0)
                .admissible());
    }

    @Test
    public void riseRecallChildHoldsOnlyTheStableBackVader() {
        Fixture fixture = new Fixture(true);
        PhysicalCard stableSite = fixture.location(
                "1_290", 34, DARK);
        PhysicalCard safeSite = fixture.location(
                "4_163", 35, DARK);
        PhysicalCard stableVader = fixture.active(
                "1_168", 36, DARK, stableSite);
        PhysicalCard safeVader = fixture.active(
                "7_175", 37, DARK, safeSite);
        PhysicalCard luke = fixture.active(
                "1_19", 38, LIGHT, stableSite);
        PhysicalCard rise = fixture.cardAndAdd(
                "9_140", 39, DARK, Zone.HAND, false);
        fixture.persona(stableVader, Persona.VADER);
        fixture.persona(safeVader, Persona.VADER);
        fixture.persona(luke, Persona.LUKE);
        fixture.control(stableSite);
        fixture.control(safeSite);
        fixture.livePlayAction(
                rise, "Take Vader into hand");

        var randoContext =
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.DecisionContext(
                        fixture.gameState, DARK,
                        "CARD_SELECTION",
                        "Choose Vader to take into hand",
                        "rise-rando", Phase.MOVE);
        randoContext.setGame(fixture.game);
        randoContext.setSide(Side.DARK);
        var randoAnalyzer = mock(
                com.gempukku.swccgo.ai.models.rando
                    .strategy.ObjectiveAnalyzer.class);
        stubAnalyzer(randoAnalyzer, true);
        randoContext.setObjectiveAnalyzer(randoAnalyzer);
        configureCardSelection(
                randoContext,
                List.of(stableVader, safeVader));
        var randoActions =
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.CaptureMovementEvaluator()
                    .evaluate(randoContext);

        var chosenContext =
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.DecisionContext(
                        fixture.gameState, DARK,
                        "CARD_SELECTION",
                        "Choose Vader to take into hand",
                        "rise-chosen", Phase.MOVE);
        chosenContext.setGame(fixture.game);
        chosenContext.setSide(Side.DARK);
        var chosenAnalyzer = mock(
                com.gempukku.swccgo.ai.models.chosenone
                    .strategy.ObjectiveAnalyzer.class);
        stubAnalyzer(chosenAnalyzer, true);
        chosenContext.setObjectiveAnalyzer(chosenAnalyzer);
        configureCardSelection(
                chosenContext,
                List.of(stableVader, safeVader));
        var chosenActions =
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.CaptureMovementEvaluator()
                    .evaluate(chosenContext);

        assertEquals(2, randoActions.size());
        assertEquals(2, chosenActions.size());
        var randoStable = randoActions.stream()
                .filter(action -> String.valueOf(stableVader.getCardId())
                    .equals(action.getActionId()))
                .findFirst().orElseThrow();
        var chosenStable = chosenActions.stream()
                .filter(action -> String.valueOf(stableVader.getCardId())
                    .equals(action.getActionId()))
                .findFirst().orElseThrow();
        var randoSafe = randoActions.stream()
                .filter(action -> String.valueOf(safeVader.getCardId())
                    .equals(action.getActionId()))
                .findFirst().orElseThrow();
        var chosenSafe = chosenActions.stream()
                .filter(action -> String.valueOf(safeVader.getCardId())
                    .equals(action.getActionId()))
                .findFirst().orElseThrow();
        assertFalse(randoStable.isHardVetoed());
        assertFalse(randoSafe.isHardVetoed());
        assertEquals(-300.0f,
                randoStable.getScore(), 0.0f);
        assertEquals(randoStable.getScore(),
                chosenStable.getScore(), 0.0f);
        assertEquals(randoSafe.getScore(),
                chosenSafe.getScore(), 0.0f);
        assertTrue(randoStable.getReasoningString()
                .contains("BHBM HOLD"));
        assertFalse(randoSafe.getReasoningString()
                .contains("BHBM HOLD"));
        var stableBackPreference = CaptureObjectivePolicy
                .scoreStableBackHold(
                    new CaptureObjectivePolicy.StableBackFacts(
                        String.valueOf(stableVader.getCardId()),
                        CaptureObjectivePolicy.ObjectiveKind.BHBM,
                        true, true, true));
        assertEquals(1, stableBackPreference.operations().size());
        assertEquals(-300.0f,
                stableBackPreference.operations().getFirst().delta(),
                0.0f);
        assertEquals(
                com.gempukku.swccgo.ai.models.common.trace.TraceDomainId
                    .OBJECTIVE_INTENT,
                stableBackPreference.operations().getFirst().domainId());
    }

    @Test
    public void riseRecallRemovalStillObeysFormationSafety() {
        Fixture fixture = new Fixture(false);
        PhysicalCard site = fixture.location(
                "1_290", 41, DARK);
        PhysicalCard vader = fixture.active(
                "1_168", 42, DARK, site);
        fixture.active("1_169", 43, DARK, site);
        fixture.active("1_19", 44, LIGHT, site);
        PhysicalCard rise = fixture.cardAndAdd(
                "9_140", 45, DARK, Zone.HAND, false);
        fixture.persona(vader, Persona.VADER);
        fixture.control(site);
        when(fixture.modifiers.getTotalPowerAtLocation(
                fixture.gameState, site, DARK,
                false, false)).thenReturn(5.0f);
        when(fixture.modifiers.getTotalPowerAtLocation(
                fixture.gameState, site, LIGHT,
                false, false)).thenReturn(10.0f);

        var assessment =
                CaptureMovementMechanismFactsReader.assess(
                    fixture.game, DARK,
                    fixture.commonAnalyzer(false),
                    CaptureMovementMechanismFactsReader
                        .Mechanism.RISE_RECALL,
                    rise, "Take Vader into hand");

        var vaderRoute = assessment.routes().stream()
                .filter(route -> route.mover() == vader)
                .findFirst().orElseThrow();
        assertFalse(vaderRoute.formationSafe());
        assertFalse(vaderRoute.admissible());
    }

    @Test
    public void mixedStableAndFormationFailuresCreateTheCombinedHold() {
        Fixture fixture = new Fixture(true);
        PhysicalCard stableSite = fixture.location(
                "1_290", 120, DARK);
        PhysicalCard unsafeSite = fixture.location(
                "4_163", 121, DARK);
        PhysicalCard stableVader = fixture.active(
                "1_168", 122, DARK, stableSite);
        PhysicalCard unsafeVader = fixture.active(
                "7_175", 123, DARK, unsafeSite);
        PhysicalCard luke = fixture.active(
                "1_19", 124, LIGHT, stableSite);
        fixture.active(
                "1_169", 127, DARK, unsafeSite);
        fixture.active(
                "1_17", 125, LIGHT, unsafeSite);
        PhysicalCard rise = fixture.cardAndAdd(
                "9_140", 126, DARK, Zone.HAND, false);
        fixture.persona(stableVader, Persona.VADER);
        fixture.persona(unsafeVader, Persona.VADER);
        fixture.persona(luke, Persona.LUKE);
        fixture.control(stableSite);
        fixture.control(unsafeSite);
        when(fixture.modifiers.getTotalPowerAtLocation(
                fixture.gameState, unsafeSite, DARK,
                false, false)).thenReturn(5.0f);
        when(fixture.modifiers.getTotalPowerAtLocation(
                fixture.gameState, unsafeSite, LIGHT,
                false, false)).thenReturn(10.0f);

        var assessment =
                CaptureMovementMechanismFactsReader.assess(
                    fixture.game, DARK,
                    fixture.commonAnalyzer(true),
                    CaptureMovementMechanismFactsReader
                        .Mechanism.RISE_RECALL,
                    rise, "Take Vader into hand");
        assertEquals(2, assessment.routes().size());
        assertTrue(assessment.routes().stream().anyMatch(
                route -> route.mover() == stableVader
                    && route.breaksStableBack()));
        assertTrue(assessment.routes().stream().anyMatch(
                route -> route.mover() == unsafeVader
                    && route.formationBlocked()
                    && !route.breaksStableBack()));

        var randoAnalyzer = mock(
                com.gempukku.swccgo.ai.models.rando
                    .strategy.ObjectiveAnalyzer.class);
        stubAnalyzer(randoAnalyzer, true);
        var randoContext =
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.DecisionContext(
                        fixture.gameState, DARK,
                        "ACTION_CHOICE", "Choose action",
                        "mixed-route-rando", Phase.MOVE);
        randoContext.setGame(fixture.game);
        randoContext.setSide(Side.DARK);
        randoContext.setObjectiveAnalyzer(randoAnalyzer);
        randoContext.setActionIds(
                List.of("recall", ""));
        randoContext.setActionTexts(
                List.of("Take Vader into hand", "Pass"));
        randoContext.setCardIds(List.of(
                String.valueOf(rise.getCardId()), ""));
        randoContext.setNoPass(false);
        var randoActions =
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.CaptureMovementEvaluator()
                    .evaluate(randoContext);

        var chosenAnalyzer = mock(
                com.gempukku.swccgo.ai.models.chosenone
                    .strategy.ObjectiveAnalyzer.class);
        stubAnalyzer(chosenAnalyzer, true);
        var chosenContext =
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.DecisionContext(
                        fixture.gameState, DARK,
                        "ACTION_CHOICE", "Choose action",
                        "mixed-route-chosen", Phase.MOVE);
        chosenContext.setGame(fixture.game);
        chosenContext.setSide(Side.DARK);
        chosenContext.setObjectiveAnalyzer(chosenAnalyzer);
        chosenContext.setActionIds(
                List.of("recall", ""));
        chosenContext.setActionTexts(
                List.of("Take Vader into hand", "Pass"));
        chosenContext.setCardIds(List.of(
                String.valueOf(rise.getCardId()), ""));
        chosenContext.setNoPass(false);
        var chosenActions =
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.CaptureMovementEvaluator()
                    .evaluate(chosenContext);

        assertEquals(1, randoActions.size());
        assertEquals(1, chosenActions.size());
        assertFalse(randoActions.get(0).isHardVetoed());
        assertFalse(chosenActions.get(0).isHardVetoed());
        assertEquals(-300.0f,
                randoActions.get(0).getScore(), 0.0f);
        assertTrue(randoActions.get(0).getReasoningString()
                .contains("ROUTE_HOLD"));
        assertEquals(
                randoActions.get(0).getReasoning(),
                chosenActions.get(0).getReasoning());
    }

    @Test
    public void riseRelocateOnlyReconstructsTheSourceEligibleVader() {
        Fixture fixture = new Fixture(false);
        PhysicalCard controlledSite = fixture.location(
                "1_290", 63, DARK);
        PhysicalCard deathStarDockingBay =
                fixture.location("9_145", 64, DARK);
        PhysicalCard eligibleVader = fixture.active(
                "1_168", 65, DARK, controlledSite);
        PhysicalCard ineligibleVader = fixture.active(
                "7_175", 66, DARK, controlledSite);
        PhysicalCard captiveLuke = fixture.active(
                "1_19", 67, LIGHT, controlledSite);
        PhysicalCard rise = fixture.cardAndAdd(
                "9_140", 68, DARK, Zone.HAND, false);
        fixture.persona(eligibleVader, Persona.VADER);
        fixture.persona(ineligibleVader, Persona.VADER);
        fixture.persona(captiveLuke, Persona.LUKE);
        fixture.control(controlledSite);
        when(fixture.gameState.getCaptivesOfEscort(
                eligibleVader))
                .thenReturn(List.of(captiveLuke));
        when(fixture.gameState.getCaptivesOfEscort(
                ineligibleVader))
                .thenReturn(List.of());
        assertTrue(
                com.gempukku.swccgo.filters.Filters
                    .Death_Star_II_Docking_Bay.accepts(
                        fixture.gameState,
                        fixture.modifiers,
                        deathStarDockingBay));
        assertTrue(
                com.gempukku.swccgo.filters.Filters
                    .escorting(
                        com.gempukku.swccgo.filters.Filters
                            .or(
                                com.gempukku.swccgo.filters
                                    .Filters.Luke,
                                com.gempukku.swccgo.filters
                                    .Filters.Leia))
                    .accepts(
                        fixture.gameState,
                        fixture.modifiers,
                        eligibleVader));
        assertTrue(
                com.gempukku.swccgo.filters.Filters
                    .at(com.gempukku.swccgo.filters.Filters
                        .and(
                            com.gempukku.swccgo.filters
                                .Filters.site,
                            com.gempukku.swccgo.filters
                                .Filters.controls(DARK)))
                    .accepts(
                        fixture.gameState,
                        fixture.modifiers,
                        eligibleVader));
        assertTrue(
                eligibleVader.getBlueprint()
                    .getValidMoveTargetFilter(
                        DARK, fixture.game,
                        eligibleVader, false)
                    .accepts(
                        fixture.gameState,
                        fixture.modifiers,
                        deathStarDockingBay));
        assertFalse(
                fixture.modifiers
                    .mayNotRelocateFromLocationToLocation(
                        fixture.gameState,
                        eligibleVader,
                        controlledSite,
                        deathStarDockingBay,
                        false, false));
        assertEquals(
                0.0f,
                fixture.modifiers
                    .getRelocateBetweenLocationsCost(
                        fixture.gameState,
                        eligibleVader,
                        controlledSite,
                        deathStarDockingBay,
                        0.0f),
                0.0f);
        assertTrue(
                com.gempukku.swccgo.filters.Filters
                    .canBeRelocatedToLocation(
                        deathStarDockingBay,
                        false, true, false,
                        0.0f, false)
                    .accepts(
                        fixture.gameState,
                        fixture.modifiers,
                        eligibleVader));

        var assessment =
                CaptureMovementMechanismFactsReader.assess(
                    fixture.game, DARK,
                    fixture.commonAnalyzer(false),
                    CaptureMovementMechanismFactsReader
                        .Mechanism.RISE_RELOCATE,
                    rise,
                    "Relocate Vader to Death Star II: Docking Bay");

        assertTrue(assessment.factsKnown());
        assertEquals(1, assessment.routes().size());
        assertTrue(
                assessment.routes().get(0).mover()
                    == eligibleVader);
        assertTrue(
                assessment.routes().get(0).destination()
                    == deathStarDockingBay);
    }

    @Test
    public void unknownFormationNeverBecomesAFormationHardBlock() {
        PhysicalCard mover = card(
                "1_168", 69, DARK, Zone.AT_LOCATION);
        PhysicalCard destination = card(
                "1_290", 70, DARK, Zone.LOCATIONS);
        var route =
                new CaptureMovementMechanismFactsReader.Route(
                    mover, null, destination, null,
                    true, false, false,
                    false, false, false);
        var stableBlocked =
                new CaptureMovementMechanismFactsReader.Route(
                    mover, destination, destination,
                    destination, true, true, true,
                    false, false, true);
        var formationBlocked =
                new CaptureMovementMechanismFactsReader.Route(
                    mover, destination, destination,
                    destination, true, true, false,
                    false, false, false);

        assertFalse(route.formationBlocked());
        assertFalse(route.hardBlocked());
        assertFalse(route.admissible());
        assertTrue(stableBlocked.hardBlocked());
        assertTrue(formationBlocked.hardBlocked());
        assertFalse(List.of(stableBlocked, route).stream()
                .allMatch(
                    CaptureMovementMechanismFactsReader
                        .Route::hardBlocked));
    }

    @Test
    public void machinationOnlyUsesItsBidirectionalSourceRoute() {
        Fixture fixture = new Fixture(false);
        PhysicalCard machination = fixture.cardAndAdd(
                "222_16", 46, DARK,
                Zone.SIDE_OF_TABLE, true);
        PhysicalCard walkway = fixture.location(
                "5_167", 47, DARK);
        PhysicalCard opponentBattleground =
                fixture.location(
                    "5_79", 48, LIGHT);
        fixture.location("1_290", 49, DARK);
        PhysicalCard vader = fixture.active(
                "1_168", 55, DARK, walkway);
        fixture.persona(vader, Persona.VADER);
        assertTrue(
                com.gempukku.swccgo.filters.Filters
                    .Chasm_Walkway.accepts(
                        fixture.gameState,
                        fixture.modifiers, walkway));
        assertTrue(
                com.gempukku.swccgo.filters.Filters
                    .and(
                        com.gempukku.swccgo.filters.Filters
                            .opponents(DARK),
                        com.gempukku.swccgo.filters.Filters
                            .battleground_site)
                    .accepts(
                        fixture.gameState,
                        fixture.modifiers,
                        opponentBattleground));
        assertTrue(
                com.gempukku.swccgo.filters.Filters
                    .locationCanBeRelocatedTo(
                        vader, true, 0.0f)
                    .accepts(
                        fixture.gameState,
                        fixture.modifiers,
                        opponentBattleground));

        var outbound =
                CaptureMovementMechanismFactsReader.assess(
                    fixture.game, DARK,
                    fixture.commonAnalyzer(false),
                    CaptureMovementMechanismFactsReader
                        .Mechanism.MACHINATION_RELOCATE,
                    machination, "Relocate Vader to site");

        assertFalse(outbound.routes().isEmpty());
        assertTrue(outbound.routes().stream().allMatch(
                route -> route.mover() == vader
                    && route.destination()
                        == opponentBattleground));
    }

    @Test
    public void machinationDestinationChoosesTheImmediateCaptureSite() {
        Fixture fixture = new Fixture(false);
        PhysicalCard machination = fixture.cardAndAdd(
                "222_16", 78, DARK,
                Zone.SIDE_OF_TABLE, true);
        PhysicalCard walkway = fixture.location(
                "5_167", 79, DARK);
        PhysicalCard captureSite = fixture.location(
                "5_79", 80, LIGHT);
        PhysicalCard decoySite = fixture.location(
                "4_163", 81, LIGHT);
        PhysicalCard vader = fixture.active(
                "1_168", 82, DARK, walkway);
        PhysicalCard luke = fixture.active(
                "1_19", 83, LIGHT, captureSite);
        fixture.persona(vader, Persona.VADER);
        fixture.persona(luke, Persona.LUKE);
        fixture.allowCaptureTarget(luke);
        fixture.liveGameTextAction(
                machination, "Relocate Vader to site");

        var randoAnalyzer = mock(
                com.gempukku.swccgo.ai.models.rando
                    .strategy.ObjectiveAnalyzer.class);
        stubAnalyzer(randoAnalyzer, false);
        var chosenAnalyzer = mock(
                com.gempukku.swccgo.ai.models.chosenone
                    .strategy.ObjectiveAnalyzer.class);
        stubAnalyzer(chosenAnalyzer, false);
        var randoContext = randoSelection(
                fixture, randoAnalyzer,
                "Choose site to relocate Vader to",
                "machination-destination-rando",
                List.of(decoySite, captureSite));
        var chosenContext = chosenSelection(
                fixture, chosenAnalyzer,
                "Choose site to relocate Vader to",
                "machination-destination-chosen",
                List.of(decoySite, captureSite));

        var randoWinner =
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.CombinedEvaluator()
                    .evaluateDecision(randoContext);
        var chosenWinner =
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.CombinedEvaluator()
                    .evaluateDecision(chosenContext);

        assertEquals(
                String.valueOf(captureSite.getCardId()),
                randoWinner.getActionId());
        assertEquals(
                randoWinner.getActionId(),
                chosenWinner.getActionId());
        assertEquals(
                randoWinner.getScore(),
                chosenWinner.getScore(), 0.0f);
    }

    @Test
    public void eachTransportSourceKeepsItsOwnSiteContract() {
        Fixture fixture = new Fixture(false);
        PhysicalCard exteriorOrigin = fixture.location(
                "7_276", 56, DARK);
        PhysicalCard exteriorDestination =
                fixture.location("1_291", 57, DARK);
        PhysicalCard interiorBattleground =
                fixture.location("1_290", 58, DARK);
        PhysicalCard vader = fixture.active(
                "1_168", 59, DARK, exteriorOrigin);
        fixture.persona(vader, Persona.VADER);
        PhysicalCard elis = fixture.cardAndAdd(
                "1_243", 60, DARK, Zone.HAND, false);
        PhysicalCard lana = fixture.cardAndAdd(
                "12_150", 61, DARK, Zone.HAND, false);
        PhysicalCard combo = fixture.cardAndAdd(
                "209_48", 62, DARK, Zone.HAND, false);
        assertTrue(
                com.gempukku.swccgo.filters.Filters
                    .exterior_site.accepts(
                        fixture.gameState,
                        fixture.modifiers,
                        exteriorOrigin));
        assertTrue(
                com.gempukku.swccgo.filters.Filters
                    .exterior_site.accepts(
                        fixture.gameState,
                        fixture.modifiers,
                        exteriorDestination));
        assertTrue(
                com.gempukku.swccgo.filters.Filters
                    .locationCanBeRelocatedTo(
                        vader, true, 0.0f)
                    .accepts(
                        fixture.gameState,
                        fixture.modifiers,
                        exteriorDestination));

        var lanaRoutes =
                CaptureMovementMechanismFactsReader.assess(
                    fixture.game, DARK,
                    fixture.commonAnalyzer(false),
                    CaptureMovementMechanismFactsReader
                        .Mechanism.TRANSPORT,
                    lana, "'Transport' characters");
        assertTrue(lanaRoutes.routes().stream().anyMatch(
                route -> route.destination()
                    == exteriorDestination));
        assertFalse(lanaRoutes.routes().stream().anyMatch(
                route -> route.destination()
                    == interiorBattleground));

        var comboRoutes =
                CaptureMovementMechanismFactsReader.assess(
                    fixture.game, DARK,
                    fixture.commonAnalyzer(false),
                    CaptureMovementMechanismFactsReader
                        .Mechanism.TRANSPORT,
                    combo, "'Transport' characters");
        assertTrue(comboRoutes.routes().stream().anyMatch(
                route -> route.destination()
                    == interiorBattleground));

        var elisRoutes =
                CaptureMovementMechanismFactsReader.assess(
                    fixture.game, DARK,
                    fixture.commonAnalyzer(false),
                    CaptureMovementMechanismFactsReader
                        .Mechanism.TRANSPORT,
                    elis, "'Transport' characters");
        assertTrue(elisRoutes.routes().stream().anyMatch(
                route -> route.destination()
                    == interiorBattleground));
        when(fixture.modifiers.hasGameTextModification(
                fixture.gameState, elis,
                com.gempukku.swccgo.logic.modifiers
                    .ModifyGameTextType
                    .NABRUN_LEIDS_ELIS_HELROT__LIMIT_USAGE))
                .thenReturn(true);
        var limitedElis =
                CaptureMovementMechanismFactsReader.assess(
                    fixture.game, DARK,
                    fixture.commonAnalyzer(false),
                    CaptureMovementMechanismFactsReader
                        .Mechanism.TRANSPORT,
                    elis, "'Transport' characters");
        assertFalse(limitedElis.routes().stream().anyMatch(
                route -> route.destination()
                    == interiorBattleground));
    }

    @Test
    public void dockingTransitParentAndDestinationChooseCaptureRoute() {
        Fixture fixture = new Fixture(false);
        PhysicalCard source = fixture.location(
                "1_291", 50, DARK);
        PhysicalCard captureBay = fixture.location(
                "1_285", 51, DARK);
        PhysicalCard decoyBay = fixture.location(
                "1_297", 52, DARK);
        PhysicalCard vader = fixture.active(
                "1_168", 53, DARK, source);
        PhysicalCard decoyMover = fixture.active(
                "1_169", 71, DARK, source);
        PhysicalCard luke = fixture.active(
                "1_19", 54, LIGHT, captureBay);
        fixture.persona(vader, Persona.VADER);
        fixture.persona(luke, Persona.LUKE);
        fixture.allowCaptureTarget(luke);
        assertTrue(
                com.gempukku.swccgo.filters.Filters.docking_bay
                    .accepts(
                        fixture.gameState,
                        fixture.modifiers, source));
        assertTrue(
                com.gempukku.swccgo.filters.Filters
                    .hasNotPerformedRegularMove.accepts(
                        fixture.gameState,
                        fixture.modifiers, vader));
        assertTrue(
                com.gempukku.swccgo.filters.Filters
                    .canMoveToUsingDockingBayTransit(
                        vader, false, 0.0f)
                    .accepts(
                        fixture.gameState,
                        fixture.modifiers, captureBay));

        var assessment =
                CaptureMovementMechanismFactsReader.assess(
                    fixture.game, DARK,
                    fixture.commonAnalyzer(false),
                    CaptureMovementMechanismFactsReader
                        .Mechanism.DOCKING_BAY_TRANSIT,
                    source, "Docking bay transit");
        assertTrue(
                "Expected real docking-bay capture route: "
                    + assessment.routes(),
                assessment.hasAdmissibleCaptureRoute());

        var rando = new com.gempukku.swccgo.ai.models.rando
                .evaluators.CombinedEvaluator();
        var randoParent =
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.DecisionContext(
                        fixture.gameState, DARK,
                        "ACTION_CHOICE", "Choose action",
                        "docking-parent-rando", Phase.MOVE);
        randoParent.setGame(fixture.game);
        randoParent.setSide(Side.DARK);
        var randoAnalyzer = mock(
                com.gempukku.swccgo.ai.models.rando
                    .strategy.ObjectiveAnalyzer.class);
        stubAnalyzer(randoAnalyzer, false);
        randoParent.setObjectiveAnalyzer(randoAnalyzer);
        configureDockingParent(randoParent, source);
        assertEquals(
                "transit",
                rando.evaluateDecision(randoParent)
                    .getActionId());

        var randoDestination =
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.DecisionContext(
                        fixture.gameState, DARK,
                        "CARD_SELECTION",
                        "Choose docking bay to transit to, "
                            + "or click 'Done' to cancel",
                        "docking-destination-rando",
                        Phase.MOVE);
        randoDestination.setGame(fixture.game);
        randoDestination.setSide(Side.DARK);
        randoDestination.setObjectiveAnalyzer(
                randoAnalyzer);
        configureCardSelection(
                randoDestination,
                List.of(decoyBay, captureBay));
        var randoWinner =
                rando.evaluateDecision(randoDestination);

        var chosen = new com.gempukku.swccgo.ai.models.chosenone
                .evaluators.CombinedEvaluator();
        var chosenParent =
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.DecisionContext(
                        fixture.gameState, DARK,
                        "ACTION_CHOICE", "Choose action",
                        "docking-parent-chosen", Phase.MOVE);
        chosenParent.setGame(fixture.game);
        chosenParent.setSide(Side.DARK);
        var chosenAnalyzer = mock(
                com.gempukku.swccgo.ai.models.chosenone
                    .strategy.ObjectiveAnalyzer.class);
        stubAnalyzer(chosenAnalyzer, false);
        chosenParent.setObjectiveAnalyzer(chosenAnalyzer);
        configureDockingParent(chosenParent, source);
        assertEquals(
                "transit",
                chosen.evaluateDecision(chosenParent)
                    .getActionId());

        var chosenDestination =
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.DecisionContext(
                        fixture.gameState, DARK,
                        "CARD_SELECTION",
                        "Choose docking bay to transit to, "
                            + "or click 'Done' to cancel",
                        "docking-destination-chosen",
                        Phase.MOVE);
        chosenDestination.setGame(fixture.game);
        chosenDestination.setSide(Side.DARK);
        chosenDestination.setObjectiveAnalyzer(
                chosenAnalyzer);
        configureCardSelection(
                chosenDestination,
                List.of(decoyBay, captureBay));
        var chosenWinner =
                chosen.evaluateDecision(chosenDestination);

        assertEquals(
                String.valueOf(captureBay.getCardId()),
                randoWinner.getActionId());
        assertEquals(
                randoWinner.getActionId(),
                chosenWinner.getActionId());
        assertEquals(
                randoWinner.getScore(),
                chosenWinner.getScore(), 0.0f);

        String moverPrompt =
                "Choose cards to docking bay transit to "
                    + com.gempukku.swccgo.logic.GameUtils
                        .getCardLink(captureBay)
                    + ", or click 'Done' to cancel";
        String sourceKey =
                ObjectiveAnalyzer
                    .DOCKING_TRANSIT_SOURCE_CARD_ID_EXTRA;
        String destinationKey =
                ObjectiveAnalyzer
                    .DOCKING_TRANSIT_DESTINATION_CARD_ID_EXTRA;
        var randoMover =
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.DecisionContext(
                        fixture.gameState, DARK,
                        "CARD_SELECTION", moverPrompt,
                        "docking-mover-rando",
                        Phase.MOVE);
        randoMover.setGame(fixture.game);
        randoMover.setSide(Side.DARK);
        randoMover.setObjectiveAnalyzer(randoAnalyzer);
        randoMover.setExtra(
                sourceKey,
                String.valueOf(source.getCardId()));
        randoMover.setExtra(
                destinationKey,
                String.valueOf(captureBay.getCardId()));
        configureCardSelection(
                randoMover,
                List.of(decoyMover, vader));

        var chosenMover =
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.DecisionContext(
                        fixture.gameState, DARK,
                        "CARD_SELECTION", moverPrompt,
                        "docking-mover-chosen",
                        Phase.MOVE);
        chosenMover.setGame(fixture.game);
        chosenMover.setSide(Side.DARK);
        chosenMover.setObjectiveAnalyzer(chosenAnalyzer);
        chosenMover.setExtra(
                sourceKey,
                String.valueOf(source.getCardId()));
        chosenMover.setExtra(
                destinationKey,
                String.valueOf(captureBay.getCardId()));
        configureCardSelection(
                chosenMover,
                List.of(decoyMover, vader));

        var randoMoverWinner =
                rando.evaluateDecision(randoMover);
        var chosenMoverWinner =
                chosen.evaluateDecision(chosenMover);
        assertEquals(
                String.valueOf(vader.getCardId()),
                randoMoverWinner.getActionId());
        assertEquals(
                randoMoverWinner.getActionId(),
                chosenMoverWinner.getActionId());
        assertEquals(
                randoMoverWinner.getScore(),
                chosenMoverWinner.getScore(), 0.0f);
    }

    @Test
    public void transportForceFloorAllowsOnlyApproachCredit() {
        Fixture fixture = new Fixture(false);
        PhysicalCard source = fixture.cardAndAdd(
                "1_243", 40, DARK, Zone.HAND, false);
        when(fixture.gameState.getForcePileSize(DARK))
                .thenReturn(3);
        when(fixture.gameState.getReserveDeckSize(DARK))
                .thenReturn(20);

        var blocked =
                CaptureMovementMechanismFactsReader.assess(
                    fixture.game, DARK,
                    fixture.commonAnalyzer(false),
                    CaptureMovementMechanismFactsReader
                        .Mechanism.TRANSPORT,
                    source, "'Transport' characters");
        assertTrue(blocked.factsKnown());
        assertFalse(blocked.forceBudgetReady());
        assertFalse(blocked.hasAdmissibleCaptureRoute());
        assertFalse(blocked.hasAdmissibleApproachRoute());

        when(fixture.gameState.getForcePileSize(DARK))
                .thenReturn(4);
        var funded =
                CaptureMovementMechanismFactsReader.assess(
                    fixture.game, DARK,
                    fixture.commonAnalyzer(false),
                    CaptureMovementMechanismFactsReader
                        .Mechanism.TRANSPORT,
                    source, "'Transport' characters");
        assertTrue(funded.forceBudgetReady());
        assertFalse(funded.hasAdmissibleCaptureRoute());
    }

    @Test
    public void transportOriginDestinationAndMoverChainWinsBothBots() {
        Fixture fixture = new Fixture(false);
        PhysicalCard source = fixture.cardAndAdd(
                "12_150", 84, DARK, Zone.HAND, false);
        PhysicalCard origin = fixture.location(
                "7_276", 85, DARK);
        PhysicalCard emptyOrigin = fixture.location(
                "1_285", 86, DARK);
        PhysicalCard captureSite = fixture.location(
                "1_291", 87, DARK);
        PhysicalCard decoySite = fixture.location(
                "1_297", 88, DARK);
        PhysicalCard vader = fixture.active(
                "1_168", 89, DARK, origin);
        PhysicalCard decoyMover = fixture.active(
                "1_169", 90, DARK, origin);
        PhysicalCard luke = fixture.active(
                "1_19", 91, LIGHT, captureSite);
        fixture.persona(vader, Persona.VADER);
        fixture.persona(luke, Persona.LUKE);
        fixture.allowCaptureTarget(luke);
        PlayCardAction live = fixture.livePlayAction(
                source, "'Transport' characters");
        when(live.getAllPrimaryTargetCards())
                .thenReturn(Map.of(
                    1, Map.of(
                        captureSite,
                        Collections
                            .<TargetingReason>emptySet())));

        var randoAnalyzer = mock(
                com.gempukku.swccgo.ai.models.rando
                    .strategy.ObjectiveAnalyzer.class);
        stubAnalyzer(randoAnalyzer, false);
        var chosenAnalyzer = mock(
                com.gempukku.swccgo.ai.models.chosenone
                    .strategy.ObjectiveAnalyzer.class);
        stubAnalyzer(chosenAnalyzer, false);
        var rando = new com.gempukku.swccgo.ai.models.rando
                .evaluators.CombinedEvaluator();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone
                .evaluators.CombinedEvaluator();

        var randoParent =
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.DecisionContext(
                        fixture.gameState, DARK,
                        "ACTION_CHOICE", "Choose action",
                        "transport-parent-rando",
                        Phase.MOVE);
        randoParent.setGame(fixture.game);
        randoParent.setSide(Side.DARK);
        randoParent.setObjectiveAnalyzer(randoAnalyzer);
        randoParent.setActionIds(
                List.of("transport", ""));
        randoParent.setActionTexts(
                List.of("'Transport' characters", "Pass"));
        randoParent.setCardIds(List.of(
                String.valueOf(source.getCardId()), ""));
        randoParent.setNoPass(false);
        var chosenParent =
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.DecisionContext(
                        fixture.gameState, DARK,
                        "ACTION_CHOICE", "Choose action",
                        "transport-parent-chosen",
                        Phase.MOVE);
        chosenParent.setGame(fixture.game);
        chosenParent.setSide(Side.DARK);
        chosenParent.setObjectiveAnalyzer(chosenAnalyzer);
        chosenParent.setActionIds(
                List.of("transport", ""));
        chosenParent.setActionTexts(
                List.of("'Transport' characters", "Pass"));
        chosenParent.setCardIds(List.of(
                String.valueOf(source.getCardId()), ""));
        chosenParent.setNoPass(false);
        var randoTransport =
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.CaptureMovementEvaluator()
                    .evaluate(randoParent).stream()
                    .filter(action ->
                        "transport".equals(action.getActionId()))
                    .findFirst().orElseThrow();
        var chosenTransport =
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.CaptureMovementEvaluator()
                    .evaluate(chosenParent).stream()
                    .filter(action ->
                        "transport".equals(action.getActionId()))
                    .findFirst().orElseThrow();
        assertEquals(300.0f,
                randoTransport.getScore(), 0.0f);
        assertEquals(300.0f,
                chosenTransport.getScore(), 0.0f);
        assertFalse(randoTransport.getReasoningString()
                .contains("CAPTURE ROUTE"));
        assertFalse(chosenTransport.getReasoningString()
                .contains("CAPTURE ROUTE"));
        assertEquals(1, randoTransport.getReasoning().stream()
                .filter(reason ->
                    reason.contains("ACTOR_ROUTE_START"))
                .count());
        assertEquals(1, chosenTransport.getReasoning().stream()
                .filter(reason ->
                    reason.contains("ACTOR_ROUTE_START"))
                .count());
        var randoParentWinner =
                rando.evaluateDecision(randoParent);
        var chosenParentWinner =
                chosen.evaluateDecision(chosenParent);
        assertEquals("transport",
                randoParentWinner.getActionId());
        assertEquals("transport",
                chosenParentWinner.getActionId());
        assertTrue(randoParentWinner.getReasoningString()
                .contains("ACTOR_ROUTE_START"));
        assertTrue(chosenParentWinner.getReasoningString()
                .contains("ACTOR_ROUTE_START"));
        assertFalse(randoParentWinner.getReasoningString()
                .contains("CAPTURE ROUTE"));
        assertFalse(chosenParentWinner.getReasoningString()
                .contains("CAPTURE ROUTE"));

        var randoOrigin = randoSelection(
                fixture, randoAnalyzer,
                "Choose site to 'transport' from",
                "transport-origin-rando",
                List.of(emptyOrigin, origin));
        var chosenOrigin = chosenSelection(
                fixture, chosenAnalyzer,
                "Choose site to 'transport' from",
                "transport-origin-chosen",
                List.of(emptyOrigin, origin));
        var randoOriginWinner =
                rando.evaluateDecision(randoOrigin);
        var chosenOriginWinner =
                chosen.evaluateDecision(chosenOrigin);
        assertEquals(
                String.valueOf(origin.getCardId()),
                randoOriginWinner.getActionId());
        assertEquals(
                String.valueOf(origin.getCardId()),
                chosenOriginWinner.getActionId());
        assertTrue(randoOriginWinner.getReasoningString()
                .contains("ACTOR_ROUTE_DESTINATION"));
        assertTrue(chosenOriginWinner.getReasoningString()
                .contains("ACTOR_ROUTE_DESTINATION"));

        var randoDestination = randoSelection(
                fixture, randoAnalyzer,
                "Choose site to 'transport' to",
                "transport-destination-rando",
                List.of(decoySite, captureSite));
        var chosenDestination = chosenSelection(
                fixture, chosenAnalyzer,
                "Choose site to 'transport' to",
                "transport-destination-chosen",
                List.of(decoySite, captureSite));
        var randoDestinationWinner =
                rando.evaluateDecision(randoDestination);
        var chosenDestinationWinner =
                chosen.evaluateDecision(chosenDestination);
        assertEquals(
                String.valueOf(captureSite.getCardId()),
                randoDestinationWinner.getActionId());
        assertEquals(
                String.valueOf(captureSite.getCardId()),
                chosenDestinationWinner.getActionId());
        assertTrue(randoDestinationWinner
                .getReasoningString()
                .contains("ACTOR_ROUTE_DESTINATION"));
        assertTrue(chosenDestinationWinner
                .getReasoningString()
                .contains("ACTOR_ROUTE_DESTINATION"));

        var randoMover = randoSelection(
                fixture, randoAnalyzer,
                "Choose characters to 'transport'",
                "transport-mover-rando",
                List.of(decoyMover, vader));
        var chosenMover = chosenSelection(
                fixture, chosenAnalyzer,
                "Choose characters to 'transport'",
                "transport-mover-chosen",
                List.of(decoyMover, vader));
        assertEquals(
                String.valueOf(vader.getCardId()),
                rando.evaluateDecision(randoMover)
                    .getActionId());
        assertEquals(
                String.valueOf(vader.getCardId()),
                chosen.evaluateDecision(chosenMover)
                    .getActionId());
    }

    @Test
    public void transportDestinationCannotBorrowAnotherOrigin() {
        Fixture fixture = new Fixture(false);
        PhysicalCard source = fixture.cardAndAdd(
                "1_243", 112, DARK, Zone.HAND, false);
        PhysicalCard selectedOrigin = fixture.location(
                "7_276", 113, DARK);
        PhysicalCard otherOrigin = fixture.location(
                "1_285", 114, DARK);
        PhysicalCard decoySite = fixture.location(
                "1_297", 115, DARK);
        PhysicalCard captureSite = fixture.location(
                "1_291", 116, DARK);
        PhysicalCard selectedVader = fixture.active(
                "7_175", 117, DARK, selectedOrigin);
        PhysicalCard otherVader = fixture.active(
                "1_168", 118, DARK, otherOrigin);
        PhysicalCard luke = fixture.active(
                "1_19", 119, LIGHT, captureSite);
        fixture.persona(selectedVader, Persona.VADER);
        fixture.persona(otherVader, Persona.VADER);
        fixture.persona(luke, Persona.LUKE);
        fixture.allowCaptureTarget(luke);
        when(fixture.modifiers
                .mayNotRelocateFromLocationToLocation(
                    fixture.gameState, selectedVader,
                    selectedOrigin, captureSite,
                    false, false))
            .thenReturn(true);
        fixture.livePlayAction(
                source, "'Transport' characters");

        var randoAnalyzer = mock(
                com.gempukku.swccgo.ai.models.rando
                    .strategy.ObjectiveAnalyzer.class);
        stubAnalyzer(randoAnalyzer, false);
        var rando = new com.gempukku.swccgo.ai.models.rando
                .evaluators.CombinedEvaluator();
        var originContext = randoSelection(
                fixture, randoAnalyzer,
                "Choose site to 'transport' from",
                "transport-bound-origin-rando",
                List.of(selectedOrigin));
        assertEquals(
                String.valueOf(selectedOrigin.getCardId()),
                rando.evaluateDecision(originContext)
                    .getActionId());

        var destinationContext = randoSelection(
                fixture, randoAnalyzer,
                "Choose site to 'transport' to",
                "transport-bound-destination-rando",
                List.of(captureSite, decoySite));
        rando.evaluateDecision(destinationContext);
        assertEquals(
                String.valueOf(selectedOrigin.getCardId()),
                destinationContext.getExtra(
                    CaptureMovementMechanismFactsReader
                        .SELECTED_ORIGIN_CARD_ID_EXTRA));

        var captureActions =
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.CaptureMovementEvaluator()
                    .evaluate(destinationContext);
        assertFalse(captureActions.stream().anyMatch(
                action -> String.valueOf(
                        captureSite.getCardId())
                    .equals(action.getActionId())));

        var assessment =
                CaptureMovementMechanismFactsReader.assess(
                    fixture.game, DARK,
                    fixture.commonAnalyzer(false),
                    CaptureMovementMechanismFactsReader
                        .Mechanism.TRANSPORT,
                    source, "'Transport' characters");
        assertTrue(assessment.routes().stream().anyMatch(
                route -> route.origin() == otherOrigin
                    && route.destination() == captureSite
                    && route.advancesCaptureApproach()
                    && !route.guaranteesImmediateCapture()));
    }

    @Test
    public void lightTransportMovesObjectiveLukeThroughTheWholeChain() {
        Fixture fixture = new Fixture(
                false, "9_61", LIGHT);
        PhysicalCard source = fixture.cardAndAdd(
                "1_97", 100, LIGHT, Zone.HAND, false);
        PhysicalCard origin = fixture.location(
                "7_276", 101, DARK);
        PhysicalCard emptyOrigin = fixture.location(
                "1_285", 102, DARK);
        PhysicalCard captureSite = fixture.location(
                "1_291", 103, DARK);
        PhysicalCard decoySite = fixture.location(
                "1_297", 104, DARK);
        PhysicalCard luke = fixture.active(
                "1_19", 105, LIGHT, origin);
        PhysicalCard decoyMover = fixture.active(
                "1_17", 106, LIGHT, origin);
        PhysicalCard imperial = fixture.active(
                "1_168", 107, DARK, captureSite);
        fixture.persona(luke, Persona.LUKE);
        fixture.persona(imperial, Persona.VADER);
        fixture.allowEscort(imperial, luke);
        PlayCardAction live = fixture.livePlayAction(
                source, "'Transport' characters");
        when(live.getAllPrimaryTargetCards())
                .thenReturn(Map.of(
                    1, Map.of(
                        captureSite,
                        Collections
                            .<TargetingReason>emptySet())));

        var common =
                CaptureMovementMechanismFactsReader.assess(
                    fixture.game, LIGHT,
                    fixture.commonAnalyzer(false),
                    CaptureMovementMechanismFactsReader
                        .Mechanism.TRANSPORT,
                    source, "'Transport' characters");
        assertFalse(common.hasAdmissibleCaptureRoute());
        assertTrue(common.hasAdmissibleApproachRoute());
        assertTrue(common.routes().stream()
                .filter(route ->
                    route.objectiveRelevantMover())
                .noneMatch(
                    CaptureMovementMechanismFactsReader.Route
                        ::guaranteesImmediateCapture));

        var randoAnalyzer = mock(
                com.gempukku.swccgo.ai.models.rando
                    .strategy.ObjectiveAnalyzer.class);
        stubAnalyzer(randoAnalyzer, false, "9_61");
        var chosenAnalyzer = mock(
                com.gempukku.swccgo.ai.models.chosenone
                    .strategy.ObjectiveAnalyzer.class);
        stubAnalyzer(chosenAnalyzer, false, "9_61");
        var rando = new com.gempukku.swccgo.ai.models.rando
                .evaluators.CombinedEvaluator();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone
                .evaluators.CombinedEvaluator();

        var randoParent =
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.DecisionContext(
                        fixture.gameState, LIGHT,
                        "ACTION_CHOICE", "Choose action",
                        "light-transport-parent-rando",
                        Phase.MOVE);
        randoParent.setGame(fixture.game);
        randoParent.setSide(Side.LIGHT);
        randoParent.setObjectiveAnalyzer(randoAnalyzer);
        randoParent.setActionIds(
                List.of("transport", ""));
        randoParent.setActionTexts(
                List.of("'Transport' characters", "Pass"));
        randoParent.setCardIds(List.of(
                String.valueOf(source.getCardId()), ""));
        randoParent.setNoPass(false);
        var chosenParent =
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.DecisionContext(
                        fixture.gameState, LIGHT,
                        "ACTION_CHOICE", "Choose action",
                        "light-transport-parent-chosen",
                        Phase.MOVE);
        chosenParent.setGame(fixture.game);
        chosenParent.setSide(Side.LIGHT);
        chosenParent.setObjectiveAnalyzer(chosenAnalyzer);
        chosenParent.setActionIds(
                List.of("transport", ""));
        chosenParent.setActionTexts(
                List.of("'Transport' characters", "Pass"));
        chosenParent.setCardIds(List.of(
                String.valueOf(source.getCardId()), ""));
        chosenParent.setNoPass(false);
        var randoParentWinner =
                rando.evaluateDecision(randoParent);
        var chosenParentWinner =
                chosen.evaluateDecision(chosenParent);
        assertEquals("transport",
                randoParentWinner.getActionId());
        assertEquals("transport",
                chosenParentWinner.getActionId());
        assertTrue(randoParentWinner.getReasoningString()
                .contains("ACTOR_ROUTE_START"));
        assertTrue(chosenParentWinner.getReasoningString()
                .contains("ACTOR_ROUTE_START"));
        assertFalse(randoParentWinner.getReasoningString()
                .contains("CAPTURE ROUTE"));
        assertFalse(chosenParentWinner.getReasoningString()
                .contains("CAPTURE ROUTE"));

        var randoOrigin = randoSelection(
                fixture, randoAnalyzer,
                LIGHT, Side.LIGHT,
                "Choose site to 'transport' from",
                "light-transport-origin-rando",
                List.of(emptyOrigin, origin));
        var chosenOrigin = chosenSelection(
                fixture, chosenAnalyzer,
                LIGHT, Side.LIGHT,
                "Choose site to 'transport' from",
                "light-transport-origin-chosen",
                List.of(emptyOrigin, origin));
        var randoOriginWinner =
                rando.evaluateDecision(randoOrigin);
        var chosenOriginWinner =
                chosen.evaluateDecision(chosenOrigin);
        assertEquals(
                String.valueOf(origin.getCardId()),
                randoOriginWinner.getActionId());
        assertEquals(
                String.valueOf(origin.getCardId()),
                chosenOriginWinner.getActionId());
        assertTrue(randoOriginWinner.getReasoningString()
                .contains("ACTOR_ROUTE_DESTINATION"));
        assertTrue(chosenOriginWinner.getReasoningString()
                .contains("ACTOR_ROUTE_DESTINATION"));

        var randoDestination = randoSelection(
                fixture, randoAnalyzer,
                LIGHT, Side.LIGHT,
                "Choose site to 'transport' to",
                "light-transport-destination-rando",
                List.of(decoySite, captureSite));
        var chosenDestination = chosenSelection(
                fixture, chosenAnalyzer,
                LIGHT, Side.LIGHT,
                "Choose site to 'transport' to",
                "light-transport-destination-chosen",
                List.of(decoySite, captureSite));
        var randoDestinationWinner =
                rando.evaluateDecision(randoDestination);
        var chosenDestinationWinner =
                chosen.evaluateDecision(chosenDestination);
        assertEquals(
                String.valueOf(captureSite.getCardId()),
                randoDestinationWinner.getActionId());
        assertEquals(
                String.valueOf(captureSite.getCardId()),
                chosenDestinationWinner.getActionId());
        assertTrue(randoDestinationWinner.getReasoningString()
                .contains("ACTOR_ROUTE_DESTINATION"));
        assertTrue(chosenDestinationWinner
                .getReasoningString()
                .contains("ACTOR_ROUTE_DESTINATION"));

        var randoMover = randoSelection(
                fixture, randoAnalyzer,
                LIGHT, Side.LIGHT,
                "Choose characters to 'transport'",
                "light-transport-mover-rando",
                List.of(decoyMover, luke));
        var chosenMover = chosenSelection(
                fixture, chosenAnalyzer,
                LIGHT, Side.LIGHT,
                "Choose characters to 'transport'",
                "light-transport-mover-chosen",
                List.of(decoyMover, luke));
        var randoMoverWinner =
                rando.evaluateDecision(randoMover);
        var chosenMoverWinner =
                chosen.evaluateDecision(chosenMover);
        assertEquals(
                String.valueOf(luke.getCardId()),
                randoMoverWinner.getActionId());
        assertEquals(
                String.valueOf(luke.getCardId()),
                chosenMoverWinner.getActionId());
        assertTrue(randoMoverWinner.getReasoningString()
                .contains("ACTOR_ROUTE_DESTINATION"));
        assertTrue(chosenMoverWinner.getReasoningString()
                .contains("ACTOR_ROUTE_DESTINATION"));
    }

    private static void configureCastleDecision(
            com.gempukku.swccgo.ai.models.rando
                .evaluators.DecisionContext context,
            PhysicalCard castle) {
        context.setActionIds(List.of("castle", ""));
        context.setActionTexts(List.of(
                "Move from here to other battleground site",
                "Pass"));
        context.setCardIds(List.of(
                String.valueOf(castle.getCardId()), ""));
        context.setNoPass(false);
        context.setMin(0);
    }

    private static void configureDockingParent(
            com.gempukku.swccgo.ai.models.rando
                .evaluators.DecisionContext context,
            PhysicalCard source) {
        context.setActionIds(List.of("transit", ""));
        context.setActionTexts(List.of(
                "Docking bay transit", "Pass"));
        context.setCardIds(List.of(
                String.valueOf(source.getCardId()), ""));
        context.setNoPass(false);
        context.setMin(0);
    }

    private static void configureDockingParent(
            com.gempukku.swccgo.ai.models.chosenone
                .evaluators.DecisionContext context,
            PhysicalCard source) {
        context.setActionIds(List.of("transit", ""));
        context.setActionTexts(List.of(
                "Docking bay transit", "Pass"));
        context.setCardIds(List.of(
                String.valueOf(source.getCardId()), ""));
        context.setNoPass(false);
        context.setMin(0);
    }

    private static void configureCardSelection(
            com.gempukku.swccgo.ai.models.rando
                .evaluators.DecisionContext context,
            List<PhysicalCard> cards) {
        context.setCardIds(cards.stream()
                .map(card -> String.valueOf(card.getCardId()))
                .toList());
        context.setSelectable(cards.stream()
                .map(ignored -> true).toList());
        context.setNoPass(false);
        context.setMin(1);
        context.setMax(1);
    }

    private static void configureCardSelection(
            com.gempukku.swccgo.ai.models.chosenone
                .evaluators.DecisionContext context,
            List<PhysicalCard> cards) {
        context.setCardIds(cards.stream()
                .map(card -> String.valueOf(card.getCardId()))
                .toList());
        context.setSelectable(cards.stream()
                .map(ignored -> true).toList());
        context.setNoPass(false);
        context.setMin(1);
        context.setMax(1);
    }

    private static com.gempukku.swccgo.ai.models.rando
            .evaluators.DecisionContext randoSelection(
            Fixture fixture,
            com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer analyzer,
            String prompt,
            String decisionId,
            List<PhysicalCard> cards) {
        return randoSelection(
                fixture, analyzer, DARK, Side.DARK,
                prompt, decisionId, cards);
    }

    private static com.gempukku.swccgo.ai.models.rando
            .evaluators.DecisionContext randoSelection(
            Fixture fixture,
            com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer analyzer,
            String playerId,
            Side side,
            String prompt,
            String decisionId,
            List<PhysicalCard> cards) {
        var context =
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.DecisionContext(
                        fixture.gameState, playerId,
                        "CARD_SELECTION", prompt,
                        decisionId, Phase.MOVE);
        context.setGame(fixture.game);
        context.setSide(side);
        context.setObjectiveAnalyzer(analyzer);
        configureCardSelection(context, cards);
        return context;
    }

    private static com.gempukku.swccgo.ai.models.chosenone
            .evaluators.DecisionContext chosenSelection(
            Fixture fixture,
            com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer analyzer,
            String prompt,
            String decisionId,
            List<PhysicalCard> cards) {
        return chosenSelection(
                fixture, analyzer, DARK, Side.DARK,
                prompt, decisionId, cards);
    }

    private static com.gempukku.swccgo.ai.models.chosenone
            .evaluators.DecisionContext chosenSelection(
            Fixture fixture,
            com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer analyzer,
            String playerId,
            Side side,
            String prompt,
            String decisionId,
            List<PhysicalCard> cards) {
        var context =
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.DecisionContext(
                        fixture.gameState, playerId,
                        "CARD_SELECTION", prompt,
                        decisionId, Phase.MOVE);
        context.setGame(fixture.game);
        context.setSide(side);
        context.setObjectiveAnalyzer(analyzer);
        configureCardSelection(context, cards);
        return context;
    }

    private static void configureCastleDecision(
            com.gempukku.swccgo.ai.models.chosenone
                .evaluators.DecisionContext context,
            PhysicalCard castle) {
        context.setActionIds(List.of("castle", ""));
        context.setActionTexts(List.of(
                "Move from here to other battleground site",
                "Pass"));
        context.setCardIds(List.of(
                String.valueOf(castle.getCardId()), ""));
        context.setNoPass(false);
        context.setMin(0);
    }

    private static void stubAnalyzer(
            ObjectiveAnalyzer analyzer,
            boolean flipped) {
        stubAnalyzer(analyzer, flipped, "9_151");
    }

    private static void stubAnalyzer(
            ObjectiveAnalyzer analyzer,
            boolean flipped,
            String objectiveBlueprintId) {
        when(analyzer.isAnalyzed()).thenReturn(true);
        when(analyzer.getObjectiveBlueprintId())
                .thenReturn(objectiveBlueprintId);
        when(analyzer.isFlipped()).thenReturn(flipped);
        when(analyzer.classifyPostFlipPayoffRoleAt(
                any(SwccgGame.class), any(String.class),
                any(PhysicalCard.class),
                any(PhysicalCard.class)))
            .thenReturn(ObjectiveAnalyzer
                .ObjectivePostFlipPayoffRole.NONE);
        when(analyzer.classifyPostFlipPayoffAt(
                any(SwccgGame.class), any(String.class),
                any(PhysicalCard.class),
                any(PhysicalCard.class)))
            .thenReturn(ObjectiveAnalyzer
                .ObjectivePostFlipPayoffRole.NONE);
    }

    private static PhysicalCard card(
            String blueprintId,
            int id,
            String owner,
            Zone zone) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint =
                CARDS.getSwccgoCardBlueprint(blueprintId);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getBlueprintId(true))
                .thenReturn(blueprintId);
        when(card.getBlueprintId(false))
                .thenReturn(blueprintId);
        when(card.getPermanentCardId()).thenReturn(id);
        when(card.getCardId()).thenReturn(id);
        when(card.getOwner()).thenReturn(owner);
        when(card.getZone()).thenReturn(zone);
        when(card.getTitle()).thenReturn(
                blueprint.getTitle());
        when(card.getTitles()).thenReturn(
                List.of(blueprint.getTitle()));
        when(card.getCardsEscorting())
                .thenReturn(List.of());
        return card;
    }

    private static final class Fixture {
        private final GameState gameState =
                mock(GameState.class);
        private final SwccgGame game =
                mock(SwccgGame.class);
        private final ModifiersQuerying modifiers =
                mock(ModifiersQuerying.class);
        private final List<PhysicalCard> cards =
                new ArrayList<>();
        private final List<PhysicalCard> locations =
                new ArrayList<>();
        private final Set<PhysicalCard> active =
                Collections.newSetFromMap(
                    new IdentityHashMap<>());
        private final Map<Integer, PhysicalCard> byId =
                new LinkedHashMap<>();
        private final Map<PhysicalCard, PhysicalCard> at =
                new IdentityHashMap<>();
        private final Map<PhysicalCard, PhysicalCard> presentPlaces =
                new IdentityHashMap<>();
        private final Map<PhysicalCard, PhysicalCard> presentLocations =
                new IdentityHashMap<>();
        private final Set<PhysicalCard> presentLocationOverrides =
                Collections.newSetFromMap(
                    new IdentityHashMap<>());
        private final Map<PhysicalCard, Set<Persona>> personas =
                new IdentityHashMap<>();
        private final Set<PhysicalCard> controlled =
                Collections.newSetFromMap(
                    new IdentityHashMap<>());
        private final PhysicalCard objective;
        private final String objectiveBlueprintId;
        private final String objectivePlayerId;

        private Fixture(boolean flipped) {
            this(flipped, "9_151", DARK);
        }

        private Fixture(
                boolean flipped,
                String objectiveBlueprintId,
                String objectivePlayerId) {
            this.objectiveBlueprintId =
                    objectiveBlueprintId;
            this.objectivePlayerId = objectivePlayerId;
            objective = objective(
                    1, flipped,
                    objectiveBlueprintId,
                    objectivePlayerId);
            add(objective, true);
            when(game.getGameState()).thenReturn(gameState);
            when(game.getModifiersQuerying())
                    .thenReturn(modifiers);
            when(gameState.getGame()).thenReturn(game);
            when(gameState.getCurrentPlayerId())
                    .thenReturn(objectivePlayerId);
            when(gameState.getOpponent(DARK))
                    .thenReturn(LIGHT);
            when(gameState.getOpponent(LIGHT))
                    .thenReturn(DARK);
            when(gameState.getPlayersLatestTurnNumber(DARK))
                    .thenReturn(3);
            when(gameState.getForcePileSize(DARK))
                    .thenReturn(10);
            when(gameState.getReserveDeckSize(DARK))
                    .thenReturn(20);
            when(gameState.getForcePileSize(LIGHT))
                    .thenReturn(10);
            when(gameState.getReserveDeckSize(LIGHT))
                    .thenReturn(20);
            when(gameState.getUsedPile(DARK))
                    .thenReturn(List.of());
            when(gameState.getHand(DARK))
                    .thenReturn(List.of());
            when(gameState.getAllPermanentCards())
                    .thenReturn(cards);
            when(gameState.getLocationsInOrder())
                    .thenReturn(locations);
            when(gameState.getTopLocations())
                    .thenReturn(locations);
            when(gameState.findCardById(anyInt()))
                    .thenAnswer(invocation ->
                        byId.get(invocation.getArgument(0)));
            when(gameState.findCardByPermanentId(anyInt()))
                    .thenAnswer(invocation ->
                        byId.get(invocation.getArgument(0)));
            when(gameState.isCardInPlayActive(
                    any(PhysicalCard.class),
                    anyBoolean(), anyBoolean(),
                    anyBoolean(), anyBoolean(),
                    anyBoolean(), anyBoolean(),
                    anyBoolean(), anyBoolean()))
                    .thenAnswer(invocation ->
                        active.contains(
                            invocation.getArgument(0)));
            when(gameState.iterateActiveCards(
                    any(PhysicalCardVisitor.class),
                    any(ModifiersQuerying.class),
                    any(), any(), any()))
                    .thenAnswer(invocation -> {
                        PhysicalCardVisitor visitor =
                                invocation.getArgument(0);
                        for (PhysicalCard card
                                : new ArrayList<>(cards)) {
                            if (active.contains(card)
                                    && visitor.visitPhysicalCard(card)) {
                                return true;
                            }
                        }
                        return false;
                    });
            when(gameState.iterateLocationsOnTable(
                    any(PhysicalCardVisitor.class),
                    anyBoolean()))
                    .thenAnswer(invocation -> {
                        PhysicalCardVisitor visitor =
                                invocation.getArgument(0);
                        for (PhysicalCard location
                                : new ArrayList<>(locations)) {
                            if (visitor.visitPhysicalCard(
                                    location)) {
                                return true;
                            }
                        }
                        return false;
                    });
            when(gameState.getCardsAtLocation(
                    any(PhysicalCard.class)))
                    .thenAnswer(invocation ->
                        cardsAt(invocation.getArgument(0)));
            when(gameState.getAttachedCards(
                    any(PhysicalCard.class)))
                    .thenReturn(List.of());
            when(gameState.getAllAttachedRecursively(
                    any(PhysicalCard.class)))
                    .thenReturn(List.of());
            when(gameState.getCaptivesOfEscort(
                    any(PhysicalCard.class)))
                    .thenReturn(List.of());
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
            when(modifiers.getCardTypes(
                    any(GameState.class),
                    any(PhysicalCard.class)))
                    .thenAnswer(invocation -> {
                        PhysicalCard card =
                                invocation.getArgument(1);
                        return card.getBlueprint()
                                .getCardTypes();
                    });
            when(modifiers.hasKeyword(
                    any(GameState.class),
                    any(PhysicalCard.class),
                    any(Keyword.class)))
                    .thenAnswer(invocation -> {
                        PhysicalCard card =
                                invocation.getArgument(1);
                        Keyword keyword =
                                invocation.getArgument(2);
                        return card != null
                                && card.getBlueprint() != null
                                && card.getBlueprint()
                                    .hasKeyword(keyword);
                    });
            when(modifiers.hasIcon(
                    any(GameState.class),
                    any(PhysicalCard.class),
                    any(Icon.class)))
                    .thenAnswer(invocation -> {
                        PhysicalCard card =
                                invocation.getArgument(1);
                        Icon icon =
                                invocation.getArgument(2);
                        return card != null
                                && card.getBlueprint() != null
                                && card.getBlueprint()
                                    .hasIcon(icon);
                    });
            when(modifiers.getLocationThatCardIsPresentAt(
                    any(GameState.class),
                    any(PhysicalCard.class)))
                    .thenAnswer(invocation -> {
                        PhysicalCard card =
                                invocation.getArgument(1);
                        return presentLocationOverrides
                                .contains(card)
                            ? presentLocations.get(card)
                            : at.get(card);
                    });
            when(modifiers.getCardIsPresentAt(
                    any(GameState.class),
                    any(PhysicalCard.class)))
                    .thenAnswer(invocation -> {
                        PhysicalCard card =
                                invocation.getArgument(1);
                        return presentPlaces.containsKey(card)
                            ? presentPlaces.get(card)
                            : at.get(card);
                    });
            when(modifiers.getLocationThatCardIsAt(
                    any(GameState.class),
                    any(PhysicalCard.class)))
                    .thenAnswer(invocation ->
                        at.get(invocation.getArgument(1)));
            when(modifiers.getLocationHere(
                    any(GameState.class),
                    any(PhysicalCard.class)))
                    .thenAnswer(invocation -> {
                        PhysicalCard card =
                                invocation.getArgument(1);
                        return locations.contains(card)
                                ? card : at.get(card);
                    });
            when(modifiers.isPresentWith(
                    any(GameState.class),
                    any(PhysicalCard.class),
                    any(PhysicalCard.class)))
                    .thenAnswer(invocation ->
                        samePresencePlace(
                            invocation.getArgument(1),
                            invocation.getArgument(2)));
            when(modifiers.isPresentWith(
                    any(GameState.class),
                    any(PhysicalCard.class),
                    any(PhysicalCard.class),
                    anyBoolean()))
                    .thenAnswer(invocation ->
                        samePresencePlace(
                            invocation.getArgument(1),
                            invocation.getArgument(2)));
            when(modifiers.controlsLocation(
                    any(GameState.class),
                    any(PhysicalCard.class),
                    any(String.class)))
                    .thenAnswer(invocation ->
                        controlled.contains(
                            invocation.getArgument(1)));
            when(modifiers.isBattleground(
                    any(GameState.class),
                    any(PhysicalCard.class),
                    any()))
                    .thenAnswer(invocation ->
                        locations.contains(
                            invocation.getArgument(1)));
            when(modifiers.getForceAvailableToUse(
                    gameState, DARK))
                    .thenAnswer(invocation ->
                        gameState.getForcePileSize(DARK));
            when(modifiers.getMoveUsingLocationTextCost(
                    any(GameState.class),
                    any(PhysicalCard.class),
                    any(PhysicalCard.class),
                    any(PhysicalCard.class),
                    anyFloat(), anyFloat()))
                    .thenReturn(0.0f);
            when(modifiers.canBeTargetedBy(
                    any(GameState.class),
                    any(PhysicalCard.class),
                    any(PhysicalCard.class),
                    any()))
                    .thenReturn(false);
        }

        private ObjectiveAnalyzer commonAnalyzer(
                boolean flipped) {
            ObjectiveAnalyzer analyzer =
                    mock(ObjectiveAnalyzer.class);
            stubAnalyzer(
                    analyzer, flipped,
                    objectiveBlueprintId);
            return analyzer;
        }

        private PhysicalCard cardAndAdd(
                String blueprintId,
                int id,
                String owner,
                Zone zone,
                boolean activeCard) {
            PhysicalCard card = card(
                    blueprintId, id, owner, zone);
            add(card, activeCard);
            return card;
        }

        private PhysicalCard location(
                String blueprintId,
                int id,
                String owner) {
            PhysicalCard location = cardAndAdd(
                    blueprintId, id, owner,
                    Zone.LOCATIONS, true);
            locations.add(location);
            at.put(location, location);
            when(location.getAtLocation())
                    .thenReturn(location);
            return location;
        }

        private PhysicalCard active(
                String blueprintId,
                int id,
                String owner,
                PhysicalCard location) {
            PhysicalCard card = cardAndAdd(
                    blueprintId, id, owner,
                    Zone.AT_LOCATION, true);
            at.put(card, location);
            when(card.getAtLocation())
                    .thenReturn(location);
            return card;
        }

        private void add(
                PhysicalCard card,
                boolean activeCard) {
            cards.add(card);
            byId.put(card.getPermanentCardId(), card);
            if (activeCard) {
                active.add(card);
            }
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

        private void control(PhysicalCard location) {
            controlled.add(location);
        }

        private void allowCaptureTarget(
                PhysicalCard target) {
            when(modifiers.canBeTargetedBy(
                    gameState, target, objective,
                    Collections.singleton(
                        TargetingReason.TO_BE_CAPTURED)))
                    .thenReturn(true);
        }

        private void aboard(
                PhysicalCard card,
                PhysicalCard carrier,
                PhysicalCard outerLocation,
                PhysicalCard presentPlace,
                PhysicalCard presentLocation) {
            when(card.getAtLocation()).thenReturn(null);
            when(card.getAttachedTo()).thenReturn(carrier);
            at.put(card, outerLocation);
            presentPlaces.put(card, presentPlace);
            presentLocationOverrides.add(card);
            presentLocations.put(card, presentLocation);
        }

        private void allowEmbark(
                PhysicalCard mover,
                PhysicalCard destination) {
            when(modifiers.isPresentWith(
                    gameState, mover,
                    destination, true))
                    .thenReturn(true);
            allowPassengerDestination(
                    mover, destination);
        }

        private void allowPassengerDestination(
                PhysicalCard mover,
                PhysicalCard destination) {
            when(gameState.getAvailablePilotCapacity(
                    modifiers, destination, mover))
                    .thenReturn(0);
            when(gameState.getAvailablePassengerCapacity(
                    modifiers, destination, mover))
                    .thenReturn(1);
        }

        private void allowEscort(
                PhysicalCard escort,
                PhysicalCard target) {
            when(modifiers.canEscortCaptive(
                    gameState, escort, target,
                    true, false, false))
                .thenReturn(true);
        }

        private void allowLocationTextMove(
                PhysicalCard mover,
                PhysicalCard origin,
                PhysicalCard destination) {
            at.put(mover, origin);
            when(mover.getAtLocation())
                    .thenReturn(origin);
            when(modifiers
                    .mayNotMoveFromLocationToLocationUsingLocationText(
                        gameState, mover,
                        origin, destination))
                    .thenReturn(false);
        }

        private void allowLandspeedMove(
                PhysicalCard mover,
                PhysicalCard destination,
                float cost) {
            when(modifiers.getLandspeedRequired(
                    gameState, mover, destination))
                    .thenReturn(1);
            when(modifiers.getLandspeed(
                    gameState, mover))
                    .thenReturn(1.0f);
            when(modifiers.getMoveUsingLandspeedCost(
                    gameState, mover,
                    at.get(mover), destination,
                    false, 0.0f))
                    .thenReturn(cost);
        }

        private PlayCardAction livePlayAction(
                PhysicalCard source,
                String text) {
            PlayCardState state =
                    mock(PlayCardState.class);
            PlayCardAction action =
                    mock(PlayCardAction.class);
            when(action.getActionSource())
                    .thenReturn(source);
            when(action.getText()).thenReturn(text);
            when(state.getPlayCardAction())
                    .thenReturn(action);
            when(gameState.getTopPlayCardState(null))
                    .thenReturn(state);
            return action;
        }

        private void liveGameTextAction(
                PhysicalCard source,
                String text) {
            GameTextActionState state =
                    mock(GameTextActionState.class);
            GameTextAction action =
                    mock(GameTextAction.class);
            when(action.getActionSource())
                    .thenReturn(source);
            when(action.getText()).thenReturn(text);
            when(state.getGameTextAction())
                    .thenReturn(action);
            when(gameState.getTopGameTextActionState())
                    .thenReturn(state);
        }

        private List<PhysicalCard> cardsAt(
                PhysicalCard location) {
            List<PhysicalCard> result =
                    new ArrayList<>();
            for (PhysicalCard card : cards) {
                if (at.get(card) == location
                        && card != location) {
                    result.add(card);
                }
            }
            return result;
        }

        private boolean samePresencePlace(
                PhysicalCard first,
                PhysicalCard second) {
            PhysicalCard firstPlace =
                    presentPlaces.containsKey(first)
                        ? presentPlaces.get(first)
                        : at.get(first);
            PhysicalCard secondPlace =
                    presentPlaces.containsKey(second)
                        ? presentPlaces.get(second)
                        : at.get(second);
            return firstPlace != null
                    && firstPlace == secondPlace;
        }

        private static PhysicalCard objective(
                int id,
                boolean flipped,
                String blueprintId,
                String owner) {
            PhysicalCard objective =
                    card(blueprintId, id, owner,
                        Zone.SIDE_OF_TABLE);
            SwccgCardBlueprint front =
                    CARDS.getSwccgoCardBlueprint(
                        blueprintId);
            SwccgCardBlueprint back =
                    CARDS.getSwccgoCardBlueprint(
                        blueprintId + "_BACK");
            when(objective.getBlueprint())
                    .thenReturn(flipped ? back : front);
            when(objective.getBlueprintId(true))
                    .thenReturn(blueprintId);
            when(objective.getBlueprintId(false))
                    .thenReturn(
                        flipped
                            ? blueprintId + "_BACK"
                            : blueprintId);
            when(objective.isFlipped())
                    .thenReturn(flipped);
            return objective;
        }
    }
}
