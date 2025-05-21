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
	/**
	 * The Dark Side player's name
	 */
	String DS = "DS Player";
	/**
	 * The Light Side player's name
	 */
	String LS = "LS Player";

	/**
	 * A constant used for performing floating-point numeric comparisons in test assertions.  In effect, any decimal
	 * difference smaller than this amount will be completely ignored when determining if two floating point numbers
	 * are "equal".
	 */
	double epsilon = 0.001;

	/**
	 * An empty outside-of-deck pile communicating that the Dark Side will have no shields or other out-of-game cards
	 * for a particular test scenario.
	 */
	HashMap<String, String> NoDSShields = new HashMap<>();
	/**
	 * An empty outside-of-deck pile communicating that the Light Side will have no shields or other out-of-game cards
	 * for a particular test scenario.
	 */
	HashMap<String, String> NoLSShields = new HashMap<>();

	/**
	 * An empty collection communicating that the Dark Side will have no objectives or starting interrupts played at
	 * the start of a particular test scenario.
	 */
	HashMap<String, String> NoDSStarters = new HashMap<>();
	/**
	 * An empty collection communicating that the Light Side will have no objectives or starting interrupts played at
	 * the start of a particular test scenario.
	 */
	HashMap<String, String> NoLSStarters = new HashMap<>();


	/*
	 * The default locations to be played by either player.  This will be included in the deck and automatically played
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

	/**
	 * The default ground location used by Dark Side.  This will be played at the start of the game automatically.
	 */
	String DefaultGroundDSLocation = "12_176"; // Tatooine: Marketplace
	/**
	 * The default ground location used by Light Side.  This will be played at the start of the game automatically.
	 */
	String DefaultGroundLSLocation = "5_079"; // Cloud City: Chasm Walkway


	/**
	 * The default space system used by Dark Side.  This will be played at the start of the game automatically.
	 */
	String DefaultSpaceDSSystem = "1_282"; // Dantooine
	/**
	 * The default space system used by Light Side.  This will be played at the start of the game automatically.
	 */
	String DefaultSpaceLSSystem = "6_087"; // Tibrin



	/**
	 * The default filler card to use for Force in the Dark Side deck.  These should essentially be ignored by tests
	 * except for activating and testing draw effects.
	 */
	String DefaultDSFiller = "1_194"; // Stormtrooper
	/**
	 * The default filler card to use for Force in the Light Side deck.  These should essentially be ignored by tests
	 * except for activating and testing draw effects.
	 */
	String DefaultLSFiller = "1_28"; // Rebel Trooper


	/*
	 * If other formats come in handy, those can also be listed here.
	 */
	/**
	 * The Open format name.
	 */
	String Open = "open";


	/*
	 * These three functions are used in the base interfaces but are unnecessary in the actual implementation, where the
	 * underlying fields can be used instead.
	 *
	 */

	/**
	 * @return Gets the virtual game table used by the test scenario.
	 */
	DefaultSwccgGame game();

	/**
	 * @return Gets the game state of the virtual table used by the test scenario.
	 */
	GameState gameState();

	/**
	 * @return Gets the user decision manager.  This contains information relating to the decision that Gemp is
	 * currently waiting on.
	 */
	DefaultUserFeedback userFeedback();


	PhysicalCardImpl GetDSCard(String cardName);
	PhysicalCardImpl GetLSCard(String cardName);
	PhysicalCardImpl GetCard(String player, String cardName);
}
