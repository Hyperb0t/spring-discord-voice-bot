package ru.itis.springbot.websocket.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.TextMessage;
import ru.itis.springbot.models.Record;
import ru.itis.springbot.services.RecordService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;

@Controller
public class AudioReceiveController {

    @Autowired
    private RecordService recordService;

    @MessageMapping("/record")
    @SendTo("/websocket/record")
    public String echo(Message<byte[]> message) throws Exception {
        try(FileOutputStream fout = new FileOutputStream("received.wav")) {
            fout.write(message.getPayload());
            fout.flush();
            fout.close();
        }catch (Exception e) {
            throw new IllegalStateException(e);
        }
        String text = recordService.recognizeText("received.wav");
        System.out.println();
        Record record = null;
        try {
            record = Record.builder()
                    .audio(Files.readAllBytes(Paths.get("received.wav")))
                    .nickname("unknown")
                    .date(new Date())
                    .text(text)
                    .build();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return "bytes received " + "\n" + "length: " + message.getPayload().length;
    }

}