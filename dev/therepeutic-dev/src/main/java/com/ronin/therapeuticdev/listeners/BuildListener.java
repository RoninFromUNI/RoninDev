package com.ronin.therapeuticdev.listeners;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompilationStatusListener;
import com.intellij.openapi.compiler.CompileContext;
import com.ronin.therapeuticdev.services.MetricCollector;
import org.jetbrains.annotations.NotNull;

/**
 * tracks build/compilation results for the builds metric category (15% weight).
 *
 * build patterns are a strong behavioural signal:
 *   - consecutive successes suggest the developer is in a productive rhythm
 *   - consecutive failures suggest they're stuck or debugging something hard
 *   - quick recovery after failure suggests good debugging flow
 *
 * i also forward compilation error counts to MetricCollector so the error
 * scoring category (25% weight) has access to compile-time error data in
 * addition to the real-time syntax errors from ErrorHighlightListener.
 */
public class BuildListener implements CompilationStatusListener {

    /**
     * fires when a compilation/build process finishes.
     * aborted builds are ignored — they don't represent a meaningful signal.
     */
    @Override
    public void compilationFinished(boolean aborted, int errors, int warnings,
                                    @NotNull CompileContext compileContext) {

        if (aborted) {
            return;
        }

        MetricCollector collector = ApplicationManager.getApplication()
                .getService(MetricCollector.class);

        if (collector == null) {
            return;
        }

        long now = System.currentTimeMillis();
        boolean success = (errors == 0);

        collector.recBuildResult(now, success, errors, warnings);

        // forward compilation errors to the error metric category as well
        if (errors > 0) {
            collector.recCompilationErrors(errors);
        }
    }

    /**
     * auto-make (incremental background compilation) finished.
     * i treat it identically to a full build — the behavioural signal is the same.
     */
    @Override
    public void automakeCompilationFinished(int errors, int warnings,
                                            @NotNull CompileContext compileContext) {
        compilationFinished(false, errors, warnings, compileContext);
    }
}
