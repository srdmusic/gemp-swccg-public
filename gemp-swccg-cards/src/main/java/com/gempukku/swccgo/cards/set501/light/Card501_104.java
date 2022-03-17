package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractRebel;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.HereCondition;
import com.gempukku.swccgo.cards.conditions.PresentAtCondition;
import com.gempukku.swccgo.cards.conditions.PresentWithCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerBattleEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.AndCondition;
import com.gempukku.swccgo.logic.conditions.InBattleCondition;
import com.gempukku.swccgo.logic.conditions.UnlessCondition;
import com.gempukku.swccgo.logic.effects.DrawDestinyEffect;
import com.gempukku.swccgo.logic.effects.ReturnCardToHandFromTableEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.UnrespondableEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToTargetFromLostPileEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.GuiUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 18
 * Type: Character
 * Subtype: Rebel
 * Title: Master Luke (V)
 */
public class Card501_104 extends AbstractRebel {
    public Card501_104() {
        super(Side.LIGHT, 1, 5, 5, 5, 8, "Master Luke", Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("Until being reunited with Yoda, Luke suspected that he had completed his training. Has a strong influence on the weak minded.");
        setGameText("Unless opponent's character of ability > 3 here, reset opponent's total battle destiny at same site to 0. Once per game, may deploy a lightsaber on Luke from Lost Pile. While present at a battleground site, opponent's aliens deploy +1 to other locations. Immune to attrition < 4.");
        addIcons(Icon.PREMIUM, Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_18);
        addPersona(Persona.LUKE);
        setTestingText("Master Luke (V)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        String opponent = game.getOpponent(self.getOwner());

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ResetTotalBattleDestinyModifier(self, Filters.sameSite(self), new AndCondition(new InBattleCondition(self),
                new UnlessCondition(new HereCondition(self, Filters.and(Filters.opponents(self), Filters.character,
                        Filters.abilityMoreThan(3))))), 0, opponent));
        modifiers.add(new DeployCostToLocationModifier(self, Filters.and(Filters.opponents(self), Filters.alien), new PresentAtCondition(self, Filters.battleground_site), 1, Filters.not(Filters.here(self))));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, 4));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.MASTER_LUKE__DEPLOY_LIGHTSABER_FROM_LOST_PILE;

        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
            && GameConditions.canDeployCardFromLostPile(game, playerId, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy lightsaber from Lost Pile");
            action.setActionMsg("Deploy a lightsaber on Luke from Lost Pile");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerGameEffect(action));
            action.appendEffect(
                    new DeployCardToTargetFromLostPileEffect(action, Filters.lightsaber, Filters.Luke, false));

            return Collections.singletonList(action);
        }
        return null;
    }
}
