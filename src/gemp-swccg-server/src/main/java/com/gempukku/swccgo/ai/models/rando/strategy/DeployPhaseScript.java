package com.gempukku.swccgo.ai.models.rando.strategy;

import com.gempukku.swccgo.ai.models.rando.RandoLogger;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.game.SwccgCardBlueprint;

import java.util.List;

/** Rando compatibility facade over the shared deploy-phase script. */
public class DeployPhaseScript
        extends com.gempukku.swccgo.ai.models.common.phase.DeployPhaseScript {

    public DeployPhaseScript() {
        super(RandoLogger.getStrategyLogger());
    }

    @Override
    protected String sourceCardFullGameText(SwccgCardBlueprint blueprint, Side side) {
        return DeckOracle.getSourceCardFullGameText(blueprint, side);
    }

    @Override
    protected List<String> parseSourceCardPullTargets(String gameText) {
        return DeckOracle.parseSourceCardPullTargets(gameText);
    }
}
