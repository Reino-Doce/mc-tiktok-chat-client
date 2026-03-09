package br.com.reinodoce.mctiktok.client.font;

import br.com.reinodoce.mctiktok.chat.RichLiveMessage;
import br.com.reinodoce.mctiktok.client.overlay.InlineMediaCache;
import br.com.reinodoce.mctiktok.util.ReinodoceLogger;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.joml.Matrix4f;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class InlineMediaFontHooks {
    private static final String OUTPUT_BUFFER_SOURCE_FIELD = "f_92937_";
    private static final String OUTPUT_DROP_SHADOW_FIELD = "f_92939_";
    private static final String OUTPUT_DIM_FACTOR_FIELD = "f_92940_";
    private static final String OUTPUT_R_FIELD = "f_92941_";
    private static final String OUTPUT_G_FIELD = "f_92942_";
    private static final String OUTPUT_B_FIELD = "f_92943_";
    private static final String OUTPUT_A_FIELD = "f_92944_";
    private static final String OUTPUT_POSE_FIELD = "f_92945_";
    private static final String OUTPUT_DISPLAY_MODE_FIELD = "f_181362_";
    private static final String OUTPUT_PACKED_LIGHT_FIELD = "f_92947_";
    private static final String OUTPUT_X_FIELD = "f_92948_";
    private static final String OUTPUT_Y_FIELD = "f_92949_";
    private static final String OUTPUT_ADD_EFFECT_METHOD = "m_92964_";
    private static final String OUTPUT_FONT_FIELD = "f_92938_";

    private static final float STRIKETHROUGH_Y = 4.5F;
    private static final float UNDERLINE_Y = 9.0F;
    private static final float EFFECT_DEPTH = 0.01F;
    private static final float INLINE_HEIGHT = 9.0F;
    private static final float SHADOW_OFFSET = 1.0F;

    private static final Class<?> STRING_RENDER_OUTPUT_CLASS = loadStringRenderOutputClass();
    private static final Field OUTPUT_BUFFER_SOURCE = ObfuscationReflectionHelper.findField(STRING_RENDER_OUTPUT_CLASS, OUTPUT_BUFFER_SOURCE_FIELD);
    private static final Field OUTPUT_DROP_SHADOW = ObfuscationReflectionHelper.findField(STRING_RENDER_OUTPUT_CLASS, OUTPUT_DROP_SHADOW_FIELD);
    private static final Field OUTPUT_DIM_FACTOR = ObfuscationReflectionHelper.findField(STRING_RENDER_OUTPUT_CLASS, OUTPUT_DIM_FACTOR_FIELD);
    private static final Field OUTPUT_R = ObfuscationReflectionHelper.findField(STRING_RENDER_OUTPUT_CLASS, OUTPUT_R_FIELD);
    private static final Field OUTPUT_G = ObfuscationReflectionHelper.findField(STRING_RENDER_OUTPUT_CLASS, OUTPUT_G_FIELD);
    private static final Field OUTPUT_B = ObfuscationReflectionHelper.findField(STRING_RENDER_OUTPUT_CLASS, OUTPUT_B_FIELD);
    private static final Field OUTPUT_A = ObfuscationReflectionHelper.findField(STRING_RENDER_OUTPUT_CLASS, OUTPUT_A_FIELD);
    private static final Field OUTPUT_POSE = ObfuscationReflectionHelper.findField(STRING_RENDER_OUTPUT_CLASS, OUTPUT_POSE_FIELD);
    private static final Field OUTPUT_DISPLAY_MODE = ObfuscationReflectionHelper.findField(STRING_RENDER_OUTPUT_CLASS, OUTPUT_DISPLAY_MODE_FIELD);
    private static final Field OUTPUT_PACKED_LIGHT = ObfuscationReflectionHelper.findField(STRING_RENDER_OUTPUT_CLASS, OUTPUT_PACKED_LIGHT_FIELD);
    private static final Field OUTPUT_X = ObfuscationReflectionHelper.findField(STRING_RENDER_OUTPUT_CLASS, OUTPUT_X_FIELD);
    private static final Field OUTPUT_Y = ObfuscationReflectionHelper.findField(STRING_RENDER_OUTPUT_CLASS, OUTPUT_Y_FIELD);
    private static final Field OUTPUT_FONT = ObfuscationReflectionHelper.findField(STRING_RENDER_OUTPUT_CLASS, OUTPUT_FONT_FIELD);
    private static final Method OUTPUT_ADD_EFFECT = ObfuscationReflectionHelper.findMethod(STRING_RENDER_OUTPUT_CLASS, OUTPUT_ADD_EFFECT_METHOD, BakedGlyph.Effect.class);

    private static volatile InlineMediaTokenRegistry tokenRegistry;
    private static volatile boolean coremodLoaded;

    private InlineMediaFontHooks() {
    }

    public static void installRegistry(InlineMediaTokenRegistry registry) {
        tokenRegistry = registry;
    }

    public static void markCoremodPatched() {
        coremodLoaded = true;
    }

    public static boolean coremodLoaded() {
        return coremodLoaded;
    }

    public static float inlineAdvanceOrSentinel(int codePoint) {
        coremodLoaded = true;
        InlineMediaTokenRegistry registry = tokenRegistry;
        if (registry == null) {
            return -1.0F;
        }
        return registry.inlineAdvanceOrSentinel(codePoint);
    }

    public static boolean tryRenderInline(Object output, int charIndex, Style style, int codePoint) {
        coremodLoaded = true;
        InlineMediaTokenRegistry registry = tokenRegistry;
        if (registry == null) {
            return false;
        }

        InlineMediaTokenRegistry.TokenEntry entry = registry.lookup(codePoint);
        if (entry == null) {
            return false;
        }

        try {
            float x = OUTPUT_X.getFloat(output);
            float y = OUTPUT_Y.getFloat(output);
            boolean dropShadow = OUTPUT_DROP_SHADOW.getBoolean(output);
            float alpha = OUTPUT_A.getFloat(output);
            Matrix4f pose = (Matrix4f) OUTPUT_POSE.get(output);
            MultiBufferSource bufferSource = (MultiBufferSource) OUTPUT_BUFFER_SOURCE.get(output);
            Font.DisplayMode displayMode = (Font.DisplayMode) OUTPUT_DISPLAY_MODE.get(output);
            int packedLight = OUTPUT_PACKED_LIGHT.getInt(output);

            RichLiveMessage.InlineMediaSegment segment = entry.segment();
            InlineMediaCache.TextureHandle handle = registry.resolveHandle(codePoint);
            float advance = registry.advanceFor(segment);

            float renderX = x + (dropShadow ? SHADOW_OFFSET : 0.0F);
            float renderY = y + (dropShadow ? SHADOW_OFFSET : 0.0F);
            int color = dropShadow ? 0x3F000000 | (((int) (alpha * 255.0F)) << 24) : 0xFFFFFFFF;
            renderInlineQuad(bufferSource, pose, displayMode, packedLight, segment, handle, renderX, renderY, advance, color, alpha);

            float[] colors = resolveEffectColor(output, style);
            float effectOffset = dropShadow ? 1.0F : 0.0F;
            if (style.isStrikethrough()) {
                addEffect(output, x + effectOffset - 1.0F, y + effectOffset + STRIKETHROUGH_Y, x + effectOffset + advance, y + effectOffset + STRIKETHROUGH_Y - 1.0F, colors);
            }
            if (style.isUnderlined()) {
                addEffect(output, x + effectOffset - 1.0F, y + effectOffset + UNDERLINE_Y, x + effectOffset + advance, y + effectOffset + UNDERLINE_Y - 1.0F, colors);
            }

            OUTPUT_X.setFloat(output, x + advance);
            return true;
        } catch (ReflectiveOperationException exception) {
            ReinodoceLogger.LOGGER.warn("Failed to render inline media token codePoint={}", codePoint, exception);
            return false;
        }
    }

    private static void renderInlineQuad(
            MultiBufferSource bufferSource,
            Matrix4f pose,
            Font.DisplayMode displayMode,
            int packedLight,
            RichLiveMessage.InlineMediaSegment segment,
            InlineMediaCache.TextureHandle handle,
            float x,
            float y,
            float advance,
            int packedColor,
            float alpha
    ) {
        float width = Math.max(1.0F, advance);
        float height = INLINE_HEIGHT;
        ResourceLocation texture = handle.texture();
        VertexConsumer consumer = bufferSource.getBuffer(renderTypeFor(texture, displayMode));
        int a = Math.max(0, Math.min(255, (int) (alpha * 255.0F)));
        int r = (packedColor >> 16) & 0xFF;
        int g = (packedColor >> 8) & 0xFF;
        int b = packedColor & 0xFF;
        float[] uv = textureUv(segment, handle);

        consumer.vertex(pose, x, y + height, 0.0F).color(r, g, b, a).uv(uv[0], uv[3]).uv2(packedLight).endVertex();
        consumer.vertex(pose, x + width, y + height, 0.0F).color(r, g, b, a).uv(uv[2], uv[3]).uv2(packedLight).endVertex();
        consumer.vertex(pose, x + width, y, 0.0F).color(r, g, b, a).uv(uv[2], uv[1]).uv2(packedLight).endVertex();
        consumer.vertex(pose, x, y, 0.0F).color(r, g, b, a).uv(uv[0], uv[1]).uv2(packedLight).endVertex();
    }

    private static float[] textureUv(RichLiveMessage.InlineMediaSegment segment, InlineMediaCache.TextureHandle handle) {
        if (segment.renderStyle() != RichLiveMessage.InlineMediaRenderStyle.SQUARE_CROP) {
            return new float[]{0.0F, 0.0F, 1.0F, 1.0F};
        }

        int width = Math.max(1, handle.sourceWidth());
        int height = Math.max(1, handle.sourceHeight());
        if (width == height) {
            return new float[]{0.0F, 0.0F, 1.0F, 1.0F};
        }

        if (width > height) {
            float inset = (width - height) / (2.0F * width);
            return new float[]{inset, 0.0F, 1.0F - inset, 1.0F};
        }

        float inset = (height - width) / (2.0F * height);
        return new float[]{0.0F, inset, 1.0F, 1.0F - inset};
    }

    private static RenderType renderTypeFor(ResourceLocation texture, Font.DisplayMode displayMode) {
        return switch (displayMode) {
            case SEE_THROUGH -> RenderType.textSeeThrough(texture);
            case POLYGON_OFFSET -> RenderType.textPolygonOffset(texture);
            case NORMAL -> RenderType.text(texture);
        };
    }

    private static float[] resolveEffectColor(Object output, Style style) throws IllegalAccessException {
        float alpha = OUTPUT_A.getFloat(output);
        TextColor color = style.getColor();
        if (color != null) {
            float dimFactor = OUTPUT_DIM_FACTOR.getFloat(output);
            int value = color.getValue();
            return new float[]{
                    ((value >> 16) & 0xFF) / 255.0F * dimFactor,
                    ((value >> 8) & 0xFF) / 255.0F * dimFactor,
                    (value & 0xFF) / 255.0F * dimFactor,
                    alpha
            };
        }
        return new float[]{
                OUTPUT_R.getFloat(output),
                OUTPUT_G.getFloat(output),
                OUTPUT_B.getFloat(output),
                alpha
        };
    }

    private static void addEffect(Object output, float x0, float y0, float x1, float y1, float[] colors) throws ReflectiveOperationException {
        OUTPUT_ADD_EFFECT.invoke(output, new BakedGlyph.Effect(x0, y0, x1, y1, EFFECT_DEPTH, colors[0], colors[1], colors[2], colors[3]));
    }

    private static Class<?> loadStringRenderOutputClass() {
        try {
            return Class.forName("net.minecraft.client.gui.Font$StringRenderOutput");
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Unable to resolve Font$StringRenderOutput", exception);
        }
    }
}
