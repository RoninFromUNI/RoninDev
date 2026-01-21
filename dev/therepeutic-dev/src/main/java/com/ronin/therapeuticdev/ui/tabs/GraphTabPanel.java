package com.ronin.therapeuticdev.ui.tabs;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

/**
 * Graph tab showing project architecture visualization.
 * 
 * <p>Displays classes as nodes grouped by package, with dependency arrows.
 * Clicking a node navigates to that file in the editor.
 * 
 * <p>Layout (simplified example):
 * <pre>
 * ┌─────────────────────────────────────────┐
 * │  ╭─ services/ ─────╮  ╭─ ui/ ─────────╮│
 * │  │ MetricCollector │  │ FlowStatePanel││
 * │  │ FlowDetector    │→→│ StatusWidget  ││
 * │  ╰─────────────────╯  ╰───────────────╯│
 * │          ↑                              │
 * │  ╭─ listeners/ ────╮  ╭─ storage/ ────╮│
 * │  │ TypingListener  │  │ MetricRepo    ││
 * │  │ FileListener    │  │ SQLiteHelper  ││
 * │  ╰─────────────────╯  ╰───────────────╯│
 * └─────────────────────────────────────────┘
 * </pre>
 */
public class GraphTabPanel extends JBPanel<GraphTabPanel> {

    private static final Color CARD_BG = new Color(0x25, 0x25, 0x25);
    private static final Color CARD_BORDER = new Color(0x3C, 0x3F, 0x41);
    private static final Color ACCENT = new Color(0xE5, 0xA8, 0x4B);
    private static final Color MUTED = new Color(0x6B, 0x73, 0x7C);
    
    // Package colors
    private static final Color SERVICES_COLOR = new Color(0x68, 0x97, 0xBB);
    private static final Color UI_COLOR = new Color(0x98, 0x76, 0xAA);
    private static final Color LISTENERS_COLOR = new Color(0x4C, 0xAF, 0x50);
    private static final Color STORAGE_COLOR = new Color(0xE5, 0xA8, 0x4B);
    
    private final Project project;
    private GraphCanvas graphCanvas;
    private JBLabel statusLabel;
    
    // Graph data
    private final Map<String, List<ClassNode>> packageGroups = new LinkedHashMap<>();
    private String currentFile = "";

    public GraphTabPanel(Project project) {
        this.project = project;
        
        setLayout(new BorderLayout());
        setBackground(JBColor.namedColor("Panel.background", new Color(0x2B, 0x2B, 0x2B)));
        setBorder(JBUI.Borders.empty(8));
        
        initializeUI();
        loadProjectStructure();
    }

