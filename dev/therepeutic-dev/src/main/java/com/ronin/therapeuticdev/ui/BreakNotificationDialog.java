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
 * the break suggestion modal — appears when BreakManager decides it's time.
 *
 * non-modal (setModal false) so the developer isn't forced to respond before
 * they can interact with the ide. three actions: Take Break resets flow tracking,
 * Snooze 15min delays the next notification, Dismiss just logs and moves on.
 *
 * the stats card shows how long they've been in flow, which gives context for
 * why the break is being suggested. "you've been in flow for 1h 42m" is more
 * persuasive than a generic "take a break" message.
 *
 * i use DialogWrapper rather than a raw JDialog because it integrates with
 * intellij's window management, theming, and action system automatically.
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

    public interface BreakCallback {
        void onTakeBreak();
        void onSnooze(int minutes);
        void onDismiss();
    }

    public BreakNotificationDialog(@Nullable Project project,
                                   long flowDurationMs,
                                   String triggerReason,
                                   BreakCallback callback) {
        super(project, false);
        this.flowDurationMs = flowDurationMs;
        this.triggerReason = triggerReason;
        this.callback = callback;

        setTitle("Therapeutic Dev");
        setModal(false);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JBPanel<?> mainPanel = new JBPanel<>(new BorderLayout());
        mainPanel.setBackground(JBColor.namedColor("Panel.background", new Color(0x2B, 0x2B, 0x2B)));
        mainPanel.setBorder(JBUI.Borders.empty(16));
        mainPanel.setPreferredSize(new Dimension(JBUI.scale(320), JBUI.scale(200)));

        JBLabel headerLabel = new JBLabel("Time for a break?");
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 18f));
        headerLabel.setForeground(JBColor.WHITE);
        headerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerLabel.setBorder(JBUI.Borders.emptyBottom(16));

        mainPanel.add(headerLabel, BorderLayout.NORTH);

        JBPanel<?> statsCard = createStatsCard();
        mainPanel.add(statsCard, BorderLayout.CENTER);

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

        JBLabel descLabel = new JBLabel("You've been in flow for");
        descLabel.setFont(descLabel.getFont().deriveFont(12f));
        descLabel.setForeground(JBColor.namedColor("Label.foreground", new Color(0xA9, 0xB7, 0xC6)));
        descLabel.setHorizontalAlignment(SwingConstants.CENTER);

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

    // resets flow tracking so the next break suggestion starts fresh
    private class TakeBreakAction extends DialogWrapperAction {
        public TakeBreakAction() {
            super("Take Break");
            putValue(DEFAULT_ACTION, true);
        }

        @Override
        protected void doAction(java.awt.event.ActionEvent e) {
            if (callback != null) callback.onTakeBreak();
            close(OK_EXIT_CODE);
        }
    }

    // delays the next notification by 15 minutes
    private class SnoozeAction extends DialogWrapperAction {
        public SnoozeAction() { super("Snooze 15min"); }

        @Override
        protected void doAction(java.awt.event.ActionEvent e) {
            if (callback != null) callback.onSnooze(15);
            close(CANCEL_EXIT_CODE);
        }
    }

    // just closes, no state change
    private class DismissAction extends DialogWrapperAction {
        public DismissAction() { super("Dismiss"); }

        @Override
        protected void doAction(java.awt.event.ActionEvent e) {
            if (callback != null) callback.onDismiss();
            close(CANCEL_EXIT_CODE);
        }
    }

    // convenience static method so callers don't have to deal with EDT dispatch
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