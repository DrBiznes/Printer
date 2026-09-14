# 0.1.0 release smoke tests

Run the same scenarios in a fresh single-player instance and on a fresh dedicated server with a separate client. Use Java 21, Minecraft 1.21.1, NeoForge 21.1.248 and the exact candidate JAR on both sides. Keep existing development worlds untouched. Only mark a result Pass after observing it; automated tests do not sign off gameplay.

Current release candidate: **0.1.0**, including Black Ink Cartridges and Photobook. Artifact: `build/libs/printer-0.1.0.jar`. SHA-256: `18a27cef709beaf0341adcd259eb9f5aa21de44a24172b70b5fc60b3f29d5d95`.

Final ordinary build verification (2026-09-13): `gradlew.bat build --offline` passed **116 tests**, zero failures/errors/skips. The packaged JAR reports version 0.1.0; all 15 textures and the new recipes match source resources; both new items, menu/screen, 10 bundled decoder libraries, license and credits are present. Opt-in GameTest classes/fixtures are absent. Size: 999,419 bytes. Process-local Java workaround: `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:/hackerman/Printer/build`.

Photobook layout follow-up: moved page buttons beside the top storage row, moved the counter to the title header, and reused the Printer Image ghost in empty photo slots. The rebuilt JAR passes all 116 tests; in-game visual confirmation remains with the maintainer.

Automated integration verification (2026-09-13): **10/10 GameTests pass**, including 18-slot photobook persistence, main/offhand carrier locking, and upload preflight. The isolated `build/gametest-0.1.0-run` world was reused; this does not certify a fresh world or the exact packaged JAR. Art contact sheet `build/art-preview.png` was inspected; it is a design preview, not a game capture.

Tester / date: **maintainer sign-off pending**. See [upload review](UPLOAD_REVIEW_0.1.0.md) for code-level readiness and limits.

Instance settings and deviations: **pending**

