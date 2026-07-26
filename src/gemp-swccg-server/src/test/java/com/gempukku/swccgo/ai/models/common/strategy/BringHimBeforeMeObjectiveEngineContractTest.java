package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.game.PhysicalCardImpl;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextModifier;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextType;
import org.junit.Test;

import java.util.HashMap;

import static com.gempukku.swccgo.framework.Assertions.assertAtLocation;
import static com.gempukku.swccgo.framework.Assertions.assertInZone;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Native engine proof for Bring Him Before Me / Take Your Father's Place.
 *
 * <p>The front targets Luke by default, with source-supported Leia and Kanan retarget
 * branches. Any real captive state satisfies the flip. Vader's automatic seizure is a
 * separate route with stricter present-with and no-current-captive requirements. The back
 * remains up while the target is captive or present with Vader, charges its owner one Force
 * at the end of every own turn, and exposes the strict greater-than-12 crossover boundary.
 */
public class BringHimBeforeMeObjectiveEngineContractTest {
    private static final String FRONT_BLUEPRINT_ID = "9_151";
    private static final String BACK_BLUEPRINT_ID = "9_151_BACK";
    private static final String LUKE_DUEL_ACTION =
            "Initiate a Luke/Vader duel";
    private static final String LEIA_DUEL_ACTION =
            "Initiate a Leia/Vader duel";
    private static final String KANAN_DUEL_ACTION =
            "Initiate a Kanan/Vader duel";

    private static VirtualTableScenario scenario() {
        HashMap<String, String> lightCards = new HashMap<>();
        lightCards.put("luke", "9_24");
        lightCards.put("leia", "1_17");
        lightCards.put("kanan", "203_6");
        lightCards.put("thereIsAnother", "209_15");
        lightCards.put("otherCaptive", "1_11");

        HashMap<String, String> darkCards = new HashMap<>();
        darkCards.put("vader", "1_168");
        darkCards.put("emperor", "9_109");
        darkCards.put("trooper", "1_170");

        return new VirtualTableScenario(
                lightCards,
                darkCards,
                30,
                30,
                StartingSetup.DefaultLSGroundLocation,
                StartingSetup.BHBMObjective,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open);
    }

    private static void advanceTo(
            VirtualTableScenario scn, Phase phase) {
        scn.SkipToPhase(phase);
        scn.PassAllResponses();
    }

    private static void assertFront(PhysicalCardImpl objective) {
        assertFalse(objective.isFlipped());
        assertEquals(FRONT_BLUEPRINT_ID, objective.getBlueprintId(false));
    }

    private static void assertBack(PhysicalCardImpl objective) {
        assertTrue(objective.isFlipped());
        assertEquals(BACK_BLUEPRINT_ID, objective.getBlueprintId(false));
    }

    private static void addKananRetarget(
            VirtualTableScenario scn, PhysicalCardImpl source) {
        scn.game().getModifiersEnvironment().addUntilEndOfGameModifier(
                new ModifyGameTextModifier(
                        source,
                        Filters.or(
                                Filters.Bring_Him_Before_Me,
                                Filters.Take_Your_Fathers_Place,
                                Filters.Insignificant_Rebellion,
                                Filters.Your_Destiny),
                        ModifyGameTextType
                                .BRING_HIM_BEFORE_ME__TARGETS_KANAN_INSTEAD_OF_LUKE));
    }

    private static void emptyDSReserveDeck(
            VirtualTableScenario scn) {
        while (scn.GetDSReserveDeckCount() > 0) {
            scn.MoveCardsToDSHand(scn.GetTopOfDSReserveDeck());
        }
    }

    private static void emptyLSReserveDeck(
            VirtualTableScenario scn) {
        while (scn.GetLSReserveDeckCount() > 0) {
            scn.MoveCardsToLSHand(scn.GetTopOfLSReserveDeck());
        }
    }

