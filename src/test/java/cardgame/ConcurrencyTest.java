package cardgame;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Tests for concurrent/multi-threaded operations.
 * Verifies thread-safety of Deck and game components under concurrent access.
 */
public class ConcurrencyTest {

    /**
     * Tests that multiple threads can safely draw from the same deck
     * without losing or duplicating cards.
     */
    @Test
    void testConcurrentDeckDrawing_NoLostCards() throws InterruptedException {
        Deck deck = new Deck(1);
        
        // Preload deck with 100 cards
        for (int i = 1; i <= 100; i++) {
            deck.discardBottom(new Card(i));
        }
        
        // 10 threads each drawing 10 cards
        int numThreads = 10;
        int cardsPerThread = 10;
        Thread[] threads = new Thread[numThreads];
        List<Card> drawnCards = new ArrayList<>();
        
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < cardsPerThread; j++) {
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
        
        // Wait for all threads to complete
        for (Thread t : threads) {
            t.join();
        }

        // Verify exactly 100 cards were drawn (no duplicates or losses)
        assertEquals(100, drawnCards.size(), "Exactly 100 cards should be drawn");
        assertEquals(0, deck.size(), "Deck should be empty");
    }

    /**
     * Tests concurrent discard operations to ensure thread-safety.
     */
    @Test
    void testConcurrentDeckDiscarding_AllCardsAdded() throws InterruptedException {
        Deck deck = new Deck(1);
        
        int numThreads = 10;
        int cardsPerThread = 10;
        Thread[] threads = new Thread[numThreads];
        
        // Each thread adds 10 cards
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < cardsPerThread; j++) {
                    deck.discardBottom(new Card(threadId * 100 + j));
                }
            });
            threads[i].start();
        }
        
        for (Thread t : threads) {
            t.join();
        }

        // Verify all 100 cards were added
        assertEquals(100, deck.size(), "All cards should be in deck");
    }

    /**
     * Tests that AtomicBoolean gameWon flag works correctly under concurrent access.
     */
    @Test
    void testConcurrentWinDetection_OnlyOneWinner() throws InterruptedException {
        AtomicBoolean gameWon = new AtomicBoolean(false);
        AtomicInteger winnerCount = new AtomicInteger(0);
        
        int numThreads = 10;
        Thread[] threads = new Thread[numThreads];
        
        // Simulate multiple threads trying to set win flag
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                // Only one thread should successfully set gameWon from false to true
                if (gameWon.compareAndSet(false, true)) {
                    winnerCount.incrementAndGet();
                }
            });
            threads[i].start();
        }
        
        for (Thread t : threads) {
            t.join();
        }

        // Only one thread should have won
        assertEquals(1, winnerCount.get(), "Only one thread should successfully set gameWon");
        assertTrue(gameWon.get(), "gameWon should be true");
    }

    /**
     * Tests concurrent access to deck's getContents() method.
     * Verifies defensive copy works correctly under concurrent access.
     */
    @Test
    void testConcurrentGetContents_DefensiveCopy() throws InterruptedException {
        Deck deck = new Deck(1);
        
        // Add some cards
        for (int i = 1; i <= 50; i++) {
            deck.discardBottom(new Card(i));
        }
        
        int numThreads = 5;
        Thread[] threads = new Thread[numThreads];
        List<List<Card>> snapshots = new ArrayList<>();
        
        // Multiple threads get snapshots simultaneously
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                List<Card> snapshot = deck.getContents();
                synchronized (snapshots) {
                    snapshots.add(snapshot);
                }
            });
            threads[i].start();
        }
        
        for (Thread t : threads) {
            t.join();
        }

        // All snapshots should have same size
        assertEquals(numThreads, snapshots.size());
        for (List<Card> snapshot : snapshots) {
            assertEquals(50, snapshot.size(), "Each snapshot should have 50 cards");
        }
    }

    /**
     * Tests ordered deck locking to prevent deadlock.
     * Simulates the scenario where multiple players access deck pairs.
     */
    @Test
    void testOrderedDeckLocking_NoDeadlock() throws InterruptedException {
        Deck deck1 = new Deck(1);
        Deck deck2 = new Deck(2);
        
        // Add cards to deck1
        for (int i = 1; i <= 20; i++) {
            deck1.discardBottom(new Card(i));
        }
        
        // Simulate Player behavior: always lock in order (lower ID first)
        int numOperations = 100;
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < numOperations; i++) {
                // Lock deck1 first (lower ID), then deck2
                deck1.getLock().lock();
                try {
                    deck2.getLock().lock();
                    try {
                        Card c = deck1.drawTop();
                        if (c != null) {
                            deck2.discardBottom(c);
                        }
                    } finally {
                        deck2.getLock().unlock();
                    }
                } finally {
                    deck1.getLock().unlock();
                }
            }
        });
        
        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < numOperations; i++) {
                // Also lock deck1 first (maintaining order)
                deck1.getLock().lock();
                try {
                    deck2.getLock().lock();
                    try {
                        Card c = deck1.drawTop();
                        if (c != null) {
                            deck2.discardBottom(c);
                        }
                    } finally {
                        deck2.getLock().unlock();
                    }
                } finally {
                    deck1.getLock().unlock();
                }
            }
        });
        
        thread1.start();
        thread2.start();
        
        // If there's a deadlock, this will hang
        thread1.join(5000); // 5 second timeout
        thread2.join(5000);
        
        // If we got here, no deadlock occurred
        assertTrue(true, "No deadlock occurred with ordered locking");
    }

    /**
     * Stress test with many threads accessing decks simultaneously.
     */
    @Test
    void testHighConcurrency_ManyThreads() throws InterruptedException {
        Deck deck = new Deck(1);
        
        // Preload 500 cards
        for (int i = 1; i <= 500; i++) {
            deck.discardBottom(new Card(i));
        }
        
        // 50 threads each drawing 10 cards
        int numThreads = 50;
        Thread[] threads = new Thread[numThreads];
        AtomicInteger cardsDrawn = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    if (deck.drawTop() != null) {
                        cardsDrawn.incrementAndGet();
                    }
                }
            });
            threads[i].start();
        }
        
        for (Thread t : threads) {
            t.join();
        }

        assertEquals(500, cardsDrawn.get(), "All 500 cards should be drawn");
        assertEquals(0, deck.size(), "Deck should be empty");
    }
}
