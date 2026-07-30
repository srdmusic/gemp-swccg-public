package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.RetrieveForceEffect;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import static com.gempukku.swccgo.framework.Assertions.assertAtLocation;
import static com.gempukku.swccgo.framework.Assertions.assertInZone;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Native engine proof for both printings of The First Order Reigns.
 *
 * <p>The objective does not flip merely because Tracked Fleet is on table. Dark Side must deploy
 * Supremacy, move it to the Fleet's current system for free, control that system at the start of
 * the next Dark Side turn, and let Tracked Fleet annihilate itself. That historical blow-away
 * state is the objective's actual flip trigger.
 */
public class FirstOrderReignsObjectiveEngineContractTest {
    private static final String DOWNLOAD_ACTION =
            "Deploy Supremacy card or battleground";
    private static final String PLAYTESTING_FORMAT = "playtesting";

    private static final List<Printing> PRINTINGS = List.of(
            new Printing("225_32", "225_34"),
            new Printing("501_60", "501_61"));

    private static final class Printing {
        private final String objectiveBlueprintId;
        private final String fleetBlueprintId;

        private Printing(String objectiveBlueprintId, String fleetBlueprintId) {
            this.objectiveBlueprintId = objectiveBlueprintId;
            this.fleetBlueprintId = fleetBlueprintId;
        }
    }

    private static StartingSetup objectiveSetup(Printing printing) {
        return new StartingSetup() {
            @Override
            public HashMap<String, String> Cards() {
                HashMap<String, String> cards = new HashMap<>();
                cards.put("objective", printing.objectiveBlueprintId);
                cards.put("dqar", "211_19");
                cards.put("crait", "225_15");
                cards.put("salt", "225_17");
                cards.put("fleet", printing.fleetBlueprintId);
                return cards;
            }

            @Override
            public void Setup(VirtualTableScenario scn) {
                // Every required starting card has one matching candidate.
            }
        };
    }

    private static VirtualTableScenario scenario(Printing printing) {
        HashMap<String, String> lightCards = new HashMap<>();
        lightCards.put("stackA", "1_28");
        lightCards.put("stackB", "1_28");
        lightCards.put("stackC", "1_28");
        lightCards.put("stackD", "1_28");
        lightCards.put("xwing1", "1_146");
        lightCards.put("xwing2", "1_146");
        lightCards.put("xwing3", "1_146");
        lightCards.put("xwing4", "1_146");
        lightCards.put("ahchTo", "211_48");
        lightCards.put("han", "1_11");
        lightCards.put("leia", "1_17");
        lightCards.put("luke", "108_3");
        lightCards.put("mace", "201_2");

        HashMap<String, String> darkCards = new HashMap<>();
        darkCards.put("kijimi", "214_6");
        darkCards.put("supremacy", "225_27");
        darkCards.put("navy", "225_24");
        darkCards.put("fulminatrix", "225_20");
        darkCards.put("hux", "204_41");
        darkCards.put("commandShuttle", "204_55");
        darkCards.put("dantooine", "1_282");
        darkCards.put("kylo", "209_37");
        darkCards.put("firstOrderTrooperA", "204_40");
        darkCards.put("firstOrderTrooperB", "204_40");
        darkCards.put("firstOrderTrooperC", "204_40");
        darkCards.put("uniqueTrooper", "204_38");
        darkCards.put("firstOrderVehicle", "225_19");
        darkCards.put("imperialVehicle", "3_157");

        return new VirtualTableScenario(
                lightCards,
                darkCards,
                32,
                32,
                StartingSetup.DefaultLSGroundLocation,
                objectiveSetup(printing),
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                PLAYTESTING_FORMAT);
    }

    private static void flipToTheResistanceIsDoomed(
            VirtualTableScenario scn, com.gempukku.swccgo.game.PhysicalCard objective) {
        String expectedBackBlueprintId = objective.getBlueprintId(true) + "_BACK";
        scn.gameState().flipCard(scn.game(), objective, true);
        assertTrue(objective.isFlipped());
        assertEquals(expectedBackBlueprintId, objective.getBlueprintId(false));
        assertEquals("The Resistance Is Doomed", objective.getBlueprint().getTitle());
    }

    private static float forceDrainAmount(
            VirtualTableScenario scn,
            com.gempukku.swccgo.game.PhysicalCard location,
            String playerId) {
        return scn.game().getModifiersQuerying().getForceDrainAmount(
                scn.gameState(), location, playerId);
    }

    private static boolean forceDrainProhibited(
            VirtualTableScenario scn,
            com.gempukku.swccgo.game.PhysicalCard location,
            String playerId) {
        return scn.game().getModifiersQuerying().isProhibitedFromForceDrainingAtLocation(
                scn.gameState(), location, playerId);
    }

    private static void retrieveOneForce(
            VirtualTableScenario scn,
            com.gempukku.swccgo.game.PhysicalCard source,
            String playerId) {
        TopLevelGameTextAction action = new TopLevelGameTextAction(
                source, playerId, source.getCardId());
        action.setText("Retrieve 1 Force");
        action.appendEffect(new RetrieveForceEffect(action, playerId, 1));
        scn.carryOutEffectInPhaseActionByPlayer(playerId, action);
        scn.PassAllResponses();
    }

