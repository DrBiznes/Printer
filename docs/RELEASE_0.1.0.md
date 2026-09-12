# Printer 0.1.0 release contract

Status: the no-frame/background-color feature contract is frozen. Functional implementation targets the 0.1.0 release candidate; texture refinement and GUI texture changes are intentionally deferred to the final separate pass. Fresh-instance testing of the exact final JAR remains mandatory before publication. 0.1.0 is an intentional pre-release breaking change and requires new worlds. This is not a release sign-off.

## Compatibility policy

Printer has not had a public release yet, so 0.1.0 does not carry compatibility or migration code for development builds. Existing development worlds, old Printer items, and old Image/frame metadata are unsupported. Start a new world when testing or playing 0.1.0.

## Supported environment

- Minecraft **1.21.1**, Java **21**, NeoForge **21.1.248** as the build and smoke-test baseline.
- Metadata permits later NeoForge 21.1 patches, but only the baseline is a release test promise. Other Minecraft versions, Forge, and Fabric are outside this release.
- Install the same Printer build on the client and dedicated server. Single-player uses the same server-owned image pipeline.
- No Create or Ponder dependency. Standard NeoForge item-handler integration is the automation contract; individual third-party transport mods are not certified.

## Gameplay and limits

| Area | 0.1.0 contract |
| --- | --- |
| Input | Direct HTTP(S) URLs or local files selected by the client to PNG, JPEG, WebP, GIF, BMP, TIFF, ICO, or TGA. |
| Decode | First frame/page only; transparent/empty pixels are composited against the selected background color, white by default. AVIF, HEIC, SVG, HTML, and other unsupported bytes are rejected. |
| Remote response | At most three redirects. Missing/binary MIME types require valid supported image bytes. URLs with credentials are rejected. |
| Source decode ceiling | 4096 pixels per axis, at most 16,777,216 pixels, checked before pixel decoding. |
| Stored source resolution | Downscale to fit 1024×1024 by default; `images.maxWidth` and `maxHeight` each permit 1–4096. These settings resize accepted images; they are not the original-image rejection threshold. |
| Download bytes | 10 MiB by default, configurable from 1–64 MiB. |
| Processed PNG | At most 4 MiB per canonical source or print variant. |
| Storage | 256 MiB by default, configurable from 16–4096 MiB, counted across encoded source and variant data. Sources and variants each deduplicate by SHA-256. Storage-full jobs fail; there is no automatic deletion of old prints. |
| Timeouts | Configured end-to-end fetch/body timeout 30 seconds by default, 5–120 permitted; connection timeout at most 15 seconds. |
| Physical size | Automatic long edge up to 4 blocks by default; manual long edge up to 8. Both are configurable within 1–8. Whole-block dimensions approximate source aspect ratio. |
| Texture size | Up to 128 pixels per long-edge block, without upscaling the saved source. |
| Supplies | One paper per occupied block and one cartridge charge (color) or ink sac (monochrome) per successful print. A fresh cartridge has three charges. |
| Output | One Image item; output slot must be empty. Supplies are consumed when the job succeeds. |
| Placement | Supported vertical wall with enough clearance, or ordinary Minecraft item frame. Printed wall displays always use no decorative border. Unprinted creative Image stacks cannot make wall displays. |
| Preset | Saved source, title, size, and background color persist on the block and on the dropped Printer item. All 0.1.0 presets use no decorative border. Image data remains in the originating world's save. |
| Background | Native 24-bit RGB, white (`#FFFFFF`) by default. The compact 16-swatch palette follows vanilla dye-color ordering inspired by Analog Audio, with pure white for the default and pure black for the black swatch. Ink-sac printing allows only `#000000` or `#FFFFFF`; all other backgrounds require a usable Color Cartridge. The palette and server background/print requests enforce the rule, including redstone; incompatible presets block printing without consuming supplies until the player chooses black/white or switches ink. Sources preserve alpha. Color variants composite the original artwork; monochrome variants dither only artwork before compositing the selected black or white background as a flat color. Transparent pixels do not participate in error diffusion, soft alpha edges retain their coverage, and opaque artwork's black/white pattern is independent of background choice. The placed entity's sides, back, and exposed canvas margins always use its saved background color, including for fully opaque images. Identical final PNG bytes deduplicate even if opaque sources make two color selections visually identical on the image face; entity canvas colors still follow each Image's metadata. No frame field or old metadata fallback is stored. |
| Display cache | Requested images are sent in chunks of at most 256 KiB. The client verifies the content hash, expires incomplete transfers, and bounds in-flight assembly. Texture LRU budget defaults to 128 MiB, configurable from 16–1024 MiB. |

Gallery requests permit a burst of 16, replenished at 10 requests per second. Response bytes permit a 32 MiB burst, replenished at 8 MiB per second per player; missing/rate-limited images can retry after the client's five-second deadline. These transport safeguards do not change the image, upload, storage, sizing, or automation limits.

Local uploads use a 24 KiB client-to-server chunk protocol, one active transfer per player, a 64 MiB transfer ceiling, a 120-second deadline, cancellation/disconnect cleanup, and the server's existing decode, dimension, storage, and request limits. The server never receives or reads a client filesystem path.

## Automation

Directions are relative to the printer's facing property, as implemented in `PrinterBlockEntity`:

