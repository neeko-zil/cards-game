package cardgame;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main class for the multi-threaded card game simulation.
 * Manages game initialization, card distribution, and player threads.
 */
public class CardGame {
    
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        int n;
        List<Integer> pack;
        Path packPath;

        // 1️⃣ prompt for n
        while (true) {
            System.out.println("Please enter the number of players:");
            String s = sc.nextLine().trim();
            try {
                n = Integer.parseInt(s);
                if (n >= 1) break;
                System.out.println("Error: number of players must be a positive integer (>=1).");
            } catch (NumberFormatException e) {
                System.out.println("Error: please enter a valid integer for number of players.");
            }
        }

        // 2️⃣ prompt for pack file
        while (true) {
            System.out.println("Please enter the location of the pack file:");
            String p = sc.nextLine().trim();
            packPath = Paths.get(p);
            ValidationResult vr = validatePack(packPath, n);
            if (vr.ok()) {
                pack = vr.values();
                break;
            }
            System.out.println("Invalid pack: " + vr.message());
        }

        // 3️⃣ if we got here, both inputs are valid — continue setup
        System.out.println("Starting game with " + n + " players...");
        // TODO: deal cards, create decks, start threads, etc.
    }

    // 4️⃣ validator helper method
    static final Pattern DIGITS = Pattern.compile("^\\d+$");

    static ValidationResult validatePack(Path path, int n) {
        // (Insert the detailed validatePack() method from the earlier message here)
    }

    static final class ValidationResult {
        // (Insert the nested ValidationResult class here)
    }
}
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        
        try {
            int n = getNumberOfPlayers(consoleReader);
            List<Card> pack = getValidPack(consoleReader, n);
            
            // Create players and decks
            List<Player> players = new ArrayList<>();
            List<Deck> decks = new ArrayList<>();
            AtomicBoolean gameWon = new AtomicBoolean(false);
            
            // Create n decks
            for (int i = 1; i <= n; i++) {
                decks.add(new Deck(i));
            }
            
            // Create n players (with ring topology)
            for (int i = 1; i <= n; i++) {
                Deck leftDeck = decks.get(i - 1);
                Deck rightDeck = decks.get(i % n);
                Player player = new Player(i, leftDeck, rightDeck, gameWon, players);
                players.add(player);
            }
            
            // Distribute cards: first 4n cards go to players in round robin
            int cardIndex = 0;
            for (int round = 0; round < 4; round++) {
                for (Player player : players) {
                    player.addCardToHand(pack.get(cardIndex++));
                }
            }
            
            // Remaining 4n cards go to decks in round robin
            while (cardIndex < pack.size()) {
                for (Deck deck : decks) {
                    if (cardIndex < pack.size()) {
                        deck.add(pack.get(cardIndex++));
                    }
                }
            }
            
            // Start all player threads
            List<Thread> threads = new ArrayList<>();
            for (Player player : players) {
                Thread thread = new Thread(player);
                threads.add(thread);
                thread.start();
            }
            
            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join();
            }
            
            // Write deck output files
            for (Deck deck : decks) {
                writeDeckOutput(deck);
            }
            
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Game interrupted");
            Thread.currentThread().interrupt();
        } finally {
            try {
                consoleReader.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
    
    /**
     * Prompts for and validates the number of players.
     */
    private static int getNumberOfPlayers(BufferedReader reader) throws IOException {
        while (true) {
            System.out.print("Please enter the number of players: ");
            String input = reader.readLine();
            
            if (input == null) {
                System.exit(0);
            }
            
            try {
                int n = Integer.parseInt(input.trim());
                if (n > 0) {
                    return n;
                } else {
                    System.out.println("Number of players must be positive.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a positive integer.");
            }
        }
    }
    
    /**
     * Prompts for and validates the pack file.
     */
    private static List<Card> getValidPack(BufferedReader reader, int n) throws IOException {
        while (true) {
            System.out.print("Please enter location of pack to load: ");
            String filename = reader.readLine();
            
            if (filename == null) {
                System.exit(0);
            }
            
            filename = filename.trim();
            
            try {
                List<Card> pack = readPack(filename, n);
                if (pack != null) {
                    return pack;
                }
            } catch (IOException e) {
                System.out.println("Error reading file: " + e.getMessage());
            }
        }
    }
    
    /**
     * Reads and validates a pack file.
     * Returns null if invalid, otherwise returns the list of cards.
     */
    private static List<Card> readPack(String filename, int n) throws IOException {
        List<Card> cards = new ArrayList<>();
        
        try (BufferedReader fileReader = new BufferedReader(new FileReader(filename))) {
            String line;
            int lineNumber = 0;
            
            while ((line = fileReader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                
                if (line.isEmpty()) {
                    continue; // Skip empty lines
                }
                
                try {
                    int value = Integer.parseInt(line);
                    if (value < 0) {
                        System.out.println("Invalid pack: card values must be non-negative (line " + lineNumber + ")");
                        return null;
                    }
                    cards.add(new Card(value));
                } catch (NumberFormatException e) {
                    System.out.println("Invalid pack: line " + lineNumber + " is not a valid integer");
                    return null;
                }
            }
        }
        
        int expectedSize = 8 * n;
        if (cards.size() != expectedSize) {
            System.out.println("Invalid pack: expected " + expectedSize + " cards, but found " + cards.size());
            return null;
        }
        
        return cards;
    }
    
    /**
     * Writes the final contents of a deck to its output file.
     */
    private static void writeDeckOutput(Deck deck) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("deck" + deck.getId() + "_output.txt"))) {
            StringBuilder contents = new StringBuilder("deck " + deck.getId() + " contents:");
            for (Card card : deck.getContents()) {
                contents.append(" ").append(card.getDenomination());
            }
            writer.write(contents.toString());
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error writing deck " + deck.getId() + " output file: " + e.getMessage());
        }
    }
}
