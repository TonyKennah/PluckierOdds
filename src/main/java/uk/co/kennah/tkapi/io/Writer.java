package uk.co.kennah.tkapi.io;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import uk.co.kennah.tkapi.model.MyRunner;

/**
 * Handles writing processed horse racing data to a file.
 * This class provides functionality to persist the collected runner data
 * into a specified file in a simple, delimited format.
 */
public class Writer {

    /**
     * Writes the runner data to a file at the specified path.
     * The data is formatted as "event#name#odds" for each runner.
     * It performs a check to ensure the data is not empty or trivial before writing.
     * @param filePath The absolute or relative path of the file to write to.
     * @param data A map containing the runner data, with selection ID as the key.
     * @throws IOException if an I/O error occurs while writing to the file.
     */
    public void publish(String filePath, HashMap<Long, MyRunner> data) throws IOException {
        // First, check if the data is substantial enough to write.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(data);
        int length = baos.toByteArray().length;
        
        if (length <= 10) {
            System.out.println("The odds data is too small or invalid, not writing file.");
            return;
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            for (MyRunner runner : data.values()) {
                Double odd = runner.getOdds() != null ? runner.getOdds() : 0.0;
                String event = runner.getEvent();
                bw.write(event + "#" + runner.getName() + "#" + odd + "\n");
            }
        }
    }
}
