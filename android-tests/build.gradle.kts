// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.junit5) apply false
}

/**
 * Creates a maven repository from the original project.
 *
 * It can run in 2 ways:
 * * prebuiltRepositoryArg: This parameter points to an environment variable that denotes where the
 *   "already built" repository exists. If provided, the task will simply copy it.
 * * compile main project: If `prebuiltRepositoryArg` is not provided, this task will compile the
 *   main project to build the repository.
 */
@CacheableTask
abstract class PrepareRepositoryTask
@Inject
constructor(private val execOps: ExecOperations, private val filesystemOps: FileSystemOperations) :
    DefaultTask() {
    @get:Input @get:Optional abstract val prebuiltRepositoryArg: Property<String>
    @get:Internal abstract val mainProjectDirectory: DirectoryProperty
    @get:Internal abstract val androidProjectDirectory: DirectoryProperty

    /**
     * Since our project is in the same directory as the main project, any changes would invalidate
     * the mvn publish task. To prevent this, we declare the [mainProjectDirectory] as an internal
     * input and instead explicitly declare what we depend on in [relevantFiles] to prevent
     * invalidations.
     */
    @Suppress("unused") // used by gradle for invalidation
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val relevantFiles: Provider<FileTree>
        get() =
            mainProjectDirectory.map {
                it.asFileTree.matching {
                    // mvn builds do not seem repeatable so we exclude its outputs.
                    // otherwise, just building them invalidates the task.
                    exclude("**/target/**")
                    // exclude our project
                    exclude(
                        androidProjectDirectory
                            .get()
                            .asFile
                            .relativeTo(mainProjectDirectory.get().asFile)
                            .path + "/**"
                    )
                }
            }

    @get:OutputDirectory abstract val repositoryLocation: DirectoryProperty

    @TaskAction
    fun prepareRepository() {
        repositoryLocation.get().asFile.let {
            it.deleteRecursively()
            it.mkdirs()
        }
        if (prebuiltRepositoryArg.isPresent) {
            val inputRepo = File(prebuiltRepositoryArg.get())
            check(inputRepo.exists()) {
                "Cannot find input repository in ${inputRepo.absolutePath}"
            }
            filesystemOps.copy {
                from(prebuiltRepositoryArg.get())
                into(repositoryLocation.get().asFile)
            }
        } else {
            execOps.exec {
                executable = "mvn"
                workingDir = mainProjectDirectory.get().asFile
                args(
                    "deploy",
                    "-DaltDeploymentRepository=local-repo::default::${repositoryLocation.get().asFile.toURI()}",
                    "-DskipTests",
                    "-Dspotless.skip=true",
                    "-DskipCheckStyle=true",
                )
            }
        }
    }
}

val localMavenRepoDir = project.layout.buildDirectory.get().asFile.resolve("chicory_repo")

val buildRepoTask: TaskProvider<PrepareRepositoryTask> =
    rootProject.tasks.register("prepareRepository", PrepareRepositoryTask::class) {
        prebuiltRepositoryArg.set(rootProject.providers.environmentVariable("CHICORY_REPO"))
        androidProjectDirectory.set(project.layout.projectDirectory)
        mainProjectDirectory.set(rootProject.layout.projectDirectory.dir("../."))
        repositoryLocation.set(localMavenRepoDir)
    }

project.subprojects {
    repositories {
        google() {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        maven { url = localMavenRepoDir.toURI() }
    }
    tasks.configureEach {
        // make sure local repository is built before running any tasks
        dependsOn(buildRepoTask)
    }
}
