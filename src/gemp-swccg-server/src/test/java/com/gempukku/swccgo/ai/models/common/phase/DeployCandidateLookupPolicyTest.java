package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeployCandidateLookupPolicyTest {

    @Test
    public void tableActionSourceCannotMasqueradeAsDeployCandidate() {
        PhysicalCard shuttle = mock(PhysicalCard.class);
        when(shuttle.getOwner()).thenReturn("rando");
        when(shuttle.getZone()).thenReturn(Zone.AT_LOCATION);
        assertFalse(DeployCandidateLookupPolicy
                .isDirectHandCandidate(shuttle, "rando"));

        PhysicalCard handCard = mock(PhysicalCard.class);
        when(handCard.getOwner()).thenReturn("rando");
        when(handCard.getZone()).thenReturn(Zone.HAND);
        assertTrue(DeployCandidateLookupPolicy
                .isDirectHandCandidate(handCard, "rando"));
    }
}
