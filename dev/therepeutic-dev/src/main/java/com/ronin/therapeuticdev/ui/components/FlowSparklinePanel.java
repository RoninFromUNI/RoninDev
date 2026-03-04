package com.ronin.therapeuticdev.ui.components;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import com.ronin.therapeuticdev.detection.FlowDetectionResult;
import com.ronin.therapeuticdev.detection.FlowState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Rolling sparkline chart of composite flow scores over the last N detection cycles.
 *
 * Each call to {@link #addResult(FlowDetectionResult)} appends the latest scored
 * result and repaints. The line is coloured by the FlowState at each point, giving
 * a visual history of state transitions. Hovering over the chart displays a tooltip
 * with the five category sub-scores at that point in time.
 *
 * Rendered with antialiasing via Graphics2D; inherits IntelliJ theme colours.
 */
public class FlowSparklinePanel extends JPanel {

    private static final int MAX_POINTS = 60;   // 60 × 2 s = 2 min live, or 60 × 60 s = 1 hr persist
    private static final int PAD        = 10;

    private final Deque<FlowDetectionResult> history = new ArrayDeque<>(MAX_POINTS);

    /** Index of the result currently under the mouse cursor (-1 = none). */
    private int hoveredIndex = -1;

    public FlowSparklinePanel() {
        setPreferredSize(new Dimension(0, 100));
        setBackground(UIUtil.getPanelBackground());
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                hoveredIndex = xToIndex(e.getX());
                repaint();
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoveredIndex = -1;
                repaint();
            }
        });
    }

    /** Appends a detection result and repaints. Thread-safe (called from EDT via scheduler). */
    public void addResult(FlowDetectionResult result) {
        if (history.size() >= MAX_POINTS) history.pollFirst();
        history.addLast(result);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        List<FlowDetectionResult> pts = new ArrayList<>(history);
        if (pts.size() < 2) {
            drawEmpty(g);
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int w = getWidth()  - 2 * PAD;
        int h = getHeight() - 2 * PAD;
        int n = pts.size();

        // Draw baseline grid
        g2.setColor(JBColor.border());
        g2.setStroke(new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                0, new float[]{3, 3}, 0));
        // 0.65 (flow threshold) and 0.35 (procrastinating threshold) guide lines
        int yFlow = PAD + h - (int)(0.65 * h);
        int yProc = PAD + h - (int)(0.35 * h);
        g2.drawLine(PAD, yFlow, PAD + w, yFlow);
        g2.drawLine(PAD, yProc, PAD + w, yProc);

        // Draw sparkline segments
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 1; i < n; i++) {
            float x0 = PAD + (float)(i - 1) / (n - 1) * w;
            float x1 = PAD + (float) i       / (n - 1) * w;
            float y0 = PAD + h - (float) pts.get(i - 1).getFlowTally() * h;
            float y1 = PAD + h - (float) pts.get(i).getFlowTally() * h;

            g2.setColor(colourFor(pts.get(i).getState()));
            g2.draw(new Line2D.Float(x0, y0, x1, y1));
        }

        // Draw dots at each data point
        for (int i = 0; i < n; i++) {
            float x = PAD + (float) i / (n - 1) * w;
            float y = PAD + h - (float) pts.get(i).getFlowTally() * h;
            boolean hov = (i == hoveredIndex);
            g2.setColor(colourFor(pts.get(i).getState()));
            int r = hov ? 5 : 3;
            g2.fillOval((int)(x - r), (int)(y - r), r * 2, r * 2);
        }

        // Draw hover tooltip
        if (hoveredIndex >= 0 && hoveredIndex < pts.size()) {
            drawTooltip(g2, pts.get(hoveredIndex), hoveredIndex, n, w, h);
        }

        g2.dispose();
    }

    private void drawEmpty(Graphics g) {
        g.setColor(JBColor.GRAY);
        FontMetrics fm = g.getFontMetrics();
        String msg = "Collecting data…";
        g.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2,
                getHeight() / 2 + fm.getAscent() / 2);
    }

    private void drawTooltip(Graphics2D g2, FlowDetectionResult r,
                              int idx, int n, int w, int h) {
        String[] lines = {
            r.getState().name().replace('_', ' '),
            String.format("Score:    %d/100", (int)(r.getFlowTally() * 100)),
            String.format("Typing:   %d%%",   (int)(r.getTypingScore() * 100)),
            String.format("Errors:   %d%%",   (int)(r.getErrorScore() * 100)),
            String.format("Focus:    %d%%",   (int)(r.getFocusScore() * 100)),
            String.format("Build:    %d%%",   (int)(r.getBuildScore() * 100)),
            String.format("Context:  %d%%",   (int)(r.getActivityScore() * 100))
        };

        float px = PAD + (float) idx / Math.max(n - 1, 1) * w;
        float py = PAD + h - (float) r.getFlowTally() * h;

        g2.setFont(g2.getFont().deriveFont(10f));
        FontMetrics fm = g2.getFontMetrics();
        int tw = 0;
        for (String l : lines) tw = Math.max(tw, fm.stringWidth(l));
        int th = fm.getHeight() * lines.length + 8;

        int tx = (int) px + 8;
        int ty = (int) py - th / 2;
        // Keep inside bounds
        tx = Math.min(tx, getWidth()  - tw - 12);
        ty = Math.max(ty, PAD);
        ty = Math.min(ty, getHeight() - th - PAD);

        g2.setColor(new Color(30, 30, 30, 210));
        g2.fillRoundRect(tx - 4, ty - 4, tw + 12, th + 4, 6, 6);
        g2.setColor(Color.WHITE);
        for (int i = 0; i < lines.length; i++) {
            g2.drawString(lines[i], tx, ty + fm.getAscent() + fm.getHeight() * i);
        }
    }

    private int xToIndex(int mouseX) {
        List<FlowDetectionResult> pts = new ArrayList<>(history);
        if (pts.size() < 2) return -1;
        int w = getWidth() - 2 * PAD;
        float rel = (float)(mouseX - PAD) / w * (pts.size() - 1);
        int idx = Math.round(rel);
        return (idx >= 0 && idx < pts.size()) ? idx : -1;
    }

    private Color colourFor(FlowState state) {
        return switch (state) {
            case DEEP_FLOW       -> new Color(0x27AE60);
            case FLOW            -> new Color(0x2ECC71);
            case EMERGING        -> new Color(0x82E0AA);
            case NEUTRAL         -> new Color(0xF39C12);
            case DISRUPTED       -> new Color(0xE67E22);
            case PROCRASTINATING -> new Color(0xE74C3C);
            case NOT_IN_FLOW     -> new Color(0x95A5A6);
        };
    }
}
