package com.gempukku.swccgo.rules.state;

import com.gempukku.swccgo.common.MovementDirection;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;
import org.junit.Test;

import java.util.HashMap;

import static com.gempukku.swccgo.framework.Assertions.assertAtLocation;
import static org.junit.Assert.*;

public class FrozenTests {
	protected VirtualTableScenario GetScenario() {
		return new VirtualTableScenario(
				new HashMap<>()
				{{
					put("chewie", "200_5");
					put("protector", "10_3"); //Chewbacca persona
				}},
				new HashMap<>()
				{{
					put("boba", "5_91");
					put("tube", "1_308");
				}},
				10,
				10,
				VirtualTableScenario.DefaultGroundLSLocation,
				VirtualTableScenario.DefaultGroundDSLocation,
				StartingSetup.NoLSStarters,
				StartingSetup.NoDSStarters,
				VirtualTableScenario.NoLSShields,
				VirtualTableScenario.NoDSShields,
				VirtualTableScenario.Open
		);
	}

	@Test
	public void FrozenCaptivesAreNotActive() {
		var scn = GetScenario();

		var chewie = scn.GetLSCard("chewie");
		var site = scn.GetLSStartingLocation();

		var boba = scn.GetDSCard("boba");

		scn.StartGame();

		scn.MoveCardsToLocation(site, boba, chewie);
		scn.FreezeCard(chewie);

		assertTrue(chewie.isCaptive());
		assertFalse(scn.IsCardActive(chewie));
	}

	@Test
	public void FrozenCaptivesHaveZeroedStats() {
		var scn = GetScenario();

		var chewie = scn.GetLSCard("chewie");
		var site = scn.GetLSStartingLocation();

		var boba = scn.GetDSCard("boba");

		scn.StartGame();

		scn.MoveCardsToLocation(site, boba, chewie);
		scn.FreezeCard(chewie);

		assertEquals(0, scn.GetPower(chewie), scn.epsilon);
		assertEquals(0, scn.GetAbility(chewie), scn.epsilon);
		assertEquals(0, scn.GetLandspeed(chewie), scn.epsilon);
	}

	@Test
	public void FrozenCaptivesCanBeTakenIntoCustodyByWarriorAtSameSiteDuringMovePhase() {
		var scn = GetScenario();

		var chewie = scn.GetLSCard("chewie");
		var site = scn.GetLSStartingLocation();

		var stormtrooper = scn.GetDSFiller(1);

		scn.StartGame();

		scn.MoveCardsToLocation(site, stormtrooper, chewie);
		scn.FreezeCard(chewie);

		scn.SkipToPhase(Phase.MOVE);

		assertTrue(chewie.isCaptive());
		assertTrue(chewie.isFrozen());
		assertNull(chewie.getEscort());
		assertEquals(0, stormtrooper.getCardsEscorting().size());

		assertTrue(scn.DSActionAvailable("Take unattended frozen captive into custody"));
		scn.DSChooseAction("Take unattended frozen captive into custody");
		scn.PassAllResponses();

		assertEquals(stormtrooper, chewie.getEscort());
		assertEquals(stormtrooper, chewie.getAttachedTo());
		assertEquals(1, stormtrooper.getCardsEscorting().size());
		assertTrue(stormtrooper.getCardsEscorting().contains(chewie));
		assertEquals(1, stormtrooper.getCardsAttached().size());
		assertTrue(stormtrooper.getCardsAttached().contains(chewie));
	}

	@Test
	public void FrozenCaptivesCanBeTakenIntoCustodyByBountyHunterAtSameSiteDuringMovePhase() {
		var scn = GetScenario();

		var chewie = scn.GetLSCard("chewie");
		var site = scn.GetLSStartingLocation();

		var boba = scn.GetDSCard("boba");

		scn.StartGame();

		scn.MoveCardsToLocation(site, boba, chewie);
		scn.FreezeCard(chewie);

		scn.SkipToPhase(Phase.MOVE);

		assertTrue(chewie.isCaptive());
		assertTrue(chewie.isFrozen());
		assertNull(chewie.getEscort());
		assertEquals(0, boba.getCardsEscorting().size());
		assertNull(chewie.getAttachedTo());
		assertEquals(0, boba.getCardsAttached().size());

		assertTrue(scn.DSActionAvailable("Take unattended frozen captive into custody"));
		scn.DSChooseAction("Take unattended frozen captive into custody");
		scn.PassAllResponses();

		assertEquals(boba, chewie.getEscort());
		assertEquals(boba, chewie.getAttachedTo());
		assertEquals(1, boba.getCardsEscorting().size());
		assertTrue(boba.getCardsEscorting().contains(chewie));
		assertEquals(1, boba.getCardsAttached().size());
		assertTrue(boba.getCardsAttached().contains(chewie));
	}

	@Test
	public void FrozenCaptivesCanBeLeftUnattendedByEscortAtSameSiteDuringMovePhase() {
		var scn = GetScenario();

		var chewie = scn.GetLSCard("chewie");
		var site = scn.GetLSStartingLocation();

		var boba = scn.GetDSCard("boba");

		scn.StartGame();

		scn.MoveCardsToLocation(site, boba, chewie);
		scn.FreezeCard(chewie);
		scn.CaptureCardWith(boba, chewie);

		scn.SkipToPhase(Phase.MOVE);

		assertTrue(chewie.isCaptive());
		assertTrue(chewie.isFrozen());
		assertEquals(boba, chewie.getEscort());
		assertEquals(boba, chewie.getAttachedTo());
		assertEquals(1, boba.getCardsEscorting().size());
		assertTrue(boba.getCardsEscorting().contains(chewie));
		assertEquals(1, boba.getCardsAttached().size());
		assertTrue(boba.getCardsAttached().contains(chewie));

		assertTrue(scn.DSActionAvailable("Leave frozen captive unattended"));
		scn.DSChooseAction("Leave frozen captive unattended");
		scn.PassAllResponses();

		assertNull(chewie.getEscort());
		assertNull(chewie.getAttachedTo());
		assertAtLocation(site, chewie);
		assertTrue(chewie.isCaptive());
		assertTrue(chewie.isFrozen());

		assertEquals(0, boba.getCardsEscorting().size());
		assertEquals(0, boba.getCardsAttached().size());

		scn.LSPass();
		assertFalse(scn.DSActionAvailable("Leave frozen captive unattended"));
	}
}