| Scenario | Expected result | Single-player | Dedicated server |
| --- | --- | --- | --- |
| Startup | No missing registries, client-class loading failures, or error logs; recipes load. | Pending | Pending |
| Final texture/GUI pass | At normal/large GUI scales: status and Inventory do not touch; Load/Browse/Print icons have tooltips and keyboard focus; Browse becomes Cancel while uploading; size symbols are centered. Header changes immediately between P-01 (empty), P-01 / B&W (Black Ink Cartridge), and P-01 / COLOR (cartridge), including hopper changes. Block front is recognizable in every orientation; trays appear only on the paper/output sides; top has the ink slot; blank Image has crisp pixels. | Pending | Pending |
| Discoverability | Printer tab, printer icon; Printer / Black Ink Cartridge / Color Cartridge / Photobook order; blank Image excluded; no Functional Blocks duplicates; creative search finds items. | Pending | Pending |
| Obtain/craft | All four recipes and unlocks work; Craft a Printer, Craft Black & White Ink, and Craft Color Ink advancements each award once. | Pending | Pending |
| Photobook | Main/offhand opening; 18 slots, only printed Images accepted; empty/full/odd/sparse pages and long titles render cleanly. Page buttons/Left/Right arrows, Shift-click, close/reopen, save/restart, disconnect, death, and both-hand carrier locks preserve items exactly once. Check normal/large GUI scales. | Pending | Pending |
| Local upload | File picker selects only the chosen file; real client/server transfer produces the same print as URL input. Cancellation, closing/walking away, disconnect, disabled uploads, invalid/oversized files and reconnection leave a usable printer and preserve the previous preset. | Pending | Pending |
| URL load | Public direct PNG/JPEG and one bundled-codec fixture load, preview and sizing agree; load advancement only on success. | Pending | Pending |
| Invalid input | Malformed URL, unsupported file, HTML, non-public destination, and blocked host reject with translated messages. | Pending | Pending |
| Limits | Download over configured byte limit and image over 4096 pixels on an axis reject without allocating the full bitmap; accepted large source scales to configured saved resolution. | Pending | Pending |
| Manual print | Paper cost matches area; color consumes one charge, monochrome one Black Ink Cartridge; full output prevents print; first-print advancement fires only on success. | Pending | Pending |
| Background fill | With a PNG whose alpha is confirmed, switch white/red/another color: preview updates, a new color print fills transparent pixels accordingly, opaque white artwork remains white, and wall canvas margins/back/edges use the selected color. | Re-test pending; white pixels/edges reported | Pending |
| Clean monochrome | Print transparent artwork using Black Ink Cartridges with pure white and pure black backgrounds. Only artwork is dithered to black/white; transparent regions stay uniformly background-colored, opaque ink patterns do not change with background selection, and soft alpha edges blend without retaining source hues. Entity sides/back keep the saved background color. | Pending | Pending |
| Ink/background policy | Black Ink Cartridges enable only pure black/white swatches. Swap a colored preset from a cartridge to an Black Ink Cartridge: printing is disabled with actionable help, manual/redstone requests produce no output and consume nothing, and colored background payloads cannot bypass the rule. Choosing black/white restores black-cartridge printing; a usable cartridge allows colored backgrounds again. Repeat with a carried/reloaded preset. | Pending | Pending |
| Opaque image canvas | Print the same fully opaque image with red and another background. The image artwork stays unchanged, but each placed entity's sides/back/exposed margins use that print's saved color, even if both prints share identical PNG content. Repeat after save/reload. | Pending | Pending |
| Tooltips | Every item with/without Shift; fresh/partially used/depleted cartridges; blank/titled/untitled Images; Create/AnalogAudio-style summary/condition/behaviour formatting; background color details; no decorative frame data or automation instructions. | Pending | Pending |
| Hoppers | Ink above, paper at intended side, extraction below/right; invalid sides/items rejected. Repeat in all four horizontal orientations. | Pending | Pending |
| Redstone | First rising edge prints once, held signal does not repeat, release/reapply prints once more; busy pulses do not queue extra output. | Pending | Pending |
| Automated advancement | Valid hopper/item-handler supply followed by successful redstone print credits the documented player once; failed/manual prints do not satisfy automation criterion. | Pending | Pending |
| Preset persistence | Load/title/resize/background color, break/re-place, then print; repeat after save/restart. Preset survives and inventory drops once; all new output is borderless. | Pending | Pending |
| Job interruption | Break/re-place or unload while loading/printing; old results do not alter replacement printers. Swap ink during processing; no wrong-mode supply consumption. | Pending | Pending |
| Placement | Hold Image, ordinary Minecraft item frame, supported wall and blocked wall; wall display has no decorative border; placing/dropping/reloading within a fresh 0.1.0 world preserves source, size, mode, and background metadata; placement advancement works. | Pending | Pending |
| Multiplayer/cache | Second client sees the same image; reconnect and exceed cache budget; evicted images reload without stuck placeholders or request floods. | N/A | Pending |
| Translation | English keys resolve. Select a different language to test English fallback; use a reviewed translation resource pack when available. No raw translation keys in tips, GUI, messages, advancements, background-color control, or tooltip condition/behaviour lines. | Pending | Pending |
| Safety/logging | Timeout and storage-full jobs leave machine usable; no sensitive URL text in player messages/logs; packets from invalid or distant menus rejected. | Pending | Pending |
| Fresh-world requirement | Start a new 0.1.0 world; confirm the release documentation clearly states that pre-0.1.0 development worlds and items are unsupported. No old-save migration test is required. | Pending | Pending |

Automated verification: `./gradlew.bat build`; retain test reports with the candidate checksum. Version is now 0.1.0. Exact-artifact fresh-instance sign-off and publication remain pending until all required rows pass or a failing feature is explicitly removed from the release contract. The art pass is implemented; rebuild and repeat exact-artifact checks after any further changes.

## Ink/background policy follow-up — 2026-09-11

Ink sacs now permit only pure black (`#000000`) or white (`#FFFFFF`) backgrounds. Other colors require a usable Color Cartridge. The palette disables colored swatches with an ink sac and supplies translated help; the black swatch and its Image tooltip name use pure black. Server background requests, print eligibility, and processor validation enforce the same rule. A colored preset is retained when ink is swapped, but incompatible printing is blocked until the player changes background or ink. Rejected manual and redstone requests consume nothing and produce no output.

All 7 required integration GameTests passed, including explicit colored/gray background rejection, rejected manual/redstone requests without supply consumption, and successful black manual / white redstone prints with the correct pixel and metadata colors. This run reused the isolated `build/gametest-0.1.0-run` test world and logged the previous test version changing from 0.1.0 to 0.0.12; it is regression evidence, not a fresh-world or exact-JAR manual sign-off. The ordinary distributable build excludes opt-in GameTest classes/resources.

The final ordinary `gradlew.bat build --offline` passes 108 unit tests with zero failures/errors, including background policy, supply swaps, processor rejection, black/white compositing, and pure-black tooltip naming. The refreshed test artifact checksum is recorded above; manual matrix rows remain pending.

