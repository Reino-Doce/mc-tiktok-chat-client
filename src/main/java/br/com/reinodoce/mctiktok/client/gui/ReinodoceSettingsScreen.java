package br.com.reinodoce.mctiktok.client.gui;

import br.com.reinodoce.mctiktok.client.ReinodoceClientService;
import br.com.reinodoce.mctiktok.config.HudPosition;
import br.com.reinodoce.mctiktok.config.OutputMode;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.rules.GiftComboMode;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Client-only settings editor for the persisted Reino Doce TikTok configuration.
 */
// The screen owns fixed widget geometry and draft state for the config editor.
@SuppressWarnings("PMD.TooManyFields")
public class ReinodoceSettingsScreen extends Screen {
    private static final Component TITLE = Component.literal("Reino Doce TikTok Settings");
    private static final Component ON = Component.literal("On");
    private static final Component OFF = Component.literal("Off");
    private static final int TITLE_COLOR = 0xFFFFFF;
    private static final int LABEL_COLOR = 0xA0A0A0;
    private static final int MAX_CONTENT_WIDTH = 520;
    private static final int HORIZONTAL_MARGIN = 10;
    private static final int COLUMN_GAP = 16;
    private static final int LABEL_CONTROL_GAP = 4;
    private static final int LABEL_WIDTH_CAP = 82;
    private static final int LABEL_WIDTH_FLOOR = 52;
    private static final int ROW_START_Y = 30;
    private static final int ROW_HEIGHT = 22;
    private static final int LABEL_Y_OFFSET = 6;
    private static final int CONTROL_HEIGHT = 20;
    private static final int BOTTOM_BUTTON_WIDTH = 98;
    private static final int BOTTOM_BUTTON_GAP = 8;
    private static final int BOTTOM_BUTTON_Y_OFFSET = 26;
    private static final int TEXT_MAX_LENGTH = 80;
    private static final int FORMAT_MAX_LENGTH = 160;
    private static final int SHORT_NUMBER_MAX_LENGTH = 6;
    private static final int LEFT_COLUMN = 0;
    private static final int RIGHT_COLUMN = 1;
    private static final int ROW_RECONNECT_SECONDS = 2;
    private static final int ROW_OUTPUT = 3;
    private static final int ROW_HUD_POSITION = 4;
    private static final int ROW_HUD_LINES = 5;
    private static final int ROW_CHAT_PREFIX = 6;
    private static final int ROW_CHAT_FORMAT = 7;
    private static final int LABEL_WIDTH_DIVISOR = 3;
    private static final Predicate<String> UNSIGNED_INTEGER = ReinodoceSettingsScreen::isUnsignedIntegerInput;
    private static final List<String> OUTPUT_MODES = valuesOf(OutputMode.ids());
    private static final List<String> HUD_POSITIONS = valuesOf(HudPosition.ids());
    private static final List<String> GIFT_COMBO_MODES = List.copyOf(GiftComboMode.ids());

    private final ReinodoceClientService service;
    private final Screen parent;
    private final ReinodoceConfig draft;
    private final Map<TextField, EditBox> fields = new EnumMap<>(TextField.class);
    private final List<RowLabel> labels = new ArrayList<>();

    /**
     * Creates the settings screen from a defensive config snapshot.
     *
     * @param service client service used to persist saved changes
     * @param initialConfig initial configuration snapshot
     * @param parent parent screen to return to when closing
     */
    public ReinodoceSettingsScreen(ReinodoceClientService service, ReinodoceConfig initialConfig, Screen parent) {
        super(TITLE);
        this.service = Objects.requireNonNull(service, "service");
        this.parent = parent;
        this.draft = Objects.requireNonNull(initialConfig, "initialConfig").copy();
    }

