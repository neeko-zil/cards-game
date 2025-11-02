ECM2414 Card Game - Test Suite
================================

This archive contains the complete test suite for the multi-threaded card game simulation.

CONTENTS
--------
- src/test/java/cardgame/*.java    Test source files
- pom.xml                          Maven configuration (for running tests)
- test_packs/                      Sample pack files for testing

TEST FRAMEWORK
--------------
JUnit 5 (Jupiter) version 5.10.0

RUNNING THE TESTS
-----------------

Prerequisites:
- Java 11 or higher
- Maven 3.6+

Command to run all tests:
    mvn test

Command to run specific test class:
    mvn test -Dtest=CardTest
    mvn test -Dtest=DeckTest
    mvn test -Dtest=IntegrationTest

To run tests with verbose output:
    mvn test -X

TEST CLASSES
------------

1. CardTest.java
   - Tests Card class creation with valid/invalid denominations
   - Verifies immutability
   - Tests toString() method

2. DeckTest.java
   - Tests Deck creation and FIFO behavior
   - Verifies thread-safe operations
   - Tests draw from empty deck
   - Validates getContents() method

3. CardGameTest.java
   - Tests pack file validation
   - Verifies handling of negative values
   - Tests incorrect pack file lengths

4. IntegrationTest.java
   - Tests complete game with immediate win scenario
   - Tests concurrent deck access with multiple threads
   - Verifies player hand management
   - Tests atomic draw+discard operations

EXPECTED RESULTS
----------------
All tests should pass when run with: mvn test

The integration tests verify:
- Immediate win detection works correctly
- Concurrent access to decks is thread-safe
- Game state management is correct
- No race conditions or deadlocks occur

MANUAL COMPILATION (if Maven unavailable)
------------------------------------------
1. Ensure JUnit 5 JARs are in classpath
2. Compile: javac -cp junit5.jar -d target/test-classes src/test/java/cardgame/*.java
3. Run: java -cp junit5.jar:target/test-classes org.junit.platform.console.ConsoleLauncher --scan-classpath

Note: Maven is recommended for ease of use.
