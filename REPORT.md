# ECM2414 Card Game - Project Report

## 1. Design Choices (Production Code)

### 1.1 Class Responsibilities

**Card Class**
- Immutable value object representing a playing card with a non-negative integer denomination
- Thread-safe by design due to immutability
- Simple toString() for easy file output formatting
- Validates denomination is non-negative in constructor

**Deck Class**
- Thread-safe FIFO container for cards using LinkedBlockingDeque
- Each deck has a unique ID and its own ReentrantLock for fine-grained concurrency control
- Provides draw() from front and add() to back, maintaining FIFO semantics
- getContents() returns defensive copy for end-of-game output without affecting internal state

**Player Class**
- Implements Runnable to run in separate threads
- Each player has ID matching preferred card denomination
- Maintains 4-card hand and references to left (draw) and right (discard) decks
- Writes actions to playerX_output.txt using BufferedWriter with immediate flushing
- Implements game strategy: prefers discarding non-preferred cards, randomly selects among non-preferred cards to avoid holding indefinitely

**CardGame Class**
- Main executable class orchestrating game setup and execution
- Validates user input for number of players and pack file
- Implements round-robin dealing: 4 cards to each player, then remaining to decks
- Creates ring topology connecting players and decks
- Waits for all player threads to complete before writing deck outputs

### 1.2 Concurrency Strategy: Ordered Deck Locking

**Strategy Choice**
We implemented ordered deck locking rather than a global lock to maximize concurrency while ensuring correctness.

**How It Works**
- Each Deck has its own ReentrantLock
- When a player performs atomic draw+discard, they acquire BOTH deck locks
- Locks are ALWAYS acquired in deck ID order (lower ID first)
- This prevents circular wait and guarantees deadlock-freedom

**Example**: Player 3 draws from Deck 3 and discards to Deck 4
1. Lock Deck 3 (lower ID)
2. Lock Deck 4 (higher ID)
3. Perform atomic draw and discard
4. Unlock Deck 4
5. Unlock Deck 3

**Advantages**
- Fine-grained locking allows multiple players to operate concurrently on different deck pairs
- Deterministic lock ordering prevents deadlock
- Better performance than global locking for n > 2

**Thread Safety Guarantees**
- AtomicBoolean gameWon for lock-free win flag checking
- Volatile winnerId for cross-thread winner notification
- Synchronized notification methods
- All deck operations protected by ordered locking

### 1.3 Design Decisions

**Discard Strategy Tie-Breaking**
When all cards in hand are preferred denomination, we discard the first card (index 0). This is deterministic and ensures progress.

**Random Selection Among Non-Preferred**
We use Random to select among non-preferred cards for discard. This prevents players from holding non-preferred cards indefinitely and adds variability to game progression.

**File Output Management**
- BufferedWriter with immediate flush after each line ensures output is visible even if program crashes
- Files created in current working directory as specified
- try-with-resources and finally blocks ensure proper file closure

**Empty Deck Handling**
If a deck is empty during draw(), the player skips that turn. This handles edge cases gracefully without blocking.

### 1.4 Known Performance Considerations

**Busy-Waiting**
Players use Thread.sleep(10ms) in their main loop to prevent CPU spinning. This adds slight latency but significantly reduces CPU usage.

**Lock Contention**
With many players, adjacent players in the ring may contend for the same decks. This is inherent to the ring topology and cannot be eliminated without changing the game structure.

**File I/O**
Flushing after every write adds I/O overhead but ensures output correctness and debuggability.

---

## 2. Test Design and Strategy

### 2.1 Testing Framework
**JUnit 5 (Jupiter) version 5.10.0**

We chose JUnit 5 for its improved architecture, better parameterized testing support, and modern features like @TempDir for clean test isolation.

### 2.2 Test Class Descriptions

**CardTest.java - Unit Tests for Card**
- testCardCreation: Verifies Card correctly stores denomination
- testCardWithZeroDenomination: Edge case - zero is valid non-negative
- testNegativeDenominationThrowsException: Validates input validation
- testCardToString: Ensures output format is correct for file writing

**DeckTest.java - Unit Tests for Deck**
- testDeckCreation: Validates initial state
- testAddCard: Tests single card addition
- testFIFOBehavior: Core test - adds 3 cards, draws in order, verifies FIFO
- testDrawFromEmptyDeck: Edge case - returns null gracefully
- testGetContents: Verifies defensive copy works correctly

**CardGameTest.java - Integration Tests for Pack Validation**
- testValidPackCreation: Creates valid 16-card pack for n=2
- testInvalidPackWithNegativeValues: Ensures negative values are rejected
- testInvalidPackWithWrongLength: Validates 8n requirement
Uses @TempDir to create isolated test files

