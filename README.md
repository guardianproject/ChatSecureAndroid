## This Repository is Retired

ChatSecure for Android has been renamed and is continuing under the name Zom.

Learn more about Zom here: https://zom.im

Head to the new repo here: https://github.com/zom/Zom-Android

---
Everything below is considered archived.
---

ChatSecure for Android (previously known as Gibberbot) is a secure messaging
app built on open standards like XMPP/Jabber and OTR encryption:
https://guardianproject.info/apps/chatsecure

It includes OTR4J:
https://github.com/otr4j/otr4j

and BouncyCastle for Java:
http://www.bouncycastle.org/java.html

and SQLCipher for Android:
https://guardianproject.info/code/sqlcipher/

Original wallpaper generated using Tapet app and Gimp:
https://play.google.com/store/apps/details?id=com.sharpregion.tapet

and previously included some CC0 public domain beautiful images:
Ry Van
https://unsplash.com/ryvanveluwen
https://unsplash.com/license

## Bug reports

Please report any and all bugs or problems that you find.  This is essential
for us to be able to improve this software!

https://dev.guardianproject.info/projects/chatsecure/issues


## Build Instructions

First make sure you have the Android SDK and Eclipse installed. Follow
instructions here:

* https://developer.android.com/sdk/index.html
* https://developer.android.com/sdk/installing.html

Please help us keep this process easy by letting us know if you have problems.
If you have any questions, don't be afraid to email us at
support@guardianproject.info or jump into our IRC chatrooms for real-time help
at #guardianproject on freenode or OFTC (https://guardianproject.info/contact/chat/).


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
    ./update-ant-build.sh
    ant clean debug

Then the installable APK will be in **bin/ChatSecure-debug.apk**.


### Eclipse setup

1. Start by adding ChatSecureAndroid to Eclipse by going to _File_ -> _New_ ->
_Project..._ -> _Android project from existing code_.

2. Open the ChatSecureAndroid folder that was just cloned from git.

3. Eclipse will next show a list of subprojects to import, all of the
libraries with _New Project Name_ of **library** must be renamed after the
project name, i.e. SlidingMenu, AndroidEmojiInput, ViewPagerIndicator.

4. Click *Deselect All*.  The sample and example projects are not needed, and
can cause conflicts.

5. Select __ChatSecure__ again by clicking the top item in the list.

6. Outside of Eclipse, open up the text file _project.properties_.  Then back
in Eclipse, for each line that starts with `android.library.reference`, select
that path from the list of included sub-projects in Eclipse.

Now you should be ready to build ChatSecure!


## Test Instructions

`mvn test`

See robo-tests/README.md for eclipse instructions.

Currently the instrumented target tests (to be run on a device) in the directory `tests` are empty.


## Logging

`adb shell setprop log.tag.GB.XmppConnection DEBUG`


## Building for a Locale

ant -Dgibberbot.locale=fa release
