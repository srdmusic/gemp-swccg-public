package com.gempukku.swccgo.framework;

import java.util.HashMap;

public interface StartingSetup {
	HashMap<String, String> Cards();
	void Setup(VirtualTableScenario scn);

	/**
	 * An empty collection communicating that the Dark Side will have no objectives or starting interrupts played at
	 * the start of a particular test scenario.
	 */
	StartingSetup NoDSStarters = new StartingSetup() {
		@Override
		public HashMap<String, String> Cards() { return new HashMap<>(); }

		@Override
		public void Setup(VirtualTableScenario scn) { scn.DSChooseCard(scn.GetDSCard("starting-location")); }
	};

	/**
	 * An empty collection communicating that the Light Side will have no objectives or starting interrupts played at
	 * the start of a particular test scenario.
	 */
	StartingSetup NoLSStarters = new StartingSetup() {
		@Override
		public HashMap<String, String> Cards() { return new HashMap<>(); }

		@Override
		public void Setup(VirtualTableScenario scn) { scn.LSChooseCard(scn.GetLSCard("starting-location")); }
	};

	/**
	 * The Light Side objective You Can Either Profit By This... / Or Be Destroyed and associated cards.
	 */
	StartingSetup Profit = new StartingSetup() {
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

