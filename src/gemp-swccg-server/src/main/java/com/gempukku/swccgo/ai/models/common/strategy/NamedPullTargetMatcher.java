package com.gempukku.swccgo.ai.models.common.strategy;

import java.util.Locale;

/** Exact-title matcher for duplicate checks on specifically named pulls. */
public final class NamedPullTargetMatcher {

    private NamedPullTargetMatcher() {
    }

    public static boolean matchesTitle(String title, String... keywords) {
        if (title == null || keywords == null || keywords.length == 0) {
            return false;
        }
        String titleLower = title.toLowerCase(Locale.ROOT);
        boolean sawKeyword = false;
        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()) {
                continue;
            }
            sawKeyword = true;
            if (!titleLower.contains(
                    keyword.trim().toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        return sawKeyword;
    }
}
