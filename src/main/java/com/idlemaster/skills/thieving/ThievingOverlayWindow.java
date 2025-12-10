package com.idlemaster.skills.thieving;

import com.idlemaster.IdleMasterConfig;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

/**
 * Floating overlay window for thieving activities.
 */
public class ThievingOverlayWindow extends JFrame {
    
    // UI Constants
    private static final class Constants {
        static final int RESIZE_BORDER = 5;
        static final int MIN_WIDTH = 150;
        static final int MIN_HEIGHT = 80;
        static final int MAX_WIDTH = 400;
        static final int MAX_HEIGHT = 300;
        static final int DEFAULT_WIDTH = 180;
        static final int DEFAULT_HEIGHT = 100;
        static final int TITLE_BAR_HEIGHT = 20;
        static final int FONT_SIZE = 12;
        static final int ICON_SIZE = 16;
        
        // Colors
        static final Color BACKGROUND_COLOR = new Color(30, 30, 30, 230);
        static final Color TITLE_BAR_COLOR = new Color(45, 45, 45, 255);
        static final Color TEXT_COLOR = new Color(255, 255, 255);
        static final Color DARK_TEXT_COLOR = new Color(128, 128, 128);
        static final Color THIEVING_COLOR = new Color(150, 50, 200); // Purple for thieving
        static final Color IDLE_COLOR = new Color(255, 100, 100);
        static final Color ACTIVE_COLOR = new Color(100, 255, 100);
        static final Color WARNING_COLOR = new Color(255, 165, 0); // Orange for warning
    }
    
    private final ThievingInfo thievingInfo;
    private final IdleMasterConfig config;
    private ConfigManager configManager;
    private JPanel contentPanel;
    private JPanel infoPanel;
    
    // Labels
    private JLabel playerStatusLabel;
    private JLabel distractionStatusLabel;
    private JLabel coinPouchLabel;
    private JPanel xpBarPanel;
    
    private JPanel titleBar;
    private JLabel characterNameLabel;
    
    // Icons
    private BufferedImage playerIcon;
    private BufferedImage distractionIcon;
    private BufferedImage coinPouchIcon;
    
    // Interaction state
    private Point dragPoint;
    private boolean isDragging = false;
    private boolean isResizing = false;
    private int resizeEdge = 0;
    
    public ThievingOverlayWindow(ThievingInfo thievingInfo, IdleMasterConfig config, ConfigManager configManager) {
        this.thievingInfo = thievingInfo;
        this.config = config;
        this.configManager = configManager;
        
        initializeWindow();
        loadIcons();
        setupLabels();
        setupLayout();
        setupInteraction();
        
        pack();
        loadPositionAndSize();
        validatePosition();
    }
    
