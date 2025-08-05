package com.dylibso.chicory.log;

public class AndroidLogger implements Logger {
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("chicory");

    @Override
    public void log(Level level, String msg, Throwable throwable) {
        LOGGER.log(toJavaLoggerLevel(level), msg, throwable);
    }

    @Override
    public boolean isLoggable(Level level) {
        return LOGGER.isLoggable(toJavaLoggerLevel(level));
    }

    java.util.logging.Level toJavaLoggerLevel(Logger.Level level) {
        switch (level) {
            case ALL:
                return java.util.logging.Level.ALL;
            case TRACE:
                return java.util.logging.Level.FINEST;
            case DEBUG:
                return java.util.logging.Level.FINE;
            case INFO:
                return java.util.logging.Level.INFO;
            case WARNING:
                return java.util.logging.Level.WARNING;
            case ERROR:
                return java.util.logging.Level.SEVERE;
            case OFF:
                return java.util.logging.Level.OFF;
            default:
                throw new IllegalArgumentException("Unsupported logger level: " + level);
        }
    }
}
