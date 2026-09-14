# Printer 0.1.0 Project Plan

## Release goal

Ship a polished first public release for Minecraft 1.21.1 / NeoForge 21.1.248 / Java 21: craft a printer, load a web or local image, print it in color or B&W, display it, and collect prints in a Photobook.

**Current status: implemented release candidate; maintainer smoke-test sign-off remains open.** Version is now 0.1.0. The final artifact, checksum, automated results, and manual matrix are recorded in [docs/SMOKE_TEST_0.1.0.md](docs/SMOKE_TEST_0.1.0.md). Passing automated tests does not sign off the packaged client or multiplayer UI.

## Compatibility policy

0.1.0 requires new worlds. Development saves, old Printer/Image items, frame metadata, and old network peers are unsupported. The frame enum, frame rendering, migration logic, and old metadata fallbacks have already been removed. This pass found no remaining old-save compatibility shims; ordinary empty-item defaults, input validation, and missing-image placeholders remain necessary runtime behavior. The network protocol is explicitly `0.1.0`.

## Implementation — 2026-09-13

- [x] Add a Black Ink Cartridge, crafted shapelessly from one iron nugget and one ink sac. Consume one cartridge per B&W print; reject raw ink sacs in menus and automation. Preserve black/white-only background rules and no-consumption rejection behavior.
- [x] Replace only the Color Cartridge recipe's black dye with an ink sac. Keep cyan, magenta, yellow, the iron nugget, and three charges unchanged.
- [x] Give both cartridges a shared P-01 cream-shell design with dark green inset, teal latch, and brass contacts. Keep the editable pixel-art generator and contact sheet current.
- [x] Add a Photobook, crafted from one book and two leather. Match the cassette bag's **18 slots (2 × 9)**, verified from `Analog-Audio-0.1.5-hotfix.1.jar`.
- [x] Provide an open-book GUI with two image previews above the photo slots and player inventory below. Page buttons and Left/Right arrows browse printed Images in slot order, skipping blanks. Preserve aspect ratio and saved print backgrounds; show titles and loading states. Page buttons sit beside the top storage row, the page counter sits in the header, and empty photo slots show Image ghost icons.
- [x] Support main/offhand opening, Shift-click transfer, per-edit persistence, sparse-slot save/network round trips, and carrier locking against pickup, drop, number-key and offhand swaps. Accept only printed Images; prohibit nested books and other items.
- [x] Hide the blank Image from the creative tab/search. Order creative items: Printer, Black Ink Cartridge, Color Cartridge, Photobook.
- [x] Use crisp nearest-neighbor placeholder rendering for unprinted Images, including JEI; keep smooth rendering for actual photos.
- [x] Shorten every item tooltip. Printer Shift help covers load, supplies, and printing in five total lines. Cartridge/Photobook tips are two or three lines; printed Image details retain title, block size, background, ink, and placement help. Remove automation from item tooltips; keep the README guide until Ponder.
- [x] Review local file upload and URL loading. Fix reservation validation occurring after printer state changes; test rejected starts. Record controls and limits in [docs/UPLOAD_REVIEW_0.1.0.md](docs/UPLOAD_REVIEW_0.1.0.md).
- [x] Set artifact version to 0.1.0; update recipes, recipe unlocks, translations, README, release contract, changelog, and credits.
- [x] Pass the final ordinary build: 116 tests, zero failures/errors/skips; verify packaged version, all 15 textures, recipes, bundled libraries, license/credits, and absence of GameTest fixtures.
- [x] Pass dedicated-server integration coverage: 10 GameTests including photobook capacity/persistence/carrier locking and upload preflight, plus existing printing/automation/upload tests.

## Existing completed scope

- URL and local-file loading, supported raster decoders, bounded transfer protocol, server-authoritative validation/storage, SHA-256 deduplication, source metadata, cache budgets, and translated errors.
- Native background metadata and a 16-color palette; alpha-preserving sources; color compositing and artwork-only monochrome dithering; selected-color canvas sides/back; no decorative frames.
- Paper costs based on block area, ink-dependent print modes, manual printing, hopper input/output, and one print per redstone rising edge.
- Printer preset persistence on break/re-place; Image item/wall placement and saved metadata; server-side advancement triggers.
- Dedicated creative tab, brief Shift tooltips, English localization and a reviewed [translation contribution workflow](docs/TRANSLATING.md).
- Original block, cartridge, photobook, Image, ghost-slot, and GUI art; license/credits packaging.

## Remaining P0 release gates

- [ ] Maintainer checks of the **exact final JAR** on a fresh single-player world and dedicated server with a separate client.
- [ ] Photobook GUI at normal and large GUI scales: empty/full/odd-count spreads, aspect ratios, titles, page controls, Shift-click, both hands, reconnect and save/restart. No item loss or duplication.
- [ ] Final cartridge textures, ink-dependent Printer header, creative/search contents, recipes/recipe unlocks, concise Shift tooltips, and crisp blank Image corners in JEI.
- [ ] Local file picker and actual client-to-server uploads, including cancel/close/disconnect, disabled policy, oversized/invalid files, and second-client image delivery.
- [ ] Resolve the earlier background-rendering report using a confirmed transparent original file through the real loading path. Processor tests preserve alpha; the earlier saved source inspected in-world was opaque. Do not mark this closed solely from processor tests.
- [ ] Complete the remaining rows of [the smoke matrix](docs/SMOKE_TEST_0.1.0.md), including normal printing, placement, hopper orientations, redstone, missing image data, and translations.
- [ ] Record maintainer sign-off before publishing. Nothing in this implementation authorizes or performs publication.

Manual testing remains with the maintainer, as previously requested. Source-runtime GameTests supplement, but do not replace, exact-artifact testing.

## Post-0.1.0

- Full Ponder scenes for printing and automation; optional Create scenes and recipes without a hard dependency.
- Configurable ink economy, multipart tiling, and configuration UI integration.
- Evaluate dedicated Printer Paper; vanilla paper remains the 0.1.0 input.
- CC:Tweaked integration and additional Minecraft/loader ports.

The current feature contract is [docs/RELEASE_0.1.0.md](docs/RELEASE_0.1.0.md). Earlier development build notes are available in Git history; only verification of the current artifact can close release gates.