    private void initializeUI() {
        // Toolbar
        JBPanel<?> toolbar = createToolbar();
        add(toolbar, BorderLayout.NORTH);
        
        // Graph canvas
        graphCanvas = new GraphCanvas();
        JBScrollPane scrollPane = new JBScrollPane(graphCanvas);
        scrollPane.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1));
        scrollPane.getViewport().setBackground(CARD_BG);
        add(scrollPane, BorderLayout.CENTER);
        
        // Status bar
        statusLabel = new JBLabel("Click a node to navigate to file");
        statusLabel.setFont(statusLabel.getFont().deriveFont(10f));
        statusLabel.setForeground(MUTED);
        statusLabel.setBorder(JBUI.Borders.emptyTop(4));
        add(statusLabel, BorderLayout.SOUTH);
    }

    private JBPanel<?> createToolbar() {
        JBPanel<?> toolbar = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(JBUI.Borders.emptyBottom(8));
        
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> {
            loadProjectStructure();
            graphCanvas.repaint();
        });
        toolbar.add(refreshBtn);
        
        JBLabel zoomLabel = new JBLabel("Zoom:");
        zoomLabel.setForeground(MUTED);
        toolbar.add(zoomLabel);
        
        JButton zoomOutBtn = new JButton("−");
        zoomOutBtn.addActionListener(e -> graphCanvas.zoom(-0.1));
        toolbar.add(zoomOutBtn);
        
        JButton zoomInBtn = new JButton("+");
        zoomInBtn.addActionListener(e -> graphCanvas.zoom(0.1));
        toolbar.add(zoomInBtn);
        
        return toolbar;
    }

    private void loadProjectStructure() {
        packageGroups.clear();
        
        // Scan for Java files in project
        VirtualFile[] sourceRoots = ProjectRootManager.getInstance(project).getContentSourceRoots();
        
        for (VirtualFile root : sourceRoots) {
            scanDirectory(root, "");
        }
        
        // If no files found, add placeholders
        if (packageGroups.isEmpty()) {
            addPlaceholderStructure();
        }
        
        graphCanvas.setPackageGroups(packageGroups);
    }

    private void scanDirectory(VirtualFile dir, String packagePath) {
        if (dir == null || !dir.isDirectory()) return;
        
        for (VirtualFile child : dir.getChildren()) {
            if (child.isDirectory()) {
                String newPath = packagePath.isEmpty() ? child.getName() : packagePath + "." + child.getName();
                scanDirectory(child, newPath);
            } else if (child.getName().endsWith(".java")) {
                String className = child.getNameWithoutExtension();
                String packageName = extractPackageName(packagePath);
                
                packageGroups.computeIfAbsent(packageName, k -> new ArrayList<>())
                        .add(new ClassNode(className, child.getPath(), packageName));
            }
        }
    }

    private String extractPackageName(String fullPackage) {
        // Simplify to last segment for display
        if (fullPackage.isEmpty()) return "default";
        String[] parts = fullPackage.split("\\.");
        return parts[parts.length - 1];
    }

    private void addPlaceholderStructure() {
        // Add example structure matching your project
        packageGroups.put("services", Arrays.asList(
                new ClassNode("MetricCollector", "", "services"),
                new ClassNode("SnapshotScheduler", "", "services")
        ));
        packageGroups.put("detection", Arrays.asList(
                new ClassNode("FlowDetector", "", "detection"),
                new ClassNode("FlowDetectionResult", "", "detection")
        ));
        packageGroups.put("listeners", Arrays.asList(
                new ClassNode("TypingActivityListener", "", "listeners"),
                new ClassNode("FileActivityListener", "", "listeners"),
                new ClassNode("BuildListener", "", "listeners")
        ));
        packageGroups.put("ui", Arrays.asList(
                new ClassNode("FlowStatePanel", "", "ui"),
                new ClassNode("StatusBarWidget", "", "ui")
        ));
        packageGroups.put("storage", Arrays.asList(
                new ClassNode("MetricRepository", "", "storage")
        ));
    }

    public void setCurrentFile(String filePath) {
        this.currentFile = filePath;
        graphCanvas.setCurrentFile(filePath);
        graphCanvas.repaint();
    }

    private void navigateToFile(ClassNode node) {
        if (node.filePath.isEmpty()) {
            statusLabel.setText("File path not available");
            return;
        }
        
        VirtualFile file = com.intellij.openapi.vfs.LocalFileSystem.getInstance()
                .findFileByPath(node.filePath);
        
        if (file != null) {
            FileEditorManager.getInstance(project).openFile(file, true);
            statusLabel.setText("Opened: " + node.className);
        }
    }

    /**
     * Represents a class node in the graph.
     */
    record ClassNode(String className, String filePath, String packageName) {}

    /**
     * Canvas for drawing the architecture graph.
     */
    private class GraphCanvas extends JBPanel<GraphCanvas> {
        
        private Map<String, List<ClassNode>> packages = new LinkedHashMap<>();
        private String currentFile = "";
        private double zoomLevel = 1.0;
        private final Map<String, Rectangle> nodeBounds = new HashMap<>();
        
        public GraphCanvas() {
            setBackground(CARD_BG);
            setPreferredSize(new Dimension(JBUI.scale(400), JBUI.scale(350)));
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    handleClick(e.getPoint());
                }
            });
        }
        
        public void setPackageGroups(Map<String, List<ClassNode>> packages) {
            this.packages = packages;
            repaint();
        }
        
        public void setCurrentFile(String filePath) {
            this.currentFile = filePath;
        }
        
        public void zoom(double delta) {
            zoomLevel = Math.max(0.5, Math.min(2.0, zoomLevel + delta));
            repaint();
        }
        
        private void handleClick(Point p) {
            for (Map.Entry<String, Rectangle> entry : nodeBounds.entrySet()) {
                if (entry.getValue().contains(p)) {
                    // Find and navigate to the node
                    for (List<ClassNode> nodes : packages.values()) {
                        for (ClassNode node : nodes) {
                            if (node.className.equals(entry.getKey())) {
                                navigateToFile(node);
                                return;
                            }
                        }
                    }
                }
            }
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.scale(zoomLevel, zoomLevel);
            
            nodeBounds.clear();
            
            int padding = JBUI.scale(20);
            int groupWidth = JBUI.scale(140);
            int groupSpacing = JBUI.scale(20);
            int nodeHeight = JBUI.scale(24);
            int nodeSpacing = JBUI.scale(4);
            
            int x = padding;
            int y = padding;
            int col = 0;
            int maxCols = 2;
            
            for (Map.Entry<String, List<ClassNode>> entry : packages.entrySet()) {
                String packageName = entry.getKey();
                List<ClassNode> nodes = entry.getValue();
                
                Color packageColor = getPackageColor(packageName);
                int groupHeight = JBUI.scale(30) + (nodes.size() * (nodeHeight + nodeSpacing));
                
                // Draw package group border (dashed)
                g2.setColor(packageColor);
                g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 
                        10, new float[]{4}, 0));
                g2.drawRoundRect(x, y, groupWidth, groupHeight, 8, 8);
                
                // Package label
                g2.setFont(g2.getFont().deriveFont(9f));
                g2.drawString(packageName + "/", x + JBUI.scale(8), y + JBUI.scale(14));
                
                // Draw nodes
                int nodeY = y + JBUI.scale(22);
                g2.setStroke(new BasicStroke(1));
                
                for (ClassNode node : nodes) {
                    boolean isCurrent = node.filePath.equals(currentFile);
                    
                    // Node background
                    g2.setColor(packageColor.darker());
                    g2.fillRoundRect(x + JBUI.scale(8), nodeY, groupWidth - JBUI.scale(16), nodeHeight, 4, 4);
                    
                    // Node border
                    g2.setColor(isCurrent ? Color.WHITE : packageColor);
                    g2.setStroke(new BasicStroke(isCurrent ? 2 : 1));
                    g2.drawRoundRect(x + JBUI.scale(8), nodeY, groupWidth - JBUI.scale(16), nodeHeight, 4, 4);
                    
                    // Node text
                    g2.setColor(Color.WHITE);
                    g2.setFont(g2.getFont().deriveFont(10f));
                    String displayName = node.className.length() > 16 ? 
                            node.className.substring(0, 14) + ".." : node.className;
                    g2.drawString(displayName, x + JBUI.scale(14), nodeY + JBUI.scale(16));
                    
                    // Store bounds for click detection
                    nodeBounds.put(node.className, new Rectangle(
                            (int)((x + JBUI.scale(8)) * zoomLevel),
                            (int)(nodeY * zoomLevel),
                            (int)((groupWidth - JBUI.scale(16)) * zoomLevel),
                            (int)(nodeHeight * zoomLevel)
                    ));
                    
                    nodeY += nodeHeight + nodeSpacing;
                }
                
                // Move to next position
                col++;
                if (col >= maxCols) {
                    col = 0;
                    x = padding;
                    y += groupHeight + groupSpacing;
                } else {
                    x += groupWidth + groupSpacing;
                }
            }
            
            g2.dispose();
        }
        
        private Color getPackageColor(String packageName) {
            return switch (packageName.toLowerCase()) {
                case "services" -> SERVICES_COLOR;
                case "ui", "components", "tabs" -> UI_COLOR;
                case "listeners" -> LISTENERS_COLOR;
                case "storage" -> STORAGE_COLOR;
                case "detection" -> new Color(0x7B, 0x68, 0xEE);
                case "metrics" -> new Color(0x20, 0xB2, 0xAA);
                case "settings" -> new Color(0xCD, 0x85, 0x3F);
                default -> MUTED;
            };
        }
    }
}
