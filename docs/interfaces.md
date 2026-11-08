# CROBS Final Skeleton & Architecture Contract

## 1. Core Constraints & Formats
- Date format: `yyyy-MM-dd` (java.time.LocalDate)
- Time format: `HH:mm` (java.time.LocalTime)
- Features: Stored as `java.util.Set<String>` (e.g., "projector", "whiteboard").
- No `System.out.println` in the `model` or `command` packages.
- All command outputs must be returned via `CommandResult`.
- `Command` must implement `undo()` and `redo()` to support BON2.

## 2. Utilities Package (`hk.edu.polyu.comp.comp2021.crobs.util`)
- `class IdGenerator`: Centralized static methods (e.g., `generateUserId()`, `generateRoomId()`, `generateBookingId()`). Guarantees unique IDs starting at U001, R001, B001.

## 3. Model Package (`hk.edu.polyu.comp.comp2021.crobs.model`)

### Enums
- `enum Role`: REGULAR, ADMIN, SUPERADMIN
- `enum RoomType`: LECTURE_THEATRE, COMPUTER_LAB, SEMINAR_ROOM, MEETING_ROOM

### User Hierarchy (Polymorphism)
- `abstract class User`: fields (id, name, role), getters, equals/hashCode.
- `class RegularUser extends User`
- `class Admin extends User`
- `class SuperAdmin extends Admin`

### Core Entities
- `class Room`: fields (id, type, capacity, features). Getters, equals/hashCode.
- `class Booking`: fields (id, userId, roomId, date, startTime, endTime, purpose). Getters, equals/hashCode.

### Repositories (Data Storage)
- `class UserRepository`, `RoomRepository`, `BookingRepository`
- Must use `Map<String, T>` for O(1) lookups by ID.
- Methods: `add(T item)`, `get(String id)`, `remove(String id)`, `getAll()`.

### Services (Business Logic & Validation)
- `class UserService`:
    - `User addUser(String name)`
    - `void removeUser(String targetUserID)` (calls BookingService to cancel bookings)
    - `void changeRole(String targetUserId, Role newRole)`
- `class RoomService`:
    - `Room addRoom(RoomType type, int capacity, Set<String> features)`
    - `void modifyRoom(String roomID, String field, String newValue)`
    - `void deleteRoom(String roomID)` (calls BookingService to cancel future bookings)
    - `Room getRoom(String roomID)`
    - `List<Room> getAllRooms()` (sorted by ID)
    - `List<Room> searchRooms(LocalDate date, LocalTime start, LocalTime end, int minCapacity, Set<String> requiredFeatures, BookingService bookingService)`
- `class BookingService`:
    - `Booking createBooking(String userId, String roomId, LocalDate date, LocalTime start, LocalTime end, String purpose)`
    - `void modifyBooking(String bookingId, String roomId, LocalDate date, LocalTime start, LocalTime end, String purpose)`
    - `void deleteBooking(String bookingId)`
    - `List<Booking> getSchedule(String roomId, LocalDate weekStartDate, String currentUserId, Role currentUserRole)`
    - `List<Booking> getBookingsByUser(String userId)`
    - `void cancelBookingsByUser(String userId)` (for REQ3)
    - `void cancelFutureBookingsForRoom(String roomId)` (for REQ9)
    - `boolean isRoomAvailable(String roomId, LocalDate date, LocalTime start, LocalTime end)`

## 4. Command Package (`hk.edu.polyu.comp.comp2021.crobs.command`)

### Command Interface
    public interface Command {
        String getName();
        CommandResult execute(String[] args, SessionContext session) throws CrobsException;
        void undo() throws CrobsException;
        void redo() throws CrobsException;
    }

### CommandResult
    public class CommandResult {
        private final boolean success;
        private final String message;
        private final Object payload; // e.g., List<Room> for GUI to display
        
        // Factory methods:
        // public static CommandResult success(String message, Object payload)
        // public static CommandResult failure(String message)
    }

## 5. Controller Package (`hk.edu.polyu.comp.comp2021.crobs.controller`)
- `class CrobsException extends Exception`: Custom checked exception for all expected errors (e.g., "Room not found", "Not authorized").
- `class SessionContext`: Manages logged-in user. Methods: `login(User user)`, `logout()`, `getCurrentUser()`, `requireLoggedIn()`, `requireAdmin()`, `requireSuperAdmin()`.
- `class CommandLogger`: Implements REQ1. Methods: `log(String rawCommand)`, `close()`.
- `class CommandDispatcher`: Holds a `Map<String, Command>`. Methods: `register(Command cmd)`, `dispatch(String rawCommand, SessionContext session)`.

## 6. Main Application (`hk.edu.polyu.comp.comp2021.crobs.Application`)
- Parses `-html` and `-txt` CLI arguments.
- Instantiates Repositories, Services, SessionContext, and CommandLogger.
- Instantiates all Command classes and registers them with CommandDispatcher.
- Runs a `while(running)` loop reading from `Scanner`. Passes input to `CommandDispatcher.dispatch()`.