package cardgame;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Deck class.
 */
public class DeckTest {
    
    @Test
    public void testDeckCreation() {
        Deck deck = new Deck(1);
        assertEquals(1, deck.getId());
        assertEquals(0, deck.size());
    }
    
    @Test
    public void testAddCard() {
        Deck deck = new Deck(1);
        Card card = new Card(5);
        deck.add(card);
        assertEquals(1, deck.size());
    }
    
    @Test
    public void testFIFOBehavior() {
        Deck deck = new Deck(1);
        Card card1 = new Card(1);
        Card card2 = new Card(2);
        Card card3 = new Card(3);
        
        deck.add(card1);
        deck.add(card2);
        deck.add(card3);
        
        assertEquals(3, deck.size());
        
        Card drawn1 = deck.draw();
        assertEquals(1, drawn1.getDenomination());
        
        Card drawn2 = deck.draw();
        assertEquals(2, drawn2.getDenomination());
        
        Card drawn3 = deck.draw();
        assertEquals(3, drawn3.getDenomination());
        
        assertEquals(0, deck.size());
    }
    
    @Test
    public void testDrawFromEmptyDeck() {
        Deck deck = new Deck(1);
        Card drawn = deck.draw();
        assertNull(drawn);
    }
    
    @Test
    public void testGetContents() {
        Deck deck = new Deck(1);
        deck.add(new Card(1));
        deck.add(new Card(2));
        deck.add(new Card(3));
        
        assertEquals(3, deck.getContents().size());
        assertEquals(1, deck.getContents().get(0).getDenomination());
        assertEquals(2, deck.getContents().get(1).getDenomination());
        assertEquals(3, deck.getContents().get(2).getDenomination());
    }
}
