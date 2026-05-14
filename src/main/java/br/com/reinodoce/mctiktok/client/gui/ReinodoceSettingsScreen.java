package br.com.reinodoce.mctiktok.client.gui;

import br.com.reinodoce.mctiktok.client.ReinodoceClientService;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Client-only settings editor for the persisted Reino Doce TikTok configuration.
 */
// The screen owns the shared draft plus Minecraft widget wiring for this client-only UI shell.
@SuppressWarnings({"PMD.CognitiveComplexity", "PMD.CouplingBetweenObjects", "PMD.TooManyMethods"})
public class ReinodoceSettingsScreen extends Screen {
    private static final int TITLE_COLOR = 0xFFFFFF;
    private static final int SUBTITLE_COLOR = 0xA0A0A0;
    private static final int LABEL_COLOR = 0xA0A0A0;
    private static final int HORIZONTAL_MARGIN = 10;
    private static final int INDEX_MAX_CONTENT_WIDTH = 440;
    private static final int INDEX_COLUMN_GAP = 12;
    private static final int INDEX_BUTTON_HEIGHT = 20;
    private static final int INDEX_ROW_HEIGHT = 24;
    private static final int INDEX_START_Y = 50;
    private static final int FORM_MAX_CONTENT_WIDTH = 430;
    private static final int FORM_LABEL_WIDTH = 118;
    private static final int LABEL_CONTROL_GAP = 6;
    private static final int ROW_START_Y = 48;
    private static final int ROW_HEIGHT = 22;
    private static final int CONTROL_HEIGHT = 20;
    private static final int LABEL_Y_OFFSET = 6;
    private static final int BOTTOM_BUTTON_WIDTH = 98;
    private static final int BOTTOM_BUTTON_GAP = 8;
    private static final int BOTTOM_BUTTON_Y_OFFSET = 26;
    private static final int SHORT_NUMBER_MAX_LENGTH = 6;
    private static final int SUBTITLE_Y_OFFSET = 12;
    private static final int LAYOUT_LABEL_WIDTH_DIVISOR = 3;
    private static final Predicate<String> UNSIGNED_INTEGER = ReinodoceSettingsScreen::isUnsignedIntegerInput;

    private final ReinodoceClientService service;
    private final Screen parent;
    private final ReinodoceConfig settingsDraft;
    private final ReinodoceSettingsPages.Page page;
    private final SettingsTextDrafts textDrafts;
    private final List<EditBox> fields = new ArrayList<>();
    private final List<RowLabel> labels = new ArrayList<>();

    /**
     * Creates the root settings screen from a defensive config snapshot.
     *
     * @param service client service used to persist saved changes
     * @param initialConfig initial configuration snapshot
     * @param parent parent screen to return to when closing
     */
    public ReinodoceSettingsScreen(ReinodoceClientService service, ReinodoceConfig initialConfig, Screen parent) {
        this(service, parent, Objects.requireNonNull(initialConfig, "initialConfig").copy(),
                ReinodoceSettingsPages.Page.ROOT, new SettingsTextDrafts());
    }

    private ReinodoceSettingsScreen(
            ReinodoceClientService service,
            Screen parent,
            ReinodoceConfig draft,
            ReinodoceSettingsPages.Page page,
            SettingsTextDrafts textDrafts
    ) {
        super(Component.literal(Objects.requireNonNull(page, "page").title()));
        this.service = Objects.requireNonNull(service, "service");
        this.parent = parent;
        this.settingsDraft = Objects.requireNonNull(draft, "draft");
        this.page = page;
        this.textDrafts = Objects.requireNonNull(textDrafts, "textDrafts");
    }

    @Override
    protected void init() {
        fields.clear();
        labels.clear();
        clearWidgets();
        ReinodoceSettingsPages.init(page, this);
    }

