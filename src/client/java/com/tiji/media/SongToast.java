package com.tiji.media;

import com.tiji.media.util.imageWithColor;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;

public class SongToast implements Toast {
    private final imageWithColor cover;
    private final String artist;
    private final Text title;
    private Long startTime;

    public SongToast(imageWithColor cover, String artist, Text title) {
        this.cover = cover;
        this.artist = artist;
        this.title = title;
    }

    public void show(ToastManager manager) {
        manager.add(this);
    }

    @Override
    public Visibility getVisibility() {
        if (this.startTime == null) {
            return Visibility.SHOW;
        }
        return System.currentTimeMillis() - this.startTime > 3000 ? Visibility.HIDE : Visibility.SHOW;
    }

    @Override
    public void update(ToastManager manager, long time) {

    }

    @Override
    public void draw(DrawContext context, TextRenderer textRenderer, long startTime) {
        if (this.startTime == null) {
            this.startTime = System.currentTimeMillis();
        }
        context.fill(0, 0, 180, 32, cover.color);

        context.drawText(textRenderer, title, 35, 6, Colors.LIGHT_YELLOW, false);
        context.drawText(textRenderer, artist, 35, 18, Colors.WHITE, false);

        context.drawTexture(RenderLayer::getGuiTextured, cover.image, 0, 0, 0, 0, 32, 32, 32, 32);
    }
}
