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

    private static final float STRIKETHROUGH_Y = 4.5F;
    private static final float UNDERLINE_Y = 9.0F;
    private static final float EFFECT_DEPTH = 0.01F;
    private static final float INLINE_HEIGHT = 9.0F;
    private static final float SHADOW_OFFSET = 1.0F;
    private static final float EFFECT_LEAD_INSET = 1.0F;
    private static final float EFFECT_THICKNESS = 1.0F;
    private static final float SENTINEL_ADVANCE = -1.0F;
    private static final int RED_SHIFT = 16;
    private static final int GREEN_SHIFT = 8;
    private static final int ALPHA_SHIFT = 24;
    private static final int COLOR_BYTE_MASK = 0xFF;
    private static final int MAX_COLOR_BYTE = 255;
    private static final float MAX_COLOR_F = 255.0F;
    private static final int SHADOW_PACKED_RGB = 0x3F000000;
    private static final int OPAQUE_WHITE_ARGB = 0xFFFFFFFF;

    private static final Class<?> STRING_RENDER_OUTPUT_CLASS = loadStringRenderOutputClass();
    private static final Field OUTPUT_BUFFER_SOURCE =
            ObfuscationReflectionHelper.findField(STRING_RENDER_OUTPUT_CLASS, OUTPUT_BUFFER_SOURCE_FIELD);
    private static final Field OUTPUT_DROP_SHADOW =
            ObfuscationReflectionHelper.findField(STRING_RENDER_OUTPUT_CLASS, OUTPUT_DROP_SHADOW_FIELD);
    private static final Field OUTPUT_DIM_FACTOR =
            ObfuscationReflectionHelper.findField(STRING_RENDER_OUTPUT_CLASS, OUTPUT_DIM_FACTOR_FIELD);
    private static final Field OUTPUT_R =
            ObfuscationReflectionHelper.findField(STRING_RENDER_OUTPUT_CLASS, OUTPUT_R_FIELD);
    private static final Field OUTPUT_G =
            ObfuscationReflectionHelper.findField(STRING_RENDER_OUTPUT_CLASS, OUTPUT_G_FIELD);
    private static final Field OUTPUT_B =
            ObfuscationReflectionHelper.findField(STRING_RENDER_OUTPUT_CLASS, OUTPUT_B_FIELD);
    private static final Field OUTPUT_A =
            ObfuscationReflectionHelper.findField(STRING_RENDER_OUTPUT_CLASS, OUTPUT_A_FIELD);
    private static final Field OUTPUT_POSE =
            ObfuscationReflectionHelper.findField(STRING_RENDER_OUTPUT_CLASS, OUTPUT_POSE_FIELD);
    private static final Field OUTPUT_DISPLAY_MODE =
            ObfuscationReflectionHelper.findField(STRING_RENDER_OUTPUT_CLASS, OUTPUT_DISPLAY_MODE_FIELD);
    private static final Field OUTPUT_PACKED_LIGHT =
            ObfuscationReflectionHelper.findField(STRING_RENDER_OUTPUT_CLASS, OUTPUT_PACKED_LIGHT_FIELD);
    private static final Field OUTPUT_X =
            ObfuscationReflectionHelper.findField(STRING_RENDER_OUTPUT_CLASS, OUTPUT_X_FIELD);
    private static final Field OUTPUT_Y =
            ObfuscationReflectionHelper.findField(STRING_RENDER_OUTPUT_CLASS, OUTPUT_Y_FIELD);
    private static final Method OUTPUT_ADD_EFFECT = ObfuscationReflectionHelper.findMethod(
            STRING_RENDER_OUTPUT_CLASS, OUTPUT_ADD_EFFECT_METHOD, BakedGlyph.Effect.class);

    private static volatile InlineMediaTokenRegistry tokenRegistry;
    private static volatile boolean coremodPatched;

    private InlineMediaFontHooks() {
    }

    public static void installRegistry(InlineMediaTokenRegistry registry) {
        tokenRegistry = registry;
    }

    public static void markCoremodPatched() {
        coremodPatched = true;
    }

    public static boolean coremodLoaded() {
        return coremodPatched;
    }

    public static float inlineAdvanceOrSentinel(int codePoint) {
        coremodPatched = true;
        InlineMediaTokenRegistry registry = tokenRegistry;
        if (registry == null) {
            return SENTINEL_ADVANCE;
        }
        return registry.inlineAdvanceOrSentinel(codePoint);
    }

    public static boolean tryRenderInline(Object output, int charIndex, Style style, int codePoint) {
        coremodPatched = true;
        InlineMediaTokenRegistry registry = tokenRegistry;
        if (registry == null) {
            return false;
        }
        InlineMediaTokenRegistry.TokenEntry entry = registry.lookup(codePoint);
        if (entry == null) {
            return false;
        }
        try {
            renderEntry(output, style, registry, entry);
            return true;
        } catch (ReflectiveOperationException exception) {
            ReinodoceLogger.LOGGER.warn(
                    "Failed to render inline media token codePoint={}", codePoint, exception);
            return false;
        }
    }

    private static void renderEntry(
            Object output,
            Style style,
            InlineMediaTokenRegistry registry,
            InlineMediaTokenRegistry.TokenEntry entry
    ) throws ReflectiveOperationException {
        OutputState state = readOutputState(output);
        RichLiveMessage.InlineMediaSegment segment = entry.segment();
        InlineMediaCache.TextureHandle handle = registry.resolveHandle(entry.codePoint());
        float advance = registry.advanceFor(segment);
        renderQuad(state, segment, handle, advance);
        renderTextEffects(output, style, state, advance);
        OUTPUT_X.setFloat(output, state.x() + advance);
    }

    private static OutputState readOutputState(Object output) throws IllegalAccessException {
        float x = OUTPUT_X.getFloat(output);
        float y = OUTPUT_Y.getFloat(output);
        boolean dropShadow = OUTPUT_DROP_SHADOW.getBoolean(output);
        float alpha = OUTPUT_A.getFloat(output);
        Matrix4f pose = (Matrix4f) OUTPUT_POSE.get(output);
        MultiBufferSource bufferSource = (MultiBufferSource) OUTPUT_BUFFER_SOURCE.get(output);
        Font.DisplayMode displayMode = (Font.DisplayMode) OUTPUT_DISPLAY_MODE.get(output);
        int packedLight = OUTPUT_PACKED_LIGHT.getInt(output);
        return new OutputState(x, y, dropShadow, alpha, pose, bufferSource, displayMode, packedLight);
    }

    private static void renderQuad(
            OutputState state,
            RichLiveMessage.InlineMediaSegment segment,
            InlineMediaCache.TextureHandle handle,
            float advance
    ) {
        float renderX = state.x() + (state.dropShadow() ? SHADOW_OFFSET : 0.0F);
        float renderY = state.y() + (state.dropShadow() ? SHADOW_OFFSET : 0.0F);
        int packedColor = state.dropShadow()
                ? SHADOW_PACKED_RGB | (((int) (state.alpha() * MAX_COLOR_F)) << ALPHA_SHIFT)
                : OPAQUE_WHITE_ARGB;
        QuadGeometry geometry = new QuadGeometry(renderX, renderY, advance, packedColor);
        emitQuadVertices(state, segment, handle, geometry);
    }

    private static void emitQuadVertices(
            OutputState state,
            RichLiveMessage.InlineMediaSegment segment,
            InlineMediaCache.TextureHandle handle,
            QuadGeometry geometry
    ) {
        float width = Math.max(1.0F, geometry.advance());
        ResourceLocation texture = handle.texture();
        VertexConsumer consumer = state.bufferSource().getBuffer(renderTypeFor(texture, state.displayMode()));
        int a = Math.max(0, Math.min(MAX_COLOR_BYTE, (int) (state.alpha() * MAX_COLOR_F)));
        int packedColor = geometry.packedColor();
        int r = (packedColor >> RED_SHIFT) & COLOR_BYTE_MASK;
        int g = (packedColor >> GREEN_SHIFT) & COLOR_BYTE_MASK;
        int b = packedColor & COLOR_BYTE_MASK;
        TextureUv uv = textureUv(segment, handle);
        Matrix4f pose = state.pose();
        int packedLight = state.packedLight();
        float x = geometry.x();
        float y = geometry.y();
        consumer.vertex(pose, x, y + INLINE_HEIGHT, 0.0F).color(r, g, b, a)
                .uv(uv.u0(), uv.v1()).uv2(packedLight).endVertex();
        consumer.vertex(pose, x + width, y + INLINE_HEIGHT, 0.0F).color(r, g, b, a)
                .uv(uv.u1(), uv.v1()).uv2(packedLight).endVertex();
        consumer.vertex(pose, x + width, y, 0.0F).color(r, g, b, a)
                .uv(uv.u1(), uv.v0()).uv2(packedLight).endVertex();
        consumer.vertex(pose, x, y, 0.0F).color(r, g, b, a)
                .uv(uv.u0(), uv.v0()).uv2(packedLight).endVertex();
    }

    private static TextureUv textureUv(
            RichLiveMessage.InlineMediaSegment segment, InlineMediaCache.TextureHandle handle
    ) {
        TextureUv full = new TextureUv(0.0F, 0.0F, 1.0F, 1.0F);
        if (segment.renderStyle() != RichLiveMessage.InlineMediaRenderStyle.SQUARE_CROP) {
            return full;
        }
        int width = Math.max(1, handle.sourceWidth());
        int height = Math.max(1, handle.sourceHeight());
        if (width == height) {
            return full;
        }
        if (width > height) {
            float inset = (width - height) / (2.0F * width);
            return new TextureUv(inset, 0.0F, 1.0F - inset, 1.0F);
        }
        float inset = (height - width) / (2.0F * height);
        return new TextureUv(0.0F, inset, 1.0F, 1.0F - inset);
    }

    private static RenderType renderTypeFor(ResourceLocation texture, Font.DisplayMode displayMode) {
        return switch (displayMode) {
            case SEE_THROUGH -> RenderType.textSeeThrough(texture);
            case POLYGON_OFFSET -> RenderType.textPolygonOffset(texture);
            case NORMAL -> RenderType.text(texture);
        };
    }

    private static void renderTextEffects(
            Object output, Style style, OutputState state, float advance
    ) throws ReflectiveOperationException {
        EffectColor color = resolveEffectColor(output, state.alpha(), style);
        float effectOffset = state.dropShadow() ? SHADOW_OFFSET : 0.0F;
        if (style.isStrikethrough()) {
            addEffectLine(output, state, advance, effectOffset, STRIKETHROUGH_Y, color);
        }
        if (style.isUnderlined()) {
            addEffectLine(output, state, advance, effectOffset, UNDERLINE_Y, color);
        }
    }

    private static void addEffectLine(
            Object output, OutputState state, float advance,
            float effectOffset, float yOffset, EffectColor color
    ) throws ReflectiveOperationException {
        float x0 = state.x() + effectOffset - EFFECT_LEAD_INSET;
        float y0 = state.y() + effectOffset + yOffset;
        float x1 = state.x() + effectOffset + advance;
        float y1 = state.y() + effectOffset + yOffset - EFFECT_THICKNESS;
        OUTPUT_ADD_EFFECT.invoke(output, new BakedGlyph.Effect(
                x0, y0, x1, y1, EFFECT_DEPTH, color.r(), color.g(), color.b(), color.a()));
    }

    private static EffectColor resolveEffectColor(Object output, float alpha, Style style)
            throws IllegalAccessException {
        TextColor color = style.getColor();
        if (color != null) {
            float dimFactor = OUTPUT_DIM_FACTOR.getFloat(output);
            int value = color.getValue();
            return new EffectColor(
                    ((value >> RED_SHIFT) & COLOR_BYTE_MASK) / MAX_COLOR_F * dimFactor,
                    ((value >> GREEN_SHIFT) & COLOR_BYTE_MASK) / MAX_COLOR_F * dimFactor,
                    (value & COLOR_BYTE_MASK) / MAX_COLOR_F * dimFactor,
                    alpha);
        }
        return new EffectColor(
                OUTPUT_R.getFloat(output),
                OUTPUT_G.getFloat(output),
                OUTPUT_B.getFloat(output),
                alpha);
    }

    private static Class<?> loadStringRenderOutputClass() {
        try {
            return Class.forName("net.minecraft.client.gui.Font$StringRenderOutput");
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Unable to resolve Font$StringRenderOutput", exception);
        }
    }

    private record OutputState(
            float x, float y, boolean dropShadow, float alpha,
            Matrix4f pose, MultiBufferSource bufferSource,
            Font.DisplayMode displayMode, int packedLight) {
    }

    private record QuadGeometry(float x, float y, float advance, int packedColor) {
    }

    private record TextureUv(float u0, float v0, float u1, float v1) {
    }

    private record EffectColor(float r, float g, float b, float a) {
    }
}
