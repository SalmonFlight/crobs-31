# CROBS Project Work Division

Member A: User, Auth & Room Domain
Member B: Booking & Scheduling Domain
Member C: Infrastructure, Logging & Queries
Shared: Reports, Manual, Presentation, Testing, Code Quality

## Member A: User, Auth & Room Domain

Requirements: REQ2, REQ3, REQ4, REQ5, REQ6, REQ7, REQ8, REQ9
Bonus Task: Undo/Redo for User & Room commands

### 1. Model Logic (hk.edu.polyu.comp.comp2021.crobs.model)
* User.java, RegularUser.java, Admin.java, SuperAdmin.java, Role.java, RoomType.java: Finish constructors, getters, and equals()/hashCode().
* UserRepository.java, RoomRepository.java: Implement the Map storage methods (add, get, remove, getAll).
* UserService.java: Implement addUser, removeUser (must call cancelBookingsByUser), and changeRole.
* RoomService.java: Implement addRoom, modifyRoom, deleteRoom (must call cancelFutureBookingsForRoom), getRoom, and getAllRooms.

### 2. Command Classes (hk.edu.polyu.comp.comp2021.crobs.command)
* LoginCommand.java, LogoutCommand.java
* AddUserCommand.java, RemoveUserCommand.java, ChangeRoleCommand.java
* AddRoomCommand.java, ModifyRoomCommand.java, DeleteRoomCommand.java
* BON2 Task: Implement the undo() and redo() methods inside these commands.

### 3. Testing (JUnit)
* Write tests for UserService and RoomService. Verify IDs are generated, users/rooms can be deleted, and cascade deletions work.

## Member B: Booking & Scheduling Domain

Requirements: REQ10, REQ11, REQ12, REQ14
Bonus Task: Undo/Redo for Booking commands

### 1. Model Logic (hk.edu.polyu.comp.comp2021.crobs.model)
* Booking.java: Finish the constructor, getters, and equals()/hashCode().
* BookingRepository.java: Implement the Map storage methods.
* BookingService.java:
    * Implement createBooking, modifyBooking, deleteBooking.
    * Implement isRoomAvailable (check date and LocalTime overlap).
    * Implement getSchedule (apply the rule: Regular users only see their own IDs, Admins see all).
    * Implement cancelBookingsByUser and cancelFutureBookingsForRoom.

### 2. Command Classes (hk.edu.polyu.comp.comp2021.crobs.command)
* BookCommand.java
* ModifyBookingCommand.java, DeleteBookingCommand.java
* ScheduleCommand.java
* BON2 Task: Implement undo() and redo() in these booking commands.

### 3. Testing (JUnit)
* Write tests for BookingService. Test valid bookings, overlapping time conflicts, modifying to a conflicting time, and schedule visibility rules.

## Member C: Infrastructure, Logging & Queries

Requirements: REQ1, REQ13, REQ15, REQ16, REQ17, REQ18
Bonus Task: Undo/Redo Engine & GUI integration

### 1. Infrastructure Logic (hk.edu.polyu.comp.comp2021.crobs.controller & root)
* CommandLogger.java: Implement REQ1. Write to log.html and log.txt.
* CommandDispatcher.java: Build the Map of commands. Implement dispatch to split input, catch errors, and log commands.
* Application.java: Build the main method. Parse -html and -txt args, initialize everything, run the while loop.
* BON2 Task: Build the Stack<Command> logic in the Application or Dispatcher to handle the undo and redo user commands.
* BON1 Task: Build the Swing GUI later, ensuring it passes inputs to the CommandDispatcher.

### 2. Command Classes (hk.edu.polyu.comp.comp2021.crobs.command)
* SearchCommand.java
* ListRoomCommand.java, ListAllRoomsCommand.java
* ListBookingsCommand.java
* QuitCommand.java

### 3. Testing (JUnit)
* Write integration tests verifying the Dispatcher routes correctly and the Logger creates the text/HTML files.

## Shared Responsibilities

### 1. Code Quality (20 Points)
Everyone must ensure COMP2021_PROJECT.xml is active in their IntelliJ. Fix all yellow/red warnings in your own files before opening a Pull Request.

### 2. Unit Tests (10 Points)
Target 90%+ line coverage on the model package.

### 3. Presentation (7 Points)
* A: Explain the User -> Admin -> SuperAdmin inheritance hierarchy.
* B: Explain the Command interface polymorphism and conflict detection.
* C: Explain the MVC architecture, the Command pattern, and how it allowed for Undo/Redo and GUI.

### 4. Project Report & User Manual (15 Points)
* Each member writes the report sections for their specific requirements.
* Each member provides screenshots for their commands for the User Manual.

## Git Workflow Rules
1. Never push to main directly. It is protected.
2. Create a branch for every requirement (e.g., feature/REQ2-add-user).
3. Push your branch and open a Pull Request on GitHub.
4. Get one approval from another member before merging.
5. Always pull the latest main before starting a new branch.