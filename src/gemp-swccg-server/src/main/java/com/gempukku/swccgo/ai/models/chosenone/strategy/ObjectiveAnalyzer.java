package com.gempukku.swccgo.ai.models.chosenone.strategy;

import com.gempukku.swccgo.ai.models.chosenone.RandoLogger;

/** ChosenOne compatibility facade over the shared objective/playbook analyzer. */
public class ObjectiveAnalyzer
        extends com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer {

    public ObjectiveAnalyzer() {
        super(RandoLogger.getStrategyLogger());
    }
}
