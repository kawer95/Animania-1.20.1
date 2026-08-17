package com.animania.client.manual;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.tags.TagKey;
import net.minecraftforge.registries.ForgeRegistries;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.animania.common.entity.AnimaniaAnimalEntity;

import java.io.InputStreamReader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.Locale;
import java.util.Map;

/**
 * Client handbook which keeps the data format used by the 1.12 manual.
 *
 * <p>The old manual is a collection of small JSON pages.  The first 1.20
 * port displayed the JSON as one unclickable paragraph, which meant that the
 * back links, headings, images and most component markers were effectively
 * lost.  This screen deliberately keeps the old format and restores those
 * behaviours without making the base mod depend on another mod at runtime.</p>
 */
public final class ManualScreen extends Screen {
    private static final int BOOK_WIDTH = 360;
    private static final int BOOK_HEIGHT = 250;
    private static final int BODY_WIDTH = 316;
    private static final int BODY_TOP = 46;
    private static final int BODY_BOTTOM = 218;
    /** InventoryScreen's direct angle component is converted at 20 degrees per unit. */
    private static final float ENTITY_PREVIEW_YAW_COMPONENT = 1.25F;
    private static final Map<ResourceLocation, int[]> IMAGE_SIZES = new HashMap<>();

    private int page;
    private int scroll;
    private final Deque<Integer> history = new ArrayDeque<>();
    private final List<ManualPage> pages;
    private final Map<String, Integer> pageById;
    /**
     * Handbook previews are client-only entities.  Keep one instance per
     * registry ID for the lifetime of the screen: constructing an Animania
     * entity calls its spawn-variant randomizer, so creating one during every
     * render pass makes multi-variant animals flash between coats.
     */
    private final Map<ResourceLocation, LivingEntity> previewEntities = new HashMap<>();
    private List<LinkHit> visibleLinks = List.of();
    private int previousButtonLeft;
    private int backButtonLeft;
    private int nextButtonLeft;
    private int buttonTop;
    private int bookLeft;
    private int bookTop;

    private ManualScreen() {
        super(Component.translatable("item.animania.manual"));
        this.pages = loadPages();
        this.pageById = indexPages(pages);
        this.page = firstPage(pages);
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new ManualScreen());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        bookLeft = width / 2 - BOOK_WIDTH / 2;
        bookTop = height / 2 - BOOK_HEIGHT / 2;
        drawBook(graphics);

        ManualPage current = pages.get(page);
        graphics.drawString(font, current.title(), bookLeft + 22, bookTop + 18, 0xFF3D2413);
        graphics.drawString(font, Component.translatable("manual.page.counter", page + 1, pages.size()),
                bookLeft + BOOK_WIDTH - 104, bookTop + 18, 0xFF6B5136);

        List<LinkHit> links = new ArrayList<>();
        int cursorY = bookTop + BODY_TOP - scroll;
        for (ManualLine line : current.lines()) {
            if (line.visual() != null) {
                int visualHeight = line.visual().height();
                if (cursorY + visualHeight >= bookTop + BODY_TOP && cursorY <= bookTop + BODY_BOTTOM) {
                    renderVisual(graphics, line.visual(), bookLeft + 22, cursorY, partialTick);
                }
                cursorY += visualHeight + 6;
                continue;
            }
            if (line.component().getString().isEmpty()) {
                cursorY += 8;
                continue;
            }
            int lineHeight = wrappedHeight(line.component(), BODY_WIDTH);
            if (cursorY + lineHeight >= bookTop + BODY_TOP && cursorY <= bookTop + BODY_BOTTOM) {
                int textX = bookLeft + 22;
                int textY = Math.max(bookTop + BODY_TOP, cursorY);
                graphics.drawWordWrap(font, line.component(), textX, textY, BODY_WIDTH, line.color());
                if (line.target() != null) {
                    links.add(new LinkHit(textX, textY, BODY_WIDTH, lineHeight, line.target()));
                }
            }
            cursorY += lineHeight + 3;
            if (cursorY > bookTop + BODY_BOTTOM + 80) break;
        }
        visibleLinks = links;

