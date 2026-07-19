package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Shared owner of DEPLOY-1 sequencing, ordering, and scripted urgency scores. */
public final class DeploySequencingPolicy {
    public enum AdapterStep {
        FALL_THROUGH,
        CONTINUE_ACTION
    }

    public record Evaluation(PolicyResult result, AdapterStep adapterStep,
                             Float scoreOverride) {
    }

    private DeploySequencingPolicy() {
    }

    public static Evaluation phaseEnvelope(String actionId, int handSize,
                                           int availableForce, int forcePile,
                                           DeploySequencingFacts.PowerGap endangered,
                                           DeploySequencingFacts.PowerGap winnableBattle) {
        List<PolicyOperation> operations = new ArrayList<>();

        float urgencyBonus = 0.0f;
        if (handSize >= 12) {
            urgencyBonus = 200.0f + (handSize - 12) * 50.0f;
        } else if (handSize >= 9) {
            urgencyBonus = 100.0f + (handSize - 9) * 30.0f;
        } else if (handSize >= 5) {
            urgencyBonus = 80.0f;
        } else if (handSize >= 1) {
            urgencyBonus = 50.0f;
        }
        if (availableForce >= 10 && handSize >= 8) {
            urgencyBonus += 100.0f;
        }
        if (availableForce >= 6 && handSize >= 1 && handSize < 8) {
            urgencyBonus += 80.0f;
        }
        if (urgencyBonus > 0.0f) {
            operations.add(add(actionId, "V38.4", TraceOutputKind.ORDERING,
                    urgencyBonus,
                    String.format("V38.4 DEPLOY URGENCY: hand=%d, force=%d \u2014 get cards on table! (+%.0f)",
                            handSize, availableForce, urgencyBonus)));
        }

        if (endangered != null) {
            operations.add(add(actionId, "V169-deploy-umbrella", TraceOutputKind.ORDERING,
                    500.0f,
                    String.format("V169 PROTECT URGENT: our characters at %s outpowered (%.0f vs %.0f) \u2014 deploy buddies NOW",
                            endangered.locationTitle(), endangered.ourPower(), endangered.theirPower())));
        }

        if (forcePile <= 2 && winnableBattle != null) {
            operations.add(add(actionId, "V176", TraceOutputKind.VETO,
                    -800.0f,
                    String.format("V176 SAVE BATTLE FORCE: winnable battle waiting at %s (%.0f vs %.0f) and only %d force left \u2014 stop deploying, keep the initiation fee",
                            winnableBattle.locationTitle(), winnableBattle.ourPower(),
                            winnableBattle.theirPower(), forcePile)));
        }

        return evaluation("DEPLOY_PHASE_ENVELOPE_POLICY", operations,
                AdapterStep.FALL_THROUGH, null);
    }

    public static Evaluation locationFromHand(String actionId, boolean actualLocationDeploy,
                                              int lifeForce, String cardTitle) {
        List<PolicyOperation> operations = new ArrayList<>();
        if (!actualLocationDeploy) {
            return evaluation("DEPLOY_LOCATION_ORDER_POLICY", operations,
                    AdapterStep.FALL_THROUGH, null);
        }
        String title = cardTitle == null ? "location" : cardTitle;
        if (lifeForce <= 10) {
            operations.add(add(actionId, "V162", TraceOutputKind.BANDED, -200.0f,
                    "V162 HOLD LOCATION: life force " + lifeForce
                            + " <= 10 \u2014 keep '" + title
                            + "' in hand as force-loss fodder"));
        } else {
            operations.add(add(actionId, "V162", TraceOutputKind.ORDERING, 500.0f,
                    "V162 LOCATION FIRST: deploy locations before anything else (life force "
                            + lifeForce + " > 10) \u2014 foundation for drains/objective +500"));
            operations.add(add(actionId, "V67ai", TraceOutputKind.ORDERING, 1400.0f,
                    "V67ai LOCATION DEPLOY ORDER [Tier 4 HAND]: deploy location from hand \u2014 force generation foundation!"));
        }
        return evaluation("DEPLOY_LOCATION_ORDER_POLICY", operations,
                AdapterStep.FALL_THROUGH, null);
    }

