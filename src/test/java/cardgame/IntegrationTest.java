package cardgame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests that exercise Players + Decks working together and
 * verify on-disk outputs are produced in the required format.
 */
public class IntegrationTest {

    /**
     * Immediate-win scenario (n=2):
     * Player 1 is dealt four 1s on the initial deal, so they should win
     * immediately. We verify:
     * - gameWon flag is set
     * - all player/deck output files exist
     * - winner file contains the required tail lines
     */
    @Test
    void immediateWin_createsFilesAndWinner(@TempDir Path tmp) throws Exception {
        // Make relative file writes (playerX_output.txt / deckX_output.txt) land in a temp folder
        setWorkingDirectory(tmp);

        int n = 2;
        // Build a pack: first 8 cards (hands) alternate P1,P2,P1,P2,...
        // P1 receives indices 1,3,5,7 -> four 1s (instant win)
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

        // Files exist
        assertTrue(new File(tmp.toFile(), "player1_output.txt").exists());
        assertTrue(new File(tmp.toFile(), "player2_output.txt").exists());
        assertTrue(new File(tmp.toFile(), "deck1_output.txt").exists());
        assertTrue(new File(tmp.toFile(), "deck2_output.txt").exists());

        // Winner tail lines present in player1_output.txt
        try (BufferedReader br = new BufferedReader(new FileReader(new File(tmp.toFile(), "player1_output.txt")))) {
            String all = br.lines().reduce("", (a, b) -> a + b + "\n");
            assertTrue(all.contains("player 1 wins"));
            assertTrue(all.contains("player 1 exits"));
            // final hand printed with colon per spec
            assertTrue(all.contains("player 1 final hand:"));
        }

        // Non-winner tail lines present in player2_output.txt
        try (BufferedReader br = new BufferedReader(new FileReader(new File(tmp.toFile(), "player2_output.txt")))) {
            String all = br.lines().reduce("", (a, b) -> a + b + "\n");
            assertTrue(all.contains("player 1 has informed player 2 that player 1 has won"));
            assertTrue(all.contains("player 2 exits"));
            assertTrue(all.contains("player 2 hand:"));
        }
    }

    /**
     * Concurrency smoke test on a single deck:
     * - Preload 100 cards
     * - 10 threads draw until empty
     * - Verify no lost/duplicated cards and deck ends empty
     */
    @Test
    void deckConcurrency_noLostCards() throws Exception {
        Deck deck = new Deck(1);
        for (int i = 1; i <= 100; i++) deck.discardBottom(new Card(i));

        List<Card> drawn = Collections.synchronizedList(new ArrayList<>());
        int threadsN = 10;
        Thread[] threads = new Thread[threadsN];

        for (int i = 0; i < threadsN; i++) {
            threads[i] = new Thread(() -> {
                while (true) {
                    Card c = deck.drawTop();
                    if (c == null) break;
                    drawn.add(c);
                }
            });
            threads[i].start();
        }
        for (Thread t : threads) t.join();

        assertEquals(100, drawn.size(), "All cards should be drawn exactly once");
        assertEquals(0, deck.size(), "Deck should be empty at the end");
    }

    // ----------------- helpers -----------------

    /**
     * Tests rely on Card/Player writing relative paths (playerX_output.txt, deckX_output.txt).
     * This method makes those paths land in a dedicated temp directory to avoid polluting the project.
     */
    private static void setWorkingDirectory(Path dir) {
        System.setProperty("user.dir", dir.toAbsolutePath().toString());
    }
}

