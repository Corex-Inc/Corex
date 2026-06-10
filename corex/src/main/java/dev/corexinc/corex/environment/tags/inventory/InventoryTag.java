package dev.corexinc.corex.environment.tags.inventory;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.engine.scripts.ScriptManager;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.PlayerIdentity;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.containers.InventoryContainer;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import dev.corexinc.corex.environment.tags.entity.EntityTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.tags.world.ItemTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import dev.corexinc.corex.environment.utils.BukkitSchedulerAdapter;
import dev.corexinc.corex.environment.utils.InventoryGuiTracker;
import dev.corexinc.corex.environment.utils.adapters.InventoryAdapter;
import dev.corexinc.corex.environment.utils.nms.NMSHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;

/* @doc object
 *
 * @Name InventoryTag
 * @Prefix inv
 *
 * @Format
 * Inventories come in two forms.
 * A generic inventory is built inline and identified as `inv@[gui=<boolean>;inventoryType=<type>;title=<text>;content=<list>]`.
 * A script inventory is a reference to an `inventory` script container, identified as `inv@<script_name>`.
 *
 * The `inventoryType` is any vanilla menu type key (e.g. `GENERIC_9X3`, `GENERIC_3X3`, `HOPPER`).
 * The `gui` property, when true, locks the inventory so that viewers cannot move or take items.
 *
 * @Description
 * An InventoryTag represents a renderable menu backed by a vanilla MenuType.
 * It carries the menu type, a title, a gui-lock flag, and a sparse slot to item mapping.
 * Script inventories resolve their contents through their container (definitions, slots, procedural items)
 * at the moment they are referenced or opened.
 */
public class InventoryTag implements AbstractTag {

    private static final String prefix = "inv";

    private String scriptName;
    private String typeName;
    private Component title = Component.empty();
    private boolean gui;
    private Inventory liveInventory;
    private final Map<Integer, ItemTag> contents = new LinkedHashMap<>();

    public static final TagProcessor<InventoryTag> TAG_PROCESSOR = new TagProcessor<>();

