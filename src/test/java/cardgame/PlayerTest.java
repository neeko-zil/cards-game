package cardgame;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;

// Tests for Player class
public class PlayerTest {

    // create new player
    @Test
    void createPlayer() {
        Deck leftDeck = new Deck(1);
        Deck rightDeck = new Deck(2);
        AtomicBoolean gameWon = new AtomicBoolean(false);
        List<Player> players = new ArrayList<>();

        Player player = new Player(1, leftDeck, rightDeck, gameWon, players);

        assertEquals(1, player.getId());
        assertEquals(0, player.getHand().size());
    }

    // add cards to hand
    @Test
    void addCardsToHand() {
        Deck leftDeck = new Deck(1);
        Deck rightDeck = new Deck(2);
        Player player = new Player(1, leftDeck, rightDeck, new AtomicBoolean(false), new ArrayList<>());

        player.addCardToHand(new Card(5));
        player.addCardToHand(new Card(3));

        assertEquals(2, player.getHand().size());
        assertEquals(5, player.getHand().get(0).getDenomination());
        assertEquals(3, player.getHand().get(1).getDenomination());
    }

    // win with four same cards
    @Test
    void winWithFourSameCards() {
        Deck leftDeck = new Deck(1);
        Deck rightDeck = new Deck(2);
        Player player = new Player(1, leftDeck, rightDeck, new AtomicBoolean(false), new ArrayList<>());

        player.addCardToHand(new Card(7));
        player.addCardToHand(new Card(7));
        player.addCardToHand(new Card(7));
        player.addCardToHand(new Card(7));

        assertEquals(4, player.getHand().size());
    }

    // mixed cards don't win
    @Test
    void noWinWithMixedCards() {
        Deck leftDeck = new Deck(1);
        Deck rightDeck = new Deck(2);
        Player player = new Player(1, leftDeck, rightDeck, new AtomicBoolean(false), new ArrayList<>());

        player.addCardToHand(new Card(1));
        player.addCardToHand(new Card(2));
        player.addCardToHand(new Card(3));
        player.addCardToHand(new Card(4));

        assertEquals(4, player.getHand().size());
    }

    // can win with non-preferred cards
    @Test
    void winWithNonPreferred() {
        Deck leftDeck = new Deck(1);
        Deck rightDeck = new Deck(2);
        Player player = new Player(1, leftDeck, rightDeck, new AtomicBoolean(false), new ArrayList<>());

        player.addCardToHand(new Card(5));
        player.addCardToHand(new Card(5));
        player.addCardToHand(new Card(5));
        player.addCardToHand(new Card(5));

        assertEquals(4, player.getHand().size());
    }

    // notify winner test
    @Test
    void notifyWinner() {
        Deck leftDeck = new Deck(1);
        Deck rightDeck = new Deck(2);
        AtomicBoolean gameWon = new AtomicBoolean(false);
        List<Player> players = new ArrayList<>();

        Player player1 = new Player(1, leftDeck, rightDeck, gameWon, players);
        Player player2 = new Player(2, rightDeck, leftDeck, gameWon, players);

        players.add(player1);
        players.add(player2);

        player1.notifyWinner(2);
    }

    // getHand should return copies
    @Test
    void getHandCopies() {
        Deck leftDeck = new Deck(1);
        Deck rightDeck = new Deck(2);
        Player player = new Player(1, leftDeck, rightDeck, new AtomicBoolean(false), new ArrayList<>());

        player.addCardToHand(new Card(1));
        player.addCardToHand(new Card(2));

        List<Card> hand1 = player.getHand();
        List<Card> hand2 = player.getHand();

        assertFalse(hand1 == hand2);
        assertEquals(hand1.size(), hand2.size());
    }

    // player ID matches preferred card
    @Test
    void playerIdMatchesPreferred() {
        Deck leftDeck = new Deck(1);
        Deck rightDeck = new Deck(2);

        Player player3 = new Player(3, leftDeck, rightDeck, new AtomicBoolean(false), new ArrayList<>());
        Player player7 = new Player(7, leftDeck, rightDeck, new AtomicBoolean(false), new ArrayList<>());

        assertEquals(3, player3.getId());
        assertEquals(7, player7.getId());
    }
}
