package cardgame;

/**
 * Represents a playing card with a denomination (non-negative integer).
 * This class is immutable for thread-safety.
 */
public class Card {
    private final int denomination;

    public Card(int denomination) {
        if (denomination < 0) {
            throw new IllegalArgumentException("Card denomination cannot be negative");
        }
        this.denomination = denomination;
    }

    public int getDenomination() {
        return denomination;
    }

    @Override
    public String toString() {
        return String.valueOf(denomination);
    }
}
