package com.ronin.therapeuticdev.listeners;

import com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import com.ronin.therapeuticdev.metrics.MetricCollector;
import org.jetbrains.annotations.NotNull;

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
        return false;
    }
}