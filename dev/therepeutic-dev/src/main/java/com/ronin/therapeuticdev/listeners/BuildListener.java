package com.ronin.therapeuticdev.listeners;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompilationStatusListener;
import com.intellij.openapi.compiler.CompileContext;
import com.ronin.therapeuticdev.services.MetricCollector;
import org.jetbrains.annotations.NotNull;

/**
 * Tracks build/compilation results.
 * 
 * <p>Build success/failure patterns indicate developer state:
 * - Consecutive successful builds → likely in flow, code is working
 * - Consecutive failures → likely struggling, potential stress
 * - Quick recovery after failure → good debugging flow
 * 
 * <p>Feeds the Builds metric category (15% weight) in FlowDetector.
 *
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/compiler.html">
 *      IntelliJ Platform SDK - Compiler</a>
 */
public class BuildListener implements CompilationStatusListener {

    /**
     * Called when a compilation/build process completes.
     *
     * @param aborted true if the compilation was cancelled
     * @param errors  number of compilation errors
     * @param warnings number of compilation warnings
     * @param compileContext context containing detailed build information
     */
    @Override
    public void compilationFinished(boolean aborted, int errors, int warnings, 
                                    @NotNull CompileContext compileContext) {
        
        if (aborted) {
            // Build was cancelled, don't count it
            return;
        }
        
        MetricCollector collector = ApplicationManager.getApplication()
                .getService(MetricCollector.class);
        
        if (collector == null) {
            return;
        }
        
        long now = System.currentTimeMillis();
        boolean success = (errors == 0);
        
        // Record the build result
        collector.recBuildResult(now, success, errors, warnings);
        
        // Also record compilation errors for the error metric category
        if (errors > 0) {
            collector.recCompilationErrors(errors);
        }
    }

    /**
     * Called when compilation starts.
     * Could be used to track build duration in the future.
     */
    @Override
    public void automakeCompilationFinished(int errors, int warnings, 
                                            @NotNull CompileContext compileContext) {
        // Auto-make (incremental background compilation) finished
        // Treat the same as regular compilation
        compilationFinished(false, errors, warnings, compileContext);
    }
}
