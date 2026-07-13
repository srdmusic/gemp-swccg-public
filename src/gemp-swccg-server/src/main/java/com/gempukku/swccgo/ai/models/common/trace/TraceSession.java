package com.gempukku.swccgo.ai.models.common.trace;

/**
 * TRACE HOOK (2026-07-13): thread-local access point for the per-decision trace session.
 *
 * CombinedEvaluator opens a session only when its sink is enabled; EvaluatedAction's
 * score/veto choke points call the record* statics unconditionally. With no session
 * open (the production default, no-op sink) every call short-circuits on a plain
 * ThreadLocal null check: zero behavior, zero allocation beyond that cheap guard.
 *
 * Instrumentation must never throw into the decision path, so every method here
 * swallows Throwable after the null check.
 */
public final class TraceSession {

    private static final ThreadLocal<TraceCollector> CURRENT = new ThreadLocal<>();

    private TraceSession() {
        // static access only
    }

    /** Open a session for one decision on this thread. Returns false on any failure. */
    public static boolean open(String decisionId, String decisionType, String decisionText) {
        try {
            CURRENT.set(new TraceCollector(decisionId, decisionType, decisionText));
            return true;
        } catch (Throwable t) {
            abandon();
            return false;
        }
    }

    /** Close the session and build the one complete record. Null when nothing was open. */
    public static DecisionTrace close() {
        try {
            TraceCollector collector = CURRENT.get();
            CURRENT.remove();
            return (collector != null) ? collector.finish() : null;
        } catch (Throwable t) {
            abandon();
            return null;
        }
    }

    /** Drop any open session without producing a record. */
    public static void abandon() {
        try {
            CURRENT.remove();
        } catch (Throwable ignored) {
            // nothing sensible left to do
        }
    }

    /** True when a trace session is open on this thread. */
    public static boolean isActive() {
        return CURRENT.get() != null;
    }

    /** Bind an evaluator id to every operation recorded until endEvaluator(). */
    public static void beginEvaluator(String evaluatorId) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.beginEvaluator(evaluatorId);
        } catch (Throwable ignored) {
        }
    }

    public static void endEvaluator() {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.endEvaluator();
        } catch (Throwable ignored) {
        }
    }

    /** Freeze an action id's first-seen candidate ordinal (merge-map insertion order). */
    public static void registerCandidate(String actionId) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.registerCandidate(actionId);
        } catch (Throwable ignored) {
        }
    }

    /** Mark an action instance synthetic (explicit ordinal + source marker in the record). */
    public static void markSynthetic(Object handle, String sourceMarker) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.markSynthetic(handle, sourceMarker);
        } catch (Throwable ignored) {
        }
    }

    /** Constructor score. */
    public static void recordInitial(Object handle, String actionId, float score,
                                     String ruleId, String domainId, String outputKind, String detail) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.record(handle, actionId, TraceOp.INITIAL,
                null, null, Float.floatToRawIntBits(score),
                false, null, ruleId, domainId, outputKind, detail);
        } catch (Throwable ignored) {
        }
    }

    /** Additive score change: before, delta, and after, all as raw float bits. */
    public static void recordAdd(Object handle, String actionId, float before, float delta, float after,
                                 String ruleId, String domainId, String outputKind, String detail) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.record(handle, actionId, TraceOp.ADD,
                Float.floatToRawIntBits(before), Float.floatToRawIntBits(delta), Float.floatToRawIntBits(after),
                false, null, ruleId, domainId, outputKind, detail);
        } catch (Throwable ignored) {
        }
    }

    /** Overwrite: before and after only. A SET never fakes an additive delta. */
    public static void recordSet(Object handle, String actionId, float before, float after,
                                 String ruleId, String domainId, String outputKind, String detail) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.record(handle, actionId, TraceOp.SET,
                Float.floatToRawIntBits(before), null, Float.floatToRawIntBits(after),
                false, null, ruleId, domainId, outputKind, detail);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Hard veto. effectiveVetoReason is the action's resulting veto reason (first reason
     * wins on repeat vetoes); requestedReason is this call's argument, kept as detail.
     */
    public static void recordHardVeto(Object handle, String actionId,
                                      String effectiveVetoReason, String requestedReason,
                                      String ruleId, String domainId, String outputKind) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.record(handle, actionId, TraceOp.HARD_VETO,
                null, null, null,
                true, effectiveVetoReason, ruleId, domainId, outputKind, requestedReason);
        } catch (Throwable ignored) {
        }
    }

    /** Merge boundary only: pre/post score bits and resulting veto state, no synthetic delta. */
    public static void recordMerge(Object handle, String actionId, float before, float after,
                                   boolean vetoed, String vetoReason, String detail) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.record(handle, actionId, TraceOp.MERGE,
                Float.floatToRawIntBits(before), null, Float.floatToRawIntBits(after),
                vetoed, vetoReason, null, null, null, detail);
        } catch (Throwable ignored) {
        }
    }

    /** Ranking step (bucket best, pre-final best). Score is nullable for empty ranks. */
    public static void recordRank(Object handle, String actionId, Float score, String detail) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.record(handle, actionId, TraceOp.RANK,
                null, null, (score != null) ? Float.floatToRawIntBits(score.floatValue()) : null,
                false, null, null, null, null, detail);
        } catch (Throwable ignored) {
        }
    }

    /** Selection of a winner (bucket winner, epilogue winner, pass, final best). */
    public static void recordSelect(Object handle, String actionId, Float score,
                                    boolean vetoed, String vetoReason, String detail) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.record(handle, actionId, TraceOp.SELECT,
                null, null, (score != null) ? Float.floatToRawIntBits(score.floatValue()) : null,
                vetoed, vetoReason, null, null, null, detail);
        } catch (Throwable ignored) {
        }
    }

    /** Finalization boundary (this increment: CombinedEvaluator's pre-final winner). */
    public static void recordFinalize(Object handle, String actionId, Float score,
                                      boolean vetoed, String vetoReason, String detail) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.record(handle, actionId, TraceOp.FINALIZE,
                null, null, (score != null) ? Float.floatToRawIntBits(score.floatValue()) : null,
                vetoed, vetoReason, null, null, null, detail);
        } catch (Throwable ignored) {
        }
    }
}
