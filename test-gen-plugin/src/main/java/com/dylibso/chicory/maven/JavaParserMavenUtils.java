package com.dylibso.chicory.maven;

import java.util.function.Supplier;
import org.apache.maven.plugin.logging.Log;

/**
 * Things to make JavaParser and Maven interact better
 */
public class JavaParserMavenUtils {

    public static void makeJavaParserLogToMavenOutput(Log log) {
        com.github.javaparser.utils.Log.setAdapter(new com.github.javaparser.utils.Log.Adapter() {
            @Override
            public void info(Supplier<String> message) {
                log.info(message.get());
            }

            @Override
            public void trace(Supplier<String> message) {
                log.debug(message.get());
            }

            @Override
            public void error(
                    Supplier<Throwable> throwableSupplier, Supplier<String> messageSupplier) {
                log.error(messageSupplier.get(), throwableSupplier.get());
            }
        });
    }
}
