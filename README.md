# Printer

Printer is a NeoForge mod for Minecraft 1.21.1 that turns web images into independent, reusable image items. Images do not consume vanilla map IDs. They can be held, displayed in ordinary item frames, or placed directly as painting-like wall displays sized from the printed image.

Original 16×16 pixel art gives the printer a warm enamel case, timber trim, colored ink accents, and a tactile control panel inspired by Analog Audio.

## Gameplay

The mod adds three items:

- **Printer** — loads a URL, saves the last image as a reusable preset, and prints copies.
- **Image** — stores a content-addressed reference to server image data and renders dynamically.
- **Color Cartridge** — a three-use color ink supply. Vanilla ink sacs can also make monochrome prints.

Open the printer, enter a direct PNG, JPEG, WebP, GIF, BMP, TIFF, ICO, or TGA URL, and select **Load**. The printer detects the image resolution and aspect ratio, then selects and displays an automatic physical size with a maximum four-block long edge. The **−** and **+** controls resize the print in block units while keeping its aspect ratio; players never need to enter pixel dimensions. Add the displayed amount of paper and ink, choose a frame, then select **Print**. Large prints consume one sheet of paper per occupied block. A printed item records its immutable image hash, texture resolution, physical block size, print mode, frame, and title. New presets start with an oak frame; the selector also offers no frame, white, spruce, dark oak, iron, gold, and copper.

Use an Image on a wall to place a painting-style display. Placed prints render as shallow canvases with block-by-block lighting and, when selected, a raised material frame. Frame materials use vanilla texture paths, so resource packs restyle them automatically. Insert an Image into a normal item frame for a map-like display.

The control panel includes a live image preview, recessed green URL and amber title fields, raised buttons with pressed/focus states, a size readout, supply-slot ghost outlines, status lights, and a complete player inventory drawer. Hover over the input slots for help. Press Enter in a text field to load the image. Print is enabled when the saved image, paper, ink, and output slot are ready.

Animated WebP/GIF files and multipage TIFF/ICO files use their first image. Transparency is printed onto white paper. The bundled TwelveMonkeys readers run in pure Java, including on dedicated servers; players do not need an extra codec mod. AVIF, HEIC, and SVG input are not supported. Missing or generic binary HTTP content types are allowed only when the file bytes decode as a supported image.

## Automation

Automation uses NeoForge's standard item-handler capability, so vanilla hoppers and compatible modded pipes, funnels, chutes, and belts can interact without a hard Create dependency.

- **Top:** color cartridge or ink-sac input.
- **Left side, relative to the printer front:** paper input.
- **Right side or bottom:** image output.
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

Regenerate the original textures and a design contact sheet (`build/art-preview.png`) with Python and Pillow:

```powershell
python -m pip install Pillow
python tools\generate_programmer_art.py
```

## Roadmap

- configurable ink economy and multipart print tiling controls;
- additional frame profiles and materials beyond the initial raised frame set;
- Create-specific visual polish where standard capability interop is insufficient;
- optional CC:Tweaked peripheral API for setting URLs, loading, querying status, and triggering prints;
- porting modules for newer NeoForge versions and selected backports.

Version-specific Minecraft/NeoForge integration is kept at the edges of the project; image processing, references, storage, and print modes are separate service/data layers to make later ports less invasive.

See [CREDITS.md](CREDITS.md) for design references and licensing notes.
