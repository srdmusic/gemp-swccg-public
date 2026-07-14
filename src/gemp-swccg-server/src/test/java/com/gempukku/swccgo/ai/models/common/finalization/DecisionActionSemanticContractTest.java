package com.gempukku.swccgo.ai.models.common.finalization;

import com.gempukku.swccgo.common.DecisionActionSemantic;
import com.gempukku.swccgo.logic.actions.CardPileAction;
import com.gempukku.swccgo.logic.timing.Action;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/** Wire contract for engine-owned action semantics on card-action decisions. */
public class DecisionActionSemanticContractTest {

    @Test
    public void actionDefaultsToUnknownSemantic() {
        Action action = new CardPileAction("asdf", EngineDecisionFixtures.card(101));

        assertEquals(DecisionActionSemantic.UNKNOWN, action.getDecisionActionSemantic());
    }

    @Test
    public void wireParserAcceptsOnlyExactEnumNames() {
        assertEquals("actionSemantic", DecisionActionSemantic.WIRE_PARAMETER);
        for (DecisionActionSemantic semantic : DecisionActionSemantic.values()) {
            assertEquals(semantic, DecisionActionSemantic.fromWire(semantic.name()));
        }

        assertNull(DecisionActionSemantic.fromWire(null));
        assertNull(DecisionActionSemantic.fromWire(""));
        assertNull(DecisionActionSemantic.fromWire("   "));
        assertNull(DecisionActionSemantic.fromWire("draw_card_into_hand_from_force_pile"));
        assertNull(DecisionActionSemantic.fromWire("NOT_A_SEMANTIC"));
    }

    @Test
    public void cardActionDecisionEmitsOrdinalAlignedNonblankSemantics() {
        Action draw = actionWithSemantic(201,
                DecisionActionSemantic.DRAW_CARD_INTO_HAND_FROM_FORCE_PILE);
        Action defaultUnknown = new CardPileAction("asdf", EngineDecisionFixtures.card(202));
        Action nullSemantic = actionWithSemantic(203, null);

        EngineDecisionFixtures.RecordingCardActionChoice decision =
                new EngineDecisionFixtures.RecordingCardActionChoice(
                        "Choose draw action or Pass",
                        List.of(draw, defaultUnknown, nullSemantic), false);

        assertArrayEquals(new String[]{"0", "1", "2"},
                decision.getDecisionParameters().get("actionId"));
        assertArrayEquals(new String[]{
                        DecisionActionSemantic.DRAW_CARD_INTO_HAND_FROM_FORCE_PILE.name(),
                        DecisionActionSemantic.UNKNOWN.name(),
                        DecisionActionSemantic.UNKNOWN.name()
                },
                decision.getDecisionParameters().get(DecisionActionSemantic.WIRE_PARAMETER));
    }

    private static Action actionWithSemantic(int cardId, DecisionActionSemantic semantic) {
        return new CardPileAction("asdf", EngineDecisionFixtures.card(cardId)) {
            @Override
            public DecisionActionSemantic getDecisionActionSemantic() {
                return semantic;
            }
        };
    }
}
