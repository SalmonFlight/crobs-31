package hk.edu.polyu.comp.comp2021.crobs;

import hk.edu.polyu.comp.comp2021.crobs.command.*;
import hk.edu.polyu.comp.comp2021.crobs.controller.*;
import hk.edu.polyu.comp.comp2021.crobs.model.*;

import java.time.Clock;
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
        String htmlPath = "log.html";
        String txtPath = "log.txt";

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-html") && i + 1 < args.length) htmlPath = args[++i];
            if (args[i].equals("-txt") && i + 1 < args.length) txtPath = args[++i];
        }

        // 1. Initialize Controllers
        CommandLogger logger = new CommandLogger(htmlPath, txtPath);

        // 2. Initialize Repositories
        UserRepository userRepo = new UserRepository();
        RoomRepository roomRepo = new RoomRepository();
        BookingRepository bookingRepo = new BookingRepository();

        // 3. Initialize Services
        BookingService bookingService = new BookingService(bookingRepo, roomRepo, Clock.systemDefaultZone());
        UserService userService = new UserService(userRepo, bookingService);
        RoomService roomService = new RoomService(roomRepo, bookingService);

        SessionContext session = new SessionContext(userRepo);
        CommandDispatcher dispatcher = new CommandDispatcher(logger, session);

        // 4. Register ALL Commands
        // Member A (Users & Rooms)
        dispatcher.register(new AddUserCommand(userService));
        dispatcher.register(new RemoveUserCommand(userService));
        dispatcher.register(new ChangeRoleCommand(userService));
        dispatcher.register(new LoginCommand(userService));
        dispatcher.register(new LogoutCommand());
        dispatcher.register(new AddRoomCommand(roomService));
        dispatcher.register(new ModifyRoomCommand(roomService));
        dispatcher.register(new DeleteRoomCommand(roomService));

        // Member B (Bookings)
        dispatcher.register(new BookCommand(bookingService));
        dispatcher.register(new ModifyBookingCommand(bookingService));
        dispatcher.register(new DeleteBookingCommand(bookingService));
        dispatcher.register(new ScheduleCommand(bookingService));

        // Member C (Infrastructure & Queries)
        dispatcher.register(new SearchCommand(roomService, bookingService));
        dispatcher.register(new ListRoomCommand(roomService));
        dispatcher.register(new ListAllRoomsCommand(roomService));
        dispatcher.register(new ListBookingsCommand(bookingService));
        dispatcher.register(new UndoCommand());
        dispatcher.register(new RedoCommand());
        dispatcher.register(new QuitCommand());

        // 5. Start the REPL Loop
        System.out.println("CROBS started. Type 'quit' to exit.");

        // Auto-create the SuperAdmin (Required by the PDF)
        try {
            userService.getSuperAdmin();
        } catch (Exception e) {
            // Ignore if already created
        }

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