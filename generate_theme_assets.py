#!/usr/bin/env python3
"""G2O Theme Asset Generator"""
from PIL import Image, ImageDraw, ImageFilter
import os

print("🎨 G2O Theme Asset Generator Starting...")

# Create directories
for dpi in ["mdpi", "hdpi", "xhdpi", "xxhdpi"]:
    os.makedirs(f"app/src/main/res/drawable-{dpi}", exist_ok=True)

def create_aero_button(width, height, color, name, dpi="xhdpi"):
    """Windows 7 Aero glossy button"""
    img = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    r, g, b = int(color[1:3], 16), int(color[3:5], 16), int(color[5:7], 16)
    
    # Gradient background
    for i in range(height):
        factor = i / height
        nr = int(r * (1.2 - factor * 0.4))
        ng = int(g * (1.2 - factor * 0.4))
        nb = int(b * (1.2 - factor * 0.4))
        draw.line([(5, i), (width-5, i)], fill=(min(255,nr), min(255,ng), min(255,nb), 255))
    
    # Glossy highlight
    for i in range(int(height * 0.5)):
        alpha = int(120 * (1 - i / (height * 0.5)))
        draw.line([(5, i), (width-5, i)], fill=(255, 255, 255, alpha))
    
    # Border
    draw.rounded_rectangle([(0, 0), (width-1, height-1)], radius=8, 
                          outline=(max(0,r-40), max(0,g-40), max(0,b-40), 255), width=2)
    
    img.save(f"app/src/main/res/drawable-{dpi}/aero_button_{name}.png")

def create_skeu_button(width, height, color, name, dpi="xhdpi"):
    """iOS 6 skeumorphic button"""
    img = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    r, g, b = int(color[1:3], 16), int(color[3:5], 16), int(color[5:7], 16)
    
    # Base color with texture
    import random
    random.seed(42)
    for y in range(height):
        for x in range(width):
            if x > 6 and x < width-6 and y > 6 and y < height-6:
                noise = random.randint(-15, 15)
                draw.point((x, y), fill=(max(0,min(255,r+noise)), 
                                        max(0,min(255,g+noise)), 
                                        max(0,min(255,b+noise)), 255))
    
    # Top highlight
    for i in range(int(height * 0.15)):
        alpha = int(80 * (1 - i / (height * 0.15)))
        draw.line([(12, i+6), (width-12, i+6)], fill=(255, 255, 255, alpha))
    
    # Border
    draw.rounded_rectangle([(3, 3), (width-4, height-4)], radius=10,
                          outline=(max(0,r-80), max(0,g-80), max(0,b-80), 255), width=3)
    
    img.save(f"app/src/main/res/drawable-{dpi}/skeu_button_{name}.png")

def create_material_button(width, height, color, name, dpi="xhdpi"):
    """Material Design flat button"""
    img = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    r, g, b = int(color[1:3], 16), int(color[3:5], 16), int(color[5:7], 16)
    
    # Shadow
    shadow = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    sdraw = ImageDraw.Draw(shadow)
    sdraw.rounded_rectangle([(4, 4), (width-4, height-2)], radius=4, fill=(0, 0, 0, 60))
    shadow = shadow.filter(ImageFilter.GaussianBlur(2))
    img.paste(shadow, (0, 0), shadow)
    
    # Flat color
    draw.rounded_rectangle([(0, 0), (width-1, height-3)], radius=4, fill=(r, g, b, 255))
    
    img.save(f"app/src/main/res/drawable-{dpi}/material_button_{name}.png")

def create_headers(width, height, dpi):
    """Create header backgrounds"""
    # Material
    img = Image.new('RGBA', (width, height), (76, 175, 80, 255))
    img.save(f"app/src/main/res/drawable-{dpi}/material_header.png")
    
    # Aero
    img = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    for i in range(height):
        factor = i / height
        r = int(180 + (210 - 180) * factor)
        g = int(220 + (240 - 220) * factor)
        draw.line([(0, i), (width, i)], fill=(r, g, 255, 240))
    img.save(f"app/src/main/res/drawable-{dpi}/aero_header.png")
    
    # Skeu
    img = Image.new('RGBA', (width, height), (101, 67, 33, 255))
    img.save(f"app/src/main/res/drawable-{dpi}/skeu_header.png")

def create_refresh_icon(size, theme, dpi):
    """Create refresh icon"""
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    center = size // 2
    radius = size // 3
    
    colors = {"aero": (33, 150, 243), "skeu": (139, 69, 19), "material": (76, 175, 80)}
    color = colors.get(theme, (76, 175, 80))
    
    # Circular arrow
    draw.arc([center-radius, center-radius, center+radius, center+radius],
             start=30, end=300, fill=color, width=4)
    
    # Arrow head
    ax = center + int(radius * 0.7)
    ay = center - int(radius * 0.7)
    draw.polygon([(ax, ay), (ax+8, ay-8), (ax+8, ay+8)], fill=color)
    
    img.save(f"app/src/main/res/drawable-{dpi}/{theme}_refresh_icon.png")

# Generate for all DPIs
dpis = {"mdpi": 0.75, "hdpi": 1.0, "xhdpi": 1.5, "xxhdpi": 2.0}

colors = {"osmand": "#FF9800", "organic": "#9C27B0", "magic": "#3F51B5", "copy": "#2196F3"}

for dpi_name, scale in dpis.items():
    print(f"📱 Generating {dpi_name} assets...")
    
    bw, bh = int(200 * scale), int(80 * scale)
    hw, hh = int(800 * scale), int(200 * scale)
    icon = int(64 * scale)
    
    for name, color in colors.items():
        create_aero_button(bw, bh, color, name, dpi_name)
        create_skeu_button(bw, bh, color, name, dpi_name)
        create_material_button(bw, bh, color, name, dpi_name)
    
    create_headers(hw, hh, dpi_name)
    
    for theme in ["aero", "skeu", "material"]:
        create_refresh_icon(icon, theme, dpi_name)

print("✅ All assets generated successfully!")
