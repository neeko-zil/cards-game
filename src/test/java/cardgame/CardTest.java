package cardgame;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Card class.
 */
public class CardTest {
    
    @Test
    public void testCardCreation() {
        Card card = new Card(5);
        assertEquals(5, card.getDenomination());
    }
    
    @Test
    public void testCardWithZeroDenomination() {
        Card card = new Card(0);
        assertEquals(0, card.getDenomination());
    }
    
    @Test
    public void testNegativeDenominationThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Card(-1);
        });
    }
    
    @Test
    public void testCardToString() {
        Card card = new Card(7);
        assertEquals("7", card.toString());
    }
}
