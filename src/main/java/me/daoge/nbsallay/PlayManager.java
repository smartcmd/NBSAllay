package me.daoge.nbsallay;

import lombok.Getter;
import net.raphimc.noteblocklib.data.MinecraftInstrument;
import net.raphimc.noteblocklib.format.nbs.model.NbsCustomInstrument;
import net.raphimc.noteblocklib.model.Note;
import net.raphimc.noteblocklib.model.Song;
import net.raphimc.noteblocklib.player.SongPlayer;
import org.allaymc.api.bossbar.BossBar;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.eventbus.event.server.PlayerQuitEvent;
import org.allaymc.api.math.MathUtils;
import org.allaymc.api.math.location.Location3dc;
import org.allaymc.api.player.Player;
import org.allaymc.api.plugin.Plugin;
import org.allaymc.api.server.Server;
import org.allaymc.api.utils.TextFormat;
import org.allaymc.api.utils.tuple.Pair;
import org.allaymc.api.world.sound.SoundNames;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.packet.PlaySoundPacket;
import org.joml.Vector3d;
import org.joml.Vector3dc;

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
        @SuppressWarnings("ALL")
        protected void playNotes(List<Note> notes) {
            for (var note : notes) {
                var soundName = toSoundName(note);
                if (soundName == null) {
                    continue;
                }

                var soundPos = calculateSoundPos(this.player.getControlledEntity().getLocation(), note.getPanning());
                var packet = new PlaySoundPacket();
                packet.setSound(soundName);
                packet.setPosition(Vector3f.from(soundPos.x(), soundPos.y(), soundPos.z()));
                packet.setVolume(note.getVolume());
                packet.setPitch(note.getPitch());

                // Send play sound packet immediately to improve the sound quality
                this.player.sendPacketImmediately(packet);
            }
        }

        protected static Vector3dc calculateSoundPos(Location3dc playerLocation, float panning) {
            // 1. 如果 Panning 为 0，声音直接位于玩家位置（立体声正中/头中效应）
            if (panning == 0) {
                return playerLocation;
            }

            // 2. 获取玩家当前的视线方向向量 (Forward Vector)
            // MathUtils 已经处理了 Yaw/Pitch 到向量的转换
            var direction = MathUtils.getDirectionVector(playerLocation);

            // 3. 计算“右侧”方向向量 (Right Vector)
            // 原理：视线向量(Forward) 叉乘 向上向量(Up, 0,1,0) 得到垂直于两者的右侧向量
            var up = new Vector3d(0, 1, 0);
            var right = new Vector3d();

            direction.cross(up, right);

            // 4. 归一化并计算最终偏移
            if (right.lengthSquared() > 0) {
                right.normalize();
            } else {
                // 极端情况：如果玩家完全垂直向上或向下看，叉积可能为0
                // 此时无法判定左右，直接返回原位置
                return playerLocation;
            }

            // 扩散距离系数 (Stereo Spread Distance)
            // 2.0 左右的值能提供比较自然的立体声分离感，既不会太远听不见，也不会太近分不开
            double spreadDistance = 2.0;

            // 偏移量 = 右侧向量 * panning(-1~1) * 距离
            // 如果 panning 是负数，这里会自动反向变为“左侧”
            right.mul(panning * spreadDistance);

            // 5. 计算最终坐标：玩家原坐标 + 偏移向量
            return playerLocation.add(right, new Vector3d());
        }

        // TODO: support nbs custom instruments
        protected static String toSoundName(Note note) {
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
                if (!(instrument instanceof NbsCustomInstrument)) {
                    NBSAllay.getInstance().getPluginLogger().debug("Unsupported instrument: {}", instrument.getClass().getName());
                }

                return null;
            }

            return name;
        }
    }
}