    private static boolean isDisplayedPileCardSelectable(
            VirtualTableScenario scn,
            com.gempukku.swccgo.game.PhysicalCard card) {
        String[] blueprintIds =
                scn.GetADParam(VirtualTableScenario.DS, "blueprintId");
        String[] selectable =
                scn.GetADParam(VirtualTableScenario.DS, "selectable");
        for (int index = 0; index < blueprintIds.length; index++) {
            if (card.getBlueprintId(true).equals(blueprintIds[index])) {
                return Boolean.parseBoolean(selectable[index]);
            }
        }
        return false;
    }

    private static void assertCompleteSaltForfeitTrigger(
            Printing printing, String matchingCharacterKey) {
        var scn = scenario(printing);
        var objective = scn.GetDSCard("objective");
        var salt = scn.GetDSCard("salt");
        var kylo = scn.GetDSCard("kylo");
        var matchingCharacter = scn.GetLSCard(matchingCharacterKey);
        var nonMatchingBattleSupport = scn.GetLSCard("mace");

        scn.StartGame();
        flipToTheResistanceIsDoomed(scn, objective);
        scn.MoveCardsToLocation(salt, kylo);
        scn.MoveCardsToLocation(
                salt, matchingCharacter, nonMatchingBattleSupport);

        assertAtLocation(salt, matchingCharacter);
        for (String otherCharacterKey : List.of("han", "leia", "luke")) {
            if (!otherCharacterKey.equals(matchingCharacterKey)) {
                assertFalse("Only " + matchingCharacterKey
                                + " may satisfy the Han/Leia/Luke branch",
                        Filters.at(salt).accepts(
                                scn.game(), scn.GetLSCard(otherCharacterKey)));
            }
        }
        assertFalse("Battle support must not satisfy the Han/Leia/Luke branch",
                Filters.or(Filters.Han, Filters.Leia, Filters.Luke)
                        .accepts(scn.game(), nonMatchingBattleSupport));

        scn.SkipToLSTurn(Phase.BATTLE);
        scn.LSInitiateBattle(salt);
        scn.SkipToDamageSegment(false);

        assertTrue(printing.objectiveBlueprintId + "_BACK must see Dark Side lose"
                        + " the physical Salt Plateau battle with only "
                        + matchingCharacterKey + " satisfying its persona branch",
                scn.LSWonBattle());
        assertTrue(scn.AwaitingDSBattleDamagePayment());
        scn.DSPayBattleDamageFromCardInPlay(kylo);

        assertInZone(Zone.LOST_PILE, kylo);
        assertInZone(Zone.OUT_OF_PLAY, objective);
    }

    @Test
    public void navyMakesARealCrewedFirstOrderChaseShipReachDqarFromCrait() {
        for (Printing printing : PRINTINGS) {
            var scn = scenario(printing);
            var crait = scn.GetDSCard("crait");
            var dqar = scn.GetDSCard("dqar");
            var navy = scn.GetDSCard("navy");
            var fulminatrix = scn.GetDSCard("fulminatrix");
            var supremacy = scn.GetDSCard("supremacy");
            var cheapCrew = scn.GetDSCard("firstOrderTrooperA");

            scn.StartGame();
            scn.MoveCardsToLocation(
                    crait, fulminatrix, supremacy);

            int chaseDistance = Math.abs(crait.getParsec() - dqar.getParsec());
            assertEquals("Crait must retain its real printed parsec", 8, crait.getParsec());
            assertEquals("D'Qar must retain its real printed parsec", 5, dqar.getParsec());
            assertEquals("The objective's real chase leg spans three parsecs",
                    3, chaseDistance);
            assertTrue("Fulminatrix must be a real First Order starship",
                    Filters.First_Order_starship.accepts(scn.game(), fulminatrix));
            assertEquals("Fulminatrix begins with its printed hyperspeed",
                    3, scn.GetHyperspeed(fulminatrix));
            assertEquals("Supremacy begins one parsec short of the real chase leg",
                    2, scn.GetHyperspeed(supremacy));

            scn.MoveCardsToDSSideOfTable(navy);

            assertInZone(Zone.SIDE_OF_TABLE, navy);
            assertEquals("Navy Of The First Order must add one real hyperspeed",
                    4, scn.GetHyperspeed(fulminatrix));
            assertEquals("Navy must make Supremacy reach the three-parsec chase leg",
                    3, scn.GetHyperspeed(supremacy));
            assertTrue("The Navy-modified ship must cover Crait to D'Qar",
                    scn.GetHyperspeed(fulminatrix) >= chaseDistance);
            assertTrue("The Navy-modified Supremacy must also cover Crait to D'Qar",
                    scn.GetHyperspeed(supremacy) >= chaseDistance);

            scn.MoveCardsToDSHand(cheapCrew);
            scn.DSActivateForceCheat(4);
            scn.SkipToPhase(Phase.DEPLOY);

            assertTrue("First Order Stormtrooper must be a legal cheap crew deploy",
                    scn.DSDeployAvailable(cheapCrew));
            scn.DSDeployCard(cheapCrew);
            assertTrue("Fulminatrix must be a legal destination for the cheap crew",
                    scn.DSHasCardChoiceAvailable(fulminatrix));
            scn.DSChooseCard(fulminatrix);
            scn.PassAllResponses();

            assertTrue("The real First Order character must deploy aboard as a passenger",
                    scn.IsAboardAsPassenger(fulminatrix, cheapCrew));

            scn.SkipToPhase(Phase.MOVE);
            assertTrue("The crewed Fulminatrix must receive a legal hyperspace move",
                    scn.DSMoveAvailable(fulminatrix));
            scn.DSMoveCard(fulminatrix, dqar);
            scn.PassAllResponses();

            assertAtLocation(dqar, fulminatrix);
            assertTrue("The cheap crew must remain aboard throughout the chase",
                    scn.IsAboardAsPassenger(fulminatrix, cheapCrew));
        }
    }

