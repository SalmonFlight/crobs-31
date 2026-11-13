package hk.edu.polyu.comp.comp2021.crobs.model;

/** The room categories supported by CROBS. */
public enum RoomType {
    /** A room arranged as a lecture theatre. */
    LECTURE_THEATRE("lectureTheatre"),
    /** A room equipped as a computer laboratory. */
    COMPUTER_LAB("computerLab"),
    /** A room arranged for seminars. */
    SEMINAR_ROOM("seminarRoom"),
    /** A room arranged for meetings. */
    MEETING_ROOM("meetingRoom");

    private final String commandValue;

    RoomType(String commandValue) {
        this.commandValue = commandValue;
    }

    /** @return the room type spelling accepted by the command interface */
    public String getCommandValue() {
        return commandValue;
    }

    /**
     * Parses a room type from the command interface.
     *
     * @param value the room type spelling
     * @return the corresponding room category
     * @throws IllegalArgumentException when the spelling is not a supported room type
     */
    public static RoomType fromCommandValue(String value) {
        for (RoomType type : values()) {
            if (type.commandValue.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown room type: " + value);
    }
}
