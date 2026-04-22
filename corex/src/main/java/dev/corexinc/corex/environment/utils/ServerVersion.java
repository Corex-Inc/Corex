package dev.corexinc.corex.environment.utils;

import org.bukkit.Bukkit;
import org.jspecify.annotations.NonNull;

public record ServerVersion(int major, int minor, int patch) implements Comparable<ServerVersion> {

    public static ServerVersion parse(String version) {
        String[] parts = version.split("\\.");
        int major = parts.length > 0 ? parseInt(parts[0]) : 1;
        int minor = parts.length > 1 ? parseInt(parts[1]) : 0;
        int patch = parts.length > 2 ? parseInt(parts[2]) : 0;
        return new ServerVersion(major, minor, patch);
    }

    public static ServerVersion getCurrent() {
        String versionString = Bukkit.getBukkitVersion().split("-")[0];
        return parse(versionString);
    }

    private static int parseInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static boolean isAtLeast(String required) {
        return getCurrent().compareTo(parse(required)) >= 0;
    }

    @Override
    public int compareTo(ServerVersion o) {
        if (this.major != o.major) return Integer.compare(this.major, o.major);
        if (this.minor != o.minor) return Integer.compare(this.minor, o.minor);
        return Integer.compare(this.patch, o.patch);
    }

    @Override
    public @NonNull String toString() {
        return major + "." + minor + "." + patch;
    }
}