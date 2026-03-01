package com.ronin.therapeuticdev.ui.tabs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Project tab showing a donut chart of file type composition,
 * similar to GitHub's language breakdown bar.
 */
public class ProjectCompositionPanel extends JBPanel<ProjectCompositionPanel> {

    private static final Color[] PALETTE = {
            new Color(0xE5, 0xA8, 0x4B), // amber   — Java
            new Color(0x4C, 0xAF, 0x50), // green   — Kotlin
            new Color(0x42, 0x9C, 0xD6), // blue    — XML / HTML
            new Color(0x9C, 0x64, 0xA6), // purple  — CSS / SCSS
            new Color(0x00, 0xBC, 0xD4), // cyan    — JSON / YAML
            new Color(0xF4, 0x43, 0x36), // red     — other
            new Color(0xFF, 0x57, 0x22), // deep orange
            new Color(0x8B, 0xC3, 0x4A), // lime
    };

    private static final Color CARD_BG     = new Color(0x25, 0x25, 0x25);
    private static final Color CARD_BORDER = new Color(0x3C, 0x3F, 0x41);
    private static final Color MUTED       = new Color(0x6B, 0x73, 0x7C);
    private static final Color ACCENT      = new Color(0xE5, 0xA8, 0x4B);

    private final Project project;
    private Map<String, Integer> fileCounts = new LinkedHashMap<>();
    private int totalFiles = 0;

    public ProjectCompositionPanel(Project project) {
        this.project = project;
        setLayout(new BorderLayout());
        setBackground(JBColor.namedColor("Panel.background", new Color(0x2B, 0x2B, 0x2B)));
        setBorder(JBUI.Borders.empty(8));

        JBLabel loading = new JBLabel("Scanning project...");
        loading.setForeground(MUTED);
        loading.setHorizontalAlignment(SwingConstants.CENTER);
        add(loading, BorderLayout.CENTER);

        scanAsync();
    }

