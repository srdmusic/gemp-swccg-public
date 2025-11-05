package com.gempukku.swccgo.rules.immunity;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ImmuneToCardTests {
	protected VirtualTableScenario GetScenario() {
		return new VirtualTableScenario(
				new HashMap<>()
				{{
					put("demotion", "1_047");
					put("revolution", "1_062");
                    put("fury","5_029"); //uncontrollable fury
				}},
				new HashMap<>()
				{{
					put("weakvader", "101_005"); //power 4
					put("lordvader", "9_113"); //immune to uncontrollable fury
                    put("promotion", "4_121"); //field promotion (target is immune to demotion)
                    put("justice", "2_121"); //imperial justice (target is immune to revolution)
                    put("ozzel", "3_082");
				}},
				10,
				10,
				StartingSetup.DefaultLSGroundLocation,
				StartingSetup.DefaultDSGroundLocation,
				StartingSetup.NoLSStartingInterrupts,
				StartingSetup.NoDSStartingInterrupts,
				StartingSetup.NoLSShields,
				StartingSetup.NoDSShields,
				VirtualTableScenario.Open
		);
	}

	@Test
	public void MayNotDeployOnImmuneTarget() {
		//verifies:
		//card A cannot be deployed on card B that already has immunity to card A

		var scn = GetScenario();

		var site = scn.GetLSStartingLocation();

		var demotion = scn.GetLSCard("demotion");

        var ozzel = scn.GetDSCard("ozzel");
        var promotion = scn.GetDSCard("promotion");
        var lordvader = scn.GetDSCard("lordvader");

        scn.StartGame();

		scn.MoveCardsToLSHand(demotion);
        scn.MoveCardsToDSHand(promotion);

		scn.MoveCardsToLocation(site,ozzel,lordvader);

        scn.SkipToPhase(Phase.DEPLOY);
        assertTrue(scn.DSDeployAvailable(promotion));
        scn.DSDeployCard(promotion);
        assertTrue(scn.DSHasCardChoiceAvailable(ozzel));
        scn.DSChooseCard(ozzel);
        scn.PassAllResponses();

        assertTrue(scn.IsAttachedTo(ozzel, promotion));
        assertEquals(4,scn.GetPower(ozzel), scn.epsilon); //3 + 1 from promotion

		scn.SkipToLSTurn(Phase.DEPLOY);

        assertTrue(scn.GetLSForcePileCount() >= 2); //enough to play demotion
        assertFalse(scn.LSCardPlayAvailable(demotion)); //because ozzel is immune
	}

    //shows fixed: https://github.com/PlayersCommittee/gemp-swccg-public/issues/845
    @Test
    public void GainingImmunityByDeployMakesAttachedCardLost() {
        //verifies:
        //if card A is attached to card B and card B later gains immunity to card A, card A is lost
        var scn = GetScenario();

        var site = scn.GetLSStartingLocation();

        var demotion = scn.GetLSCard("demotion");

        var ozzel = scn.GetDSCard("ozzel");
        var promotion = scn.GetDSCard("promotion");
        var lordvader = scn.GetDSCard("lordvader");

        scn.StartGame();

        scn.MoveCardsToDSHand(promotion);

        scn.MoveCardsToLocation(site,ozzel,lordvader);
        scn.AttachCardsTo(ozzel,demotion);

        assertTrue(scn.IsAttachedTo(ozzel, demotion));
        assertEquals(1,scn.GetPower(ozzel), scn.epsilon); //2 - 1 from demotion

        scn.SkipToPhase(Phase.DEPLOY);
        assertTrue(scn.DSDeployAvailable(promotion));
        scn.DSDeployCard(promotion);
        assertTrue(scn.DSHasCardChoiceAvailable(ozzel));
        scn.DSChooseCard(ozzel);
        scn.PassAllResponses(); //demotion sent lost here

        assertTrue(scn.AwaitingLSDeployPhaseActions());
        assertTrue(scn.IsAttachedTo(ozzel, promotion));
        assertFalse(scn.IsAttachedTo(ozzel,demotion));
        assertEquals(1,scn.GetLSLostPileCount()); //demotion in lost
        assertEquals(4,scn.GetPower(ozzel), scn.epsilon); //3 + 1 from promotion
    }

    @Test
    public void GainingImmunityByPersonaReplacementMakesAttachedCardLost() {
        //verifies:
        //if card A is attached to card B and card B is persona replaced by a card with immunity to card A, card A is lost

        var scn = GetScenario();

        var site = scn.GetLSStartingLocation();

        var fury = scn.GetLSCard("fury");

        var weakvader = scn.GetDSCard("weakvader");
        var lordvader = scn.GetDSCard("lordvader");

        scn.StartGame();

        scn.MoveCardsToLSHand(fury);
        scn.MoveCardsToDSHand(lordvader);

        scn.MoveCardsToLocation(site,weakvader);
        scn.AttachCardsTo(weakvader,fury);

        assertTrue(scn.IsAttachedTo(weakvader, fury));
        assertEquals(6,scn.GetPower(weakvader), scn.epsilon); //4 + 2 from fury
        scn.SkipToDSTurn(Phase.DEPLOY);
        assertTrue(scn.DSCardPlayAvailable(lordvader));
        scn.DSPlayCard(lordvader);
        assertTrue(scn.DSHasCardChoiceAvailable(weakvader));
        scn.DSChooseCard(weakvader);
        scn.PassAllResponses(); //fury sent lost here

        assertFalse(scn.IsAttachedTo(lordvader, fury));
        assertEquals(7,scn.GetPower(lordvader), scn.epsilon); //7 + 0 (no fury)
        assertEquals(1,scn.GetLSLostPileCount()); //fury in lost
        assertEquals(1,scn.GetDSLostPileCount()); //weakvader in lost
    }

    //add testing for immunity involving utinni effects

    //add testing for immunity to interrupt targeting?
}
