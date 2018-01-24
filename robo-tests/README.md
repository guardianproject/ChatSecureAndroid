=== Testing ===

This sub-project contains JUnit-4 tests for ChatSecure.

Use `cd .. ; mvn test` to run the tests from the command line.

=== Eclipse ===

You can also import this `robo-tests` project directory into eclipse as a JVM based project.  If you go this route, add a project named _Android_ with android.jar and maps.jar as exported libraries.  You might have to symlink AndroidManifest.xml and res from the top-level project into robo-tests.

To add jars that this project depends on, do:

    cd ..
    mvn dependency:copy-dependencies
    mv target/dependency/*.jar robo-tests/libs

and add maps.jar and android.jar from the android SDK, level 10.

