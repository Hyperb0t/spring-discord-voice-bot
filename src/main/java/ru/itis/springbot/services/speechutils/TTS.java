package ru.itis.springbot.services.speechutils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

public class TTS {

    public TTS() {
    }

    public void createAudioFile(String filename, String text) throws IOException, InterruptedException {
        writeBomBytes(filename + ".txt");
        try(
                PrintWriter writer = new PrintWriter(new FileOutputStream(filename + ".txt", true));
        ) {
            writer.print(text);
            writer.flush();
            writer.close();
        }catch (IOException e) {
            throw new IllegalStateException(e);
        }
        Process process = new ProcessBuilder("tts.bat").start();
        process.waitFor();
    }

    private void writeBomBytes(String filename) {
        try (OutputStream out = new FileOutputStream(filename)) {

            // write a byte sequence
            out.write(0xEF);
            out.write(0xBB);
            out.write(0xBF);

            out.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
