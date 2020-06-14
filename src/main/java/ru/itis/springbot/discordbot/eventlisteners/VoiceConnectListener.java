package ru.itis.springbot.discordbot.eventlisteners;

import com.google.api.gax.rpc.BidiStream;
import com.google.cloud.dialogflow.v2.StreamingDetectIntentRequest;
import com.google.cloud.dialogflow.v2.StreamingDetectIntentResponse;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.itis.springbot.discordbot.audio.AudioReceiver;
import ru.itis.springbot.discordbot.audio.AudioSender;
import ru.itis.springbot.services.speechutils.GoogleDialogflowSTT;

import javax.annotation.Nonnull;

@Slf4j
@Component
public class VoiceConnectListener extends ListenerAdapter {

    @Autowired
    private BidiStream<StreamingDetectIntentRequest, StreamingDetectIntentResponse> bidiStream;

    @Autowired
    private GoogleDialogflowSTT googleDialogflowSTT;

    private AudioSender audioSender = new AudioSender();
    @Autowired
    private AudioReceiver audioReceiver;

    public VoiceConnectListener() {

    }

    @Override
    public void onGuildVoiceJoin(@Nonnull GuildVoiceJoinEvent event) {
        VoiceChannel oldChannel = (VoiceChannel) event.getOldValue();
        VoiceChannel newChannel = (VoiceChannel) event.getNewValue();
        log.info("old userlist");
        if (oldChannel == null) {
            log.info("old channel is NULL");
        } else {
            oldChannel.getMembers().stream().forEach(m -> log.info(m.toString()));
        }
        log.info("new userlist");
        newChannel.getMembers().stream().forEach(m -> log.info(m.toString()));

        AudioManager audioManager = event.getGuild().getAudioManager();
        audioManager.setSendingHandler(audioSender);
        audioManager.setReceivingHandler(audioReceiver);
        audioManager.openAudioConnection(newChannel);
        log.info("connected to voice channel " + newChannel);
        audioSender.loadFileAndPlay("text.mp3");

    }


}
