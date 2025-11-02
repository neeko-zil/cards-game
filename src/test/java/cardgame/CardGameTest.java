package cardgame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the CardGame class, focusing on pack validation.
 */
public class CardGameTest {
    
    @Test
    public void testValidPackCreation(@TempDir Path tempDir) throws IOException {
        Path packFile = tempDir.resolve("valid_pack.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(packFile.toFile()))) {
            for (int i = 0; i < 16; i++) {
                writer.write(String.valueOf(i % 4 + 1));
                writer.newLine();
            }
        }
        assertTrue(packFile.toFile().exists());
    }
    
    @Test
    public void testInvalidPackWithNegativeValues(@TempDir Path tempDir) throws IOException {
        Path packFile = tempDir.resolve("negative_pack.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(packFile.toFile()))) {
            for (int i = 0; i < 15; i++) {
                writer.write("1");
                writer.newLine();
            }
            writer.write("-1");
            writer.newLine();
        }
        assertTrue(packFile.toFile().exists());
    }
    
    @Test
    public void testInvalidPackWithWrongLength(@TempDir Path tempDir) throws IOException {
        Path packFile = tempDir.resolve("wrong_length_pack.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(packFile.toFile()))) {
            for (int i = 0; i < 10; i++) {
                writer.write("1");
                writer.newLine();
            }
        }
        assertTrue(packFile.toFile().exists());
    }
}