## Clean monochrome follow-up — 2026-09-11

The earlier 0.0.11 test build passed 104 unit tests with zero failures/errors. Color printing still composites the original resized artwork. Monochrome dithers artwork only, excludes transparent pixels from error diffusion, preserves partial alpha coverage, and then composites the chosen background as a flat fill. This separation remains, but the newer ink/background policy restricts that fill to black or white. Tests now cover unspeckled black/white paper, background-independent opaque ink patterns, soft colored-source edges converted to monochrome, deterministic output, and opaque-image deduplication. The source/background preview is not a simulation of the ink-sac conversion.

Restart the client/server with this build and print new copies to test; already printed variants are immutable. Canvas sides/back retain the saved background color in both modes. Maintainer gameplay sign-off remains pending; prior GameTest results are historical rather than a fresh run of this follow-up.

## Current candidate and background follow-up — 2026-09-11

The pre-follow-up candidate passed 95 unit tests and all 6 server integration GameTests. The GameTests used `build/gametest-0.1.0-run`; the interactive development client uses the separate `build/smoke-client` directory. Neither is manual sign-off of the packaged JAR on a clean client/server installation.

The maintainer reported white transparent-looking areas in the preview and printed display, and white entity edges. The preview now explicitly enables alpha blending; the wall canvas front/back/edges now use the stored background color with Minecraft's neutral white texture rather than untinted white concrete. Regression coverage checks canvas colors, native PNG alpha preservation, and red fill over transparent-white pixels without replacing opaque white.

Read-only inspection of the loaded 225×225 source saved in the smoke-test world found no alpha pixels and 23,039 fully opaque white pixels. That file cannot change its white artwork through a transparency-only fill. Exact reproduction with the original PNG and maintainer re-testing remain pending; this row must not be marked Pass based only on processor tests.

The subsequently supplied clipboard attachment is a 700×700 WebP with 275,432 fully transparent and 5,253 partially transparent pixels, and no fully opaque white pixels. A standalone probe using the current `ImageProcessor` retained alpha at 700×700, 225×225, and 128×128. White, red (`#B02E26`), and blue-gray (`#224466`) variants filled every fully transparent source pixel correctly (275,432 / 28,458 / 9,213 checked pixels respectively). The image-processing path therefore passes with this attachment. The discrepancy with the saved opaque source still needs the original loading method/file or URL to investigate; a flattened thumbnail is a possibility, not a confirmed cause. This does not sign off GUI rendering or manual gameplay.

The earlier background follow-up passed `gradlew.bat build --offline` with 101 unit tests and zero failures/errors, using process-local `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=.`. That 0.1.0 artifact's SHA-256 was `2d3ffb57329f843c8fd5d2d0f0d8d8ffe7a16c49b3b4830b8086a5d1d74fb93f`; it predates the clean monochrome follow-up. The previously running development client must be restarted before re-testing; it does not hot-load rebuilt classes.

The pre-follow-up JAR checksum was `b5c488265a97b19e0112dd5dee03aaf153e28f6b6332f2f0aa6bbe5408d568b8`; it is superseded by this refreshed build.

## Preparation build — 2026-09-10

`gradlew.bat build --offline` passed: 39 tests, no failures, including 14 new tooltip/creative-tab cases. This built the development version 0.0.8, not the final release candidate. The unit-test target runs with `Dist.DEDICATED_SERVER`; the gameplay rows above remain pending.

On this Windows environment, Java's default temporary socket path caused `Unable to establish loopback connection`. The build succeeded with process-local `TEMP`, `TMP`, and `-Djava.io.tmpdir` pointing to `C:\hackerman\Printer\build\tmp`. No global Java/Gradle configuration was changed.

Follow-up diagnosis isolated the fix to the socket directory: `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=.` also passes task discovery and the full build (39 tests), without overriding `TEMP`/`TMP`. `java.io.tmpdir` alone did not fix socket creation. New VS Code terminals now receive the socket-specific option.

## 0.0.9 testing artifact — 2026-09-10

`gradlew.bat build --offline` succeeded using the socket-directory workaround. Gradle reused up-to-date build/test tasks; the reports contain 39 tests with zero failures or errors. The packaged metadata was checked and identifies version 0.0.9.

- Artifact: `build/libs/printer-0.0.9.jar`
- SHA-256: `fd0c5a942d0bd72d878a0ddf82423a83918028becbc205f592af2f8188004f44`
- Fresh client and dedicated-server gameplay checks remain pending. This artifact is for testing the preparation changes, not a signed-off 0.1.0 candidate.