    public static void register() {
        BaseTagProcessor.registerBaseTag("inventory", attr -> attr.hasParam() ? new InventoryTag(attr.getParam()) : null);
        ObjectFetcher.registerFetcher(prefix, InventoryTag::new);

        /* @doc tag
         *
         * @Name gui
         * @RawName <InventoryTag.gui>
         * @Object InventoryTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether the inventory is gui-locked (viewers cannot move items).
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "gui", (attr, obj) -> new ElementTag(obj.gui)).ignoreTest();

        /* @doc tag
         *
         * @Name inventoryType
         * @RawName <InventoryTag.inventoryType>
         * @Object InventoryTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the menu type key of the inventory (e.g. GENERIC_9X3).
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "inventoryType", (attr, obj) ->
                obj.typeName != null ? new ElementTag(obj.typeName) : null).ignoreTest();

        /* @doc tag
         *
         * @Name title
         * @RawName <InventoryTag.title>
         * @Object InventoryTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the title of the inventory.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "title", (attr, obj) ->
                new ElementTag(LegacyComponentSerializer.legacySection().serialize(obj.title))).ignoreTest();

        /* @doc tag
         *
         * @Name contents
         * @RawName <InventoryTag.contents>
         * @Object InventoryTag
         * @ReturnType ListTag(ItemTag)
         * @NoArg
         * @Description
         * Returns the items inside the inventory, ordered by slot, with empty slots represented as air.
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "contents", (attr, obj) -> {
            ListTag result = new ListTag();
            if (obj.liveInventory != null) {
                for (ItemStack stack : obj.liveInventory.getContents()) {
                    result.addObject(stack != null ? new ItemTag(stack) : new ItemTag("air"));
                }
                return result;
            }
            int count = obj.populatedCount();
            for (int slot = 0; slot < count; slot++) {
                ItemTag item = obj.contents.get(slot);
                result.addObject(item != null ? item : new ItemTag("air"));
            }
            return result;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name holder
         * @RawName <InventoryTag.holder>
         * @Object InventoryTag
         * @ReturnType PlayerTag, EntityTag, LocationTag
         * @NoArg
         * @Description
         * Returns the holder of the inventory - a PlayerTag for player inventories,
         * an EntityTag for entity-held inventories, or a LocationTag for block containers.
         * Returns null for generic inventories that have no Bukkit holder.
         */
        TAG_PROCESSOR.registerTag(AbstractTag.class, "holder", (attr, obj) -> obj.holder()).ignoreTest();

        /* @doc tag
         *
         * @Name script
         * @RawName <InventoryTag.script>
         * @Object InventoryTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the name of the backing inventory script container, or null for a generic inventory.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "script", (attr, obj) ->
                obj.scriptName != null ? new ElementTag(obj.scriptName) : null).ignoreTest();
    }

    public InventoryTag(String scriptName, String typeName, Component title,
                        boolean gui, Map<Integer, ItemTag> contents) {
        this.scriptName = scriptName;
        this.typeName = typeName;
        this.title = title != null ? title : Component.empty();
        this.gui = gui;
        if (contents != null) this.contents.putAll(contents);
    }

    public InventoryTag(Inventory liveInventory) {
        this.liveInventory = liveInventory;
    }

    public InventoryTag(String raw) {
        String clean = raw;
        if (clean.toLowerCase().startsWith(prefix + "@")) clean = clean.substring(prefix.length() + 1);
        if (clean.startsWith("[") && clean.endsWith("]")) clean = clean.substring(1, clean.length() - 1);

        UUID holderId = tryUuid(clean);
        if (holderId != null) {
            Player holder = Bukkit.getPlayer(holderId);
            if (holder != null) {
                this.liveInventory = holder.getInventory();
                return;
            }
        }

        if (clean.contains("=")) {
            parseGeneric(clean);
        } else {
            loadReference(clean);
        }
    }

    @Nullable
    private static UUID tryUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    @Nullable
    private AbstractTag holder() {
        if (liveInventory == null) return null;
        InventoryHolder holder = liveInventory.getHolder();
        if (holder instanceof Player player) return new PlayerTag(player);
        if (holder instanceof Entity entity) return new EntityTag(entity);
        if (holder instanceof BlockState blockState) return new LocationTag(blockState.getLocation());
        return null;
    }

    private void parseGeneric(String raw) {
        MapTag properties = new MapTag(raw);

        this.typeName = properties.getObject("inventoryType") != null
                ? properties.getObject("inventoryType").identify().toUpperCase()
                : "GENERIC_9X3";

        AbstractTag titleTag = properties.getObject("title");
        if (titleTag != null) this.title = titleTag.asComponent();

        AbstractTag guiTag = properties.getObject("gui");
        this.gui = guiTag instanceof ElementTag element && element.asBoolean();

        AbstractTag contentTag = properties.getObject("content");
        if (contentTag != null) {
            ListTag list = contentTag instanceof ListTag listTag ? listTag : new ListTag(contentTag.identify());
            int slot = 0;
            for (AbstractTag entry : list.getList()) {
                contents.put(slot++, entry instanceof ItemTag item ? item : new ItemTag(entry.identify()));
            }
        }
    }

    private void loadReference(String name) {
        if (!(ScriptManager.getContainer(name) instanceof InventoryContainer container)) {
            Debugger.error("Inventory script '" + name + "' does not exist.");
            return;
        }
        InventoryTag built = container.build(new MapTag(), null);
        this.scriptName = name;
        this.typeName = built.typeName;
        this.title = built.title;
        this.gui = built.gui;
        this.contents.putAll(built.contents);
    }

    public InventoryTag rebuild(MapTag overrides, @Nullable PlayerIdentity player) {
        if (scriptName == null) return this;
        if (!(ScriptManager.getContainer(scriptName) instanceof InventoryContainer container)) return this;
        return container.build(overrides, player);
    }

    public void open(Player player) {
        if (liveInventory != null) {
            SchedulerAdapter.get().runAt(BukkitSchedulerAdapter.toPosition(player.getLocation()), () -> {
                player.openInventory(liveInventory);
                if (gui) InventoryGuiTracker.lock(liveInventory);
            });
            return;
        }
        InventoryAdapter adapter = NMSHandler.get().get(InventoryAdapter.class);
        if (adapter == null) {
            Debugger.error("No inventory adapter available for this server version.");
            return;
        }

        Map<Integer, ItemStack> items = new LinkedHashMap<>();
        contents.forEach((slot, item) -> {
            if (item != null) items.put(slot, item.getItemStack());
        });

        String type = typeName != null ? typeName : "GENERIC_9X3";
        SchedulerAdapter.get().runAt(BukkitSchedulerAdapter.toPosition(player.getLocation()), () -> {
            Inventory opened = adapter.openMenu(player, type, title, items);
            if (opened == null) {
                Debugger.error("Cannot open inventory with an unknown menu type '" + type + "'.");
                return;
            }
            if (gui) InventoryGuiTracker.lock(opened);
        });
    }

    private int populatedCount() {
        int highest = -1;
        for (int slot : contents.keySet()) highest = Math.max(highest, slot);
        return highest + 1;
    }

    public boolean isScript() {
        return scriptName != null;
    }

    public boolean isLive() {
        return liveInventory != null;
    }

    @Nullable
    public Inventory getLiveInventory() {
        return liveInventory;
    }

    @Nullable
    public Location getHolderLocation() {
        if (liveInventory == null) return null;
        InventoryHolder holder = liveInventory.getHolder();
        if (holder instanceof Player player) return player.getLocation();
        if (holder instanceof Entity entity) return entity.getLocation();
        if (holder instanceof BlockState blockState) return blockState.getLocation();
        return null;
    }

    @Override
    public @NonNull String getPrefix() {
        return prefix;
    }

    @Override
    public @NonNull String identify() {
        if (liveInventory != null) {
            return liveInventory.getHolder() instanceof Player player
                    ? prefix + "@" + player.getUniqueId()
                    : prefix + "@live";
        }
        if (scriptName != null) return prefix + "@" + scriptName;

        StringJoiner joiner = new StringJoiner(";");
        joiner.add("gui=" + gui);
        if (typeName != null) joiner.add("inventoryType=" + typeName);
        joiner.add("title=" + LegacyComponentSerializer.legacySection().serialize(title));

        if (!contents.isEmpty()) {
            ListTag list = new ListTag();
            int count = populatedCount();
            for (int slot = 0; slot < count; slot++) {
                ItemTag item = contents.get(slot);
                list.addObject(item != null ? item : new ItemTag("air"));
            }
            joiner.add("content=" + list.identify());
        }

        return prefix + "@[" + joiner + "]";
    }

    @Override
    public @Nullable AbstractTag getAttribute(@NonNull Attribute attribute) {
        return TAG_PROCESSOR.process(this, attribute);
    }

    @Override
    public @NonNull TagProcessor<InventoryTag> getProcessor() {
        return TAG_PROCESSOR;
    }

    @Override
    public @NonNull String getTestValue() {
        return "inv@[gui=true;inventoryType=GENERIC_9X3;title=Test;content=li@stone|apple]";
    }
}
