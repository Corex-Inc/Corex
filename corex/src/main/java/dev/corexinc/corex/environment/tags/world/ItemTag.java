package dev.corexinc.corex.environment.tags.world;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.MechanismProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Adjustable;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.containers.ItemContainer;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import dev.corexinc.corex.environment.utils.adapters.VersionController;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ItemTag implements AbstractTag, Adjustable {

    private static final String prefix = "i";
    private ItemStack item;

    public static final TagProcessor<ItemTag> TAG_PROCESSOR = new TagProcessor<>();
    public static final MechanismProcessor<ItemTag> MECHANISM_PROCESSOR = new MechanismProcessor<>();
    private String scriptName;

    @SuppressWarnings("unchecked")
    public static void register() {
        BaseTagProcessor.registerBaseTag("item", attr -> new ItemTag(attr.getParam()));
        ObjectFetcher.registerFetcher(prefix, ItemTag::new);

        /* @doc tag
         *
         * @Name material
         * @RawName <ItemTag.material>
         * @Object ItemTag
         * @ReturnType MaterialTag
         * @Mechanism ItemTag.material
         * @NoArg
         * @Description
         * Returns the MaterialTag that is the basis of the item.
         * EG, a stone with lore and a display name, etc. will return only "m@stone".
         *
         * @Implements ItemTag.material
         */
        TAG_PROCESSOR.registerTag(MaterialTag.class, "material", (attr, obj) -> new MaterialTag(obj.item.getType()));

        /* @doc tag
         *
         * @Name amount
         * @RawName <ItemTag.amount>
         * @Object ItemTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Mechanism ItemTag.amount
         * @Description
         * Returns the number of items in the ItemTag's itemstack.
         *
         * @Implements ItemTag.quantity
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "amount", (attr, obj) -> new ElementTag(obj.item.getAmount()));

        /* @doc tag
         *
         * @Name customModelData
         * @RawName <ItemTag.customModelData>
         * @Object ItemTag
         * @ReturnType ElementTag(Number), MapTag
         * @NoArg
         * @Mechanism ItemTag.customModelData
         * @Description
         * Controls the custom model data ID number of the item.
         * Use with no input to remove the custom model data.
         *
         * @Implements ItemTag.custom_model_data
         */
        TAG_PROCESSOR.registerTag(AbstractTag.class, "customModelData", (attr, obj) -> {
            Object data = VersionController.getCustomModelDataAdapter().getCustomModelData(obj.item);
            if (data instanceof Map<?, ?> map) {
                return new MapTag((Map<String, ?>) map);
            } else if (data instanceof Integer integer) {
                return new ElementTag(integer);
            }
            return null;
        }).ignoreTest();

        /* @doc mechanism
         *
         * @Name material
         * @Object ItemTag
         * @Input MaterialTag
         * @Description
         * Changes the item's material to the given material.
         * Only copies the base material type, not any advanced block-data material properties.
         * Note that this may cause some properties of the item to be lost.
         *
         * @Implements ItemTag.material
         */
        MECHANISM_PROCESSOR.registerMechanism("material", (obj, val) -> {
            Material targetMaterial = Material.matchMaterial(val.identify().toUpperCase());
            if (targetMaterial != null) obj.item = obj.item.withType(targetMaterial);
            return obj;
        });

        /* @doc mechanism
         *
         * @Name amount
         * @Object ItemTag
         * @Input ElementTag(Number)
         * @Description
         * Changes the number of items in this stack.
         *
         * @Implements ItemTag.quantity
         */
        MECHANISM_PROCESSOR.registerMechanism("amount", (obj, val) -> {
            if (val instanceof ElementTag el) obj.item.setAmount(Math.max(1, el.asInt()));
            return obj;
        });

        /* @doc mechanism
         *
         * @Name displayName
         * @Object ItemTag
         * @Input ElementTag
         * @Description
         * Changes the item's display name.
         * Give no input to remove the item's display name.
         *
         * @Implements ItemTag.display
         */
        MECHANISM_PROCESSOR.registerMechanism("displayName", (obj, val) -> {
            ItemMeta meta = obj.item.getItemMeta();
            if (meta != null) {
                Component name = val.asComponent()
                        .colorIfAbsent(net.kyori.adventure.text.format.NamedTextColor.WHITE)
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
                meta.displayName(name);
                obj.item.setItemMeta(meta);
            }
            return obj;
        });

        /* @doc mechanism
         *
         * @Name lore
         * @Object ItemTag
         * @Input ListTag
         * @Description
         * Sets the item's lore.
         *
         * @Implements ItemTag.lore
         */
        MECHANISM_PROCESSOR.registerMechanism("lore", (obj, val) -> {
            ItemMeta meta = obj.item.getItemMeta();
            if (meta != null) {
                List<Component> lore = new ArrayList<>();
                if (val instanceof ListTag listTag) {
                    for (AbstractTag tag : listTag.getList()) {
                        lore.add(tag.asComponent()
                                .colorIfAbsent(net.kyori.adventure.text.format.NamedTextColor.WHITE)
                                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                    }
                } else {
                    lore.add(val.asComponent()
                            .colorIfAbsent(net.kyori.adventure.text.format.NamedTextColor.WHITE)
                            .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                }
                meta.lore(lore);
                obj.item.setItemMeta(meta);
            }
            return obj;
        });

        /* @doc mechanism
         *
         * @Name customModelData
         * @object ItemTag
         * @input ElementTag(Number)
         * @description
         * Controls the custom model data ID number of the item.
         * Use with no input to remove the custom model data.
         *
         * @Implements ItemTag.custom_model_data
         */
        MECHANISM_PROCESSOR.registerMechanism("customModelData", (obj, val) -> {
            try {
                VersionController.getCustomModelDataAdapter().applyCustomModelData(obj.item, val);
            } catch (Exception ignored) {}
            return obj;
        });

        /* @doc mechanism
         *
         * @Name customModel
         * @object ItemTag
         * @input ElementTag
         * @description
         * Sets the custom model for the item using a NamespacedKey.
         * This is an alternative to custom_model_data, and generally preferred.
         *
         * @Implements ItemTag.customModel
         */
        MECHANISM_PROCESSOR.registerMechanism("customModel", (obj, val) -> {
            ItemMeta meta = obj.item.getItemMeta();
            if (meta != null) {
                NamespacedKey key = NamespacedKey.fromString(val.identify());
                if (key != null) meta.setItemModel(key);
                obj.item.setItemMeta(meta);
            }
            return obj;
        });
    }

    public ItemTag(ItemStack item) {
        this.item = item != null ? item.clone() : null;
    }

    @Override
    public Adjustable duplicate() {
        return new ItemTag(this.item.clone());
    }

    public ItemTag(String raw) {
        String cleanRaw = raw.toLowerCase().startsWith(prefix + "@") ? raw.substring(2) : raw;
        int bracketStart = cleanRaw.indexOf('[');

        if (bracketStart > 0 && cleanRaw.endsWith("]")) {
            String materialPart = cleanRaw.substring(0, bracketStart);
            String propertiesPart = cleanRaw.substring(bracketStart + 1, cleanRaw.length() - 1);

            initBaseItem(materialPart);

            List<String> pairs = ObjectFetcher.splitIgnoringBrackets(propertiesPart, ';');
            for (String pair : pairs) {
                int eqIndex = pair.indexOf('=');
                if (eqIndex > 0) {
                    String mechName = pair.substring(0, eqIndex).trim();
                    String mechValue = pair.substring(eqIndex + 1).trim();
                    applyMechanism(mechName, ObjectFetcher.pickObject(mechValue));
                }
            }
        } else {
            initBaseItem(cleanRaw);
        }
    }

    private void initBaseItem(String name) {
        ItemTag cached = ItemContainer.ItemCache.get(name);
        if (cached != null) {
            this.item = cached.getItemStack().clone();
            this.scriptName = cached.scriptName;
        } else {
            Material parsedMaterial = Material.matchMaterial(name.toUpperCase());
            this.item = parsedMaterial != null ? new ItemStack(parsedMaterial) : null;
        }
    }

    public void setScriptName(String name) {
        this.scriptName = name;
    }

    public ItemStack getItemStack() { return item; }

    @Override public @NonNull String getPrefix() { return prefix; }

    @Override
    public @NonNull String identify() {
        StringBuilder stringBuilder = new StringBuilder(prefix + "@");

        stringBuilder.append(Objects.requireNonNullElseGet(scriptName, () -> item.getType().name()).toLowerCase());

        List<String> propertiesList = new ArrayList<>();

        if (item.getAmount() > 1) {
            propertiesList.add("amount=" + item.getAmount());
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName() && meta.displayName() != null) {
                String nameStr = LegacyComponentSerializer.legacySection().serialize(Objects.requireNonNull(meta.displayName()));
                propertiesList.add("displayName=" + nameStr.replace(";", "\\;").replace("=", "\\=")); // FIX #2: имя механизма приведено к camelCase
            }
        }

        if (!propertiesList.isEmpty()) {
            stringBuilder.append("[").append(String.join(";", propertiesList)).append("]");
        }

        return stringBuilder.toString();
    }

    @Override public @Nullable AbstractTag getAttribute(@NonNull Attribute attribute) {
        return TAG_PROCESSOR.process(this, attribute);
    }

    @Override public @NonNull TagProcessor<ItemTag> getProcessor() { return TAG_PROCESSOR; }

    @Override public @Nullable MechanismProcessor<? extends AbstractTag> getMechanismProcessor() {
        return MECHANISM_PROCESSOR;
    }

    @Override public @NotNull AbstractTag applyMechanism(@NotNull String mechanism, @NotNull AbstractTag value) {
        return MECHANISM_PROCESSOR.process(this, mechanism, value);
    }

    @Override public @NonNull String getTestValue() { return "i@stone[customModelData=1]"; }
}