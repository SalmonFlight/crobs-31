package hk.edu.polyu.comp.comp2021.crobs.controller;

import hk.edu.polyu.comp.comp2021.crobs.command.Command;
import hk.edu.polyu.comp.comp2021.crobs.command.CommandResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Routes user input to the appropriate command.
 */
public class CommandDispatcher {
    private final Map<String, Command> commands = new HashMap<>();
    private final CommandLogger logger;
    private final SessionContext session;

    // BON2: Stacks for Undo and Redo
    private final Stack<Command> undoStack = new Stack<>();
    private final Stack<Command> redoStack = new Stack<>();

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

        // BON2: Handle Undo and Redo manually
        if (commandName.equals("undo")) {
            handleUndo(inputLine);
            return true;
        }
        if (commandName.equals("redo")) {
            handleRedo(inputLine);
            return true;
        }

        Command command = commands.get(commandName);
        if (command == null) {
            System.out.println("Unknown command: " + commandName);
            return true;
        }

        try {
            CommandResult result = command.execute(tokens, session);
            logger.log(inputLine);

            if (result != null) {
                System.out.println(result.getMessage());

                // BON2: If successful, push to undo stack and clear redo stack
                // (We don't push commands that can't be undone, like list or login)
                if (result.isSuccess() && isUndoable(commandName)) {
                    undoStack.push(command);
                    redoStack.clear();
                }

                if (commandName.equals("quit")) {
                    return false;
                }
            }
        } catch (CrobsException e) {
            System.out.println("Error: " + e.getMessage());
            logger.log(inputLine);
        } catch (Exception e) {
            System.out.println("Unexpected error: " + e.getMessage());
        }
        return true;
    }

    private void handleUndo(String inputLine) {
        if (undoStack.isEmpty()) {
            System.out.println("Nothing to undo.");
            return;
        }
        Command command = undoStack.pop();
        try {
            command.undo();
            redoStack.push(command);
            System.out.println("Undid: " + command.getName());
            logger.log(inputLine);
        } catch (CrobsException e) {
            System.out.println("Error during undo: " + e.getMessage());
            undoStack.push(command); // Put it back if it failed
        }
    }

    private void handleRedo(String inputLine) {
        if (redoStack.isEmpty()) {
            System.out.println("Nothing to redo.");
            return;
        }
        Command command = redoStack.pop();
        try {
            command.redo();
            undoStack.push(command);
            System.out.println("Redid: " + command.getName());
            logger.log(inputLine);
        } catch (CrobsException e) {
            System.out.println("Error during redo: " + e.getMessage());
            redoStack.push(command); // Put it back if it failed
        }
    }

    // Helper to exclude commands from the undo stack (like quit, list, login)
    private boolean isUndoable(String commandName) {
        String[] nonUndoable = {"quit", "login", "logout", "listRoom", "listAllRooms", "listBookings", "search", "schedule"};
        for (String c : nonUndoable) {
            if (c.equals(commandName)) return false;
        }
        return true;
    }
}