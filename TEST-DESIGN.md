Card Game Test Design Document

Testing Framework: JUnit 5 (Jupiter) version 5.10.0

We chose JUnit 5 for modern testing features including @TempDir for file isolation and improved assertions.

Test Strategy

Our tests follow the testing pyramid: many unit tests, fewer integration tests, and some concurrency tests. This gives fast feedback during development while validating complete system behavior.

Unit Tests

CardTest.java
Tests the Card class as an immutable value object. Tests verify cards store their denomination correctly, reject negative values, handle zero as valid, and return correct string format for file output. We test immutability by calling getDenomination() twice to ensure values never change. This matters because cards are shared between player threads.

DeckTest.java
Tests the Deck class FIFO behavior and thread safety basics. Tests verify decks maintain first-in-first-out order, handle empty state gracefully by returning null, reject null cards, and provide defensive copies via getContents(). The getContents() test is important because it ensures snapshots don't affect the original deck state.

PlayerTest.java
Tests Player class hand management and basic functionality. Tests verify players can add cards to their hand, track their ID correctly, and provide defensive copies when getHand() is called. We test that players can hold four cards and that the hand list returned is a copy not the original.

CardGameTest.java
Tests pack file validation logic. Tests verify the validatePack() method correctly handles valid 8n card packs, rejects files with too few or too many lines, rejects non-integer values, negative numbers, blank lines, and overflow numbers. Uses @TempDir to create isolated temporary files for each test. This ensures no test pollution and clean test environments.

Integration Tests

GameFlowTest.java
Tests complete game scenarios from start to finish. The immediateWin test creates a pack where player 1 starts with four 1s and verifies the game completes with correct output. The multiTurnWin test verifies games that require multiple turns work correctly. These tests ensure the complete system integrates properly and produces correct output files.

IntegrationTest.java
Tests exact output file formats match specification requirements. The exactOutputFormat test verifies all output files are created, contain correct messages in correct order, and match the required format precisely. This is critical because the specification requires exact output formatting.

Concurrency Tests

ConcurrencyTest.java
Tests thread safety under stress conditions.

concurrentDrawing creates 10 threads drawing from one deck and verifies exactly 100 cards are drawn with no duplicates or losses. This validates the ReentrantLock protects draw operations.

concurrentDiscarding tests 10 threads adding cards simultaneously and verifies all 100 cards are added. This validates thread-safe adding.

oneWinnerOnly tests that AtomicBoolean prevents multiple winners. 10 threads try to win simultaneously but only one succeeds via compareAndSet.

getContentsThreadSafe verifies multiple threads can safely get deck snapshots without corruption.

orderedLockingNoDeadlock tests the ordered locking strategy by having two threads transfer cards between decks. Both lock in same order (lower ID first) preventing deadlock.

manyThreadsStressTest runs 50 threads drawing 500 cards total, verifying the locking mechanism works under high contention.

Test Design Rationale

Why Test-Driven Development
We wrote tests before implementation code (TDD approach). This ensures testability from the start and helps define clear interfaces. For example, writing DeckTest first helped us decide that drawTop() should return null for empty decks rather than throwing an exception.

Why @TempDir
Using JUnit 5's @TempDir annotation gives each file test its own isolated directory that's automatically cleaned up. This prevents tests from interfering with each other and ensures consistent results.

Why Stress Testing
Concurrency bugs are hard to reproduce, so we use stress tests with many threads. The manyThreadsStressTest with 50 threads helps expose race conditions that might not appear with just 2-3 threads.

Why Fixed Random Seeds
Players use randomness when discarding cards. Tests that check this behavior would be unreliable with true randomness. Using Random with a fixed seed (like new Random(1L)) makes tests deterministic and reproducible.

Why Defensive Copy Tests
Testing getHand() and getContents() return copies not original collections is important. If these returned the actual internal list, external code could modify player hands or deck contents, breaking encapsulation and thread safety.

Coverage Approach

What We Test
- All public methods of Card, Deck, Player, CardGame
- Input validation and error handling
- Edge cases like empty decks, zero values, immediate wins
- Thread safety with concurrent access patterns
- Exact output format compliance

What We Don't Test
- Private methods (tested indirectly through public API)
- main() method interactive input (requires stdin simulation)
- Random thread timing (non-deterministic)
- Manual verification of output files (checked by integration tests)

Test Execution

All tests run with: mvn test

Expected results: All tests pass, demonstrating correct FIFO behavior, input validation, thread-safe concurrent operations, and correct game logic implementation.

Test Independence

Each test is completely independent. No shared state exists between tests. Each test sets up its own data, runs its operation, and verifies results. This means tests can run in any order and won't affect each other.

Arrange-Act-Assert Pattern

All tests follow AAA structure:
Arrange: Set up test data and objects
Act: Execute the method being tested  
Assert: Verify expected outcomes

This makes tests readable and maintainable.

Why This Testing Strategy Matters

The combination of unit tests, integration tests, and concurrency tests gives us confidence the system works correctly. Unit tests catch bugs early in individual classes. Integration tests verify components work together. Concurrency tests ensure thread safety which is critical for this multi-threaded application. Together they validate all specification requirements are met.
