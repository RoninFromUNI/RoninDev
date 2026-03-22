package com.ronin.therapeuticdev.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.ronin.therapeuticdev.services.ParticipantSession;
import org.jetbrains.annotations.NotNull;

/**
 * factory that intellij calls when the tool window is first accessed.
 *
 * the key decision here: if no participant ID is set, i show ParticipantSetupPanel
 * instead of the main dashboard. once the participant enters their ID, the setup
 * panel's onConfirmed callback swaps in FlowStatePanel. this gating ensures every
 * study session has a participant ID before any metrics start being namespaced.
 */
public class FlowStateToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        if (ParticipantSession.getInstance().hasParticipant()) {
            showFlowStatePanel(project, toolWindow);
        } else {
            showSetupPanel(project, toolWindow);
        }
    }

    private void showSetupPanel(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ParticipantSetupPanel setupPanel = new ParticipantSetupPanel(() -> {
            toolWindow.getContentManager().removeAllContents(true);
            showFlowStatePanel(project, toolWindow);
        });

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(setupPanel, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    private void showFlowStatePanel(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        FlowStatePanel flowStatePanel = new FlowStatePanel(project);

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(
                flowStatePanel.getContent(),
                "",
                false
        );
        toolWindow.getContentManager().addContent(content);
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        // Always available for any project
        return true;
    }
}
