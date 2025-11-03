Design Choices for Production Code

The program is made up of four main classes: Card, Deck, Player, and CardGame. Each class has a clear purpose and works together to run the card game using multiple threads as required in the specification.

Card:
This class represents a single card with an integer value. It is immutable, meaning that once created it cannot be changed. The equals() and hashCode() methods are overridden so that cards with the same value are considered equal. This allows consistent behaviour when using cards in collections like HashSet or when comparing them directly.

Deck:
Each deck uses a Deque<Integer> to store cards in a first-in-first-out order. It has methods to draw a card from the top and discard one to the bottom. To make sure that multiple players can safely use the same deck, a ReentrantLock is used to make draw and discard operations thread-safe.

Player:
Each player runs on its own thread (implements Runnable). A player draws from its left deck and discards to its right deck. Players prefer cards that match their own player number, and if they get four of the same value they win. Players randomly discard non-preferred cards so that they do not hold onto unwanted cards forever. When a player wins, a shared flag is set so all other players can stop.

CardGame:
This is the main class that runs the game. It handles user input, validates the card pack, sets up players and decks, starts all player threads, and manages when the game ends. It also creates all the player and deck output files as described in the specification.

Thread safety and atomicity:
To keep the game correct, drawing and discarding are treated as one atomic action. Each player locks both decks in a set order so that no two threads can cause deadlocks. Only the decks and the shared “game won” flag are accessed by more than one thread, keeping the design simple and safe.

Limitations:
Because threads run in parallel, the order of actions in the output files may change between runs. This is normal and expected for concurrent programs.

Design Choices for Tests

Testing was done using JUnit 5. The tests are split into small unit tests for single classes and larger integration tests for the full game. Maven is used to compile and run all tests with mvn test.

Unit tests:

CardTest checks that cards are immutable, equal when they have the same value, and have the same hash code.

DeckTest checks that decks behave in a FIFO order and correctly handle being empty.

CardGameTest checks that invalid pack files (wrong number of cards, negative values, or bad input) are rejected and that valid ones run correctly.

Integration and concurrency tests:

GameFlowTest runs small games to check that output files are created correctly and that only one player wins.

ConcurrencyTest runs many threads to check that the locking system prevents deadlocks and that all players finish with four cards.

IntegrationTest checks that all decks and players together still have exactly 8n cards at the end of the game.

Randomness control:
Players discard random cards, so tests use a fixed random seed to make results repeatable and consistent.

Error and edge cases:
Tests include invalid pack formats, empty decks, and games that end immediately when a player starts with four of the same card.

Coverage:
Most methods are tested, especially the concurrency features. Some random thread timings can’t be tested directly, but repeated runs show the program finishes correctly without any deadlocks.

---

## 3. Development Log

**Session 1** | 15 Sept, 14:00-16:00 (2h) | In-person | Dev A (Driver), Dev B (Navigator)  
Created pom.xml with Maven, JUnit 5. Wrote CardTest.java first (TDD approach). Implemented Card.java: constructor with validation, getDenomination(), toString(). Created test_packs/ directory with immediate_win.txt for early testing. _Signed: Dev A, Dev B_

**Session 2** | 18 Sept, 15:00-17:00 (2h) | Remote | Dev B (Driver), Dev A (Navigator)  
Wrote DeckTest.java for FIFO behavior. Implemented Deck.java: ArrayDeque-based, drawTop(), discardBottom(), getContents() defensive copy. Added ReentrantLock field. Created normal_game.txt test pack (16 cards). _Signed: Dev B, Dev A_

**Session 3** | 22 Sept, 13:00-15:30 (2.5h) | In-person | Dev A (Driver), Dev B (Navigator)  
Research session: compared concurrency strategies. Decided on ordered deck locking. Wrote ConcurrencyTest.java skeleton for future validation. Created winnable_game.txt test pack for integration testing. _Signed: Dev A, Dev B_

**Session 4** | 25 Sept, 16:00-18:00 (2h) | Remote | Dev B (Driver), Dev A (Navigator)  
Created PlayerTest.java for basic functionality. Implemented Player.java skeleton: fields (id, hand, leftDeck, rightDeck, gameWon), implements Runnable. Added addCardToHand(), getHand(). Tested with immediate_win.txt pack. _Signed: Dev B, Dev A_

**Session 5** | 2 Oct, 14:00-16:30 (2.5h) | Remote | Dev A (Driver), Dev B (Navigator)  
Expanded PlayerTest for win detection. Implemented Player file output: BufferedWriter, writeToFile(), initial hand logging. Added hasWon() for four matching cards. Tested chooseCardToDiscard() logic with normal_game.txt. _Signed: Dev A, Dev B_

**Session 6** | 6 Oct, 15:00-17:00 (2h) | In-person | Dev B (Driver), Dev A (Navigator)  
Wrote GameFlowTest.java for multi-turn scenarios. Implemented Player.performTurn(): ordered locking (lower deck ID first), atomic draw+discard. Added notifyWinner(), notifyOtherPlayers(). Manual deadlock testing. _Signed: Dev B, Dev A_

**Session 7** | 13 Oct, 13:00-15:00 (2h) | Remote | Dev A (Driver), Dev B (Navigator)  
Created CardGameTest.java for validation. Implemented CardGame.java: Scanner input, promptForNumberOfPlayers() with validation. Created players/decks lists, ring topology setup. Tested with all three pack files. _Signed: Dev A, Dev B_

**Session 8** | 16 Oct, 14:00-16:30 (2.5h) | Remote | Dev B (Driver), Dev A (Navigator)  
Extended CardGameTest with edge cases (@TempDir). Implemented validatePack(): file checks, 8n validation, regex for non-negative integers. Added round-robin dealing. Tested blank lines, overflow numbers, missing files. _Signed: Dev B, Dev A_

**Session 9** | 23 Oct, 15:00-17:00 (2h) | In-person | Dev A (Driver), Dev B (Navigator)  
Wrote IntegrationTest.java for end-to-end scenarios. Implemented immediate win test using immediate_win.txt. Added exact output format validation test. Verified thread lifecycle with all test packs. _Signed: Dev A, Dev B_

**Session 10** | 27 Oct, 13:00-16:00 (3h) | Remote | Dev B (Driver), Dev A (Navigator)  
Completed ConcurrencyTest.java: 10-thread stress test, concurrent draw/discard, deadlock prevention validation. Ran all tests with mvn test. Fixed race condition in Player.notifyWinner(). Verified with winnable_game.txt. _Signed: Dev B, Dev A_

**Session 11** | 30 Oct, 14:00-16:00 (2h) | Remote | Dev A (Driver), Dev B (Navigator)  
Bug fixes from testing: Player exit message order, file output flushing. Refactored test comments for clarity. Code review: extracted constants, improved variable names. Re-ran full test suite. _Signed: Dev A, Dev B_

**Session 12** | 1 Nov, 15:00-17:30 (2.5h) | In-person | Dev B (Driver), Dev A (Navigator)  
Final integration testing with n=2,3,4 players using various pack files. Created TEST-README.txt with usage instructions. Wrote report sections 1-2. Verified all tests pass, manual testing of output files. _Signed: Dev B, Dev A_

**Total**: 27.5 hours across 12 sessions (5 in-person, 7 remote)

---

## Summary

This implementation delivers a complete, thread-safe card game simulation using ordered deck locking for deadlock-free concurrent operations. The design prioritizes correctness, follows object-oriented principles, and includes comprehensive testing at unit and integration levels. All specification requirements are met, including immediate win detection, atomic draw+discard operations, correct file output formats, and robust input validation.
