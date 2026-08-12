package com.gempukku.swccgo.ai.models.chosenone.evaluators;

import java.util.Random;

/**
 * Predicts battle outcomes using Monte Carlo simulation.
 * 
 * Week 4 Improvement: Instead of simple power comparison,
 * simulates battles 50 times to predict win probability.
 * 
 * Accounts for:
 * - Destiny draws (random 0-6 per draw)
 * - Power totals
 * - Multiple simulation runs for accuracy
 */
public class BattlePredictor {
    
    private static final Random random = new Random();
    private static final int SIMULATIONS = 50;  // Run 50 battle simulations
    private static final int DESTINY_MAX = 6;   // Destiny range: 0-6
    
    /**
     * Predict battle outcome.
     * 
     * @param myPower Our total power at location
     * @param myDestinyDraws Number of destiny draws we'll get
     * @param oppPower Opponent's total power
     * @param oppDestinyDraws Opponent's destiny draws
     * @return Battle outcome with win probability and expected damage
     */
    public static BattleOutcome predictBattle(
        int myPower, int myDestinyDraws,
        int oppPower, int oppDestinyDraws) {

        // int wins = 0;
        // ADJUSTED 2026-08-08 (passivity fix, m01683): ties counted as LOSSES in the
        // winRate (only strict wins incremented), biasing every parity fight toward the
        // V76 defeat block. A tie is no damage either way — count it as half a win.
        float wins = 0f;
        int totalDamageDealt = 0;
        int totalDamageTaken = 0;
        int totalMyBattleDestiny = 0;
        int totalOpponentBattleDestiny = 0;

        // Run simulations
        for (int i = 0; i < SIMULATIONS; i++) {
            int myBattleDestiny = simulateDestiny(myDestinyDraws);
            int opponentBattleDestiny = simulateDestiny(oppDestinyDraws);
            int myTotal = myPower + myBattleDestiny;
            int oppTotal = oppPower + opponentBattleDestiny;
            totalMyBattleDestiny += myBattleDestiny;
            totalOpponentBattleDestiny += opponentBattleDestiny;

            if (myTotal > oppTotal) {
                wins++;
                totalDamageDealt += (myTotal - oppTotal);
            } else if (oppTotal > myTotal) {
                totalDamageTaken += (oppTotal - myTotal);
            } else {
                wins += 0.5f;
            }
            // Ties = no damage either way
        }

        float winRate = wins / SIMULATIONS;
        float avgDamageDealt = (float) totalDamageDealt / SIMULATIONS;
        float avgDamageTaken = (float) totalDamageTaken / SIMULATIONS;
        float avgMyBattleDestiny =
                (float) totalMyBattleDestiny / SIMULATIONS;
        float avgOpponentBattleDestiny =
                (float) totalOpponentBattleDestiny / SIMULATIONS;
        
        return new BattleOutcome(
                winRate, avgDamageDealt, avgDamageTaken,
                avgMyBattleDestiny, avgOpponentBattleDestiny);
    }
    
    /**
     * V24.7: Predict battle using KNOWN opponent destiny average from deck peek.
     * Uses the real average instead of random 0-6 for opponent draws.
     * Our draws still use random simulation (we don't know our own draw order).
     */
    public static BattleOutcome predictBattleWithIntel(
        int myPower, int myDestinyDraws,
        int oppPower, int oppDestinyDraws,
        float knownOppDestinyAvg) {

        // int wins = 0;
        // ADJUSTED 2026-08-08 (passivity fix, m01683): same tie-as-loss bias as the
        // no-intel loop above — count ties as half a win on this route too.
        float wins = 0f;
        int totalDamageDealt = 0;
        int totalDamageTaken = 0;
        int totalMyBattleDestiny = 0;
        int opponentBattleDestiny =
                Math.round(knownOppDestinyAvg * oppDestinyDraws);

        for (int i = 0; i < SIMULATIONS; i++) {
            int myBattleDestiny = simulateDestiny(myDestinyDraws);
            int myTotal = myPower + myBattleDestiny;
            // Use known average for opponent instead of random
            int oppTotal = oppPower + opponentBattleDestiny;
            totalMyBattleDestiny += myBattleDestiny;

            if (myTotal > oppTotal) {
                wins++;
                totalDamageDealt += (myTotal - oppTotal);
            } else if (oppTotal > myTotal) {
                totalDamageTaken += (oppTotal - myTotal);
            } else {
                wins += 0.5f;
            }
        }

        float winRate = wins / SIMULATIONS;
        float avgDamageDealt = (float) totalDamageDealt / SIMULATIONS;
        float avgDamageTaken = (float) totalDamageTaken / SIMULATIONS;
        float avgMyBattleDestiny =
                (float) totalMyBattleDestiny / SIMULATIONS;

        return new BattleOutcome(
                winRate, avgDamageDealt, avgDamageTaken,
                avgMyBattleDestiny, opponentBattleDestiny);
    }

