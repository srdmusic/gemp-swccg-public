package com.gempukku.swccgo.cards.set5.dark;

import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;
import org.junit.Test;

import java.util.HashMap;

import static com.gempukku.swccgo.framework.Assertions.assertAtLocation;
import static com.gempukku.swccgo.framework.Assertions.assertInHand;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Card_5_106_Tests {
	protected VirtualTableScenario GetScenario() throws DecisionResultInvalidException {
		return new VirtualTableScenario(
				new HashMap<>()
				{{
					put("chewie", "2_3");
					put("han", "108_1");
					put("leia", "6_32");
				}},
				new HashMap<>()
				{{
					put("binders", "5_106");
					put("boba", "5_91");
					put("bobas_blaster", "5_179");
					put("ig88", "109_11"); //with riot gun
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
	public void BindersStatsAndKeywordsAreCorrect() throws DecisionResultInvalidException {
		/**
		 * Title: Binders
		 * Uniqueness: Unrestricted
		 * Side: Dark
		 * Type: Device
		 * Destiny: 6
		 * Game Text: Deploy on one of your warriors or bounty hunters. May now escort any number of captives.
		 * 			If device removed from your character, select one captive escorted by that character to remain and release all others.
		 * Lore: Because standard binders are durable but not easily adaptable, bounty hunters often carry special
		 * 			binders which automatically tighten around a captive's appendages.
		 * Set: Cloud City
		 * Rarity: C
		 */

		var scn = GetScenario();

		var card = scn.GetDSCard("binders").getBlueprint();

		assertEquals("Binders", card.getTitle());
		assertEquals(Uniqueness.UNRESTRICTED, card.getUniqueness());
		assertEquals(Side.DARK, card.getSide());
		assertTrue(card.isCardType(CardType.DEVICE));
		assertEquals(6, card.getDestiny(), scn.epsilon);
		assertEquals(1, card.getIconCount(Icon.CLOUD_CITY));
	}

	@Test
	public void BindersDeploysOnAWarrior() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var trooper = scn.GetLSFiller(1);
		var site = scn.GetLSStartingLocation();

		var stormtrooper = scn.GetDSFiller(1);
		var binders = scn.GetDSCard("binders");

		scn.StartGame();

		scn.MoveCardsToLocation(site, stormtrooper);
		scn.AttachCardsTo(stormtrooper, binders);

		assertTrue(false);
	}

	@Test
	public void BindersDeploysOnABountyHunter() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var trooper = scn.GetLSFiller(1);
		var site = scn.GetLSStartingLocation();

		var stormtrooper = scn.GetDSFiller(1);
		var binders = scn.GetDSCard("binders");

		scn.StartGame();

		scn.MoveCardsToLocation(site, stormtrooper);
		scn.AttachCardsTo(stormtrooper, binders);

		assertTrue(false);
	}

	@Test
	public void BindersPermitsBearerToCaptureMultipleCaptives() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var trooper = scn.GetLSFiller(1);
		var site = scn.GetLSStartingLocation();

		var stormtrooper = scn.GetDSFiller(1);
		var binders = scn.GetDSCard("binders");

		scn.StartGame();

		scn.MoveCardsToLocation(site, stormtrooper);
		scn.AttachCardsTo(stormtrooper, binders);

		assertTrue(false);
	}

	@Test
	public void BindersPermitsMultipleCaptivesToBeTransferredToBearer() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var trooper = scn.GetLSFiller(1);
		var site = scn.GetLSStartingLocation();

		var stormtrooper = scn.GetDSFiller(1);
		var binders = scn.GetDSCard("binders");

		scn.StartGame();

		scn.MoveCardsToLocation(site, stormtrooper);
		scn.AttachCardsTo(stormtrooper, binders);

		assertTrue(false);
	}

	@Test
	public void BindersWhenLostCauseAllBut1CaptiveToBeReleasedFromBearer() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var trooper = scn.GetLSFiller(1);
		var site = scn.GetLSStartingLocation();

		var stormtrooper = scn.GetDSFiller(1);
		var binders = scn.GetDSCard("binders");

		scn.StartGame();

		scn.MoveCardsToLocation(site, stormtrooper);
		scn.AttachCardsTo(stormtrooper, binders);

		assertTrue(false);
	}

	@Test
	public void BindersWhenTransferredCauseAllBut1CaptiveToBeReleasedFromBearer() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var trooper = scn.GetLSFiller(1);
		var site = scn.GetLSStartingLocation();

		var stormtrooper = scn.GetDSFiller(1);
		var binders = scn.GetDSCard("binders");

		scn.StartGame();

		scn.MoveCardsToLocation(site, stormtrooper);
		scn.AttachCardsTo(stormtrooper, binders);

		assertTrue(false);
	}

}
