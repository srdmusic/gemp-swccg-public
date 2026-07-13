package com.gempukku.swccgo.ai.models.common.trace;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * TRACE ORACLE V2 (2026-07-13, Handoffs/CODEX_TRACE_ORACLE_V2_CONTRACT_2026-07-13.md
 * "Operation record"): validated stable rule-arm identity for migrated arms.
 *
 * Replaces the former free-form String ruleId. The id form is the registry tag shape
 * from resources/DOMAIN_REGISTRY_2026-07-12.md ("V24.15-drain", "V67bc",
 * "FS-L1-abandon", "vehicle-pilot+docking-bay", ...): nonblank, no whitespace, limited
 * charset, bounded length — prose can NEVER validate as identity, so a V-tag parsed out
 * of reasoning text is rejected at construction. Unmigrated code uses the one explicit
 * {@link #LEGACY_UNTAGGED} value: visible debt, never guessed metadata.
 */
public record TraceRuleId(String id) {

    /** Registry tag shape: starts alphanumeric; then alphanumerics . _ + # - ; no whitespace.
     *  NOTE: declared BEFORE the LEGACY_UNTAGGED constant — static initializers run in
     *  declaration order, and the constructor validates against this pattern. */
    private static final Pattern STABLE_ID_FORM = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_.+#-]*");
    private static final int MAX_LENGTH = 64;

    /** Rule id recorded by the un-migrated legacy choke points. */
    public static final TraceRuleId LEGACY_UNTAGGED = new TraceRuleId("LEGACY_UNTAGGED");

    public TraceRuleId {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("rule id must be nonblank");
        }
        if (id.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("rule id exceeds " + MAX_LENGTH
                + " chars — prose is not identity: \"" + id + "\"");
        }
        if (!STABLE_ID_FORM.matcher(id).matches()) {
            throw new IllegalArgumentException("rule id is not a stable registry tag form"
                + " (no whitespace/prose allowed): \"" + id + "\"");
        }
    }

    /** Factory for migrated arms. Identical validation to the canonical constructor. */
    public static TraceRuleId of(String id) {
        return new TraceRuleId(id);
    }
}
