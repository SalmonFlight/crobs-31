package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.controller.SessionContext;
import hk.edu.polyu.comp.comp2021.crobs.model.Room;
import hk.edu.polyu.comp.comp2021.crobs.model.RoomService;

import java.util.List;

/**
 * Command to list all rooms in the system.
 */
public class ListAllRoomsCommand implements Command {
    private final RoomService roomService;

    /**
     * Constructs a ListAllRoomsCommand.
     *
     * @param roomService the room service
     */
    public ListAllRoomsCommand(RoomService roomService) {
        this.roomService = roomService;
    }

    /**
     * Gets the name of the command.
     *
     * @return the command name
     */
    @Override
    public String getName() { return "listAllRooms"; }

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
        List<Room> rooms = roomService.getAllRooms();

        if (rooms.isEmpty()) {
            return CommandResult.success("No rooms available.", null);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("All Rooms:\n");
        for (Room room : rooms) {
            sb.append(String.format("ID: %s | Type: %s | Capacity: %d | Features: %s\n",
                    room.getId(), room.getType(), room.getCapacity(), room.getFeatures()));
        }
        return CommandResult.success(sb.toString(), rooms);
    }

    @Override
    public void undo() throws CrobsException {}

    @Override
    public void redo() throws CrobsException {}
}