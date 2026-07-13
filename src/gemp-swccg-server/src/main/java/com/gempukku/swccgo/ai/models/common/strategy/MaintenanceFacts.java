package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.game.SwccgCardBlueprint;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * T2 helper COMMIT-1 (2026-07-06, audit force-economy-1): the ONE maintenance-cost
 * basis for both bots (rando AND chosenone — shared file, precedent
 * CharacterDeploySiteEvaluator).
 *
 * GROUND TRUTH: the engine charges a CARD-SPECIFIC maintain cost, NOT deploy cost —
 * AbstractNonLocationPlaysToTable.java:1833/1894 builds the end-of-turn maintain
 * option from each card's own override returning a UseForceEffect. Full universe is
 * 8 cards, all encoding the number in game text ("End of your turn: ... Use N ..."):
 *   Lando, Scoundrel 1 (Card13_027, deploy 5) — Han, Chewie, And The Falcon 3
 *   (Card13_021) — Chewie, Enraged 2 (Card13_009) — Boba Fett, Bounty Hunter 2
 *   (Card13_059) — Blizzard 4 1 (Card13_056) — Thok And Thug 2
 *   (Card13_092) — Stormtrooper Garrison 1 (Card13_087) — Ap'lek 1 (Card222_002, "Use 1 or
 *   [Skull]" variant).
 * The AI's old basis (deploy cost, V22.3/V27/V24.5/V29.13/V38/V59 sites) was
 * OVER-reserving 2-5x; the V67w basis (+1 flat) was UNDER for maintain-2/3 cards.
 *
 * Regex verified to parse all 8 cards above. Fallback on no-match (future V-set
 * wordings) = deployCost, today's conservative majority basis (orchestrator ruling
 * H3). Memoized by blueprint (blueprints are per-card singletons in the library).
 */
public final class MaintenanceFacts {

    // Case-insensitive; DOTALL so the ".*?" bridge survives any line breaks in text.
    private static final Pattern MAINTAIN_PATTERN =
        Pattern.compile("end of your turn:.*?use (\\d+)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Map<SwccgCardBlueprint, Integer> CACHE = new ConcurrentHashMap<>();

    private MaintenanceFacts() {
    }

    /**
     * Engine-accurate end-of-turn maintain cost for this blueprint.
     * 0 for non-maintenance cards (no Icon.MAINTENANCE).
     */
    public static int maintainCost(SwccgCardBlueprint bp) {
        if (bp == null) return 0;
        try {
            if (!bp.hasIcon(Icon.MAINTENANCE)) return 0;
            Integer cached = CACHE.get(bp);
            if (cached != null) return cached;
            int cost = computeMaintainCost(bp);
            CACHE.put(bp, cost);
            return cost;
        } catch (Exception e) {
            return 0;  // matches the try/ignore semantics of the old inline scans
        }
    }

    private static int computeMaintainCost(SwccgCardBlueprint bp) {
        try {
            String gameText = bp.getGameText();
            if (gameText != null) {
                Matcher m = MAINTAIN_PATTERN.matcher(gameText);
                if (m.find()) {
                    return Integer.parseInt(m.group(1));
                }
            }
        } catch (Exception e) {
            // fall through to the deploy-cost fallback
        }
        // Fallback = deployCost (H3): conservative, preserves the old majority basis
        // if a future V-set words maintenance differently.
        try {
            Float dc = bp.getDeployCost();
            return dc != null ? dc.intValue() : 1;
        } catch (Exception e) {
            return 1;
        }
    }
}
