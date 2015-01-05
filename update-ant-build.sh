#!/bin/sh

set -e

if ! which android > /dev/null; then
    if [ -z $ANDROID_HOME ]; then
        if [ -e ~/.android/bashrc ]; then
            . ~/.android/bashrc
        else
            echo "'android' not found, ANDROID_HOME must be set!"
            exit
        fi
    else
        export PATH="${ANDROID_HOME}/tools:$PATH"
    fi
fi

# fetch target from project.properties
eval `grep '^target=' project.properties`

projectname=`sed -n 's,.*name="app_name">\(.*\)<.*,\1,p' res/values/strings.xml`

for lib in `sed -n 's,^android\.library\.reference\.[0-9][0-9]*=\(.*\),\1,p' project.properties`; do
    android update lib-project --path $lib --target $target
done

android update project --path . --name $projectname --target $target --subprojects

cp libs/android-support-v4.jar external/SlidingMenu/library/libs/android-support-v4.jar
cp libs/android-support-v4.jar external/cacheword/cachewordlib/libs/android-support-v4.jar
cp libs/android-support-v4.jar external/ViewPagerIndicator/library/libs/android-support-v4.jar
cp libs/android-support-v4.jar external/AndroidEmojiInput/library/libs/android-support-v4.jar
