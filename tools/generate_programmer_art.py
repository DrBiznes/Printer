"""Original Printer pixel art. Requires Pillow; no generated imagery or borrowed assets.

All block/item/ghost textures are 16x16. GUI pixels are Minecraft GUI units.
Run from any directory. The contact sheet is a design preview, not a game capture.
"""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
TEXTURES = ROOT / 'src/main/resources/assets/printer/textures'
INK = '#283331'
SHADOW = '#596052'
MID = '#9c9c80'
SHELL = '#c6c3a0'
LIGHT = '#e8dfba'
PAPER = '#fff0d0'
TEAL = '#578f87'
CYAN = '#6ebbb4'
MAGENTA = '#c66e85'
GOLD = '#dcb564'
WOOD = '#776047'


def new(size=(16,16), color=(0,0,0,0)):
    im = Image.new('RGBA', size, color)
    return im, ImageDraw.Draw(im)


def box(d, xy, fill, hi=LIGHT, lo=SHADOW):
    x,y,r,b = xy
    d.rectangle(xy, fill)
    d.line((x,y,r-1,y), fill=hi)
    d.line((x,y,x,b-1), fill=hi)
    d.line((x,b,r,b), fill=lo)
    d.line((r,y,r,b), fill=lo)


def case():
    im,d = new(color=INK)
    box(d,(1,1,14,14),SHELL)
    d.line((2,6,13,6), fill=MID)
    d.line((2,7,13,7), fill=LIGHT)
    d.line((1,14,14,14), fill=WOOD)
    for x in (2,13):
        d.point((x,2),fill=SHADOW)
        d.point((x,3),fill=LIGHT)
    return im,d


def front(active=False):
    im,d = case()
    box(d,(3,2,10,5),INK,SHADOW,LIGHT)
    d.line((4,3,8,3), fill=CYAN if active else TEAL)
    d.point((9,4),fill=GOLD)
    d.rectangle((12,3,13,4),fill=GOLD if active else TEAL)
    d.line((3,9,12,9),fill=SHADOW)
    d.rectangle((2,10,13,12),fill=INK)
    d.line((3,12,12,12),fill=MID)
    d.line((3,13,12,13),fill=LIGHT)
    if active:
        d.rectangle((5,10,10,14),fill=PAPER)
        d.line((6,11,9,11),fill=TEAL)
        d.line((6,12,9,12),fill=MAGENTA)
        d.line((6,13,9,13),fill=GOLD)
    else:
        d.line((4,11,11,11),fill='#647165')
    return im


def side(output=False):
    im,d = case()
    box(d,(3,3,12,11),MID,SHADOW,LIGHT)
    d.rectangle((4,5,11,8),fill=INK)
    if output:
        d.rectangle((5,6,8,6),fill=CYAN)
        d.polygon([(8,5),(10,7),(8,8)],fill=CYAN)
    else:
        d.rectangle((5,5,10,8),fill=PAPER)
        d.line((6,6,9,6),fill=MID)
        d.line((6,8,9,8),fill=SHADOW)
    d.line((5,10,10,10),fill=SHADOW)
    return im


def image_item():
    im,d = new()
    d.rectangle((2,2,14,14),fill=INK)
    box(d,(1,1,13,13),PAPER, '#fff8e3', '#afa286')
    d.rectangle((2,2,12,10),fill='#619995')
    d.rectangle((2,2,12,3),fill='#416c75')
    d.rectangle((9,4,10,5),fill=GOLD)
    d.polygon([(2,10),(5,5),(9,10)],fill='#365a4e')
    d.polygon([(5,10),(9,7),(12,9),(12,10)],fill='#93ac75')
    d.point((5,6),fill=PAPER)
    d.line((3,12,6,12),fill=MID)
    return im


def cartridge():
    im,d = new()
    d.rectangle((5,1,10,2),fill=INK)
    d.line((6,1,9,1),fill=MID)
    box(d,(3,3,12,13),SHADOW,MID,INK)
    box(d,(4,4,11,11),PAPER,LIGHT,WOOD)
    for x,c in [(5,CYAN),(7,MAGENTA),(9,GOLD)]:
        d.rectangle((x,5,x+1,9),fill=c)
        d.point((x,5),fill=LIGHT)
    d.line((5,11,10,11),fill=INK)
    for x in (5,8,10):
        d.rectangle((x,13,x,14),fill=GOLD)
    return im


