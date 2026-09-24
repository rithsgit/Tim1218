package com.example.addon.mixin;

import com.example.addon.modules.DungeonAssistant;
import com.example.addon.modules.Inventory101;
import com.example.addon.modules.LootLens;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BooleanSupplier;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin extends Screen {
    @Shadow protected int backgroundWidth;
    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected Slot focusedSlot;

    @Unique private ButtonWidget s1Button;
    @Unique private ButtonWidget s2Button;

    protected HandledScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button == 2) { // Middle click
            Inventory101 inv101 = Modules.get().get(Inventory101.class);
            if (inv101 != null && inv101.isActive()) {
                if (this.focusedSlot != null && this.focusedSlot.hasStack()) {
                    ItemStack stack = this.focusedSlot.getStack();
                    if (Inventory101.isShulker(stack)) {
                        inv101.openPreview(stack);
                        cir.setReturnValue(true);
                    } else if (Inventory101.isEnderChest(stack)) {
                        inv101.openEnderChestPreview(stack);
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderGlobalShulkerIcons(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Inventory101 inv101 = Modules.get().get(Inventory101.class);
        if (inv101 == null || !inv101.isActive()) return;

        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;

        if (screen instanceof GenericContainerScreen containerScreen) {
            String titleStr = containerScreen.getTitle().getString().toLowerCase();
            if (titleStr.contains("ender chest") || containerScreen.getScreenHandler().getInventory() instanceof net.minecraft.inventory.EnderChestInventory) {
                Inventory101.updateCachedEnderChest(containerScreen.getScreenHandler().getInventory());
            }
        }

        for (Slot slot : screen.getScreenHandler().slots) {
            if (slot.hasStack()) {
                ItemStack stack = slot.getStack();
                if (Inventory101.isShulker(stack)) {
                    ItemStack dominant = Inventory101.getDominantItem(stack);
                    if (!dominant.isEmpty()) {
                        float scale = (float) inv101.getIconScale();
                        float centerOffset = (16.0f * (1.0f - scale)) / 2.0f;

                        context.getMatrices().pushMatrix();
                        context.getMatrices().translate(this.x + slot.x + 1 + centerOffset, this.y + slot.y + 1 + centerOffset);
                        context.getMatrices().scale(scale, scale);
                        context.drawItem(dominant, 0, 0);
                        context.getMatrices().popMatrix();
                    }
                }
            }
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        s1Button = null;
        s2Button = null;

        Inventory101 inv101       = Modules.get().get(Inventory101.class);
        if ((Object) this instanceof InventoryScreen && inv101 != null && inv101.isActive()) {
            int bx = this.x - 25;
            int by = this.y;

            s1Button = mouseOnly(Text.literal("S1"),
                btn -> inv101.startInvSort(1), bx, by, 20, 20,
                net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Sort to " + inv101.getPresetName(1))),
                () -> !inv101.isPresetEmpty(1));

            s2Button = mouseOnly(Text.literal("S2"),
                btn -> inv101.startInvSort(2), bx, by + 25, 20, 20,
                net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Sort to " + inv101.getPresetName(2))),
                () -> !inv101.isPresetEmpty(2));

            this.addDrawableChild(s1Button);
            this.addDrawableChild(s2Button);
            return;
        }

        HandledScreen<?> screen        = (HandledScreen<?>) (Object) this;
        int              containerSlots = screen.getScreenHandler().slots.size() - 36;

        if (inv101 != null && inv101.isActive()) {
            if ((Object) this instanceof ShulkerBoxScreen) {
                int bx = this.x - 25;
                int by = this.y;

                this.addDrawableChild(mouseOnly(Text.literal("S"),
                    btn -> inv101.toggleSaveMode(), bx, by, 20, 20,
                    net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Save Current Layout"))));
                by += 25;

                this.addDrawableChild(mouseOnly(Text.literal("1"),
                    btn -> {
                        if (!inv101.isSaveMode() && inv101.isPresetEmpty(1)) return;
                        inv101.handlePreset(1);
                    }, bx, by, 20, 20,
                    net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Load " + inv101.getPresetName(1))),
                    () -> !inv101.isPresetEmpty(1)));
                by += 25;

                this.addDrawableChild(mouseOnly(Text.literal("2"),
                    btn -> {
                        if (!inv101.isSaveMode() && inv101.isPresetEmpty(2)) return;
                        inv101.handlePreset(2);
                    }, bx, by, 20, 20,
                    net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Load " + inv101.getPresetName(2))),
                    () -> !inv101.isPresetEmpty(2)));
                by += 25;

                this.addDrawableChild(mouseOnly(Text.literal("C"),
                    btn -> inv101.clearPresets(), bx, by, 20, 20,
                    net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Clear Presets"))));
                by += 25;

                if (inv101.isRegearButtonEnabled()) {
                    this.addDrawableChild(mouseOnly(Text.literal("G"),
                        btn -> inv101.startRegearing(), bx, by, 20, 20,
                        net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Equip armor and replenish essentials"))));
                    by += 25;
                }

                if (inv101.isReplenishButtonEnabled()) {
                    this.addDrawableChild(mouseOnly(Text.literal("R"),
                        btn -> inv101.startReplenishing(), bx, by, 20, 20,
                        net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Replenish whitelisted items from shulker"))));
                }

                return;
            }

            if ((Object) this instanceof GenericContainerScreen && inv101.isSortButtonEnabled()) {
                int bx = this.x + this.backgroundWidth - 70;
                int by = this.y + 2;
                this.addDrawableChild(mouseOnly(Text.literal("Sort"),
                    btn -> inv101.startSorting(), bx, by, 30, 14, 
                    net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Sort shulkers by colour"))));
            }
        }

        if (containerSlots <= 0) return;

        LootLens ll = Modules.get().get(LootLens.class);
        if (ll != null && ll.shouldShowStealDumpButtons()) {
            addStealDumpButtons(screen, containerSlots);
        } else {
            DungeonAssistant da = Modules.get().get(DungeonAssistant.class);
            if (da != null && da.isActive()) {
                addStealDumpButtons(screen, containerSlots);
            }
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if ((Object) this instanceof InventoryScreen && s1Button != null && s2Button != null) {
            int defaultX = (this.width - this.backgroundWidth) / 2;
            boolean isRecipeBookOpen = this.x > defaultX + 50; 
            
            int bx = isRecipeBookOpen ? (this.x + this.backgroundWidth + 5) : (this.x - 25);
            int by = this.y;
            
            s1Button.setPosition(bx, by);
            s2Button.setPosition(bx, by + 25);
        }
    }

    private static ButtonWidget mouseOnly(Text label, ButtonWidget.PressAction action,
                                       int x, int y, int width, int height,
                                       net.minecraft.client.gui.tooltip.Tooltip tooltip) {
        return mouseOnly(label, action, x, y, width, height, tooltip, null);
    }

    private static ButtonWidget mouseOnly(Text label, ButtonWidget.PressAction action,
                                       int x, int y, int width, int height,
                                       net.minecraft.client.gui.tooltip.Tooltip tooltip,
                                       BooleanSupplier hasData) {
        ButtonWidget btn = new ButtonWidget(x, y, width, height, label, action,
            textSupplier -> textSupplier.get().copy()) {
            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                return false;
            }
            @Override
            public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
                return false;
            }
            @Override
            protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
                if (hasData != null) {
                    boolean prev = this.active;
                    this.active = hasData.getAsBoolean();
                    super.renderWidget(context, mouseX, mouseY, delta);
                    this.active = prev;
                } else {
                    super.renderWidget(context, mouseX, mouseY, delta);
                }
            }
        };
        if (tooltip != null) btn.setTooltip(tooltip);
        return btn;
    }

    private void addStealDumpButtons(HandledScreen<?> screen, int containerSlots) {
        int buttonX, buttonY, buttonW, buttonH, buttonGap;

        if ((Object) this instanceof GenericContainerScreen) {
            buttonW = 14;
            buttonH = 14;
            buttonGap = 2;
            buttonX = this.x + this.backgroundWidth - 8 - buttonW - buttonGap - buttonW;
            buttonY = this.y + 2;
        } else {
            buttonW = 20;
            buttonH = 20;
            buttonGap = 4;

            int screenWidth = this.width;
            int rightEdge = this.x + this.backgroundWidth + 5 + buttonW;

            if (rightEdge <= screenWidth) {
                buttonX = this.x + this.backgroundWidth + 5;
                buttonY = this.y + 5;
            } else {
                int containerRows = (containerSlots + 8) / 9;
                buttonX = this.x + (this.backgroundWidth - (buttonW * 2 + buttonGap)) / 2;
                buttonY = this.y + containerRows * 18 + 2;
            }
        }

        this.addDrawableChild(mouseOnly(Text.literal("S"),
            button -> {
                for (int i = 0; i < containerSlots; i++) {
                    if (screen.getScreenHandler().getSlot(i).hasStack()) {
                        client.interactionManager.clickSlot(
                            screen.getScreenHandler().syncId, i, 0,
                            SlotActionType.QUICK_MOVE, client.player);
                    }
                }
            }, buttonX, buttonY, buttonW, buttonH,
            net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Steal all items from container"))));

        this.addDrawableChild(mouseOnly(Text.literal("D"),
            button -> {
                for (int i = containerSlots; i < screen.getScreenHandler().slots.size(); i++) {
                    if (screen.getScreenHandler().getSlot(i).getStack().isEmpty()) continue;
                    client.interactionManager.clickSlot(
                        screen.getScreenHandler().syncId, i, 0,
                        SlotActionType.QUICK_MOVE, client.player);
                }
            }, buttonX + buttonW + buttonGap, buttonY, buttonW, buttonH,
            net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Dump all inventory items into container"))));
    }
}