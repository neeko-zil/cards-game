package cardgame;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe FIFO deck of cards.
 * <p>
 * Contract:
 * - Draw from the TOP (front) of the deck.
 * - Discard to the BOTTOM (back) of the deck.
 * - Methods are protected by an internal lock.
 * - For operations that span TWO decks (atomic draw+discard), the Player class
 *   is responsible for acquiring both deck locks in a consistent global order
 *   (e.g., lower-id first) to avoid deadlocks.
 */
public class Deck {

    private final int id;
    private final Deque<Card> cards = new ArrayDeque<>();
    private final ReentrantLock lock = new ReentrantLock();

    public Deck(int id) {
        this.id = id;
    }

    /** Deck identifier (1..n). */
    public int getId() {
        return id;
    }

    /**
     * Draws a card from the TOP (front) of the deck.
     * @return the card drawn, or null if the deck is empty.
     */
    public Card drawTop() {
        lock.lock();
        try {
            return cards.pollFirst();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Discards a card to the BOTTOM (back) of the deck.
     * @param card non-null card to add
     */
    public void discardBottom(Card card) {
        Objects.requireNonNull(card, "card");
        lock.lock();
        try {
            cards.addLast(card);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns a snapshot of the deck contents from TOP to BOTTOM.
     * Used for writing deckX_output.txt.
     */
    public List<Card> getContents() {
        lock.lock();
        try {
            return new ArrayList<>(cards);
        } finally {
            lock.unlock();
        }
    }

    /** Current number of cards in the deck. */
    public int size() {
        lock.lock();
        try {
            return cards.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Exposes the lock to callers that need to coordinate atomic
     * cross-deck operations (e.g., Player performing draw+discard).
     * <p>
     * NOTE: Callers MUST follow a global lock ordering (such as locking
     * the lower-id deck first) to prevent deadlocks.
     */
    public ReentrantLock getLock() {
        return lock;
    }
}

