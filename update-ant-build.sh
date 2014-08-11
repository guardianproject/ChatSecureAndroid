#!/bin/sh

target="android-20"
projectname=`sed -n 's,.*name="app_name">\(.*\)<.*,\1,p' res/values/strings.xml`

# make sure your Android SDK tools path is set in SDK_BASE
android update lib-project --target $target --path external/ActionBarSherlock/actionbarsherlock
android update lib-project --target $target --path external/MemorizingTrustManager
android update lib-project --target $target --path external/OnionKit/libnetcipher
android update lib-project --target $target --path external/AndroidPinning
android update lib-project --target $target --path external/cacheword/cachewordlib
android update lib-project --target $target --path external/SlidingMenu/library
android update lib-project --target $target --path external/AndroidEmojiInput/library
android update lib-project --target $target --path external/ViewPagerIndicator/library

android update project --path . --name $projectname --target $target --subprojects

cp libs/android-support-v4.jar external/OnionKit/libnetcipher/libs/android-support-v4.jar
cp libs/android-support-v4.jar external/ActionBarSherlock/actionbarsherlock/libs/android-support-v4.jar
cp libs/android-support-v4.jar external/SlidingMenu/library/libs/android-support-v4.jar
cp libs/android-support-v4.jar external/cacheword/cachewordlib/libs/android-support-v4.jar
cp libs/android-support-v4.jar external/ViewPagerIndicator/library/libs/android-support-v4.jar

cp libs/sqlcipher.jar external/cacheword/cachewordlib/libs/sqlcipher.jar
