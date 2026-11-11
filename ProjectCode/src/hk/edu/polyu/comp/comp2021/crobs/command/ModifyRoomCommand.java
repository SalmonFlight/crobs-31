package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.controller.SessionContext;
import hk.edu.polyu.comp.comp2021.crobs.model.Room;
import hk.edu.polyu.comp.comp2021.crobs.model.RoomService;

import java.util.Objects;

/** Modifies one room field on behalf of an administrator (REQ8). */
public final class ModifyRoomCommand extends AbstractUndoableCommand {
    private static final int ARGUMENT_COUNT = 4;
    private static final int FIELD_ARGUMENT_INDEX = 2;
    private static final int VALUE_ARGUMENT_INDEX = 3;
    private final RoomService roomService;

    /**
     * Constructs the command.
     * @param roomService service validating room changes
     */
    public ModifyRoomCommand(RoomService roomService) {
        this.roomService = Objects.requireNonNull(roomService, "roomService");
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "modifyRoom";
    }

    /** {@inheritDoc} */
    @Override
    public CommandResult execute(String[] args, SessionContext session) throws CrobsException {
        session.requireAdmin();
        CommandArguments.requireCount(args, getName(), ARGUMENT_COUNT, "modifyRoom roomID field newValue");
        Room room = remember(roomService.modifyRoomWithUndo(
                args[1], args[FIELD_ARGUMENT_INDEX], args[VALUE_ARGUMENT_INDEX]));
        return CommandResult.success("Modified room " + room.getId() + ".", room);
    }
}
