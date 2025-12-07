package me.daoge.nbsallay;

import lombok.Getter;
import lombok.SneakyThrows;
import net.raphimc.noteblocklib.NoteBlockLib;
import net.raphimc.noteblocklib.format.SongFormat;
import net.raphimc.noteblocklib.model.Song;
import org.allaymc.api.plugin.Plugin;
import org.allaymc.api.registry.Registries;
import org.allaymc.api.utils.TextFormat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static java.util.stream.Collectors.toSet;

public class NBSAllay extends Plugin {

    @Getter
    protected static NBSAllay instance;

    {
        instance = this;
    }

    @Getter
    protected Path songFolder;
    @Getter
    protected List<Song> songs;
    @Getter
    protected PlayManager playManager;

    @SneakyThrows
    @Override
    public void onEnable() {
        this.songFolder = this.pluginContainer.dataFolder().resolve("songs");
        this.songs = readSongs();
        this.playManager = new PlayManager(this);
        Registries.COMMANDS.register(new NBSACommand(this));
    }

    @SneakyThrows
    protected List<Song> readSongs() {
        if (!Files.exists(songFolder)) {
            Files.createDirectory(songFolder);
        }

        List<Song> map = new ArrayList<>();
        try (var stream = Files.list(songFolder)) {
            var paths = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> SongFormat.getByExtension(getFileExt(path)) != null)
                    .collect(toSet());
            for (var path : paths) {
                Song song;
                try {
                    song = NoteBlockLib.readSong(path);
                } catch (Exception e) {
                    this.pluginLogger.error("Failed to read song {}{}", TextFormat.GREEN, path, e);
                    continue;
                }

                map.add(song);
                this.pluginLogger.info("Loaded song {}{}", TextFormat.GREEN, song.getTitleOrFileName());
            }
        }

        return map;
    }

    @Override
    public boolean isReloadable() {
        return true;
    }

    @Override
    public void reload() {
        this.pluginLogger.info("Reloading songs...");
        this.songs = readSongs();
        this.pluginLogger.info("Reloaded {}{}{} songs", TextFormat.GREEN, this.songs.size(), TextFormat.RESET);
    }

    protected static String getFileExt(Path path) {
        var name = path.getFileName().toString();
        var dot = name.lastIndexOf('.');
        if (dot <= 0 || dot == name.length() - 1) {
            return "";
        }

        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}