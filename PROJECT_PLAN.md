# Printer 0.1.0 Project Plan

## Release goal

Ship 0.1.0 as a coherent first public release of Printer: a player can discover the mod, craft it, load and print an image, understand every item without opening a wiki, automate the machine with vanilla components, and play with it in a non-English language once a translation is available.

The 0.1.0 release should prioritize a small, polished gameplay loop over a large compatibility surface. Ponder integration is a long-term goal and is intentionally not a release blocker.

## Baseline when this plan was written

- Current version: 0.0.8.
- URL loading, server-authoritative image storage, printed Image items, wall displays, frames, color/monochrome printing, hopper-compatible item handling, and rising-edge redstone printing already exist.
- The three mod items are currently inserted into vanilla Functional Blocks rather than a dedicated Printer creative tab.
- `image.png` is already 16×16, but its rounded, high-fidelity appearance does not match the rest of the mod's hard-edged pixel-art style.
- Only `en_us.json` is present, so translation readiness and a second shipped language still need to be planned.

## Implementation progress — 2026-09-10

- Frozen the first-release scope in [docs/RELEASE_0.1.0.md](docs/RELEASE_0.1.0.md), with exact implemented limits, supported versions, open safety/compatibility blockers, and a concrete path for each P0 gate.
- Added a dedicated Printer creative tab and collapsed/Shift-expanded help for all three registered items. The Image tooltip currently reports **texture** dimensions accurately; source dimensions still require a backward-compatible metadata change before the full tooltip gate closes.
- Added registered-item tooltip and creative-tab regression tests. Fresh client visual verification remains pending.
- Verification: `gradlew.bat build --offline` passes with **39 tests, zero failures** (14 new tooltip/creative-tab cases). The NeoForge unit-test target loads the mod on the dedicated-server distribution; this is not a dedicated-server gameplay smoke test. Java required a short temporary directory at `build/tmp` on this machine.
- Selected the reviewed [translation contribution workflow](docs/TRANSLATING.md) option; no second language is claimed as shipped.
- Deferred local file upload from 0.1.0 to keep the first release focused on the existing URL pipeline and its hardening gaps.
- Added [docs/SMOKE_TEST_0.1.0.md](docs/SMOKE_TEST_0.1.0.md). Gameplay checks, advancements, art, the full translation audit, and final packaging are still open. These preparation changes are packaged as testing version 0.0.9; this is not a 0.1.0 release sign-off.

## 0.1.0 launch checklist

### P0 — must be complete before release

- [x] Define and freeze the 0.1.0 feature contract: supported Minecraft/NeoForge versions, supported image formats, size/storage limits, automation behavior, and known limitations. See [release contract](docs/RELEASE_0.1.0.md).
- [ ] Replace the blank Image placeholder texture at `src/main/resources/assets/printer/textures/item/image.png` with a deliberately simple 16×16 pixel-art item. Use hard pixel edges, no anti-aliasing, a small palette, and a silhouette that remains readable in the inventory, printer preview, and fallback render.
- [x] Add a dedicated Printer creative tab with a printer icon and a deliberate item order: Printer, Color Cartridge, Image. Remove the duplicate vanilla Functional Blocks insertion unless there is a clear discoverability reason to keep it.
- [ ] Refine the tooltip for every registered Printer item and add Shift-expanded details, following the AnalogAudio-style interaction:
  - **Printer:** default explains that it loads and prints images; Shift adds the basic workflow, paper/ink requirements, output behavior, and hopper/redstone automation summary.
  - **Color Cartridge:** default shows its remaining charges and purpose; Shift adds that it supplies color ink and can be inserted into the printer's ink slot.
  - **Image:** default shows the title or a useful fallback name plus a short placement hint; Shift adds title, source pixel dimensions, physical block size, frame, print mode, and placement behavior.
  - Every collapsed tooltip shows a translated “Hold Shift for details” hint where more information is available.
  - Keep tooltip behavior safe for dedicated servers and avoid exposing the source URL or other unnecessary network information.
- [x] Add tooltip regression coverage for all three items in both collapsed and expanded states, including unprinted and printed Image stacks and depleted/partially-used Color Cartridges.
- [ ] Add advancements for the core loop:
  - craft or obtain the Printer;
  - load an image;
  - print the first Image;
  - place an Image display;
  - complete an automated print using hopper/item handling and redstone;
  - optionally, make a color print with a Color Cartridge if the first pass has a meaningful reward.
