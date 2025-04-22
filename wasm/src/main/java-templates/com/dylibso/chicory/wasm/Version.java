package com.dylibso.chicory.wasm;

/**
 * This class provides the version of the project.
 * The version is set during the build process and can be accessed using the {@link #version()} method.
 */
public final class Version {

    private Version() {
    }

    /**
     * Returns the version of the project.
     *
     * @return The version of the project as a String.
     */
    public static String version() {
        return "${project.version}";
    }

}
