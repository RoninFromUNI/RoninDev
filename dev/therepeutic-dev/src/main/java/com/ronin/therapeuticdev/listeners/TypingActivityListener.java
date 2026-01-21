package com.ronin.therapeuticdev.listeners;

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.ronin.therapeuticdev.services.MetricCollector;
import org.jetbrains.annotations.NotNull;

/**
 * Intercepts keystrokes in IntelliJ editors and forwards them to MetricCollector.
 * Called on every character typed in any editor window.
 * 
 * Feeds the Typing metric category (30% weight) in FlowDetector.
 */
public class TypingActivityListener extends TypedHandlerDelegate {

    @Override
    public @NotNull Result charTyped(char c, @NotNull Project project, 
                                      @NotNull Editor editor, @NotNull PsiFile file) {
        
        MetricCollector collector = ApplicationManager.getApplication()
                .getService(MetricCollector.class);

        if (collector == null) {
            return Result.CONTINUE;
        }

        long now = System.currentTimeMillis();

        if (c == '\b') {
            collector.recBackspc(now);
        } else {
            collector.recKeystroke(now);
        }

        return Result.CONTINUE;
    }
}
