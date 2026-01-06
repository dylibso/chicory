plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.junit5)
}

android {
    namespace = "com.dylibso.runtimeTests"
    compileSdk = 35

    defaultConfig {
        minSdk = 28

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    val chicoryDimension = "chicoryDimension"
    flavorDimensions += chicoryDimension
    productFlavors {
        create("runtime") { dimension = chicoryDimension }
        // add future modules similar to the runtime configuration above.
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set("11")
        }
    }

    packaging {
        resources {
            pickFirsts.add("logging.properties")
            excludes.add("META-INF/jpms.args")
        }
    }
}

dependencies {
    // "androidTestRuntimeImplementation" name here comes from Android's product
    // flavor convention. androidTest<productFlavorName>Implementation
    addLibraryTests(configurationName = "androidTestRuntimeImplementation", libraryPath = "runtime")
    addLibraryTests(configurationName = "androidTestRuntimeImplementation", libraryPath = "wasi")
    // common dependencies can be added here
    // if you need to add a dependency on a specific module, you can use
    // "androidTest<productFlavorName>Implementation"(<your dependency>)
    // e.g.
    // "androidTestRuntimeImplementation"(libs.chicory.runtime)
    androidTestImplementation(libs.chicory.wasi)
    androidTestImplementation(libs.chicory.runtime)
    androidTestImplementation(libs.chicory.wasm)
    androidTestImplementation(libs.chicory.wasmCorpus)
    androidTestImplementation(libs.junit.jupiter.api)
}

/**
 * Creates a jar of all built test classes from the given library path.
 *
 * The target project ([libraryPath]) requires the maven-test-jar plugin configured for the tests of
 * the target maven project. See the pom file for runtime project for a sample setup.
 *
 * @param configurationName Target Gradle configuration name
 * @param libraryPath Library path relative to the main chicory maven project
 */
fun addLibraryTests(configurationName: String, libraryPath: String) {
    // Add the jar task's output as a dependency.
    // Gradle will figure out that it needs to run the task before compiling the
    // project.
    project.dependencies.add(
        configurationName,
        project.dependencies.create(
            project.rootProject.files("../$libraryPath/target").asFileTree.matching {
                include("*tests.jar")
            }
        ),
    )
}
