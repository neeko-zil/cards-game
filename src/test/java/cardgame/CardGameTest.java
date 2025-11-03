package cardgame;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// Tests for pack validation
public class CardGameTest {

    @TempDir
    Path tmp;

    private Path tempFile;

    @AfterEach
    void cleanup() throws IOException {
        if (tempFile != null) {
            Files.deleteIfExists(tempFile);
        }
    }

    // tests

    // valid pack with 8*n lines
    @Test
    void validPackWithCorrectLines() throws Exception {
        tempFile = tmp.resolve("pack.txt");
        Files.write(tempFile, List.of(
            "1","2","3","4","5","6","7","8",
            "9","10","11","12","13","14","15","16"
        ));
        
        var result = CardGame.validatePack(tempFile, 2);
        
        assertTrue(result.ok());
        assertEquals(16, result.values().size());
    }

    // file doesn't exist
    @Test
    void fileNotFound() {
        Path p = tmp.resolve("does-not-exist.txt");
        var result = CardGame.validatePack(p, 2);
        
        assertFalse(result.ok());
        assertTrue(result.message().contains("file not found"));
    }

    // too few lines
    @Test
    void packWithTooFewLines() throws Exception {
        tempFile = tmp.resolve("pack.txt");
        Files.write(tempFile, List.of(
            "0","0","0","0","0","0","0","0",
            "0","0","0","0","0","0","0"
        ));
        
        var result = CardGame.validatePack(tempFile, 2);
        
        assertFalse(result.ok());
        assertTrue(result.message().contains("15 lines"));
    }

    // too many lines
    @Test
    void packWithTooManyLines() throws Exception {
        tempFile = tmp.resolve("pack.txt");
        Files.write(tempFile, List.of(
            "0","0","0","0","0","0","0","0",
            "0","0","0","0","0","0","0","0","0"
        ));
        
        var result = CardGame.validatePack(tempFile, 2);
        
        assertFalse(result.ok());
        assertTrue(result.message().contains("17 lines"));
    }

    // non-integer token
    @Test
    void packWithNonInteger() throws Exception {
        tempFile = tmp.resolve("pack.txt");
        Files.write(tempFile, List.of("1","2","3","four","5","6","7","8"));
        
        var result = CardGame.validatePack(tempFile, 1);
        
        assertFalse(result.ok());
        assertTrue(result.message().contains("line 4"));
    }

    // negative numbers not allowed
    @Test
    void packWithNegativeNumber() throws Exception {
        tempFile = tmp.resolve("pack.txt");
        Files.write(tempFile, List.of("1","-2","3","4","5","6","7","8"));
        
        var result = CardGame.validatePack(tempFile, 1);
        
        assertFalse(result.ok());
        assertTrue(result.message().contains("line 2"));
    }

    // blank lines not allowed
    @Test
    void packWithBlankLine() throws Exception {
        tempFile = tmp.resolve("pack.txt");
        Files.write(tempFile, List.of("1","2","","4","5","6","7","8"));
        
        var result = CardGame.validatePack(tempFile, 1);
        
        assertFalse(result.ok());
        assertTrue(result.message().contains("line 3"));
    }

    // number too big
    @Test
    void packWithOverflowNumber() throws Exception {
        tempFile = tmp.resolve("pack.txt");
        Files.write(tempFile, List.of("1","2","3","40000000000","5","6","7","8"));
        
        var result = CardGame.validatePack(tempFile, 1);
        
        assertFalse(result.ok());
        assertTrue(result.message().contains("line 4"));
    }

}
