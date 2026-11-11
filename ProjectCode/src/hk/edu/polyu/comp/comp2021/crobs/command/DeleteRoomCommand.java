package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.controller.SessionContext;
import hk.edu.polyu.comp.comp2021.crobs.model.Room;
import hk.edu.polyu.comp.comp2021.crobs.model.RoomService;

import java.util.Objects;

/** Removes a room and cancels only its future bookings (REQ9). */
public final class DeleteRoomCommand extends AbstractUndoableCommand {
    private static final int ARGUMENT_COUNT = 2;
    private final RoomService roomService;

    /**
     * Constructs the command.
     * @param roomService service managing rooms and cancellation callbacks
     */
    public DeleteRoomCommand(RoomService roomService) {
        this.roomService = Objects.requireNonNull(roomService, "roomService");
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "deleteRoom";
    }

    /** {@inheritDoc} */
    @Override
    public CommandResult execute(String[] args, SessionContext session) throws CrobsException {
        session.requireAdmin();
        CommandArguments.requireCount(args, getName(), ARGUMENT_COUNT, "deleteRoom roomID");
        Room room = remember(roomService.deleteRoomWithUndo(args[1]));
        return CommandResult.success("Deleted room " + room.getId() + " and cancelled future bookings.", room);
    }
}
