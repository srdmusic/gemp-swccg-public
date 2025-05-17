package com.gempukku.swccgo.framework;

import com.gempukku.swccgo.game.PhysicalCardImpl;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.timing.DefaultSwccgGame;
import com.gempukku.swccgo.logic.timing.DefaultUserFeedback;

import java.util.HashMap;

/**
 * This interface holds all the static definitions used throughout the test rig.  It is not a true interface, but
 * since Java does not support partial classes this will have to do.
 */
public interface TestBase {
	String DS = "DS Player";
	String LS = "LS Player";

	double epsilon = 0.001;

	HashMap<String, String> NoLSShields = new HashMap<>();
	HashMap<String, String> NoDSShields = new HashMap<>();
	HashMap<String, String> NoLSStarters = new HashMap<>();
	HashMap<String, String> NoDSStarters = new HashMap<>();

	/*
	 * The default location to be played by either player.  This will be included in the deck and automatically played
	 * at the start of the game.
	 *
	 * When choosing default locations, be sure to pick ones that leave both sides with identical lightsaber icons,
	 * whether that means that both are the same or that one is 1/2 and the other is 2/1.  This way testers do not need
	 * to remember nuances around default activation amounts being different per-side.
	 *
	 * Also ensure that any defaults that are chosen avoid altering the game state; ideally the sites would be blank,
	 * but if that cannot be arranged do not choose cards that grant modifiers (granting +/- X to any stat), as these
	 * inevitably are rakes that future tests will step on. Instead, fall back on locations that have optional actions
	 * that can be ignored.
	 */

	String DefaultGroundLSLocation = "5_079"; // Cloud City: Chasm Walkway
	String DefaultGroundDSLocation = "12_176"; // Tatooine: Marketplace

	String DefaultSpaceLSSystem = "6_087"; // Tibrin
	String DefaultSpaceDSSystem = "1_282"; // Dantooine

	/*
	 * The default filler cards to use for Force in each deck.  These should essentially be ignored by tests except
	 * for activating and testing draw effects, etc.
	 */

	String DefaultLSFiller = "1_28"; // Rebel Trooper
	String DefaultDSFiller = "1_194"; // Stormtrooper

	/*
	 * If other formats come in handy, those can also be listed here.
	 */
	String Open = "open";


	/*
	 * These three functions are used in the base interfaces but are unnecessary in the actual implementation, where the
	 * underlying fields can be used instead.
	 *
	 */
	DefaultSwccgGame game();
	GameState gameState();
	DefaultUserFeedback userFeedback();


	PhysicalCardImpl GetDSCard(String cardName);
	PhysicalCardImpl GetLSCard(String cardName);
	PhysicalCardImpl GetCard(String player, String cardName);
}
