package com.dooji.unslab.ui;

import com.dooji.omnilib.ui.OmniField;
import com.dooji.omnilib.ui.OmniFieldListWidget;
import com.dooji.omnilib.OmnilibClient;
import com.dooji.unslab.UnslabMapping;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class UnslabConfigScreen extends Screen {
    private final Screen parentScreen;
    private final String title;
    private OmniField searchField;
    private OmniFieldListWidget fieldListWidget;
    private String searchQuery = "";
    private final Map<Block, String> customMappings = new HashMap<>();
    private final Map<Block, Block> combinedMappings = new HashMap<>();
    private final File configFile = new File("config/Unslab/custom-mappings.json");

    public UnslabConfigScreen(Screen parentScreen, String title) {
        super(Text.translatable(title));
        this.parentScreen = parentScreen;
        this.title = title;
        loadCustomMappings();
        mergeMappings();
    }

    @Override
    protected void init() {
        super.init();

        int topPadding = 10;
        int fieldWidth = 200;
        int fieldHeight = 20;
        int centerX = this.width / 2;

        int searchFieldY = topPadding + 30;
        int listTop = searchFieldY + fieldHeight + topPadding;
        int listBottom = this.height - 50;
        int buttonAreaHeight = 50;
        int buttonY = this.height - buttonAreaHeight / 2 - fieldHeight / 2;
        int buttonSpacing = 20;
        int buttonWidth = 100;
        int listWidth = this.width;
        int itemHeight = 30;
        int horizontalPadding = 140;

        this.searchField = OmnilibClient.createOmniField(
                this.textRenderer,
                centerX - fieldWidth / 2,
                searchFieldY,
                fieldWidth,
                fieldHeight,
                Text.of("Search..."),
                this.searchQuery,
                this::onSearchChanged
        );
        this.addDrawableChild(this.searchField);

        this.addDrawableChild(OmnilibClient.createOmniButton(
                centerX - buttonSpacing - buttonWidth,
                buttonY,
                buttonWidth,
                fieldHeight,
                Text.of("Save"),
                0x80000000,
                0x80999999,
                0xFFFFFFFF,
                0xFFFFFF88,
                this::saveCustomMappings
        ));

        this.addDrawableChild(OmnilibClient.createOmniButton(
                centerX + buttonSpacing,
                buttonY,
                buttonWidth,
                fieldHeight,
                Text.of("Cancel"),
                0x80000000,
                0x80999999,
                0xFFFFFFFF,
                0xFFFFFF88,
                () -> this.client.setScreen(this.parentScreen)
        ));

        this.fieldListWidget = OmnilibClient.createOmniFieldListWidget(
                this.client,
                listWidth,
                listBottom - listTop,
                listTop,
                listBottom,
                listWidth - horizontalPadding,
                itemHeight,
                horizontalPadding,
                5,
                5,
                null,
                null,
                0x88000000,
                0x99555555,
                0x88000000,
                0x88888888,
                0xCC888888,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
        );

        mergeMappings();
        setFilteredItems();
        this.addSelectableChild(this.fieldListWidget);
    }

    private void loadCustomMappings() {
        if (!configFile.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(configFile)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                Block slab = Registries.BLOCK.get(Identifier.tryParse(entry.getKey()));
                customMappings.put(slab, entry.getValue().getAsString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveCustomMappings() {
        try {
            JsonObject json = new JsonObject();
            customMappings.forEach((slab, fullBlock) -> json.addProperty(
                    Registries.BLOCK.getId(slab).toString(),
                    fullBlock
            ));

            configFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(json.toString());
            }

            this.client.setScreen(this.parentScreen);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mergeMappings() {
        combinedMappings.putAll(UnslabMapping.getSlabToBlockMap());

        customMappings.forEach((slab, fullBlockId) -> {
            Block fullBlock = Registries.BLOCK.get(Identifier.tryParse(fullBlockId));

            if (fullBlock != null) {
                combinedMappings.put(slab, fullBlock);
            }
        });
    }

    private void onSearchChanged(String query) {
        this.searchQuery = query.toLowerCase().trim();
        this.setFilteredItems();
    }

    private void setFilteredItems() {
        String[] searchTokens = searchQuery.split("\\s+");

        List<Block> slabs = combinedMappings.keySet().stream()
                .filter(block -> matchesSearch(block, searchTokens))
                .collect(Collectors.toList());

        List<ItemStack> itemStacks = slabs.stream()
                .map(Block::asItem)
                .map(ItemStack::new)
                .collect(Collectors.toList());

        List<String> content = slabs.stream()
                .map(slab -> Registries.BLOCK.getId(slab).getPath())
                .collect(Collectors.toList());

        List<OmniField> fields = slabs.stream()
                .map(slab -> createOmniFieldForSlab(slab))
                .collect(Collectors.toList());

        this.fieldListWidget.setItemsWithFields(itemStacks, content, fields);
    }

    private boolean matchesSearch(Block block, String[] searchTokens) {
        if (searchTokens.length == 0) {
            return true;
        }

        String blockName = Registries.BLOCK.getId(block).getPath().toLowerCase();

        for (String token : searchTokens) {
            if (!blockName.contains(token)) {
                return false;
            }
        }

        return true;
    }

    private OmniField createOmniFieldForSlab(Block slab) {
        Block mappedBlock = combinedMappings.get(slab);
        String mappedBlockId = mappedBlock != null ? Registries.BLOCK.getId(mappedBlock).toString() : "";
        AtomicReference<OmniField> fieldRef = new AtomicReference<>();

        OmniField field = OmnilibClient.createOmniField(
                this.textRenderer,
                0, 0,
                200, 20,
                Text.of(""),
                mappedBlockId,
                newValue -> {
                    Identifier id = Identifier.tryParse(newValue);
                    OmniField actualField = fieldRef.get();
                    if (id != null && Registries.BLOCK.containsId(id)) {
                        customMappings.put(slab, newValue);

                        if (actualField != null) {
                            actualField.setColor(0x88000000, 0x99000000);
                        }
                    } else {
                        customMappings.remove(slab);

                        if (actualField != null) {
                            actualField.setColor(0x88FF4444, 0x99FF6666);
                        }
                    }
                }
        );

        fieldRef.set(field);

        Identifier id = Identifier.tryParse(mappedBlockId);

        if (id == null || !Registries.BLOCK.containsId(id)) {
            field.setColor(0x88FF4444, 0x99FF6666);
        }

        return field;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.saveCustomMappings();
            this.client.setScreen(this.parentScreen);
            
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        int titleHeight = this.textRenderer.fontHeight;
        int titleY = (this.searchField.getY() - titleHeight) / 2;
        int titleWidth = this.textRenderer.getWidth(this.title);

        context.drawText(this.textRenderer, this.title, (this.width - titleWidth) / 2, titleY, 0xFFFFFF, false);

        if (this.fieldListWidget != null) {
            this.fieldListWidget.render(context, mouseX, mouseY, delta);
        }
    }
}