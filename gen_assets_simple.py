#!/usr/bin/env python3
from PIL import Image, ImageDraw
import os

print("Starting asset generation...")

# Create directories
for dpi in ["mdpi", "hdpi", "xhdpi", "xxhdpi"]:
    os.makedirs(f"app/src/main/res/drawable-{dpi}", exist_ok=True)
    print(f"Created drawable-{dpi}")

# Simple test image
img = Image.new('RGBA', (200, 80), (76, 175, 80, 255))
img.save("app/src/main/res/drawable-xhdpi/material_button_copy.png")
print("Created test button")

print("Done!")
