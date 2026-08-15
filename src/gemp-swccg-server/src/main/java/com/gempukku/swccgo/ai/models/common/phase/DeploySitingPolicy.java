package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.policy.ObjectivePreferencePolicy;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Shared pure owner of the bounded DEPLOY-2 siting score stream. */
public final class DeploySitingPolicy {
    private static final float V89_BAD_SITE_SCORE = -1500.0f;
    private static final float FORMATION_DEFER_SCORE = -800.0f;

    public enum FormationState {
        ALLOW,
        HARD_BLOCK,
        DEFER_UNSUPPORTED_SOLO,
        UNKNOWN
    }

    public enum StarshipDestinationState {
        NONE,
        SITE_BLOCKED,
        SPACE_FALLBACK,
        SPACE_UNCONTESTED,
        SPACE_DISADVANTAGE,
        SPACE_ADVANTAGE,
        SPACE_CLOSE
    }

    public enum VehicleDestinationState {
        NONE,
        SPACE_INVALID,
        INTERIOR_INVALID,
        EXTERIOR_VALID
    }

    public enum PermanentWeaponDestinationState {
        NONE,
        SPACE,
        GROUND
    }

    public record Facts(
            String actionId,
            String cardTitle,
            String siteTitle,
            boolean evazanWithoutArmedFriend,
            FormationState formationState,
            String formationReason,
            float v136Score,
            boolean v193Eligible,
            boolean v193FormationSupported,
            float v193PlaybookWeight,
            String v193GateCardTitle,
            boolean v96Applicable,
            float friendlyPower,
            float opponentPower,
            float deployingPower) {
        // V96 ADJUSTED 2026-08-08 (passivity fix, m01683): legacy-signature
        // constructor — deployingPower 0 preserves the pre-projection diff for
        // callers that never carried the deploying card's power.
        public Facts(
                String actionId, String cardTitle, String siteTitle,
                boolean evazanWithoutArmedFriend, FormationState formationState,
                String formationReason, float v136Score, boolean v193Eligible,
                boolean v193FormationSupported, float v193PlaybookWeight,
                String v193GateCardTitle, boolean v96Applicable,
                float friendlyPower, float opponentPower) {
            this(actionId, cardTitle, siteTitle, evazanWithoutArmedFriend,
                    formationState, formationReason, v136Score, v193Eligible,
                    v193FormationSupported, v193PlaybookWeight,
                    v193GateCardTitle, v96Applicable, friendlyPower,
                    opponentPower, 0.0f);
        }

        public Facts {
            Objects.requireNonNull(actionId, "actionId");
            cardTitle = cardTitle == null ? "" : cardTitle;
            siteTitle = siteTitle == null ? "" : siteTitle;
            formationState = formationState == null ? FormationState.ALLOW : formationState;
            formationReason = formationReason == null ? "" : formationReason;
            v193GateCardTitle = v193GateCardTitle == null ? "" : v193GateCardTitle;
            if (formationState != FormationState.ALLOW && formationReason.isBlank()) {
                throw new IllegalArgumentException("formationReason must be nonblank for an active formation state");
            }
        }
    }

    private DeploySitingPolicy() {
    }

    /** Preserves the direct-action order: V89, V136, V193, then V96. */
    public static PolicyResult evaluateDirect(Facts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();

        if (facts.evazanWithoutArmedFriend()) {
            operations.add(addSiting(facts.actionId(), "V89", TraceOutputKind.VETO,
                    V89_BAD_SITE_SCORE,
                    "V89 DR. EVAZAN: '" + facts.cardTitle() + "' deploying to '"
                            + facts.siteTitle()
                            + "' with no armed friend — block (will get sniped)"));
        }

        addV136(operations, facts.actionId(), "V136", facts.siteTitle(),
                facts.v136Score(), false);

        if (facts.v193Eligible() && facts.v193FormationSupported()) {
            operations.add(addObjectiveSiting(facts.actionId(), "V193", TraceOutputKind.BANDED,
                    "V193 FLIP-GATE CONTROL: steer one body to '" + facts.siteTitle()
                            + "' to enable '" + facts.v193GateCardTitle()
                            + "' (objective flip gate)"));
        }

        addV96(operations, facts);
        return new PolicyResult("DEPLOY_SITING_DIRECT_POLICY", operations);
    }

