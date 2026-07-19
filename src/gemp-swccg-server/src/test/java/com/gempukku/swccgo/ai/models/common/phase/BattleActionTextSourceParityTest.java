package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BattleActionTextSourceParityTest {

    @Test
    public void v25ConditionsWeightsAndReasonsHaveOnePureOwner()
            throws IOException {
        String policy = policySource();

        for (String bot : new String[] {"rando", "chosenone"}) {
            String adapter = adapterSource(bot);
            assertTrue(adapter.contains(
                    "BattleActionTextPolicy.effectivePowerDifference("));
            assertTrue(adapter.contains(
                    "BattleActionTextPolicy.scoreInitiation("));
            assertTrue(adapter.contains(
                    "new BattleActionTextFacts.InitiationFacts("));

            for (String retired : new String[] {
                    "facts.theirPower() > facts.ourPower() * 2.0f",
                    "effectiveDiff >= 8.0f",
                    "effectiveDiff >= 5.0f",
                    "effectiveDiff >= 2.0f",
                    "effectiveDiff >= -2.0f",
                    "if (effectiveDiff < -8.0f)",
                    "if (effectiveDiff < -15.0f)",
                    "V25 BATTLE CRUSH at",
                    "V25 BATTLE: Initiate battle (no location data)",
                    "V25 BATTLE: Low reserve ("}) {
                assertFalse(bot + ": " + retired, adapter.contains(retired));
                assertTrue(retired, policy.contains(retired));
            }
        }
    }

    @Test
    public void adaptersRetainEngineReadsResolutionLogsAndMutation()
            throws IOException {
        for (String bot : new String[] {"rando", "chosenone"}) {
            String adapter = adapterSource(bot);
            assertTrue(adapter.contains("BattleTargetResolver.resolve("));
            assertTrue(adapter.contains("getTotalPowerAtLocation("));
            assertTrue(adapter.contains("getTotalAbilityAtLocation("));
            assertTrue(adapter.contains("getReserveDeckSize("));
            assertTrue(adapter.contains("logger.warn(\"V25 BATTLE EVAL"));
            assertTrue(adapter.contains(
                    "PolicyOperationAdapter.apply(action, battleInitiationTextLedger)"));
        }
    }

    @Test
    public void battleTwoWeightsAndReasonsHaveOnePureOwner()
            throws IOException {
        String policy = policySource();
        String[] policyMethods = {
                "scoreYouAreBeatenMode(",
                "scoreAddBattleDestiny(", "scoreHatred(",
                "scoreIHaveYouNow(", "scoreFmftd(",
                "scoreVaderRecall(", "scoreInquisitorRecall(",
                "scoreStunningLeader(", "scoreGenericYouAreBeaten(",
                "scoreBattleDestinyModifier(", "scoreWeaponDestinyModifier(",
                "scoreProtectDestiny(", "scorePreventOpponentBattleDestiny(",
                "scoreKillShot(", "scoreSubstituteDestiny(",
                "scoreCancelWeaponTargeting(", "scoreImmuneToAttrition(",
                "scoreProtectForfeit(", "scoreRetargetWeapon("};
        String[] retiredMutations = {
                "action.addReasoning(\"V37.1 HATRED:",
                "action.addReasoning(\"V35.7 HATRED:",
                "action.addReasoning(\"V29.9 IHYN:",
                "action.addReasoning(\"V35 FMFTD",
                "action.addReasoning(\"V35 VADER RECALL:",
                "action.addReasoning(\"V35.1 INQUISITOR RECALL",
                "action.addReasoning(\"V37.2 STUNNING LEADER:",
                "action.addReasoning(\"V35.4 YOU ARE BEATEN:",
                "action.addReasoning(\"+1 to battle destiny",
                "action.addReasoning(\"Boost weapon destiny",
                "action.addReasoning(\"Prevent opponent battle destiny",
                "action.addReasoning(\"V175 KILL SHOT:",
                "action.addReasoning(\"V175 SUBSTITUTE",
                "action.addReasoning(\"Cancel weapon targeting",
                "action.addReasoning(\"Make character immune to attrition",
                "action.addReasoning(\"Protect forfeit value during battle",
                "action.addReasoning(\"Re-target weapon at enemy"};

        for (String method : policyMethods) {
            assertTrue(method, policy.contains(method));
        }
        for (String bot : new String[] {"rando", "chosenone"}) {
            String adapter = adapterSource(bot);
            for (String method : policyMethods) {
                assertTrue(bot + ": " + method,
                        adapter.contains("BattleActionTextPolicy." + method));
            }
            for (String retired : retiredMutations) {
                assertFalse(bot + ": " + retired, adapter.contains(retired));
            }
        }
    }

    @Test
    public void battleTwoAdaptersRetainAllGameAndCardReads()
            throws IOException {
        for (String bot : new String[] {"rando", "chosenone"}) {
            String adapter = adapterSource(bot);
            for (String retained : new String[] {
                    "context.getDeckOracle()", "findCardById(",
                    "getBattleState()", "getStackedCards(",
                    "getTopLocations()", "getTotalPowerAtLocation(",
                    "getTopOfUnresolvedDestinyDraws(",
                    "getBlueprint().getDestiny()", "hasAbilityAttribute()"}) {
                assertTrue(bot + ": " + retained, adapter.contains(retained));
            }
            assertTrue(adapter.contains("applyBattleActionTextPolicy(action,"));
        }
    }

    @Test
    public void pureOwnerHasNoEngineOrForbiddenMetadataDependencies()
            throws IOException {
        String sources = policySource() + factsSource();
        for (String forbidden : new String[] {
                "DecisionContext", "EvaluatedAction", "PhysicalCard",
                "SwccgGame", "GameState", "ModifiersQuerying",
                "DecisionOrigin", "DecisionActionSemantic", "DecisionWire",
                "PullDeployRef", "PullPhysicalCardRef", "DeployDestinationRef",
                "DeployPhysicalCardRef", "DeployActionMetadata",
                "hardVeto(", "defer("}) {
            assertFalse(forbidden, sources.contains(forbidden));
        }
    }

    @Test
    public void stockCardActionIdsAreNonNullOrdinals() throws IOException {
        String decisionSource = Files.readString(repositoryRoot().resolve(
                "src/gemp-swccg-logic/src/main/java/com/gempukku/swccgo/logic/decisions/CardActionSelectionDecision.java"));

        assertTrue(decisionSource.contains("result[i] = String.valueOf(i);"));
    }

    private static String policySource() throws IOException {
        return Files.readString(phaseRoot().resolve("BattleActionTextPolicy.java"));
    }

    private static String factsSource() throws IOException {
        return Files.readString(phaseRoot().resolve("BattleActionTextFacts.java"));
    }

    private static String adapterSource(String bot) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators/ActionTextEvaluator.java"));
    }

    private static Path phaseRoot() {
        return mainJavaRoot().resolve("com/gempukku/swccgo/ai/models/common/phase");
    }

    private static Path mainJavaRoot() {
        Path cursor = Paths.get("").toAbsolutePath().normalize();
        while (cursor != null) {
            Path repoLayout = cursor.resolve(
                    "src/gemp-swccg-server/src/main/java");
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

    private static Path repositoryRoot() {
        Path cursor = Paths.get("").toAbsolutePath().normalize();
        while (cursor != null) {
            if (Files.isRegularFile(cursor.resolve(
                    "src/gemp-swccg-logic/src/main/java/com/gempukku/swccgo/logic/decisions/CardActionSelectionDecision.java"))) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        throw new AssertionError("Could not locate repository root");
    }
}
