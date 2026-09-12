# Printer

Printer is a NeoForge mod for Minecraft 1.21.1 that turns web or local images into independent, reusable image items. Images do not consume vanilla map IDs. They can be held, displayed in ordinary Minecraft item frames, or placed directly as borderless painting-like wall displays sized from the printed image.

Original 16×16 pixel art gives the printer a warm enamel case, timber trim, colored ink accents, and a tactile control panel inspired by Analog Audio.

## Gameplay

The mod adds three items:

- **Printer** — loads a URL, saves the last image as a reusable preset, and prints copies.
- **Image** — stores a content-addressed reference to server image data and renders dynamically.
- **Color Cartridge** — a three-use color ink supply. Vanilla ink sacs can also make monochrome prints.

Find them in the **Printer** creative tab. Hold **Shift** over any of the three items for workflow, supply, automation, or placement details. Printed Image tooltips show a title (or an untitled fallback) and block size; expanded details include source/texture dimensions, background color, and print mode.

Open the printer, enter a direct PNG, JPEG, WebP, GIF, BMP, TIFF, ICO, or TGA URL, and select **Load**, or select **Browse** to choose a local file. Local files are sent as bounded bytes to the server; their filesystem path never leaves the client. The printer detects the image resolution and aspect ratio, then selects and displays an automatic physical size with a maximum four-block long edge. The **−** and **+** controls resize the print in block units while keeping its aspect ratio; players never need to enter pixel dimensions. Choose a background fill from the **BG** swatches, which use friendly color names, add the displayed amount of paper and ink, then select **Print**. White remains the default. The selected swatch has a gold outline and updates the preview. Large prints consume one sheet of paper per occupied block. A printed item records its immutable variant hash, source dimensions, texture resolution, physical block size, background color, print mode, and title. Printed wall displays use no decorative border.

Use an Image on a wall to place a painting-style display. Placed prints render as shallow, borderless canvases with block-by-block lighting. Insert an Image into a normal Minecraft item frame for a map-like display.

The control panel includes a live image preview, recessed green URL and amber title fields, raised buttons with pressed/focus states, a size readout, supply-slot ghost outlines, status lights, and a complete player inventory drawer. Hover over the input slots for help. Press Enter in a text field to load the image. Print is enabled when the saved image, paper, ink, and output slot are ready.

Animated WebP/GIF files and multipage TIFF/ICO files use their first image. Transparency is printed against the selected background color, which defaults to white. The bundled TwelveMonkeys readers run in pure Java, including on dedicated servers; players do not need an extra codec mod. AVIF, HEIC, and SVG input are not supported. Missing or generic binary HTTP content types are allowed only when the file bytes decode as a supported image.

Monochrome printing converts only the artwork to dithered black and white, then applies the selected background as a flat color. Changing the background does not change the opaque artwork's ink pattern or add speckles to transparent areas; partially transparent edges remain softly blended. The canvas sides and back use the saved background color in both print modes. The live preview shows the source and background, not the monochrome conversion.

Version 0.1.0 is the first public release and intentionally allows breaking changes during development. It requires new worlds; pre-0.1.0 development worlds and old Printer/Image items are not supported. The current 0.1.0 build is a release candidate until the final art pass and exact-artifact smoke tests are signed off.

### Crafting

The Printer's shaped recipe is `III / RGP / SSS`: `I` is an iron ingot, `R` redstone, `G` a glass pane, `P` a piston, and `S` smooth stone. A Color Cartridge is shapeless: cyan, magenta, yellow, and black dye plus one iron nugget. Images are produced by the Printer, not a crafting recipe.

## Automation

Automation uses NeoForge's standard item-handler capability, so vanilla hoppers and compatible modded pipes, funnels, chutes, and belts can interact without a hard Create dependency.

- **Top:** color cartridge or ink-sac input.
- **Your right while looking at the front panel:** paper input.
- **Your left while looking at the front panel, or bottom:** image output.
- **Front and back:** no automated inventory access.
- **Redstone:** a rising edge prints the saved preset once. A constant signal does not repeatedly print.

The loaded preset is persisted with the block entity and is copied to the printer item when broken, allowing a configured machine to be moved and reused. Paper and ink are consumed only after a successful image job.

## Server safety and storage

Image downloading is server-authoritative. Requests are limited by protocol, MIME type, redirects, byte size, source dimensions, configured target dimensions, timeouts, and public-IP checks that reject loopback, private, link-local, and other non-public destinations. Image data is deduplicated by SHA-256 and stored separately from vanilla map data. Clients receive only requested images in bounded network chunks and keep an LRU texture cache.

The server config controls source-image limits, automatic and manually selectable block-size limits, download size, storage budget, timeout, and optional hostname allow/block lists.

## Build

Use Java 21 and run:

```powershell
.\gradlew.bat build
```

If VS Code reports a Gradle connection failure and the daemon log contains `Unable to establish loopback connection` followed by `UnixDomainSockets` / `Invalid argument: connect`, Java is failing to create a local socket in the Windows temporary directory. The workspace settings apply a socket-directory workaround to **new** integrated terminals.

This setting affects integrated terminals, not the environment of an already running Gradle extension. Open a new terminal before retrying the build.

Regenerate the original textures and a design contact sheet (`build/art-preview.png`) with Python and Pillow:

```powershell
python -m pip install Pillow
python tools\generate_programmer_art.py
```

## Roadmap

The active 0.1.0 release checklist and phased implementation plan are in
[PROJECT_PLAN.md](PROJECT_PLAN.md). The short version is:

- frame removal, selectable background color, source-dimension metadata, advancements, translation readiness, local upload, and summary/condition/behaviour tooltips are implemented;
- finish the separately deferred texture refinement and GUI texture pass, then sign off clean-instance gameplay against the exact candidate artifact;
- pursue full Ponder scenes for manual printing, hopper/redstone automation, and optional Create setups after 0.1.0.

The frozen [0.1.0 release contract](docs/RELEASE_0.1.0.md) records exact limits and final verification gates, and the [smoke-test matrix](docs/SMOKE_TEST_0.1.0.md) tracks release validation. English plus a [reviewed translation contribution workflow](docs/TRANSLATING.md) is the localization deliverable; all built-in messages are translation-ready.

Longer-term work includes full Ponder and optional Create scenes, configurable ink economy and multipart print tiling controls, an optional CC:Tweaked peripheral API, Create-specific visual polish where standard capability interop is insufficient, and ports for newer NeoForge versions and selected backports.

Version-specific Minecraft/NeoForge integration is kept at the edges of the project; image processing, references, storage, and print modes are separate service/data layers to make later ports less invasive.

See [CREDITS.md](CREDITS.md) for design references and licensing notes.

## Support and release testing

Report bugs through the [repository issue tracker](https://github.com/DrBiznes/Printer/issues). Include Printer/Minecraft/NeoForge versions, fresh-world reproduction steps, whether the server is dedicated, and a sanitized log. Do not include private image URLs, client file paths, or personal images. Other Minecraft versions, old development saves, and individual third-party transport mods are not certified.

Automated integration checks: `./gradlew.bat runGameTestServer -PprinterGameTests --offline`. For isolated development-client UI checks, use `./gradlew.bat runClient -PprinterSmokeClient --offline`; this uses `build/smoke-client` and must not be mistaken for testing the exact packaged JAR.
