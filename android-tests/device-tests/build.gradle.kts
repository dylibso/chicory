import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly

val mainProjectDirectory = rootProject.projectDir.resolve("../.")

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.junit5)
}

android {
    namespace = "com.dylibso.runtimeTests"
    compileSdk = 35

    defaultConfig {
        minSdk = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    val chicoryDimension = "ourDimension"
    flavorDimensions += chicoryDimension
    productFlavors {
        create("runtime") { dimension = chicoryDimension }
        // add future modules similar to the runtime configuration above.
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
}

dependencies {
    // "androidTestRuntimeImplementation" name here comes from Android's product
    // flavor convention. androidTest<productFlavorName>Implementation
    addLibraryTests(configurationName = "androidTestRuntimeImplementation", libraryPath = "runtime")
    // common dependencies can be added here
    // if you need to add a dependency on a specific module, you can use
    // "androidTest<productFlavorName>Implementation"(<your dependency>)
    // e.g.
    // "androidTestRuntimeImplementation"(libs.chicory.runtime)
    androidTestImplementation(libs.chicory.runtime)
    androidTestImplementation(libs.chicory.wasm)
    androidTestImplementation(libs.chicory.wasmCorpus)
    androidTestImplementation(libs.junit.jupiter.api)
}

/**
 * Creates a jar of all built test classes from the given library path.
 *
 * @param configurationName Target configuration name
 * @param libraryPath Library path relative to the main chicory maven project
 */
fun addLibraryTests(configurationName: String, libraryPath: String) {
    val taskSuffix = libraryPath
    val jarTask =
        project.tasks.register(
            "jarTestClassesFor${taskSuffix.capitalizeAsciiOnly()}",
            Jar::class.java,
        ) {
            archiveBaseName.set("${taskSuffix}Tests")
            from(mainProjectDirectory.resolve(libraryPath).resolve("target/test-classes"))
            destinationDirectory.set(project.layout.buildDirectory.dir("testJars/$taskSuffix"))
        }
    // Add the jar task's output as a dependency.
    // Gradle will figure out that it needs to run the task before compiling the
    // project.
    project.dependencies.add(
        configurationName,
        project.dependencies.create(
            project
                .files({
                    jarTask.get().destinationDirectory.asFileTree.matching { include("*.jar") }
                })
                .builtBy(jarTask)
        ),
    )
}
