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
     */
    private static class FlowStatusBarWidget implements StatusBarWidget, 
            StatusBarWidget.MultipleTextValuesPresentation,
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

        // ========== MultipleTextValuesPresentation ==========

        @Override
        public @Nullable String getSelectedValue() {
            // Return the text to display
            return String.format("● %d %s", currentScore, currentState.name());
        }

        @Override
        public @Nullable String getTooltipText() {
            String stateDesc = switch (currentState) {
                case FLOW -> "Deep work - don't interrupt";
                case NEUTRAL -> "Normal activity";
                case PROCRASTINATING -> "High distraction / errors detected";
            };
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
        public @Nullable java.util.List<String> getValues() {
            return null; // Not used for simple display
        }

        @Override
        public @Nullable Icon getIcon() {
            // Return a colored icon based on state
            return new StateIcon(currentState);
        }
    }

    /**
     * Simple colored circle icon for the status bar.
     */
    private static class StateIcon implements Icon {
        
        private static final int SIZE = 10;
        private final FlowState state;
        
        public StateIcon(FlowState state) {
            this.state = state;
        }
        
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            Color color = switch (state) {
                case FLOW -> new Color(0x4C, 0xAF, 0x50);
                case NEUTRAL -> new Color(0xE5, 0xA8, 0x4B);
                case PROCRASTINATING -> new Color(0xF4, 0x43, 0x36);
            };
            
            g2.setColor(color);
            g2.fillOval(x, y, SIZE, SIZE);
            
            g2.dispose();
        }
        
        @Override
        public int getIconWidth() {
            return SIZE;
        }
        
        @Override
        public int getIconHeight() {
            return SIZE;
        }
    }
}
