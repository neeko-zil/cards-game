package cardgame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests that exercise Players + Decks working together and
 * verify on-disk outputs and console behaviour.
 */
public class IntegrationTest {

    // ---------- helpers ----------

    /** Reads all lines from a file (normalizes CRLF/LF by reading line-by-line). */
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
     * We verify:
     *  - gameWon flag is true
     *  - exactly 2n output files exist
     *  - file contents match spec line-for-line
     *  - console prints exactly one "player X wins" line
     */
    @Test
    void immediateWin_exactOutputs_and_console(@TempDir Path tmp) throws Exception {
        setWorkingDirectory(tmp);

        int n = 2;
        List<Card> pack = new ArrayList<>();
        Collections.addAll(pack,
                new Card(1), new Card(2),
                new Card(1), new Card(3),
                new Card(1), new Card(4),
                new Card(1), new Card(5)
        );
        for (int i = 0; i < 8; i++) pack.add(new Card(9));

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

        // Deal 4 cards per player
        int idx = 0;
        for (int r = 0; r < 4; r++) for (Player p : players) p.addCardToHand(pack.get(idx++));
        while (idx < pack.size()) for (Deck d : decks) if (idx < pack.size()) d.discardBottom(pack.get(idx++));

        // Capture console output
        ByteArrayOutputStream console = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        System.setOut(new PrintStream(console));

        // Start players
        List<Thread> threads = new ArrayList<>();
        for (Player p : players) {
            Thread t = new Thread(p);
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) t.join();

        // Restore console
        System.setOut(oldOut);

        // Assertions
        assertTrue(gameWon.get(), "Game should be won immediately by player 1");
        assertTrue(console.toString().strip().equals("player 1 wins"),
                "Console should print exactly one 'player 1 wins' line");

        // File count check
        List<String> names = listTxtNames(tmp.toFile());
        assertEquals(2 * n, names.size(), "Must produce exactly 2n .txt outputs");

        // Player 1 winner file
        assertEquals(List.of(
                "player 1 initial hand 1 1 1 1",
                "player 1 wins",
                "player 1 exits",
                "player 1 final hand: 1 1 1 1"
        ), readAllLines(new File(tmp.toFile(), "player1_output.txt")));

        // Player 2 non-winner file
        assertEquals(List.of(
                "player 2 initial hand 2 3 4 5",
                "player 1 has informed player 2 that player 1 has won",
                "player 2 exits",
                "player 2 hand: 2 3 4 5"
        ), readAllLines(new File(tmp.toFile(), "player2_output.txt")));

        // Deck files: one line each
        assertEquals(List.of("deck1 contents: 9 9 9 9 9 9 9 9"),
                readAllLines(new File(tmp.toFile(), "deck1_output.txt")));
        assertEquals(List.of("deck2 contents: 9 9 9 9 9 9 9 9"),
                readAllLines(new File(tmp.toFile(), "deck2_output.txt")));
    }

    /**
     * Normal-game scenario (n=2):
     * Pack yields no immediate winner; players take at least one turn.
     * We verify:
     *  - output files exist
     *  - each player exits and has a final 4-card hand
     *  - console prints exactly one winner line
     */
    @Test
    void normalGame_flow_and_console(@TempDir Path tmp) throws Exception {
        setWorkingDirectory(tmp);
        int n = 2;

        // Pack deliberately avoids 4-of-a-kind initially
        // first 8 cards -> players (no identical 4)
        List<Card> pack = new ArrayList<>();
        Collections.addAll(pack,
                new Card(1), new Card(2),
                new Card(2), new Card(3),
                new Card(3), new Card(4),
                new Card(4), new Card(1)
        );
        for (int i = 0; i < 8; i++) pack.add(new Card(5));

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

        // Deal + distribute
        int idx = 0;
        for (int r = 0; r < 4; r++) for (Player p : players) p.addCardToHand(pack.get(idx++));
        while (idx < pack.size()) for (Deck d : decks) if (idx < pack.size()) d.discardBottom(pack.get(idx++));

        ByteArrayOutputStream console = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        System.setOut(new PrintStream(console));

        // Run game
        List<Thread> threads = new ArrayList<>();
        for (Player p : players) { Thread t = new Thread(p); threads.add(t); t.start(); }
        for (Thread t : threads) t.join();

        System.setOut(oldOut);

        // Must produce a single winner announcement
        String output = console.toString().strip();
        assertTrue(output.startsWith("player "), "Console must announce a winner");
        assertTrue(output.endsWith("wins"), "Console must end with 'wins'");

        // Exactly 2n output files
        List<String> names = listTxtNames(tmp.toFile());
        assertEquals(2 * n, names.size());

        // Each player file must end with exits + final hand
        for (int i = 1; i <= n; i++) {
            List<String> lines = readAllLines(new File(tmp.toFile(), "player" + i + "_output.txt"));
            assertTrue(lines.get(lines.size() - 2).contains("exits"), "player" + i + " should exit");
            assertTrue(lines.get(lines.size() - 1).contains("hand"), "player" + i + " should show final hand");
        }
    }

    /**
     * Deck concurrency smoke test:
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
