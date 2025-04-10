package com.tiji.media.ui;

import com.tiji.media.Media;
import com.tiji.media.MediaClient;
import com.tiji.media.api.ApiCalls;
import com.tiji.media.util.repeatMode;
import io.github.cottonmc.cotton.gui.client.BackgroundPainter;
import io.github.cottonmc.cotton.gui.client.CottonClientScreen;
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.client.ScreenDrawing;
import io.github.cottonmc.cotton.gui.widget.*;
import io.github.cottonmc.cotton.gui.widget.data.Axis;
import io.github.cottonmc.cotton.gui.widget.data.HorizontalAlignment;
import io.github.cottonmc.cotton.gui.widget.data.Insets;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;

import com.tiji.media.widgets.*;
import net.minecraft.util.Util;

public class NowPlayingScreen extends LightweightGuiDescription {
    private static class RootPanel extends WPlainPanel {
        public void onHidden() {
            MediaClient.nowPlayingScreen = null;
        }
    }

    private static final Style ICON = Style.EMPTY.withFont(Identifier.of("media", "icon"));

    public WLabel songName = new WLabel(Text.translatable("ui.media.nothing_playing"));
    public WLabel artistName = new WLabel(Text.translatable("ui.media.unknown_artist"));
    public progressWidget progressBar = new progressWidget(0, 100, Axis.HORIZONTAL);
    public WLabel durationLabel = new WLabel(Text.translatable("ui.media.unknown_duration"));
    public WLabel currentTimeLabel = new WLabel(Text.translatable("ui.media.unknown_time"));
    public borderlessButtonWidget playPauseButton = new borderlessButtonWidget(Text.literal("2").setStyle(ICON));
    public WSprite albumCover = new WSprite(Identifier.of("media", "ui/nothing.png"));
    public borderlessButtonWidget repeat = new borderlessButtonWidget(repeatMode.getAsText(MediaClient.repeat));
    public borderlessButtonWidget shuffle = new borderlessButtonWidget(Text.literal("4").setStyle(ICON));
    public borderlessButtonWidget like = new borderlessButtonWidget(Text.literal("b").setStyle(ICON));
    public WPlainPanel root = new RootPanel();

    public NowPlayingScreen() {
        root.setSize(300, 200);
        root.setInsets(Insets.NONE);

        root.add(albumCover, 100, 10, 100, 100);

        root.add(new clickableSprite(Identifier.of("media", "ui/attribution.png")).setOnClick(() -> {
            if (MediaClient.currentlyPlaying.songURI == null) return;
            Util.getOperatingSystem().open(MediaClient.currentlyPlaying.songURI);
        }), 270, 10);

        songName = songName.setHorizontalAlignment(HorizontalAlignment.CENTER);
        root.add(songName, 100, 120, 100, 20);

        artistName = artistName.setHorizontalAlignment(HorizontalAlignment.CENTER);
        root.add(artistName, 100, 135, 100, 20);

        root.add(new borderlessButtonWidget(Text.literal("c").setStyle(ICON)).setOnClick(() -> {
            Screen screen = new CottonClientScreen(new SearchScreen());
            MinecraftClient.getInstance().setScreen(screen);
            MediaClient.nowPlayingScreen = null;
        }), 80, 150, 20, 20);

        shuffle.setOnClick(() -> {
            MediaClient.shuffle = !MediaClient.shuffle;
            ApiCalls.setShuffle(MediaClient.shuffle);
        });
        root.add(shuffle, 100, 150, 20, 20);

        root.add(new borderlessButtonWidget(Text.literal("0").setStyle(ICON)).setOnClick(ApiCalls::previousTrack), 120, 150, 20, 20);

        playPauseButton.setOnClick(() -> {
            if (MediaClient.currentlyPlaying.Id.isEmpty()) return;

            MediaClient.isPlaying = !MediaClient.isPlaying;
            ApiCalls.playPause(MediaClient.isPlaying);
        });
        root.add(playPauseButton, 140, 150, 20, 20);

        root.add(new borderlessButtonWidget(Text.literal("1").setStyle(ICON)).setOnClick(ApiCalls::nextTrack), 160, 150, 20, 20);

        repeat.setOnClick(() -> {
            MediaClient.repeat = repeatMode.getNextMode(MediaClient.repeat);
            ApiCalls.setRepeat(MediaClient.repeat);
        });
        root.add(repeat, 180, 150, 20, 20);

        like.setOnClick(() -> {
            MediaClient.isLiked = !MediaClient.isLiked;
            ApiCalls.toggleLikeSong(MediaClient.currentlyPlaying.Id, MediaClient.isLiked);
        });
        root.add(like, 200, 150, 20, 20);

        currentTimeLabel = currentTimeLabel.setHorizontalAlignment(HorizontalAlignment.LEFT);
        root.add(currentTimeLabel, 10, 160, 60, 20);

        progressBar.setMaxValue(300);
        root.add(progressBar, 10, 175, 280, 10);

        durationLabel = durationLabel.setHorizontalAlignment(HorizontalAlignment.RIGHT);
        root.add(durationLabel, 230, 160, 60, 20);

        root.validate(this);

        setRootPanel(root);
    }
    public void updateStatus() {
        if (MediaClient.currentlyPlaying.Id.isEmpty()) return;

        if (progressBar.allowUpdateProgress) {
            progressBar.setValue((int) Math.round(MediaClient.progressValue * 300));
        }
        currentTimeLabel.setText(Text.of(MediaClient.progressLabel));
        playPauseButton.setLabel(Text.literal(MediaClient.isPlaying ? "2" : "3").setStyle(ICON));
        repeat.setLabel(repeatMode.getAsText(MediaClient.repeat));
        shuffle.setLabel(Text.literal(MediaClient.shuffle ? "5" : "4").setStyle(ICON));
        like.setLabel(Text.literal(MediaClient.isLiked ? "b" : "a").setStyle(ICON));
        root.setBackgroundPainter(BackgroundPainter.createColorful(MediaClient.currentlyPlaying.coverImage.color));
    }
    public void updateNowPlaying() {
        if (MediaClient.currentlyPlaying.Id.isEmpty()) {
            nothingPlaying();
            return;
        }

        Media.LOGGER.info(MediaClient.currentlyPlaying.toString());

        songName.setText(MediaClient.currentlyPlaying.title);
        artistName.setText(Text.of(MediaClient.currentlyPlaying.artist));
        durationLabel.setText(Text.of(MediaClient.currentlyPlaying.durationLabel));
        updateCoverImage();
        updateStatus();
    }
    public void nothingPlaying() {
        songName.setText(Text.translatable("ui.media.nothing_playing"));
        artistName.setText(Text.translatable("ui.media.unknown_artist"));
        durationLabel.setText(Text.translatable("ui.media.unknown_duration"));
        updateCoverImage();
        progressBar.setValue(0);
        currentTimeLabel.setText(Text.translatable("ui.media.unknown_time"));
    }
    public void updateCoverImage() {
        albumCover.setImage(MediaClient.currentlyPlaying.coverImage.image);
    }
}