def ghost(kind):
    im,d = new()
    c='#8e9e8a'
    if kind=='paper':
        d.polygon([(4,2),(9,2),(12,5),(12,13),(4,13)],outline=c)
        d.line((9,2,9,5,12,5),fill=c)
        d.line((6,8,10,8),fill=c)
        d.line((6,10,9,10),fill=c)
    elif kind=='ink':
        d.rectangle((5,2,10,3),outline=c)
        d.rectangle((3,4,12,12),outline=c)
        for x in (5,7,9): d.line((x,6,x,10),fill=c)
    else:
        d.rectangle((2,2,13,13),outline=c)
        d.polygon([(4,10),(7,6),(11,10)],outline=c)
        d.point((10,5),fill=c)
    return im


def panel():
    im,d = new((256,238))
    box(d,(0,0,255,237),INK,SHADOW,INK)
    box(d,(2,2,253,235),WOOD,'#af8e61','#4d4737')
    # Timber cheek strips, on the same pixel grid as the machine case.
    for x in (3,4,251,252):
        for y in range(5,232,8):
            d.line((x,y,x,y+4),fill='#8e7450' if x%2 else '#66523e')
    box(d,(6,3,249,234),SHELL,LIGHT,SHADOW)
    d.rectangle((8,5,247,15),fill=INK)
    for x in (8,246):
        for y in (20,146,230):
            d.rectangle((x,y,x+1,y+1),fill=SHADOW)
            d.point((x,y),fill=LIGHT)
    # Colored glass text wells and viewfinder.
    box(d,(10,27,173,46), '#30493f',INK,LIGHT)
    d.line((12,29,171,29),fill='#486355')
    box(d,(10,50,107,69),'#514c36',INK,LIGHT)
    d.line((12,52,105,52),fill='#716648')
    box(d,(188,27,243,77),INK,SHADOW,LIGHT)
    d.rectangle((190,29,241,75),fill='#47584c')
    for x in range(192,241,4):
        d.point((x,30),fill='#6c7c62')
        d.point((x,74),fill='#6c7c62')
    d.line((10,73,177,73),fill=MID)
    d.line((10,74,177,74),fill=LIGHT)
    for x,accent in [(18,GOLD),(48,MAGENTA),(112,CYAN)]:
        box(d,(x-2,89,x+17,108), '#4c594d',INK,LIGHT)
        d.line((x,110,x+15,110),fill=accent)
    d.line((72,98,103,98),fill=SHADOW)
    d.polygon([(100,95),(104,98),(100,101)],fill=SHADOW)
    box(d,(176,91,223,108),'#394a3f',INK,LIGHT)
    # Status strip and separate inventory drawer.
    d.rectangle((10,133,245,142),fill=INK)
    box(d,(42,153,213,232),MID,SHADOW,LIGHT)
    for y in (155,173,191,213):
        for col in range(9):
            x=48+18*col
            box(d,(x-1,y-1,x+16,y+16),'#8d957d','#555f51',LIGHT)
            d.line((x,y,x+14,y),fill='#737e69')
    # Finger grips flanking inventory, and cartridge registration marks.
    for x in (19,227):
        for y in range(168,209,4):
            d.line((x,y,x+8,y),fill=SHADOW)
            d.line((x,y+1,x+8,y+1),fill=LIGHT)
    for i,c in enumerate((CYAN,MAGENTA,GOLD)):
        d.rectangle((14+i*7,224,18+i*7,228),fill=c)
    return im


def main():
    top,d=case()
    box(d,(4,3,11,12),INK,SHADOW,LIGHT)
    d.rectangle((5,4,10,10),fill=SHADOW)
    for x,c in ((5,CYAN),(7,MAGENTA),(9,GOLD)):
        d.line((x,5,x,9),fill=c)
    d.line((6,12,9,12),fill=GOLD)
    back,d=case()
    for y in (4,6,8):
        d.line((4,y,11,y),fill=INK)
        d.line((4,y+1,11,y+1),fill=MID)
    d.rectangle((9,11,11,12),fill=INK)
    d.point((10,11),fill=MAGENTA)
    d.line((4,11,6,11),fill=LIGHT)
    bottom,d=new(color=SHADOW)
    box(d,(2,2,13,13),MID,SHADOW,INK)
    for x in (2,11):
        for y in (2,11): box(d,(x,y,x+2,y+2),INK,WOOD,INK)
    for y in (6,8): d.line((5,y,10,y),fill=SHADOW)
    outputs={'block/printer_front.png':front(), 'block/printer_front_printing.png':front(True),
        'block/printer_input.png':side(), 'block/printer_output.png':side(True),
        'block/printer_top.png':top,'block/printer_back.png':back,'block/printer_bottom.png':bottom,
        'item/image.png':image_item(),'item/color_cartridge.png':cartridge(),'gui/printer.png':panel()}
    for kind in ('paper','ink','output'): outputs[f'gui/ghost_{kind}.png']=ghost(kind)
    for name,im in outputs.items():
        p=TEXTURES/name
        p.parent.mkdir(parents=True,exist_ok=True)
        im.save(p)
        assert im.size == ((256,238) if name=='gui/printer.png' else (16,16))
    preview(outputs)
    print(f'Generated {len(outputs)} original textures and build/art-preview.png')


