package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeploySitingSourceCharacterizationTest {

    @Test
    public void deploySitingAdaptersStayNormalizedMirrors() throws IOException {
        assertEquals(normalize(evaluatorSource("rando", "DeployEvaluator.java")),
                normalize(evaluatorSource("chosenone", "DeployEvaluator.java")));
        assertEquals(normalize(evaluatorSource("rando", "CardSelectionEvaluator.java")),
                normalize(evaluatorSource("chosenone", "CardSelectionEvaluator.java")));
    }

    @Test
    public void sharedPoliciesOwnExtractedSitingOperations() throws IOException {
        String deploy = evaluatorSource("rando", "DeployEvaluator.java");
        String destination = evaluatorSource("rando", "CardSelectionEvaluator.java");

        assertTrue(deploy.contains("DeploySitingPolicy.evaluateDirect("));
        assertTrue(destination.contains("DeploySitingPolicy.evaluateDestination("));
        assertTrue(deploy.contains("advancesUnfilledFlipGateActorRequirement("));
        assertTrue(destination.contains("advancesUnfilledFlipGateActorRequirement("));
        assertTrue(destination.contains(
                "v212V193FormationSupported ="));
        assertTrue(destination.contains(
                "fsFriendlyCharacters > 0 || fsFundedBuddy"));
        assertTrue(destination.contains(
                "&& v212V193FormationSupported"));
        assertTrue(destination.contains("DeployObjectiveSitingPolicy.evaluate("));
        assertTrue(destination.contains("DeployTacticalPolicy.scoreV166ContestDrain("));
        assertTrue(destination.contains("DeployTacticalPolicy.scoreV169ProtectEndangered("));
        assertTrue(destination.contains("DeployTacticalPolicy.scoreV170SpyDrainBlock("));
        assertTrue(destination.contains("DeployTacticalPolicy.scoreV171V172Contact("));
        assertTrue(destination.contains(
                "DeployTacticalPolicy.evaluateStarshipDrainContact("));
        assertTrue(destination.contains(
                "new DeployTacticalPolicy.StarshipDrainContactFacts("));
        assertTrue(destination.contains(
                ".evaluateEmptyDestinationTopology("));
        assertTrue(destination.contains(
                ".evaluateReinforcementTopology("));
        assertTrue(destination.contains(
                "DeployFormationSitingPolicy.evaluateBuddyTopology("));
        for (String call : new String[]{
                "DeployFormationSitingPolicy.evaluateLegacySolo(",
                "DeployFormationSitingPolicy.evaluateStrongReinforcement(",
                "DeployFormationSitingPolicy.evaluateBuddySeek(",
                "DeployFormationSitingPolicy.evaluateHuntGrouping(",
                "DeployFormationSitingPolicy.scoreHighDrainSite(",
                "DeployFormationSitingPolicy.scoreGoodDrainSite(",
                "DeployFormationSitingPolicy.evaluatePositiveFormation(",
                "DeployTacticalPolicy.evaluateV53V51Drain(",
                "DeployTacticalPolicy.scoreV51VaderFlip(",
                "DeployTacticalPolicy.evaluateV50PowerDanger(",
                "DeployTacticalPolicy.scoreV34DirectEngage(",
                "DeployTacticalPolicy.scoreV36EmptyDeploy(",
                "DeployTacticalPolicy.scoreV51V43SpyPlacement(",
                "DeployObjectiveSitingPolicy.scoreCloudCityArmy(",
                "DeployObjectiveSitingPolicy.scoreObjectiveFirst(",
                "DeployObjectiveSitingPolicy.scoreKeyCharacter(",
                "DeployObjectiveSitingPolicy.evaluateCloudCityEngine(",
                "DeployObjectiveSitingPolicy.scoreGherant(",
                "DeployObjectiveSitingPolicy.evaluateLandoLobot(",
                "DeployObjectiveSitingPolicy.selectPostFlipHoldLocations(",
                "DeployObjectiveSitingPolicy.evaluateFlipSiting(",
                "DeployFormationSitingPolicy.evaluateAbilityThreshold(",
                "DeployFormationSitingPolicy.evaluateBuddyAbility(",
                "PullActionPolicy.evaluateWeaponOrder(",
                "DeployTacticalPolicy.scoreEvazanCombo("}) {
            assertTrue(call, deploy.contains(call));
        }

        assertFalse(deploy.contains("pairedDeployPossible"));
        assertFalse(deploy.contains("canDeployToOpponents"));
        assertFalse(deploy.contains("V38 SOLO CAUTION: %s (power %d) solo"));
        assertFalse(deploy.contains("V35.1 HUNT GROUP+ENGAGE: Deploy %s"));
        assertFalse(deploy.contains("V51 BUDDY DESTINY: Ability %.0f"));
        assertFalse(deploy.contains("V51 DRAIN EMERGENCY: %s drains %.0f"));
        assertFalse(deploy.contains("V50 EARLY DANGER: Turn %d"));
        assertFalse(deploy.contains("V34 DIRECT ENGAGE: Deploy %s"));
        assertFalse(deploy.contains("V51 SPY CRIPPLE: Spy at %s"));
        assertFalse(deploy.contains("V51 CC ARMY: Deploy to %s pre-flip"));
        assertFalse(deploy.contains("V51 OBJ FIRST: Deploy to %s"));
        assertFalse(deploy.contains("V67ak KEY CHARACTER: %s is named"));
        assertFalse(deploy.contains("action.addReasoning(\"V22.7 BLOCKED:"));
        assertFalse(deploy.contains("V24 TDIGWATT ENGINE: Deploy \" + card.getTitle()"));
        assertFalse(deploy.contains("V24.1 GHERANT: Deploys an Executor site"));
        assertFalse(deploy.contains("V29.2 LANDO: Key piece + backup present"));
        assertFalse(deploy.contains("V47 LANDO SOLO BLOCK: No friendlies at CC"));
        assertFalse(deploy.contains("V29.2 LOBOT: Helps flip TDIGWATT"));
        assertFalse(deploy.contains("V47 LOBOT SOLO BLOCK: No friendlies at CC"));
        assertFalse(deploy.contains("V36 DEFEND TERRITORY: Deploy to unoccupied obj location"));
        assertFalse(deploy.contains("V31 PRE-FLIP: %d obj locations still unoccupied"));
        assertFalse(deploy.contains("V31 POST-FLIP: Reinforce key hold location"));
        assertFalse(deploy.contains("V40 POST-FLIP: Deploying to 3rd obj loc"));
        assertFalse(deploy.contains("V32 ABILITY FIX: Deploy brings ability"));
        assertFalse(deploy.contains("V40 ABILITY: Solo deploy with ability"));
        assertFalse(deploy.contains("V40 ABILITY: Solo ability"));
        assertFalse(deploy.contains("V40 ABILITY: Total ability"));
        assertFalse(deploy.contains("V67ag NON-BG STACK PENALTY: %s already has %s"));
        assertFalse(deploy.contains("V33 BUDDY FIX: Deploy brings ability"));
        assertFalse(deploy.contains("V33 BUDDY BONUS: Reinforcing ability"));
        assertFalse(deploy.contains(
                "action.addReasoning(\"V67ao ORDER GATE:"));
        assertFalse(deploy.contains(
                "action.addReasoning(\"V149 NO LIGHTSABER WIELDER:"));
        assertFalse(deploy.contains(
                "action.addReasoning(\"V24.3 EVAZAN COMBO:"));

        int earlyDanger = deploy.indexOf(
                "DeployTacticalPolicy.PowerDangerOutcome.EARLY_DANGER");
        int locationContinue = deploy.indexOf("continue;", earlyDanger);
        int directEngage = deploy.indexOf(
                "DeployTacticalPolicy.scoreV34DirectEngage(", earlyDanger);
        assertTrue(earlyDanger >= 0);
        assertTrue(locationContinue > earlyDanger);
        assertTrue(directEngage > locationContinue);

        int cloudCityBlocked = deploy.indexOf(
                "new DeployObjectiveSitingPolicy.CloudCityEngineFacts(\n"
                        + "                                        actionId, card.getTitle(), false, false)");
        int blockedActionsAdd = deploy.indexOf("actions.add(action);", cloudCityBlocked);
        int blockedContinue = deploy.indexOf("continue;", blockedActionsAdd);
        int lazyDeckOracle = deploy.indexOf("context.getDeckOracle()", blockedContinue);
        assertTrue(cloudCityBlocked >= 0);
        assertTrue(blockedActionsAdd > cloudCityBlocked);
        assertTrue(blockedContinue > blockedActionsAdd);
        assertTrue(lazyDeckOracle > blockedContinue);

        int abilityTotal = deploy.indexOf(
                "float totalAfterDeploy = currentAbilityAtSite + cardAbility;");
        int lazyHandRead = deploy.indexOf(
                "gameState.getHand(v32PlayerId)", abilityTotal);
        int abilityPolicy = deploy.indexOf(
                "DeployFormationSitingPolicy.evaluateAbilityThreshold(",
                lazyHandRead);
        assertTrue(abilityTotal >= 0);
        assertTrue(deploy.indexOf(
                "if (totalAfterDeploy < 4.0f && friendlyCharCount == 0)",
                abilityTotal) > abilityTotal);
        assertTrue(lazyHandRead > abilityTotal);
        assertTrue(abilityPolicy > lazyHandRead);

        int nonBattleground = deploy.indexOf("if (!v67abIsBg)");
        int nonBgPolicy = deploy.indexOf(
                "DeployFormationSitingPolicy.evaluateBuddyAbility(",
                nonBattleground);
        int nonBgBreak = deploy.indexOf("break;", nonBgPolicy);
        int battlegroundAbilityRead = deploy.indexOf(
                "float v33CurrentAbility = 0;", nonBgBreak);
        assertTrue(nonBattleground >= 0);
        assertTrue(nonBgPolicy > nonBattleground);
        assertTrue(nonBgBreak > nonBgPolicy);
        assertTrue(battlegroundAbilityRead > nonBgBreak);

        int directWeaponOrder = deploy.indexOf(
                "PullActionPolicy.WeaponOrderEvaluation weaponOrder");
        int nextDeployRule = deploy.indexOf(
                "// === V51: CLOUD CITY ARMY PRE-FLIP", directWeaponOrder);
        assertTrue(directWeaponOrder >= 0);
        assertTrue(nextDeployRule > directWeaponOrder);
        String directWeaponAdapter = deploy.substring(
                directWeaponOrder, nextDeployRule);
        assertFalse(directWeaponAdapter.contains("continue;"));
        assertFalse(directWeaponAdapter.contains("return "));
        assertFalse(directWeaponAdapter.contains("actions.add("));

        int comboOracle = deploy.indexOf(
                "DeckOracle comboOracle = context.getDeckOracle()");
        int evazanClassification = deploy.indexOf(
                "boolean isEvazan =", comboOracle);
        int evazanBranch = deploy.indexOf("if (isEvazan)", evazanClassification);
        int partnerRead = deploy.indexOf(
                "weaponPartnerInPlay = comboOracle.isCardInPlay(\"Maul With Lightsaber\")",
                evazanBranch);
        int weaponBranch = deploy.indexOf(
                "else if (isWeaponChar)", partnerRead);
        int evazanRead = deploy.indexOf(
                "evazanInPlay = comboOracle.isCardInPlay(\"Evazan\")",
                weaponBranch);
        int comboPolicy = deploy.indexOf(
                "DeployTacticalPolicy.scoreEvazanCombo(", evazanRead);
        assertTrue(comboOracle >= 0);
        assertTrue(evazanClassification > comboOracle);
        assertTrue(evazanBranch > evazanClassification);
        assertTrue(partnerRead > evazanBranch);
        assertTrue(weaponBranch > partnerRead);
        assertTrue(evazanRead > weaponBranch);
        assertTrue(comboPolicy > evazanRead);
    }

    @Test
    public void retiredSitingBodiesCannotReenterTheDecisionTree() throws IOException {
        String destination = evaluatorSource("rando", "CardSelectionEvaluator.java");
        assertFalse(destination.contains("if (false /* V122 SUPERSEDED V136 */"));
        assertFalse(destination.contains("if (false /* V67as SUPERSEDED V136 */"));
        assertFalse(destination.contains("V75 KILL-BOX OVERRIDE:"));
        assertFalse(destination.contains("V67bj DON'T BAIT (uncommitted):"));
        assertFalse(destination.contains("action.addReasoning(\"V29 CONCENTRATE:"));
        assertFalse(destination.contains(
                "action.addReasoning(\"Establish at empty location"));
        assertFalse(destination.contains(
                "action.addReasoning(\"V29 REINFORCE SOLO CHARACTER"));
        assertFalse(destination.contains(
                "action.addReasoning(\"V29: Reinforce outnumbered pair"));
        assertTrue(destination.contains(
                "V67as, including nested V67br/V75/V67bj, became unreachable"));
    }

    @Test
    public void objectiveProgressFactsStayShadowOnlyAtPhysicalDeployRoutes() throws IOException {
        String deploy = evaluatorSource("rando", "DeployEvaluator.java");
        String destination = evaluatorSource("rando", "CardSelectionEvaluator.java");
        String actionText = evaluatorSource("rando", "ActionTextEvaluator.java");

        assertEquals(0, countOccurrences(deploy, ".assessDeployChild("));
        assertEquals(1, countOccurrences(destination, ".assessDeployChild("));
        assertEquals(0, countOccurrences(actionText, ".assessDeployChild("));
        assertTrue(destination.contains("V214 DEPLOY CHILD OBJECTIVE FACTS"));
    }

    @Test
    public void postFlipHoldAdaptersUseStructuredPhysicalLocationFacts()
            throws IOException {
        for (String bot : new String[]{"rando", "chosenone"}) {
            String deploy = evaluatorSource(bot, "DeployEvaluator.java");
            int postFlip = deploy.indexOf(
                    "// === POST-FLIP: Consolidate to fewer locations ===");
            int authoritative = deploy.indexOf(
                    "flipObjAnalyzer.hasStructuredFlipBackLocationRules()",
                    postFlip);
            int physicalRisk = deploy.indexOf(
                    "flipObjAnalyzer.isFlipBackProtectionLocation(\n"
                            + "                                                    loc, game, flipPlayerId)",
                    authoritative);
            int selection = deploy.indexOf(
                    "DeployObjectiveSitingPolicy.selectPostFlipHoldLocations(\n"
                            + "                                            structuredHoldLocationsAuthoritative,",
                    physicalRisk);

            assertTrue(bot, postFlip >= 0);
            assertTrue(bot, authoritative > postFlip);
            assertTrue(bot, physicalRisk > authoritative);
            assertTrue(bot, selection > physicalRisk);
            assertEquals(bot, 1, countOccurrences(deploy,
                    "hasStructuredFlipBackLocationRules()"));
        }
    }

    @Test
    public void sitingPoliciesContainNoEngineDecisionMetadata() throws IOException {
        String combined = commonPhaseSource("DeploySitingPolicy.java")
                + commonPhaseSource("DeployObjectiveSitingPolicy.java")
                + commonPhaseSource("DeployTacticalPolicy.java")
                + commonPhaseSource("DeployFormationSitingPolicy.java");
        for (String forbidden : new String[]{
                "DecisionOrigin", "DecisionActionSemantic", "DecisionWire",
                "PullDeployRef", "PullPhysicalCardRef", "DeployDestinationRef",
                "DeployPhysicalCardRef", "DeployActionMetadata"}) {
            assertFalse(forbidden, combined.contains(forbidden));
        }
    }

    private static String evaluatorSource(String bot, String fileName)
            throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators").resolve(fileName));
    }

    private static String commonPhaseSource(String fileName) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/common/phase")
                .resolve(fileName));
    }

    private static Path mainJavaRoot() {
        Path cursor = Paths.get("").toAbsolutePath().normalize();
        while (cursor != null) {
            Path repoLayout = cursor.resolve("src/gemp-swccg-server/src/main/java");
            if (Files.isDirectory(repoLayout)) {
                return repoLayout;
            }
            Path moduleLayout = cursor.resolve("src/main/java");
            if (Files.isDirectory(moduleLayout.resolve(
                    "com/gempukku/swccgo/ai/models"))) {
                return moduleLayout;
            }
            cursor = cursor.getParent();
        }
        throw new AssertionError("Could not locate gemp-swccg-server main/java");
    }

    private static String normalize(String source) {
        return source.replace("models.rando", "models.BOT")
                .replace("models.chosenone", "models.BOT");
    }

    private static int countOccurrences(String source, String needle) {
        int count = 0;
        int from = 0;
        while ((from = source.indexOf(needle, from)) >= 0) {
            count++;
            from += needle.length();
        }
        return count;
    }
}
