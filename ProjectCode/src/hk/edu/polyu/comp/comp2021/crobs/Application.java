package hk.edu.polyu.comp.comp2021.crobs;

import hk.edu.polyu.comp.comp2021.crobs.command.QuitCommand;
import hk.edu.polyu.comp.comp2021.crobs.controller.CommandDispatcher;
import hk.edu.polyu.comp.comp2021.crobs.controller.CommandLogger;
import hk.edu.polyu.comp.comp2021.crobs.controller.SessionContext;

import java.util.Scanner;

/**
 * Main application class for CROBS.
 */
public class Application {

    /**
     * Main method to start the CROBS application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        // Default log paths
        String htmlPath = "log.html";
        String txtPath = "log.txt";

        // Parse command line arguments for log paths
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-html") && i + 1 < args.length) {
                htmlPath = args[++i];
            }
            if (args[i].equals("-txt") && i + 1 < args.length) {
                txtPath = args[++i];
            }
        }

        // Initialize core components
        CommandLogger logger = new CommandLogger(htmlPath, txtPath);
        SessionContext session = new SessionContext();
        CommandDispatcher dispatcher = new CommandDispatcher(logger, session);

        // Register commands
        dispatcher.register(new QuitCommand());

        // Start the REPL (Read-Eval-Print Loop)
        System.out.println("CROBS started. Type 'quit' to exit.");
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.print("> ");
            if (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                running = dispatcher.dispatch(line);
            } else {
                break;
            }
        }

        scanner.close();
        System.out.println("CROBS closed.");
    }
}