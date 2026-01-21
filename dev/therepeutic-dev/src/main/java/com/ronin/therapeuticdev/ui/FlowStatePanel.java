package com.ronin.therapeuticdev.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import com.ronin.therapeuticdev.detection.FlowDetectionResult;
import com.ronin.therapeuticdev.detection.FlowState;
import com.ronin.therapeuticdev.metrics.FlowMetrics;
import com.ronin.therapeuticdev.services.MetricCollector;
import com.ronin.therapeuticdev.services.SnapshotScheduler;
import com.ronin.therapeuticdev.ui.components.HeroScoreCard;
import com.ronin.therapeuticdev.ui.tabs.ActivityTabPanel;
import com.ronin.therapeuticdev.ui.tabs.GraphTabPanel;
import com.ronin.therapeuticdev.ui.tabs.MetricsTabPanel;

import javax.swing.*;
import java.awt.*;

/**
 * Main panel for the Therapeutic Dev tool window.
 * 
 * <p>Layout structure (from wireframe v3):
 * <pre>
 * ┌─────────────────────────────┐
 * │      Hero Score Card        │  ← Flow score, trend, state badge
 * ├─────────────────────────────┤
 * │  Metrics │ Activity │ Graph │  ← Tab navigation
 * ├─────────────────────────────┤
 * │                             │
 * │      Tab Content Area       │  ← Switches based on selected tab
 * │                             │
 * ├─────────────────────────────┤
 * │      Session Footer         │  ← Duration + sparkline
 * └─────────────────────────────┘
 * </pre>
 */
public class FlowStatePanel implements SnapshotScheduler.FlowDetectionListener {

    private final Project project;
    private final JBPanel<JBPanel<?>> mainPanel;
    
    // UI Components
    private HeroScoreCard heroCard;
    private JBTabbedPane tabbedPane;
    private MetricsTabPanel metricsTab;
    private ActivityTabPanel activityTab;
    private GraphTabPanel graphTab;
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
        graphTab = new GraphTabPanel(project);
        
        tabbedPane.addTab("Metrics", metricsTab);
        tabbedPane.addTab("Activity", activityTab);
        tabbedPane.addTab("Graph", graphTab);
        
        // Add settings button as a tab (hacky but works)
        JBPanel<?> settingsPlaceholder = new JBPanel<>();
        tabbedPane.addTab("⚙", settingsPlaceholder);
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 3) {
                // Open settings dialog
                openSettings();
                tabbedPane.setSelectedIndex(0); // Switch back
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
        
        // Start timer to update session duration
        Timer sessionTimer = new Timer(1000, e -> updateSessionDuration());
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
