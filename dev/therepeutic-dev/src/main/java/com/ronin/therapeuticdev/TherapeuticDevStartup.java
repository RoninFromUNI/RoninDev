package com.ronin.therapeuticdev;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.ronin.therapeuticdev.services.SnapshotScheduler;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Plugin startup activity - runs when a project opens.
 * 
 * <p>Responsibilities:
 * - Ensure SnapshotScheduler service is initialized
 * - Start periodic flow detection
 * 
 * <p>Registered in plugin.xml as postStartupActivity.
 */
public class TherapeuticDevStartup implements ProjectActivity {

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        // Initialize the snapshot scheduler (starts periodic flow detection)
        SnapshotScheduler scheduler = ApplicationManager.getApplication()
                .getService(SnapshotScheduler.class);
        
        if (scheduler != null) {
            // Scheduler is now active - it will handle periodic snapshots
            System.out.println("Therapeutic Dev: Plugin initialized for project " + project.getName());
        }
        
        return Unit.INSTANCE;
    }
}