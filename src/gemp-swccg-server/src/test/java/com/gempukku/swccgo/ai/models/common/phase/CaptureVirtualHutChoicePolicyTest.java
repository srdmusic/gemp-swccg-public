package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CaptureVirtualHutChoicePolicyTest {

    @Test
    public void admissibleCandidateIsPreferred() {
        assertEquals(
                CaptureVirtualHutChoicePolicy.Choice.PREFER,
                CaptureVirtualHutChoicePolicy.choose(
                        new CaptureVirtualHutChoicePolicy.Facts(
                                true, true)));
    }

    @Test
    public void badCandidateIsVetoedOnlyWhenASelectableRouteExists() {
        assertEquals(
                CaptureVirtualHutChoicePolicy.Choice.HARD_VETO,
                CaptureVirtualHutChoicePolicy.choose(
                        new CaptureVirtualHutChoicePolicy.Facts(
                                false, true)));
        assertEquals(
                CaptureVirtualHutChoicePolicy.Choice.NEUTRAL,
                CaptureVirtualHutChoicePolicy.choose(
                        new CaptureVirtualHutChoicePolicy.Facts(
                                false, false)));
    }
}
