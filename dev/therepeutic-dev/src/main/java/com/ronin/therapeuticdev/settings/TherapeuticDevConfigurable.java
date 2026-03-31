package com.ronin.therapeuticdev.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.ronin.therapeuticdev.services.ParticipantSession;
import com.ronin.therapeuticdev.storage.MetricRepository;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;


import javax.swing.*;
import java.awt.*;

/**
 * the settings panel which is accessible via Settings > Tools > Therapeutic Dev.
 *
 * i use intellij's Configurable interface which gives me the standard apply/reset/
 * isModified lifecycle for free. the panel is built with FormBuilder which handles
 * label alignment and spacing automatically so it looks native.
 *
 * the export button at the bottom is the study data extraction point andddd it pulls
 * all snapshots from MetricRepository.exportToCsv() and writes to a user-chosen
 * file path. this is how i get participant data out of the plugin for analysis.
 */
public class TherapeuticDevConfigurable implements Configurable {

    private JPanel mainPanel;

    // detection controls
    private JSlider sensitivitySlider;
    private JBLabel sensitivityLabel;

    // break controls
    private JComboBox<String> breakIntervalCombo;
    private JBCheckBox autoBreakCheckbox;

    // notification controls
    private JBCheckBox modalNotificationCheckbox;
    private JBCheckBox balloonNotificationCheckbox;
    private JBCheckBox statusBarCheckbox;
    private JBCheckBox soundCheckbox;

    // activity view controls
    private JBCheckBox enableHeatmapCheckbox;
    private JBCheckBox trackSwitchesCheckbox;
    private JSpinner switchThresholdSpinner;

    // graph view controls
    private JBCheckBox enableGraphCheckbox;
    private JBCheckBox autoRefreshGraphCheckbox;
    private JBCheckBox showDependenciesCheckbox;

