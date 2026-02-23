package com.ronin.therapeuticdev.ui.tabs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.ronin.therapeuticdev.metrics.FlowMetrics;
import com.ronin.therapeuticdev.services.MetricCollector;
import com.ronin.therapeuticdev.settings.TherapeuticDevSettings;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Comparator;

/**
 * Activity tab showing file heatmap and context switch timeline.
 * 
 * <p>Layout:
 * <pre>
 * ┌─────────────────────────────────┐
 * │ ⚠ 7 context switches in 10 min │  ← Warning banner (if threshold exceeded)
 * ├─────────────────────────────────┤
 * │ FILE ACTIVITY (last 30 min)    │
 * │ FlowDetector.java    ████████ 42%│
 * │ MetricCollector.java █████    28%│
 * │ FlowMetrics.java     ███      15%│
 * │ ...                              │
 * ├─────────────────────────────────┤
 * │ SWITCH TIMELINE                 │
 * │ ●──●●────●────●──●──●──────→    │
 * │ 30m ago                    now  │
 * └─────────────────────────────────┘
 * </pre>
 */
public class ActivityTabPanel extends JBPanel<ActivityTabPanel> {

    private static final Color CARD_BG = new Color(0x25, 0x25, 0x25);
    private static final Color CARD_BORDER = new Color(0x3C, 0x3F, 0x41);
    private static final Color WARNING_BG = new Color(0x3D, 0x2B, 0x2B);
    private static final Color WARNING_BORDER = new Color(0x5C, 0x3A, 0x3A);
    private static final Color WARNING_TEXT = new Color(0xF4, 0x43, 0x36);
    private static final Color ACCENT = new Color(0xE5, 0xA8, 0x4B);
    private static final Color MUTED = new Color(0x6B, 0x73, 0x7C);
    
    private final Project project;
    private JBPanel<?> warningBanner;
    private JBLabel warningLabel;
    private JBPanel<?> heatmapPanel;
    private SwitchTimelinePanel timelinePanel;
    
    private int contextSwitchCount = 0;

    public ActivityTabPanel(Project project) {
        this.project = project;
        
        setLayout(new BorderLayout(0, JBUI.scale(8)));
        setBackground(JBColor.namedColor("Panel.background", new Color(0x2B, 0x2B, 0x2B)));
        setBorder(JBUI.Borders.empty(8));
        
        initializeUI();
    }

    private void initializeUI() {
        JBPanel<?> contentPanel = new JBPanel<>();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        
        // === WARNING BANNER ===
        warningBanner = createWarningBanner();
        warningBanner.setVisible(false);
        contentPanel.add(warningBanner);
        contentPanel.add(Box.createVerticalStrut(JBUI.scale(8)));
        
        // === FILE HEATMAP ===
        JBPanel<?> heatmapCard = createHeatmapCard();
        contentPanel.add(heatmapCard);
        contentPanel.add(Box.createVerticalStrut(JBUI.scale(8)));
        
        // === SWITCH TIMELINE ===
        JBPanel<?> timelineCard = createTimelineCard();
        contentPanel.add(timelineCard);
        
        add(contentPanel, BorderLayout.NORTH);
    }