    /**
     * V24.7: Predict battle using BOTH sides' known destiny averages.
     * Rando knows his own reserve deck contents (DeckOracle), and may know
     * opponent's average from verification peeks (OpponentDeckTracker).
     * Uses deterministic calculation when averages are known — no randomness needed.
     */
    public static BattleOutcome predictBattleFullIntel(
        int myPower, int myDestinyDraws, float myDestinyAvg,
        int oppPower, int oppDestinyDraws, float oppDestinyAvg) {

        // With both averages known, use deterministic prediction
        int myTotal = myPower + Math.round(myDestinyAvg * myDestinyDraws);
        int oppTotal = oppPower + Math.round(oppDestinyAvg * oppDestinyDraws);

        float winRate;
        float damageDealt;
        float damageTaken;

        // ADJUSTED 2026-08-08 (passivity fix, m01683): BINARY winRate 1.0/0.5/0.0 meant
        // ANY projected average deficit — even 1 point — returned 0.0 and tripped the V76
        // flat -800 "probable defeat" block, while any edge claimed certainty. Grade by
        // margin instead: 0.5 +/- margin over the maximum destiny swing both sides could
        // still produce (destiny averages are AVERAGES, actual draws range 0..DESTINY_MAX),
        // clamped to [0.05, 0.95] so intel never claims certainty. Monotone in
        // (myTotal - oppTotal); an exact tie stays 0.5.
        // if (myTotal > oppTotal) {
        //     winRate = 1.0f;
        //     damageDealt = myTotal - oppTotal;
        //     damageTaken = 0;
        // } else if (oppTotal > myTotal) {
        //     winRate = 0.0f;
        //     damageDealt = 0;
        //     damageTaken = oppTotal - myTotal;
        // } else {
        //     winRate = 0.5f;
        //     damageDealt = 0;
        //     damageTaken = 0;
        // }
        float maxSwing = DESTINY_MAX * Math.max(1, myDestinyDraws + oppDestinyDraws);
        winRate = Math.max(0.05f, Math.min(0.95f,
            0.5f + (myTotal - oppTotal) / (2.0f * maxSwing)));
        if (myTotal > oppTotal) {
            damageDealt = myTotal - oppTotal;
            damageTaken = 0;
        } else {
            damageDealt = 0;
            damageTaken = oppTotal - myTotal;
        }

        return new BattleOutcome(
                winRate, damageDealt, damageTaken,
                Math.round(myDestinyAvg * myDestinyDraws),
                Math.round(oppDestinyAvg * oppDestinyDraws));
    }

    /**
     * V24.7: Predict battle using all available intel.
     * - Uses DeckOracle's average destiny for Rando's draws
     * - Uses OpponentDeckTracker's average for opponent draws (if available)
     * - Falls back to random simulation only where intel is missing
     */
    public static BattleOutcome predictBattle(
        int myPower, int myDestinyDraws,
        int oppPower, int oppDestinyDraws,
        com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle deckOracle,
        com.gempukku.swccgo.ai.models.chosenone.strategy.OpponentDeckTracker tracker) {

        float myAvg = -1;
        float oppAvg = -1;

        // Get Rando's own destiny average from DeckOracle
        if (deckOracle != null && deckOracle.isAnalyzed()) {
            double avg = deckOracle.getAverageDestinyInReserve();
            if (avg > 0) myAvg = (float) avg;
        }

        // Get opponent's destiny average from peek intel
        if (tracker != null && tracker.hasIntel()) {
            oppAvg = tracker.getOpponentDestinyAverage();
        }

        // Use full intel if both known
        if (myAvg > 0 && oppAvg > 0) {
            return predictBattleFullIntel(myPower, myDestinyDraws, myAvg,
                oppPower, oppDestinyDraws, oppAvg);
        }
        // Use opponent intel only
        if (oppAvg > 0) {
            return predictBattleWithIntel(myPower, myDestinyDraws,
                oppPower, oppDestinyDraws, oppAvg);
        }
        // No intel — fall back to random simulation
        return predictBattle(myPower, myDestinyDraws, oppPower, oppDestinyDraws);
    }

    /**
     * V24.7: Predict battle using OpponentDeckTracker intel if available.
     * Falls back to random simulation if no intel gathered yet.
     */
    public static BattleOutcome predictBattle(
        int myPower, int myDestinyDraws,
        int oppPower, int oppDestinyDraws,
        com.gempukku.swccgo.ai.models.chosenone.strategy.OpponentDeckTracker tracker) {

        if (tracker != null && tracker.hasIntel()) {
            return predictBattleWithIntel(myPower, myDestinyDraws,
                oppPower, oppDestinyDraws, tracker.getOpponentDestinyAverage());
        }
        return predictBattle(myPower, myDestinyDraws, oppPower, oppDestinyDraws);
    }

