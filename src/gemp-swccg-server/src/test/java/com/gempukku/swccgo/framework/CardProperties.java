package com.gempukku.swccgo.framework;

import com.gempukku.swccgo.common.CardType;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCardImpl;

import java.util.List;

/**
 * Functions for checking aspects of a card, such as numeric states and other state-based properties.
 *
 * If you are just checking the printed stats, then you can retrieve the blueprint of a card and check those values
 * directly.  These helper functions are more for live cards which may be affected by modifiers sourced from other cards.
 */
public interface CardProperties extends TestBase {


	/**
	 * @param card The target card
	 * @return All cards currently attached to the given card.
	 */
    default List<PhysicalCardImpl> GetAttachedCards(PhysicalCardImpl card) {
        return (List<PhysicalCardImpl>)(List<?>)gameState().getAttachedCards(card);
    }

	/**
	 * @param card The target card
	 * @return All cards currently stacked on the given card.
	 */
    default List<PhysicalCardImpl> GetStackedCards(PhysicalCardImpl card) {
        return (List<PhysicalCardImpl>)(List<?>)gameState().getStackedCards(card);
    }


	/**
	 * Determines if a given card is currently attached to another.  Checks both that the current zone of the card is
	 * correct, and also that it currently records the bearer as its attachment point.
	 * @param card The card which may or may not be attached
	 * @param bearer The card which supposedly bears the other card
	 * @return True if card is in the ATTACHED zone and records bearer as its AttachedTo.
	 */
    default boolean IsAttachedTo(PhysicalCardImpl card, PhysicalCardImpl bearer) {
        if(card.getZone() != Zone.ATTACHED) {
            return false;
        }

        return bearer == card.getAttachedTo();
    }


	/**
	 * @param card The card to inspect.
	 * @return The modified current destiny of the card, as altered by all current in-game effects.
	 */
	default float GetDestiny(PhysicalCardImpl card)
	{
		return game().getModifiersQuerying().getDestiny(gameState(), card);
	}

	/**
	 * @param card The card to inspect.
	 * @return The modified current ability of the card, as altered by all current in-game effects.
	 */
	default float GetAbility(PhysicalCardImpl card)
	{
		return game().getModifiersQuerying().getAbility(gameState(), card);
	}

	/**
	 * @param card The card to inspect.
	 * @return The modified current battle destiny ability of the card, as altered by all current in-game effects.
	 */
	default float GetBattleDestinyAbility(PhysicalCardImpl card)
	{
		return game().getModifiersQuerying().getAbilityForBattleDestiny(gameState(), card);
	}

	/**
	 * @param card The card to inspect.
	 * @return The modified current armor of the card, as altered by all current in-game effects.
	 */
	default float GetArmor(PhysicalCardImpl card)
	{
		return game().getModifiersQuerying().getArmor(gameState(), card);
	}

	/**
	 * @param card The card to inspect.
	 * @return The modified current deploy cost of the card, as altered by all current in-game effects.
	 */
	default float GetDeployCost(PhysicalCardImpl card)
	{
		return game().getModifiersQuerying().getDeployCost(gameState(), card);
	}

	/**
	 * @param card The card to inspect.
	 * @return The modified current forfeit value of the card, as altered by all current in-game effects.
	 */
	default float GetForfeit(PhysicalCardImpl card)
	{
		return game().getModifiersQuerying().getForfeit(gameState(), card);
	}

	/**
	 * @param card The card to inspect.
	 * @return The modified current hyperspeed of the card, as altered by all current in-game effects.
	 */
	default float GetHyperspeed(PhysicalCardImpl card)
	{
		return game().getModifiersQuerying().getHyperspeed(gameState(), card);
	}

	/**
	 * @param card The card to inspect.
	 * @param icon The icon to check for.
	 * @return The modified current icon count of the card, as altered by all current in-game effects.
	 */
	default float GetIconCount(PhysicalCardImpl card, Icon icon)
	{
		return game().getModifiersQuerying().getIconCount(gameState(), card, icon);
	}

	/**
	 * @param card The card to inspect.
	 * @param keyword The keyword to check for.
	 * @return Whether the current card has the given keyword, either printed on it or added (or removed) by a game effect.
	 */
    default boolean HasKeyword(PhysicalCardImpl card, Keyword keyword)
    {
        return game().getModifiersQuerying().hasKeyword(gameState(), card, keyword);
    }

	/**
	 * @param card The card to inspect.
	 * @param type The card type to check for.
	 * @return Whether the current card has the given type, either printed on it or added (or removed) by a game effect.
	 */
    default boolean HasType(PhysicalCardImpl card, CardType type)
    {
        return game().getModifiersQuerying().getCardTypes(gameState(), card).contains(type);
    }

}
