#!/bin/sh

# make sure your Android SDK tools path is set in SDK_BASE
android update project --path . --name Gibberbot --subprojects
android update project --path external/ActionBarSherlock/library/ --name ActionBarSherlock
android update project --path external/MemorizingTrustManager --name MemorizingTrustManager --subprojects
android update project --path external/OnionKit/library --name OnionKit
