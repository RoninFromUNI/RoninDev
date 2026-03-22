package com.ronin.therapeuticdev.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import com.ronin.therapeuticdev.detection.FlowDetectionResult;
import com.ronin.therapeuticdev.detection.FlowState; // ADDED: Missing import
import com.ronin.therapeuticdev.metrics.FlowMetrics;
import com.ronin.therapeuticdev.services.MetricCollector;
import com.ronin.therapeuticdev.services.SnapshotScheduler;
import com.ronin.therapeuticdev.ui.components.FlowSparklinePanel;
import com.ronin.therapeuticdev.ui.components.HeroScoreCard;
import com.ronin.therapeuticdev.ui.components.MediaControlPanel;
import com.ronin.therapeuticdev.ui.tabs.ActivityTabPanel;
import com.ronin.therapeuticdev.ui.tabs.GraphAndProjectTab;
import com.ronin.therapeuticdev.ui.tabs.MetricsTabPanel;

import javax.swing.*;
import java.awt.*;

/**
 * the main dashboard panel — this is what the developer sees in the tool window.
 *
 * layout from top to bottom:
 *   hero score card — big number + state badge + trend arrow
 *   tabbed content — Metrics, Activity, History (sparkline), Graph & Project
 *   session footer — running clock + media controls
 *
 * i register as a FlowDetectionListener on SnapshotScheduler so every 2 seconds
 * the UI refreshes with fresh detection data. all updates go through onFlowDetected()
 * which dispatches to the EDT via SwingUtilities.invokeLater — the scheduler callback
 * runs on a background thread so direct swing access would be a threading violation.
 *
 * the settings gear tab (index 4) is a hacky but effective pattern — clicking it
 * opens the settings dialog and immediately switches back to tab 0. no actual
 * panel exists behind that tab.
 */
public class FlowStatePanel implements SnapshotScheduler.FlowDetectionListener {

    private final Project project;
    private final JBPanel<JBPanel<?>> mainPanel;

    // UI Components
    private HeroScoreCard heroCard;
    private JBTabbedPane tabbedPane;
    private MetricsTabPanel metricsTab;
    private ActivityTabPanel activityTab;
    private GraphAndProjectTab graphAndProjectTab;
    private FlowSparklinePanel sparklinePanel;
    private JBLabel sessionLabel;

    // State
    private FlowDetectionResult lastResult;
    private long sessionStartMs;

    public FlowStatePanel(Project project) {
        this.project = project;
        this.sessionStartMs = System.currentTimeMillis();
        this.mainPanel = new JBPanel<>(new BorderLayout());

        initializeUI();
        registerListener();

        // Initial update with placeholder data
        updateWithPlaceholder();
    }