    private static void prepareLukeWinningDuel(
            VirtualTableScenario scn, int crossoverDestiny) {
        emptyDSReserveDeck(scn);
        emptyLSReserveDeck(scn);

        scn.PrepareDSDestiny(crossoverDestiny);
        scn.PrepareDSDestiny(1);
        scn.PrepareDSDestiny(0);
        scn.PrepareLSDestiny(6);
        scn.PrepareLSDestiny(7);

        assertEquals(3, scn.GetDSReserveDeckCount());
        assertEquals(2, scn.GetLSReserveDeckCount());
    }

    private static void finishCrossoverResponses(
            VirtualTableScenario scn) {
        for (int attempts = 0;
             attempts < 20 && scn.GetLSLifeForceRemaining() > 0;
             attempts++) {
            if (!scn.DSAnyDecisionsAvailable()
                    && !scn.LSAnyDecisionsAvailable()) {
                scn.game().carryOutPendingActionsUntilDecisionNeeded();
                if (!scn.DSAnyDecisionsAvailable()
                        && !scn.LSAnyDecisionsAvailable()) {
                    return;
                }
            }

            AwaitingDecisionType decisionType =
                    scn.GetCurrentDecision().getDecisionType();
            if (decisionType == AwaitingDecisionType.MULTIPLE_CHOICE) {
                if (scn.LSAnyDecisionsAvailable()) {
                    scn.LSDecided(0);
                }
                else {
                    scn.DSDecided(0);
                }
                continue;
            }
            if (decisionType == AwaitingDecisionType.CARD_ACTION_CHOICE
                    || decisionType
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
                            + decisionType);
        }
    }

    private static VirtualTableScenario flippedDuelScenario(
            int crossoverDestiny) {
        VirtualTableScenario scn = scenario();
        PhysicalCardImpl objective = scn.GetDSCard("bhbm");
        PhysicalCardImpl throne = scn.GetDSCard("throne");
        PhysicalCardImpl rebellion = scn.GetDSCard("rebellion");
        PhysicalCardImpl luke = scn.GetLSCard("luke");
        PhysicalCardImpl vader = scn.GetDSCard("vader");
        PhysicalCardImpl emperor = scn.GetDSCard("emperor");

        scn.StartGame();
        scn.MoveCardsToLocation(throne, vader, emperor, luke);
        advanceTo(scn, Phase.CONTROL);

        assertTrue(luke.isCaptive());
        assertEquals(vader, luke.getEscort());
        assertBack(objective);
        assertTrue(scn.DSCardActionAvailable(
                objective, LUKE_DUEL_ACTION));

        scn.StackCardsOn(
                rebellion,
                scn.GetDSFiller(1),
                scn.GetDSFiller(2),
                scn.GetDSFiller(3));
        prepareLukeWinningDuel(scn, crossoverDestiny);
        return scn;
    }

    @Test
    public void setupDeploysTheExactThreeRequiredCards() {
        VirtualTableScenario scn = scenario();
        PhysicalCardImpl objective = scn.GetDSCard("bhbm");
        PhysicalCardImpl throne = scn.GetDSCard("throne");
        PhysicalCardImpl rebellion = scn.GetDSCard("rebellion");
        PhysicalCardImpl destiny = scn.GetDSCard("destiny");

        scn.StartGame();

        assertInZone(Zone.SIDE_OF_TABLE, objective, rebellion, destiny);
        assertInZone(Zone.LOCATIONS, throne);
        assertEquals(FRONT_BLUEPRINT_ID,
                objective.getBlueprintId(true));
        assertEquals("9_147", throne.getBlueprintId(true));
        assertEquals("9_127", rebellion.getBlueprintId(true));
        assertEquals("9_134", destiny.getBlueprintId(true));
        assertTrue(Filters.Throne_Room.accepts(scn.game(), throne));
        assertTrue(Filters.Insignificant_Rebellion.accepts(
                scn.game(), rebellion));
        assertTrue(Filters.Your_Destiny.accepts(
                scn.game(), destiny));
        assertEquals(VirtualTableScenario.DS, throne.getOwner());
        assertFront(objective);
    }

