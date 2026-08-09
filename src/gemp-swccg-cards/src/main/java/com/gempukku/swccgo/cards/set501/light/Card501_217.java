package com.gempukku.swccgo.cards.set501.light;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.CancelForceRetrievalEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Species;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.modifiers.AddsPowerToPilotedBySelfModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotBeExcludedFromBattle;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;


/**
 * Set: Playtesting
 * Type: Effect
 * Title: Chewbacca Of Kashyyyk (V)
 */
public class Card501_217 extends AbstractAlien {
    public Card501_217() {
        super(Side.LIGHT, 1, 4, 6, 2, 6, "Chewbacca Of Kashyyyk", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Wookiee scout. Volunteered for Han's Endor strike team. Keeps his distance, but doesn't look like he's keeping his distance. Always thinks with his stomach.");
        setGameText("[Pilot] 2. Once per turn, may [download] Wuta (or Chewbacca's Bowcaster; or lose 1 Force to deploy it from Lost Pile) here. Characters may not be excluded from battles here. If Bunker has been 'blown away,' opponent's Force retrieval is canceled.");
        addIcons(Icon.ENDOR, Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_27);
        addKeywords(Keyword.SCOUT);
        setSpecies(Species.WOOKIEE);
        addPersona(Persona.CHEWIE);
        setVirtualSuffix(true);
        setTestingText("Chewbacca Of Kashyyyk (V)");
    }

    @Override
    public final boolean hasSpecialDefenseValueAttribute() {
        return true;
    }

    @Override
    public final float getSpecialDefenseValue() {
        return 4;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, 2));
        modifiers.add(new MayNotBeExcludedFromBattle(self, Filters.and(Filters.character, Filters.here(self))));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        if (TriggerConditions.isAboutToRetrieveForce(game, effectResult, game.getOpponent(self.getOwner()))
                && GameConditions.isBlownAway(game, Filters.Bunker)) {
            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Cancel retrieval");
            action.setActionMsg("Force retrieval is canceled");
            action.appendEffect(
                    new CancelForceRetrievalEffect(action)
            );
            return Collections.singletonList(action);
        }

        return null;
    }
}
