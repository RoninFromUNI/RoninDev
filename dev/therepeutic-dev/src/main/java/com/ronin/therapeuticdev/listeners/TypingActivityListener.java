package com.ronin.therapeuticdev.listeners;

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.ronin.therapeuticdev.services.MetricCollector;
import org.jetbrains.annotations.NotNull;

/**
 * intercepts every printable character typed in any editor window.
 *
 * feeds the typing metric category (30% weight) — the highest weighted category
 * in the algorithm because keyboard input is the primary output modality of a
 * developer in flow.
 *
 * important: the backspace check (c == '\b') in charTyped will never actually fire.
 * intellij routes backspace through the action system, not the typed handler pipeline.
 * i left the check in as a safety net but the real backspace capture happens in
 * BackspaceHandler via BackspaceHandlerDelegate. i should probably remove this
 * dead code but it doesn't hurt anything.
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
            // this branch is effectively dead code — see class javadoc
            collector.recBackspc(now);
        } else {
            collector.recKeystroke(now);
        }

        return Result.CONTINUE;
    }
}
