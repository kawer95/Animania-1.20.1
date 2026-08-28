package com.animania.client.config;

import com.animania.common.config.AnimaniaConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Dependency-free editor shared by all four Animania Forge config specs. */
@OnlyIn(Dist.CLIENT)
public final class AnimaniaConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 26;

    private final Screen parent;
    private final String translationNamespace;
    private final ForgeConfigSpec spec;
    private final List<Entry> entries;
    private final Map<ForgeConfigSpec.ConfigValue<?>, Object> staged = new IdentityHashMap<>();
    private int page;
    private int pageSize = 1;
    private Button saveButton;
    private Component validationMessage = Component.empty();

    public AnimaniaConfigScreen(Screen parent) {
        this(parent, Component.translatable("screen.animania.config.title"), "animania", AnimaniaConfig.COMMON_SPEC);
    }

    public AnimaniaConfigScreen(Screen parent, Component title, String translationNamespace, ForgeConfigSpec spec) {
        super(title);
        this.parent = parent;
        this.translationNamespace = translationNamespace;
        this.spec = spec;
        this.entries = collectEntries(spec);
        for (Entry entry : entries) {
            Object value = entry.value().get();
            staged.put(entry.value(), value instanceof List<?> list ? joinList(list) : value);
        }
    }

    @Override
    protected void init() {
        pageSize = Math.max(3, (height - 92) / ROW_HEIGHT);
        int pages = pageCount();
        page = Math.max(0, Math.min(page, pages - 1));

        int editorWidth = Math.min(300, Math.max(150, width / 2 - 40));
        int editorX = width / 2 + 8;
        int first = page * pageSize;
        int last = Math.min(entries.size(), first + pageSize);
        for (int index = first; index < last; index++) {
            Entry entry = entries.get(index);
            int y = 38 + (index - first) * ROW_HEIGHT;
            addEditor(entry, editorX, y, editorWidth);
        }

        int buttonY = height - 28;
        Button previous = addRenderableWidget(Button.builder(Component.translatable("screen.animania.config.previous"), button -> {
            page--;
            rebuildPage();
        }).bounds(width / 2 - 210, buttonY, 80, 20).build());
        previous.active = page > 0;
        Button next = addRenderableWidget(Button.builder(Component.translatable("screen.animania.config.next"), button -> {
            page++;
            rebuildPage();
        }).bounds(width / 2 - 124, buttonY, 80, 20).build());
        next.active = page + 1 < pages;
        addRenderableWidget(Button.builder(Component.translatable("screen.animania.config.defaults"), button -> {
            resetCurrentPage();
            rebuildPage();
        }).bounds(width / 2 - 38, buttonY, 80, 20).build());
        saveButton = addRenderableWidget(Button.builder(Component.translatable("screen.animania.config.save"), button -> saveAndClose())
                .bounds(width / 2 + 48, buttonY, 80, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(width / 2 + 134, buttonY, 80, 20).build());
        updateValidation();
    }

    private void addEditor(Entry entry, int x, int y, int width) {
        Object current = staged.get(entry.value());
        Component tooltip = tooltip(entry);
        if (entry.defaultValue() instanceof Boolean) {
            boolean enabled = Boolean.TRUE.equals(current);
            Button button = Button.builder(toggleText(enabled), pressed -> {
                boolean next = !Boolean.TRUE.equals(staged.get(entry.value()));
                staged.put(entry.value(), next);
                pressed.setMessage(toggleText(next));
                updateValidation();
            }).bounds(x, y, width, 20).build();
            button.setTooltip(Tooltip.create(tooltip));
            addRenderableWidget(button);
            return;
        }

        EditBox box = new EditBox(font, x, y, width, 20, label(entry));
        box.setMaxLength(8192);
        box.setValue(String.valueOf(current));
        box.setTooltip(Tooltip.create(tooltip));
        box.setResponder(value -> {
            staged.put(entry.value(), value);
            boolean valid = parse(entry, value).valid();
            box.setTextColor(valid ? 0xFFE0E0E0 : 0xFFFF5555);
            updateValidation();
        });
        box.setTextColor(parse(entry, box.getValue()).valid() ? 0xFFE0E0E0 : 0xFFFF5555);
        addRenderableWidget(box);
    }

    private void rebuildPage() {
        clearWidgets();
        init();
    }

    private void resetCurrentPage() {
        int first = page * pageSize;
        int last = Math.min(entries.size(), first + pageSize);
        for (int index = first; index < last; index++) {
            Entry entry = entries.get(index);
            Object value = entry.defaultValue();
            staged.put(entry.value(), value instanceof List<?> list ? joinList(list) : value);
        }
    }

    private void updateValidation() {
        Entry invalid = firstInvalidEntry();
        if (saveButton != null) saveButton.active = invalid == null;
        validationMessage = invalid == null
                ? Component.translatable("screen.animania.config.restart_hint")
                : Component.translatable("screen.animania.config.invalid", label(invalid));
    }

    private Entry firstInvalidEntry() {
        for (Entry entry : entries) {
            if (!parse(entry, staged.get(entry.value())).valid()) return entry;
        }
        return null;
    }

    private void saveAndClose() {
        if (firstInvalidEntry() != null) return;
        for (Entry entry : entries) {
            Parsed parsed = parse(entry, staged.get(entry.value()));
            setValue(entry.value(), parsed.value());
        }
        spec.save();
        minecraft.setScreen(parent);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setValue(ForgeConfigSpec.ConfigValue value, Object valueToSet) {
        value.set(valueToSet);
    }

    private static Parsed parse(Entry entry, Object stagedValue) {
        try {
            Object value;
            if (entry.defaultValue() instanceof Boolean) {
                value = stagedValue;
            } else {
                String text = String.valueOf(stagedValue).trim();
                if (entry.defaultValue() instanceof Integer) value = Integer.parseInt(text);
                else if (entry.defaultValue() instanceof Long) value = Long.parseLong(text);
                else if (entry.defaultValue() instanceof Double) value = Double.parseDouble(text);
                else if (entry.defaultValue() instanceof List<?>) {
                    value = text.isEmpty() ? List.of() : java.util.Arrays.stream(text.split(","))
                            .map(String::trim).filter(part -> !part.isEmpty()).toList();
                } else value = text;
            }
            return new Parsed(entry.spec().test(value), value);
        } catch (RuntimeException ignored) {
            return new Parsed(false, null);
        }
    }

    private Component label(Entry entry) {
        String key = "config." + translationNamespace + "." + entry.path();
        return I18n.exists(key) ? Component.translatable(key) : Component.literal(humanize(entry.leafName()));
    }

    private Component tooltip(Entry entry) {
        MutableComponent result = Component.empty();
        if (entry.spec().getComment() != null && !entry.spec().getComment().isBlank()) {
            result.append(Component.literal(entry.spec().getComment())).append("\n");
        }
        if (entry.spec().getRange() != null) {
            result.append(Component.translatable("screen.animania.config.range", entry.spec().getRange().toString())).append("\n");
        }
        if (entry.defaultValue() instanceof List<?>) {
            result.append(Component.translatable("screen.animania.config.list_hint")).append("\n");
        }
        return result.append(Component.translatable("screen.animania.config.default", displayValue(entry.defaultValue())));
    }

    private static String displayValue(Object value) {
        return value instanceof List<?> list ? joinList(list) : String.valueOf(value);
    }

    private static String joinList(List<?> list) {
        return String.join(", ", list.stream().map(String::valueOf).toList());
    }

    private static Component toggleText(boolean value) {
        return Component.translatable(value ? "options.on" : "options.off");
    }

    private static String humanize(String key) {
        String spaced = key.replace('_', ' ').replaceAll("([a-z0-9])([A-Z])", "$1 $2");
        if (spaced.isEmpty()) return key;
        return spaced.substring(0, 1).toUpperCase(Locale.ROOT) + spaced.substring(1);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFFFF);
        graphics.drawCenteredString(font,
                Component.translatable("screen.animania.config.page", page + 1, pageCount()),
                width / 2, 24, 0xFFB0B0B0);

        int first = page * pageSize;
        int last = Math.min(entries.size(), first + pageSize);
        int labelX = Math.max(10, width / 2 - Math.min(300, width / 2 - 20));
        int labelWidth = Math.max(80, width / 2 - labelX - 16);
        for (int index = first; index < last; index++) {
            Entry entry = entries.get(index);
            int y = 44 + (index - first) * ROW_HEIGHT;
            String text = font.plainSubstrByWidth(label(entry).getString(), labelWidth);
            graphics.drawString(font, text, labelX, y, 0xFFE0E0E0, false);
        }
        graphics.drawCenteredString(font, validationMessage, width / 2, height - 40,
                firstInvalidEntry() == null ? 0xFFAAAAAA : 0xFFFF5555);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private int pageCount() {
        return Math.max(1, (entries.size() + pageSize - 1) / pageSize);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static List<Entry> collectEntries(ForgeConfigSpec spec) {
        List<Entry> result = new ArrayList<>();
        collectEntries(spec.getValues(), spec.getSpec(), new ArrayList<>(), result);
        return List.copyOf(result);
    }

    private static void collectEntries(UnmodifiableConfig values, UnmodifiableConfig specs,
                                       List<String> parentPath, List<Entry> destination) {
        for (Map.Entry<String, Object> child : values.valueMap().entrySet()) {
            Object childSpec = specs.valueMap().get(child.getKey());
            List<String> path = new ArrayList<>(parentPath);
            path.add(child.getKey());
            if (child.getValue() instanceof UnmodifiableConfig childValues
                    && childSpec instanceof UnmodifiableConfig childSpecs) {
                collectEntries(childValues, childSpecs, path, destination);
            } else if (child.getValue() instanceof ForgeConfigSpec.ConfigValue<?> value
                    && childSpec instanceof ForgeConfigSpec.ValueSpec valueSpec) {
                destination.add(new Entry(String.join(".", path), child.getKey(), value, valueSpec, value.getDefault()));
            }
        }
    }

    private record Entry(String path, String leafName, ForgeConfigSpec.ConfigValue<?> value,
                         ForgeConfigSpec.ValueSpec spec, Object defaultValue) { }

    private record Parsed(boolean valid, Object value) { }
}
