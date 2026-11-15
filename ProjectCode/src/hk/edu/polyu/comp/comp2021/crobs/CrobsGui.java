package hk.edu.polyu.comp.comp2021.crobs;

import hk.edu.polyu.comp.comp2021.crobs.controller.CommandDispatcher;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * A graphical user interface for the CROBS application.
 */
public class CrobsGui extends JFrame {

    private final CommandDispatcher dispatcher;
    private final JTextArea outputArea;
    private final JTextField inputField;

    /**
     * Constructs the GUI.
     *
     * @param dispatcher the command dispatcher
     */
    public CrobsGui(CommandDispatcher dispatcher) {
        this.dispatcher = dispatcher;

        setTitle("CROBS - Campus Room Booking System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());

        // --- 1. Top Toolbar with Buttons ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.setBackground(new Color(45, 45, 45));

        JButton roomsBtn = createButton("List Rooms", "listAllRooms");
        JButton bookingsBtn = createButton("My Bookings", "listBookings");
        JButton undoBtn = createButton("Undo", "undo");
        JButton redoBtn = createButton("Redo", "redo");

        buttonPanel.add(roomsBtn);
        buttonPanel.add(bookingsBtn);
        buttonPanel.add(undoBtn);
        buttonPanel.add(redoBtn);

        mainPanel.add(buttonPanel, BorderLayout.NORTH);

        // --- 2. Output Area (Dark Mode) ---
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setBackground(new Color(30, 30, 30)); // Dark background
        outputArea.setForeground(new Color(0, 255, 0));  // Green text
        outputArea.setCaretColor(Color.WHITE);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(null);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // --- 3. Bottom Input Area ---
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(new Color(45, 45, 45));

        inputField = new JTextField();
        inputField.setBackground(new Color(60, 60, 60));
        inputField.setForeground(Color.WHITE);
        inputField.setCaretColor(Color.WHITE);
        inputField.setFont(new Font("Monospaced", Font.PLAIN, 14));
        inputField.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

        inputPanel.add(inputField, BorderLayout.CENTER);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        redirectSystemOut();

        // --- 4. Action Listeners ---
        inputField.addActionListener(e -> {
            String input = inputField.getText().trim();
            if (!input.isEmpty()) {
                outputArea.append("\n> " + input + "\n");
                boolean keepRunning = this.dispatcher.dispatch(input);
                inputField.setText("");
                if (!keepRunning) System.exit(0);
            }
        });

        setVisible(true);
    }

    /**
     * Helper to create a styled button.
     */
    private JButton createButton(String title, String command) {
        JButton button = new JButton(title);
        button.setBackground(new Color(70, 130, 180));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.addActionListener(e -> {
            outputArea.append("\n> " + command + "\n");
            dispatcher.dispatch(command);
        });
        return button;
    }

    /**
     * Redirects console output to the GUI text area safely.
     */
    private void redirectSystemOut() {
        OutputStream out = new OutputStream() {
            private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            @Override
            public void write(int b) {
                if (b == '\n') {
                    final String text = baos.toString() + "\n";
                    baos.reset();
                    SwingUtilities.invokeLater(() -> {
                        outputArea.append(text);
                        outputArea.setCaretPosition(outputArea.getDocument().getLength());
                    });
                } else {
                    baos.write(b);
                }
            }
        };
        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
    }
}