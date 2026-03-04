package com.ronin.therapeuticdev.listeners;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.ronin.therapeuticdev.services.MetricCollector;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Detects likely AI suggestion acceptances via a document-change heuristic.
 *
 * A single DocumentEvent that adds ≥ 40 net characters is unlikely to be normal
 * typed input and is counted as a probable AI acceptance (Copilot, JetBrains AI,
 * or paste). The metric is labelled heuristic in both the UI and exported data;
 * the ESM probe asks participants to self-report actual AI tool usage per interval.
 *
 * Registered as an application-level EditorFactoryListener in plugin.xml.
 * Uses editorReleased to clean up document listeners and avoid memory leaks.
 */
public class AiSuggestionListener implements EditorFactoryListener {

    private static final int AI_THRESHOLD_CHARS = 40;

    /** Maps documents to their registered listener for proper cleanup. */
    private final Map<Document, DocumentListener> listenerMap = new ConcurrentHashMap<>();

    @Override
    public void editorCreated(@NotNull EditorFactoryEvent event) {
        Document doc = event.getEditor().getDocument();
        // One listener per document is sufficient — multiple editors may share the same doc
        if (listenerMap.containsKey(doc)) return;

        DocumentListener listener = new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent e) {
                int delta = e.getNewLength() - e.getOldLength();
                if (delta >= AI_THRESHOLD_CHARS) {
                    MetricCollector collector = ApplicationManager.getApplication()
                            .getService(MetricCollector.class);
                    if (collector != null) {
                        collector.recAiSuggestionAccepted(System.currentTimeMillis());
                    }
                }
            }
        };

        doc.addDocumentListener(listener);
        listenerMap.put(doc, listener);
    }

    @Override
    public void editorReleased(@NotNull EditorFactoryEvent event) {
        Document doc = event.getEditor().getDocument();
        DocumentListener listener = listenerMap.remove(doc);
        if (listener != null) {
            doc.removeDocumentListener(listener);
        }
    }
}
