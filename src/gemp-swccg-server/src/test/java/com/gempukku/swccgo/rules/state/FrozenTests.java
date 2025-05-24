package com.gempukku.swccgo.rules.state;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

public class FrozenTests {
	protected VirtualTableScenario GetScenario() throws DecisionResultInvalidException {
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
				VirtualTableScenario.NoLSStarters,
				VirtualTableScenario.NoDSStarters,
				VirtualTableScenario.NoLSShields,
				VirtualTableScenario.NoDSShields,
				VirtualTableScenario.Open
		);
	}

	@Test
	public void FrozenCaptivesAreNotActive() throws DecisionResultInvalidException {
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
	public void FrozenCaptivesHaveZeroedStats() throws DecisionResultInvalidException {
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
}