    @Test
    public void navyUploadsTheAffordableReplayShipAndPilotPackageToCrait() {
        for (Printing printing : PRINTINGS) {
            var scn = scenario(printing);
            var dqar = scn.GetDSCard("dqar");
            var crait = scn.GetDSCard("crait");
            var fleet = scn.GetDSCard("fleet");
            var navy = scn.GetDSCard("navy");
            var hux = scn.GetDSCard("hux");
            var commandShuttle = scn.GetDSCard("commandShuttle");

            scn.StartGame();

            assertInZone(Zone.ATTACHED, fleet);
            assertTrue("Tracked Fleet must physically attach to D'Qar",
                    scn.IsAttachedTo(dqar, fleet));
            scn.MoveCardsToDSSideOfTable(navy);
            scn.MoveCardsToDSHand(hux);
            scn.MoveCardsToBottomOfDSReserveDeck(commandShuttle);

            while (scn.GetDSForcePileCount() > 0) {
                scn.MoveCardsToHand(scn.GetTopOfDSForcePile());
            }
            scn.DSActivateForceCheat(6);
            scn.SkipToPhase(Phase.DEPLOY);

            for (ObjectiveAnalyzer analyzer : List.of(
                    new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer(),
                    new com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer())) {
                analyzer.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
                assertEquals("Analyzer must resolve the physical attachment host",
                        dqar, analyzer.getFirstOrderReignsTrackedFleetHostSystem(
                                scn.game(), VirtualTableScenario.DS));
                assertTrue(analyzer.isFirstOrderReignsRouteOpen(
                        scn.game(), VirtualTableScenario.DS));
                assertTrue(analyzer.isFirstOrderReignsNavyRouteAction(
                        scn.game(), VirtualTableScenario.DS, navy,
                        "Reveal starship or pilot from hand"));
                String destinationPrompt =
                        "Choose where to deploy "
                                + "<div class='cardHint' value='"
                                + commandShuttle.getBlueprintId(true)
                                + "'>" + commandShuttle.getTitle()
                                + "</div> and "
                                + "<div class='cardHint' value='"
                                + hux.getBlueprintId(true)
                                + "'>" + hux.getTitle()
                                + "</div> simultaneously";
                assertTrue(analyzer
                        .isFirstOrderReignsNavyRouteDestinationCandidate(
                                scn.game(), VirtualTableScenario.DS,
                                navy, destinationPrompt, crait));
                assertFalse(analyzer
                        .isFirstOrderReignsNavyRouteDestinationCandidate(
                                scn.game(), VirtualTableScenario.DS,
                                navy, destinationPrompt, dqar));
                assertEquals("The route must preserve the engine's six-Force legal-selection boundary",
                        6, analyzer.getFirstOrderReignsRouteForceReserve(
                                scn.game(), VirtualTableScenario.DS, null));
            }

            assertEquals(6.0f,
                    scn.game().getModifiersQuerying().getSimultaneousDeployCost(
                            scn.gameState(), navy,
                            commandShuttle, false, 0,
                            hux, false, 0,
                            crait, null, false),
                    0.0f);
            assertTrue(scn.DSCardActionAvailable(
                    navy, "Reveal starship or pilot from hand"));
            scn.DSUseCardAction(
                    navy, "Reveal starship or pilot from hand");
            assertTrue(scn.DSDecisionAvailable(
                    "Choose card from hand, or click 'Done' to cancel"));
            assertTrue(scn.DSHasCardChoiceAvailable(hux));
            scn.DSChooseCard(hux);

            assertTrue(scn.DSDecisionAvailable(
                    "Choose card to deploy from Reserve Deck simultaneously with"));
            assertTrue(scn.DSHasCardChoiceAvailable(commandShuttle));
            scn.DSChooseCard(commandShuttle);
            scn.PassAllResponses();

            assertTrue(scn.DSDecisionAvailable("Choose where to deploy")
                    || scn.DSDecisionAvailable("Choose location where to deploy"));
            assertTrue(scn.DSHasCardChoiceAvailable(crait));
            assertFalse("Tracked Fleet prohibits deployment directly to its host",
                    scn.DSHasCardChoiceAvailable(dqar));
            scn.DSChooseCard(crait);
            if (scn.DSDecisionAvailable("Choose capacity slot")) {
                scn.DSChoose("Pilot");
            }
            scn.PassAllResponses();

            assertAtLocation(crait, commandShuttle);
            assertTrue(scn.IsAboardAsPilot(commandShuttle, hux));
        }
    }

