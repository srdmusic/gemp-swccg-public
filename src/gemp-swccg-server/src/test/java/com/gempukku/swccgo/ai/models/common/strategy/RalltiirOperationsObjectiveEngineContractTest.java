package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.AiActionSourceProvenance;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.PhysicalCardImpl;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Public-bot behavior proof for Ralltiir Operations / In The Hands Of The Empire. */
public class RalltiirOperationsObjectiveEngineContractTest {

    private static StartingSetup ralltiirOperations(
            String systemBlueprintId) {
        return new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "7_300");
                put("system", systemBlueprintId);
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            // One matching system lets the objective setup auto-resolve.
        }
        };
    }

    private VirtualTableScenario scenario() {
        return scenario("2_148");
    }

    private VirtualTableScenario scenario(String systemBlueprintId) {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("xwing", "1_146");
                    put("xwingBattle", "1_146");
                }},
                new HashMap<>() {{
                    put("forest", "7_284");
                    put("jungle", "7_285");
                    put("desert", "7_281");
                    put("swamp", "7_292");
                    put("jawa", "1_182");
                    put("jawaTwo", "1_182");
                    put("jawaThree", "1_182");
                    put("jawaFour", "1_182");
                    put("labria", "1_184");
                    put("uniqueImperial", "2_106");
                    put("tie", "1_304");
                    put("pilot", "1_180");
                    put("emptyShip", "1_300");
                    put("emptyShipDecoy", "1_299");
                    put("dantooine", "1_282");
                    put("alderaan", "1_281");
                    put("victory", "2_155");
                    put("victoryBattle", "2_155");
                    put("vader", "1_168");
                    put("deathStar", "2_143");
                    put("superlaser", "2_161");
                    put("cpi", "2_130");
                    put("deathStarSiteOne", "1_283");
                    put("deathStarSiteTwo", "1_284");
                    put("deathStarSiteThree", "1_285");
                }},
                24,
                24,
                StartingSetup.DefaultLSGroundLocation,
                ralltiirOperations(systemBlueprintId),
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open);
    }

    private void moveSiteToRalltiir(
            VirtualTableScenario scn, PhysicalCardImpl site) {
        scn.RemoveCardZone(site);
        var placements = scn.gameState().getLocationPlacement(
                scn.game(), site, Title.Ralltiir, null);
        assertFalse("Expected a legal Ralltiir placement",
                placements.isEmpty());
        scn.gameState().addLocationToTable(
                scn.game(), site, placements.getFirst());
    }

    private void addAllSites(VirtualTableScenario scn) {
        moveSiteToRalltiir(scn, scn.GetDSCard("forest"));
        moveSiteToRalltiir(scn, scn.GetDSCard("jungle"));
        moveSiteToRalltiir(scn, scn.GetDSCard("desert"));
    }

    private void startFlipped(VirtualTableScenario scn) {
        var objective = scn.GetDSCard("objective");
        var forest = scn.GetDSCard("forest");
        var jungle = scn.GetDSCard("jungle");
        var desert = scn.GetDSCard("desert");
        var pulse = scn.GetDSCard("labria");

        scn.MoveCardsToDSHand(pulse);
        scn.StartGame();
        addAllSites(scn);
        scn.MoveCardsToLocation(forest, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(jungle, scn.GetDSFiller(2));
        scn.MoveCardsToLocation(desert, scn.GetDSFiller(3));
        scn.SkipToDSTurn(Phase.DEPLOY);
        scn.DSActivateForceCheat(10);
        scn.DSDeployCardAndPassResponses(pulse, forest);

        assertTrue("A real deployment must pulse the native front-side flip",
                objective.isFlipped());
    }

    private void finishBlowAwayResolution(VirtualTableScenario scn) {
        for (int attempt = 0; attempt < 30; attempt++) {
            if (scn.AwaitingDSForceLossPayment()) {
                scn.DSPayRemainingForceLossFromReserveDeck();
            } else if (scn.AwaitingLSForceLossPayment()) {
                scn.LSPayRemainingForceLossFromReserveDeck();
            } else if (scn.DSDecisionAvailable(
                    "Choose card to put on Lost Pile")) {
                scn.DSDecided(scn.DSGetCardChoices().getFirst());
            } else if (scn.LSDecisionAvailable(
                    "Choose card to put on Lost Pile")) {
                scn.LSDecided(scn.LSGetCardChoices().getFirst());
            } else if (scn.GetCurrentDecision().getText()
                    .toLowerCase().contains("optional response")) {
                scn.PassAllResponses();
            } else {
                return;
            }
        }
        throw new AssertionError(
                "Ralltiir blow-away resolution did not finish");
    }

    private void keepOnlyDarkHandCards(
            VirtualTableScenario scn, PhysicalCard... keep) {
        Set<PhysicalCard> protectedCards = Set.of(keep);
        var remove = new ArrayList<PhysicalCardImpl>();
        for (PhysicalCard card : scn.gameState().getHand(
                VirtualTableScenario.DS)) {
            if (card instanceof PhysicalCardImpl physical
                    && !protectedCards.contains(card)) {
                remove.add(physical);
            }
        }
        for (PhysicalCardImpl card : remove) {
            scn.MoveOutOfPlay(card);
        }
    }

    private void keepExactlyDarkForce(
            VirtualTableScenario scn, int amount) {
        while (scn.GetDSForcePileCount() > amount) {
            scn.MoveCardsToTopOfDSUsedPile(
                    scn.GetTopOfDSForcePile());
        }
        while (scn.GetDSForcePileCount() < amount) {
            scn.MoveCardsToTopOfDSForcePile(
                    scn.GetTopOfDSReserveDeck());
        }
        assertEquals(amount, scn.GetDSForcePileCount());
    }

    private void keepOnlyDarkLifeForce(
            VirtualTableScenario scn, PhysicalCard... keep) {
        Set<PhysicalCard> protectedCards = Set.of(keep);
        var lifeForce = new ArrayList<PhysicalCard>();
        lifeForce.addAll(scn.gameState().getHand(
                VirtualTableScenario.DS));
        lifeForce.addAll(scn.gameState().getReserveDeck(
                VirtualTableScenario.DS));
        lifeForce.addAll(scn.gameState().getForcePile(
                VirtualTableScenario.DS));
        lifeForce.addAll(scn.gameState().getUsedPile(
                VirtualTableScenario.DS));
        for (PhysicalCard card : lifeForce) {
            if (card instanceof PhysicalCardImpl physical
                    && !protectedCards.contains(card)) {
                scn.MoveOutOfPlay(physical);
            }
        }
    }

    private void keepOnlyDarkReserveRouteCandidates(
            VirtualTableScenario scn, PhysicalCard... keep) {
        Set<PhysicalCard> protectedCards = Set.of(keep);
        var remove = new ArrayList<PhysicalCardImpl>();
        for (PhysicalCard card : scn.gameState().getReserveDeck(
                VirtualTableScenario.DS)) {
            if (card instanceof PhysicalCardImpl physical
                    && !protectedCards.contains(card)
                    && Filters.or(
                        Filters.site,
                        Filters.and(
                            Filters.non_unique,
                            Filters.Imperial)).accepts(
                                scn.gameState(),
                                scn.game().getModifiersQuerying(), card)) {
                remove.add(physical);
            }
        }
        for (PhysicalCardImpl card : remove) {
            scn.MoveOutOfPlay(card);
        }
    }

    private record PublicBots(
            com.gempukku.swccgo.ai.models.rando.RandoCalAi rando,
            com.gempukku.swccgo.ai.models.chosenone.TheChosenOneAi chosen) {
        private static PublicBots forGame(VirtualTableScenario scn) {
            var rando = new com.gempukku.swccgo.ai.models.rando
                    .RandoCalAi();
            var chosen = new com.gempukku.swccgo.ai.models.chosenone
                    .TheChosenOneAi();
            rando.setGame(scn.game());
            chosen.setGame(scn.game());
            return new PublicBots(rando, chosen);
        }

        private String decideBoth(VirtualTableScenario scn) {
            AwaitingDecision decision = scn.GetAwaitingDecision(
                    VirtualTableScenario.DS);
            assertNotNull("Dark Side must own the bot decision", decision);
            String randoResponse = rando.decide(
                    VirtualTableScenario.DS,
                    decision, scn.gameState());
            String chosenResponse = chosen.decide(
                    VirtualTableScenario.DS,
                    decision, scn.gameState());
            assertEquals("Rando and Chosen One must match",
                    randoResponse, chosenResponse);
            return randoResponse;
        }
    }

    private PhysicalCard selectedPhysicalCard(
            VirtualTableScenario scn,
            AwaitingDecision decision, String response) {
        PhysicalCard selected = AiActionSourceProvenance
                .selectedActionSource(decision, response);
        if (selected != null) return selected;
        try {
            return scn.gameState().findCardById(
                    Integer.parseInt(response));
        } catch (NumberFormatException ignored) {
            if (response != null && response.startsWith("temp")) {
                try {
                    int index = Integer.parseInt(response.substring(4));
                    var reserve = scn.gameState().getReserveDeck(
                            VirtualTableScenario.DS);
                    String[] blueprints = decision.getDecisionParameters()
                            .get("blueprintId");
                    if (index >= 0 && index < reserve.size()
                            && blueprints != null
                            && index < blueprints.length) {
                        PhysicalCard candidate = reserve.get(index);
                        if (candidate != null
                                && blueprints[index].equals(
                                    candidate.getBlueprintId(true))) {
                            return candidate;
                        }
                    }
                } catch (NumberFormatException ignoredTempId) {
                    return null;
                }
            }
            return null;
        }
    }

    private void resolveDarkBotChoicesUntil(
            VirtualTableScenario scn, PublicBots bots,
            PhysicalCard card, Zone expectedZone) {
        for (int i = 0; i < 8 && card.getZone() != expectedZone; i++) {
            if (scn.GetAwaitingDecision(VirtualTableScenario.DS) != null) {
                scn.DSDecided(bots.decideBoth(scn));
                scn.PassAllResponses();
            } else if (scn.GetAwaitingDecision(
                    VirtualTableScenario.LS) != null) {
                scn.LSPass();
            } else {
                break;
            }
        }
    }

    @Test
    public void dedicatedMixedRouteStaysOutOfLocationOnlyApis() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var forest = scn.GetDSCard("forest");
        var jungle = scn.GetDSCard("jungle");
        var swamp = scn.GetDSCard("swamp");
        var imperial = scn.GetDSFiller(3);

        scn.StartGame();
        addAllSites(scn);
        scn.MoveCardsToLocation(forest, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(jungle, scn.GetDSFiller(2));
        scn.SkipToDSTurn(Phase.DEPLOY);
        scn.MoveCardsToBottomOfDSReserveDeck(swamp, imperial);
        keepOnlyDarkReserveRouteCandidates(scn, swamp, imperial);
        keepExactlyDarkForce(scn, 3);

        var rando =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        var chosen =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        rando.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);

        assertFalse(rando.usesObjectiveLocationPullSequence());
        assertFalse(chosen.usesObjectiveLocationPullSequence());
        assertFalse(rando.hasObjectiveLocationRouteCandidateInReserve(
                scn.game(), VirtualTableScenario.DS, objective));
        assertFalse(rando.hasMissingPreFlipRequiredLocationInReserve(
                scn.game(), VirtualTableScenario.DS));
        assertFalse(rando.isNativeObjectiveLocationRouteCandidate(
                scn.game(), VirtualTableScenario.DS,
                objective, swamp));
        assertFalse(rando.isNativeObjectiveLocationRouteCandidate(
                scn.game(), VirtualTableScenario.DS,
                objective, imperial));
        assertTrue(rando.hasRalltiirFrontRouteCandidateInReserve(
                scn.game(), VirtualTableScenario.DS, objective));
        assertEquals(0, rando.getRalltiirFrontPullCandidatePriority(
                scn.game(), VirtualTableScenario.DS,
                objective, swamp));
        assertEquals(2, rando.getRalltiirFrontPullCandidatePriority(
                scn.game(), VirtualTableScenario.DS,
                objective, imperial));
        assertEquals(
                rando.getRalltiirFrontPullCandidatePriority(
                    scn.game(), VirtualTableScenario.DS,
                    objective, imperial),
                chosen.getRalltiirFrontPullCandidatePriority(
                    scn.game(), VirtualTableScenario.DS,
                    objective, imperial));
    }

    @Test
    public void publicBotsPullMissingSiteBeforeAvailableImperial() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var forest = scn.GetDSCard("forest");
        var jungle = scn.GetDSCard("jungle");
        var desert = scn.GetDSCard("desert");
        var tie = scn.GetDSCard("tie");
        var availableImperial = scn.GetDSFiller(3);

        scn.MoveCardsToDSHand(tie);
        scn.StartGame();
        moveSiteToRalltiir(scn, forest);
        moveSiteToRalltiir(scn, jungle);
        scn.MoveCardsToLocation(forest, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(jungle, scn.GetDSFiller(2));
        scn.SkipToDSTurn(Phase.DEPLOY);
        scn.MoveCardsToBottomOfDSReserveDeck(
                desert, availableImperial);
        keepOnlyDarkReserveRouteCandidates(
                scn, desert, availableImperial);
        keepOnlyDarkHandCards(scn, tie);
        keepExactlyDarkForce(scn, 3);

        String route = scn.GetCardActionId(
                VirtualTableScenario.DS, objective,
                "Deploy card from Reserve Deck");
        assertNotNull(route);
        assertNotNull("The unrelated TIE deploy must be available",
                scn.GetCardActionId(
                    VirtualTableScenario.DS, tie, "Deploy"));

        var bots = PublicBots.forGame(scn);
        assertEquals("The exact objective route must beat the distractor",
                route, bots.decideBoth(scn));
        scn.DSDecided(route);
        scn.PassAllResponses();

        AwaitingDecision child = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(child);
        String childResponse = bots.decideBoth(scn);
        assertSame("The mixed route must build geography first",
                desert,
                selectedPhysicalCard(scn, child, childResponse));
        scn.DSDecided(childResponse);
        scn.PassAllResponses();
        resolveDarkBotChoicesUntil(
                scn, bots, desert, Zone.LOCATIONS);

        assertSame(Zone.LOCATIONS, desert.getZone());
        assertTrue("The unchosen Imperial must remain in Reserve Deck",
                scn.gameState().getReserveDeck(VirtualTableScenario.DS)
                    .contains(availableImperial));
        assertFalse("Geography alone must not flip the objective",
                objective.isFlipped());
    }

    @Test
    public void publicBotsPullFinalImperialForExactCostAndFlipNatively() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var forest = scn.GetDSCard("forest");
        var jungle = scn.GetDSCard("jungle");
        var desert = scn.GetDSCard("desert");
        var swamp = scn.GetDSCard("swamp");
        var tie = scn.GetDSCard("tie");
        var finalImperial = scn.GetDSFiller(3);

        scn.MoveCardsToDSHand(tie);
        scn.StartGame();
        addAllSites(scn);
        scn.MoveCardsToLocation(forest, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(jungle, scn.GetDSFiller(2));
        scn.SkipToDSTurn(Phase.DEPLOY);
        scn.MoveCardsToBottomOfDSReserveDeck(swamp, finalImperial);
        keepOnlyDarkReserveRouteCandidates(scn, swamp, finalImperial);
        keepOnlyDarkHandCards(scn, tie);
        keepExactlyDarkForce(scn, 3);

        var rando =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        var chosen =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertFalse(GameConditions.occupies(
                scn.game(), VirtualTableScenario.DS, desert));
        assertTrue(rando.advancesPreFlipRequirementAt(
                scn.game(), VirtualTableScenario.DS,
                finalImperial, desert));
        assertEquals(0, rando.getRalltiirFrontPullCandidatePriority(
                scn.game(), VirtualTableScenario.DS, objective, swamp));
        assertEquals(2, rando.getRalltiirFrontPullCandidatePriority(
                scn.game(), VirtualTableScenario.DS,
                objective, finalImperial));
        assertFalse(rando.hasRalltiirFrontSiteRouteCandidateInReserve(
                scn.game(), VirtualTableScenario.DS, objective));
        assertEquals(
                rando.getRalltiirFrontPullCandidatePriority(
                    scn.game(), VirtualTableScenario.DS,
                    objective, finalImperial),
                chosen.getRalltiirFrontPullCandidatePriority(
                    scn.game(), VirtualTableScenario.DS,
                    objective, finalImperial));

        int exactCost = (int) Math.ceil(
                scn.game().getModifiersQuerying().getDeployCost(
                    scn.gameState(), objective, finalImperial,
                    desert, false, null, false,
                    0.0f, null, true));
        int before = scn.GetDSForcePileCount();
        String route = scn.GetCardActionId(
                VirtualTableScenario.DS, objective,
                "Deploy card from Reserve Deck");
        assertNotNull(route);

        var bots = PublicBots.forGame(scn);
        assertEquals(route, bots.decideBoth(scn));
        scn.DSDecided(route);
        scn.PassAllResponses();

        AwaitingDecision child = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(child);
        String childResponse = bots.decideBoth(scn);
        assertSame("With three sites down, the mixed route must pull the Imperial",
                finalImperial,
                selectedPhysicalCard(scn, child, childResponse));
        scn.DSDecided(childResponse);
        scn.PassAllResponses();

        AwaitingDecision destination = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(destination);
        String destinationResponse = bots.decideBoth(scn);
        assertSame("The final Imperial must qualify the empty third site",
                desert,
                selectedPhysicalCard(
                    scn, destination, destinationResponse));
        scn.DSDecided(destinationResponse);
        scn.PassAllResponses();

        assertEquals("The native route must pay the exact deploy cost",
                before - exactCost, scn.GetDSForcePileCount());
        assertSame(Zone.AT_LOCATION, finalImperial.getZone());
        assertTrue(scn.gameState().getReserveDeck(
                VirtualTableScenario.DS).contains(swamp));
        assertTrue("The real table-change trigger must flip Ralltiir Operations",
                objective.isFlipped());
    }

    @Test
    public void publicBotsRejectFourthSiteAndDeployHandImperialToOpenThird() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var forest = scn.GetDSCard("forest");
        var jungle = scn.GetDSCard("jungle");
        var desert = scn.GetDSCard("desert");
        var swamp = scn.GetDSCard("swamp");
        var handImperial = scn.GetDSFiller(3);

        scn.MoveCardsToDSHand(handImperial);
        scn.StartGame();
        addAllSites(scn);
        scn.MoveCardsToLocation(forest, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(jungle, scn.GetDSFiller(2));
        scn.SkipToDSTurn(Phase.DEPLOY);
        scn.MoveCardsToBottomOfDSReserveDeck(swamp);
        keepOnlyDarkReserveRouteCandidates(scn, swamp);
        keepOnlyDarkHandCards(scn, handImperial);
        keepExactlyDarkForce(scn, 1);

        var rando =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        var chosen =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        rando.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertEquals(0, rando.getRalltiirFrontPullCandidatePriority(
                scn.game(), VirtualTableScenario.DS,
                objective, swamp));
        assertFalse(rando.hasRalltiirFrontRouteCandidateInReserve(
                scn.game(), VirtualTableScenario.DS, objective));
        assertTrue(rando.isExhaustedRalltiirFrontRouteAction(
                scn.game(), VirtualTableScenario.DS,
                objective, "Deploy card from Reserve Deck"));
        assertEquals(
                rando.isExhaustedRalltiirFrontRouteAction(
                    scn.game(), VirtualTableScenario.DS,
                    objective, "Deploy card from Reserve Deck"),
                chosen.isExhaustedRalltiirFrontRouteAction(
                    scn.game(), VirtualTableScenario.DS,
                    objective, "Deploy card from Reserve Deck"));

        String decorativeRoute = scn.GetCardActionId(
                VirtualTableScenario.DS, objective,
                "Deploy card from Reserve Deck");
        String deploy = scn.GetCardActionId(
                VirtualTableScenario.DS, handImperial, "Deploy");
        assertNotNull("The engine still offers the decorative fourth-site action",
                decorativeRoute);
        assertNotNull(deploy);
        var bots = PublicBots.forGame(scn);
        assertEquals("Both bots must use the hand Imperial instead of wasting Force on site four",
                deploy, bots.decideBoth(scn));
        scn.DSDecided(deploy);

        AwaitingDecision destination = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(destination);
        String destinationResponse = bots.decideBoth(scn);
        assertSame(desert,
                selectedPhysicalCard(
                    scn, destination, destinationResponse));
        scn.DSDecided(destinationResponse);
        scn.PassAllResponses();

        assertSame(Zone.AT_LOCATION, handImperial.getZone());
        assertTrue("The ordinary hand deploy must trigger the real flip",
                objective.isFlipped());
        assertTrue("Decorative site four must remain in Reserve Deck",
                scn.gameState().getReserveDeck(
                    VirtualTableScenario.DS).contains(swamp));
    }

    @Test
    public void publicBotsPullFourthSiteWhenThirdExistingSiteIsContested() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var forest = scn.GetDSCard("forest");
        var jungle = scn.GetDSCard("jungle");
        var desert = scn.GetDSCard("desert");
        var swamp = scn.GetDSCard("swamp");
        var reserveImperial = scn.GetDSFiller(4);

        scn.StartGame();
        addAllSites(scn);
        scn.MoveCardsToLocation(forest, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(jungle, scn.GetDSFiller(2));
        scn.MoveCardsToLocation(
                desert, scn.GetDSFiller(3), scn.GetLSFiller(1));
        scn.SkipToDSTurn(Phase.DEPLOY);
        scn.MoveCardsToBottomOfDSReserveDeck(swamp, reserveImperial);
        keepOnlyDarkReserveRouteCandidates(
                scn, swamp, reserveImperial);
        keepOnlyDarkHandCards(scn);
        keepExactlyDarkForce(scn, 2);

        assertTrue(GameConditions.occupies(
                scn.game(), VirtualTableScenario.DS, desert));
        assertTrue(GameConditions.occupies(
                scn.game(), VirtualTableScenario.LS, desert));
        assertFalse(GameConditions.controls(
                scn.game(), VirtualTableScenario.DS, desert));
        assertFalse(GameConditions.controls(
                scn.game(), VirtualTableScenario.LS, desert));
        var rando =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        var chosen =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertFalse(rando.advancesPreFlipRequirementAt(
                scn.game(), VirtualTableScenario.DS,
                reserveImperial, desert));
        assertEquals(3, rando.getRalltiirFrontPullCandidatePriority(
                scn.game(), VirtualTableScenario.DS, objective, swamp));
        assertEquals(0, rando.getRalltiirFrontPullCandidatePriority(
                scn.game(), VirtualTableScenario.DS,
                objective, reserveImperial));
        assertTrue(rando.hasRalltiirFrontSiteRouteCandidateInReserve(
                scn.game(), VirtualTableScenario.DS, objective));
        assertEquals(
                rando.getRalltiirFrontPullCandidatePriority(
                    scn.game(), VirtualTableScenario.DS,
                    objective, swamp),
                chosen.getRalltiirFrontPullCandidatePriority(
                    scn.game(), VirtualTableScenario.DS,
                    objective, swamp));

        String route = scn.GetCardActionId(
                VirtualTableScenario.DS, objective,
                "Deploy card from Reserve Deck");
        assertNotNull(route);
        var bots = PublicBots.forGame(scn);
        assertEquals(route, bots.decideBoth(scn));
        scn.DSDecided(route);
        scn.PassAllResponses();
        AwaitingDecision child = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        String childResponse = bots.decideBoth(scn);
        assertSame(swamp,
                selectedPhysicalCard(scn, child, childResponse));
        scn.DSDecided(childResponse);
        scn.PassAllResponses();
        resolveDarkBotChoicesUntil(
                scn, bots, swamp, Zone.LOCATIONS);

        assertTrue(Filters.Ralltiir_site.accepts(
                scn.gameState(), scn.game().getModifiersQuerying(), swamp));
        assertTrue(scn.gameState().getReserveDeck(
                VirtualTableScenario.DS).contains(reserveImperial));
        assertFalse(objective.isFlipped());
    }

    @Test
    public void nativePullBudgetEndsAfterUseWhileMovementBudgetRemains() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var forest = scn.GetDSCard("forest");
        var firstCandidate = scn.GetDSFiller(2);
        var secondCandidate = scn.GetDSFiller(3);

        scn.StartGame();
        addAllSites(scn);
        scn.MoveCardsToLocation(forest, scn.GetDSFiller(1));
        scn.SkipToDSTurn(Phase.DEPLOY);
        scn.MoveCardsToBottomOfDSReserveDeck(
                firstCandidate, secondCandidate);
        keepOnlyDarkReserveRouteCandidates(
                scn, firstCandidate, secondCandidate);
        keepOnlyDarkHandCards(scn);
        keepExactlyDarkForce(scn, 2);

        var rando =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        var chosen =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertEquals(2, rando.getRalltiirFrontPullCandidatePriority(
                scn.game(), VirtualTableScenario.DS,
                objective, firstCandidate));
        assertEquals(2, rando.getRalltiirFrontPullCandidatePriority(
                scn.game(), VirtualTableScenario.DS,
                objective, secondCandidate));
        assertEquals(1, rando.getRalltiirCurrentRouteForceReserve(
                scn.game(), VirtualTableScenario.DS));
        assertEquals(
                rando.getRalltiirCurrentRouteForceReserve(
                    scn.game(), VirtualTableScenario.DS),
                chosen.getRalltiirCurrentRouteForceReserve(
                    scn.game(), VirtualTableScenario.DS));

        String route = scn.GetCardActionId(
                VirtualTableScenario.DS, objective,
                "Deploy card from Reserve Deck");
        assertNotNull(route);
        var bots = PublicBots.forGame(scn);
        assertEquals(route, bots.decideBoth(scn));
        scn.DSDecided(route);
        scn.PassAllResponses();

        AwaitingDecision child = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        String childResponse = bots.decideBoth(scn);
        PhysicalCard pulled = selectedPhysicalCard(
                scn, child, childResponse);
        assertTrue(pulled == firstCandidate || pulled == secondCandidate);
        scn.DSDecided(childResponse);
        scn.PassAllResponses();
        scn.DSDecided(bots.decideBoth(scn));
        scn.PassAllResponses();
        resolveDarkBotChoicesUntil(
                scn, bots, pulled, Zone.AT_LOCATION);

        assertFalse(objective.isFlipped());
        assertEquals(1, scn.GetDSForcePileCount());
        PhysicalCard remaining = pulled == firstCandidate
                ? secondCandidate : firstCandidate;
        assertTrue(scn.gameState().getReserveDeck(
                VirtualTableScenario.DS).contains(remaining));
        assertEquals(2, rando.getRalltiirFrontPullCandidatePriority(
                scn.game(), VirtualTableScenario.DS,
                objective, remaining));
        assertEquals(1, scn.game().getModifiersQuerying()
                .getUntilEndOfPhaseLimitCounter(
                    objective, VirtualTableScenario.DS,
                    objective.getCardId(),
                    GameTextActionId
                        .RALLTIIR_OPERATIONS__DOWNLOAD_SITE_OR_NONUNIQUE_IMPERIAL_TO_RALLTIIR,
                    Phase.DEPLOY)
                .getUsedLimit());
        assertEquals("A distinct site move remains a real later route",
                1, rando.getRalltiirCurrentMoveForceReserve(
                scn.game(), VirtualTableScenario.DS));
        assertEquals(0, rando.getRalltiirCurrentBattleForceReserve(
                scn.game(), VirtualTableScenario.DS));
        assertEquals("The consumed native pull must not be added again",
                rando.getRalltiirCurrentMoveForceReserve(
                    scn.game(), VirtualTableScenario.DS),
                rando.getRalltiirCurrentRouteForceReserve(
                    scn.game(), VirtualTableScenario.DS));
        assertEquals(
                rando.getRalltiirCurrentRouteForceReserve(
                    scn.game(), VirtualTableScenario.DS),
                chosen.getRalltiirCurrentRouteForceReserve(
                    scn.game(), VirtualTableScenario.DS));
    }

    @Test
    public void publicBotsBankMoveForceMoveSurplusImperialAndFlip() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var forest = scn.GetDSCard("forest");
        var jungle = scn.GetDSCard("jungle");
        var desert = scn.GetDSCard("desert");
        var labria = scn.GetDSCard("labria");
        var forestImperial = scn.GetDSFiller(1);
        var jungleImperial = scn.GetDSFiller(2);
        var mover = scn.GetDSFiller(3);

        scn.MoveCardsToDSHand(labria);
        scn.StartGame();
        addAllSites(scn);
        scn.MoveCardsToLocation(forest, forestImperial);
        scn.MoveCardsToLocation(
                jungle, jungleImperial, mover);
        scn.SkipToDSTurn(Phase.DEPLOY);
        keepOnlyDarkReserveRouteCandidates(scn);
        keepOnlyDarkHandCards(scn, labria);
        keepExactlyDarkForce(scn, 1);

        var randoAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        var chosenAnalyzer =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        randoAnalyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosenAnalyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertEquals("One Force must be reserved for the safe landspeed route",
                1, randoAnalyzer.getRalltiirCurrentMoveForceReserve(
                    scn.game(), VirtualTableScenario.DS));
        assertEquals(
                randoAnalyzer.getRalltiirCurrentMoveForceReserve(
                    scn.game(), VirtualTableScenario.DS),
                chosenAnalyzer.getRalltiirCurrentMoveForceReserve(
                    scn.game(), VirtualTableScenario.DS));

        String distractor = scn.GetCardActionId(
                VirtualTableScenario.DS, labria, "Deploy");
        assertNotNull("The one-Force distractor must be legal", distractor);
        var bots = PublicBots.forGame(scn);
        assertEquals("Both bots must preserve the movement payment",
                "", bots.decideBoth(scn));
        scn.DSPass();
        scn.LSPass();

        assertTrue(scn.AwaitingDSBattlePhaseActions());
        assertEquals("No battle may consume the final movement Force",
                "", bots.decideBoth(scn));
        scn.DSPass();
        scn.LSPass();

        AwaitingDecision afterBattle = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertTrue("Expected DS move actions after the battle; phase="
                        + scn.gameState().getCurrentPhase()
                        + "; decision="
                        + (afterBattle != null
                            ? afterBattle.getText() : "none"),
                scn.AwaitingDSMovePhaseActions());
        AwaitingDecision moveParent = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        String moveResponse = bots.decideBoth(scn);
        PhysicalCard selectedMover = AiActionSourceProvenance
                .selectedActionSource(moveParent, moveResponse);
        assertTrue("One of the two Jungle Imperials must move; selected="
                        + moveResponse,
                selectedMover == jungleImperial
                    || selectedMover == mover);
        scn.DSDecided(moveResponse);

        AwaitingDecision destination = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(destination);
        String destinationResponse = bots.decideBoth(scn);
        assertSame("The surplus Imperial must move to the empty Desert",
                desert,
                selectedPhysicalCard(
                    scn, destination, destinationResponse));
        scn.DSDecided(destinationResponse);
        scn.PassAllResponses();

        assertEquals("The exact move must spend the banked Force",
                0, scn.GetDSForcePileCount());
        assertSame(desert,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsPresentAt(
                        scn.gameState(), selectedMover));
        assertTrue("The native move event must fire the actual flip",
                objective.isFlipped());
    }

    @Test
    public void publicBotsDeploySecondSiteThenBankMoveForceAndFlip() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var forest = scn.GetDSCard("forest");
        var jungle = scn.GetDSCard("jungle");
        var desert = scn.GetDSCard("desert");
        var moverOne = scn.GetDSFiller(1);
        var moverTwo = scn.GetDSFiller(2);
        var handImperial = scn.GetDSFiller(3);

        scn.MoveCardsToDSHand(handImperial);
        scn.StartGame();
        addAllSites(scn);
        scn.MoveCardsToLocation(jungle, moverOne, moverTwo);
        scn.SkipToDSTurn(Phase.DEPLOY);
        keepOnlyDarkReserveRouteCandidates(scn);
        keepOnlyDarkHandCards(scn, handImperial);
        keepExactlyDarkForce(scn, 2);

        var randoAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        var chosenAnalyzer =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        randoAnalyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosenAnalyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertEquals("The hand deploy still leaves a distinct move leg",
                1, randoAnalyzer.getRalltiirCurrentRouteForceReserve(
                    scn.game(), VirtualTableScenario.DS,
                    handImperial));
        assertEquals(
                randoAnalyzer.getRalltiirCurrentRouteForceReserve(
                    scn.game(), VirtualTableScenario.DS,
                    handImperial),
                chosenAnalyzer.getRalltiirCurrentRouteForceReserve(
                    scn.game(), VirtualTableScenario.DS,
                    handImperial));
        assertTrue(randoAnalyzer.advancesPreFlipRequirementAt(
                scn.game(), VirtualTableScenario.DS,
                handImperial, forest));
        assertFalse(randoAnalyzer.advancesPreFlipRequirementAt(
                scn.game(), VirtualTableScenario.DS,
                handImperial, jungle));
        assertTrue(randoAnalyzer.advancesPreFlipRequirementAt(
                scn.game(), VirtualTableScenario.DS,
                handImperial, desert));

        String deploy = scn.GetCardActionId(
                VirtualTableScenario.DS, handImperial, "Deploy");
        assertNotNull(deploy);
        var bots = PublicBots.forGame(scn);
        assertEquals("Both bots must deploy the second-site Imperial",
                deploy, bots.decideBoth(scn));
        scn.DSDecided(deploy);
        assertTrue("Forest must remain a prospective progress destination; zone="
                        + handImperial.getZone(),
                randoAnalyzer.isRalltiirFrontProgressDeployDestination(
                    scn.game(), VirtualTableScenario.DS,
                    handImperial, forest));
        assertTrue("Desert must remain a prospective progress destination; zone="
                        + handImperial.getZone(),
                randoAnalyzer.isRalltiirFrontProgressDeployDestination(
                    scn.game(), VirtualTableScenario.DS,
                    handImperial, desert));

        AwaitingDecision deployDestination = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(deployDestination);
        String deployDestinationResponse = bots.decideBoth(scn);
        PhysicalCard occupiedSite = selectedPhysicalCard(
                scn, deployDestination, deployDestinationResponse);
        assertTrue("The deploy must use an open Ralltiir site",
                occupiedSite == forest || occupiedSite == desert);
        scn.DSDecided(deployDestinationResponse);
        scn.PassAllResponses();

        assertEquals("The deploy must leave the movement payment banked",
                1, scn.GetDSForcePileCount());
        assertFalse(objective.isFlipped());
        scn.SkipToPhase(Phase.BATTLE);

        assertTrue(scn.AwaitingDSBattlePhaseActions());
        assertEquals("No battle may consume the movement payment",
                "", bots.decideBoth(scn));
        scn.SkipToPhase(Phase.MOVE);

        AwaitingDecision moveParent = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertTrue(scn.AwaitingDSMovePhaseActions());
        String moveResponse = bots.decideBoth(scn);
        PhysicalCard selectedMover = AiActionSourceProvenance
                .selectedActionSource(moveParent, moveResponse);
        assertTrue(selectedMover == moverOne || selectedMover == moverTwo);
        scn.DSDecided(moveResponse);

        AwaitingDecision moveDestination = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(moveDestination);
        String moveDestinationResponse = bots.decideBoth(scn);
        PhysicalCard expectedDestination = occupiedSite == forest
                ? desert : forest;
        assertSame("The surplus Imperial must fill the remaining site",
                expectedDestination,
                selectedPhysicalCard(
                    scn, moveDestination, moveDestinationResponse));
        scn.DSDecided(moveDestinationResponse);
        scn.PassAllResponses();

        assertEquals(0, scn.GetDSForcePileCount());
        assertTrue("The distinct deploy-plus-move route must flip natively",
                objective.isFlipped());
    }

    @Test
    public void publicBotsBankBattleForceClearThirdSiteAndFlip() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var forest = scn.GetDSCard("forest");
        var jungle = scn.GetDSCard("jungle");
        var desert = scn.GetDSCard("desert");
        var labria = scn.GetDSCard("labria");
        var rebel = scn.GetLSFiller(1);

        scn.MoveCardsToDSHand(labria);
        scn.StartGame();
        addAllSites(scn);
        scn.MoveCardsToLocation(forest, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(jungle, scn.GetDSFiller(2));
        scn.MoveCardsToLocation(
                desert, scn.GetDSFiller(3),
                scn.GetDSFiller(4), scn.GetDSFiller(5),
                scn.GetDSFiller(6), rebel);
        scn.SkipToDSTurn(Phase.DEPLOY);
        keepOnlyDarkReserveRouteCandidates(scn);
        keepOnlyDarkHandCards(scn, labria);
        keepExactlyDarkForce(scn, 1);

        var randoAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        var chosenAnalyzer =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        randoAnalyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosenAnalyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertEquals("One Force must be reserved for the winnable route battle",
                1, randoAnalyzer.getRalltiirCurrentBattleForceReserve(
                    scn.game(), VirtualTableScenario.DS));
        assertEquals(
                randoAnalyzer.getRalltiirCurrentBattleForceReserve(
                    scn.game(), VirtualTableScenario.DS),
                chosenAnalyzer.getRalltiirCurrentBattleForceReserve(
                    scn.game(), VirtualTableScenario.DS));
        assertEquals("The contested site already has its required Imperial",
                0, randoAnalyzer.getRalltiirCurrentMoveForceReserve(
                    scn.game(), VirtualTableScenario.DS));
        assertEquals(
                randoAnalyzer.getRalltiirCurrentMoveForceReserve(
                    scn.game(), VirtualTableScenario.DS),
                chosenAnalyzer.getRalltiirCurrentMoveForceReserve(
                    scn.game(), VirtualTableScenario.DS));

        var bots = PublicBots.forGame(scn);
        assertEquals("The distractor may not spend the battle payment",
                "", bots.decideBoth(scn));
        scn.DSPass();
        scn.LSPass();

        assertTrue(scn.AwaitingDSBattlePhaseActions());
        scn.PrepareDSDestiny(7);
        scn.PrepareLSDestiny(0);
        String battle = scn.GetCardActionId(
                VirtualTableScenario.DS, desert,
                "Initiate battle");
        assertNotNull(battle);
        assertEquals("Both bots must clear the exact contested third site",
                battle, bots.decideBoth(scn));

        scn.DSInitiateBattle(desert);
        scn.SkipToDamageSegment(true);
        assertTrue(scn.AwaitingLSBattleDamagePayment());
        scn.LSPayBattleDamageFromCardInPlay(rebel);
        if (scn.AwaitingLSBattleDamagePayment()) {
            scn.LSPayRemainingBattleDamageFromReserveDeck();
        }
        scn.PassAllResponses();

        assertTrue("The real battle removal must fire the native flip",
                objective.isFlipped());
    }

    @Test
    public void publicBotsFundBattleThenMoveAsOneFlipChain() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var forest = scn.GetDSCard("forest");
        var jungle = scn.GetDSCard("jungle");
        var desert = scn.GetDSCard("desert");
        var labria = scn.GetDSCard("labria");
        var jungleImperial = scn.GetDSFiller(2);
        var mover = scn.GetDSFiller(3);
        var rebel = scn.GetLSFiller(1);

        scn.MoveCardsToDSHand(labria);
        scn.StartGame();
        addAllSites(scn);
        scn.MoveCardsToLocation(forest, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(jungle, jungleImperial, mover);
        scn.MoveCardsToLocation(
                desert, scn.GetDSCard("jawa"),
                scn.GetDSCard("jawaTwo"),
                scn.GetDSCard("jawaThree"),
                scn.GetDSCard("jawaFour"), rebel);
        scn.SkipToDSTurn(Phase.DEPLOY);
        keepOnlyDarkReserveRouteCandidates(scn);
        keepOnlyDarkHandCards(scn, labria);
        keepExactlyDarkForce(scn, 2);

        var analyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        analyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertEquals("The live turn needs one battle and one move payment",
                2, analyzer.getRalltiirCurrentRouteForceReserve(
                    scn.game(), VirtualTableScenario.DS));
        assertEquals("The cleared site still needs an Imperial to move in",
                1, analyzer.getRalltiirCurrentMoveForceReserve(
                    scn.game(), VirtualTableScenario.DS));
        var chosenAnalyzer =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        chosenAnalyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertEquals(
                analyzer.getRalltiirCurrentMoveForceReserve(
                    scn.game(), VirtualTableScenario.DS),
                chosenAnalyzer.getRalltiirCurrentMoveForceReserve(
                    scn.game(), VirtualTableScenario.DS));

        var bots = PublicBots.forGame(scn);
        assertNotNull(scn.GetCardActionId(
                VirtualTableScenario.DS, labria, "Deploy"));
        assertEquals("The deploy phase must preserve both later payments",
                "", bots.decideBoth(scn));
        scn.DSPass();
        scn.LSPass();

        scn.PrepareDSDestiny(7);
        scn.PrepareLSDestiny(0);
        String battle = scn.GetCardActionId(
                VirtualTableScenario.DS, desert,
                "Initiate battle");
        assertNotNull(battle);
        assertEquals("The route battle may spend only its half of the budget",
                battle, bots.decideBoth(scn));
        scn.DSInitiateBattle(desert);
        scn.SkipToDamageSegment(true);
        scn.LSPayBattleDamageFromCardInPlay(rebel);
        if (scn.AwaitingLSBattleDamagePayment()) {
            scn.LSPayRemainingBattleDamageFromReserveDeck();
        }
        scn.PassAllResponses();
        assertFalse("Aliens control the site but do not satisfy WITH an Imperial",
                objective.isFlipped());

        for (int i = 0;
                i < 4 && !scn.AwaitingDSMovePhaseActions(); i++) {
            if (scn.AwaitingDSBattlePhaseActions()) {
                scn.DSPass();
            } else if (scn.AwaitingLSBattlePhaseActions()) {
                scn.LSPass();
            } else {
                break;
            }
        }
        AwaitingDecision afterRouteBattle = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertTrue("Expected DS move actions after the route battle; phase="
                        + scn.gameState().getCurrentPhase()
                        + "; decision="
                        + (afterRouteBattle != null
                            ? afterRouteBattle.getText() : "none"),
                scn.AwaitingDSMovePhaseActions());
        AwaitingDecision moveParent = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        String moveResponse = bots.decideBoth(scn);
        PhysicalCard selectedMover = AiActionSourceProvenance
                .selectedActionSource(moveParent, moveResponse);
        assertTrue(selectedMover == jungleImperial
                || selectedMover == mover);
        scn.DSDecided(moveResponse);
        assertEquals(Integer.toString(desert.getCardId()),
                bots.decideBoth(scn));
        scn.DSDecided(Integer.toString(desert.getCardId()));
        scn.PassAllResponses();

        assertEquals(0, scn.GetDSForcePileCount());
        assertTrue("Battle plus movement must complete the real flip",
                objective.isFlipped());
    }

    @Test
    public void publicBotsLoseFodderBeforeMissingSiteAndUniqueImperial() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var forest = scn.GetDSCard("forest");
        var jungle = scn.GetDSCard("jungle");
        var desert = scn.GetDSCard("desert");
        var finalImperial = scn.GetDSCard("uniqueImperial");
        var labria = scn.GetDSCard("labria");

        scn.StartGame();
        moveSiteToRalltiir(scn, forest);
        moveSiteToRalltiir(scn, jungle);
        scn.MoveCardsToLocation(forest, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(jungle, scn.GetDSFiller(2));
        scn.MoveCardsToLocation(
                scn.GetLSStartingLocation(), scn.GetLSFiller(1));
        scn.SkipToLSTurn(Phase.CONTROL);
        scn.MoveCardsToDSHand(finalImperial, labria);
        scn.MoveCardsToTopOfDSReserveDeck(desert);
        keepOnlyDarkLifeForce(
                scn, desert, finalImperial, labria);

        var analyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        var chosen =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        analyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue(Filters.Imperial.accepts(
                scn.gameState(), scn.game().getModifiersQuerying(),
                finalImperial));
        assertFalse(Filters.non_unique.accepts(
                scn.gameState(), scn.game().getModifiersQuerying(),
                finalImperial));
        assertTrue(analyzer
                .isPreferredCountedObjectiveLocationForceLossCandidate(
                    scn.game(), VirtualTableScenario.DS, desert));
        assertTrue(analyzer
                .isPreferredCountedObjectivePresenceForceLossCandidate(
                    scn.game(), VirtualTableScenario.DS,
                    finalImperial));
        assertTrue(chosen
                .isPreferredCountedObjectivePresenceForceLossCandidate(
                    scn.game(), VirtualTableScenario.DS,
                    finalImperial));
        assertEquals("The native route remains non-unique only",
                0, analyzer.getRalltiirFrontPullCandidatePriority(
                    scn.game(), VirtualTableScenario.DS,
                    objective, finalImperial));
        assertEquals(
                analyzer.getRalltiirFrontPullCandidatePriority(
                    scn.game(), VirtualTableScenario.DS,
                    objective, finalImperial),
                chosen.getRalltiirFrontPullCandidatePriority(
                    scn.game(), VirtualTableScenario.DS,
                    objective, finalImperial));

        scn.LSForceDrainAt(scn.GetLSStartingLocation());
        scn.PassAllResponses();
        assertTrue(scn.DSDecisionAvailable("Choose Force to lose"));
        AwaitingDecision lossDecision = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        var offered = java.util.Arrays.asList(
                lossDecision.getDecisionParameters().get("cardId"));
        assertTrue(offered.contains(
                Integer.toString(desert.getCardId())));
        assertTrue(offered.contains(
                Integer.toString(finalImperial.getCardId())));
        assertTrue(offered.contains(
                Integer.toString(labria.getCardId())));

        String loss = PublicBots.forGame(scn).decideBoth(scn);
        assertEquals("Both bots must spend the unrelated fodder",
                Integer.toString(labria.getCardId()), loss);
        scn.DSDecided(loss);
        scn.PassAllResponses();

        assertTrue(scn.gameState().getReserveDeck(
                VirtualTableScenario.DS).contains(desert));
        assertSame(Zone.HAND, finalImperial.getZone());
        assertTrue("Labria must be the card actually lost",
                labria.getZone() == Zone.LOST_PILE
                    || labria.getZone() == Zone.TOP_OF_LOST_PILE);
    }

    @Test
    public void publicBotsForfeitLabriaBeforeTheThirdSiteImperial() {
        var scn = scenario();
        var forest = scn.GetDSCard("forest");
        var jungle = scn.GetDSCard("jungle");
        var desert = scn.GetDSCard("desert");
        var finalImperial = scn.GetDSFiller(3);
        var labria = scn.GetDSCard("labria");

        scn.StartGame();
        addAllSites(scn);
        scn.MoveCardsToLocation(forest, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(jungle, scn.GetDSFiller(2));
        scn.MoveCardsToLocation(
                desert, finalImperial, labria);
        scn.MoveCardsToLocation(
                desert, scn.GetLSFillerRange(1, 8));

        var analyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        analyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                analyzer.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.DS,
                    finalImperial));
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                analyzer.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.DS, labria));

        scn.LSActivateForceCheat(1);
        scn.SkipToLSTurn(Phase.BATTLE);
        scn.LSInitiateBattle(desert);
        scn.SkipToDamageSegment(false);

        AwaitingDecision pendingDark = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        AwaitingDecision pendingLight = scn.GetAwaitingDecision(
                VirtualTableScenario.LS);
        assertTrue("The real battle must require a Dark Side forfeiture; DS="
                    + (pendingDark != null ? pendingDark.getText() : "none")
                    + "; LS="
                    + (pendingLight != null ? pendingLight.getText() : "none"),
                scn.AwaitingDSAttritionPayment()
                    || scn.AwaitingDSBattleDamagePayment());
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole
                        .LAST_REQUIRED_ACTOR,
                analyzer.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.DS,
                    finalImperial));
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                analyzer.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.DS, labria));
        AwaitingDecision forfeit = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(forfeit);
        String[] offered = forfeit.getDecisionParameters().get("cardId");
        assertNotNull(offered);
        var offeredIds = java.util.Arrays.asList(offered);
        assertTrue(offeredIds.contains(
                Integer.toString(finalImperial.getCardId())));
        assertTrue(offeredIds.contains(
                Integer.toString(labria.getCardId())));

        String selected = PublicBots.forGame(scn).decideBoth(scn);
        assertEquals("The final site Imperial must survive while Labria is legal",
                Integer.toString(labria.getCardId()), selected);
        scn.DSDecided(selected);
        scn.PassAllResponses();

        assertTrue(labria.getZone() == Zone.LOST_PILE
                || labria.getZone() == Zone.TOP_OF_LOST_PILE);
        assertSame(Zone.AT_LOCATION, finalImperial.getZone());
    }

    @Test
    public void duplicateImperialsAtContestedThirdSiteRemainOrdinaryForfeits() {
        var scn = scenario();
        var forest = scn.GetDSCard("forest");
        var jungle = scn.GetDSCard("jungle");
        var desert = scn.GetDSCard("desert");
        var firstImperial = scn.GetDSFiller(3);
        var secondImperial = scn.GetDSFiller(4);

        scn.StartGame();
        addAllSites(scn);
        scn.MoveCardsToLocation(forest, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(jungle, scn.GetDSFiller(2));
        scn.MoveCardsToLocation(
                desert, firstImperial, secondImperial);
        scn.MoveCardsToLocation(
                desert, scn.GetLSFillerRange(1, 8));

        var analyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        analyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);

        scn.LSActivateForceCheat(1);
        scn.SkipToLSTurn(Phase.BATTLE);
        scn.LSInitiateBattle(desert);
        scn.SkipToDamageSegment(false);

        assertTrue(scn.AwaitingDSAttritionPayment()
                || scn.AwaitingDSBattleDamagePayment());
        assertEquals("Either Imperial can replace the other at the third site",
                ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                analyzer.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.DS,
                    firstImperial));
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                analyzer.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.DS,
                    secondImperial));
    }

    @Test
    public void publicBotsUseBackTutorForExactlyTwoForceWithHealthyReserve() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var reserveOne = scn.GetDSFiller(4);
        var reserveTwo = scn.GetDSFiller(5);
        var reserveThree = scn.GetDSFiller(6);
        var forceOne = scn.GetDSFiller(7);
        var forceTwo = scn.GetDSFiller(8);

        startFlipped(scn);
        scn.MoveOutOfPlay(scn.GetDSFiller(1));
        scn.MoveOutOfPlay(scn.GetDSFiller(2));
        scn.MoveOutOfPlay(scn.GetDSFiller(3));
        scn.MoveOutOfPlay(scn.GetDSCard("labria"));
        scn.SkipToDSTurn(Phase.CONTROL);
        keepOnlyDarkLifeForce(scn,
                reserveOne, reserveTwo, reserveThree,
                forceOne, forceTwo);
        scn.MoveCardsToBottomOfDSReserveDeck(
                reserveOne, reserveTwo, reserveThree);
        scn.MoveCardsToTopOfDSForcePile(forceOne, forceTwo);

        assertEquals(3, scn.GetDSReserveDeckCount());
        assertEquals(2, scn.GetDSForcePileCount());
        String tutor = scn.GetCardActionId(
                VirtualTableScenario.DS, objective,
                "Take card into hand from Reserve Deck");
        assertNotNull(tutor);

        var bots = PublicBots.forGame(scn);
        assertEquals("Both bots must use the source-verified back-side tutor",
                tutor, bots.decideBoth(scn));
        scn.DSDecided(tutor);
        scn.PassAllResponses();

        AwaitingDecision child = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(child);
        assertEquals("Choose card to take into hand", child.getText());
        assertEquals("1", child.getDecisionParameters().get("min")[0]);
        assertEquals("1", child.getDecisionParameters().get("max")[0]);
        String selected = bots.decideBoth(scn);
        PhysicalCard selectedCard = selectedPhysicalCard(
                scn, child, selected);
        assertNotNull(selectedCard);
        scn.DSDecided(selected);
        scn.PassAllResponses();

        assertEquals("The objective must use exactly 2 Force", 0,
                scn.GetDSForcePileCount());
        assertSame(Zone.HAND, selectedCard.getZone());
        if (scn.GetAwaitingDecision(VirtualTableScenario.LS) != null) {
            scn.LSPass();
        }
        assertFalse("The tutor is once during each control phase",
                scn.DSCardActionAvailable(
                    objective,
                    "Take card into hand from Reserve Deck"));
    }

    @Test
    public void publicBotsTutorUrgentHoldBodyOverUnrelatedLocation() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var desert = scn.GetDSCard("desert");
        var swamp = scn.GetDSCard("swamp");
        var reinforcement = scn.GetDSCard("labria");
        var reserveOther = scn.GetDSCard("tie");
        var forceOne = scn.GetDSFiller(4);
        var forceTwo = scn.GetDSFiller(5);

        startFlipped(scn);
        scn.MoveOutOfPlay(scn.GetDSFiller(3));
        scn.MoveCardsToLocation(desert, scn.GetLSFiller(1));
        scn.SkipToDSTurn(Phase.CONTROL);
        keepOnlyDarkLifeForce(scn,
                reinforcement, swamp, reserveOther,
                forceOne, forceTwo);
        scn.MoveCardsToBottomOfDSReserveDeck(
                swamp, reserveOther, reinforcement);
        scn.MoveCardsToTopOfDSForcePile(forceOne, forceTwo);

        var rando =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        var chosen =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        rando.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertFalse("The hold law is presence, not the front side's Imperial restriction",
                Filters.Imperial.accepts(
                    scn.gameState(), scn.game().getModifiersQuerying(),
                    reinforcement));
        assertTrue(rando.isRalltiirBackUrgentHoldTutorCandidate(
                scn.game(), VirtualTableScenario.DS,
                objective, reinforcement));
        assertFalse(rando.isRalltiirBackUrgentHoldTutorCandidate(
                scn.game(), VirtualTableScenario.DS,
                objective, swamp));
        assertEquals(
                rando.isRalltiirBackUrgentHoldTutorCandidate(
                    scn.game(), VirtualTableScenario.DS,
                    objective, reinforcement),
                chosen.isRalltiirBackUrgentHoldTutorCandidate(
                    scn.game(), VirtualTableScenario.DS,
                    objective, reinforcement));

        String tutor = scn.GetCardActionId(
                VirtualTableScenario.DS, objective,
                "Take card into hand from Reserve Deck");
        assertNotNull(tutor);
        var bots = PublicBots.forGame(scn);
        assertEquals(tutor, bots.decideBoth(scn));
        scn.DSDecided(tutor);
        scn.PassAllResponses();

        AwaitingDecision child = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(child);
        assertEquals("ARBITRARY_CARDS",
                child.getDecisionType().name());
        assertEquals("Choose card to take into hand", child.getText());
        var actionState = scn.gameState().getTopGameTextActionState();
        assertNotNull(actionState);
        var liveAction = actionState.getGameTextAction();
        assertNotNull(liveAction);
        assertSame(objective, liveAction.getActionSource());
        assertEquals(GameTextActionId
                .IN_THE_HANDS_OF_THE_EMPIRE__UPLOAD_CARD,
                liveAction.getGameTextActionId());
        assertTrue("The urgent candidate must remain structurally valid after paying the tutor",
                rando.isRalltiirBackUrgentHoldTutorCandidate(
                    scn.game(), VirtualTableScenario.DS,
                    objective, reinforcement));
        String selected = bots.decideBoth(scn);
        assertSame("The one-control flip-back threat must beat generic location value",
                reinforcement,
                selectedPhysicalCard(scn, child, selected));
        scn.DSDecided(selected);
        scn.PassAllResponses();

        assertSame(Zone.HAND, reinforcement.getZone());
        assertTrue(scn.gameState().getReserveDeck(
                VirtualTableScenario.DS).contains(swamp));
        assertEquals("The tutor must still pay exactly 2 Force",
                0, scn.GetDSForcePileCount());
        assertTrue("One opponent-controlled Ralltiir location is not enough to flip back",
                objective.isFlipped());
    }

    @Test
    public void publicBotsTutorPresenceShipForSoleSystemThreat() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var system = scn.GetDSCard("system");
        var victory = scn.GetDSCard("victory");
        var emptyShip = scn.GetDSCard("emptyShip");
        var swamp = scn.GetDSCard("swamp");
        var forceOne = scn.GetDSFiller(4);
        var forceTwo = scn.GetDSFiller(5);

        startFlipped(scn);
        scn.MoveCardsToLocation(system, scn.GetLSCard("xwing"));
        scn.MoveOutOfPlay(scn.GetDSCard("labria"));
        scn.SkipToDSTurn(Phase.CONTROL);
        keepOnlyDarkLifeForce(scn,
                victory, emptyShip, swamp,
                forceOne, forceTwo);
        scn.MoveCardsToBottomOfDSReserveDeck(
                swamp, emptyShip, victory);
        scn.MoveCardsToTopOfDSForcePile(forceOne, forceTwo);

        var rando =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        var chosen =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        rando.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue(rando.isRalltiirBackUrgentHoldTutorCandidate(
                scn.game(), VirtualTableScenario.DS,
                objective, victory));
        assertFalse("An unpiloted ship does not contest system control",
                rando.isRalltiirBackUrgentHoldTutorCandidate(
                    scn.game(), VirtualTableScenario.DS,
                    objective, emptyShip));
        assertFalse(rando.isRalltiirBackUrgentHoldTutorCandidate(
                scn.game(), VirtualTableScenario.DS,
                objective, swamp));
        assertEquals(
                rando.isRalltiirBackUrgentHoldTutorCandidate(
                    scn.game(), VirtualTableScenario.DS,
                    objective, victory),
                chosen.isRalltiirBackUrgentHoldTutorCandidate(
                    scn.game(), VirtualTableScenario.DS,
                    objective, victory));

        String tutor = scn.GetCardActionId(
                VirtualTableScenario.DS, objective,
                "Take card into hand from Reserve Deck");
        assertNotNull(tutor);
        var bots = PublicBots.forGame(scn);
        assertEquals(tutor, bots.decideBoth(scn));
        scn.DSDecided(tutor);
        scn.PassAllResponses();

        AwaitingDecision child = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(child);
        assertTrue("The system hold candidate must survive the tutor's 2-Force payment",
                rando.isRalltiirBackUrgentHoldTutorCandidate(
                    scn.game(), VirtualTableScenario.DS,
                    objective, victory));
        String selected = bots.decideBoth(scn);
        assertSame("Presence-capable space reinforcement must beat the unrelated location",
                victory,
                selectedPhysicalCard(scn, child, selected));
        scn.DSDecided(selected);
        scn.PassAllResponses();

        assertSame(Zone.HAND, victory.getZone());
        assertTrue(scn.gameState().getReserveDeck(
                VirtualTableScenario.DS).contains(swamp));
        assertTrue(scn.gameState().getReserveDeck(
                VirtualTableScenario.DS).contains(emptyShip));
        assertEquals(0, scn.GetDSForcePileCount());
        assertTrue(objective.isFlipped());
    }

    @Test
    public void publicBotsDeclineBackTutorAtTwoCardReserveBoundary() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var reserveOne = scn.GetDSFiller(4);
        var reserveTwo = scn.GetDSFiller(5);
        var forceOne = scn.GetDSFiller(6);
        var forceTwo = scn.GetDSFiller(7);

        startFlipped(scn);
        scn.MoveOutOfPlay(scn.GetDSFiller(1));
        scn.MoveOutOfPlay(scn.GetDSFiller(2));
        scn.MoveOutOfPlay(scn.GetDSFiller(3));
        scn.MoveOutOfPlay(scn.GetDSCard("labria"));
        scn.SkipToDSTurn(Phase.CONTROL);
        keepOnlyDarkLifeForce(scn,
                reserveOne, reserveTwo, forceOne, forceTwo);
        scn.MoveCardsToBottomOfDSReserveDeck(
                reserveOne, reserveTwo);
        scn.MoveCardsToTopOfDSForcePile(forceOne, forceTwo);

        assertEquals(2, scn.GetDSReserveDeckCount());
        assertEquals(2, scn.GetDSForcePileCount());
        assertTrue("The engine must still offer the legal tutor",
                scn.DSCardActionAvailable(
                    objective,
                    "Take card into hand from Reserve Deck"));
        assertEquals("The established two-card destiny buffer must dominate",
                "", PublicBots.forGame(scn).decideBoth(scn));
    }

    @Test
    public void publicBotsHoldSoleContestedBlockerAndManualMoveFlipsFront() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var forest = scn.GetDSCard("forest");
        var jungle = scn.GetDSCard("jungle");
        var desert = scn.GetDSCard("desert");
        var blocker = scn.GetDSFiller(1);

        startFlipped(scn);
        scn.MoveOutOfPlay(scn.GetDSFiller(2));
        scn.MoveOutOfPlay(scn.GetDSFiller(3));
        scn.MoveOutOfPlay(scn.GetDSCard("labria"));
        scn.MoveCardsToLocation(
                forest, scn.GetLSFiller(1), scn.GetLSFiller(2));
        scn.MoveCardsToLocation(desert, scn.GetLSFiller(3));
        scn.SkipToDSTurn(Phase.MOVE);
        keepExactlyDarkForce(scn, 1);

        var rando =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        var chosen =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        var risk = rando.assessPostFlipLocationRisk(
                scn.game(), VirtualTableScenario.DS, forest);
        assertEquals(1, risk.opponentControlCount());
        assertTrue("Losing the contested site gives the opponent location two",
                risk.criticalIfOpponentGainsControl());
        assertTrue(rando.wouldDepartureTriggerFlipBack(
                scn.game(), VirtualTableScenario.DS, blocker));
        assertEquals(
                rando.wouldDepartureTriggerFlipBack(
                    scn.game(), VirtualTableScenario.DS, blocker),
                chosen.wouldDepartureTriggerFlipBack(
                    scn.game(), VirtualTableScenario.DS, blocker));

        String move = scn.GetCardActionId(
                VirtualTableScenario.DS, blocker,
                "Move using landspeed");
        assertNotNull(move);
        assertEquals("Both bots must preserve the sole contested blocker",
                "", PublicBots.forGame(scn).decideBoth(scn));

        scn.DSDecided(move);
        assertTrue(scn.DSHasCardChoiceAvailable(jungle));
        scn.DSChooseCard(jungle);
        scn.PassAllResponses();

        assertEquals(0, scn.GetDSForcePileCount());
        assertFalse("The unchanged back-side trigger must flip front",
                objective.isFlipped());
    }

    @Test
    public void publicBotsVetoCpiForClassicAndVirtualRalltiir() {
        assertCpiVetoAndManualCounterfactual(
                "2_148", "Attempt to 'blow away' Ralltiir");
        assertCpiVetoAndManualCounterfactual(
                "220_3", "Attempt to 'blow away' Ralltiir (V)");
    }

    private void assertCpiVetoAndManualCounterfactual(
            String systemBlueprintId, String ignitionText) {
        var scn = scenario(systemBlueprintId);
        var objective = scn.GetDSCard("objective");
        var ralltiir = scn.GetDSCard("system");
        var deathStar = scn.GetDSCard("deathStar");
        var superlaser = scn.GetDSCard("superlaser");
        var cpi = scn.GetDSCard("cpi");

        scn.MoveCardsToDSHand(cpi);
        scn.StartGame();
        scn.MoveLocationToTable(deathStar);
        scn.MoveLocationToTable(scn.GetDSCard("deathStarSiteOne"));
        scn.MoveLocationToTable(scn.GetDSCard("deathStarSiteTwo"));
        scn.MoveLocationToTable(scn.GetDSCard("deathStarSiteThree"));
        deathStar.setSystemOrbited(Title.Ralltiir);
        scn.AttachCardsTo(deathStar, superlaser);
        scn.DSActivateForceCheat(8);
        scn.SkipToDSTurn(Phase.CONTROL);
        scn.PrepareDSDestiny(7);

        String ignition = scn.GetCardActionId(
                VirtualTableScenario.DS, cpi,
                ignitionText);
        assertNotNull("Classic CPI must be fully armed against "
                + ignitionText, ignition);
        var analyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        analyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue(analyzer.isRalltiirObjectiveSelfDestructAction(
                scn.game(), VirtualTableScenario.DS,
                cpi, ignitionText));
        assertEquals("Both bots must veto their own objective destruction",
                "", PublicBots.forGame(scn).decideBoth(scn));

        scn.DSPlayCard(cpi, ignitionText);
        scn.PassAllResponses();
        if (scn.LSDecisionAvailable("Choose value for Z")) {
            scn.LSChoose("Total sites at Hoth: 0");
            scn.PassAllResponses();
        }
        finishBlowAwayResolution(scn);

        assertTrue("The manual CPI counterfactual must blow away Ralltiir",
                ralltiir.isBlownAway());
        assertTrue(cpi.getZone() == Zone.LOST_PILE
                || cpi.getZone() == Zone.TOP_OF_LOST_PILE);
        assertSame("The unchanged objective trigger must place it out of play",
                Zone.OUT_OF_PLAY, objective.getZone());
    }

    @Test
    public void terminalSystemRouteRequiresPresenceThreeSitesAndOneBlocker() {
        var scn = scenario();
        var system = scn.GetDSCard("system");
        var forest = scn.GetDSCard("forest");
        var jungle = scn.GetDSCard("jungle");
        var desert = scn.GetDSCard("desert");
        var swamp = scn.GetDSCard("swamp");
        var presenceShip = scn.GetDSCard("victory");
        var emptyShip = scn.GetDSCard("emptyShip");

        scn.MoveCardsToDSHand(presenceShip, emptyShip);
        scn.StartGame();
        addAllSites(scn);
        scn.MoveCardsToLocation(forest, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(jungle, scn.GetDSFiller(2));
        scn.MoveCardsToLocation(system, scn.GetLSCard("xwing"));

        var rando =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        var chosen =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        rando.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);

        assertFalse("Two qualified sites cannot arm the terminal shortcut",
                rando.isRalltiirSoleSystemBlockerPresenceDestination(
                    scn.game(), VirtualTableScenario.DS,
                    presenceShip, system));
        assertFalse(rando.wouldCompletePreFlipRequirementAt(
                scn.game(), VirtualTableScenario.DS,
                presenceShip, system));
        assertFalse("An unpiloted ship never supplies the required presence",
                rando.isRalltiirSoleSystemBlockerPresenceDestination(
                    scn.game(), VirtualTableScenario.DS,
                    emptyShip, system));
        assertFalse(rando.wouldCompletePreFlipRequirementAt(
                scn.game(), VirtualTableScenario.DS,
                emptyShip, system));

        scn.MoveCardsToLocation(desert, scn.GetDSFiller(3));
        assertTrue("The exact three-site plus one-system-blocker state must arm",
                rando.isRalltiirSoleSystemBlockerPresenceDestination(
                    scn.game(), VirtualTableScenario.DS,
                    presenceShip, system));
        assertTrue(rando.wouldCompletePreFlipRequirementAt(
                scn.game(), VirtualTableScenario.DS,
                presenceShip, system));
        assertEquals(
                rando.wouldCompletePreFlipRequirementAt(
                    scn.game(), VirtualTableScenario.DS,
                    presenceShip, system),
                chosen.wouldCompletePreFlipRequirementAt(
                    scn.game(), VirtualTableScenario.DS,
                    presenceShip, system));

        moveSiteToRalltiir(scn, swamp);
        scn.MoveCardsToLocation(swamp, scn.GetLSFiller(1));
        assertFalse("Two opponent-controlled Ralltiir locations are not a sole blocker",
                rando.isRalltiirSoleSystemBlockerPresenceDestination(
                    scn.game(), VirtualTableScenario.DS,
                    presenceShip, system));
        assertFalse(rando.wouldCompletePreFlipRequirementAt(
                scn.game(), VirtualTableScenario.DS,
                presenceShip, system));
        assertEquals(
                rando.wouldCompletePreFlipRequirementAt(
                    scn.game(), VirtualTableScenario.DS,
                    presenceShip, system),
                chosen.wouldCompletePreFlipRequirementAt(
                    scn.game(), VirtualTableScenario.DS,
                    presenceShip, system));
    }

    @Test
    public void publicBotsDeployPresenceShipToSoleSystemBlockerAndFlip() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var system = scn.GetDSCard("system");
        var dantooine = scn.GetDSCard("dantooine");
        var tie = scn.GetDSCard("tie");
        var victory = scn.GetDSCard("victory");
        var labria = scn.GetDSCard("labria");

        scn.MoveCardsToDSHand(tie, labria);
        scn.StartGame();
        addAllSites(scn);
        scn.MoveCardsToLocation(system, scn.GetLSCard("xwing"));
        scn.MoveCardsToLocation(
                scn.GetDSCard("forest"), scn.GetDSFiller(1));
        scn.MoveCardsToLocation(
                scn.GetDSCard("jungle"), scn.GetDSFiller(2));
        scn.MoveCardsToLocation(
                scn.GetDSCard("desert"), scn.GetDSFiller(3));
        scn.MoveLocationToTable(dantooine);
        scn.MoveCardsToLocation(dantooine, victory);
        scn.SkipToDSTurn(Phase.DEPLOY);
        keepOnlyDarkReserveRouteCandidates(scn);
        keepOnlyDarkHandCards(scn, tie, labria);
        keepExactlyDarkForce(scn, 1);

        assertFalse(objective.isFlipped());
        assertTrue(GameConditions.controls(
                scn.game(), VirtualTableScenario.LS, system));
        var rando =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        var chosen =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue(rando.isRalltiirSoleSystemBlockerPresenceDestination(
                scn.game(), VirtualTableScenario.DS, tie, system));
        assertFalse(rando.isRalltiirSoleSystemBlockerPresenceDestination(
                scn.game(), VirtualTableScenario.DS, tie, dantooine));
        assertTrue(rando.wouldCompletePreFlipRequirementAt(
                scn.game(), VirtualTableScenario.DS, tie, system));
        assertFalse(rando.wouldCompletePreFlipRequirementAt(
                scn.game(), VirtualTableScenario.DS, tie, dantooine));
        assertEquals("The advancing deploy pays its own one-Force cost",
                0, rando.getRalltiirCurrentRouteForceReserve(
                    scn.game(), VirtualTableScenario.DS, tie));
        assertEquals("An unrelated deploy must preserve the TIE payment",
                1, rando.getRalltiirCurrentRouteForceReserve(
                    scn.game(), VirtualTableScenario.DS, labria));
        assertEquals("Deploy and hyperspeed are alternatives, not additive",
                1, rando.getRalltiirCurrentMoveForceReserve(
                    scn.game(), VirtualTableScenario.DS));
        assertEquals(
                rando.getRalltiirCurrentRouteForceReserve(
                    scn.game(), VirtualTableScenario.DS, labria),
                chosen.getRalltiirCurrentRouteForceReserve(
                    scn.game(), VirtualTableScenario.DS, labria));

        String tieDeploy = scn.GetCardActionId(
                VirtualTableScenario.DS, tie, "Deploy");
        assertNotNull(tieDeploy);
        var bots = PublicBots.forGame(scn);
        assertEquals("The terminal presence ship must beat ground deployment",
                tieDeploy, bots.decideBoth(scn));
        scn.DSDecided(tieDeploy);
        assertTrue(scn.DSHasCardChoiceAvailable(system));
        assertTrue(scn.DSHasCardChoiceAvailable(dantooine));
        assertEquals(Integer.toString(system.getCardId()),
                bots.decideBoth(scn));
        scn.DSDecided(Integer.toString(system.getCardId()));
        scn.PassAllResponses();

        assertEquals(0, scn.GetDSForcePileCount());
        assertSame(system,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsPresentAt(
                        scn.gameState(), tie));
        assertFalse(GameConditions.controls(
                scn.game(), VirtualTableScenario.LS, system));
        assertTrue("Presence at the sole system blocker must flip natively",
                objective.isFlipped());
    }

    @Test
    public void publicBotsPilotEmptyShipAtSoleSystemBlockerAndFlip() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var system = scn.GetDSCard("system");
        var dantooine = scn.GetDSCard("dantooine");
        var pilot = scn.GetDSCard("pilot");
        var emptyShip = scn.GetDSCard("emptyShip");
        var emptyShipDecoy = scn.GetDSCard("emptyShipDecoy");
        var labria = scn.GetDSCard("labria");

        scn.MoveCardsToDSHand(pilot, labria);
        scn.StartGame();
        addAllSites(scn);
        scn.MoveCardsToLocation(system, scn.GetLSCard("xwing"));
        scn.MoveCardsToLocation(system, emptyShip);
        scn.MoveLocationToTable(dantooine);
        scn.MoveCardsToLocation(dantooine, emptyShipDecoy);
        scn.MoveCardsToLocation(
                scn.GetDSCard("forest"), scn.GetDSFiller(1));
        scn.MoveCardsToLocation(
                scn.GetDSCard("jungle"), scn.GetDSFiller(2));
        scn.MoveCardsToLocation(
                scn.GetDSCard("desert"), scn.GetDSFiller(3));
        scn.SkipToDSTurn(Phase.DEPLOY);
        keepOnlyDarkReserveRouteCandidates(scn);
        keepOnlyDarkHandCards(scn, pilot, labria);
        int pilotCost = (int) Math.ceil(
                scn.game().getModifiersQuerying().getDeployCost(
                    scn.gameState(), pilot, pilot,
                    emptyShip, false, null, false,
                    0.0f, null, true));
        keepExactlyDarkForce(scn, pilotCost);

        assertTrue(GameConditions.controls(
                scn.game(), VirtualTableScenario.LS, system));
        assertFalse(GameConditions.occupies(
                scn.game(), VirtualTableScenario.DS, system));
        var rando =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        var chosen =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue(rando.isRalltiirSoleSystemBlockerPilotHost(
                scn.game(), VirtualTableScenario.DS,
                pilot, emptyShip));
        assertFalse(rando.isRalltiirSoleSystemBlockerPilotHost(
                scn.game(), VirtualTableScenario.DS,
                pilot, emptyShipDecoy));
        assertTrue(rando.hasLegalRalltiirSoleSystemBlockerDeployRoute(
                scn.game(), VirtualTableScenario.DS, pilot));
        assertTrue(rando.wouldCompletePreFlipRequirementAt(
                scn.game(), VirtualTableScenario.DS,
                pilot, emptyShip));
        assertEquals(0, rando.getRalltiirCurrentRouteForceReserve(
                scn.game(), VirtualTableScenario.DS, pilot));
        assertEquals(pilotCost,
                rando.getRalltiirCurrentRouteForceReserve(
                    scn.game(), VirtualTableScenario.DS, labria));
        assertEquals(
                rando.getRalltiirCurrentRouteForceReserve(
                    scn.game(), VirtualTableScenario.DS, labria),
                chosen.getRalltiirCurrentRouteForceReserve(
                    scn.game(), VirtualTableScenario.DS, labria));

        String deployPilot = scn.GetCardActionId(
                VirtualTableScenario.DS, pilot, "Deploy");
        assertNotNull(deployPilot);
        var bots = PublicBots.forGame(scn);
        assertEquals("The pilot route must beat unrelated ground deployment",
                deployPilot, bots.decideBoth(scn));
        scn.DSDecided(deployPilot);
        assertTrue(scn.DSHasCardChoiceAvailable(emptyShip));
        assertTrue(scn.DSHasCardChoiceAvailable(emptyShipDecoy));
        assertEquals(Integer.toString(emptyShip.getCardId()),
                bots.decideBoth(scn));
        scn.DSDecided(Integer.toString(emptyShip.getCardId()));
        scn.PassAllResponses();
        resolveDarkBotChoicesUntil(
                scn, bots, pilot, Zone.AT_LOCATION);

        assertEquals(0, scn.GetDSForcePileCount());
        assertSame(emptyShip,
                scn.game().getModifiersQuerying()
                    .getIsPilotOf(scn.gameState(), pilot));
        assertFalse(GameConditions.controls(
                scn.game(), VirtualTableScenario.LS, system));
        assertTrue("Boarding the empty ship must fire the native flip",
                objective.isFlipped());
    }

    @Test
    public void publicBotsBankHyperspeedForceToSoleSystemBlockerAndFlip() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var system = scn.GetDSCard("system");
        var dantooine = scn.GetDSCard("dantooine");
        var alderaan = scn.GetDSCard("alderaan");
        var victory = scn.GetDSCard("victory");
        var victoryBattle = scn.GetDSCard("victoryBattle");
        var vader = scn.GetDSCard("vader");
        var labria = scn.GetDSCard("labria");

        scn.MoveCardsToDSHand(labria);
        scn.StartGame();
        addAllSites(scn);
        scn.MoveCardsToLocation(system, scn.GetLSCard("xwing"));
        scn.MoveCardsToLocation(
                scn.GetDSCard("forest"), scn.GetDSFiller(1));
        scn.MoveCardsToLocation(
                scn.GetDSCard("jungle"), scn.GetDSFiller(2));
        scn.MoveCardsToLocation(
                scn.GetDSCard("desert"), scn.GetDSFiller(3));
        scn.MoveLocationToTable(dantooine);
        scn.MoveLocationToTable(alderaan);
        scn.MoveCardsToLocation(dantooine, victory);
        scn.MoveCardsToLocation(
                alderaan, victoryBattle, scn.GetLSCard("xwingBattle"));
        scn.BoardAsPilot(victoryBattle, vader);
        scn.SkipToDSTurn(Phase.DEPLOY);
        keepOnlyDarkReserveRouteCandidates(scn);
        keepOnlyDarkHandCards(scn, labria);
        keepExactlyDarkForce(scn, 1);

        var rando =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        var chosen =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue(rando.isRalltiirSoleSystemBlockerPresenceDestination(
                scn.game(), VirtualTableScenario.DS, victory, system));
        assertFalse(rando.isRalltiirSoleSystemBlockerPresenceDestination(
                scn.game(), VirtualTableScenario.DS, victory, alderaan));
        assertTrue(rando.wouldCompletePreFlipRequirementAt(
                scn.game(), VirtualTableScenario.DS, victory, system));
        assertFalse(rando.wouldCompletePreFlipRequirementAt(
                scn.game(), VirtualTableScenario.DS, victory, alderaan));
        assertEquals(1, rando.getRalltiirCurrentMoveForceReserve(
                scn.game(), VirtualTableScenario.DS));
        assertEquals(1, rando.getRalltiirCurrentRouteForceReserve(
                scn.game(), VirtualTableScenario.DS, labria));
        assertEquals(
                rando.getRalltiirCurrentMoveForceReserve(
                    scn.game(), VirtualTableScenario.DS),
                chosen.getRalltiirCurrentMoveForceReserve(
                    scn.game(), VirtualTableScenario.DS));

        assertNotNull(scn.GetCardActionId(
                VirtualTableScenario.DS, labria, "Deploy"));
        var bots = PublicBots.forGame(scn);
        assertEquals("Deploy must bank the exact hyperspeed payment",
                "", bots.decideBoth(scn));
        scn.DSPass();
        scn.LSPass();
        assertTrue("A real favorable battle must compete for the last Force",
                scn.DSCanInitiateBattle(alderaan));
        assertEquals("Battle must preserve that same movement payment",
                "", bots.decideBoth(scn));
        scn.DSPass();
        scn.LSPass();

        assertTrue(scn.AwaitingDSMovePhaseActions());
        String move = scn.GetCardActionId(
                VirtualTableScenario.DS, victory,
                "Move using hyperspeed");
        assertNotNull(move);
        assertEquals(move, bots.decideBoth(scn));
        scn.DSDecided(move);
        assertTrue(scn.DSHasCardChoiceAvailable(system));
        assertTrue(scn.DSHasCardChoiceAvailable(alderaan));
        assertEquals(Integer.toString(system.getCardId()),
                bots.decideBoth(scn));
        scn.DSDecided(Integer.toString(system.getCardId()));
        scn.PassAllResponses();

        assertEquals(0, scn.GetDSForcePileCount());
        assertSame(system,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsPresentAt(
                        scn.gameState(), victory));
        assertFalse(GameConditions.controls(
                scn.game(), VirtualTableScenario.LS, system));
        assertTrue("The unchanged table-change trigger must flip",
                objective.isFlipped());
    }

    @Test
    public void mirroredAnalyzersHydrateTheExactRalltiirProfile() {
        var scn = scenario();
        scn.StartGame();
        var rando = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue(rando.isRalltiirOperationsFamily());
        assertEquals(rando.getObjectiveBlueprintId(),
                chosen.getObjectiveBlueprintId());
    }
}
