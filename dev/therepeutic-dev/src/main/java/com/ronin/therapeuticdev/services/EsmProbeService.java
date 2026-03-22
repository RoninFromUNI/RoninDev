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
 * delivers experience sampling method (ESM) probes at timed intervals.
 *
 * the esm probe is a 7-item flow state scale adapted from jackson and marsh (1996)
 * for software development context. it asks participants to self-report their
 * subjective flow experience, which i then correlate against the plugin's algorithmic
 * classification. that correlation is the primary ecological validity signal in the
 * dissertation — if the algorithm says FLOW and the participant says "i was focused",
 * that's convergent validity. if they disagree, i have interesting discussion material.
 *
 * called by SnapshotScheduler on each persist cycle (every 60 seconds). i evaluate
 * whether enough time has passed since the last probe, and whether the developer's
 * current state allows interruption.
 *
 * suppression logic:
 *   - probes are skipped entirely if auto break suggestions are disabled
 *   - if the developer is in DEEP_FLOW, FLOW, or EMERGING, i add extra minutes
 *     to the interval to avoid disrupting the flow state i'm trying to study
 *   - minimum gap between probes is the configured interval (default 30 min)
 *
 * the dialog is non-modal so it doesn't block the developer's workflow.
 * they can dismiss it and keep coding if they want.
 */
@Service(Service.Level.APP)
public final class EsmProbeService {

    private static final int DEFAULT_PROBE_INTERVAL_MINUTES = 30;

    // extra minutes added when the developer is in a flow state
    // i'd rather miss a probe than disrupt flow — the whole point is authenticity
    private static final int SUPPRESSION_MINUTES = 5;

    private Instant lastProbeTime = Instant.EPOCH;

    /**
     * checks whether a probe should fire and delivers it on the EDT if so.
     * safe to call from a background thread — invokeLater handles the EDT dispatch.
     */
    public void checkAndDeliver(FlowDetectionResult result) {
        TherapeuticDevSettings settings = ApplicationManager.getApplication()
                .getService(TherapeuticDevSettings.class);
        if (settings == null || !settings.autoBreakSuggestions) return;

        int intervalMinutes = settings.breakIntervalMinutes > 0
                ? settings.breakIntervalMinutes
                : DEFAULT_PROBE_INTERVAL_MINUTES;

        Duration sinceLast = Duration.between(lastProbeTime, Instant.now());

        // if the developer is in a flow-adjacent state, extend the interval
        // to avoid interrupting them. better to delay the probe than disrupt
        // the exact thing i'm trying to measure
        long requiredMinutes = result.getState().shouldAvoidInterruption()
                ? Math.max(intervalMinutes, intervalMinutes + SUPPRESSION_MINUTES)
                : intervalMinutes;

        if (sinceLast.toMinutes() < requiredMinutes) return;

        lastProbeTime = Instant.now();

        // need a project reference for the dialog parent — grab the first open one
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        if (openProjects.length == 0) return;
        Project project = openProjects[0];

        ApplicationManager.getApplication().invokeLater(() -> {
            EsmProbeDialog dialog = new EsmProbeDialog(project, result);
            dialog.show();
        });
    }

    /**
     * resets the probe timer — called after a participant takes a break
     * so the next probe fires relative to when they resumed, not when
     * the last probe happened.
     */
    public void resetTimer() {
        lastProbeTime = Instant.now();
    }
}
