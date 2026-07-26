package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.game.PhysicalCardImpl;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import static com.gempukku.swccgo.framework.Assertions.assertAtLocation;
import static com.gempukku.swccgo.framework.Assertions.assertInZone;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Native engine proof for There Is Good In Him / I Can Save Him.
 *
 * <p>The objective's Light Side owner deliberately advances by allowing its own Luke to be
 * captured. The exact front-side route is a capture-capable Imperial at Luke's physical site.
 * The back remains up while Luke is captive or present with Vader, then converts I Feel The
 * Conflict's stack into the modifier-adjusted crossover destiny total.
 */
public class ThereIsGoodInHimObjectiveEngineContractTest {
    private static final String OBJECTIVE_BLUEPRINT_ID = "9_61";
    private static final String BACK_BLUEPRINT_ID = "9_61_BACK";
    private static final String CROSSOVER_ACTION =
            "Shuffle Reserve Deck and draw destiny";

    private static final List<String> DEATH_STAR_II_LUKE_PRINTINGS = List.of(
            "9_24",
            "10_10",
            "200_21",
            "222_29");

    private static StartingSetup objectiveSetup(String lukeBlueprintId) {
        return new StartingSetup() {
            @Override
            public HashMap<String, String> Cards() {
                HashMap<String, String> cards = new HashMap<>();
                cards.put("objective", OBJECTIVE_BLUEPRINT_ID);
                cards.put("hut", "8_71");
                cards.put("luke", lukeBlueprintId);
                cards.put("lightsaber", "9_90");
                cards.put("platform", "8_76");
                cards.put("conflict", "9_34");
                return cards;
            }

            @Override
            public void Setup(VirtualTableScenario scn) {
                if (scn.LSDecisionAvailable("On which side")) {
                    scn.LSChoose("Left");
                }
            }
        };
    }

    private static VirtualTableScenario scenario(String lukeBlueprintId) {
        HashMap<String, String> lightCards = new HashMap<>();

        HashMap<String, String> darkCards = new HashMap<>();
        darkCards.put("vader", "1_168");
        darkCards.put("imperial", "1_170");
        darkCards.put("alien", "2_89");

        return new VirtualTableScenario(
                lightCards,
                darkCards,
                20,
                20,
                objectiveSetup(lukeBlueprintId),
                StartingSetup.DefaultDSGroundLocation,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open);
    }

    private static void captureLukeAndFlip(
            VirtualTableScenario scn, PhysicalCardImpl escort) {
        PhysicalCardImpl objective = scn.GetLSCard("objective");
        PhysicalCardImpl hut = scn.GetLSCard("hut");
        PhysicalCardImpl luke = scn.GetLSCard("luke");

        assertFalse(objective.isFlipped());
        scn.MoveCardsToLocation(hut, escort);
        scn.SkipToPhase(Phase.CONTROL);
        scn.PassAllResponses();

        assertTrue("Luke must be captured by the exact eligible Imperial", luke.isCaptive());
        assertEquals(escort, luke.getEscort());
        assertEquals(escort, luke.getAttachedTo());
        assertTrue(escort.getCardsEscorting().contains(luke));
        assertTrue("The native captive-state trigger must flip the objective",
                objective.isFlipped());
        assertEquals(BACK_BLUEPRINT_ID, objective.getBlueprintId(false));
    }

    private static void leaveOnlyLSDestiny(
            VirtualTableScenario scn, int destiny) {
        while (scn.GetLSReserveDeckCount() > 0) {
            scn.MoveCardsToLSHand(scn.GetTopOfLSReserveDeck());
        }
        scn.PrepareLSDestiny(destiny);
        assertEquals(1, scn.GetLSReserveDeckCount());
    }

    private static float crossoverTotal(
            VirtualTableScenario scn, PhysicalCardImpl character, float baseDestiny) {
        return scn.game().getModifiersQuerying().getCrossoverAttemptTotal(
                scn.gameState(), character, baseDestiny);
    }

