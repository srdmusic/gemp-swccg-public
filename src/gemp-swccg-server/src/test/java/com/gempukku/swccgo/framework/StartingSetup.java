package com.gempukku.swccgo.framework;

import java.util.HashMap;

public interface StartingSetup {
	HashMap<String, String> Cards();
	void Setup(VirtualTableScenario scn);

	/**
	 * An inert startup process that has no cards and does nothing.
	 */
	StartingSetup DoNothingSetup = new StartingSetup() {
		@Override
		public HashMap<String, String> Cards() { return new HashMap<>(); }

		@Override
		public void Setup(VirtualTableScenario scn) { }
	};

	/**
	 * An empty outside-of-deck pile communicating that the Dark Side will have no shields or other out-of-game cards
	 * for a particular test scenario.
	 */
	StartingSetup NoDSShields = DoNothingSetup;
	/**
	 * An empty outside-of-deck pile communicating that the Light Side will have no shields or other out-of-game cards
	 * for a particular test scenario.
	 */
	StartingSetup NoLSShields = DoNothingSetup;


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
	StartingSetup DefaultDSGroundLocation = new StartingSetup() {
		@Override
		public HashMap<String, String> Cards() {
			return new HashMap<>() {{
				put("starting-location", "12_176"); // Tatooine: Marketplace
			}};
		}

		@Override
		public void Setup(VirtualTableScenario scn) { scn.DSChooseCard(scn.GetDSCard("starting-location")); }
	};

	/**
	 * The default ground location used by Light Side.  This will be played at the start of the game automatically.
	 */
	StartingSetup DefaultLSGroundLocation = new StartingSetup() {
		@Override
		public HashMap<String, String> Cards() {
			return new HashMap<>() {{
				put("starting-location", "5_079"); // Cloud City: Chasm Walkway
			}};
		}

		@Override
		public void Setup(VirtualTableScenario scn) { scn.LSChooseCard(scn.GetLSCard("starting-location")); }
	};

	/**
	 * The default space system used by Dark Side.  This will be played at the start of the game automatically.
	 */
	StartingSetup DefaultDSSpaceSystem = new StartingSetup() {
		@Override
		public HashMap<String, String> Cards() {
			return new HashMap<>() {{
				put("starting-location", "1_282"); // Dantooine
			}};
		}

		@Override
		public void Setup(VirtualTableScenario scn) { scn.DSChooseCard(scn.GetDSCard("starting-location")); }
	};

	/**
	 * The default space system used by Light Side.  This will be played at the start of the game automatically.
	 */
	StartingSetup DefaultLSSpaceSystem = new StartingSetup() {
		@Override
		public HashMap<String, String> Cards() {
			return new HashMap<>() {{
				put("starting-location", "6_087"); // Tibrin
			}};
		}

		@Override
		public void Setup(VirtualTableScenario scn) { scn.LSChooseCard(scn.GetLSCard("starting-location")); }
	};


	class StartingLocation implements StartingSetup {
		HashMap<String, String> _cards = new HashMap<>();
		public StartingLocation(String id) {
			_cards.put("starting-location", id);
		}

		@Override
		public HashMap<String, String> Cards() { return _cards; }

		@Override
		public void Setup(VirtualTableScenario scn) {
			if(scn.DSDecisionAvailable("Choose starting location")) {
				scn.DSChooseCard(scn.GetDSCard("starting-location"));
			}
			else if(scn.LSDecisionAvailable("Choose starting location")) {
				scn.LSChooseCard(scn.GetLSCard("starting-location"));
			}
		}
	}


	/**
	 * An empty collection communicating that the Dark Side will have no starting interrupts played at
	 * the start of a particular test scenario.
	 */
	StartingSetup NoDSStartingInterrupts = DoNothingSetup;

	/**
	 * An empty collection communicating that the Light Side will have no starting interrupts played at
	 * the start of a particular test scenario.
	 */
	StartingSetup NoLSStartingInterrupts = DoNothingSetup;



	/**
	 * The Light Side objective You Can Either Profit By This... / Or Be Destroyed and associated cards.
	 */
	StartingSetup ProfitObjective = new StartingSetup() {
		@Override
		public HashMap<String, String> Cards() {
			return new HashMap<>() {{
				put("profit", "110_4"); // Objective
				put("palace", "7_131"); // Tatooine: Jabba's Palace
				put("chamber", "6_81"); // Jabba's Palace: Audience Chamber
				put("han", "1_11"); // Han Solo
			}};
		}

		@Override
		public void Setup(VirtualTableScenario scn) {
			if(scn.LSDecisionAvailable("On which side")) {
				scn.LSChoose("Left");
			}

			if(scn.DSDecisionAvailable("Choose alien(s) to deploy to Audience Chamber")) {
				scn.DSPass();
			}
		}
	};
}