    @Test
    public void bothPrintingsCompleteTheNativeControlBlowAwayAndFlipChain() {
        for (Printing printing : PRINTINGS) {
            var scn = scenario(printing);
            var objective = scn.GetDSCard("objective");
            var dqar = scn.GetDSCard("dqar");
            var crait = scn.GetDSCard("crait");
            var salt = scn.GetDSCard("salt");
            var fleet = scn.GetDSCard("fleet");
            var kijimi = scn.GetDSCard("kijimi");
            var supremacy = scn.GetDSCard("supremacy");

            scn.StartGame();

            assertInZone(Zone.SIDE_OF_TABLE, objective);
            assertInZone(Zone.LOCATIONS, dqar, crait, salt);
            assertTrue(printing.objectiveBlueprintId
                            + " must attach its matching Tracked Fleet to D'Qar during setup",
                    scn.IsAttachedTo(dqar, fleet));
            assertFalse(objective.isFlipped());

            // Keep both objective targets below natural activation.
            scn.MoveCardsToBottomOfDSReserveDeck(kijimi, supremacy);
            scn.SkipToPhase(Phase.DEPLOY);

            assertTrue(scn.DSCardActionAvailable(objective, DOWNLOAD_ACTION));
            scn.DSUseCardAction(objective, DOWNLOAD_ACTION);
            assertTrue("Kijimi is the Episode VII battleground staging pull",
                    scn.DSHasCardChoiceAvailable(kijimi));
            scn.DSChooseCard(kijimi);
            scn.PassAllResponses();
            if (scn.DSDecisionAvailable("On which side")) {
                scn.DSChoose("Left");
                scn.PassAllResponses();
            }
            assertInZone(Zone.LOCATIONS, kijimi);
            assertFalse(objective.isFlipped());

            // The Kijimi search reshuffled Reserve Deck, so pin Supremacy below next turn's
            // natural activation before advancing.
            scn.MoveCardsToBottomOfDSReserveDeck(supremacy);
            scn.SkipToDSTurn(Phase.DEPLOY);
            assertTrue(scn.IsAttachedTo(dqar, fleet));
            assertFalse(GameConditions.isBlownAway(
                    scn.game(), Filters.Tracked_Fleet));

            while (scn.GetDSForcePileCount() > 0) {
                scn.MoveCardsToHand(scn.GetTopOfDSForcePile());
            }
            scn.DSActivateForceCheat(7);
            assertEquals("Supremacy must begin with exactly its objective-reset deploy cost",
                    7, scn.GetDSForcePileCount());

            assertTrue(scn.DSCardActionAvailable(objective, DOWNLOAD_ACTION));
            scn.DSUseCardAction(objective, DOWNLOAD_ACTION);
            assertTrue(scn.DSHasCardChoiceAvailable(supremacy));
            scn.DSChooseCard(supremacy);
            scn.PassAllResponses();

            assertTrue(scn.DSDecisionAvailable("Choose where to deploy")
                    || scn.DSDecisionAvailable("Choose location where to deploy"));
            assertTrue("Kijimi must be a legal Episode VII staging system",
                    scn.DSHasCardChoiceAvailable(kijimi));
            assertTrue("Crait must also be a legal Episode VII system",
                    scn.DSHasCardChoiceAvailable(crait));
            assertFalse("Tracked Fleet prohibits owner starship deployment to its host",
                    scn.DSHasCardChoiceAvailable(dqar));
            scn.DSChooseCard(kijimi);
            scn.PassAllResponses();

            assertEquals("Objective reset must charge exactly 7 Force for Supremacy",
                    0, scn.GetDSForcePileCount());
            assertAtLocation(kijimi, supremacy);

            scn.SkipToPhase(Phase.MOVE);
            assertEquals(0, scn.GetDSForcePileCount());
            assertTrue("Tracked Fleet must make Supremacy's move to D'Qar free",
                    scn.DSMoveAvailable(supremacy));
            scn.DSMoveCard(supremacy, dqar);
            scn.PassAllResponses();

            assertAtLocation(dqar, supremacy);
            assertEquals("The legal hyperspace move to Tracked Fleet must spend no Force",
                    0, scn.GetDSForcePileCount());
            assertTrue("Supremacy's permanent pilot must establish ordinary control",
                    GameConditions.controls(
                            scn.game(), VirtualTableScenario.DS, dqar));
            assertFalse(GameConditions.isBlownAway(
                    scn.game(), Filters.Tracked_Fleet));
            assertFalse("The objective waits for the next owner-turn trigger",
                    objective.isFlipped());

            scn.SkipToLSTurn();
            scn.SkipToDSTurn();
            scn.PassAllResponses();

            assertTrue("Tracked Fleet must be historically recorded as blown away",
                    GameConditions.isBlownAway(
                            scn.game(), Filters.Tracked_Fleet));
            assertInZone(Zone.OUT_OF_PLAY, fleet);
            assertInZone(Zone.SIDE_OF_TABLE, objective);
            assertTrue(printing.objectiveBlueprintId
                            + " must flip from its native blown-away table-change trigger",
                    objective.isFlipped());
        }
    }

