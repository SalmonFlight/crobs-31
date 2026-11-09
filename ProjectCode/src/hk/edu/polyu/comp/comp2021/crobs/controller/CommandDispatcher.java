package hk.edu.polyu.comp.comp2021.crobs.controller;

import hk.edu.polyu.comp.comp2021.crobs.command.Command;
import hk.edu.polyu.comp.comp2021.crobs.command.CommandResult;

import java.util.HashMap;
import java.util.Map;

/**
 * Routes user input to the appropriate command.
 */
public class CommandDispatcher {
    private final Map<String, Command> commands = new HashMap<>();
    private final CommandLogger logger;
    private final SessionContext session;

    /**
     * Constructs a CommandDispatcher.
     *
     * @param logger the logger for commands
     * @param session the current session context
     */
    public CommandDispatcher(CommandLogger logger, SessionContext session) {
        this.logger = logger;
        this.session = session;
    }

    /**
     * Registers a command to the dispatcher.
     *
     * @param command the command to register
     */
    public void register(Command command) {
        commands.put(command.getName(), command);
    }

    /**
     * Dispatches a raw input string to the appropriate command.
     *
     * @param inputLine the raw input string
     * @return true if the application should keep running, false if it should quit
     */
    public boolean dispatch(String inputLine) {
        String[] tokens = inputLine.trim().split("\\s+");
        if (tokens.length == 0 || tokens[0].isEmpty()) return true;

        String commandName = tokens[0];
        Command command = commands.get(commandName);

        if (command == null) {
            System.out.println("Unknown command: " + commandName);
            return true;
        }

        try {
            CommandResult result = command.execute(tokens, session);
            logger.log(inputLine); // REQ1: Log successful commands

            if (result != null) {
                System.out.println(result.getMessage());
                // If the command is "quit", we return false to stop the loop in Application
                if (commandName.equals("quit")) {
                    return false;
                }
            }
        } catch (CrobsException e) {
            System.out.println("Error: " + e.getMessage());
            logger.log(inputLine); // REQ1: Log failed commands too
        } catch (Exception e) {
            System.out.println("Unexpected error: " + e.getMessage());
        }
        return true;
    }
}