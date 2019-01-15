Title page analyser
==================

This is a prototype for an android app for gathering bibliographic data
from pictures of the title page and colophon of books and other written
material.

Building
--------
```bash
./gradlew build
```

Development
-----------
You need the android sdk: https://developer.android.com/studio/#downloads (under "Command line tools only", https://dl.google.com/android/repository/sdk-tools-linux-4333796.zip).
Then download the necessary tools:
```
android-sdk-linux/tools/bin/sdkmanager --verbose "tools" "build-tools;28.0.3" "platform-tools" "platforms;android-28" "extras;android;m2repository" "extras;google;m2repository"
```

Then place the path to your local copy of the sdk into the file
```local.properties```:
```sdk.dir=$path_to_android_sdk```

Tests
-----
```bash
# run instrumentation tests on a real device or an emulator:
./gradlew connectedAndroidTest
```
