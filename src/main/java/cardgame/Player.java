package cardgame;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Player thread:
 * - Holds a 4-card hand.
 * - On each turn: atomically draw from left deck, discard to right deck.
 * - Prefers its own id as card value; discards non-preferred when possible.
 * - Announces a win when all 4 cards have the same value.
 * - Writes exact-format logs to playerX_output.txt.
 *
 * NOTE: Atomicity across two decks is achieved in Player by locking the two
 * Deck locks in a consistent global order (lower-id deck first), to avoid deadlock.
 */
public class Player implements Runnable {

    // Global winner id so non-winners can always print the "informed" line,
    // even if their local notification arrives late.
    private static volatile int GLOBAL_WINNER_ID = -1;

    private final int id;
    private final int preferredValue;
    private final List<Card> hand = new ArrayList<>(4);
    private final Deck leftDeck;
    private final Deck rightDeck;
    private final AtomicBoolean gameWon;
    private final List<Player> allPlayers;
    private final Random random = new Random();

    private BufferedWriter writer;
    private volatile int winnerId = -1; // set by notifyWinner(..)

    public Player(int id, Deck leftDeck, Deck rightDeck, AtomicBoolean gameWon, List<Player> allPlayers) {
        this.id = id;
        this.preferredValue = id;
        this.leftDeck = leftDeck;
        this.rightDeck = rightDeck;
        this.gameWon = gameWon;
        this.allPlayers = allPlayers;
    }

    public int getId() {
        return id;
    }

    /** Used by CardGame during dealing. */
    public void addCardToHand(Card card) {
        hand.add(card);
    }

    /** Defensive copy for tests if needed. */
    public List<Card> getHand() {
        return new ArrayList<>(hand);
    }

    /** Four of a kind? */
    private boolean hasWon() {
        if (hand.size() != 4) return false;
        int v = hand.get(0).getDenomination();
        for (int i = 1; i < 4; i++) {
            if (hand.get(i).getDenomination() != v) return false;
        }
        return true;
    }

    /** Logging helper (swallows IO errors but keeps the game running). */
    private void writeToFile(String line) {
        try {
            writer.write(line);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.err.println("Error writing player " + id + " log: " + e.getMessage());
        }
    }

    /**
     * Choose a discard card:
     * - Prefer discarding any non-preferred value (to avoid hoarding).
     * - If all four are preferred (rare), discard the first.
     */
    private Card chooseCardToDiscard() {
        List<Card> nonPreferred = new ArrayList<>(4);
        for (Card c : hand) {
            if (c.getDenomination() != preferredValue) {
                nonPreferred.add(c);
            }
        }
        if (!nonPreferred.isEmpty()) {
            return nonPreferred.get(random.nextInt(nonPreferred.size()));
        }
        // all preferred — discard first to maintain progress
        return hand.get(0);
    }

    /** Called by the winner to let others know who won. */
    public synchronized void notifyWinner(int winnerPlayerId) {
        this.winnerId = winnerPlayerId;
    }

    @Override
    public void run() {
        try {
            writer = new BufferedWriter(new FileWriter("player" + id + "_output.txt"));

            // Initial hand line
            writeToFile("player " + id + " initial hand " + handToString());

            // Immediate win check (before any draw)
            if (hasWon()) {
                GLOBAL_WINNER_ID = id;
                gameWon.set(true);
                System.out.println("player " + id + " wins");
                writeToFile("player " + id + " wins");
                writeToFile("player " + id + " exits");
                writeToFile("player " + id + " final hand: " + handToString());
                notifyOtherPlayers();
                return;
            }

            // Main loop
            while (!gameWon.get()) {
                performTurn(); // atomic draw+discard (or skip if left deck empty)

                // Win after this turn?
                if (hasWon() && gameWon.compareAndSet(false, true)) {
                    GLOBAL_WINNER_ID = id;
                    System.out.println("player " + id + " wins");
                    writeToFile("player " + id + " wins");
                    writeToFile("player " + id + " exits");
                    writeToFile("player " + id + " final hand: " + handToString());
                    notifyOtherPlayers();
                    return;
                }

                // Keep things polite, avoid hot spinning
                try {
                    Thread.sleep(5);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // Game ended by someone else winning
            int w = (winnerId != -1) ? winnerId : GLOBAL_WINNER_ID;
            if (w != -1 && w != id) {
                writeToFile("player " + w + " has informed player " + id + " that player " + w + " has won");
            }
            writeToFile("player " + id + " exits");
            writeToFile("player " + id + " hand: " + handToString());

        } catch (IOException e) {
            System.err.println("Error creating player " + id + " output file: " + e.getMessage());
        } finally {
            if (writer != null) {
                try { writer.close(); } catch (IOException ignored) {}
            }
        }
    }

    /**
     * One atomic turn:
     * - Lock both decks in global id order.
     * - Draw from left; if empty, skip turn.
     * - Discard to right.
     * - Log draw, discard, and current hand.
     *
     * Hand remains at 4 cards throughout (draw then discard inside one critical section).
     */
    private void performTurn() {
        // Determine lock order (lower id first)
        Deck firstLock = (leftDeck.getId() < rightDeck.getId()) ? leftDeck : rightDeck;
        Deck secondLock = (firstLock == leftDeck) ? rightDeck : leftDeck;

        firstLock.getLock().lock();
        try {
            secondLock.getLock().lock();
            try {
                // Draw from left deck
                Card drawn = leftDeck.drawTop();
                if (drawn == null) {
                    return; // left deck empty; skip this turn
                }

                hand.add(drawn);
                writeToFile("player " + id + " draws a " + drawn.getDenomination()
                        + " from deck " + leftDeck.getId());

                // Choose discard to right deck
                Card toDiscard = chooseCardToDiscard();
                hand.remove(toDiscard);
                rightDeck.discardBottom(toDiscard);
                writeToFile("player " + id + " discards a " + toDiscard.getDenomination()
                        + " to deck " + rightDeck.getId());

                // Log current hand
                writeToFile("player " + id + " current hand is " + handToString());

            } finally {
                secondLock.getLock().unlock();
            }
        } finally {
            firstLock.getLock().unlock();
        }
    }

    /** Notify all other players of the winner (for their file logs). */
    private void notifyOtherPlayers() {
        for (Player p : allPlayers) {
            if (p.getId() != this.id) {
                p.notifyWinner(this.id);
            }
        }
    }

    /** Space-separated denominations, e.g., "1 1 2 4". */
    private String handToString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hand.size(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(hand.get(i).getDenomination());
        }
        return sb.toString();
    }
}
