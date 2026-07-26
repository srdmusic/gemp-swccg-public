package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TdigwattObjectiveFactsTest {
    @Test
    public void exactBlueprintAndPhysicalIdentitySeparateSharedTitlePrints() {
        var classic = new TdigwattObjectiveFacts.ObjectiveIdentity(
                101, "109_12", false);
        var classicBack =
                new TdigwattObjectiveFacts.ObjectiveIdentity(
                        101, "109_12", true);
        var virtual = new TdigwattObjectiveFacts.ObjectiveIdentity(
                102, "226_12", false);

        assertEquals(
                TdigwattObjectiveFacts.Printing.CLASSIC,
                classic.printing());
        assertEquals(
                TdigwattObjectiveFacts.Printing.VIRTUAL,
                virtual.printing());
        assertTrue(classic.isSamePhysicalObjective(classicBack));
        assertFalse(classic.isSamePhysicalObjective(virtual));
        assertNotEquals(classic, classicBack);
    }

    @Test
    public void rejectsTitleMatchingBackBlueprintAndUnknownObjective() {
        assertBadIdentity(
                "This Deal Is Getting Worse All The Time");
        assertBadIdentity("109_12_BACK");
        assertBadIdentity("226_12_BACK");
        assertBadIdentity("109_012");
    }

    @Test
    public void stateTypesCannotConflateClassicAndVirtualLaw() {
        var classic = new TdigwattObjectiveFacts.ObjectiveIdentity(
                101, "109_12", false);
        var virtual = new TdigwattObjectiveFacts.ObjectiveIdentity(
                102, "226_12", false);

        try {
            new TdigwattObjectiveFacts.ClassicState(
                    virtual, true, true, true,
                    false, false, false);
            fail("expected virtual-as-classic rejection");
        } catch (IllegalArgumentException expected) {
            // expected
        }

        try {
            new TdigwattObjectiveFacts.VirtualState(
                    classic, 3, 0);
            fail("expected classic-as-virtual rejection");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void virtualCountsMustBeKnownNonnegativeValues() {
        var virtual = new TdigwattObjectiveFacts.ObjectiveIdentity(
                102, "226_12", false);
        try {
            new TdigwattObjectiveFacts.VirtualState(
                    virtual, -1, 0);
            fail("expected negative count rejection");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    private static void assertBadIdentity(String blueprintId) {
        try {
            new TdigwattObjectiveFacts.ObjectiveIdentity(
                    101, blueprintId, false);
            fail("expected exact blueprint rejection for "
                    + blueprintId);
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
