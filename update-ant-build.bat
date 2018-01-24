@echo off
rem update-ant-build script for Windows Command prompt
rem make sure PATH environment variable includes Android SDK tools path

set target=android-21
set projectname=ChatSecure

echo Updating Android target...
call android update lib-project --path external/MemorizingTrustManager --target %target%
call android update lib-project --path external/AndroidPinning --target %target%
call android update lib-project --path external/cacheword/cachewordlib --target %target%
call android update lib-project --path external/SlidingMenu/library --target %target%
call android update lib-project --path external/AndroidEmojiInput/library --target %target%

call android update project --path . --name %projectname% --target %target% --subprojects

echo Synchronizing jar files...
copy "libs\android-support-v4.jar" "external\SlidingMenu\library\libs\android-support-v4.jar"
copy "libs\android-support-v4.jar" "external\cacheword\cachewordlib\libs\android-support-v4.jar"
copy "libs\android-support-v4.jar" "external\ViewPagerIndicator\library\libs\android-support-v4.jar"
copy "libs\android-support-v4.jar" "external\AndroidEmojiInputlibs\android-support-v4.jar"

