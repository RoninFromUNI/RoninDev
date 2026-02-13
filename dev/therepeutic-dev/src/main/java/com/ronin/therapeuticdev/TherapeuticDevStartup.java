package com.ronin.therapeuticdev;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.ronin.therapeuticdev.services.SnapshotScheduler;
import org.jetbrains.annotations.NotNull;

public class TherapeuticDevStartup implements StartupActivity.DumbAware {
    @Override
    public void runActivity(@NotNull Project project) {
        SnapshotScheduler scheduler = ApplicationManager.getApplication()
                .getService(SnapshotScheduler.class);
        if (scheduler != null && !scheduler.isRunning()) {
            scheduler.start();
        }
    }
}