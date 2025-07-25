package com.dylibso.chicory.runtime.internal;

import com.dylibso.chicory.runtime.ExecutionListener;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;

/**
 * This class is experimental and may change in future releases.
 * It provides a way to copy an Instance with options for execution listeners and import values.
 * Copying an Instance is dangerous and should be done with caution.  Potential issues include:
 * <ul>
 * <li>Linking breaks.</li>
 * <li>Stateful host functions break: therefore it also breaks WASI.
 * </ul>
 */
public final class ExperimentalCopier {

    private ExperimentalCopier() {
        // Prevent instantiation
    }

    /**
     * Options for copying Instances.
     * This class allows setting an execution listener and import values for the copy operation.
     */
    public static class Options {
        private ExecutionListener listener;
        private ImportValues imports;

        /**
         * Set this if you want to replace the execution listener set on the original Instance.
         */
        public Options withListener(ExecutionListener listener) {
            this.listener = listener;
            return this;
        }

        /**
         * Set this if you want to replace the imports used on the original Instance.
         *
         * @param imports The import values to use when copying the Instance.
         * @return This Options instance for method chaining.
         */
        public Options withImports(ImportValues imports) {
            this.imports = imports;
            return this;
        }

        public ExecutionListener listener() {
            return listener;
        }

        public ImportValues imports() {
            return imports;
        }
    }

    /**
     * Creates a new Options instance for copying Instances.
     *
     * @return A new Options instance with default settings.
     */
    public static Options options() {
        return new Options();
    }

    /**
     * Copies an Instance with default options.
     *
     * @param original The original Instance to copy.
     * @return A new Instance that is a copy of the original.
     */
    public static Instance copy(Instance original) {
        return copy(original, options());
    }

    /**
     * Copies an Instance with the specified options.
     *
     * @param original The original Instance to copy.
     * @param options  The options for copying, including execution listener and import values.
     * @return A new Instance that is a copy of the original.
     */
    public static Instance copy(Instance original, Options options) {
        // We have to subclass Instance to access this protected constructor
        return new Instance(original, options) {};
    }
}
