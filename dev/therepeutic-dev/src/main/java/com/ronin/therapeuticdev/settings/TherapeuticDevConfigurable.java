package com.ronin.therapeuticdev.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.ronin.therapeuticdev.storage.MetricRepository;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Settings panel for Therapeutic Dev plugin.
 * Accessible via Settings > Tools > Therapeutic Dev
 *
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/settings-guide.html">
 *      IntelliJ Platform SDK - Settings Guide</a>
 */
public class TherapeuticDevConfigurable implements Configurable {

    private JPanel mainPanel;

    // Detection
    private JSlider sensitivitySlider;
    private JBLabel sensitivityLabel;

    // Breaks
    private JComboBox<String> breakIntervalCombo;
    private JBCheckBox autoBreakCheckbox;

    // Notifications
    private JBCheckBox modalNotificationCheckbox;
    private JBCheckBox balloonNotificationCheckbox;
    private JBCheckBox statusBarCheckbox;
    private JBCheckBox soundCheckbox;

    // Activity View
    private JBCheckBox enableHeatmapCheckbox;
    private JBCheckBox trackSwitchesCheckbox;
    private JSpinner switchThresholdSpinner;

    // Graph View
    private JBCheckBox enableGraphCheckbox;
    private JBCheckBox autoRefreshGraphCheckbox;
    private JBCheckBox showDependenciesCheckbox;

    // Data
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

        // ==================== DETECTION SECTION ====================
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

        // ==================== BREAKS SECTION ====================
        breakIntervalCombo = new JComboBox<>(new String[]{"30 minutes", "60 minutes", "90 minutes"});
        breakIntervalCombo.setSelectedIndex(settings.breakIntervalMinutes == 30 ? 0 :
                settings.breakIntervalMinutes == 60 ? 1 : 2);

        autoBreakCheckbox = new JBCheckBox("Auto-suggest breaks", settings.autoBreakSuggestions);

        // ==================== NOTIFICATIONS SECTION ====================
        modalNotificationCheckbox = new JBCheckBox("Modal dialog", settings.useModalNotifications);
        balloonNotificationCheckbox = new JBCheckBox("Balloon notification", settings.useBalloonNotifications);
        statusBarCheckbox = new JBCheckBox("Status bar widget", settings.showStatusBarWidget);
        soundCheckbox = new JBCheckBox("Play sound", settings.playSoundOnBreak);

        // ==================== ACTIVITY VIEW SECTION ====================
        enableHeatmapCheckbox = new JBCheckBox("Enable activity heatmap", settings.enableActivityHeatmap);
        trackSwitchesCheckbox = new JBCheckBox("Track context switches", settings.trackContextSwitches);

        SpinnerNumberModel switchModel = new SpinnerNumberModel(
                settings.contextSwitchWarningThreshold, 1, 20, 1);
        switchThresholdSpinner = new JSpinner(switchModel);

        // ==================== GRAPH VIEW SECTION ====================
        enableGraphCheckbox = new JBCheckBox("Enable graph view", settings.enableGraphView);
        autoRefreshGraphCheckbox = new JBCheckBox("Auto-refresh on changes", settings.autoRefreshGraph);
        showDependenciesCheckbox = new JBCheckBox("Show dependencies", settings.showDependencies);

        // ==================== DATA SECTION ====================
        collectMetricsCheckbox = new JBCheckBox("Collect metrics", settings.collectMetrics);
        exportButton = new JButton("Export CSV");
        exportButton.addActionListener(e -> exportData());

        // ==================== BUILD PANEL ====================
        mainPanel = FormBuilder.createFormBuilder()
                // Detection
                .addSeparator()
                .addComponent(new JBLabel("Detection"))
                .addLabeledComponent("Sensitivity threshold:", sensitivityPanel)
                .addComponentToRightColumn(new JBLabel("Higher = stricter flow detection"), 1)

                // Breaks
                .addSeparator()
                .addComponent(new JBLabel("Breaks"))
                .addLabeledComponent("Break interval:", breakIntervalCombo)
                .addComponent(autoBreakCheckbox)

                // Notifications
                .addSeparator()
                .addComponent(new JBLabel("Notifications"))
                .addComponent(modalNotificationCheckbox)
                .addComponent(balloonNotificationCheckbox)
                .addComponent(statusBarCheckbox)
                .addComponent(soundCheckbox)

                // Activity View
                .addSeparator()
                .addComponent(new JBLabel("Activity View"))
                .addComponent(enableHeatmapCheckbox)
                .addComponent(trackSwitchesCheckbox)
                .addLabeledComponent("Switch warning threshold:", switchThresholdSpinner)

                // Graph View
                .addSeparator()
                .addComponent(new JBLabel("Graph View"))
                .addComponent(enableGraphCheckbox)
                .addComponent(autoRefreshGraphCheckbox)
                .addComponent(showDependenciesCheckbox)

                // Data
                .addSeparator()
                .addComponent(new JBLabel("Data"))
                .addComponent(collectMetricsCheckbox)
                .addComponent(exportButton)

                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();

        mainPanel.setBorder(JBUI.Borders.empty(10));

        return mainPanel;
    }

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
            String csvContent = repo.exportToCsv();

            try (java.io.FileWriter writer = new java.io.FileWriter(fileToSave)) {
                writer.write(csvContent);
                JOptionPane.showMessageDialog(mainPanel,
                        "Exported successfully to:\n" + fileToSave.getAbsolutePath(),
                        "Export Complete",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (java.io.IOException ex) {
                JOptionPane.showMessageDialog(mainPanel,
                        "Export failed: " + ex.getMessage(),
                        "Export Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

