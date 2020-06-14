package ru.itis.springbot.discordbot.audio;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.CombinedAudio;
import net.dv8tion.jda.api.audio.OpusPacket;
import net.dv8tion.jda.api.audio.UserAudio;
import net.dv8tion.jda.api.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.itis.springbot.models.Record;
import ru.itis.springbot.services.RecordService;

import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class AudioReceiver implements AudioReceiveHandler {

//    private PipedOutputStream pout = new PipedOutputStream();
//    private PipedInputStream pin = new PipedInputStream();
    private FileOutputStream fileOutputStream;
//    private AtomicReference<Boolean> inputStreamClosedProperty = new AtomicReference<>();
//    private boolean pipeOpen = true;
    private long timerMillis;
    private int fileNumber = 0;
    private String speakerNickname;
    private final int SILENCE_TIMEOUT = 15000;

    @Autowired
    private RecordService recordService;

    public AudioReceiver() {
//        inputStreamClosedProperty.set(false);

        try {
            Files.walk(Paths.get(""))
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().contains("recorded"))
                    .forEach(path -> path.toFile().delete());

//            pout.connect(pin);
            fileOutputStream = new FileOutputStream("recorded" + fileNumber + ".wav");
            timerMillis = System.currentTimeMillis();
//            refreshTimer();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean canReceiveCombined() {
        return false;
    }

    @Override
    public boolean canReceiveUser() {
        return true;
    }

    @Override
    public boolean canReceiveEncoded() {
        return false;
    }

    @Override
    public void handleEncodedAudio(@Nonnull OpusPacket packet) {
    }

    @Override
    public void handleCombinedAudio(@Nonnull CombinedAudio combinedAudio) {

    }

    @Override
    public void handleUserAudio(@Nonnull UserAudio userAudio) {
//        stopInputSilenceTimer.cancel();
//        refreshTimer();
//        if(!streamOpen) {
//            try {
//                pin = new PipedInputStream();
//                synchronized (pout) {
//                    pout = new PipedOutputStream();
//                    pout.connect(pin);
//                }
//            } catch (IOException e) {
//                throw new IllegalStateException(e);
//            }
//        }
        if(System.currentTimeMillis() - timerMillis > SILENCE_TIMEOUT) {
            try {
                fileOutputStream.close();
                (new Thread(this::handleFile)).start();
                fileNumber++;
                fileOutputStream = new FileOutputStream("recorded" + fileNumber + ".wav");

            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        timerMillis = System.currentTimeMillis();
        speakerNickname = userAudio.getUser().getName();
        byte[] bytes = convertAudio(userAudio);
//        if(!pipeOpen) {
//            pout = new PipedOutputStream();
//            pin = new PipedInputStream();
//            try {
//                pout.connect(pin);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
        try {
            log.info("recieving sound");
//            pout.write(bytes);
//            pout.flush();
            fileOutputStream.write(bytes);
            fileOutputStream.flush();
//            synchronized (pin) {
//                pin.notify();
//            }
        } catch (IOException e) {
            log.error("can NOT write to pout");
//            try {
//                synchronized (pin) {
//                    pin.close();
//                    pout.close();
//                    pipeOpen = false;
//                }
//            } catch (IOException ioException) {
//                log.error("can NOT close pipe");
//            }
        }
    }

    @Override
    public boolean includeUserInCombinedAudio(@Nonnull User user) {
        return false;
    }

//    public PipedInputStream getPin() {
//        return pin;
//    }


    //to 16bit 16khz mono little endian for google recognition
    private byte[] convertAudio(UserAudio userAudio) {
        int lengthInSamples = userAudio.getAudioData(1).length/4;
        ByteArrayInputStream byteInput = new ByteArrayInputStream(userAudio.getAudioData(1));
        AudioFormat oldFormat = new AudioFormat(48000, 16, 2, true, true);
        AudioInputStream audioInputStream = new AudioInputStream(byteInput, oldFormat, lengthInSamples);
        AudioFormat newFormat = new AudioFormat(16000,16,1,true,false);
        AudioInputStream converted = AudioSystem.getAudioInputStream(newFormat, audioInputStream);
        int convertedByteLength = userAudio.getAudioData(1).length/6;
        byte[] result = new byte[convertedByteLength];
        try {
            converted.read(result, 0, userAudio.getAudioData(1).length/6);
        } catch (IOException e) {
            throw  new IllegalStateException(e);
        }
        return result;
    }

    private void handleFile() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        System.out.println("file size before recog. " + new File("recorded" + fileNumber + ".wav").length());
        String text = recordService.recognizeText("recorded" + fileNumber + ".wav");
        System.out.println();
        Record record = null;
        try {
            record = Record.builder()
                    .audio(Files.readAllBytes(Paths.get("recorded" + fileNumber + ".wav")))
                    .nickname(speakerNickname)
                    .date(new Date())
                    .text(text)
                    .build();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        recordService.saveRecord(record);
    }

}
