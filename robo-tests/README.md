This sub-project contains JUnit-4 tests for Gibberbot.

Grab the robolectric 1.1-SNAPSHOT-jar-with-dependencies jar here:

https://oss.sonatype.org/index.html#nexus-search;quick~robolectric

and put it in robo-tests/libs/lib/robolectric-1.1-jar-with-dependencies.jar

To grab the other jars that this project depends on, do:

    cd ..
    mvn dependency:copy-dependencies
    mv target/dependency/*.jar robo-tests/libs

and add maps.jar and android.jar from the android SDK, level 10.

You can also import this `robo-tests` project directory into eclipse as a JVM based project.  If you go this route, add a project named _Android_ with android.jar and maps.jar as exported libraries.

