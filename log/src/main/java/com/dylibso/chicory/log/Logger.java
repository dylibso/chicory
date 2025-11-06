package com.dylibso.chicory.log;

import com.google.errorprone.annotations.FormatMethod;
import java.util.Objects;
import java.util.function.Supplier;

//
// for convenience, we are inspired by java.lang.System:Logger.Level
//
public interface Logger {
    enum Level {
        ALL(Integer.MIN_VALUE),
        TRACE(400),
        DEBUG(500),
        INFO(800),
        WARNING(900),
        ERROR(1000),
        OFF(Integer.MAX_VALUE);

        private final int severity;

        Level(int severity) {
            this.severity = severity;
        }

        /**
         * Returns the name of this level.
         *
         * @return this level {@linkplain #name()}.
         */
        public final String getName() {
            return name();
        }

        /**
         * Returns the severity of this level.
         * A higher severity means a more severe condition.
         *
         * @return this level severity.
         */
        public final int getSeverity() {
            return severity;
        }
    }

    void log(Level level, String msg, Throwable throwable);

    boolean isLoggable(Level level);

    //
    // TRACE
    //

    default void trace(String msg) {
        Objects.requireNonNull(msg);

        if (isLoggable(Level.TRACE)) {
            log(Level.TRACE, msg, null);
        }
    }

    default void trace(Supplier<String> msgSupplier) {
        Objects.requireNonNull(msgSupplier);

        if (isLoggable(Level.TRACE)) {
            log(Level.TRACE, msgSupplier.get(), null);
        }
    }

    default void trace(Supplier<String> msgSupplier, Throwable throwable) {
        Objects.requireNonNull(msgSupplier);

        if (isLoggable(Level.TRACE)) {
            log(Level.TRACE, msgSupplier.get(), throwable);
        }
    }

    default void tracef(String format, Object... args) {
        Objects.requireNonNull(format);

        if (isLoggable(Level.TRACE)) {
            String msg = format;
            if (args.length != 0) {
                msg = String.format(msg, args);
            }

            log(Level.TRACE, msg, null);
        }
    }

    //
    // DEBUG
    //

    default void debug(String msg) {
        Objects.requireNonNull(msg);

        if (isLoggable(Level.DEBUG)) {
            log(Level.DEBUG, msg, null);
        }
    }

    default void debug(Supplier<String> msgSupplier) {
        Objects.requireNonNull(msgSupplier);

        if (isLoggable(Level.DEBUG)) {
            log(Level.DEBUG, msgSupplier.get(), null);
        }
    }

    default void debug(Supplier<String> msgSupplier, Throwable throwable) {
        Objects.requireNonNull(msgSupplier);

        if (isLoggable(Level.DEBUG)) {
            log(Level.DEBUG, msgSupplier.get(), throwable);
        }
    }

    default void debugf(String format, Object... args) {
        Objects.requireNonNull(format);

        if (isLoggable(Level.DEBUG)) {
            String msg = format;
            if (args.length != 0) {
                msg = String.format(msg, args);
            }

            log(Level.DEBUG, msg, null);
        }
    }

    //
    // INFO
    //

    default void info(String msg) {
        Objects.requireNonNull(msg);

        if (isLoggable(Level.INFO)) {
            log(Level.INFO, msg, null);
        }
    }

    default void info(Supplier<String> msgSupplier) {
        Objects.requireNonNull(msgSupplier);

        if (isLoggable(Level.INFO)) {
            log(Level.INFO, msgSupplier.get(), null);
        }
    }

    default void info(Supplier<String> msgSupplier, Throwable throwable) {
        Objects.requireNonNull(msgSupplier);

        if (isLoggable(Level.INFO)) {
            log(Level.INFO, msgSupplier.get(), throwable);
        }
    }

    @FormatMethod
    default void infof(String format, Object... args) {
        Objects.requireNonNull(format);

        if (isLoggable(Level.INFO)) {
            String msg = format;
            if (args.length != 0) {
                msg = String.format(msg, args);
            }

            log(Level.INFO, msg, null);
        }
    }

    //
    // WARNING
    //

    default void warn(String msg) {
        Objects.requireNonNull(msg);

        if (isLoggable(Level.WARNING)) {
            log(Level.WARNING, msg, null);
        }
    }

    default void warn(Supplier<String> msgSupplier) {
        Objects.requireNonNull(msgSupplier);

        if (isLoggable(Level.WARNING)) {
            log(Level.WARNING, msgSupplier.get(), null);
        }
    }

    default void warn(Supplier<String> msgSupplier, Throwable throwable) {
        Objects.requireNonNull(msgSupplier);

        if (isLoggable(Level.WARNING)) {
            log(Level.WARNING, msgSupplier.get(), throwable);
        }
    }

    default void warnf(String format, Object... args) {
        Objects.requireNonNull(format);

        if (isLoggable(Level.WARNING)) {
            String msg = format;
            if (args.length != 0) {
                msg = String.format(msg, args);
            }

            log(Level.WARNING, msg, null);
        }
    }

    //
    // ERROR
    //

    default void error(String msg) {
        Objects.requireNonNull(msg);

        if (isLoggable(Level.ERROR)) {
            log(Level.ERROR, msg, null);
        }
    }

    default void error(Supplier<String> msgSupplier) {
        Objects.requireNonNull(msgSupplier);

        if (isLoggable(Level.ERROR)) {
            log(Level.ERROR, msgSupplier.get(), null);
        }
    }

    default void error(Supplier<String> msgSupplier, Throwable throwable) {
        Objects.requireNonNull(msgSupplier);

        if (isLoggable(Level.ERROR)) {
            log(Level.ERROR, msgSupplier.get(), throwable);
        }
    }

    default void errorf(String format, Object... args) {
        Objects.requireNonNull(format);

        if (isLoggable(Level.ERROR)) {
            String msg = format;
            if (args.length != 0) {
                msg = String.format(msg, args);
            }

            log(Level.ERROR, msg, null);
        }
    }
}
