package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Zone;
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
 * Batch Fourteen (2026-07-27): native engine contract for They Have No Idea
 * We're Coming / Until We Win, Or The Chances Are Spent (209_29, LIGHT).
 * Card Java unchanged.
 *
 * Law (Card209_029.java L138-L156): flips when you control two Scarif
 * locations (system or sites). Back (Card209_029_BACK.java L176-L196):
 * flips back when you occupy fewer than two Scarif locations UNLESS a
 * Rogue One is at a Scarif site you occupy.
 */
public class TheyHaveNoIdeaObjectiveEngineContractTest {

    private static final StartingSetup THEY_HAVE_NO_IDEA = new StartingSetup() {
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
            // Four required start deploys (system, Data Vault, Stardust on
            // the vault, Massassi War Room). Side prompts first; any card
            // choice defaults to the designated Data Vault.
            for (int i = 0; i < 10; i++) {
                if (scn.LSDecisionAvailable("On which side")) {
                    scn.LSChoose("Left");
                } else if (scn.LSDecisionAvailable("to deploy")) {
                    scn.LSChooseCard(scn.GetLSCard("dataVault"));
                }
            }
        }
    };

    private VirtualTableScenario thniScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("beach", "209_24");
                    put("landingPad", "209_26");
                    put("rogueOne", "206_7");
                    put("bodhi", "206_1");
                    put("trooper", "1_28");
                    put("corvette", "1_140");
                    put("xwing", "1_146");
                    put("unrelated", "1_17");
                }},
                new HashMap<>() {{
                    put("tie", "1_304");
                }},
                24,
                24,
                THEY_HAVE_NO_IDEA,
                StartingSetup.DefaultDSGroundLocation,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

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
            assertNotNull("Expected Light Side decision", decision);
            String randoResponse = rando.decide(
                    VirtualTableScenario.LS,
                    decision, scn.gameState());
            String chosenResponse = chosen.decide(
                    VirtualTableScenario.LS,
                    decision, scn.gameState());
            assertEquals("Rando/Chosen parity for " + decision.getText(),
                    randoResponse, chosenResponse);
            return randoResponse;
        }
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
                        + "; decision=" + decision.getText()
                        + "; blueprints=" + Arrays.toString(blueprintIds),
                index >= 0);
        return cardIds[index];
    }

    private String selectedBlueprintId(
            AwaitingDecision decision, String response) {
        String[] cardIds = decision.getDecisionParameters().get("cardId");
        String[] blueprintIds = decision.getDecisionParameters()
                .get("blueprintId");
        assertNotNull(cardIds);
        assertNotNull(blueprintIds);
        int index = Arrays.asList(cardIds).indexOf(response);
        assertTrue("Response was not an offered card: " + response,
                index >= 0);
        return blueprintIds[index];
    }

    private void moveLocationToScarif(
            VirtualTableScenario scn, PhysicalCardImpl location) {
        scn.RemoveCardZone(location);
        var placements = scn.gameState().getLocationPlacement(
                scn.game(), location, Title.Scarif, null);
        assertFalse("Expected a legal placement at Scarif",
                placements.isEmpty());
        scn.gameState().addLocationToTable(
                scn.game(), location, placements.getFirst());
    }

    private void assertRemovalRoleBoth(
            String message,
            ObjectiveAnalyzer.FlipGateFormationRole expected,
            VirtualTableScenario scn,
            ObjectiveAnalyzer rando,
            ObjectiveAnalyzer chosen,
            PhysicalCard candidate) {
        var randoRole = rando.classifyGateFormationPieceIfRemoved(
                scn.game(), VirtualTableScenario.LS, candidate);
        var chosenRole = chosen.classifyGateFormationPieceIfRemoved(
                scn.game(), VirtualTableScenario.LS, candidate);
        assertEquals(message + " (Rando)", expected, randoRole);
        assertEquals(message + " (Chosen One parity)",
                randoRole, chosenRole);
    }

    private Object buildPrivateEvaluatorContext(
            Object ai, Class<?> aiType,
            VirtualTableScenario scn) throws Exception {
        var builder = aiType.getDeclaredMethod(
                "buildEvaluatorContext",
                String.class,
                AwaitingDecision.class,
                com.gempukku.swccgo.game.state.GameState.class,
                boolean.class);
        builder.setAccessible(true);
        return builder.invoke(
                ai, VirtualTableScenario.LS,
                scn.GetAwaitingDecision(VirtualTableScenario.LS),
                scn.gameState(), false);
    }

    @Test
    public void thniFrontFlipsOnTwoControlledScarifLocations() {
        var scn = thniScenario();
        var objective = scn.GetLSCard("objective");
        var system = scn.GetLSCard("system");
        var beach = scn.GetLSCard("beach");
        var xwing = scn.GetLSCard("xwing");
        var pulseOne = scn.GetLSFiller(2);
        var pulseTwo = scn.GetLSFiller(3);

        scn.MoveCardsToLSHand(pulseOne, pulseTwo);
        scn.StartGame();
        moveLocationToScarif(scn, beach);
        scn.MoveCardsToLocation(beach, scn.GetLSFiller(1));

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertFalse("One controlled Scarif location must not flip",
                objective.isFlipped());
        scn.DSPass();

        // The system counts toward the LOCATION pool: site + system = two.
        scn.MoveCardsToLocation(system, xwing);
        scn.LSDeployCardAndPassResponses(
                pulseTwo, scn.GetDSStartingLocation());
        assertTrue("Two controlled Scarif locations (site + system) must flip",
                objective.isFlipped());
    }

    @Test
    public void thniFrontRequiresControlNotMereOccupation() {
        var scn = thniScenario();
        var objective = scn.GetLSCard("objective");
        var beach = scn.GetLSCard("beach");
        var landingPad = scn.GetLSCard("landingPad");
        var pulseOne = scn.GetLSFiller(2);

        scn.MoveCardsToLSHand(pulseOne);
        scn.StartGame();
        moveLocationToScarif(scn, beach);
        moveLocationToScarif(scn, landingPad);
        scn.MoveCardsToLocation(beach, scn.GetLSFiller(1));
        // The second location is contested: occupied but not controlled.
        scn.MoveCardsToLocation(landingPad, scn.GetLSFiller(3));
        scn.MoveCardsToLocation(landingPad, scn.GetDSFiller(1));

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertFalse("An occupied-but-contested second location must not flip",
                objective.isFlipped());
    }

    @Test
    public void thniBackRogueOneHoldsBelowTheOccupationFloor() {
        var scn = thniScenario();
        var objective = scn.GetLSCard("objective");
        var system = scn.GetLSCard("system");
        var beach = scn.GetLSCard("beach");
        var rogueOne = scn.GetLSCard("rogueOne");
        var xwing = scn.GetLSCard("xwing");
        var pulseOne = scn.GetLSFiller(2);
        var pulseTwo = scn.GetLSFiller(3);
        var pulseThree = scn.GetLSFiller(4);

        scn.MoveCardsToLSHand(pulseOne, pulseTwo, pulseThree);
        scn.StartGame();
        moveLocationToScarif(scn, beach);
        scn.MoveCardsToLocation(beach, scn.GetLSFiller(1));

        scn.LSActivateForceCheat(16);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.MoveCardsToLocation(system, xwing);
        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertTrue("Two controlled Scarif locations must flip",
                objective.isFlipped());
        scn.DSPass();

        // Drop to ONE occupied Scarif location, but park Rogue One at the
        // occupied beach: the exception must hold the back.
        scn.MoveOutOfPlay(xwing);
        scn.MoveCardsToLocation(beach, rogueOne);
        scn.LSDeployCardAndPassResponses(
                pulseTwo, scn.GetDSStartingLocation());
        assertTrue("Rogue One at an occupied Scarif site must hold the back",
                objective.isFlipped());
        scn.DSPass();

        // Remove Rogue One: occupation floor unmet, no exception, flip back.
        scn.MoveOutOfPlay(rogueOne);
        scn.LSDeployCardAndPassResponses(
                pulseThree, scn.GetDSStartingLocation());
        assertFalse("Without Rogue One the sub-floor occupation must flip back",
                objective.isFlipped());
    }

    @Test
    public void thniProfileRulesTrackTheEngineLaw() {
        var scn = thniScenario();
        var system = scn.GetLSCard("system");
        var beach = scn.GetLSCard("beach");
        var xwing = scn.GetLSCard("xwing");

        scn.StartGame();
        moveLocationToScarif(scn, beach);
        scn.MoveCardsToLocation(beach, scn.GetLSFiller(1));

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertTrue("Profile must hydrate for 209_29", analyzer.isAnalyzed());

        var preFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertEquals("THNI front encodes one rule", 1, preFlip.size());
        assertFalse("One controlled location leaves the rule unmet",
                preFlip.get(0).conditionSatisfied());

        scn.MoveCardsToLocation(system, xwing);
        var complete = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertTrue("Two controlled locations complete the encoded law",
                complete.get(0).conditionSatisfied());

        var postFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "postFlip", "flipBack");
        assertEquals("The back encodes one two-leg hold rule", 1,
                postFlip.size());
    }

    @Test
    public void thniBackCounterfactualProtectsOnlyActualAllOfBlockers() {
        var scn = thniScenario();
        var objective = scn.GetLSCard("objective");
        var system = scn.GetLSCard("system");
        var dataVault = scn.GetLSCard("dataVault");
        var landingPad = scn.GetLSCard("landingPad");
        var rogueOne = scn.GetLSCard("rogueOne");
        var trooper = scn.GetLSCard("trooper");
        var xwing = scn.GetLSCard("xwing");
        var siteHolder = scn.GetLSFiller(1);
        var pulse = scn.GetLSFiller(2);

        scn.MoveCardsToLSHand(pulse);
        scn.StartGame();
        moveLocationToScarif(scn, landingPad);
        scn.MoveCardsToLocation(dataVault, trooper);
        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.MoveCardsToLocation(system, xwing);
        scn.LSDeployCardAndPassResponses(
                pulse, scn.GetDSStartingLocation());
        assertTrue("The counterfactual matrix must begin on the back",
                objective.isFlipped());

        ObjectiveAnalyzer rando =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        ObjectiveAnalyzer chosen =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        chosen.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);

        // With exactly two occupied Scarif locations and no exception,
        // either sole holder blocks the complete flip-back conjunction.
        assertRemovalRoleBoth(
                "The sole Data Vault holder must preserve the two-location floor",
                ObjectiveAnalyzer.FlipGateFormationRole
                    .LAST_FLIP_BACK_BLOCKER,
                scn, rando, chosen, trooper);
        assertRemovalRoleBoth(
                "The sole Scarif system holder must preserve the two-location floor",
                ObjectiveAnalyzer.FlipGateFormationRole
                    .LAST_FLIP_BACK_BLOCKER,
                scn, rando, chosen, xwing);

        // If the remaining occupied site has Rogue One, abandoning the other
        // location is safe because the exception still defeats the allOf.
        scn.MoveOutOfPlay(xwing);
        scn.MoveCardsToLocation(landingPad, rogueOne, siteHolder);
        assertRemovalRoleBoth(
                "The other holder is expendable while the Rogue One exception remains",
                ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                scn, rando, chosen, trooper);

        // At one occupied exterior Scarif site, both halves are causal: lose
        // Rogue One or lose the site's sole presence, and the back flips.
        scn.MoveOutOfPlay(trooper);
        var heldByException = rando.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS,
                "postFlip", "flipBack");
        assertFalse("One occupied site with Rogue One must defeat flip-back",
                heldByException.get(0).conditionSatisfied());
        assertRemovalRoleBoth(
                "Rogue One at the sole occupied Scarif site is a blocker",
                ObjectiveAnalyzer.FlipGateFormationRole
                    .LAST_FLIP_BACK_BLOCKER,
                scn, rando, chosen, rogueOne);
        assertRemovalRoleBoth(
                "The sole holder making Rogue One's Scarif site occupied is a blocker",
                ObjectiveAnalyzer.FlipGateFormationRole
                    .LAST_FLIP_BACK_BLOCKER,
                scn, rando, chosen, siteHolder);

        // Rogue One at the system is not the printed site exception. With
        // only Landing Pad occupied, the full flip-back condition is true.
        scn.MoveCardsToLocation(system, rogueOne);
        var systemDoesNotQualify = rando.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS,
                "postFlip", "flipBack");
        assertTrue("Rogue One at Scarif system must not satisfy the site exception",
                systemDoesNotQualify.get(0).conditionSatisfied());
        assertRemovalRoleBoth(
                "Rogue One at the system is not an exception blocker",
                ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                scn, rando, chosen, rogueOne);
    }

    @Test
    public void publicBotsLandRogueOneAtLandingPadAndKeepTheBack() {
        var scn = thniScenario();
        var objective = scn.GetLSCard("objective");
        var system = scn.GetLSCard("system");
        var dataVault = scn.GetLSCard("dataVault");
        var landingPad = scn.GetLSCard("landingPad");
        var rogueOne = scn.GetLSCard("rogueOne");
        var bodhi = scn.GetLSCard("bodhi");
        var trooper = scn.GetLSCard("trooper");
        var pulse = scn.GetLSFiller(2);

        scn.MoveCardsToLSHand(pulse);
        scn.StartGame();
        moveLocationToScarif(scn, landingPad);
        scn.MoveCardsToLocation(dataVault, trooper);
        scn.MoveCardsToLocation(system, rogueOne);
        scn.BoardAsPilot(rogueOne, bodhi);
        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(
                pulse, scn.GetDSStartingLocation());
        assertTrue("Data Vault plus piloted Rogue One at Scarif must flip",
                objective.isFlipped());
        var landingAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        landingAnalyzer.analyze(
                scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertTrue("The exact analyzer must recognize Landing Pad Nine",
                landingAnalyzer
                    .isTheyHaveNoIdeaRogueOneLandingDestination(
                        scn.game(), VirtualTableScenario.LS,
                        rogueOne, landingPad));

        scn.SkipToPhase(Phase.MOVE);
        if (scn.AwaitingDSMovePhaseActions()) {
            scn.DSPass();
        }
        PublicBots bots = PublicBots.forGame(scn);
        String land = scn.GetCardActionId(
                VirtualTableScenario.LS, rogueOne, "Land");
        assertNotNull("Piloted Rogue One must expose its real Land action",
                land);
        assertEquals("Both public bots must choose Land over Pass",
                land, bots.decideBoth(scn));

        scn.LSDecided(land);
        AwaitingDecision destinationDecision = scn.GetAwaitingDecision(
                VirtualTableScenario.LS);
        assertNotNull("Land must open the engine destination prompt",
                destinationDecision);
        assertTrue("Expected the real landing prompt; got "
                        + destinationDecision.getText(),
                destinationDecision.getText().contains(
                    "Choose where to land"));
        String landingPadResponse = String.valueOf(
                landingPad.getCardId());
        assertTrue("Landing Pad Nine must be an offered destination",
                Arrays.asList(destinationDecision.getDecisionParameters()
                        .get("cardId"))
                    .contains(landingPadResponse));
        assertEquals("Both public bots must choose Landing Pad Nine",
                landingPadResponse, bots.decideBoth(scn));

        scn.LSDecided(landingPadResponse);
        scn.PassAllResponses();
        assertEquals("Rogue One must complete the real landing move",
                landingPad,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsAt(
                        scn.gameState(), rogueOne));
        assertTrue("The Landing Pad exception must keep the objective back",
                objective.isFlipped());
    }

    @Test
    public void publicBotsStageLandingPadBeforeRogueOne() {
        var scn = thniScenario();
        var objective = scn.GetLSCard("objective");
        var beach = scn.GetLSCard("beach");
        var landingPad = scn.GetLSCard("landingPad");
        var rogueOne = scn.GetLSCard("rogueOne");
        var bodhi = scn.GetLSCard("bodhi");
        var trooper = scn.GetLSCard("trooper");
        var corvette = scn.GetLSCard("corvette");

        scn.MoveCardsToLSHand(bodhi, trooper);
        scn.StartGame();
        keepOnlyLightHandCards(scn, bodhi, trooper);
        scn.MoveCardsToBottomOfLSReserveDeck(
                rogueOne, corvette, beach, landingPad);
        scn.LSActivateForceCheat(5);
        scn.SkipToLSTurn(Phase.DEPLOY);
        while (scn.GetLSForcePileCount() > 5) {
            scn.MoveCardsToTopOfLSUsedPile(
                    scn.GetTopOfLSForcePile());
        }
        assertEquals("The route decision has exactly five usable Force",
                5, scn.GetLSForcePileCount());
        if (scn.AwaitingDSDeployPhaseActions()) {
            scn.DSPass();
        }

        AwaitingDecision parentDecision = scn.GetAwaitingDecision(
                VirtualTableScenario.LS);
        assertNotNull(parentDecision);
        String parentAction = scn.GetCardActionId(
                VirtualTableScenario.LS, objective,
                "Deploy starship or location from Reserve Deck");
        assertNotNull("The exact native parent must be offered",
                parentAction);
        String publicParent = PublicBots.forGame(scn).decideBoth(scn);

        // Drive the exact card action regardless of the current bot winner so
        // the same RED test independently diagnoses its Reserve child.
        scn.LSDecided(parentAction);
        AwaitingDecision childDecision = scn.GetAwaitingDecision(
                VirtualTableScenario.LS);
        assertNotNull(childDecision);
        offeredResponseForBlueprint(childDecision, "206_7");
        offeredResponseForBlueprint(childDecision, "1_140");
        offeredResponseForBlueprint(childDecision, "209_24");
        String landingPadResponse = offeredResponseForBlueprint(
                childDecision, "209_26");
        String publicChild = PublicBots.forGame(scn).decideBoth(scn);

        assertEquals("Both public bots must choose the exact 209_29 parent; "
                        + "decision=" + parentDecision.getText()
                        + "; parameters="
                        + parentDecision.getDecisionParameters(),
                parentAction, publicParent);
        assertEquals("Both public bots must stage Landing Pad before "
                        + "spending the only front-side pull on Rogue One; "
                        + "decision="
                        + childDecision.getText()
                        + "; selectedResponse=" + publicChild
                        + "; offeredBlueprints="
                        + Arrays.toString(childDecision
                            .getDecisionParameters().get("blueprintId")),
                landingPadResponse, publicChild);
    }

    @Test
    public void publicBotsChooseRogueOneAfterLandingPadStage() {
        var scn = thniScenario();
        var objective = scn.GetLSCard("objective");
        var dataVault = scn.GetLSCard("dataVault");
        var beach = scn.GetLSCard("beach");
        var landingPad = scn.GetLSCard("landingPad");
        var rogueOne = scn.GetLSCard("rogueOne");
        var bodhi = scn.GetLSCard("bodhi");
        var trooper = scn.GetLSCard("trooper");
        var corvette = scn.GetLSCard("corvette");

        scn.MoveCardsToLSHand(bodhi);
        scn.StartGame();
        keepOnlyLightHandCards(scn, bodhi);
        moveLocationToScarif(scn, landingPad);
        scn.MoveCardsToLocation(dataVault, trooper);
        scn.MoveCardsToBottomOfLSReserveDeck(
                rogueOne, corvette, beach);
        scn.LSActivateForceCheat(4);
        scn.SkipToLSTurn(Phase.DEPLOY);
        while (scn.GetLSForcePileCount() > 4) {
            scn.MoveCardsToTopOfLSUsedPile(
                    scn.GetTopOfLSForcePile());
        }
        assertEquals("The remaining Rogue One plus Bodhi route costs four",
                4, scn.GetLSForcePileCount());
        if (scn.AwaitingDSDeployPhaseActions()) {
            scn.DSPass();
        }

        AwaitingDecision parentDecision = scn.GetAwaitingDecision(
                VirtualTableScenario.LS);
        assertNotNull(parentDecision);
        String parentAction = scn.GetCardActionId(
                VirtualTableScenario.LS, objective,
                "Deploy starship or location from Reserve Deck");
        assertNotNull("The exact native parent must be offered",
                parentAction);
        String publicParent = PublicBots.forGame(scn).decideBoth(scn);

        scn.LSDecided(parentAction);
        AwaitingDecision childDecision = scn.GetAwaitingDecision(
                VirtualTableScenario.LS);
        assertNotNull(childDecision);
        offeredResponseForBlueprint(childDecision, "206_7");
        offeredResponseForBlueprint(childDecision, "1_140");
        offeredResponseForBlueprint(childDecision, "209_24");
        String publicChild = PublicBots.forGame(scn).decideBoth(scn);

        assertEquals("Both public bots must choose the exact 209_29 parent",
                parentAction, publicParent);
        assertEquals("Both public bots must choose Rogue One after Landing "
                        + "Pad and the Data Vault holder are established; "
                        + "offeredBlueprints="
                        + Arrays.toString(childDecision
                            .getDecisionParameters().get("blueprintId")),
                "206_7", selectedBlueprintId(
                        childDecision, publicChild));
    }

    @Test
    public void publicBotsRejectPaidDeployThatUnderfundsFiveForceRoute()
            throws Exception {
        var scn = thniScenario();
        var objective = scn.GetLSCard("objective");
        var landingPad = scn.GetLSCard("landingPad");
        var rogueOne = scn.GetLSCard("rogueOne");
        var bodhi = scn.GetLSCard("bodhi");
        var trooper = scn.GetLSCard("trooper");
        var unrelated = scn.GetLSCard("unrelated");

        scn.MoveCardsToLSHand(bodhi, trooper, unrelated);
        scn.StartGame();
        keepOnlyLightHandCards(scn, bodhi, trooper, unrelated);
        moveLocationToScarif(scn, landingPad);
        scn.MoveCardsToBottomOfLSReserveDeck(rogueOne);
        scn.LSActivateForceCheat(5);
        scn.SkipToLSTurn(Phase.DEPLOY);
        while (scn.GetLSForcePileCount() > 5) {
            scn.MoveCardsToTopOfLSUsedPile(
                    scn.GetTopOfLSForcePile());
        }
        assertEquals("The reserve boundary begins at exactly five Force",
                5, scn.GetLSForcePileCount());
        if (scn.AwaitingDSDeployPhaseActions()) {
            scn.DSPass();
        }

        String pullParent = scn.GetCardActionId(
                VirtualTableScenario.LS, objective,
                "Deploy starship or location from Reserve Deck");
        String unrelatedDeploy = scn.GetCardActionId(
                VirtualTableScenario.LS, unrelated, "Deploy");
        assertNotNull("The funded native route must be offered", pullParent);
        assertNotNull("The unrelated three-Force deploy must be offered",
                unrelatedDeploy);

        var randoAi = new com.gempukku.swccgo.ai.models.rando.RandoCalAi();
        randoAi.setGame(scn.game());
        String randoChoice = randoAi.decide(
                VirtualTableScenario.LS,
                scn.GetAwaitingDecision(VirtualTableScenario.LS),
                scn.gameState());
        var randoContext =
                (com.gempukku.swccgo.ai.models.rando.evaluators
                    .DecisionContext) buildPrivateEvaluatorContext(
                        randoAi,
                        com.gempukku.swccgo.ai.models.rando.RandoCalAi.class,
                        scn);
        var randoUnrelated =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                    .DeployEvaluator().evaluate(randoContext).stream()
                    .filter(action -> unrelatedDeploy.equals(
                        action.getActionId()))
                    .findFirst().orElseThrow();

        var chosenAi = new com.gempukku.swccgo.ai.models.chosenone
                .TheChosenOneAi();
        chosenAi.setGame(scn.game());
        String chosenChoice = chosenAi.decide(
                VirtualTableScenario.LS,
                scn.GetAwaitingDecision(VirtualTableScenario.LS),
                scn.gameState());
        var chosenContext =
                (com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .DecisionContext) buildPrivateEvaluatorContext(
                        chosenAi,
                        com.gempukku.swccgo.ai.models.chosenone
                            .TheChosenOneAi.class,
                        scn);
        var chosenUnrelated =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .DeployEvaluator().evaluate(chosenContext).stream()
                    .filter(action -> unrelatedDeploy.equals(
                        action.getActionId()))
                    .findFirst().orElseThrow();

        assertEquals("The analyzer must preserve all five future payments",
                5, randoContext.getObjectiveAnalyzer()
                    .getTheyHaveNoIdeaFutureRouteForceReserve(
                        scn.game(), VirtualTableScenario.LS, unrelated));
        assertEquals("Chosen One must calculate the same exact reserve",
                5, chosenContext.getObjectiveAnalyzer()
                    .getTheyHaveNoIdeaFutureRouteForceReserve(
                        scn.game(), VirtualTableScenario.LS, unrelated));
        assertTrue("Rando must hard-veto the underfunding deploy; reasoning="
                        + randoUnrelated.getReasoningString(),
                randoUnrelated.isHardVetoed());
        assertEquals(
                "OBJECTIVE.THNI.ROUTE_FORCE_RESERVE: preserve the remaining "
                    + "Rogue One, pilot, and Data Vault payments",
                randoUnrelated.getVetoReason());
        assertEquals("Chosen One must mirror the exact reserve veto",
                randoUnrelated.getVetoReason(),
                chosenUnrelated.getVetoReason());
        assertEquals("Rando must keep the funded native route ahead",
                pullParent, randoChoice);
        assertEquals("Chosen One must make the same public decision",
                randoChoice, chosenChoice);
        assertEquals("Rejecting the distraction must spend no Force",
                5, scn.GetLSForcePileCount());
    }

    @Test
    public void publicBotsPreserveSoleRouteCopiesDuringRealForceLoss() {
        var scn = thniScenario();
        var landingPad = scn.GetLSCard("landingPad");
        var rogueOne = scn.GetLSCard("rogueOne");
        var bodhi = scn.GetLSCard("bodhi");
        var trooper = scn.GetLSCard("trooper");
        var fodder = scn.GetLSCard("unrelated");

        scn.MoveCardsToLSHand(
                landingPad, rogueOne, bodhi, trooper, fodder);
        scn.StartGame();
        scn.MoveCardsToLocation(
                scn.GetDSStartingLocation(), scn.GetDSFiller(1));
        scn.DSActivateForceCheat(4);
        scn.SkipToDSTurn(Phase.CONTROL);
        scn.LSActivateForceCheat(8);
        scn.MoveCardsToLSHand(
                landingPad, rogueOne, bodhi, trooper, fodder);
        keepOnlyLightHandCards(
                scn, landingPad, rogueOne, bodhi, trooper, fodder);

        var randoAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        var chosenAnalyzer =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        randoAnalyzer.analyze(
                scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        chosenAnalyzer.analyze(
                scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        for (PhysicalCard routeCard : List.of(
                landingPad, rogueOne, bodhi, trooper)) {
            assertTrue("Rando must protect the sole needed "
                            + routeCard.getTitle(),
                    randoAnalyzer
                        .isPreferredTheyHaveNoIdeaRouteForceLossCandidate(
                            scn.game(), VirtualTableScenario.LS,
                            routeCard));
            assertTrue("Chosen One must protect the same "
                            + routeCard.getTitle(),
                    chosenAnalyzer
                        .isPreferredTheyHaveNoIdeaRouteForceLossCandidate(
                            scn.game(), VirtualTableScenario.LS,
                            routeCard));
        }
        assertFalse("The unrelated card must remain legal loss fodder",
                randoAnalyzer
                    .isPreferredTheyHaveNoIdeaRouteForceLossCandidate(
                        scn.game(), VirtualTableScenario.LS, fodder));

        scn.DSForceDrainAt(scn.GetDSStartingLocation());
        scn.PassAllResponses();
        assertTrue("The real drain must open a Light Side Force-loss prompt",
                scn.LSDecisionAvailable("Choose Force to lose"));
        AwaitingDecision forceLoss = scn.GetAwaitingDecision(
                VirtualTableScenario.LS);
        List<String> offered = Arrays.asList(
                forceLoss.getDecisionParameters().get("cardId"));
        List<String> protectedIds = List.of(
                Integer.toString(landingPad.getCardId()),
                Integer.toString(rogueOne.getCardId()),
                Integer.toString(bodhi.getCardId()),
                Integer.toString(trooper.getCardId()));
        for (String protectedId : protectedIds) {
            assertTrue("Every sole route copy must be offered in the real "
                            + "loss decision; offered=" + offered,
                    offered.contains(protectedId));
        }

        String response = PublicBots.forGame(scn).decideBoth(scn);
        assertTrue("The public response must be an offered legal loss; got "
                        + response + " from " + offered,
                offered.contains(response));
        assertFalse("Both public bots must lose fodder instead of a sole "
                        + "THNI route copy; response=" + response,
                protectedIds.contains(response));
        scn.LSDecided(response);
        scn.PassAllResponses();

        for (PhysicalCard routeCard : List.of(
                landingPad, rogueOne, bodhi, trooper)) {
            assertEquals("The preserved route copy must remain in hand: "
                            + routeCard.getTitle(),
                    Zone.HAND, routeCard.getZone());
        }
    }

    @Test
    public void publicBotsCompleteExactFiveForceFrontChain() {
        var scn = thniScenario();
        var objective = scn.GetLSCard("objective");
        var system = scn.GetLSCard("system");
        var dataVault = scn.GetLSCard("dataVault");
        var landingPad = scn.GetLSCard("landingPad");
        var rogueOne = scn.GetLSCard("rogueOne");
        var bodhi = scn.GetLSCard("bodhi");
        var trooper = scn.GetLSCard("trooper");

        scn.MoveCardsToLSHand(bodhi, trooper);
        scn.StartGame();
        keepOnlyLightHandCards(scn, bodhi, trooper);
        moveLocationToScarif(scn, landingPad);
        scn.MoveCardsToBottomOfLSReserveDeck(rogueOne);
        scn.LSActivateForceCheat(5);
        scn.SkipToLSTurn(Phase.DEPLOY);
        while (scn.GetLSForcePileCount() > 5) {
            scn.MoveCardsToTopOfLSUsedPile(
                    scn.GetTopOfLSForcePile());
        }
        assertEquals("The complete route starts with exactly five Force",
                5, scn.GetLSForcePileCount());
        if (scn.AwaitingDSDeployPhaseActions()) {
            scn.DSPass();
        }

        var routeAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        routeAnalyzer.analyze(
                scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertEquals("The analyzer must reserve the exact funded route",
                5, routeAnalyzer.getTheyHaveNoIdeaFutureRouteForceReserve(
                    scn.game(), VirtualTableScenario.LS, null));
        assertEquals("A Trooper deployment may spend its own one Force",
                4, routeAnalyzer.getTheyHaveNoIdeaFutureRouteForceReserve(
                    scn.game(), VirtualTableScenario.LS, trooper));
        assertFalse("Bodhi belongs in Rogue One's simultaneous deployment, "
                        + "not as an unrelated pre-ship deploy",
                routeAnalyzer.isTheyHaveNoIdeaRouteDeployCandidate(
                    scn.game(), VirtualTableScenario.LS, bodhi));

        PublicBots bots = PublicBots.forGame(scn);
        String pullParent = scn.GetCardActionId(
                VirtualTableScenario.LS, objective,
                "Deploy starship or location from Reserve Deck");
        assertNotNull("The native objective pull parent must be offered",
                pullParent);
        assertEquals("Both public bots must begin the funded route",
                pullParent, bots.decideBoth(scn));
        scn.LSDecided(pullParent);

        AwaitingDecision pullChild = scn.GetAwaitingDecision(
                VirtualTableScenario.LS);
        String rogueOneResponse = offeredResponseForBlueprint(
                pullChild, "206_7");
        assertEquals("Both public bots must pull Rogue One",
                rogueOneResponse, bots.decideBoth(scn));
        scn.LSDecided(rogueOneResponse);

        scn.PassAllResponses();

        AwaitingDecision simultaneousPilot = scn.GetAwaitingDecision(
                VirtualTableScenario.LS);
        assertNotNull("The engine must expose Rogue One's pilot choice",
                simultaneousPilot);
        assertTrue("Expected Rogue One's simultaneous-pilot prompt; got "
                        + simultaneousPilot.getText(),
                simultaneousPilot.getText().contains(
                    "simultaneously deploy a pilot"));
        String simultaneousPilotResponse = bots.decideBoth(scn);
        scn.LSDecided(simultaneousPilotResponse);
        scn.PassAllResponses();

        AwaitingDecision shipDestination = scn.GetAwaitingDecision(
                VirtualTableScenario.LS);
        assertNotNull("Both bots must accept the simultaneous pilot route; "
                        + "response=" + simultaneousPilotResponse
                        + "; current="
                        + (scn.GetCurrentDecision() != null
                            ? scn.GetCurrentDecision().getText() : "none"),
                shipDestination);
        String systemResponse = String.valueOf(system.getCardId());
        String[] shipDestinationIds = shipDestination
                .getDecisionParameters().get("cardId");
        assertNotNull("Expected card destinations for "
                        + shipDestination.getText() + "; parameters="
                        + shipDestination.getDecisionParameters(),
                shipDestinationIds);
        assertTrue("Scarif system must be an offered Rogue One destination",
                Arrays.asList(shipDestinationIds).contains(systemResponse));
        assertEquals("Both public bots must deploy Rogue One to Scarif",
                systemResponse, bots.decideBoth(scn));
        scn.LSDecided(systemResponse);
        scn.PassAllResponses();

        assertEquals("Rogue One must reach Scarif system through the native pull",
                system, scn.game().getModifiersQuerying()
                    .getLocationThatCardIsAt(scn.gameState(), rogueOne));
        assertEquals("Both bots must simultaneously deploy Bodhi as pilot",
                rogueOne, scn.game().getModifiersQuerying()
                    .getIsPilotOf(scn.gameState(), bodhi));
        assertEquals("Rogue One plus discounted spy Bodhi must cost four",
                1, scn.GetLSForcePileCount());
        assertFalse("The Data Vault still needs its controller",
                objective.isFlipped());
        if (scn.AwaitingDSDeployPhaseActions()) {
            scn.DSPass();
        }

        String trooperDeploy = scn.GetCardActionId(
                VirtualTableScenario.LS, trooper, "Deploy");
        assertNotNull("The Rebel Trooper must have a native deploy action",
                trooperDeploy);
        assertEquals("Both public bots must spend the final Force on the Trooper",
                trooperDeploy, bots.decideBoth(scn));
        scn.LSDecided(trooperDeploy);

        AwaitingDecision trooperDestination = scn.GetAwaitingDecision(
                VirtualTableScenario.LS);
        String vaultResponse = String.valueOf(dataVault.getCardId());
        assertTrue("Data Vault must be an offered Trooper destination",
                Arrays.asList(trooperDestination.getDecisionParameters()
                        .get("cardId")).contains(vaultResponse));
        assertEquals("Both public bots must deploy the Trooper to Data Vault",
                vaultResponse, bots.decideBoth(scn));
        scn.LSDecided(vaultResponse);
        scn.PassAllResponses();

        assertEquals("The Trooper must complete the Data Vault control leg",
                dataVault, scn.game().getModifiersQuerying()
                    .getLocationThatCardIsAt(scn.gameState(), trooper));
        assertEquals("The exact five-Force route must be fully paid",
                0, scn.GetLSForcePileCount());
        assertTrue("The engine must natively flip on two controlled Scarif locations",
                objective.isFlipped());
    }
}
