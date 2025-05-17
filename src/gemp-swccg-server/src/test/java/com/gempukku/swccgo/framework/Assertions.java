package com.gempukku.swccgo.framework;

import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCardImpl;

import javax.xml.stream.Location;

import static org.junit.Assert.assertEquals;

public class Assertions {

	public static void assertAtLocation(PhysicalCardImpl location, PhysicalCardImpl...cards) {
		for(var card : cards) {
			assertEquals(Zone.AT_LOCATION, card.getZone());
			assertEquals(location, card.getAtLocation());
		}
	}

	public static void assertInZone(Zone zone, PhysicalCardImpl...cards) {
		for(var card : cards) {
			assertEquals(zone, card.getZone());
		}
	}

	public static void assertInHand(PhysicalCardImpl...cards) { assertInZone(Zone.HAND, cards); }
}