    private void initializeWindow() {
        setTitle("Thieving");
        setType(Type.UTILITY);
        setUndecorated(true);
        setAlwaysOnTop(true);
        setBackground(new Color(0, 0, 0, 0));
        setMinimumSize(new Dimension(Constants.MIN_WIDTH, Constants.MIN_HEIGHT));
        setPreferredSize(new Dimension(Constants.DEFAULT_WIDTH, Constants.DEFAULT_HEIGHT));
        
        contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw rounded background
                g2d.setColor(Constants.BACKGROUND_COLOR);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                
                // Draw border
                g2d.setColor(Constants.TITLE_BAR_COLOR);
                g2d.setStroke(new BasicStroke(1));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                
                g2d.dispose();
            }
        };
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BorderLayout(5, 5));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(2, 8, 8, 8));
    }
    
    private void loadIcons() {
        playerIcon = createPlaceholderIcon(Constants.ICON_SIZE, Constants.ICON_SIZE, Constants.THIEVING_COLOR);
        playerIcon = loadAndScaleIcon("/com/idlemaster/icons/player.png", playerIcon);
        
        distractionIcon = createPlaceholderIcon(Constants.ICON_SIZE, Constants.ICON_SIZE, Constants.ACTIVE_COLOR);
        distractionIcon = loadAndScaleIcon("/com/idlemaster/icons/Thief_Wealthy_citizen.png", distractionIcon);
        
        coinPouchIcon = createPlaceholderIcon(Constants.ICON_SIZE, Constants.ICON_SIZE, Constants.WARNING_COLOR);
        coinPouchIcon = loadAndScaleIcon("/com/idlemaster/icons/coin_pouch.png", coinPouchIcon);
    }
    
    private BufferedImage loadAndScaleIcon(String path, BufferedImage fallback) {
        try {
            BufferedImage loaded = ImageUtil.loadImageResource(getClass(), path);
            if (loaded != null) {
                if (loaded.getWidth() != Constants.ICON_SIZE || loaded.getHeight() != Constants.ICON_SIZE) {
                    BufferedImage scaled = new BufferedImage(Constants.ICON_SIZE, Constants.ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = scaled.createGraphics();
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.drawImage(loaded, 0, 0, Constants.ICON_SIZE, Constants.ICON_SIZE, null);
                    g2d.dispose();
                    return scaled;
                }
                return loaded;
            }
        } catch (Exception e) {
            // Use fallback
        }
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
    
    private void setupLabels() {
        playerStatusLabel = createLabel("Player: IDLE", playerIcon);
        distractionStatusLabel = createLabel("Citizen: ALERT", distractionIcon);
        coinPouchLabel = createLabel("Pouches: 0/28", coinPouchIcon);
        xpBarPanel = createXpBarPanel();
    }
    
    private JLabel createLabel(String text, BufferedImage icon) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.PLAIN, Constants.FONT_SIZE));
        label.setForeground(Constants.TEXT_COLOR);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0)); // Add vertical spacing
        if (icon != null) {
            label.setIcon(new ImageIcon(icon));
            label.setIconTextGap(6);
        }
        return label;
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
                
                // Draw XP progress fill (purple for thieving)
                int xpPercent = thievingInfo.getXpProgressPercentage();
                int fillWidth = (int) (barWidth * (xpPercent / 100.0));
                
                g2d.setColor(new Color(150, 50, 200)); // Purple
                g2d.fillRoundRect(barX, barY, fillWidth, barHeight, 4, 4);
                
                // Draw border
                g2d.setColor(new Color(60, 60, 60));
                g2d.drawRoundRect(barX, barY, barWidth, barHeight, 4, 4);
                
                // Get level info
                int currentLevel = thievingInfo.getThievingLevel();
                int nextLevel = Math.min(currentLevel + 1, 99);
                
                g2d.setFont(new Font("Arial", Font.PLAIN, 10));
                FontMetrics fm = g2d.getFontMetrics();
                int textY = barY + ((barHeight - fm.getHeight()) / 2) + fm.getAscent();
                int padding = 4;
                
                // Left text - current level
                String leftText = "Lvl " + currentLevel;
                int leftTextX = barX + padding;
                
                // Right text - next level
                String rightText = currentLevel >= 99 ? "" : "Lvl " + nextLevel;
                int rightTextWidth = fm.stringWidth(rightText);
                int rightTextX = barX + barWidth - rightTextWidth - padding;
                
                // Center text - XP remaining
                String centerText = currentLevel >= 99 ? "Max Level" : thievingInfo.getXpRemainingText();
                int centerTextWidth = fm.stringWidth(centerText);
                int centerTextX = barX + (barWidth - centerTextWidth) / 2;
                
                // Draw left text
                g2d.setColor(Color.BLACK);
                g2d.drawString(leftText, leftTextX + 1, textY + 1);
                g2d.setColor(Color.WHITE);
                g2d.drawString(leftText, leftTextX, textY);
                
                // Draw center text
                g2d.setColor(Color.BLACK);
                g2d.drawString(centerText, centerTextX + 1, textY + 1);
                g2d.setColor(Color.WHITE);
                g2d.drawString(centerText, centerTextX, textY);
                
                // Draw right text
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
        
        if (config.showThievingDistractionStatus()) {
            infoPanel.add(distractionStatusLabel);
        }
        if (config.showThievingCoinPouches()) {
            infoPanel.add(coinPouchLabel);
        }
        if (config.showThievingPlayerStatus()) {
            infoPanel.add(playerStatusLabel);
        }
        if (config.showThievingXpBar()) {
            infoPanel.add(xpBarPanel);
        }
        
        contentPanel.add(infoPanel, BorderLayout.CENTER);
        
        titleBar = createTitleBar();
        contentPanel.add(titleBar, BorderLayout.NORTH);
        
        setContentPane(contentPanel);
    }
    
    private JPanel createTitleBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, Constants.TITLE_BAR_HEIGHT));
        
        characterNameLabel = new JLabel("");
        characterNameLabel.setFont(new Font("Arial", Font.BOLD, 11));
        characterNameLabel.setForeground(Constants.TEXT_COLOR);
        bar.add(characterNameLabel, BorderLayout.WEST);
        
        // Close button
        JLabel closeBtn = new JLabel("×");
        closeBtn.setFont(new Font("Arial", Font.BOLD, 16));
        closeBtn.setForeground(Constants.DARK_TEXT_COLOR);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setVisible(false);
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                closeBtn.setForeground(new Color(255, 100, 100));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                closeBtn.setForeground(Constants.DARK_TEXT_COLOR);
            }
        });
        bar.add(closeBtn, BorderLayout.EAST);
        
        return bar;
    }
    
    private void setupInteraction() {
        MouseAdapter dragAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point p = e.getPoint();
                int width = getWidth();
                int height = getHeight();
                
                // Check for resize edges
                if (p.x > width - Constants.RESIZE_BORDER && p.y > height - Constants.RESIZE_BORDER) {
                    isResizing = true;
                    resizeEdge = 3; // SE corner
                    setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
                } else if (p.x > width - Constants.RESIZE_BORDER) {
                    isResizing = true;
                    resizeEdge = 1; // E edge
                    setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                } else if (p.y > height - Constants.RESIZE_BORDER) {
                    isResizing = true;
                    resizeEdge = 2; // S edge
                    setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
                } else if (p.y < Constants.TITLE_BAR_HEIGHT + 10) {
                    isDragging = true;
                    dragPoint = p;
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (isDragging || isResizing) {
                    savePositionAndSize();
                }
                isDragging = false;
                isResizing = false;
                resizeEdge = 0;
                setCursor(Cursor.getDefaultCursor());
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDragging && dragPoint != null) {
                    Point location = getLocation();
                    setLocation(location.x + e.getX() - dragPoint.x,
                               location.y + e.getY() - dragPoint.y);
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
                } else if (p.y < Constants.TITLE_BAR_HEIGHT + 10) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                } else {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };
        
        addMouseListener(dragAdapter);
        addMouseMotionListener(dragAdapter);
    }
    
    private void validatePosition() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle screenBounds = ge.getMaximumWindowBounds();
        
        Point location = getLocation();
        if (location.x < 0) location.x = 0;
        if (location.y < 0) location.y = 0;
        if (location.x + getWidth() > screenBounds.width) {
            location.x = screenBounds.width - getWidth();
        }
        if (location.y + getHeight() > screenBounds.height) {
            location.y = screenBounds.height - getHeight();
        }
        setLocation(location);
    }
    
    private void loadPositionAndSize() {
        if (configManager == null) return;
        
        try {
            String xStr = configManager.getConfiguration("idlemaster", "thievingWindowX");
            String yStr = configManager.getConfiguration("idlemaster", "thievingWindowY");
            String widthStr = configManager.getConfiguration("idlemaster", "thievingWindowWidth");
            String heightStr = configManager.getConfiguration("idlemaster", "thievingWindowHeight");
            
            int x = xStr != null ? Integer.parseInt(xStr) : 100;
            int y = yStr != null ? Integer.parseInt(yStr) : 100;
            int width = widthStr != null ? Integer.parseInt(widthStr) : Constants.DEFAULT_WIDTH;
            int height = heightStr != null ? Integer.parseInt(heightStr) : Constants.DEFAULT_HEIGHT;
            
            // Clamp to valid range
            width = Math.max(Constants.MIN_WIDTH, Math.min(Constants.MAX_WIDTH, width));
            height = Math.max(Constants.MIN_HEIGHT, Math.min(Constants.MAX_HEIGHT, height));
            
            setLocation(x, y);
            setSize(width, height);
        } catch (NumberFormatException e) {
            setLocation(100, 100);
            setSize(Constants.DEFAULT_WIDTH, Constants.DEFAULT_HEIGHT);
        }
    }
    
    public void savePositionAndSize() {
        if (configManager == null) return;
        
        configManager.setConfiguration("idlemaster", "thievingWindowX", String.valueOf(getX()));
        configManager.setConfiguration("idlemaster", "thievingWindowY", String.valueOf(getY()));
        configManager.setConfiguration("idlemaster", "thievingWindowWidth", String.valueOf(getWidth()));
        configManager.setConfiguration("idlemaster", "thievingWindowHeight", String.valueOf(getHeight()));
    }
    
    public void updateDisplay() {
        SwingUtilities.invokeLater(() -> {
            updateDistractionStatusDisplay();
            updateCoinPouchDisplay();
            updatePlayerStatusDisplay();
            updateXpBarDisplay();
            contentPanel.repaint();
        });
    }
    
    private void updateDistractionStatusDisplay() {
        if (config.showThievingDistractionStatus()) {
            String statusText = "Citizen: " + thievingInfo.getDistractionStatusText();
            String timeText = thievingInfo.getDistractionTimeText();
            distractionStatusLabel.setText(statusText + timeText);
            
            if (thievingInfo.isCitizenDistracted()) {
                distractionStatusLabel.setForeground(Constants.ACTIVE_COLOR);
            } else {
                // Orange if <60s since last distraction, red if >=60s
                int timeSince = thievingInfo.getTimeSinceLastDistraction();
                if (timeSince > 0 && timeSince < 60) {
                    distractionStatusLabel.setForeground(Constants.WARNING_COLOR);
                } else {
                    distractionStatusLabel.setForeground(Constants.IDLE_COLOR);
                }
            }
        }
    }
    
    private void updateCoinPouchDisplay() {
        if (config.showThievingCoinPouches()) {
            String pouchText = "Pouches: " + thievingInfo.getCoinPouchText();
            coinPouchLabel.setText(pouchText);
            
            // Color based on fill percentage: green (low) -> orange (medium) -> red (full)
            int percentage = thievingInfo.getCoinPouchPercentage();
            if (thievingInfo.isCoinPouchFull()) {
                coinPouchLabel.setForeground(Constants.IDLE_COLOR); // Red when full
            } else if (percentage >= 75) {
                coinPouchLabel.setForeground(Constants.WARNING_COLOR); // Orange when 75%+
            } else {
                coinPouchLabel.setForeground(Constants.ACTIVE_COLOR); // Green when low
            }
        }
    }
    
    private void updatePlayerStatusDisplay() {
        if (config.showThievingPlayerStatus()) {
            String statusText = "Player: " + thievingInfo.getPlayerStatusText();
            String idleTime = thievingInfo.getIdleTimeText();
            playerStatusLabel.setText(statusText + idleTime);
            
            if (thievingInfo.isPlayerThieving()) {
                playerStatusLabel.setForeground(Constants.ACTIVE_COLOR);
            } else {
                playerStatusLabel.setForeground(Constants.IDLE_COLOR);
            }
        }
    }
    
    private void updateXpBarDisplay() {
        if (config.showThievingXpBar()) {
            xpBarPanel.repaint();
        }
    }
    
    public void updateCharacterName(String name) {
        if (characterNameLabel != null) {
            SwingUtilities.invokeLater(() -> {
                if (name != null && !name.isEmpty()) {
                    characterNameLabel.setText(name + " - Thieving");
                } else {
                    characterNameLabel.setText("");
                }
            });
        }
    }
    
    public void updateConfig() {
        SwingUtilities.invokeLater(() -> {
            distractionStatusLabel.setVisible(config.showThievingDistractionStatus());
            coinPouchLabel.setVisible(config.showThievingCoinPouches());
            playerStatusLabel.setVisible(config.showThievingPlayerStatus());
            xpBarPanel.setVisible(config.showThievingXpBar());
            
            rebuildInfoPanel();
            updateDisplay();
        });
    }
    
    private void rebuildInfoPanel() {
        contentPanel.remove(infoPanel);
        infoPanel.removeAll();
        
        if (config.showThievingDistractionStatus()) {
            infoPanel.add(distractionStatusLabel);
        }
        if (config.showThievingCoinPouches()) {
            infoPanel.add(coinPouchLabel);
        }
        if (config.showThievingPlayerStatus()) {
            infoPanel.add(playerStatusLabel);
        }
        if (config.showThievingXpBar()) {
            infoPanel.add(xpBarPanel);
        }
        
        contentPanel.add(infoPanel, BorderLayout.CENTER);
    }
}
