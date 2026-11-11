package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.controller.SessionContext;
import hk.edu.polyu.comp.comp2021.crobs.model.Room;
import hk.edu.polyu.comp.comp2021.crobs.model.RoomService;

/**
 * Command to list information about a specific room.
 */
public class ListRoomCommand implements Command {
    private final RoomService roomService;

    /**
     * Constructs a ListRoomCommand.
     *
     * @param roomService the room service
     */
    public ListRoomCommand(RoomService roomService) {
        this.roomService = roomService;
    }

    /**
     * Gets the name of the command.
     *
     * @return the command name
     */
    @Override
    public String getName() { return "listRoom"; }

    /**
     * Executes the command.
     *
     * @param args the command arguments
     * @param session the current session context
     * @return the command result
     * @throws CrobsException if an error occurs
     */
    @Override
    public CommandResult execute(String[] args, SessionContext session) throws CrobsException {
        if (args.length != 2) {
            throw new CrobsException("Usage: listRoom <roomId>");
        }

        String roomId = args[1];
        Room room = roomService.getRoom(roomId);

        if (room == null) {
            throw new CrobsException("Room not found: " + roomId);
        }

        String output = String.format("Room ID: %s\nType: %s\nCapacity: %d\nFeatures: %s",
                room.getId(), room.getType(), room.getCapacity(), room.getFeatures());

        return CommandResult.success(output, room);
    }

    @Override
    public void undo() throws CrobsException {}

    @Override
    public void redo() throws CrobsException {}
}