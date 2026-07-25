package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import org.junit.Test;

import java.util.HashMap;

import static com.gempukku.swccgo.framework.Assertions.assertAtLocation;
import static com.gempukku.swccgo.framework.Assertions.assertInZone;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HuntDownObjectiveEngineContractTest {
    private enum Version {
        CLASSIC,
        VIRTUAL
    }

    private static final StartingSetup CLASSIC_HUNT_DOWN = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "7_297");
                put("holotheatre", "4_161");
                put("visage", "4_135");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            // Each required starting card has one matching candidate.
        }
    };

    private static final StartingSetup VIRTUAL_HUNT_DOWN = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "213_31");
                put("castle", "209_50");
                put("visage", "213_16");
                put("setupSite", "5_169");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            if (scn.DSDecisionAvailable("On which side")) {
                scn.DSChoose("Left");
            }
        }
    };

    private VirtualTableScenario scenario(Version version) {
        HashMap<String, String> lsCards = new HashMap<>() {{
            put("luke", "1_19");
            put("padawan", "203_6");
            put("rebel1", "1_28");
            put("rebel2", "1_28");
        }};
        HashMap<String, String> dsCards = new HashMap<>() {{
            put("vader", "1_168");
            put("storm1", "1_194");
            put("storm2", "1_194");
            put("storm3", "1_194");
            put("downloadSite", "5_166");
        }};
        if (version == Version.CLASSIC) {
            dsCards.put("castle", "209_50");
        }

        return new VirtualTableScenario(
                lsCards,
                dsCards,
                24,
                24,
                StartingSetup.DefaultLSGroundLocation,
                version == Version.CLASSIC ? CLASSIC_HUNT_DOWN : VIRTUAL_HUNT_DOWN,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    private void startWithCardsForBothTurns(VirtualTableScenario scn) {
        scn.MoveCardsToDSHand(
                scn.GetDSCard("storm1"),
                scn.GetDSCard("storm2"),
                scn.GetDSCard("storm3"));
        scn.MoveCardsToLSHand(
                scn.GetLSCard("luke"),
                scn.GetLSCard("padawan"),
                scn.GetLSCard("rebel1"),
                scn.GetLSCard("rebel2"));
        scn.StartGame();
        if (scn.GetDSCard("castle").getZone() != Zone.LOCATIONS) {
            scn.MoveLocationToTable(scn.GetDSCard("castle"));
        }
        scn.MoveCardsToBottomOfDSReserveDeck(scn.GetDSCard("downloadSite"));
    }

    private void enterDSDeployPhase(VirtualTableScenario scn) {
        scn.SkipToPhase(Phase.DEPLOY);
        scn.DSActivateForceCheat(12);
    }

    private void triggerTableChangeWithDSImperial(
            VirtualTableScenario scn,
            String stormAlias) {
        scn.DSDeployCardAndPassResponses(
                scn.GetDSCard(stormAlias),
                scn.GetLSCard("starting-location"));
    }

    private VirtualTableScenario startFlipped(Version version) {
        VirtualTableScenario scn = scenario(version);
        startWithCardsForBothTurns(scn);
        scn.MoveCardsToLocation(
                scn.GetLSCard("starting-location"),
                scn.GetDSCard("vader"));
        enterDSDeployPhase(scn);

        triggerTableChangeWithDSImperial(scn, "storm1");
        assertTrue("Vader at a battleground with no blocker must fire the real front-side trigger",
                scn.GetDSCard("objective").isFlipped());
        // Restore Dark Side's top-level action decision after the required flip.
        scn.LSPass();
        return scn;
    }

    private void moveVaderToCastleUsingRealLocationText(VirtualTableScenario scn) {
        var castle = scn.GetDSCard("castle");
        var battleground = scn.GetLSCard("starting-location");
        var vader = scn.GetDSCard("vader");

        scn.SkipToPhase(Phase.MOVE);
        assertTrue("Vader's Castle must offer its real cross-location movement action",
                scn.DSCardActionAvailable(castle, "Move from other battleground site to here"));
        scn.DSUseCardAction(castle, "Move from other battleground site to here");
        if (scn.DSDecisionAvailable("Choose card to move from")) {
            scn.DSChooseCard(battleground);
        }
        if (scn.DSDecisionAvailable("Choose card to move to")) {
            scn.DSChooseCard(castle);
        }
        if (scn.DSDecisionAvailable("Choose card to move to ")) {
            scn.DSChooseCard(vader);
        }
        scn.PassAllResponses();

        assertAtLocation(castle, vader);
    }

    private void moveVaderFromCastleUsingRealLocationText(VirtualTableScenario scn) {
        var castle = scn.GetDSCard("castle");
        var battleground = scn.GetLSCard("starting-location");
        var vader = scn.GetDSCard("vader");

        scn.SkipToPhase(Phase.MOVE);
        assertTrue("Vader's Castle must offer its real outbound movement action",
                scn.DSCardActionAvailable(castle, "Move from here to other battleground site"));
        scn.DSUseCardAction(castle, "Move from here to other battleground site");
        if (scn.DSDecisionAvailable("Choose card to move from")) {
            scn.DSChooseCard(castle);
        }
        if (scn.DSDecisionAvailable("Choose card to move to")) {
            scn.DSChooseCard(battleground);
        }
        if (scn.DSDecisionAvailable("Choose card to move to ")) {
            scn.DSChooseCard(vader);
        }
        scn.PassAllResponses();

        assertAtLocation(battleground, vader);
    }

    @Test
    public void classicSetupAndFrontRequireVaderAtABattleground() {
        var nonBattleground = scenario(Version.CLASSIC);
        startWithCardsForBothTurns(nonBattleground);
        var objective = nonBattleground.GetDSCard("objective");
        var holotheatre = nonBattleground.GetDSCard("holotheatre");
        var visage = nonBattleground.GetDSCard("visage");
        var vader = nonBattleground.GetDSCard("vader");

        assertInZone(Zone.SIDE_OF_TABLE, objective);
        assertInZone(Zone.LOCATIONS, holotheatre);
        assertTrue("Classic setup must deploy Visage on Holotheatre",
                nonBattleground.IsAttachedTo(holotheatre, visage));

        nonBattleground.MoveCardsToLocation(holotheatre, vader);
        enterDSDeployPhase(nonBattleground);
        triggerTableChangeWithDSImperial(nonBattleground, "storm1");
        assertFalse("Vader on a non-battleground site must not satisfy the front side",
                objective.isFlipped());

        var battleground = scenario(Version.CLASSIC);
        startWithCardsForBothTurns(battleground);
        battleground.MoveCardsToLocation(
                battleground.GetLSCard("starting-location"),
                battleground.GetDSCard("vader"));
        enterDSDeployPhase(battleground);
        triggerTableChangeWithDSImperial(battleground, "storm1");
        assertTrue("Vader at a battleground with no Jedi or Luke at any battleground must flip",
                battleground.GetDSCard("objective").isFlipped());
    }

    @Test
    public void virtualSetupDownloadAndFrontRequireVaderAtABattleground() {
        var setup = scenario(Version.VIRTUAL);
        startWithCardsForBothTurns(setup);
        var objective = setup.GetDSCard("objective");
        var castle = setup.GetDSCard("castle");
        var visage = setup.GetDSCard("visage");
        var setupSite = setup.GetDSCard("setupSite");
        var downloadSite = setup.GetDSCard("downloadSite");

        assertInZone(Zone.SIDE_OF_TABLE, objective);
        assertInZone(Zone.LOCATIONS, castle, setupSite);
        assertTrue("Virtual setup must deploy its Set 13 Visage on Vader's Castle",
                setup.IsAttachedTo(castle, visage));

        setup.SkipToPhase(Phase.DEPLOY);
        assertTrue("The virtual objective must expose its once-per-deploy-phase battleground-site download",
                setup.DSCardActionAvailable(objective, "Deploy a location from Reserve Deck"));
        setup.DSUseCardAction(objective, "Deploy a location from Reserve Deck");
        assertTrue("A Cloud City battleground site must be a legal download",
                setup.DSHasCardChoiceAvailable(downloadSite));
        setup.DSChooseCard(downloadSite);
        setup.PassAllResponses();
        if (setup.DSDecisionAvailable("On which side")) {
            setup.DSChoose("Right");
            setup.PassAllResponses();
        }
        assertInZone(Zone.LOCATIONS, downloadSite);

        var nonBattleground = scenario(Version.VIRTUAL);
        startWithCardsForBothTurns(nonBattleground);
        nonBattleground.MoveCardsToLocation(
                nonBattleground.GetDSCard("castle"),
                nonBattleground.GetDSCard("vader"));
        enterDSDeployPhase(nonBattleground);
        triggerTableChangeWithDSImperial(nonBattleground, "storm1");
        assertFalse("Vader at his non-battleground Castle must not satisfy the front side",
                nonBattleground.GetDSCard("objective").isFlipped());

        var battleground = scenario(Version.VIRTUAL);
        startWithCardsForBothTurns(battleground);
        battleground.MoveCardsToLocation(
                battleground.GetLSCard("starting-location"),
                battleground.GetDSCard("vader"));
        enterDSDeployPhase(battleground);
        triggerTableChangeWithDSImperial(battleground, "storm1");
        assertTrue("Virtual Hunt Down must flip when Vader reaches a battleground and no blocker is there",
                battleground.GetDSCard("objective").isFlipped());
    }

    @Test
    public void virtualCastleDeployBudgetFundsRealMoveAndFrontFlipChain() {
        var scn = scenario(Version.VIRTUAL);
        startWithCardsForBothTurns(scn);
        var objective = scn.GetDSCard("objective");
        var castle = scn.GetDSCard("castle");
        var vader = scn.GetDSCard("vader");

        scn.SkipToPhase(Phase.DEPLOY);
        while (scn.GetDSForcePileCount() > 0) {
            scn.MoveCardsToHand(scn.GetTopOfDSForcePile());
        }
        scn.MoveCardsToBottomOfDSReserveDeck(vader);

        int deployCost = vader.getBlueprint().getDeployCost().intValue();
        int forceBudget = deployCost + 1;
        scn.DSActivateForceCheat(forceBudget);
        assertEquals("Fixture must fund ordinary Vader deploy plus Castle's one-Force move",
                7, scn.GetDSForcePileCount());

        assertTrue("Vader's Castle must expose its real Reserve Deck deploy action",
                scn.DSCardActionAvailable(castle, "Deploy Vader from Reserve Deck"));
        scn.DSUseCardAction(castle, "Deploy Vader from Reserve Deck");
        assertTrue("The explicit Vader must be a legal Castle download",
                scn.DSHasCardChoiceAvailable(vader));
        scn.DSChooseCard(vader);
        scn.PassAllResponses();

        assertAtLocation(castle, vader);
        assertEquals("Castle download must pay Vader's ordinary deploy cost and preserve the move reserve",
                1, scn.GetDSForcePileCount());
        assertFalse("Vader at the non-battleground Castle must not satisfy the front side",
                objective.isFlipped());

        moveVaderFromCastleUsingRealLocationText(scn);

        assertEquals("Castle outbound movement must spend the preserved one Force",
                0, scn.GetDSForcePileCount());
        assertTrue("The real Castle deploy-move chain must fire Hunt Down's front-side flip trigger",
                objective.isFlipped());
    }

    @Test
    public void blockerAtAnotherBattlegroundPreventsBothFrontsButNotAtANonBattleground() {
        for (Version version : Version.values()) {
            var blocked = scenario(version);
            startWithCardsForBothTurns(blocked);
            blocked.MoveCardsToLocation(
                    blocked.GetLSCard("starting-location"),
                    blocked.GetDSCard("vader"));
            blocked.MoveLocationToTable(blocked.GetDSCard("downloadSite"));
            blocked.MoveCardsToLocation(
                    blocked.GetDSCard("downloadSite"),
                    blocked.GetLSCard("luke"));
            enterDSDeployPhase(blocked);
            triggerTableChangeWithDSImperial(blocked, "storm1");
            assertFalse("Luke at a different battleground must block " + version + " Hunt Down",
                    blocked.GetDSCard("objective").isFlipped());

            var nonBattleground = scenario(version);
            startWithCardsForBothTurns(nonBattleground);
            nonBattleground.MoveCardsToLocation(
                    nonBattleground.GetLSCard("starting-location"),
                    nonBattleground.GetDSCard("vader"));
            nonBattleground.MoveCardsToLocation(
                    nonBattleground.GetDSCard("castle"),
                    nonBattleground.GetLSCard("luke"));
            enterDSDeployPhase(nonBattleground);
            triggerTableChangeWithDSImperial(nonBattleground, "storm1");
            assertTrue("Luke at a non-battleground must not block " + version + " Hunt Down",
                    nonBattleground.GetDSCard("objective").isFlipped());
        }
    }

    @Test
    public void classicIgnoresPadawanAtBattlegroundButVirtualDoesNot() {
        var classic = scenario(Version.CLASSIC);
        startWithCardsForBothTurns(classic);
        classic.MoveCardsToLocation(
                classic.GetLSCard("starting-location"),
                classic.GetDSCard("vader"));
        classic.MoveCardsToLocation(
                classic.GetLSCard("starting-location"),
                classic.GetLSCard("padawan"));
        enterDSDeployPhase(classic);
        triggerTableChangeWithDSImperial(classic, "storm1");
        assertTrue("Classic source checks Jedi or Luke, not a below-Jedi Padawan",
                classic.GetDSCard("objective").isFlipped());

        var virtual = scenario(Version.VIRTUAL);
        startWithCardsForBothTurns(virtual);
        virtual.MoveCardsToLocation(
                virtual.GetLSCard("starting-location"),
                virtual.GetDSCard("vader"));
        virtual.MoveCardsToLocation(
                virtual.GetLSCard("starting-location"),
                virtual.GetLSCard("padawan"));
        enterDSDeployPhase(virtual);
        triggerTableChangeWithDSImperial(virtual, "storm1");
        assertFalse("Virtual source explicitly adds Padawan as a battleground blocker",
                virtual.GetDSCard("objective").isFlipped());
    }

    @Test
    public void classicBackFlipsForLukeAtAnotherBattleground() {
        var scn = startFlipped(Version.CLASSIC);
        var objective = scn.GetDSCard("objective");

        scn.MoveLocationToTable(scn.GetDSCard("downloadSite"));
        scn.MoveCardsToLocation(
                scn.GetDSCard("downloadSite"),
                scn.GetLSCard("luke"));
        triggerTableChangeWithDSImperial(scn, "storm2");

        assertFalse("Luke at any battleground must fire classic Hunt Down's real back-side trigger",
                objective.isFlipped());
    }

    @Test
    public void classicBackIgnoresPadawanAtAnotherBattlegroundButVirtualBackDoesNot() {
        var classic = startFlipped(Version.CLASSIC);
        classic.MoveLocationToTable(classic.GetDSCard("downloadSite"));
        classic.MoveCardsToLocation(
                classic.GetDSCard("downloadSite"),
                classic.GetLSCard("padawan"));
        triggerTableChangeWithDSImperial(classic, "storm2");
        assertTrue("Classic back source checks Jedi or Luke, not a below-Jedi Padawan",
                classic.GetDSCard("objective").isFlipped());

        var virtual = startFlipped(Version.VIRTUAL);
        virtual.MoveLocationToTable(virtual.GetDSCard("downloadSite"));
        virtual.MoveCardsToLocation(
                virtual.GetDSCard("downloadSite"),
                virtual.GetLSCard("padawan"));
        triggerTableChangeWithDSImperial(virtual, "storm2");
        assertFalse("A Padawan at another battleground must fire virtual Hunt Down's real back-side trigger",
                virtual.GetDSCard("objective").isFlipped());
    }

    @Test
    public void bothBacksFlipWhenVaderLeavesTable() {
        for (Version version : Version.values()) {
            var scn = startFlipped(version);
            var objective = scn.GetDSCard("objective");
            scn.MoveOutOfPlay(scn.GetDSCard("vader"));

            triggerTableChangeWithDSImperial(scn, "storm2");

            assertFalse("Vader absent from the table must flip back " + version + " Hunt Down",
                    objective.isFlipped());
        }
    }

    @Test
    public void bothBacksIgnoreBlockersAtNonBattlegroundSites() {
        for (Version version : Version.values()) {
            var scn = startFlipped(version);
            var objective = scn.GetDSCard("objective");
            var blocker = version == Version.CLASSIC
                    ? scn.GetLSCard("luke")
                    : scn.GetLSCard("padawan");

            scn.MoveCardsToLocation(scn.GetDSCard("castle"), blocker);
            triggerTableChangeWithDSImperial(scn, "storm2");

            assertTrue("A blocker at a non-battleground must not flip back " + version + " Hunt Down",
                    objective.isFlipped());
        }
    }

    @Test
    public void vaderMayLeaveBattlegroundForCastleWithoutEitherBackFlipping() {
        for (Version version : Version.values()) {
            var scn = startFlipped(version);
            var objective = scn.GetDSCard("objective");

            moveVaderToCastleUsingRealLocationText(scn);

            assertTrue("The back side requires Vader on table, not at a battleground, for " + version,
                    objective.isFlipped());
        }
    }
}
