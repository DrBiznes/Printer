# Printer 0.1.1

This update combines Minecraft-based printer audio with Phantom Membrane background removal. It targets Minecraft 1.21.1 / NeoForge 21.1.248 or later 21.1 patches; install matching Printer 0.1.1 versions on client and server.

## Phantom Membranes

Right-click a placed Image with a Phantom Membrane in either hand to permanently remove its printer-selected background and all canvas backing, including the sides and unused margins. Survival consumes one membrane on success; creative consumes none. Repeated clicks and unavailable artwork consume none. Adventure/spectator players cannot change the image.

Color and B&W prints retain an additional background-free PNG made from the same resized artwork, including its original alpha and soft edges. Artwork with the same RGB as the selected paper stays intact. This removes the printer's paper color; it does not extract a subject or erase opaque backgrounds inside a JPEG or other source image. Fully opaque artwork reuses the ordinary PNG, avoiding duplicate storage. Both variants must fit the server storage limit before a print consumes supplies.

The transparent state and artwork reference synchronize to clients and persist through entity saves, dropped/picked Image items, re-placement, item frames displaying those items, and Photobooks. World/item renderers blend alpha; transparent Photobook previews show the page underneath. Tooltips report a transparent background.

**0.1.1 requires a new world.** Earlier saves and printed Images are unsupported. No migration, optional transparency metadata defaults, or legacy-image reprint path is included.

## Audio

See [SOUNDS_0.1.1.md](SOUNDS_0.1.1.md). Every sound references Minecraft events at runtime, with no bundled recordings or audio-generation dependencies.

## Verification

Automated verification (2026-09-27): all **131 unit tests** and **12 server GameTests** pass. The ordinary `test build --offline` succeeds. The packaged `build/libs/printer-0.1.1.jar` reports version 0.1.1, includes both features, and contains no audio files or GameTest classes/fixtures. Server tests use the isolated `build/gametest-0.1.1-run` test world, not a live development save.

Automated coverage includes source alpha and same-color artwork, color/B&W variants, metadata codecs, canvas omission, tooltip state, and operation sound cancellation. Server GameTests cover printing, membrane consumption, both hands, creative/adventure/spectator behavior, unavailable artwork, saving, drops, and re-placement.

Manual checks still needed on the packaged JAR:

- Place a PNG with fully transparent and partly transparent edges on each wall orientation. Print it on a colored background, use a membrane, and check the world is visible through the original transparent pixels with no colored fringe, backing, or margins.
- Repeat with B&W ink, fully opaque artwork, multiple overlapping transparent displays, an item frame holding an already-treated Image, and a Photobook.
- Check a second client sees the change immediately; reconnect/restart and break/re-place the image.
- Listen to the preview/printing sequences at normal volume and test GUI closing/replacement during playback.

Automated checks do not certify in-game visual quality or sound balance.
