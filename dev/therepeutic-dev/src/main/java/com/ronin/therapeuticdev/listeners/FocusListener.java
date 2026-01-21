package com.ronin.therapeuticdev.listeners;

import com.intellij.openapi.application.ApplicationActivationListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.wm.IdeFrame;
import com.ronin.therapeuticdev.services.MetricCollector;
import org.jetbrains.annotations.NotNull;

/**
 * Tracks when the IDE gains or loses focus (user switches to another app).
 * 
 * <p>Focus loss events indicate:
 * - Alt-tabbing to browser/docs → possibly researching (acceptable)
 * - Frequent focus switching → likely distracted
 * - Extended focus loss → break or interruption
 * 
 * <p>Feeds the Focus metric category (20% weight) in FlowDetector.
 *
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/plugin-listeners.html">
 *      IntelliJ Platform SDK - Plugin Listeners</a>
 */
public class FocusListener implements ApplicationActivationListener {

    private long lastDeactivationTime = 0;

    /**
     * Called when IntelliJ gains focus (user switches back to IDE).
     */
    @Override
    public void applicationActivated(@NotNull IdeFrame ideFrame) {
        MetricCollector collector = ApplicationManager.getApplication()
                .getService(MetricCollector.class);
        
        if (collector == null) {
            return;
        }
        
        long now = System.currentTimeMillis();
        
        // Calculate how long we were away
        if (lastDeactivationTime > 0) {
            long awayDurationMs = now - lastDeactivationTime;
            collector.recFocusRegained(now, awayDurationMs);
        }
        
        lastDeactivationTime = 0;
    }

    /**
     * Called when IntelliJ loses focus (user switches to another app).
     */
    @Override
    public void applicationDeactivated(@NotNull IdeFrame ideFrame) {
        MetricCollector collector = ApplicationManager.getApplication()
                .getService(MetricCollector.class);
        
        if (collector == null) {
            return;
        }
        
        lastDeactivationTime = System.currentTimeMillis();
        collector.recFocusLost(lastDeactivationTime);
    }
}