    private JBPanel<?> createWarningBanner() {
        JBPanel<?> banner = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        banner.setBackground(WARNING_BG);
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(WARNING_BORDER, 1),
                JBUI.Borders.empty(8)
        ));
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(40)));
        
        warningLabel = new JBLabel("⚠ 0 context switches in 10 min");
        warningLabel.setForeground(WARNING_TEXT);
        warningLabel.setFont(warningLabel.getFont().deriveFont(11f));
        
        banner.add(warningLabel);
        
        return banner;
    }

    private JBPanel<?> createHeatmapCard() {
        JBPanel<?> card = new JBPanel<>(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1),
                JBUI.Borders.empty(12)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(200)));
        
        // Header
        JBLabel header = new JBLabel("FILE ACTIVITY (last 30 min)");
        header.setFont(header.getFont().deriveFont(Font.BOLD, 10f));
        header.setForeground(ACCENT);
        card.add(header, BorderLayout.NORTH);
        
        // Heatmap content
        heatmapPanel = new JBPanel<>();
        heatmapPanel.setLayout(new BoxLayout(heatmapPanel, BoxLayout.Y_AXIS));
        heatmapPanel.setOpaque(false);
        heatmapPanel.setBorder(JBUI.Borders.emptyTop(8));
        
        // Placeholder entries
        addHeatmapEntry("No activity yet", 0);
        
        JBScrollPane scrollPane = new JBScrollPane(heatmapPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        
        card.add(scrollPane, BorderLayout.CENTER);
        
        return card;
    }

    private JBPanel<?> createTimelineCard() {
        JBPanel<?> card = new JBPanel<>(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1),
                JBUI.Borders.empty(12)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(80)));
        
        // Header
        JBLabel header = new JBLabel("SWITCH TIMELINE");
        header.setFont(header.getFont().deriveFont(Font.BOLD, 10f));
        header.setForeground(ACCENT);
        card.add(header, BorderLayout.NORTH);
        
        // Timeline visualization
        timelinePanel = new SwitchTimelinePanel();
        card.add(timelinePanel, BorderLayout.CENTER);
        
        // Time labels
        JBPanel<?> labelPanel = new JBPanel<>(new BorderLayout());
        labelPanel.setOpaque(false);
        
        JBLabel startLabel = new JBLabel("30m ago");
        startLabel.setFont(startLabel.getFont().deriveFont(9f));
        startLabel.setForeground(MUTED);
        
        JBLabel endLabel = new JBLabel("now");
        endLabel.setFont(endLabel.getFont().deriveFont(9f));
        endLabel.setForeground(MUTED);
        
        labelPanel.add(startLabel, BorderLayout.WEST);
        labelPanel.add(endLabel, BorderLayout.EAST);
        
        card.add(labelPanel, BorderLayout.SOUTH);
        
        return card;
    }

    private void addHeatmapEntry(String filename, int percentage) {
        JBPanel<?> entry = new JBPanel<>(new BorderLayout(JBUI.scale(8), 0));
        entry.setOpaque(false);
        entry.setMaximumSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(24)));
        entry.setBorder(JBUI.Borders.empty(2, 0));
        
        // Filename (truncated)
        String displayName = filename.length() > 25 ? 
                filename.substring(0, 22) + "..." : filename;
        JBLabel nameLabel = new JBLabel(displayName);
        nameLabel.setFont(nameLabel.getFont().deriveFont(10f));
        nameLabel.setForeground(JBColor.namedColor("Label.foreground", new Color(0xA9, 0xB7, 0xC6)));
        nameLabel.setPreferredSize(new Dimension(JBUI.scale(120), -1));
        entry.add(nameLabel, BorderLayout.WEST);
        
        // Heat bar
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(percentage);
        bar.setStringPainted(false);
        bar.setPreferredSize(new Dimension(JBUI.scale(80), JBUI.scale(6)));
        bar.setBackground(new Color(0x3C, 0x3F, 0x41));
        bar.setForeground(getHeatColor(percentage));
        
        JBPanel<?> barPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        barPanel.setOpaque(false);
        barPanel.add(bar);
        entry.add(barPanel, BorderLayout.CENTER);
        
        // Percentage label
        JBLabel pctLabel = new JBLabel(percentage + "%");
        pctLabel.setFont(pctLabel.getFont().deriveFont(10f));
        pctLabel.setForeground(MUTED);
        pctLabel.setPreferredSize(new Dimension(JBUI.scale(30), -1));
        pctLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        entry.add(pctLabel, BorderLayout.EAST);
        
        heatmapPanel.add(entry);
    }

    private Color getHeatColor(int percentage) {
        // Gradient from muted to accent based on percentage
        if (percentage >= 40) return ACCENT;
        if (percentage >= 25) return new Color(0x8B, 0x73, 0x55);
        if (percentage >= 10) return new Color(0x5C, 0x50, 0x40);
        return new Color(0x4A, 0x42, 0x35);
    }

    /**
     * Updates the activity display with live data from MetricCollector.
     */
    public void updateActivity(FlowMetrics metrics) {
        contextSwitchCount = metrics.getFileChangesLast5Min();

        // Update warning banner
        TherapeuticDevSettings settings = TherapeuticDevSettings.getInstance();
        if (contextSwitchCount >= settings.contextSwitchWarningThreshold) {
            warningLabel.setText("⚠ " + contextSwitchCount + " context switches in 10 min");
            warningBanner.setVisible(true);
        } else {
            warningBanner.setVisible(false);
        }

        // Pull live data directly from MetricCollector
        MetricCollector collector = ApplicationManager.getApplication()
                .getService(MetricCollector.class);
        if (collector == null) return;

        // Sync timeline with real switch timestamps
        timelinePanel.setSwitchTimes(collector.getFileSwitchTimestamps());
        timelinePanel.repaint();

        // Rebuild heatmap from actual time-spent-per-file data
        Map<String, Long> activityMap = collector.getFileActivityMap(30 * 60 * 1000L);
        updateHeatmap(activityMap);
    }

    private void updateHeatmap(Map<String, Long> activityMap) {
        heatmapPanel.removeAll();

        if (activityMap.isEmpty()) {
            addHeatmapEntry("No activity yet", 0);
            heatmapPanel.revalidate();
            heatmapPanel.repaint();
            return;
        }

        long total = activityMap.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) {
            addHeatmapEntry("No activity yet", 0);
            heatmapPanel.revalidate();
            heatmapPanel.repaint();
            return;
        }

        activityMap.entrySet().stream()
                .sorted(Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(6)
                .forEach(e -> {
                    int pct = (int) ((e.getValue() * 100) / total);
                    addHeatmapEntry(e.getKey(), pct);
                });

        heatmapPanel.revalidate();
        heatmapPanel.repaint();
    }

    /**
     * Custom panel for drawing the switch timeline.
     */
    private static class SwitchTimelinePanel extends JBPanel<SwitchTimelinePanel> {
        private static final Color TIMELINE_COLOR = new Color(0x6B, 0x73, 0x7C);
        private static final Color SWITCH_COLOR = new Color(0xF4, 0x43, 0x36);

        private List<Long> switchTimes = new java.util.ArrayList<>();
        private final long windowMs = 30 * 60 * 1000; // 30 minutes

        public SwitchTimelinePanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(-1, JBUI.scale(30)));
        }

        /** Replace the timeline with the actual switch timestamps from MetricCollector. */
        public void setSwitchTimes(List<Long> times) {
            switchTimes = times;
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int width = getWidth();
            int height = getHeight();
            int centerY = height / 2;
            
            // Draw timeline line
            g2.setColor(TIMELINE_COLOR);
            g2.setStroke(new BasicStroke(1));
            g2.drawLine(0, centerY, width, centerY);
            
            // Draw switch dots
            long now = System.currentTimeMillis();
            g2.setColor(SWITCH_COLOR);
            
            for (Long switchTime : switchTimes) {
                double relativeTime = (double)(now - switchTime) / windowMs;
                int x = (int)(width * (1 - relativeTime));
                g2.fillOval(x - 4, centerY - 4, 8, 8);
            }
            
            g2.dispose();
        }
    }
}
