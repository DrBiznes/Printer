# Printer 0.1.0 release contract

Status: scope frozen; release gates still open. This is the target contract, not a release sign-off.
The current testing artifact is 0.0.9; 0.1.0 remains gated on the release checks.

## Supported environment

- Minecraft **1.21.1**, Java **21**, NeoForge **21.1.248** as the build and smoke-test baseline.
- Metadata permits later NeoForge 21.1 patches, but only the baseline is a release test promise. Other Minecraft versions, Forge, and Fabric are outside this release.
- Install the same Printer build on the client and dedicated server. Single-player uses the same server-owned image pipeline.
- No Create or Ponder dependency. Standard NeoForge item-handler integration is the automation contract; individual third-party transport mods are not certified.

## Gameplay and limits

| Area | 0.1.0 contract |
| --- | --- |
| Input | Direct HTTP(S) URLs to PNG, JPEG, WebP, GIF, BMP, TIFF, ICO, or TGA. No local upload in 0.1.0. |
| Decode | First frame/page only; transparency becomes white paper. AVIF, HEIC, SVG, HTML, and other unsupported bytes are rejected. |
| Remote response | At most three redirects. Missing/binary MIME types require valid supported image bytes. URLs with credentials are rejected. |
| Source decode ceiling | 4096 pixels per axis, at most 16,777,216 pixels, checked before pixel decoding. |
| Stored source resolution | Downscale to fit 1024×1024 by default; `images.maxWidth` and `maxHeight` each permit 1–4096. These settings resize accepted images; they are not the original-image rejection threshold. |
| Download bytes | 10 MiB by default, configurable from 1–64 MiB. |
| Processed PNG | At most 4 MiB per canonical source or print variant. |
| Storage | 256 MiB by default, configurable from 16–4096 MiB, counted across encoded source and variant data. Sources and variants each deduplicate by SHA-256. Storage-full jobs fail; there is no automatic deletion of old prints. |
| Timeouts | Configured fetch timeout 30 seconds by default, 5–120 permitted; connection timeout at most 15 seconds. End-to-end body timeout enforcement is an open hardening gate below. |
| Physical size | Automatic long edge up to 4 blocks by default; manual long edge up to 8. Both are configurable within 1–8. Whole-block dimensions approximate source aspect ratio. |
| Texture size | Up to 128 pixels per long-edge block, without upscaling the saved source. |
| Supplies | One paper per occupied block and one cartridge charge (color) or ink sac (monochrome) per successful print. A fresh cartridge has three charges. |
| Output | One Image item; output slot must be empty. Supplies are consumed when the job succeeds. |
| Placement | Supported vertical wall with enough clearance, or ordinary item frame. Unprinted creative Image stacks cannot make wall displays. |
| Preset | Saved source, title, size, and frame persist on the block and on the dropped Printer item. Image data remains in the originating world's save. |
| Display cache | Requested images are sent in chunks of at most 256 KiB. Client texture LRU budget defaults to 128 MiB, configurable from 16–1024 MiB. |

## Automation

Directions are relative to the printer's facing property, as implemented in `PrinterBlockEntity`:

- Top accepts ink; counterclockwise from facing accepts paper.
- Clockwise from facing or bottom extracts printed Images.
- For a player looking at the front panel, paper is on the player's right and output is on the player's left. Tooltips use this viewpoint to avoid ambiguous left/right directions.
- Front, back, and unsided requests expose no automated slots.
- One low-to-high redstone transition requests one print of the saved preset. Sustained power does not repeat. Pulses while busy are ignored, not queued.
- Prefer a button pulse followed by time for completion and output extraction. A side hopper must point into the paper face; a bottom hopper extracts output. Test all four printer orientations.

## Discovery and localization

The Printer creative tab orders Printer, Color Cartridge, then Image, using a Printer icon. These items are removed from the vanilla Functional Blocks insertion. Tooltips provide a short description and a translated Shift hint, with expanded workflow, automation, supplies, and placement details.

The shipped language is English. A documented, reviewable translation contribution workflow is the 0.1.0 community-ready deliverable permitted by the project plan; see [TRANSLATING.md](TRANSLATING.md). No unreviewed machine translation will be shipped. Every built-in message must still be translatable before release, even if English is the only bundled language.

## Open release blockers from the initial audit

These need implementation and/or explicit verification before the smoke-test sign-off:

1. **Asynchronous job identity and supplies:** completion callbacks look up only the block position, so a removed/replaced Printer can receive an old job's result. Ink type can also change while processing; completion checks supplies but does not confirm the selected print mode. Bind completion to the original block/job and validate or reserve supplies; add regressions for replacement, unload, and ink swaps.
2. **Errors and logging:** `PrinterJobService` sends English literals and raw exception messages; URL failures log the entire URL and exception. Use translated, actionable error categories and avoid logging URL credentials/query tokens or passing exception text to players.
3. **Download hardening:** the HTTP client resolves hosts separately from the address validation check. Audit DNS rebinding and the completeness of non-public address rejection. Reading an `InputStream` body also needs a tested total deadline. The two-worker executor has an unbounded queue and no per-player rate limiter; bound work before calling this release hardened.
4. **Request validity:** load/resize/frame/print packets check the open menu and position, but do not call `menu.stillValid(player)` as source-preview requests do. Add server-side validity checks and packet regressions.
5. **Image metadata:** printed Images store texture dimensions, not source dimensions. Tooltips deliberately label these as texture pixels. Add backward-compatible source metadata, preserve it through placement/drop/save/network round trips, and test old items before closing the full Image-tooltip checklist item.
6. **Build reproducibility:** the existing Parchment configuration names 1.21.11 with a nightly snapshot while Minecraft is 1.21.1. Select and validate matching stable mappings before the final artifact build; the first pass keeps the existing cached mapping setup.
7. **Gameplay gates:** texture redesign, advancement triggers and tests, full translation audit, client visual checks, dedicated-server gameplay checks, and exact-artifact smoke tests remain open.

## Implementation path for the remaining P0 work

| Gate | Implementation and evidence |
| --- | --- |
| Contract | This document; metadata bounded to Minecraft 1.21.1 / NeoForge 21.1. |
| Image art | Update the existing art workflow and the 16×16 asset; inspect inventory, preview, and fallback rendering. |
| Creative tab | Registered tab plus tests of icon, order, search contents, and no vanilla duplication. |
| Tooltips | All three registered items, both Shift states, blank/printed Images, all frames/modes, used/depleted cartridges; source metadata remains tracked above. |
| Advancements | Server-side success triggers for obtain, load, print, place, and automated print. Decide automation credit/ownership explicitly; test failure and repeated-trigger cases. |
| Localization | Replace literal job failures and audit GUI/config/advancement/entity text. Document contributor review and in-game validation. |
| Safety regressions | Address the identity, supply, request, and downloader gaps above before expanding input types. Upload protocol tests are inapplicable while upload is deferred. |
| Smoke tests | Run [SMOKE_TEST_0.1.0.md](SMOKE_TEST_0.1.0.md) on fresh saves and the exact final artifact. |
| Release packaging | Update changelog, README, credits/license review and metadata; bump to 0.1.0 only after release gates pass. Record artifact checksum and bug-report path. |

Local file upload, the broader art pass, Ponder scenes, Create scenes, extra frames, CC:Tweaked, version ports, and configurable ink economy are deferred. Reopening scope requires updating this contract and its verification matrix before cutting a release.
