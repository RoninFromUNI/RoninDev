package com.ronin.therapeuticdev.listeners;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.psi.PsiFile;
import com.ronin.therapeuticdev.services.MetricCollector;

import java.util.List;

/**
 * Tracks real-time syntax errors from IntelliJ's code analysis.
 * 
 * <p>Syntax errors detected as you type indicate:
 * - Incomplete code (normal during active typing)
 * - Actual mistakes (potential flow disruption)
 * - Recovery patterns (errors clearing indicates progress)
 * 
 * <p>This provides real-time error feedback, complementing
 * build-time errors from {@link BuildListener}.
 *
 * <p>Feeds the Errors metric category (25% weight) in FlowDetector.
 */
public class ErrorHighlightListener {

    /**
     * Counts current syntax errors in the active editor.
     * Call this periodically (e.g., every snapshot interval) rather than
     * on every change to avoid performance overhead.
     *
     * @param project the current project
     * @return count of ERROR-severity highlights in the current file
     */
    public static int countCurrentErrors(Project project) {
        if (project == null || project.isDisposed()) {
            return 0;
        }
        
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        Editor editor = editorManager.getSelectedTextEditor();
        
        if (editor == null) {
            return 0;
        }
        
        PsiFile psiFile = com.intellij.psi.PsiDocumentManager
                .getInstance(project)
                .getPsiFile(editor.getDocument());
        
        if (psiFile == null) {
            return 0;
        }
        
        // Get highlights from the daemon code analyzer
        DaemonCodeAnalyzerImpl analyzer = (DaemonCodeAnalyzerImpl) 
                DaemonCodeAnalyzer.getInstance(project);
        
        List<HighlightInfo> highlights = analyzer.getFileLevelHighlights(project, psiFile);
        
        int errorCount = 0;
        for (HighlightInfo info : highlights) {
            if (info.getSeverity() == HighlightSeverity.ERROR) {
                errorCount++;
            }
        }
        
        return errorCount;
    }

    /**
     * Records current error count to MetricCollector.
     * Should be called from the snapshot scheduler.
     */
    public static void recordCurrentErrors() {
        MetricCollector collector = ApplicationManager.getApplication()
                .getService(MetricCollector.class);
        
        if (collector == null) {
            return;
        }
        
        // Check all open projects
        int totalErrors = 0;
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            totalErrors += countCurrentErrors(project);
        }
        
        collector.recSyntaxErrors(totalErrors);
    }
}
