package hk.edu.polyu.comp.comp2021.crobs.controller;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Handles logging of commands to HTML and TXT files.
 */
public class CommandLogger {
    private final String htmlFilePath;
    private final String txtFilePath;
    private int commandIndex = 1;
    private boolean htmlHeaderWritten = false;

    /**
     * Constructs a CommandLogger with the specified file paths.
     *
     * @param htmlFilePath the path to the HTML log file
     * @param txtFilePath the path to the TXT log file
     */
    public CommandLogger(String htmlFilePath, String txtFilePath) {
        this.htmlFilePath = htmlFilePath;
        this.txtFilePath = txtFilePath;
    }

    /**
     * Logs a command to both the HTML and TXT files.
     *
     * @param command the command string to log
     */
    public void log(String command) {
        try (BufferedWriter txtWriter = new BufferedWriter(new FileWriter(txtFilePath, true));
             BufferedWriter htmlWriter = new BufferedWriter(new FileWriter(htmlFilePath, true))) {

            // Write to TXT file
            txtWriter.write(command);
            txtWriter.newLine();

            // Write to HTML file
            if (!htmlHeaderWritten) {
                htmlWriter.write("<html><body><table border='1'><tr><th>Index</th><th>Command</th></tr>\n");
                htmlHeaderWritten = true;
            }
            htmlWriter.write("<tr><td>" + commandIndex + "</td><td>" + command + "</td></tr>\n");
            commandIndex++;

        } catch (IOException e) {
            System.out.println("Error writing to log files: " + e.getMessage());
        }
    }
}