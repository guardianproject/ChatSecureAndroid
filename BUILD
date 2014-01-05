== Preparation

    git submodule update --init
    ./update-ant-build.sh

== Building with ant

Follow the steps from the prep section, then:

    ant debug


== Eclipse

Add the following as Android projects (File->New->Project..., Android project from existing code), 
after following the steps in the prep section:

Note: Libraries with root folder 'library' must be renamed
after import, as Eclipse does not allow duplicate project names within a workspace.
The Eclipse project name does not affect the project's directory structure.

1. File --> Import... --> Android --> Existing Android Code Into Workspace
    * external/ActionBarSherlock/actionbarsherlock
    * external/AndroidPinning
    * external/cacheword/cachewordlib
    * external/OnionKit/libonionkit
    * external/MemorizingTrustManager
    * external/AndroidEmojiInput/library
    * external/SlidingMenu/library

2. If project imports with name 'library': Right-click project 'library' --> Refactor --> Rename... and
   give it a descriptive name. 

3. Import Gibberbot itself like #1 above
    
== Old Stuff

Patching Smack library for Android [1]

$ svn co -r 10869 \
     http://svn.igniterealtime.org/svn/repos/smack/trunk smack-android
$ cd smack-android/source
$ patch -p0 -i patches/smack/smack.diff
$ cd ../build
$ ant
$ cd ../target



[1] Thanks to: http://bjdodson.blogspot.com/2009/07/xmpp-on-android-using-smack.html