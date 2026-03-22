package com.ronin.therapeuticdev.ui.tabs;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.ronin.therapeuticdev.ui.components.MetricBar;

import javax.swing.*;
import java.awt.*;

/**
 * metrics tab — shows all five scoring categories as labelled progress bars.
 *
 * each bar corresponds to one of the five weighted dimensions in FlowDetector:
 *   ⌨ Typing     30%
 *   ⚠ Errors     25%
 *   📁 Focus     20%
 *   🔨 Builds    15%
 *   ⚡ Activity  10%
 *
 * updateMetrics() receives the five sub-scores (0.0–1.0) from FlowStatePanel's
 * onFlowDetected callback and forwards them to the individual MetricBar components.
 * this gives the developer a breakdown of which dimensions are contributing to or
 * dragging down their composite score.
 */
public class MetricsTabPanel extends JBPanel<MetricsTabPanel> {

    private static final Color CARD_BG = new Color(0x25, 0x25, 0x25);
    private static final Color CARD_BORDER = new Color(0x3C, 0x3F, 0x41);

    private final MetricBar typingBar;
    private final MetricBar errorsBar;
    private final MetricBar focusBar;
    private final MetricBar buildsBar;
    private final MetricBar activityBar;

    public MetricsTabPanel() {
        setLayout(new BorderLayout());
        setBackground(JBColor.namedColor("Panel.background", new Color(0x2B, 0x2B, 0x2B)));
        setBorder(JBUI.Borders.empty(8));
        
        // Card container
        JBPanel<?> card = new JBPanel<>();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1),
                JBUI.Borders.empty(12)
        ));
        
        // Create metric bars
        typingBar = new MetricBar("⌨", "Typing", "30%");
        errorsBar = new MetricBar("⚠", "Errors", "25%");
        focusBar = new MetricBar("📁", "Focus", "20%");
        buildsBar = new MetricBar("🔨", "Builds", "15%");
        activityBar = new MetricBar("⚡", "Activity", "10%");
        
        // Add with separators
        card.add(typingBar);
        card.add(createSeparator());
        card.add(errorsBar);
        card.add(createSeparator());
        card.add(focusBar);
        card.add(createSeparator());
        card.add(buildsBar);
        card.add(createSeparator());
        card.add(activityBar);
        
        add(card, BorderLayout.NORTH);
        
        // Fill remaining space
        add(Box.createVerticalGlue(), BorderLayout.CENTER);
    }

    private JSeparator createSeparator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(CARD_BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    /**
     * Updates all metric scores.
     * 
     * @param typing   typing score (0.0 - 1.0)
     * @param errors   error score (0.0 - 1.0)
     * @param focus    focus score (0.0 - 1.0)
     * @param builds   build score (0.0 - 1.0)
     * @param activity activity score (0.0 - 1.0)
     */
    public void updateMetrics(double typing, double errors, double focus, 
                              double builds, double activity) {
        typingBar.setScore(typing);
        errorsBar.setScore(errors);
        focusBar.setScore(focus);
        buildsBar.setScore(builds);
        activityBar.setScore(activity);
    }
}
