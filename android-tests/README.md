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

## Environment Setup
You'll need Android build tools and a running emulator (or connected device with developer
mode) to build and run these tests.

The easiest way to obtain a working local setup is to use
[Android Studio](https://developer.android.com/studio).

* Download Android Studio from [this link](https://developer.android.com/studio).
  * Alternatively, you can use [Jetbrains Toolbox](https://www.jetbrains.com/toolbox-app/).
* Start Android Studio. It will guide you through the SDK installation.
* Next, open the project in Android Studio (`<checkout-root>/android-tests`). This will also
  automatically add a `local.properties` file, specifying your Android SDK location.
* Finally, go to `View > Tool Windows > Device Manager` and create an emulator. You can select
  any device-version configuration as long as it is at least API 33. See
  [documentation](https://developer.android.com/studio/run/managing-avds) for more details.


You can also complete the Android SDK setup using the
[command line tools](https://developer.android.com/tools) but the steps to follow will depend on
your operating system and might get fairly complicated
(see [github action](https://github.com/ReactiveCircus/android-emulator-runner/blob/main/src/sdk-installer.ts#L7)).
