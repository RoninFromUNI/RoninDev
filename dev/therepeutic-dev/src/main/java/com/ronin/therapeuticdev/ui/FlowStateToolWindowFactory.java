package com.ronin.therapeuticdev.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating the Therapeutic Dev tool window.
 * 
 * <p>Registered in plugin.xml, instantiated by IntelliJ when
 * the tool window is first accessed.
 *
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/tool-windows.html">
 *      IntelliJ Platform SDK - Tool Windows</a>
 */
public class FlowStateToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
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
