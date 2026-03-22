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
import java.util.Comparator;
import java.util.Map.Entry;

/**
 * activity tab — live stats, file heatmap, and context switch timeline.
 *
 * three sections stacked vertically:
 *   1. live stat chips — kpm, idle time, current file (update every 1 second)
 *   2. file heatmap — proportional bars showing time spent in each file over
 *      the last 30 minutes, colour-coded by intensity
 *   3. switch timeline — vertical list of recent file visits with timestamps
 *      and duration, most recent at top
 *
 * the warning banner appears when context switches exceed the configured threshold
 * (default 7 per interval). this is a visual cue that the focus metric is taking
 * a hit, which might explain why the composite score is dropping.
 *
 * updateActivity() is called from FlowStatePanel's onFlowDetected (every 2s for full
 * refresh). updateLiveChips() is called from FlowStatePanel's 1-second timer for the
 * stat chips only — keeping kpm and idle time visibly ticking without a full redraw.
 */
public class ActivityTabPanel extends JBPanel<ActivityTabPanel> {

    private static final Color CARD_BG      = new Color(0x25, 0x25, 0x25);
    private static final Color CARD_BORDER  = new Color(0x3C, 0x3F, 0x41);
    private static final Color WARNING_BG   = new Color(0x3D, 0x2B, 0x2B);
    private static final Color WARNING_BORDER = new Color(0x5C, 0x3A, 0x3A);
    private static final Color WARNING_TEXT = new Color(0xF4, 0x43, 0x36);
    private static final Color ACCENT       = new Color(0xE5, 0xA8, 0x4B);
    private static final Color MUTED        = new Color(0x6B, 0x73, 0x7C);
    private static final Color CHIP_BG      = new Color(0x32, 0x35, 0x37);
    private static final Color SWITCH_DOT   = new Color(0xE5, 0xA8, 0x4B);

    private final Project project;

    // Live stat chips
    private JBLabel kpmChip;
    private JBLabel idleChip;
    private JBLabel fileChip;

    // Warning banner
    private JBPanel<?> warningBanner;
    private JBLabel warningLabel;

    // Heatmap
    private JBPanel<?> heatmapPanel;

    // Vertical timeline
    private JBPanel<?> timelineListPanel;

    public ActivityTabPanel(Project project) {
        this.project = project;
        setLayout(new BorderLayout(0, JBUI.scale(8)));
        setBackground(JBColor.namedColor("Panel.background", new Color(0x2B, 0x2B, 0x2B)));
        setBorder(JBUI.Borders.empty(8));
        initUI();
    }

    // ==================== INIT ====================

