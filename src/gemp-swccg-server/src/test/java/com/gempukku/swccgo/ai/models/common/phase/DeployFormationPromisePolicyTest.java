package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.Set;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeployFormationPromisePolicyTest {

    @Test
    public void exactAffordableBuddyPersistsUntilFormationIsSafe() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard anchor = character(1, Zone.AT_LOCATION);
        PhysicalCard companion = character(2, Zone.HAND);
        PhysicalCard site = mock(PhysicalCard.class);
        SwccgCardBlueprint siteBlueprint = mock(SwccgCardBlueprint.class);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.findCardByPermanentId(1)).thenReturn(anchor);
        when(gameState.findCardByPermanentId(2)).thenReturn(companion);
        when(gameState.findCardById(100)).thenReturn(site);
        when(site.getCardId()).thenReturn(100);
        when(site.getBlueprint()).thenReturn(siteBlueprint);
        when(siteBlueprint.getCardSubtype()).thenReturn(CardSubtype.SITE);
        when(modifiers.getLocationThatCardIsAt(gameState, anchor))
                .thenReturn(site);
        when(gameState.getCardsAtLocation(site)).thenReturn(List.of(anchor));
        when(modifiers.getTotalAbilityAtLocation(
                gameState, "rando", site)).thenReturn(1.0f);
        when(modifiers.isDeployableToTarget(
                any(), any(), any(), anyBoolean(), any(), anyBoolean(),
                anyFloat(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), anyFloat())).thenReturn(true);
        when(modifiers.getDeployCost(
                gameState, companion, companion, site,
                false, null, false, 0.0f, null, true))
                .thenReturn(5.0f);

        List<DeployFormationPromisePolicy.PlannedInstruction> plan = List.of(
                new DeployFormationPromisePolicy.PlannedInstruction(
                        1, 1, "100"),
                new DeployFormationPromisePolicy.PlannedInstruction(
                        2, 2, "100"));
        DeployFormationPromisePolicy.Promise promise =
                DeployFormationPromisePolicy.detect(
                        game, "rando", 6, 1, plan);
        assertNotNull(promise);
        assertTrue(DeployFormationPromisePolicy.shouldContinue(
                game, "rando", 6, 1, promise));
        assertFalse("The battle reserve cannot fund the companion",
                DeployFormationPromisePolicy.shouldContinue(
                    game, "rando", 5, 1, promise));

        when(modifiers.getTotalAbilityAtLocation(
                gameState, "rando", site)).thenReturn(7.0f);
        assertTrue("A high-ability first piece still has a promised buddy",
                DeployFormationPromisePolicy.shouldContinue(
                    game, "rando", 6, 1, promise));

        when(modifiers.getTotalAbilityAtLocation(
                gameState, "rando", site)).thenReturn(1.0f);
        when(anchor.getCardId()).thenReturn(9);
        assertNotNull("A normal hand-to-table current-id change must keep the same physical anchor",
                DeployFormationPromisePolicy.detect(
                    game, "rando", 6, 1, plan));

        when(companion.getCardId()).thenReturn(10);
        assertNull("A pending hand card must still match its exact current id",
                DeployFormationPromisePolicy.detect(
                    game, "rando", 6, 1, plan));
    }

    @Test
    public void fundedSameSiteGroupRemainsAnOrderedQueue() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard anchor = character(1, Zone.AT_LOCATION);
        PhysicalCard second = character(2, Zone.HAND);
        PhysicalCard third = character(3, Zone.HAND);
        PhysicalCard site = site(100, "Battleground");

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.findCardByPermanentId(1)).thenReturn(anchor);
        when(gameState.findCardByPermanentId(2)).thenReturn(second);
        when(gameState.findCardByPermanentId(3)).thenReturn(third);
        when(gameState.findCardById(100)).thenReturn(site);
        when(modifiers.getLocationThatCardIsAt(gameState, anchor))
                .thenReturn(site);
        when(gameState.getCardsAtLocation(site))
                .thenReturn(List.of(anchor));
        allowDeploys(modifiers);
        when(modifiers.getDeployCost(
                gameState, second, second, site,
                false, null, false, 0.0f, null, true))
                .thenReturn(2.0f);
        when(modifiers.getDeployCost(
                gameState, third, third, site,
                false, null, false, 0.0f, null, true))
                .thenReturn(3.0f);

        List<DeployFormationPromisePolicy.PlannedInstruction> plan = List.of(
                new DeployFormationPromisePolicy.PlannedInstruction(
                        1, 1, "100"),
                new DeployFormationPromisePolicy.PlannedInstruction(
                        2, 2, "100"),
                new DeployFormationPromisePolicy.PlannedInstruction(
                        3, 3, "100"));
        var promise = DeployFormationPromisePolicy.detect(
                game, "rando", 6, 1, plan);
        assertNotNull(promise);
        assertEquals(2, promise.committedCards().size());

        when(second.getZone()).thenReturn(Zone.AT_LOCATION);
        when(second.getCardId()).thenReturn(22);
        when(modifiers.getLocationThatCardIsAt(gameState, second))
                .thenReturn(site);
        when(gameState.getCardsAtLocation(site))
                .thenReturn(List.of(anchor, second));
        var remaining = DeployFormationPromisePolicy
                .remainingCommittedCards(
                    game, "rando", 4, 1, promise);
        assertNotNull(remaining);
        assertEquals(1, remaining.size());
        assertEquals(3, remaining.get(0).permanentCardId());
    }

    @Test
    public void differentPhysicalAnchorCannotInheritAFormationPromise() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard replacement = character(9, Zone.AT_LOCATION);
        PhysicalCard companion = character(2, Zone.HAND);
        PhysicalCard site = site(100, "Battleground");

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.findCardByPermanentId(1)).thenReturn(null);
        when(gameState.findCardByPermanentId(9)).thenReturn(replacement);
        when(gameState.findCardByPermanentId(2)).thenReturn(companion);
        when(gameState.findCardById(100)).thenReturn(site);
        when(modifiers.getLocationThatCardIsAt(gameState, replacement))
                .thenReturn(site);
        when(gameState.getCardsAtLocation(site))
                .thenReturn(List.of(replacement));
        allowDeploys(modifiers);
        when(modifiers.getDeployCost(
                gameState, companion, companion, site,
                false, null, false, 0.0f, null, true))
                .thenReturn(2.0f);

        List<DeployFormationPromisePolicy.PlannedInstruction> plan = List.of(
                new DeployFormationPromisePolicy.PlannedInstruction(
                        1, 1, "100"),
                new DeployFormationPromisePolicy.PlannedInstruction(
                        2, 2, "100"));

        assertNull(DeployFormationPromisePolicy.detect(
                game, "rando", 4, 1, plan));
    }

    @Test
    public void exactPairRequiresBothEngineCostsAfterBattleReserve() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard first = character(1, Zone.HAND);
        PhysicalCard companion = character(2, Zone.HAND);
        PhysicalCard site = site(100, "Battleground");

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.findCardByPermanentId(1)).thenReturn(first);
        when(gameState.findCardByPermanentId(2)).thenReturn(companion);
        when(gameState.findCardById(100)).thenReturn(site);
        allowDeploys(modifiers);
        when(modifiers.getDeployCost(
                gameState, first, first, site,
                false, null, false, 0.0f, null, true))
                .thenReturn(3.0f);
        when(modifiers.getDeployCost(
                gameState, companion, companion, site,
                false, null, false, 0.0f, null, true))
                .thenReturn(4.0f);

        List<DeployFormationPromisePolicy.PlannedInstruction> plan = List.of(
                new DeployFormationPromisePolicy.PlannedInstruction(
                        1, 1, "100"),
                new DeployFormationPromisePolicy.PlannedInstruction(
                        2, 2, "100"));

        DeployFormationPromisePolicy.FundedPair funded =
                DeployFormationPromisePolicy.detectFundedPair(
                        game, "rando", 8, 1,
                        1, 1, "100", plan);
        assertNotNull(funded);
        assertEquals(3, funded.firstCost());
        assertEquals(4, funded.companionCost());
        assertNull("Seven total Force leaves only six after reserve",
                DeployFormationPromisePolicy.detectFundedPair(
                        game, "rando", 7, 1,
                        1, 1, "100", plan));
    }

    @Test
    public void newHighAbilityContestedAnchorTakesStrongUnpairedCompanion() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard dooku = character(
                10, Zone.AT_LOCATION, "Count Dooku", "rando", 4, 7);
        PhysicalCard obi = character(
                11, Zone.AT_LOCATION, "Obi-Wan", "opponent", 5, 6);
        PhysicalCard asajj = character(
                20, Zone.HAND, "Asajj Ventress", "rando", 5, 5);
        PhysicalCard nute = character(
                21, Zone.HAND, "Nute Gunray", "rando", 2, 3);
        PhysicalCard evazan = character(
                22, Zone.HAND, "Dr. Evazan", "rando", 2, 2);
        PhysicalCard jabiim = site(100, "Jabiim: Hangar");
        PhysicalCard works = site(200, "Coruscant: Works");
        PhysicalCard mining = site(300, "Mapuzo: Mining Village");

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getOpponent("rando")).thenReturn("opponent");
        when(gameState.getLocationsInOrder())
                .thenReturn(List.of(jabiim, works, mining));
        when(gameState.getCardsAtLocation(jabiim))
                .thenReturn(List.of(dooku, obi));
        when(gameState.getCardsAtLocation(works)).thenReturn(List.of());
        when(gameState.getCardsAtLocation(mining)).thenReturn(List.of());
        when(modifiers.isBattleground(gameState, jabiim, null))
                .thenReturn(true);
        when(modifiers.occupiesLocation(
                gameState, jabiim, "opponent")).thenReturn(true);
        when(gameState.findCardByPermanentId(10)).thenReturn(dooku);
        when(gameState.findCardByPermanentId(20)).thenReturn(asajj);
        when(gameState.findCardByPermanentId(21)).thenReturn(nute);
        when(gameState.findCardByPermanentId(22)).thenReturn(evazan);
        when(gameState.findCardById(100)).thenReturn(jabiim);
        when(gameState.findCardById(200)).thenReturn(works);
        when(gameState.findCardById(300)).thenReturn(mining);
        allowDeploys(modifiers);
        when(modifiers.getDeployCost(
                gameState, asajj, asajj, jabiim,
                false, null, false, 0.0f, null, true))
                .thenReturn(4.0f);
        when(modifiers.getDeployCost(
                gameState, nute, nute, mining,
                false, null, false, 0.0f, null, true))
                .thenReturn(3.0f);
        when(modifiers.getDeployCost(
                gameState, evazan, evazan, mining,
                false, null, false, 0.0f, null, true))
                .thenReturn(3.0f);

        List<DeployFormationPromisePolicy.PlannedInstruction> plan = List.of(
                new DeployFormationPromisePolicy.PlannedInstruction(
                        20, 20, "200"),
                new DeployFormationPromisePolicy.PlannedInstruction(
                        21, 21, "300"),
                new DeployFormationPromisePolicy.PlannedInstruction(
                        22, 22, "300"));
        DeployFormationPromisePolicy.Promise repair =
                DeployFormationPromisePolicy.detectLiveContestedAnchor(
                        game, "rando", 12, 1, plan, Set.of());

        assertNotNull(repair);
        assertEquals(10, repair.anchorPermanentCardId());
        assertEquals(20, repair.companionPermanentCardId());
        assertEquals("100", repair.targetLocationId());
        assertEquals(4, repair.companionCost());
        assertNull("An anchor present when the plan was made is not new",
                DeployFormationPromisePolicy.detectLiveContestedAnchor(
                        game, "rando", 12, 1, plan, Set.of(10)));
    }

    @Test
    public void emptyMapuzoTrapPredicateKeepsJediOnlyBoundary() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard mapuzo = site(100, "Mapuzo: Mining Village");
        SwccgCardBlueprint nonJedi = mock(SwccgCardBlueprint.class);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getOpponent("rando")).thenReturn("opponent");
        when(nonJedi.getCardCategory()).thenReturn(CardCategory.CHARACTER);
        when(nonJedi.hasKeyword(Keyword.JEDI_SURVIVOR)).thenReturn(false);
        when(modifiers.getTotalPowerAtLocation(
                gameState, mapuzo, "opponent", false, false))
                .thenReturn(0.0f);

        assertTrue(DeployFormationPromisePolicy
                .isEmptyMapuzoNonJediTrap(
                        game, "rando", nonJedi, mapuzo));
        when(nonJedi.hasKeyword(Keyword.JEDI_SURVIVOR)).thenReturn(true);
        assertFalse(DeployFormationPromisePolicy
                .isEmptyMapuzoNonJediTrap(
                        game, "rando", nonJedi, mapuzo));
    }

    private static PhysicalCard character(int id, Zone zone) {
        return character(id, zone, "Character " + id,
                "rando", 1, 1);
    }

    private static PhysicalCard character(
            int id, Zone zone, String title, String owner,
            float power, float ability) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getPermanentCardId()).thenReturn(id);
        when(card.getCardId()).thenReturn(id);
        when(card.getOwner()).thenReturn(owner);
        when(card.getZone()).thenReturn(zone);
        when(card.isUndercover()).thenReturn(false);
        when(card.getTitle()).thenReturn(title);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getCardCategory())
                .thenReturn(CardCategory.CHARACTER);
        when(blueprint.hasPowerAttribute()).thenReturn(true);
        when(blueprint.getPower()).thenReturn(power);
        when(blueprint.hasAbilityAttribute()).thenReturn(true);
        when(blueprint.getAbility()).thenReturn(ability);
        return card;
    }

    private static PhysicalCard site(int id, String title) {
        PhysicalCard site = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(site.getCardId()).thenReturn(id);
        when(site.getTitle()).thenReturn(title);
        when(site.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getCardSubtype()).thenReturn(CardSubtype.SITE);
        return site;
    }

    private static void allowDeploys(ModifiersQuerying modifiers) {
        when(modifiers.isDeployableToTarget(
                any(), any(), any(), anyBoolean(), any(), anyBoolean(),
                anyFloat(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), anyFloat())).thenReturn(true);
    }
}
