package com.idlemaster;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.config.ConfigManager;

public class FloatingOverlayWindow extends JFrame {
    
    // Constants
    private static class Constants {
        // Window sizing
        static final int RESIZE_BORDER = 5;
        static final int MIN_WIDTH = 80;
        static final int MIN_HEIGHT = 80;
        static final int MAX_WIDTH = 500;
        static final int MAX_HEIGHT = 350;
        
        // Content sizing
        static final int MIN_ICON_SIZE = 18;
        static final int MAX_ICON_SIZE = 36;
        static final int MIN_FONT_SIZE = 12;
        static final int MAX_FONT_SIZE = 18;
        
        // Layout
        static final int TITLE_BAR_HEIGHT = 20;
        static final int PADDING = 10;
        static final int COMPONENT_SPACING = 6;
        static final int ICON_TEXT_GAP = 6;
        static final int BUTTON_SIZE = 20;
        static final int BUTTON_SPACING = 4;
        static final int DRAG_AREA_HEIGHT = 30;
        
        // Colors
        static final Color DARK_BORDER_COLOR = new Color(60, 60, 60, 200);
        static final Color DARK_TEXT_COLOR = new Color(220, 220, 220);
        static final Color BOAT_HEALTH_COLOR = new Color(100, 200, 255);
        static final Color INVENTORY_COLOR = new Color(200, 180, 100);
        static final Color CARGO_COLOR = new Color(150, 200, 150);
        static final Color SALVAGING_COLOR = new Color(120, 255, 120);
        static final Color IDLE_COLOR = new Color(255, 180, 100);
        static final Color CREW_COLOR = new Color(180, 150, 255);
        static final Color DANGER_COLOR = new Color(255, 100, 100);
        static final Color WARNING_COLOR = new Color(255, 200, 100);
        static final Color SAFE_COLOR = new Color(120, 255, 120);
        static final Color WHITE = Color.WHITE;
    }

    // Instance variables
    private final SalvageInfo salvageInfo;
    private final IdleMasterConfig config;
    private final ConfigManager configManager;
    private JPanel contentPanel;
    private JPanel infoPanel;
    
    // Labels for each info type
    private JPanel boatHealthPanel;
    private JLabel inventoryLabel;
    private JLabel cargoLabel;
    private JLabel playerStatusLabel;
    private JLabel crewStatusLabel;
    private JLabel monsterAlertLabel;
    private JPanel xpBarPanel;
    
    private JPanel titleBar;
    private JLabel characterNameLabel;
    private Window runeliteWindow;
    
    // Icons
    private BufferedImage boatIcon;
    private BufferedImage inventoryIcon;
    private BufferedImage cargoIcon;
    private BufferedImage playerIcon;
    private BufferedImage crewIcon;
    private BufferedImage alertIcon;
    
    // Interaction state
    private Point dragPoint;
    private boolean isDragging = false;
    private boolean isResizing = false;
    private int resizeEdge = 0;
    
    // Monster alert flash state
    private Timer flashTimer;
    private boolean flashState = false;

    public FloatingOverlayWindow(SalvageInfo salvageInfo, IdleMasterConfig config, ConfigManager configManager) {
        this.salvageInfo = salvageInfo;
        this.config = config;
        this.configManager = configManager;
        
        initializeWindow();
        loadIcons();
        setupContentPanel();
        setupLabels();
        setupLayout();
        setupEventListeners();
        
        if (!loadPositionAndSize()) {
            setSize(200, 150);
        }
        
        validatePosition();
        updateDisplay();
    }
    
    private void initializeWindow() {
        setTitle("Idle Master - Salvaging");
        setUndecorated(true);
        setAlwaysOnTop(true);
        setType(Type.NORMAL);
        setResizable(false);
        setBackground(new Color(0, 0, 0, 0));
        
        try {
            this.runeliteWindow = findRuneLiteWindow();
        } catch (Exception e) {
            // Ignore
        }
    }
    
