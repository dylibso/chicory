package com.dylibso.chicory.log;

public class SystemLogger implements Logger {
    private static final System.Logger LOGGER = System.getLogger("chicory");

    @Override
    public void log(Level level, String msg, Throwable throwable) {
        System.Logger.Level sll = toSystemLoggerLevel(level);

        LOGGER.log(sll, msg, throwable);
    }

    @Override
    public boolean isLoggable(Level level) {
        System.Logger.Level sll = toSystemLoggerLevel(level);

        return LOGGER.isLoggable(sll);
    }

    System.Logger.Level toSystemLoggerLevel(Logger.Level level) {
        switch (level) {
            case ALL:
                return System.Logger.Level.ALL;
            case TRACE:
                return System.Logger.Level.TRACE;
            case DEBUG:
                return System.Logger.Level.DEBUG;
            case INFO:
                return System.Logger.Level.INFO;
            case WARNING:
                return System.Logger.Level.WARNING;
            case ERROR:
                return System.Logger.Level.ERROR;
            default:
                throw new IllegalArgumentException("Unsupported logger level: " + level);
        }
    }
}
