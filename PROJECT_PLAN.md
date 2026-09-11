# Printer 0.1.0 Project Plan

## Release goal

Ship 0.1.0 as a coherent first public release of Printer: a player can discover the mod, craft it, load and print an image, understand every item without opening a wiki, automate the machine with vanilla components, and play with it in a non-English language once a translation is available.

The 0.1.0 release should prioritize a small, polished gameplay loop over a large compatibility surface. Ponder integration is a long-term goal and is intentionally not a release blocker.

## Current baseline — 0.0.10

- Current version: 0.0.10.
- URL loading, local file upload, server-authoritative image storage, source-dimension metadata, printed Image items, wall displays, color/monochrome printing, hopper-compatible item handling, and rising-edge redstone printing are implemented.
- The dedicated Printer creative tab, all-item Shift tooltip behavior, core advancements, translated built-in strings, and regression coverage are implemented.
- `image.png` is still 16×16, but its rounded, high-fidelity appearance does not match the rest of the mod's hard-edged pixel-art style.
- The active design still exposes selectable frame materials and a frame button; this will be removed before 0.1.0.
- The image-processing pipeline currently composites empty/transparent pixels against white; 0.1.0 will make that background color selectable, with white remaining the default.

## Implementation progress — 2026-09-10

- The first-release scope and exact implementation limits are documented in [docs/RELEASE_0.1.0.md](docs/RELEASE_0.1.0.md).
- The dedicated Printer creative tab, all-item collapsed/Shift-expanded tooltips, source metadata, advancements, translation workflow, and local upload are implemented and covered by tests.
- Server packet menu-validity checks, upload validation, image transfer integrity, and legacy metadata handling are implemented.
- Verification recorded for the 0.0.10 preparation work: `gradlew.bat build --offline` passes with 39 tests and zero failures. This is automated verification only; fresh client and dedicated-server gameplay checks remain pending.
- The only new feature work required for the final pass is frame removal, selectable background fill color, Create/AnalogAudio-style tooltip formatting, texture cleanup, and GUI polish.

## 0.1.0 launch checklist

### P0 — must be complete before release

- [ ] Update and re-freeze the 0.1.0 feature contract for the no-frame/background-color design. Keep the existing version, image, upload, storage, and automation limits. See [release contract](docs/RELEASE_0.1.0.md).
- [ ] Replace the blank Image placeholder texture at `src/main/resources/assets/printer/textures/item/image.png` with a deliberately simple 16×16 pixel-art item. Texture polish remains a final release gate.
- [x] Add a dedicated Printer creative tab with a printer icon and a deliberate item order: Printer, Color Cartridge, Image. Remove the duplicate vanilla Functional Blocks insertion unless there is a clear discoverability reason to keep it.
- [x] Implement refined tooltips for every registered Printer item with collapsed and Shift-expanded details.
- [ ] Restyle those tooltips to match the shared Create/AnalogAudio convention:
  - use separate translated `tooltip.summary`, `tooltip.conditionN`, and `tooltip.behaviourN` entries;
  - keep the concise summary visible by default;
  - show condition/behaviour detail lines only while Shift is held;
  - use the same emphasis/formatting convention in localized values, without concatenated English-only paragraphs;
  - update Image details to report background color and omit frame data;
  - retain the dedicated-server-safe keyboard bridge and never expose the source URL or internal image ID.
- [x] Add tooltip regression coverage for all three items in both collapsed and expanded states, including unprinted and printed Image stacks and depleted/partially-used Color Cartridges.
- [ ] Add visual tooltip checks at normal and large GUI scales after the formatting migration.
- [x] Add advancements for the core loop:
  - craft or obtain the Printer;
  - load an image;
  - print the first Image;
  - place an Image display;
  - complete an automated print using hopper/item handling and redstone;
  - optionally, make a color print with a Color Cartridge if the first pass has a meaningful reward.
- [x] Make all player-facing strings translation-ready. Audit Java literals, GUI labels, tooltip lines, status/error messages, advancement titles/descriptions, and item/block names for missing translation keys.
- [x] Choose and ship at least one additional language, or explicitly mark the translation contribution workflow as the 0.1.0 community-ready deliverable if no target language is available yet. Selected the [reviewed contribution workflow](docs/TRANSLATING.md); no unreviewed machine translations.
- [x] Add regression coverage for advancement triggers, creative-tab contents, tooltip states, upload protocol validation, image transfer integrity, metadata round trips, and local file reads.
- [ ] Remove the frame selector and make all new prints use `PrintFrame.NONE`. Preserve old save/item/network decoding long enough to normalize legacy frame values to `NONE`, then remove unused frame rendering and active frame-selection paths.
- [ ] Add a selectable background fill color with white as the default. Prefer a compact in-GUI palette for 0.1.0; persist the selected 24-bit color in the preset and Image metadata, carry it through save/drop/network paths, and include it in variant generation/deduplication.
- [ ] Preserve transparency until the selected background is applied during canonical/variant processing. Existing stored white-composited sources must continue loading with white as their compatibility default.
- [ ] Run a release smoke test in a fresh instance on both client and dedicated server:
  - manual URL load and print;
  - invalid/oversized image rejection;
  - hopper input/output;
  - one redstone rising edge per print;
  - block break/re-place with the saved preset;
  - item-frame and wall placement;
  - tooltip with and without Shift, using the final Create/AnalogAudio-style formatting;
  - default and non-white background colors, including transparent source pixels;
  - no frame selector, no rendered decorative border, and legacy saved data normalized safely;
  - translated text loading.
- [ ] Update README, CHANGELOG, credits/license notes, mod metadata, and the release artifact version to 0.1.0.