    @Test
    public void freeLukeDoesNotFlipEvenWhenPresentWithAnotherCapturer() {
        VirtualTableScenario scn = scenario();
        PhysicalCardImpl objective = scn.GetDSCard("bhbm");
        PhysicalCardImpl throne = scn.GetDSCard("throne");
        PhysicalCardImpl luke = scn.GetLSCard("luke");
        PhysicalCardImpl trooper = scn.GetDSCard("trooper");

        scn.StartGame();
        scn.MoveCardsToLocation(throne, trooper, luke);
        advanceTo(scn, Phase.CONTROL);

        assertAtLocation(throne, trooper, luke);
        assertFalse(luke.isCaptive());
        assertNull(luke.getEscort());
        assertFront(objective);
    }

    @Test
    public void captiveLukeFlipsRegardlessOfWhoCapturedHim() {
        VirtualTableScenario scn = scenario();
        PhysicalCardImpl objective = scn.GetDSCard("bhbm");
        PhysicalCardImpl throne = scn.GetDSCard("throne");
        PhysicalCardImpl luke = scn.GetLSCard("luke");
        PhysicalCardImpl trooper = scn.GetDSCard("trooper");

        scn.StartGame();
        scn.MoveCardsToLocation(throne, trooper, luke);
        scn.CaptureCardWith(trooper, luke);

        assertTrue(luke.isCaptive());
        assertEquals(trooper, luke.getEscort());
        assertEquals(trooper, luke.getAttachedTo());
        assertTrue(trooper.getCardsEscorting().contains(luke));

        advanceTo(scn, Phase.CONTROL);

        assertBack(objective);
        assertEquals("The non-Vader capturer must remain Luke's escort",
                trooper, luke.getEscort());
    }

    @Test
    public void vaderSelfSeizesOnlyWhenActuallyPresentWithLuke() {
        VirtualTableScenario scn = scenario();
        PhysicalCardImpl objective = scn.GetDSCard("bhbm");
        PhysicalCardImpl throne = scn.GetDSCard("throne");
        PhysicalCardImpl otherSite = scn.GetLSStartingLocation();
        PhysicalCardImpl luke = scn.GetLSCard("luke");
        PhysicalCardImpl vader = scn.GetDSCard("vader");

        scn.StartGame();
        scn.MoveCardsToLocation(throne, vader);
        scn.MoveCardsToLocation(otherSite, luke);
        advanceTo(scn, Phase.CONTROL);

        assertFalse(luke.isCaptive());
        assertFront(objective);

        scn.MoveCardsToLocation(otherSite, vader);
        advanceTo(scn, Phase.DEPLOY);

        assertTrue("The table-change proxy must capture Luke",
                luke.isCaptive());
        assertEquals(vader, luke.getEscort());
        assertEquals(vader, luke.getAttachedTo());
        assertBack(objective);
    }

    @Test
    public void vaderAlreadyEscortingACaptiveCannotSelfSeizeLuke() {
        VirtualTableScenario scn = scenario();
        PhysicalCardImpl objective = scn.GetDSCard("bhbm");
        PhysicalCardImpl throne = scn.GetDSCard("throne");
        PhysicalCardImpl luke = scn.GetLSCard("luke");
        PhysicalCardImpl otherCaptive =
                scn.GetLSCard("otherCaptive");
        PhysicalCardImpl vader = scn.GetDSCard("vader");

        scn.StartGame();
        scn.MoveCardsToLocation(
                throne, vader, otherCaptive, luke);
        scn.CaptureCardWith(vader, otherCaptive);
        advanceTo(scn, Phase.CONTROL);

        assertTrue(otherCaptive.isCaptive());
        assertEquals(vader, otherCaptive.getEscort());
        assertFalse("Vader may not seize a second captive",
                luke.isCaptive());
        assertNull(luke.getEscort());
        assertFront(objective);
    }

