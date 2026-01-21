package com.ronin.therapeuticdev.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.TimeUnit;

/**
 * Modal dialog for break suggestions.
 * 
 * <p>Triggered when:
 * - Flow duration exceeds threshold (e.g., 90 min)
 * - Flow score drops significantly (exit flow state)
 * - High context switching detected (stress indicator)
 * 
 * <p>Layout:
 * <pre>
 * ┌─────────────────────────────────┐
 * │      Time for a break?         │
 * │  ┌───────────────────────────┐ │
 * │  │ You've been in flow for   │ │
 * │  │        1h 42m             │ │
 * │  └───────────────────────────┘ │
 * │  Suggested: exceeded 90 min    │
 * │                                │
 * │ [Take Break] [Snooze] [Dismiss]│
 * └─────────────────────────────────┘
 * </pre>
 */
public class BreakNotificationDialog extends DialogWrapper {

    private static final Color CARD_BG = new Color(0x25, 0x25, 0x25);
    private static final Color CARD_BORDER = new Color(0x3C, 0x3F, 0x41);
    private static final Color ACCENT = new Color(0xE5, 0xA8, 0x4B);
    private static final Color MUTED = new Color(0x6B, 0x73, 0x7C);
    private static final Color GREEN = new Color(0x4C, 0xAF, 0x50);

    private final long flowDurationMs;
    private final String triggerReason;
    private final BreakCallback callback;
    
    /**
     * Callback interface for break dialog actions.
     */
    public interface BreakCallback {
        void onTakeBreak();
        void onSnooze(int minutes);
        void onDismiss();
    }

    /**
     * Creates a break notification dialog.
     *
     * @param project        current project
     * @param flowDurationMs how long the user has been in flow (milliseconds)
     * @param triggerReason  why the break was suggested
     * @param callback       callback for handling user actions
     */
    public BreakNotificationDialog(@Nullable Project project, 
                                   long flowDurationMs,
                                   String triggerReason,
                                   BreakCallback callback) {
        super(project, false);
        this.flowDurationMs = flowDurationMs;
        this.triggerReason = triggerReason;
        this.callback = callback;
        
        setTitle("Therapeutic Dev");
        setModal(false); // Non-blocking
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JBPanel<?> mainPanel = new JBPanel<>(new BorderLayout());
        mainPanel.setBackground(JBColor.namedColor("Panel.background", new Color(0x2B, 0x2B, 0x2B)));
        mainPanel.setBorder(JBUI.Borders.empty(16));
        mainPanel.setPreferredSize(new Dimension(JBUI.scale(320), JBUI.scale(200)));
        
        // === HEADER ===
        JBLabel headerLabel = new JBLabel("Time for a break?");
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 18f));
        headerLabel.setForeground(JBColor.WHITE);
        headerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerLabel.setBorder(JBUI.Borders.emptyBottom(16));
        
        mainPanel.add(headerLabel, BorderLayout.NORTH);
        
        // === STATS CARD ===
        JBPanel<?> statsCard = createStatsCard();
        mainPanel.add(statsCard, BorderLayout.CENTER);
        
        // === REASON TEXT ===
        JBLabel reasonLabel = new JBLabel("Suggested: " + triggerReason);
        reasonLabel.setFont(reasonLabel.getFont().deriveFont(11f));
        reasonLabel.setForeground(MUTED);
        reasonLabel.setHorizontalAlignment(SwingConstants.CENTER);
        reasonLabel.setBorder(JBUI.Borders.empty(12, 0, 0, 0));
        
        JBPanel<?> bottomPanel = new JBPanel<>(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.add(reasonLabel, BorderLayout.NORTH);
        
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        return mainPanel;
    }

    private JBPanel<?> createStatsCard() {
        JBPanel<?> card = new JBPanel<>(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1),
                JBUI.Borders.empty(16)
        ));
        
        // "You've been in flow for" text
        JBLabel descLabel = new JBLabel("You've been in flow for");
        descLabel.setFont(descLabel.getFont().deriveFont(12f));
        descLabel.setForeground(JBColor.namedColor("Label.foreground", new Color(0xA9, 0xB7, 0xC6)));
        descLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Duration display
        String duration = formatDuration(flowDurationMs);
        JBLabel durationLabel = new JBLabel(duration);
        durationLabel.setFont(durationLabel.getFont().deriveFont(Font.BOLD, 24f));
        durationLabel.setForeground(ACCENT);
        durationLabel.setHorizontalAlignment(SwingConstants.CENTER);
        durationLabel.setBorder(JBUI.Borders.emptyTop(8));
        
        JBPanel<?> centerPanel = new JBPanel<>();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        durationLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        centerPanel.add(descLabel);
        centerPanel.add(durationLabel);
        
        card.add(centerPanel, BorderLayout.CENTER);
        
        return card;
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{
                new TakeBreakAction(),
                new SnoozeAction(),
                new DismissAction()
        };
    }

    private String formatDuration(long durationMs) {
        long hours = TimeUnit.MILLISECONDS.toHours(durationMs);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60;
        
        if (hours > 0) {
            return String.format("%dh %02dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }

    /**
     * "Take Break" action - primary button.
     */
    private class TakeBreakAction extends DialogWrapperAction {
        
        public TakeBreakAction() {
            super("Take Break");
            putValue(DEFAULT_ACTION, true);
        }
        
        @Override
        protected void doAction(java.awt.event.ActionEvent e) {
            if (callback != null) {
                callback.onTakeBreak();
            }
            close(OK_EXIT_CODE);
        }
    }

    /**
     * "Snooze" action - delays the notification.
     */
    private class SnoozeAction extends DialogWrapperAction {
        
        public SnoozeAction() {
            super("Snooze 15min");
        }
        
        @Override
        protected void doAction(java.awt.event.ActionEvent e) {
            if (callback != null) {
                callback.onSnooze(15);
            }
            close(CANCEL_EXIT_CODE);
        }
    }

    /**
     * "Dismiss" action - ignores the suggestion.
     */
    private class DismissAction extends DialogWrapperAction {
        
        public DismissAction() {
            super("Dismiss");
        }
        
        @Override
        protected void doAction(java.awt.event.ActionEvent e) {
            if (callback != null) {
                callback.onDismiss();
            }
            close(CANCEL_EXIT_CODE);
        }
    }

    /**
     * Shows a break notification dialog.
     *
     * @param project        current project
     * @param flowDurationMs flow duration in milliseconds
     * @param reason         trigger reason
     * @param callback       action callback
     */
    public static void show(@Nullable Project project,
                           long flowDurationMs,
                           String reason,
                           BreakCallback callback) {
        SwingUtilities.invokeLater(() -> {
            BreakNotificationDialog dialog = new BreakNotificationDialog(
                    project, flowDurationMs, reason, callback);
            dialog.show();
        });
    }
}
