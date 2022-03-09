package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractRebel;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.AtCondition;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.conditions.UnlessCondition;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfGameModifierEffect;
import com.gempukku.swccgo.logic.modifiers.*;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 18
 * Type: Character
 * Subtype: Rebel
 * Title: Kanan, Rebel Infiltrator
 */
public class Card501_062 extends AbstractRebel {
    public Card501_062() {
        super(Side.LIGHT, 1, 6, 4, 5, 6, "Kanan, Rebel Infiltrator", Uniqueness.UNIQUE);
        setArmor(5);
        setLore("Stormtrooper.");
        setGameText("Unless Luke has been deployed this game, may be targeted instead of Luke by Bring Him Before Me (opponent's [Death Star II] objective, Insignificant Rebellion, and Your Destiny target Kanan instead of Luke for remainder of game). Ezra moves to here for free.");
        addPersona(Persona.KANAN);
        addIcons(Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_18);
        addKeywords(Keyword.STORMTROOPER);
        setTestingText("Kanan, Rebel Infiltrator");
    }

    @Override
    protected List<Modifier> getGameTextAlwaysOnModifiers(final SwccgGame game, PhysicalCard self) {
        final String playerId = self.getOwner();
        final String opponent = game.getOpponent(playerId);
        Condition lukeHasBeenDeployedCondition = new Condition() {
            @Override
            public boolean isFulfilled(GameState gameState, ModifiersQuerying modifiersQuerying) {
                return GameConditions.hasDeployedAtLeastXCardsThisGame(game, playerId, 1, Filters.Luke)
                        || GameConditions.hasDeployedAtLeastXCardsThisGame(game, opponent, 1, Filters.Luke);
            }
        };
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayBeTargetedByModifier(self, self, new UnlessCondition(lukeHasBeenDeployedCondition), Title.Bring_Him_Before_Me));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (GameConditions.canSpot(game, self, Filters.and(Filters.Bring_Him_Before_Me, Filters.not(Filters.hasGameTextModification(ModifyGameTextType.BRING_HIM_BEFORE_ME__TARGETS_KANAN_INSTEAD_OF_LUKE))))
            && !(GameConditions.hasDeployedAtLeastXCardsThisGame(game, playerId, 1, Filters.Luke)
                || GameConditions.hasDeployedAtLeastXCardsThisGame(game, game.getOpponent(playerId), 1, Filters.Luke))) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId);
            action.setText("Make Bring Him Before Me target Kanan");
            action.setActionMsg("Make Insignificant Rebellion, Your Destiny, and opponent's [Death Star II] objective target Kanan instead of Luke for remainder of game");
            // Perform result(s)
            action.appendEffect(
                    new AddUntilEndOfGameModifierEffect(action, new ModifyGameTextModifier(self,
                            Filters.or(Filters.and(Filters.opponents(self), Icon.DEATH_STAR_II, Filters.Objective), Filters.Insignificant_Rebellion, Filters.Your_Destiny), ModifyGameTextType.BRING_HIM_BEFORE_ME__TARGETS_KANAN_INSTEAD_OF_LUKE),
                            "Makes Insignificant Rebellion, Your Destiny, and opponent's [Death Star II] objective target Kanan instead of Luke for remainder of game"));
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new MovesFreeToLocationModifier(self, Filters.Ezra, Filters.here(self)));
        return modifiers;
    }
}
