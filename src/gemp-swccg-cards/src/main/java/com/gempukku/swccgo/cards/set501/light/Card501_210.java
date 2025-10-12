package com.gempukku.swccgo.cards.set501.light;

import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractJediMasterRepublic;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.choose.DeployCardsToTargetFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.AddsPowerToPilotedBySelfModifier;
import com.gempukku.swccgo.logic.modifiers.MayDeployToTargetModifier;
import com.gempukku.swccgo.logic.modifiers.MayUseWeaponModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Jedi Master/Republic
 * Title: Kelleran Beq
 */

public class Card501_210 extends AbstractJediMasterRepublic {
    public Card501_210() {
        super(Side.LIGHT, 1, 8, 6, 7, 8, "Kelleran Beq", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Jedi survivor.");
        setGameText("[Pilot] 2. Any lightsaber may deploy on Beq. Once per game, may deploy up to two lightsabers on Beq from Lost Pile. Your other characters here are defense value +1 for each lightsaber on Beq. Adds one battle destiny with Grogu or a Padawan. Immune to attrition < 6.");
        addKeyword(Keyword.JEDI_SURVIVOR);
        addPersona(Persona.BEQ);
        addIcons(Icon.EPISODE_I, Icon.PILOT, Icon.VIRTUAL_SET_26);
        addIcon(Icon.WARRIOR, 2);
        setTestingText("Kelleran Beq");
    }
    
    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, 2));
        modifiers.add(new MayDeployToTargetModifier(self, Filters.lightsaber, self));
        modifiers.add(new MayUseWeaponModifier(self, Filters.lightsaber));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<TopLevelGameTextAction>();

        GameTextActionId gameTextActionId = GameTextActionId.KELLERAN_BEQ__DEPLOY_LIGHTSABERS;
        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
                && GameConditions.canSearchLostPile(game, playerId, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy lightsaber(s) from Lost Pile");
            action.setActionMsg("Deploy up to two lightsabers on Beq from Lost Pile");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerGameEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new DeployCardsToTargetFromReserveDeckEffect(action, Filters.lightsaber, 1, 2, Filters.Beq, false) {
                        @Override
                        public String getChoiceText(int numCardsToChoose) {
                            return "Choose lightsaber(s) to deploy on Beq";
                        }
                    });
            actions.add(action);
        }
        return actions;
    }
}
