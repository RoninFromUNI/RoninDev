package com.ronin.therapeuticdev.ui.components;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.ronin.therapeuticdev.detection.FlowState;

import javax.swing.*;
import java.awt.*;

/**
 * Hero card displaying the current flow score prominently.
 * 
 * <p>Layout:
 * <pre>
 * ┌─────────────────────────────────┐
 * │                                 │
 * │           72        ▲ +5       │  ← Score + Trend
 * │         ● FLOW                  │  ← State badge
 * │                                 │
 * └─────────────────────────────────┘
 * </pre>
 */
public class HeroScoreCard extends JBPanel<HeroScoreCard> {

    // Colors
    private static final Color FLOW_GREEN = new Color(0x4C, 0xAF, 0x50);
    private static final Color NEUTRAL_AMBER = new Color(0xE5, 0xA8, 0x4B);
    private static final Color STRESS_RED = new Color(0xF4, 0x43, 0x36);
    private static final Color CARD_BG = new Color(0x25, 0x25, 0x25);
    private static final Color CARD_BORDER = new Color(0x3C, 0x3F, 0x41);
    
    private final JBLabel scoreLabel;
    private final JBLabel trendLabel;
    private final JBLabel stateLabel;
    private final JBPanel<?> stateBadge;
    
    private int currentScore = 0;
    private FlowState currentState = FlowState.NEUTRAL;

    public HeroScoreCard() {
        setLayout(new BorderLayout());
        setBackground(CARD_BG);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1),
                JBUI.Borders.empty(16)
        ));
        setPreferredSize(new Dimension(-1, JBUI.scale(100)));
        
        // === CENTER: Score + Trend ===
        JBPanel<?> centerPanel = new JBPanel<>(new FlowLayout(FlowLayout.CENTER, 10, 0));
        centerPanel.setOpaque(false);
        
        // Big score number
        scoreLabel = new JBLabel("0");
        scoreLabel.setFont(scoreLabel.getFont().deriveFont(Font.BOLD, 48f));
        scoreLabel.setForeground(JBColor.WHITE);
        centerPanel.add(scoreLabel);
        
        // Trend indicator
        trendLabel = new JBLabel("");
        trendLabel.setFont(trendLabel.getFont().deriveFont(14f));
        trendLabel.setForeground(FLOW_GREEN);
        centerPanel.add(trendLabel);
        
        add(centerPanel, BorderLayout.CENTER);
        
        // === BOTTOM: State badge ===
        JBPanel<?> bottomPanel = new JBPanel<>(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setOpaque(false);
        
        stateBadge = createStateBadge();
        stateLabel = new JBLabel("NEUTRAL");
        stateLabel.setFont(stateLabel.getFont().deriveFont(11f));
        stateLabel.setForeground(JBColor.namedColor("Label.foreground", new Color(0xA9, 0xB7, 0xC6)));
        
        bottomPanel.add(stateBadge);
        bottomPanel.add(stateLabel);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JBPanel<?> createStateBadge() {
        JBPanel<?> badge = new JBPanel<>() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                g2.setColor(getStateColor(currentState));
                g2.fillOval(2, 2, getWidth() - 4, getHeight() - 4);
                
                g2.dispose();
            }
        };
        badge.setPreferredSize(new Dimension(JBUI.scale(12), JBUI.scale(12)));
        badge.setOpaque(false);
        return badge;
    }

    /**
     * Updates the displayed score, state, and trend.
     *
     * @param score  flow score 0-100
     * @param state  current flow state
     * @param trend  trend value (positive = improving, negative = declining)
     */
    public void updateScore(int score, FlowState state, int trend) {
        this.currentScore = score;
        this.currentState = state;
        
        // Update score
        scoreLabel.setText(String.valueOf(score));
        
        // Update trend
        if (trend > 0) {
            trendLabel.setText("▲ +" + trend);
            trendLabel.setForeground(FLOW_GREEN);
        } else if (trend < 0) {
            trendLabel.setText("▼ " + trend);
            trendLabel.setForeground(STRESS_RED);
        } else {
            trendLabel.setText("");
        }
        
        // Update state badge
        stateLabel.setText(state.name());
        stateBadge.repaint();
        
        // Update score color based on state
        scoreLabel.setForeground(getStateColor(state));
    }

    private Color getStateColor(FlowState state) {
        return switch (state) {
            case FLOW -> FLOW_GREEN;
            case NEUTRAL -> NEUTRAL_AMBER;
            case PROCRASTINATING -> STRESS_RED;
        };
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw rounded background
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
        
        g2.dispose();
        
        super.paintComponent(g);
    }
}
