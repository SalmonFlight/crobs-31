package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.model.RoomService;

import java.util.Set;

/** Validation shared by Member A's command parsers. */
final class CommandArguments {
    private CommandArguments() {
    }

    /**
     * Requires the exact token count, including the command name.
     * @param args dispatcher tokens
     * @param name expected command name
     * @param count required token count
     * @param usage usage text shown for invalid arguments
     * @throws CrobsException when the arguments are malformed
     */
    static void requireCount(String[] args, String name, int count, String usage)
            throws CrobsException {
        requireMinimum(args, name, count, usage);
        if (args.length != count) {
            throw new CrobsException("Usage: " + usage);
        }
    }

    /**
     * Requires a minimum token count and nonblank arguments.
     * @param args dispatcher tokens
     * @param name expected command name
     * @param count minimum token count
     * @param usage usage text shown for invalid arguments
     * @throws CrobsException when the arguments are malformed
     */
    static void requireMinimum(String[] args, String name, int count, String usage)
            throws CrobsException {
        if (args == null || args.length < count || !name.equals(args[0])) {
            throw new CrobsException("Usage: " + usage);
        }
        for (String arg : args) {
            if (arg == null || arg.trim().isEmpty()) {
                throw new CrobsException("Usage: " + usage);
            }
        }
    }

    /**
     * Parses features consistently for room creation and modification.
     * @param value comma-separated names, or '-' for none
     * @return the feature set
     * @throws CrobsException when a feature name is malformed
     */
    static Set<String> parseFeatures(String value) throws CrobsException {
        return RoomService.parseFeatures(value);
    }
}