    @Test
    public void bothPrintingsRelocateOnlyWithinRangeAndFollowTheCurrentHost() {
        for (Printing printing : PRINTINGS) {
            var scn = scenario(printing);
            var objective = scn.GetDSCard("objective");
            var fleet = scn.GetDSCard("fleet");
            var dqar = scn.GetDSCard("dqar");
            var crait = scn.GetDSCard("crait");
            var kijimi = scn.GetDSCard("kijimi");
            var dantooine = scn.GetDSCard("dantooine");
            var ahchTo = scn.GetLSCard("ahchTo");
            var supremacy = scn.GetDSCard("supremacy");
            var stackA = scn.GetLSCard("stackA");
            var stackB = scn.GetLSCard("stackB");

            scn.MoveCardsToLSHand(stackA, stackB);
            scn.StartGame();
            scn.MoveLocationToTable(kijimi);
            scn.MoveLocationToTable(dantooine);
            scn.MoveLocationToTable(ahchTo);
            scn.MoveCardsToLocation(dqar, supremacy);

            assertTrue(GameConditions.controls(
                    scn.game(), VirtualTableScenario.DS, dqar));
            scn.SkipToLSTurn(Phase.MOVE);

            assertTrue(scn.LSCardActionAvailable(fleet, "Stack card from hand"));
            scn.LSUseCardAction(fleet, "Stack card from hand");

            assertTrue("Kijimi is within 1 parsec of D'Qar",
                    scn.LSHasCardChoiceAvailable(kijimi));
            assertTrue("Crait is the inclusive 3-parsec boundary",
                    scn.LSHasCardChoiceAvailable(crait));
            assertFalse("Tracked Fleet may not relocate to its current host",
                    scn.LSHasCardChoiceAvailable(dqar));
            assertFalse("Ahch-To is 4 parsecs from D'Qar",
                    scn.LSHasCardChoiceAvailable(ahchTo));
            assertFalse("Dantooine is in range but is not Episode VII",
                    scn.LSHasCardChoiceAvailable(dantooine));

            scn.LSChooseCard(crait);
            assertTrue(scn.LSHasCardChoiceAvailable(stackA));
            assertTrue(scn.LSHasCardChoiceAvailable(stackB));
            scn.LSChooseCard(stackA);
            scn.PassAllResponses();

            assertTrue(scn.IsStackedOn(fleet, stackA));
            assertEquals(1, scn.GetStackedCards(fleet).size());
            assertTrue("The legal relocation must move the Fleet attachment to Crait",
                    scn.IsAttachedTo(crait, fleet));
            assertTrue("Old-host control remains true, proving the next check follows the Fleet",
                    GameConditions.controls(
                            scn.game(), VirtualTableScenario.DS, dqar));
            assertFalse(GameConditions.controls(
                    scn.game(), VirtualTableScenario.DS, crait));

            scn.SkipToDSTurn();
            scn.PassAllResponses();

            assertFalse("Controlling stale D'Qar must not annihilate Fleet now at Crait",
                    GameConditions.isBlownAway(
                            scn.game(), Filters.Tracked_Fleet));
            assertTrue(scn.IsAttachedTo(crait, fleet));
            assertFalse(objective.isFlipped());
        }
    }

    @Test
    public void bothPrintingsDoNotAnnihilateAtAContestedHost() {
        for (Printing printing : PRINTINGS) {
            var scn = scenario(printing);
            var objective = scn.GetDSCard("objective");
            var fleet = scn.GetDSCard("fleet");
            var dqar = scn.GetDSCard("dqar");
            var supremacy = scn.GetDSCard("supremacy");

            scn.StartGame();
            scn.MoveCardsToLocation(
                    dqar,
                    supremacy,
                    scn.GetLSCard("xwing1"),
                    scn.GetLSCard("xwing2"),
                    scn.GetLSCard("xwing3"),
                    scn.GetLSCard("xwing4"));

            assertFalse("Ability 4 versus ability 4 is contested, not Dark control",
                    GameConditions.controls(
                            scn.game(), VirtualTableScenario.DS, dqar));
            assertFalse("Ability 4 versus ability 4 is contested, not Light control",
                    GameConditions.controls(
                            scn.game(), VirtualTableScenario.LS, dqar));

            scn.SkipToLSTurn();
            scn.SkipToDSTurn();
            scn.PassAllResponses();

            assertFalse(GameConditions.isBlownAway(
                    scn.game(), Filters.Tracked_Fleet));
            assertTrue(scn.IsAttachedTo(dqar, fleet));
            assertFalse(objective.isFlipped());
        }
    }

    @Test
    public void bothPrintingsStopRelocationAfterThreeStackedCards() {
        for (Printing printing : PRINTINGS) {
            var scn = scenario(printing);
            var fleet = scn.GetDSCard("fleet");
            var kijimi = scn.GetDSCard("kijimi");
            var stackA = scn.GetLSCard("stackA");
            var stackB = scn.GetLSCard("stackB");
            var stackC = scn.GetLSCard("stackC");
            var stackD = scn.GetLSCard("stackD");

            scn.StartGame();
            scn.MoveLocationToTable(kijimi);
            scn.StackCardsOn(fleet, stackA, stackB, stackC);
            scn.MoveCardsToLSHand(stackD);
            scn.SkipToLSTurn(Phase.MOVE);

            assertEquals(3, scn.GetStackedCards(fleet).size());
            assertFalse("Three stacked cards close the relocation action",
                    scn.LSCardActionAvailable(fleet, "Stack card from hand"));
        }
    }

