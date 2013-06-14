#!/bin/sh

# make sure your Android SDK tools path is set in SDK_BASE
android update project --path . --name Gibberbot --subprojects
android update project --path external/ActionBarSherlock/actionbarsherlock -t android-17
android update project --path external/MemorizingTrustManager --name MemorizingTrustManager -t android-17 --subprojects
android update project --path external/OnionKit/libonionkit --name OnionKit -t android-17
android update project --path external/AndroidPinning --name libpinning -t android-17
android update project --path external/cacheword --name cacheword -t android-17
android update project --path external/SlidingMenu --name sliding -t android-17