    private void initUI() {
        JBPanel<?> content = new JBPanel<>();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        // Live stats row
        content.add(buildStatsRow());
        content.add(Box.createVerticalStrut(JBUI.scale(8)));

        // Warning banner (hidden by default)
        warningBanner = buildWarningBanner();
        warningBanner.setVisible(false);
        content.add(warningBanner);
        content.add(Box.createVerticalStrut(JBUI.scale(4)));

        // File heatmap
        content.add(buildHeatmapCard());
        content.add(Box.createVerticalStrut(JBUI.scale(8)));

        // Vertical timeline
        content.add(buildTimelineCard());

        JBScrollPane scroll = new JBScrollPane(content);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);
    }

    // ==================== STATS ROW ====================

    private JBPanel<?> buildStatsRow() {
        JBPanel<?> row = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(28)));

        kpmChip  = chip("⌨ – KPM");
        idleChip = chip("💤 –");
        fileChip = chip("📄 –");

        row.add(kpmChip);
        row.add(idleChip);
        row.add(fileChip);
        return row;
    }

    private JBLabel chip(String text) {
        JBLabel label = new JBLabel(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CHIP_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        label.setForeground(JBColor.namedColor("Label.foreground", new Color(0xA9, 0xB7, 0xC6)));
        label.setFont(label.getFont().deriveFont(10f));
        label.setBorder(JBUI.Borders.empty(3, 7));
        label.setOpaque(false);
        return label;
    }

    // ==================== WARNING BANNER ====================

    private JBPanel<?> buildWarningBanner() {
        JBPanel<?> banner = new JBPanel<>(new FlowLayout(FlowLayout.LEFT));
        banner.setBackground(WARNING_BG);
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(WARNING_BORDER, 1),
                JBUI.Borders.empty(6)
        ));
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(36)));

        warningLabel = new JBLabel("⚠ 0 context switches");
        warningLabel.setForeground(WARNING_TEXT);
        warningLabel.setFont(warningLabel.getFont().deriveFont(11f));
        banner.add(warningLabel);
        return banner;
    }

    // ==================== HEATMAP ====================

    private JBPanel<?> buildHeatmapCard() {
        JBPanel<?> card = new JBPanel<>(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1),
                JBUI.Borders.empty(12)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(220)));

        JBLabel header = new JBLabel("FILE ACTIVITY  (last 30 min)");
        header.setFont(header.getFont().deriveFont(Font.BOLD, 10f));
        header.setForeground(ACCENT);
        card.add(header, BorderLayout.NORTH);

        heatmapPanel = new JBPanel<>();
        heatmapPanel.setLayout(new BoxLayout(heatmapPanel, BoxLayout.Y_AXIS));
        heatmapPanel.setOpaque(false);
        heatmapPanel.setBorder(JBUI.Borders.emptyTop(8));
        addHeatmapRow("No activity yet", 0);

        JBScrollPane scroll = new JBScrollPane(heatmapPanel);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private void addHeatmapRow(String name, int pct) {
        JBPanel<?> row = new JBPanel<>(new BorderLayout(JBUI.scale(8), 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(22)));
        row.setBorder(JBUI.Borders.empty(2, 0));

        String display = name.length() > 26 ? name.substring(0, 23) + "…" : name;
        JBLabel nameLabel = new JBLabel(display);
        nameLabel.setFont(nameLabel.getFont().deriveFont(10f));
        nameLabel.setForeground(JBColor.namedColor("Label.foreground", new Color(0xA9, 0xB7, 0xC6)));
        nameLabel.setPreferredSize(new Dimension(JBUI.scale(120), -1));
        row.add(nameLabel, BorderLayout.WEST);

        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(pct);
        bar.setStringPainted(false);
        bar.setPreferredSize(new Dimension(JBUI.scale(80), JBUI.scale(5)));
        bar.setBackground(new Color(0x3C, 0x3F, 0x41));
        bar.setForeground(heatColor(pct));

        JBPanel<?> barWrap = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        barWrap.setOpaque(false);
        barWrap.add(bar);
        row.add(barWrap, BorderLayout.CENTER);

        JBLabel pctLabel = new JBLabel(pct > 0 ? pct + "%" : "");
        pctLabel.setFont(pctLabel.getFont().deriveFont(10f));
        pctLabel.setForeground(MUTED);
        pctLabel.setPreferredSize(new Dimension(JBUI.scale(30), -1));
        pctLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        row.add(pctLabel, BorderLayout.EAST);

        heatmapPanel.add(row);
    }

    private Color heatColor(int pct) {
        if (pct >= 40) return ACCENT;
        if (pct >= 25) return new Color(0x8B, 0x73, 0x55);
        if (pct >= 10) return new Color(0x5C, 0x50, 0x40);
        return new Color(0x4A, 0x42, 0x35);
    }

    // ==================== VERTICAL TIMELINE ====================

    private JBPanel<?> buildTimelineCard() {
        JBPanel<?> card = new JBPanel<>(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1),
                JBUI.Borders.empty(12)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(200)));

        JBLabel header = new JBLabel("SWITCH TIMELINE");
        header.setFont(header.getFont().deriveFont(Font.BOLD, 10f));
        header.setForeground(ACCENT);
        card.add(header, BorderLayout.NORTH);

        timelineListPanel = new JBPanel<>();
        timelineListPanel.setLayout(new BoxLayout(timelineListPanel, BoxLayout.Y_AXIS));
        timelineListPanel.setOpaque(false);
        timelineListPanel.setBorder(JBUI.Borders.emptyTop(8));

        JBLabel empty = new JBLabel("No switches yet");
        empty.setForeground(MUTED);
        empty.setFont(empty.getFont().deriveFont(10f));
        timelineListPanel.add(empty);

        JBScrollPane scroll = new JBScrollPane(timelineListPanel);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private void rebuildTimeline(List<String[]> visits) {
        timelineListPanel.removeAll();

        if (visits.isEmpty()) {
            JBLabel empty = new JBLabel("No switches yet");
            empty.setForeground(MUTED);
            empty.setFont(empty.getFont().deriveFont(10f));
            timelineListPanel.add(empty);
            return;
        }

        long now = System.currentTimeMillis();
        for (String[] visit : visits) {
            String filename  = visit[0];
            long   startMs   = Long.parseLong(visit[1]);
            long   endMs     = Long.parseLong(visit[2]); // 0 = in-progress

            String agoText  = formatAgo(now - startMs);
            String durText  = endMs > 0 ? formatDuration(endMs - startMs) : "active";

            timelineListPanel.add(buildTimelineRow(filename, agoText, durText, endMs == 0));
            timelineListPanel.add(Box.createVerticalStrut(JBUI.scale(4)));
        }

        timelineListPanel.revalidate();
        timelineListPanel.repaint();
    }

    private JBPanel<?> buildTimelineRow(String filename, String ago, String duration, boolean active) {
        JBPanel<?> row = new JBPanel<>(new BorderLayout(JBUI.scale(8), 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(20)));
        row.setAlignmentX(LEFT_ALIGNMENT);

        // Dot
        JPanel dot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(active ? ACCENT : MUTED);
                g2.fillOval(0, 3, 8, 8);
                g2.dispose();
            }
        };
        dot.setPreferredSize(new Dimension(10, 14));
        dot.setOpaque(false);
        row.add(dot, BorderLayout.WEST);

        // Filename
        String display = filename.length() > 24 ? filename.substring(0, 21) + "…" : filename;
        JBLabel name = new JBLabel(display);
        name.setFont(name.getFont().deriveFont(10f));
        name.setForeground(active
                ? JBColor.namedColor("Label.foreground", new Color(0xA9, 0xB7, 0xC6))
                : MUTED);
        row.add(name, BorderLayout.CENTER);

        // Time info
        JBLabel time = new JBLabel(ago + "  · " + duration);
        time.setFont(time.getFont().deriveFont(9f));
        time.setForeground(MUTED);
        row.add(time, BorderLayout.EAST);

        return row;
    }

    private String formatAgo(long ms) {
        if (ms < 60_000)  return "just now";
        if (ms < 3600_000) return (ms / 60_000) + "m ago";
        return (ms / 3600_000) + "h ago";
    }

    private String formatDuration(long ms) {
        if (ms < 60_000)  return (ms / 1000) + "s";
        if (ms < 3600_000) return (ms / 60_000) + "m";
        return (ms / 3600_000) + "h " + ((ms % 3600_000) / 60_000) + "m";
    }

    // ==================== UPDATE (called by FlowStatePanel every 5s) ====================

    public void updateActivity(FlowMetrics metrics) {
        MetricCollector collector = ApplicationManager.getApplication()
                .getService(MetricCollector.class);

        // --- Live stat chips ---
        if (collector != null) {
            double kpm  = collector.getKeystrokesPerMinute();
            long   idle = collector.getKeyboardIdleMs();
            String file = shortName(collector.getCurrentFilePath());

            kpmChip.setText(String.format("⌨ %.0f KPM", kpm));
            idleChip.setText("💤 " + formatDuration(idle) + " idle");
            fileChip.setText("📄 " + (file.isEmpty() ? "–" : file));
        }

        // --- Warning banner ---
        int switches = metrics.getFileChangesLast5Min();
        TherapeuticDevSettings settings = TherapeuticDevSettings.getInstance();
        if (switches >= settings.contextSwitchWarningThreshold) {
            warningLabel.setText("⚠ " + switches + " context switches this interval");
            warningBanner.setVisible(true);
        } else {
            warningBanner.setVisible(false);
        }

        // --- Heatmap ---
        if (collector != null) {
            Map<String, Long> activityMap = collector.getFileActivityMap(30 * 60 * 1000L);
            rebuildHeatmap(activityMap);

            // --- Vertical timeline ---
            List<String[]> visits = collector.getRecentVisits(8);
            rebuildTimeline(visits);
        }
    }

    private void rebuildHeatmap(Map<String, Long> activityMap) {
        heatmapPanel.removeAll();
        if (activityMap.isEmpty()) {
            addHeatmapRow("No activity yet", 0);
        } else {
            long total = activityMap.values().stream().mapToLong(Long::longValue).sum();
            if (total == 0) {
                addHeatmapRow("No activity yet", 0);
            } else {
                activityMap.entrySet().stream()
                        .sorted(Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                        .limit(6)
                        .forEach(e -> addHeatmapRow(e.getKey(), (int)((e.getValue() * 100) / total)));
            }
        }
        heatmapPanel.revalidate();
        heatmapPanel.repaint();
    }

    /**
     * Lightweight 1-second tick — only refreshes the stat chips.
     * Keeps idle time and KPM visibly changing every second without a full redraw.
     */
    public void updateLiveChips() {
        MetricCollector collector = ApplicationManager.getApplication()
                .getService(MetricCollector.class);
        if (collector == null) return;

        double kpm  = collector.getKeystrokesPerMinute();
        long   idle = collector.getKeyboardIdleMs();
        String file = shortName(collector.getCurrentFilePath());

        kpmChip.setText(String.format("⌨ %.1f KPM", kpm));
        idleChip.setText("💤 " + formatDuration(idle));
        fileChip.setText("📄 " + (file.isEmpty() ? "–" : file));
    }

    private String shortName(String path) {
        if (path == null || path.isEmpty()) return "";
        int idx = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return idx >= 0 ? path.substring(idx + 1) : path;
    }
}
