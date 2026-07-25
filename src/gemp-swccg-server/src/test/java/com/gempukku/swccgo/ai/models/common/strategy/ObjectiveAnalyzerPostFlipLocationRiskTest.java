package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextType;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ObjectiveAnalyzerPostFlipLocationRiskTest {
    private static final String PLAYER_ID = "player";
    private static final String OPPONENT_ID = "opponent";

    private static final ObjectiveFamily RALLTIIR = new ObjectiveFamily(
            "Ralltiir Operations",
            "In The Hands Of The Empire",
            "7_300",
            "Ralltiir",
            Side.DARK);
    private static final ObjectiveFamily DANTOOINE = new ObjectiveFamily(
            "Dantooine Base Operations",
            "More Dangerous Than You Realize",
            "7_135",
            "Dantooine",
            Side.LIGHT);
    private static final ObjectiveFamily ZERO_HOUR = new ObjectiveFamily(
            "Zero Hour",
            "The Liberation Of Lothal",
            "219_48",
            "Lothal",
            Side.LIGHT);

    @Test
    public void fixedRegionalThresholdMakesTheSecondOpponentControlCritical() {
        for (ObjectiveFamily family : List.of(RALLTIIR, DANTOOINE)) {
            Fixture fixture = fixture(
                    family,
                    List.of(CardSubtype.SITE, CardSubtype.SITE,
                            CardSubtype.SITE),
                    Set.of(1), Set.of(0), Set.of(),
                    false, null);

            ObjectiveAnalyzer.PostFlipLocationRisk risk =
                    fixture.analyzer.assessPostFlipLocationRisk(
                            fixture.game, PLAYER_ID,
                            fixture.locations.get(1));

            assertTrue(family.frontTitle, risk.applies());
            assertTrue(family.frontTitle, risk.inScope());
            assertFalse(family.frontTitle, risk.flipBackNow());
            assertEquals(family.frontTitle, 1, risk.selfControlCount());
            assertEquals(family.frontTitle, 1, risk.opponentControlCount());
            assertFalse(family.frontTitle,
                    risk.criticalIfSelfControlLost());
            assertTrue(family.frontTitle,
                    risk.criticalIfOpponentGainsControl());
            assertEquals(family.frontTitle, 1,
                    risk.adverseStepsRemaining());
        }
    }

    @Test
    public void dantooineLegacyRaisesTheFixedThresholdFromTwoToThree() {
        Fixture oneOpponentControl = fixture(
                DANTOOINE,
                List.of(CardSubtype.SITE, CardSubtype.SITE,
                        CardSubtype.SITE, CardSubtype.SITE),
                Set.of(1), Set.of(0), Set.of(),
                true, null);
        ObjectiveAnalyzer.PostFlipLocationRisk twoWouldNotFlip =
                oneOpponentControl.analyzer.assessPostFlipLocationRisk(
                        oneOpponentControl.game, PLAYER_ID,
                        oneOpponentControl.locations.get(1));

        assertFalse(twoWouldNotFlip.flipBackNow());
        assertFalse(twoWouldNotFlip.criticalIfSelfControlLost());
        assertFalse(twoWouldNotFlip.criticalIfOpponentGainsControl());
        assertEquals(2, twoWouldNotFlip.adverseStepsRemaining());

        Fixture twoOpponentControls = fixture(
                DANTOOINE,
                List.of(CardSubtype.SITE, CardSubtype.SITE,
                        CardSubtype.SITE, CardSubtype.SITE),
                Set.of(2), Set.of(0, 1), Set.of(),
                true, null);
        ObjectiveAnalyzer.PostFlipLocationRisk thirdWouldFlip =
                twoOpponentControls.analyzer.assessPostFlipLocationRisk(
                        twoOpponentControls.game, PLAYER_ID,
                        twoOpponentControls.locations.get(2));

        assertFalse(thirdWouldFlip.flipBackNow());
        assertTrue(thirdWouldFlip.criticalIfOpponentGainsControl());
        assertEquals(1, thirdWouldFlip.adverseStepsRemaining());
    }

    @Test
    public void zeroHourTieIsSafeButLosingControlFromATieIsCritical() {
        Fixture tie = fixture(
                ZERO_HOUR,
                List.of(CardSubtype.SITE, CardSubtype.SITE,
                        CardSubtype.SITE, CardSubtype.SITE),
                Set.of(0, 1), Set.of(2, 3), Set.of(),
                false, null);
        ObjectiveAnalyzer.PostFlipLocationRisk tieRisk =
                tie.analyzer.assessPostFlipLocationRisk(
                        tie.game, PLAYER_ID, tie.locations.get(0));

        assertEquals(2, tieRisk.selfControlCount());
        assertEquals(2, tieRisk.opponentControlCount());
        assertFalse(tieRisk.flipBackNow());
        assertTrue(tieRisk.criticalIfSelfControlLost());
        assertTrue(tieRisk.criticalIfOpponentGainsControl());
        assertEquals(1, tieRisk.adverseStepsRemaining());

        Fixture oneLocationLead = fixture(
                ZERO_HOUR,
                List.of(CardSubtype.SITE, CardSubtype.SITE,
                        CardSubtype.SITE, CardSubtype.SITE),
                Set.of(0, 1), Set.of(2), Set.of(),
                false, null);
        ObjectiveAnalyzer.PostFlipLocationRisk leadRisk =
                oneLocationLead.analyzer.assessPostFlipLocationRisk(
                        oneLocationLead.game, PLAYER_ID,
                        oneLocationLead.locations.get(0));

        assertEquals(2, leadRisk.selfControlCount());
        assertEquals(1, leadRisk.opponentControlCount());
        assertFalse(leadRisk.flipBackNow());
        assertFalse(leadRisk.criticalIfSelfControlLost());
        assertTrue(leadRisk.criticalIfOpponentGainsControl());
        assertEquals(2, leadRisk.adverseStepsRemaining());
    }

    @Test
    public void regionalSystemsCountUsingTheExcludedFromBattleOverride() {
        Fixture fixture = fixture(
                RALLTIIR,
                List.of(CardSubtype.SYSTEM, CardSubtype.SITE,
                        CardSubtype.SITE),
                Set.of(1), Set.of(0), Set.of(),
                false, null);

        ObjectiveAnalyzer.PostFlipLocationRisk risk =
                fixture.analyzer.assessPostFlipLocationRisk(
                        fixture.game, PLAYER_ID,
                        fixture.locations.get(1));

        assertEquals(1, risk.opponentControlCount());
        assertTrue(risk.criticalIfOpponentGainsControl());
        verify(fixture.modifiers, atLeastOnce()).controlsLocation(
                fixture.gameState, fixture.locations.get(0), OPPONENT_ID,
                SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE);
    }

    @Test
    public void departureOnlyTriggersForAContestedSoleBlocker() {
        Fixture contested = fixture(
                RALLTIIR,
                List.of(CardSubtype.SITE, CardSubtype.SITE,
                        CardSubtype.SITE),
                Set.of(1), Set.of(0), Set.of(1),
                false, 1);
        assertTrue(contested.analyzer.wouldDepartureTriggerFlipBack(
                contested.game, PLAYER_ID, contested.mover));

        Fixture uncontested = fixture(
                RALLTIIR,
                List.of(CardSubtype.SITE, CardSubtype.SITE,
                        CardSubtype.SITE),
                Set.of(1), Set.of(0), Set.of(),
                false, 1);
        assertFalse(uncontested.analyzer.wouldDepartureTriggerFlipBack(
                uncontested.game, PLAYER_ID, uncontested.mover));
    }

    private static Fixture fixture(
            ObjectiveFamily family,
            List<CardSubtype> locationTypes,
            Set<Integer> selfControlled,
            Set<Integer> opponentControlled,
            Set<Integer> opponentOccupied,
            boolean dantooineLegacy,
            Integer moverLocationIndex) {
        ObjectiveAnalyzer analyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer();
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
        when(objective.getOwner()).thenReturn(PLAYER_ID);
        when(objective.getZone()).thenReturn(Zone.SIDE_OF_TABLE);
        when(objective.getBlueprint()).thenReturn(front);
        when(objective.getOtherSideBlueprint()).thenReturn(back);
        when(objective.getBlueprintId(true)).thenReturn(family.blueprintId);
        when(objective.isFlipped()).thenReturn(true);
        when(front.getTitle()).thenReturn(family.frontTitle);
        when(front.getGameText()).thenReturn("Flip this card.");
        when(front.getCardCategory()).thenReturn(CardCategory.OBJECTIVE);
        when(back.getTitle()).thenReturn(family.backTitle);
        when(back.getGameText()).thenReturn("Flip this card.");

        List<PhysicalCard> locations = new ArrayList<>();
        for (int i = 0; i < locationTypes.size(); i++) {
            CardSubtype subtype = locationTypes.get(i);
            String title = subtype == CardSubtype.SYSTEM
                    ? family.systemName
                    : family.systemName + ": Site " + (i + 1);
            PhysicalCard location =
                    location(title, family.systemName, subtype);
            locations.add(location);
            when(modifiers.controlsLocation(
                    gameState, location, PLAYER_ID,
                    SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE))
                    .thenReturn(selfControlled.contains(i));
            when(modifiers.controlsLocation(
                    gameState, location, OPPONENT_ID,
                    SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE))
                    .thenReturn(opponentControlled.contains(i));
            when(modifiers.occupiesLocation(
                    gameState, location, OPPONENT_ID,
                    SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE))
                    .thenReturn(opponentControlled.contains(i)
                            || opponentOccupied.contains(i));
        }
        when(gameState.getLocationsInOrder()).thenReturn(locations);
        when(gameState.getTopLocations()).thenReturn(locations);

        PhysicalCard mover = null;
        List<PhysicalCard> permanents = new ArrayList<>();
        permanents.add(objective);
        if (moverLocationIndex != null) {
            mover = presenceSource(gameState, modifiers);
            permanents.add(mover);
            when(modifiers.getLocationThatCardIsPresentAt(
                    gameState, mover))
                    .thenReturn(locations.get(moverLocationIndex));
        }
        when(gameState.getAllPermanentCards()).thenReturn(permanents);

        if (dantooineLegacy) {
            when(modifiers.hasGameTextModification(
                    gameState, objective,
                    ModifyGameTextType
                            .LEGACY__MORE_DANGEROUS_THAN_YOU_REALIZE__REQUIRES_THREE_SITES_TO_FLIP_BACK))
                    .thenReturn(true);
        }

        analyzer.analyze(game, PLAYER_ID, family.side);
        return new Fixture(
                analyzer, game, gameState, modifiers,
                locations, mover);
    }

    private static PhysicalCard location(
            String title, String systemName, CardSubtype subtype) {
        PhysicalCard location = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(location.getTitle()).thenReturn(title);
        when(location.getTitles()).thenReturn(List.of(title));
        when(location.getPartOfSystem()).thenReturn(systemName);
        when(location.getBlueprint()).thenReturn(blueprint);
        when(location.isBlownAway()).thenReturn(false);
        when(blueprint.getCardCategory()).thenReturn(CardCategory.LOCATION);
        when(blueprint.getCardSubtype()).thenReturn(subtype);
        return location;
    }

    private static PhysicalCard presenceSource(
            GameState gameState, ModifiersQuerying modifiers) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getOwner()).thenReturn(PLAYER_ID);
        when(card.isUndercover()).thenReturn(false);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getCardCategory()).thenReturn(CardCategory.CHARACTER);
        when(modifiers.hasAbility(gameState, card, true)).thenReturn(true);
        return card;
    }

    private record ObjectiveFamily(
            String frontTitle,
            String backTitle,
            String blueprintId,
            String systemName,
            Side side) { }

    private record Fixture(
            ObjectiveAnalyzer analyzer,
            SwccgGame game,
            GameState gameState,
            ModifiersQuerying modifiers,
            List<PhysicalCard> locations,
            PhysicalCard mover) { }
}
