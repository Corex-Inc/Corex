package dev.corexinc.corex.environment.events.implementation.block;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.events.AbstractEvent;
import dev.corexinc.corex.environment.events.EventData;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.NotePlayEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name BlockNotePlay
 *
 * @Events
 * noteblock plays note
 *
 * @Switches
 * instrument:<name> - Only process the event if a specific instrument was played (e.g. PIANO, BASS_DRUM, GUITAR).
 *
 * @Cancellable
 *
 * @Description
 * Fires when a Note Block plays a note.
 *
 * @Context
 * <context.location> - returns the LocationTag of the note block.
 * <context.instrument> - returns an ElementTag of the instrument name.
 * <context.tone> - returns the note tone played (A to G).
 * <context.octave> - returns the octave the note is played at (as a number).
 * <context.sharp> - returns a boolean indicating whether the note is sharp.
 * <context.pitch> - returns the computed pitch value (for use in playsound commands).
 *
 * @Usage
 * // Prevent guitars from playing.
 * on noteblock plays note instrument:GUITAR:
 * - return cancelled
 */
public class BlockNotePlayEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "BlockNotePlay";
    }

    @Override
    public @NotNull String getSyntax() {
        return "noteblock plays note";
    }

    @Override
    public void addScript(@NotNull EventData data) {
        scripts.add(data);
    }

    @Override
    public void initListener() {
        if (!isRegistered && !scripts.isEmpty()) {
            Bukkit.getPluginManager().registerEvents(this, Corex.getInstance());
            isRegistered = true;
        }
    }

    @EventHandler
    public void onNotePlay(NotePlayEvent event) {
        String instrumentName = event.getInstrument().name().toUpperCase();
        ContextTag context = null;

        for (EventData data : scripts) {
            String instSwitch = data.getSwitch("instrument");
            if (instSwitch != null && !instSwitch.equalsIgnoreCase(instrumentName) && !instSwitch.equals("*")) {
                continue;
            }

            if (context == null) {
                context = new ContextTag();
                context.put("location", new LocationTag(event.getBlock().getLocation()));
                context.put("instrument", new ElementTag(instrumentName));
                context.put("tone", new ElementTag(event.getNote().getTone().name()));
                context.put("octave", new ElementTag(event.getNote().getOctave()));
                context.put("sharp", new ElementTag(event.getNote().isSharped()));

                double pitch = Math.pow(2.0, (double) (event.getNote().getId() - 12) / 12.0);
                context.put("pitch", new ElementTag(pitch));
            }

            ScriptQueue queue = EventRegistry.fire(data, null, context);
            if (queue.isCancelled()) event.setCancelled(true);
        }
    }

    @Override
    public void reset() {
        isRegistered = false;
        scripts.clear();
    }
}