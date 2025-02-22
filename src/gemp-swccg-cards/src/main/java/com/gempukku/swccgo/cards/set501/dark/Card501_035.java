package com.gempukku.swccgo.cards.set501.dark;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.HitCondition;
import com.gempukku.swccgo.cards.conditions.WithCondition;
import com.gempukku.swccgo.common.ExpansionSet;
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
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.conditions.AndCondition;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.conditions.UnlessCondition;
import com.gempukku.swccgo.logic.effects.PutCardFromHandOnForcePileEffect;
import com.gempukku.swccgo.logic.modifiers.MayNotBeTargetedByModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotBeTargetedBySpecificWeaponsModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.PowerModifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Alien
 * Title: Bib Fortuna (V)
 */
public class Card501_035 extends AbstractAlien {
    public Card501_035() {
        super(Side.DARK, 1, 3, 3, 1, 4, Title.Bib, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Twi'lek leader and majordomo of Jabba's palace. Succeeded Jabba's last majordomo, Naroon Cuthus. Plotting to kill Jabba.");
        setGameText("If opponent just deployed a character here, may place a card from hand on Force pile. While with Jabba, Bib is power +2 and, unless 'hit,' opponent may not target your other characters here with blasters (or this site with I Must Be Allowed To Speak).");
        addPersona(Persona.BIB);
        addIcons(Icon.JABBAS_PALACE, Icon.VIRTUAL_SET_25);
        setSpecies(Species.TWILEK);
        addKeywords(Keyword.LEADER);
        setVirtualSuffix(true);
        setTestingText("Bib Fortuna (V)");
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(String playerId, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if(TriggerConditions.justDeployedToLocation(game, effectResult, game.getOpponent(playerId), Filters.character, Filters.here(self))
            && GameConditions.hasHand(game, playerId)){

            OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Place card from hand on Force Pile");
            action.setActionMsg("Place a card from hand on Force Pile");

            // Perform result(s)
            action.appendEffect(new PutCardFromHandOnForcePileEffect(action, playerId));

            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        Condition withJabba = new WithCondition(self, Filters.Jabba);
        Condition unlessHit = new UnlessCondition(new HitCondition(self));
        Condition withJabbaAndUnlessHit = new AndCondition(withJabba, unlessHit);

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new PowerModifier(self, withJabba, 2));
        modifiers.add(new MayNotBeTargetedBySpecificWeaponsModifier(self, Filters.and(Filters.your(self), Filters.other(self), Filters.character, Filters.here(self)),
                withJabbaAndUnlessHit, Filters.blaster));
        modifiers.add(new MayNotBeTargetedByModifier(self, Filters.sameSite(self), withJabbaAndUnlessHit, Filters.title(Title.I_Must_Be_Allowed_To_Speak)));
        return modifiers;
    }
}
