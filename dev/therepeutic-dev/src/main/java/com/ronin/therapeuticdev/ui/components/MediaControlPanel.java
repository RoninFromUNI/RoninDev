package com.ronin.therapeuticdev.ui.components;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * compact media control bar — prev/play-pause/next/mute buttons.
 *
 * this is a quality-of-life feature i added because i personally hate alt-tabbing
 * to youtube music just to skip a track. the buttons send os-level media key events
 * via powershell's keybd_event interop, which means they control whatever media app
 * is currently playing (spotify, youtube music in chrome, etc.) without leaving the ide.
 *
 * java's Robot class only fires jvm-level key events which don't reach other apps,
 * so i have to shell out to powershell with the Add-Type / DllImport pattern to
 * call user32.dll's keybd_event directly. this only works on windows — the
 * IOException catch silently fails on mac/linux.
 *
 * the buttons live in the session footer of FlowStatePanel, right-aligned opposite
 * the session duration clock.
 */
public class MediaControlPanel extends JBPanel<MediaControlPanel> {

    // Windows VK codes for media keys
    private static final int VK_MEDIA_PREV_TRACK = 0xB1;
    private static final int VK_MEDIA_PLAY_PAUSE = 0xB3;
    private static final int VK_MEDIA_NEXT_TRACK = 0xB0;
    private static final int VK_VOLUME_MUTE      = 0xAD;

    // PowerShell snippet that calls keybd_event at the OS level
    private static final String PS_TEMPLATE =
        "Add-Type -MemberDefinition '" +
        "[DllImport(\"user32.dll\")] public static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, int dwExtraInfo);'" +
        " -Name KB -Namespace U; [U.KB]::keybd_event(%d,0,0,0); [U.KB]::keybd_event(%d,0,2,0)";

    private static final Color BTN_BG      = new Color(0x3C, 0x3F, 0x41);
    private static final Color BTN_HOVER   = new Color(0x55, 0x58, 0x5A);
    private static final Color LABEL_COLOR = new Color(0x6B, 0x73, 0x7C);

    public MediaControlPanel() {
        setLayout(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        setOpaque(false);

        JLabel label = new JLabel("♫");
        label.setForeground(LABEL_COLOR);
        label.setFont(label.getFont().deriveFont(11f));
        add(label);

        add(createMediaButton("⏮", VK_MEDIA_PREV_TRACK, "Previous track"));
        add(createMediaButton("⏯", VK_MEDIA_PLAY_PAUSE, "Play / Pause"));
        add(createMediaButton("⏭", VK_MEDIA_NEXT_TRACK, "Next track"));
        add(createMediaButton("🔇", VK_VOLUME_MUTE,      "Mute / Unmute"));
    }

    private JButton createMediaButton(String icon, int keyCode, String tooltip) {
        JButton btn = new JButton(icon) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? BTN_HOVER : BTN_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(btn.getFont().deriveFont(13f));
        btn.setForeground(JBColor.WHITE);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setToolTipText(tooltip);
        btn.setPreferredSize(new Dimension(JBUI.scale(28), JBUI.scale(24)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> pressMediaKey(keyCode));
        return btn;
    }

    private void pressMediaKey(int keyCode) {
        try {
            String ps = String.format(PS_TEMPLATE, keyCode, keyCode);
            new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-Command", ps)
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException ignored) {
            // Silently fail on non-Windows or restricted environments
        }
    }
}