    private static void finishCrossoverResponses(VirtualTableScenario scn) {
        for (int attempts = 0;
             attempts < 20 && scn.GetDSLifeForceRemaining() > 0;
             attempts++) {
            if (!scn.DSAnyDecisionsAvailable() && !scn.LSAnyDecisionsAvailable()) {
                scn.game().carryOutPendingActionsUntilDecisionNeeded();
                if (!scn.DSAnyDecisionsAvailable() && !scn.LSAnyDecisionsAvailable()) {
                    return;
                }
            }

            if (scn.GetCurrentDecision().getDecisionType()
                    == AwaitingDecisionType.MULTIPLE_CHOICE) {
                if (scn.LSAnyDecisionsAvailable()) {
                    scn.LSDecided(0);
                }
                else {
                    scn.DSDecided(0);
                }
                continue;
            }
            if (scn.GetCurrentDecision().getDecisionType()
                    == AwaitingDecisionType.CARD_ACTION_CHOICE
                    || scn.GetCurrentDecision().getDecisionType()
                    == AwaitingDecisionType.ACTION_CHOICE) {
                if (scn.LSAnyDecisionsAvailable()) {
                    scn.LSPass();
                }
                else {
                    scn.DSPass();
                }
                continue;
            }

            throw new AssertionError(
                    "Unexpected crossover decision type: "
                            + scn.GetCurrentDecision().getDecisionType());
        }
    }

    @Test
    public void setupDeploysAllFiveRequiredCardsForEveryDeathStarIILukePrinting() {
        for (String lukeBlueprintId : DEATH_STAR_II_LUKE_PRINTINGS) {
            VirtualTableScenario scn = scenario(lukeBlueprintId);
            PhysicalCardImpl objective = scn.GetLSCard("objective");
            PhysicalCardImpl hut = scn.GetLSCard("hut");
            PhysicalCardImpl luke = scn.GetLSCard("luke");
            PhysicalCardImpl lightsaber = scn.GetLSCard("lightsaber");
            PhysicalCardImpl platform = scn.GetLSCard("platform");
            PhysicalCardImpl conflict = scn.GetLSCard("conflict");

            scn.StartGame();

            assertInZone(Zone.SIDE_OF_TABLE, objective, conflict);
            assertInZone(Zone.LOCATIONS, hut, platform);
            assertAtLocation(hut, luke);
            assertTrue("Luke's Lightsaber must be deployed at Chief Chirpa's Hut",
                    Filters.at(hut).accepts(scn.game(), lightsaber));
            assertEquals(lukeBlueprintId, luke.getBlueprintId(true));
            assertTrue("Setup must use the exact [Death Star II] Luke filter",
                    Filters.and(Icon.DEATH_STAR_II, Filters.Luke)
                            .accepts(scn.game(), luke));
            assertFalse(objective.isFlipped());
        }
    }

    @Test
    public void eligibleImperialCapturesOnlyAtLukesExactPhysicalSiteAndFlips() {
        VirtualTableScenario scn = scenario("222_29");
        PhysicalCardImpl objective = scn.GetLSCard("objective");
        PhysicalCardImpl hut = scn.GetLSCard("hut");
        PhysicalCardImpl platform = scn.GetLSCard("platform");
        PhysicalCardImpl luke = scn.GetLSCard("luke");
        PhysicalCardImpl imperial = scn.GetDSCard("imperial");

        scn.StartGame();
        scn.MoveCardsToLocation(platform, imperial);
        assertTrue(Filters.Imperial.accepts(scn.game(), imperial));
        assertTrue(Filters.canEscortCaptive(luke, true).accepts(scn.game(), imperial));
        assertFalse("A legal distinct site is not Luke's physical site",
                Filters.atSameSite(luke).accepts(scn.game(), imperial));
        scn.SkipToPhase(Phase.CONTROL);

        assertFalse(luke.isCaptive());
        assertNull(luke.getEscort());
        assertFalse(objective.isFlipped());

        scn.MoveCardsToLocation(hut, imperial);
        assertTrue(Filters.atSameSite(luke).accepts(scn.game(), imperial));
        scn.SkipToPhase(Phase.DEPLOY);
        scn.PassAllResponses();

        assertTrue(luke.isCaptive());
        assertEquals(imperial, luke.getEscort());
        assertTrue(objective.isFlipped());
        assertEquals(BACK_BLUEPRINT_ID, objective.getBlueprintId(false));
    }

    @Test
    public void sameSiteCaptureCapableNonImperialDoesNotCaptureLuke() {
        VirtualTableScenario scn = scenario("9_24");
        PhysicalCardImpl objective = scn.GetLSCard("objective");
        PhysicalCardImpl hut = scn.GetLSCard("hut");
        PhysicalCardImpl luke = scn.GetLSCard("luke");
        PhysicalCardImpl alien = scn.GetDSCard("alien");

        scn.StartGame();
        scn.MoveCardsToLocation(hut, alien);

        assertTrue(Filters.atSameSite(luke).accepts(scn.game(), alien));
        assertTrue(Filters.canEscortCaptive(luke, true).accepts(scn.game(), alien));
        assertFalse(Filters.Imperial.accepts(scn.game(), alien));
        scn.SkipToPhase(Phase.CONTROL);

        assertFalse(luke.isCaptive());
        assertNull(luke.getEscort());
        assertFalse(objective.isFlipped());
    }

