package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeployDestinationCompatibilitySourceParityTest {

    @Test
    public void destinationCompatibilityAdaptersRemainExactMirrors()
            throws IOException {
        assertEquals(normalize(compatibilitySlice(evaluatorSource("rando"))),
                normalize(compatibilitySlice(evaluatorSource("chosenone"))));
    }

    @Test
    public void sharedSitingOwnerReplacesEveryDirectScoreBody()
            throws IOException {
        String source = compatibilitySlice(evaluatorSource("rando"));
        for (String call : new String[]{
                "DeploySitingPolicy.evaluateShipReferenceGround(",
                "DeploySitingPolicy.evaluateStarshipDestination(",
                "DeploySitingPolicy.evaluateVehicleDestination(",
                "DeploySitingPolicy.evaluatePermanentWeaponDestination(",
                "DeploySitingPolicy.evaluateEmptyDockingBay(",
                "DeploySitingPolicy.evaluateBattlegroundLocation("}) {
            assertTrue(call, source.contains(call));
        }
        for (String retired : new String[]{
                "action.addReasoning(\"V29 SHIP CHARACTER ON GROUND:",
                "action.addReasoning(\"⚠️ STARSHIP TO SITE",
                "action.addReasoning(String.format(\n                                                    \"⚠️ SPACE POWER",
                "action.addReasoning(String.format(\n                                                    \"Good space position",
                "action.addReasoning(String.format(\n                                                    \"Close space fight",
                "action.addReasoning(\"Uncontested space system",
                "action.addReasoning(\"Starship to space system",
                "action.addReasoning(\"VEHICLE TO SPACE",
                "action.addReasoning(\"VEHICLE TO INTERIOR-ONLY",
                "action.addReasoning(\"Vehicle to exterior ground",
                "action.addReasoning(\"V24.14B WEAPON CHAR TO SPACE:",
                "action.addReasoning(\"V24.14B WEAPON CHAR ON GROUND:",
                "action.addReasoning(\"V29.7 EMPTY BAY:",
                "action.addReasoning(\"V29.6 Battleground location"}) {
            assertFalse(retired, source.contains(retired));
        }
        assertFalse("V271 must not add candidate short-circuiting",
                source.contains("continue;"));
    }

    @Test
    public void policyCallsRemainInLegacyContributionOrder() throws IOException {
        String source = compatibilitySlice(evaluatorSource("rando"));
        int shipReference = source.indexOf(
                "DeploySitingPolicy.evaluateShipReferenceGround(");
        int starship = source.indexOf(
                "DeploySitingPolicy.evaluateStarshipDestination(");
        int vehicle = source.indexOf(
                "DeploySitingPolicy.evaluateVehicleDestination(");
        int weaponSpace = source.indexOf(
                "DeploySitingPolicy.PermanentWeaponDestinationState.SPACE");
        int weaponGround = source.indexOf(
                "DeploySitingPolicy.PermanentWeaponDestinationState.GROUND");
        int emptyBay = source.indexOf(
                "DeploySitingPolicy.evaluateEmptyDockingBay(");
        int battleground = source.indexOf(
                "DeploySitingPolicy.evaluateBattlegroundLocation(");

        assertTrue(shipReference >= 0 && starship > shipReference
                && vehicle > starship && weaponSpace > vehicle
                && weaponGround > weaponSpace && emptyBay > weaponGround
                && battleground > emptyBay);
    }

    @Test
    public void expensiveReadsRemainInsideOriginalLazyBranches()
            throws IOException {
        String source = compatibilitySlice(evaluatorSource("rando"));

        int starship = source.indexOf("if (isStarship) {");
        int system = source.indexOf("else if (isSpaceSystem) {", starship);
        int gameGuard = source.indexOf("if (game != null) {", system);
        int ourPower = source.indexOf("float ourPower =", gameGuard);
        int theirPower = source.indexOf("float theirPower =", ourPower);
        int contested = source.indexOf("if (theirPower > 0) {", theirPower);
        int deployingBlueprint = source.indexOf(
                "getBlueprintFromId(context, deployingBlueprintId)", contested);
        int fallbackCatch = source.indexOf("} catch (Exception e) {", deployingBlueprint);
        int fallbackPolicy = source.indexOf(
                "StarshipDestinationState.SPACE_FALLBACK", fallbackCatch);
        assertTrue(starship >= 0 && system > starship && gameGuard > system
                && ourPower > gameGuard && theirPower > ourPower
                && contested > theirPower && deployingBlueprint > contested
                && fallbackCatch > deployingBlueprint
                && fallbackPolicy > fallbackCatch);

        int vehicle = source.indexOf("if (isVehicle) {");
        int vehicleSite = source.indexOf(
                "else if (isGroundSite || isDockingBay) {", vehicle);
        int exterior = source.indexOf("blueprint.hasIcon", vehicleSite);
        assertTrue(vehicle >= 0 && vehicleSite > vehicle && exterior > vehicleSite);

        int weaponSpace = source.indexOf("if (isCharacter && isSpaceSystem) {");
        int weaponBlueprint = source.indexOf(
                "getBlueprintFromId(context, deployingBlueprintId)", weaponSpace);
        int weaponFallback = source.indexOf("if (!hasPermanentWeapon) {", weaponBlueprint);
        int weaponPolicy = source.indexOf(
                "PermanentWeaponDestinationState.SPACE", weaponFallback);
        assertTrue(weaponSpace >= 0 && weaponBlueprint > weaponSpace
                && weaponFallback > weaponBlueprint && weaponPolicy > weaponFallback);

        int emptyBay = source.indexOf("if (isCharacter && isDockingBay");
        int bayLoop = source.indexOf("for (PhysicalCard bc : bayCards)", emptyBay);
        int bayBreak = source.indexOf("break;", bayLoop);
        int bayPolicy = source.indexOf(
                "DeploySitingPolicy.evaluateEmptyDockingBay(", bayBreak);
        assertTrue(emptyBay >= 0 && bayLoop > emptyBay
                && bayBreak > bayLoop && bayPolicy > bayBreak);

        int battleground = source.indexOf(
                "if (blueprint != null && blueprint.getCardCategory() == CardCategory.LOCATION)");
        int gameText = source.indexOf("gameTextBg", battleground);
        int titleFallback = source.indexOf("titleLower.contains(\"battleground\")", gameText);
        int iconFallback = source.indexOf("blueprint.hasIcon", titleFallback);
        int bgPolicy = source.indexOf(
                "DeploySitingPolicy.evaluateBattlegroundLocation(", iconFallback);
        assertTrue(battleground >= 0 && gameText > battleground
                && titleFallback > gameText && iconFallback > titleFallback
                && bgPolicy > iconFallback);
    }

    @Test
    public void pureOwnerHasNoEngineOrForbiddenMetadataDependencies()
            throws IOException {
        String policy = Files.readString(mainJavaRoot().resolve(
                "com/gempukku/swccgo/ai/models/common/phase/DeploySitingPolicy.java"));
        for (String forbidden : new String[]{
                "DecisionContext", "EvaluatedAction", "PhysicalCard",
                "SwccgGame", "GameState", "ModifiersQuerying", "DeckOracle",
                "ObjectiveAnalyzer", "DecisionOrigin", "DecisionActionSemantic",
                "DecisionWire", "PullDeployRef", "PullPhysicalCardRef",
                "DeployDestinationRef", "DeployPhysicalCardRef",
                "DeployActionMetadata"}) {
            assertFalse(forbidden, policy.contains(forbidden));
        }
    }

    private static String compatibilitySlice(String source) {
        int start = source.indexOf(
                "// V29: SHIP-REFERENCING CHARACTERS ON GROUND");
        int end = source.indexOf("// V22: OBJECTIVE LOCATION BONUS", start);
        assertTrue(start >= 0 && end > start);
        return source.substring(start, end);
    }

    private static String evaluatorSource(String bot) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators/CardSelectionEvaluator.java"));
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

    private static String normalize(String source) {
        return source.replace("models.rando", "models.BOT")
                .replace("models.chosenone", "models.BOT");
    }
}
