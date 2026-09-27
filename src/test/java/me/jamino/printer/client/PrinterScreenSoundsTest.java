package me.jamino.printer.client;

import me.jamino.printer.registry.ModSounds;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PrinterScreenSoundsTest {
    private final List<PrinterScreenSounds.Cue> played = new ArrayList<>();
    private final List<PrinterScreenSounds.Cue> stopped = new ArrayList<>();
    private final PrinterScreenSounds sounds = new PrinterScreenSounds(cue -> {
        played.add(cue);
        return () -> stopped.add(cue);
    });

    @Test void previewUsesSpacedPassesAndDoesNotLoop() {
        sounds.start(false);
        assertEquals(1, played.size());
        ticks(4);
        assertEquals(1, played.size());
        ticks(1);
        assertEquals(2, played.size());
        assertTrue(played.get(1).pitch() > played.getFirst().pitch());
        ticks(200);
        assertEquals(2, played.size());
    }

    @Test void printFeedsPaperBeforeCarriageAndEjectsAfterIt() {
        sounds.start(true);
        assertEquals(ModSounds.PAPER_FEED, played.getFirst().sound());
        ticks(1);
        assertEquals(ModSounds.PRINT, played.getLast().sound());
        ticks(14);
        assertFalse(played.stream().anyMatch(cue -> cue.sound() == ModSounds.PAPER_EJECT));
        ticks(1);
        assertEquals(ModSounds.PAPER_EJECT, played.getLast().sound());
        int completedCount = played.size();
        ticks(200);
        assertEquals(completedCount, played.size(), "A print must not loop");
        sounds.stop();
        assertEquals(played, stopped, "Keep ownership of natural tails until close/replacement");
    }

    @Test void closingStopsAllLayersAndCancelsPendingPlayback() {
        sounds.start(true);
        ticks(5);
        assertTrue(played.size() > 1);
        var beforeClose = List.copyOf(played);
        sounds.stop();
        sounds.stop();
        ticks(200);
        assertEquals(beforeClose, played, "Pending layers must not play after close");
        assertEquals(beforeClose, stopped, "Stop each owned layer exactly once");
    }

    @Test void replacementCancelsOldSequenceAndRestartsTiming() {
        sounds.start(true);
        ticks(1);
        var previous = List.copyOf(played);
        sounds.start(false);
        assertEquals(previous, stopped);
        assertEquals(ModSounds.PREVIEW_LOAD, played.getLast().sound());
        int afterStart = played.size();
        ticks(4);
        assertEquals(afterStart, played.size());
        ticks(1);
        assertEquals(afterStart + 1, played.size());
        ticks(200);
        assertTrue(played.subList(previous.size(), played.size()).stream()
                .allMatch(cue -> cue.sound() == ModSounds.PREVIEW_LOAD));
    }

    private void ticks(int count) {
        for (int i = 0; i < count; i++) sounds.tick();
    }
}
