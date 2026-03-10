package com.ronin.therapeuticdev.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.ronin.therapeuticdev.detection.FlowDetectionResult;
import com.ronin.therapeuticdev.settings.TherapeuticDevSettings;
import com.ronin.therapeuticdev.ui.EsmProbeDialog;

import java.time.Instant;
import java.time.Duration;

/**
 * Manages delivery of Experience Sampling Method (ESM) probes.
 *
 * Called by SnapshotScheduler on each persist cycle (every 60 s).
 * Delivers a non-blocking flow state self-report dialog every
 * {@code probeIntervalMinutes} of active development (default 30 min).
 *
 * A probe is suppressed if:
 *  - Auto break suggestions are disabled in settings
 *  - The developer is in a DEEP_FLOW or FLOW state (shouldAvoidInterruption)
 *  - The minimum interval since the last probe has not elapsed
 */
@Service(Service.Level.APP)
public final class EsmProbeService {

    private static final int DEFAULT_PROBE_INTERVAL_MINUTES = 30;
    private static final int SUPPRESSION_MINUTES = 5; // minimum gap when in flow

    private Instant lastProbeTime = Instant.EPOCH;

    /**
     * Evaluates whether a probe should fire and, if so, delivers it on the EDT.
     * Safe to call from a background thread.
     */
    public void checkAndDeliver(FlowDetectionResult result) {
        TherapeuticDevSettings settings = ApplicationManager.getApplication()
                .getService(TherapeuticDevSettings.class);
        if (settings == null || !settings.autoBreakSuggestions) return;

        int intervalMinutes = settings.breakIntervalMinutes > 0
                ? settings.breakIntervalMinutes
                : DEFAULT_PROBE_INTERVAL_MINUTES;

        Duration sinceLast = Duration.between(lastProbeTime, Instant.now());

        // If in flow: enforce a shorter suppression window rather than silently skipping
        long requiredMinutes = result.getState().shouldAvoidInterruption()
                ? Math.max(intervalMinutes, intervalMinutes + SUPPRESSION_MINUTES)
                : intervalMinutes;

        if (sinceLast.toMinutes() < requiredMinutes) return;

        lastProbeTime = Instant.now();

        // Deliver on the EDT via the first available open project
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        if (openProjects.length == 0) return;
        Project project = openProjects[0];

        ApplicationManager.getApplication().invokeLater(() -> {
            EsmProbeDialog dialog = new EsmProbeDialog(project, result);
            dialog.show();
        });
    }

    /** Resets the probe timer (e.g. after a participant takes a break). */
    public void resetTimer() {
        lastProbeTime = Instant.now();
    }
}
