package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.PhysicalCardImpl;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Adversarial destination contract for They Have No Idea We're Coming
 * (209_29). The real objective pull deploys Rogue One with Bodhi while both
 * Scarif and an unrelated system are legal destinations.
 */
public class TheyHaveNoIdeaRogueOneDestinationEngineContractTest {

    private static final StartingSetup THEY_HAVE_NO_IDEA =
            new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "209_29");
                put("system", "209_23");
                put("dataVault", "209_25");
                put("stardust", "209_18");
                put("warRoom", "1_139");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            for (int i = 0; i < 10; i++) {
                if (scn.LSDecisionAvailable("On which side")) {
                    scn.LSChoose("Left");
                } else if (scn.LSDecisionAvailable("to deploy")) {
                    scn.LSChooseCard(scn.GetLSCard("dataVault"));
                }
            }
        }
    };

    private record PublicBots(
            com.gempukku.swccgo.ai.models.rando.RandoCalAi rando,
            com.gempukku.swccgo.ai.models.chosenone.TheChosenOneAi chosen) {
        private static PublicBots forGame(VirtualTableScenario scn) {
            var rando = new com.gempukku.swccgo.ai.models.rando.RandoCalAi();
            var chosen = new com.gempukku.swccgo.ai.models.chosenone
                    .TheChosenOneAi();
            rando.setGame(scn.game());
            chosen.setGame(scn.game());
            return new PublicBots(rando, chosen);
        }

        private String decideBoth(VirtualTableScenario scn) {
            AwaitingDecision decision = scn.GetAwaitingDecision(
                    VirtualTableScenario.LS);
            assertNotNull("Expected a Light Side decision", decision);
            String randoResponse = rando.decide(
                    VirtualTableScenario.LS,
                    decision, scn.gameState());
            String chosenResponse = chosen.decide(
                    VirtualTableScenario.LS,
                    decision, scn.gameState());
            assertEquals("Rando and Chosen One must agree for "
                            + decision.getText(),
                    randoResponse, chosenResponse);
            return randoResponse;
        }
    }

    private VirtualTableScenario scenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("landingPad", "209_26");
                    put("rogueOne", "206_7");
                    put("bodhi", "206_1");
                    put("trooper", "1_28");
                }},
                new HashMap<>(),
                24,
                24,
                THEY_HAVE_NO_IDEA,
                StartingSetup.DefaultDSSpaceSystem,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open);
    }

    private void keepOnlyLightHandCards(
            VirtualTableScenario scn, PhysicalCard... keep) {
        List<PhysicalCard> kept = List.of(keep);
        for (PhysicalCard card : new ArrayList<>(
                scn.gameState().getHand(VirtualTableScenario.LS))) {
            if (!kept.contains(card)) {
                scn.MoveCardsToBottomOfLSReserveDeck(
                        (PhysicalCardImpl) card);
            }
        }
    }

    private void moveLandingPadToScarif(
            VirtualTableScenario scn, PhysicalCardImpl landingPad) {
        scn.RemoveCardZone(landingPad);
        var placements = scn.gameState().getLocationPlacement(
                scn.game(), landingPad,
                com.gempukku.swccgo.common.Title.Scarif, null);
        assertFalse("Landing Pad Nine must have a Scarif placement",
                placements.isEmpty());
        scn.gameState().addLocationToTable(
                scn.game(), landingPad, placements.getFirst());
    }

    private String offeredResponseForBlueprint(
            AwaitingDecision decision, String blueprintId) {
        String[] cardIds = decision.getDecisionParameters().get("cardId");
        String[] blueprintIds = decision.getDecisionParameters()
                .get("blueprintId");
        assertNotNull("Expected card ids for " + decision.getText(),
                cardIds);
        assertNotNull("Expected blueprint ids for " + decision.getText(),
                blueprintIds);
        int index = Arrays.asList(blueprintIds).indexOf(blueprintId);
        assertTrue("Expected offered blueprint " + blueprintId
                        + "; prompt=" + decision.getText()
                        + "; options=" + Arrays.toString(blueprintIds),
                index >= 0);
        return cardIds[index];
    }

    @Test
    public void publicBotsChooseScarifOverAnotherLegalSystemForRogueOne() {
        var scn = scenario();
        var objective = scn.GetLSCard("objective");
        var scarif = scn.GetLSCard("system");
        var landingPad = scn.GetLSCard("landingPad");
        var rogueOne = scn.GetLSCard("rogueOne");
        var bodhi = scn.GetLSCard("bodhi");
        var trooper = scn.GetLSCard("trooper");

        scn.MoveCardsToLSHand(bodhi, trooper);
        scn.StartGame();
        keepOnlyLightHandCards(scn, bodhi, trooper);
        moveLandingPadToScarif(scn, landingPad);
        scn.MoveCardsToBottomOfLSReserveDeck(rogueOne);
        scn.LSActivateForceCheat(5);
        scn.SkipToLSTurn(Phase.DEPLOY);
        while (scn.GetLSForcePileCount() > 5) {
            scn.MoveCardsToTopOfLSUsedPile(
                    scn.GetTopOfLSForcePile());
        }
        assertEquals("The real route starts with exactly five Force",
                5, scn.GetLSForcePileCount());
        if (scn.AwaitingDSDeployPhaseActions()) {
            scn.DSPass();
        }

        PublicBots bots = PublicBots.forGame(scn);
        String pullParent = scn.GetCardActionId(
                VirtualTableScenario.LS, objective,
                "Deploy starship or location from Reserve Deck");
        assertNotNull("The native objective action must be offered",
                pullParent);
        assertEquals("Both bots must start the native Rogue One route",
                pullParent, bots.decideBoth(scn));
        scn.LSDecided(pullParent);

        AwaitingDecision pullChild = scn.GetAwaitingDecision(
                VirtualTableScenario.LS);
        String rogueOneResponse = offeredResponseForBlueprint(
                pullChild, "206_7");
        assertEquals("Both bots must choose Rogue One from the native pull",
                rogueOneResponse, bots.decideBoth(scn));
        scn.LSDecided(rogueOneResponse);
        scn.PassAllResponses();

        AwaitingDecision pilotDecision = scn.GetAwaitingDecision(
                VirtualTableScenario.LS);
        assertNotNull("Rogue One must expose the real simultaneous-pilot prompt",
                pilotDecision);
        assertTrue("Unexpected pilot prompt: " + pilotDecision.getText(),
                pilotDecision.getText().contains(
                    "simultaneously deploy a pilot"));
        String[] pilotOptions = pilotDecision
                .getDecisionParameters().get("results");
        assertNotNull("Expected Yes/No options for the pilot prompt",
                pilotOptions);
        int yesIndex = Arrays.asList(pilotOptions).indexOf("Yes");
        assertTrue("Expected a Yes option for the pilot prompt: "
                        + Arrays.toString(pilotOptions),
                yesIndex >= 0);
        String bodhiResponse = Integer.toString(yesIndex);
        assertEquals("Both bots must choose Bodhi for the exact route",
                bodhiResponse, bots.decideBoth(scn));
        scn.LSDecided(bodhiResponse);
        scn.PassAllResponses();

        AwaitingDecision destinationDecision = scn.GetAwaitingDecision(
                VirtualTableScenario.LS);
        assertNotNull("The native pair must reach a destination prompt",
                destinationDecision);
        String[] destinationIds = destinationDecision
                .getDecisionParameters().get("cardId");
        assertNotNull("Expected physical system options for prompt: "
                        + destinationDecision.getText(),
                destinationIds);
        String scarifResponse = Integer.toString(scarif.getCardId());
        String otherSystemResponse = Integer.toString(
                scn.GetDSStartingLocation().getCardId());
        List<String> offeredSystems = Arrays.asList(destinationIds);
        assertTrue("Scarif must be a legal offered destination; prompt="
                        + destinationDecision.getText()
                        + "; options=" + offeredSystems,
                offeredSystems.contains(scarifResponse));
        assertTrue("The Dark Side starting system must be a real competing "
                        + "destination; prompt="
                        + destinationDecision.getText()
                        + "; options=" + offeredSystems,
                offeredSystems.contains(otherSystemResponse));

        String publicDestination = bots.decideBoth(scn);
        assertEquals("Both public bots must choose Scarif when another legal "
                        + "system is offered; prompt="
                        + destinationDecision.getText()
                        + "; options=" + offeredSystems
                        + "; decision=" + publicDestination,
                scarifResponse, publicDestination);
        scn.LSDecided(publicDestination);
        scn.PassAllResponses();

        assertEquals("The real native deployment must place Rogue One at Scarif",
                scarif,
                scn.game().getModifiersQuerying().getLocationThatCardIsAt(
                    scn.gameState(), rogueOne));
        assertEquals("Bodhi must be Rogue One's simultaneous pilot",
                rogueOne,
                scn.game().getModifiersQuerying().getIsPilotOf(
                    scn.gameState(), bodhi));
    }
}
