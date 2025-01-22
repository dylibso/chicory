# Running Chicory tests on Android

This is an Android project designed to run Chicory tests on device.

Since Chicory uses maven and Android doesn't have official maven
support, we cannot make Android testing part of the main build.

Instead, we use this project where it uses the outputs of the
main maven build to run those tests as Android instrumentation tests.

Inside the `device-tests` folder, there is an Android library project
setup. It doesn't have any code, except for declaring product flavors
for each main chicory project and setting up its dependencies to run
tests.

To run the tests on Android you need first to build a local `CHICORY_REPO`:

```bash
mvn -Dandroid-prepare
```

The relevant artifacts will be produced in the `local-repo` directory.
Setup this folder as the source for the subsequent steps:

```bash
export CHICORY_REPO=${PWD}/local-repo
```

and finally you can run the tests from the gradle project:

```bash
./android-tests/gradlew -p android-tests device-tests:connectedCheck
```

## Environment Setup

You'll need Android build tools and a running emulator (or connected device with developer
mode) to build and run these tests.

The easiest way to obtain a working local setup is to use
[Android Studio](https://developer.android.com/studio).

* Download Android Studio from [this link](https://developer.android.com/studio).
* Start Android Studio. It will guide you through the SDK installation.
* Next, open the project in Android Studio (`<checkout-root>/android-tests`). This will also
  automatically add a `local.properties` file, specifying your Android SDK location.
* Finally, go to `View > Tool Windows > Device Manager` create and run an emulator. You can select
  any device-version configuration as long as it is at least API 33. See
  [documentation](https://developer.android.com/studio/run/managing-avds) for more details.

## Adding a New Test Project

When adding a new project to be tested, follow these steps:
* Update `build.gradle.kts` and add it to the `testedProjects` parameter of the build repo task.
* Update `device-tests/build.gradle.kts`
  * Create a new product flavor in the `productFlavors` section.
  * Add its dependencies in the `dependencies` section, including its test via the
    `addLibraryTests` helper method (see docs around them for details).
* Update the project's pom file to include the `maven-jar-plugin` plugin for tests (you can check
  the `runtime` project for an example). This test jar goal will be used by the `addLibraryTests`
  method.
