package com.ronin.therapeuticdev.ui.tabs;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBPanel;

import javax.swing.*;
import java.awt.*;

/**
 * combined tab — architecture graph on top, project file-type composition donut below.
 *
 * split vertically with a draggable divider (55/45 default split). the graph shows
 * classes grouped by package with dependency arrows; the donut shows the file type
 * breakdown similar to github's language bar.
 *
 * i combined these into one tab because they're both project-level views that don't
 * need to be visible simultaneously with the per-interval metrics. keeping them in
 * separate tabs would have cluttered the tab bar.
 */
public class GraphAndProjectTab extends JBPanel<GraphAndProjectTab> {

    private final GraphTabPanel graphPanel;
    private final ProjectCompositionPanel compositionPanel;

    public GraphAndProjectTab(Project project) {
        setLayout(new BorderLayout());
        setBackground(JBColor.namedColor("Panel.background", new Color(0x2B, 0x2B, 0x2B)));

        graphPanel      = new GraphTabPanel(project);
        compositionPanel = new ProjectCompositionPanel(project);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, graphPanel, compositionPanel);
        split.setResizeWeight(0.55);   // graph gets ~55 % on resize
        split.setDividerSize(5);
        split.setBorder(null);
        split.setOpaque(false);

        // Defer setDividerLocation until the panel is actually laid out
        addHierarchyListener(e -> {
            if (isShowing() && split.getDividerLocation() <= 1) {
                split.setDividerLocation(0.55);
            }
        });

        add(split, BorderLayout.CENTER);
    }

    /** Forwards "currently open file" highlight to the graph canvas. */
    public void setCurrentFile(String filePath) {
        graphPanel.setCurrentFile(filePath);
    }
}