    /** Preserves the destination order: V89-CS, formation, V136-CS, then V193-CS. */
    public static PolicyResult evaluateDestination(Facts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();

        if (facts.evazanWithoutArmedFriend()) {
            operations.add(addSiting(facts.actionId(), "V89-CS", TraceOutputKind.VETO,
                    V89_BAD_SITE_SCORE,
                    "V89-CS DR. EVAZAN: '" + facts.cardTitle() + "' → '"
                            + facts.siteTitle()
                            + "' with no armed friend — block (will get sniped)"));
        }

        addFormation(operations, facts);
        addV136(operations, facts.actionId(), "V136-CS", facts.siteTitle(),
                facts.v136Score(), true);

        if (facts.v193Eligible() && facts.v193FormationSupported()) {
            operations.add(addObjectiveSiting(facts.actionId(), "V193-CS", TraceOutputKind.BANDED,
                    "V193 (CS) FLIP-GATE CONTROL: steer one ability body to '"
                            + facts.siteTitle() + "' to enable '"
                            + facts.v193GateCardTitle() + "' (objective flip gate)"));
        }

        // V96 ADJUSTED 2026-08-08 (passivity fix, m01683): wire CONCENTRATE into the
        // destination route — the route that actually picks sites carried no V96 at all
        // (its facts were hardcoded off at the CardSelection adapters; now populated).
        addV96(operations, facts);

        return new PolicyResult("DEPLOY_SITING_DESTINATION_POLICY", operations);
    }

