package com.ronin.therapeuticdev.ui.components;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.ronin.therapeuticdev.detection.FlowState;

import javax.swing.*;
import java.awt.*;

/**
 * the big score display at the top of the tool window.
 *
 * shows three things: the numeric composite score (0–100), a trend arrow
 * indicating whether the score is improving or declining since last cycle,
 * and a coloured state badge with the FlowState name.
 *
 * all seven FlowState values map to distinct colours so the developer can
 * glance at the badge colour without reading the text. green family for
 * flow states, amber for neutral, orange/red for disrupted/procrastinating,
 * slate for disengaged.
 *
 * the rounded background is painted manually in paintComponent because
 * JBPanel's default rectangular background doesn't support border radius.
 */
public class HeroScoreCard extends JBPanel<HeroScoreCard> {

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
        setPreferredSize(new Dimension(-1, JBUI.scale(115)));

        JBPanel<?> centerPanel = new JBPanel<>(new FlowLayout(FlowLayout.CENTER, 10, 0));
        centerPanel.setOpaque(false);

        scoreLabel = new JBLabel("0");
        scoreLabel.setFont(scoreLabel.getFont().deriveFont(Font.BOLD, 48f));
        scoreLabel.setForeground(JBColor.WHITE);
        centerPanel.add(scoreLabel);

        trendLabel = new JBLabel("");
        trendLabel.setFont(trendLabel.getFont().deriveFont(14f));
        trendLabel.setForeground(FLOW_GREEN);
        centerPanel.add(trendLabel);

        add(centerPanel, BorderLayout.CENTER);

        JBPanel<?> bottomPanel = new JBPanel<>(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setOpaque(false);

        stateBadge = createStateBadge();
        stateLabel = new JBLabel("NEUTRAL");
        stateLabel.setFont(stateLabel.getFont().deriveFont(Font.BOLD, 16f));
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
        badge.setPreferredSize(new Dimension(JBUI.scale(18), JBUI.scale(18)));
        badge.setOpaque(false);
        return badge;
    }

    // score is 0–100, trend is positive (improving) or negative (declining)
    public void updateScore(int score, FlowState state, int trend) {
        this.currentScore = score;
        this.currentState = state;

        scoreLabel.setText(String.valueOf(score));

        if (trend > 0) {
            trendLabel.setText("▲ +" + trend);
            trendLabel.setForeground(FLOW_GREEN);
        } else if (trend < 0) {
            trendLabel.setText("▼ " + trend);
            trendLabel.setForeground(STRESS_RED);
        } else {
            trendLabel.setText("");
        }

        stateLabel.setText(state.name());
        stateLabel.setForeground(getStateColor(state));
        stateBadge.repaint();
        scoreLabel.setForeground(getStateColor(state));
    }

    private Color getStateColor(FlowState state) {
        if (state == null) return NEUTRAL_AMBER;
        return switch (state) {
            case DEEP_FLOW       -> new Color(0x27, 0xAE, 0x60);
            case FLOW            -> FLOW_GREEN;
            case EMERGING        -> new Color(0x82, 0xE0, 0xAA);
            case NEUTRAL         -> NEUTRAL_AMBER;
            case DISRUPTED       -> new Color(0xE6, 0x7E, 0x22);
            case PROCRASTINATING -> STRESS_RED;
            case NOT_IN_FLOW     -> new Color(0x95, 0xA5, 0xA6);
        };
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

        g2.dispose();

        super.paintComponent(g);
    }
}