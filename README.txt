ECM2414 Software Development – Card Game Project (2025/26)

The project contains the following files:
cards.jar – executable program containing both source (.java) and compiled (.class) files
cardsTest.zip – test code and test resources
Report/ – design, testing, and pair programming log reports

How to Build the JAR File

To create cards.jar with both source (.java) and compiled (.class) files:

Requirements:
Java 11 or higher
Maven 3.6+

Steps:
1. Navigate to the project root directory (where pom.xml is located)
2. Run: mvn clean package
3. The cards.jar file will be created directly in the target/ directory

This creates target/cards.jar containing both .class and .java files as required.

How to Run the Program

Requirements:
Java 11 or higher
(Optional) Maven 3.6+ if rebuilding from source

Running the compiled jar:
java -jar cards.jar

When prompted:
Enter the number of players (integer n).
Enter the path to a valid card pack file (text file with 8n non-negative integers, one per line).

The game will start automatically and produce:
playerX_output.txt for each player
deckX_output.txt for each deck
It will also print to the console which player wins.

Example run:
Please enter the number of players: 4
Please enter location of pack: packs/pack1.txt
player 3 wins

How to Run the Tests

Requirements:
Java 11 or higher
Maven 3.6+

Run all tests from the project root:
mvn test

or if using the packaged test zip:
Extract cardsTest.zip
From inside the extracted folder, run:
mvn test

Test Suite Description

JUnit Version: JUnit 5

Test Files:
CardTest.java – tests card equality, immutability, and hashCode correctness
DeckTest.java – tests FIFO behaviour and thread safety of draw/discard
CardGameTest.java – tests pack validation and input errors
GameFlowTest.java – checks correct game flow, file creation, and winner detection
ConcurrencyTest.java – checks thread safety, atomic actions, and deadlock prevention
IntegrationTest.java – runs a full game and verifies file outputs and total card count

All tests can be run automatically using Maven. They cover normal, boundary, and error cases.

Input Pack Format

The input pack file must contain exactly 8n integers, where n is the number of players. Each integer must be non-negative and written on its own line.

Example pack for n = 2:
1
2
3
4
1
2
3
4
1
2
3
4
1
2
3
4

Output Files

When the game finishes, the program creates the following files:
player1_output.txt, player2_output.txt, …, playerN_output.txt
deck1_output.txt, deck2_output.txt, …, deckN_output.txt

Each player file shows:
Initial hand
Every draw and discard
Notification when another player wins
Final hand before exiting

Each deck file lists the final contents of that deck.

Error Handling

If the input pack is invalid (wrong number of cards, negative values, or non-integers), the program displays an error message and asks for a new input. It does not crash or start the game until valid input is given.

Notes

The program may produce slightly different action orders between runs because of thread scheduling. This is expected behaviour and does not affect correctness. All output formats follow the wording and layout described in the coursework specification.
