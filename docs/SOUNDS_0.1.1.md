# Printer GUI audio — 0.1.1

The radio GUI in `Analog-Audio-0.1.5-hotfix.1.jar` uses vanilla UI clicks, separate custom switch/dial cues, and pitch tied to its volume dial. Its `sounds.json` references its own recordings; it is an interaction reference, not a vanilla-only implementation. Printer follows the distinction between controls and mechanical actions without copying Analog Audio code or assets or adding a dependency.

All Printer effects now reference Minecraft sound events at runtime. No audio recordings, synthesized clips, audio generator, or audio build dependencies are needed. Resource packs that replace these Minecraft sounds also affect Printer.

| Action | Vanilla ingredients | Arrangement |
| --- | --- | --- |
| Button | Stone-button click | Quieter, pitched up; lower/higher size controls and lighter palette clicks. Selecting the current color is silent. |
| Preview load | Piston contraction | Two quiet scanner-like passes, 5 ticks apart, rising in pitch. |
| Print | Loom result, piston extension/contraction, book page | Paper pickup, four alternating pitched carriage strokes, then a softer sheet rustle. Last onset at tick 16 (0.8 seconds at 20 TPS), plus the vanilla sound's natural tail. |

Pitch and volume are tuned per layer in `PrinterScreenSounds.java`. `assets/printer/sounds.json` uses `type: event` aliases into the `minecraft` namespace, retaining Printer's translated subtitles. Multiple entries in a sound definition would choose random alternatives; the client sequence explicitly schedules each layer instead.

These are finite action cues, not progress timers or success confirmations. The server triggers preview audio when it accepts a URL load or local-upload reservation, and print audio after validating image, supplies, and background. Upload-to-decode does not replay the cue. Later processing failures retain their error status.

Job acknowledgements go only to the initiating player's menu ID. Client screen ticks schedule layers independently of frame rate. Closing/canceling stops every started layer and discards all pending layers; starting another operation first stops the previous sequence. GUI resizing does not replay it. Redstone printing stays silent. Sounds use Minecraft's Master volume and Printer subtitles.

## Verification

Automated tests check vanilla event IDs, translated subtitles, absence of bundled audio, finite playback, tick spacing, interruption, and cancellation of pending layers. Build verification also checks the JAR for audio files.

In-game listening remains a maintainer check:

- Compare preview and printing at normal Master volume against vanilla UI sounds.
- Load with mouse, Enter, and local file picker; test cancellation and rejected requests.
- Print in color/B&W; check missing supplies and blocked output stay silent.
- Close/reopen during playback, resize the window, and start printing after loading; check no lingering layers.
- With two clients, confirm only the initiating GUI hears the cue; redstone stays silent.
- Check subtitles, Master mute, and a resource pack that replaces the referenced sounds.