        buttonTop = bookTop + BOOK_HEIGHT - 24;
        previousButtonLeft = bookLeft + 18;
        backButtonLeft = bookLeft + BOOK_WIDTH / 2 - 54;
        nextButtonLeft = bookLeft + BOOK_WIDTH - 98;
        drawButton(graphics, previousButtonLeft, buttonTop, 76,
                Component.translatable("manual.page.previous"), page > 0, mouseX, mouseY);
        drawButton(graphics, backButtonLeft, buttonTop, 108,
                Component.translatable("manual.topic.previous"), canGoBack(), mouseX, mouseY);
        drawButton(graphics, nextButtonLeft, buttonTop, 76,
                Component.translatable("manual.page.next"), page + 1 < pages.size(), mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        if (mouseY >= buttonTop && mouseY < buttonTop + 20) {
            if (mouseX >= previousButtonLeft && mouseX < previousButtonLeft + 76 && page > 0) {
                changePage(page - 1);
                return true;
            }
            if (mouseX >= backButtonLeft && mouseX < backButtonLeft + 108 && canGoBack()) {
                goBack();
                return true;
            }
            if (mouseX >= nextButtonLeft && mouseX < nextButtonLeft + 76 && page + 1 < pages.size()) {
                changePage(page + 1);
                return true;
            }
        }
        for (LinkHit link : visibleLinks) {
            if (mouseX >= link.x() && mouseX <= link.x() + link.width()
                    && mouseY >= link.y() && mouseY <= link.y() + link.height()) {
                Integer targetPage = resolveTarget(link.target());
                if (targetPage != null) {
                    navigateTo(targetPage);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxScroll = maxScroll(pages.get(page));
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (delta * 18)));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 263 || keyCode == 266) {
            changePage(Math.max(0, page - 1));
            return true;
        }
        if (keyCode == 262 || keyCode == 267) {
            changePage(Math.min(pages.size() - 1, page + 1));
            return true;
        }
        if (keyCode == 264) {
            scroll = Math.min(maxScroll(pages.get(page)), scroll + 18);
            return true;
        }
        if (keyCode == 265) {
            scroll = Math.max(0, scroll - 18);
            return true;
        }
        if (keyCode == 256) onClose();
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        previewEntities.values().forEach(LivingEntity::discard);
        previewEntities.clear();
        super.removed();
    }

    private void drawBook(GuiGraphics graphics) {
        graphics.fill(bookLeft - 4, bookTop - 3, bookLeft + BOOK_WIDTH + 4, bookTop + BOOK_HEIGHT + 4, 0x55000000);
        graphics.fill(bookLeft, bookTop, bookLeft + BOOK_WIDTH, bookTop + BOOK_HEIGHT, 0xFFF3E4C2);
        graphics.fill(bookLeft + 10, bookTop + 34, bookLeft + BOOK_WIDTH - 10, bookTop + BOOK_HEIGHT - 34, 0xFFE8D3A8);
        graphics.fill(bookLeft + 12, bookTop + 36, bookLeft + BOOK_WIDTH - 12, bookTop + BOOK_HEIGHT - 36, 0xFFF9EDCF);
    }

    private void drawButton(GuiGraphics graphics, int left, int top, int buttonWidth, Component label,
                            boolean enabled, int mouseX, int mouseY) {
        boolean hovered = enabled && mouseX >= left && mouseX < left + buttonWidth && mouseY >= top && mouseY < top + 20;
        int background = !enabled ? 0x55906F48 : hovered ? 0xCCB27D43 : 0xAA9B6C3D;
        graphics.fill(left, top, left + buttonWidth, top + 20, background);
        int color = enabled ? 0xFFFFF4D7 : 0xFF9A896E;
        graphics.drawCenteredString(font, label, left + buttonWidth / 2, top + 6, color);
    }

    private void changePage(int target) {
        if (target < 0 || target >= pages.size() || target == page) return;
        page = target;
        scroll = 0;
        visibleLinks = List.of();
    }

    private void navigateTo(int target) {
        if (target < 0 || target >= pages.size() || target == page) return;
        history.push(page);
        changePage(target);
    }

    private boolean canGoBack() {
        if (!history.isEmpty()) return true;
        ManualPage current = pages.get(page);
        return current.parentTarget() != null || page != firstPage(pages);
    }

    private void goBack() {
        if (!history.isEmpty()) {
            changePage(history.pop());
            return;
        }
        String parentTarget = pages.get(page).parentTarget();
        Integer parent = parentTarget == null ? null : resolveTarget(parentTarget);
        if (parent != null && parent != page) {
            changePage(parent);
        } else {
            changePage(firstPage(pages));
        }
    }

    private Integer resolveTarget(String target) {
        Integer direct = pageById.get(target);
        if (direct != null) return direct;
        int separator = target.indexOf(':');
        return separator >= 0 ? pageById.get(target.substring(separator + 1)) : null;
    }

    private int maxScroll(ManualPage manualPage) {
        int contentHeight = 0;
        for (ManualLine line : manualPage.lines()) {
            contentHeight += line.visual() != null ? line.visual().height() + 6
                    : wrappedHeight(line.component(), BODY_WIDTH) + 3;
        }
        return Math.max(0, contentHeight - (BODY_BOTTOM - BODY_TOP));
    }

