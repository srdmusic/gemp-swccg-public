package com.gempukku.swccgo.cards.set5.light;

import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;
import org.junit.Test;

import java.util.HashMap;

import static com.gempukku.swccgo.framework.Assertions.assertAtLocation;
import static com.gempukku.swccgo.framework.Assertions.assertInHand;
import static org.junit.Assert.*;

public class Card_5_36_Tests {
	protected VirtualTableScenario GetScenario() throws DecisionResultInvalidException {
		return new VirtualTableScenario(
				new HashMap<>()
				{{
					put("fury", "5_36");
					put("chewie", "2_3");
				}},
				new HashMap<>()
				{{
					put("boba", "105_4");
				}},
				10,
				10,
				VirtualTableScenario.DefaultGroundLSLocation,
				VirtualTableScenario.DefaultGroundDSLocation,
				VirtualTableScenario.NoLSStarters,
				VirtualTableScenario.NoDSStarters,
				VirtualTableScenario.NoLSShields,
				VirtualTableScenario.NoDSShields,
				VirtualTableScenario.Open
		);
	}

	@Test
	public void CaptiveFuryStatsAndKeywordsAreCorrect() throws DecisionResultInvalidException {
		/**
		 * Title: Captive Fury
		 * Uniqueness: Unique
		 * Side: Light
		 * Type: Interrupt
		 * Subtype: Used or Lost
		 * Destiny: 4
		 * Game Text: USED: Cancel Force drain bonus from IT-O this turn.  LOST: During your battle phase, any of your
		 * 		escorted captives at same site may initiate and participate in one battle (they may not use weapons or
		 * 		devices and you may not voluntarily forfeit or relocate them).
		 * Lore: Chewie's life debt to Han forced him to act, retaliating unexpectedly against his captors.
		 * Set: Cloud City
		 * Rarity: U
		 */

		var scn = GetScenario();

		var card = scn.GetLSCard("fury").getBlueprint();

		assertEquals("Captive Fury", card.getTitle());
		assertEquals(Uniqueness.UNIQUE, card.getUniqueness());
		assertEquals(Side.LIGHT, card.getSide());
		assertTrue(card.isCardType(CardType.INTERRUPT));
		assertEquals(CardSubtype.USED_OR_LOST, card.getCardSubtype());
		assertEquals(4, card.getDestiny(), scn.epsilon);
		assertEquals(1, card.getIconCount(Icon.CLOUD_CITY));
	}

	@Test
	public void CaptiveFuryInitializesBattle() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var fury = scn.GetLSCard("fury");
		var chewie = scn.GetLSCard("chewie");
		scn.MoveCardsToLSHand(fury);

		var site = scn.GetLSStartingLocation();

		var boba = scn.GetDSCard("boba");

		scn.StartGame();

		scn.MoveCardsToLocation(site, boba);
		scn.CaptureCardWith(boba, chewie);

		scn.SkipToLSTurn();

		scn.SkipToPhase(Phase.BATTLE);

		assertTrue(scn.LSActionAvailable("LOST"));
		scn.LSUseCardAction(fury);

		assertTrue(scn.LSDecisionAvailable("Choose escorted captives to participate in battle"));
		scn.LSChooseCard(chewie);

		assertTrue(scn.DSDecisionAvailable("Optional responses"));
		scn.PassCardPlayResponses();
		scn.PassForceUseResponses();
		scn.BothPassInverted();

		assertAtLocation(site, chewie);
		assertFalse(scn.IsAttachedTo(chewie, boba));
		assertTrue(scn.IsActiveBattle());
		assertTrue(scn.IsParticipatingInBattle(chewie));
		assertTrue(scn.IsParticipatingInBattle(boba));

		assertTrue(scn.LSDecisionAvailable("Choose weapons segment action to play or Pass"));
	}

	@Test
	public void RebelTrooperDeploysFor1ForceWithAbility2Rebel() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var trooper = scn.GetLSCard("trooper");
		var biggs = scn.GetLSCard("biggs");
		scn.MoveCardsToLSHand(trooper, biggs);

		var site = scn.GetLSStartingLocation();

		scn.StartGame();

		scn.SkipToLSTurn();

		scn.MoveCardsToLocation(site, biggs);

		assertEquals(Phase.ACTIVATE, scn.GetCurrentPhase());
		assertEquals(scn.LS, scn.GetCurrentPlayer());
		assertTrue(scn.LSDecisionAvailable("Choose Activate action or Pass"));

		scn.SkipToPhase(Phase.DEPLOY);

		assertInHand(trooper);
		assertAtLocation(site, biggs);
		assertEquals(2, scn.GetAbility(biggs), scn.epsilon);
		assertEquals(3, scn.GetLSForcePileCount());

		scn.LSDeployCardAndPassResponses(trooper, site);

		assertAtLocation(site, trooper);
		assertEquals(2, scn.GetLSForcePileCount());
	}

	@Test
	public void RebelTrooperDeploysFor0ForceWithAbility3Rebel() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var trooper = scn.GetLSCard("trooper");
		var ackbar = scn.GetLSCard("ackbar");
		scn.MoveCardsToLSHand(trooper, ackbar);

		var site = scn.GetLSStartingLocation();

		scn.StartGame();

		scn.SkipToLSTurn();

		scn.MoveCardsToLocation(site, ackbar);

		assertEquals(Phase.ACTIVATE, scn.GetCurrentPhase());
		assertEquals(scn.LS, scn.GetCurrentPlayer());
		assertTrue(scn.LSDecisionAvailable("Choose Activate action or Pass"));

		scn.SkipToPhase(Phase.DEPLOY);

		assertInHand(trooper);
		assertAtLocation(site, ackbar);
		assertEquals(3, scn.GetAbility(ackbar), scn.epsilon);
		assertEquals(3, scn.GetLSForcePileCount());

		scn.LSDeployCardAndPassResponses(trooper, site);

		assertAtLocation(site, trooper);
		assertEquals(3, scn.GetLSForcePileCount());
	}
}
