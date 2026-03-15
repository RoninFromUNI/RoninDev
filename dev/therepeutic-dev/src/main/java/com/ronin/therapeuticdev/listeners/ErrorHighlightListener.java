package com.ronin.therapeuticdev.listeners;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.psi.PsiFile;
import com.ronin.therapeuticdev.services.MetricCollector;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks real-time syntax errors from IntelliJ's code analysis.
 *
 * Syntax errors detected as you type indicate:
 * - Incomplete code (normal during active typing)
 * - Actual mistakes (potential flow disruption)
 * - Recovery patterns (errors clearing indicates progress)
 *
 * This provides real-time error feedback, complementing
 * build-time errors from {@link BuildListener}.
 *
 * Feeds the Errors metric category (25% weight) in FlowDetector.
 *
 * THREADING NOTE:
 * DaemonCodeAnalyzerImpl.getFileLevelHighlights() asserts EDT access,
 * and PSI operations (getPsiFile, getSelectedTextEditor) require a
 * read action. Since SnapshotScheduler calls recordCurrentErrors()
 * from a POOLED_THREAD alarm, we use invokeAndWait to bounce onto
 * the EDT where both requirements are satisfied.
 */
public class ErrorHighlightListener {

    /**
     * Counts current syntax errors in the active editor.
     *
     * MUST be called on the EDT — DaemonCodeAnalyzerImpl asserts this.
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

        // getFileLevelHighlights() asserts EDT — thats why this whole
        // method needs to run on the event dispatch thread
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
     * Called from SnapshotScheduler's persist cycle (background thread).
     *
     * Since countCurrentErrors() touches the daemon analyzer and PSI,
     * both of which require EDT access, we bounce the work onto the
     * EDT with invokeAndWait. This blocks the calling background thread
     * until the EDT finishes — which is fine because:
     *   - the persist cycle only runs once per minute
     *   - the error scan is fast (just reading cached highlights)
     *   - we need the result synchronously before persisting the snapshot
     *
     * using ModalityState.defaultModalityState() so it runs even if a
     * modal dialog is open (unlikely during normal coding, but safe).
     */
    public static void recordCurrentErrors() {
        MetricCollector collector = ApplicationManager.getApplication()
                .getService(MetricCollector.class);

        if (collector == null) {
            return;
        }

        // AtomicInteger lets us pass the result out of the EDT runnable
        // back to the calling background thread
        AtomicInteger totalErrors = new AtomicInteger(0);

        try {
            ApplicationManager.getApplication().invokeAndWait(() -> {
                int count = 0;
                for (Project project : ProjectManager.getInstance().getOpenProjects()) {
                    count += countCurrentErrors(project);
                }
                totalErrors.set(count);
            }, ModalityState.defaultModalityState());
        } catch (ProcessCanceledException e) {
            // must rethrow — the platform needs this to propagate so the IDE
            // can properly cancel operations during shutdown or indexing.
            // swallowing this breaks the cancellation machinery.
            throw e;
        } catch (Exception e) {
            // if the EDT call fails for any other reason (e.g. application
            // shutting down), just record zero rather than crashing the persist cycle
            totalErrors.set(0);
        }

        collector.recSyntaxErrors(totalErrors.get());
    }
}