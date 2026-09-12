# 0.1.0 release smoke tests

Run the same scenarios in a fresh single-player instance and on a fresh dedicated server with a separate client. Use Java 21, Minecraft 1.21.1, NeoForge 21.1.248 and the exact candidate JAR on both sides. Keep existing development worlds untouched. Only mark a result Pass after observing it; automated tests do not sign off gameplay.

Current test build: **0.0.11** (release target: **0.1.0**). Artifact: `build/libs/printer-0.0.11.jar`. SHA-256: `c8706e5cb81669de418c955ec6e58029d81387adb727f9e2b119b430858f0b36`.

Tester / date: **maintainer testing in progress — 2026-09-11**; exact-artifact matrix sign-off pending.

Instance settings and deviations: **pending**

| Scenario | Expected result | Single-player | Dedicated server |
| --- | --- | --- | --- |
| Startup | No missing registries, client-class loading failures, or error logs; recipes load. | Pending | Pending |
| Discoverability | Printer tab, printer icon; Printer / Color Cartridge / Image order; no Functional Blocks duplicates; creative search finds items. | Pending | Pending |
| Obtain/craft | Both recipes work; obtain advancement awards once. | Pending | Pending |
| URL load | Public direct PNG/JPEG and one bundled-codec fixture load, preview and sizing agree; load advancement only on success. | Pending | Pending |
| Invalid input | Malformed URL, unsupported file, HTML, non-public destination, and blocked host reject with translated messages. | Pending | Pending |
| Limits | Download over configured byte limit and image over 4096 pixels on an axis reject without allocating the full bitmap; accepted large source scales to configured saved resolution. | Pending | Pending |
| Manual print | Paper cost matches area; color consumes one charge, monochrome one sac; full output prevents print; first-print advancement fires only on success. | Pending | Pending |
| Background fill | With a PNG whose alpha is confirmed, switch white/red/another color: preview updates, a new color print fills transparent pixels accordingly, opaque white artwork remains white, and wall canvas margins/back/edges use the selected color. | Re-test pending; white pixels/edges reported | Pending |
| Clean monochrome | Print transparent artwork using ink sacs with white/red/another background. Only artwork is dithered to black/white; transparent regions stay uniformly background-colored, opaque ink patterns do not change with background selection, and soft alpha edges blend without retaining source hues. Entity sides/back keep the saved background color. | Pending | Pending |
| Opaque image canvas | Print the same fully opaque image with red and another background. The image artwork stays unchanged, but each placed entity's sides/back/exposed margins use that print's saved color, even if both prints share identical PNG content. Repeat after save/reload. | Pending | Pending |
| Tooltips | Every item with/without Shift; fresh/partially used/depleted cartridges; blank/titled/untitled Images; Create/AnalogAudio-style summary/condition/behaviour formatting; background color details; no decorative frame data. | Pending | Pending |
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

Automated verification: `./gradlew.bat build`; retain test reports with the candidate checksum. The working version is currently 0.0.11 for maintainer testing; restore 0.1.0 for the final artifact. Exact-artifact fresh-instance sign-off and publication remain pending until all required rows pass or a failing feature is explicitly removed from the release contract. Texture refinement and GUI texture changes remain a separate final pass; rebuild and repeat exact-artifact checks after that pass.

## Clean monochrome follow-up — 2026-09-11

The current 0.0.11 test build passes 104 unit tests with zero failures/errors. Color printing still composites the original resized artwork. Monochrome now dithers artwork only, excludes transparent pixels from error diffusion, preserves partial alpha coverage, and then composites the chosen background as a flat fill. Tests cover unspeckled colored paper, background-independent opaque ink patterns, soft colored-source edges converted to monochrome, deterministic output, and opaque-image deduplication. The source/background preview is not a simulation of the ink-sac conversion.

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
