package com.ronin.therapeuticdev.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.Consumer;
import com.ronin.therapeuticdev.detection.FlowDetectionResult;
import com.ronin.therapeuticdev.detection.FlowState;
import com.ronin.therapeuticdev.metrics.FlowMetrics;
import com.ronin.therapeuticdev.services.SnapshotScheduler;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * Factory for creating the Therapeutic Dev status bar widget.
 *
 * <p>Widget displays:
 * <pre>
 * ┌─────────────────┐
 * │ ● 72 FLOW ████ │
 * └─────────────────┘
 * </pre>
 *
 * <p>Color-coded dot indicates state (green/amber/red).
 * Clicking opens the Tool Window.
 */
public class FlowStatusBarWidgetFactory implements StatusBarWidgetFactory {

    private static final String WIDGET_ID = "TherapeuticDevWidget";

    @Override
    public @NotNull @NonNls String getId() {
        return WIDGET_ID;
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Therapeutic Dev";
    }

    @Override
    public boolean isAvailable(@NotNull Project project) {
        return true;
    }

    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
        return new FlowStatusBarWidget(project);
    }

    @Override
    public void disposeWidget(@NotNull StatusBarWidget widget) {
        // Cleanup handled by widget's dispose method
    }

    @Override
    public boolean canBeEnabledOn(@NotNull StatusBar statusBar) {
        return true;
    }

    /**
     * The actual status bar widget implementation.
     *
     * TODO LATER: Consider IconPresentation for custom colored dot icon.
     * Switched from MultipleTextValuesPresentation to TextPresentation for SDK compatibility.
     */
    private static class FlowStatusBarWidget implements StatusBarWidget,
            StatusBarWidget.TextPresentation,
            SnapshotScheduler.FlowDetectionListener {

        private final Project project;
        private StatusBar statusBar;

        private int currentScore = 0;
        private FlowState currentState = FlowState.NEUTRAL;

        private static final Color FLOW_GREEN = new Color(0x4C, 0xAF, 0x50);
        private static final Color NEUTRAL_AMBER = new Color(0xE5, 0xA8, 0x4B);
        private static final Color STRESS_RED = new Color(0xF4, 0x43, 0x36);

        public FlowStatusBarWidget(Project project) {
            this.project = project;
            registerListener();
        }

        private void registerListener() {
            SnapshotScheduler scheduler = ApplicationManager.getApplication()
                    .getService(SnapshotScheduler.class);

            if (scheduler != null) {
                scheduler.addListener(this);
            }
        }

        @Override
        public void onFlowDetected(FlowDetectionResult result, FlowMetrics metrics) {
            currentScore = (int)(result.getFlowTally() * 100);
            currentState = result.getState();

            // Update widget on EDT
            if (statusBar != null) {
                SwingUtilities.invokeLater(() -> statusBar.updateWidget(WIDGET_ID));
            }
        }

        @Override
        public @NotNull @NonNls String ID() {
            return WIDGET_ID;
        }

        @Override
        public void install(@NotNull StatusBar statusBar) {
            this.statusBar = statusBar;
        }

        @Override
        public void dispose() {
            SnapshotScheduler scheduler = ApplicationManager.getApplication()
                    .getService(SnapshotScheduler.class);

            if (scheduler != null) {
                scheduler.removeListener(this);
            }
        }

        @Override
        public @Nullable WidgetPresentation getPresentation() {
            return this;
        }

        // ========== TextPresentation ==========

        @Override
        public @NotNull String getText() {
            // State indicator dot + score + state name
            String dot = getStateDot(currentState);
            return String.format("%s %d %s", dot, currentScore, currentState.name());
        }

        @Override
        public @Nullable String getTooltipText() {
            String stateDesc = getStateDescription(currentState);
            return String.format("Flow Score: %d\nState: %s\n%s\n\nClick to open Therapeutic Dev",
                    currentScore, currentState.name(), stateDesc);
        }

        @Override
        public @Nullable Consumer<MouseEvent> getClickConsumer() {
            return e -> {
                // Open the tool window on click
                ToolWindowManager manager = ToolWindowManager.getInstance(project);
                var toolWindow = manager.getToolWindow("Therapeutic Dev");
                if (toolWindow != null) {
                    toolWindow.show();
                }
            };
        }

        @Override
        public float getAlignment() {
            return Component.CENTER_ALIGNMENT;
        }

        // ========== Helper Methods ==========

        /**
         * Returns emoji dot for state display.
         * Uses if-else to avoid switch expression exhaustiveness issues.
         */
        private String getStateDot(FlowState state) {
            if (state == null) {
                return "○";
            } else if (state == FlowState.FLOW) {
                return "🟢";
            } else if (state == FlowState.PROCRASTINATING) {
                return "🔴";
            } else {
                // NEUTRAL fallback
                return "🟡";
            }
        }

        /**
         * Returns human-readable state description.
         * Uses if-else to avoid switch expression exhaustiveness issues.
         */
        private String getStateDescription(FlowState state) {
            if (state == null) {
                return "Unknown state";
            } else if (state == FlowState.FLOW) {
                return "Deep work - don't interrupt";
            } else if (state == FlowState.PROCRASTINATING) {
                return "High distraction / errors detected";
            } else {
                // NEUTRAL fallback
                return "Normal activity";
            }
        }
    }
}