    @Test
    public void backStaysFlippedWhileLukeRemainsCaptive() {
        VirtualTableScenario scn = scenario("9_24");
        PhysicalCardImpl objective = scn.GetLSCard("objective");
        PhysicalCardImpl platform = scn.GetLSCard("platform");
        PhysicalCardImpl luke = scn.GetLSCard("luke");
        PhysicalCardImpl imperial = scn.GetDSCard("imperial");

        scn.StartGame();
        captureLukeAndFlip(scn, imperial);

        scn.MoveCardsToLocation(platform, scn.GetDSFiller(1));
        scn.SkipToPhase(Phase.DEPLOY);

        assertTrue(luke.isCaptive());
        assertEquals(imperial, luke.getEscort());
        assertTrue("Captive Luke satisfies the first back-side hold branch",
                objective.isFlipped());
    }

    @Test
    public void freeLukePresentWithVaderHoldsThenLeavingVaderFlipsBack() {
        VirtualTableScenario scn = scenario("9_24");
        PhysicalCardImpl objective = scn.GetLSCard("objective");
        PhysicalCardImpl hut = scn.GetLSCard("hut");
        PhysicalCardImpl platform = scn.GetLSCard("platform");
        PhysicalCardImpl luke = scn.GetLSCard("luke");
        PhysicalCardImpl imperial = scn.GetDSCard("imperial");
        PhysicalCardImpl vader = scn.GetDSCard("vader");

        scn.StartGame();
        captureLukeAndFlip(scn, imperial);

        scn.MoveCardsToLocation(platform, imperial);
        scn.MoveCardsToLocation(hut, luke, vader);
        scn.SkipToPhase(Phase.DEPLOY);

        assertFalse(luke.isCaptive());
        assertAtLocation(hut, luke, vader);
        assertTrue("Free Luke present with Vader satisfies the second hold branch",
                objective.isFlipped());

        scn.MoveCardsToLocation(platform, vader);
        scn.SkipToPhase(Phase.MOVE);

        assertAtLocation(hut, luke);
        assertAtLocation(platform, vader);
        assertFalse("Free Luke away from Vader must trigger the native flip-back",
                objective.isFlipped());
        assertEquals(OBJECTIVE_BLUEPRINT_ID, objective.getBlueprintId(false));
    }

    @Test
    public void endOfOpponentTurnLosesExactlyTwoUnlessVaderEscortsLuke() {
        VirtualTableScenario unescortedByVader = scenario("9_24");
        PhysicalCardImpl imperial = unescortedByVader.GetDSCard("imperial");

        unescortedByVader.StartGame();
        captureLukeAndFlip(unescortedByVader, imperial);
        unescortedByVader.SkipToPhase(Phase.DRAW);
        int lifeBeforeLoss = unescortedByVader.GetDSLifeForceRemaining();
        unescortedByVader.DSPass();
        unescortedByVader.LSPass();
        unescortedByVader.PassAllResponses();

        assertTrue(unescortedByVader.AwaitingDSForceLossPayment());
        unescortedByVader.DSPayRemainingForceLossFromReserveDeck();
        assertEquals("The required end-turn clock must be exactly 2 Force",
                lifeBeforeLoss - 2, unescortedByVader.GetDSLifeForceRemaining());

        VirtualTableScenario escortedByVader = scenario("9_24");
        PhysicalCardImpl vader = escortedByVader.GetDSCard("vader");

        escortedByVader.StartGame();
        captureLukeAndFlip(escortedByVader, vader);
        escortedByVader.SkipToPhase(Phase.DRAW);
        int lifeBeforeExemption = escortedByVader.GetDSLifeForceRemaining();
        escortedByVader.DSPass();
        escortedByVader.LSPass();

        assertFalse("Vader escorting Luke closes the 2-Force trigger",
                escortedByVader.AwaitingDSForceLossPayment());
        assertFalse(escortedByVader.DSDecisionAvailable(
                "FORCE_LOSS_INITIATED - Optional responses"));
        assertEquals(lifeBeforeExemption, escortedByVader.GetDSLifeForceRemaining());
    }

