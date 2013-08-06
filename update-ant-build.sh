#!/bin/sh

# make sure your Android SDK tools path is set in SDK_BASE
android update project --path . --name Gibberbot --subprojects
android update project --path external/ActionBarSherlock/actionbarsherlock -t android-17
android update project --path external/MemorizingTrustManager --name MemorizingTrustManager -t android-17 --subprojects
android update project --path external/OnionKit/libonionkit --name OnionKit -t android-17
android update project --path external/AndroidPinning --name libpinning -t android-17
android update project --path external/cacheword/cachewordlib --name cacheword -t android-17
android update project --path external/SlidingMenu/library --name sliding -t android-17
android update project --path external/SlideListView/library --name slidelist -t android-17
android update project --path external/NineOldAndroids/library --name nineold -t android-17
android update project --path external/MessageBar/library --name messagebar -t android-17
android update project --path external/ShowcaseView/library --name showcase -t android-17
android update project --path external/AndroidEmojiInput/library --name emoji -t android-17

cp libs/android-support-v4.jar external/OnionKit/libonionkit/libs/android-support-v4.jar
cp libs/android-support-v4.jar external/ActionBarSherlock/actionbarsherlock/libs/android-support-v4.jar
cp libs/android-support-v4.jar external/SlidingMenu/library/libs/android-support-v4.jar
cp libs/android-support-v4.jar external/cacheword/cachewordlib/libs/android-support-v4.jar

cp libs/sqlcipher.jar external/cacheword/cachewordlib/libs/sqlcipher.jar