    @Override
    protected void init() {
        fields.clear();
        labels.clear();
        Layout layout = Layout.fromWidth(width);
        addTextField(layout, LEFT_COLUMN, 0, TextField.LAST_USERNAME);
        addToggle(layout, position(LEFT_COLUMN, 1, "Auto-connect"), draft::isAutoConnectOnStart,
                draft::setAutoConnectOnStart);
        addTextField(layout, LEFT_COLUMN, ROW_RECONNECT_SECONDS, TextField.RECONNECT_SECONDS);
        addCycle(layout, position(LEFT_COLUMN, ROW_OUTPUT, "Output"), draft::getOutputMode,
                draft::setOutputMode, OUTPUT_MODES);
        addCycle(layout, position(LEFT_COLUMN, ROW_HUD_POSITION, "HUD position"), draft::getHudPosition,
                draft::setHudPosition, HUD_POSITIONS);
        addTextField(layout, LEFT_COLUMN, ROW_HUD_LINES, TextField.HUD_LINES);
        addTextField(layout, LEFT_COLUMN, ROW_CHAT_PREFIX, TextField.CHAT_PREFIX);
        addTextField(layout, LEFT_COLUMN, ROW_CHAT_FORMAT, TextField.CHAT_FORMAT);
        addToggle(layout, position(RIGHT_COLUMN, 0, "Chat emotes"), draft::isChatEmotesEnabled,
                draft::setChatEmotesEnabled);
        addTextField(layout, RIGHT_COLUMN, 1, TextField.SYNTHETIC_GIFT_MIN_VALUE);
        addCycle(layout, position(RIGHT_COLUMN, 2, "Gift combo"), draft::getSyntheticGiftComboMode,
                draft::setSyntheticGiftComboMode, GIFT_COMBO_MODES);
        addToggle(layout, position(RIGHT_COLUMN, ROW_OUTPUT, "Follow event"), draft::isSyntheticFollowEnabled,
                draft::setSyntheticFollowEnabled);
        addToggle(layout, position(RIGHT_COLUMN, ROW_HUD_POSITION, "Join event"), draft::isSyntheticJoinEnabled,
                draft::setSyntheticJoinEnabled);
        addToggle(layout, position(RIGHT_COLUMN, ROW_HUD_LINES, "Member event"), draft::isSyntheticMemberLevelEnabled,
                draft::setSyntheticMemberLevelEnabled);
        addToggle(layout, position(RIGHT_COLUMN, ROW_CHAT_PREFIX, "Follower only"), draft::isRuleFollowerOnly,
                draft::setRuleFollowerOnly);
        addTextField(layout, RIGHT_COLUMN, ROW_CHAT_FORMAT, TextField.RULE_MIN_MEMBER_LEVEL);
        addBottomButtons();
    }

