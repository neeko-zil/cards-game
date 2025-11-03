package cardgame;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Card class.
 */
public class CardTest {

    @Test
    void createsCard_withPositiveDenomination() {
        Card card = new Card(5);
        assertEquals(5, card.getDenomination());
    }

    @Test
    void createsCard_withZeroDenomination() {
        Card card = new Card(0);
        assertEquals(0, card.getDenomination());
    }

    @Test
    void negativeDenomination_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Card(-1));
    }

    @Test
    void toString_returnsTheNumber() {
        assertEquals("7", new Card(7).toString());
        assertEquals("0", new Card(0).toString());
    }

    @Test
    void equals_reflexiveSymmetricTransitive() {
        Card a = new Card(3);
        Card b = new Card(3);
        Card c = new Card(3);
        Card d = new Card(4);

        // reflexive
        assertEquals(a, a);

        // symmetric
        assertEquals(a, b);
        assertEquals(b, a);

        // transitive
        assertEquals(a, b);
        assertEquals(b, c);
        assertEquals(a, c);

        // not equal to different denom
        assertNotEquals(a, d);

        // not equal to null / different type
        assertNotEquals(a, null);
        assertNotEquals(a, "3");
    }

    @Test
    void hashCode_equalCardsHaveSameHash() {
        Card a = new Card(9);
        Card b = new Card(9);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void immutability_denominationsDoNotChange() {
        Card card = new Card(11);
        int before = card.getDenomination();
        // nothing we can call will change it; just assert it’s stable on repeated calls
        assertEquals(before, card.getDenomination());
        assertEquals(11, card.getDenomination());
    }
}
