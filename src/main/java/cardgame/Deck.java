package cardgame;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.ReentrantLock;
import java.util.ArrayList;
import java.util.List;

/**
 * Thread-safe FIFO deck of cards.
 * Players draw from the front and discard to the back.
 * Uses ReentrantLock for ordered locking strategy to prevent deadlock.
 */
public class Deck {
    private final int id;
    private final LinkedBlockingDeque<Card> cards;
    private final ReentrantLock lock;

    public Deck(int id) {
        this.id = id;
        this.cards = new LinkedBlockingDeque<>();
        this.lock = new ReentrantLock();
    }

    public int getId() {
        return id;
    }

    /**
     * Draws a card from the front of the deck (FIFO).
     * Returns null if deck is empty.
     */
    public Card draw() {
        return cards.pollFirst();
    }

    /**
     * Adds a card to the back of the deck.
     */
    public void add(Card card) {
        cards.addLast(card);
    }

    /**
     * Returns the lock for this deck (used for ordered locking strategy).
     */
    public ReentrantLock getLock() {
        return lock;
    }

    /**
     * Returns the current contents of the deck as a list.
     */
    public List<Card> getContents() {
        return new ArrayList<>(cards);
    }

    /**
     * Returns the number of cards in the deck.
     */
    public int size() {
        return cards.size();
    }
}
