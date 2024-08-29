package io.wdsj.asw.common.datatype;

import org.jetbrains.annotations.NotNull;

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

    /**
     * Returns the string.
     * @return the string
     */
    public String getString() {
        return str;
    }

    /**
     * Returns the timestamp.
     * @return the timestamp
     */
    public long getTime() {
        return time;
    }

    /**
     * Returns a new TimedString instance.
     * @param str the string
     * @param time the timestamp
     * @return a new TimedString instance
     */
    public static TimedString of(@NotNull String str, long time) {
        Objects.requireNonNull(str, "String cannot be null");
        if (time < 0) throw new IllegalArgumentException("Time cannot be negative");
        return new TimedString(str, time);
    }

    /**
     * Returns a new TimedString instance with the current timestamp.
     * @param str the string
     * @return a new TimedString instance
     */
    public static TimedString of(@NotNull String str) {
        return of(str, System.currentTimeMillis());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimedString that = (TimedString) o;
        return time == that.time && str.equals(that.str);
    }

    @Override
    public int hashCode() {
        return Objects.hash(str, time);
    }

    @Override
    public String toString() {
        return "TimedString{" +
                "str='" + str + '\'' +
                ", time=" + time +
                '}';
    }
}