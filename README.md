Gibberbot, an Android app to support XMPP Jabber chat using OTR encryption
https://guardianproject.info/apps/gibberbot

It includes OTR4J:
http://code.google.com/p/otr4j/

and BouncyCastle for Java:
http://www.bouncycastle.org/java.html

and SQLCipher for Android:
https://guardianproject.info/code/sqlcipher/

## Build Instructions
1. For these instructions, you'll need the Android SDK and Eclipse installed. Follow instructions here: http://developer.android.com/sdk/index.html and here: https://developer.android.com/sdk/installing.html
2. Gibberbot is currently configured to run on the version 4+ of the Android SDK. This corresponds to Platform 1.6 - make sure to install it.
3. From the main Gibberbot GitHub project page (https://github.com/guardianproject/Gibberbot) grab the Gibberbot source through your method of choice.
4. Open up Eclipse and select File > Import > Existing Projects into Workspace. Follow the prompts and select the root directory of the Gibberbot source.
5. Depending on how willing to cooperate Eclipse is, you may need to Clean the project manually.
6. In Eclipse, right-click on the project root and select Run As > Android Application. Run on your favorite debug-enabled Android device or emulator!

That's it! Generally speaking, this should be an easy project to build locally for anyone who's used Eclipse and/or ADT before. If you have any questions, don't be afraid to jump into IRC for real-time help at #guardianproject on freenode or OFTC.

## Test Instructions

`mvn test`

See robo-tests/README.md for eclipse instructions.

Currently the instrumented target tests (to be run on a device) in the directory `tests` are empty.

## Logging

`adb shell setprop log.tag.GB.XmppConnection DEBUG`

## Building for a Locale

ant -Dgibberbot.locale=fa release
