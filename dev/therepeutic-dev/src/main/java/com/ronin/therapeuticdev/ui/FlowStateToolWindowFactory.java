package com.ronin.therapeuticdev.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.ronin.therapeuticdev.services.ParticipantSession;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating the Therapeutic Dev tool window.
 *
 * <p>Registered in plugin.xml, instantiated by IntelliJ when
 * the tool window is first accessed.
 *
 * <p>If no participant ID has been set, shows ParticipantSetupPanel first.
 * Once confirmed, swaps to the main FlowStatePanel.
 *
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/tool-windows.html">
 *      IntelliJ Platform SDK - Tool Windows</a>
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
