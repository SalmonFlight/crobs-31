package hk.edu.polyu.comp.comp2021.crobs;

import hk.edu.polyu.comp.comp2021.crobs.command.AddRoomCommand;
import hk.edu.polyu.comp.comp2021.crobs.command.AddUserCommand;
import hk.edu.polyu.comp.comp2021.crobs.command.BookCommand;
import hk.edu.polyu.comp.comp2021.crobs.command.ChangeRoleCommand;
import hk.edu.polyu.comp.comp2021.crobs.command.DeleteBookingCommand;
import hk.edu.polyu.comp.comp2021.crobs.command.DeleteRoomCommand;
import hk.edu.polyu.comp.comp2021.crobs.command.ListAllRoomsCommand;
import hk.edu.polyu.comp.comp2021.crobs.command.ListBookingsCommand;
import hk.edu.polyu.comp.comp2021.crobs.command.ListRoomCommand;
import hk.edu.polyu.comp.comp2021.crobs.command.LoginCommand;
import hk.edu.polyu.comp.comp2021.crobs.command.LogoutCommand;
import hk.edu.polyu.comp.comp2021.crobs.command.ModifyBookingCommand;
import hk.edu.polyu.comp.comp2021.crobs.command.ModifyRoomCommand;
import hk.edu.polyu.comp.comp2021.crobs.command.QuitCommand;
import hk.edu.polyu.comp.comp2021.crobs.command.RedoCommand;
import hk.edu.polyu.comp.comp2021.crobs.command.RemoveUserCommand;
import hk.edu.polyu.comp.comp2021.crobs.command.ScheduleCommand;
import hk.edu.polyu.comp.comp2021.crobs.command.SearchCommand;
import hk.edu.polyu.comp.comp2021.crobs.command.UndoCommand;
import hk.edu.polyu.comp.comp2021.crobs.controller.CommandDispatcher;
import hk.edu.polyu.comp.comp2021.crobs.controller.CommandLogger;
import hk.edu.polyu.comp.comp2021.crobs.controller.SessionContext;
import hk.edu.polyu.comp.comp2021.crobs.model.BookingRepository;
import hk.edu.polyu.comp.comp2021.crobs.model.BookingService;
import hk.edu.polyu.comp.comp2021.crobs.model.RoomRepository;
import hk.edu.polyu.comp.comp2021.crobs.model.RoomService;
import hk.edu.polyu.comp.comp2021.crobs.model.UserRepository;
import hk.edu.polyu.comp.comp2021.crobs.model.UserService;

import javax.swing.SwingUtilities;
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
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void main(String[] args) {
        String htmlPath = "log.html";
        String txtPath = "log.txt";
        boolean useGui = false;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-html") && i + 1 < args.length) htmlPath = args[++i];
            if (args[i].equals("-txt") && i + 1 < args.length) txtPath = args[++i];
            if (args[i].equals("--gui")) useGui = true;
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
        // Member A
        dispatcher.register(new AddUserCommand(userService));
        dispatcher.register(new RemoveUserCommand(userService));
        dispatcher.register(new ChangeRoleCommand(userService));
        dispatcher.register(new LoginCommand(userService));
        dispatcher.register(new LogoutCommand());
        dispatcher.register(new AddRoomCommand(roomService));
        dispatcher.register(new ModifyRoomCommand(roomService));
        dispatcher.register(new DeleteRoomCommand(roomService));

        // Member B
        dispatcher.register(new BookCommand(bookingService));
        dispatcher.register(new ModifyBookingCommand(bookingService));
        dispatcher.register(new DeleteBookingCommand(bookingService));
        dispatcher.register(new ScheduleCommand(bookingService));

        // Member C
        dispatcher.register(new SearchCommand(roomService, bookingService));
        dispatcher.register(new ListRoomCommand(roomService));
        dispatcher.register(new ListAllRoomsCommand(roomService));
        dispatcher.register(new ListBookingsCommand(bookingService));
        dispatcher.register(new UndoCommand());
        dispatcher.register(new RedoCommand());
        dispatcher.register(new QuitCommand());

        // Auto-create the SuperAdmin
        userService.getSuperAdmin();

        // 5. Launch GUI or CLI
        if (useGui) {
            SwingUtilities.invokeLater(() -> new CrobsGui(dispatcher));
        } else {
            runCli(dispatcher);
        }
    }

    private static void runCli(CommandDispatcher dispatcher) {
        System.out.println("CROBS started. Type 'quit' to exit.");
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