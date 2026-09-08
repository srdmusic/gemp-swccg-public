package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KyloBattleKitFactsReaderTest {
    private static final String PLAYER = "dark";
    private static final String OPPONENT = "light";

    @Test
    public void readsExactPersonaGroundRouteAndPostPayCosts() {
        Fixture fixture = fixture();
        fixture.allow(fixture.source, 1.0f);

        DeployWeaponPolicy.KyloBattleKitActionFacts facts =
                fixture.readAction("101");

        assertTrue(facts.kylosLightsaber());
        assertTrue(facts.hasEngineLegalKyloTarget());
        assertTrue(facts.hasEngineLegalGroundSiteKyloTarget());
        assertTrue(facts.hasKyloAtPlannedBattleTarget());
        assertTrue(facts.hasEngineLegalPlannedBattleTarget());
        assertTrue(facts.battleCanInitiate());
        assertEquals(1, facts.exactDeployPayment());
        assertEquals(1, facts.exactBattlePayment());
        assertEquals(2, facts.forceAvailable());
    }

    @Test
    public void requiresTheExactLightsaberAndKyloPersonas() {
        Fixture fixture = fixture();
        fixture.allow(fixture.source, 1.0f);
        when(fixture.saber.getBlueprint()
                .hasPersona(Persona.KYLOS_LIGHTSABER))
                .thenReturn(false);

        assertTrue(KyloBattleKitFactsReader.readAction(
                "a", fixture.game, PLAYER,
                fixture.source, fixture.saber,
                "101", false, 0.0f).isEmpty());

        when(fixture.saber.getBlueprint()
                .hasPersona(Persona.KYLOS_LIGHTSABER))
                .thenReturn(true);
        when(fixture.modifiers.hasPersona(
                fixture.gameState, fixture.kylo, Persona.KYLO))
                .thenReturn(false);

        DeployWeaponPolicy.KyloBattleKitActionFacts facts =
                fixture.readAction("101");
        assertFalse(facts.hasEngineLegalKyloTarget());
        assertFalse(facts.hasKyloAtPlannedBattleTarget());
    }

    @Test
    public void destinationExposesAboardOffSiteAndArmedBoundaries() {
        Fixture aboard = fixture();
        PhysicalCard starship = card(
                301, PLAYER, Zone.AT_LOCATION,
                CardCategory.STARSHIP, CardSubtype.CAPITAL);
        when(aboard.kylo.getAttachedTo()).thenReturn(starship);
        aboard.allow(aboard.source, 1.0f);
        DeployWeaponPolicy.KyloBattleKitDestinationFacts aboardFacts =
                aboard.readDestination("101");
        assertTrue(aboardFacts.atSite());
        assertTrue(aboardFacts.aboardStarship());

        Fixture offSite = fixture();
        PhysicalCard system = card(
                102, null, Zone.LOCATIONS,
                CardCategory.LOCATION, CardSubtype.SYSTEM);
        when(offSite.modifiers.getLocationThatCardIsAt(
                offSite.gameState, offSite.kylo)).thenReturn(system);
        offSite.allow(offSite.source, 1.0f);
        assertFalse(offSite.readDestination("102").atSite());

        Fixture armed = fixture();
        when(armed.kylo.getBlueprint()
                .hasIcon(Icon.PERMANENT_WEAPON)).thenReturn(true);
        armed.allow(armed.source, 1.0f);
        assertFalse(armed.readDestination("101").unarmed());
    }

    @Test
    public void destinationKeepsUnrelatedGroundKyloNeutral() {
        Fixture fixture = fixture();
        fixture.allow(fixture.source, 1.0f);

        DeployWeaponPolicy.KyloBattleKitDestinationFacts facts =
                fixture.readDestination("999");

        assertTrue(facts.kyloTarget());
        assertTrue(facts.engineDeployLegal());
        assertTrue(facts.atSite());
        assertTrue(facts.unarmed());
        assertFalse(facts.atPlannedBattleTarget());
        assertTrue(DeployWeaponPolicy
                .evaluateKyloBattleKitDestination(facts)
                .operations().isEmpty());
    }

    @Test
    public void exactCombinedPaymentPreservesBattleForce() {
        Fixture fixture = fixture();
        fixture.allow(fixture.source, 1.1f);
        when(fixture.modifiers.getForceAvailableToUse(
                fixture.gameState, PLAYER)).thenReturn(2);

        DeployWeaponPolicy.KyloBattleKitDestinationFacts facts =
                fixture.readDestination("101");

        assertEquals(2, facts.exactDeployPayment());
        assertEquals(1, facts.exactBattlePayment());
        assertEquals(-9999.0f,
                DeployWeaponPolicy
                    .evaluateKyloBattleKitDestination(facts)
                    .operations().get(0).delta(),
                0.0f);
    }

    @Test
    public void reserveDeploySourceDrivesLegalityAndExactCost() {
        Fixture fixture = fixture();
        PhysicalCard reserveSource = card(
                401, PLAYER, Zone.AT_LOCATION,
                CardCategory.EFFECT, CardSubtype.NORMAL);
        when(fixture.saber.getZone()).thenReturn(Zone.RESERVE_DECK);
        fixture.allow(reserveSource, 1.0f);

        DeployWeaponPolicy.KyloBattleKitDestinationFacts facts =
                KyloBattleKitFactsReader.readDestination(
                        "a", fixture.game, PLAYER,
                        reserveSource, fixture.saber, fixture.kylo,
                        "101", false, 0.0f).orElseThrow();

        assertTrue(facts.engineDeployLegal());
        verify(fixture.modifiers).getDeployCost(
                fixture.gameState,
                reserveSource,
                fixture.saber,
                fixture.kylo,
                false,
                null,
                false,
                0.0f,
                null,
                true);
    }

    private static Fixture fixture() {
        SwccgGame game = mock(SwccgGame.class);
        GameState gameState = mock(GameState.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard saber = card(
                10, PLAYER, Zone.HAND,
                CardCategory.WEAPON, CardSubtype.CHARACTER);
        PhysicalCard site = card(
                101, null, Zone.LOCATIONS,
                CardCategory.LOCATION, CardSubtype.SITE);
        PhysicalCard kylo = card(
                20, PLAYER, Zone.AT_LOCATION,
                CardCategory.CHARACTER, CardSubtype.CHARACTER);
        Set<PhysicalCard> legalTargets = new HashSet<>();
        PhysicalCard[] legalSource = new PhysicalCard[1];

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getGame()).thenReturn(game);
        when(gameState.getCurrentPlayerId()).thenReturn(PLAYER);
        when(gameState.getOpponent(PLAYER)).thenReturn(OPPONENT);
        when(gameState.getAllPermanentCards()).thenReturn(
                List.of(saber, site, kylo));
        when(gameState.getAttachedCards(kylo)).thenReturn(List.of());
        when(saber.getBlueprint()
                .hasPersona(Persona.KYLOS_LIGHTSABER))
                .thenReturn(true);
        when(modifiers.hasPersona(
                gameState, kylo, Persona.KYLO)).thenReturn(true);
        when(modifiers.getLocationThatCardIsAt(gameState, kylo))
                .thenReturn(site);
        when(modifiers.getForceAvailableToUse(gameState, PLAYER))
                .thenReturn(2);
        when(modifiers.getInitiateBattleCost(
                gameState, site, PLAYER, true)).thenReturn(1.0f);
        when(modifiers.getInitiateBattleCost(
                gameState, site, PLAYER, false)).thenReturn(1.0f);
        when(modifiers.hasPresenceAt(
                same(gameState), eq(PLAYER), same(site),
                eq(true), eq(PLAYER), isNull())).thenReturn(true);
        when(modifiers.hasPresenceAt(
                same(gameState), eq(OPPONENT), same(site),
                eq(true), eq(PLAYER), isNull())).thenReturn(true);
        when(modifiers.isDeployableToTarget(
                same(gameState),
                any(PhysicalCard.class),
                same(saber),
                eq(false),
                any(Filter.class),
                eq(false),
                eq(0.0f),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(false),
                eq(0.0f)))
                .thenAnswer(invocation -> {
                    PhysicalCard source = invocation.getArgument(1);
                    Filter targetFilter = invocation.getArgument(4);
                    return source == legalSource[0]
                            && legalTargets.stream().anyMatch(
                                target -> targetFilter.accepts(
                                    gameState, modifiers, target));
                });

        return new Fixture(
                game, gameState, modifiers,
                saber, saber, kylo, site,
                legalTargets, legalSource);
    }

    private static PhysicalCard card(
            int id,
            String owner,
            Zone zone,
            CardCategory category,
            CardSubtype subtype) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getPermanentCardId()).thenReturn(id);
        when(card.getCardId()).thenReturn(id);
        when(card.getAdditionalCardIds()).thenReturn(List.of());
        when(card.getOwner()).thenReturn(owner);
        when(card.getZone()).thenReturn(zone);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getCardCategory()).thenReturn(category);
        when(blueprint.getCardSubtype()).thenReturn(subtype);
        return card;
    }

    private record Fixture(
            SwccgGame game,
            GameState gameState,
            ModifiersQuerying modifiers,
            PhysicalCard source,
            PhysicalCard saber,
            PhysicalCard kylo,
            PhysicalCard site,
            Set<PhysicalCard> legalTargets,
            PhysicalCard[] legalSource) {
        private void allow(
                PhysicalCard sourceCard,
                float deployCost) {
            legalSource[0] = sourceCard;
            legalTargets.add(kylo);
            when(modifiers.getDeployCost(
                    gameState,
                    sourceCard,
                    saber,
                    kylo,
                    false,
                    null,
                    false,
                    0.0f,
                    null,
                    true)).thenReturn(deployCost);
        }

        private DeployWeaponPolicy.KyloBattleKitActionFacts readAction(
                String plannedTargetId) {
            return KyloBattleKitFactsReader.readAction(
                    "a", game, PLAYER, source, saber,
                    plannedTargetId, false, 0.0f).orElseThrow();
        }

        private DeployWeaponPolicy.KyloBattleKitDestinationFacts
                readDestination(String plannedTargetId) {
            return KyloBattleKitFactsReader.readDestination(
                    "a", game, PLAYER, source, saber, kylo,
                    plannedTargetId, false, 0.0f).orElseThrow();
        }
    }
}
