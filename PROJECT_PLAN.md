# Printer 0.1.0 Project Plan

## Release goal

Ship 0.1.0 as a coherent first public release of Printer: a player can discover the mod, craft it, load and print an image, understand every item without opening a wiki, automate the machine with vanilla components, and play with it in a non-English language once a translation is available.

The 0.1.0 release should prioritize a small, polished gameplay loop over a large compatibility surface. Because Printer has not been released yet, 0.1.0 may make breaking save/item/data changes without compatibility shims. Ponder integration is a long-term goal and is intentionally not a release blocker.

## Compatibility policy for 0.1.0

0.1.0 is a clean break before the first public release. We do not need to preserve compatibility with development-world saves, pre-0.1.0 Printer items, or the current frame metadata. The 0.1.0 release requires new worlds.

- [x] Remove compatibility and migration code for old Image/background/frame metadata rather than adding fallback paths.
- [x] Make the new background color a native part of the 0.1.0 preset and Image data model, with one defined default color.
- [x] Ensure the release documentation clearly warns that existing development worlds and old Printer items are unsupported.
- [ ] Smoke-test only fresh 0.1.0 worlds and the exact 0.1.0 client/server artifact.

## Current candidate — 0.1.0

- Release target: 0.1.0 (unpublished; requires new worlds). Current working test version: 0.0.12, retained from the maintainer's version change.
- URL loading, local file upload, server-authoritative image storage, source-dimension metadata, printed Image items, wall displays, color/monochrome printing, hopper-compatible item handling, and rising-edge redstone printing are implemented.
- The dedicated Printer creative tab, all-item Shift tooltip behavior, core advancements, translated built-in strings, and regression coverage are implemented.
- `image.png` is now a deliberately simple, hard-edged 16×16 paper-and-landscape icon, generated from the editable art source.
- Frame selection, decorative frames, and pre-release frame compatibility paths have been removed.
- Canonical images preserve transparency; print variants composite against the selected background, with white as the default. The palette is code-drawn, so no texture assets were changed.

## Implementation progress — 2026-09-11

- Final requested art pass: simplified all seven block textures (front/printing controls, side-only trays, quiet back/bottom, top cartridge slot) and replaced the blank Image with hard-edged 16×16 art. Updated the editable generator and its labeled design preview. GUI now has 18×18 Load/Browse/Print icons inspired by AnalogAudio's tape-deck controls, upload cancel feedback, centered pixel size symbols, more title space, separated status/inventory text, and an ink-dependent model header (P-01 empty, B&W ink sac, COLOR cartridge). Automated build passes 108 unit tests; maintainer gameplay/visual checks remain open.
- Final texture and GUI art pass is complete across the Printer block, cartridge, GUI panel, ghost slots, Image fallback, background-color control, status indicators, and controls. The editable texture sources and labeled design preview are up to date; only ordinary release smoke testing remains.
- Functional 0.1.0 implementation is complete: borderless prints, native background metadata throughout preset/item/entity/save/network paths, alpha-preserving canonical processing, the 16-color palette, and Create/AnalogAudio-style localized tooltips.
- Release metadata, version, README, changelog, credits/license packaging, and the frozen [release contract](docs/RELEASE_0.1.0.md) are updated. Image-transfer request/byte budgets and regression coverage are included.
- Before the background-rendering follow-up, the full offline build passed 95 unit tests with no failures/errors, and all 6 integration GameTests passed in the separate `build/gametest-0.1.0-run` directory. These results do not sign off manual gameplay or the exact packaged artifact.
- Maintainer testing reported white background pixels in the preview and printed display, plus white entity edges. Follow-up fixes explicitly enable preview alpha blending and tint the canvas front/back/edges with the stored background color instead of white concrete. Added renderer, native-decoder alpha, and transparent-white/red-background regression tests.
- Refreshed follow-up verification: `gradlew.bat build --offline` passes with 101 unit tests, zero failures/errors. Candidate: `build/libs/printer-0.1.0.jar`; SHA-256 `2d3ffb57329f843c8fd5d2d0f0d8d8ffe7a16c49b3b4830b8086a5d1d74fb93f`. The running interactive client still needs restarting to load these changes.
- Read-only inspection of the loaded 225×225 source in the smoke-test world found zero transparent pixels and opaque white background pixels. Background fill does not replace opaque artwork; reproduction with the original PNG remains needed to resolve that part of the report.
- Follow-up attachment inspection: the supplied 700×700 clipboard WebP has 275,432 fully transparent and 5,253 partially transparent pixels. A standalone probe of the current `ImageProcessor` preserves alpha at 700, 225, and 128 pixels and correctly fills every fully transparent pixel with white, red (`#B02E26`), or blue-gray (`#224466`). This verifies the attached file's processing, not the original URL/file-loading path or rendered gameplay. Determine whether the original load used a flattened URL thumbnail or different file; maintainer re-test remains open.
- Manual testing is owned by the maintainer at their request. Background re-testing, normal/large GUI-scale checks, and fresh single-player/dedicated-server checks of the exact candidate remain open in [the smoke-test matrix](docs/SMOKE_TEST_0.1.0.md).
- Texture refinement and GUI texture changes are complete. No publication approval is implied by the implementation checkboxes; exact-artifact smoke testing remains a separate release gate.
- Clean monochrome follow-up: artwork-only alpha-aware dithering now precedes the flat background fill; color processing is unchanged. The earlier `0.0.11` test build passed 104 unit tests with zero failures/errors. Artifact: `build/libs/printer-0.0.11.jar`; SHA-256 `c8706e5cb81669de418c955ec6e58029d81387adb727f9e2b119b430858f0b36`. Existing prints remain immutable; print new copies to test the new monochrome handling. Gameplay testing remains with the maintainer.
- Ink/background policy follow-up: ink sacs now permit only pure black/white; other backgrounds require a Color Cartridge. All 7 integration GameTests pass, including no-consumption rejection of colored manual/redstone attempts and successful black/white prints. This reused the isolated test world and does not replace fresh exact-artifact maintainer testing. The new `0.0.12` version selected by the maintainer is retained.
- Final ink-policy build: `gradlew.bat build --offline` passes 108 unit tests with zero failures/errors. Artifact: `build/libs/printer-0.0.12.jar`; SHA-256 `8210ff9b16047916235265dd9360d859bedb24d34b627cf0de9492cc9bb14437`. Normal packaging excludes opt-in GameTest fixtures. Maintainer GUI/gameplay sign-off remains open.

