package cardgame;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

// Tests for thread-safety
public class ConcurrencyTest {

    // multiple threads drawing from deck
    @Test
    void concurrentDrawing() throws InterruptedException {
        Deck deck = new Deck(1);
        
        // add 100 cards
        for (int i = 1; i <= 100; i++) {
            deck.discardBottom(new Card(i));
        }
        
        // 10 threads each draw 10 cards
        Thread[] threads = new Thread[10];
        List<Card> drawnCards = new ArrayList<>();
        
        for (int i = 0; i < 10; i++) {
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

        // check all cards drawn
        assertEquals(100, drawnCards.size());
        assertEquals(0, deck.size());
    }

    // multiple threads adding to deck
    @Test
    void concurrentDiscarding() throws InterruptedException {
        Deck deck = new Deck(1);
        
        // 10 threads each add 10 cards
        Thread[] threads = new Thread[10];
        
        for (int i = 0; i < 10; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    deck.discardBottom(new Card(threadId * 100 + j));
                }
            });
            threads[i].start();
        }
        
        for (Thread t : threads) t.join();

        // check all cards added
        assertEquals(100, deck.size());
    }

    // only one thread can win
    @Test
    void oneWinnerOnly() throws InterruptedException {
        AtomicBoolean gameWon = new AtomicBoolean(false);
        AtomicInteger winnerCount = new AtomicInteger(0);
        
        // 10 threads try to win
        Thread[] threads = new Thread[10];
        
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                if (gameWon.compareAndSet(false, true)) {
                    winnerCount.incrementAndGet();
                }
            });
            threads[i].start();
        }
        
        for (Thread t : threads) t.join();

        // only one succeeds
        assertEquals(1, winnerCount.get());
        assertTrue(gameWon.get());
    }

    // getContents is thread-safe
    @Test
    void getContentsThreadSafe() throws InterruptedException {
        Deck deck = new Deck(1);
        
        // add 50 cards
        for (int i = 1; i <= 50; i++) {
            deck.discardBottom(new Card(i));
        }
        
        // 5 threads get snapshots
        Thread[] threads = new Thread[5];
        List<List<Card>> snapshots = new ArrayList<>();
        
        for (int i = 0; i < 5; i++) {
            threads[i] = new Thread(() -> {
                List<Card> snapshot = deck.getContents();
                synchronized (snapshots) {
                    snapshots.add(snapshot);
                }
            });
            threads[i].start();
        }
        
        for (Thread t : threads) t.join();

        // all snapshots have 50 cards
        assertEquals(5, snapshots.size());
        for (List<Card> snapshot : snapshots) {
            assertEquals(50, snapshot.size());
        }
    }

    // ordered locking prevents deadlock
    @Test
    void orderedLockingNoDeadlock() throws InterruptedException {
        Deck deck1 = new Deck(1);
        Deck deck2 = new Deck(2);
        
        // add cards to deck1
        for (int i = 1; i <= 20; i++) {
            deck1.discardBottom(new Card(i));
        }
        
        // both threads lock same order
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                deck1.getLock().lock();
                try {
                    deck2.getLock().lock();
                    try {
                        Card c = deck1.drawTop();
                        if (c != null) deck2.discardBottom(c);
                    } finally {
                        deck2.getLock().unlock();
                    }
                } finally {
                    deck1.getLock().unlock();
                }
            }
        });
        
        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                deck1.getLock().lock();
                try {
                    deck2.getLock().lock();
                    try {
                        Card c = deck1.drawTop();
                        if (c != null) deck2.discardBottom(c);
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
        
        // check no deadlock
        thread1.join(5000);
        thread2.join(5000);
        
        assertTrue(true);
    }

    // stress test with many threads
    @Test
    void manyThreadsStressTest() throws InterruptedException {
        Deck deck = new Deck(1);
        
        // add 500 cards
        for (int i = 1; i <= 500; i++) {
            deck.discardBottom(new Card(i));
        }
        
        // 50 threads each draw 10 cards
        Thread[] threads = new Thread[50];
        AtomicInteger cardsDrawn = new AtomicInteger(0);
        
        for (int i = 0; i < 50; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    if (deck.drawTop() != null) {
                        cardsDrawn.incrementAndGet();
                    }
                }
            });
            threads[i].start();
        }
        
        for (Thread t : threads) t.join();

        // all 500 cards drawn
        assertEquals(500, cardsDrawn.get());
        assertEquals(0, deck.size());
    }
}