    @Test
    public void iFeelTheConflictAddsExactlyThreePerStackedCardForVaderOnly() {
        VirtualTableScenario scn = scenario("9_24");
        PhysicalCardImpl conflict = scn.GetLSCard("conflict");
        PhysicalCardImpl platform = scn.GetLSCard("platform");
        PhysicalCardImpl vader = scn.GetDSCard("vader");
        PhysicalCardImpl imperial = scn.GetDSCard("imperial");
        PhysicalCardImpl stackA = scn.GetDSFiller(1);
        PhysicalCardImpl stackB = scn.GetDSFiller(2);
        PhysicalCardImpl stackC = scn.GetDSFiller(3);

        scn.StartGame();
        scn.MoveCardsToLocation(platform, vader, imperial);

        assertEquals(5f, crossoverTotal(scn, vader, 5f), scn.epsilon);
        scn.StackCardsOn(conflict, stackA);
        assertEquals(8f, crossoverTotal(scn, vader, 5f), scn.epsilon);
        scn.StackCardsOn(conflict, stackB, stackC);
        assertEquals(14f, crossoverTotal(scn, vader, 5f), scn.epsilon);
        assertEquals("I Feel The Conflict modifies Vader, not an arbitrary Imperial",
                5f, crossoverTotal(scn, imperial, 5f), scn.epsilon);
    }

    @Test
    public void exactFourteenFailsAndConsumesTheOncePerTurnCrossoverAction() {
        VirtualTableScenario scn = scenario("9_24");
        PhysicalCardImpl objective = scn.GetLSCard("objective");
        PhysicalCardImpl conflict = scn.GetLSCard("conflict");
        PhysicalCardImpl vader = scn.GetDSCard("vader");

        scn.StartGame();
        captureLukeAndFlip(scn, vader);
        scn.StackCardsOn(
                conflict,
                scn.GetDSFiller(1),
                scn.GetDSFiller(2),
                scn.GetDSFiller(3));
        scn.SkipToLSTurn(Phase.CONTROL);
        leaveOnlyLSDestiny(scn, 5);

        assertEquals(14f, crossoverTotal(scn, vader, 5f), scn.epsilon);
        assertTrue(scn.LSCardActionAvailable(objective, CROSSOVER_ACTION));
        assertEquals("The exact objective action must appear once",
                1,
                scn.GetLSAvailableActions().stream()
                        .filter(CROSSOVER_ACTION::equals)
                        .count());
        int darkLifeBefore = scn.GetDSLifeForceRemaining();

        scn.LSUseCardAction(objective, CROSSOVER_ACTION);
        scn.PassAllResponses();

        assertFalse("The source comparison is strict: 14 does not cross Vader",
                vader.isCrossedOver());
        assertEquals(VirtualTableScenario.DS, vader.getOwner());
        assertEquals(darkLifeBefore, scn.GetDSLifeForceRemaining());
        assertFalse("OncePerTurnEffect must close the exact action's engine latch",
                GameConditions.isOnceDuringYourTurn(
                        scn.game(),
                        objective,
                        VirtualTableScenario.LS,
                        objective.getCardId(),
                        GameTextActionId.OTHER_CARD_ACTION_4));
    }

    @Test
    public void modifierAdjustedFifteenCrossesVaderAndDepletesOpponent() {
        VirtualTableScenario scn = scenario("9_24");
        PhysicalCardImpl objective = scn.GetLSCard("objective");
        PhysicalCardImpl conflict = scn.GetLSCard("conflict");
        PhysicalCardImpl vader = scn.GetDSCard("vader");

        scn.StartGame();
        captureLukeAndFlip(scn, vader);
        scn.StackCardsOn(
                conflict,
                scn.GetDSFiller(1),
                scn.GetDSFiller(2),
                scn.GetDSFiller(3));
        scn.SkipToLSTurn(Phase.CONTROL);
        leaveOnlyLSDestiny(scn, 6);

        assertEquals(15f, crossoverTotal(scn, vader, 6f), scn.epsilon);
        assertTrue(scn.LSCardActionAvailable(objective, CROSSOVER_ACTION));
        assertTrue(scn.GetDSLifeForceRemaining() > 0);

        scn.LSUseCardAction(objective, CROSSOVER_ACTION);
        scn.PassAllResponses();

        assertTrue("A modifier-adjusted total above 14 must cross Vader",
                vader.isCrossedOver());
        assertEquals(VirtualTableScenario.LS, vader.getOwner());
        finishCrossoverResponses(scn);
        assertEquals("Successful crossover totally depletes the opponent",
                0, scn.GetDSLifeForceRemaining());
    }
}
