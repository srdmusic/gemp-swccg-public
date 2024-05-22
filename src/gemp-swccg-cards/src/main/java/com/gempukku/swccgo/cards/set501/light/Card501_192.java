package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractJediMaster;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.cards.evaluators.CardMatchesEvaluator;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToTargetFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.AddsPowerToPilotedBySelfModifier;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/*
 * Set: Set 23
 * Type: Character
 * Subtype: Alien
 * Title: Jedi Marshal Avar Kriss
 */
public class Card501_192 extends AbstractJediMaster {
    public Card501_192() {
        super(Side.LIGHT, 2, 5, 5, 7, 7, "Jedi Marshal Avar Kriss", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Female musician. High Republic.");
        setGameText("Adds 2 to power of anything she pilots (3 if piloting a capital starship). While piloting a capital starship, it is immune to attrition < 5. Once per game, may retrieve a musician (or take one into hand from Reserve Deck; reshuffle). Immune to attrition < 5.");
        addIcons(Icon.EPISODE_I, Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_23);
        addKeywords(Keyword.FEMALE, Keyword.MUSICIAN);
        setTestingText("Jedi Marshal Avar Kriss");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, new CardMatchesEvaluator(2, 3, Filters.capital_starship)));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, Filters.and(Filters.capital_starship, Filters.hasPiloting(self)), 5));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, 5));
        return modifiers;
    }

    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.BEILART_VALANCE__DOWNLOAD_CYBORG_CONSTRUCT;

        // Check condition(s)
        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId, Title.Cyborg_Construct)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy Cyborg Construct from Reserve Deck");
            action.setActionMsg("Deploy Cyborg Construct on " + GameUtils.getCardLink(self) + " from Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerGameEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new DeployCardToTargetFromReserveDeckEffect(action, Filters.Cyborg_Construct, Filters.sameCardId(self), true));
            return Collections.singletonList(action);
        }
        return null;
    }

}