    private void scanAsync() {
        SwingWorker<Map<String, Integer>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Integer> doInBackground() {
                Map<String, Integer> counts = new HashMap<>();
                VirtualFile[] roots = ProjectRootManager.getInstance(project).getContentRoots();
                for (VirtualFile root : roots) {
                    walkFiles(root, counts, 0);
                }
                return sortByValue(counts);
            }

            @Override
            protected void done() {
                try {
                    fileCounts = get();
                    totalFiles = fileCounts.values().stream().mapToInt(Integer::intValue).sum();
                    rebuildUI();
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private void rebuildUI() {
        removeAll();
        setLayout(new BorderLayout(0, JBUI.scale(8)));

        JBPanel<?> content = new JBPanel<>();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        // Donut chart card
        DonutChartPanel donut = new DonutChartPanel();
        JBPanel<?> donutCard = card(donut, JBUI.scale(180));
        content.add(donutCard);
        content.add(Box.createVerticalStrut(JBUI.scale(8)));

        // Legend card
        JBPanel<?> legend = buildLegend();
        content.add(card(legend, -1));

        JBScrollPane scroll = new JBScrollPane(content);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    private JBPanel<?> buildLegend() {
        JBPanel<?> panel = new JBPanel<>();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        JBLabel header = new JBLabel("FILE BREAKDOWN");
        header.setFont(header.getFont().deriveFont(Font.BOLD, 10f));
        header.setForeground(ACCENT);
        header.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(header);
        panel.add(Box.createVerticalStrut(JBUI.scale(8)));

        int idx = 0;
        for (Map.Entry<String, Integer> e : fileCounts.entrySet()) {
            if (idx >= PALETTE.length) break;
            int pct = totalFiles > 0 ? (e.getValue() * 100) / totalFiles : 0;
            panel.add(legendRow("." + e.getKey(), e.getValue(), pct, PALETTE[idx++]));
            panel.add(Box.createVerticalStrut(JBUI.scale(3)));
        }

        panel.add(Box.createVerticalStrut(JBUI.scale(6)));
        JBLabel total = new JBLabel("Total: " + totalFiles + " files");
        total.setFont(total.getFont().deriveFont(10f));
        total.setForeground(MUTED);
        total.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(total);

        return panel;
    }

    private JBPanel<?> legendRow(String ext, int count, int pct, Color color) {
        JBPanel<?> row = new JBPanel<>(new BorderLayout(JBUI.scale(6), 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(18)));
        row.setAlignmentX(LEFT_ALIGNMENT);

        // Color dot
        JPanel dot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(0, 2, 10, 10);
                g2.dispose();
            }
        };
        dot.setPreferredSize(new Dimension(12, 14));
        dot.setOpaque(false);
        row.add(dot, BorderLayout.WEST);

        JBLabel name = new JBLabel(ext);
        name.setFont(name.getFont().deriveFont(10f));
        name.setForeground(JBColor.namedColor("Label.foreground", new Color(0xA9, 0xB7, 0xC6)));
        name.setPreferredSize(new Dimension(JBUI.scale(55), -1));
        row.add(name, BorderLayout.CENTER);

        JBLabel info = new JBLabel(count + "  " + pct + "%");
        info.setFont(info.getFont().deriveFont(10f));
        info.setForeground(MUTED);
        row.add(info, BorderLayout.EAST);

        return row;
    }

    private JBPanel<?> card(JComponent content, int maxHeight) {
        JBPanel<?> card = new JBPanel<>(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1),
                JBUI.Borders.empty(12)
        ));
        if (maxHeight > 0) card.setMaximumSize(new Dimension(Integer.MAX_VALUE, maxHeight));
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private void walkFiles(VirtualFile file, Map<String, Integer> counts, int depth) {
        if (depth > 12) return;
        if (file.isDirectory()) {
            String n = file.getName();
            if (n.equals(".git") || n.equals("build") || n.equals("out")
                    || n.equals("node_modules") || n.equals(".idea") || n.equals("gradle")) return;
            for (VirtualFile child : file.getChildren()) walkFiles(child, counts, depth + 1);
        } else {
            String ext = file.getExtension();
            if (ext != null && !ext.isEmpty()) counts.merge(ext.toLowerCase(), 1, Integer::sum);
        }
    }

    private Map<String, Integer> sortByValue(Map<String, Integer> map) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
        Map<String, Integer> sorted = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : list) sorted.put(e.getKey(), e.getValue());
        return sorted;
    }

    /** Donut chart painted entirely with Graphics2D. */
    private class DonutChartPanel extends JPanel {
        DonutChartPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(-1, JBUI.scale(140)));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (fileCounts.isEmpty()) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int size    = Math.min(getWidth(), getHeight()) - JBUI.scale(16);
            int ox      = (getWidth()  - size) / 2;
            int oy      = (getHeight() - size) / 2;
            double total = fileCounts.values().stream().mapToInt(Integer::intValue).sum();

            double angle = -90.0;
            int colorIdx = 0;
            for (Map.Entry<String, Integer> e : fileCounts.entrySet()) {
                if (colorIdx >= PALETTE.length) break;
                double sweep = (e.getValue() / total) * 360.0;
                g2.setColor(PALETTE[colorIdx++]);
                g2.fillArc(ox, oy, size, size, (int) angle, (int) Math.ceil(sweep));
                angle += sweep;
            }

            // Donut hole
            int hole = (int)(size * 0.56);
            int hx = (getWidth()  - hole) / 2;
            int hy = (getHeight() - hole) / 2;
            g2.setColor(CARD_BG);
            g2.fillOval(hx, hy, hole, hole);

            // Center label
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 16f));
            g2.setColor(Color.WHITE);
            String num = String.valueOf(totalFiles);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(num, getWidth()/2 - fm.stringWidth(num)/2, getHeight()/2 + 5);

            g2.setFont(g2.getFont().deriveFont(9f));
            g2.setColor(MUTED);
            String sub = "files";
            fm = g2.getFontMetrics();
            g2.drawString(sub, getWidth()/2 - fm.stringWidth(sub)/2, getHeight()/2 + 18);

            g2.dispose();
        }
    }
}
