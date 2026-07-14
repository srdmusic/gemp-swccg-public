package com.gempukku.swccgo.ai.models.rando.evaluators;

import com.gempukku.swccgo.ai.models.common.evaluators.AbstractForcedDestinationDeploySafetyMatrixTest;
import com.gempukku.swccgo.ai.models.common.strategy.ForcedDestinationDeploySafety;
import com.gempukku.swccgo.ai.models.common.strategy.FormationSafety;

public class RandoForcedDestinationDeploySafetyMatrixTest
        extends AbstractForcedDestinationDeploySafetyMatrixTest {
    @Override
    protected ForcedDestinationDeploySafety.Assessment assess(
            boolean identityResolved,
            ForcedDestinationDeploySafety.ObjectiveState objectiveState,
            FormationSafety.CharacterDeployCheck formation,
            boolean weakSoloNoPlan) {
        return ActionTextEvaluator.assessForcedDestinationDeploySafety(
                identityResolved, objectiveState, formation, weakSoloNoPlan);
    }
}
