== Preparation

    git submodule update --init
    ./update-ant-build.sh
    * setup external/asmack/local.properties to point to your android sdk *
    (cd external/asmack && ./build.bash && cp build/asmack-android-4.jar ../../libs)

== Building with ant

Follow the steps from the prep section, then:

    ant debug

== Eclipse

First, follow the above instructions in the Preparation section.  Then, import
the following as existing Android projects

1. File --> Import... --> Android --> Existing Android Code Into Workspace

    * external/ActionBarSherlock/library (use ActionBarSherlock as project name)
    * external/OnionKit/library
    * external/MemorizingTrustManager

2. Right-click on the project called 'library' --> Refactor --> Rename... and
   rename it to 'ActionBarSherlock'

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



[1] Thanks to: http://bjdodson.blogspot.com/2009/07/xmpp-on-android-using-smack.html)
l
