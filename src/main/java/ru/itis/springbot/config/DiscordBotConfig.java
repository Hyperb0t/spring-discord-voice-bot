package ru.itis.springbot.config;

import com.google.api.gax.rpc.BidiStream;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.StreamingDetectIntentRequest;
import com.google.cloud.dialogflow.v2.StreamingDetectIntentResponse;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.itis.springbot.discordbot.eventlisteners.VoiceConnectListener;

import javax.security.auth.login.LoginException;
import java.io.IOException;

@Configuration
public class DiscordBotConfig {
    @Autowired
    private VoiceConnectListener voiceConnectListener;
    @Bean
    public JDA jda () throws LoginException {
        JDA jda = new JDABuilder("NzA5MzM4NDU3MjM0ODY2MTc2.XrkdHA.b4OfS4p6PewmxoX4Jc6KjlKcuYE")
                .addEventListeners(voiceConnectListener)
                .build();
        return jda;
    }

    @Bean
    public BidiStream<StreamingDetectIntentRequest, StreamingDetectIntentResponse> bidiStream() {
        SessionsClient sessionsClient = null;
        try {
            sessionsClient = SessionsClient.create();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        BidiStream<StreamingDetectIntentRequest, StreamingDetectIntentResponse> bidiStream =
                sessionsClient.streamingDetectIntentCallable().call();
        return bidiStream;
    }
}
