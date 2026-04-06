package dev.corexinc.corex.environment.tags.world;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.MechanismProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.containers.ItemContainer;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
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
import java.util.Objects;

public class ItemTag implements AbstractTag {

    private static final String prefix = "i";
    private ItemStack item;

    public static final TagProcessor<ItemTag> TAG_PROCESSOR = new TagProcessor<>();
    public static final MechanismProcessor<ItemTag> MECHANISM_PROCESSOR = new MechanismProcessor<>();
    private String scriptName = null;

    public static void register() {
        BaseTagProcessor.registerBaseTag("item", attr -> new ItemTag(attr.getParam()));
        ObjectFetcher.registerFetcher(prefix, ItemTag::new);

        TAG_PROCESSOR.registerTag(MaterialTag.class, "material", (attr, obj) -> new MaterialTag(obj.item.getType()));
        TAG_PROCESSOR.registerTag(ElementTag.class, "amount", (attr, obj) -> new ElementTag(obj.item.getAmount()));
        TAG_PROCESSOR.registerTag(ElementTag.class, "customModelData", (attr, obj) -> {
            ItemMeta meta = obj.item.getItemMeta();
            return new ElementTag(meta != null && meta.hasCustomModelData() ? meta.getCustomModelData() : 0);
        });

        MECHANISM_PROCESSOR.registerMechanism("material", (obj, val) -> {
            Material targetMaterial = Material.matchMaterial(val.identify().toUpperCase());
            if (targetMaterial != null) obj.item.withType(targetMaterial);
            return obj;
        });

        MECHANISM_PROCESSOR.registerMechanism("amount", (obj, val) -> {
            if (val instanceof ElementTag el) obj.item.setAmount(Math.max(1, el.asInt()));
            return obj;
        });

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

        MECHANISM_PROCESSOR.registerMechanism("customModelData", (obj, val) -> {
            ItemMeta meta = obj.item.getItemMeta();
            if (meta != null && val instanceof ElementTag el) {
                meta.setCustomModelData(el.asInt());
                obj.item.setItemMeta(meta);
            }
            return obj;
        });

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
        this.item = item != null ? item.clone() : new ItemStack(Material.AIR);
    }

    public ItemTag(String raw) {
        if (raw == null || raw.isEmpty()) {
            this.item = new ItemStack(Material.AIR);
            return;
        }

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
            this.item = new ItemStack(parsedMaterial != null ? parsedMaterial : Material.AIR);
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

        if (scriptName != null) {
            stringBuilder.append(scriptName.toLowerCase());
        } else {
            stringBuilder.append(item.getType().name().toLowerCase());
        }

        List<String> propertiesList = new ArrayList<>();

        if (item.getAmount() != 1) {
            propertiesList.add("amount=" + item.getAmount());
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName() && meta.displayName() != null) {
                String nameStr = LegacyComponentSerializer.legacySection().serialize(Objects.requireNonNull(meta.displayName()));
                propertiesList.add("displayname=" + nameStr.replace(";", "\\;").replace("=", "\\="));
            }
            if (meta.hasLore() && meta.lore() != null) {
                List<String> loreStrs = new ArrayList<>();
                for (Component comp : Objects.requireNonNull(meta.lore())) {
                    loreStrs.add(LegacyComponentSerializer.legacySection().serialize(comp).replace("|", "\\|"));
                }
                propertiesList.add("lore=li@" + String.join("|", loreStrs));
            }
            if (meta.hasCustomModelData()) {
                propertiesList.add("custommodeldata=" + meta.getCustomModelData());
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

    @Override public @NonNull String getTestValue() { return "i@stone"; }
}