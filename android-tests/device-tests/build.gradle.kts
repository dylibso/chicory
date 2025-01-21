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
 * The target project ([libraryPath]) requires the maven-test-jar plugin configured for the tests of
 * the target maven project. See the pom file for runtime project for a sample setup.
 *
 * @param configurationName Target Gradle configuration name
 * @param libraryPath Library path relative to the main chicory maven project
 */
fun addLibraryTests(configurationName: String, libraryPath: String) {
    val jarTask =
        project.tasks.register<MavenTestJarTask>(
            "jarTestClassesFor${libraryPath.capitalizeAsciiOnly()}"
        ) {
            projectName.set(libraryPath)
            // always run this task because we don't track maven dependencies.
            // Its output jar is reproducible so it won't invalidate gradle caches.
            onlyIf { true }
            mainProjectDirectory.set(project.rootProject.layout.projectDirectory.dir("../."))
            outputDirectory.set(project.layout.buildDirectory.dir("testJars/$libraryPath"))
        }
    // Add the jar task's output as a dependency.
    // Gradle will figure out that it needs to run the task before compiling the
    // project.
    project.dependencies.add(
        configurationName,
        project.dependencies.create(
            project
                .files({ jarTask.get().outputDirectory.asFileTree.matching { include("*.jar") } })
                .builtBy(jarTask)
        ),
    )
}

/**
 * Compiles tests for a maven project and packages them into a jar.
 *
 * e.g. mvn -Ddev test-compile jar:test-jar -pl runtime
 */
@DisableCachingByDefault(because = "Uses maven")
abstract class MavenTestJarTask
@Inject
constructor(private val execOps: ExecOperations, private val fileOps: FileSystemOperations) :
    DefaultTask() {
    @get:Internal abstract val mainProjectDirectory: DirectoryProperty
    @get:Input abstract val projectName: Property<String>
    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun createJar() {
        val mainProjectDir = mainProjectDirectory.get().asFile
        execOps.exec {
            executable = "mvn"
            workingDir = mainProjectDir
            args("-Ddev", "test-compile", "jar:test-jar", "-pl", projectName.get())
        }
        // copy the output into our output directory
        fileOps.copy {
            from(mainProjectDir.resolve(projectName.get()).resolve("target")) {
                include("*tests.jar")
            }
            into(outputDirectory)
        }
    }
}
