package hk.edu.polyu.comp.comp2021.crobs;

import hk.edu.polyu.comp.comp2021.crobs.controller.CommandDispatcher;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A GUI for CROBS.
 */
public class CrobsGui extends JFrame {

    // --- Window Dimensions & Sizes ---
    private static final int WINDOW_WIDTH = 1050;
    private static final int WINDOW_HEIGHT = 750;
    private static final int SIDEBAR_WIDTH = 230;
    private static final int TITLE_BAR_HEIGHT = 40;
    private static final int TRAFFIC_LIGHT_SIZE = 12;
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 32;
    private static final int SCROLLBAR_WIDTH = 12;
    private static final int INPUT_ROUND = 12;
    private static final int FONT_SIZE = 14;

    // --- Layout Gaps ---
    private static final int GAP_2 = 2;
    private static final int GAP_5 = 5;
    private static final int GAP_8 = 8;
    private static final int GAP_10 = 10;
    private static final int GAP_15 = 15;
    private static final int GAP_20 = 20;

    // --- Colors ---
    private static final Color MAC_BG = new Color(30, 30, 30);
    private static final Color MAC_SIDEBAR = new Color(40, 40, 40);
    private static final Color MAC_TERMINAL_BG = new Color(20, 20, 20);
    private static final Color MAC_BORDER = new Color(60, 60, 60);
    private static final Color MAC_TEXT = new Color(230, 230, 230);
    private static final Color MAC_ACCENT = new Color(10, 132, 255);
    private static final Color MAC_GREEN = new Color(48, 209, 88);
    private static final Color HOVER_GRAY = new Color(70, 70, 70);
    private static final Color TEXT_MUTED = new Color(150, 150, 150);
    private static final Color TEXT_DARK_GRAY = new Color(120, 120, 120);
    private static final Color SCROLL_THUMB = new Color(100, 100, 100);
    private static final Color MAC_RED = new Color(255, 95, 86);
    private static final Color MAC_YELLOW = new Color(255, 189, 46);
    private static final Color MAC_GREEN_BTN = new Color(39, 201, 63);

    // --- Fonts ---
    private static final Font FONT_SANS = new Font("Helvetica Neue", Font.PLAIN, FONT_SIZE);
    private static final Font FONT_SANS_BOLD = new Font("Helvetica Neue", Font.BOLD, FONT_SIZE);
    private static final Font FONT_MONO = new Font("Menlo", Font.PLAIN, FONT_SIZE);

    private final CommandDispatcher dispatcher;
    private final JTextPane outputPane;
    private final JTextField inputField;

    // Command History State
    private final List<String> commandHistory = new ArrayList<>();
    private int historyIndex = 0;
    private String currentDraft = "";

    /**
     * Constructs the GUI.
     *
     * @param dispatcher the command dispatcher
     */
    public CrobsGui(CommandDispatcher dispatcher) {
        this.dispatcher = dispatcher;
        this.outputPane = new JTextPane();
        this.inputField = new JTextField();

        setTitle("CROBS - Campus Room Booking System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(MAC_BG);

        // ==========================================
        // 1. Title Bar
        // ==========================================
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(MAC_SIDEBAR);
        titleBar.setPreferredSize(new Dimension(0, TITLE_BAR_HEIGHT));
        titleBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, MAC_BORDER));

