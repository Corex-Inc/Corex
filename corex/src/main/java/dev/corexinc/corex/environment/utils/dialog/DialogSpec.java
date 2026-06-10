package dev.corexinc.corex.environment.utils.dialog;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class DialogSpec {

    public String name = "";
    public String type = "notice";
    public Component title = Component.empty();
    public Component externalTitle;
    public boolean canCloseWithEscape = true;
    public String afterAction = "close";
    public int columns = 2;

    public final List<Body> bodies = new ArrayList<>();
    public final List<Input> inputs = new ArrayList<>();
    public final List<Button> buttons = new ArrayList<>();
    public final List<DialogSpec> children = new ArrayList<>();

    public static class Body {
        public String type = "message";
        public Component text = Component.empty();
        public int width = 200;
        public ItemStack item;
        public Component description;
    }

    public static class Input {
        public String type = "text";
        public String key = "";
        public Component label = Component.empty();
        public int maxLength = 32;
        public boolean multiline = false;
        public String initial = "";
        public boolean initialBool = false;
        public String onTrue = "Yes";
        public String onFalse = "No";
        public float min = 0;
        public float max = 100;
        public float step = 1;
        public Float initialNumber;
        public final List<Option> options = new ArrayList<>();
    }

    public static class Option {
        public String id = "";
        public Component display = Component.empty();
        public boolean initial = false;
    }

    public static class Button {
        public String id = "";
        public Component label = Component.empty();
        public Component tooltip;
        public int width = 150;
        public String action = "close";
        public String value = "";
        public boolean script = false;
    }
}
