package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.controller.SessionContext;
import hk.edu.polyu.comp.comp2021.crobs.model.Room;
import hk.edu.polyu.comp.comp2021.crobs.model.RoomService;
import hk.edu.polyu.comp.comp2021.crobs.model.RoomType;

import java.util.Objects;
import java.util.Set;

/** Creates a room on behalf of an administrator (REQ7). */
public final class AddRoomCommand extends AbstractUndoableCommand {
    private static final int ARGUMENT_COUNT = 4;
    private static final int CAPACITY_ARGUMENT_INDEX = 2;
    private static final int FEATURES_ARGUMENT_INDEX = 3;
    private final RoomService roomService;

    /**
     * Constructs the command.
     * @param roomService service managing rooms
     */
    public AddRoomCommand(RoomService roomService) {
        this.roomService = Objects.requireNonNull(roomService, "roomService");
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "addRoom";
    }

    /** {@inheritDoc} */
    @Override
    public CommandResult execute(String[] args, SessionContext session) throws CrobsException {
        session.requireAdmin();
        CommandArguments.requireCount(args, getName(), ARGUMENT_COUNT, "addRoom roomType capacity features");
        RoomType type;
        int capacity;
        try {
            type = RoomType.fromCommandValue(args[1]);
            capacity = Integer.parseInt(args[CAPACITY_ARGUMENT_INDEX]);
        } catch (IllegalArgumentException exception) {
            throw new CrobsException("Use a valid room type and an integer capacity.");
        }
        Set<String> features = CommandArguments.parseFeatures(args[FEATURES_ARGUMENT_INDEX]);
        Room room = remember(roomService.addRoomWithUndo(type, capacity, features));
        return CommandResult.success("Added room " + room.getId() + ".", room);
    }
}
