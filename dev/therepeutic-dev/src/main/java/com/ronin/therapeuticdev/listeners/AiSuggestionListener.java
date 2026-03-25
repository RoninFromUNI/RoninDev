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
 * heuristic detector for ai suggestion acceptances (copilot, jetbrains ai, etc).
 *
 * the idea: if a single DocumentEvent adds >= 40 net characters in one go, that's
 * almost certainly not normal typing. it's either an ai completion acceptance or a
 * paste, both of which are worth tracking. i label the metric as "heuristic" in
 * both the ui and exported csv because it's not definitive — the esm probe asks
 * participants to self-report actual ai tool usage so i have ground truth to
 * compare this against in the analysis.
 *
 * registered as an application-level EditorFactoryListener in plugin.xml.
 * i track which documents already have listeners via a ConcurrentHashMap so i
 * don't double-register when multiple editors share the same document. cleanup
 * happens in editorReleased to avoid memory leaks.
 */
public class AiSuggestionListener implements EditorFactoryListener {

    private static final int AI_THRESHOLD_CHARS = 40;

    // tracks which documents already have a listener attached
    private final Map<Document, DocumentListener> listenerMap = new ConcurrentHashMap<>();

    @Override
    public void editorCreated(@NotNull EditorFactoryEvent event) {
        Document doc = event.getEditor().getDocument();
        // multiple editors can share the same document, one listener is enough
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