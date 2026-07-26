# PDFBox Android optionally supports JPEG-2000 image decoding through a separate
# Gemalto decoder. Mayra's document pipeline extracts text only and does not render
# embedded JPX images, so the optional decoder is intentionally not bundled.
-dontwarn com.gemalto.jp2.**
