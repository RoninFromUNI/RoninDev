package com.ronin.therapeuticdev.listeners;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.ronin.therapeuticdev.services.MetricCollector;
import org.jetbrains.annotations.NotNull;

/**
 * Tracks file switching and focus changes in the editor.
 * 
 * <p>Context switching frequency is a key indicator of flow state:
 * - Low switching (staying in one file) → likely in flow
 * - High switching (jumping between files) → likely distracted or exploring
 * 
 * <p>This feeds the Focus metric category (20% weight) in FlowDetector.
 *
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/editor-basics.html">
 *      IntelliJ Platform SDK - Editor Basics</a>
 */
public class FileActivityListener implements FileEditorManagerListener {

    /**
     * Called when the selected file/editor tab changes.
     * Records a file switch event in MetricCollector.
     *
     * @param event contains old and new file references
     */
    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        VirtualFile oldFile = event.getOldFile();
        VirtualFile newFile = event.getNewFile();
        
        // Only count as a switch if we're actually changing files
        // (not just opening the first file or closing the last)
        if (oldFile != null && newFile != null && !oldFile.equals(newFile)) {
            MetricCollector collector = ApplicationManager.getApplication()
                    .getService(MetricCollector.class);
            
            if (collector != null) {
                collector.recFileChange(System.currentTimeMillis(), newFile.getPath());
            }
        }
    }

    /**
     * Called when a file is opened in the editor.
     * We track this for activity monitoring but don't count it as a "switch".
     */
    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        MetricCollector collector = ApplicationManager.getApplication()
                .getService(MetricCollector.class);
        
        if (collector != null) {
            collector.recFileOpen(file.getPath());
        }
    }

    /**
     * Called when a file is closed in the editor.
     */
    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        // Optional: could track file close events if needed
        // For now, we focus on switches and opens
    }
}
