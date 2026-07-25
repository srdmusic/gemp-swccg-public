package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.MoveObjectiveGateHoldPolicy;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HuntDownObjectiveBehaviorTest {
    private static final String PLAYER_ID = "player";
    private static final String OPPONENT_ID = "opponent";

    private static final List<HuntFamily> FAMILIES = List.of(
            new HuntFamily("7_297", "classic"),
            new HuntFamily("213_31", "virtual"));

    @Test
    public void frontRuleRequiresRealVaderAtRuntimeBattlegroundForBothBots() {
        for (HuntFamily family : FAMILIES) {
            for (Bot bot : Bot.values()) {
                Fixture empty = fixture(family, bot, false);
                assertFalse(label(family, bot),
                        frontState(empty).conditionSatisfied());

                Fixture battleground = fixture(family, bot, false);
                place(battleground, battleground.vader,
                        battleground.battleground);
                assertTrue(label(family, bot),
                        frontState(battleground).conditionSatisfied());

                Fixture nonBattleground = fixture(family, bot, false);
                place(nonBattleground, nonBattleground.vader,
                        nonBattleground.nonBattleground);
                assertFalse(label(family, bot),
                        frontState(nonBattleground).conditionSatisfied());

                Fixture impostor = fixture(family, bot, false);
                place(impostor, impostor.impostorVader,
                        impostor.battleground);
                assertFalse(label(family, bot),
                        frontState(impostor).conditionSatisfied());

                Fixture opponentVader = fixture(family, bot, false);
                place(opponentVader, opponentVader.opponentVader,
                        opponentVader.battleground);
                assertTrue("The source actor scope is any player: "
                                + label(family, bot),
                        frontState(opponentVader).conditionSatisfied());
            }
        }
    }

    @Test
    public void rulesAtCountsAboardActorsAndBlockers() {
        for (HuntFamily family : FAMILIES) {
            for (Bot bot : Bot.values()) {
                Fixture aboardVader =
                        fixture(family, bot, false);
                aboardVader.permanents.add(
                        aboardVader.vader);
                when(aboardVader.modifiers
                        .getLocationThatCardIsPresentAt(
                            aboardVader.gameState,
                            aboardVader.vader))
                        .thenReturn(null);
                when(aboardVader.modifiers
                        .getLocationThatCardIsAt(
                            aboardVader.gameState,
                            aboardVader.vader))
                        .thenReturn(
                            aboardVader.battleground);
                assertTrue("Aboard Vader is at the battleground: "
                                + label(family, bot),
                        frontState(aboardVader)
                                .conditionSatisfied());
                assertEquals("A sole aboard Vader is still the last required actor: "
                                + label(family, bot),
                        ObjectiveAnalyzer.FlipGateFormationRole
                                .LAST_REQUIRED_ACTOR,
                        aboardVader.analyzer
                                .classifyGateFormationPieceIfRemoved(
                                    aboardVader.game,
                                    PLAYER_ID,
                                    aboardVader.vader));

                Fixture duplicateAboard =
                        fixture(family, bot, false);
                for (PhysicalCard vader : List.of(
                        duplicateAboard.vader,
                        duplicateAboard.secondVader)) {
                    duplicateAboard.permanents.add(vader);
                    when(duplicateAboard.modifiers
                            .getLocationThatCardIsPresentAt(
                                duplicateAboard.gameState, vader))
                            .thenReturn(null);
                    when(duplicateAboard.modifiers
                            .getLocationThatCardIsAt(
                                duplicateAboard.gameState, vader))
                            .thenReturn(
                                duplicateAboard.battleground);
                }
                assertEquals("A duplicate aboard Vader releases the casualty hold: "
                                + label(family, bot),
                        ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                        duplicateAboard.analyzer
                                .classifyGateFormationPieceIfRemoved(
                                    duplicateAboard.game,
                                    PLAYER_ID,
                                    duplicateAboard.vader));

                Fixture aboardBlocker =
                        fixture(family, bot, false);
                place(aboardBlocker,
                        aboardBlocker.vader,
                        aboardBlocker.battleground);
                aboardBlocker.permanents.add(
                        aboardBlocker.luke);
                when(aboardBlocker.modifiers
                        .getLocationThatCardIsPresentAt(
                            aboardBlocker.gameState,
                            aboardBlocker.luke))
                        .thenReturn(null);
                when(aboardBlocker.modifiers
                        .getLocationThatCardIsAt(
                            aboardBlocker.gameState,
                            aboardBlocker.luke))
                        .thenReturn(
                            aboardBlocker
                                .otherBattleground);
                assertFalse("Aboard blocker is at the battleground: "
                                + label(family, bot),
                        frontState(aboardBlocker)
                                .conditionSatisfied());
                assertTrue(label(family, bot),
                        aboardBlocker.analyzer
                            .isPreFlipGlobalBlockerAt(
                                aboardBlocker.game,
                                PLAYER_ID,
                                aboardBlocker
                                    .otherBattleground));
            }
        }
    }

    @Test
    public void objectiveLeavingPlayClearsClassicHardLossIdentity() {
        for (Bot bot : Bot.values()) {
            Fixture fixture =
                    fixture(FAMILIES.get(0), bot, false);
            assertTrue(label(FAMILIES.get(0), bot),
                    fixture.analyzer
                            .isClassicHuntDownObjective());

            fixture.permanents.clear();
            fixture.analyzer.analyze(
                    fixture.game, PLAYER_ID, Side.DARK);

            assertFalse(label(FAMILIES.get(0), bot),
                    fixture.analyzer.isAnalyzed());
            assertFalse(label(FAMILIES.get(0), bot),
                    fixture.analyzer
                            .isClassicHuntDownObjective());
        }
    }

    @Test
    public void LukeOrJediAtAnyOtherBattlegroundIsAGlobalBlocker() {
        for (HuntFamily family : FAMILIES) {
            for (Bot bot : Bot.values()) {
                for (Blocker blocker : List.of(
                        Blocker.LUKE, Blocker.JEDI)) {
                    Fixture blocked = fixture(family, bot, false);
                    place(blocked, blocked.vader,
                            blocked.battleground);
                    place(blocked, blocker.card(blocked),
                            blocked.otherBattleground);

                    assertFalse(label(family, bot),
                            frontState(blocked).conditionSatisfied());
                    assertTrue(label(family, bot),
                            blocked.analyzer.isPreFlipGlobalBlockerAt(
                                    blocked.game, PLAYER_ID,
                                    blocked.otherBattleground));
                    assertTrue(label(family, bot),
                            blocked.analyzer
                                    .isPreFlipBattleRemovableGlobalBlockerAt(
                                            blocked.game, PLAYER_ID,
                                            blocked
                                                    .otherBattleground));
                    assertFalse(label(family, bot),
                            blocked.analyzer.isPreFlipGlobalBlockerAt(
                                    blocked.game, PLAYER_ID,
                                    blocked.battleground));
                }

                Fixture offBattleground = fixture(family, bot, false);
                place(offBattleground, offBattleground.vader,
                        offBattleground.battleground);
                place(offBattleground, offBattleground.luke,
                        offBattleground.nonBattleground);
                assertTrue(label(family, bot),
                        frontState(offBattleground).conditionSatisfied());
            }
        }
    }

    @Test
    public void PadawanBlocksOnlyTheVirtualObjective() {
        for (Bot bot : Bot.values()) {
            Fixture classic = fixture(FAMILIES.get(0), bot, false);
            place(classic, classic.vader, classic.battleground);
            place(classic, classic.padawan, classic.otherBattleground);
            assertTrue(label(FAMILIES.get(0), bot),
                    frontState(classic).conditionSatisfied());
            assertFalse(label(FAMILIES.get(0), bot),
                    classic.analyzer.isPreFlipGlobalBlockerAt(
                            classic.game, PLAYER_ID,
                            classic.otherBattleground));

            Fixture virtual = fixture(FAMILIES.get(1), bot, false);
            place(virtual, virtual.vader, virtual.battleground);
            place(virtual, virtual.padawan, virtual.otherBattleground);
            assertFalse(label(FAMILIES.get(1), bot),
                    frontState(virtual).conditionSatisfied());
            assertTrue(label(FAMILIES.get(1), bot),
                    virtual.analyzer.isPreFlipGlobalBlockerAt(
                            virtual.game, PLAYER_ID,
                            virtual.otherBattleground));
        }
    }

    @Test
    public void excludedFromBattleActorsAndBlockersStillCount() {
        for (HuntFamily family : FAMILIES) {
            for (Bot bot : Bot.values()) {
                Fixture actor = fixture(family, bot, false);
                when(actor.gameState.isDuringBattle()).thenReturn(true);
                place(actor, actor.vader, actor.battleground);
                when(actor.modifiers.isExcludedFromBattle(
                        actor.gameState, actor.vader)).thenReturn(true);
                assertTrue("Excluded Vader remains spotted: "
                                + label(family, bot),
                        frontState(actor).conditionSatisfied());

                Fixture blocked = fixture(family, bot, false);
                when(blocked.gameState.isDuringBattle()).thenReturn(true);
                place(blocked, blocked.vader, blocked.battleground);
                place(blocked, blocked.luke,
                        blocked.otherBattleground);
                when(blocked.modifiers.isExcludedFromBattle(
                        blocked.gameState, blocked.luke)).thenReturn(true);
                assertFalse("Excluded blocker remains spotted: "
                                + label(family, bot),
                        frontState(blocked).conditionSatisfied());
                assertTrue(label(family, bot),
                        blocked.analyzer.isPreFlipGlobalBlockerAt(
                                blocked.game, PLAYER_ID,
                                blocked.otherBattleground));
                assertFalse("Excluded blocker is not a battle target: "
                                + label(family, bot),
                        blocked.analyzer
                                .isPreFlipBattleRemovableGlobalBlockerAt(
                                        blocked.game, PLAYER_ID,
                                        blocked.otherBattleground));
            }
        }
    }

    @Test
    public void inactiveActorsAndBlockersDoNotSatisfySourceSpotting() {
        for (HuntFamily family : FAMILIES) {
            for (Bot bot : Bot.values()) {
                Fixture inactiveActor = fixture(family, bot, false);
                place(inactiveActor, inactiveActor.vader,
                        inactiveActor.battleground);
                setActive(inactiveActor, inactiveActor.vader, false);
                assertFalse("Inactive Vader cannot satisfy the flip: "
                                + label(family, bot),
                        frontState(inactiveActor).conditionSatisfied());
                assertFalse(label(family, bot),
                        inactiveActor.analyzer
                                .advancesPreFlipActorAtRuntimeLocation(
                                        inactiveActor.game, PLAYER_ID,
                                        inactiveActor.vader,
                                        inactiveActor.otherBattleground));

                Fixture inactiveBlocker = fixture(family, bot, false);
                place(inactiveBlocker, inactiveBlocker.vader,
                        inactiveBlocker.battleground);
                place(inactiveBlocker, inactiveBlocker.luke,
                        inactiveBlocker.otherBattleground);
                setActive(inactiveBlocker, inactiveBlocker.luke, false);
                assertTrue("Inactive blocker cannot prevent the flip: "
                                + label(family, bot),
                        frontState(inactiveBlocker).conditionSatisfied());
                assertFalse(label(family, bot),
                        inactiveBlocker.analyzer
                                .isPreFlipGlobalBlockerAt(
                                        inactiveBlocker.game, PLAYER_ID,
                                        inactiveBlocker
                                                .otherBattleground));
            }
        }
    }

    @Test
    public void candidateCompletesTheFrontOnlyWhenEveryAlternativeIsTrue() {
        for (HuntFamily family : FAMILIES) {
            for (Bot bot : Bot.values()) {
                Fixture clear = fixture(family, bot, false);
                assertTrue(label(family, bot),
                        clear.analyzer.wouldCompletePreFlipRequirementAt(
                                clear.game, PLAYER_ID, clear.vader,
                                clear.battleground));
                assertFalse(label(family, bot),
                        clear.analyzer.wouldCompletePreFlipRequirementAt(
                                clear.game, PLAYER_ID,
                                clear.impostorVader,
                                clear.battleground));
                assertFalse(label(family, bot),
                        clear.analyzer.wouldCompletePreFlipRequirementAt(
                                clear.game, PLAYER_ID, clear.vader,
                                clear.nonBattleground));

                Fixture blocked = fixture(family, bot, false);
                place(blocked, blocked.luke,
                        blocked.otherBattleground);
                assertFalse("A remote Luke still blocks completion: "
                                + label(family, bot),
                        blocked.analyzer.wouldCompletePreFlipRequirementAt(
                                blocked.game, PLAYER_ID, blocked.vader,
                                blocked.battleground));
            }
        }
    }

    @Test
    public void movingVaderFromNonBattlegroundToBattlegroundAdvancesTheGate() {
        for (HuntFamily family : FAMILIES) {
            for (Bot bot : Bot.values()) {
                Fixture moving = fixture(family, bot, false);
                place(moving, moving.vader, moving.nonBattleground);

                assertTrue(label(family, bot),
                        moving.analyzer
                                .advancesPreFlipActorAtRuntimeLocation(
                                        moving.game, PLAYER_ID,
                                        moving.vader,
                                        moving.battleground));
                assertFalse(label(family, bot),
                        moving.analyzer
                                .advancesPreFlipActorAtRuntimeLocation(
                                        moving.game, PLAYER_ID,
                                        moving.vader,
                                        moving.nonBattleground));

                Fixture alreadyThere = fixture(family, bot, false);
                place(alreadyThere, alreadyThere.vader,
                        alreadyThere.battleground);
                assertFalse(label(family, bot),
                        alreadyThere.analyzer
                                .advancesPreFlipActorAtRuntimeLocation(
                                        alreadyThere.game, PLAYER_ID,
                                        alreadyThere.vader,
                                        alreadyThere.otherBattleground));
            }
        }
    }

    @Test
    public void virtualLocationDownloadUsesTheExactSourceFilter() {
        for (Bot bot : Bot.values()) {
            Fixture virtual = fixture(FAMILIES.get(1), bot, false);
            PhysicalCard eligible = site(
                    "Cloud City: Downtown Plaza", 104);
            when(virtual.modifiers.isBattleground(
                    virtual.gameState, eligible, null)).thenReturn(true);
            when(virtual.modifiers.hasIcon(
                    virtual.gameState, eligible,
                    Icon.CLOUD_CITY)).thenReturn(true);
            when(virtual.gameState.getReserveDeck(PLAYER_ID))
                    .thenReturn(List.of(eligible));
            when(virtual.modifiers.isDeployable(
                    eq(virtual.gameState),
                    any(PhysicalCard.class),
                    eq(eligible),
                    anyBoolean(),
                    any(),
                    anyBoolean(),
                    anyFloat(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    anyBoolean(),
                    anyFloat())).thenReturn(true);
            assertTrue(label(FAMILIES.get(1), bot),
                    virtual.analyzer
                            .hasVirtualHuntDownLocationDownloadInReserve(
                                    virtual.game, PLAYER_ID));

            when(virtual.modifiers.isBattleground(
                    virtual.gameState, eligible, null)).thenReturn(false);
            assertFalse(label(FAMILIES.get(1), bot),
                    virtual.analyzer
                            .hasVirtualHuntDownLocationDownloadInReserve(
                                    virtual.game, PLAYER_ID));

            when(virtual.modifiers.isBattleground(
                    virtual.gameState, eligible, null)).thenReturn(true);
            when(virtual.modifiers.isDeployable(
                    eq(virtual.gameState),
                    any(PhysicalCard.class),
                    eq(eligible),
                    anyBoolean(),
                    any(),
                    anyBoolean(),
                    anyFloat(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    anyBoolean(),
                    anyFloat())).thenReturn(false);
            assertFalse("Matching but undeployable is not legal: "
                            + label(FAMILIES.get(1), bot),
                    virtual.analyzer
                            .hasVirtualHuntDownLocationDownloadInReserve(
                                    virtual.game, PLAYER_ID));

            Fixture classic = fixture(FAMILIES.get(0), bot, false);
            when(classic.gameState.getReserveDeck(PLAYER_ID))
                    .thenReturn(List.of(eligible));
            assertFalse(label(FAMILIES.get(0), bot),
                    classic.analyzer
                            .hasVirtualHuntDownLocationDownloadInReserve(
                                    classic.game, PLAYER_ID));
        }
    }

    @Test
    public void vaderCastleDeployRequiresExactEngineDeployabilityAtOrdinaryCost() {
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(FAMILIES.get(1), bot, false);
            PhysicalCard castle = addCastle(
                    fixture, 150, true);
            when(fixture.gameState.getReserveDeck(PLAYER_ID))
                    .thenReturn(List.of(fixture.vader));
            com.gempukku.swccgo.filters.Filter validMove =
                    mock(com.gempukku.swccgo.filters.Filter.class);
            when(validMove.accepts(
                    fixture.gameState, fixture.modifiers,
                    fixture.battleground)).thenReturn(true);
            when(validMove.accepts(
                    fixture.gameState, fixture.modifiers,
                    fixture.otherBattleground)).thenReturn(true);
            when(fixture.vader.getBlueprint()
                    .getValidMoveTargetFilter(
                        PLAYER_ID, fixture.game,
                        fixture.vader, false))
                    .thenReturn(validMove);
            when(fixture.modifiers
                    .getMoveUsingLocationTextCost(
                        fixture.gameState, fixture.vader,
                        castle, fixture.battleground,
                        1.0f, 0.0f))
                    .thenReturn(2.25f);
            when(fixture.modifiers
                    .getMoveUsingLocationTextCost(
                        fixture.gameState, fixture.vader,
                        castle, fixture.otherBattleground,
                        1.0f, 0.0f))
                    .thenReturn(2.25f);
            when(fixture.modifiers.isDeployableToTarget(
                    any(), any(), any(), anyBoolean(), any(),
                    anyBoolean(), anyFloat(), any(), any(), any(),
                    any(), any(), any(), anyBoolean(), anyFloat()))
                    .thenReturn(true);

            assertTrue(label(FAMILIES.get(1), bot),
                    fixture.analyzer
                            .hasLegalVaderCastleDeployInReserve(
                                    fixture.game, PLAYER_ID));
            assertTrue(label(FAMILIES.get(1), bot),
                    fixture.analyzer
                            .hasVaderCastleDeployWithMoveReserve(
                                    fixture.game, PLAYER_ID));

            when(fixture.modifiers.isDeployableToTarget(
                    any(), any(), any(), anyBoolean(), any(),
                    anyBoolean(), anyFloat(), any(), any(), any(),
                    any(), any(), any(), anyBoolean(), anyFloat()))
                    .thenAnswer(invocation ->
                            invocation.getArgument(
                                    6, Float.class) <= 1.0f);
            assertTrue("Ordinary deploy remains legal: "
                            + label(FAMILIES.get(1), bot),
                    fixture.analyzer
                            .hasLegalVaderCastleDeployInReserve(
                                    fixture.game, PLAYER_ID));
            assertFalse("One Force cannot fund the exact three-Force move reserve: "
                            + label(FAMILIES.get(1), bot),
                    fixture.analyzer
                            .hasVaderCastleDeployWithMoveReserve(
                                    fixture.game, PLAYER_ID));

            when(fixture.modifiers.isDeployableToTarget(
                    any(), any(), any(), anyBoolean(), any(),
                    anyBoolean(), anyFloat(), any(), any(), any(),
                    any(), any(), any(), anyBoolean(), anyFloat()))
                    .thenAnswer(invocation ->
                            invocation.getArgument(
                                    6, Float.class) <= 3.0f);
            assertTrue("The ceiled three-Force reserve is sufficient: "
                            + label(FAMILIES.get(1), bot),
                    fixture.analyzer
                            .hasVaderCastleDeployWithMoveReserve(
                                    fixture.game, PLAYER_ID));

            when(fixture.modifiers.isDeployableToTarget(
                    any(), any(), any(), anyBoolean(), any(),
                    anyBoolean(), anyFloat(), any(), any(), any(),
                    any(), any(), any(), anyBoolean(), anyFloat()))
                    .thenReturn(false);
            assertFalse("Matching but unaffordable is not legal: "
                            + label(FAMILIES.get(1), bot),
                    fixture.analyzer
                            .hasLegalVaderCastleDeployInReserve(
                                    fixture.game, PLAYER_ID));
        }
    }

    @Test
    public void castleOutboundReserveRequiresExactLegalRouteAndUsesEngineCost() {
        for (Bot bot : Bot.values()) {
            Fixture adjusted = fixture(FAMILIES.get(1), bot, false);
            PhysicalCard adjustedCastle =
                    addCastle(adjusted, 170, true);
            prepareCastleOutbound(
                    adjusted, adjustedCastle,
                    adjusted.vader, 171, true, 2.25f);
            assertEquals("Modifier-adjusted 2.25 Force must reserve the engine's ceiled cost: "
                            + label(FAMILIES.get(1), bot),
                    3,
                    adjusted.analyzer
                            .getVaderCastleOutboundMoveReserve(
                                    adjusted.game, PLAYER_ID));

            Fixture free = fixture(FAMILIES.get(1), bot, false);
            PhysicalCard freeCastle =
                    addCastle(free, 175, true);
            prepareCastleOutbound(
                    free, freeCastle,
                    free.vader, 176, true, 0.0f);
            ObjectiveAnalyzer.VaderCastleOutboundAssessment
                    freeAssessment =
                    free.analyzer.assessVaderCastleOutbound(
                            free.game, PLAYER_ID);
            assertTrue("A free route is still a proven route: "
                            + label(FAMILIES.get(1), bot),
                    freeAssessment.safeRoute());
            assertEquals(label(FAMILIES.get(1), bot),
                    0, freeAssessment.minimumForce());

            Fixture undercover =
                    fixture(FAMILIES.get(1), bot, false);
            PhysicalCard undercoverCastle =
                    addCastle(undercover, 177, true);
            when(undercover.vader.isUndercover())
                    .thenReturn(true);
            prepareCastleOutbound(
                    undercover, undercoverCastle,
                    undercover.vader, 178, true, 1.0f);
            when(undercover.gameState.isCardInPlayActive(
                    undercover.vader,
                    false, false, false, false,
                    false, false, false, false))
                    .thenReturn(false);
            when(undercover.gameState.isCardInPlayActive(
                    undercover.vader,
                    true, false, false, false,
                    false, false, false, false))
                    .thenReturn(false);
            List<ObjectiveAnalyzer.VaderCastleRouteAssessment>
                    undercoverRoutes =
                    undercover.analyzer.assessVaderCastleRoutes(
                            undercover.game, PLAYER_ID,
                            undercoverCastle, true);
            assertEquals("Castle may legally move an undercover Vader: "
                            + label(FAMILIES.get(1), bot),
                    2, undercoverRoutes.size());
            assertTrue("Undercover legality must not pretend to satisfy the front trigger: "
                            + label(FAMILIES.get(1), bot),
                    undercoverRoutes.stream()
                        .noneMatch(
                            ObjectiveAnalyzer
                                .VaderCastleRouteAssessment
                                ::objectiveAdvance));
            assertFalse(label(FAMILIES.get(1), bot),
                    undercover.analyzer
                        .assessVaderCastleOutbound(
                            undercover.game, PLAYER_ID)
                        .safeRoute());

            Fixture duplicate = fixture(FAMILIES.get(1), bot, false);
            PhysicalCard duplicateCastle =
                    addCastle(duplicate, 180, true);
            prepareCastleOutbound(
                    duplicate, duplicateCastle,
                    duplicate.vader, 181, true, 1.0f);
            prepareCastleOutbound(
                    duplicate, duplicateCastle,
                    duplicate.secondVader, 182, true, 1.0f);
            when(duplicate.gameState.getCardsAtLocation(
                    duplicateCastle)).thenReturn(List.of(
                            duplicate.vader,
                            duplicate.secondVader));
            assertEquals("Two legal Vaders must not suppress the existence query: "
                            + label(FAMILIES.get(1), bot),
                    1,
                    duplicate.analyzer
                            .getVaderCastleOutboundMoveReserve(
                                    duplicate.game, PLAYER_ID));
            assertTrue(label(FAMILIES.get(1), bot),
                    duplicate.analyzer
                        .assessVaderCastleOutbound(
                            duplicate.game, PLAYER_ID)
                        .safeRoute());
            assertEquals("Each physical Vader and destination must remain a separate engine route: "
                            + label(FAMILIES.get(1), bot),
                    4,
                    duplicate.analyzer
                        .assessVaderCastleRoutes(
                            duplicate.game, PLAYER_ID,
                            duplicateCastle, true)
                        .size());

            Fixture unrelated = fixture(FAMILIES.get(1), bot, false);
            addCastle(unrelated, 190, true);
            place(unrelated, unrelated.vader,
                    unrelated.nonBattleground);
            assertEquals("Vader at an unrelated site has no Castle route: "
                            + label(FAMILIES.get(1), bot),
                    0,
                    unrelated.analyzer
                            .getVaderCastleOutboundMoveReserve(
                                    unrelated.game, PLAYER_ID));

            Fixture inactive = fixture(FAMILIES.get(1), bot, false);
            PhysicalCard inactiveCastle =
                    addCastle(inactive, 200, false);
            prepareCastleOutbound(
                    inactive, inactiveCastle,
                    inactive.vader, 201, true, 1.0f);
            assertEquals("An inactive Castle cannot fund a game-text route: "
                            + label(FAMILIES.get(1), bot),
                    0,
                    inactive.analyzer
                            .getVaderCastleOutboundMoveReserve(
                                    inactive.game, PLAYER_ID));

            Fixture noDestination =
                    fixture(FAMILIES.get(1), bot, false);
            PhysicalCard noDestinationCastle =
                    addCastle(noDestination, 210, true);
            prepareCastleOutbound(
                    noDestination, noDestinationCastle,
                    noDestination.vader, 211, false, 1.0f);
            assertEquals("No engine-legal battleground means no reserve: "
                            + label(FAMILIES.get(1), bot),
                    0,
                    noDestination.analyzer
                            .getVaderCastleOutboundMoveReserve(
                                    noDestination.game, PLAYER_ID));
            assertFalse("Zero reserve must not disguise an absent route: "
                            + label(FAMILIES.get(1), bot),
                    noDestination.analyzer
                        .assessVaderCastleOutbound(
                            noDestination.game, PLAYER_ID)
                        .safeRoute());

            Fixture alreadyMoved =
                    fixture(FAMILIES.get(1), bot, false);
            PhysicalCard movedCastle =
                    addCastle(alreadyMoved, 220, true);
            prepareCastleOutbound(
                    alreadyMoved, movedCastle,
                    alreadyMoved.vader, 221, true, 1.0f);
            when(alreadyMoved.modifiers
                    .hasPerformedRegularMoveThisTurn(
                            alreadyMoved.vader)).thenReturn(true);
            assertEquals("A Vader that already moved has no legal Castle action: "
                            + label(FAMILIES.get(1), bot),
                    0,
                    alreadyMoved.analyzer
                            .getVaderCastleOutboundMoveReserve(
                                    alreadyMoved.game, PLAYER_ID));

            Fixture frozen = fixture(FAMILIES.get(1), bot, false);
            PhysicalCard frozenCastle =
                    addCastle(frozen, 230, true);
            prepareCastleOutbound(
                    frozen, frozenCastle,
                    frozen.vader, 231, true, 1.0f);
            when(frozen.modifiers.mayNotMove(
                    frozen.gameState, frozen.vader)).thenReturn(true);
            assertEquals("A frozen or otherwise immobile Vader has no legal route: "
                            + label(FAMILIES.get(1), bot),
                    0,
                    frozen.analyzer
                            .getVaderCastleOutboundMoveReserve(
                                    frozen.game, PLAYER_ID));
        }
    }

    @Test
    public void castleReturnReleasesAtTheSharedWeaponAdjustedRetreatBoundary() {
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(FAMILIES.get(1), bot, false);
            PhysicalCard castle = site(
                    "Mustafar: Vader's Castle", 160);
            when(castle.getOwner()).thenReturn(PLAYER_ID);
            when(castle.getBlueprintId(true)).thenReturn("209_50");
            fixture.permanents.add(castle);
            when(fixture.gameState.findCardByPermanentId(160))
                    .thenReturn(castle);
            when(fixture.modifiers.getLocationHere(
                    fixture.gameState, castle)).thenReturn(castle);
            setActive(fixture, castle, true);
            setActive(fixture, fixture.battleground, true);
            setActive(fixture, fixture.otherBattleground, true);
            when(fixture.gameState.getLocationsInOrder())
                    .thenReturn(List.of(
                        fixture.battleground,
                        fixture.otherBattleground,
                        fixture.nonBattleground,
                        castle));

            when(fixture.vader.getPermanentCardId())
                    .thenReturn(161);
            when(fixture.gameState.findCardByPermanentId(161))
                    .thenReturn(fixture.vader);
            place(fixture, fixture.vader, fixture.battleground);
            setActive(fixture, fixture.vader, true);
            when(fixture.modifiers.getLocationHere(
                    fixture.gameState, fixture.vader))
                    .thenReturn(fixture.battleground);
            com.gempukku.swccgo.filters.Filter validMove =
                    mock(com.gempukku.swccgo.filters.Filter.class);
            when(validMove.accepts(
                    fixture.gameState, fixture.modifiers, castle))
                    .thenReturn(true);
            when(fixture.vader.getBlueprint()
                    .getValidMoveTargetFilter(
                            eq(PLAYER_ID), eq(fixture.game),
                            eq(fixture.vader), eq(false)))
                    .thenReturn(validMove);
            when(fixture.modifiers.getForceAvailableToUse(
                    fixture.gameState, PLAYER_ID)).thenReturn(10);
            when(fixture.modifiers.getTotalPowerAtLocation(
                    fixture.gameState, fixture.battleground,
                    PLAYER_ID, false, false)).thenReturn(3.0f);
            when(fixture.modifiers.getTotalPowerAtLocation(
                    fixture.gameState, fixture.battleground,
                    OPPONENT_ID, false, false)).thenReturn(5.0f);

            PhysicalCard opponent = character(
                    fixture.gameState, fixture.modifiers,
                    "Opponent Guard", OPPONENT_ID);
            when(fixture.gameState.getCardsAtLocation(
                    fixture.battleground))
                    .thenReturn(List.of(opponent));
            when(fixture.gameState.getAttachedCards(opponent))
                    .thenReturn(List.of());
            assertTrue("Raw 5 versus 3 is inside the six-power hold: "
                            + label(FAMILIES.get(1), bot),
                    fixture.analyzer
                            .mustHoldAllVaderCastleReturnMovers(
                                    fixture.game, PLAYER_ID, castle));

            PhysicalCard lightsaber = mock(PhysicalCard.class);
            SwccgCardBlueprint weapon =
                    mock(SwccgCardBlueprint.class);
            when(lightsaber.getTitle()).thenReturn(
                    "Opponent's Lightsaber");
            when(lightsaber.getBlueprint()).thenReturn(weapon);
            when(weapon.getCardCategory())
                    .thenReturn(CardCategory.WEAPON);
            when(fixture.gameState.getAttachedCards(opponent))
                    .thenReturn(List.of(lightsaber));
            assertFalse("Weapon-adjusted 10 versus 3 releases retreat: "
                            + label(FAMILIES.get(1), bot),
                    fixture.analyzer
                            .mustHoldAllVaderCastleReturnMovers(
                                    fixture.game, PLAYER_ID, castle));
        }
    }

    @Test
    public void classicRecallPreservesTheActorUnlessItCanChaseARemoteBlocker() {
        for (Bot bot : Bot.values()) {
            Fixture sole = fixture(FAMILIES.get(0), bot, false);
            place(sole, sole.vader, sole.battleground);
            when(sole.modifiers.controlsLocation(
                    sole.gameState, sole.battleground, PLAYER_ID))
                    .thenReturn(true);
            ObjectiveAnalyzer.ClassicHuntDownRecallAssessment
                    soleAssessment =
                    sole.analyzer.assessClassicHuntDownVaderRecall(
                            sole.game, PLAYER_ID);
            assertFalse(label(FAMILIES.get(0), bot),
                    soleAssessment.safeTarget());
            assertFalse(label(FAMILIES.get(0), bot),
                    soleAssessment.remoteJediOrLukeBlocker());

            Fixture chase = fixture(FAMILIES.get(0), bot, false);
            place(chase, chase.vader, chase.battleground);
            place(chase, chase.luke, chase.otherBattleground);
            when(chase.modifiers.controlsLocation(
                    chase.gameState, chase.battleground, PLAYER_ID))
                    .thenReturn(true);
            ObjectiveAnalyzer.ClassicHuntDownRecallAssessment
                    chaseAssessment =
                    chase.analyzer.assessClassicHuntDownVaderRecall(
                            chase.game, PLAYER_ID);
            assertTrue(label(FAMILIES.get(0), bot),
                    chaseAssessment.safeTarget());
            assertTrue(label(FAMILIES.get(0), bot),
                    chaseAssessment.remoteJediOrLukeBlocker());

            Fixture enemyVader =
                    fixture(FAMILIES.get(0), bot, false);
            place(enemyVader, enemyVader.opponentVader,
                    enemyVader.battleground);
            place(enemyVader, enemyVader.luke,
                    enemyVader.otherBattleground);
            when(enemyVader.modifiers.controlsLocation(
                    enemyVader.gameState,
                    enemyVader.battleground,
                    PLAYER_ID)).thenReturn(true);
            ObjectiveAnalyzer.HuntDownRecallTargetAssessment
                    enemyAssessment =
                    enemyVader.analyzer
                        .assessClassicHuntDownVaderRecallTarget(
                            enemyVader.game, PLAYER_ID,
                            enemyVader.opponentVader);
            assertTrue("The source can expose an opponent Vader: "
                            + label(FAMILIES.get(0), bot),
                    enemyAssessment.applies());
            assertFalse("Returning an opponent Vader is not a redeploy plan: "
                            + label(FAMILIES.get(0), bot),
                    enemyAssessment.safeTarget());
            assertFalse(label(FAMILIES.get(0), bot),
                    enemyAssessment.remoteBlockerChase());

            Fixture sameSiteAboard =
                    fixture(FAMILIES.get(0), bot, false);
            place(sameSiteAboard, sameSiteAboard.vader,
                    sameSiteAboard.battleground);
            sameSiteAboard.permanents.add(
                    sameSiteAboard.luke);
            when(sameSiteAboard.modifiers
                    .getLocationThatCardIsPresentAt(
                        sameSiteAboard.gameState,
                        sameSiteAboard.luke))
                    .thenReturn(null);
            when(sameSiteAboard.modifiers
                    .getLocationThatCardIsAt(
                        sameSiteAboard.gameState,
                        sameSiteAboard.luke))
                    .thenReturn(
                        sameSiteAboard.battleground);
            when(sameSiteAboard.modifiers.controlsLocation(
                    sameSiteAboard.gameState,
                    sameSiteAboard.battleground,
                    PLAYER_ID)).thenReturn(true);
            ObjectiveAnalyzer.ClassicHuntDownRecallAssessment
                    sameSiteAssessment =
                    sameSiteAboard.analyzer
                        .assessClassicHuntDownVaderRecall(
                            sameSiteAboard.game,
                            PLAYER_ID);
            assertFalse("An aboard blocker at Vader's site is not remote: "
                            + label(FAMILIES.get(0), bot),
                    sameSiteAssessment.safeTarget());
            assertFalse(label(FAMILIES.get(0), bot),
                    sameSiteAssessment
                        .remoteJediOrLukeBlocker());

            Fixture remoteAboard =
                    fixture(FAMILIES.get(0), bot, false);
            place(remoteAboard, remoteAboard.vader,
                    remoteAboard.battleground);
            remoteAboard.permanents.add(
                    remoteAboard.luke);
            when(remoteAboard.modifiers
                    .getLocationThatCardIsPresentAt(
                        remoteAboard.gameState,
                        remoteAboard.luke))
                    .thenReturn(null);
            when(remoteAboard.modifiers
                    .getLocationThatCardIsAt(
                        remoteAboard.gameState,
                        remoteAboard.luke))
                    .thenReturn(
                        remoteAboard.otherBattleground);
            when(remoteAboard.modifiers.controlsLocation(
                    remoteAboard.gameState,
                    remoteAboard.battleground,
                    PLAYER_ID)).thenReturn(true);
            ObjectiveAnalyzer.ClassicHuntDownRecallAssessment
                    remoteAboardAssessment =
                    remoteAboard.analyzer
                        .assessClassicHuntDownVaderRecall(
                            remoteAboard.game,
                            PLAYER_ID);
            assertTrue("An aboard blocker at another battleground is remote: "
                            + label(FAMILIES.get(0), bot),
                    remoteAboardAssessment.safeTarget());
            assertTrue(label(FAMILIES.get(0), bot),
                    remoteAboardAssessment
                        .remoteJediOrLukeBlocker());

            Fixture duplicate = fixture(FAMILIES.get(0), bot, false);
            place(duplicate, duplicate.vader,
                    duplicate.battleground);
            place(duplicate, duplicate.secondVader,
                    duplicate.nonBattleground);
            when(duplicate.modifiers.controlsLocation(
                    duplicate.gameState, duplicate.battleground,
                    PLAYER_ID)).thenReturn(true);
            when(duplicate.modifiers.controlsLocation(
                    duplicate.gameState, duplicate.nonBattleground,
                    PLAYER_ID)).thenReturn(true);
            ObjectiveAnalyzer.HuntDownRecallTargetAssessment
                    battlegroundTarget =
                    duplicate.analyzer
                        .assessClassicHuntDownVaderRecallTarget(
                            duplicate.game, PLAYER_ID,
                            duplicate.vader);
            ObjectiveAnalyzer.HuntDownRecallTargetAssessment
                    safeDuplicateTarget =
                    duplicate.analyzer
                        .assessClassicHuntDownVaderRecallTarget(
                            duplicate.game, PLAYER_ID,
                            duplicate.secondVader);
            assertTrue(label(FAMILIES.get(0), bot),
                    battlegroundTarget.applies());
            assertFalse("The sole battleground Vader is not the safe recall target: "
                            + label(FAMILIES.get(0), bot),
                    battlegroundTarget.safeTarget());
            assertTrue("The controlled non-battleground duplicate is safe: "
                            + label(FAMILIES.get(0), bot),
                    safeDuplicateTarget.safeTarget());
            assertTrue(label(FAMILIES.get(0), bot),
                    safeDuplicateTarget.preservesRequiredVader());
            ObjectiveAnalyzer.ClassicHuntDownRecallAssessment
                    duplicateAssessment =
                    duplicate.analyzer.assessClassicHuntDownVaderRecall(
                            duplicate.game, PLAYER_ID);
            assertTrue(label(FAMILIES.get(0), bot),
                    duplicateAssessment.safeTarget());
            assertFalse(label(FAMILIES.get(0), bot),
                    duplicateAssessment.remoteJediOrLukeBlocker());

            Fixture virtual = fixture(FAMILIES.get(1), bot, false);
            place(virtual, virtual.vader, virtual.battleground);
            when(virtual.modifiers.controlsLocation(
                    virtual.gameState, virtual.battleground, PLAYER_ID))
                    .thenReturn(true);
            assertFalse(label(FAMILIES.get(1), bot),
                    virtual.analyzer
                            .assessClassicHuntDownVaderRecall(
                                    virtual.game, PLAYER_ID)
                            .safeTarget());

            Fixture virtualPostFlip =
                    fixture(FAMILIES.get(1), bot, true);
            place(virtualPostFlip, virtualPostFlip.vader,
                    virtualPostFlip.battleground);
            when(virtualPostFlip.modifiers.controlsLocation(
                    virtualPostFlip.gameState,
                    virtualPostFlip.battleground, PLAYER_ID))
                    .thenReturn(true);
            assertFalse("The sole post-flip Vader must remain on table: "
                            + label(FAMILIES.get(1), bot),
                    virtualPostFlip.analyzer
                            .hasSafeVirtualHuntDownVaderRecallTarget(
                                    virtualPostFlip.game, PLAYER_ID));
            assertFalse(label(FAMILIES.get(1), bot),
                    virtualPostFlip.analyzer
                        .assessVirtualHuntDownVaderRecallTarget(
                            virtualPostFlip.game, PLAYER_ID,
                            virtualPostFlip.vader)
                        .safeTarget());
            place(virtualPostFlip, virtualPostFlip.secondVader,
                    virtualPostFlip.nonBattleground);
            assertTrue("A second on-table Vader preserves the back side: "
                            + label(FAMILIES.get(1), bot),
                    virtualPostFlip.analyzer
                            .hasSafeVirtualHuntDownVaderRecallTarget(
                                    virtualPostFlip.game, PLAYER_ID));
            assertTrue("The broad virtual child target is not limited to the controlled trigger site: "
                            + label(FAMILIES.get(1), bot),
                    virtualPostFlip.analyzer
                        .assessVirtualHuntDownVaderRecallTarget(
                            virtualPostFlip.game, PLAYER_ID,
                            virtualPostFlip.vader)
                        .safeTarget());
        }
    }

    @Test
    public void classicMaulDuelExceptionRequiresExactSourceAndModifier() {
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(FAMILIES.get(0), bot, false);
            PhysicalCard objective = fixture.permanents.get(0);
            when(fixture.modifiers.hasGameTextModification(
                    fixture.gameState, objective,
                    ModifyGameTextType
                            .HUNT_DOWN__DO_NOT_PLACE_OUT_OF_PLAY_IF_MAUL_DUELS))
                    .thenReturn(true);

            PhysicalCard maul = character(
                    fixture.gameState, fixture.modifiers,
                    "Darth Maul", PLAYER_ID);
            when(fixture.modifiers.hasPersona(
                    fixture.gameState, maul, Persona.MAUL))
                    .thenReturn(true);
            PhysicalCard maulStrikes =
                    sourceCard("Maul Strikes");
            PhysicalCard ordinaryDuel =
                    sourceCard("The Circle Is Now Complete");

            assertTrue(label(FAMILIES.get(0), bot),
                    fixture.analyzer
                            .hasClassicHuntDownMaulDuelException(
                                    fixture.game, PLAYER_ID, maul));
            assertTrue(label(FAMILIES.get(0), bot),
                    fixture.analyzer
                            .hasClassicHuntDownMaulDuelException(
                                    fixture.game, PLAYER_ID,
                                    maulStrikes));
            assertFalse(label(FAMILIES.get(0), bot),
                    fixture.analyzer
                            .hasClassicHuntDownMaulDuelException(
                                    fixture.game, PLAYER_ID,
                                    ordinaryDuel));

            when(fixture.modifiers.hasGameTextModification(
                    fixture.gameState, objective,
                    ModifyGameTextType
                            .HUNT_DOWN__DO_NOT_PLACE_OUT_OF_PLAY_IF_MAUL_DUELS))
                    .thenReturn(false);
            assertFalse(label(FAMILIES.get(0), bot),
                    fixture.analyzer
                            .hasClassicHuntDownMaulDuelException(
                                    fixture.game, PLAYER_ID, maul));
        }
    }

    @Test
    public void preFlipSoleBattlegroundVaderIsTheActorAndNeverNeedsABuddy() {
        for (HuntFamily family : FAMILIES) {
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(family, bot, false);
                place(fixture, fixture.vader, fixture.battleground);
                place(fixture, fixture.buddy, fixture.battleground);

                assertEquals(label(family, bot),
                        ObjectiveAnalyzer.FlipGateFormationRole
                                .LAST_REQUIRED_ACTOR,
                        fixture.analyzer
                                .classifyGateFormationPieceIfRemoved(
                                        fixture.game, PLAYER_ID,
                                        fixture.vader));
                assertEquals(label(family, bot),
                        ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                        fixture.analyzer
                                .classifyGateFormationPieceIfRemoved(
                                        fixture.game, PLAYER_ID,
                                        fixture.buddy));
            }
        }
    }

    @Test
    public void remoteBlockerDoesNotPermitEvacuatingTheSoleActor() {
        for (HuntFamily family : FAMILIES) {
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(family, bot, false);
                place(fixture, fixture.vader, fixture.battleground);
                place(fixture, fixture.luke,
                        fixture.otherBattleground);

                assertFalse(label(family, bot),
                        frontState(fixture).conditionSatisfied());
                ObjectiveAnalyzer.FlipGateFormationRole role =
                        fixture.analyzer
                                .classifyGateFormationPieceIfRemoved(
                                        fixture.game, PLAYER_ID,
                                        fixture.vader);
                assertEquals(label(family, bot),
                        ObjectiveAnalyzer.FlipGateFormationRole
                                .LAST_REQUIRED_ACTOR,
                        role);
                assertTrue(label(family, bot),
                        MoveObjectiveGateHoldPolicy
                                .evaluateRuntimeActorFormation(
                                        fixture.analyzer
                                                .hasPreFlipRuntimeActorRule(),
                                        role, 8.0f, 14.0f)
                                .hardVeto());
                assertFalse(label(family, bot),
                        MoveObjectiveGateHoldPolicy
                                .evaluateRuntimeActorFormation(
                                        true, role, 8.0f, 15.0f)
                                .hardVeto());
            }
        }
    }

    @Test
    public void postFlipSoleOnTableVaderUsesOnTableRole() {
        for (HuntFamily family : FAMILIES) {
            for (Bot bot : Bot.values()) {
                Fixture soleVader = fixture(family, bot, true);
                place(soleVader, soleVader.vader,
                        soleVader.nonBattleground);
                assertEquals(label(family, bot),
                        ObjectiveAnalyzer.FlipGateFormationRole
                                .LAST_REQUIRED_ON_TABLE_ACTOR,
                        soleVader.analyzer
                                .classifyGateFormationPieceIfRemoved(
                                        soleVader.game, PLAYER_ID,
                                        soleVader.vader));

                place(soleVader, soleVader.secondVader,
                        soleVader.otherBattleground);
                assertEquals(label(family, bot),
                        ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                        soleVader.analyzer
                                .classifyGateFormationPieceIfRemoved(
                                        soleVader.game, PLAYER_ID,
                                        soleVader.vader));
                assertEquals(label(family, bot),
                        ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                        soleVader.analyzer
                                .classifyGateFormationPieceIfRemoved(
                                        soleVader.game, PLAYER_ID,
                                        soleVader.buddy));
            }
        }
    }

    @Test
    public void postFlipMovementOffBattlegroundIsNotAPhysicalHold() {
        for (HuntFamily family : FAMILIES) {
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(family, bot, true);
                place(fixture, fixture.vader, fixture.battleground);

                assertFalse(label(family, bot),
                        fixture.analyzer.isFlipBackProtectionLocation(
                                fixture.battleground,
                                fixture.game, PLAYER_ID));
                assertFalse(label(family, bot),
                        fixture.analyzer.isFlipBackProtectionLocation(
                                fixture.nonBattleground,
                                fixture.game, PLAYER_ID));
                assertFalse(label(family, bot),
                        fixture.analyzer.wouldDepartureTriggerFlipBack(
                                fixture.game, PLAYER_ID,
                                fixture.vader));
            }
        }
    }

    private static ObjectiveAnalyzer.FlipLocationRuleState frontState(
            Fixture fixture) {
        List<ObjectiveAnalyzer.FlipLocationRuleState> states =
                fixture.analyzer.assessFlipLocationRules(
                        fixture.game, PLAYER_ID, "preFlip", "flip");
        assertEquals(1, states.size());
        return states.get(0);
    }

    private static Fixture fixture(
            HuntFamily family, Bot bot, boolean flipped) {
        ObjectiveAnalyzer analyzer = bot.analyzer();
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard objective = mock(PhysicalCard.class);
        SwccgCardBlueprint front = mock(SwccgCardBlueprint.class);
        SwccgCardBlueprint back = mock(SwccgCardBlueprint.class);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(game.getOpponent(PLAYER_ID)).thenReturn(OPPONENT_ID);
        when(gameState.getGame()).thenReturn(game);
        when(gameState.getOpponent(PLAYER_ID)).thenReturn(OPPONENT_ID);
        when(gameState.getSide(PLAYER_ID)).thenReturn(Side.DARK);
        when(gameState.getSide(OPPONENT_ID)).thenReturn(Side.LIGHT);
        when(gameState.getHand(PLAYER_ID)).thenReturn(List.of());
        when(objective.getOwner()).thenReturn(PLAYER_ID);
        when(objective.getZone()).thenReturn(Zone.SIDE_OF_TABLE);
        when(objective.getBlueprint()).thenReturn(
                flipped ? back : front);
        when(objective.getOtherSideBlueprint()).thenReturn(
                flipped ? front : back);
        when(objective.getBlueprintId(true))
                .thenReturn(family.blueprintId);
        when(objective.isFlipped()).thenReturn(flipped);
        when(front.getTitle()).thenReturn(
                "Hunt Down And Destroy The Jedi");
        when(front.getGameText()).thenReturn(
                "Flip this card if Vader is at a battleground site.");
        when(front.getCardCategory()).thenReturn(CardCategory.OBJECTIVE);
        when(back.getTitle()).thenReturn(
                "Their Fire Has Gone Out Of The Universe");
        when(back.getGameText()).thenReturn(
                "Flip this card if Vader is not on table.");
        when(back.getCardCategory()).thenReturn(CardCategory.OBJECTIVE);

        PhysicalCard battleground = site(
                "Tatooine: Cantina", 101);
        PhysicalCard otherBattleground = site(
                "Cloud City: Carbonite Chamber", 102);
        PhysicalCard nonBattleground = site(
                "Executor: Meditation Chamber", 103);
        when(modifiers.isBattleground(
                gameState, battleground, null)).thenReturn(true);
        when(modifiers.isBattleground(
                gameState, otherBattleground, null)).thenReturn(true);
        when(modifiers.isBattleground(
                gameState, nonBattleground, null)).thenReturn(false);

        PhysicalCard vader = character(
                gameState, modifiers, "Darth Vader", PLAYER_ID);
        when(modifiers.hasPersona(
                gameState, vader, Persona.VADER)).thenReturn(true);
        PhysicalCard secondVader = character(
                gameState, modifiers, "Lord Vader", PLAYER_ID);
        when(modifiers.hasPersona(
                gameState, secondVader, Persona.VADER)).thenReturn(true);
        PhysicalCard opponentVader = character(
                gameState, modifiers, "Anakin Skywalker", OPPONENT_ID);
        when(modifiers.hasPersona(
                gameState, opponentVader, Persona.VADER)).thenReturn(true);
        PhysicalCard impostorVader = character(
                gameState, modifiers, "Vader Impostor", PLAYER_ID);

        PhysicalCard luke = character(
                gameState, modifiers, "Luke Skywalker", OPPONENT_ID);
        when(modifiers.hasPersona(
                gameState, luke, Persona.LUKE)).thenReturn(true);
        PhysicalCard jedi = character(
                gameState, modifiers, "Obi-Wan Kenobi", OPPONENT_ID);
        when(modifiers.getAbility(
                gameState, jedi)).thenReturn(6.0f);
        PhysicalCard padawan = character(
                gameState, modifiers, "Young Padawan", OPPONENT_ID);
        when(modifiers.hasKeyword(
                gameState, padawan, Keyword.PADAWAN)).thenReturn(true);
        when(modifiers.getAbility(
                gameState, padawan)).thenReturn(4.0f);
        PhysicalCard buddy = character(
                gameState, modifiers, "Admiral Ozzel", PLAYER_ID);

        List<PhysicalCard> locations = List.of(
                battleground, otherBattleground, nonBattleground);
        List<PhysicalCard> permanents = new ArrayList<>();
        permanents.add(objective);
        when(gameState.getLocationsInOrder()).thenReturn(locations);
        when(gameState.getTopLocations()).thenReturn(locations);
        when(gameState.getAllPermanentCards()).thenReturn(permanents);

        analyzer.analyze(game, PLAYER_ID, Side.DARK);
        return new Fixture(
                analyzer, game, gameState, modifiers, permanents,
                battleground, otherBattleground, nonBattleground,
                vader, secondVader, opponentVader, impostorVader,
                luke, jedi, padawan, buddy);
    }

    private static void place(
            Fixture fixture, PhysicalCard card, PhysicalCard location) {
        if (!fixture.permanents.contains(card)) {
            fixture.permanents.add(card);
        }
        when(fixture.modifiers.getLocationThatCardIsPresentAt(
                fixture.gameState, card)).thenReturn(location);
        when(fixture.modifiers.getLocationThatCardIsAt(
                fixture.gameState, card)).thenReturn(location);
        when(fixture.gameState.findCardByPermanentId(
                location.getPermanentCardId())).thenReturn(location);
    }

    private static void setActive(
            Fixture fixture, PhysicalCard card, boolean active) {
        when(fixture.gameState.isCardInPlayActive(
                card, false, false, false, false,
                false, false, false, false)).thenReturn(active);
        when(fixture.gameState.isCardInPlayActive(
                card, true, false, false, false,
                false, false, false, false)).thenReturn(active);
        when(fixture.gameState.isCardInPlayActive(
                card, false, true, false, false,
                false, false, false, false)).thenReturn(active);
    }

    private static PhysicalCard addCastle(
            Fixture fixture, int permanentId, boolean active) {
        PhysicalCard castle = site(
                "Mustafar: Vader's Castle", permanentId);
        when(castle.getOwner()).thenReturn(PLAYER_ID);
        when(castle.getZone()).thenReturn(Zone.LOCATIONS);
        when(castle.getBlueprintId(true)).thenReturn("209_50");
        fixture.permanents.add(castle);
        for (PhysicalCard location : List.of(
                castle, fixture.battleground,
                fixture.otherBattleground)) {
            if (!fixture.permanents.contains(location)) {
                fixture.permanents.add(location);
            }
            setActive(fixture, location, true);
            when(fixture.gameState.findCardByPermanentId(
                    location.getPermanentCardId()))
                    .thenReturn(location);
            when(fixture.modifiers.getLocationHere(
                    fixture.gameState, location))
                    .thenReturn(location);
        }
        setActive(fixture, castle, active);
        return castle;
    }

    private static void prepareCastleOutbound(
            Fixture fixture, PhysicalCard castle,
            PhysicalCard mover, int permanentId,
            boolean legalDestination, float cost) {
        when(mover.getPermanentCardId()).thenReturn(permanentId);
        when(mover.getCardId()).thenReturn(permanentId);
        when(fixture.gameState.findCardByPermanentId(
                permanentId)).thenReturn(mover);
        place(fixture, mover, castle);
        setActive(fixture, mover, true);
        when(mover.getAtLocation()).thenReturn(castle);
        when(fixture.modifiers.getLocationHere(
                fixture.gameState, mover)).thenReturn(castle);
        com.gempukku.swccgo.filters.Filter validMove =
                mock(com.gempukku.swccgo.filters.Filter.class);
        when(validMove.accepts(
                fixture.gameState, fixture.modifiers,
                fixture.battleground))
                .thenReturn(legalDestination);
        when(validMove.accepts(
                fixture.gameState, fixture.modifiers,
                fixture.otherBattleground))
                .thenReturn(legalDestination);
        when(mover.getBlueprint().getValidMoveTargetFilter(
                eq(PLAYER_ID), eq(fixture.game),
                eq(mover), eq(false))).thenReturn(validMove);
        when(fixture.modifiers.getForceAvailableToUse(
                fixture.gameState, PLAYER_ID)).thenReturn(10);
        when(fixture.modifiers.getMoveUsingLocationTextCost(
                fixture.gameState, mover, castle,
                fixture.battleground, 1.0f, 0.0f))
                .thenReturn(cost);
        when(fixture.modifiers.getMoveUsingLocationTextCost(
                fixture.gameState, mover, castle,
                fixture.otherBattleground, 1.0f, 0.0f))
                .thenReturn(cost);
        when(fixture.gameState.getCardsAtLocation(
                castle)).thenReturn(List.of(mover));
        when(fixture.gameState.getCardsAtLocation(
                fixture.battleground)).thenReturn(List.of());
        when(fixture.gameState.getCardsAtLocation(
                fixture.otherBattleground)).thenReturn(List.of());
    }

    private static PhysicalCard site(String title, int permanentId) {
        PhysicalCard site = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(site.getTitle()).thenReturn(title);
        when(site.getTitles()).thenReturn(List.of(title));
        when(site.getPermanentCardId()).thenReturn(permanentId);
        when(site.getCardId()).thenReturn(permanentId);
        when(site.getAdditionalCardIds()).thenReturn(List.of());
        when(site.getBlueprint()).thenReturn(blueprint);
        when(site.isBlownAway()).thenReturn(false);
        when(blueprint.getCardCategory()).thenReturn(CardCategory.LOCATION);
        when(blueprint.getCardSubtype()).thenReturn(CardSubtype.SITE);
        return site;
    }

    private static PhysicalCard character(
            GameState gameState,
            ModifiersQuerying modifiers,
            String title,
            String owner) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getTitle()).thenReturn(title);
        when(card.getOwner()).thenReturn(owner);
        when(card.getZone()).thenReturn(Zone.AT_LOCATION);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.isCaptive()).thenReturn(false);
        when(card.isUndercover()).thenReturn(false);
        when(blueprint.getCardCategory()).thenReturn(CardCategory.CHARACTER);
        when(blueprint.hasAbilityAttribute()).thenReturn(true);
        when(blueprint.getAbility()).thenReturn(3.0f);
        when(modifiers.getAbility(gameState, card)).thenReturn(3.0f);
        when(gameState.isCardInPlayActive(
                card, false, false, false, false,
                false, false, false, false)).thenReturn(true);
        when(gameState.isCardInPlayActive(
                card, true, false, false, false,
                false, false, false, false)).thenReturn(true);
        return card;
    }

    private static PhysicalCard sourceCard(String title) {
        PhysicalCard card = mock(PhysicalCard.class);
        when(card.getTitle()).thenReturn(title);
        when(card.getTitles()).thenReturn(List.of(title));
        when(card.isBlownAway()).thenReturn(false);
        return card;
    }

    private static String label(HuntFamily family, Bot bot) {
        return family.blueprintId + " " + family.variant + " " + bot;
    }

    private enum Bot {
        RANDO {
            @Override
            ObjectiveAnalyzer analyzer() {
                return new com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer();
            }
        },
        CHOSEN_ONE {
            @Override
            ObjectiveAnalyzer analyzer() {
                return new com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer();
            }
        };

        abstract ObjectiveAnalyzer analyzer();
    }

    private enum Blocker {
        LUKE {
            @Override
            PhysicalCard card(Fixture fixture) {
                return fixture.luke;
            }
        },
        JEDI {
            @Override
            PhysicalCard card(Fixture fixture) {
                return fixture.jedi;
            }
        };

        abstract PhysicalCard card(Fixture fixture);
    }

    private record HuntFamily(
            String blueprintId,
            String variant) { }

    private record Fixture(
            ObjectiveAnalyzer analyzer,
            SwccgGame game,
            GameState gameState,
            ModifiersQuerying modifiers,
            List<PhysicalCard> permanents,
            PhysicalCard battleground,
            PhysicalCard otherBattleground,
            PhysicalCard nonBattleground,
            PhysicalCard vader,
            PhysicalCard secondVader,
            PhysicalCard opponentVader,
            PhysicalCard impostorVader,
            PhysicalCard luke,
            PhysicalCard jedi,
            PhysicalCard padawan,
            PhysicalCard buddy) { }
}
