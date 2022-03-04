package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractPermanentAboard;
import com.gempukku.swccgo.cards.AbstractPermanentPilot;
import com.gempukku.swccgo.cards.AbstractStarfighter;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToLocationFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.*;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 18
 * Type: Starship
 * Subtype: Starfighter
 * Title: Auzituck Gunship
 */
public class Card501_091 extends AbstractStarfighter {
    public Card501_091() {
        super(Side.LIGHT, 2, 5, 5, 5, null, 5, 5, "Auzituck Gunship", Uniqueness.UNIQUE);
        setGameText("May add 1 pilot. Permanent pilot provides ability of 2. If Wookiee Homestead on table, deploy -1, power +2, immune to attrition < 5, and your total power is +2 at related Kashyyyk sites.");
        addIcons(Icon.INDEPENDENT, Icon.PILOT, Icon.NAV_COMPUTER, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_18);
        addModelType(ModelType.AUZITUCK_GUNSHIP);
        setPilotCapacity(1);
        setTestingText("Auzituck Gunship");
    }

    @Override
    protected List<? extends AbstractPermanentAboard> getGameTextPermanentsAboard() {
        return Collections.singletonList(new AbstractPermanentPilot(2) {});
    }

    @Override
    protected List<Modifier> getGameTextAlwaysOnModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new DeployCostModifier(self, new OnTableCondition(self, Filters.Wookiee_Homestead), -1));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        Condition wookieeHomesteadOnTableCondition = new OnTableCondition(self, Filters.Wookiee_Homestead);
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new PowerModifier(self, wookieeHomesteadOnTableCondition, 2));
        modifiers.add(new TotalPowerModifier(self, Filters.and(Filters.relatedSite(self), Filters.Kashyyyk_site), wookieeHomesteadOnTableCondition, 2, self.getOwner()));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, wookieeHomesteadOnTableCondition,5));
        return modifiers;
    }
}
