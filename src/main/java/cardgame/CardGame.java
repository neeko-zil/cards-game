package cardgame;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * Main entry point for the multi-threaded card game.
 * - Prompts for inputs (n, pack file)
 * - Validates inputs (does not start until valid)
 * - Distributes cards
 * - Starts player threads
 * - Writes deck outputs on completion
 */
public class CardGame {

    // -------------------- MAIN --------------------
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        try {
            // 1) Prompt for number of players (must be >= 1)
            int n = promptForNumberOfPlayers(sc);

            // 2) Prompt for pack file (must be exactly 8n non-negative integers, one per line)
            List<Integer> packInts = promptForPack(sc, n);

            // 3) Convert integers to Card objects
            List<Card> pack = new ArrayList<>(packInts.size());
            for (int v : packInts) {
                pack.add(new Card(v));
            }

            // 4) Set up players and decks
            System.out.println("Starting game with " + n + " players...");
            List<Player> players = new ArrayList<>(n);
            List<Deck> decks = new ArrayList<>(n);
            AtomicBoolean gameWon = new AtomicBoolean(false);

            // Create n decks (1..n)
            for (int i = 1; i <= n; i++) {
                decks.add(new Deck(i));
            }

            // Create n players with ring topology:
            // player i draws from deck i (left) and discards to deck i+1 (right; wrap to 1 after n)
            for (int i = 1; i <= n; i++) {
                Deck leftDeck = decks.get(i - 1);         // deck i
                Deck rightDeck = decks.get(i % n);        // deck i+1 (wrap)
                Player player = new Player(i, leftDeck, rightDeck, gameWon, players);
                players.add(player);
            }

            // 5) Distribute cards:
            // - First 4n cards to players in round-robin (4 cards per player)
            int cardIndex = 0;
            for (int round = 0; round < 4; round++) {
                for (Player player : players) {
                    player.addCardToHand(pack.get(cardIndex++));
                }
            }

            // - Remaining 4n cards to decks in round-robin order
            while (cardIndex < pack.size()) {
                for (Deck deck : decks) {
                    if (cardIndex < pack.size()) {
                        deck.discardBottom(pack.get(cardIndex++));
                    }
                }
            }

            // 6) Start all player threads
            List<Thread> threads = new ArrayList<>(n);
            for (Player player : players) {
                Thread t = new Thread(player);
                threads.add(t);
                t.start();
            }

            // 7) Wait for all players to finish
            for (Thread t : threads) {
                try {
                    t.join();
                } catch (InterruptedException ie) {
                    // Preserve interrupt status and continue shutdown
                    Thread.currentThread().interrupt();
                    System.err.println("Game interrupted");
                }
            }

            // 8) Write deck output files
            for (Deck deck : decks) {
                writeDeckOutput(deck);
            }
        } finally {
            sc.close();
        }
    }

    // -------------------- INPUT PROMPTS --------------------

    private static int promptForNumberOfPlayers(Scanner sc) {
        int n;
        while (true) {
            System.out.println("Please enter the number of players:");
            String s = sc.nextLine().trim();
            try {
                n = Integer.parseInt(s);
                if (n >= 1) {
                    return n;
                }
                System.out.println("Error: number of players must be a positive integer (>=1).");
            } catch (NumberFormatException e) {
                System.out.println("Error: please enter a valid integer for number of players.");
            }
        }
    }

    private static List<Integer> promptForPack(Scanner sc, int n) {
        while (true) {
            System.out.println("Please enter the location of the pack file:");
            String p = sc.nextLine().trim();
            Path packPath = Paths.get(p);
            ValidationResult vr = validatePack(packPath, n);
            if (vr.ok()) {
                return vr.values();
            }
            System.out.println("Invalid pack: " + vr.message());
        }
    }

    // -------------------- PACK VALIDATION --------------------

    private static final Pattern DIGITS = Pattern.compile("^\\d+$");

    /**
     * Validates that the pack file:
     * - exists, is a regular readable file
     * - contains exactly 8*n lines
     * - each line is a single non-negative integer (digits only)
     * - handles UTF-8 and BOM on the first line
     */
    static ValidationResult validatePack(Path path, int n) {
        // 1) Path checks
        try {
            if (!Files.exists(path)) return ValidationResult.err("file not found: " + path);
            if (!Files.isRegularFile(path)) return ValidationResult.err("not a regular file: " + path);
            if (!Files.isReadable(path)) return ValidationResult.err("file not readable: " + path);
        } catch (SecurityException se) {
            return ValidationResult.err("cannot access file: " + se.getMessage());
        }

        // 2) Read all lines (UTF-8)
        final List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return ValidationResult.err("failed to read file: " + e.getMessage());
        }
        if (lines.isEmpty()) {
            return ValidationResult.err("file is empty; expected " + (8 * n) + " lines.");
        }

        // 3) Parse and validate: exactly one non-negative integer per line
        final List<Integer> out = new ArrayList<>(8 * n);
        int idx = 0;
        for (String raw : lines) {
            idx++;
            String s = raw == null ? "" : raw.trim();
            if (s.isEmpty()) {
                return ValidationResult.err("line " + idx + " is blank; expected a non-negative integer.");
            }
            // Remove BOM on very first line if present
            if (idx == 1 && !s.isEmpty() && s.charAt(0) == '\uFEFF') {
                s = s.substring(1).trim();
                if (s.isEmpty()) {
                    return ValidationResult.err("line 1 is blank after BOM; expected a non-negative integer.");
                }
            }
            if (!DIGITS.matcher(s).matches()) {
                return ValidationResult.err("line " + idx + " is not a non-negative integer: '" + raw + "'");
            }
            long v;
            try {
                v = Long.parseLong(s);
            } catch (NumberFormatException nfe) {
                return ValidationResult.err("line " + idx + " value too large: '" + s + "'");
            }
            if (v > Integer.MAX_VALUE) {
                return ValidationResult.err("line " + idx + " exceeds max int: " + v);
            }
            out.add((int) v);
        }

        // 4) Exactly 8n values required
        int expected = 8 * n;
        if (out.size() != expected) {
            return ValidationResult.err("pack has " + out.size() + " lines; expected " + expected + " (8 × " + n + ").");
        }

        return ValidationResult.ok(Collections.unmodifiableList(out));
    }

    static final class ValidationResult {
        private final boolean ok;
        private final String message;
        private final List<Integer> values;

        private ValidationResult(boolean ok, String message, List<Integer> values) {
            this.ok = ok;
            this.message = message;
            this.values = values;
        }

        static ValidationResult ok(List<Integer> v) {
            return new ValidationResult(true, "", v);
        }

        static ValidationResult err(String m) {
            return new ValidationResult(false, m, null);
        }

        boolean ok() { return ok; }
        String message() { return message; }
        List<Integer> values() { return values; }
    }

    // -------------------- OUTPUT HELPERS --------------------

    /**
     * Writes the final contents of a deck to its output file
     * in the exact required format: "deckX contents: v1 v2 v3 ..."
     */
    private static void writeDeckOutput(Deck deck) {
        String filename = "deck" + deck.getId() + "_output.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            StringBuilder sb = new StringBuilder();
            sb.append("deck").append(deck.getId()).append(" contents:");
            for (Card c : deck.getContents()) {
                sb.append(" ").append(c.getDenomination());
            }
            writer.write(sb.toString());
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error writing " + filename + ": " + e.getMessage());
        }
    }
}