    private void initializeUI() {
        mainPanel.setBackground(JBColor.namedColor("Panel.background", new Color(0x2B, 0x2B, 0x2B)));
        mainPanel.setBorder(JBUI.Borders.empty(8));

        // === HERO SCORE CARD ===
        heroCard = new HeroScoreCard();
        mainPanel.add(heroCard, BorderLayout.NORTH);

        // === TABBED CONTENT ===
        tabbedPane = new JBTabbedPane();
        tabbedPane.setTabPlacement(SwingConstants.TOP);

        metricsTab = new MetricsTabPanel();
        activityTab = new ActivityTabPanel(project);
        graphAndProjectTab = new GraphAndProjectTab(project);

        // History tab — sparkline panel wrapped in a card
        sparklinePanel = new FlowSparklinePanel();
        JBPanel<?> historyCard = new JBPanel<>(new BorderLayout());
        historyCard.setBorder(JBUI.Borders.empty(12));
        JBLabel sparklineHeader = new JBLabel("FLOW SCORE HISTORY  (last 60 readings)");
        sparklineHeader.setFont(sparklineHeader.getFont().deriveFont(Font.BOLD, 10f));
        sparklineHeader.setForeground(new Color(0xE5, 0xA8, 0x4B));
        sparklineHeader.setBorder(JBUI.Borders.emptyBottom(8));
        historyCard.add(sparklineHeader, BorderLayout.NORTH);
        historyCard.add(sparklinePanel, BorderLayout.CENTER);

        tabbedPane.addTab("Metrics",         metricsTab);
        tabbedPane.addTab("Activity",         activityTab);
        tabbedPane.addTab("History",          historyCard);
        tabbedPane.addTab("Graph & Project",  graphAndProjectTab);

        // Settings button as a tab (index 4)
        JBPanel<?> settingsPlaceholder = new JBPanel<>();
        tabbedPane.addTab("⚙", settingsPlaceholder);
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 4) {
                openSettings();
                tabbedPane.setSelectedIndex(0);
            }
        });

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // === SESSION FOOTER ===
        JBPanel<?> footerPanel = createFooterPanel();
        mainPanel.add(footerPanel, BorderLayout.SOUTH);
    }

    private JBPanel<?> createFooterPanel() {
        JBPanel<?> footer = new JBPanel<>(new BorderLayout());
        footer.setBackground(JBColor.namedColor("Panel.background", new Color(0x25, 0x25, 0x25)));
        footer.setBorder(JBUI.Borders.empty(8, 0, 0, 0));

        sessionLabel = new JBLabel("Session: 00:00:00");
        sessionLabel.setForeground(JBColor.namedColor("Label.foreground", new Color(0xA9, 0xB7, 0xC6)));
        sessionLabel.setFont(sessionLabel.getFont().deriveFont(11f));

        footer.add(sessionLabel, BorderLayout.WEST);

        // Media controls (play/pause/skip for YouTube Music etc.)
        MediaControlPanel mediaControls = new MediaControlPanel();
        footer.add(mediaControls, BorderLayout.EAST);

        // 1-second timer: updates session clock + live stat chips (KPM, idle, file)
        Timer sessionTimer = new Timer(1000, e -> {
            updateSessionDuration();
            activityTab.updateLiveChips();
        });
        sessionTimer.start();

        return footer;
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
        // Update UI on EDT
        SwingUtilities.invokeLater(() -> {
            lastResult = result;

            // Update hero card
            heroCard.updateScore(
                    (int)(result.getFlowTally() * 100),
                    result.getState(),
                    calculateTrend(result)
            );

            // Update metrics tab
            metricsTab.updateMetrics(
                    result.getTypingScore(),
                    result.getErrorScore(),
                    result.getFocusScore(),
                    result.getBuildScore(),
                    result.getActivityScore()
            );

            // Update activity tab
            activityTab.updateActivity(metrics);

            // Feed sparkline
            sparklinePanel.addResult(result);
        });
    }

    private int calculateTrend(FlowDetectionResult result) {
        // Simple trend calculation - compare to last result
        // Positive = improving, negative = declining
        if (lastResult == null) return 0;

        double diff = result.getFlowTally() - lastResult.getFlowTally();
        return (int)(diff * 100);
    }

    private void updateSessionDuration() {
        long elapsed = System.currentTimeMillis() - sessionStartMs;
        long hours = elapsed / 3600000;
        long minutes = (elapsed % 3600000) / 60000;
        long seconds = (elapsed % 60000) / 1000;

        sessionLabel.setText(String.format("Session: %02d:%02d:%02d", hours, minutes, seconds));
    }

    private void updateWithPlaceholder() {
        // Show placeholder data until first real detection
        heroCard.updateScore(0, FlowState.NEUTRAL, 0);
        metricsTab.updateMetrics(0, 0, 0, 0, 0);
    }

    private void openSettings() {
        // Open the settings dialog
        com.intellij.openapi.options.ShowSettingsUtil.getInstance()
                .showSettingsDialog(project, "Therapeutic Dev");
    }

    /**
     * Triggers an immediate refresh of flow detection.
     */
    public void refresh() {
        SnapshotScheduler scheduler = ApplicationManager.getApplication()
                .getService(SnapshotScheduler.class);

        if (scheduler != null) {
            FlowDetectionResult result = scheduler.triggerImmediateSnapshot();
            if (result != null) {
                MetricCollector collector = ApplicationManager.getApplication()
                        .getService(MetricCollector.class);
                if (collector != null) {
                    onFlowDetected(result, collector.snapshot());
                }
            }
        }
    }

    public JComponent getContent() {
        return mainPanel;
    }
}