## Implementation progress — 2026-09-10

- The first-release scope and exact implementation limits are documented in [docs/RELEASE_0.1.0.md](docs/RELEASE_0.1.0.md).
- The dedicated Printer creative tab, all-item collapsed/Shift-expanded tooltips, source metadata, advancements, translation workflow, and local upload are implemented and covered by tests.
- Server packet menu-validity checks, upload validation, and image transfer integrity are implemented.
- Verification recorded for the 0.0.10 preparation work: `gradlew.bat build --offline` passes with 39 tests and zero failures. This is automated verification only; fresh client and dedicated-server gameplay checks remain pending.
- At this preparation stage, the planned final feature work was frame removal, selectable background fill color, Create/AnalogAudio-style tooltip formatting, texture cleanup, and GUI polish. The functional work is now implemented; see the newer progress entry above.

## 0.1.0 launch checklist

### P0 — must be complete before release

- [x] Update and re-freeze the 0.1.0 feature contract for the no-frame/background-color design. Keep the existing version, image, upload, storage, and automation limits. See [release contract](docs/RELEASE_0.1.0.md).
- [x] Replace the blank Image placeholder texture at `src/main/resources/assets/printer/textures/item/image.png` with a deliberately simple 16×16 pixel-art item and complete the visual sign-off.
- [x] Add a dedicated Printer creative tab with a printer icon and a deliberate item order: Printer, Color Cartridge, Image. Remove the duplicate vanilla Functional Blocks insertion unless there is a clear discoverability reason to keep it.
- [x] Implement refined tooltips for every registered Printer item with collapsed and Shift-expanded details.
- [x] Restyle those tooltips to match the shared Create/AnalogAudio convention:
  - use separate translated `tooltip.summary`, `tooltip.conditionN`, and `tooltip.behaviourN` entries;
  - keep the concise summary visible by default;
  - show condition/behaviour detail lines only while Shift is held;
  - use the same emphasis/formatting convention in localized values, without concatenated English-only paragraphs;
  - update Image details to report background color and omit frame data;
  - retain the dedicated-server-safe keyboard bridge and never expose the source URL or internal image ID.
- [x] Add tooltip regression coverage for all three items in both collapsed and expanded states, including unprinted and printed Image stacks and depleted/partially-used Color Cartridges.
- [ ] Add visual tooltip checks at normal and large GUI scales after the formatting migration.
- [ ] Maintainer re-test of the background rendering follow-up: a file with confirmed alpha changes preview/print fill when switching white/red/another color; opaque white artwork stays white; canvas margins and shallow edges use the selected color. Attached transparent WebP passes processor checks; original URL/file-loading path still needed for exact reproduction.
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
- [x] Remove the frame selector and make all 0.1.0 prints unconditionally borderless. The obsolete `PrintFrame` enum is deleted rather than retaining selectable materials or a serialized frame field. Delete old frame compatibility/migration paths; pre-0.1.0 frame-bearing saves and items do not need to load.
- [x] Add a selectable background fill color with white as the default. Prefer a compact in-GUI palette for 0.1.0 refer to the Analog Audio Tape Deck color picker; persist the selected 24-bit color in the new preset and Image metadata, carry it through save/drop/network paths, and include it in variant generation/deduplication. Implemented as 16 code-drawn swatches; the final GUI texture and control art pass is complete.
- [x] Preserve transparency until the selected background is applied during canonical/variant processing. No fallback conversion is needed for old white-composited sources because 0.1.0 requires new worlds.
- [x] Always render the placed Image entity's shallow sides, back, and exposed canvas margins in its saved background color, even when the printed image is fully opaque. Canvas color is independent of image alpha and variant deduplication; renderer regression coverage checks all six canvas faces. Maintainer visual sign-off remains pending.
- [x] Separate monochrome artwork conversion from color printing: dither only source artwork, preserve soft alpha coverage, skip transparent pixels when diffusing error, then composite the chosen black/white background without dithering it. Opaque artwork's ink pattern is independent of background selection. Added flat-paper, stable ink-pattern, soft-edge, and opaque deduplication regression coverage. Maintainer visual sign-off remains pending.
- [x] Restrict ink-sac backgrounds to pure black or white; all other colors require a Color Cartridge. Disable colored swatches with an ink sac, reject incompatible background/print requests on the server (including redstone), re-check supplies/preset at completion, and reject colored monochrome variants in the processor. Preserve an existing colored preset but block ink-sac printing until explicitly corrected; no supplies are consumed on rejection. The black swatch is pure `#000000`. Maintainer visual sign-off remains pending.
- [ ] Run a release smoke test in a fresh instance on both client and dedicated server:
  - manual URL load and print;
  - invalid/oversized image rejection;
  - hopper input/output;
  - one redstone rising edge per print;
  - block break/re-place with the saved preset in a fresh 0.1.0 world;
  - item-frame and wall placement;
  - tooltip with and without Shift, using the final Create/AnalogAudio-style formatting;
  - default and non-white background colors, including transparent source pixels;
  - no frame selector, no rendered decorative border, and no compatibility path for legacy saved data;
  - translated text loading.
