package com.gempukku.swccgo.cards.set5.light;

import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;
import org.junit.Test;

import java.util.HashMap;

import static com.gempukku.swccgo.framework.Assertions.assertAtLocation;
import static com.gempukku.swccgo.framework.Assertions.assertInZone;
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
					put("path", "5_62");
					put("threepio", "1_5");

					put("core_tunnel", "7_112"); //cloud city inside location
				}},
				new HashMap<>()
				{{
					put("boba", "5_91");
					put("bobas_blaster", "5_179");
					put("vader", "7_175");
					put("ig88", "109_11");
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

		assertTrue(scn.LSPlayLostInterruptAvailable(fury));
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
		scn.SkipToDamageSegment(true);

		assertTrue(scn.DSWonBattle());
		//Vader drew destiny 1
		assertEquals(1, scn.GetUnpaidLSAttrition());
		// Vader 6 + destiny 1 > Chewbacca 6
		assertEquals(1, scn.GetUnpaidLSBattleDamage());
		assertTrue(scn.AwaitingLSAttritionPayment());
		assertTrue(scn.AwaitingLSBattleDamagePayment());
		assertFalse(chewie.isHit());
		assertFalse(scn.LSHasCardChoiceAvailable(chewie));

		assertEquals(15, scn.GetLSLifeForceRemaining());
		scn.PayLSBattleDamageFromReserveDeck();
		assertEquals(14, scn.GetLSLifeForceRemaining());

		assertFalse(scn.IsActiveBattle());
	}

	@Test
	public void CaptiveFuryCaptiveCanBeForfeitIfHit() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var fury = scn.GetLSCard("fury");
		var chewie = scn.GetLSCard("chewie");
		scn.MoveCardsToLSHand(fury);

		var site = scn.GetLSStartingLocation();

		var boba = scn.GetDSCard("boba");
		var bobas_blaster = scn.GetDSCard("bobas_blaster");
		var vader = scn.GetDSCard("vader");

		scn.StartGame();

		scn.MoveCardsToLocation(site, boba, vader);
		scn.AttachCardsTo(boba, bobas_blaster);
		scn.CaptureCardWith(boba, chewie);

		scn.SkipToLSTurn(Phase.BATTLE);

		assertTrue(scn.LSPlayLostInterruptAvailable(fury));
		scn.LSPlayLostInterrupt(fury);
		scn.LSChooseCard(chewie);
		scn.PassCardAndForceUseResponses();
		scn.PassBattleStartResponses();
		scn.LSPass();

		assertTrue(scn.DSActionAvailable(bobas_blaster));
		scn.DSUseCardAction(bobas_blaster);

		assertTrue(scn.DSDecisionAvailable("Choose target"));
		scn.DSChooseCard(chewie);
		scn.PassForceUseResponses();
		scn.PrepareDSDestiny(7);
		scn.PassWeaponFireWithDestinyDraw();

		assertTrue(chewie.isHit());
		//Boba's blaster can keep firing, we just want the once
		scn.DSChooseNo();

		scn.SkipToDamageSegment(true);

		assertTrue(scn.DSWonBattle());
		//DS drew destiny 1
		assertEquals(1, scn.GetUnpaidLSAttrition());
		// Vader 6 + Boba Fett 3 + destiny 1 + gun destiny 1 > Chewbacca 6
		assertEquals(5, scn.GetUnpaidLSBattleDamage());

		assertTrue(scn.AwaitingLSAttritionPayment());
		assertTrue(scn.AwaitingLSBattleDamagePayment());
		assertTrue(chewie.isHit());
		assertTrue(scn.LSHasCardChoiceAvailable(chewie));

		assertEquals(15, scn.GetLSLifeForceRemaining());
		scn.PayLSBattleDamageFromCardInPlay(chewie);
		assertEquals(15, scn.GetLSLifeForceRemaining());
		assertInZone(Zone.LOST_PILE, chewie);

		assertFalse(scn.IsActiveBattle());
	}

	@Test
	public void CaptiveFuryCaptiveCannotBeRelocated() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var fury = scn.GetLSCard("fury");
		var chewie = scn.GetLSCard("chewie");
		var path = scn.GetLSCard("path");
		var core_tunnel = scn.GetLSCard("core_tunnel");
		scn.MoveCardsToLSHand(fury, path, core_tunnel);

		var site = scn.GetLSStartingLocation();

		var boba = scn.GetDSCard("boba");

		scn.StartGame();

		scn.MoveCardsToLocation(site, boba);
		scn.CaptureCardWith(boba, chewie);

		scn.SkipToLSTurn(Phase.DEPLOY);
		scn.LSDeployLocation(core_tunnel);
		scn.PassCardPlayResponses();
		scn.SkipToPhase(Phase.BATTLE);

		assertTrue(scn.LSPlayLostInterruptAvailable(fury));
		scn.LSPlayLostInterrupt(fury);
		scn.LSChooseCard(chewie);
		scn.PassCardAndForceUseResponses();
		scn.PassBattleStartResponses();

		assertFalse(scn.LSCardPlayAvailable(path));
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

		assertTrue(scn.LSPlayLostInterruptAvailable(fury));
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

		assertTrue(scn.LSPlayLostInterruptAvailable(fury));
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

		assertTrue(scn.LSPlayLostInterruptAvailable(fury));
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

		assertTrue(scn.LSPlayLostInterruptAvailable(fury));
		scn.LSPlayLostInterrupt(fury);

		assertTrue(scn.LSDecisionAvailable("Choose escorted captives to participate in battle"));

		assertTrue(false);
	}

	@Test
	public void CaptiveFuryCancelsBattleIfCaptiveHasNoPresence() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var fury = scn.GetLSCard("fury");
		var threepio = scn.GetLSCard("threepio");
		scn.MoveCardsToLSHand(fury);

		var site = scn.GetLSStartingLocation();

		var boba = scn.GetDSCard("boba");

		scn.StartGame();

		scn.MoveCardsToLocation(site, boba);
		scn.CaptureCardWith(boba, threepio);

		scn.SkipToLSTurn(Phase.BATTLE);

		assertTrue(scn.LSPlayLostInterruptAvailable(fury));
		scn.LSPlayLostInterrupt(fury);
		scn.LSChooseCard(threepio);
		scn.PassCardAndForceUseResponses();

		assertTrue(scn.IsActiveBattle());
		scn.PassBattleStartResponses();

		assertFalse(scn.IsActiveBattle());
	}

	@Test
	public void CaptiveFuryCancelsBattleIfCaptorHasNoPresence() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var fury = scn.GetLSCard("fury");
		var chewie = scn.GetLSCard("chewie");
		scn.MoveCardsToLSHand(fury);

		var site = scn.GetLSStartingLocation();

		var ig88 = scn.GetDSCard("ig88");

		scn.StartGame();

		scn.MoveCardsToLocation(site, ig88);
		scn.CaptureCardWith(ig88, chewie);

		scn.SkipToLSTurn(Phase.BATTLE);

		assertTrue(scn.LSPlayLostInterruptAvailable(fury));
		scn.LSPlayLostInterrupt(fury);
		scn.LSChooseCard(chewie);
		scn.PassCardAndForceUseResponses();

		assertTrue(scn.IsActiveBattle());
		scn.PassBattleStartResponses();

		assertFalse(scn.IsActiveBattle());
	}

	@Test
	public void CaptiveFuryReturnsCaptiveToCaptorAfterBattleIfBothAlive() throws DecisionResultInvalidException {
		var scn = GetScenario();

		var fury = scn.GetLSCard("fury");
		var chewie = scn.GetLSCard("chewie");
		scn.MoveCardsToLSHand(fury);

		var site = scn.GetLSStartingLocation();

		var vader = scn.GetDSCard("vader");

		scn.StartGame();

		scn.MoveCardsToLocation(site, vader);
		scn.CaptureCardWith(vader, chewie);

		assertTrue(chewie.isCaptive());

		scn.SkipToLSTurn(Phase.BATTLE);
		scn.LSPlayLostInterrupt(fury);

		assertTrue(chewie.isCaptive());
		scn.LSChooseCard(chewie);
		assertTrue(chewie.isCaptive());

		scn.PassCardAndForceUseResponses();
		scn.SkipToDamageSegment(true);

		assertTrue(scn.DSWonBattle());
		scn.PayLSBattleDamageFromReserveDeck();
		assertFalse(scn.IsActiveBattle());

		assertTrue(chewie.isCaptive());
		assertTrue(scn.IsAttachedTo(vader, chewie));
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

		assertTrue(scn.LSPlayLostInterruptAvailable(fury));
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

		assertTrue(scn.LSPlayLostInterruptAvailable(fury));
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

		assertTrue(scn.LSPlayLostInterruptAvailable(fury));
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

		assertTrue(scn.LSPlayLostInterruptAvailable(fury));
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

		assertTrue(scn.LSPlayLostInterruptAvailable(fury));
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

		assertTrue(scn.LSPlayLostInterruptAvailable(fury));
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

		assertTrue(scn.LSPlayLostInterruptAvailable(fury));
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

		assertTrue(scn.LSPlayLostInterruptAvailable(fury));
		scn.LSPlayLostInterrupt(fury);

		assertTrue(scn.LSDecisionAvailable("Choose escorted captives to participate in battle"));

		assertTrue(false);
	}

	@Test
	public void CaptiveFuryPermitsUsedHumanShieldToKillCaptive() throws DecisionResultInvalidException {
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

		assertTrue(scn.LSPlayLostInterruptAvailable(fury));
		scn.LSPlayLostInterrupt(fury);

		assertTrue(scn.LSDecisionAvailable("Choose escorted captives to participate in battle"));

		assertTrue(false);
	}

	@Test
	public void CaptiveFuryPermitsLostHumanShieldToForfeitCaptive() throws DecisionResultInvalidException {
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

		assertTrue(scn.LSPlayLostInterruptAvailable(fury));
		scn.LSPlayLostInterrupt(fury);

		assertTrue(scn.LSDecisionAvailable("Choose escorted captives to participate in battle"));

		assertTrue(false);
	}

}