- [ ] Make all player-facing strings translation-ready. Audit Java literals, GUI labels, tooltip lines, status/error messages, advancement titles/descriptions, and item/block names for missing translation keys.
- [x] Choose and ship at least one additional language, or explicitly mark the translation contribution workflow as the 0.1.0 community-ready deliverable if no target language is available yet. Selected the [reviewed contribution workflow](docs/TRANSLATING.md); no unreviewed machine translations.
- [ ] Add regression coverage for advancement triggers, creative-tab contents, tooltip states, and any upload protocol validation.
- [ ] Run a release smoke test in a fresh instance on both client and dedicated server:
  - manual URL load and print;
  - invalid/oversized image rejection;
  - hopper input/output;
  - one redstone rising edge per print;
  - block break/re-place with the saved preset;
  - item-frame and wall placement;
  - tooltip with and without Shift;
  - translated text loading.
- [ ] Update README, CHANGELOG, credits/license notes, mod metadata, and the release artifact version to 0.1.0.

### P1 — strong 0.1.0 candidates

- [ ] Add local file upload if the UX and protocol can be completed without weakening the server-authoritative safety model. The MVP should use a client file picker, bounded chunk transfer, server-side decode/validation, content-addressed storage, and the same byte/dimension/rate limits as URL loading.
- [ ] Make local upload work clearly in single-player and on dedicated servers. Show actionable errors for cancellation, unsupported formats, size limits, transfer failure, and server policy rejection.
- [ ] Add a small in-game help path for automation, either through the expanded tooltip/GUI help or a short README recipe, until Ponder is available.
- [ ] Add a visual art pass over the block, cartridge, GUI, ghost slots, and fallback textures so the new Image texture does not look isolated.
- [ ] Add automated tests for malformed upload packets, truncated transfers, duplicate content, disconnects during transfer, and upload permission/rate limits.

### P2 — explicitly post-0.1.0

- [ ] Add the full Ponder integration and interactive scenes. This is not a 0.1.0 launch gate.
- [ ] Add Create-specific automation scenes and polish after the base Ponder scenes are stable.
- [ ] Configurable ink economy and multipart print tiling controls.
- [ ] Additional frame profiles/materials, CC:Tweaked support, and broader version ports.

## Work phases

### Phase 1 — Lock the release contract

Write down what 0.1.0 promises and what it does not. Verify the current automation and image-safety behavior against that contract. Create a short manual test matrix and record any bugs that must be fixed before feature work is considered complete.

Exit criteria: the scope is frozen, known limitations are documented, and every P0 item has an owner or a concrete implementation path.

### Phase 2 — Player-facing polish

Complete the 16×16 Image texture redesign, dedicated creative tab, refined tooltips for all three items with Shift-expanded details, translation-key audit, and advancements. These are tightly coupled: the creative tab gives discovery, the tooltips explain every item and printed image, translations cover every new string, and advancements provide a guided first session.

Exit criteria: a new player can find the mod in Creative, understand the Printer, Color Cartridge, and both unprinted and printed Image states from their tooltips, and follow the advancement tree through a first successful print.

### Phase 3 — Local upload MVP

Design the upload flow before coding it. The client selects a local file and sends bounded chunks; the server owns validation, decoding, dimension checks, storage, deduplication, and the resulting image reference. Reuse the existing image limits and content-addressed store rather than creating a second image pipeline.

Recommended safeguards:

- maximum file size and maximum decoded dimensions enforced on the server;
- chunk count, chunk size, total transfer timeout, and per-player rate limits;
- cancellation and disconnect cleanup;
- no arbitrary filesystem paths sent to or read by the server;
- clear distinction between local upload and URL loading in the GUI;
- no client-only classes referenced from common/server code.

Exit criteria: a local file can be selected and printed in single-player and dedicated-server testing, and every failure mode produces a translated, actionable message.

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
- Local upload is either shipped with server-side safeguards and tests or clearly deferred before the release branch is cut.
- Build, automated tests, and clean-instance smoke tests pass on the supported environment.
- Full Ponder scenes are tracked as the next documentation milestone, not a 0.1.0 release dependency.

## Reference projects

- [Create Ponder](https://github.com/Creators-of-Create/Ponder) — interactive in-game documentation library and NeoForge 1.21.1 setup reference.
- [AnalogAudio](https://github.com/palmmc/AnalogAudio) — interaction and presentation reference for Shift-expanded tooltips.