    /**
     * Simulate destiny draws for one side.
     * Each draw is a random number 0-6.
     */
    private static int simulateDestiny(int draws) {
        int total = 0;
        for (int i = 0; i < draws; i++) {
            total += random.nextInt(DESTINY_MAX + 1);  // 0-6 inclusive
        }
        return total;
    }

    /**
     * Quick check: Should we initiate this battle?
     */
    public static boolean shouldInitiateBattle(
        int myPower, int myDestinyDraws,
        int oppPower, int oppDestinyDraws,
        float conservativeThreshold) {

        BattleOutcome outcome = predictBattle(myPower, myDestinyDraws, oppPower, oppDestinyDraws);
        return outcome.winProbability >= conservativeThreshold &&
               outcome.expectedDamageDealt >= outcome.expectedDamageTaken;
    }

    /**
     * V24.7: Should we initiate battle — using opponent deck intel if available.
     */
    public static boolean shouldInitiateBattle(
        int myPower, int myDestinyDraws,
        int oppPower, int oppDestinyDraws,
        float conservativeThreshold,
        com.gempukku.swccgo.ai.models.chosenone.strategy.OpponentDeckTracker tracker) {

        BattleOutcome outcome = predictBattle(myPower, myDestinyDraws, oppPower, oppDestinyDraws, tracker);
        return outcome.winProbability >= conservativeThreshold &&
               outcome.expectedDamageDealt >= outcome.expectedDamageTaken;
    }

    /**
     * V24.7: Should we initiate battle — using FULL intel (both sides' destiny averages).
     */
    public static boolean shouldInitiateBattle(
        int myPower, int myDestinyDraws,
        int oppPower, int oppDestinyDraws,
        float conservativeThreshold,
        com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle deckOracle,
        com.gempukku.swccgo.ai.models.chosenone.strategy.OpponentDeckTracker tracker) {

        BattleOutcome outcome = predictBattle(myPower, myDestinyDraws, oppPower, oppDestinyDraws, deckOracle, tracker);
        return outcome.winProbability >= conservativeThreshold &&
               outcome.expectedDamageDealt >= outcome.expectedDamageTaken;
    }

    /**
     * Quick favorable check with default 60% threshold.
     */
    public static boolean shouldInitiateBattle(
        int myPower, int myDestinyDraws,
        int oppPower, int oppDestinyDraws) {
        return shouldInitiateBattle(myPower, myDestinyDraws, oppPower, oppDestinyDraws, 0.6f);
    }
    
    /**
     * Outcome of battle prediction.
     */
    public static class BattleOutcome {
        /** Probability of winning (0.0 to 1.0) */
        public final float winProbability;
        
        /** Average damage we'll deal if we win */
        public final float expectedDamageDealt;
        
        /** Average damage we'll take if we lose */
        public final float expectedDamageTaken;

        /** Average total battle destiny added to our power */
        public final float expectedMyBattleDestiny;

        /** Average total battle destiny added to opponent power */
        public final float expectedOpponentBattleDestiny;
        
        public BattleOutcome(float winProb, float damageDealt, float damageTaken) {
            this(winProb, damageDealt, damageTaken, Float.NaN, Float.NaN);
        }

        public BattleOutcome(
                float winProb,
                float damageDealt,
                float damageTaken,
                float myBattleDestiny,
                float opponentBattleDestiny) {
            this.winProbability = winProb;
            this.expectedDamageDealt = damageDealt;
            this.expectedDamageTaken = damageTaken;
            this.expectedMyBattleDestiny = myBattleDestiny;
            this.expectedOpponentBattleDestiny = opponentBattleDestiny;
        }
        
        /**
         * Is this battle favorable?
         * Default threshold: 60%+ win rate.
         */
        public boolean isFavorable() {
            return winProbability >= 0.6f;
        }
        
        /**
         * Is this battle favorable with custom threshold?
         */
        public boolean isFavorable(float threshold) {
            return winProbability >= threshold;
        }
        
        /**
         * Is this a risky battle?
         * Risky = win probability between 40-60% (coin flip)
         */
        public boolean isRisky() {
            return winProbability >= 0.4f && winProbability <= 0.6f;
        }
        
        /**
         * Is this battle dangerous?
         * Dangerous = less than 40% win probability
         */
        public boolean isDangerous() {
            return winProbability < 0.4f;
        }
        
        /**
         * Get expected net damage (positive = we deal more, negative = we take more)
         */
        public float getExpectedNetDamage() {
            return expectedDamageDealt - expectedDamageTaken;
        }
        
        @Override
        public String toString() {
            return String.format("BattleOutcome[winRate=%.1f%%, dmgDealt=%.1f, dmgTaken=%.1f]",
                winProbability * 100, expectedDamageDealt, expectedDamageTaken);
        }
    }
}
