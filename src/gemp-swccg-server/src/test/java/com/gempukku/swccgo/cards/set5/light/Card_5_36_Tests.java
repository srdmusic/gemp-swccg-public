package com.gempukku.swccgo.cards.set5.light;

import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;
import org.junit.Test;

import java.util.HashMap;

import static com.gempukku.swccgo.framework.Assertions.assertAtLocation;
import static org.junit.Assert.*;

public class Card_5_36_Tests {
	protected VirtualTableScenario GetScenario() throws DecisionResultInvalidException {
		return new VirtualTableScenario(
				new HashMap<>()
				{{
					put("fury", "5_36");
					put("chewie", "2_3");
					put("bowcaster", "8_86"); //weapon
					put("electrobinoculars", "1_35"); //device
				}},
				new HashMap<>()
				{{
					put("boba", "105_4");
					put("vader", "7_175");
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
		 * Game Text: USED: Cancel Force drain bonus from IT-O this turn.
		 * 		LOST: During your battle phase, any of your escorted captives at same site may initiate and participate
		 * 		in one battle (they may not use weapons or devices and you may not voluntarily forfeit or relocate them).
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

		scn.SkipToLSTurn(Phase.BATTLE);

		assertTrue(scn.LSLostInterruptPlayAvailable(fury));
		scn.LSPlayLostInterrupt(fury);

		assertTrue(scn.LSDecisionAvailable("Choose escorted captives to participate in battle"));
		scn.LSChooseCard(chewie);

		assertTrue(scn.DSDecisionAvailable("Optional responses"));
		scn.PassCardAndForceUseResponses();
		scn.PassBattleStartResponses();

		assertAtLocation(site, chewie);
		assertFalse(scn.IsAttachedTo(chewie, boba));
		assertTrue(scn.IsActiveBattle());
		assertTrue(scn.IsParticipatingInBattle(chewie));
		assertTrue(scn.IsParticipatingInBattle(boba));

		assertTrue(scn.LSDecisionAvailable("Choose weapons segment action to play or Pass"));
	}

	@Test
	public void CaptiveFuryCaptiveCannotUseWeapons() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var fury = scn.GetLSCard("fury");
		var chewie = scn.GetLSCard("chewie");
		var bowcaster = scn.GetLSCard("bowcaster");
		scn.MoveCardsToLSHand(fury);

		var site = scn.GetLSStartingLocation();

		var boba = scn.GetDSCard("boba");

		scn.StartGame();

		scn.MoveCardsToLocation(site, boba, chewie);
		scn.AttachCardsTo(chewie, bowcaster);
		scn.CaptureCardWith(boba, chewie);

		scn.SkipToLSTurn(Phase.BATTLE);

		scn.LSPlayLostInterrupt(fury);
		scn.LSChooseCard(chewie);
		scn.PassCardAndForceUseResponses();
		scn.PassBattleStartResponses();

		assertTrue(scn.LSDecisionAvailable("Choose weapons segment action to play or Pass"));

		assertFalse(scn.LSCardActionAvailable(bowcaster));
	}

	@Test
	public void CaptiveFuryCaptiveCannotUseDevices() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var fury = scn.GetLSCard("fury");
		var chewie = scn.GetLSCard("chewie");
		var eb = scn.GetLSCard("electrobinoculars");
		scn.MoveCardsToLSHand(fury);

		var site = scn.GetLSStartingLocation();

		var boba = scn.GetDSCard("boba");

		scn.StartGame();

		scn.MoveCardsToLocation(site, boba, chewie);
		scn.AttachCardsTo(chewie, eb);
		scn.CaptureCardWith(boba, chewie);

		scn.SkipToLSTurn(Phase.BATTLE);

		scn.LSPlayLostInterrupt(fury);
		scn.LSChooseCard(chewie);
		scn.PassCardAndForceUseResponses();
		scn.PassBattleStartResponses();

		assertFalse(scn.LSCardActionAvailable(eb));
	}

	@Test
	public void CaptiveFuryCaptiveCannotBeVoluntarilyForfeit() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var fury = scn.GetLSCard("fury");
		var chewie = scn.GetLSCard("chewie");
		scn.MoveCardsToLSHand(fury);

		var site = scn.GetLSStartingLocation();

		var vader = scn.GetDSCard("vader");

		scn.StartGame();

		scn.MoveCardsToLocation(site, vader);
		scn.CaptureCardWith(vader, chewie);

		scn.SkipToLSTurn(Phase.BATTLE);

		scn.LSPlayLostInterrupt(fury);
		scn.LSChooseCard(chewie);
		scn.PassCardAndForceUseResponses();
		scn.SkipToAttritionOrBattleDamage(true);

		//battle damage

		assertTrue(scn.DSWonBattle());
		assertEquals(1, scn.GetUnpaidLSAttrition());
		assertTrue(scn.AwaitingLSAttritionPayment());
		assertFalse(scn.LSHasCardChoiceAvailable(chewie));
	}