    @Test
    public void thereIsAnotherRetargetsCaptureFlipAndDuelToLeia() {
        VirtualTableScenario scn = scenario();
        PhysicalCardImpl objective = scn.GetDSCard("bhbm");
        PhysicalCardImpl throne = scn.GetDSCard("throne");
        PhysicalCardImpl destiny = scn.GetDSCard("destiny");
        PhysicalCardImpl shield =
                scn.GetLSCard("thereIsAnother");
        PhysicalCardImpl luke = scn.GetLSCard("luke");
        PhysicalCardImpl leia = scn.GetLSCard("leia");
        PhysicalCardImpl vader = scn.GetDSCard("vader");
        PhysicalCardImpl emperor = scn.GetDSCard("emperor");
        PhysicalCardImpl trooper = scn.GetDSCard("trooper");

        scn.StartGame();
        scn.AttachCardsTo(destiny, shield);
        assertTrue(GameConditions.hasGameTextModification(
                scn.game(),
                objective,
                ModifyGameTextType
                        .BRING_HIM_BEFORE_ME__TARGETS_LEIA_INSTEAD_OF_LUKE));

        scn.MoveCardsToLocation(
                scn.GetLSStartingLocation(), trooper, luke);
        scn.CaptureCardWith(trooper, luke);
        advanceTo(scn, Phase.CONTROL);
        assertTrue(luke.isCaptive());
        assertFront(objective);

        scn.MoveCardsToLocation(
                throne, vader, emperor, leia);
        advanceTo(scn, Phase.DEPLOY);

        assertTrue(leia.isCaptive());
        assertEquals(vader, leia.getEscort());
        assertBack(objective);
        assertTrue(scn.DSCardActionAvailable(
                objective, LEIA_DUEL_ACTION));
        assertFalse(scn.DSCardActionAvailable(
                objective, LUKE_DUEL_ACTION));
    }

    @Test
    public void kananRetargetBranchUsesKananForCaptureFlipAndDuel() {
        VirtualTableScenario scn = scenario();
        PhysicalCardImpl objective = scn.GetDSCard("bhbm");
        PhysicalCardImpl throne = scn.GetDSCard("throne");
        PhysicalCardImpl luke = scn.GetLSCard("luke");
        PhysicalCardImpl kanan = scn.GetLSCard("kanan");
        PhysicalCardImpl vader = scn.GetDSCard("vader");
        PhysicalCardImpl emperor = scn.GetDSCard("emperor");
        PhysicalCardImpl trooper = scn.GetDSCard("trooper");

        scn.StartGame();
        // Kanan, Rebel Infiltrator is not in the card registry. Install the
        // exact modifier that its objective download callback installs.
        addKananRetarget(scn, kanan);
        assertTrue(GameConditions.hasGameTextModification(
                scn.game(),
                objective,
                ModifyGameTextType
                        .BRING_HIM_BEFORE_ME__TARGETS_KANAN_INSTEAD_OF_LUKE));

        scn.MoveCardsToLocation(
                scn.GetLSStartingLocation(), trooper, luke);
        scn.CaptureCardWith(trooper, luke);
        advanceTo(scn, Phase.CONTROL);
        assertTrue(luke.isCaptive());
        assertFront(objective);

        scn.MoveCardsToLocation(
                throne, vader, emperor, kanan);
        advanceTo(scn, Phase.DEPLOY);

        assertTrue(kanan.isCaptive());
        assertEquals(vader, kanan.getEscort());
        assertBack(objective);
        assertTrue(scn.DSCardActionAvailable(
                objective, KANAN_DUEL_ACTION));
        assertFalse(scn.DSCardActionAvailable(
                objective, LUKE_DUEL_ACTION));
    }

