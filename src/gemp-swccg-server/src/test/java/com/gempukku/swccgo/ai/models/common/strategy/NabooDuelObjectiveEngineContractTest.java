package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.game.PhysicalCardImpl;
import com.gempukku.swccgo.logic.modifiers.ExcludedFromBattleModifier;
import org.junit.Test;

import java.util.HashMap;

import static com.gempukku.swccgo.framework.Assertions.assertInZone;
import static com.gempukku.swccgo.framework.TestBase.DS;
import static com.gempukku.swccgo.framework.TestBase.LS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Native engine contract for the mirrored Reflections III Naboo duel objectives. */
public class NabooDuelObjectiveEngineContractTest {
    private enum Family {
        LIGHT,
        DARK
    }

    private static final StartingSetup WELL_HANDLE_THIS = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "13_46");
                put("core", "13_32");
                put("generator", "13_31");
                put("duelEvent", "13_24");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            for (int i = 0; i < 8; i++) {
                if (scn.LSDecisionAvailable("On which side")) {
                    scn.LSChoose("Right");
                } else if (scn.LSDecisionAvailable("Choose Theed Palace Generator Core")) {
                    scn.LSChooseCard(scn.GetLSCard("core"));
                } else if (scn.LSDecisionAvailable("Choose Theed Palace Generator to deploy")) {
                    scn.LSChooseCard(scn.GetLSCard("generator"));
                } else if (scn.LSDecisionAvailable("Choose Inner Strength")) {
                    scn.LSChooseCard(scn.GetLSCard("duelEvent"));
                }
            }
        }
    };

    private static final StartingSetup LET_THEM_MAKE_THE_FIRST_MOVE = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "13_73");
                put("core", "13_77");
                put("generator", "13_76");
                put("duelEvent", "13_65");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            for (int i = 0; i < 8; i++) {
                if (scn.DSDecisionAvailable("On which side")) {
                    scn.DSChoose("Right");
                } else if (scn.DSDecisionAvailable("Choose Theed Palace Generator Core")) {
                    scn.DSChooseCard(scn.GetDSCard("core"));
                } else if (scn.DSDecisionAvailable("Choose Theed Palace Generator to deploy")) {
                    scn.DSChooseCard(scn.GetDSCard("generator"));
                } else if (scn.DSDecisionAvailable("Choose Deep Hatred")) {
                    scn.DSChooseCard(scn.GetDSCard("duelEvent"));
                }
            }
        }
    };

    private VirtualTableScenario scenario(Family family) {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("jedi", "13_33");
                    put("wrongLight", "1_28");
                    put("lsPulse1", "1_28");
                    put("lsPulse2", "1_28");
                    put("lsPulse3", "1_28");
                    put("swamp", "12_80");
                }},
                new HashMap<>() {{
                    put("darkJedi", "1_168");
                    put("wrongDark", "1_194");
                    put("dsPulse1", "1_194");
                    put("dsPulse2", "1_194");
                    put("dsPulse3", "1_194");
                }},
                24,
                24,
                family == Family.LIGHT
                        ? WELL_HANDLE_THIS
                        : StartingSetup.DefaultLSGroundLocation,
                family == Family.DARK
                        ? LET_THEM_MAKE_THE_FIRST_MOVE
                        : StartingSetup.DefaultDSGroundLocation,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    private void startInOwnersDeployPhase(VirtualTableScenario scn, Family family) {
        if (family == Family.LIGHT) {
            scn.MoveCardsToLSHand(
                    scn.GetLSCard("lsPulse1"),
                    scn.GetLSCard("lsPulse2"),
                    scn.GetLSCard("lsPulse3"));
        } else {
            scn.MoveCardsToDSHand(
                    scn.GetDSCard("dsPulse1"),
                    scn.GetDSCard("dsPulse2"),
                    scn.GetDSCard("dsPulse3"));
        }
        scn.StartGame();
        scn.MoveLocationToTable(scn.GetLSCard("swamp"));
        if (family == Family.LIGHT) {
            scn.SkipToLSTurn(Phase.DEPLOY);
            scn.LSActivateForceCheat(20);
        } else {
            scn.SkipToPhase(Phase.DEPLOY);
            scn.DSActivateForceCheat(20);
        }
    }

    private PhysicalCardImpl objective(VirtualTableScenario scn, Family family) {
        return family == Family.LIGHT
                ? scn.GetLSCard("objective")
                : scn.GetDSCard("objective");
    }

    private PhysicalCardImpl gate(VirtualTableScenario scn, Family family) {
        return family == Family.LIGHT
                ? scn.GetLSCard("generator")
                : scn.GetDSCard("generator");
    }

    private PhysicalCardImpl qualifyingOpponent(
            VirtualTableScenario scn,
            Family family) {
        return family == Family.LIGHT
                ? scn.GetDSCard("darkJedi")
                : scn.GetLSCard("jedi");
    }

    private PhysicalCardImpl ownDuelist(
            VirtualTableScenario scn,
            Family family) {
        return family == Family.LIGHT
                ? scn.GetLSCard("jedi")
                : scn.GetDSCard("darkJedi");
    }

    private void pulseTable(
            VirtualTableScenario scn,
            Family family,
            int pulseNumber) {
        if (family == Family.LIGHT) {
            scn.LSDeployCardAndPassResponses(
                    scn.GetLSCard("lsPulse" + pulseNumber),
                    gate(scn, family));
        } else {
            scn.DSDeployCardAndPassResponses(
                    scn.GetDSCard("dsPulse" + pulseNumber),
                    gate(scn, family));
        }
    }

    private void restoreOwnersDeployDecision(
            VirtualTableScenario scn,
            Family family) {
        if (family == Family.LIGHT && scn.AwaitingDSDeployPhaseActions()) {
            scn.DSPass();
        } else if (family == Family.DARK && scn.AwaitingLSDeployPhaseActions()) {
            scn.LSPass();
        }
    }

    private void assertDoesNotFlip(
            Family family,
            String actorAlias,
            boolean actorIsLight,
            boolean exteriorNaboo) {
        var scn = scenario(family);
        startInOwnersDeployPhase(scn, family);
        var actor = actorIsLight
                ? scn.GetLSCard(actorAlias)
                : scn.GetDSCard(actorAlias);
        var location = exteriorNaboo
                ? scn.GetLSCard("swamp")
                : gate(scn, family);
        scn.MoveCardsToLocation(location, actor);

        pulseTable(scn, family, 1);

        assertFalse(objective(scn, family).isFlipped());
    }

    @Test
    public void bothFrontsRequireTheExactOpponentActorAtAnInteriorNabooBattleground() {
        for (Family family : Family.values()) {
            var valid = scenario(family);
            startInOwnersDeployPhase(valid, family);
            var gate = gate(valid, family);
            assertTrue(Filters.interior_Naboo_site.accepts(valid.game(), gate));
            assertTrue(Filters.battleground_site.accepts(valid.game(), gate));
            valid.MoveCardsToLocation(gate, qualifyingOpponent(valid, family));

            pulseTable(valid, family, 1);

            assertTrue(objective(valid, family).isFlipped());

            assertDoesNotFlip(
                    family,
                    family == Family.LIGHT ? "darkJedi" : "jedi",
                    family == Family.DARK,
                    true);
            assertDoesNotFlip(
                    family,
                    family == Family.LIGHT ? "wrongDark" : "wrongLight",
                    family == Family.DARK,
                    false);
            assertDoesNotFlip(
                    family,
                    family == Family.LIGHT ? "jedi" : "darkJedi",
                    family == Family.LIGHT,
                    false);
        }
    }

    @Test
    public void bothBacksHoldThenFlipAndRetrieveWhenTheOpponentActorLeaves() {
        for (Family family : Family.values()) {
            var scn = scenario(family);
            startInOwnersDeployPhase(scn, family);
            var objective = objective(scn, family);
            var actor = qualifyingOpponent(scn, family);
            scn.MoveCardsToLocation(gate(scn, family), actor);

            pulseTable(scn, family, 1);
            assertTrue(objective.isFlipped());
            restoreOwnersDeployDecision(scn, family);

            pulseTable(scn, family, 2);
            assertTrue("The back must hold while the opponent's duel actor remains",
                    objective.isFlipped());
            restoreOwnersDeployDecision(scn, family);

            PhysicalCardImpl retrievedCard = family == Family.LIGHT
                    ? scn.GetLSFiller(1)
                    : scn.GetDSFiller(1);
            if (family == Family.LIGHT) {
                scn.MoveCardsToTopOfLSLostPile(retrievedCard);
            } else {
                scn.MoveCardsToTopOfDSLostPile(retrievedCard);
            }
            int lostBefore = family == Family.LIGHT
                    ? scn.GetLSLostPileCount()
                    : scn.GetDSLostPileCount();

            scn.MoveOutOfPlay(actor);
            pulseTable(scn, family, 3);

            assertFalse("The back must flip when the qualifying opponent actor leaves",
                    objective.isFlipped());
            assertEquals(lostBefore - 1,
                    family == Family.LIGHT
                            ? scn.GetLSLostPileCount()
                            : scn.GetDSLostPileCount());
            assertInZone(Zone.USED_PILE, retrievedCard);
        }
    }

    @Test
    public void excludedOpponentStillSatisfiesBothFronts() {
        for (Family family : Family.values()) {
            var scn = scenario(family);
            startInOwnersDeployPhase(scn, family);
            var actor = qualifyingOpponent(scn, family);
            scn.MoveCardsToLocation(gate(scn, family), actor);
            scn.ApplyAdHocModifier(new ExcludedFromBattleModifier(
                    null, Filters.sameCardId(actor)));

            pulseTable(scn, family, 1);

            assertTrue("The source explicitly includes excluded-from-battle actors",
                    objective(scn, family).isFlipped());
        }
    }

    @Test
    public void bothBacksForbidOwnDrainAndBattleAtTheDuelistsLocation() {
        for (Family family : Family.values()) {
            var scn = scenario(family);
            startInOwnersDeployPhase(scn, family);
            var gate = gate(scn, family);
            scn.MoveCardsToLocation(
                    gate, qualifyingOpponent(scn, family));

            pulseTable(scn, family, 1);
            assertTrue(objective(scn, family).isFlipped());

            scn.MoveCardsToLocation(gate, ownDuelist(scn, family));
            String owner = family == Family.LIGHT ? LS : DS;

            assertTrue(scn.game().getModifiersQuerying()
                    .isProhibitedFromForceDrainingAtLocation(
                            scn.gameState(), gate, owner));
            assertTrue(scn.game().getModifiersQuerying()
                    .mayNotInitiateBattleAtLocation(
                            scn.gameState(), gate, owner));
            assertFalse(scn.game().getModifiersQuerying()
                    .isProhibitedFromForceDrainingAtLocation(
                            scn.gameState(), scn.GetLSCard("swamp"), owner));
            assertFalse(scn.game().getModifiersQuerying()
                    .mayNotInitiateBattleAtLocation(
                            scn.gameState(), scn.GetLSCard("swamp"), owner));
        }
    }
}
