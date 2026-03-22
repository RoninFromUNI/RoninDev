package com.ronin.therapeuticdev.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.ronin.therapeuticdev.detection.FlowDetectionResult;
import com.ronin.therapeuticdev.detection.FlowState;
import com.ronin.therapeuticdev.metrics.FlowMetrics;
import com.ronin.therapeuticdev.settings.TherapeuticDevSettings;
import com.ronin.therapeuticdev.ui.BreakNotificationDialog;

import java.util.concurrent.TimeUnit;

/**
 * manages break notifications based on flow detection results.
 *
 * i register as a FlowDetectionListener on SnapshotScheduler, so every 2 seconds
 * i get the latest detection result and decide whether the developer needs a break.
 *
 * three trigger conditions:
 *   1. flow duration exceeded the configured threshold (default 60 min)
 *   2. flow state just ended (score dropped out of FLOW) after a sustained session
 *   3. high distraction detected (PROCRASTINATING + excessive context switches)
 *
 * condition 2 is the interesting one from a ux perspective — suggesting a break
 * right when flow ends naturally feels less intrusive than interrupting mid-flow.
 * the developer already lost their rhythm, so the break suggestion lands at a
 * moment where they're least likely to resent the interruption.
 *
 * snooze and minimum-interval guards prevent notification spam. the 5-minute
 * cooldown between suggestions is a hard floor regardless of what triggers fire.
 */
@Service(Service.Level.APP)
public final class BreakManager implements SnapshotScheduler.FlowDetectionListener {

    private static final Logger LOG = Logger.getInstance(BreakManager.class);

    // flow tracking state — when did flow start, when did i last suggest a break, etc.
    private long flowStartTimeMs = 0;
    private long lastBreakSuggestionMs = 0;
    private long snoozedUntilMs = 0;
    private FlowState previousState = FlowState.NEUTRAL;
    private boolean inActiveFlow = false;

    public BreakManager() {
        registerListener();
    }

    private void registerListener() {
        SnapshotScheduler scheduler = ApplicationManager.getApplication()
                .getService(SnapshotScheduler.class);

        if (scheduler != null) {
            scheduler.addListener(this);
        }
    }

    @Override
    public void onFlowDetected(FlowDetectionResult result, FlowMetrics metrics) {
        TherapeuticDevSettings settings = TherapeuticDevSettings.getInstance();

        if (!settings.autoBreakSuggestions) {
            return;
        }

        FlowState currentState = result.getState();
        long now = System.currentTimeMillis();

        // track when flow starts and ends so i can measure duration
        if (currentState == FlowState.FLOW && !inActiveFlow) {
            flowStartTimeMs = now;
            inActiveFlow = true;
            LOG.info("Flow state started");
        } else if (currentState != FlowState.FLOW && inActiveFlow) {
            inActiveFlow = false;
            LOG.info("Flow state ended");
        }

        if (shouldSuggestBreak(result, metrics, settings, now)) {
            suggestBreak(metrics, determineReason(result, metrics, settings, now));
            lastBreakSuggestionMs = now;
        }

        previousState = currentState;
    }

    /**
     * evaluates all three trigger conditions against the current state.
     * returns false early if snoozed or within the 5-minute cooldown.
     */
    private boolean shouldSuggestBreak(FlowDetectionResult result, FlowMetrics metrics,
                                       TherapeuticDevSettings settings, long now) {
        if (now < snoozedUntilMs) {
            return false;
        }

        // hard 5-minute minimum between any two notifications
        long minIntervalMs = TimeUnit.MINUTES.toMillis(5);
        if (now - lastBreakSuggestionMs < minIntervalMs) {
            return false;
        }

        // trigger 1: flow duration exceeded threshold
        if (inActiveFlow) {
            long flowDurationMs = now - flowStartTimeMs;
            long thresholdMs = TimeUnit.MINUTES.toMillis(settings.breakIntervalMinutes);
            if (flowDurationMs >= thresholdMs) {
                return true;
            }
        }

        // trigger 2: just exited flow after a sustained session
        if (previousState == FlowState.FLOW && result.getState() != FlowState.FLOW) {
            long minFlowMs = TimeUnit.MINUTES.toMillis(settings.minFlowDurationForBreak);
            if (inActiveFlow && (now - flowStartTimeMs) >= minFlowMs) {
                return true;
            }
        }

        // trigger 3: procrastinating with heavy context switching
        if (result.getState() == FlowState.PROCRASTINATING &&
            metrics.getFileChangesLast5Min() >= settings.contextSwitchWarningThreshold) {
            return true;
        }

        return false;
    }

    /**
     * figures out which trigger condition fired so i can show the developer
     * a meaningful reason in the break dialog rather than just "take a break".
     */
    private String determineReason(FlowDetectionResult result, FlowMetrics metrics,
                                   TherapeuticDevSettings settings, long now) {
        if (inActiveFlow) {
            long flowDurationMs = now - flowStartTimeMs;
            long thresholdMs = TimeUnit.MINUTES.toMillis(settings.breakIntervalMinutes);
            if (flowDurationMs >= thresholdMs) {
                return String.format("exceeded %d min threshold", settings.breakIntervalMinutes);
            }
        }

        if (previousState == FlowState.FLOW && result.getState() != FlowState.FLOW) {
            return "flow state ended naturally";
        }

        if (result.getState() == FlowState.PROCRASTINATING) {
            return "high distraction detected";
        }

        return "scheduled break time";
    }

    /**
     * shows the break notification dialog on the EDT.
     * uses the first open project as the dialog parent since BreakManager
     * is an application-level service without a project reference.
     */
    private void suggestBreak(FlowMetrics metrics, String reason) {
        TherapeuticDevSettings settings = TherapeuticDevSettings.getInstance();
        long flowDurationMs = inActiveFlow ?
                System.currentTimeMillis() - flowStartTimeMs :
                metrics.getSessionDurationMs();

        LOG.info("Suggesting break: " + reason);

        Project project = null;
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        if (projects.length > 0) {
            project = projects[0];
        }

        if (settings.useModalNotifications) {
            BreakNotificationDialog.show(project, flowDurationMs, reason,
                    new BreakNotificationDialog.BreakCallback() {
                        @Override
                        public void onTakeBreak() {
                            handleTakeBreak();
                        }

                        @Override
                        public void onSnooze(int minutes) {
                            handleSnooze(minutes);
                        }

                        @Override
                        public void onDismiss() {
                            handleDismiss();
                        }
                    });
        }

        // TODO: balloon notification path — not implemented yet
    }

    private void handleTakeBreak() {
        LOG.info("User took break");
        flowStartTimeMs = 0;
        inActiveFlow = false;
    }

    private void handleSnooze(int minutes) {
        LOG.info("User snoozed for " + minutes + " minutes");
        snoozedUntilMs = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(minutes);
    }

    private void handleDismiss() {
        LOG.info("User dismissed break suggestion");
    }

    /**
     * manual trigger — callable from the status bar widget or a keyboard shortcut.
     * bypasses all the automatic trigger conditions and just shows the dialog.
     */
    public void triggerManualBreak() {
        MetricCollector collector = ApplicationManager.getApplication()
                .getService(MetricCollector.class);

        if (collector != null) {
            FlowMetrics metrics = collector.snapshot();
            suggestBreak(metrics, "manual trigger");
        }
    }

    public long getCurrentFlowDurationMs() {
        if (!inActiveFlow) return 0;
        return System.currentTimeMillis() - flowStartTimeMs;
    }

    public boolean isInFlow() {
        return inActiveFlow;
    }
}
