package com.ronin.therapeuticdev.listeners;

import com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import com.ronin.therapeuticdev.services.MetricCollector;
import org.jetbrains.annotations.NotNull;

/**
 * Intercepts backspace/delete keystrokes for tracking correction patterns.
 * 
 * High backspace ratios may indicate:
 * - Struggling with syntax
 * - Iterating on approach
 * - Normal editing patterns
 * 
 * Feeds the Typing metric category (30% weight) in FlowDetector.
 */
public class BackspaceHandler extends BackspaceHandlerDelegate {

    @Override
    public void beforeCharDeleted(char c, @NotNull PsiFile file, @NotNull Editor editor) {
        MetricCollector collector = ApplicationManager.getApplication()
                .getService(MetricCollector.class);

        if (collector != null) {
            collector.recBackspc(System.currentTimeMillis());
        }
    }

    @Override
    public boolean charDeleted(char c, @NotNull PsiFile file, @NotNull Editor editor) {
        // Return false to allow normal backspace processing
        return false;
    }
}
