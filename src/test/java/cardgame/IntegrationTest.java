package cardgame;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the complete game system.
 */
public class IntegrationTest {
    
    @Test
    public void testImmediateWinScenario() throws InterruptedException {
        // Create pack that gives player 1 four 1s
        int n = 2;
        List<Card> pack = new ArrayList<>();
        
        // First 4 cards go to player 1 (all 1s - winning hand)
        pack.add(new Card(1));
        pack.add(new Card(2));
        pack.add(new Card(1));
        pack.add(new Card(2));
        pack.add(new Card(1));
        pack.add(new Card(2));
        pack.add(new Card(1));
        pack.add(new Card(2));
        
        // Remaining cards for decks
        for (int i = 0; i < 8; i++) {
            pack.add(new Card(i + 1));
        }
        
        // Set up game
        List<Player> players = new ArrayList<>();
        List<Deck> decks = new ArrayList<>();
        AtomicBoolean gameWon = new AtomicBoolean(false);
        
        for (int i = 1; i <= n; i++) {
            decks.add(new Deck(i));
        }
        
        for (int i = 1; i <= n; i++) {
            Deck leftDeck = decks.get(i - 1);
            Deck rightDeck = decks.get(i % n);
            Player player = new Player(i, leftDeck, rightDeck, gameWon, players);
            players.add(player);
        }
        
        // Distribute cards
        int cardIndex = 0;
        for (int round = 0; round < 4; round++) {
            for (Player player : players) {
                player.addCardToHand(pack.get(cardIndex++));
            }
        }
        
            while (cardIndex < pack.size()) {
                for (Deck deck : decks) {
                    if (cardIndex < pack.size()) {
                        deck.discardBottom(pack.get(cardIndex++));
                    }
                }
            }
        
        // Start threads
        List<Thread> threads = new ArrayList<>();
        for (Player player : players) {
            Thread thread = new Thread(player);
            threads.add(thread);
            thread.start();
        }
        
        // Wait for completion
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Verify game was won
        assertTrue(gameWon.get(), "Game should have been won");
    }
    
    @Test
    public void testPlayerHandManagement() {
        Deck leftDeck = new Deck(1);
        Deck rightDeck = new Deck(2);
        AtomicBoolean gameWon = new AtomicBoolean(false);
        List<Player> players = new ArrayList<>();
        
        Player player = new Player(1, leftDeck, rightDeck, gameWon, players);
        
        // Add initial hand
        player.addCardToHand(new Card(1));
        player.addCardToHand(new Card(2));
        player.addCardToHand(new Card(3));
        player.addCardToHand(new Card(4));
        
        assertEquals(4, player.getHand().size());
        assertEquals(1, player.getId());
    }
    
    @Test
    public void testConcurrentDeckAccess() throws InterruptedException {
        Deck deck = new Deck(1);
        
        // Add cards to deck
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
        
        // Wait for all threads
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Verify exactly 100 cards were drawn
        assertEquals(100, drawnCards.size());
        assertEquals(0, deck.size());
    }
}