    private int wrappedHeight(Component component, int width) {
        return Math.max(9, font.split(component, width).size() * 9);
    }

    private static List<ManualPage> loadPages() {
        List<ManualPage> result = new ArrayList<>();
        var manager = Minecraft.getInstance().getResourceManager();
        // Base pages live at `<namespace>:manual/...`; the legacy addon packs
        // retain their `animania/manual/<addon>/...` path.  Read both modern
        // layouts so installing an addon actually extends the in-game book.
        Map<ResourceLocation, Resource> resources = new LinkedHashMap<>();
        resources.putAll(manager.listResources("manual", id -> id.getPath().endsWith(".json")));
        resources.putAll(manager.listResources("animania/manual", id -> id.getPath().endsWith(".json")));
        resources.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString))).forEach(entry -> {
            try (var reader = new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                if (!parsed.isJsonObject()) return;
                JsonObject object = parsed.getAsJsonObject();
                String name = object.has("name") ? object.get("name").getAsString() : entry.getKey().getPath();
                List<String> rawLines = new ArrayList<>();
                if (object.has("contents")) collectContents(object.get("contents"), rawLines);
                if (rawLines.isEmpty()) collectContents(parsed, rawLines);
                boolean firstPage = object.has("isFirstPage") && object.get("isFirstPage").getAsBoolean();
                result.add(new ManualPage(entry.getKey(), asComponent(name), parseLines(rawLines),
                        firstPage, firstLinkTarget(rawLines, firstPage)));
            } catch (Exception ignored) {
                // A malformed optional page must not prevent the handbook or
                // the client from opening; the remaining pages still render.
            }
        });
        // ResourceManager ordering is namespace-first. Keep the explicit start
        // page at index zero so the counter and navigation match the book.
        result.sort(Comparator.comparing(ManualPage::firstPage).reversed()
                .thenComparing(page -> page.id().toString()));
        if (result.isEmpty()) {
            result.add(new ManualPage(new ResourceLocation("animania", "manual/startpage.json"),
                    Component.translatable("manual.startpage.title"),
                    List.of(new ManualLine(Component.translatable("manual.animania.page.0"), null, null,
                            0xFF3D2413)), true, null));
        }
        return result;
    }

    private static String firstLinkTarget(List<String> lines, boolean firstPage) {
        if (firstPage) return null;
        for (String line : lines) {
            if (!line.startsWith("@link@")) continue;
            String payload = line.substring("@link@".length());
            int separator = payload.indexOf('#');
            return (separator < 0 ? payload : payload.substring(0, separator)).trim();
        }
        return null;
    }

    private static Map<String, Integer> indexPages(List<ManualPage> pages) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < pages.size(); i++) {
            ResourceLocation id = pages.get(i).id();
            index.put(id.toString(), i);
            index.putIfAbsent(id.getPath(), i);
            index.putIfAbsent(id.getPath().replaceFirst("^animania/", ""), i);
            index.putIfAbsent(id.getPath().replaceFirst("^animania/manual/", "manual/"), i);
            index.putIfAbsent(id.getPath().replaceFirst("^manual/", ""), i);
        }
        // Links in the original pages use the base namespace even when the
        // target page is supplied by an addon.  The path alias above makes
        // those links work without rewriting addon resources.
        return index;
    }

    private static int firstPage(List<ManualPage> pages) {
        for (int i = 0; i < pages.size(); i++) if (pages.get(i).firstPage()) return i;
        for (int i = 0; i < pages.size(); i++) if (pages.get(i).id().getPath().endsWith("manual/startpage.json")) return i;
        return 0;
    }

    private static void collectContents(JsonElement element, List<String> lines) {
        if (element == null || element.isJsonNull()) return;
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            lines.add(element.getAsString());
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            array.forEach(child -> collectContents(child, lines));
        } else if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("contents")) collectContents(object.get("contents"), lines);
        }
    }

    private static List<ManualLine> parseLines(List<String> rawLines) {
        List<ManualLine> lines = new ArrayList<>();
        for (String raw : rawLines) {
            if (raw == null || raw.isBlank()) {
                lines.add(new ManualLine(Component.empty(), null, null, 0xFF3D2413));
                continue;
            }
            if (raw.startsWith("@image@")) {
                ResourceLocation image = resolveImage(raw.substring("@image@".length()).trim());
                ManualVisual visual = image == null ? null
                        : new ManualVisual(VisualKind.IMAGE, image, List.of(), 128);
                lines.add(new ManualLine(Component.empty(), null, visual, 0xFF3D2413));
                continue;
            }
            if (raw.startsWith("@link@")) {
                String payload = raw.substring("@link@".length());
                String[] parts = payload.split("#", -1);
                String target = parts.length > 0 ? parts[0] : "";
                String label = parts.length > 1 ? parts[1] : target;
                boolean append = parts.length > 2 && "prevline".equalsIgnoreCase(parts[2]);
                ManualLine line = new ManualLine(asComponent(label).copy().withStyle(ChatFormatting.UNDERLINE),
                        target, null, 0xFF4B3A8A);
                appendOrAdd(lines, line, append);
                continue;
            }
            if (raw.startsWith("@text@")) {
                String payload = raw.substring("@text@".length());
                String[] parts = payload.split("#", -1);
                boolean append = parts.length > 1 && "prevline".equalsIgnoreCase(parts[1]);
                appendOrAdd(lines, new ManualLine(asComponent(parts[0]), null, null, 0xFF3D2413), append);
                continue;
            }
            if (raw.startsWith("@crafting@") || raw.startsWith("@item@") || raw.startsWith("@entity@")
                    || raw.startsWith("@loottable@") || raw.startsWith("@config@") || raw.startsWith("@configitem@")) {
                int end = raw.indexOf('@', 1);
                String marker = raw.substring(1, end);
                String payload = raw.substring(end + 1);
                boolean append = payload.endsWith("#prevline");
                if (append) payload = payload.substring(0, payload.length() - "#prevline".length());
                payload = payload.trim();
                ManualVisual visual = visualForMarker(marker, payload);
                Component fallback = visual == null ? componentForMarker(marker, payload) : Component.empty();
                appendOrAdd(lines, new ManualLine(fallback, null, visual, 0xFF3D2413), append);
                continue;
            }
            appendOrAdd(lines, new ManualLine(asComponent(raw), null, null, 0xFF3D2413), false);
        }
        return lines;
    }

    private static void appendOrAdd(List<ManualLine> lines, ManualLine line, boolean append) {
        if (!append || lines.isEmpty()) {
            lines.add(line);
            return;
        }
        ManualLine previous = lines.remove(lines.size() - 1);
        Component combined = previous.component().copy().append(line.component());
        lines.add(new ManualLine(combined, line.target() != null ? line.target() : previous.target(),
                previous.visual(), previous.color()));
    }

    private static ManualVisual visualForMarker(String marker, String payload) {
        VisualKind kind = switch (marker.toLowerCase(Locale.ROOT)) {
            case "crafting" -> VisualKind.CRAFTING;
            case "item" -> VisualKind.ITEMS;
            case "entity" -> VisualKind.ENTITIES;
            default -> null;
        };
        if (kind == null) return null;
        List<ResourceLocation> ids = parseIds(payload);
        if (ids.isEmpty()) return null;
        int height = switch (kind) {
            case CRAFTING -> 78;
            case ITEMS -> 42;
            case ENTITIES -> 86;
            default -> 128;
        };
        return new ManualVisual(kind, null, ids, height);
    }

    private static List<ResourceLocation> parseIds(String payload) {
        List<ResourceLocation> ids = new ArrayList<>();
        for (String token : payload.split(",")) {
            String value = token.trim();
            int metadata = value.indexOf('#');
            if (metadata >= 0) value = value.substring(0, metadata).trim();
            ResourceLocation id = ResourceLocation.tryParse(value);
            if (id != null && !ids.contains(id)) ids.add(id);
        }
        return ids;
    }

    private static ResourceLocation resolveImage(String raw) {
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null) return null;
        if (hasResource(id)) return id;
        if (!"animania".equals(id.getNamespace())) return id;
        for (String namespace : addonNamespaces()) {
            ResourceLocation candidate = new ResourceLocation(namespace, id.getPath());
            if (hasResource(candidate)) return candidate;
        }
        return id;
    }

    private static boolean hasResource(ResourceLocation id) {
        return Minecraft.getInstance().getResourceManager().getResource(id).isPresent();
    }

    private static ResourceLocation resolveRegistryId(ResourceLocation id, Predicate<ResourceLocation> exists) {
        if (id == null || exists.test(id)) return id;
        if (!"animania".equals(id.getNamespace())) return id;
        for (String namespace : addonNamespaces()) {
            ResourceLocation candidate = new ResourceLocation(namespace, id.getPath());
            if (exists.test(candidate)) return candidate;
        }
        return id;
    }

    private static List<String> addonNamespaces() {
        return List.of("animania_farm", "animania_extra", "animania_catsdogs");
    }

    private void renderVisual(GuiGraphics graphics, ManualVisual visual, int x, int y, float partialTick) {
        switch (visual.kind()) {
            case IMAGE -> renderImage(graphics, visual.image(), x, y);
            case ITEMS -> renderItems(graphics, visual.ids(), x, y);
            case CRAFTING -> renderCrafting(graphics, visual.ids(), x, y);
            case ENTITIES -> renderEntities(graphics, visual.ids(), x, y, partialTick);
        }
    }

    private void renderImage(GuiGraphics graphics, ResourceLocation image, int x, int y) {
        if (image == null) return;
        int[] sourceSize = imageSize(image);
        int maxSize = Math.min(128, BODY_WIDTH);
        float scale = Math.min(1.0F, Math.min((float) maxSize / sourceSize[0],
                (float) maxSize / sourceSize[1]));
        int drawWidth = Math.max(1, Math.round(sourceSize[0] * scale));
        int drawHeight = Math.max(1, Math.round(sourceSize[1] * scale));
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(image, x + (BODY_WIDTH - drawWidth) / 2, y,
                drawWidth, drawHeight, 0.0F, 0.0F,
                sourceSize[0], sourceSize[1], sourceSize[0], sourceSize[1]);
    }

    private static int[] imageSize(ResourceLocation image) {
        return IMAGE_SIZES.computeIfAbsent(image, ManualScreen::readImageSize);
    }

    private static int[] readImageSize(ResourceLocation image) {
        try {
            Resource resource = Minecraft.getInstance().getResourceManager().getResource(image).orElse(null);
            if (resource == null) return new int[]{128, 128};
            try (InputStream stream = resource.open(); NativeImage nativeImage = NativeImage.read(stream)) {
                return new int[]{Math.max(1, nativeImage.getWidth()), Math.max(1, nativeImage.getHeight())};
            }
        } catch (Exception ignored) {
            return new int[]{128, 128};
        }
    }

    private void renderItems(GuiGraphics graphics, List<ResourceLocation> ids, int x, int y) {
        int count = Math.min(8, ids.size());
        int slotWidth = 36;
        int left = x + (BODY_WIDTH - count * slotWidth) / 2;
        for (int i = 0; i < count; i++) {
            ItemStack stack = itemStack(ids.get(i));
            if (!stack.isEmpty()) graphics.renderItem(stack, left + i * slotWidth + 2, y + 4);
        }
    }

    private void renderCrafting(GuiGraphics graphics, List<ResourceLocation> ids, int x, int y) {
        Recipe<?> recipe = findRecipe(ids);
        if (recipe == null) {
            // Several legacy pages use the old metadata form (for example the
            // coloured hamster balls) rather than a recipe id.  Still render
            // the referenced output items instead of leaking the marker text.
            renderItems(graphics, ids, x, y + 16);
            return;
        }
        int gridLeft = x + (BODY_WIDTH - 108) / 2;
        List<Ingredient> ingredients = recipe.getIngredients();
        for (int slot = 0; slot < 9; slot++) {
            if (slot >= ingredients.size()) break;
            ItemStack stack = firstIngredientStack(ingredients.get(slot));
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, gridLeft + (slot % 3) * 18, y + (slot / 3) * 18);
            }
        }
        graphics.drawString(font, Component.literal("→"), gridLeft + 56, y + 20, 0xFF6B5136);
        if (Minecraft.getInstance().level != null) {
            ItemStack result = recipe.getResultItem(Minecraft.getInstance().level.registryAccess());
            if (!result.isEmpty()) graphics.renderItem(result, gridLeft + 74, y + 18);
        }
    }

    private static ItemStack firstIngredientStack(Ingredient ingredient) {
        // Tag-backed ingredients cache their expanded stacks.  The cache can
        // still be stale when a handbook is opened immediately after the
        // client receives the recipe/tag sync, so explicitly invalidate it
        // before choosing a representative icon.
        ingredient.checkInvalidation();
        // Ingredient.TagValue returns a barrier stack when a tag cannot be
        // expanded on the client. That stack is a diagnostic placeholder,
        // not a crafting ingredient; prefer the JSON representative so the
        // handbook can still show a concrete item (for example white wool,
        // an iron ingot, or the item whose path matches a dye tag).
        ItemStack representative = representativeFromIngredientJson(ingredient.toJson());
        if (!representative.isEmpty()) return representative;
        ItemStack[] stacks = ingredient.getItems();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty() && !stack.is(Items.BARRIER)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack representativeFromIngredientJson(JsonElement element) {
        if (element == null || element.isJsonNull()) return ItemStack.EMPTY;
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                ItemStack result = representativeFromIngredientJson(child);
                if (!result.isEmpty()) return result;
            }
            return ItemStack.EMPTY;
        }
        if (!element.isJsonObject()) return ItemStack.EMPTY;
        JsonObject object = element.getAsJsonObject();
        if (object.has("item")) {
            ResourceLocation id = ResourceLocation.tryParse(object.get("item").getAsString());
            return id == null ? ItemStack.EMPTY : itemStack(id);
        }
        if (object.has("tag")) {
            ResourceLocation id = ResourceLocation.tryParse(object.get("tag").getAsString());
            if (id == null) return ItemStack.EMPTY;
            TagKey<Item> tag = ForgeRegistries.ITEMS.tags().createTagKey(id);
            for (Item item : ForgeRegistries.ITEMS.tags().getTag(tag)) {
                if (item != null) return new ItemStack(item);
            }
            // The handbook can be opened while a recipe/tag packet is still
            // being applied.  Keep the icon useful in that short window by
            // choosing a stable vanilla representative for common tags.
            return fallbackTagStack(id);
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack fallbackTagStack(ResourceLocation tagId) {
        // A number of legacy recipes use a tag whose path is also the item
        // path (for example minecraft:red_wool or animania:salt).
        ItemStack direct = itemStack(tagId);
        if (!direct.isEmpty()) return direct;
        String itemId = switch (tagId.toString()) {
            case "minecraft:seeds" -> "minecraft:wheat_seeds";
            case "minecraft:planks" -> "minecraft:oak_planks";
            case "minecraft:iron_ingots" -> "minecraft:iron_ingot";
            case "minecraft:iron_nuggets" -> "minecraft:iron_nugget";
            case "minecraft:gold_nuggets" -> "minecraft:gold_nugget";
            case "minecraft:wool" -> "minecraft:white_wool";
            case "minecraft:logs" -> "minecraft:oak_log";
            case "minecraft:dyes" -> "minecraft:white_dye";
            case "minecraft:leather" -> "minecraft:leather";
            case "minecraft:leaves" -> "minecraft:oak_leaves";
            case "minecraft:cooked_porkchop" -> "minecraft:cooked_porkchop";
            case "minecraft:sugar" -> "minecraft:sugar";
            case "minecraft:sand" -> "minecraft:sand";
            case "minecraft:cobblestone" -> "minecraft:cobblestone";
            case "minecraft:string" -> "minecraft:string";
            case "minecraft:wooden_slabs" -> "minecraft:oak_slab";
            case "minecraft:fences" -> "minecraft:oak_fence";
            case "minecraft:trapdoors" -> "minecraft:oak_trapdoor";
            case "minecraft:chests" -> "minecraft:chest";
            case "minecraft:glass" -> "minecraft:glass";
            case "minecraft:glass_panes" -> "minecraft:glass_pane";
            case "minecraft:buttons" -> "minecraft:oak_button";
            case "minecraft:pressure_plates" -> "minecraft:oak_pressure_plate";
            default -> null;
        };
        ResourceLocation item = itemId == null ? null : ResourceLocation.tryParse(itemId);
        return item == null ? ItemStack.EMPTY : itemStack(item);
    }

    private static ItemStack itemStack(ResourceLocation id) {
        ResourceLocation resolved = resolveRegistryId(id, candidate -> ForgeRegistries.ITEMS.containsKey(candidate));
        Item item = ForgeRegistries.ITEMS.getValue(resolved);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static Recipe<?> findRecipe(List<ResourceLocation> ids) {
        if (Minecraft.getInstance().level == null) return null;
        var manager = Minecraft.getInstance().level.getRecipeManager();
        for (ResourceLocation id : ids) {
            ResourceLocation resolved = resolveRegistryId(id, candidate -> manager.byKey(candidate).isPresent());
            var holder = manager.byKey(resolved);
            if (holder.isPresent()) return holder.get();
        }
        return null;
    }

    private void renderEntities(GuiGraphics graphics, List<ResourceLocation> ids, int x, int y, float partialTick) {
        if (Minecraft.getInstance().level == null) return;
        int count = Math.min(3, ids.size());
        int slotWidth = Math.max(72, BODY_WIDTH / Math.max(1, count));
        for (int i = 0; i < count; i++) {
            ResourceLocation resolved = resolveRegistryId(ids.get(i),
                    candidate -> ForgeRegistries.ENTITY_TYPES.containsKey(candidate));
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(resolved);
            if (type == null) continue;
            try {
                LivingEntity preview = previewEntities.get(resolved);
                if (preview == null || preview.isRemoved()) {
                    preview = type.create(Minecraft.getInstance().level) instanceof LivingEntity living
                            ? living : null;
                    if (preview != null) {
                        // The handbook is a catalogue, not a random-spawn
                        // simulation.  Use the resolver's canonical fallback
                        // (white/black/variant 0 as appropriate) so the first
                        // frame is deterministic as well as subsequent ones.
                        if (preview instanceof AnimaniaAnimalEntity animal) animal.setVariantName("default");
                        previewEntities.put(resolved, preview);
                    }
                }
                if (preview == null) continue;
                preview.tickCount = 1;
                // InventoryScreen performs the complete GUI projection,
                // depth state, lighting, camera override and buffer flush.
                // Calling EntityRenderDispatcher directly from a 2-D screen
                // leaves the model behind the GUI and appears as tiny dots.
                // Forge's FollowsAngle overload accepts a direct angle
                // component: one unit is 20 degrees of body yaw. A 1.25
                // component gives the catalogue a stable 25-degree
                // three-quarter view, which exposes the animal's silhouette
                // without turning it into a side/back view.
                InventoryScreen.renderEntityInInventoryFollowsAngle(graphics,
                        x + slotWidth * i + slotWidth / 2, y + 66, 58,
                        ENTITY_PREVIEW_YAW_COMPONENT, 0.0F, preview);
            } catch (RuntimeException ignored) {
                // An optional addon can register an entity without a client
                // renderer. One bad preview must not close the handbook.
            }
        }
    }

    private static Component componentForMarker(String marker, String payload) {
        String key = switch (marker.toLowerCase(Locale.ROOT)) {
            case "crafting" -> "manual.component.crafting";
            case "item" -> "manual.component.item";
            case "entity" -> "manual.component.entity";
            case "loottable" -> "manual.component.loot";
            case "config", "configitem" -> "manual.component.config";
            default -> "manual.component.value";
        };
        String value = marker.equalsIgnoreCase("config") || marker.equalsIgnoreCase("configitem")
                ? formatConfigReference(payload) : payload;
        return Component.translatable(key, Component.literal(value));
    }

    private static String formatConfigReference(String payload) {
        String category = "";
        String key = payload == null ? "" : payload.trim();
        int separator = key.indexOf(';');
        if (separator >= 0) {
            category = key.substring(0, separator).trim();
            key = key.substring(separator + 1).trim();
        }
        String categoryLabel = switch (category) {
            case "general.catsdogs" -> "猫与狗";
            case "general.farm", "general.farm.spawning_and_breeding" -> "农场";
            case "general.extra", "general.extra.spawning_and_breeding" -> "额外动物";
            case "general.careandfeeding" -> "照料与喂养";
            case "general.gamerules" -> "游戏规则";
            default -> "";
        };
        String keyLabel = switch (key) {
            case "catFood" -> "猫咪食物";
            case "catBed" -> "猫窝";
            case "catBed2" -> "猫窝（第二种）";
            case "dogFood" -> "狗粮";
            case "dogBed" -> "狗窝";
            case "dogBed2" -> "狗窝（第二种）";
            case "petBowlFood" -> "宠物食盆食物";
            case "wolfBiomeTypes" -> "狼生成生物群系";
            case "foxBiomeTypes" -> "狐狸生成生物群系";
            case "ocelotBiomeTypes" -> "豹猫生成生物群系";
            case "cheeseMaturityTime" -> "奶酪成熟时间";
            case "foodsGiveBonusEffects" -> "食物额外效果";
            case "hiveWildHoneyRate" -> "野生蜂巢产蜜速度";
            case "hivePlayermadeHoneyRate" -> "人工蜂箱产蜜速度";
            case "hiveValidBiomeTypes" -> "蜂巢生效生物群系";
            case "hamsterWheelUseTime" -> "仓鼠轮使用时间";
            case "hamsterWheelRFGeneration" -> "仓鼠轮发电量";
            case "hamsterWheelCapacity" -> "仓鼠轮容量";
            case "troughFood" -> "食槽食物";
            case "ferretFood" -> "雪貂食物";
            case "hamsterFood" -> "仓鼠食物";
            case "hedgehogFood" -> "刺猬食物";
            case "peacockFood" -> "孔雀食物";
            case "rabbitFood" -> "兔子食物";
            case "chickenFood" -> "鸡食物";
            case "cowFood" -> "牛食物";
            case "goatFood" -> "山羊食物";
            case "horseFood" -> "马食物";
            case "pigFood" -> "猪食物";
            case "sheepFood" -> "绵羊食物";
            default -> configKeyLabel(key);
        };
        if (categoryLabel.isEmpty()) return keyLabel;
        return categoryLabel + "：" + keyLabel;
    }

    private static String splitCamelCase(String value) {
        if (value == null || value.isBlank()) return "";
        return value.replace('_', ' ')
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .trim();
    }

    private static String configKeyLabel(String key) {
        if (key == null || key.isBlank()) return "";
        if (key.endsWith("BiomeTypes")) {
            String species = key.substring(0, key.length() - "BiomeTypes".length());
            String speciesLabel = switch (species) {
                case "fox" -> "狐狸";
                case "ocelot" -> "豹猫";
                case "wolf" -> "狼";
                case "dartFrog" -> "箭毒蛙";
                case "ferretGray" -> "灰雪貂";
                case "frog" -> "青蛙";
                case "hamster" -> "仓鼠";
                case "hedgehog" -> "刺猬";
                case "toad" -> "蟾蜍";
                case "draftHorse" -> "挽马";
                case "peafowlBlue" -> "蓝孔雀";
                case "peafowlCharcoal" -> "炭灰孔雀";
                case "peafowlOpal" -> "欧泊孔雀";
                case "peafowlPeach" -> "蜜桃孔雀";
                case "peafowlPurple" -> "紫孔雀";
                case "peafowlTaupe" -> "灰褐孔雀";
                case "peafowlWhite" -> "白孔雀";
                case "rabbitChinchilla" -> "龙猫兔";
                case "rabbitCottontail" -> "棉尾兔";
                case "rabbitDutch" -> "荷兰兔";
                case "rabbitHavana" -> "哈瓦那兔";
                case "rabbitJack" -> "杰克兔";
                case "rabbitLop" -> "垂耳兔";
                case "rabbitNewZealand" -> "新西兰兔";
                case "rabbitRex" -> "雷克斯兔";
                case "chickenLeghorn" -> "来航鸡";
                case "chickenOrpington" -> "奥平顿鸡";
                case "chickenPlymouthRock" -> "普利茅斯岩鸡";
                case "chickenRhodeIslandRed" -> "罗得岛红鸡";
                case "chickenWyandotte" -> "怀恩多特鸡";
                case "cowAngus" -> "安格斯牛";
                case "cowFriesian" -> "弗里斯兰牛";
                case "cowHereford" -> "赫里福德牛";
                case "cowHigland" -> "高地牛";
                case "cowHolstein" -> "荷斯坦牛";
                case "cowJersey" -> "泽西牛";
                case "cowLonghorn" -> "长角牛";
                case "cowMooshroom" -> "哞菇牛";
                case "goatAlpine" -> "阿尔卑斯山羊";
                case "goatAngora" -> "安哥拉山羊";
                case "goatFainting" -> "晕倒山羊";
                case "goatKiko" -> "基科山羊";
                case "goatKinder" -> "金德山羊";
                case "goatNigerianDwarf" -> "尼日利亚矮山羊";
                case "goatPygmy" -> "侏儒山羊";
                case "pigDuroc" -> "杜洛克猪";
                case "pigHampshire" -> "汉普夏猪";
                case "pigLargeBlack" -> "大黑猪";
                case "pigLargeWhite" -> "大白猪";
                case "pigOldSpot" -> "老斑猪";
                case "pigYorkshire" -> "约克夏猪";
                case "sheepDorper" -> "多普羊";
                case "sheepDorset" -> "多塞特羊";
                case "sheepFriesian" -> "弗里斯兰羊";
                case "sheepJacob" -> "雅各布羊";
                case "sheepMerino" -> "美利奴羊";
                case "sheepSuffolk" -> "萨福克羊";
                default -> splitCamelCase(species);
            };
            return speciesLabel + "生成生物群系";
        }
        return splitCamelCase(key);
    }

    private static Component asComponent(String raw) {
        if (raw == null || raw.isBlank()) return Component.empty();
        String value = raw.trim();
        Style style = Style.EMPTY;
        if (value.startsWith("§l")) {
            value = value.substring(2);
            style = style.withBold(true);
        }
        if (value.startsWith("§r")) value = value.substring(2);
        value = value.replaceFirst("^[←→↩]\\s*", "");
        value = value.replaceFirst("^-\\s*", "");
        int metadata = value.indexOf('$');
        if (metadata > 0) value = value.substring(0, metadata);
        if (value.matches("[a-zA-Z0-9_.:-]+")) return Component.translatable(value).withStyle(style);
        return Component.literal(value).withStyle(style);
    }

    private enum VisualKind { IMAGE, ITEMS, CRAFTING, ENTITIES }

    private record ManualVisual(VisualKind kind, ResourceLocation image, List<ResourceLocation> ids, int height) { }

    private record ManualPage(ResourceLocation id, Component title, List<ManualLine> lines, boolean firstPage,
                              String parentTarget) { }

    private record ManualLine(Component component, String target, ManualVisual visual, int color) { }

    private record LinkHit(int x, int y, int width, int height, String target) { }
}
