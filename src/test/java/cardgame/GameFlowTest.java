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

// Tests for complete game scenarios
public class GameFlowTest {

    @AfterEach
    void cleanup() {
        // clean up output files
        for (int i = 1; i <= 3; i++) {
            new File("player" + i + "_output.txt").delete();
            new File("deck" + i + "_output.txt").delete();
        }
    }

    // player wins immediately on deal
    @Test
    void immediateWin() throws Exception {
        List<Card> pack = new ArrayList<>();
        
        // P1 gets four 1s
        Collections.addAll(pack,
            new Card(1), new Card(2),
            new Card(1), new Card(3),
            new Card(1), new Card(4),
            new Card(1), new Card(5));
        
        // fill decks
        for (int i = 0; i < 8; i++) pack.add(new Card(9));

        runGame(2, pack);

        // check P1 won
        String p1 = readFile("player1_output.txt");
        assertTrue(p1.contains("player 1 wins"));
        assertTrue(p1.contains("1 1 1 1"));
    }

    // game runs multiple turns
    @Test
    void multiTurnWin() throws Exception {
        List<Card> pack = new ArrayList<>();
        
        // P1 can draw winning card
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

        // check someone won
        String p1 = readFile("player1_output.txt");
        String p2 = readFile("player2_output.txt");
        assertTrue(p1.contains("wins") || p2.contains("wins"));
    }

    // helper methods

    // run a game
    private void runGame(int n, List<Card> pack) throws InterruptedException {
        List<Player> players = new ArrayList<>();
        List<Deck> decks = new ArrayList<>();
        AtomicBoolean gameWon = new AtomicBoolean(false);

        // create decks
        for (int i = 1; i <= n; i++) decks.add(new Deck(i));
        
        // create players
        for (int i = 1; i <= n; i++) {
            players.add(new Player(i, decks.get(i - 1), decks.get(i % n), gameWon, players));
        }

        // deal cards to players
        int idx = 0;
        for (int r = 0; r < 4; r++) {
            for (Player p : players) p.addCardToHand(pack.get(idx++));
        }
        
        // fill decks
        while (idx < pack.size()) {
            for (Deck d : decks) {
                if (idx < pack.size()) d.discardBottom(pack.get(idx++));
            }
        }

        // start threads
        List<Thread> threads = new ArrayList<>();
        for (Player p : players) {
            Thread t = new Thread(p);
            threads.add(t);
            t.start();
        }
        
        // wait for threads
        for (Thread t : threads) t.join();

        // write deck outputs
        for (Deck d : decks) {
            writeDeckOutput(d);
        }
    }

    // write deck output
    private void writeDeckOutput(Deck deck) {
        try (BufferedWriter w = new BufferedWriter(new FileWriter("deck" + deck.getId() + "_output.txt"))) {
            w.write("deck" + deck.getId() + " contents:");
            for (Card c : deck.getContents()) w.write(" " + c.getDenomination());
            w.newLine();
        } catch (IOException ignored) {}
    }

    // read file
    private String readFile(String file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
        }
        return sb.toString();
    }
}
