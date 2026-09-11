# Changelog

## 0.0.9 — testing build

- Add a dedicated Printer creative tab with Printer, Color Cartridge, and Image in order, and remove their vanilla Functional Blocks insertion.
- Add translated summaries and Shift-expanded help for all three items, cartridge charge counts, untitled-image fallback text, and explicit no-frame details.
- Add regression coverage for registered item tooltip states, translation arguments, and creative-tab contents.
- Document the release contract, smoke-test matrix, and reviewed translation contribution workflow. Defer local upload from 0.1.0.
- Restrict advertised compatibility to Minecraft 1.21.1 and NeoForge 21.1.248 or later 21.1 patches.
- Configure VS Code terminals for Java's Windows socket-directory workaround and ignore IDE compiler output and temporary socket files.

## 0.0.8

- Fix printed images and previews remaining on the placeholder when the encoded PNG exceeds LWJGL's native stack capacity. Client decoding now uses a native heap buffer.
- Keep the retry delay after a failed decode to prevent repeated requests and log flooding.
- Add client decoder regressions for detailed 256×256, 512×512, and 1024×1024 images, including pixel-color checks and recovery after invalid input.
- Preserve the existing frame models, textures, and GUI design.
