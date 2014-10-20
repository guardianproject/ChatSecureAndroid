ChatSecure for Android (previously known as Gibberbot) is a secure messaging
app built on open standards like XMPP/Jabber and OTR encryption:
https://guardianproject.info/apps/chatsecure/

It includes OTR4J:
https://code.google.com/p/otr4j/

and BouncyCastle for Java:
http://www.bouncycastle.org/java.html

and SQLCipher for Android:
https://guardianproject.info/code/sqlcipher/


## Get the source
1. Clone this repository
2. Open the file 'BUILD' and follow the instructions


## Build Instructions

First make sure you have the Android SDK and Eclipse installed. Follow
instructions here: https://developer.android.com/sdk/index.html and here:
https://developer.android.com/sdk/installing.html.  Please help us keep this
process easy by letting us know if you have problems.  If you have any
questions, don't be afraid to email support@guardianproject.info or jump into
IRC for real-time help at #guardianproject on freenode or OFTC.


### Get the source

The source code is all in the main git repos, with sub-projects setup as git
submodules:

    git clone https://github.com/guardianproject/ChatSecureAndroid.git
    cd ChatSecureAndroid
    git submodule update --init


### ant setup

We use `ant` to make our official releases and automated test builds.  If you
are not familiar with Eclipse, then it is easier to start with the `ant`
build:

    export ANDROID_HOME=/path/to/android-sdk
    export ANDROID_NDK=/path/to/android-ndk
    ./update-ant-build.sh
    ant clean debug

Then the installable APK will be in **bin/ChatSecure-debug.apk**.


## Test Instructions

`mvn test`

See robo-tests/README.md for eclipse instructions.

Currently the instrumented target tests (to be run on a device) in the directory `tests` are empty.

## Logging

`adb shell setprop log.tag.GB.XmppConnection DEBUG`

## Building for a Locale

ant -Dgibberbot.locale=fa release
