package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.controller.SessionContext;
import hk.edu.polyu.comp.comp2021.crobs.model.BookingService;
import hk.edu.polyu.comp.comp2021.crobs.model.Room;
import hk.edu.polyu.comp.comp2021.crobs.model.RoomService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Command to search for available rooms based on criteria.
 */
public class SearchCommand implements Command {
    private static final int ARGUMENT_COUNT = 6;

    private final RoomService roomService;
    private final BookingService bookingService;

    /**
     * Constructs a SearchCommand.
     *
     * @param roomService the room service
     * @param bookingService the booking service
     */
    public SearchCommand(RoomService roomService, BookingService bookingService) {
        this.roomService = roomService;
        this.bookingService = bookingService;
    }

    /**
     * Gets the name of the command.
     *
     * @return the command name
     */
    @Override
    public String getName() { return "search"; }

    /**
     * Executes the search command.
     *
     * @param args the command arguments
     * @param session the current session context
     * @return the command result
     * @throws CrobsException if the input format is invalid
     */
    @Override
    public CommandResult execute(String[] args, SessionContext session) throws CrobsException {
        if (args.length != ARGUMENT_COUNT) {
            throw new CrobsException("Usage: search <date> <startTime> <endTime> <minCapacity> <features>");
        }

        try {
            LocalDate date = LocalDate.parse(args[1]);
            LocalTime startTime = LocalTime.parse(args[2]);
            LocalTime endTime = LocalTime.parse(args[3]);
            int minCapacity = Integer.parseInt(args[4]);

            Set<String> requiredFeatures = new HashSet<>();
            if (!args[5].equalsIgnoreCase("none") && !args[5].isEmpty()) {
                String[] featuresArray = args[5].split(",");
                for (String f : featuresArray) {
                    requiredFeatures.add(f.trim().toLowerCase());
                }
            }

            List<Room> allRooms = roomService.getAllRooms();
            StringBuilder sb = new StringBuilder("Available Rooms:\n");
            int count = 0;

            for (Room room : allRooms) {
                if (room.getCapacity() >= minCapacity && room.getFeatures().containsAll(requiredFeatures)) {
                    if (bookingService.isRoomAvailable(room.getId(), date, startTime, endTime)) {
                        sb.append(String.format("ID: %s | Type: %s | Capacity: %d | Features: %s\n",
                                room.getId(), room.getType(), room.getCapacity(), room.getFeatures()));
                        count++;
                    }
                }
            }

            if (count == 0) {
                return CommandResult.success("No rooms found matching your criteria.", null);
            }
            return CommandResult.success(sb.toString(), null);

        } catch (DateTimeParseException | NumberFormatException e) {
            throw new CrobsException("Invalid format. Ensure date is yyyy-MM-dd, time is HH:mm, and capacity is a number.");
        }
    }

    /**
     * Undoes the command.
     */
    @Override
    public void undo() {}

    /**
     * Redoes the command.
     */
    @Override
    public void redo() {}
}