    @Test
    public void backHoldsForCaptiveOrPresentWithVaderThenFlipsForNeither() {
        VirtualTableScenario scn = scenario();
        PhysicalCardImpl objective = scn.GetDSCard("bhbm");
        PhysicalCardImpl throne = scn.GetDSCard("throne");
        PhysicalCardImpl otherSite = scn.GetLSStartingLocation();
        PhysicalCardImpl luke = scn.GetLSCard("luke");
        PhysicalCardImpl otherCaptive =
                scn.GetLSCard("otherCaptive");
        PhysicalCardImpl vader = scn.GetDSCard("vader");
        PhysicalCardImpl emperor = scn.GetDSCard("emperor");
        PhysicalCardImpl trooper = scn.GetDSCard("trooper");

        scn.StartGame();
        scn.MoveCardsToLocation(
                throne, vader, otherCaptive, trooper, luke);
        scn.CaptureCardWith(vader, otherCaptive);
        scn.CaptureCardWith(trooper, luke);
        advanceTo(scn, Phase.CONTROL);
        assertBack(objective);

        scn.MoveCardsToLocation(throne, emperor);
        advanceTo(scn, Phase.DEPLOY);
        assertTrue(luke.isCaptive());
        assertBack(objective);

        scn.MoveCardsToLocation(throne, luke);
        advanceTo(scn, Phase.MOVE);
        assertFalse(luke.isCaptive());
        assertAtLocation(throne, luke, vader);
        assertTrue(vader.getCardsEscorting().contains(
                otherCaptive));
        assertBack(objective);

        scn.MoveCardsToLocation(otherSite, vader);
        advanceTo(scn, Phase.DRAW);
        assertFalse(luke.isCaptive());
        assertAtLocation(throne, luke);
        assertAtLocation(otherSite, vader);
        assertFront(objective);
    }

    @Test
    public void backAlwaysMakesItsOwnerLoseExactlyOneAtEndOfOwnTurn() {
        VirtualTableScenario scn = scenario();
        PhysicalCardImpl objective = scn.GetDSCard("bhbm");
        PhysicalCardImpl throne = scn.GetDSCard("throne");
        PhysicalCardImpl luke = scn.GetLSCard("luke");
        PhysicalCardImpl trooper = scn.GetDSCard("trooper");

        scn.StartGame();
        scn.MoveCardsToLocation(throne, trooper, luke);
        scn.CaptureCardWith(trooper, luke);
        advanceTo(scn, Phase.CONTROL);
        assertBack(objective);

        scn.SkipToPhase(Phase.DRAW);
        int lifeBeforeLoss = scn.GetDSLifeForceRemaining();
        scn.DSPass();
        scn.LSPass();
        scn.PassAllResponses();

        assertTrue(scn.AwaitingDSForceLossPayment());
        scn.DSPayRemainingForceLossFromReserveDeck();
        assertEquals("The back-side clock is unconditional and exactly 1",
                lifeBeforeLoss - 1,
                scn.GetDSLifeForceRemaining());
    }

    @Test
    public void thereIsAnotherCancelsTheBackSideForceDrip() {
        VirtualTableScenario scn = scenario();
        PhysicalCardImpl objective = scn.GetDSCard("bhbm");
        PhysicalCardImpl throne = scn.GetDSCard("throne");
        PhysicalCardImpl destiny = scn.GetDSCard("destiny");
        PhysicalCardImpl shield =
                scn.GetLSCard("thereIsAnother");
        PhysicalCardImpl leia = scn.GetLSCard("leia");
        PhysicalCardImpl vader = scn.GetDSCard("vader");

        scn.StartGame();
        scn.AttachCardsTo(destiny, shield);
        scn.MoveCardsToLocation(throne, vader, leia);
        advanceTo(scn, Phase.CONTROL);

        assertTrue(leia.isCaptive());
        assertEquals(vader, leia.getEscort());
        assertBack(objective);

        scn.SkipToPhase(Phase.DRAW);
        int lifeBeforeEndTurn = scn.GetDSLifeForceRemaining();
        scn.DSPass();
        scn.LSPass();
        scn.PassAllResponses();

        assertFalse(scn.AwaitingDSForceLossPayment());
        assertEquals(lifeBeforeEndTurn,
                scn.GetDSLifeForceRemaining());
    }

