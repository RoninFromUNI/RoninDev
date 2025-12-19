package com.ronin.therapeuticdev.listeners;

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.ronin.therapeuticdev.metrics.MetricCollector;
import org.jetbrains.annotations.NotNull;

public class TypActivityListener extends TypedHandlerDelegate
{
    //helps to intercept keystrokes in intelliJ and forwards them to metriccollector
    //calls on every character typed specifcially in any editor window

    @Override

    public @NotNull Result charTyped(char c, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file)
    {
        MetricCollector collector = ApplicationManager.getApplication()
        .getService(MetricCollector.class);

        long now = System.currentTimeMillis();

        if(c=='\b')
        {
            collector.recBackspc(now);
        } else
        {
            collector.recKeystroke(now);
        }

        return Result.CONTINUE;
    }
}
