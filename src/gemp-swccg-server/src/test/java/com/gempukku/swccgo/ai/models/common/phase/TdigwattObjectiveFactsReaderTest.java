package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.DestinyType;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Species;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.PhysicalCardVisitor;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.BattleState;
import com.gempukku.swccgo.game.state.DrawDestinyState;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.effects.DrawDestinyEffect;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextType;
import com.gempukku.swccgo.logic.modifiers.ModifierType;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TdigwattObjectiveFactsReaderTest {
    private static final String DARK = "dark";
    private static final String LIGHT = "light";

    @Test
    public void identityUsesExactFrontBlueprintAndPermanentId() {
        Fixture fixture = fixture("109_12", false);

        Optional<TdigwattObjectiveFacts.ObjectiveIdentity> identity =
                TdigwattObjectiveFactsReader
                    .readObjectiveIdentity(
                        fixture.game, DARK);

        assertTrue(identity.isPresent());
        assertEquals(101,
                identity.get().physicalCardId());
        assertEquals(
                TdigwattObjectiveFacts.Printing.CLASSIC,
                identity.get().printing());

        when(fixture.objective.getBlueprintId(true))
                .thenReturn("109_12_BACK");
        assertTrue(TdigwattObjectiveFactsReader
                .readObjectiveIdentity(
                    fixture.game, DARK).isEmpty());

        when(fixture.objective.getBlueprintId(true))
                .thenReturn("109_12");
        when(fixture.objective
                .isObjectiveDeploymentComplete())
                .thenReturn(false);
        assertTrue(TdigwattObjectiveFactsReader
                .readObjectiveIdentity(
                    fixture.game, DARK).isEmpty());
    }

    @Test
    public void classicReadsOccupationWithoutRequiringUncontestedControl() {
        Fixture fixture = fixture("109_12", false);
        PhysicalCard bespinSystem = location(
                201, Title.Bespin,
                CardSubtype.SYSTEM, Title.Bespin);
        PhysicalCard cloudCity = location(
                202, Title.Bespin_Cloud_City,
                CardSubtype.SITE, Title.Bespin);
        when(fixture.gameState.getTopLocations())
                .thenReturn(List.of(
                        bespinSystem, cloudCity));
        when(fixture.modifiers.occupiesLocation(
                fixture.gameState, bespinSystem, DARK,
                SpotOverride
                    .INCLUDE_EXCLUDED_FROM_BATTLE))
                .thenReturn(true);
        when(fixture.modifiers.occupiesLocation(
                fixture.gameState, cloudCity, DARK,
                SpotOverride
                    .INCLUDE_EXCLUDED_FROM_BATTLE))
                .thenReturn(true);
        when(fixture.modifiers.controlsLocation(
                fixture.gameState, bespinSystem, LIGHT,
                SpotOverride
                    .INCLUDE_EXCLUDED_FROM_BATTLE))
                .thenReturn(false);

        Optional<TdigwattObjectiveFacts.ClassicState> state =
                TdigwattObjectiveFactsReader
                    .readClassicFrontState(
                        fixture.game, DARK);

        assertTrue(state.isPresent());
        assertTrue(state.get()
                .darkOccupiesBespinSystem());
        assertTrue(state.get()
                .darkOccupiesBespinCloudCity());
        verify(fixture.modifiers, never())
                .controlsLocation(
                    fixture.gameState, bespinSystem,
                    DARK,
                    SpotOverride
                        .INCLUDE_EXCLUDED_FROM_BATTLE);
        verify(fixture.modifiers, never())
                .controlsLocation(
                    fixture.gameState, cloudCity,
                    DARK,
                    SpotOverride
                        .INCLUDE_EXCLUDED_FROM_BATTLE);
    }

    @Test
    public void virtualCountsBothSidesAndPreservesTie() {
        Fixture fixture = fixture("226_12", true);
        PhysicalCard first = location(
                211, "Bespin: First",
                CardSubtype.SITE, Title.Bespin);
        PhysicalCard second = location(
                212, "Bespin: Second",
                CardSubtype.SITE, Title.Bespin);
        PhysicalCard third = location(
                213, "Bespin: Third",
                CardSubtype.SITE, Title.Bespin);
        PhysicalCard fourth = location(
                214, "Bespin: Fourth",
                CardSubtype.SITE, Title.Bespin);
        when(fixture.gameState.getTopLocations())
                .thenReturn(List.of(
                        first, second, third, fourth));
        when(fixture.modifiers.controlsLocation(
                fixture.gameState, first, DARK,
                SpotOverride
                    .INCLUDE_EXCLUDED_FROM_BATTLE))
                .thenReturn(true);
        when(fixture.modifiers.controlsLocation(
                fixture.gameState, second, DARK,
                SpotOverride
                    .INCLUDE_EXCLUDED_FROM_BATTLE))
                .thenReturn(true);
        when(fixture.modifiers.controlsLocation(
                fixture.gameState, third, LIGHT,
                SpotOverride
                    .INCLUDE_EXCLUDED_FROM_BATTLE))
                .thenReturn(true);
        when(fixture.modifiers.controlsLocation(
                fixture.gameState, fourth, LIGHT,
                SpotOverride
                    .INCLUDE_EXCLUDED_FROM_BATTLE))
                .thenReturn(true);

        TdigwattObjectiveFacts.VirtualState state =
                TdigwattObjectiveFactsReader
                    .readVirtualState(
                        fixture.game, DARK)
                    .orElseThrow();

        assertEquals(2,
                state.darkControlledBespinLocations());
        assertEquals(2,
                state.lightControlledBespinLocations());
        assertTrue(TdigwattObjectivePolicy
                .virtualStableBack(state));
    }

    @Test
    public void virtualDeployProjectionAddsOnlyProvenBespinControl() {
        Fixture fixture = fixture("226_12", false);
        PhysicalCard first = location(
                215, "Bespin: First",
                CardSubtype.SITE, Title.Bespin);
        PhysicalCard second = location(
                216, "Bespin: Second",
                CardSubtype.SITE, Title.Bespin);
        PhysicalCard third = location(
                217, "Bespin: Third",
                CardSubtype.SITE, Title.Bespin);
        PhysicalCard maul =
                character(218, "Darth Maul");
        when(maul.getOwner()).thenReturn(DARK);
        when(maul.getZone()).thenReturn(Zone.HAND);
        when(fixture.gameState.getTopLocations())
                .thenReturn(List.of(
                        first, second, third));
        when(fixture.modifiers.controlsLocation(
                fixture.gameState, first, DARK,
                SpotOverride
                    .INCLUDE_EXCLUDED_FROM_BATTLE))
                .thenReturn(true);
        when(fixture.modifiers.controlsLocation(
                fixture.gameState, second, DARK,
                SpotOverride
                    .INCLUDE_EXCLUDED_FROM_BATTLE))
                .thenReturn(true);
        when(fixture.modifiers.getAbility(
                fixture.gameState, maul))
                .thenReturn(6.0f);
        when(fixture.modifiers
                .getTotalAbilityAtLocation(
                    fixture.gameState, DARK,
                    third))
                .thenReturn(0.0f);
        when(fixture.modifiers
                .getModifiersAffectingCard(
                    fixture.gameState,
                    ModifierType
                        .ABILITY_REQUIRED_TO_CONTROL_LOCATION,
                    third))
                .thenReturn(List.of());

        TdigwattObjectiveFactsReader
                .VirtualDeployProjection projection =
            TdigwattObjectiveFactsReader
                .readVirtualDeployProjection(
                    fixture.game, DARK,
                    maul, third)
                .orElseThrow();
        assertEquals(2, projection.before()
                .darkControlledBespinLocations());
        assertEquals(3, projection.after()
                .darkControlledBespinLocations());
        assertTrue(TdigwattObjectivePolicy
                .virtualFlipReady(projection.after()));

        when(fixture.modifiers.hasPresenceAt(
                fixture.gameState, LIGHT,
                third, false, null,
                SpotOverride
                    .INCLUDE_EXCLUDED_FROM_BATTLE))
                .thenReturn(true);
        assertTrue(TdigwattObjectiveFactsReader
                .readVirtualDeployProjection(
                    fixture.game, DARK,
                    maul, third)
                .isEmpty());
    }

    @Test
    public void enginePersistenceUsesExactPrintCancellationLaw() {
        Fixture fixture = fixture("109_12", false);
        PhysicalCard classicDarkDeal =
                effect(221, "5_115");
        PhysicalCard virtualDarkDeal =
                effect(222, "223_9");
        PhysicalCard occupation =
                effect(223, "7_223");
        PhysicalCard bespinSystem = location(
                224, Title.Bespin,
                CardSubtype.SYSTEM, Title.Bespin);
        PhysicalCard first = location(
                225, "Bespin: First",
                CardSubtype.SITE, Title.Bespin);
        PhysicalCard second = location(
                226, "Bespin: Second",
                CardSubtype.SITE, Title.Bespin);
        PhysicalCard third = location(
                227, "Bespin: Third",
                CardSubtype.SITE, Title.Bespin);
        when(fixture.gameState.getTopLocations())
                .thenReturn(List.of(
                        bespinSystem, first,
                        second, third));
        when(fixture.modifiers
                .getGameTextModificationCount(
                    fixture.gameState,
                    classicDarkDeal,
                    ModifyGameTextType
                        .DARK_DEAL__ADDITIONAL_BESPIN_LOCATION_TO_CANCEL))
                .thenReturn(0);

        assertEquals(
                Optional.of(true),
                TdigwattObjectiveFactsReader
                    .readEngineEffectPersistsAfterDeploy(
                        fixture.game, DARK,
                        virtualDarkDeal));
        assertEquals(
                Optional.of(true),
                TdigwattObjectiveFactsReader
                    .readEngineEffectPersistsAfterDeploy(
                        fixture.game, DARK,
                        occupation));

        when(fixture.modifiers.controlsLocation(
                fixture.gameState, bespinSystem,
                LIGHT,
                SpotOverride
                    .INCLUDE_EXCLUDED_FROM_BATTLE))
                .thenReturn(true);
        assertEquals(
                Optional.of(false),
                TdigwattObjectiveFactsReader
                    .readEngineEffectPersistsAfterDeploy(
                        fixture.game, DARK,
                        occupation));

        for (PhysicalCard location : List.of(
                bespinSystem, first, second, third)) {
            when(fixture.modifiers.occupiesLocation(
                    fixture.gameState, location,
                    LIGHT,
                    SpotOverride
                        .INCLUDE_EXCLUDED_FROM_BATTLE))
                    .thenReturn(true);
        }
        assertEquals(
                Optional.of(false),
                TdigwattObjectiveFactsReader
                    .readEngineEffectPersistsAfterDeploy(
                        fixture.game, DARK,
                        classicDarkDeal));
        when(fixture.modifiers.occupiesLocation(
                fixture.gameState, third,
                LIGHT,
                SpotOverride
                    .INCLUDE_EXCLUDED_FROM_BATTLE))
                .thenReturn(false);
        assertEquals(
                Optional.of(true),
                TdigwattObjectiveFactsReader
                    .readEngineEffectPersistsAfterDeploy(
                        fixture.game, DARK,
                        classicDarkDeal));

        assertTrue(TdigwattObjectiveFactsReader
                .readEngineEffectPersistsAfterDeploy(
                    fixture.game, DARK,
                    effect(228, "999_999"))
                .isEmpty());
    }

    @Test
    public void pullClassificationNeverMergesThePrintingLists() {
        Fixture classic = fixture("109_12", false);
        PhysicalCard occupation = reserveCard(
                301, "109_99",
                Title.Cloud_City_Occupation,
                null, false);
        PhysicalCard bounty = reserveCard(
                302, "226_99",
                Title.Vaders_Bounty,
                null, false);
        when(classic.gameState.getReserveDeck(DARK))
                .thenReturn(List.of(occupation, bounty));

        TdigwattObjectiveFacts.PullFacts classicPull =
                TdigwattObjectiveFactsReader
                    .readSourceLegalPullCandidate(
                        classic.game, DARK,
                        classic.objective, occupation)
                    .orElseThrow();
        assertEquals(
                TdigwattObjectiveFacts.PullTarget
                    .CLOUD_CITY_OCCUPATION,
                classicPull.target());
        assertTrue(TdigwattObjectiveFactsReader
                .readSourceLegalPullCandidate(
                    classic.game, DARK,
                    classic.objective, bounty)
                .isEmpty());
        TdigwattObjectiveFacts.PullFacts knownDecoy =
                TdigwattObjectiveFactsReader
                    .readObjectivePullCandidate(
                        classic.game, DARK,
                        classic.objective, bounty)
                    .orElseThrow();
        assertEquals(
                TdigwattObjectiveFacts.PullTarget
                    .VADERS_BOUNTY,
                knownDecoy.target());
        assertFalse(TdigwattObjectivePolicy
                .sourceLegalPull(knownDecoy));
        assertEquals(
                List.of(classicPull),
                TdigwattObjectiveFactsReader
                    .readSourceLegalPullCandidatesInReserve(
                        classic.game, DARK,
                        classic.objective)
                    .orElseThrow());
        assertEquals(
                Optional.of(true),
                TdigwattObjectiveFactsReader
                    .hasAnySourceLegalPullInReserve(
                        classic.game, DARK,
                        classic.objective));

        when(classic.gameState.getReserveDeck(DARK))
                .thenReturn(List.of(bounty));
        assertEquals(
                Optional.of(false),
                TdigwattObjectiveFactsReader
                    .hasAnySourceLegalPullInReserve(
                        classic.game, DARK,
                        classic.objective));
        when(classic.gameState.getReserveDeck(DARK))
                .thenReturn(null);
        assertTrue(TdigwattObjectiveFactsReader
                .hasAnySourceLegalPullInReserve(
                    classic.game, DARK,
                    classic.objective)
                .isEmpty());

        Fixture virtual = fixture("226_12", false);
        PhysicalCard nonSpecialEditionBespin =
                reserveCard(
                    303, "1_123",
                    Title.Bespin,
                    CardSubtype.SYSTEM, false);
        PhysicalCard specialEditionBespin =
                reserveCard(
                    304, "7_123",
                    Title.Bespin,
                    CardSubtype.SYSTEM, true);
        when(virtual.gameState.getReserveDeck(DARK))
                .thenReturn(List.of(
                        nonSpecialEditionBespin,
                        specialEditionBespin,
                        occupation));

        assertTrue(TdigwattObjectiveFactsReader
                .readSourceLegalPullCandidate(
                    virtual.game, DARK,
                    virtual.objective,
                    nonSpecialEditionBespin)
                .isEmpty());
        TdigwattObjectiveFacts.PullFacts virtualPull =
                TdigwattObjectiveFactsReader
                    .readSourceLegalPullCandidate(
                        virtual.game, DARK,
                        virtual.objective,
                        specialEditionBespin)
                    .orElseThrow();
        assertEquals(
                TdigwattObjectiveFacts.PullTarget
                    .BESPIN_SYSTEM,
                virtualPull.target());
        assertTrue(virtualPull.specialEditionPrint());
        assertTrue(TdigwattObjectiveFactsReader
                .readSourceLegalPullCandidate(
                    virtual.game, DARK,
                    virtual.objective, occupation)
                .isEmpty());
    }

    @Test
    public void liveBattleUsesExactParticipantsAndAnyLobotOwner() {
        Fixture fixture = fixture("226_12", true);
        PhysicalCard alien = card(401, "Alien");
        PhysicalCard imperial = card(402, "Imperial");
        PhysicalCard lando = card(403, "Lando");
        PhysicalCard opponentsLobot =
                card(404, "Lobot");
        when(alien.getOwner()).thenReturn(DARK);
        when(imperial.getOwner()).thenReturn(DARK);
        when(lando.getOwner()).thenReturn(DARK);
        when(opponentsLobot.getOwner())
                .thenReturn(LIGHT);
        when(fixture.modifiers.hasIcon(
                fixture.gameState, alien,
                Icon.ALIEN)).thenReturn(true);
        when(fixture.modifiers.hasIcon(
                fixture.gameState, imperial,
                Icon.IMPERIAL)).thenReturn(true);
        when(fixture.modifiers.isSpecies(
                fixture.gameState, alien,
                Species.UGNAUGHT)).thenReturn(true);
        when(fixture.modifiers.hasPersona(
                fixture.gameState, lando,
                Persona.LANDO)).thenReturn(true);
        when(fixture.modifiers.hasPersona(
                fixture.gameState, opponentsLobot,
                Persona.LOBOT)).thenReturn(true);
        when(fixture.modifiers.isWith(
                fixture.gameState, alien,
                imperial)).thenReturn(true);
        BattleState battle = mock(BattleState.class);
        when(fixture.gameState.getBattleState())
                .thenReturn(battle);
        when(battle.getCardsParticipating(DARK))
                .thenReturn(List.of(
                        alien, imperial, lando));
        when(battle.getAllCardsParticipating())
                .thenReturn(List.of(
                        alien, imperial, lando,
                        opponentsLobot));

        TdigwattObjectiveFacts.BattleFacts facts =
                TdigwattObjectiveFactsReader
                    .readLiveBackSideBattleFacts(
                        fixture.game, DARK)
                    .orElseThrow();

        assertTrue(facts.yourAlienInBattle());
        assertTrue(facts.yourImperialInBattle());
        assertTrue(facts.yourUgnaughtInBattle());
        assertTrue(facts.yourLandoInBattle());
        assertTrue(facts.anyLobotParticipating());
        assertEquals(2,
                TdigwattObjectivePolicy
                    .battlePayoff(facts)
                    .landoDestinyAdjustments());
    }

    @Test
    public void liveDestinyAdjustmentReadsExactDrawerAndLobotCeiling() {
        Fixture fixture = fixture("226_12", true);
        PhysicalCard lando = card(421, "Lando");
        PhysicalCard opponentsLobot =
                card(422, "Lobot");
        when(lando.getOwner()).thenReturn(DARK);
        when(opponentsLobot.getOwner())
                .thenReturn(LIGHT);
        when(fixture.modifiers.hasPersona(
                fixture.gameState, lando,
                Persona.LANDO)).thenReturn(true);
        when(fixture.modifiers.hasPersona(
                fixture.gameState, opponentsLobot,
                Persona.LOBOT)).thenReturn(true);
        BattleState battle = mock(BattleState.class);
        when(fixture.gameState.getBattleState())
                .thenReturn(battle);
        when(battle.getCardsParticipating(DARK))
                .thenReturn(List.of(lando));
        when(battle.getAllCardsParticipating())
                .thenReturn(List.of(
                        lando, opponentsLobot));
        DrawDestinyState drawState =
                mock(DrawDestinyState.class);
        DrawDestinyEffect drawEffect =
                mock(DrawDestinyEffect.class);
        when(fixture.gameState
                .getTopDrawDestinyState())
                .thenReturn(drawState);
        when(drawState.getDrawDestinyEffect())
                .thenReturn(drawEffect);
        when(drawEffect.getPlayerDrawingDestiny())
                .thenReturn(DARK);
        when(drawEffect.getDestinyType())
                .thenReturn(DestinyType.BATTLE_DESTINY);

        TdigwattObjectiveFacts.DestinyAdjustmentFacts
                ownDraw =
            TdigwattObjectiveFactsReader
                .readLiveDestinyAdjustmentFacts(
                    fixture.game, DARK,
                    fixture.objective)
                .orElseThrow();

        assertEquals(
                TdigwattObjectiveFacts
                    .DestinyDrawOwner.YOURS,
                ownDraw.drawOwner());
        assertEquals(2, ownDraw.usesPerBattle());
        assertEquals(
                DestinyType.BATTLE_DESTINY,
                ownDraw.destinyType());
        assertEquals(101,
                ownDraw.actionSourcePhysicalCardId());

        when(drawEffect.getPlayerDrawingDestiny())
                .thenReturn(LIGHT);
        assertEquals(
                TdigwattObjectiveFacts
                    .DestinyDrawOwner.OPPONENTS,
                TdigwattObjectiveFactsReader
                    .readLiveDestinyAdjustmentFacts(
                        fixture.game, DARK,
                        fixture.objective)
                    .orElseThrow()
                    .drawOwner());

        when(drawEffect.getPlayerDrawingDestiny())
                .thenReturn("unknown");
        assertTrue(TdigwattObjectiveFactsReader
                .readLiveDestinyAdjustmentFacts(
                    fixture.game, DARK,
                    fixture.objective)
                .isEmpty());
        when(drawEffect.getPlayerDrawingDestiny())
                .thenReturn(DARK);
        when(drawEffect.getDestinyType())
                .thenReturn(null);
        assertTrue(TdigwattObjectiveFactsReader
                .readLiveDestinyAdjustmentFacts(
                    fixture.game, DARK,
                    fixture.objective)
                .isEmpty());
        assertTrue(TdigwattObjectiveFactsReader
                .readLiveDestinyAdjustmentFacts(
                    fixture.game, DARK,
                    card(423, "Wrong Source"))
                .isEmpty());
    }

    @Test
    public void prospectiveBattleUsesEligibleCardsAtExactTarget() {
        Fixture fixture = fixture("226_12", true);
        PhysicalCard target = location(
                451, "Bespin: Battle Site",
                CardSubtype.SITE, Title.Bespin);
        PhysicalCard alien = character(452, "Alien");
        PhysicalCard imperial =
                character(453, "Imperial");
        PhysicalCard lando = character(454, "Lando");
        PhysicalCard opponentsLobot =
                character(455, "Lobot");
        when(alien.getOwner()).thenReturn(DARK);
        when(imperial.getOwner()).thenReturn(DARK);
        when(lando.getOwner()).thenReturn(DARK);
        when(opponentsLobot.getOwner())
                .thenReturn(LIGHT);
        when(fixture.gameState.getTopLocations())
                .thenReturn(List.of(target));
        when(fixture.gameState
                .findCardByPermanentId(451))
                .thenReturn(target);
        for (PhysicalCard participant : List.of(
                alien, imperial, lando,
                opponentsLobot)) {
            when(fixture.modifiers
                    .getLocationThatCardIsAt(
                        fixture.gameState,
                        participant))
                    .thenReturn(target);
        }
        when(fixture.modifiers.hasIcon(
                fixture.gameState, alien,
                Icon.ALIEN)).thenReturn(true);
        when(fixture.modifiers.hasIcon(
                fixture.gameState, imperial,
                Icon.IMPERIAL)).thenReturn(true);
        when(fixture.modifiers.hasPersona(
                fixture.gameState, lando,
                Persona.LANDO)).thenReturn(true);
        when(fixture.modifiers.hasPersona(
                fixture.gameState, opponentsLobot,
                Persona.LOBOT)).thenReturn(true);
        when(fixture.modifiers.isWith(
                fixture.gameState, alien,
                imperial)).thenReturn(true);
        activeCards(fixture, List.of(
                alien, imperial, lando,
                opponentsLobot));

        TdigwattObjectiveFacts.BattleFacts facts =
                TdigwattObjectiveFactsReader
                    .readBackSideBattleFactsAtLocation(
                        fixture.game, DARK, target)
                    .orElseThrow();

        assertTrue(facts.yourAlienInBattle());
        assertTrue(facts.yourImperialInBattle());
        assertTrue(facts.yourLandoInBattle());
        assertTrue(facts.anyLobotParticipating());
        assertEquals(2,
                TdigwattObjectivePolicy
                    .battlePayoff(facts)
                    .landoDestinyAdjustments());

        PhysicalCard unknownTarget = location(
                456, "Bespin: Unknown",
                CardSubtype.SITE, Title.Bespin);
        assertTrue(TdigwattObjectiveFactsReader
                .readBackSideBattleFactsAtLocation(
                    fixture.game, DARK,
                    unknownTarget)
                .isEmpty());
    }

    @Test
    public void landspeedRouteRequiresEveryProofAndUsesExactCost() {
        Fixture fixture = fixture("226_12", false);
        PhysicalCard origin = location(
                501, "Bespin: Origin",
                CardSubtype.SITE, Title.Bespin);
        PhysicalCard destination = location(
                502, "Bespin: Destination",
                CardSubtype.SITE, Title.Bespin);
        PhysicalCard lando = card(503, "Lando");
        when(lando.getOwner()).thenReturn(DARK);
        when(lando.getZone()).thenReturn(
                Zone.AT_LOCATION);
        when(lando.getAtLocation()).thenReturn(origin);
        when(lando.getBlueprint()
                .getCardCategory())
                .thenReturn(CardCategory.CHARACTER);
        when(lando.getBlueprint()
                .isMovesLikeCharacter())
                .thenReturn(false);
        when(fixture.modifiers.hasPersona(
                fixture.gameState, lando,
                Persona.LANDO)).thenReturn(true);
        when(fixture.gameState
                .findCardByPermanentId(503))
                .thenReturn(lando);
        when(fixture.gameState.getTopLocations())
                .thenReturn(List.of(
                        origin, destination));
        when(fixture.modifiers
                .getLocationThatCardIsAt(
                    fixture.gameState, lando))
                .thenReturn(origin);
        when(fixture.modifiers
                .getLandspeedRequired(
                    fixture.gameState, lando,
                    destination))
                .thenReturn(1);
        when(fixture.modifiers.getLandspeed(
                fixture.gameState, lando))
                .thenReturn(1.0f);
        when(fixture.modifiers
                .getForceAvailableToUse(
                    fixture.gameState, DARK))
                .thenReturn(10);
        when(fixture.modifiers
                .getMoveUsingLandspeedCost(
                    fixture.gameState, lando,
                    origin, destination,
                    false, 0.0f))
                .thenReturn(1.25f);

        TdigwattObjectiveFacts.LandoMoveFacts route =
                TdigwattObjectiveFactsReader
                    .readVirtualLandoLandspeedRoute(
                        fixture.game, DARK,
                        fixture.objective,
                        lando, origin, destination,
                        TdigwattObjectiveFactsReader
                            .Proof.PROVEN,
                        TdigwattObjectiveFactsReader
                            .Proof.PROVEN,
                        TdigwattObjectiveFactsReader
                            .Proof.PROVEN)
                    .orElseThrow();

        assertEquals(2, route.requiredForceCost());
        assertEquals(101,
                route.actionSourcePhysicalCardId());
        assertTrue(route.sourceActionAvailable());
        assertTrue(route.exactRouteKnown());
        assertTrue(route.legalDestinationExists());
        assertTrue(route.advancesOrProtectsObjective());
        assertTrue(route.formationSafe());
        assertTrue(TdigwattObjectiveFactsReader
                .readVirtualLandoLandspeedRoute(
                    fixture.game, DARK,
                    fixture.objective,
                    lando, origin, destination,
                    TdigwattObjectiveFactsReader
                        .Proof.PROVEN,
                    TdigwattObjectiveFactsReader
                        .Proof.PROVEN,
                    TdigwattObjectiveFactsReader
                        .Proof.UNKNOWN)
                .isEmpty());
    }

    private static Fixture fixture(
            String blueprintId, boolean flipped) {
        SwccgGame game = mock(SwccgGame.class);
        GameState gameState = mock(GameState.class);
        ModifiersQuerying modifiers =
                mock(ModifiersQuerying.class);
        PhysicalCard objective =
                objective(blueprintId, flipped);
        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying())
                .thenReturn(modifiers);
        when(game.getDarkPlayer()).thenReturn(DARK);
        when(gameState.getObjectivePlayed(DARK))
                .thenReturn(objective);
        when(gameState.getOpponent(DARK))
                .thenReturn(LIGHT);
        when(gameState.getTopLocations())
                .thenReturn(List.of());
        when(gameState.getReserveDeck(DARK))
                .thenReturn(List.of());
        return new Fixture(
                game, gameState, modifiers,
                objective);
    }

    private static PhysicalCard objective(
            String blueprintId, boolean flipped) {
        PhysicalCard objective =
                card(101, "This Deal");
        when(objective.getOwner()).thenReturn(DARK);
        when(objective.getZone()).thenReturn(
                Zone.SIDE_OF_TABLE);
        when(objective.isObjectiveDeploymentComplete())
                .thenReturn(true);
        when(objective.getBlueprintId(true))
                .thenReturn(blueprintId);
        when(objective.isFlipped()).thenReturn(flipped);
        return objective;
    }

    private static PhysicalCard reserveCard(
            int permanentId,
            String blueprintId,
            String title,
            CardSubtype subtype,
            boolean specialEdition) {
        PhysicalCard card =
                card(permanentId, title);
        when(card.getOwner()).thenReturn(DARK);
        when(card.getZone()).thenReturn(
                Zone.RESERVE_DECK);
        when(card.getBlueprintId(true))
                .thenReturn(blueprintId);
        when(card.getBlueprint().getCardSubtype())
                .thenReturn(subtype);
        when(card.getBlueprint().hasIcon(
                Icon.SPECIAL_EDITION))
                .thenReturn(specialEdition);
        return card;
    }

    private static PhysicalCard effect(
            int permanentId, String blueprintId) {
        PhysicalCard card =
                card(permanentId, "Engine Effect");
        when(card.getOwner()).thenReturn(DARK);
        when(card.getZone()).thenReturn(Zone.HAND);
        when(card.getBlueprintId(true))
                .thenReturn(blueprintId);
        return card;
    }

    private static PhysicalCard location(
            int permanentId,
            String title,
            CardSubtype subtype,
            String partOfSystem) {
        PhysicalCard card =
                card(permanentId, title);
        when(card.getBlueprint().getCardCategory())
                .thenReturn(CardCategory.LOCATION);
        when(card.getBlueprint().getCardSubtype())
                .thenReturn(subtype);
        when(card.getPartOfSystem())
                .thenReturn(partOfSystem);
        when(card.getZone()).thenReturn(
                Zone.LOCATIONS);
        return card;
    }

    private static PhysicalCard character(
            int permanentId, String title) {
        PhysicalCard card =
                card(permanentId, title);
        when(card.getBlueprint().getCardCategory())
                .thenReturn(CardCategory.CHARACTER);
        when(card.getZone()).thenReturn(
                Zone.AT_LOCATION);
        return card;
    }

    private static void activeCards(
            Fixture fixture,
            List<PhysicalCard> cards) {
        doAnswer(invocation -> {
            PhysicalCardVisitor visitor =
                    invocation.getArgument(0);
            for (PhysicalCard card : cards) {
                if (visitor.visitPhysicalCard(card)) {
                    return true;
                }
            }
            return false;
        }).when(fixture.gameState)
                .iterateActiveCards(
                    any(PhysicalCardVisitor.class),
                    eq(fixture.modifiers),
                    any(PhysicalCard.class),
                    isNull(),
                    isNull());
    }

    private static PhysicalCard card(
            int permanentId, String title) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint =
                mock(SwccgCardBlueprint.class);
        when(card.getPermanentCardId())
                .thenReturn(permanentId);
        when(card.getCardId()).thenReturn(permanentId);
        when(card.getTitle()).thenReturn(title);
        when(card.getTitles())
                .thenReturn(List.of(title));
        when(card.getBlueprint())
                .thenReturn(blueprint);
        return card;
    }

    private record Fixture(
            SwccgGame game,
            GameState gameState,
            ModifiersQuerying modifiers,
            PhysicalCard objective) {
    }
}
