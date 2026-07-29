package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.game.PhysicalCardImpl;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Extension batch (2026-07-29): native engine contract for the Rebel
 * Strike Team family — classic 8_78 / Garrison Destroyed and playtest
 * 501_94 (V). Card Java unchanged. NOT TWINS (the gap matrix's twin note
 * was wrong): they share ONLY the Bunker-blown-away front leg. The classic
 * adds route B (during YOUR move phase, control three exterior Endor sites
 * each with TWO of your Rebel scouts — Card8_078.java L113-L115), an
 * opponent-MOVE-phase flip-back window with a permanent post-blow-away
 * lock (Card8_078_BACK.java L88-L93), and an Endor-blown-away hard-loss on
 * both sides; the V has none of those, and its back flips back only while
 * an opponent controls a spottable (never-blown-away) Bunker.
 *
 * This family exercises all three new schema primitives at once: the
 * blownAway relation (latched snapshot reader), the gamePhase/whosePhase
 * window, and the minActorsPerLocation pair floor.
 */
public class RebelStrikeTeamFamilyObjectiveEngineContractTest {

    // ==================== classic 8_78 ====================

    private static final StartingSetup RST_CLASSIC = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "8_78");
                put("endor", "8_68");
                put("landingSite", "8_77");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            for (int i = 0; i < 8; i++) {
                if (scn.LSDecisionAvailable("On which side")) {
                    scn.LSChoose("Left");
                } else if (scn.LSDecisionAvailable("to deploy")) {
                    if (scn.GetLSCard("endor").getZone() == Zone.RESERVE_DECK) {
                        scn.LSChooseCard(scn.GetLSCard("endor"));
                    } else {
                        scn.LSChooseCard(scn.GetLSCard("landingSite"));
                    }
                }
            }
        }
    };

    private VirtualTableScenario classicScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("denseForest", "8_72");
                    put("greatForest", "8_74");
                    put("bunker", "8_70");
                    put("dtsg", "8_43");
                    put("scout1", "8_9");
                    put("scout2", "8_9");
                    put("scout3", "8_9");
                    put("scout4", "8_9");
                    put("scout5", "8_9");
                    put("scout6", "8_9");
                }},
                new HashMap<>() {{
                }},
                24,
                24,
                RST_CLASSIC,
                StartingSetup.DefaultDSGroundLocation,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    private void moveSiteToSystem(
            VirtualTableScenario scn, PhysicalCardImpl site, String system) {
        scn.RemoveCardZone(site);
        var placements = scn.gameState().getLocationPlacement(
                scn.game(), site, system, null);
        assertFalse("Expected a legal placement at " + system,
                placements.isEmpty());
        scn.gameState().addLocationToTable(
                scn.game(), site, placements.getFirst());
    }

    /** After a move resolves, hand the window back to the mover. */
    private void settleAfterMove(VirtualTableScenario scn) {
        scn.PassAllResponses();
        if (scn.LSGetDecision() == null && scn.DSGetDecision() != null) {
            scn.DSPass();
        }
    }

    /**
     * Blowing away an OCCUPIED Bunker (unlike the barely-held Endor
     * fixture) surfaces "put on Lost Pile" card choices for the collapsing
     * site's occupants and attached Epic Event, then the opponent's 8-Force
     * loss. Answer each as it appears until the table settles.
     */
    private void resolveBlowAwayAftermath(VirtualTableScenario scn) {
        for (int i = 0; i < 12; i++) {
            var ls = scn.LSGetDecision();
            if (ls != null && ls.getText() != null
                    && ls.getText().contains("Lost Pile")) {
                String[] ids = ls.getDecisionParameters().get("cardId");
                if (ids != null && ids.length > 0) {
                    scn.PlayerDecided(VirtualTableScenario.LS, ids[0]);
                    continue;
                }
            }
            var ds = scn.DSGetDecision();
            if (ds != null && ds.getText() != null
                    && ds.getText().toLowerCase().contains("force")
                    && ds.getText().toLowerCase().contains("lose")) {
                scn.DSPayRemainingForceLossFromReserveDeck();
                continue;
            }
            var current = scn.GetCurrentDecision();
            if (current != null && current.getText() != null
                    && current.getText().toLowerCase()
                            .contains("optional response")) {
                scn.PassAllResponses();
                continue;
            }
            return;
        }
    }

    @Test
    public void rstClassicRouteBNeedsThePairsAndYourMovePhase() {
        var scn = classicScenario();
        var objective = scn.GetLSCard("objective");
        var landingSite = scn.GetLSCard("landingSite");
        var denseForest = scn.GetLSCard("denseForest");
        var greatForest = scn.GetLSCard("greatForest");
        var pulseOne = scn.GetLSFiller(1);

        scn.MoveCardsToLSHand(pulseOne);
        scn.StartGame();
        moveSiteToSystem(scn, denseForest, Title.Endor);
        moveSiteToSystem(scn, greatForest, Title.Endor);
        // Three controlled exterior Endor sites, each with a scout PAIR —
        // route B is complete except for the phase window. The third
        // scout at the landing site is the mobile piece.
        scn.MoveCardsToLocation(landingSite, scn.GetLSCard("scout1"),
                scn.GetLSCard("scout2"), scn.GetLSCard("scout3"));
        scn.MoveCardsToLocation(denseForest, scn.GetLSCard("scout4"),
                scn.GetLSCard("scout5"));
        scn.MoveCardsToLocation(greatForest, scn.GetLSCard("scout6"));

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);

        // Complete board, WRONG phase: a deploy-phase table change must not
        // flip (the card's route B fires only during your move phase).
        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertFalse("Route B outside your move phase must not flip (with the third pair also missing)",
                objective.isFlipped());
        scn.DSPass();

        scn.SkipToPhase(Phase.MOVE);
        // The skip can land with the opponent holding the window; hand it
        // back before acting.
        if (scn.LSGetDecision() == null) {
            scn.DSPass();
        }
        // Pair-floor near miss inside the right phase: the great forest
        // holds ONE scout. Moving a spare scout off the landing site's
        // trio changes the table during YOUR move phase without completing
        // the third pair.
        scn.LSMoveCard(scn.GetLSCard("scout3"), denseForest);
        settleAfterMove(scn);
        assertFalse("Two pair-sites plus a singleton must not flip even in your move phase",
                objective.isFlipped());

        // A DIFFERENT scout completes the third pair (a card regular-moves
        // once per turn).
        scn.LSMoveCard(scn.GetLSCard("scout4"), greatForest);
        settleAfterMove(scn);
        assertTrue("Three pair-sites during your move phase must flip (six scouts, not three)",
                objective.isFlipped());
    }

    @Test
    public void rstClassicRouteAFlipsWhenTheBunkerIsBlownAway() {
        var scn = classicScenario();
        var objective = scn.GetLSCard("objective");
        var bunker = scn.GetLSCard("bunker");
        var dtsg = scn.GetLSCard("dtsg");

        scn.MoveCardsToLSHand(dtsg);
        scn.StartGame();
        moveSiteToSystem(scn, bunker, Title.Endor);
        // Control the Bunker with Rebels (scouts are Rebels): the DTSG
        // deploy and blow-away gates.
        scn.MoveCardsToLocation(bunker, scn.GetLSCard("scout1"),
                scn.GetLSCard("scout2"));

        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(dtsg, bunker);
        assertTrue(scn.IsAttachedTo(bunker, dtsg));

        scn.SkipToLSTurn(Phase.CONTROL);
        assertTrue(scn.LSCardActionAvailable(dtsg,
                "Attempt to 'blow away' Bunker"));
        scn.PrepareLSDestiny(6);
        scn.PrepareLSDestiny(7);
        scn.LSUseCardAction(dtsg, "Attempt to 'blow away' Bunker");
        scn.PassAllResponses();
        resolveBlowAwayAftermath(scn);

        assertTrue("A destiny total of 13 must actually blow away the Bunker",
                bunker.isBlownAway());
        assertTrue("The Bunker blown away must flip the classic front (route A, any phase)",
                objective.isFlipped());
    }

    @Test
    public void rstClassicProfileTracksPhaseWindowPairFloorAndLatchedBlowAway() {
        var scn = classicScenario();
        var landingSite = scn.GetLSCard("landingSite");
        var denseForest = scn.GetLSCard("denseForest");
        var greatForest = scn.GetLSCard("greatForest");

        scn.StartGame();
        moveSiteToSystem(scn, denseForest, Title.Endor);
        moveSiteToSystem(scn, greatForest, Title.Endor);
        scn.MoveCardsToLocation(landingSite, scn.GetLSCard("scout1"),
                scn.GetLSCard("scout2"));
        scn.MoveCardsToLocation(denseForest, scn.GetLSCard("scout3"),
                scn.GetLSCard("scout4"));
        scn.MoveCardsToLocation(greatForest, scn.GetLSCard("scout5"),
                scn.GetLSCard("scout6"));

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertTrue("Profile must hydrate for 8_78", analyzer.isAnalyzed());

        var preFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertEquals("The classic front encodes one two-route anyOf rule", 1,
                preFlip.size());
        assertFalse("Outside your move phase the complete pair board leaves the rule unmet (phase window)",
                preFlip.get(0).conditionSatisfied());

        scn.LSActivateForceCheat(4);
        scn.SkipToLSTurn(Phase.MOVE);
        var inWindow = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertTrue("Inside your move phase the three pair-sites complete the encoded law",
                inWindow.get(0).conditionSatisfied());

        var postFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "postFlip", "flipBack");
        assertEquals("The classic back encodes the window/lock rule plus the anyOf control rule", 2,
                postFlip.size());
    }

    // ==================== playtest 501_94 (V) ====================

    private static final StartingSetup RST_VIRTUAL = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "501_94");
                put("endor", "8_68");
                put("landingSite", "8_77");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            for (int i = 0; i < 8; i++) {
                if (scn.LSDecisionAvailable("On which side")) {
                    scn.LSChoose("Left");
                } else if (scn.LSDecisionAvailable("to deploy")) {
                    if (scn.GetLSCard("endor").getZone() == Zone.RESERVE_DECK) {
                        scn.LSChooseCard(scn.GetLSCard("endor"));
                    } else {
                        scn.LSChooseCard(scn.GetLSCard("landingSite"));
                    }
                }
            }
        }
    };

    private VirtualTableScenario virtualScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("denseForest", "8_72");
                    put("greatForest", "8_74");
                    put("bunker", "8_70");
                    put("dtsg", "8_43");
                    put("scout1", "8_9");
                    put("scout2", "8_9");
                    put("scout3", "8_9");
                    put("scout4", "8_9");
                    put("scout5", "8_9");
                    put("scout6", "8_9");
                }},
                new HashMap<>() {{
                }},
                24,
                24,
                RST_VIRTUAL,
                StartingSetup.DefaultDSGroundLocation,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    @Test
    public void rstVirtualIgnoresRouteBAndFlipsOnlyOnTheBlowAway() {
        var scn = virtualScenario();
        var objective = scn.GetLSCard("objective");
        var landingSite = scn.GetLSCard("landingSite");
        var denseForest = scn.GetLSCard("denseForest");
        var greatForest = scn.GetLSCard("greatForest");
        var bunker = scn.GetLSCard("bunker");
        var dtsg = scn.GetLSCard("dtsg");

        scn.MoveCardsToLSHand(dtsg);
        scn.StartGame();
        moveSiteToSystem(scn, denseForest, Title.Endor);
        moveSiteToSystem(scn, greatForest, Title.Endor);
        moveSiteToSystem(scn, bunker, Title.Endor);
        // The classic's ENTIRE route B board: three pair-sites. The V must
        // ignore it — the false-sibling proof.
        scn.MoveCardsToLocation(landingSite, scn.GetLSCard("scout1"),
                scn.GetLSCard("scout2"));
        scn.MoveCardsToLocation(denseForest, scn.GetLSCard("scout3"),
                scn.GetLSCard("scout4"));
        scn.MoveCardsToLocation(greatForest, scn.GetLSCard("scout5"),
                scn.GetLSCard("scout6"));

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.MOVE);
        if (scn.LSGetDecision() == null) {
            scn.DSPass();
        }
        scn.LSMoveCard(scn.GetLSCard("scout3"), landingSite);
        settleAfterMove(scn);
        assertFalse("The V has NO pair route: the classic's complete route B board must not flip it",
                objective.isFlipped());

        // Its sole route: the Bunker blown away. Rebels must first hold it.
        scn.LSMoveCard(scn.GetLSCard("scout1"), denseForest);
        scn.MoveCardsToLocation(bunker, scn.GetLSCard("scout1"),
                scn.GetLSCard("scout2"));
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(dtsg, bunker);
        scn.SkipToLSTurn(Phase.CONTROL);
        scn.PrepareLSDestiny(6);
        scn.PrepareLSDestiny(7);
        scn.LSUseCardAction(dtsg, "Attempt to 'blow away' Bunker");
        scn.PassAllResponses();
        resolveBlowAwayAftermath(scn);

        assertTrue("The Bunker must be blown away", bunker.isBlownAway());
        assertTrue("The blow-away must flip the V front (its sole route)",
                objective.isFlipped());
    }

    @Test
    public void rstVirtualProfileHydratesItsOwnRulesNotTheClassics() {
        var scn = virtualScenario();
        var landingSite = scn.GetLSCard("landingSite");
        var denseForest = scn.GetLSCard("denseForest");
        var greatForest = scn.GetLSCard("greatForest");

        scn.StartGame();
        moveSiteToSystem(scn, denseForest, Title.Endor);
        moveSiteToSystem(scn, greatForest, Title.Endor);
        scn.MoveCardsToLocation(landingSite, scn.GetLSCard("scout1"),
                scn.GetLSCard("scout2"));
        scn.MoveCardsToLocation(denseForest, scn.GetLSCard("scout3"),
                scn.GetLSCard("scout4"));
        scn.MoveCardsToLocation(greatForest, scn.GetLSCard("scout5"),
                scn.GetLSCard("scout6"));

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertTrue("The authored profile must hydrate for 501_94",
                analyzer.isAnalyzed());

        var preFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertEquals("The V front encodes ONE blow-away-only rule", 1,
                preFlip.size());
        assertFalse("The classic's complete pair board leaves the V's encoded law unmet (hijack closed)",
                preFlip.get(0).conditionSatisfied());

        var postFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "postFlip", "flipBack");
        assertEquals("The V back encodes one opponent-controls-Bunker rule", 1,
                postFlip.size());
    }
}
