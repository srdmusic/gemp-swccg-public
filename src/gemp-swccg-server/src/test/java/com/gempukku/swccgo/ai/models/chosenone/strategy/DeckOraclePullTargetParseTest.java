package com.gempukku.swccgo.ai.models.chosenone.strategy;

import com.gempukku.swccgo.common.Persona;
import org.junit.Test;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * BATCH1 correction fixtures (Codex m00262 gate): forced-location grammar vs
 * title tokens in parseSourceCardPullTargets, plus the typed-persona flip
 * exemption helper. Pure static tests — no server, no logs, no replays.
 *
 * The grammar rule under test: Decipher game text writes card TITLES in Title
 * Case ("I've Got A Problem Here") but the location-forcing adverb lowercase
 * ("[download] Krennic here"). The suffix strip is case-sensitive and runs
 * before lowercasing.
 */
public class DeckOraclePullTargetParseTest {

    // ── destination suffix (lowercase adverb) is stripped ──

    @Test
    public void downloadKrennicHere_stripsForcedLocationSuffix() {
        // Card216_016 dark-side text (the real Krennic pull).
        List<String> t = DeckOracle.parseSourceCardPullTargets("May [download] Krennic here.");
        assertTrue("expected bare 'krennic', got " + t, t.contains("krennic"));
        assertFalse("suffix must be stripped, got " + t, t.contains("krennic here"));
    }

    @Test
    public void deployThere_stripsForcedLocationSuffix() {
        List<String> t = DeckOracle.parseSourceCardPullTargets(
            "May deploy Vader there from Reserve Deck.");
        assertTrue("expected bare 'vader', got " + t, t.contains("vader"));
        assertFalse(t.contains("vader there"));
    }

    @Test
    public void atThatLocation_stripsForcedLocationSuffix() {
        List<String> t = DeckOracle.parseSourceCardPullTargets(
            "May [download] Krennic at that location.");
        assertTrue("expected bare 'krennic', got " + t, t.contains("krennic"));
    }

    // ── Title Case Here/There endings are REAL card titles and must survive ──

    @Test
    public void iveGotAProblemHere_titlePreserved() {
        // Real light-side card title (Codex m00262 regression case).
        List<String> t = DeckOracle.parseSourceCardPullTargets(
            "Take I've Got A Problem Here into hand from Reserve Deck.");
        assertTrue("full title must survive, got " + t, t.contains("i've got a problem here"));
    }

    @Test
    public void empireKnowsWereHere_titlePreserved() {
        // Real card title ending in 'Here' (verifier corpus case).
        List<String> t = DeckOracle.parseSourceCardPullTargets(
            "May [download] The Empire Knows We're Here.");
        assertTrue("full title must survive, got " + t, t.contains("empire knows we're here"));
    }

    @Test
    public void titleAndForcedSuffixMixedList_bothHandled() {
        List<String> t = DeckOracle.parseSourceCardPullTargets(
            "May [download] I've Got A Problem Here or Krennic here.");
        assertTrue("title target must survive, got " + t, t.contains("i've got a problem here"));
        assertTrue("forced-location target must be stripped, got " + t, t.contains("krennic"));
    }

    // ── persona flip exemption helper (m00225 #1 / m00262 fixtures) ──

    @Test
    public void personaKrennic_matchesFlipTextForBothPrintings() {
        // Both Krennic printings (Card207_020 'Director Orson Krennic' and
        // Card209_036 'Krennic, Death Star Commandant') declare Persona.KRENNIC;
        // the flip condition names him by persona word, not title order.
        String flip = "flip this card if krennic on scarif and your leader controls this site";
        assertTrue(DeckOracle.personaNamedInText(EnumSet.of(Persona.KRENNIC), flip));
    }

    @Test
    public void personaNotNamed_noExemption() {
        // Death Star-only condition: KRENNIC persona must NOT match.
        String flip = "flip this card if death star system on table and opponent occupies no scarif sites";
        assertFalse(DeckOracle.personaNamedInText(EnumSet.of(Persona.KRENNIC), flip));
    }

    @Test
    public void personaSubstring_noFalsePositive() {
        // Word boundary blocks 'krennicity'-style substrings.
        String flip = "flip this card if krennicity established at this site";
        assertFalse(DeckOracle.personaNamedInText(EnumSet.of(Persona.KRENNIC), flip));
    }

    @Test
    public void personaEmptyOrNull_noExemption() {
        assertFalse(DeckOracle.personaNamedInText(Collections.emptySet(), "krennic"));
        assertFalse(DeckOracle.personaNamedInText(null, "krennic"));
        assertFalse(DeckOracle.personaNamedInText(EnumSet.of(Persona.KRENNIC), null));
    }

    // ── guard sanity: parse output stays lowercase-normalized ──

    @Test
    public void outputsAreLowercase() {
        for (String t : DeckOracle.parseSourceCardPullTargets(
                "May [download] The Empire Knows We're Here.")) {
            assertEquals(t, t.toLowerCase(java.util.Locale.ROOT));
        }
    }
}
