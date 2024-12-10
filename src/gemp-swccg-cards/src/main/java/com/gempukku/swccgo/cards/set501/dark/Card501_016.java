package com.gempukku.swccgo.cards.set501.dark;

import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractDroid;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.ModelType;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.MayNotUseCardToTransportToOrFromLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Droid
 * Title: DRK-1 (Dark Eye Probe Droid)
 */
public class Card501_016 extends AbstractDroid {
    public Card501_016() {
        super(Side.DARK, 3, 1, 1, 2, "DRK-1 (Dark Eye Probe Droid)", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setManeuver(3);
        setGameText("Nabrun Leids may not transport to or from here. During your move phase, may use 2 Force to relocate a Dark Jedi (with any captives they are escorting), your Mara, or an Inquisitor from here to same site as a Dark Jedi or Jedi; place this droid in Used Pile.");
        addIcons(Icon.DEATH_STAR_II, Icon.VIRTUAL_SET_0);
        addModelTypes(ModelType.PROBE, ModelType.RECON);
        setTestingText("DRK-1 (Dark Eye Probe Droid)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new MayNotUseCardToTransportToOrFromLocationModifier(self, Filters.Nabrun_Leids, Filters.here(self)));
        return modifiers;
    }
}
