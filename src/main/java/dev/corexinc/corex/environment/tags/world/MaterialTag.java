package dev.corexinc.corex.environment.tags.world;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public class MaterialTag implements AbstractTag {

    private static final String prefix = "m";
    private final Material material;
    private final BlockData blockData;

    public static final TagProcessor<MaterialTag> PROCESSOR = new TagProcessor<>();

    public static void register() {
        BaseTagProcessor.registerBaseTag("material", attr -> new MaterialTag(attr.getParam()));

        ObjectFetcher.registerFetcher(prefix, MaterialTag::new);

        PROCESSOR.registerTag(ElementTag.class, "name", (attr, obj) -> new ElementTag(obj.material.name().toLowerCase()));
        PROCESSOR.registerTag(ElementTag.class, "isBlock", (attr, obj) -> new ElementTag(obj.material.isBlock()));
        PROCESSOR.registerTag(ElementTag.class, "isItem", (attr, obj) -> new ElementTag(obj.material.isItem()));
        PROCESSOR.registerTag(ElementTag.class, "isEdible", (attr, obj) -> new ElementTag(obj.material.isEdible()));
        PROCESSOR.registerTag(ElementTag.class, "maxStackSize", (attr, obj) -> new ElementTag(obj.material.getMaxStackSize()));
    }

    public MaterialTag(Material material) {
        this.material = material;
        this.blockData = material.isBlock() ? material.createBlockData() : null;
    }

    public MaterialTag(Block block) {
        this.material = block.getType();
        this.blockData = block.getBlockData();
    }

    public MaterialTag(String raw) {
        if (raw == null || raw.isEmpty()) {
            this.material = Material.AIR;
            this.blockData = material.createBlockData();
            return;
        }

        if (raw.toLowerCase().startsWith(prefix + "@")) {
            raw = raw.substring(2);
        }

        if (raw.equalsIgnoreCase("RANDOM")) {
            Material[] values = Material.values();
            this.material = values[new java.util.Random().nextInt(values.length)];
            this.blockData = this.material.isBlock() ? this.material.createBlockData() : null;
            return;
        }

        Material match = Material.matchMaterial(raw.toUpperCase());

        this.material = Objects.requireNonNullElse(match, Material.AIR);

        this.blockData = this.material.isBlock() ? this.material.createBlockData() : null;
    }

    public Material getMaterial() { return material; }
    public BlockData getBlockData() { return blockData; }

    @Override public @NonNull String getPrefix() { return prefix; }

    @Override
    public @NonNull String identify() {
        return prefix + "@" + material.name().toLowerCase();
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return PROCESSOR.process(this, attribute);
    }

    @Override
    public @NonNull TagProcessor<MaterialTag> getProcessor() {
        return PROCESSOR;
    }

    @Override
    public @NonNull String getTestValue() {
        return "m@stone";
    }
}