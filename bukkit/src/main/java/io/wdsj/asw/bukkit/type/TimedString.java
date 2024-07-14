package io.wdsj.asw.bukkit.type;

/**
 * A class that represents a string with timestamp.
 * @author HaHaWTH
 */
public class TimedString {
    private final String message;
    private final long time;
    public TimedString(String message, long time) {
        this.message = message;
        this.time = time;
    }

    public String getMessage() {
        return message;
    }

    public long getTime() {
        return time;
    }
}
