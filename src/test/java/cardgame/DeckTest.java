package cardgame;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Deck class.
 */
public class DeckTest {

    /** New decks have the given id and start empty. */
    @Test
    void deckCreation_idAndEmpty() {
        Deck deck = new Deck(1);
        assertEquals(1, deck.getId());
        assertEquals(0, deck.size());
        assertTrue(deck.getContents().isEmpty());
    }

    /** Discarding to the bottom increases size. */
    @Test
    void discardIncreasesSize() {
        Deck deck = new Deck(1);
        deck.discardBottom(new Card(5));
        assertEquals(1, deck.size());
    }

    /** FIFO: draw from top returns cards in the order they were inserted. */
    @Test
    void fifoBehavior_drawTopThenTop() {
        Deck deck = new Deck(1);
        deck.discardBottom(new Card(1));
        deck.discardBottom(new Card(2));
        deck.discardBottom(new Card(3));

        assertEquals(3, deck.size());
        assertEquals(1, deck.drawTop().getDenomination());
        assertEquals(2, deck.drawTop().getDenomination());
        assertEquals(3, deck.drawTop().getDenomination());
        assertEquals(0, deck.size());
        assertNull(deck.drawTop(), "Drawing from an empty deck should return null");
    }

    /** Drawing from an empty deck returns null. */
    @Test
    void drawFromEmptyReturnsNull() {
        Deck deck = new Deck(1);
        assertNull(deck.drawTop());
    }

    /** getContents returns a snapshot in top-to-bottom order and is independent of future mutations. */
    @Test
    void getContents_snapshotOrderAndIndependence() {
        Deck deck = new Deck(1);
        deck.discardBottom(new Card(1));
        deck.discardBottom(new Card(2));
        deck.discardBottom(new Card(3));

        List<Card> snap1 = deck.getContents();
        assertEquals(3, snap1.size());
        assertEquals(1, snap1.get(0).getDenomination());
        assertEquals(2, snap1.get(1).getDenomination());
        assertEquals(3, snap1.get(2).getDenomination());

        // mutate deck
        deck.drawTop(); // remove 1
        deck.discardBottom(new Card(4));

        // previous snapshot unchanged
        assertEquals(3, snap1.size());
        assertEquals(1, snap1.get(0).getDenomination());
        assertEquals(2, snap1.get(1).getDenomination());
        assertEquals(3, snap1.get(2).getDenomination());

        // new snapshot reflects new state
        List<Card> snap2 = deck.getContents();
        assertEquals(3, snap2.size());
        assertEquals(2, snap2.get(0).getDenomination()); // top is now 2
        assertEquals(3, snap2.get(1).getDenomination());
        assertEquals(4, snap2.get(2).getDenomination()); // bottom is 4
    }

    /** Null discards are rejected. */
    @Test
    void discardNullThrows() {
        Deck deck = new Deck(1);
        assertThrows(NullPointerException.class, () -> deck.discardBottom(null));
    }

    /**
     * Concurrency smoke test:
     * - preload 200 cards
     * - multiple threads draw until empty
     * - no lost/duplicated cards; deck ends empty
     */
    @Test
    void concurrency_noLostOrDuplicatedCards() throws InterruptedException {
        Deck deck = new Deck(1);
        int total = 200;
        for (int i = 1; i <= total; i++) {
            deck.discardBottom(new Card(i));
        }

        List<Card> drawn = Collections.synchronizedList(new ArrayList<>());
        int threadsN = 8;
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

        assertEquals(total, drawn.size(), "All cards should be drawn exactly once");
        assertEquals(0, deck.size(), "Deck should be empty at the end");

        // Optional duplicate check by value (cheap validation)
        boolean[] seen = new boolean[total + 1];
        for (Card c : drawn) {
            int v = c.getDenomination();
            assertTrue(v >= 1 && v <= total, "Unexpected card value: " + v);
            assertFalse(seen[v], "Duplicate card drawn: " + v);
            seen[v] = true;
        }
    }
}
