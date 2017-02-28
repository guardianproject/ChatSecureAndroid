#!/bin/sh

# make sure your Android SDK tools path is set in SDK_BASE
android update lib-project --path external/ActionBarSherlock/actionbarsherlock
android update lib-project --path external/MemorizingTrustManager
android update lib-project --path external/OnionKit/libonionkit
android update lib-project --path external/AndroidPinning
android update lib-project --path external/cacheword/cachewordlib
android update lib-project --path external/SlidingMenu/library
android update lib-project --path external/SlideListView/library
android update lib-project --path external/MessageBar/library
android update lib-project --path external/AndroidEmojiInput/library

android update project --path . --name Gibberbot --target android-17 --subprojects

cp libs/android-support-v4.jar external/OnionKit/libonionkit/libs/android-support-v4.jar
cp libs/android-support-v4.jar external/ActionBarSherlock/actionbarsherlock/libs/android-support-v4.jar
cp libs/android-support-v4.jar external/SlidingMenu/library/libs/android-support-v4.jar
cp libs/android-support-v4.jar external/cacheword/cachewordlib/libs/android-support-v4.jar

cp libs/sqlcipher.jar external/cacheword/cachewordlib/libs/sqlcipher.jar
