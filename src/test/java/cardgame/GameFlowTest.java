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
 * End-to-end game flow tests.
 */
public class GameFlowTest {

    @AfterEach
    void cleanup() {
        for (int i = 1; i <= 3; i++) {
            new File("player" + i + "_output.txt").delete();
            new File("deck" + i + "_output.txt").delete();
        }
    }

    @Test
    void testImmediateWin() throws Exception {
        List<Card> pack = new ArrayList<>();
        // P1 gets four 1s, P2 gets mixed
        Collections.addAll(pack,
            new Card(1), new Card(2),
            new Card(1), new Card(3),
            new Card(1), new Card(4),
            new Card(1), new Card(5));
        for (int i = 0; i < 8; i++) pack.add(new Card(9));

        runGame(2, pack);

        String p1 = read("player1_output.txt");
        assertTrue(p1.contains("player 1 wins"));
        assertTrue(p1.contains("1 1 1 1"));
    }

    @Test
    void testMultiTurnWin() throws Exception {
        List<Card> pack = new ArrayList<>();
        // P1: [1,1,1,2], deck1 has 1 at front
        Collections.addAll(pack,
            new Card(1), new Card(2),
            new Card(1), new Card(3),
            new Card(1), new Card(4),
            new Card(2), new Card(5),
            new Card(1), new Card(6),
            new Card(7), new Card(8),
            new Card(9), new Card(10),
            new Card(11), new Card(12));

        runGame(2, pack);

        String p1 = read("player1_output.txt");
        String p2 = read("player2_output.txt");
        assertTrue(p1.contains("wins") || p2.contains("wins"));
    }

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

        for (Deck d : decks) {
            try (BufferedWriter w = new BufferedWriter(new FileWriter("deck" + d.getId() + "_output.txt"))) {
                w.write("deck" + d.getId() + " contents:");
                for (Card c : d.getContents()) w.write(" " + c.getDenomination());
                w.newLine();
            } catch (IOException ignored) {}
        }
    }

    private String read(String file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
        }
        return sb.toString();
    }
}