    @Test
    public void duelRequiresVaderTargetAndEmperorAtOwnThroneRoom() {
        VirtualTableScenario scn = scenario();
        PhysicalCardImpl objective = scn.GetDSCard("bhbm");
        PhysicalCardImpl throne = scn.GetDSCard("throne");
        PhysicalCardImpl otherSite = scn.GetLSStartingLocation();
        PhysicalCardImpl luke = scn.GetLSCard("luke");
        PhysicalCardImpl vader = scn.GetDSCard("vader");
        PhysicalCardImpl emperor = scn.GetDSCard("emperor");

        scn.StartGame();
        scn.MoveCardsToLocation(throne, vader, luke);
        scn.MoveCardsToLocation(otherSite, emperor);
        advanceTo(scn, Phase.CONTROL);

        assertTrue(luke.isCaptive());
        assertBack(objective);
        assertFalse(scn.DSCardActionAvailable(
                objective, LUKE_DUEL_ACTION));

        scn.MoveCardsToLocation(throne, emperor);
        advanceTo(scn, Phase.DEPLOY);

        assertAtLocation(throne, vader, emperor);
        assertEquals(VirtualTableScenario.DS, throne.getOwner());
        assertTrue(scn.DSCardActionAvailable(
                objective, LUKE_DUEL_ACTION));
    }

    @Test
    public void modifierAdjustedExactTwelveFailsCrossover() {
        VirtualTableScenario scn = flippedDuelScenario(6);
        PhysicalCardImpl objective = scn.GetDSCard("bhbm");
        PhysicalCardImpl luke = scn.GetLSCard("luke");

        assertEquals(12f,
                scn.game().getModifiersQuerying()
                        .getCrossoverAttemptTotal(
                                scn.gameState(), luke, 6f),
                scn.epsilon);
        int lightLifeBefore = scn.GetLSLifeForceRemaining();

        scn.DSUseCardAction(objective, LUKE_DUEL_ACTION);
        scn.PassAllResponses();
        scn.PassAllResponses();

        assertFalse("The source comparison is strict: 12 fails",
                luke.isCrossedOver());
        assertEquals(VirtualTableScenario.LS, luke.getOwner());
        assertEquals(lightLifeBefore,
                scn.GetLSLifeForceRemaining());
        assertFalse(GameConditions.isOnceDuringYourTurn(
                scn.game(),
                objective,
                VirtualTableScenario.DS,
                objective.getCardId(),
                GameTextActionId.OTHER_CARD_ACTION_2));
    }

    @Test
    public void modifierAdjustedThirteenCrossesTargetAtStrictBoundary() {
        VirtualTableScenario scn = flippedDuelScenario(7);
        PhysicalCardImpl objective = scn.GetDSCard("bhbm");
        PhysicalCardImpl luke = scn.GetLSCard("luke");

        assertEquals(13f,
                scn.game().getModifiersQuerying()
                        .getCrossoverAttemptTotal(
                                scn.gameState(), luke, 7f),
                scn.epsilon);

        scn.DSUseCardAction(objective, LUKE_DUEL_ACTION);
        scn.PassAllResponses();
        finishCrossoverResponses(scn);

        assertTrue("A modifier-adjusted total above 12 crosses Luke",
                luke.isCrossedOver());
        assertEquals(VirtualTableScenario.DS, luke.getOwner());
        // The two duel destiny cards return to Used Pile after the card's
        // DepleteLifeForceEffect resolves. This contract therefore pins the
        // strict crossover boundary without freezing that cleanup ordering.
    }
}
