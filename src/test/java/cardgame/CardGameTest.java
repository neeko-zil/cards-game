package cardgame;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for CardGame.validatePack(...) and related input handling.
 * These tests assert exact behaviours required by the spec:
 *  - exactly 8*n lines
 *  - each line is a single non-negative integer (digits only)
 *  - helpful error messages
 *  - UTF-8 + BOM tolerance
 */
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

    // ----------------- helpers -----------------

    private Path writeLines(String... lines) throws IOException {
        tempFile = tmp.resolve("pack-" + System.nanoTime() + ".txt");
        Files.write(tempFile, List.of(lines), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return tempFile;
    }

    private static void assertOk(CardGame.ValidationResult vr) {
        assertTrue(vr.ok(), () -> "Expected ok() but got error: " + vr.message());
        assertNotNull(vr.values(), "values() should not be null for ok()");
    }

    private static void assertErr(CardGame.ValidationResult vr, String... mustContain) {
        assertFalse(vr.ok(), "Expected error result");
        assertNotNull(vr.message(), "message() should be present on error");
        for (String s : mustContain) {
            assertTrue(vr.message().contains(s), () -> "Error message should contain '" + s + "', but was: " + vr.message());
        }
    }

    // ----------------- tests -----------------

    @Test
    void validPack_exact8n_nonNegativeIntegers() throws Exception {
        int n = 2; // expect 16 lines
        Path p = writeLines(
            "1","2","3","4","5","6","7","8",
            "9","10","11","12","13","14","15","16"
        );
        var vr = CardGame.validatePack(p, n);
        assertOk(vr);
        assertEquals(16, vr.values().size(), "Should parse exactly 8*n integers");
        // values() should be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> vr.values().add(99));
    }

    @Test
    void invalidPack_fileNotFound() {
        Path p = tmp.resolve("does-not-exist.txt");
        var vr = CardGame.validatePack(p, 2);
        assertErr(vr, "file not found");
    }

    @Test
    void invalidPack_wrongCount_tooFew() throws Exception {
        int n = 2; // need 16, provide 15
        Path p = writeLines(
            "0","0","0","0","0","0","0","0",
            "0","0","0","0","0","0","0"
        );
        var vr = CardGame.validatePack(p, n);
        assertErr(vr, "pack has 15 lines", "expected 16");
    }

    @Test
    void invalidPack_wrongCount_tooMany() throws Exception {
        int n = 2; // need 16, provide 17
        Path p = writeLines(
            "0","0","0","0","0","0","0","0",
            "0","0","0","0","0","0","0","0","0"
        );
        var vr = CardGame.validatePack(p, n);
        assertErr(vr, "pack has 17 lines", "expected 16");
    }

    @Test
    void invalidPack_nonIntegerToken() throws Exception {
        int n = 1; // need 8
        Path p = writeLines("1","2","3","four","5","6","7","8");
        var vr = CardGame.validatePack(p, n);
        assertErr(vr, "line 4", "not a non-negative integer");
    }

    @Test
    void invalidPack_negativeNumber_orSign() throws Exception {
        int n = 1;
        Path p = writeLines("1","-2","3","4","5","6","7","8");
        var vr = CardGame.validatePack(p, n);
        // our validator rejects '-' by regex before negativity check
        assertErr(vr, "line 2", "not a non-negative integer");
    }

    @Test
    void invalidPack_blankLine() throws Exception {
        int n = 1;
        Path p = writeLines("1","2","","4","5","6","7","8");
        var vr = CardGame.validatePack(p, n);
        assertErr(vr, "line 3 is blank");
    }

    @Test
    void invalidPack_overflowTooLarge() throws Exception {
        int n = 1;
        Path p = writeLines("1","2","3","40000000000","5","6","7","8"); // > Integer.MAX_VALUE
        var vr = CardGame.validatePack(p, n);
        assertErr(vr, "line 4");
    }

    @Test
    void validPack_handlesBOMandSpaces() throws Exception {
        int n = 1;
        // add BOM to first line and spaces around numbers
        String bom = "\uFEFF";
        Path p = writeLines(bom + " 1 "," 2 "," 3 "," 4 "," 5 "," 6 "," 7 "," 8 ");
        var vr = CardGame.validatePack(p, n);
        assertOk(vr);
        assertEquals(8, vr.values().size());
        assertEquals(1, vr.values().get(0));
    }
}
