package com.ronin.therapeuticdev.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.ronin.therapeuticdev.detection.FlowDetectionResult;
import com.ronin.therapeuticdev.services.MetricCollector;
import com.ronin.therapeuticdev.storage.MetricRepository;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;

/**
 * the esm self-report dialog — a 7-item flow state scale adapted from jackson & marsh (1996).
 *
 * each item is rated 1–5 (strongly disagree to strongly agree). the items are adapted
 * from the original flow state scale to fit software development context — "the challenge
 * of this task matches my programming skill level" rather than the generic version.
 *
 * non-modal so it doesn't block the developer. they can dismiss it and keep coding.
 * responses are saved to the esm_responses table in MetricRepository with a foreign
 * key linking to the nearest snapshot — that link is how i pair subjective self-report
 * data with the algorithm's classification at the same moment.
 *
 * i also ask about ai tool usage (checkbox + text field for which tool) because the
 * research question explicitly addresses ai-augmented development. the heuristic in
 * AiSuggestionListener gives me one signal; the self-report gives me ground truth.
 */
public class EsmProbeDialog extends DialogWrapper {

    private static final Logger LOG = Logger.getInstance(EsmProbeDialog.class);

    private static final String[] ITEMS = {
        "The difficulty of this task matches my programming skill level",
        "Coding is happening automatically — not thinking about how I do it",
        "I know clearly what I want to accomplish next",
        "I can tell whether I am coding well or not",
        "I am completely focused on what I am coding",
        "I feel in control of what I am coding",
        "I am coding for the enjoyment of it, not just to finish a task"
    };

    private static final String[] SCALE_LABELS = {
        "1\nStrongly\nDisagree", "2", "3\nNeutral", "4", "5\nStrongly\nAgree"
    };

    private final FlowDetectionResult triggerResult;
    private final Instant triggeredAt = Instant.now();
    private final ButtonGroup[] itemGroups = new ButtonGroup[ITEMS.length];

    private JBCheckBox  usingAiCheck;
    private JBTextField aiToolNameField;
    private JTextArea   qualNoteField;

    public EsmProbeDialog(@Nullable Project project, FlowDetectionResult triggerResult) {
        super(project, false); // non-modal
        this.triggerResult = triggerResult;
        setTitle("How focused are you right now?");
        setOKButtonText("Submit");
        setCancelButtonText("Skip");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 8, 4, 8);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill   = GridBagConstraints.HORIZONTAL;

        // Header
        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 6;
        JLabel header = new JLabel("<html><b>Quick check-in (study data — takes ~30 seconds)</b></html>");
        panel.add(header, gc);

        gc.gridy = 1;
        panel.add(new JLabel("<html>Detected state: <b>" +
                triggerResult.getState().name().replace('_', ' ') +
                "</b> · Score: " + (int)(triggerResult.getFlowTally() * 100) + "/100</html>"), gc);

        // Scale header
        gc.gridy = 2; gc.gridwidth = 1;
        gc.gridx = 1;
        for (int s = 1; s <= 5; s++) {
            panel.add(new JBLabel(String.valueOf(s)), gc);
            gc.gridx++;
        }

        // Item rows
        for (int i = 0; i < ITEMS.length; i++) {
            gc.gridy   = 3 + i;
            gc.gridx   = 0;
            gc.gridwidth = 1;
            panel.add(new JBLabel("<html><small>" + ITEMS[i] + "</small></html>"), gc);

            itemGroups[i] = new ButtonGroup();
            for (int s = 1; s <= 5; s++) {
                gc.gridx = s;
                JRadioButton rb = new JRadioButton();
                rb.setActionCommand(String.valueOf(s));
                itemGroups[i].add(rb);
                panel.add(rb, gc);
            }
        }

        int nextRow = 3 + ITEMS.length;

        // AI tool usage
        gc.gridy = nextRow++; gc.gridx = 0; gc.gridwidth = 6;
        usingAiCheck = new JBCheckBox("I am using an AI coding assistant right now");
        panel.add(usingAiCheck, gc);

        gc.gridy = nextRow++;
        JPanel aiPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        aiPanel.add(new JBLabel("Which tool?  "));
        aiToolNameField = new JBTextField("e.g. Copilot, JetBrains AI", 20);
        aiToolNameField.setEnabled(false);
        aiPanel.add(aiToolNameField);
        panel.add(aiPanel, gc);
        usingAiCheck.addActionListener(e -> aiToolNameField.setEnabled(usingAiCheck.isSelected()));

        // Qualitative note
        gc.gridy = nextRow++;
        panel.add(new JBLabel("Anything to add? (optional)"), gc);

        gc.gridy = nextRow;
        qualNoteField = new JTextArea(3, 40);
        qualNoteField.setLineWrap(true);
        qualNoteField.setWrapStyleWord(true);
        panel.add(new JScrollPane(qualNoteField), gc);

        return panel;
    }

    @Override
    protected void doOKAction() {
        saveResponse();
        super.doOKAction();
    }

    private void saveResponse() {
        Integer[] scores = new Integer[ITEMS.length];
        double sum = 0;
        int count = 0;
        for (int i = 0; i < ITEMS.length; i++) {
            ButtonModel selected = itemGroups[i].getSelection();
            if (selected != null) {
                scores[i] = Integer.parseInt(selected.getActionCommand());
                sum += scores[i];
                count++;
            }
        }
        Double composite = count > 0 ? sum / count : null;

        MetricCollector collector = ApplicationManager.getApplication()
                .getService(MetricCollector.class);
        String sessionId = collector != null ? collector.getSessionId() : "unknown";

        MetricRepository.EsmResponse response = new MetricRepository.EsmResponse(
            sessionId,
            triggeredAt,
            Instant.now(),
            scores[0], scores[1], scores[2], scores[3], scores[4], scores[5], scores[6],
            composite,
            qualNoteField.getText().trim().isEmpty() ? null : qualNoteField.getText().trim(),
            usingAiCheck.isSelected(),
            usingAiCheck.isSelected() ? aiToolNameField.getText().trim() : null
        );

        MetricRepository repo = ApplicationManager.getApplication()
                .getService(MetricRepository.class);
        if (repo != null) {
            long id = repo.saveEsmResponse(response);
            LOG.info("ESM response saved, id=" + id + ", composite=" + composite);
        }
    }
}
