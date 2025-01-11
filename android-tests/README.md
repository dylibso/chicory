This is an Android project designed to run Chicory tests on device.

Since Chicory uses maven and Android doesn't have official maven
support, we cannot make Android testing part of the main build.

Instead, we use this project where it uses the outputs of the
main maven build to run those tests as Android instrumentation tests.

Inside the `device-tests` folder, there is an Android library project
setup. It doesn't have any code, except for declaring product flavors
for each main chicory project and setting up its dependencies to run
tests.

Inside the root `build.gradle.kts` file, we setup a task that creates a
maven repository from the outputs of the main project.

The dependencies between this project and the maven project are setup properly
such that, if the code in the main maven project changes, this Android project
will recompile the repository and run up-to-date tests.

To avoid re-building the main project (e.g. in CI), you can also pass
`CHICORY_REPO` environment variable, in which case, this Android project will
re-use its output instead of recompiling the main project.
(It won't make any attempt to compile their tests either)
```
mvn deploy -DaltDeploymentRepository=local-repo::default::file:./local-repo -DskipTests
cd android-tests && CHICORY_REPO=../local-repo ./gradlew device-tests:connectedCheck
```

Tests in this project can be run via:
```
// this will require a connected emulator
cd android-tests && ./gradlew device-tests:connectedCheck
```

Or, to run just one flavor (1 module from the main repo), you can use its test task:
```
// connected<mainModuleNameCapitalized>DebugAndroidTest
cd android-tests && ./gradlew device-tests:connectedRuntimeDebugAndroidTest
```
