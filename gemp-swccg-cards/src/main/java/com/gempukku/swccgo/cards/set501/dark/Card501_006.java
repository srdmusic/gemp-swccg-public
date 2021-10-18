package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.CancelCardOnTableEffect;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextType;
import com.gempukku.swccgo.logic.modifiers.TotalPowerModifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 17
 * Type: Effect
 * Title: Dark Deal (V)
 */
public class Card501_006 extends AbstractNormalEffect {
    public Card501_006() {
        super(Side.DARK, 3, PlayCardZoneOption.ATTACHED, Title.Dark_Deal, Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("'Perhaps you think you're being treated unfairly?' 'No.' 'Good. It would be unfortunate if I had to leave a garrison here.'");
        setGameText("Deploy on Bespin: Cloud City if you occupy two related locations with an alien and an Imperal. Aliens and TIEs deploy -1 to Bespin locations. Once per game, may take a [Cloud City] Imperial (or [Cloud City] TIE) into hand from Reserve Deck; reshuffle. [Immune to Alter.]");
        addIcons(Icon.CLOUD_CITY, Icon.VIRTUAL_SET_17);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Dark Deal (V)");
        hideFromDeckBuilder();
    }
}