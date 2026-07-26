package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.BattleDecisionPolicy;
import com.gempukku.swccgo.ai.models.common.phase.TdigwattObjectiveFacts;
import com.gempukku.swccgo.ai.models.common.phase.TdigwattObjectiveFactsReader;
import com.gempukku.swccgo.ai.models.common.phase.TdigwattObjectiveScoringPolicy;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.PhysicalCardImpl;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import static com.gempukku.swccgo.framework.TestBase.DS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Caller-level proof that the shared battle policy reads the live TDIGWATT
 * back side and exact source-defined participants before adding its payoff.
 */
public class TdigwattBattleDecisionBehaviorTest {
    private static final String CLASSIC_PAYOFF_RULE =
            "TDIGWATT.109_12.BACK.BATTLE_PAYOFF";

    private static final StartingSetup CLASSIC_TDIGWATT =
            new StartingSetup() {
                @Override
                public HashMap<String, String> Cards() {
                    return new HashMap<>() {{
                        put("objective", "109_12");
                        put("setupSite", "7_270");
                    }};
                }

                @Override
                public void Setup(VirtualTableScenario scn) {
                    // setupSite is the only setup-eligible site.
                }
            };

    @Test
    public void sharedCallerRewardsExactSafePairButNotUnsafeBattle() {
        VirtualTableScenario scn = startFlippedClassicBattleBoard();
        PhysicalCardImpl objective = scn.GetDSCard("objective");
        PhysicalCardImpl battleSite = scn.GetDSCard("battleSite");

        assertTrue("The real classic objective must be back-side up",
                objective.isFlipped());
        TdigwattObjectiveFacts.BattleFacts liveFacts =
                TdigwattObjectiveFactsReader
                    .readBackSideBattleFactsAtLocation(
                        scn.game(), DS, battleSite)
                    .orElseThrow();
        assertEquals(TdigwattObjectiveFacts.Printing.CLASSIC,
                liveFacts.objective().printing());
        assertTrue("Djas Puhr must satisfy the owned alien half",
                liveFacts.yourAlienInBattle());
        assertTrue("Grand Moff Tarkin must satisfy the owned Imperial half",
                liveFacts.yourImperialInBattle());
        assertFalse("Djas Puhr is not an Ugnaught",
                liveFacts.yourUgnaughtInBattle());

        BattleDecisionPolicy.ScoredAction safe =
                onlyBattle(BattleDecisionPolicy.evaluate(
                    context(scn, battleSite, true)));
        BattleDecisionPolicy.Contribution payoff =
                contribution(safe, CLASSIC_PAYOFF_RULE);
        assertEquals(
                "The shared caller must apply the source's +2 destiny payoff",
                2 * TdigwattObjectiveScoringPolicy
                    .BATTLE_DESTINY_POINT_BONUS,
                payoff.delta(), 0.0f);
        assertFalse(payoff.hardVeto());

        BattleDecisionPolicy.ScoredAction unsafe =
                onlyBattle(BattleDecisionPolicy.evaluate(
                    context(scn, battleSite, false)));
        assertFalse(
                "A predictor-negative battle must receive no objective bonus",
                hasContribution(unsafe, CLASSIC_PAYOFF_RULE));
    }

    private static VirtualTableScenario startFlippedClassicBattleBoard() {
        VirtualTableScenario scn = scenario();
        scn.MoveCardsToLSHand(scn.GetLSCard("luke"));
        scn.MoveCardsToDSHand(
                scn.GetDSCard("bespin"),
                scn.GetDSCard("cloudCity"),
                scn.GetDSCard("site2"),
                scn.GetDSCard("site3"),
                scn.GetDSCard("battleSite"),
                scn.GetDSCard("darkDeal"),
                scn.GetDSCard("tieSystem"),
                scn.GetDSCard("obsidianSector"),
                scn.GetDSCard("controller1"),
                scn.GetDSCard("controller2"),
                scn.GetDSCard("controller3"),
                scn.GetDSCard("djas"),
                scn.GetDSCard("tarkin"));
        scn.StartGame();

        PhysicalCardImpl bespin = scn.GetDSCard("bespin");
        PhysicalCardImpl cloudCity = scn.GetDSCard("cloudCity");
        PhysicalCardImpl darkDeal = scn.GetDSCard("darkDeal");
        scn.MoveLocationToTable(bespin);
        scn.MoveLocationToTable(cloudCity);
        scn.MoveLocationToTable(scn.GetDSCard("site2"));
        scn.MoveLocationToTable(scn.GetDSCard("site3"));
        scn.MoveLocationToTable(scn.GetDSCard("battleSite"));
        scn.MoveCardsToLocation(
                bespin, scn.GetDSCard("tieSystem"));
        scn.MoveCardsToLocation(
                cloudCity, scn.GetDSCard("obsidianSector"));
        scn.MoveCardsToLocation(
                scn.GetDSCard("setupSite"),
                scn.GetDSCard("controller1"));
        scn.MoveCardsToLocation(
                scn.GetDSCard("site2"),
                scn.GetDSCard("controller2"));
        scn.MoveCardsToLocation(
                scn.GetDSCard("site3"),
                scn.GetDSCard("controller3"));

        scn.DSActivateForceCheat(12);
        scn.SkipToPhase(Phase.DEPLOY);
        assertTrue(GameConditions.controls(
                scn.game(), DS, cloudCity));
        assertTrue(GameConditions.controls(
                scn.game(), DS, 3,
                Filters.relatedSiteTo(
                    darkDeal, Filters.Bespin_Cloud_City)));
        assertTrue(scn.DSDeployAvailable(darkDeal));
        scn.DSDeployCardAndPassResponses(darkDeal, cloudCity);
        if (scn.AwaitingLSDeployPhaseActions()) {
            scn.LSPass();
        }
        assertTrue(
                "The legal Dark Deal deployment must trigger the real flip",
                scn.GetDSCard("objective").isFlipped());

        scn.MoveCardsToLocation(
                scn.GetDSCard("battleSite"),
                scn.GetDSCard("djas"),
                scn.GetDSCard("tarkin"),
                scn.GetLSCard("luke"));
        return scn;
    }

