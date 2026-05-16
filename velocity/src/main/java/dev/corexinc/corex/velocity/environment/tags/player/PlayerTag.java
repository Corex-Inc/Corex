package dev.corexinc.corex.velocity.environment.tags.player;

import com.velocitypowered.api.proxy.Player;
import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.MechanismProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Adjustable;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.tags.Flaggable;
import dev.corexinc.corex.engine.flags.trackers.AbstractFlagTracker;
import dev.corexinc.corex.engine.flags.trackers.SqlFlagTracker;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.PlayerIdentity;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.velocity.CorexVelocity;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.util.Optional;
import java.util.UUID;

public class PlayerTag implements AbstractTag, Adjustable, Flaggable, PlayerIdentity {

    private static final String PREFIX = "p";

    public static final TagProcessor<PlayerTag> TAG_PROCESSOR = new TagProcessor<>();
    public static final MechanismProcessor<PlayerTag> MECHANISM_PROCESSOR = new MechanismProcessor<>();

    private final Player player;

    public PlayerTag(Player player) {
        this.player = player;
    }

    public PlayerTag(UUID uuid) {
        this.player = CorexVelocity.getInstance().getServer().getPlayer(uuid).orElse(null);
    }

    public PlayerTag(String raw) {
        String clean = raw.toLowerCase().startsWith(PREFIX + "@") ? raw.substring(2) : raw;
        Player resolved;
        try {
            resolved = CorexVelocity.getInstance().getServer().getPlayer(UUID.fromString(clean)).orElse(null);
        } catch (IllegalArgumentException e) {
            resolved = CorexVelocity.getInstance().getServer().getPlayer(clean).orElse(null);
        }
        this.player = resolved;
    }

    public static void register() {
        BaseTagProcessor.registerBaseTag("player", (attribute) -> {
            if (attribute.hasParam()) {
                PlayerTag tag = new PlayerTag(attribute.getParam());
                return tag.isOnline() ? tag : null;
            }
            return (AbstractTag) attribute.getQueue().getPlayer();
        });

        ObjectFetcher.registerFetcher(PREFIX, (uuidStr) -> {
            PlayerTag tag = new PlayerTag(UUID.fromString(uuidStr));
            return tag.isOnline() ? tag : null;
        });

        TAG_PROCESSOR.registerTag(ElementTag.class, "name", (attribute, object) ->
                new ElementTag(object.player.getUsername()));

        TAG_PROCESSOR.registerTag(ElementTag.class, "uuid", (attribute, object) ->
                new ElementTag(object.player.getUniqueId().toString()));

        TAG_PROCESSOR.registerTag(ElementTag.class, "isOnline", (attribute, object) ->
                new ElementTag(object.isOnline()));

        TAG_PROCESSOR.registerTag(ElementTag.class, "ping", (attribute, object) ->
                new ElementTag(object.player.getPing()));

        TAG_PROCESSOR.registerTag(ElementTag.class, "server", (attribute, object) ->
                object.player.getCurrentServer()
                        .map(s -> new ElementTag(s.getServerInfo().getName()))
                        .orElse(null));

        TAG_PROCESSOR.registerTag(ElementTag.class, "address", (attribute, object) ->
                new ElementTag(object.player.getRemoteAddress().getAddress().getHostAddress()));

        TAG_PROCESSOR.registerTag(ElementTag.class, "hasPermission", (attribute, object) -> {
            if (!attribute.hasParam()) return null;
            return new ElementTag(object.player.hasPermission(attribute.getParam()));
        });
    }

    public Optional<Player> getPlayer() {
        return Optional.of(player);
    }

    @Override
    public UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override
    public String getName() {
        return player.getUsername();
    }

    @Override
    public boolean isOnline() { return true; }

    @Override
    public @NonNull String getPrefix() { return PREFIX; }

    @Override
    public @NonNull String identify() {
        return PREFIX + "@" + player.getUniqueId();
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return TAG_PROCESSOR.process(this, attribute);
    }

    @Override
    public @NonNull TagProcessor<PlayerTag> getProcessor() { return TAG_PROCESSOR; }

    @Override
    public @NotNull Adjustable duplicate() { return this; }

    @Override
    public @NotNull AbstractTag applyMechanism(@NotNull String mechanism, @NotNull AbstractTag value) {
        return MECHANISM_PROCESSOR.process(this, mechanism, value);
    }

    @Override
    public @NonNull MechanismProcessor<? extends AbstractTag> getMechanismProcessor() { return MECHANISM_PROCESSOR; }

    @Override
    public AbstractFlagTracker getFlagTracker() {
        File dbFile = CorexVelocity.getInstance().getDataFolder().toFile();
        return new SqlFlagTracker(dbFile, player.getUniqueId().toString());
    }

    @Override
    public @NonNull String getTestValue() { return "p@465876c1-2a15-4fc0-9f0b-97de13aa46f1"; }
}