    @Test
    public void bothBackSidesExposeTheExactLostPileDeployFilter() {
        for (Printing printing : PRINTINGS) {
            var scn = scenario(printing);
            var objective = scn.GetDSCard("objective");
            var eligibleTrooper = scn.GetDSCard("firstOrderTrooperA");
            var eligibleVehicle = scn.GetDSCard("firstOrderVehicle");
            var uniqueTrooper = scn.GetDSCard("uniqueTrooper");
            var nonFirstOrderVehicle = scn.GetDSCard("imperialVehicle");

            scn.StartGame();
            scn.MoveCardsToTopOfDSLostPile(
                    eligibleTrooper,
                    eligibleVehicle,
                    uniqueTrooper,
                    nonFirstOrderVehicle);

            assertFalse("Lost Pile action belongs only to the back side",
                    scn.DSCardActionAvailable(objective, "Deploy card from Lost Pile"));

            flipToTheResistanceIsDoomed(scn, objective);
            scn.SkipToPhase(Phase.DEPLOY);
            scn.DSActivateForceCheat(20);

            assertTrue("Phasma must satisfy the source trooper filter",
                    Filters.trooper.accepts(scn.game(), uniqueTrooper));
            assertFalse("Phasma must fail the source non-unique filter",
                    Filters.non_unique.accepts(scn.game(), uniqueTrooper));
            assertTrue(scn.DSCardActionAvailable(objective, "Deploy card from Lost Pile"));
            scn.DSUseCardAction(objective, "Deploy card from Lost Pile");

            assertTrue("Non-unique trooper must be selectable",
                    isDisplayedPileCardSelectable(scn, eligibleTrooper));
            assertTrue("Non-unique First Order vehicle must be selectable",
                    isDisplayedPileCardSelectable(scn, eligibleVehicle));
            assertFalse("Unique trooper may be shown but must not be selectable",
                    isDisplayedPileCardSelectable(scn, uniqueTrooper));
            assertFalse("Non-First Order vehicle may be shown but must not be selectable",
                    isDisplayedPileCardSelectable(scn, nonFirstOrderVehicle));

            scn.DSChooseCard(eligibleTrooper);
            scn.PassAllResponses();
            assertTrue(scn.DSDecisionAvailable("Choose where to deploy")
                    || scn.DSDecisionAvailable("Choose location where to deploy"));
            assertTrue(scn.DSHasCardChoiceAvailable(scn.GetLSStartingLocation()));
            scn.DSChooseCard(scn.GetLSStartingLocation());
            scn.PassAllResponses();

            assertAtLocation(scn.GetLSStartingLocation(), eligibleTrooper);
        }
    }

    @Test
    public void bothBackSidesAddOneDrainOnlyForTheCompleteCraitAndFirstOrderPairState() {
        for (Printing printing : PRINTINGS) {
            var complete = scenario(printing);
            var completeObjective = complete.GetDSCard("objective");
            var completeWalkway = complete.GetLSStartingLocation();

            complete.StartGame();
            flipToTheResistanceIsDoomed(complete, completeObjective);
            complete.MoveCardsToLocation(
                    complete.GetDSCard("salt"),
                    complete.GetDSCard("kylo"));
            complete.MoveCardsToLocation(
                    completeWalkway,
                    complete.GetDSCard("firstOrderTrooperA"),
                    complete.GetDSCard("firstOrderTrooperB"));

            assertEquals("Two co-located First Order characters earn exactly +1",
                    2f,
                    forceDrainAmount(
                            complete, completeWalkway, VirtualTableScenario.DS),
                    complete.epsilon);

            complete.MoveCardsToLocation(
                    completeWalkway, complete.GetDSCard("firstOrderTrooperC"));
            assertEquals("The source says two, so a third character must not disable the bonus",
                    2f,
                    forceDrainAmount(
                            complete, completeWalkway, VirtualTableScenario.DS),
                    complete.epsilon);

            var noKylo = scenario(printing);
            var noKyloWalkway = noKylo.GetLSStartingLocation();
            noKylo.StartGame();
            flipToTheResistanceIsDoomed(noKylo, noKylo.GetDSCard("objective"));
            noKylo.MoveCardsToLocation(
                    noKyloWalkway,
                    noKylo.GetDSCard("firstOrderTrooperA"),
                    noKylo.GetDSCard("firstOrderTrooperB"));
            assertEquals("The First Order pair does nothing without Kylo occupying Crait",
                    1f,
                    forceDrainAmount(noKylo, noKyloWalkway, VirtualTableScenario.DS),
                    noKylo.epsilon);

            var separated = scenario(printing);
            var separatedWalkway = separated.GetLSStartingLocation();
            separated.StartGame();
            flipToTheResistanceIsDoomed(separated, separated.GetDSCard("objective"));
            separated.MoveCardsToLocation(
                    separated.GetDSCard("salt"), separated.GetDSCard("kylo"));
            separated.MoveCardsToLocation(
                    separatedWalkway, separated.GetDSCard("firstOrderTrooperA"));
            separated.MoveCardsToLocation(
                    separated.GetDSCard("salt"),
                    separated.GetDSCard("firstOrderTrooperB"));
            assertEquals("Two First Order characters at different locations are not a pair",
                    1f,
                    forceDrainAmount(
                            separated, separatedWalkway, VirtualTableScenario.DS),
                    separated.epsilon);
        }
    }

