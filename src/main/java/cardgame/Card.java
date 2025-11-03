package cardgame;

/**
 * Represents a playing card with a denomination (non-negative integer).
 * Immutable and thread-safe.
 */
public final class Card {

    private final int denomination;

    /**
     * Creates a card with the given denomination.
     * @param denomination a non-negative integer
     * @throws IllegalArgumentException if denomination is negative
     */
    public Card(int denomination) {
        if (denomination < 0) {
            throw new IllegalArgumentException("Card denomination cannot be negative");
        }
        this.denomination = denomination;
    }

    /** Returns the denomination (value) of this card. */
    public int getDenomination() {
        return denomination;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Card)) return false;
        Card other = (Card) o;
        return denomination == other.denomination;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(denomination);
    }

    @Override
    public String toString() {
        return String.valueOf(denomination);
    }
}
