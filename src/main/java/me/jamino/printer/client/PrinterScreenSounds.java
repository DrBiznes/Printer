package me.jamino.printer.client;

import me.jamino.printer.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Finite, screen-owned sequences of vanilla sounds; timing uses client ticks, never frames. */
final class PrinterScreenSounds {
    record Cue(int tick, Holder<SoundEvent> sound, float pitch, float volume) {}

    // Two light scanner passes, rising in pitch. This acknowledges a load, not its completion.
    private static final List<Cue> PREVIEW = List.of(
            new Cue(0, ModSounds.PREVIEW_LOAD, 1.35F, 0.20F),
            new Cue(5, ModSounds.PREVIEW_LOAD, 1.65F, 0.15F));

    // Paper pickup, alternating carriage strokes, then a soft sheet rustle (last onset at 0.8s).
    private static final List<Cue> PRINT = List.of(
            new Cue(0, ModSounds.PAPER_FEED, 1.35F, 0.24F),
            new Cue(1, ModSounds.PRINT, 1.55F, 0.24F),
            new Cue(5, ModSounds.PRINT_RETURN, 1.80F, 0.20F),
            new Cue(9, ModSounds.PRINT, 1.65F, 0.22F),
            new Cue(13, ModSounds.PRINT_RETURN, 1.90F, 0.16F),
            new Cue(16, ModSounds.PAPER_EJECT, 0.85F, 0.28F));

    private final Function<Cue, Runnable> play;
    private final List<Runnable> playing = new ArrayList<>();
    private List<Cue> sequence = List.of();
    private int age;
    private int next;

    PrinterScreenSounds() { this(PrinterScreenSounds::playCue); }

    /** Playback returns a stop action so tests can exercise timing/cancellation without OpenAL. */
    PrinterScreenSounds(Function<Cue, Runnable> play) { this.play = play; }

    static void button(float pitch) {
        playCue(new Cue(0, ModSounds.BUTTON, pitch, 0.30F));
    }

    private static Runnable playCue(Cue cue) {
        var manager = Minecraft.getInstance().getSoundManager();
        var sound = SimpleSoundInstance.forUI(cue.sound().value(), cue.pitch(), cue.volume());
        manager.play(sound);
        return () -> manager.stop(sound);
    }

    void start(boolean printing) {
        stop();
        sequence = printing ? PRINT : PREVIEW;
        playDue();
    }

    void tick() {
        if (next >= sequence.size()) return;
        age++;
        playDue();
    }

    private void playDue() {
        while (next < sequence.size() && sequence.get(next).tick() <= age) {
            playing.add(play.apply(sequence.get(next++)));
        }
    }

    void stop() {
        // Own pending cues instead of playDelayed: stopping a sound in SoundManager does
        // not remove its delayed entry, so it could start after the screen closes.
        sequence = List.of();
        next = 0;
        age = 0;
        playing.forEach(Runnable::run);
        playing.clear();
    }
}
