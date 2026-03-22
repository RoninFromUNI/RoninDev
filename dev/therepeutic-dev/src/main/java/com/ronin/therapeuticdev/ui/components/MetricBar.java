package com.ronin.therapeuticdev.ui.components;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

/**
 * reusable progress bar component for a single metric category.
 *
 * layout: Icon | Name | Weight | Progress Bar | Score
 * example:  ⌨  Typing   30%   ████████░░   72
 *
 * one of these exists for each of the five scoring categories in MetricsTabPanel.
 * the weight label (30%, 25%, etc.) reminds the developer how much this category
 * contributes to the composite score.
 *
 * the progress bar uses a custom RoundedProgressBarUI because the default
 * BasicProgressBarUI renders rectangular fills which look harsh in the dark theme.
 */
public class MetricBar extends JBPanel<MetricBar> {

    private static final Color BAR_BG = new Color(0x3C, 0x3F, 0x41);
    private static final Color BAR_FILL = new Color(0xE5, 0xA8, 0x4B); // Amber accent
    
    private final JBLabel iconLabel;
    private final JBLabel nameLabel;
    private final JBLabel weightLabel;
    private final JProgressBar progressBar;
    private final JBLabel scoreLabel;
    
    private double score = 0;

    /**
     * Creates a metric bar.
     *
     * @param icon   emoji or character icon
     * @param name   metric name
     * @param weight weight percentage (e.g., "30%")
     */
    public MetricBar(String icon, String name, String weight) {
        setLayout(new BorderLayout(JBUI.scale(8), 0));
        setOpaque(false);
        setBorder(JBUI.Borders.empty(4, 0));
        
        // Left side: Icon + Name + Weight
        JBPanel<?> leftPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(4), 0));
        leftPanel.setOpaque(false);
        
        iconLabel = new JBLabel(icon);
        iconLabel.setFont(iconLabel.getFont().deriveFont(12f));
        
        nameLabel = new JBLabel(name);
        nameLabel.setFont(nameLabel.getFont().deriveFont(11f));
        nameLabel.setForeground(JBColor.namedColor("Label.foreground", new Color(0xA9, 0xB7, 0xC6)));
        nameLabel.setPreferredSize(new Dimension(JBUI.scale(55), -1));
        
        weightLabel = new JBLabel(weight);
        weightLabel.setFont(weightLabel.getFont().deriveFont(10f));
        weightLabel.setForeground(JBColor.namedColor("Label.disabledForeground", new Color(0x6B, 0x73, 0x7C)));
        
        leftPanel.add(iconLabel);
        leftPanel.add(nameLabel);
        leftPanel.add(weightLabel);
        
        add(leftPanel, BorderLayout.WEST);
        
        // Center: Progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(false);
        progressBar.setPreferredSize(new Dimension(JBUI.scale(80), JBUI.scale(8)));
        progressBar.setBackground(BAR_BG);
        progressBar.setForeground(BAR_FILL);
        progressBar.setBorderPainted(false);
        
        // Custom UI for rounded corners
        progressBar.setUI(new RoundedProgressBarUI());
        
        JBPanel<?> centerPanel = new JBPanel<>(new FlowLayout(FlowLayout.CENTER));
        centerPanel.setOpaque(false);
        centerPanel.add(progressBar);
        
        add(centerPanel, BorderLayout.CENTER);
        
        // Right side: Score value
        scoreLabel = new JBLabel("0");
        scoreLabel.setFont(scoreLabel.getFont().deriveFont(Font.BOLD, 11f));
        scoreLabel.setForeground(JBColor.WHITE);
        scoreLabel.setPreferredSize(new Dimension(JBUI.scale(25), -1));
        scoreLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        
        add(scoreLabel, BorderLayout.EAST);
    }

    /**
     * Updates the score value (0.0 - 1.0).
     */
    public void setScore(double score) {
        this.score = score;
        int percentage = (int)(score * 100);
        progressBar.setValue(percentage);
        scoreLabel.setText(String.valueOf(percentage));
    }

    /**
     * Custom progress bar UI for rounded appearance.
     */
    private static class RoundedProgressBarUI extends javax.swing.plaf.basic.BasicProgressBarUI {
        @Override
        protected void paintDeterminate(Graphics g, JComponent c) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int width = c.getWidth();
            int height = c.getHeight();
            int arc = height;
            
            // Background
            g2.setColor(BAR_BG);
            g2.fillRoundRect(0, 0, width, height, arc, arc);
            
            // Fill
            int fillWidth = (int)(width * ((double)progressBar.getValue() / 100));
            if (fillWidth > 0) {
                g2.setColor(BAR_FILL);
                g2.fillRoundRect(0, 0, fillWidth, height, arc, arc);
            }
            
            g2.dispose();
        }
    }
}
