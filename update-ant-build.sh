#!/bin/sh

target="android-17"
projectname=`sed -n 's,.*name="app_name">\(.*\)<.*,\1,p' res/values/strings.xml`

# make sure your Android SDK tools path is set in SDK_BASE
android update lib-project --path external/ActionBarSherlock/actionbarsherlock --target $target
android update lib-project --path external/MemorizingTrustManager --target $target
android update lib-project --path external/OnionKit/libonionkit --target $target
android update lib-project --path external/AndroidPinning --target $target
android update lib-project --path external/cacheword/cachewordlib --target $target
android update lib-project --path external/SlidingMenu/library --target $target
android update lib-project --path external/AndroidEmojiInput/library --target $target

android update project --path . --name $projectname --target android-17 --subprojects

cp libs/android-support-v4.jar external/OnionKit/libonionkit/libs/android-support-v4.jar
cp libs/android-support-v4.jar external/ActionBarSherlock/actionbarsherlock/libs/android-support-v4.jar
cp libs/android-support-v4.jar external/SlidingMenu/library/libs/android-support-v4.jar
cp libs/android-support-v4.jar external/cacheword/cachewordlib/libs/android-support-v4.jar

cp libs/sqlcipher.jar external/cacheword/cachewordlib/libs/sqlcipher.jar
