package cardgame;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Tests for complete game scenarios - verifies games run correctly from start to finish.
 */
public class GameFlowTest {

    @AfterEach
    void cleanup() {
        // Clean up output files after each test
        for (int i = 1; i <= 3; i++) {
            new File("player" + i + "_output.txt").delete();
            new File("deck" + i + "_output.txt").delete();
        }
    }

    /**
     * Test immediate win scenario.
     * Player 1 gets four 1s on initial deal and wins right away.
     */
    @Test
    void immediateWin() throws Exception {
        List<Card> pack = new ArrayList<>();
        
        // P1 gets four 1s, P2 gets mixed cards
        Collections.addAll(pack,
            new Card(1), new Card(2),
            new Card(1), new Card(3),
            new Card(1), new Card(4),
            new Card(1), new Card(5));
        
        // Fill remaining slots for decks
        for (int i = 0; i < 8; i++) pack.add(new Card(9));

        runGame(2, pack);

        // Verify P1 won
        String p1 = readFile("player1_output.txt");
        assertTrue(p1.contains("player 1 wins"));
        assertTrue(p1.contains("1 1 1 1"));
    }

    /**
     * Test game that runs multiple turns before someone wins.
     * Player 1 starts with [1,1,1,2] and can draw a 1 to win.
     */
    @Test
    void multiTurnWin() throws Exception {
        List<Card> pack = new ArrayList<>();
        
        // P1: [1,1,1,2], deck1 has 1 at front for P1 to draw
        Collections.addAll(pack,
            new Card(1), new Card(2),
            new Card(1), new Card(3),
            new Card(1), new Card(4),
            new Card(2), new Card(5),
            new Card(1), new Card(6),  // P1 can draw this 1
            new Card(7), new Card(8),
            new Card(9), new Card(10),
            new Card(11), new Card(12));

        runGame(2, pack);

        // One player should win
        String p1 = readFile("player1_output.txt");
        String p2 = readFile("player2_output.txt");
        assertTrue(p1.contains("wins") || p2.contains("wins"));
    }

    // Helper methods

    /**
     * Runs a complete game with given pack.
     */
    private void runGame(int n, List<Card> pack) throws InterruptedException {
        List<Player> players = new ArrayList<>();
        List<Deck> decks = new ArrayList<>();
        AtomicBoolean gameWon = new AtomicBoolean(false);

        // Create decks
        for (int i = 1; i <= n; i++) decks.add(new Deck(i));
        
        // Create players with ring topology
        for (int i = 1; i <= n; i++) {
            players.add(new Player(i, decks.get(i - 1), decks.get(i % n), gameWon, players));
        }

        // Deal cards to players
        int idx = 0;
        for (int r = 0; r < 4; r++) {
            for (Player p : players) p.addCardToHand(pack.get(idx++));
        }
        
        // Fill decks with remaining cards
        while (idx < pack.size()) {
            for (Deck d : decks) {
                if (idx < pack.size()) d.discardBottom(pack.get(idx++));
            }
        }

        // Start all player threads
        List<Thread> threads = new ArrayList<>();
        for (Player p : players) {
            Thread t = new Thread(p);
            threads.add(t);
            t.start();
        }
        
        // Wait for all threads to finish
        for (Thread t : threads) t.join();

        // Write deck outputs
        for (Deck d : decks) {
            writeDeckOutput(d);
        }
    }

    /**
     * Writes deck output file.
     */
    private void writeDeckOutput(Deck deck) {
        try (BufferedWriter w = new BufferedWriter(new FileWriter("deck" + deck.getId() + "_output.txt"))) {
            w.write("deck" + deck.getId() + " contents:");
            for (Card c : deck.getContents()) w.write(" " + c.getDenomination());
            w.newLine();
        } catch (IOException ignored) {}
    }

    /**
     * Reads entire file as string.
     */
    private String readFile(String file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
        }
        return sb.toString();
    }
}
