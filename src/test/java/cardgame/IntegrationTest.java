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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * End-to-end tests that verify exact output format and file contents.
 */
public class IntegrationTest {

    @AfterEach
    void cleanup() {
        // Clean up output files after each test
        for (int i = 1; i <= 3; i++) {
            new File("player" + i + "_output.txt").delete();
            new File("deck" + i + "_output.txt").delete();
        }
    }

    /**
     * Tests immediate win with exact output verification.
     * Player 1 gets four 1s and wins immediately.
     */
    @Test
    void testImmediateWinExactOutput() throws Exception {
        int n = 2;

        // Build pack where P1 gets four 1s
        List<Card> pack = new ArrayList<>();
        Collections.addAll(pack,
            new Card(1), new Card(2),
            new Card(1), new Card(3),
            new Card(1), new Card(4),
            new Card(1), new Card(5)
        );
        for (int i = 0; i < 8; i++) pack.add(new Card(9));

        runGame(n, pack);

        assertTrue(new File("player1_output.txt").exists());
        assertTrue(new File("player2_output.txt").exists());
        assertTrue(new File("deck1_output.txt").exists());
        assertTrue(new File("deck2_output.txt").exists());

        // Verify exact player1 output (winner)
        List<String> p1 = readLines("player1_output.txt");
        assertEquals(List.of(
            "player 1 initial hand 1 1 1 1",
            "player 1 wins",
            "player 1 exits",
            "player 1 final hand: 1 1 1 1"
        ), p1);

        // Verify exact player2 output (non-winner)
        List<String> p2 = readLines("player2_output.txt");
        assertEquals(List.of(
            "player 2 initial hand 2 3 4 5",
            "player 1 has informed player 2 that player 1 has won",
            "player 2 exits",
            "player 2 hand: 2 3 4 5"
        ), p2);

        // Verify deck outputs
        List<String> d1 = readLines("deck1_output.txt");
        List<String> d2 = readLines("deck2_output.txt");
        assertEquals(List.of("deck1 contents: 9 9 9 9"), d1);
        assertEquals(List.of("deck2 contents: 9 9 9 9"), d2);
    }

    // Helper methods

    private void runGame(int n, List<Card> pack) throws InterruptedException {
        List<Player> players = new ArrayList<>();
        List<Deck> decks = new ArrayList<>();
        AtomicBoolean gameWon = new AtomicBoolean(false);

        for (int i = 1; i <= n; i++) decks.add(new Deck(i));
        for (int i = 1; i <= n; i++) {
            players.add(new Player(i, decks.get(i - 1), decks.get(i % n), gameWon, players));
        }

        int idx = 0;
        for (int r = 0; r < 4; r++) {
            for (Player p : players) p.addCardToHand(pack.get(idx++));
        }
        while (idx < pack.size()) {
            for (Deck d : decks) {
                if (idx < pack.size()) d.discardBottom(pack.get(idx++));
            }
        }

        List<Thread> threads = new ArrayList<>();
        for (Player p : players) {
            Thread t = new Thread(p);
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) t.join();

        // Write deck outputs
        for (Deck d : decks) {
            writeDeckOutput(d);
        }
    }

    private void writeDeckOutput(Deck deck) {
        try (BufferedWriter w = new BufferedWriter(new FileWriter("deck" + deck.getId() + "_output.txt"))) {
            w.write("deck" + deck.getId() + " contents:");
            for (Card c : deck.getContents()) w.write(" " + c.getDenomination());
            w.newLine();
        } catch (IOException ignored) {}
    }

    private List<String> readLines(String file) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) lines.add(line);
        }
        return lines;
    }
}
