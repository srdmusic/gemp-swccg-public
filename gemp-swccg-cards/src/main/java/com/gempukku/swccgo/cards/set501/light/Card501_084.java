package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextModifier;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextType;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.ChoiceMadeResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 17
 * Type: Location
 * Subtype: Site
 * Title: Endor: Anakin's Funeral Pyre
 */
public class Card501_084 extends AbstractSite {
    public Card501_084() {
        super(Side.LIGHT, Title.Anakins_Funeral_Pyre, Title.Endor);
        setLocationDarkSideGameText("");
        setLocationLightSideGameText("Deploys only at start of game. If a [Skywalker] objective on table, There Is Another does not cause Luke to be lost.");
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.SKYWALKER, Icon.EXTERIOR_SITE, Icon.PLANET, Icon.VIRTUAL_SET_17);
        setTestingText("Endor: Anakin's Funeral Pyre (ERRATA)");
        hideFromDeckBuilder();
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self) {
        // Deploys only at start of game.
        return GameConditions.isDuringStartOfGame(game);
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ModifyGameTextModifier(self, Filters.title(Title.There_Is_Another), new OnTableCondition(self, Filters.and(Icon.SKYWALKER, Filters.Objective)), ModifyGameTextType.THERE_IS_ANOTHER__DOES_NOT_MAKE_LUKE_LOST));
        return modifiers;
    }
}