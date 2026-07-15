package com.gempukku.swccgo.ai.models.common.phase;

/** Pure owner of the ordered V57/V61c/V67at/V43 Force-activation amount policy. */
public final class ActivateAmountPolicy {

    public enum Mode {
        ACTIVATE_FULL,
        KEEP_THREE_FOR_BATTLE,
        KEEP_TWO_AT_LOW_LIFE
    }

    public record Input(int rawMinimum,
                        int rawMaximum,
                        int reserveDeckSize,
                        int lifeForce,
                        boolean battlePlausible) {
        public Input {
            if (rawMinimum < 0 || rawMaximum <= 0 || rawMaximum < rawMinimum) {
                throw new IllegalArgumentException("activation bounds must satisfy 0 <= min <= max and max > 0");
            }
        }
    }

    public record Result(int amount, Mode mode) {
    }

    private ActivateAmountPolicy() {
    }

    public static Result assess(Input input) {
        int amount = input.rawMaximum();
        Mode mode = Mode.ACTIVATE_FULL;

        if (input.battlePlausible()) {
            int keepThree = Math.max(1,
                    Math.min(input.rawMaximum(), input.reserveDeckSize() - 3));
            if (keepThree < amount) {
                amount = keepThree;
                mode = Mode.KEEP_THREE_FOR_BATTLE;
            }
        }

        if (input.lifeForce() <= 10) {
            int keepTwo = Math.max(1, input.rawMaximum() - 2);
            if (keepTwo < amount) {
                amount = keepTwo;
                mode = Mode.KEEP_TWO_AT_LOW_LIFE;
            }
        }

        if (amount <= 0) {
            amount = 1;
        }
        amount = Math.max(input.rawMinimum(), Math.min(amount, input.rawMaximum()));
        return new Result(amount, mode);
    }
}