    public static PolicyResult evaluateShipReferenceGround(
            ShipReferenceGroundFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);
        if (!facts.referencedShipName().isBlank()) {
            operations.add(addSiting(facts.actionId(), "V29-ship-ground",
                    TraceOutputKind.BANDED, -200.0f,
                    "V29 SHIP CHARACTER ON GROUND: Game text mentions "
                            + facts.referencedShipName()
                            + " — should deploy to space!"));
        }
        return new PolicyResult("DEPLOY_SHIP_REFERENCE_GROUND_POLICY", operations);
    }

    public static PolicyResult evaluateStarshipDestination(
            StarshipDestinationFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);
        switch (facts.state()) {
            case SITE_BLOCKED -> operations.add(addSiting(
                    facts.actionId(), "V190", TraceOutputKind.VETO, -1500.0f,
                    "⚠️ STARSHIP TO SITE = 0 POWER! (V190: ships deploy to systems)"));
            case SPACE_FALLBACK -> operations.add(addSiting(
                    facts.actionId(), "starship-space-fallback",
                    TraceOutputKind.BANDED, 20.0f,
                    "Starship to space system"));
            case SPACE_UNCONTESTED -> operations.add(addSiting(
                    facts.actionId(), "starship-space-uncontested",
                    TraceOutputKind.BANDED, 30.0f,
                    "Uncontested space system"));
            case SPACE_DISADVANTAGE -> operations.add(addSiting(
                    facts.actionId(), "starship-space-power",
                    TraceOutputKind.BANDED, -80.0f,
                    String.format("⚠️ SPACE POWER DISADVANTAGE: %.0f vs %.0f after deploy",
                            facts.projectedPower(), facts.opponentPower())));
            case SPACE_ADVANTAGE -> operations.add(addSiting(
                    facts.actionId(), "starship-space-power",
                    TraceOutputKind.BANDED, 30.0f,
                    String.format("Good space position: %.0f vs %.0f after deploy",
                            facts.projectedPower(), facts.opponentPower())));
            case SPACE_CLOSE -> operations.add(addSiting(
                    facts.actionId(), "starship-space-power",
                    TraceOutputKind.BANDED, 10.0f,
                    String.format("Close space fight: %.0f vs %.0f after deploy",
                            facts.projectedPower(), facts.opponentPower())));
            default -> {
            }
        }
        return new PolicyResult("DEPLOY_STARSHIP_DESTINATION_POLICY", operations);
    }

    public static PolicyResult evaluateVehicleDestination(
            VehicleDestinationFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);
        switch (facts.state()) {
            case SPACE_INVALID -> operations.add(addSiting(
                    facts.actionId(), "vehicle-space",
                    TraceOutputKind.BANDED, -150.0f,
                    "VEHICLE TO SPACE - invalid!"));
            case INTERIOR_INVALID -> operations.add(addSiting(
                    facts.actionId(), "vehicle-interior",
                    TraceOutputKind.BANDED, -150.0f,
                    "VEHICLE TO INTERIOR-ONLY - can't deploy!"));
            case EXTERIOR_VALID -> operations.add(addSiting(
                    facts.actionId(), "vehicle-exterior",
                    TraceOutputKind.BANDED, 10.0f,
                    "Vehicle to exterior ground - good"));
            default -> {
            }
        }
        return new PolicyResult("DEPLOY_VEHICLE_DESTINATION_POLICY", operations);
    }

    public static PolicyResult evaluatePermanentWeaponDestination(
            PermanentWeaponDestinationFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);
        if (facts.state() == PermanentWeaponDestinationState.SPACE) {
            operations.add(addSiting(facts.actionId(), "V24.14B-weapon-space",
                    TraceOutputKind.BANDED, -300.0f,
                    "V24.14B WEAPON CHAR TO SPACE: Permanent weapon can't fire at system locations — useless in space!"));
        } else if (facts.state() == PermanentWeaponDestinationState.GROUND) {
            operations.add(addSiting(facts.actionId(), "V24.14B-weapon-ground",
                    TraceOutputKind.BANDED, 100.0f,
                    "V24.14B WEAPON CHAR ON GROUND: Strong battle presence — weapon fires here!"));
        }
        return new PolicyResult("DEPLOY_PERMANENT_WEAPON_DESTINATION_POLICY", operations);
    }

    public static PolicyResult evaluateEmptyDockingBay(
            EmptyDockingBayFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);
        if (facts.ownEmptyDockingBay()) {
            operations.add(addSiting(facts.actionId(), "V29.7-empty-bay",
                    TraceOutputKind.BANDED, 80.0f,
                    "V29.7 EMPTY BAY: Deploy character to protect our docking bay from opponent!"));
        }
        return new PolicyResult("DEPLOY_EMPTY_DOCKING_BAY_POLICY", operations);
    }

    public static PolicyResult evaluateBattlegroundLocation(
            BattlegroundLocationFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);
        if (facts.battleground()) {
            operations.add(addSiting(facts.actionId(), "V29.6-battleground",
                    TraceOutputKind.BANDED, 50.0f,
                    "V29.6 Battleground location — force drains!"));
        }
        return new PolicyResult("DEPLOY_BATTLEGROUND_LOCATION_POLICY", operations);
    }

    public static PolicyResult evaluateOpponentForceIcons(
            OpponentForceIconsFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);
        if (facts.opponentIcons() > 0) {
            operations.add(addSiting(facts.actionId(), "V23-force-icons",
                    TraceOutputKind.BANDED, facts.opponentIcons() * 30.0f,
                    "V23 FORCE DRAIN: " + facts.opponentIcons()
                            + " opponent force icon(s) — better drain target!"));
        }
        return new PolicyResult("DEPLOY_OPPONENT_FORCE_ICONS_POLICY", operations);
    }

    public static PolicyResult evaluateMapuzoDestination(
            MapuzoDestinationFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);
        if (!facts.jediSurvivor()) {
            if (facts.opponentPower() > 0.0f) {
                operations.add(addSiting(facts.actionId(), "V64-mapuzo-defense",
                        TraceOutputKind.BANDED, 30.0f,
                        "V64 MAPUZO DEFENSE: Opponent at "
                                + facts.destinationTitle() + " (power "
                                + (int) facts.opponentPower()
                                + ") — non-Jedi defender OK here"));
            } else {
                operations.add(addSiting(facts.actionId(), "V64-mapuzo-trap",
                        TraceOutputKind.VETO, -1500.0f,
                        "V64 MAPUZO TRAP: Non-Jedi character at "
                                + facts.destinationTitle()
                                + " will be STUCK — only Jedi Survivors transit off Mapuzo!"));
            }
        }
        return new PolicyResult("DEPLOY_MAPUZO_DESTINATION_POLICY", operations);
    }

    private static void addFormation(List<PolicyOperation> operations,
                                     Facts facts) {
        switch (facts.formationState()) {
            case HARD_BLOCK -> operations.add(PolicyOperation.hardVeto(
                    facts.actionId(), TraceRuleId.of("FS-L3-solo-deploy-hard"),
                    TraceDomainId.SOLO_FORMATION, TraceOutputKind.VETO,
                    facts.formationReason()));
            case DEFER_UNSUPPORTED_SOLO -> operations.add(PolicyOperation.defer(
                    facts.actionId(), TraceRuleId.of("V201-deploy-siting"),
                    TraceDomainId.SOLO_FORMATION, TraceOutputKind.VETO,
                    FORMATION_DEFER_SCORE, facts.formationReason()));
            case UNKNOWN -> operations.add(PolicyOperation.add(
                    facts.actionId(), TraceRuleId.of("V201-deploy-siting-unknown"),
                    TraceDomainId.SOLO_FORMATION, TraceOutputKind.BANDED,
                    0.0f,
                    "V201 formation assessment unknown: " + facts.formationReason()));
            default -> {
            }
        }
    }

    public record ShipReferenceGroundFacts(String actionId,
                                           String referencedShipName) {
        public ShipReferenceGroundFacts {
            Objects.requireNonNull(actionId, "actionId");
            referencedShipName = referencedShipName == null ? "" : referencedShipName;
        }
    }

    public record StarshipDestinationFacts(
            String actionId, StarshipDestinationState state,
            float projectedPower, float opponentPower) {
        public StarshipDestinationFacts {
            Objects.requireNonNull(actionId, "actionId");
            state = state == null ? StarshipDestinationState.NONE : state;
        }
    }

    public record VehicleDestinationFacts(String actionId,
                                          VehicleDestinationState state) {
        public VehicleDestinationFacts {
            Objects.requireNonNull(actionId, "actionId");
            state = state == null ? VehicleDestinationState.NONE : state;
        }
    }

    public record PermanentWeaponDestinationFacts(
            String actionId, PermanentWeaponDestinationState state) {
        public PermanentWeaponDestinationFacts {
            Objects.requireNonNull(actionId, "actionId");
            state = state == null ? PermanentWeaponDestinationState.NONE : state;
        }
    }

    public record EmptyDockingBayFacts(String actionId,
                                       boolean ownEmptyDockingBay) {
        public EmptyDockingBayFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record BattlegroundLocationFacts(String actionId,
                                            boolean battleground) {
        public BattlegroundLocationFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record OpponentForceIconsFacts(String actionId,
                                          int opponentIcons) {
        public OpponentForceIconsFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record MapuzoDestinationFacts(
            String actionId, String destinationTitle,
            boolean jediSurvivor, float opponentPower) {
        public MapuzoDestinationFacts {
            Objects.requireNonNull(actionId, "actionId");
            destinationTitle = destinationTitle == null
                    ? "null" : destinationTitle;
        }
    }

    private static void addV136(List<PolicyOperation> operations, String actionId,
                                String rule, String siteTitle, float score,
                                boolean destinationRoute) {
        if (score == 0.0f) {
            return;
        }
        String route = destinationRoute ? " (CS)" : "";
        operations.add(addSiting(actionId, rule, TraceOutputKind.BANDED, score,
                "V136 unified deploy-site score" + route + " → "
                        + siteTitle + ": " + score));
    }

    private static void addV96(List<PolicyOperation> operations, Facts facts) {
        if (!facts.v96Applicable() || facts.opponentPower() <= 0.0f) {
            return;
        }
        // V96 ADJUSTED 2026-08-08 (passivity fix, m01683): project the DEPLOYING card
        // into the diff — ask "would I overwhelm AFTER deploying", not the pre-deploy
        // standoff (evaluateDirect callers pass deployingPower 0 → old behavior).
        // float difference = facts.friendlyPower() - facts.opponentPower();
        float difference = facts.friendlyPower() + facts.deployingPower()
                - facts.opponentPower();
        if (difference >= -10.0f && difference <= 10.0f) {
            operations.add(addSiting(facts.actionId(), "V96", TraceOutputKind.BANDED,
                    500.0f,
                    String.format("V96 CONCENTRATE: %s contested (us %.0f vs them %.0f) — pile on for overflow battle damage!",
                            facts.siteTitle(),
                            facts.friendlyPower() + facts.deployingPower(),
                            facts.opponentPower())));
        } else if (difference > 10.0f) {
            operations.add(addSiting(facts.actionId(), "V96", TraceOutputKind.BANDED,
                    100.0f,
                    String.format("V96 CONCENTRATE: %s contested, already winning by %.0f — finish them",
                            facts.siteTitle(), difference)));
        }
    }

    private static PolicyOperation addSiting(String actionId, String rule,
                                             TraceOutputKind outputKind,
                                             float delta, String reason) {
        return PolicyOperation.add(actionId, TraceRuleId.of(rule),
                TraceDomainId.DEPLOY_SITING, outputKind, delta, reason);
    }

    private static PolicyOperation addObjectiveSiting(
            String actionId, String rule, TraceOutputKind outputKind,
            String reason) {
        return PolicyOperation.add(actionId, TraceRuleId.of(rule),
                TraceDomainId.OBJECTIVE_INTENT, outputKind,
                ObjectivePreferencePolicy.SCORE, reason);
    }
}