- [x] Update README, CHANGELOG, credits/license notes, and mod metadata for the 0.1.0 release contract. This is preparation, not publication approval; final art and exact-artifact smoke sign-off remain required.
- [ ] Restore the final release artifact version to 0.1.0 after the current maintainer-selected 0.0.12 testing phase, then rebuild and record the final checksum.

### P1 — strong 0.1.0 candidates

- [x] Add local file upload with a client file picker, bounded chunk transfer, server-side decode/validation, content-addressed storage, and the same byte/dimension/rate limits as URL loading.
- [x] Make local upload work in single-player and on dedicated servers. Show actionable errors for cancellation, unsupported formats, size limits, transfer failure, and server policy rejection.
- [x] Add a small in-game help path for automation through the expanded item tooltip and README recipe until Ponder is available.
- [x] Add the final visual art pass over the block, cartridge, GUI, ghost slots, fallback texture, background-color control, and status indicators.
- [x] Add automated tests for malformed upload packets, truncated transfers, duplicate content, disconnects during transfer, and upload permission/rate limits.

### P2 — explicitly post-0.1.0

- [ ] Add the full Ponder integration and interactive scenes. This is not a 0.1.0 launch gate.
- [ ] Add Create-specific automation scenes and polish after the base Ponder scenes are stable.
- [ ] Evaluate a dedicated Printer Paper item as a future replacement or addition to vanilla paper. Decide whether vanilla paper remains accepted, define the recipe and production balance, and provide a migration/compatibility policy before implementation.
- [ ] Add deeper optional Create integration when Create is installed: conditionally add Create-facing recipes such as packing and a custom-shape Mechanical Crafter recipe for Printer items, then evaluate compatible transfer/automation recipes. Create must remain an optional dependency; the base mod, recipes, and client/server startup must continue to work cleanly without it.
- [ ] Configurable ink economy and multipart print tiling controls + Mod Menu config compat.
- [ ] CC:Tweaked support and broader version ports.

## Work phases

### Phase 1 — Lock the release contract

Write down what 0.1.0 promises and what it does not. Verify the current automation and image-safety behavior against that contract. Create a short manual test matrix and record any bugs that must be fixed before feature work is considered complete.

Exit criteria: the scope is frozen, known limitations are documented, and every P0 item has an owner or a concrete implementation path.

### Phase 2 — Player-facing polish

Frame removal, selectable background color, Create/AnalogAudio-style tooltip formatting, the creative tab, translation audit, advancements, upload flow, automated regression coverage, the 16×16 Image texture redesign, and final GUI texture polish are implemented. Maintainer background-rendering and exact-artifact smoke sign-off remains pending. The new data model is implemented directly without development-build compatibility code.

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

### Phase 4 — Release hardening

Test vanilla automation against the documented sides and slots, verify redstone edge behavior, test multiplayer image delivery and cache eviction, and verify the new 0.1.0 data model in fresh worlds. Do not spend release time on old-save migration or compatibility shims. Review logs for noisy or sensitive output, especially around URLs, upload failures, and image identifiers.

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
- Local upload and the new no-frame/background data model are implemented with server-side safeguards and tests; the remaining release gates are maintainer background-rendering sign-off and exact-artifact clean-instance verification.
- Build, automated tests, and clean-instance smoke tests pass on the supported environment.
- 0.1.0 requires new worlds; compatibility code for pre-release Image/frame data is intentionally out of scope.
- Full Ponder scenes are tracked as the next documentation milestone, not a 0.1.0 release dependency.

## Reference projects

- [Create Ponder](https://github.com/Creators-of-Create/Ponder) — interactive in-game documentation library and NeoForge 1.21.1 setup reference.
- [AnalogAudio](https://github.com/palmmc/AnalogAudio) — interaction and presentation reference for Shift-expanded tooltips.
