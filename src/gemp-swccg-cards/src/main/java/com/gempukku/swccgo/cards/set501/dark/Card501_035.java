package com.gempukku.swccgo.cards.set501.dark;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.HitCondition;
import com.gempukku.swccgo.cards.conditions.WithCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Species;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.conditions.UnlessCondition;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToLocationFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.DeploysFreeToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotBeTargetedBySpecificWeaponsModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.PowerModifier;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Alien
 * Title: Bib Fortuna (V)
 */
public class Card501_035 extends AbstractAlien {
    public Card501_035() {
        super(Side.DARK, 1, 2, 3, 1, 3, Title.Bib, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Twi'lek leader and majordomo of Jabba's palace. Succeeded Jabba's last majordomo, Naroon Cuthus. Plotting to kill Jabba.");
        setGameText("Deploys free to opponent's Audience Chamber. May ▼ [Jabba's Palace] No Bargain. While with Jabba, Bib is power +2 and Jabba is immune to attrition. While at Audience Chamber, I Must Be Allowed To Speak may not target this site.");
        addPersona(Persona.BIB);
        addIcons(Icon.JABBAS_PALACE, Icon.VIRTUAL_SET_25);
        setSpecies(Species.TWILEK);
        addKeywords(Keyword.LEADER);
        setVirtualSuffix(true);
        setTestingText("Bib Fortuna (V)");
    }

    @Override
    protected List<Modifier> getGameTextAlwaysOnModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new DeploysFreeToLocationModifier(self, Filters.and(Filters.opponents(self), Icon.JABBAS_PALACE, Filters.site)));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<TopLevelGameTextAction>();

        GameTextActionId gameTextActionId = GameTextActionId.BIB_FORTUNA__DOWNLOAD_CARD;

        // Check condition(s)
        if (GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId, Arrays.asList(Title.Salacious_Crumb, Title.Jabbas_Influence))
                && GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy card from Reserve Deck");
            action.setActionMsg("Deploy Salacious Crumb (or Jabba's Influence) here from Reserve Deck");

            // Update usage limit(s)
            action.appendUsage(
                    new OncePerTurnEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new DeployCardToLocationFromReserveDeckEffect(action, Filters.or(Filters.title(Title.Salacious_Crumb), Filters.title(Title.Jabbas_Influence)), Filters.here(self), true));

            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        Condition withJabba = new WithCondition(self, Filters.Jabba);
        Condition unlessHit = new UnlessCondition(new HitCondition(self));

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new PowerModifier(self, withJabba, 2));
        modifiers.add(new ImmuneToAttritionModifier(self, Filters.Jabba, withJabba));

        modifiers.add(new MayNotBeTargetedBySpecificWeaponsModifier(self, Filters.and(Filters.your(self), Filters.other(self), Filters.character, Filters.here(self)), unlessHit, Filters.blaster));
        return modifiers;
    }
}