    @Test
    public void bothBackSidesCancelRetrievalAndBlockOnlyLoneOpponentAbilitySources() {
        for (Printing printing : PRINTINGS) {
            var scn = scenario(printing);
            var objective = scn.GetDSCard("objective");
            var salt = scn.GetDSCard("salt");
            var walkway = scn.GetLSStartingLocation();
            var dqar = scn.GetDSCard("dqar");
            var kylo = scn.GetDSCard("kylo");
            var han = scn.GetLSCard("han");
            var leia = scn.GetLSCard("leia");
            var xwing1 = scn.GetLSCard("xwing1");
            var xwing2 = scn.GetLSCard("xwing2");

            scn.StartGame();
            flipToTheResistanceIsDoomed(scn, objective);
            scn.MoveCardsToLocation(salt, kylo);
            scn.MoveCardsToLocation(walkway, han);
            scn.MoveCardsToLocation(dqar, xwing1);

            assertTrue(GameConditions.controlsWith(
                    scn.game(),
                    objective,
                    VirtualTableScenario.DS,
                    Filters.Crait_Salt_Plateau,
                    Filters.Kylo));
            assertTrue("A lone opposing character may not Force drain",
                    forceDrainProhibited(scn, walkway, VirtualTableScenario.LS));
            assertTrue("A lone opposing permanent pilot may not Force drain",
                    forceDrainProhibited(scn, dqar, VirtualTableScenario.LS));

            scn.MoveCardsToLocation(walkway, leia);
            scn.MoveCardsToLocation(dqar, xwing2);
            assertFalse("Two opposing characters are not the prohibited lone case",
                    forceDrainProhibited(scn, walkway, VirtualTableScenario.LS));
            assertFalse("Two opposing permanent pilots are not the prohibited lone case",
                    forceDrainProhibited(scn, dqar, VirtualTableScenario.LS));

            scn.MoveCardsToTopOfLSLostPile(scn.GetLSFiller(1));
            int lostBeforeCanceledRetrieval = scn.GetLSLostPileCount();
            int usedBeforeCanceledRetrieval = scn.GetLSUsedPileCount();
            scn.SkipToLSTurn(Phase.CONTROL);
            retrieveOneForce(scn, han, VirtualTableScenario.LS);

            assertEquals("Kylo controlling Salt must cancel opponent retrieval",
                    lostBeforeCanceledRetrieval, scn.GetLSLostPileCount());
            assertEquals("Canceled retrieval must not move a card to Used Pile",
                    usedBeforeCanceledRetrieval, scn.GetLSUsedPileCount());

            var allowed = scenario(printing);
            var allowedObjective = allowed.GetDSCard("objective");
            var allowedSalt = allowed.GetDSCard("salt");
            var allowedKylo = allowed.GetDSCard("kylo");
            var allowedHan = allowed.GetLSCard("han");
            var allowedLeia = allowed.GetLSCard("leia");

            allowed.StartGame();
            flipToTheResistanceIsDoomed(allowed, allowedObjective);
            allowed.MoveCardsToLocation(
                    allowedSalt, allowedKylo, allowedHan, allowedLeia);
            assertFalse("Kylo merely present at a Salt he does not control is insufficient",
                    GameConditions.controlsWith(
                            allowed.game(),
                            allowedObjective,
                            VirtualTableScenario.DS,
                            Filters.Crait_Salt_Plateau,
                            Filters.Kylo));

            allowed.MoveCardsToTopOfLSLostPile(allowed.GetLSFiller(1));
            int lostBeforeAllowedRetrieval = allowed.GetLSLostPileCount();
            int usedBeforeAllowedRetrieval = allowed.GetLSUsedPileCount();
            allowed.SkipToLSTurn(Phase.CONTROL);
            retrieveOneForce(allowed, allowedHan, VirtualTableScenario.LS);

            assertEquals("Without Kylo control, opponent retrieves normally",
                    lostBeforeAllowedRetrieval - 1, allowed.GetLSLostPileCount());
            assertEquals(
                    usedBeforeAllowedRetrieval + 1, allowed.GetLSUsedPileCount());
        }
    }

    @Test
    public void bothBackSidesPlaceTheObjectiveOutOfPlayForTheCompleteSaltForfeitTrigger() {
        for (Printing printing : PRINTINGS) {
            for (String matchingCharacterKey : List.of(
                    "han", "leia", "luke")) {
                assertCompleteSaltForfeitTrigger(
                        printing, matchingCharacterKey);
            }
        }
    }

