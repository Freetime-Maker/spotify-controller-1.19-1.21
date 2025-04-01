package com.tiji.media.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.tiji.media.Media;
import com.tiji.media.MediaClient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.tiji.media.api.SongDataExtractor.*;
public class ImageDownloader {
    private static final ArrayList<Identifier> loadedCover = new ArrayList<>();
    private static final ArrayBlockingQueue<JsonObject> queue = new ArrayBlockingQueue<>(200);
    private static final HashMap<JsonObject, ArrayList<Consumer<Identifier>>> onComplete = new HashMap<>();

    @SuppressWarnings("deprecation") // It will be re-visited
    private static Identifier getAlbumCover(JsonObject trackObj) {
        try {
            Identifier id = Identifier.of("media", getId(trackObj).toLowerCase());

            int wantedSize = 100 * MinecraftClient.getInstance().options.getGuiScale().getValue();
            int closest = Integer.MAX_VALUE;
            JsonArray ImageList = trackObj.getAsJsonObject("album")
                    .getAsJsonArray("images");
            String closestUrl = ImageList.get(0)
                    .getAsJsonObject().get("url").getAsString();

            for (int i = 0; i < ImageList.size(); i++) {
                int size = ImageList.get(i)
                        .getAsJsonObject().get("height").getAsInt();
                if (closest > size && size >= wantedSize) {
                    closest = size;
                    closestUrl = ImageList.get(i)
                            .getAsJsonObject().get("url").getAsString();
                }
            }
            InputStream albumCoverUrl = new URL(closestUrl).openStream();

            // Spotify provides JPEG image that Minecraft cannot handle
            // Convert to PNG
            BufferedImage jpegImage = ImageIO.read(albumCoverUrl);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(jpegImage, "png", outputStream);
            ByteArrayInputStream imageStream = new ByteArrayInputStream(outputStream.toByteArray());

            NativeImage image = NativeImage.read(imageStream);
            MinecraftClient.getInstance().getTextureManager().registerTexture(id,
                    new NativeImageBackedTexture(image));

            Thread.sleep(150); // Wait until the texture is loaded

            loadedCover.add(id);
            return id;
        }catch (IOException e) {
            Media.LOGGER.error("Failed to download album cover for {}: {}", getId(trackObj), e);
            Media.LOGGER.error(trackObj.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (NullPointerException e) {
            Media.LOGGER.error("Unexpected response from Spotify: {}\n{}", trackObj, e.getLocalizedMessage());
        }
        return Identifier.of("media", "ui/nothing.png");
    }

    public static void addDownloadTask(JsonObject data, Consumer<Identifier> callback) {
        if (loadedCover.contains(Identifier.of("media", getId(data).toLowerCase()))){
            Media.LOGGER.debug("Cache hit for {}", getId(data));
            CompletableFuture.runAsync(() -> {
                try{
                    Thread.sleep(100); // Wait until SongData object is ready to accept image
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                callback.accept(Identifier.of("media", getId(data).toLowerCase()));
            });
            return;
        }
        Media.LOGGER.debug("Adding download task lister for {}", getId(data));
        if (onComplete.containsKey(data)) {
            onComplete.get(data).add(callback);
            return;
        }
        ArrayList<Consumer<Identifier>> callbacks = new ArrayList<>();
        callbacks.add(callback);
        onComplete.put(data, callbacks);
        queue.add(data);
        Media.LOGGER.debug("Added download task for {} - Queue size: {}", getId(data), queue.size());
    }

    public static void startThreads() {
        for (int i = 0; i < MediaClient.CONFIG.imageIoThreadCount(); i++) {
            Thread thread = new Thread(null, ImageDownloader::threadWorker, "Image-IO-" + i);
            thread.start();
        }
    }

    private static void threadWorker(){
        while (!Thread.interrupted()) {
            try {
                JsonObject task = queue.take();
                Identifier coverId = getAlbumCover(task);

                for (Consumer<Identifier> callback : onComplete.remove(task)) {
                    callback.accept(coverId);
                }
                Media.LOGGER.debug("Finished downloading cover for {}", getId(task));
            } catch (Exception e) {
                StringBuilder sb = new StringBuilder();
                for (StackTraceElement element : e.getStackTrace()) {
                    sb.append("at ");
                    sb.append(element.toString()).append("\n");
                }
                Media.LOGGER.error("Error in Image-IO thread: {}\n{}", e.getLocalizedMessage(), sb);
                // Exception shouldn't stop thread
                // They are mostly not from IO
            }
        }
    }
}