    @Override
    public void tick() {
        for (EditBox field : fields) {
            field.tick();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, HORIZONTAL_MARGIN, TITLE_COLOR);
        drawSubtitle(graphics);
        renderLabels(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        closeToParent();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    void addDomainButton(int column, int row, ReinodoceSettingsPages.Page targetPage) {
        IndexLayout layout = IndexLayout.fromWidth(width);
        addRenderableWidget(Button.builder(Component.literal(targetPage.buttonLabel()), button -> openPage(targetPage))
                .bounds(layout.buttonX(column), layout.rowY(row), layout.buttonWidth(), INDEX_BUTTON_HEIGHT)
                .build());
    }

    void addTextField(
            int row,
            String label,
            int maxLength,
            Supplier<String> getter,
            Consumer<String> setter
    ) {
        FormLayout layout = FormLayout.fromWidth(width);
        EditBox field = new EditBox(font, layout.controlX(), layout.rowY(row), layout.controlWidth(),
                CONTROL_HEIGHT, Component.literal(label));
        String draftKey = textKey(page, label);
        field.setMaxLength(maxLength);
        field.setValue(textDrafts.value(draftKey, getter, setter));
        field.setResponder(value -> textDrafts.update(draftKey, value));
        fields.add(field);
        addRowLabel(layout, row, label);
        addRenderableWidget(field);
    }

    void addNumberField(
            int row,
            String label,
            IntSupplier getter,
            IntConsumer setter
    ) {
        FormLayout layout = FormLayout.fromWidth(width);
        EditBox field = new EditBox(font, layout.controlX(), layout.rowY(row), layout.controlWidth(),
                CONTROL_HEIGHT, Component.literal(label));
        field.setMaxLength(SHORT_NUMBER_MAX_LENGTH);
        field.setFilter(UNSIGNED_INTEGER);
        field.setValue(String.valueOf(getter.getAsInt()));
        field.setResponder(value -> setter.accept(parseUnsignedInteger(value)));
        fields.add(field);
        addRowLabel(layout, row, label);
        addRenderableWidget(field);
    }

    void addToggle(int row, String label, BooleanSupplier getter, Consumer<Boolean> setter) {
        FormLayout layout = FormLayout.fromWidth(width);
        Button button = Button.builder(booleanMessage(getter.getAsBoolean()), pressed -> {
            setter.accept(!getter.getAsBoolean());
            pressed.setMessage(booleanMessage(getter.getAsBoolean()));
        }).bounds(layout.controlX(), layout.rowY(row), layout.controlWidth(), CONTROL_HEIGHT).build();
        addRowLabel(layout, row, label);
        addRenderableWidget(button);
    }

    void addCycle(
            int row,
            String label,
            Supplier<String> getter,
            Consumer<String> setter,
            List<String> values
    ) {
        FormLayout layout = FormLayout.fromWidth(width);
        Button button = Button.builder(Component.literal(getter.get()), pressed -> {
            setter.accept(nextValue(values, getter.get()));
            pressed.setMessage(Component.literal(getter.get()));
        }).bounds(layout.controlX(), layout.rowY(row), layout.controlWidth(), CONTROL_HEIGHT).build();
        addRowLabel(layout, row, label);
        addRenderableWidget(button);
    }

    void addRootButtons() {
        int y = height - BOTTOM_BUTTON_Y_OFFSET;
        int doneX = width / 2 - BOTTOM_BUTTON_WIDTH - BOTTOM_BUTTON_GAP / 2;
        int cancelX = width / 2 + BOTTOM_BUTTON_GAP / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> saveAndClose())
                .bounds(doneX, y, BOTTOM_BUTTON_WIDTH, CONTROL_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> closeToParent())
                .bounds(cancelX, y, BOTTOM_BUTTON_WIDTH, CONTROL_HEIGHT)
                .build());
    }