    private void setupContentPanel() {
        contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                
                RoundRectangle2D roundedRectangle = new RoundRectangle2D.Float(
                    0, 0, getWidth(), getHeight(), 12, 12);
                
                g2d.setColor(getBackgroundColor());
                g2d.fill(roundedRectangle);
                
                if (config.showWindowBorder()) {
                    g2d.setColor(Constants.DARK_BORDER_COLOR);
                    g2d.setStroke(new BasicStroke(1.5f));
                    g2d.draw(roundedRectangle);
                }
                
                g2d.dispose();
            }
        };
        
        contentPanel.setLayout(new BorderLayout(Constants.COMPONENT_SPACING, Constants.COMPONENT_SPACING));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(Constants.PADDING, Constants.PADDING, Constants.PADDING, Constants.PADDING));
        contentPanel.setOpaque(false);
    }
    
    private Color getBackgroundColor() {
        // Flash red when monster is attacking
        if (salvageInfo.isMonsterAttacking() && flashState) {
            return new Color(150, 30, 30, config.opacity()); // Dark red flash
        }
        // Default black background with configured opacity
        return new Color(0, 0, 0, config.opacity());
    }
    
    private void startFlashTimer() {
        if (flashTimer == null) {
            flashTimer = new Timer(500, e -> {
                flashState = !flashState;
                contentPanel.repaint();
            });
            flashTimer.start();
        }
    }
    
    private void stopFlashTimer() {
        if (flashTimer != null) {
            flashTimer.stop();
            flashTimer = null;
            flashState = false;
            contentPanel.repaint();
        }
    }
    
    private void setupLabels() {
        boatHealthPanel = createHealthBarPanel();
        inventoryLabel = createLabel("Inv: 0/28", inventoryIcon);
        cargoLabel = createLabel("Cargo: 0/xx", cargoIcon);
        playerStatusLabel = createLabel("Player: IDLE", playerIcon);
        crewStatusLabel = createLabel("Crew: No Crew", crewIcon);
        monsterAlertLabel = createLabel("Alert: Safe", alertIcon);
        xpBarPanel = createXpBarPanel();
    }
    
    private JPanel createHealthBarPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int width = getWidth();
                int height = getHeight();
                int barHeight = 16;
                int barY = (height - barHeight) / 2;
                int barX = 0;
                int barWidth = width - 4;
                
                // Draw red background (missing health)
                g2d.setColor(new Color(180, 40, 40));
                g2d.fillRoundRect(barX, barY, barWidth, barHeight, 4, 4);
                
                // Draw green health fill (same size as red when at 100%)
                int healthPercent = salvageInfo.getBoatHealthPercentage();
                int fillWidth = (int) (barWidth * (healthPercent / 100.0));
                
                g2d.setColor(new Color(40, 180, 40)); // Green
                g2d.fillRoundRect(barX, barY, fillWidth, barHeight, 4, 4);
                
                // Draw border
                g2d.setColor(new Color(40, 40, 40));
                g2d.drawRoundRect(barX, barY, barWidth, barHeight, 4, 4);
                
                // Draw text in center
                String text = salvageInfo.getBoatHealth() + "/" + salvageInfo.getMaxBoatHealth();
                g2d.setFont(new Font("Arial", Font.BOLD, 11));
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(text);
                int textX = barX + (barWidth - textWidth) / 2;
                int textY = barY + ((barHeight - fm.getHeight()) / 2) + fm.getAscent();
                
                // Text shadow
                g2d.setColor(Color.BLACK);
                g2d.drawString(text, textX + 1, textY + 1);
                
                // Text
                g2d.setColor(Color.WHITE);
                g2d.drawString(text, textX, textY);
                
                g2d.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(150, 22));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        return panel;
    }
    
    private JPanel createXpBarPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int width = getWidth();
                int height = getHeight();
                int barHeight = 14;
                int barY = (height - barHeight) / 2;
                int barX = 0;
                int barWidth = width - 4;
                
                // Draw dark background
                g2d.setColor(new Color(30, 30, 30));
                g2d.fillRoundRect(barX, barY, barWidth, barHeight, 4, 4);
                
                // Draw XP progress fill (cyan/teal color like RuneLite XP tracker)
                int xpPercent = salvageInfo.getXpProgressPercentage();
                int fillWidth = (int) (barWidth * (xpPercent / 100.0));
                
                g2d.setColor(new Color(0, 180, 180)); // Cyan/teal
                g2d.fillRoundRect(barX, barY, fillWidth, barHeight, 4, 4);
                
                // Draw border
                g2d.setColor(new Color(60, 60, 60));
                g2d.drawRoundRect(barX, barY, barWidth, barHeight, 4, 4);
                
                // Get level info
                int currentLevel = salvageInfo.getSailingLevel();
                int nextLevel = Math.min(currentLevel + 1, 99);
                
                g2d.setFont(new Font("Arial", Font.PLAIN, 10));
                FontMetrics fm = g2d.getFontMetrics();
                int textY = barY + ((barHeight - fm.getHeight()) / 2) + fm.getAscent();
                int padding = 4;
                
                // Left text - current level (clamped to left)
                String leftText = "Lvl " + currentLevel;
                int leftTextX = barX + padding;
                
                // Right text - next level (clamped to right)
                String rightText = currentLevel >= 99 ? "" : "Lvl " + nextLevel;
                int rightTextWidth = fm.stringWidth(rightText);
                int rightTextX = barX + barWidth - rightTextWidth - padding;
                
                // Center text - XP remaining
                String centerText = currentLevel >= 99 ? "Max Level" : salvageInfo.getXpRemainingText();
                int centerTextWidth = fm.stringWidth(centerText);
                int centerTextX = barX + (barWidth - centerTextWidth) / 2;
                
                // Draw left text (current level)
                g2d.setColor(Color.BLACK);
                g2d.drawString(leftText, leftTextX + 1, textY + 1);
                g2d.setColor(Color.WHITE);
                g2d.drawString(leftText, leftTextX, textY);
                
                // Draw center text (XP remaining)
                g2d.setColor(Color.BLACK);
                g2d.drawString(centerText, centerTextX + 1, textY + 1);
                g2d.setColor(Color.WHITE);
                g2d.drawString(centerText, centerTextX, textY);
                
                // Draw right text (next level)
                if (!rightText.isEmpty()) {
                    g2d.setColor(Color.BLACK);
                    g2d.drawString(rightText, rightTextX + 1, textY + 1);
                    g2d.setColor(Color.WHITE);
                    g2d.drawString(rightText, rightTextX, textY);
                }
                
                g2d.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(150, 18));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        return panel;
    }
    
    private void setupLayout() {
        infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        
        addComponentIfVisible(config.showBoatHealth(), boatHealthPanel);
        addComponentIfVisible(config.showInventory(), inventoryLabel);
        addComponentIfVisible(config.showCargo(), cargoLabel);
        addComponentIfVisible(config.showPlayerStatus(), playerStatusLabel);
        addComponentIfVisible(config.showCrewStatus(), crewStatusLabel);
        addComponentIfVisible(config.showMonsterAlert(), monsterAlertLabel);
        addComponentIfVisible(config.showXpBar(), xpBarPanel);
        
        contentPanel.add(infoPanel, BorderLayout.CENTER);
        
        titleBar = createTitleBar();
        contentPanel.add(titleBar, BorderLayout.NORTH);
        
        setContentPane(contentPanel);
    }
    
    private void addComponentIfVisible(boolean isVisible, JComponent component) {
        if (isVisible) {
            infoPanel.add(component);
            infoPanel.add(Box.createVerticalStrut(Constants.COMPONENT_SPACING));
        }
    }
    
    private JPanel createTitleBar() {
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setOpaque(false);
        titleBar.setPreferredSize(new Dimension(0, Constants.TITLE_BAR_HEIGHT));
        
        characterNameLabel = new JLabel("");
        characterNameLabel.setFont(new Font("Arial", Font.BOLD, Constants.MIN_FONT_SIZE));
        characterNameLabel.setForeground(Constants.DARK_TEXT_COLOR);
        characterNameLabel.setBorder(BorderFactory.createEmptyBorder(0, Constants.PADDING, 0, 0));
        
        if (config.showCharacterName()) {
            titleBar.add(characterNameLabel, BorderLayout.WEST);
        }
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, Constants.BUTTON_SPACING, 0));
        buttonPanel.setOpaque(false);
        
        if (config.showMinimizeButton()) {
            buttonPanel.add(createCustomButton("−"));
        }
        
        if (config.showCloseButton()) {
            buttonPanel.add(createCustomButton("×"));
        }
        
        titleBar.add(buttonPanel, BorderLayout.EAST);
        return titleBar;
    }
    
    private JButton createCustomButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (getModel().isRollover()) {
                    g2d.setColor(new Color(0, 0, 0, 120));
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                }
                
                g2d.setColor(Constants.DARK_TEXT_COLOR);
                g2d.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                
                FontMetrics fm = g2d.getFontMetrics();
                int textX = (getWidth() - fm.stringWidth(text)) / 2;
                int textY = (getHeight() + fm.getAscent()) / 2 - 1;
                g2d.drawString(text, textX, textY);
                
                g2d.dispose();
            }
        };
        
        button.setPreferredSize(new Dimension(Constants.BUTTON_SIZE, Constants.BUTTON_SIZE));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        
        if ("−".equals(text)) {
            button.addActionListener(e -> {
                setState(Frame.ICONIFIED);
                setAlwaysOnTop(false);
            });
        } else if ("×".equals(text)) {
            button.addActionListener(e -> setVisible(false));
        }
        
        return button;
    }
    
    private JLabel createLabel(String text, BufferedImage icon) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, Constants.MIN_FONT_SIZE));
        label.setForeground(Constants.DARK_TEXT_COLOR);
        
        if (icon != null) {
            label.setIcon(new ImageIcon(icon));
            label.setIconTextGap(Constants.ICON_TEXT_GAP);
        }
        
        return label;
    }
    
    private void setupEventListeners() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowDeiconified(WindowEvent e) {
                setAlwaysOnTop(true);
            }
            
            @Override
            public void windowClosing(WindowEvent e) {
                savePositionAndSize();
            }
            
            @Override
            public void windowClosed(WindowEvent e) {
                savePositionAndSize();
            }
        });
        
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                savePositionAndSize();
            }
            
            @Override
            public void componentMoved(ComponentEvent e) {
                savePositionAndSize();
            }
        });
        
        addMouseListeners();
    }
    
    private void addMouseListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point p = e.getPoint();
                int width = getWidth();
                int height = getHeight();
                
                if (p.x > width - Constants.RESIZE_BORDER && p.y > height - Constants.RESIZE_BORDER) {
                    isResizing = true;
                    resizeEdge = 3;
                    setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
                } else if (p.x > width - Constants.RESIZE_BORDER) {
                    isResizing = true;
                    resizeEdge = 1;
                    setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                } else if (p.y > height - Constants.RESIZE_BORDER) {
                    isResizing = true;
                    resizeEdge = 2;
                    setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
                } else if (p.y < Constants.DRAG_AREA_HEIGHT) {
                    isDragging = true;
                    dragPoint = p;
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (isDragging || isResizing) {
                    savePositionAndSize();
                    validatePosition();
                }
                
                isDragging = false;
                isResizing = false;
                resizeEdge = 0;
                setCursor(Cursor.getDefaultCursor());
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && !isResizing && !isDragging) {
                    focusRuneLiteWindow();
                }
            }
        });
        
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDragging) {
                    Point p = getLocation();
                    setLocation(p.x + e.getX() - dragPoint.x, p.y + e.getY() - dragPoint.y);
                } else if (isResizing) {
                    Point p = e.getPoint();
                    int newWidth = getWidth();
                    int newHeight = getHeight();
                    
                    if (resizeEdge == 1 || resizeEdge == 3) {
                        newWidth = Math.max(Constants.MIN_WIDTH, Math.min(Constants.MAX_WIDTH, p.x));
                    }
                    if (resizeEdge == 2 || resizeEdge == 3) {
                        newHeight = Math.max(Constants.MIN_HEIGHT, Math.min(Constants.MAX_HEIGHT, p.y));
                    }
                    
                    setSize(newWidth, newHeight);
                }
            }
            
            @Override
            public void mouseMoved(MouseEvent e) {
                Point p = e.getPoint();
                int width = getWidth();
                int height = getHeight();
                
                if (p.x > width - Constants.RESIZE_BORDER && p.y > height - Constants.RESIZE_BORDER) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
                } else if (p.x > width - Constants.RESIZE_BORDER) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                } else if (p.y > height - Constants.RESIZE_BORDER) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
                } else if (p.y < Constants.DRAG_AREA_HEIGHT) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                } else {
                    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });
    }
    
    private boolean loadPositionAndSize() {
        int x = 100;
        int y = 100;
        int width = 280;
        int height = 200;
        
        boolean loadedFromConfig = false;
        
        if (configManager != null) {
            try {
                String xStr = configManager.getConfiguration("idlemaster", "windowX", String.class);
                String yStr = configManager.getConfiguration("idlemaster", "windowY", String.class);
                String widthStr = configManager.getConfiguration("idlemaster", "windowWidth", String.class);
                String heightStr = configManager.getConfiguration("idlemaster", "windowHeight", String.class);
                
                if (xStr != null && yStr != null && widthStr != null && heightStr != null) {
                    x = Integer.parseInt(xStr);
                    y = Integer.parseInt(yStr);
                    width = Integer.parseInt(widthStr);
                    height = Integer.parseInt(heightStr);
                    
                    width = Math.max(width, Constants.MIN_WIDTH);
                    height = Math.max(height, Constants.MIN_HEIGHT);
                    
                    setBounds(x, y, width, height);
                    loadedFromConfig = true;
                } else {
                    setBounds(x, y, width, height);
                }
            } catch (Exception e) {
                setBounds(x, y, width, height);
            }
        } else {
            setBounds(x, y, width, height);
        }
        
        return loadedFromConfig;
    }
    
    public void savePositionAndSize() {
        if (configManager != null) {
            int x = getX();
            int y = getY();
            int width = getWidth();
            int height = getHeight();
            
            configManager.setConfiguration("idlemaster", "windowX", String.valueOf(x));
            configManager.setConfiguration("idlemaster", "windowY", String.valueOf(y));
            configManager.setConfiguration("idlemaster", "windowWidth", String.valueOf(width));
            configManager.setConfiguration("idlemaster", "windowHeight", String.valueOf(height));
        }
    }
    
    private void validatePosition() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devices = ge.getScreenDevices();
        
        Rectangle windowBounds = getBounds();
        Rectangle screenBounds = null;
        
        for (GraphicsDevice device : devices) {
            Rectangle deviceBounds = device.getDefaultConfiguration().getBounds();
            if (deviceBounds.intersects(windowBounds)) {
                screenBounds = deviceBounds;
                break;
            }
        }
        
        if (screenBounds == null) {
            screenBounds = ge.getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        }
        
        int x = windowBounds.x;
        int y = windowBounds.y;
        int width = windowBounds.width;
        int height = windowBounds.height;
        
        if (x + width > screenBounds.x + screenBounds.width) {
            x = screenBounds.x + screenBounds.width - width;
        }
        if (y + height > screenBounds.y + screenBounds.height) {
            y = screenBounds.y + screenBounds.height - height;
        }
        if (x < screenBounds.x) {
            x = screenBounds.x;
        }
        if (y < screenBounds.y) {
            y = screenBounds.y;
        }
        
        if (width < Constants.MIN_WIDTH) width = Constants.MIN_WIDTH;
        if (height < Constants.MIN_HEIGHT) height = Constants.MIN_HEIGHT;
        
        setBounds(x, y, width, height);
    }
    
    public void resetPosition() {
        setLocation(100, 100);
        validatePosition();
        savePositionAndSize();
    }
    
    public void updateConfig() {
        if (config.resetPosition()) {
            resetPosition();
            if (configManager != null) {
                configManager.setConfiguration("idlemaster", "resetPosition", false);
            }
        }
        
        contentPanel.remove(titleBar);
        titleBar = createTitleBar();
        contentPanel.add(titleBar, BorderLayout.NORTH);
        
        boatHealthPanel.setVisible(config.showBoatHealth());
        inventoryLabel.setVisible(config.showInventory());
        cargoLabel.setVisible(config.showCargo());
        playerStatusLabel.setVisible(config.showPlayerStatus());
        crewStatusLabel.setVisible(config.showCrewStatus());
        monsterAlertLabel.setVisible(config.showMonsterAlert());
        xpBarPanel.setVisible(config.showXpBar());
        
        rebuildInfoPanel();
        updateDisplay();
    }
    
    private void rebuildInfoPanel() {
        contentPanel.remove(infoPanel);
        infoPanel.removeAll();
        
        addComponentIfVisible(config.showBoatHealth(), boatHealthPanel);
        addComponentIfVisible(config.showInventory(), inventoryLabel);
        addComponentIfVisible(config.showCargo(), cargoLabel);
        addComponentIfVisible(config.showPlayerStatus(), playerStatusLabel);
        addComponentIfVisible(config.showCrewStatus(), crewStatusLabel);
        addComponentIfVisible(config.showMonsterAlert(), monsterAlertLabel);
        addComponentIfVisible(config.showXpBar(), xpBarPanel);
        
        contentPanel.add(infoPanel, BorderLayout.CENTER);
    }
    
    public void updateCharacterName(String name) {
        if (characterNameLabel != null) {
            SwingUtilities.invokeLater(() -> {
                if (name != null && !name.isEmpty()) {
                    characterNameLabel.setText(name + " - Salvaging");
                } else {
                    characterNameLabel.setText("");
                }
            });
        }
    }
    
    private Window findRuneLiteWindow() {
        try {
            Window parent = SwingUtilities.getWindowAncestor(this);
            if (parent instanceof JFrame && parent.isVisible()) {
                String title = ((JFrame) parent).getTitle();
                if (title != null && title.toLowerCase().contains("runelite")
                        && !title.toLowerCase().contains("starting")
                        && !title.toLowerCase().contains("plugin")
                        && parent.getWidth() > 400 && parent.getHeight() > 400) {
                    return parent;
                }
            }
            
            for (Window window : Window.getWindows()) {
                if (window instanceof JFrame && window.isVisible()) {
                    String title = ((JFrame) window).getTitle();
                    if (title != null && title.toLowerCase().contains("runelite")
                            && !title.toLowerCase().contains("starting")
                            && !title.toLowerCase().contains("plugin")
                            && window.getWidth() > 400 && window.getHeight() > 400) {
                        return window;
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
    
    private void focusRuneLiteWindow() {
        try {
            Window targetWindow = runeliteWindow;
            if (targetWindow == null) {
                targetWindow = findRuneLiteWindow();
            }
            
            if (targetWindow != null) {
                if (targetWindow instanceof Frame) {
                    Frame frame = (Frame) targetWindow;
                    if (frame.getState() == Frame.ICONIFIED) {
                        frame.setState(Frame.NORMAL);
                    }
                }
                
                targetWindow.setVisible(true);
                targetWindow.toFront();
                targetWindow.requestFocus();
                targetWindow.requestFocusInWindow();
            }
        } catch (Exception e) {
            // Ignore
        }
    }
    
    private static final int ICON_SIZE = 16;
    
    private void loadIcons() {
        // Create placeholders first
        boatIcon = createPlaceholderIcon(ICON_SIZE, ICON_SIZE, Constants.BOAT_HEALTH_COLOR);
        inventoryIcon = createPlaceholderIcon(ICON_SIZE, ICON_SIZE, Constants.INVENTORY_COLOR);
        cargoIcon = createPlaceholderIcon(ICON_SIZE, ICON_SIZE, Constants.CARGO_COLOR);
        playerIcon = createPlaceholderIcon(ICON_SIZE, ICON_SIZE, Constants.SALVAGING_COLOR);
        crewIcon = createPlaceholderIcon(ICON_SIZE, ICON_SIZE, Constants.CREW_COLOR);
        alertIcon = createPlaceholderIcon(ICON_SIZE, ICON_SIZE, Constants.DANGER_COLOR);
        
        // Load actual icons with correct filenames and scale to consistent size
        inventoryIcon = loadAndScaleIcon("/com/idlemaster/icons/Inventory.png", inventoryIcon);
        cargoIcon = loadAndScaleIcon("/com/idlemaster/icons/cargo.png", cargoIcon);
        playerIcon = loadAndScaleIcon("/com/idlemaster/icons/player.png", playerIcon);
        crewIcon = loadAndScaleIcon("/com/idlemaster/icons/crew.png", crewIcon);
        alertIcon = loadAndScaleIcon("/com/idlemaster/icons/alert.png", alertIcon);
    }
    
    private BufferedImage loadAndScaleIcon(String path, BufferedImage fallback) {
        try {
            BufferedImage loaded = ImageUtil.loadImageResource(getClass(), path);
            if (loaded != null) {
                // Scale to consistent size
                if (loaded.getWidth() != ICON_SIZE || loaded.getHeight() != ICON_SIZE) {
                    BufferedImage scaled = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = scaled.createGraphics();
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.drawImage(loaded, 0, 0, ICON_SIZE, ICON_SIZE, null);
                    g2d.dispose();
                    return scaled;
                }
                return loaded;
            }
        } catch (Exception e) { /* Use fallback */ }
        return fallback;
    }
    
    private BufferedImage createPlaceholderIcon(int width, int height, Color color) {
        BufferedImage icon = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = icon.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(color);
        g2d.fillOval(2, 2, width - 4, height - 4);
        g2d.dispose();
        return icon;
    }
    
    public void updateDisplay() {
        SwingUtilities.invokeLater(() -> {
            updateBoatHealthDisplay();
            updateInventoryDisplay();
            updateCargoDisplay();
            updatePlayerStatusDisplay();
            updateCrewStatusDisplay();
            updateMonsterAlertDisplay();
            updateXpBarDisplay();
            contentPanel.repaint();
        });
    }
    
    private void updateBoatHealthDisplay() {
        if (config.showBoatHealth()) {
            // The health bar panel repaints itself with current salvageInfo values
            boatHealthPanel.repaint();
        }
    }
    
    private void updateInventoryDisplay() {
        if (config.showInventory()) {
            inventoryLabel.setText("Inv: " + salvageInfo.getInventoryText());
            int inventoryPercent = salvageInfo.getInventoryPercentage();
            inventoryLabel.setForeground(getCargoColor(inventoryPercent));
        }
    }
    
    private void updateCargoDisplay() {
        if (config.showCargo()) {
            cargoLabel.setText("Cargo: " + salvageInfo.getCargoText());
            int cargoPercent = salvageInfo.getCargoPercentage();
            cargoLabel.setForeground(getCargoColor(cargoPercent));
        }
    }
    
    /**
     * Returns a color that smoothly transitions from green (0%) to red (100%)
     * Green -> Yellow -> Orange -> Red
     */
    private Color getCargoColor(int percent) {
        // Clamp percent to 0-100
        percent = Math.max(0, Math.min(100, percent));
        
        int r, g, b;
        if (percent <= 50) {
            // Green to Yellow (0-50%): increase red from 0 to 255
            float ratio = percent / 50.0f;
            r = (int) (255 * ratio);
            g = 255;
            b = 0;
        } else {
            // Yellow to Red (50-100%): decrease green from 255 to 0
            float ratio = (percent - 50) / 50.0f;
            r = 255;
            g = (int) (255 * (1 - ratio));
            b = 0;
        }
        
        return new Color(r, g, b);
    }
    
    private void updatePlayerStatusDisplay() {
        if (config.showPlayerStatus()) {
            String statusText = "Player: " + salvageInfo.getPlayerStatusText();
            // Add idle timer if player is idle
            if (!salvageInfo.isPlayerSalvaging() && !salvageInfo.isPlayerSortingSalvage()) {
                statusText += salvageInfo.getIdleTimeText();
            }
            playerStatusLabel.setText(statusText);
            if (salvageInfo.isPlayerSalvaging() || salvageInfo.isPlayerSortingSalvage()) {
                playerStatusLabel.setForeground(Constants.SALVAGING_COLOR); // Green for both salvaging and sorting
            } else {
                playerStatusLabel.setForeground(Constants.IDLE_COLOR);
            }
        }
    }
    
    private void updateCrewStatusDisplay() {
        if (config.showCrewStatus()) {
            crewStatusLabel.setText("Crew: " + salvageInfo.getCrewStatusText());
            if (salvageInfo.getCrewCount() == 0) {
                crewStatusLabel.setForeground(Constants.DARK_TEXT_COLOR);
            } else if (salvageInfo.isCrewSalvaging()) {
                crewStatusLabel.setForeground(Constants.CREW_COLOR);
            } else {
                crewStatusLabel.setForeground(Constants.IDLE_COLOR);
            }
        }
    }
    
    private void updateMonsterAlertDisplay() {
        if (config.showMonsterAlert()) {
            monsterAlertLabel.setText("Alert: " + salvageInfo.getMonsterAlertText());
            if (salvageInfo.isMonsterAttacking()) {
                monsterAlertLabel.setForeground(Constants.DANGER_COLOR);
                startFlashTimer(); // Start flashing background
            } else {
                monsterAlertLabel.setForeground(Constants.SAFE_COLOR);
                stopFlashTimer(); // Stop flashing when safe
            }
        }
    }
    
    private void updateXpBarDisplay() {
        if (config.showXpBar()) {
            // The XP bar panel repaints itself with current salvageInfo values
            xpBarPanel.repaint();
        }
    }
}
