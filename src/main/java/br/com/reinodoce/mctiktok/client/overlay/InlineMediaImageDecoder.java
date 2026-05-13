package br.com.reinodoce.mctiktok.client.overlay;

import com.mojang.blaze3d.platform.NativeImage;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

final class InlineMediaImageDecoder {
    static final int MAX_IMAGE_PIXELS = 1_048_576;

    private static final int ARGB_ALPHA_SHIFT = 24;
    private static final int ARGB_RED_SHIFT = 16;
    private static final int BYTE_MASK = 0xFF;
    private static final int GREEN_MASK = 0xFF00;
    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            "image/gif",
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp");

    private InlineMediaImageDecoder() {
    }

    static InlineMediaLoadedMedia decode(byte[] bytes, String contentType) throws IOException {
        try (ImageInputStream inputStream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (inputStream == null) {
                throw new IOException("ImageIO could not inspect contentType=" + contentType);
            }
            return decode(inputStream, contentType);
        }
    }

    static boolean isSupportedContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return false;
        }
        String normalized = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return SUPPORTED_CONTENT_TYPES.contains(normalized);
    }

    static void validateContentType(String contentType) throws IOException {
        if (!isSupportedContentType(contentType)) {
            throw new IOException("Unsupported inline media content type: " + contentType);
        }
    }

    static void validateDimensions(int width, int height) throws IOException {
        if (width <= 0 || height <= 0 || (long) width * height > MAX_IMAGE_PIXELS) {
            throw new IOException("Inline media dimensions exceed supported bounds: " + width + "x" + height);
        }
    }

    private static InlineMediaLoadedMedia decode(ImageInputStream inputStream, String contentType) throws IOException {
        Iterator<ImageReader> readers = ImageIO.getImageReaders(inputStream);
        if (!readers.hasNext()) {
            throw new IOException("ImageIO found no reader for contentType=" + contentType);
        }
        ImageReader reader = readers.next();
        try {
            reader.setInput(inputStream, true, true);
            int width = reader.getWidth(0);
            int height = reader.getHeight(0);
            validateDimensions(width, height);
            BufferedImage image = reader.read(0);
            NativeImage nativeImage = toNativeImage(image);
            return new InlineMediaLoadedMedia(nativeImage, nativeImage.getWidth(), nativeImage.getHeight());
        } finally {
            reader.dispose();
        }
    }

    private static NativeImage toNativeImage(BufferedImage image) {
        NativeImage nativeImage = new NativeImage(image.getWidth(), image.getHeight(), true);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                nativeImage.setPixelRGBA(x, y, swapArgbToRgba(image.getRGB(x, y)));
            }
        }
        return nativeImage;
    }

    private static int swapArgbToRgba(int argb) {
        int alpha = (argb >> ARGB_ALPHA_SHIFT) & BYTE_MASK;
        int red = (argb >> ARGB_RED_SHIFT) & BYTE_MASK;
        int green = argb & GREEN_MASK;
        int blue = argb & BYTE_MASK;
        return (alpha << ARGB_ALPHA_SHIFT) | (blue << ARGB_RED_SHIFT) | green | red;
    }
}