    public static Evaluation tailScripts(TailFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();

        if (facts.deploymentsMade() >= 1) {
            float momentum = facts.deploymentsMade() >= 3 ? 200.0f
                    : facts.deploymentsMade() >= 2 ? 150.0f : 100.0f;
            operations.add(add(facts.actionId(), "V52-momentum", TraceOutputKind.ORDERING,
                    momentum,
                    String.format("V52 MOMENTUM: Already deployed %d cards this turn \u2014 keep deploying! (+%.0f)",
                            facts.deploymentsMade(), momentum)));
        }

        addTdigwattScript(operations, facts);
        addSkywalkerScript(operations, facts);

        if (facts.character() && facts.ability() >= 6.0f) {
            float bonus = facts.turnNumber() <= 3 ? 500.0f
                    : facts.turnNumber() <= 6 ? 350.0f : 200.0f;
            operations.add(add(facts.actionId(), "V55", TraceOutputKind.ORDERING,
                    bonus,
                    "V55 HIGH-ABILITY: " + facts.cardTitle()
                            + " (ability " + (int) facts.ability()
                            + ") in hand \u2014 deploy, don't hoard!"));
        }

        if (facts.hiddenPath() && facts.turnNumber() <= 2) {
            boolean jediCharacter = facts.character() && (facts.ability() >= 6.0f
                    || containsAny(facts.cardTitleLower(), "obi-wan", "quinlan", "kelleran",
                    "cal kestis", "ezra", "kanan", "ahsoka tano", "cere", "luke", "yoda"));
            boolean jediDeploy = facts.actionLower().contains("jedi survivor")
                    || facts.actionLower().contains("fallen order");
            if (jediCharacter) {
                operations.add(add(facts.actionId(), "V52b", TraceOutputKind.ORDERING, 800.0f,
                        "V52b HIDDEN PATH: Jedi character \u2014 deploy FIRST!"));
            } else if (jediDeploy) {
                operations.add(add(facts.actionId(), "V52b", TraceOutputKind.ORDERING, 800.0f,
                        "V52b HIDDEN PATH: Fallen Order Jedi deploy \u2014 deploy FIRST!"));
            } else if (facts.cardTitleLower().contains("lightsaber")
                    || facts.cardTitleLower().contains("shoto")) {
                operations.add(add(facts.actionId(), "V52b", TraceOutputKind.ORDERING, 700.0f,
                        "V52b HIDDEN PATH: Lightsaber \u2014 arm the Jedi!"));
            } else if (facts.cardTitleLower().contains("holocron")
                    || facts.actionLower().contains("holocron")) {
                operations.add(add(facts.actionId(), "V52b", TraceOutputKind.ORDERING, 600.0f,
                        "V52b HIDDEN PATH: Jedi Holocron!"));
            }
        }

        return evaluation("DEPLOY_TAIL_SCRIPT_POLICY", operations,
                AdapterStep.FALL_THROUGH, null);
    }

    public static Evaluation woklingEarlySearch(String actionId, boolean matchingSearch,
                                                boolean woklingSource, int turnNumber) {
        List<PolicyOperation> operations = new ArrayList<>();
        if (matchingSearch && woklingSource && turnNumber <= 3) {
            operations.add(add(actionId, "V53c", TraceOutputKind.VETO, -9999.0f,
                    "V53c BLOCK WOKLING: Turns 1-3 \u2014 save force for deploys, don't search!"));
            return evaluation("DEPLOY_WOKLING_POLICY", operations,
                    AdapterStep.CONTINUE_ACTION, -9999.0f);
        }
        return evaluation("DEPLOY_WOKLING_POLICY", operations,
                AdapterStep.FALL_THROUGH, null);
    }

    public static Evaluation locationsFirstNonDeploy(String actionId,
                                                     boolean locationInHand,
                                                     boolean exempt) {
        List<PolicyOperation> operations = new ArrayList<>();
        if (locationInHand && !exempt) {
            operations.add(add(actionId, "V24.4", TraceOutputKind.ORDERING, -800.0f,
                    "V24.4 LOCATIONS FIRST: Deploy locations in hand before activating effects!"));
        }
        return evaluation("DEPLOY_LOCATION_FIRST_ACTION_POLICY", operations,
                AdapterStep.FALL_THROUGH, null);
    }

    public record TailFacts(String actionId, int turnNumber, int deploymentsMade,
                            boolean tdigwattPreFlip, boolean skywalkerSaga,
                            boolean hiddenPath, String cardTitle,
                            String cardTitleLower, String actionLower,
                            boolean character, float ability) {
        public TailFacts {
            Objects.requireNonNull(actionId, "actionId");
            cardTitle = cardTitle == null ? "" : cardTitle;
            cardTitleLower = cardTitleLower == null ? "" : cardTitleLower;
            actionLower = actionLower == null ? "" : actionLower;
        }
    }

