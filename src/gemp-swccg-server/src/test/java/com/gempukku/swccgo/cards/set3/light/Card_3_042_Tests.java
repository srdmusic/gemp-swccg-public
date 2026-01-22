package com.gempukku.swccgo.cards.set3.light;

import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.CardType;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class Card_3_042_Tests {
	protected VirtualTableScenario GetScenario() {
		return new VirtualTableScenario(
				new HashMap<>()
				{{
					put("fallback", "3_042"); //Fall Back!
                    put("rogue1", "3_066"); //(enclosed vehicle)
                }},
				new HashMap<>()
				{{
                    put("hothsite1","3_150"); //Hoth: Wampa Cave (7th marker)
                    put("hothsite2","3_148"); //Hoth: Ice Plains (5th marker)
                    put("hothsite3","3_149"); //Hoth: North Ridge (4th marker)
                    put("hothsite4","3_144"); //Hoth: Defensive Perimeter (3rd marker)
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
	public void FallBackStatsAndKeywordsAreCorrect() {
		/**
		 * Title: Fall Back!
		 * Uniqueness: Unrestricted
		 * Side: Light
		 * Type: Interrupt
		 * Subtype: Lost
		 * Destiny: 5
		 * Icons: Interrupt, Hoth
		 * Game Text: If opponent just initiated a battle at an exterior site with more than double your total power,
         *      use 1 Force to target an adjacent site where opponent has no presence.
         *      All your characters present in battle move away (for free) to the target site. The battle is canceled.
		 * Lore: 'K-one zero...all troops disengage.'
		 * Set: Hoth
		 * Rarity: C2
		 */

		var scn = GetScenario();

		var card = scn.GetLSCard("fallback").getBlueprint();

		assertEquals("Fall Back!", card.getTitle());
		assertEquals(Uniqueness.UNRESTRICTED, card.getUniqueness());
		assertEquals(Side.LIGHT, card.getSide());
        scn.BlueprintCardTypeCheck(card, new ArrayList<>() {{
            add(CardType.INTERRUPT);
        }});
		assertEquals(CardSubtype.LOST, card.getCardSubtype());
		assertEquals(5, card.getDestiny(), scn.epsilon);
        scn.BlueprintKeywordCheck(card, new ArrayList<>() {{
        }});
        scn.BlueprintPersonaCheck(card, new ArrayList<>() {{
        }});
        scn.BlueprintIconCheck(card, new ArrayList<>() {{
            add(Icon.INTERRUPT);
            add(Icon.HOTH);
        }});
        assertEquals(ExpansionSet.HOTH,card.getExpansionSet());
        assertEquals(Rarity.C2,card.getRarity());

	}

	@Test
	public void FallBackWorks() {
		var scn = GetScenario();

		var fallback = scn.GetLSCard("fallback");
        var rebelTrooper1 = scn.GetLSFiller(1);

        var hothsite1 = scn.GetDSCard("hothsite1");
        var hothsite2 = scn.GetDSCard("hothsite2");
        var hothsite3 = scn.GetDSCard("hothsite3");
        var hothsite4 = scn.GetDSCard("hothsite4");
        var trooper1 = scn.GetDSFiller(1);
        var trooper2 = scn.GetDSFiller(2);
        var trooper3 = scn.GetDSFiller(3);

		scn.StartGame();

		scn.MoveCardsToLSHand(fallback);

        scn.MoveLocationToTable(hothsite1);
        scn.MoveLocationToTable(hothsite2);
        scn.MoveLocationToTable(hothsite3);
        scn.MoveLocationToTable(hothsite4);

        scn.MoveCardsToLocation(hothsite2,trooper1,trooper2,trooper3,rebelTrooper1);

        scn.LSActivateForceCheat(1); //enough to pay Fall Back! cost

		scn.SkipToPhase(Phase.BATTLE);
        scn.DSInitiateBattle(hothsite2);

        assertTrue(scn.LSDecisionAvailable("Battle just initiated")); //Battle just initiated at - Optional responses
        assertTrue(scn.LSCardPlayAvailable(fallback,"Fall back"));
        scn.LSPlayCard(fallback);

        assertTrue(scn.LSHasCardChoicesAvailable(hothsite1,hothsite3));
        assertFalse(scn.LSHasCardChoicesAvailable(hothsite2,hothsite4));
        scn.LSChooseCard(hothsite3);

        scn.DSPass(); //Use 1 Force - Optional responses
        scn.LSPass();

        scn.DSPass(); //Playing Fall Back! - Optional responses
        scn.LSPass();

        assertTrue(scn.LSDecisionAvailable("Choose next card to move away"));
        assertTrue(scn.LSHasCardChoicesAvailable(rebelTrooper1));
        scn.LSChooseCard(rebelTrooper1);

        ///automatically chooses, since only 1 option
        //assertTrue(scn.LSDecisionAvailable("Choose where to move"));
        //assertTrue(scn.LSHasCardChoicesAvailable(hothsite1,hothsite3));
        //assertFalse(scn.LSHasCardChoicesAvailable(hothsite2,hothsite4));
        //scn.LSChooseCard(hothsite3);

        scn.DSPass(); //MOVING_USING_LANDSPEED - Optional responses
        scn.LSPass();

        scn.DSPass(); //MOVED_USING_LANDSPEED - Optional responses
        scn.LSPass();

        scn.DSPass(); //PUT_IN_CARD_PILE_FROM_OFF_TABLE - Optional responses
        scn.LSPass();

        scn.DSPass(); //BATTLE_INITIATED - Optional responses
        scn.LSPass();

        //battle canceled here due to lack of presence?

        assertTrue(scn.AwaitingLSBattlePhaseActions());

        assertTrue(scn.CardsAtLocation(hothsite3,rebelTrooper1));
        assertEquals(1,scn.GetLSUsedPileCount()); //cost to play Fall Back!
        assertSame(Zone.TOP_OF_LOST_PILE,fallback.getZone());
	}

    @Test
    public void FallBackWorks2() {
        var scn = GetScenario();

        var fallback = scn.GetLSCard("fallback");
        var rebelTrooper1 = scn.GetLSFiller(1);
        var rebelTrooper2 = scn.GetLSFiller(2);
        var rogue1 = scn.GetLSCard("rogue1");

        var hothsite1 = scn.GetDSCard("hothsite1");
        var hothsite2 = scn.GetDSCard("hothsite2");
        var hothsite3 = scn.GetDSCard("hothsite3");
        var hothsite4 = scn.GetDSCard("hothsite4");
        var trooper1 = scn.GetDSFiller(1);
        var trooper2 = scn.GetDSFiller(2);
        var trooper3 = scn.GetDSFiller(3);

        scn.StartGame();

        scn.MoveCardsToLSHand(fallback);

        scn.MoveLocationToTable(hothsite1);
        scn.MoveLocationToTable(hothsite2);
        scn.MoveLocationToTable(hothsite3);
        scn.MoveLocationToTable(hothsite4);

        scn.MoveCardsToLocation(hothsite2,trooper1,trooper2,trooper3,rebelTrooper1,rogue1);
        scn.BoardAsPassenger(rogue1,rebelTrooper2);

        scn.LSActivateForceCheat(1); //enough to pay Fall Back! cost

        scn.SkipToPhase(Phase.BATTLE);
        scn.DSInitiateBattle(hothsite2);

        assertTrue(scn.LSDecisionAvailable("Battle just initiated")); //Battle just initiated at - Optional responses
        assertTrue(scn.LSCardPlayAvailable(fallback,"Fall back"));
        scn.LSPlayCard(fallback);

        assertTrue(scn.LSHasCardChoicesAvailable(hothsite1,hothsite3));
        assertFalse(scn.LSHasCardChoicesAvailable(hothsite2,hothsite4));
        scn.LSChooseCard(hothsite3);

        scn.DSPass(); //Use 1 Force - Optional responses
        scn.LSPass();

        scn.DSPass(); //Playing Fall Back! - Optional responses
        scn.LSPass();

        assertTrue(scn.LSDecisionAvailable("Choose next card to move away"));
        assertTrue(scn.LSHasCardChoicesAvailable(rebelTrooper1));
        assertFalse(scn.LSHasCardChoicesAvailable(rebelTrooper2,rogue1));
        scn.LSChooseCard(rebelTrooper1);

        ///automatically chooses, since only 1 option
        //assertTrue(scn.LSDecisionAvailable("Choose where to move"));
        //assertTrue(scn.LSHasCardChoicesAvailable(hothsite1,hothsite3));
        //assertFalse(scn.LSHasCardChoicesAvailable(hothsite2,hothsite4));
        //scn.LSChooseCard(hothsite3);

        scn.DSPass(); //MOVING_USING_LANDSPEED - Optional responses
        scn.LSPass();

        scn.DSPass(); //MOVED_USING_LANDSPEED - Optional responses
        scn.LSPass();

        scn.DSPass(); //BATTLE_CANCELED - Optional responses
        scn.LSPass();

        scn.DSPass(); //PUT_IN_CARD_PILE_FROM_OFF_TABLE - Optional responses
        scn.LSPass();

        scn.DSPass(); //BATTLE_INITIATED - Optional responses
        scn.LSPass();

        //battle canceled here due to lack of presence?

        assertTrue(scn.AwaitingLSBattlePhaseActions());

        assertTrue(scn.CardsAtLocation(hothsite3,rebelTrooper1));
        assertTrue(scn.CardsAtLocation(hothsite2,rogue1));
        assertTrue(scn.IsAboardAsPassenger(rogue1,rebelTrooper2));
        assertEquals(1,scn.GetLSUsedPileCount()); //cost to play Fall Back!
        assertSame(Zone.TOP_OF_LOST_PILE,fallback.getZone());
    }

}
