package cardgame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/**
 * Tests for Card class - verifies cards work correctly.
 */
public class CardTest {

    @Test
    void createCardWithValue() {
        Card card = new Card(5);
        assertEquals(5, card.getDenomination());
    }

    @Test
    void createCardWithZero() {
        Card card = new Card(0);
        assertEquals(0, card.getDenomination());
    }

    @Test
    void negativeValueThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Card(-1));
    }

    @Test
    void toStringShowsValue() {
        Card card = new Card(7);
        assertEquals("7", card.toString());
    }

    @Test
    void equalCardsHaveSameValue() {
        Card card1 = new Card(3);
        Card card2 = new Card(3);
        assertEquals(card1, card2);
    }

    @Test
    void equalCardsHaveSameHash() {
        Card card1 = new Card(3);
        Card card2 = new Card(3);
        assertEquals(card1.hashCode(), card2.hashCode());
    }

    @Test
    void valueNeverChanges() {
        Card card = new Card(5);
        assertEquals(5, card.getDenomination());
        assertEquals(5, card.getDenomination());
    }
}