	@Test
	public void CaptiveFuryCaptiveCanBeForcefullyForfeit() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var fury = scn.GetLSCard("fury");
		var chewie = scn.GetLSCard("chewie");
		scn.MoveCardsToLSHand(fury);

		var site = scn.GetLSStartingLocation();

		var boba = scn.GetDSCard("boba");

		scn.StartGame();

		scn.MoveCardsToLocation(site, boba);
		scn.CaptureCardWith(boba, chewie);

		scn.SkipToLSTurn(Phase.BATTLE);

		assertTrue(scn.LSLostInterruptPlayAvailable(fury));
		scn.LSPlayLostInterrupt(fury);

		assertTrue(scn.LSDecisionAvailable("Choose escorted captives to participate in battle"));

		assertTrue(false);
	}

	@Test
	public void CaptiveFuryCaptiveCannotBeRelocated() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var fury = scn.GetLSCard("fury");
		var chewie = scn.GetLSCard("chewie");
		scn.MoveCardsToLSHand(fury);

		var site = scn.GetLSStartingLocation();

		var boba = scn.GetDSCard("boba");

		scn.StartGame();

		scn.MoveCardsToLocation(site, boba);
		scn.CaptureCardWith(boba, chewie);

		scn.SkipToLSTurn(Phase.BATTLE);

		assertTrue(scn.LSLostInterruptPlayAvailable(fury));
		scn.LSPlayLostInterrupt(fury);

		assertTrue(scn.LSDecisionAvailable("Choose escorted captives to participate in battle"));

		assertTrue(false);
	}

	@Test
	public void CaptiveFuryRelocatesCaptiveEvenIfEnclosed() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var fury = scn.GetLSCard("fury");
		var chewie = scn.GetLSCard("chewie");
		scn.MoveCardsToLSHand(fury);

		var site = scn.GetLSStartingLocation();

		var boba = scn.GetDSCard("boba");

		scn.StartGame();

		scn.MoveCardsToLocation(site, boba);
		scn.CaptureCardWith(boba, chewie);

		scn.SkipToLSTurn(Phase.BATTLE);

		assertTrue(scn.LSLostInterruptPlayAvailable(fury));
		scn.LSPlayLostInterrupt(fury);

		assertTrue(scn.LSDecisionAvailable("Choose escorted captives to participate in battle"));

		assertTrue(false);
	}

	@Test
	public void CaptiveFuryDoesNotTriggerRelease() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var fury = scn.GetLSCard("fury");
		var chewie = scn.GetLSCard("chewie");
		scn.MoveCardsToLSHand(fury);

		var site = scn.GetLSStartingLocation();

		var boba = scn.GetDSCard("boba");

		scn.StartGame();

		scn.MoveCardsToLocation(site, boba);
		scn.CaptureCardWith(boba, chewie);

		scn.SkipToLSTurn(Phase.BATTLE);

		assertTrue(scn.LSLostInterruptPlayAvailable(fury));
		scn.LSPlayLostInterrupt(fury);

		assertTrue(scn.LSDecisionAvailable("Choose escorted captives to participate in battle"));

		assertTrue(false);
	}

	@Test
	public void CaptiveFuryIncludesNonCaptivesInBattleIfEligible() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var fury = scn.GetLSCard("fury");
		var chewie = scn.GetLSCard("chewie");
		scn.MoveCardsToLSHand(fury);

		var site = scn.GetLSStartingLocation();

		var boba = scn.GetDSCard("boba");

		scn.StartGame();

		scn.MoveCardsToLocation(site, boba);
		scn.CaptureCardWith(boba, chewie);

		scn.SkipToLSTurn(Phase.BATTLE);

		assertTrue(scn.LSLostInterruptPlayAvailable(fury));
		scn.LSPlayLostInterrupt(fury);

		assertTrue(scn.LSDecisionAvailable("Choose escorted captives to participate in battle"));

		assertTrue(false);
	}

	@Test
	public void CaptiveFuryCannotBeUsedOnCaptiveThatBattledEarlierThisTurn() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var fury = scn.GetLSCard("fury");
		var chewie = scn.GetLSCard("chewie");
		scn.MoveCardsToLSHand(fury);

		var site = scn.GetLSStartingLocation();

		var boba = scn.GetDSCard("boba");

		scn.StartGame();

		scn.MoveCardsToLocation(site, boba);
		scn.CaptureCardWith(boba, chewie);

		scn.SkipToLSTurn(Phase.BATTLE);

		assertTrue(scn.LSLostInterruptPlayAvailable(fury));
		scn.LSPlayLostInterrupt(fury);

		assertTrue(scn.LSDecisionAvailable("Choose escorted captives to participate in battle"));

		assertTrue(false);
	}

	@Test
	public void CaptiveFuryCancelsBattleIfCaptiveHasNoPresence() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var fury = scn.GetLSCard("fury");
		var chewie = scn.GetLSCard("chewie");
		scn.MoveCardsToLSHand(fury);

		var site = scn.GetLSStartingLocation();

		var boba = scn.GetDSCard("boba");

		scn.StartGame();

		scn.MoveCardsToLocation(site, boba);
		scn.CaptureCardWith(boba, chewie);

		scn.SkipToLSTurn(Phase.BATTLE);

		assertTrue(scn.LSLostInterruptPlayAvailable(fury));
		scn.LSPlayLostInterrupt(fury);

		assertTrue(scn.LSDecisionAvailable("Choose escorted captives to participate in battle"));

		assertTrue(false);
	}

	@Test
	public void CaptiveFuryCancelsBattleIfCaptorHasNoPresence() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var fury = scn.GetLSCard("fury");
		var chewie = scn.GetLSCard("chewie");
		scn.MoveCardsToLSHand(fury);

		var site = scn.GetLSStartingLocation();

		var boba = scn.GetDSCard("boba");

		scn.StartGame();

		scn.MoveCardsToLocation(site, boba);
		scn.CaptureCardWith(boba, chewie);

		scn.SkipToLSTurn(Phase.BATTLE);

		assertTrue(scn.LSLostInterruptPlayAvailable(fury));
		scn.LSPlayLostInterrupt(fury);

		assertTrue(scn.LSDecisionAvailable("Choose escorted captives to participate in battle"));

		assertTrue(false);
	}

	@Test
	public void CaptiveFuryReturnsCaptiveToCaptorAfterBattleIfBothAlive() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var fury = scn.GetLSCard("fury");
		var chewie = scn.GetLSCard("chewie");
		scn.MoveCardsToLSHand(fury);

		var site = scn.GetLSStartingLocation();

		var boba = scn.GetDSCard("boba");

		scn.StartGame();

		scn.MoveCardsToLocation(site, boba);
		scn.CaptureCardWith(boba, chewie);

		scn.SkipToLSTurn(Phase.BATTLE);

		assertTrue(scn.LSLostInterruptPlayAvailable(fury));
		scn.LSPlayLostInterrupt(fury);

		assertTrue(scn.LSDecisionAvailable("Choose escorted captives to participate in battle"));

		assertTrue(false);
	}

	@Test
	public void CaptiveFuryReleasesBattlingCaptiveIfCaptorIsKilled() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var fury = scn.GetLSCard("fury");
		var chewie = scn.GetLSCard("chewie");
		scn.MoveCardsToLSHand(fury);

		var site = scn.GetLSStartingLocation();

		var boba = scn.GetDSCard("boba");

		scn.StartGame();

		scn.MoveCardsToLocation(site, boba);
		scn.CaptureCardWith(boba, chewie);

		scn.SkipToLSTurn(Phase.BATTLE);

		assertTrue(scn.LSLostInterruptPlayAvailable(fury));
		scn.LSPlayLostInterrupt(fury);

		assertTrue(scn.LSDecisionAvailable("Choose escorted captives to participate in battle"));

		assertTrue(false);
	}

	@Test
	public void CaptiveFuryReleasesBattlingCaptiveIfCaptorIsMissing() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var fury = scn.GetLSCard("fury");
		var chewie = scn.GetLSCard("chewie");
		scn.MoveCardsToLSHand(fury);

		var site = scn.GetLSStartingLocation();

		var boba = scn.GetDSCard("boba");

		scn.StartGame();

		scn.MoveCardsToLocation(site, boba);
		scn.CaptureCardWith(boba, chewie);

		scn.SkipToLSTurn(Phase.BATTLE);

		assertTrue(scn.LSLostInterruptPlayAvailable(fury));
		scn.LSPlayLostInterrupt(fury);

		assertTrue(scn.LSDecisionAvailable("Choose escorted captives to participate in battle"));

		assertTrue(false);
	}

	@Test
	public void CaptiveFuryDoesNotReturnCaptiveAfterBattleIfKilled() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var fury = scn.GetLSCard("fury");
		var chewie = scn.GetLSCard("chewie");
		scn.MoveCardsToLSHand(fury);

		var site = scn.GetLSStartingLocation();

		var boba = scn.GetDSCard("boba");

		scn.StartGame();

		scn.MoveCardsToLocation(site, boba);
		scn.CaptureCardWith(boba, chewie);

		scn.SkipToLSTurn(Phase.BATTLE);

		assertTrue(scn.LSLostInterruptPlayAvailable(fury));
		scn.LSPlayLostInterrupt(fury);

		assertTrue(scn.LSDecisionAvailable("Choose escorted captives to participate in battle"));

		assertTrue(false);
	}

	@Test
	public void CaptiveFuryDoesNotReturnCaptiveAfterBattleIfMissing() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var fury = scn.GetLSCard("fury");
		var chewie = scn.GetLSCard("chewie");
		scn.MoveCardsToLSHand(fury);

		var site = scn.GetLSStartingLocation();

		var boba = scn.GetDSCard("boba");

		scn.StartGame();

		scn.MoveCardsToLocation(site, boba);
		scn.CaptureCardWith(boba, chewie);

		scn.SkipToLSTurn(Phase.BATTLE);

		assertTrue(scn.LSLostInterruptPlayAvailable(fury));
		scn.LSPlayLostInterrupt(fury);

		assertTrue(scn.LSDecisionAvailable("Choose escorted captives to participate in battle"));

		assertTrue(false);
	}

	@Test
	public void CaptiveFuryDoesNotReturnCaptiveIfCapturedByAnotherCaptor() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var fury = scn.GetLSCard("fury");
		var chewie = scn.GetLSCard("chewie");
		scn.MoveCardsToLSHand(fury);

		var site = scn.GetLSStartingLocation();

		var boba = scn.GetDSCard("boba");

		scn.StartGame();

		scn.MoveCardsToLocation(site, boba);
		scn.CaptureCardWith(boba, chewie);

		scn.SkipToLSTurn(Phase.BATTLE);

		assertTrue(scn.LSLostInterruptPlayAvailable(fury));
		scn.LSPlayLostInterrupt(fury);

		assertTrue(scn.LSDecisionAvailable("Choose escorted captives to participate in battle"));

		assertTrue(false);
	}

	@Test
	public void CaptiveFuryMustChooseAtLeast1CaptiveButMayIgnoreTheRest() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var fury = scn.GetLSCard("fury");
		var chewie = scn.GetLSCard("chewie");
		scn.MoveCardsToLSHand(fury);

		var site = scn.GetLSStartingLocation();

		var boba = scn.GetDSCard("boba");

		scn.StartGame();

		scn.MoveCardsToLocation(site, boba);
		scn.CaptureCardWith(boba, chewie);

		scn.SkipToLSTurn(Phase.BATTLE);

		assertTrue(scn.LSLostInterruptPlayAvailable(fury));
		scn.LSPlayLostInterrupt(fury);

		assertTrue(scn.LSDecisionAvailable("Choose escorted captives to participate in battle"));

		assertTrue(false);
	}

	@Test
	public void CaptiveFuryCanChooseMultipleCaptivesAtSameSiteToBattle() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var fury = scn.GetLSCard("fury");
		var chewie = scn.GetLSCard("chewie");
		scn.MoveCardsToLSHand(fury);

		var site = scn.GetLSStartingLocation();

		var boba = scn.GetDSCard("boba");

		scn.StartGame();

		scn.MoveCardsToLocation(site, boba);
		scn.CaptureCardWith(boba, chewie);

		scn.SkipToLSTurn(Phase.BATTLE);

		assertTrue(scn.LSLostInterruptPlayAvailable(fury));
		scn.LSPlayLostInterrupt(fury);

		assertTrue(scn.LSDecisionAvailable("Choose escorted captives to participate in battle"));

		assertTrue(false);
	}

	@Test
	public void CaptiveFuryStillPermitsCaptiveSpottingEffectsToWorkDuringBattle() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var fury = scn.GetLSCard("fury");
		var chewie = scn.GetLSCard("chewie");
		scn.MoveCardsToLSHand(fury);

		var site = scn.GetLSStartingLocation();

		var boba = scn.GetDSCard("boba");

		scn.StartGame();

		scn.MoveCardsToLocation(site, boba);
		scn.CaptureCardWith(boba, chewie);

		scn.SkipToLSTurn(Phase.BATTLE);

		assertTrue(scn.LSLostInterruptPlayAvailable(fury));
		scn.LSPlayLostInterrupt(fury);

		assertTrue(scn.LSDecisionAvailable("Choose escorted captives to participate in battle"));

		assertTrue(false);
	}

}