def preview(outputs):
    # Use the actual Minecraft bitmap font when a development resources jar exists.
    import zipfile
    import io
    resources=ROOT/'build/moddev/artifacts/neoforge-21.1.248-client-extra-aka-minecraft-resources.jar'
    font_atlas=None
    if resources.exists():
        with zipfile.ZipFile(resources) as z:
            font_atlas=Image.open(io.BytesIO(z.read('assets/minecraft/textures/font/ascii.png'))).convert('RGBA')
    def text(im,s,x,y,color=INK,center=False):
        widths=[]
        for ch in s:
            if ch==' ': widths.append(4); continue
            n=ord(ch)
            glyph=font_atlas.crop((n%16*8,n//16*8,n%16*8+8,n//16*8+8)) if font_atlas else None
            widths.append((glyph.getbbox()[2]+1) if glyph and glyph.getbbox() else 6)
        if center: x-=sum(widths)//2
        for ch,w in zip(s,widths):
            if font_atlas and ch!=' ':
                n=ord(ch)
                glyph=font_atlas.crop((n%16*8,n//16*8,n%16*8+8,n//16*8+8))
                tint=Image.new('RGBA',(8,8),color); tint.putalpha(glyph.getchannel('A'))
                im.alpha_composite(tint,(x,y))
            elif not font_atlas: ImageDraw.Draw(im).text((x,y),ch,fill=color)
            x+=w
    im=outputs['gui/printer.png'].copy()
    d=ImageDraw.Draw(im)
    for s,x,y,c in [('Printer',20,7,LIGHT),('P-01 / COLOR',171,7,MID),('IMAGE SOURCE',12,18,INK),
        ('https://example.org/art.webp',14,32,'#bde0cd'),('Mountain study',14,55,'#f1d699'),('PAPER',12,79,INK),
        ('INK',48,79,INK),('OUT',104,79,INK),('12 paper',12,118,INK),('Ready',12,134,'#bde0cd'),('Inventory',48,143,INK)]:
        text(im,s,x,y,c)
    text(im,'PREVIEW',216,18,INK,True);text(im,'SIZE',200,79,INK,True);text(im,'4 x 3',200,96,LIGHT,True)
    for x,y,w,h,s,c in [(112,51,62,18,'Load',TEAL),(154,91,18,18,'-',WOOD),(228,91,18,18,'+',WOOD),
        (100,114,44,18,'Print','#af653f'),(154,114,92,18,'Oak',WOOD)]:
        d.rectangle((x,y,x+w-1,y+h-1),fill=INK)
        d.rectangle((x+1,y+1,x+w-2,y+h-4),fill=c)
        d.line((x+1,y+1,x+w-2,y+1),fill=LIGHT)
        text(im,s,x+w//2,y+(h-9)//2-1,LIGHT,True)
    for x,k in [(18,'paper'),(48,'ink'),(112,'output')]: im.alpha_composite(ghost(k),(x,91))
    im.alpha_composite(image_item().resize((44,44),Image.Resampling.NEAREST),(194,30))
    board=Image.new('RGBA',(900,810),'#202927')
    board.alpha_composite(im.resize((768,714),Image.Resampling.NEAREST),(12,48))
    text(board,'PRINTER / P-01 - PROGRAMMATIC ART PREVIEW',12,18,LIGHT)
    for i,(name,asset) in enumerate((n,a) for n,a in outputs.items() if n.startswith(('item/','block/'))):
        board.alpha_composite(asset.resize((64,64),Image.Resampling.NEAREST),(810,48+i*78))
    text(board,'16 x 16 BLOCKS + ITEMS / PIXEL GUI / ORIGINAL ASSETS',12,784,LIGHT)
    p=ROOT/'build/art-preview.png';p.parent.mkdir(exist_ok=True);board.save(p)

if __name__=='__main__': main()
