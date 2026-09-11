# 0.1.0 release smoke tests

Run the same scenarios in a fresh single-player instance and on a fresh dedicated server with a separate client. Use Java 21, Minecraft 1.21.1, NeoForge 21.1.248 and the exact candidate JAR on both sides. Keep existing development worlds untouched. Only mark a result Pass after observing it; automated tests do not sign off gameplay.

Candidate version / SHA-256: **pending**

Tester / date: **pending**

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
| Tooltips | Every item with/without Shift; fresh/partially used/depleted cartridges; blank/titled/untitled Images; Create/AnalogAudio-style summary/condition/behaviour formatting; background color details; no decorative frame data. | Pending | Pending |
| Hoppers | Ink above, paper at intended side, extraction below/right; invalid sides/items rejected. Repeat in all four horizontal orientations. | Pending | Pending |
| Redstone | First rising edge prints once, held signal does not repeat, release/reapply prints once more; busy pulses do not queue extra output. | Pending | Pending |
| Automated advancement | Valid hopper/item-handler supply followed by successful redstone print credits the documented player once; failed/manual prints do not satisfy automation criterion. | Pending | Pending |
| Preset persistence | Load/title/resize/background color, break/re-place, then print; repeat after save/restart. Preset survives and inventory drops once; all new output is borderless. | Pending | Pending |
| Job interruption | Break/re-place or unload while loading/printing; old results do not alter replacement printers. Swap ink during processing; no wrong-mode supply consumption. | Pending | Pending |
| Placement | Hold Image, ordinary Minecraft item frame, supported wall and blocked wall; wall display has no decorative border; placing/dropping/reloading preserves source, size, mode, and background metadata; placement advancement works. | Pending | Pending |
| Multiplayer/cache | Second client sees the same image; reconnect and exceed cache budget; evicted images reload without stuck placeholders or request floods. | N/A | Pending |
| Translation | English keys resolve. Select a different language to test English fallback; use a reviewed translation resource pack when available. No raw translation keys in tips, GUI, messages, advancements, background-color control, or tooltip condition/behaviour lines. | Pending | Pending |
| Safety/logging | Timeout and storage-full jobs leave machine usable; no sensitive URL text in player messages/logs; packets from invalid or distant menus rejected. | Pending | Pending |
| Save compatibility | Copy a 0.0.8 test save, load existing printers/items/displays, verify metadata and printing. Do not overwrite the original save. | Pending | Pending |

Automated verification: `./gradlew.bat build`; retain test reports with the candidate checksum. The final version bump, fresh-instance runs, and publication remain blocked until all required rows pass or a failing feature is explicitly removed from the release contract.

## Preparation build — 2026-09-10

`gradlew.bat build --offline` passed: 39 tests, no failures, including 14 new tooltip/creative-tab cases. This built the development version 0.0.8, not the final release candidate. The unit-test target runs with `Dist.DEDICATED_SERVER`; the gameplay rows above remain pending.

On this Windows environment, Java's default temporary socket path caused `Unable to establish loopback connection`. The build succeeded with process-local `TEMP`, `TMP`, and `-Djava.io.tmpdir` pointing to `C:\hackerman\Printer\build\tmp`. No global Java/Gradle configuration was changed.

Follow-up diagnosis isolated the fix to the socket directory: `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=.` also passes task discovery and the full build (39 tests), without overriding `TEMP`/`TMP`. `java.io.tmpdir` alone did not fix socket creation. New VS Code terminals now receive the socket-specific option.

## 0.0.9 testing artifact — 2026-09-10

`gradlew.bat build --offline` succeeded using the socket-directory workaround. Gradle reused up-to-date build/test tasks; the reports contain 39 tests with zero failures or errors. The packaged metadata was checked and identifies version 0.0.9.

- Artifact: `build/libs/printer-0.0.9.jar`
- SHA-256: `fd0c5a942d0bd72d878a0ddf82423a83918028becbc205f592af2f8188004f44`
- Fresh client and dedicated-server gameplay checks remain pending. This artifact is for testing the preparation changes, not a signed-off 0.1.0 candidate.
