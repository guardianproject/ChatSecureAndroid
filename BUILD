== Preparation

    git submodule update --init
    android update project --path ActionBarSherlock/library/ --target android-15 --name ActionBarSherlock
    android update project --path MemorizingTrustManager --target android-15 --name MemorizingTrustManager
    (cd asmack && ./build.bash && cp build/asmack-android-4.jar ../libs)

== Building with ant

Follow the steps from the prep section, then:

    ant debug

== Eclipse

Add the following as Android projects, after following the steps in the prep section:

    * Gibberbot/ActionBarSherlock/library (use ActionBarSherlock as project name)
    * Gibberbot
    

== Old Stuff

Patching Smack library for Android [1]

$ svn co -r 10869 \
     http://svn.igniterealtime.org/svn/repos/smack/trunk smack-android
$ cd smack-android/source
$ patch -p0 -i patches/smack/smack.diff
$ cd ../build
$ ant
$ cd ../target



[1] Thanks to: http://bjdodson.blogspot.com/2009/07/xmpp-on-android-using-smack.html)
l
