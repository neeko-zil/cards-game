package cardgame;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a player in the card game.
 * Each player runs in its own thread and attempts to collect four cards of the same value.
 */
public class Player implements Runnable {
    private final int id;
    private final int preferredValue;
    private final List<Card> hand;
    private final Deck leftDeck;
    private final Deck rightDeck;
    private final AtomicBoolean gameWon;
    private final List<Player> allPlayers;
    private final Random random;
    private BufferedWriter writer;
    private volatile int winnerId = -1;

    public Player(int id, Deck leftDeck, Deck rightDeck, AtomicBoolean gameWon, List<Player> allPlayers) {
        this.id = id;
        this.preferredValue = id;
        this.hand = new ArrayList<>();
        this.leftDeck = leftDeck;
        this.rightDeck = rightDeck;
        this.gameWon = gameWon;
        this.allPlayers = allPlayers;
        this.random = new Random();
    }

    public int getId() {
        return id;
    }

    public void addCardToHand(Card card) {
        hand.add(card);
    }

    public List<Card> getHand() {
        return new ArrayList<>(hand);
    }

    /**
     * Checks if the player has won (all 4 cards same value).
     */
    private boolean hasWon() {
        if (hand.size() != 4) return false;
        int value = hand.get(0).getDenomination();
        for (Card card : hand) {
            if (card.getDenomination() != value) {
                return false;
            }
        }
        return true;
    }

    /**
     * Writes a message to the player's output file.
     */
    private void writeToFile(String message) {
        try {
            writer.write(message);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.err.println("Error writing to player " + id + " output file: " + e.getMessage());
        }
    }

    /**
     * Chooses a card to discard (prefers non-preferred cards).
     */
    private Card chooseCardToDiscard() {
        List<Card> nonPreferred = new ArrayList<>();
        for (Card card : hand) {
            if (card.getDenomination() != preferredValue) {
                nonPreferred.add(card);
            }
        }
        
        if (!nonPreferred.isEmpty()) {
            // Randomly choose from non-preferred cards
            return nonPreferred.get(random.nextInt(nonPreferred.size()));
        } else {
            // All cards are preferred, discard the first one
            return hand.get(0);
        }
    }

    /**
     * Notifies this player that another player has won.
     */
    public synchronized void notifyWinner(int winnerPlayerId) {
        this.winnerId = winnerPlayerId;
    }

    @Override
    public void run() {
        try {
            writer = new BufferedWriter(new FileWriter("player" + id + "_output.txt"));
            
            // Write initial hand
            StringBuilder initialHand = new StringBuilder("player " + id + " initial hand");
            for (Card card : hand) {
                initialHand.append(" ").append(card.getDenomination());
            }
            writeToFile(initialHand.toString());

            // Check for immediate win
            if (hasWon()) {
                gameWon.set(true);
                System.out.println("player " + id + " wins");
                writeToFile("player " + id + " wins");
                writeToFile("player " + id + " exits");
                writeToFile("player " + id + " final hand: " + handToString());
                notifyOtherPlayers();
                return;
            }

            // Main game loop
            while (!gameWon.get()) {
                // Atomic draw and discard operation using ordered locking
                performTurn();

                // Check if won after this turn
                if (hasWon() && gameWon.compareAndSet(false, true)) {
                    System.out.println("player " + id + " wins");
                    writeToFile("player " + id + " wins");
                    writeToFile("player " + id + " exits");
                    writeToFile("player " + id + " final hand: " + handToString());
                    notifyOtherPlayers();
                    return;
                }

                // Small delay to prevent busy waiting
                Thread.sleep(10);
            }

            // Another player won
            if (winnerId != -1) {
                writeToFile("player " + winnerId + " has informed player " + id + " that player " + winnerId + " has won");
            }
            writeToFile("player " + id + " exits");
            writeToFile("player " + id + " hand: " + handToString());

        } catch (IOException e) {
            System.err.println("Error creating output file for player " + id + ": " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing file for player " + id);
            }
        }
    }

    /**
     * Performs one turn: draw from left deck, discard to right deck.
     * Uses ordered locking to prevent deadlock.
     */
    private void performTurn() {
        // Ordered locking: lock lower ID deck first
        Deck firstLock = (leftDeck.getId() < rightDeck.getId()) ? leftDeck : rightDeck;
        Deck secondLock = (leftDeck.getId() < rightDeck.getId()) ? rightDeck : leftDeck;

        firstLock.getLock().lock();
        try {
            secondLock.getLock().lock();
            try {
                // Draw from left deck
                Card drawnCard = leftDeck.draw();
                if (drawnCard == null) {
                    return; // Deck is empty, skip this turn
                }

                hand.add(drawnCard);
                writeToFile("player " + id + " draws a " + drawnCard.getDenomination() + " from deck " + leftDeck.getId());

                // Choose and discard a card to right deck
                Card cardToDiscard = chooseCardToDiscard();
                hand.remove(cardToDiscard);
                rightDeck.add(cardToDiscard);
                writeToFile("player " + id + " discards a " + cardToDiscard.getDenomination() + " to deck " + rightDeck.getId());

                // Write current hand
                writeToFile("player " + id + " current hand is " + handToString());

            } finally {
                secondLock.getLock().unlock();
            }
        } finally {
            firstLock.getLock().unlock();
        }
    }

    /**
     * Notifies all other players that this player has won.
     */
    private void notifyOtherPlayers() {
        for (Player player : allPlayers) {
            if (player.getId() != this.id) {
                player.notifyWinner(this.id);
            }
        }
    }

    /**
     * Returns hand as a space-separated string of denominations.
     */
    private String handToString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hand.size(); i++) {
            if (i > 0) sb.append(" ");
            sb.append(hand.get(i).getDenomination());
        }
        return sb.toString();
    }
}
