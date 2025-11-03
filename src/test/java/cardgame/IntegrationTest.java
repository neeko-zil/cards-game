package cardgame;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * End-to-end tests that exercise Players + Decks working together and
 * verify on-disk outputs are produced in the required format.
 */
public class IntegrationTest {

    /**
     * Tests immediate win scenario where Player 1 receives four cards of the same value
     * on the initial deal. Verifies that the game correctly detects the win, stops all
     * threads, and produces the required output files with proper formatting.
     */
    @Test
    void immediateWin_createsFilesAndWinner() throws Exception {
        int n = 2;
        // Build a pack where Player 1 gets four 1s (instant win)
        // Cards alternate P1, P2, P1, P2... during round-robin dealing
        List<Card> pack = new ArrayList<>();
        Collections.addAll(pack,
                new Card(1), new Card(2),
                new Card(1), new Card(3),
                new Card(1), new Card(4),
                new Card(1), new Card(5)
        );
        // Remaining 8 cards go to decks (values arbitrary non-negative)
        for (int i = 0; i < 8; i++) pack.add(new Card(9));

        // --- Wire up game programmatically (bypass CLI) ---
        List<Player> players = new ArrayList<>();
        List<Deck> decks = new ArrayList<>();
        AtomicBoolean gameWon = new AtomicBoolean(false);

        for (int i = 1; i <= n; i++) decks.add(new Deck(i));
        for (int i = 1; i <= n; i++) {
            Deck left = decks.get(i - 1);
            Deck right = decks.get(i % n);
            Player p = new Player(i, left, right, gameWon, players);
            players.add(p);
        }

        // Deal 4 cards to each player round-robin
        int idx = 0;
        for (int round = 0; round < 4; round++) {
            for (Player p : players) {
                p.addCardToHand(pack.get(idx++));
            }
        }
        // Remaining cards to decks (bottom) round-robin
        while (idx < pack.size()) {
            for (Deck d : decks) {
                if (idx < pack.size()) d.discardBottom(pack.get(idx++));
            }
        }

        // Start and join player threads
        List<Thread> threads = new ArrayList<>();
        for (Player p : players) {
            Thread t = new Thread(p);
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) t.join();

        // --- Assertions ---
        assertTrue(gameWon.get(), "Game should have been won immediately by player 1");

        // Verify all output files were created
        assertTrue(new File("player1_output.txt").exists(), "player1_output.txt should exist");
        assertTrue(new File("player2_output.txt").exists(), "player2_output.txt should exist");
        assertTrue(new File("deck1_output.txt").exists(), "deck1_output.txt should exist");
        assertTrue(new File("deck2_output.txt").exists(), "deck2_output.txt should exist");

        // Verify winner's output file contains required messages
        try (BufferedReader br = new BufferedReader(new FileReader("player1_output.txt"))) {
            String all = br.lines().reduce("", (a, b) -> a + b + "\n");
            assertTrue(all.contains("player 1 wins"));
            assertTrue(all.contains("player 1 exits"));
            // final hand printed with colon per spec
            assertTrue(all.contains("player 1 final hand:"));
        }

        // Verify non-winner's output file contains required messages
        try (BufferedReader br = new BufferedReader(new FileReader("player2_output.txt"))) {
            String all = br.lines().reduce("", (a, b) -> a + b + "\n");
            assertTrue(all.contains("player 1 has informed player 2 that player 1 has won"));
            assertTrue(all.contains("player 2 exits"));
            assertTrue(all.contains("player 2 hand:"));
        }
    }

    /**
     * Tests thread-safe concurrent access to a single deck.
     * Multiple threads draw cards simultaneously and we verify no cards are lost or duplicated.
     */
    @Test
    void deckConcurrency_noLostCards() throws Exception {
        Deck deck = new Deck(1);
        
        // Preload deck with 100 cards
        for (int i = 1; i <= 100; i++) {
            deck.discardBottom(new Card(i));
        }
        
        // Create threads that draw from deck
        int numThreads = 10;
        Thread[] threads = new Thread[numThreads];
        List<Card> drawnCards = new ArrayList<>();
        
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    Card drawn = deck.drawTop();
                    if (drawn != null) {
                        synchronized (drawnCards) {
                            drawnCards.add(drawn);
                        }
                    }
                }
            });
            threads[i].start();
        }
        for (Thread t : threads) t.join();

        assertEquals(100, drawnCards.size(), "All cards should be drawn exactly once");
        assertEquals(0, deck.size(), "Deck should be empty at the end");
    }
}
