package me.daoge.nbsallay;

/**
 * @author daoge_cmd
 */
public class Utils {
    public static String toHumanReadableLength(int milliseconds) {
        var seconds = (int) Math.ceil(milliseconds / 1000F);
        return String.format("%02d:%02d", (seconds / 60) % 60, seconds % 60);
    }
}
