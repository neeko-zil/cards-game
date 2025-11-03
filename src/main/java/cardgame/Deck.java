package cardgame;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
A deck of cards that works with multiple threads.

A deck is an ordered, thread-safe container (FIFO) holding card
denominations from the shared 8n pack. Players draw from the top (front) and discard
to the bottom (back) of decks as specified by the ring topology (player i draws from deck i
and discards to deck i+1). Draw and discard together are treated as one atomic action. At
game end, each player holds exactly four cards, and each deck’s final contents are
written to deckX_output.txt (deck sizes may vary)

 */
public class Deck {

    private final int id;
    private final Deque<Card> cards = new ArrayDeque<>();
    private final ReentrantLock lock = new ReentrantLock();

    public Deck(int id) {
        this.id = id;
    }

    // Deck identifier (1 to n).
    public int getId() {
        return id;
    }

    /**
     * Draws a card from the top of the deck.
     * Return the card drawn, or null if the deck is empty.
     */
    public Card drawTop() {
        lock.lock();
        try {
            return cards.pollFirst();
        } finally {
            lock.unlock();
        }
    }

    //Discards a card to the bottom of the deck.
    public void discardBottom(Card card) {
        Objects.requireNonNull(card, "card");
        lock.lock();
        try {
            cards.addLast(card);
        } finally {
            lock.unlock();
        }
    }

    // Return all cards in deck
    public List<Card> getContents() {
        lock.lock();
        try {
            return new ArrayList<>(cards);
        } finally {
            lock.unlock();
        }
    }

    // Current number of cards in the deck
    public int size() {
        lock.lock();
        try {
            return cards.size();
        } finally {
            lock.unlock();
        }
    }

    public ReentrantLock getLock() {
        return lock;
    }
}
