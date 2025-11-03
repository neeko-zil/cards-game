package cardgame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

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
        deck.discardBottom(card);
        assertEquals(1, deck.size());
    }
    
    @Test
    public void testFIFOBehavior() {
        Deck deck = new Deck(1);
        Card card1 = new Card(1);
        Card card2 = new Card(2);
        Card card3 = new Card(3);
        
        deck.discardBottom(card1);
        deck.discardBottom(card2);
        deck.discardBottom(card3);
        
        assertEquals(3, deck.size());
        
        Card drawn1 = deck.drawTop();
        assertEquals(1, drawn1.getDenomination());
        
        Card drawn2 = deck.drawTop();
        assertEquals(2, drawn2.getDenomination());
        
        Card drawn3 = deck.drawTop();
        assertEquals(3, drawn3.getDenomination());
        
        assertEquals(0, deck.size());
    }
    
    @Test
    public void testDrawFromEmptyDeck() {
        Deck deck = new Deck(1);
        Card drawn = deck.drawTop();
        assertNull(drawn);
    }
    
    @Test
    public void testGetContents() {
        Deck deck = new Deck(1);
        deck.discardBottom(new Card(1));
        deck.discardBottom(new Card(2));
        deck.discardBottom(new Card(3));
        
        assertEquals(3, deck.getContents().size());
        assertEquals(1, deck.getContents().get(0).getDenomination());
        assertEquals(2, deck.getContents().get(1).getDenomination());
        assertEquals(3, deck.getContents().get(2).getDenomination());
    }
}
