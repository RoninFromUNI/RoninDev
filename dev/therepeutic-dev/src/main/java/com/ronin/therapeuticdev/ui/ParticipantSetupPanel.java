package com.ronin.therapeuticdev.ui;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.ronin.therapeuticdev.services.ParticipantSession;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Setup screen shown inside the tool window before any participant ID is set.
 *
 * Renders a centred card with a text field for the participant ID and a
 * Confirm button. Once a valid ID is entered, the provided callback is
 * invoked so the tool window factory can swap in the main FlowStatePanel.
 */
public class ParticipantSetupPanel extends JBPanel<ParticipantSetupPanel> {

    private static final Color CARD_BG = new Color(0x1E, 0x1E, 0x1E);
    private static final Color CARD_BORDER = new Color(0x3C, 0x3F, 0x41);
    private static final Color ACCENT = new Color(0xE5, 0xA8, 0x4B);
    private static final Color ERROR_RED = new Color(0xF4, 0x43, 0x36);

    private final Runnable onConfirmed;
    private final JBTextField idField;
    private final JBLabel errorLabel;

    public ParticipantSetupPanel(Runnable onConfirmed) {
        this.onConfirmed = onConfirmed;

        setLayout(new GridBagLayout());
        setBackground(JBColor.namedColor("Panel.background", new Color(0x2B, 0x2B, 0x2B)));

        // Centred card
        JBPanel<?> card = new JBPanel<>();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1),
                JBUI.Borders.empty(24)
        ));
        card.setPreferredSize(new Dimension(JBUI.scale(320), JBUI.scale(280)));

        // Title
        JBLabel title = new JBLabel("Therapeutic Dev");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setForeground(ACCENT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(JBUI.scale(8)));

        // Subtitle
        JBLabel subtitle = new JBLabel("<html><div style='text-align:center;'>"
                + "This plugin is part of a research study on<br>"
                + "developer flow states at Brunel University London.<br><br>"
                + "Please enter your participant ID to begin."
                + "</div></html>");
        subtitle.setFont(subtitle.getFont().deriveFont(12f));
        subtitle.setForeground(JBColor.namedColor("Label.foreground", new Color(0xA9, 0xB7, 0xC6)));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(JBUI.scale(20)));

        // Label
        JBLabel fieldLabel = new JBLabel("Participant ID");
        fieldLabel.setFont(fieldLabel.getFont().deriveFont(Font.BOLD, 11f));
        fieldLabel.setForeground(JBColor.namedColor("Label.foreground", new Color(0xA9, 0xB7, 0xC6)));
        fieldLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(fieldLabel);
        card.add(Box.createVerticalStrut(JBUI.scale(6)));

        // Text field
        idField = new JBTextField();
        idField.getEmptyText().setText("e.g. P001 or your name");
        idField.setMaximumSize(new Dimension(JBUI.scale(240), JBUI.scale(32)));
        idField.setAlignmentX(Component.CENTER_ALIGNMENT);
        idField.addActionListener(this::handleConfirm);
        card.add(idField);
        card.add(Box.createVerticalStrut(JBUI.scale(6)));

        // Error label
        errorLabel = new JBLabel(" ");
        errorLabel.setFont(errorLabel.getFont().deriveFont(10f));
        errorLabel.setForeground(ERROR_RED);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(JBUI.scale(12)));

        // Confirm button
        JButton confirmBtn = new JButton("Confirm");
        confirmBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        confirmBtn.addActionListener(this::handleConfirm);
        card.add(confirmBtn);

        add(card);
    }

    private void handleConfirm(ActionEvent e) {
        String id = idField.getText().trim();
        if (id.isBlank() || id.length() < 2) {
            errorLabel.setText("ID must be at least 2 characters");
            return;
        }
        errorLabel.setText(" ");
        ParticipantSession.getInstance().setParticipantId(id);
        onConfirmed.run();
    }
}
