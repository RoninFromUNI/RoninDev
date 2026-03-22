package com.ronin.therapeuticdev.listeners;

import com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import com.ronin.therapeuticdev.services.MetricCollector;
import org.jetbrains.annotations.NotNull;

/**
 * intercepts backspace/delete keystrokes for tracking the correction ratio.
 *
 * this exists as a separate class because intellij's TypedHandlerDelegate only
 * receives printable characters — backspace events are routed through the action
 * system instead, so a BackspaceHandlerDelegate is required to catch them.
 * i discovered this the hard way when the '\b' check in TypingActivityListener
 * never fired. i need to write about this platform constraint in the dissertation.
 *
 * high backspace ratios feed into the typing score (30% weight) via FlowDetector.
 * the correction ratio penalises heavy deletion because it suggests the developer
 * is struggling with syntax, iterating on approach, or second-guessing their code.
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
        // returning false lets intellij continue with normal backspace processing
        // i'm only observing, not intercepting
        return false;
    }
}
