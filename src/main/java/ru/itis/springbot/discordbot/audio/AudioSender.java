package ru.itis.springbot.discordbot.audio;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerRegistry;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.audio.AudioSendHandler;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

@Slf4j
public class AudioSender implements AudioSendHandler {

    private AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    private final AudioPlayer audioPlayer = playerManager.createPlayer();
    private AudioFrame lastFrame;
    private boolean playing = false;
    private boolean lastFrameWasNull = false;

    public AudioSender() {
        AudioSourceManagers.registerLocalSource(playerManager, MediaContainerRegistry.DEFAULT_REGISTRY);
    }

    @Override
    public boolean canProvide() {
        lastFrame = audioPlayer.provide();
        if (lastFrame != null) {
            lastFrameWasNull = false;
            return true;
        }else {
            if(lastFrameWasNull) {
                audioPlayer.stopTrack();
            }
            lastFrameWasNull = true;
           return false;
        }
    }

    @Nullable
    @Override
    public ByteBuffer provide20MsAudio() {
        return ByteBuffer.wrap(lastFrame.getData());
    }


    @Override
    public boolean isOpus() {
        return true;
    }

    public void loadFileAndPlay(String path) {
        playerManager.loadItem(path, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
                audioPlayer.playTrack(audioTrack);
                playing = true;
                log.info("audio file loaded successfully");
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                log.info("playlist loaded???");
            }

            @Override
            public void noMatches() {
                log.info("no matches for track???");
            }

            @Override
            public void loadFailed(FriendlyException e) {
                log.error("can't load audio file");
            }
        });
    }
}
