package com.ronin.therapeuticdev.listeners;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.ronin.therapeuticdev.services.MetricCollector;
import org.jetbrains.annotations.NotNull;

/**
 * tracks file tab switches and file opens in the editor.
 *
 * context switching frequency is a core signal for the focus metric (20% weight).
 * csikszentmihalyi's "concentration on the task at hand" dimension maps directly
 * to how long a developer stays in one file without jumping around.
 *
 * i only count a "switch" when both oldFile and newFile are non-null and different.
 * opening the first file in an empty editor or closing the last file aren't real
 * context switches — they're session start/end events.
 */
public class FileActivityListener implements FileEditorManagerListener {

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        VirtualFile oldFile = event.getOldFile();
        VirtualFile newFile = event.getNewFile();

        // only count genuine file-to-file switches
        if (oldFile != null && newFile != null && !oldFile.equals(newFile)) {
            MetricCollector collector = ApplicationManager.getApplication()
                    .getService(MetricCollector.class);

            if (collector != null) {
                collector.recFileChange(System.currentTimeMillis(), newFile.getPath());
            }
        }
    }

    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        MetricCollector collector = ApplicationManager.getApplication()
                .getService(MetricCollector.class);

        if (collector != null) {
            collector.recFileOpen(file.getPath());
        }
    }

    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        // not tracking closes currently — switches and opens give enough signal
    }
}
