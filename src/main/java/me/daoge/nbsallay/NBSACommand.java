package me.daoge.nbsallay;

import org.allaymc.api.command.Command;
import org.allaymc.api.command.SenderType;
import org.allaymc.api.command.tree.CommandTree;
import org.allaymc.api.form.Forms;
import org.allaymc.api.utils.TextFormat;

/**
 * @author daoge_cmd
 */
public class NBSACommand extends Command {

    protected NBSAllay plugin;

    public NBSACommand(NBSAllay plugin) {
        super("nbs", "NBSAllay plugin main command", "nbsa.command");
        this.plugin = plugin;
    }

    @Override
    public void prepareCommandTree(CommandTree tree) {
        tree.getRoot()
                .key("play")
                .exec((context, p) -> {
                    var player = p.getController();
                    var songs = this.plugin.getSongs();
                    var form = Forms.simple()
                            .title("NBSAllay")
                            .content("Choose a song to play (" + TextFormat.GREEN + songs.size() + TextFormat.WHITE +" in total):");
                    for (var song : songs) {
                        var buttonText = song.getTitleOrFileName() + "\n" + TextFormat.DARK_GREEN + Utils.toHumanReadableLength(song.getLengthInMilliseconds());
                        form.button(buttonText).onClick($ -> {
                            this.plugin.getPlayManager().play(player, song);
                            player.sendMessage("Start playing: " + TextFormat.GREEN + song.getTitleOrFileName());
                        });
                    }
                    form.sendTo(player);

                    return context.success();
                }, SenderType.ACTUAL_PLAYER)
                .root()
                .key("pause")
                .exec((context, p) -> {
                    var player = p.getController();
                    this.plugin.getPlayManager().pause(player);
                    player.sendMessage(TextFormat.YELLOW + "Paused");
                    return context.success();
                }, SenderType.ACTUAL_PLAYER)
                .root()
                .key("resume")
                .exec((context, p) -> {
                    var player = p.getController();
                    this.plugin.getPlayManager().resume(player);
                    player.sendMessage(TextFormat.GREEN + "Resumed");
                    return context.success();
                }, SenderType.ACTUAL_PLAYER)
                .root()
                .key("stop")
                .exec((context, p) -> {
                    var player = p.getController();
                    this.plugin.getPlayManager().stop(player);
                    player.sendMessage(TextFormat.RED + "Stopped");
                    return context.success();
                }, SenderType.ACTUAL_PLAYER)
                .root()
                .key("reload")
                .exec(context -> {
                    this.plugin.reload();
                    context.addOutput(TextFormat.GREEN + "Reloaded successfully");
                    return context.success();
                });
    }
}
