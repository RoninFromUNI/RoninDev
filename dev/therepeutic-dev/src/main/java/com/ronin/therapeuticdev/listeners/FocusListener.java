package com.ronin.therapeuticdev.listeners;

import com.intellij.openapi.application.ApplicationActivationListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.wm.IdeFrame;
import com.ronin.therapeuticdev.services.MetricCollector;
import org.jetbrains.annotations.NotNull;

/**
 * tracks when the ide gains or loses os-level focus (user alt-tabs away).
 *
 * this feeds two metrics:
 *   - focusLossCount for the focus score (20% weight) — how many times
 *     the developer left the ide this interval
 *   - ideFocusPct for the context score (10% weight) — what proportion
 *     of the interval was spent in the ide vs other applications
 *
 * frequent alt-tabbing strongly correlates with disrupted flow in gloria mark's
 * interruption research. but i don't penalise a single long absence as harshly
 * as frequent short ones because a single long absence might be a legitimate
 * research break, while rapid switching suggests distraction.
 */
public class FocusListener implements ApplicationActivationListener {

    private long lastDeactivationTime = 0;

    /**
     * ide gained focus — developer switched back.
     * i calculate how long they were away and record the return event.
     */
    @Override
    public void applicationActivated(@NotNull IdeFrame ideFrame) {
        MetricCollector collector = ApplicationManager.getApplication()
                .getService(MetricCollector.class);

        if (collector == null) {
            return;
        }

        long now = System.currentTimeMillis();

        if (lastDeactivationTime > 0) {
            long awayDurationMs = now - lastDeactivationTime;
            collector.recFocusRegained(now, awayDurationMs);
        }

        lastDeactivationTime = 0;
    }

    /**
     * ide lost focus — developer switched to another application.
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
