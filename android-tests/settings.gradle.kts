pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal {
            val repoOverride = settings.providers.environmentVariable("CHICORY_REPO")
            val relativePath =
                if (repoOverride.isPresent) {
                    repoOverride.get()
                } else {
                    "local-repo"
                }
            // resolve it relative to the main maven project directory.
            val localRepoDirectory = settings.settingsDir.parentFile.resolve(relativePath)
            check(localRepoDirectory.exists()) {
                "Cannot find ${localRepoDirectory}, did you run  `mvn -Dandroid-prepare`"
            }
            url = localRepoDirectory.toURI()
        }
        google()
        mavenCentral()
    }
}

include(":device-tests")
