# Changelog

## 0.0.8

- Fix printed images and previews remaining on the placeholder when the encoded PNG exceeds LWJGL's native stack capacity. Client decoding now uses a native heap buffer.
- Keep the retry delay after a failed decode to prevent repeated requests and log flooding.
- Add client decoder regressions for detailed 256×256, 512×512, and 1024×1024 images, including pixel-color checks and recovery after invalid input.
- Preserve the existing frame models, textures, and GUI design.