    private static VirtualTableScenario scenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("luke", "1_19");
                }},
                new HashMap<>() {{
                    put("bespin", "5_164");
                    put("cloudCity", "5_165");
                    put("site2", "5_166");
                    put("site3", "5_167");
                    put("battleSite", "5_168");
                    put("darkDeal", "5_115");
                    put("tieSystem", "1_304");
                    put("obsidianSector", "5_175");
                    put("controller1", "1_194");
                    put("controller2", "1_194");
                    put("controller3", "1_194");
                    put("djas", "1_171");
                    put("tarkin", "1_179");
                }},
                30,
                30,
                StartingSetup.DoNothingSetup,
                CLASSIC_TDIGWATT,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open);
    }

    private static BattleDecisionPolicy.Context context(
            VirtualTableScenario scn,
            PhysicalCard battleSite,
            boolean predictorSafe) {
        return new BattleDecisionPolicy.Context() {
            @Override
            public String getDecisionType() {
                return "CARD_ACTION_CHOICE";
            }

            @Override
            public Phase getPhase() {
                return Phase.BATTLE;
            }

            @Override
            public String getDecisionText() {
                return "Choose battle action";
            }

            @Override
            public List<String> getActionIds() {
                return List.of("classic-battle");
            }

            @Override
            public List<String> getActionTexts() {
                return List.of(
                        "Initiate battle at "
                            + battleSite.getTitle());
            }

            @Override
            public List<String> getCardIds() {
                return List.of(Integer.toString(
                        battleSite.getCardId()));
            }

            @Override
            public GameState getGameState() {
                return scn.gameState();
            }

            @Override
            public SwccgGame getGame() {
                return scn.game();
            }

            @Override
            public String getPlayerId() {
                return DS;
            }

            @Override
            public int getReserveDeckSize() {
                return 20;
            }

            @Override
            public int getLifeForce() {
                return 20;
            }

            @Override
            public int getForcePileSize() {
                return 10;
            }

            @Override
            public int getHandSize() {
                return 0;
            }

            @Override
            public ObjectiveAnalyzer getObjectiveAnalyzer() {
                return null;
            }

            @Override
            public float getVaderExpendabilityFactor() {
                return 1.0f;
            }

            @Override
            public int getCriticalLifeForce() {
                return 7;
            }

            @Override
            public BattleDecisionPolicy.Prediction predictBattle(
                    int myPower,
                    int myDestinyDraws,
                    int opponentPower,
                    int opponentDestinyDraws) {
                return predictorSafe
                        ? new BattleDecisionPolicy.Prediction(
                            0.80f, 4.0f, 1.0f)
                        : new BattleDecisionPolicy.Prediction(
                            0.20f, 1.0f, 4.0f);
            }

            @Override
            public Logger getLogger() {
                return LogManager.getLogger(
                        TdigwattBattleDecisionBehaviorTest.class);
            }
        };
    }

    private static BattleDecisionPolicy.ScoredAction onlyBattle(
            List<BattleDecisionPolicy.ScoredAction> actions) {
        assertEquals(1, actions.size());
        return actions.get(0);
    }

    private static BattleDecisionPolicy.Contribution contribution(
            BattleDecisionPolicy.ScoredAction action,
            String ruleId) {
        return action.contributions().stream()
                .filter(candidate ->
                    candidate.ruleArmId() != null
                        && ruleId.equals(
                            candidate.ruleArmId().id()))
                .findFirst()
                .orElseThrow();
    }

    private static boolean hasContribution(
            BattleDecisionPolicy.ScoredAction action,
            String ruleId) {
        return action.contributions().stream()
                .anyMatch(candidate ->
                    candidate.ruleArmId() != null
                        && ruleId.equals(
                            candidate.ruleArmId().id()));
    }
}