    @Override
    public void tick() {
        for (EditBox field : fields.values()) {
            field.tick();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, HORIZONTAL_MARGIN, TITLE_COLOR);
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

    private void addTextField(Layout layout, int column, int row, TextField fieldId) {
        EditBox field = new EditBox(font, layout.controlX(column), layout.rowY(row), layout.controlWidth(),
                CONTROL_HEIGHT, Component.literal(fieldId.label()));
        field.setMaxLength(fieldId.maxLength());
        if (fieldId.numeric()) {
            field.setFilter(UNSIGNED_INTEGER);
        }
        field.setValue(fieldId.value(draft));
        fields.put(fieldId, field);
        addRowLabel(layout, column, row, fieldId.label());
        addRenderableWidget(field);
    }

    private void addToggle(Layout layout, ControlPosition position, BooleanSupplier getter, Consumer<Boolean> setter) {
        Button button = Button.builder(booleanMessage(getter.getAsBoolean()), pressed -> {
            setter.accept(!getter.getAsBoolean());
            pressed.setMessage(booleanMessage(getter.getAsBoolean()));
        }).bounds(layout.controlX(position.column()), layout.rowY(position.row()), layout.controlWidth(), CONTROL_HEIGHT)
                .build();
        addRowLabel(layout, position.column(), position.row(), position.label());
        addRenderableWidget(button);
    }

    private void addCycle(
            Layout layout,
            ControlPosition position,
            Supplier<String> getter,
            Consumer<String> setter,
            List<String> values
    ) {
        Button button = Button.builder(Component.literal(getter.get()), pressed -> {
            setter.accept(nextValue(values, getter.get()));
            pressed.setMessage(Component.literal(getter.get()));
        }).bounds(layout.controlX(position.column()), layout.rowY(position.row()), layout.controlWidth(), CONTROL_HEIGHT)
                .build();
        addRowLabel(layout, position.column(), position.row(), position.label());
        addRenderableWidget(button);
    }

    private void addBottomButtons() {
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

    private void saveAndClose() {
        ReinodoceConfig updated = draft.copy();
        updated.setLastUsername(text(TextField.LAST_USERNAME));
        updated.setReconnectSeconds(number(TextField.RECONNECT_SECONDS));
        updated.setHudLines(number(TextField.HUD_LINES));
        updated.setChatPrefix(text(TextField.CHAT_PREFIX));
        updated.setChatFormat(text(TextField.CHAT_FORMAT));
        updated.setSyntheticGiftMinValue(number(TextField.SYNTHETIC_GIFT_MIN_VALUE));
        updated.setRuleMinMemberLevel(number(TextField.RULE_MIN_MEMBER_LEVEL));
        service.saveSettingsDraft(updated);
        closeToParent();
    }

    private void closeToParent() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private String text(TextField fieldId) {
        EditBox field = fields.get(fieldId);
        return field == null ? "" : field.getValue();
    }

    private int number(TextField fieldId) {
        String value = text(fieldId);
        if (value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void addRowLabel(Layout layout, int column, int row, String label) {
        labels.add(new RowLabel(label, layout.labelX(column), layout.rowY(row), layout.labelWidth()));
    }

    private void renderLabels(GuiGraphics graphics) {
        for (RowLabel label : labels) {
            graphics.drawString(font, label.trimmed(font), label.x(), label.y() + LABEL_Y_OFFSET, LABEL_COLOR);
        }
    }

    private static ControlPosition position(int column, int row, String label) {
        return new ControlPosition(column, row, label);
    }

    private static Component booleanMessage(boolean enabled) {
        return enabled ? ON : OFF;
    }

    private static String nextValue(List<String> values, String current) {
        int index = values.indexOf(current);
        int nextIndex = index < 0 ? 0 : (index + 1) % values.size();
        return values.get(nextIndex);
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

    private static List<String> valuesOf(Iterable<String> values) {
        List<String> ids = new ArrayList<>();
        values.forEach(ids::add);
        return List.copyOf(ids);
    }

    private enum TextField {
        LAST_USERNAME("Last user", TEXT_MAX_LENGTH, false) {
            @Override
            String value(ReinodoceConfig config) {
                return config.getLastUsername();
            }
        },
        RECONNECT_SECONDS("Reconnect", SHORT_NUMBER_MAX_LENGTH, true) {
            @Override
            String value(ReinodoceConfig config) {
                return String.valueOf(config.getReconnectSeconds());
            }
        },
        HUD_LINES("HUD lines", SHORT_NUMBER_MAX_LENGTH, true) {
            @Override
            String value(ReinodoceConfig config) {
                return String.valueOf(config.getHudLines());
            }
        },
        CHAT_PREFIX("Chat prefix", TEXT_MAX_LENGTH, false) {
            @Override
            String value(ReinodoceConfig config) {
                return config.getChatPrefix();
            }
        },
        CHAT_FORMAT("Chat format", FORMAT_MAX_LENGTH, false) {
            @Override
            String value(ReinodoceConfig config) {
                return config.getChatFormat();
            }
        },
        SYNTHETIC_GIFT_MIN_VALUE("Gift minimum", SHORT_NUMBER_MAX_LENGTH, true) {
            @Override
            String value(ReinodoceConfig config) {
                return String.valueOf(config.getSyntheticGiftMinValue());
            }
        },
        RULE_MIN_MEMBER_LEVEL("Min level", SHORT_NUMBER_MAX_LENGTH, true) {
            @Override
            String value(ReinodoceConfig config) {
                return String.valueOf(config.getRuleMinMemberLevel());
            }
        };

        private final String displayLabel;
        private final int lengthLimit;
        private final boolean numericOnly;

        TextField(String label, int maxLength, boolean numeric) {
            this.displayLabel = label;
            this.lengthLimit = maxLength;
            this.numericOnly = numeric;
        }

        String label() {
            return displayLabel;
        }

        int maxLength() {
            return lengthLimit;
        }

        boolean numeric() {
            return numericOnly;
        }

        abstract String value(ReinodoceConfig config);
    }

    private record Layout(int leftX, int rightX, int labelWidth, int controlWidth) {
        static Layout fromWidth(int screenWidth) {
            int contentWidth = Math.min(MAX_CONTENT_WIDTH, screenWidth - HORIZONTAL_MARGIN * 2);
            int columnWidth = (contentWidth - COLUMN_GAP) / 2;
            int labelWidth = Math.max(
                    LABEL_WIDTH_FLOOR,
                    Math.min(LABEL_WIDTH_CAP, columnWidth / LABEL_WIDTH_DIVISOR));
            int controlWidth = columnWidth - labelWidth - LABEL_CONTROL_GAP;
            int leftX = (screenWidth - contentWidth) / 2;
            return new Layout(leftX, leftX + columnWidth + COLUMN_GAP, labelWidth, controlWidth);
        }

        int labelX(int column) {
            return column == LEFT_COLUMN ? leftX : rightX;
        }

        int controlX(int column) {
            return labelX(column) + labelWidth + LABEL_CONTROL_GAP;
        }

        int rowY(int row) {
            return ROW_START_Y + ROW_HEIGHT * row;
        }
    }

    private record ControlPosition(int column, int row, String label) {
    }

    private record RowLabel(String label, int x, int y, int width) {
        String trimmed(Font font) {
            return font.plainSubstrByWidth(label, width);
        }
    }
}
