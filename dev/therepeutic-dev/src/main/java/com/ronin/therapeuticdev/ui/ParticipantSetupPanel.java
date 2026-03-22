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
 * the first screen participants see — a centred card prompting for their ID.
 *
 * this gates the entire plugin experience behind participant identification.
 * once an ID is entered and confirmed, the onConfirmed callback fires which
 * tells FlowStateToolWindowFactory to swap this panel out for the main dashboard.
 *
 * minimum ID length is 2 characters to prevent accidental empty submissions.
 * the enter key also triggers confirmation so participants don't have to reach
 * for the button.
 */
public class ParticipantSetupPanel extends JBPanel<ParticipantSetupPanel> {

    private final Runnable onConfirmed;
    private final JBTextField idField;
    private final JBLabel errorLabel;

    public ParticipantSetupPanel(Runnable onConfirmed) {
        super(new GridBagLayout());
        this.onConfirmed = onConfirmed;

        setBackground(new JBColor(new Color(0x1E, 0x1E, 0x1E), new Color(0x1E, 0x1E, 0x1E)));

        // Card container
        JBPanel<?> card = new JBPanel<>(new GridBagLayout());
        card.setBackground(new JBColor(new Color(0x2B, 0x2B, 0x2B), new Color(0x2B, 0x2B, 0x2B)));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new JBColor(new Color(0x3C, 0x3C, 0x3C), new Color(0x3C, 0x3C, 0x3C))),
                JBUI.Borders.empty(32, 40)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(4, 0);

        // Title
        gbc.gridy = 0;
        JBLabel title = new JBLabel("Therapeutic Dev");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setForeground(new JBColor(new Color(0xE5, 0xA8, 0x4B), new Color(0xE5, 0xA8, 0x4B)));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(title, gbc);

        // Subtitle
        gbc.gridy = 1;
        gbc.insets = JBUI.insets(8, 0, 20, 0);
        JBLabel subtitle = new JBLabel("<html><div style='text-align:center;'>"
                + "Flow-state detection research study<br>"
                + "Please enter your participant ID to begin.</div></html>");
        subtitle.setFont(subtitle.getFont().deriveFont(12f));
        subtitle.setForeground(new JBColor(new Color(0xA9, 0xB7, 0xC6), new Color(0xA9, 0xB7, 0xC6)));
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(subtitle, gbc);

        // Field label
        gbc.gridy = 2;
        gbc.insets = JBUI.insets(4, 0, 2, 0);
        JBLabel fieldLabel = new JBLabel("Participant ID");
        fieldLabel.setFont(fieldLabel.getFont().deriveFont(Font.BOLD, 11f));
        fieldLabel.setForeground(new JBColor(new Color(0xBB, 0xBB, 0xBB), new Color(0xBB, 0xBB, 0xBB)));
        card.add(fieldLabel, gbc);

        // Text field
        gbc.gridy = 3;
        gbc.insets = JBUI.insets(2, 0, 4, 0);
        idField = new JBTextField();
        idField.getEmptyText().setText("e.g. P001 or your name");
        idField.setColumns(20);
        card.add(idField, gbc);

        // Error label
        gbc.gridy = 4;
        gbc.insets = JBUI.insets(0, 0, 8, 0);
        errorLabel = new JBLabel(" ");
        errorLabel.setFont(errorLabel.getFont().deriveFont(11f));
        errorLabel.setForeground(new JBColor(new Color(0xFF, 0x55, 0x55), new Color(0xFF, 0x55, 0x55)));
        card.add(errorLabel, gbc);

        // Confirm button
        gbc.gridy = 5;
        gbc.insets = JBUI.insets(4, 0);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton confirmButton = new JButton("Confirm");
        confirmButton.addActionListener(this::onConfirmClicked);
        card.add(confirmButton, gbc);

        // Enter key triggers confirm
        idField.addActionListener(this::onConfirmClicked);

        // Centre the card in the panel
        add(card);
    }

    private void onConfirmClicked(ActionEvent e) {
        String id = idField.getText().trim();
        if (id.length() < 2) {
            errorLabel.setText("ID must be at least 2 characters.");
            return;
        }
        errorLabel.setText(" ");
        ParticipantSession.getInstance().setParticipantId(id);
        onConfirmed.run();
    }
}
