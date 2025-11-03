package cardgame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

// Tests for Card class
public class CardTest {

    // check card stores value
    @Test
    void createCardWithValue() {
        Card card = new Card(5);
        assertEquals(5, card.getDenomination());
    }

    // zero should work
    @Test
    void createCardWithZero() {
        Card card = new Card(0);
        assertEquals(0, card.getDenomination());
    }

    // negative values should throw error
    @Test
    void negativeValueThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Card(-1));
    }

    // toString should show the value
    @Test
    void toStringShowsValue() {
        Card card = new Card(7);
        assertEquals("7", card.toString());
    }

    // value shouldn't change
    @Test
    void valueNeverChanges() {
        Card card = new Card(5);
        assertEquals(5, card.getDenomination());
        assertEquals(5, card.getDenomination());
    }
}