        JPanel trafficPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, GAP_8, GAP_10));
        trafficPanel.setOpaque(false);

        // Passing method references instead of anonymous lambdas completely fixes the complexity error
        trafficPanel.add(new TrafficLight(MAC_RED, this::closeApp));
        trafficPanel.add(new TrafficLight(MAC_YELLOW, this::minimizeApp));
        trafficPanel.add(new TrafficLight(MAC_GREEN_BTN, this::toggleMaximize));

        titleBar.add(trafficPanel, BorderLayout.WEST);

        JLabel title = new JLabel("Terminal — crobs-31");
        title.setForeground(TEXT_MUTED);
        title.setFont(FONT_SANS_BOLD);
        title.setHorizontalAlignment(JLabel.CENTER);
        titleBar.add(title, BorderLayout.CENTER);

        JPanel rightSpacer = new JPanel();
        rightSpacer.setOpaque(false);
        rightSpacer.setPreferredSize(new Dimension(TRAFFIC_LIGHT_SIZE * 5, 0));
        titleBar.add(rightSpacer, BorderLayout.EAST);

        add(titleBar, BorderLayout.NORTH);

        // ==========================================
        // 2. Main Split (Sidebar & Terminal)
        // ==========================================
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(MAC_BG);

        // --- Sidebar ---
        JPanel sidebar = new JPanel();
        sidebar.setBackground(MAC_SIDEBAR);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, MAC_BORDER));

        sidebar.add(Box.createRigidArea(new Dimension(0, GAP_20)));
        sidebar.add(createMacSidebarButton("Rooms Dashboard", "listAllRooms"));
        sidebar.add(createMacSidebarButton("My Bookings", "listBookings"));
        sidebar.add(createMacSidebarButton("Search", "search 2026-11-20 09:00 11:00 20 none"));
        sidebar.add(Box.createRigidArea(new Dimension(0, GAP_20)));
        sidebar.add(createMacSidebarButton("Undo", "undo"));
        sidebar.add(createMacSidebarButton("Redo", "redo"));

        sidebar.add(Box.createVerticalGlue());

        JButton clearBtn = createMacSidebarButton("Clear Screen", "clear");
        clearBtn.addActionListener(e -> outputPane.setText(""));
        sidebar.add(clearBtn);
        sidebar.add(Box.createRigidArea(new Dimension(0, GAP_20)));

        mainPanel.add(sidebar, BorderLayout.WEST);

        // --- Terminal Area ---
        JPanel terminalArea = new JPanel(new BorderLayout());
        terminalArea.setBackground(MAC_TERMINAL_BG);

        outputPane.setEditable(false);
        outputPane.setBackground(MAC_TERMINAL_BG);
        outputPane.setForeground(MAC_TEXT);
        outputPane.setFont(FONT_MONO);
        outputPane.setCaretColor(MAC_TEXT);
        outputPane.setBorder(new EmptyBorder(GAP_15, GAP_20, GAP_15, GAP_20));

        JScrollPane scrollPane = new JScrollPane(outputPane);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(MAC_TERMINAL_BG);

        JScrollBar vertical = scrollPane.getVerticalScrollBar();
        vertical.setPreferredSize(new Dimension(SCROLLBAR_WIDTH, 0));
        vertical.setUI(new ModernScrollBarUI());

        terminalArea.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(terminalArea, BorderLayout.CENTER);

        // ==========================================
        // 3. Input Area
        // ==========================================
        JPanel inputWrapper = new JPanel(new BorderLayout());
        inputWrapper.setBackground(MAC_BG);
        inputWrapper.setBorder(new EmptyBorder(GAP_10, GAP_20, GAP_10, GAP_20));

        RoundedPanel inputContainer = new RoundedPanel();
        inputContainer.setLayout(new BorderLayout());
        inputContainer.setBorder(new EmptyBorder(GAP_8, GAP_15, GAP_8, GAP_15));

        JLabel prompt = new JLabel("❯ ");
        prompt.setForeground(MAC_ACCENT);
        prompt.setFont(new Font("Menlo", Font.BOLD, FONT_SIZE + 2));
        inputContainer.add(prompt, BorderLayout.WEST);

        inputField.setBackground(MAC_SIDEBAR);
        inputField.setForeground(MAC_TEXT);
        inputField.setCaretColor(MAC_ACCENT);
        inputField.setFont(FONT_MONO);
        inputField.setBorder(null);
        inputContainer.add(inputField, BorderLayout.CENTER);

        inputWrapper.add(inputContainer, BorderLayout.CENTER);
        mainPanel.add(inputWrapper, BorderLayout.SOUTH);

        add(mainPanel);
        redirectSystemOut();

        // --- Listeners ---
        inputField.addActionListener(e -> executeInput());
        inputField.addKeyListener(new HistoryKeyListener());

        // Welcome Message
        appendToConsole("Last login: " + java.time.LocalDate.now() + " on ttys001\n", TEXT_DARK_GRAY);
        appendToConsole("CROBS (Campus Room Booking System) v1.0\n", MAC_TEXT);
        appendToConsole("Type a command or use the sidebar shortcuts. Use ↑/↓ for history.\n\n", TEXT_DARK_GRAY);

        setVisible(true);
        inputField.requestFocusInWindow();
    }

    // --- Window Action Methods (Fixes anonymous class complexity) ---

    private void closeApp() {
        System.exit(0);
    }

    private void minimizeApp() {
        setState(JFrame.ICONIFIED);
    }

    private void toggleMaximize() {
        if (getExtendedState() != JFrame.MAXIMIZED_BOTH) {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        } else {
            setExtendedState(JFrame.NORMAL);
        }
    }

    // -------------------------------------------------------------

    private void executeInput() {
        String input = inputField.getText().trim();
        if (!input.isEmpty()) {
            commandHistory.add(input);
            historyIndex = commandHistory.size();
            currentDraft = "";

            appendToConsole("\n❯ " + input + "\n", MAC_ACCENT);
            boolean keepRunning = this.dispatcher.dispatch(input);
            inputField.setText("");
            if (!keepRunning) System.exit(0);
        }
    }

    private JButton createMacSidebarButton(String title, String command) {
        JButton btn = new JButton(title);
        btn.setMaximumSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
        btn.setAlignmentX(CENTER_ALIGNMENT);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setForeground(MAC_TEXT);
        btn.setFont(FONT_SANS);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(GAP_5, GAP_15, GAP_5, GAP_15));

        btn.addMouseListener(new SidebarButtonHoverListener(btn));

        if (!command.equals("clear")) {
            btn.addActionListener(e -> {
                appendToConsole("\n❯ " + command + "\n", MAC_ACCENT);
                dispatcher.dispatch(command);
                inputField.requestFocusInWindow();
            });
        }
        return btn;
    }

    private void appendToConsole(String text, Color color) {
        StyledDocument doc = outputPane.getStyledDocument();
        SimpleAttributeSet style = new SimpleAttributeSet();
        StyleConstants.setForeground(style, color);
        try {
            doc.insertString(doc.getLength(), text, style);
            outputPane.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            System.err.println("Error appending text: " + e.getMessage());
        }
    }

    private void redirectSystemOut() {
        OutputStream out = new GuiOutputStream();
        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
    }

    // ==========================================
    // Custom Inner Classes
    // ==========================================

    private class HistoryKeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_UP) {
                e.consume();
                if (historyIndex > 0) {
                    if (historyIndex == commandHistory.size()) {
                        currentDraft = inputField.getText();
                    }
                    historyIndex--;
                    inputField.setText(commandHistory.get(historyIndex));
                }
            } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                e.consume();
                if (historyIndex < commandHistory.size()) {
                    historyIndex++;
                    if (historyIndex == commandHistory.size()) {
                        inputField.setText(currentDraft);
                    } else {
                        inputField.setText(commandHistory.get(historyIndex));
                    }
                }
            }
        }
    }

    private class GuiOutputStream extends OutputStream {
        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        @Override
        public void write(int b) {
            if (b == '\n') {
                final String text = new String(baos.toByteArray(), StandardCharsets.UTF_8) + "\n";
                baos.reset();
                SwingUtilities.invokeLater(() -> appendToConsole(text, MAC_GREEN));
            } else {
                baos.write(b);
            }
        }
    }

    private static class SidebarButtonHoverListener extends MouseAdapter {
        private final JButton btn;

        SidebarButtonHoverListener(JButton btn) {
            this.btn = btn;
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            btn.setContentAreaFilled(true);
            btn.setBackground(HOVER_GRAY);
            btn.setOpaque(true);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            btn.setContentAreaFilled(false);
            btn.setOpaque(false);
        }
    }

    private static class RoundedPanel extends JPanel {
        RoundedPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(MAC_SIDEBAR);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), INPUT_ROUND, INPUT_ROUND);
            g2.setColor(MAC_BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, INPUT_ROUND, INPUT_ROUND);
            g2.dispose();
        }
    }

    /**
     * TrafficLight now implements MouseListener directly.
     * This completely eliminates the anonymous MouseAdapter and its complexity warning.
     */
    private static class TrafficLight extends JPanel implements MouseListener {
        private final Color color;
        private final Runnable onClickAction;

        TrafficLight(Color color, Runnable onClickAction) {
            this.color = color;
            this.onClickAction = onClickAction;
            setOpaque(false);
            setPreferredSize(new Dimension(TRAFFIC_LIGHT_SIZE, TRAFFIC_LIGHT_SIZE));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            addMouseListener(this); // <--- Using 'this' instead of 'new MouseAdapter()'
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            onClickAction.run();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            // Not used
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            // Not used
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            // Not used
        }

        @Override
        public void mouseExited(MouseEvent e) {
            // Not used
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(0, 0, TRAFFIC_LIGHT_SIZE, TRAFFIC_LIGHT_SIZE);
            g2.dispose();
        }
    }

    private static class ModernScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = SCROLL_THUMB;
            this.trackColor = MAC_TERMINAL_BG;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton button = new JButton();
            Dimension zero = new Dimension(0, 0);
            button.setPreferredSize(zero);
            button.setMinimumSize(zero);
            button.setMaximumSize(zero);
            return button;
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(thumbColor);
            g2.fillRoundRect(thumbBounds.x + GAP_2, thumbBounds.y + GAP_2,
                    thumbBounds.width - GAP_5, thumbBounds.height - GAP_5, GAP_10, GAP_10);
            g2.dispose();
        }
    }
}