package hk.edu.polyu.comp.comp2021.crobs.util;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/** Generates non-reused identifiers for Member A's users and rooms. */
public final class IdGenerator {
    private static final AtomicLong NEXT_USER = new AtomicLong(1);
    private static final AtomicLong NEXT_ROOM = new AtomicLong(1);

    private IdGenerator() {
    }

    /**
     * Generates a user identifier starting at U001.
     * @return a new user ID
     */
    public static String generateUserId() {
        return String.format(Locale.ROOT, "U%03d", NEXT_USER.getAndIncrement());
    }

    /**
     * Generates a room identifier starting at R001.
     * @return a new room ID
     */
    public static String generateRoomId() {
        return String.format(Locale.ROOT, "R%03d", NEXT_ROOM.getAndIncrement());
    }
}
