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
 * tracks real-time syntax errors from intellij's daemon code analyzer.
 *
 * this gives me error data between builds — not just at compile time but as the
 * developer types. error introduction and resolution patterns feed the error
 * scoring category (25% weight) in FlowDetector.
 *
 * threading note that cost me a day to debug:
 * DaemonCodeAnalyzerImpl.getFileLevelHighlights() throws an AssertionError if called
 * off the EDT. since SnapshotScheduler's persist cycle runs on a POOLED_THREAD alarm,
 * i have to bounce the error scan onto the EDT with invokeAndWait. this blocks the
 * background thread until the EDT finishes, which is fine because:
 *   - the persist cycle only runs once per minute
 *   - the error scan just reads cached highlights (fast)
 *   - i need the count synchronously before persisting the snapshot
 */
public class ErrorHighlightListener {

    /**
     * counts ERROR-severity highlights in the active editor.
     * MUST be called on the EDT — the daemon analyzer asserts this internally.
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

        // this is the call that requires EDT — getFileLevelHighlights asserts it
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
     * called from SnapshotScheduler's persist cycle (background thread).
     * bounces the actual error scan onto the EDT because of the threading
     * constraint described above.
     *
     * AtomicInteger passes the result out of the EDT runnable back to the
     * calling background thread — it's the lightest way to cross that boundary.
     *
     * using ModalityState.defaultModalityState() so the scan runs even if a
     * modal dialog is open (unlikely during normal coding, but defensive).
     */
    public static void recordCurrentErrors() {
        MetricCollector collector = ApplicationManager.getApplication()
                .getService(MetricCollector.class);

        if (collector == null) {
            return;
        }

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
            // must rethrow — the platform needs this to propagate so the ide
            // can properly cancel operations during shutdown or indexing.
            // swallowing this breaks the cancellation machinery.
            throw e;
        } catch (Exception e) {
            // any other failure (e.g. ide shutting down): record zero
            // rather than crashing the persist cycle
            totalErrors.set(0);
        }

        collector.recSyntaxErrors(totalErrors.get());
    }
}
