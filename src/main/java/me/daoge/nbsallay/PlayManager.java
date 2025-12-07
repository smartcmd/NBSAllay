package me.daoge.nbsallay;

import lombok.Getter;
import net.raphimc.noteblocklib.data.MinecraftInstrument;
import net.raphimc.noteblocklib.model.Note;
import net.raphimc.noteblocklib.model.Song;
import net.raphimc.noteblocklib.player.SongPlayer;
import org.allaymc.api.bossbar.BossBar;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.eventbus.event.server.PlayerQuitEvent;
import org.allaymc.api.player.Player;
import org.allaymc.api.plugin.Plugin;
import org.allaymc.api.server.Server;
import org.allaymc.api.utils.TextFormat;
import org.allaymc.api.utils.tuple.Pair;
import org.allaymc.api.world.sound.CustomSound;
import org.allaymc.api.world.sound.Sound;
import org.allaymc.api.world.sound.SoundNames;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author daoge_cmd
 */
public class PlayManager {
    protected final Map<UUID, Pair<AllaySongPlayer, BossBar>> players;

    public PlayManager(Plugin plugin) {
        this.players = new HashMap<>();
        Server.getInstance().getEventBus().registerListener(this);
        Server.getInstance().getScheduler().scheduleRepeating(plugin, this::update, 20);
    }

    public synchronized void play(Player player, Song song) {
        var uuid = player.getLoginData().getUuid();
        stop(player);

        var songPlayer = new AllaySongPlayer(song, player);
        var bar = BossBar.create();
        bar.setTitle(generateBarTitle(songPlayer));
        bar.setProgress(0);
        bar.addViewer(player);
        this.players.put(uuid, new Pair<>(songPlayer, bar));

        songPlayer.start();
    }

    public synchronized void pause(Player player) {
        var pair = this.players.get(player.getLoginData().getUuid());
        if (pair == null) {
            return;
        }

        var songPlayer = pair.left();
        if (!songPlayer.isPaused()) {
            songPlayer.setPaused(true);
        }
    }

    public synchronized void resume(Player player) {
        var pair = this.players.get(player.getLoginData().getUuid());
        if (pair == null) {
            return;
        }

        var songPlayer = pair.left();
        if (songPlayer.isPaused()) {
            songPlayer.setPaused(false);
        }
    }

    public synchronized void stop(Player player) {
        var uuid = player.getLoginData().getUuid();
        var pair = players.remove(uuid);
        if (pair != null) {
            pair.left().stop();
            pair.right().removeViewer(player);
        }
    }

    protected synchronized boolean update() {
        for (var pair : players.values()) {
            var songPlayer = pair.left();
            if (!songPlayer.isRunning()) {
                stop(songPlayer.getPlayer());
                continue;
            }

            var bar = pair.right();
            bar.setProgress((float) songPlayer.getMillisecondPosition() / (float) songPlayer.getSong().getLengthInMilliseconds());
            bar.setTitle(generateBarTitle(songPlayer));
        }

        return true;
    }

    protected String generateBarTitle(SongPlayer songPlayer) {
        var title = new StringBuilder().append(songPlayer.getSong().getTitleOrFileName());
        if (songPlayer.isPaused()) {
            title.append(TextFormat.YELLOW).append(" (Paused)");
        }

        title.append(TextFormat.GREEN)
                .append(" (")
                .append(Utils.toHumanReadableLength(songPlayer.getMillisecondPosition()))
                .append("/")
                .append(Utils.toHumanReadableLength(songPlayer.getSong().getLengthInMilliseconds()))
                .append(")");

        return title.toString();
    }

    @EventHandler
    protected void onPlayerQuit(PlayerQuitEvent event) {
        stop(event.getPlayer());
    }

    @Getter
    protected static class AllaySongPlayer extends SongPlayer {

        protected final Player player;

        public AllaySongPlayer(Song song, Player player) {
            super(song);
            this.player = player;
        }

        @Override
        protected void playNotes(List<Note> notes) {
            for (var note : notes) {
                var sound = toSound(note);
                if (sound == null) {
                    continue;
                }

                this.player.viewSound(toSound(note), this.player.getControlledEntity().getLocation(), false);
            }
        }

        protected static Sound toSound(Note note) {
            var instrument = note.getInstrument();
            var name = switch (instrument) {
                case MinecraftInstrument.HARP -> SoundNames.NOTE_HARP;
                case MinecraftInstrument.BASS -> SoundNames.NOTE_BASS;
                case MinecraftInstrument.BASS_DRUM -> SoundNames.NOTE_BD;
                case MinecraftInstrument.SNARE -> SoundNames.NOTE_SNARE;
                case MinecraftInstrument.HAT -> SoundNames.NOTE_HAT;
                case MinecraftInstrument.GUITAR -> SoundNames.NOTE_GUITAR;
                case MinecraftInstrument.FLUTE -> SoundNames.NOTE_FLUTE;
                case MinecraftInstrument.BELL -> SoundNames.NOTE_BELL;
                case MinecraftInstrument.CHIME -> SoundNames.NOTE_CHIME;
                case MinecraftInstrument.XYLOPHONE -> SoundNames.NOTE_XYLOPHONE;
                case MinecraftInstrument.IRON_XYLOPHONE -> SoundNames.NOTE_IRON_XYLOPHONE;
                case MinecraftInstrument.COW_BELL -> SoundNames.NOTE_COW_BELL;
                case MinecraftInstrument.DIDGERIDOO -> SoundNames.NOTE_DIDGERIDOO;
                case MinecraftInstrument.BIT -> SoundNames.NOTE_BIT;
                case MinecraftInstrument.BANJO -> SoundNames.NOTE_BANJO;
                case MinecraftInstrument.PLING -> SoundNames.NOTE_PLING;
                default -> null;
            };

            if (name == null) {
                NBSAllay.getInstance().getPluginLogger().warn("Unsupported instrument: {}", instrument.getClass().getName());
                return null;
            }

            return new CustomSound(name, note.getVolume(), note.getPitch());
        }
    }
}
