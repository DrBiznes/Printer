"""Generate Printer's original 16x16 programmer-art PNG textures."""

from pathlib import Path
import struct
import zlib


ROOT = Path(__file__).resolve().parents[1]
TEXTURES = ROOT / "src" / "main" / "resources" / "assets" / "printer" / "textures"


def canvas(color):
    return [[color for _ in range(16)] for _ in range(16)]


def rect(pixels, x0, y0, x1, y1, color):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if 0 <= x < 16 and 0 <= y < 16:
                pixels[y][x] = color


def write_png(path, pixels):
    path.parent.mkdir(parents=True, exist_ok=True)
    raw = b"".join(b"\x00" + b"".join(bytes(pixel) for pixel in row) for row in pixels)

    def chunk(kind, payload):
        return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9))
    png += chunk(b"IEND", b"")
    path.write_bytes(png)


def printer_face(active=False):
    p = canvas((56, 61, 68, 255))
    rect(p, 1, 1, 14, 14, (91, 99, 108, 255))
    rect(p, 2, 2, 13, 5, (39, 43, 49, 255))
    rect(p, 4, 3, 11, 4, (174, 218, 218, 255))
    rect(p, 3, 8, 12, 13, (37, 39, 44, 255))
    rect(p, 5, 9, 10, 12, (235, 232, 210, 255))
    rect(p, 11, 6, 12, 7, (66, 214, 105, 255) if active else (100, 119, 105, 255))
    if active:
        rect(p, 6, 13, 9, 15, (235, 232, 210, 255))
    return p


def side_panel(accent):
    p = canvas((55, 60, 67, 255))
    rect(p, 1, 1, 14, 14, (83, 91, 100, 255))
    rect(p, 3, 4, 12, 11, (37, 41, 46, 255))
    rect(p, 5, 6, 10, 9, accent)
    return p


def main():
    image = canvas((0, 0, 0, 0))
    rect(image, 1, 1, 14, 14, (237, 232, 207, 255))
    rect(image, 2, 2, 13, 13, (62, 109, 139, 255))
    rect(image, 3, 9, 12, 12, (75, 132, 75, 255))
    rect(image, 4, 7, 8, 10, (111, 164, 93, 255))
    rect(image, 10, 4, 11, 5, (249, 214, 94, 255))

    cartridge = canvas((0, 0, 0, 0))
    rect(cartridge, 4, 1, 11, 14, (42, 44, 49, 255))
    rect(cartridge, 5, 2, 10, 3, (112, 120, 126, 255))
    rect(cartridge, 5, 5, 6, 11, (35, 194, 204, 255))
    rect(cartridge, 7, 5, 8, 11, (211, 56, 161, 255))
    rect(cartridge, 9, 5, 10, 11, (238, 210, 52, 255))

    top = canvas((71, 78, 86, 255))
    rect(top, 2, 2, 13, 13, (91, 99, 108, 255))
    rect(top, 5, 4, 10, 11, (32, 35, 40, 255))
    rect(top, 6, 5, 9, 10, (211, 56, 161, 255))

    bottom = canvas((52, 57, 63, 255))
    for i in range(2, 14, 3):
        rect(bottom, i, 2, i, 13, (40, 44, 49, 255))

    back = canvas((63, 69, 76, 255))
    rect(back, 2, 2, 13, 13, (82, 90, 99, 255))
    rect(back, 5, 5, 10, 10, (46, 51, 57, 255))
    rect(back, 7, 7, 8, 8, (174, 64, 58, 255))

    outputs = {
        "item/image.png": image,
        "item/color_cartridge.png": cartridge,
        "block/printer_front.png": printer_face(False),
        "block/printer_front_printing.png": printer_face(True),
        "block/printer_back.png": back,
        "block/printer_input.png": side_panel((235, 232, 210, 255)),
        "block/printer_output.png": side_panel((95, 167, 215, 255)),
        "block/printer_top.png": top,
        "block/printer_bottom.png": bottom,
    }
    for name, pixels in outputs.items():
        write_png(TEXTURES / name, pixels)
        print(f"generated {name}")


if __name__ == "__main__":
    main()