### P1 — strong 0.1.0 candidates

- [x] Add local file upload with a client file picker, bounded chunk transfer, server-side decode/validation, content-addressed storage, and the same byte/dimension/rate limits as URL loading.
- [x] Make local upload work in single-player and on dedicated servers. Show actionable errors for cancellation, unsupported formats, size limits, transfer failure, and server policy rejection.
- [x] Add a small in-game help path for automation through the expanded item tooltip and README recipe until Ponder is available.
- [ ] Add the final visual art pass over the block, cartridge, GUI, ghost slots, fallback texture, background-color control, and status indicators.
- [x] Add automated tests for malformed upload packets, truncated transfers, duplicate content, disconnects during transfer, and upload permission/rate limits.

### P2 — explicitly post-0.1.0

- [ ] Add the full Ponder integration and interactive scenes. This is not a 0.1.0 launch gate.
- [ ] Add Create-specific automation scenes and polish after the base Ponder scenes are stable.
- [ ] Configurable ink economy and multipart print tiling controls.
- [ ] CC:Tweaked support and broader version ports.

## Work phases

### Phase 1 — Lock the release contract

Write down what 0.1.0 promises and what it does not. Verify the current automation and image-safety behavior against that contract. Create a short manual test matrix and record any bugs that must be fixed before feature work is considered complete.

Exit criteria: the scope is frozen, known limitations are documented, and every P0 item has an owner or a concrete implementation path.

### Phase 2 — Player-facing polish

Complete frame removal, selectable background color, Create/AnalogAudio-style tooltip formatting, the 16×16 Image texture redesign, and final GUI polish. The creative tab, item tooltip behavior, translation audit, advancements, upload flow, and automated regression coverage are already complete.

Exit criteria: a new player can find the mod in Creative, understand all three items from consistently formatted tooltips, choose a background color, print a borderless Image, and follow the advancement tree through a first successful print.

### Phase 3 — Local upload MVP

Local upload is implemented. Keep this phase as a verification and maintenance phase: the client selects a local file and sends bounded chunks; the server owns validation, decoding, dimension checks, storage, deduplication, and the resulting image reference. Reuse the existing image limits and content-addressed store rather than creating a second image pipeline.

Recommended safeguards:

- maximum file size and maximum decoded dimensions enforced on the server;
- chunk count, chunk size, total transfer timeout, and per-player rate limits;
- cancellation and disconnect cleanup;
- no arbitrary filesystem paths sent to or read by the server;
- clear distinction between local upload and URL loading in the GUI;
- no client-only classes referenced from common/server code.

Exit criteria: the implemented local-file path passes single-player and dedicated-server testing, and every failure mode produces a translated, actionable message.

### Phase 4 — Compatibility and release hardening

Test vanilla automation against the documented sides and slots, verify redstone edge behavior, test multiplayer image delivery and cache eviction, and audit save/load compatibility. Review logs for noisy or sensitive output, especially around URLs, upload failures, and image identifiers.

Exit criteria: clean build, passing tests, fresh-instance smoke test, no known P0 bugs, and release documentation ready.

### Phase 5 — 0.1.0 release

Freeze code and assets, bump the version, create the changelog entry, build the distributable, test the exact artifact in a clean instance, and publish the README's known limitations and support expectations.

Exit criteria: the artifact version is 0.1.0, the release checklist is signed off, and the first public bug-report path is documented.

## Post-0.1.0 Ponder track

Ponder should be treated as the in-game documentation destination after the core loop and item tooltips are stable. Full Ponder scenes are explicitly deferred until after 0.1.0. The Ponder project documents a NeoForge 1.21.1 dependency path using `Ponder-NeoForge-${minecraft_version}`; when this work starts, pin a compatible version and verify it against the current NeoForge toolchain rather than copying an unpinned dependency.

Suggested sequence:

1. Add the dependency behind a compatibility decision and confirm whether it is acceptable as a required or optional runtime dependency.
2. Register a Printer Ponder entry on the Printer block/item.
3. Build a vanilla automation scene showing paper and ink entering from the documented hopper sides, the Image leaving the output, and a redstone rising edge triggering one print.
4. Build a manual scene showing the saved preset, paper/ink requirements, output slot, and printed Image placement.
5. Add a Create compatibility scene for funnels, belts, chutes, or other Create automation only when Create is present; keep Create-specific references out of the base path if Create remains optional.
6. Localize scene titles, controls, and instructional text, then validate scenes in a clean client with and without Create installed.

Ponder exit criteria: after 0.1.0, a player can open the guide from the Printer, understand both manual and vanilla automation, and optionally see a Create setup without the base mod acquiring a hard Create dependency.

## Definition of done for 0.1.0

- The core print loop is discoverable, understandable, and stable.
- The blank Image texture is unmistakably intentional 16×16 pixel art.
- Creative inventory, refined tooltips for every item, Shift-expanded details, advancements, and GUI strings are complete and translated according to the release decision.
- Vanilla hopper and redstone automation remains reliable and documented.
- Local upload is shipped with server-side safeguards and tests; the remaining release gate is texture polish and clean-instance verification.
- Build, automated tests, and clean-instance smoke tests pass on the supported environment.
- Full Ponder scenes are tracked as the next documentation milestone, not a 0.1.0 release dependency.

## Reference projects

- [Create Ponder](https://github.com/Creators-of-Create/Ponder) — interactive in-game documentation library and NeoForge 1.21.1 setup reference.
- [AnalogAudio](https://github.com/palmmc/AnalogAudio) — interaction and presentation reference for Shift-expanded tooltips.
