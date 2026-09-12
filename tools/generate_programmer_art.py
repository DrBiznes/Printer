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
    return im,d


def front(active=False):
    im,d = case()
    # The only control panel: broad dark glass, display and a warm action key.
    box(d,(2,3,13,9),INK,SHADOW,LIGHT)
    d.rectangle((3,4,8,7),fill='#30493f')
    d.line((4,5,7,5),fill=CYAN if active else TEAL)
    d.line((4,6,6 if active else 5,6),fill=CYAN if active else TEAL)
    d.rectangle((10,5,11,6),fill=GOLD if active else '#af653f')
    d.line((4,12,7,12),fill=SHADOW)
    d.point((11,12),fill=GOLD if active else TEAL)
    return im


def side(output=False):
    im,d = case()
    # Trays occur only on the two automation sides.
    if output:
        d.rectangle((2,8,13,10),fill=INK)
        d.rectangle((4,9,11,12),fill=PAPER)
        d.line((5,10,8,10),fill=TEAL)
        d.line((5,11,10,11),fill=MAGENTA)
        d.line((3,13,12,13),fill=SHADOW)
    else:
        d.rectangle((4,3,11,7),fill=SHADOW)
        d.rectangle((5,3,10,7),fill=PAPER)
        d.line((5,4,10,4),fill=LIGHT)
        d.rectangle((2,7,13,9),fill=INK)
        d.line((3,9,12,9),fill=MID)
        d.line((3,10,12,10),fill=LIGHT)
    return im


def image_item():
    im,d = new()
    # Square paper silhouette, flat landscape, no smoothing or rounded edges.
    d.rectangle((2,1,13,14),fill=SHADOW)
    d.rectangle((3,2,12,13),fill=PAPER)
    d.rectangle((4,3,11,10),fill=TEAL)
    d.rectangle((9,4,10,5),fill=GOLD)
    d.polygon([(4,10),(4,8),(6,6),(10,10)],fill=INK)
    d.polygon([(7,10),(10,7),(11,8),(11,10)],fill=SHADOW)
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
    box(d,(10,50,133,69),'#514c36',INK,LIGHT)
    d.line((12,52,131,52),fill='#716648')
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
    d.rectangle((10,132,245,141),fill=INK)
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
    box(d,(4,6,11,9),INK,SHADOW,LIGHT)
    d.line((5,7,10,7),fill='#182321')
    for x,c in ((5,CYAN),(7,MAGENTA),(9,GOLD)):
        d.point((x,10),fill=c)
    back,d=case()
    d.line((3,12,12,12),fill=MID)
    bottom,d=new(color=SHADOW)
    for x in (2,11):
        for y in (2,11): d.rectangle((x,y,x+2,y+2),fill=INK)
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
        ('INK',48,79,INK),('OUT',104,79,INK),('12 paper',12,118,INK),('Ready',12,133,'#bde0cd'),('Inventory',48,145,INK)]:
        text(im,s,x,y,c)
    text(im,'PREVIEW',216,18,INK,True);text(im,'SIZE',200,79,INK,True);text(im,'4 x 3',200,96,LIGHT,True)
    # Read the runtime pixel masks so the preview cannot drift from the icons.
    import re
    java=(ROOT/'src/main/java/me/jamino/printer/client/PrinterButton.java').read_text()
    masks={name:re.findall(r'"([01]{8})"',rows) for name,rows in
           re.findall(r'(LOAD|BROWSE|PRINT|MINUS|PLUS|CANCEL)\(([^;]*?)\)',java)}
    for x,y,w,h,s,c in [(136,51,18,18,'LOAD',TEAL),(156,51,18,18,'BROWSE',TEAL),
        (154,91,18,18,'MINUS','#736c59'),(228,91,18,18,'PLUS','#736c59'),
        (112,113,18,18,'PRINT','#af653f')]:
        d.rectangle((x,y,x+w-1,y+h-1),fill=INK)
        d.rectangle((x+1,y+1,x+w-2,y+h-4),fill=c)
        d.line((x+1,y+1,x+w-2,y+1),fill=LIGHT)
        for row,line in enumerate(masks[s]):
            for col,pixel in enumerate(line):
                if pixel=='1': d.point((x+(w-8)//2+col,y+(h-10)//2+row),fill='#f8f1d7')
    text(im,'BG',146,119,INK)
    palette=['#ffffff','#ababab','#404040','#000000','#51301a','#b02e26','#f9801d','#fed83d',
             '#80c71f','#3c441f','#169c9c','#3ab3da','#3c44aa','#8932b8','#c74ebd','#f38baa']
    for i,c in enumerate(palette):
        x,y=159+i%8*11,114+i//8*9
        d.rectangle((x,y,x+9,y+7),fill=GOLD if i==0 else INK)
        d.rectangle((x+1,y+1,x+8,y+6),fill=c)
    for x,k in [(18,'paper'),(48,'ink'),(112,'output')]: im.alpha_composite(ghost(k),(x,91))
    im.alpha_composite(image_item().resize((44,44),Image.Resampling.NEAREST),(194,30))
    board=Image.new('RGBA',(1130,850),'#202927')
    board.alpha_composite(im.resize((768,714),Image.Resampling.NEAREST),(12,48))
    text(board,'PRINTER / P-01 - PROGRAMMATIC ART PREVIEW',12,18,LIGHT)
    for i,(name,asset) in enumerate((n,a) for n,a in outputs.items() if n.startswith(('item/','block/'))):
        x,y=810+i%2*155,48+i//2*145
        board.alpha_composite(asset.resize((96,96),Image.Resampling.NEAREST),(x,y))
        text(board,name.split('/')[-1].replace('printer_','').replace('.png',''),x,y+102,LIGHT)
    text(board,'16 x 16 BLOCKS + ITEMS / PIXEL GUI / ORIGINAL ASSETS',12,784,LIGHT)
    p=ROOT/'build/art-preview.png';p.parent.mkdir(exist_ok=True);board.save(p)

if __name__=='__main__': main()
