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
 * Manages break notifications and timing.
 * 
 * <p>Listens to flow detection results and triggers break suggestions when:
 * - Flow duration exceeds configured threshold
 * - User exits flow state (score drops)
 * - High stress/distraction detected
 *
 * <p>Respects user preferences for notification style and snooze behavior.
 */
@Service(Service.Level.APP)
public final class BreakManager implements SnapshotScheduler.FlowDetectionListener {

    private static final Logger LOG = Logger.getInstance(BreakManager.class);

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
        
        // Track flow start time
        if (currentState == FlowState.FLOW && !inActiveFlow) {
            flowStartTimeMs = now;
            inActiveFlow = true;
            LOG.info("Flow state started");
        } else if (currentState != FlowState.FLOW && inActiveFlow) {
            inActiveFlow = false;
            LOG.info("Flow state ended");
        }
        
        // Check if we should suggest a break
        if (shouldSuggestBreak(result, metrics, settings, now)) {
            suggestBreak(metrics, determineReason(result, metrics, settings, now));
            lastBreakSuggestionMs = now;
        }
        
        previousState = currentState;
    }

    private boolean shouldSuggestBreak(FlowDetectionResult result, FlowMetrics metrics,
                                       TherapeuticDevSettings settings, long now) {
        // Check snooze
        if (now < snoozedUntilMs) {
            return false;
        }
        
        // Don't spam notifications
        long minIntervalMs = TimeUnit.MINUTES.toMillis(5);
        if (now - lastBreakSuggestionMs < minIntervalMs) {
            return false;
        }
        
        // Check flow duration threshold
        if (inActiveFlow) {
            long flowDurationMs = now - flowStartTimeMs;
            long thresholdMs = TimeUnit.MINUTES.toMillis(settings.breakIntervalMinutes);
            
            if (flowDurationMs >= thresholdMs) {
                return true;
            }
        }
        
        // Check for flow exit (score dropped significantly)
        if (previousState == FlowState.FLOW && result.getState() != FlowState.FLOW) {
            // Only suggest if they were in flow for a while
            long minFlowMs = TimeUnit.MINUTES.toMillis(settings.minFlowDurationForBreak);
            if (inActiveFlow && (now - flowStartTimeMs) >= minFlowMs) {
                return true;
            }
        }
        
        // Check for high stress/distraction
        if (result.getState() == FlowState.PROCRASTINATING && 
            metrics.getFileChangesLast5Min() >= settings.contextSwitchWarningThreshold) {
            return true;
        }
        
        return false;
    }

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

    private void suggestBreak(FlowMetrics metrics, String reason) {
        TherapeuticDevSettings settings = TherapeuticDevSettings.getInstance();
        long flowDurationMs = inActiveFlow ? 
                System.currentTimeMillis() - flowStartTimeMs : 
                metrics.getSessionDuration();
        
        LOG.info("Suggesting break: " + reason);
        
        // Get active project
        Project project = null;
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        if (projects.length > 0) {
            project = projects[0];
        }
        
        if (settings.useModalNotifications) {
            final Project finalProject = project;
            BreakNotificationDialog.show(finalProject, flowDurationMs, reason,
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
        
        // TODO: Implement balloon notification option
        // if (settings.useBalloonNotifications) { ... }
    }

    private void handleTakeBreak() {
        LOG.info("User took break");
        // Reset flow tracking
        flowStartTimeMs = 0;
        inActiveFlow = false;
        
        // Could trigger additional actions here:
        // - Pause metric collection
        // - Log break event
        // - Show break timer
    }

    private void handleSnooze(int minutes) {
        LOG.info("User snoozed for " + minutes + " minutes");
        snoozedUntilMs = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(minutes);
    }

    private void handleDismiss() {
        LOG.info("User dismissed break suggestion");
        // Just log, no special action
    }

    /**
     * Manually triggers a break suggestion.
     * Can be called from status bar widget or keyboard shortcut.
     */
    public void triggerManualBreak() {
        MetricCollector collector = ApplicationManager.getApplication()
                .getService(MetricCollector.class);
        
        if (collector != null) {
            FlowMetrics metrics = collector.snapshot();
            suggestBreak(metrics, "manual trigger");
        }
    }

    /**
     * Returns current flow duration if in flow, 0 otherwise.
     */
    public long getCurrentFlowDurationMs() {
        if (!inActiveFlow) return 0;
        return System.currentTimeMillis() - flowStartTimeMs;
    }

    /**
     * Returns true if currently in active flow state.
     */
    public boolean isInFlow() {
        return inActiveFlow;
    }
}
