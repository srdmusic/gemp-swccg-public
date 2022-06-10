package com.gempukku.swccgo.cards.set501.light;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.logic.modifiers.*;
import java.util.LinkedList;
import java.util.List;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.LostFromTableResult;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.cards.effects.usage.OncePerBattleEffect;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromLostPileEffect;
import java.util.Collections;
import com.gempukku.swccgo.logic.TriggerConditions;

public class Card501_053 extends AbstractNormalEffect {
    public Card501_053() {
        super(Side.LIGHT, 5, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.I_Cant_Believe_Hes_Gone, Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("Even though Luke felt the pain of losing his mentor, Obi-Wan continued to give him strength and guidance through the Force.");
        setGameText(" If Obi-Wan ‘communing,’ deploy on table. If Master Kenobi just modified your power, opponent’s total battle destiny is -1. Once per game, may take your just lost Rebel into hand. Once per battle, may activate 1 Force or add 1 to a just drawn destiny. [Immune to Alter.]");
        addIcons(Icon.TATOOINE, Icon.VIRTUAL_SET_19);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("I Can't Believe He's Gone (v)");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.canSpot(game, self, Filters.and(Filters.Communing, Filters.hasStacked(Filters.ObiWan)));
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new ModifyGameTextModifier(self, Filters.title(Title.Master_Kenobi), ModifyGameTextType.MASTER_KENOBI__SUBTRACT_FROM_BATTLE_DESTINY_IF_POWER_MODIFIED));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (GameConditions.isOncePerBattle(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && GameConditions.canActivateForce(game, playerId)) {
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Activate 1 Force");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerBattleEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new ActivateForceEffect(action, playerId, 1));
            actions.add(action);
        }
        return actions;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (TriggerConditions.isDestinyJustDrawn(game, effectResult)
                && GameConditions.isOncePerBattle(game, self, playerId, gameTextSourceCardId, gameTextActionId)) {
            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Add 1 to destiny");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerBattleEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new ModifyDestinyEffect(action, 1));
            return Collections.singletonList(action);
        }

        gameTextActionId = GameTextActionId.I_CANT_BELIEVE_HES_GONE__RETRIEVE_A_REBEL;
        if (TriggerConditions.justLost(game, effectResult, Filters.and(Filters.your(self), Filters.Rebel))
                && GameConditions.isOncePerGame(game, self, gameTextActionId)) {
            final PhysicalCard justLostCard = ((LostFromTableResult) effectResult).getCard();
            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Place " + GameUtils.getFullName(justLostCard) + " in hand");
            action.setActionMsg("Place " + GameUtils.getCardLink(justLostCard) + " in hand");

            // Update usage limit(s)
            action.appendUsage(
                    new OncePerGameEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new TakeCardIntoHandFromLostPileEffect(action, playerId, justLostCard, false, true));
            return Collections.singletonList(action);
        }
        return null;
    }


}