    @Test
    public void bothBackSidesRequireEverySaltForfeitTriggerConjunct() {
        for (Printing printing : PRINTINGS) {
            var wrongLocation = scenario(printing);
            var wrongLocationObjective = wrongLocation.GetDSCard("objective");
            var walkway = wrongLocation.GetLSStartingLocation();
            var wrongLocationKylo = wrongLocation.GetDSCard("kylo");

            wrongLocation.StartGame();
            flipToTheResistanceIsDoomed(
                    wrongLocation, wrongLocationObjective);
            wrongLocation.MoveCardsToLocation(walkway, wrongLocationKylo);
            wrongLocation.MoveCardsToLocation(
                    walkway,
                    wrongLocation.GetLSCard("han"),
                    wrongLocation.GetLSCard("luke"));
            wrongLocation.SkipToLSTurn(Phase.BATTLE);
            wrongLocation.LSInitiateBattle(walkway);
            wrongLocation.SkipToDamageSegment(false);
            assertTrue(wrongLocation.LSWonBattle());
            wrongLocation.DSPayBattleDamageFromCardInPlay(
                    wrongLocationKylo);
            assertInZone(Zone.LOST_PILE, wrongLocationKylo);
            assertInZone(Zone.SIDE_OF_TABLE, wrongLocationObjective);

            var noHanLeiaLuke = scenario(printing);
            var noHanLeiaLukeObjective =
                    noHanLeiaLuke.GetDSCard("objective");
            var noHanLeiaLukeSalt = noHanLeiaLuke.GetDSCard("salt");
            var noHanLeiaLukeKylo = noHanLeiaLuke.GetDSCard("kylo");

            noHanLeiaLuke.StartGame();
            flipToTheResistanceIsDoomed(
                    noHanLeiaLuke, noHanLeiaLukeObjective);
            noHanLeiaLuke.MoveCardsToLocation(
                    noHanLeiaLukeSalt, noHanLeiaLukeKylo);
            noHanLeiaLuke.MoveCardsToLocation(
                    noHanLeiaLukeSalt,
                    noHanLeiaLuke.GetLSCard("mace"),
                    noHanLeiaLuke.GetLSCard("stackA"));
            noHanLeiaLuke.SkipToLSTurn(Phase.BATTLE);
            noHanLeiaLuke.LSInitiateBattle(noHanLeiaLukeSalt);
            noHanLeiaLuke.SkipToDamageSegment(false);
            assertTrue(noHanLeiaLuke.LSWonBattle());
            noHanLeiaLuke.DSPayBattleDamageFromCardInPlay(
                    noHanLeiaLukeKylo);
            assertInZone(Zone.LOST_PILE, noHanLeiaLukeKylo);
            assertInZone(Zone.SIDE_OF_TABLE, noHanLeiaLukeObjective);

            var notKylo = scenario(printing);
            var notKyloObjective = notKylo.GetDSCard("objective");
            var notKyloSalt = notKylo.GetDSCard("salt");
            var firstOrderTrooper =
                    notKylo.GetDSCard("firstOrderTrooperA");

            notKylo.StartGame();
            flipToTheResistanceIsDoomed(notKylo, notKyloObjective);
            notKylo.MoveCardsToLocation(
                    notKyloSalt, firstOrderTrooper);
            notKylo.MoveCardsToLocation(
                    notKyloSalt, notKylo.GetLSCard("luke"));
            notKylo.SkipToLSTurn(Phase.BATTLE);
            notKylo.LSInitiateBattle(notKyloSalt);
            notKylo.SkipToDamageSegment(false);
            assertTrue(notKylo.LSWonBattle());
            notKylo.DSPayBattleDamageFromCardInPlay(firstOrderTrooper);
            assertInZone(Zone.LOST_PILE, firstOrderTrooper);
            assertInZone(Zone.SIDE_OF_TABLE, notKyloObjective);

            var fromHand = scenario(printing);
            var fromHandObjective = fromHand.GetDSCard("objective");
            var fromHandSalt = fromHand.GetDSCard("salt");
            var fromHandKylo = fromHand.GetDSCard("kylo");

            fromHand.StartGame();
            flipToTheResistanceIsDoomed(fromHand, fromHandObjective);
            fromHand.MoveCardsToDSHand(fromHandKylo);
            fromHand.MoveCardsToLocation(
                    fromHandSalt,
                    fromHand.GetDSCard("firstOrderTrooperA"));
            fromHand.MoveCardsToLocation(
                    fromHandSalt, fromHand.GetLSCard("luke"));
            fromHand.SkipToLSTurn(Phase.BATTLE);
            fromHand.LSInitiateBattle(fromHandSalt);
            fromHand.SkipToDamageSegment(false);
            assertTrue(fromHand.LSWonBattle());
            fromHand.DSPayBattleDamageFromCardInHand(fromHandKylo);
            assertInZone(Zone.LOST_PILE, fromHandKylo);
            assertInZone(Zone.SIDE_OF_TABLE, fromHandObjective);

            var darkWon = scenario(printing);
            var darkWonObjective = darkWon.GetDSCard("objective");
            var darkWonSalt = darkWon.GetDSCard("salt");
            var darkWonKylo = darkWon.GetDSCard("kylo");

            darkWon.StartGame();
            flipToTheResistanceIsDoomed(darkWon, darkWonObjective);
            darkWon.MoveCardsToLocation(darkWonSalt, darkWonKylo);
            darkWon.GetDSFillerRange(8).forEach(
                    card -> darkWon.MoveCardsToLocation(
                            darkWonSalt, card));
            darkWon.MoveCardsToLocation(
                    darkWonSalt, darkWon.GetLSCard("luke"));
            darkWon.SkipToLSTurn(Phase.BATTLE);
            darkWon.PrepareDSDestiny(0);
            darkWon.PrepareLSDestiny(7);
            darkWon.LSInitiateBattle(darkWonSalt);
            darkWon.SkipToDamageSegment(true);

            assertTrue("Kylo forfeit must not trigger when Dark won",
                    darkWon.DSWonBattle());
            assertTrue(darkWon.AwaitingLSBattleDamagePayment());
            darkWon.LSPayBattleDamageFromReserveDeck();
            assertTrue(darkWon.AwaitingDSAttritionPayment());
            darkWon.DSPayAttritionFromCardInPlay(darkWonKylo);
            assertInZone(Zone.LOST_PILE, darkWonKylo);
            assertInZone(Zone.SIDE_OF_TABLE, darkWonObjective);
        }
    }
}
