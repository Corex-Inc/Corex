package dev.corexinc.corex.nms.v1_21_9;

import dev.corexinc.corex.environment.utils.adapters.DialogAdapter;
import dev.corexinc.corex.environment.utils.dialog.DialogSpec;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class DialogAdapterImpl implements DialogAdapter {

    @Override
    public void show(Player player, DialogSpec spec, Callback callback) {
        player.showDialog(buildDialog(spec, callback));
    }

    @Override
    public void close(Player player) {
        player.closeDialog();
    }

    private Dialog buildDialog(DialogSpec spec, Callback callback) {
        return Dialog.create(builder -> builder.empty()
                .base(buildBase(spec))
                .type(buildType(spec, callback)));
    }

    private DialogBase buildBase(DialogSpec spec) {
        DialogBase.Builder builder = DialogBase.builder(spec.title)
                .canCloseWithEscape(spec.canCloseWithEscape)
                .afterAction(afterAction(spec.afterAction))
                .body(buildBodies(spec))
                .inputs(buildInputs(spec));
        if (spec.externalTitle != null) builder.externalTitle(spec.externalTitle);
        return builder.build();
    }

    private DialogBase.DialogAfterAction afterAction(String value) {
        return switch (value) {
            case "none" -> DialogBase.DialogAfterAction.NONE;
            case "wait_for_response" -> DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE;
            default -> DialogBase.DialogAfterAction.CLOSE;
        };
    }

    private List<DialogBody> buildBodies(DialogSpec spec) {
        List<DialogBody> bodies = new ArrayList<>();
        for (DialogSpec.Body body : spec.bodies) {
            if ("item".equals(body.type) && body.item != null) {
                if (body.description != null) {
                    bodies.add(DialogBody.item(body.item).description(DialogBody.plainMessage(body.description)).build());
                } else {
                    bodies.add(DialogBody.item(body.item).build());
                }
            } else {
                bodies.add(DialogBody.plainMessage(body.text, body.width));
            }
        }
        return bodies;
    }

    private List<DialogInput> buildInputs(DialogSpec spec) {
        List<DialogInput> inputs = new ArrayList<>();
        for (DialogSpec.Input input : spec.inputs) {
            switch (input.type) {
                case "bool" -> inputs.add(DialogInput.bool(input.key, input.label,
                        input.initialBool, input.onTrue, input.onFalse));
                case "number" -> {
                    if (input.initialNumber != null) {
                        inputs.add(DialogInput.numberRange(input.key, input.label, input.min, input.max)
                                .step(input.step).initial(input.initialNumber).build());
                    } else {
                        inputs.add(DialogInput.numberRange(input.key, input.label, input.min, input.max)
                                .step(input.step).build());
                    }
                }
                case "single_option" -> {
                    List<SingleOptionDialogInput.OptionEntry> options = new ArrayList<>();
                    for (DialogSpec.Option option : input.options) {
                        options.add(SingleOptionDialogInput.OptionEntry.create(option.id, option.display, option.initial));
                    }
                    inputs.add(DialogInput.singleOption(input.key, input.label, options).build());
                }
                default -> inputs.add(DialogInput.text(input.key, input.label)
                        .maxLength(input.maxLength)
                        .initial(input.initial)
                        .build());
            }
        }
        return inputs;
    }

    private DialogType buildType(DialogSpec spec, Callback callback) {
        List<ActionButton> buttons = new ArrayList<>();
        for (DialogSpec.Button button : spec.buttons) {
            buttons.add(buildButton(button, spec, callback));
        }

        return switch (spec.type) {
            case "confirmation" -> DialogType.confirmation(
                    buttons.isEmpty() ? defaultButton("Yes") : buttons.get(0),
                    buttons.size() < 2 ? defaultButton("No") : buttons.get(1));
            case "multi" -> DialogType.multiAction(buttons).columns(spec.columns).build();
            case "list" -> {
                List<ActionButton> entries = new ArrayList<>(buttons);
                for (DialogSpec child : spec.children) {
                    Dialog childDialog = buildDialog(child, callback);
                    entries.add(ActionButton.builder(child.title)
                            .action(DialogAction.customClick(
                                    (view, audience) -> {
                                        if (audience instanceof Player player) player.showDialog(childDialog);
                                    },
                                    ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).build()))
                            .build());
                }
                yield DialogType.multiAction(entries).columns(spec.columns).build();
            }
            default -> buttons.isEmpty() ? DialogType.notice() : DialogType.notice(buttons.getFirst());
        };
    }

    private ActionButton buildButton(DialogSpec.Button button, DialogSpec spec, Callback callback) {
        DialogAction action = switch (button.action) {
            case "script" -> DialogAction.customClick(
                    (view, audience) -> {
                        if (audience instanceof Player player) {
                            callback.onButton(player, spec.name, button.id, readResponses(spec, view));
                        }
                    },
                    ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).build());
            case "command" -> DialogAction.staticAction(ClickEvent.runCommand(button.value));
            case "url" -> DialogAction.staticAction(ClickEvent.openUrl(button.value));
            case "clipboard" -> DialogAction.staticAction(ClickEvent.copyToClipboard(button.value));
            default -> null;
        };

        ActionButton.Builder builder = ActionButton.builder(button.label).width(button.width);
        if (button.tooltip != null) builder.tooltip(button.tooltip);
        if (action != null) builder.action(action);
        return builder.build();
    }

    private ActionButton defaultButton(String label) {
        return ActionButton.builder(Component.text(label)).build();
    }

    private Map<String, String> readResponses(DialogSpec spec, DialogResponseView view) {
        Map<String, String> responses = new LinkedHashMap<>();
        for (DialogSpec.Input input : spec.inputs) {
            switch (input.type) {
                case "bool" -> {
                    Boolean value = view.getBoolean(input.key);
                    if (value != null) responses.put(input.key, String.valueOf(value));
                }
                case "number" -> {
                    Float value = view.getFloat(input.key);
                    if (value != null) responses.put(input.key, value % 1 == 0 ? String.valueOf(value.intValue()) : String.valueOf(value));
                }
                default -> {
                    String value = view.getText(input.key);
                    if (value != null) responses.put(input.key, value);
                }
            }
        }
        return responses;
    }
}
