package cardgame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests that exercise Players + Decks working together and
 * verify on-disk outputs are produced in the required format (exact lines).
 */
public class IntegrationTest {

    // ---------- helpers ----------

    /** Read all lines from a file (normalizes CRLF/LF by reading line-by-line). */
    private static List<String> readAllLines(File f) throws Exception {
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            List<String> out = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) out.add(line);
            return out;
        }
    }

    /** Make relative file writes (playerX_output.txt / deckX_output.txt) land in a temp folder. */
    private static void setWorkingDirectory(Path dir) {
        System.setProperty("user.dir", dir.toAbsolutePath().toString());
    }

    /** List *.txt files in a dir by name. */
    private static List<String> listTxtNames(File dir) {
        String[] arr = dir.list((d, name) -> name.endsWith(".txt"));
        return arr == null ? List.of() : Arrays.asList(arr);
    }

    // ---------- tests ----------

    /**
     * Immediate-win scenario (n=2):
     * Player 1 is dealt four 1s on the initial deal, so they should win immediately.
     * We verify exact file contents (line-by-line) and that exactly 2n files are produced.
     */
    @Test
    void immediateWin_exactOutputs_and_fileCount(@TempDir Path tmp) throws Exception {
        setWorkingDirectory(tmp);

        int n = 2;

        // Build a pack where P1 gets four 1s at deal (indices 1,3,5,7 among first 8 cards)
        List<Card> pack = new ArrayList<>();
        Collections.addAll(pack,
            new Card(1), new Card(2),
            new Card(1), new Card(3),
            new Card(1), new Card(4),
            new Card(1), new Card(5)
        );
        // Remaining 8 cards go into decks (arbitrary non-negative)
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

        // Deal 4 cards per player, round-robin
        int idx = 0;
        for (int round = 0; round < 4; round++) {
            for (Player p : players) p.addCardToHand(pack.get(idx++));
        }
        // Remaining cards to decks, round-robin
        while (idx < pack.size()) {
            for (Deck d : decks) {
                if (idx < pack.size()) d.discardBottom(pack.get(idx++));
            }
        }

        // Start players
        List<Thread> threads = new ArrayList<>();
        for (Player p : players) {
            Thread t = new Thread(p);
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) t.join();

        assertTrue(gameWon.get(), "Game should be won immediately by player 1");

        // Check exactly 2n files exist
        List<String> names = listTxtNames(tmp.toFile());
        assertEquals(2 * n, names.size(), "Must produce exactly 2n .txt outputs");

        // Exact file contents (line-by-line)

        // player1_output.txt (winner) — exactly 4 lines
        List<String> p1 = readAllLines(new File(tmp.toFile(), "player1_output.txt"));
        assertEquals(List.of(
            "player 1 initial hand 1 1 1 1",
            "player 1 wins",
            "player 1 exits",
            "player 1 final hand: 1 1 1 1"
        ), p1, "player1_output.txt must match exactly");

        // player2_output.txt (non-winner) — exactly 4 lines
        List<String> p2 = readAllLines(new File(tmp.toFile(), "player2_output.txt"));
        assertEquals(List.of(
            "player 2 initial hand 2 3 4 5",
            "player 1 has informed player 2 that player 1 has won",
            "player 2 exits",
            "player 2 hand: 2 3 4 5"
        ), p2, "player2_output.txt must match exactly");

        // deck files: one line each, exact wording "deckX contents:"
        List<String> d1 = readAllLines(new File(tmp.toFile(), "deck1_output.txt"));
        List<String> d2 = readAllLines(new File(tmp.toFile(), "deck2_output.txt"));
        assertEquals(List.of("deck1 contents: 9 9 9 9 9 9 9 9"), d1, "deck1_output.txt must match exactly");
        assertEquals(List.of("deck2 contents: 9 9 9 9 9 9 9 9"), d2, "deck2_output.txt must match exactly");
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
}
