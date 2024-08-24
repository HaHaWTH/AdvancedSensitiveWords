package io.wdsj.asw.common.datatype;

import java.util.Objects;

/**
 * A class that represents a string with timestamp.
 * <p>
 * Represents a string paired with a timestamp, typically used to track
 * when the string was generated or modified.
 * <p>
 * Example usage:
 * <pre>
 *     TimedString timedString = TimedString.of("Hello World", System.currentTimeMillis());
 * </pre>
 *
 * This class is immutable and thread-safe.
 *
 * @author HaHaWTH
 */
public class TimedString {
    private final String str;
    private final long time;

    private TimedString(String str, long time) {
        this.str = str;
        this.time = time;
    }

    public String getString() {
        return str;
    }

    public long getTime() {
        return time;
    }

    public static TimedString of(String str, long time) {
        Objects.requireNonNull(str, "String cannot be null");
        if (time < 0) throw new IllegalArgumentException("Time cannot be negative");
        return new TimedString(str, time);
    }

    public static TimedString of(String str) {
        return of(str, System.currentTimeMillis());
    }

    @Override
    public String toString() {
        return "TimedString{" +
                "str='" + str + '\'' +
                ", time=" + time +
                '}';
    }
}