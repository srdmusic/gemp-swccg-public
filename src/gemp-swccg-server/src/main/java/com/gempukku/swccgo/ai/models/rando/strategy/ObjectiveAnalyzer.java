package com.gempukku.swccgo.ai.models.rando.strategy;

import com.gempukku.swccgo.ai.models.rando.RandoLogger;

/** Rando compatibility facade over the shared objective/playbook analyzer. */
public class ObjectiveAnalyzer
        extends com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer {

    public ObjectiveAnalyzer() {
        super(RandoLogger.getStrategyLogger());
    }
}