- Top accepts ink; counterclockwise from facing accepts paper.
- Clockwise from facing or bottom extracts printed Images.
- For a player looking at the front panel, paper is on the player's right and output is on the player's left. Tooltips use this viewpoint to avoid ambiguous left/right directions.
- Front, back, and unsided requests expose no automated slots.
- One low-to-high redstone transition requests one print of the saved preset. Sustained power does not repeat. Pulses while busy are ignored, not queued. Supplies, output occupancy, and the busy gate are the print-rate gates.
- Prefer a button pulse followed by time for completion and output extraction. A side hopper must point into the paper face; a bottom hopper extracts output. Test all four printer orientations.

## Discovery and localization

The Printer creative tab orders Printer, Color Cartridge, then Image, using a Printer icon. These items are removed from the vanilla Functional Blocks insertion. Tooltips provide translated titles or summaries, a block size for printed Images, a Shift hint, and expanded workflow, automation, supplies, placement, and background-color details using the Create/AnalogAudio condition/behaviour presentation style.

The shipped language is English. A documented, reviewable translation contribution workflow is the 0.1.0 community-ready deliverable permitted by the project plan; see [TRANSLATING.md](TRANSLATING.md). No unreviewed machine translation will be shipped. Every built-in message must still be translatable before release, even if English is the only bundled language.

## Completed implementation audit

The 0.0.10 implementation addressed the initial audit items:

1. **Asynchronous job identity and supplies:** implemented with block identity/job generations, loaded-chunk checks, mode validation, and supply rechecks; covered by unit and GameTest regressions.
2. **Errors and logging:** implemented with translated categories and sanitized debug logging.
3. **Download hardening:** implemented with the validated Apache resolver, public-address checks, redirect/body deadlines, bounded workers, and shared URL/upload throttling.
- Request-validity checks now verify the open, nearby menu on server-bound actions.
- Printed Images carry source-dimension metadata through placement/drop/save/network round trips; this is part of the new 0.1.0 data model, not a promise to load older development items.
- The version and mappings are aligned to Minecraft 1.21.1 / NeoForge 21.1.248 for the current preparation build.
- Advancements, translation-key coverage, local upload, automated tests, and regression coverage are implemented.

## Functional implementation completed for the candidate

1. **Frame simplification:** the selector, `PrintFrame` enum, frame metadata codecs, legacy lookup/cycling, and decorative-border renderer are deleted. All new prints are unconditionally borderless.
2. **Background color:** a texture-independent two-row, 16-swatch in-GUI palette replaces the frame button. The selected RGB value persists through saves, dropped Printer items, Image components, placed/dropped entities, and networks. Canonical sources retain transparency; final variant content incorporates the selected background and remains SHA-256 verified.
3. **Tooltip presentation:** every item uses translated summary, condition, and behaviour keys where applicable. Printed Images show their title and block size without Shift; Shift reveals gold condition headings and gray metadata/behaviour lines. Image metadata reports background color and never frame data, source URLs, or internal IDs. The common keyboard bridge remains dedicated-server safe.
4. **Packaging:** candidate metadata is 0.1.0, with the existing author edit retained. README recipes/support expectations, changelog, credits, translation instructions, and packaged MIT/third-party license notes are updated.

## Remaining release gates

1. **Art and GUI textures:** final 16×16 Image refinement and the block/cartridge/GUI/ghost-slot/fallback/status/control art pass are deferred at the maintainer's request. The functional palette does not alter PNG assets.
2. **Release verification:** complete visual tooltip checks at normal and large GUI scales and the full client/dedicated-server smoke matrix against the exact final 0.1.0 JAR. Automated source-runtime checks are supplementary evidence, not a substitute for exact-artifact gameplay sign-off.

## Implementation path for the remaining P0 work

| Gate | Implementation and evidence |
| --- | --- |
| Contract | This document; metadata bounded to Minecraft 1.21.1 / NeoForge 21.1. |
| Image art | Update the existing art workflow and the 16×16 asset; inspect inventory, preview, and fallback rendering. |
| Creative tab | Registered tab plus tests of icon, order, search contents, and no vanilla duplication. |
| Tooltips | All three registered items, both Shift states, blank/printed Images, used/depleted cartridges, background color details, and the final Create/AnalogAudio-style formatting; no frame data is shown. |
| Advancements | Server-side success triggers for obtain, load, print, place, and automated print. Decide automation credit/ownership explicitly; test failure and repeated-trigger cases. |
| Localization | Replace literal job failures and audit GUI/config/advancement/entity text. Document contributor review and in-game validation. |
| Safety regressions | Unit and GameTest checks cover identity, supply, request budgets, downloads, upload validation/transfers, content hashes, and no-frame/background metadata. |
| Smoke tests | Run [SMOKE_TEST_0.1.0.md](SMOKE_TEST_0.1.0.md) on fresh saves and the exact final artifact. |
| Release packaging | Candidate version is 0.1.0; the MIT license and credits ship under `META-INF`. Record the candidate checksum and [bug-report path](https://github.com/DrBiznes/Printer/issues). Do not publish until the separately deferred art pass and exact-artifact smoke gates pass. |

Full Ponder scenes, Create scenes, extra frame profiles/materials, CC:Tweaked, version ports, and configurable ink economy are deferred until after 0.1.0. Background color and tooltip presentation are in scope for 0.1.0. Reopening scope requires updating this contract and its verification matrix before cutting a release.