    private static void addTdigwattScript(List<PolicyOperation> operations,
                                          TailFacts facts) {
        if (!facts.tdigwattPreFlip() || facts.turnNumber() > 1) {
            return;
        }
        String title = facts.cardTitleLower();
        if (title.contains("bespin") && facts.actionLower().contains("system")) {
            operations.add(add(facts.actionId(), "V52-TDIGWATT-T1", TraceOutputKind.ORDERING,
                    1500.0f, "V52 TDIGWATT T1: Bespin system \u2014 FOUNDATION!"));
        } else if (title.contains("cloud city")
                || facts.actionLower().contains("i'm sorry")
                || facts.actionLower().contains("i am sorry")) {
            operations.add(add(facts.actionId(), "V52-TDIGWATT-T1", TraceOutputKind.ORDERING,
                    1200.0f, "V52 TDIGWATT T1: Cloud City site via I'm Sorry!"));
        } else if (title.contains("lando") && title.contains("broker")) {
            operations.add(add(facts.actionId(), "V52-TDIGWATT-T1", TraceOutputKind.ORDERING,
                    1000.0f, "V52 TDIGWATT T1: Lando as Broker \u2014 key engine piece!"));
        } else if (title.contains("executor") || title.contains("flagship")) {
            operations.add(add(facts.actionId(), "V52-TDIGWATT-T1", TraceOutputKind.ORDERING,
                    900.0f, "V52 TDIGWATT T1: Executor/Flagship \u2014 Bespin control!"));
        } else if (title.contains("chiraneau")) {
            operations.add(add(facts.actionId(), "V52-TDIGWATT-T1", TraceOutputKind.ORDERING,
                    850.0f, "V52 TDIGWATT T1: Chiraneau \u2014 pilot for Executor!"));
        }
    }

    private static void addSkywalkerScript(List<PolicyOperation> operations,
                                           TailFacts facts) {
        if (!facts.skywalkerSaga() || facts.turnNumber() > 3) {
            return;
        }
        float multiplier = facts.turnNumber() == 1 ? 1.0f
                : facts.turnNumber() == 2 ? 0.85f : 0.7f;
        String title = facts.cardTitleLower();
        float score = 0.0f;
        String reason = null;
        if (title.contains("tatooine: cantina") || title.equals("cantina")) {
            score = 1500.0f;
            reason = "Tatooine: Cantina \u2014 drain engine!";
        } else if (title.contains("mos eisley")) {
            score = 1500.0f;
            reason = "Tatooine: Mos Eisley \u2014 Cantina shuttle!";
        } else if (title.contains("lars") && title.contains("moisture")) {
            score = 1500.0f;
            reason = "Lars' Moisture Farm \u2014 Tatooine site!";
        } else if (title.startsWith("tatooine:") && !title.contains("jabba")) {
            score = 1300.0f;
            reason = "Tatooine battleground site!";
        } else if (title.equals("tatooine") && facts.actionLower().contains("system")) {
            score = 900.0f;
            reason = "Tatooine system \u2014 ship presence!";
        } else if (title.contains("young skywalker")) {
            score = 1200.0f;
            reason = "Young Skywalker \u2014 Luke persona (I have it)!";
        } else if (title.contains("luke") && facts.character()) {
            score = 1100.0f;
            reason = "Luke persona \u2014 deploy for drain power!";
        } else if (title.contains("luke's lightsaber")) {
            score = 1100.0f;
            reason = "Luke's Lightsaber \u2014 arm Luke NOW!";
        } else if ((title.contains("obi-wan") || title.contains("yoda"))
                && facts.character()) {
            score = 800.0f;
            reason = "Jedi buddy for Luke!";
        }
        if (reason != null) {
            operations.add(add(facts.actionId(), "V54", TraceOutputKind.ORDERING,
                    score * multiplier,
                    "V54 LMFBM T" + facts.turnNumber() + ": " + reason));
        }
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static PolicyOperation add(String actionId, String rule,
                                       TraceOutputKind kind, float delta,
                                       String reason) {
        return PolicyOperation.add(actionId, TraceRuleId.of(rule),
                TraceDomainId.DEPLOY_SEQUENCING, kind, delta, reason);
    }

    private static Evaluation evaluation(String producer,
                                         List<PolicyOperation> operations,
                                         AdapterStep step,
                                         Float scoreOverride) {
        return new Evaluation(new PolicyResult(producer, operations), step, scoreOverride);
    }
}
