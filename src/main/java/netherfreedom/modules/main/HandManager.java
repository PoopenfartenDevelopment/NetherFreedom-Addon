/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 * Modified by RedCarlos#0001
 */

package netherfreedom.modules.main;

import netherfreedom.modules.NetherFreedom;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.AutoTotem;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public class HandManager extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General
    private final Setting<Item> item = sgGeneral.add(new EnumSetting.Builder<Item>()
        .name("item")
        .description("Which item to hold in your offhand.")
        .defaultValue(Item.EGap)
        .build()
    );

    private final Setting<Boolean> hotbar = sgGeneral.add(new BoolSetting.Builder()
        .name("hotbar")
        .description("Whether to use items from your hotbar.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> rightClick = sgGeneral.add(new BoolSetting.Builder()
        .name("right-click")
        .description("Only holds the item in your offhand when you are holding right click.")
        .defaultValue(false)
        .build()
    );

    private boolean isClicking;
    private boolean sentMessage;
    private Item currentItem;

    public HandManager() {
        super(NetherFreedom.MAIN, "hand-manager", "Allows you to hold specified items in your offhand.");
    }

    @Override
    public void onActivate() {
        sentMessage = false;
        isClicking = false;
        currentItem = item.get();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        AutoTotem autoTotem = Modules.get().get(AutoTotem.class);

        currentItem = item.get();

        // Checking offhand item
        if (mc.player.getOffHandStack().getItem() != currentItem.item) {
            FindItemResult item = InvUtils.find(itemStack -> itemStack.getItem() == currentItem.item, hotbar.get() ? 0 : 9, 35);

            // No offhand item
            if (!item.found()) {
                if (!sentMessage) {
                    warning("Chosen item not found.");
                    sentMessage = true;
                }
            }

            // Swap to offhand
            else if ((isClicking || !rightClick.get()) && !autoTotem.isLocked() && !item.isOffhand()) {
                InvUtils.move().from(item.slot()).toOffhand();
                sentMessage = false;
            }
        }

        // If not clicking, set to totem if auto totem is on
        else if (!isClicking && rightClick.get()) {
            if (autoTotem.isActive()) {
                FindItemResult totem = InvUtils.find(itemStack -> itemStack.getItem() == Items.TOTEM_OF_UNDYING, hotbar.get() ? 0 : 9, 35);

                if (totem.found() && !totem.isOffhand()) {
                    InvUtils.move().from(totem.slot()).toOffhand();
                }
            } else {
                FindItemResult empty = InvUtils.find(ItemStack::isEmpty, hotbar.get() ? 0 : 9, 35);
                if (empty.found()) InvUtils.move().fromOffhand().to(empty.slot());
            }
        }
    }

    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        isClicking = mc.currentScreen == null && !Modules.get().get(AutoTotem.class).isLocked() && !usableItem() && !mc.player.isUsingItem() && event.action == KeyAction.Press && event.button == GLFW_MOUSE_BUTTON_RIGHT;
    }

    private boolean usableItem() {
        return mc.player.getMainHandStack().getItem() == Items.BOW
            || mc.player.getMainHandStack().getItem() == Items.TRIDENT
            || mc.player.getMainHandStack().getItem() == Items.CROSSBOW
            || mc.player.getMainHandStack().getItem().isFood();
    }

    @Override
    public String getInfoString() {
        return item.get().name();
    }

    public enum Item {
        EGap(Items.ENCHANTED_GOLDEN_APPLE),
        GoldenCarrot(Items.GOLDEN_CARROT),
        Porkchop(Items.COOKED_PORKCHOP),
        Steak(Items.COOKED_BEEF);

        net.minecraft.item.Item item;

        Item(net.minecraft.item.Item item) {
            this.item = item;
        }
    }
}
