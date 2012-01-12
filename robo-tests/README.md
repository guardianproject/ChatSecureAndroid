This sub-project contains JUnit-4 tests for Gibberbot.

To grab jars that this project depends on, do:

   `cd ..`
   `mvn dependency:copy-dependencies`
   `mv target/dependency/*.jar robo-tests/libs`

and add maps.jar and android.jar from the android SDK, level 10.

You can also import this `robo-tests` project directory into eclipse as a JVM based project.  If you go this route, add a project named _Android_ with android.jar and maps.jar as exported libraries.

