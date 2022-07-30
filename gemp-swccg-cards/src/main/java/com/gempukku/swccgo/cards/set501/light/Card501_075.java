package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractRebel;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.modifiers.*;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 19
 * Type: Character
 * Subtype: Rebel
 * Title: Ezra, Hero Of Phoenix Squadron
 */
public class Card501_075 extends AbstractRebel {
    public Card501_075() {
        super(Side.LIGHT, 1, 5, 4, 5, 6, "Ezra, Hero Of Phoenix Squadron", Uniqueness.UNIQUE);
        setLore("Padawan.");
        setGameText("Other Phoenix Squadron characters here are forfeit and defense value +2. Once per game, may retrieve a Phoenix Squadron character into hand. [Set 13] Maul may not modify destinies. Immune to attrition < 3.");
        addIcons(Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_19);
        addPersona(Persona.EZRA);
        addKeywords(Keyword.PADAWAN, Keyword.PHOENIX_SQUADRON);
        setTestingText("Ezra, Hero Of Phoenix Squadron");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ForfeitModifier(self, Filters.and(Filters.other(self), Filters.here(self), Filters.Phoenix_Squadron_character), 2));
        modifiers.add(new DefenseValueModifier(self, Filters.and(Filters.other(self), Filters.here(self), Filters.Phoenix_Squadron_character), 2));
        modifiers.add(new ModifyGameTextModifier(self, Filters.and(Icon.VIRTUAL_SET_13, Filters.Maul), ModifyGameTextType.MAUL__MAY_NOT_MODIFIY_DESTINIES));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, 3));
        return modifiers;
    }


    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.EZRA_HERO_OF_PHOENIX_SQUADRON__RETRIEVE_PHOENIX_SQUADRON_CHARACTER_INTO_HAND;

        // Check condition(s)
        if (GameConditions.isOncePerGame(game, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Retrieve a character into hand");
            action.setActionMsg("Retrieve a Phoenix Squadron character into hand");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerGameEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new RetrieveCardIntoHandEffect(action, playerId, Filters.Phoenix_Squadron_character));
            return Collections.singletonList(action);
        }
        return null;
    }
}
