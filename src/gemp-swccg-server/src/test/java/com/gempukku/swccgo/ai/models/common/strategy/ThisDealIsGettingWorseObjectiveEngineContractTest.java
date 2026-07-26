package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import org.junit.Test;

import java.util.HashMap;

import static com.gempukku.swccgo.framework.Assertions.assertInZone;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ThisDealIsGettingWorseObjectiveEngineContractTest {
    private static final StartingSetup THIS_DEAL_IS_GETTING_WORSE =
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
                    // Only setupSite remains as an eligible Cloud City
                    // battleground site, so the required deploy resolves
                    // without a card-choice decision.
                }
            };

    private VirtualTableScenario scenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("xwingSystem", "1_146");
                    put("cloudCarSector", "5_88");
                    put("rebel1", "1_28");
                    put("rebel2", "1_28");
                    put("rebel3", "1_28");
                    put("rebel4", "1_28");
                }},
                new HashMap<>() {{
                    put("bespin", "5_164");
                    put("cloudCity", "5_165");
                    put("extraSite1", "5_166");
                    put("extraSite2", "5_167");
                    put("darkDeal", "5_115");
                    put("tieSystem", "1_304");
                    put("obsidianSector", "5_175");
                    put("storm1", "1_194");
                    put("storm2", "1_194");
                    put("storm3", "1_194");
                    put("tableChange1", "1_194");
                    put("tableChange2", "1_194");
                    put("deathStar", "2_143");
                    put("superlaser", "2_161");
                    put("commencePrimaryIgnition", "2_130");
                    put("deathStarSite1", "1_283");
                    put("deathStarSite2", "1_284");
                    put("deathStarSite3", "1_285");
                }},
                40,
                40,
                StartingSetup.DoNothingSetup,
                THIS_DEAL_IS_GETTING_WORSE,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    private void startWithCoreCardsInHand(VirtualTableScenario scn) {
        scn.MoveCardsToDSHand(
                scn.GetDSCard("bespin"),
                scn.GetDSCard("cloudCity"),
                scn.GetDSCard("extraSite1"),
                scn.GetDSCard("extraSite2"),
                scn.GetDSCard("darkDeal"),
                scn.GetDSCard("tieSystem"),
                scn.GetDSCard("obsidianSector"),
                scn.GetDSCard("storm1"),
                scn.GetDSCard("storm2"),
                scn.GetDSCard("storm3"),
                scn.GetDSCard("tableChange1"),
                scn.GetDSCard("tableChange2"),
                scn.GetDSCard("deathStar"),
                scn.GetDSCard("superlaser"),
                scn.GetDSCard("commencePrimaryIgnition"),
                scn.GetDSCard("deathStarSite1"),
                scn.GetDSCard("deathStarSite2"),
                scn.GetDSCard("deathStarSite3"));
        scn.MoveCardsToLSHand(
                scn.GetLSCard("xwingSystem"),
                scn.GetLSCard("cloudCarSector"),
                scn.GetLSCard("rebel1"),
                scn.GetLSCard("rebel2"),
                scn.GetLSCard("rebel3"),
                scn.GetLSCard("rebel4"));
        scn.StartGame();
    }

    private void putCoreBespinLocationsOnTable(VirtualTableScenario scn) {
        scn.MoveLocationToTable(scn.GetDSCard("bespin"));
        scn.MoveLocationToTable(scn.GetDSCard("cloudCity"));
        scn.MoveLocationToTable(scn.GetDSCard("extraSite1"));
        scn.MoveLocationToTable(scn.GetDSCard("extraSite2"));
    }

    private void startAndPutCoreBespinLocationsOnTable(
            VirtualTableScenario scn) {
        startWithCoreCardsInHand(scn);
        putCoreBespinLocationsOnTable(scn);
    }

    private void enterDarkDeployWithForce(VirtualTableScenario scn) {
        scn.DSActivateForceCheat(24);
        scn.SkipToPhase(Phase.DEPLOY);
    }

    private void triggerTableChange(
            VirtualTableScenario scn,
            String stormtrooperAlias) {
        scn.DSDeployCardAndPassResponses(
                scn.GetDSCard(stormtrooperAlias),
                scn.GetDSCard("setupSite"));
    }

    private void finishBlowAwayResolution(VirtualTableScenario scn) {
        for (int attempt = 0; attempt < 30; attempt++) {
            if (scn.DSDecisionAvailable("Choose card to put on Lost Pile")) {
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
        throw new AssertionError("Bespin blow-away resolution did not finish");
    }

    private VirtualTableScenario startFlippedByLegalDarkDealDeployment() {
        VirtualTableScenario scn = scenario();
        startAndPutCoreBespinLocationsOnTable(scn);

        var bespin = scn.GetDSCard("bespin");
        var cloudCity = scn.GetDSCard("cloudCity");
        var darkDeal = scn.GetDSCard("darkDeal");
        scn.MoveCardsToLocation(bespin, scn.GetDSCard("tieSystem"));
        scn.MoveCardsToLocation(
                cloudCity, scn.GetDSCard("obsidianSector"));
        scn.MoveCardsToLocation(
                scn.GetDSCard("setupSite"), scn.GetDSCard("storm1"));
        scn.MoveCardsToLocation(
                scn.GetDSCard("extraSite1"), scn.GetDSCard("storm2"));
        scn.MoveCardsToLocation(
                scn.GetDSCard("extraSite2"), scn.GetDSCard("storm3"));

        enterDarkDeployWithForce(scn);
        assertTrue(GameConditions.controls(
                scn.game(), VirtualTableScenario.DS, cloudCity));
        assertTrue(GameConditions.controls(
                scn.game(), VirtualTableScenario.DS, 3,
                Filters.relatedSiteTo(darkDeal, Filters.Bespin_Cloud_City)));
        assertTrue("Dark Deal must be legally deployable only after its own control requirements are met",
                scn.DSDeployAvailable(darkDeal));

        scn.DSDeployCardAndPassResponses(darkDeal, cloudCity);

        assertTrue(scn.IsAttachedTo(cloudCity, darkDeal));
        assertTrue("The actual Dark Deal deployment must emit the table change that flips the objective",
                scn.GetDSCard("objective").isFlipped());
        if (scn.AwaitingLSDeployPhaseActions()) {
            scn.LSPass();
        }
        return scn;
    }

    @Test
    public void setupDeploysCanonicalCloudCityBattlegroundSite() {
        var scn = scenario();
        startWithCoreCardsInHand(scn);
        var objective = scn.GetDSCard("objective");
        var setupSite = scn.GetDSCard("setupSite");

        assertInZone(Zone.SIDE_OF_TABLE, objective);
        assertInZone(Zone.LOCATIONS, setupSite);
        assertTrue("The required setup card must really be a Cloud City battleground site",
                Filters.Cloud_City_battleground_site.accepts(
                        scn.game(), setupSite));
        assertFalse(objective.isFlipped());
    }

    @Test
    public void frontRequiresDarkDealAndBothOccupiedLocations() {
        var noDarkDeal = scenario();
        startAndPutCoreBespinLocationsOnTable(noDarkDeal);
        noDarkDeal.MoveCardsToLocation(
                noDarkDeal.GetDSCard("bespin"),
                noDarkDeal.GetDSCard("tieSystem"));
        noDarkDeal.MoveCardsToLocation(
                noDarkDeal.GetDSCard("cloudCity"),
                noDarkDeal.GetDSCard("obsidianSector"));
        enterDarkDeployWithForce(noDarkDeal);
        triggerTableChange(noDarkDeal, "tableChange1");
        assertFalse("Occupying both locations without Dark Deal must not flip",
                noDarkDeal.GetDSCard("objective").isFlipped());

        var noSystemOccupation = scenario();
        startAndPutCoreBespinLocationsOnTable(noSystemOccupation);
        noSystemOccupation.AttachCardsTo(
                noSystemOccupation.GetDSCard("cloudCity"),
                noSystemOccupation.GetDSCard("darkDeal"));
        noSystemOccupation.MoveCardsToLocation(
                noSystemOccupation.GetDSCard("cloudCity"),
                noSystemOccupation.GetDSCard("obsidianSector"));
        enterDarkDeployWithForce(noSystemOccupation);
        triggerTableChange(noSystemOccupation, "tableChange1");
        assertFalse("Dark Deal plus Cloud City occupation without Bespin system occupation must not flip",
                noSystemOccupation.GetDSCard("objective").isFlipped());

        var noSectorOccupation = scenario();
        startAndPutCoreBespinLocationsOnTable(noSectorOccupation);
        noSectorOccupation.AttachCardsTo(
                noSectorOccupation.GetDSCard("cloudCity"),
                noSectorOccupation.GetDSCard("darkDeal"));
        noSectorOccupation.MoveCardsToLocation(
                noSectorOccupation.GetDSCard("bespin"),
                noSectorOccupation.GetDSCard("tieSystem"));
        enterDarkDeployWithForce(noSectorOccupation);
        triggerTableChange(noSectorOccupation, "tableChange1");
        assertFalse("Dark Deal plus Bespin system occupation without Cloud City occupation must not flip",
                noSectorOccupation.GetDSCard("objective").isFlipped());
    }

    @Test
    public void contestedLocationsCountAsOccupationAndRealTableChangeFlips() {
        var scn = scenario();
        startAndPutCoreBespinLocationsOnTable(scn);
        var objective = scn.GetDSCard("objective");
        var bespin = scn.GetDSCard("bespin");
        var cloudCity = scn.GetDSCard("cloudCity");

        scn.AttachCardsTo(cloudCity, scn.GetDSCard("darkDeal"));
        scn.MoveCardsToLocation(
                bespin,
                scn.GetDSCard("tieSystem"),
                scn.GetLSCard("xwingSystem"));
        scn.MoveCardsToLocation(
                cloudCity,
                scn.GetDSCard("obsidianSector"),
                scn.GetLSCard("cloudCarSector"));

        assertTrue(GameConditions.occupies(
                scn.game(), VirtualTableScenario.DS, bespin));
        assertTrue(GameConditions.occupies(
                scn.game(), VirtualTableScenario.DS, cloudCity));
        assertFalse("Contested Bespin is occupied, not controlled, by Dark Side",
                GameConditions.controls(
                        scn.game(), VirtualTableScenario.DS, bespin));
        assertFalse("Contested Cloud City is occupied, not controlled, by Dark Side",
                GameConditions.controls(
                        scn.game(), VirtualTableScenario.DS, cloudCity));

        enterDarkDeployWithForce(scn);
        triggerTableChange(scn, "tableChange1");

        assertTrue("The source uses occupies, so contested Bespin and Cloud City must satisfy the flip",
                objective.isFlipped());
    }

    @Test
    public void legalDarkDealDeploymentCompletesAndFiresFullFrontFlip() {
        var scn = startFlippedByLegalDarkDealDeployment();

        assertTrue(GameConditions.occupies(
                scn.game(), VirtualTableScenario.DS,
                scn.GetDSCard("bespin")));
        assertTrue(GameConditions.occupies(
                scn.game(), VirtualTableScenario.DS,
                scn.GetDSCard("cloudCity")));
        assertTrue(scn.GetDSCard("objective").isFlipped());
    }

    @Test
    public void backIgnoresNonCancellationRemovalAndUncontrolledOccupationLoss() {
        var removedWithoutCancellation =
                startFlippedByLegalDarkDealDeployment();
        removedWithoutCancellation.MoveCardsToTopOfDSUsedPile(
                removedWithoutCancellation.GetDSCard("darkDeal"));
        triggerTableChange(removedWithoutCancellation, "tableChange1");
        assertTrue("Dark Deal merely leaving table is not the back side's just-canceled trigger",
                removedWithoutCancellation.GetDSCard("objective").isFlipped());

        var occupationLoss = startFlippedByLegalDarkDealDeployment();
        occupationLoss.MoveOutOfPlay(
                occupationLoss.GetDSCard("obsidianSector"));
        assertFalse(GameConditions.occupies(
                occupationLoss.game(), VirtualTableScenario.DS,
                occupationLoss.GetDSCard("cloudCity")));
        triggerTableChange(occupationLoss, "tableChange1");
        assertTrue("Ceasing to occupy Cloud City does not flip the back side",
                occupationLoss.GetDSCard("objective").isFlipped());
        if (occupationLoss.AwaitingLSDeployPhaseActions()) {
            occupationLoss.LSPass();
        }

        occupationLoss.MoveCardsToLocation(
                occupationLoss.GetDSCard("cloudCity"),
                occupationLoss.GetDSCard("obsidianSector"));
        occupationLoss.MoveOutOfPlay(
                occupationLoss.GetDSCard("tieSystem"));
        assertFalse(GameConditions.occupies(
                occupationLoss.game(), VirtualTableScenario.DS,
                occupationLoss.GetDSCard("bespin")));
        assertFalse("An empty Bespin system is not controlled by the opponent",
                GameConditions.controls(
                        occupationLoss.game(), VirtualTableScenario.LS,
                        occupationLoss.GetDSCard("bespin")));
        triggerTableChange(occupationLoss, "tableChange2");
        assertTrue("Ceasing to occupy an otherwise uncontrolled Bespin does not flip the back side",
                occupationLoss.GetDSCard("objective").isFlipped());
    }

    @Test
    public void darkDealCancellationFlipsBack() {
        var scn = startFlippedByLegalDarkDealDeployment();
        var objective = scn.GetDSCard("objective");
        var darkDeal = scn.GetDSCard("darkDeal");

        scn.MoveCardsToLocation(
                scn.GetDSCard("bespin"), scn.GetLSCard("xwingSystem"));
        scn.MoveCardsToLocation(
                scn.GetDSCard("cloudCity"),
                scn.GetLSCard("cloudCarSector"));
        scn.MoveCardsToLocation(
                scn.GetDSCard("setupSite"), scn.GetLSCard("rebel1"));
        assertTrue(GameConditions.occupies(
                scn.game(), VirtualTableScenario.LS, 3,
                Filters.Bespin_location));

        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(
                scn.GetLSCard("rebel2"), scn.GetDSCard("extraSite1"));

        assertInZone(Zone.LOST_PILE, darkDeal);
        assertFalse("Dark Deal's actual self-cancellation must fire the back side's just-canceled trigger",
                objective.isFlipped());
    }

    @Test
    public void opponentControlOfBespinFlipsBack() {
        var scn = startFlippedByLegalDarkDealDeployment();
        var objective = scn.GetDSCard("objective");
        var bespin = scn.GetDSCard("bespin");

        scn.MoveOutOfPlay(scn.GetDSCard("tieSystem"));
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(
                scn.GetLSCard("xwingSystem"), bespin);

        assertTrue(GameConditions.controls(
                scn.game(), VirtualTableScenario.LS, bespin));
        assertFalse("Opponent control of the Bespin system must flip the back side",
                objective.isFlipped());
    }

    @Test
    public void bespinBlownAwayByActualCpiFlipsBack() {
        var scn = startFlippedByLegalDarkDealDeployment();
        var objective = scn.GetDSCard("objective");
        var bespin = scn.GetDSCard("bespin");
        var deathStar = scn.GetDSCard("deathStar");
        var cpi = scn.GetDSCard("commencePrimaryIgnition");

        scn.MoveLocationToTable(deathStar);
        scn.MoveLocationToTable(scn.GetDSCard("deathStarSite1"));
        scn.MoveLocationToTable(scn.GetDSCard("deathStarSite2"));
        scn.MoveLocationToTable(scn.GetDSCard("deathStarSite3"));
        deathStar.setSystemOrbited("Bespin");
        scn.AttachCardsTo(deathStar, scn.GetDSCard("superlaser"));

        scn.SkipToDSTurn(Phase.CONTROL);
        scn.PrepareDSDestiny(7);
        assertTrue("CPI must see the orbiting Death Star, attached Superlaser, and sufficient Force",
                scn.DSCardPlayAvailable(
                        cpi, "Attempt to 'blow away' Bespin"));

        scn.DSPlayCard(cpi, "Attempt to 'blow away' Bespin");
        scn.PassAllResponses();
        assertTrue(scn.LSDecisionAvailable("Choose value for Z"));
        scn.LSChoose("Total sites at Hoth: 0");
        scn.PassAllResponses();
        finishBlowAwayResolution(scn);

        assertTrue("CPI must actually blow away the Bespin system",
                bespin.isBlownAway());
        assertInZone(Zone.LOST_PILE, cpi);
        assertFalse("Bespin's actual blown-away last step must flip the back side",
                objective.isFlipped());
    }
}