    void addBackButton() {
        int x = width / 2 - BOTTOM_BUTTON_WIDTH / 2;
        int y = height - BOTTOM_BUTTON_Y_OFFSET;
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> closeToParent())
                .bounds(x, y, BOTTOM_BUTTON_WIDTH, CONTROL_HEIGHT)
                .build());
    }

    private void saveAndClose() {
        textDrafts.apply();
        service.saveSettingsDraft(settingsDraft.copy());
        closeToParent();
    }

    private void openPage(ReinodoceSettingsPages.Page targetPage) {
        if (minecraft != null) {
            minecraft.setScreen(new ReinodoceSettingsScreen(service, this, settingsDraft, targetPage, textDrafts));
        }
    }

    private void closeToParent() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private void addRowLabel(FormLayout layout, int row, String label) {
        labels.add(new RowLabel(label, layout.labelX(), layout.rowY(row), layout.labelWidth()));
    }

    private void renderLabels(GuiGraphics graphics) {
        for (RowLabel label : labels) {
            graphics.drawString(font, label.trimmed(font), label.x(), label.y() + LABEL_Y_OFFSET, LABEL_COLOR);
        }
    }

    private void drawSubtitle(GuiGraphics graphics) {
        int maxWidth = width - HORIZONTAL_MARGIN * 2;
        String subtitle = font.plainSubstrByWidth(page.subtitle(), maxWidth);
        graphics.drawCenteredString(font, subtitle, width / 2, HORIZONTAL_MARGIN + SUBTITLE_Y_OFFSET, SUBTITLE_COLOR);
    }

    ReinodoceConfig configDraft() {
        return settingsDraft;
    }

    static String textKey(ReinodoceSettingsPages.Page page, String label) {
        return page.name() + ':' + label;
    }

    static List<String> parseCsvList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        String[] values = value.split(",");
        List<String> parsed = new ArrayList<>(values.length);
        for (String candidate : values) {
            String trimmed = candidate.trim();
            if (!trimmed.isEmpty()) {
                parsed.add(trimmed);
            }
        }
        return List.copyOf(parsed);
    }

    static String joinCsv(List<String> values) {
        return String.join(", ", values);
    }

    private static Component booleanMessage(boolean enabled) {
        return Component.literal(enabled ? "On" : "Off");
    }

    private static String nextValue(List<String> values, String current) {
        int index = values.indexOf(current);
        int nextIndex = index < 0 ? 0 : (index + 1) % values.size();
        return values.get(nextIndex);
    }

    private static int parseUnsignedInteger(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static boolean isUnsignedIntegerInput(String value) {
        if (value.isEmpty()) {
            return true;
        }
        for (int index = 0; index < value.length(); index++) {
            if (!Character.isDigit(value.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private record FormLayout(int leftX, int labelWidth, int controlWidth) {
        static FormLayout fromWidth(int screenWidth) {
            int contentWidth = Math.min(FORM_MAX_CONTENT_WIDTH, screenWidth - HORIZONTAL_MARGIN * 2);
            int labelWidth = Math.min(FORM_LABEL_WIDTH, contentWidth / LAYOUT_LABEL_WIDTH_DIVISOR);
            int controlWidth = contentWidth - labelWidth - LABEL_CONTROL_GAP;
            int leftX = (screenWidth - contentWidth) / 2;
            return new FormLayout(leftX, labelWidth, controlWidth);
        }

        int labelX() {
            return leftX;
        }

        int controlX() {
            return leftX + labelWidth + LABEL_CONTROL_GAP;
        }

        int rowY(int row) {
            return ROW_START_Y + ROW_HEIGHT * row;
        }
    }

    private record IndexLayout(int leftX, int buttonWidth) {
        static IndexLayout fromWidth(int screenWidth) {
            int contentWidth = Math.min(INDEX_MAX_CONTENT_WIDTH, screenWidth - HORIZONTAL_MARGIN * 2);
            int buttonWidth = (contentWidth - INDEX_COLUMN_GAP) / 2;
            int leftX = (screenWidth - contentWidth) / 2;
            return new IndexLayout(leftX, buttonWidth);
        }

        int buttonX(int column) {
            return leftX + column * (buttonWidth + INDEX_COLUMN_GAP);
        }

        int rowY(int row) {
            return INDEX_START_Y + row * INDEX_ROW_HEIGHT;
        }
    }

    private record RowLabel(String label, int x, int y, int width) {
        String trimmed(Font font) {
            return font.plainSubstrByWidth(label, width);
        }
    }
}