    // data controls
    private JBCheckBox collectMetricsCheckbox;
    private JButton exportButton;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Therapeutic Dev";
    }

    @Override
    public @Nullable JComponent createComponent() {
        TherapeuticDevSettings settings = TherapeuticDevSettings.getInstance();

        // ==================== DETECTION ====================
        // slider range 50–80 maps to flowThreshold 0.50–0.80
        // higher = stricter, meaning the developer needs a higher composite score to hit FLOW
        sensitivitySlider = new JSlider(50, 80, (int) (settings.flowThreshold * 100));
        sensitivitySlider.setMajorTickSpacing(10);
        sensitivitySlider.setMinorTickSpacing(5);
        sensitivitySlider.setPaintTicks(true);
        sensitivitySlider.setPaintLabels(true);

        sensitivityLabel = new JBLabel(String.valueOf(sensitivitySlider.getValue()));
        sensitivitySlider.addChangeListener(e ->
                sensitivityLabel.setText(String.valueOf(sensitivitySlider.getValue())));

        JPanel sensitivityPanel = new JPanel(new BorderLayout());
        sensitivityPanel.add(sensitivitySlider, BorderLayout.CENTER);
        sensitivityPanel.add(sensitivityLabel, BorderLayout.EAST);

        // ==================== BREAKS ====================
        breakIntervalCombo = new JComboBox<>(new String[]{"30 minutes", "60 minutes", "90 minutes"});
        breakIntervalCombo.setSelectedIndex(settings.breakIntervalMinutes == 30 ? 0 :
                settings.breakIntervalMinutes == 60 ? 1 : 2);

        autoBreakCheckbox = new JBCheckBox("Auto-suggest breaks", settings.autoBreakSuggestions);

        // ==================== NOTIFICATIONS ====================
        modalNotificationCheckbox = new JBCheckBox("Modal dialog", settings.useModalNotifications);
        balloonNotificationCheckbox = new JBCheckBox("Balloon notification", settings.useBalloonNotifications);
        statusBarCheckbox = new JBCheckBox("Status bar widget", settings.showStatusBarWidget);
        soundCheckbox = new JBCheckBox("Play sound", settings.playSoundOnBreak);

        // ==================== ACTIVITY VIEW ====================
        enableHeatmapCheckbox = new JBCheckBox("Enable activity heatmap", settings.enableActivityHeatmap);
        trackSwitchesCheckbox = new JBCheckBox("Track context switches", settings.trackContextSwitches);

        SpinnerNumberModel switchModel = new SpinnerNumberModel(
                settings.contextSwitchWarningThreshold, 1, 20, 1);
        switchThresholdSpinner = new JSpinner(switchModel);

        // ==================== GRAPH VIEW ====================
        enableGraphCheckbox = new JBCheckBox("Enable graph view", settings.enableGraphView);
        autoRefreshGraphCheckbox = new JBCheckBox("Auto-refresh on changes", settings.autoRefreshGraph);
        showDependenciesCheckbox = new JBCheckBox("Show dependencies", settings.showDependencies);

        // ==================== DATA ====================
        collectMetricsCheckbox = new JBCheckBox("Collect metrics", settings.collectMetrics);
        exportButton = new JButton("Export CSV");
        exportButton.addActionListener(e -> exportData());

        JButton resetSessionButton = new JButton("Reset Participant Session");
        resetSessionButton.addActionListener(e -> resetSession());

        // ==================== ASSEMBLE ====================
        // FormBuilder handles label alignment and consistent spacing across all sections
        mainPanel = FormBuilder.createFormBuilder()
                .addSeparator()
                .addComponent(new JBLabel("Detection"))
                .addLabeledComponent("Sensitivity threshold:", sensitivityPanel)
                .addComponentToRightColumn(new JBLabel("Higher = stricter flow detection"), 1)

                .addSeparator()
                .addComponent(new JBLabel("Breaks"))
                .addLabeledComponent("Break interval:", breakIntervalCombo)
                .addComponent(autoBreakCheckbox)

                .addSeparator()
                .addComponent(new JBLabel("Notifications"))
                .addComponent(modalNotificationCheckbox)
                .addComponent(balloonNotificationCheckbox)
                .addComponent(statusBarCheckbox)
                .addComponent(soundCheckbox)

                .addSeparator()
                .addComponent(new JBLabel("Activity View"))
                .addComponent(enableHeatmapCheckbox)
                .addComponent(trackSwitchesCheckbox)
                .addLabeledComponent("Switch warning threshold:", switchThresholdSpinner)

                .addSeparator()
                .addComponent(new JBLabel("Graph View"))
                .addComponent(enableGraphCheckbox)
                .addComponent(autoRefreshGraphCheckbox)
                .addComponent(showDependenciesCheckbox)

                .addSeparator()
                .addComponent(new JBLabel("Data"))
                .addComponent(collectMetricsCheckbox)
                .addComponent(exportButton)
                .addComponent(resetSessionButton)

                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();

        mainPanel.setBorder(JBUI.Borders.empty(10));

        return mainPanel;
    }

    /**
     * intellij calls this to decide whether the "Apply" button should be enabled.
     * i compare every control's current value against the persisted settings.
     */
    @Override
    public boolean isModified() {
        TherapeuticDevSettings settings = TherapeuticDevSettings.getInstance();

        return sensitivitySlider.getValue() != (int) (settings.flowThreshold * 100)
                || getBreakIntervalFromCombo() != settings.breakIntervalMinutes
                || autoBreakCheckbox.isSelected() != settings.autoBreakSuggestions
                || modalNotificationCheckbox.isSelected() != settings.useModalNotifications
                || balloonNotificationCheckbox.isSelected() != settings.useBalloonNotifications
                || statusBarCheckbox.isSelected() != settings.showStatusBarWidget
                || soundCheckbox.isSelected() != settings.playSoundOnBreak
                || enableHeatmapCheckbox.isSelected() != settings.enableActivityHeatmap
                || trackSwitchesCheckbox.isSelected() != settings.trackContextSwitches
                || (Integer) switchThresholdSpinner.getValue() != settings.contextSwitchWarningThreshold
                || enableGraphCheckbox.isSelected() != settings.enableGraphView
                || autoRefreshGraphCheckbox.isSelected() != settings.autoRefreshGraph
                || showDependenciesCheckbox.isSelected() != settings.showDependencies
                || collectMetricsCheckbox.isSelected() != settings.collectMetrics;
    }

    /**
     * writes all control values to the persistent settings object.
     * intellij handles the actual xml serialisation after this returns.
     */
    @Override
    public void apply() throws ConfigurationException {
        TherapeuticDevSettings settings = TherapeuticDevSettings.getInstance();

        settings.flowThreshold = sensitivitySlider.getValue() / 100.0;
        settings.breakIntervalMinutes = getBreakIntervalFromCombo();
        settings.autoBreakSuggestions = autoBreakCheckbox.isSelected();

        settings.useModalNotifications = modalNotificationCheckbox.isSelected();
        settings.useBalloonNotifications = balloonNotificationCheckbox.isSelected();
        settings.showStatusBarWidget = statusBarCheckbox.isSelected();
        settings.playSoundOnBreak = soundCheckbox.isSelected();

        settings.enableActivityHeatmap = enableHeatmapCheckbox.isSelected();
        settings.trackContextSwitches = trackSwitchesCheckbox.isSelected();
        settings.contextSwitchWarningThreshold = (Integer) switchThresholdSpinner.getValue();

        settings.enableGraphView = enableGraphCheckbox.isSelected();
        settings.autoRefreshGraph = autoRefreshGraphCheckbox.isSelected();
        settings.showDependencies = showDependenciesCheckbox.isSelected();

        settings.collectMetrics = collectMetricsCheckbox.isSelected();
    }

    /**
     * restores all controls to the currently persisted values.
     * called when the user clicks "Reset" or re-opens the settings panel.
     */
    @Override
    public void reset() {
        TherapeuticDevSettings settings = TherapeuticDevSettings.getInstance();

        sensitivitySlider.setValue((int) (settings.flowThreshold * 100));
        setBreakIntervalCombo(settings.breakIntervalMinutes);
        autoBreakCheckbox.setSelected(settings.autoBreakSuggestions);

        modalNotificationCheckbox.setSelected(settings.useModalNotifications);
        balloonNotificationCheckbox.setSelected(settings.useBalloonNotifications);
        statusBarCheckbox.setSelected(settings.showStatusBarWidget);
        soundCheckbox.setSelected(settings.playSoundOnBreak);

        enableHeatmapCheckbox.setSelected(settings.enableActivityHeatmap);
        trackSwitchesCheckbox.setSelected(settings.trackContextSwitches);
        switchThresholdSpinner.setValue(settings.contextSwitchWarningThreshold);

        enableGraphCheckbox.setSelected(settings.enableGraphView);
        autoRefreshGraphCheckbox.setSelected(settings.autoRefreshGraph);
        showDependenciesCheckbox.setSelected(settings.showDependencies);

        collectMetricsCheckbox.setSelected(settings.collectMetrics);
    }

    private int getBreakIntervalFromCombo() {
        return switch (breakIntervalCombo.getSelectedIndex()) {
            case 0 -> 30;
            case 2 -> 90;
            default -> 60;
        };
    }

    private void setBreakIntervalCombo(int minutes) {
        breakIntervalCombo.setSelectedIndex(minutes == 30 ? 0 : minutes == 90 ? 2 : 1);
    }

    /**
     * the csv export handler — this is how study data gets out of the plugin.
     * opens a file chooser, pulls all snapshots from MetricRepository, writes to disk.
     * the csv includes every column from the snapshots table: timestamps, all five
     * sub-scores, composite tally, classified state, and session id.
     */
    private void exportData() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Flow Metrics");
        fileChooser.setSelectedFile(new java.io.File("therapeutic-dev-export.csv"));
        fileChooser.setFileFilter(
                new javax.swing.filechooser.FileNameExtensionFilter("CSV files", "csv")
        );

        int userSelection = fileChooser.showSaveDialog(mainPanel);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            java.io.File fileToSave = fileChooser.getSelectedFile();

            MetricRepository repo = ApplicationManager.getApplication()
                    .getService(MetricRepository.class);

            // snapshot export (existing behaviour)
            String csvContent = repo.exportToCsv();
            try (java.io.FileWriter writer = new java.io.FileWriter(fileToSave)) {
                writer.write(csvContent);
            } catch (java.io.IOException ex) {
                JOptionPane.showMessageDialog(mainPanel,
                        "Export failed: " + ex.getMessage(),
                        "Export Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // esm export — sits alongside the snapshot csv with a suffixed filename
            String baseName = fileToSave.getAbsolutePath();
            String esmPath = baseName.endsWith(".csv")
                    ? baseName.substring(0, baseName.length() - 4) + "-esm.csv"
                    : baseName + "-esm.csv";

            String esmContent = repo.exportEsmToCsv();
            try (java.io.FileWriter writer = new java.io.FileWriter(esmPath)) {
                writer.write(esmContent);
            } catch (java.io.IOException ex) {
                JOptionPane.showMessageDialog(mainPanel,
                        "Snapshot CSV exported but ESM export failed: " + ex.getMessage(),
                        "Partial Export",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            JOptionPane.showMessageDialog(mainPanel,
                    "Exported successfully:\n" + fileToSave.getAbsolutePath()
                            + "\n" + esmPath,
                    "Export Complete",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void resetSession() {
        int confirm = JOptionPane.showConfirmDialog(mainPanel,
                "This clears the current participant ID.\nThe setup panel will appear on next tool window open.",
                "Reset Session",
                JOptionPane.OK_CANCEL_OPTION);

        if (confirm == JOptionPane.OK_OPTION) {
            ParticipantSession.getInstance().clearParticipant();

            JOptionPane.showMessageDialog(mainPanel,
                    "Session reset. Reopen the Therapeutic Dev tool window to see the setup panel.",
                    "Reset Complete",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
