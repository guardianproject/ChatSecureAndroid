ChatSecure for Android, also known as Gibberbot, an Android app to support XMPP Jabber chat using OTR encryption
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
1. For these instructions, you'll need the Android SDK and Eclipse installed. Follow instructions here: https://developer.android.com/sdk/index.html and here: https://developer.android.com/sdk/installing.html
2. From the main Gibberbot GitHub project page (https://github.com/guardianproject/Gibberbot) grab the Gibberbot source through your method of choice.

That's it! Generally speaking, this should be an easy project to build locally for anyone who's used Eclipse and/or ADT before. If you have any questions, don't be afraid to jump into IRC for real-time help at #guardianproject on freenode or OFTC.

## Test Instructions

`mvn test`

See robo-tests/README.md for eclipse instructions.

Currently the instrumented target tests (to be run on a device) in the directory `tests` are empty.

## Logging

`adb shell setprop log.tag.GB.XmppConnection DEBUG`

## Building for a Locale

ant -Dgibberbot.locale=fa release
