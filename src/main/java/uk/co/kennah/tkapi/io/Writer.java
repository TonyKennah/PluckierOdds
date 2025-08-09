package uk.co.kennah.tkapi.io;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import uk.co.kennah.tkapi.model.MyRunner;

public class Writer {

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
