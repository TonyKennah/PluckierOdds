package uk.co.kennah.tkapi.io;

import uk.co.kennah.tkapi.MyRunner;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;

public class OddsWriter {

    /**
     * Writes the provided market data to a file in a specific format (name#odds).
     * It first checks if the serialized data is larger than 500 bytes before writing.
     *
     * @param filePath The path of the file to write to.
     * @param data     The market data to write.
     * @throws IOException if an I/O error occurs.
     */
    public void write(String filePath, HashMap<Long, MyRunner> data) throws IOException {
        // First, check if the data is substantial enough to write.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(data);
        int length = baos.toByteArray().length;
        System.out.println("Length of file is: " + length + " bytes");

        if (length <= 500) {
            System.out.println("The odds data is too small or invalid, not writing file.");
            return;
        }

        // Use try-with-resources to ensure the writer is closed automatically.
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            for (MyRunner runner : data.values()) {
                Double odd = runner.getOdds() != null ? runner.getOdds() : 0.0;
                bw.write(runner.getName() + "#" + odd + "\n");
            }
        }
    }
}

