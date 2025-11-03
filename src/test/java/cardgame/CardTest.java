package cardgame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/**
 * Tests for Card class - verifies cards work correctly.
 */
public class CardTest {

    /**
     * Test that a card stores its value correctly.
     */
    @Test
    void createCardWithValue() {
        Card card = new Card(5);
        assertEquals(5, card.getDenomination());
    }

    /**
     * Test that zero is a valid card value.
     * The spec says non-negative, so zero should work.
     */
    @Test
    void createCardWithZero() {
        Card card = new Card(0);
        assertEquals(0, card.getDenomination());
    }

    /**
     * Test that negative values are rejected.
     * Cards must have non-negative denominations.
     */
    @Test
    void negativeValueThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Card(-1));
    }

    /**
     * Test that toString returns the card value as a string.
     * Used for file output formatting.
     */
    @Test
    void toStringShowsValue() {
        Card card = new Card(7);
        assertEquals("7", card.toString());
    }

    /**
     * Test that two cards with same value are equal.
     */
    @Test
    void equalCardsHaveSameValue() {
        Card card1 = new Card(3);
        Card card2 = new Card(3);
        assertEquals(card1, card2);
    }

    /**
     * Test that equal cards have the same hash code.
     * Required for using cards in hash-based collections.
     */
    @Test
    void equalCardsHaveSameHash() {
        Card card1 = new Card(3);
        Card card2 = new Card(3);
        assertEquals(card1.hashCode(), card2.hashCode());
    }

    /**
     * Test that card values don't change (immutability).
     * Cards should be immutable once created.
     */
    @Test
    void valueNeverChanges() {
        Card card = new Card(5);
        assertEquals(5, card.getDenomination());
        assertEquals(5, card.getDenomination());
    }
}
