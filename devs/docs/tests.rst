===============
Test cheatsheet
===============

Run tests in a single module using multiple forks::

    $ ./gradlew --parallel -PtestForks=2 :sql:test

Run the doc-tests::

    $ ./gradlew itest
      (export ITEST_FILE_NAME_FILTER=<file-name>
       if you want to only run the test of a particular file.
       Clear the env var to test all files.)

Filter tests::

    $ ./gradlew test -Dtest.single='YourTestClass'

    $ ./gradlew test --tests '*ClassName.testMethodName'

Extra options::

    $ ./gradlew :server:test -Dtests.seed=8352BE0120F826A9

    $ ./gradlew :server:test -Dtests.iters=20

    $ ./gradlew :server:test -Dtests.nightly=true # defaults to "false"

    $ ./gradlew :server:test -Dtests.verbose=true # log result of all invoked tests

More logging::

    $ ./gradlew -PtestLogging -Dtests.loggers.levels=io.crate:DEBUG,io.crate.planner.consumer.NestedLoopConsumer:TRACE :server:test

More logging by changing code:

Use ``@TestLogging(["<packageName1>:<logLevel1>", ...])`` on your test class or
test method to enable more detailed logging. For example::

    @TestLogging("io.crate:DEBUG,io.crate.planner.consumer.NestedLoopConsumer:TRACE")
