package cardgame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

// Tests for Deck class
public class DeckTest {

    // create new deck
    @Test
    void createDeck() {
        Deck deck = new Deck(1);
        assertEquals(1, deck.getId());
        assertEquals(0, deck.size());
    }

    // add card to deck
    @Test
    void addCard() {
        Deck deck = new Deck(1);
        deck.discardBottom(new Card(5));
        assertEquals(1, deck.size());
    }

    // should draw in FIFO order
    @Test
    void drawInFIFOOrder() {
        Deck deck = new Deck(1);
        deck.discardBottom(new Card(1));
        deck.discardBottom(new Card(2));
        deck.discardBottom(new Card(3));

        assertEquals(1, deck.drawTop().getDenomination());
        assertEquals(2, deck.drawTop().getDenomination());
        assertEquals(3, deck.drawTop().getDenomination());
        assertEquals(0, deck.size());
    }

    // draw from empty deck returns null
    @Test
    void drawFromEmpty() {
        Deck deck = new Deck(1);
        assertNull(deck.drawTop());
    }

    // get contents of deck
    @Test
    void getContents() {
        Deck deck = new Deck(1);
        deck.discardBottom(new Card(1));
        deck.discardBottom(new Card(2));
        deck.discardBottom(new Card(3));

        assertEquals(3, deck.getContents().size());
        assertEquals(1, deck.getContents().get(0).getDenomination());
        assertEquals(2, deck.getContents().get(1).getDenomination());
        assertEquals(3, deck.getContents().get(2).getDenomination());
    }

    // null card should throw error
    @Test
    void nullCardThrows() {
        Deck deck = new Deck(1);
        assertThrows(NullPointerException.class, () -> deck.discardBottom(null));
    }
}