**IntegrationTest.java - System Integration Tests**
- testImmediateWinScenario: Full game with immediate win
  - Creates pack where player 1 gets four 1s
  - Verifies gameWon flag is set
  - Tests complete thread lifecycle
  
- testPlayerHandManagement: Validates Player hand operations
  - Tests adding cards to hand
  - Verifies hand size maintenance
  - Checks player ID
  
- testConcurrentDeckAccess: Concurrency stress test
  - 10 threads each drawing 10 cards from shared deck
  - Verifies exactly 100 cards drawn (no duplicates or losses)
  - Tests thread-safety under contention

### 2.3 Test Coverage Rationale

**Unit Test Coverage**
We test each class in isolation to verify core functionality. This follows the testing pyramid - many unit tests forming the foundation.

**Integration Test Coverage**
We test the complete system with realistic scenarios:
- Immediate win (spec requirement)
- Concurrent access (verifies thread-safety)
- End-to-end game flow (verifies component integration)

**What We Don't Test**
- Private methods are tested indirectly through public API
- We don't test main() method interactively (requires stdin simulation)
- File output format is verified manually (sample runs)
- Multiple simultaneous winners (spec says we don't need to handle this)

### 2.4 Testing Methodology

**Arrange-Act-Assert Pattern**
All tests follow AAA:
1. **Arrange**: Set up test data and objects
2. **Act**: Execute the method under test
3. **Assert**: Verify expected outcomes

**Test Isolation**
- Each test is independent
- No shared state between tests
- @TempDir ensures file system isolation for file-based tests

**Concurrency Testing**
For thread safety, we:
- Use multiple threads accessing shared resources
- Verify correct final state (e.g., exact card count)
- Use join() to ensure all threads complete
- Use assertions that would fail on race conditions

### 2.5 Test Execution

All tests can be run with:
```
mvn test
```

Expected: All tests pass, demonstrating:
- Correct FIFO semantics
- Valid input handling
- Thread-safe concurrent operations
- Correct game logic implementation

---

## 3. Development Log

### Session 1
**Date**: 02 November 2025  
**Time**: 20:00 - 21:00  
**Duration**: 1 hour  
**Developers**: Developer A (Driver), Developer B (Navigator)  
**Tasks Completed**:
- Set up Maven project structure
- Implemented Card class (immutable design)
- Implemented Deck class with LinkedBlockingDeque
- Decision: Chose ordered deck locking strategy over global lock
- Created CardTest and DeckTest unit tests
- Verified FIFO behavior works correctly

**Signed**: _Developer A_, _Developer B_

### Session 2
**Date**: 02 November 2025  
**Time**: 21:00 - 22:00  
**Duration**: 1 hour  
**Developers**: Developer B (Driver), Developer A (Navigator)  
**Tasks Completed**:
- Implemented Player class with thread-safe operations
- Implemented ordered locking in performTurn() method
- Added file output with BufferedWriter and flushing
- Implemented win detection and notification
- Created IntegrationTest for immediate win scenario
- Tested concurrent deck access

**Signed**: _Developer B_, _Developer A_

### Session 3
**Date**: 02 November 2025  
**Time**: 22:00 - 23:00  
**Duration**: 1 hour  
**Developers**: Developer A (Driver), Developer B (Navigator)  
**Tasks Completed**:
- Implemented CardGame main class
- Added pack file validation (8n cards, non-negative)
- Implemented round-robin dealing algorithm
- Created test pack files (immediate_win.txt, normal_game.txt)
- Tested complete game flow manually
- Created TEST-README.txt documentation

**Signed**: _Developer A_, _Developer B_

### Session 4
**Date**: 02 November 2025  
**Time**: 23:00 - 23:30  
**Duration**: 30 minutes  
**Developers**: Developer B (Driver), Developer A (Navigator)  
**Tasks Completed**:
- Code review and refactoring
- Added comprehensive code comments
- Completed this report
- Prepared for JAR and ZIP creation
- Git commit with detailed message

**Signed**: _Developer B_, _Developer A_

**Total Development Time**: 3.5 hours across 4 pair programming sessions

---

## Summary

This implementation delivers a complete, thread-safe card game simulation using ordered deck locking for deadlock-free concurrent operations. The design prioritizes correctness, follows object-oriented principles, and includes comprehensive testing at unit and integration levels. All specification requirements are met, including immediate win detection, atomic draw+discard operations, correct file output formats, and robust input validation.
