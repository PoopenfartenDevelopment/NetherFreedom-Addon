package me.redcarlos.netherfreedom.modules.main;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.AutoTotem;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;
import me.redcarlos.netherfreedom.NFAddon;

public class HandManager extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAutoGap = settings.createGroup("Auto Gap");
    private final SettingGroup sgAutoTotem = settings.createGroup("Auto Totem");

    // General

    private final Setting<Boolean> hotbar = sgGeneral.add(new BoolSetting.Builder()
        .name("hotbar")
        .description("Whether to use items from your hotbar.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> warningMsg = sgGeneral.add(new BoolSetting.Builder()
        .name("notify")
        .description("Warns you when the selected item is not found in your inventory.")
        .defaultValue(false)
        .build()
    );

    // Auto Gap

    private final Setting<Integer> hungerThreshold = sgAutoGap.add(new IntSetting.Builder()
        .name("hunger")
        .description("Hunger to gap at.")
        .defaultValue(16)
        .range(1, 19)
        .sliderRange(1, 19)
        .build()
    );

    private final Setting<Boolean> autoFireRes = sgAutoGap.add(new BoolSetting.Builder()
        .name("fire-resistance")
        .description("Gap when no fire resistance effect.")
        .defaultValue(true)
        .build()
    );

    // Auto Totem

    private final Setting<Integer> healthThreshold = sgAutoTotem.add(new IntSetting.Builder()
        .name("min-health")
        .description("The minimum health to hold a totem at.")
        .defaultValue(6)
        .range(0, 35)
        .sliderMax(35)
        .build()
    );

    private final Setting<Boolean> fallDamage = sgAutoTotem.add(new BoolSetting.Builder()
        .name("fall-damage")
        .description("Holds a totem when fall damage could kill you.")
        .defaultValue(true)
        .build()
    );

    private Item currentItem;
    private boolean eating;
    private boolean sentMsg;
    private boolean swapped;

    public HandManager() {
        super(NFAddon.Main, "hand-manager", "Automatically manages your offhand (optimized for digging netherrack).");
    }

    @Override
    public void onActivate() {
        eating = false;
        sentMsg = false;
        swapped = false;
        currentItem = Item.Totem;
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (!Utils.canUpdate()) return;

        Modules modules = Modules.get();
        AutoTotem autoTotem = modules.get(AutoTotem.class);
        if (Modules.get().get(NFScaffold.class).hasWorked()) return;

        if (mc.options.swapHandsKey.isPressed() && !swapped && mc.player.getOffHandStack().getItem() == currentItem.item) swapped = true;
        else if (mc.options.swapHandsKey.isPressed() && swapped && mc.player.getMainHandStack().getItem() == currentItem.item) swapped = false;
        else swapped = false;

        // Checking offhand item
        if (mc.player.getOffHandStack().getItem() != currentItem.item && !swapped) {
            FindItemResult item = InvUtils.find(itemStack -> itemStack.getItem() == currentItem.item, hotbar.get() ? 0 : 9, 35);

            if (!item.found()) item = InvUtils.find(itemStack -> itemStack.getItem() == null, hotbar.get() ? 0 : 9, 35);

            if (!item.found()) {
                if (!sentMsg) {
                    if (warningMsg.get()) warning("Chosen item not found.");
                    sentMsg = true;
                }
            }

            // Swap to offhand
            else if (!autoTotem.isLocked() && !item.isOffhand()) {
                InvUtils.move().from(item.slot()).toOffhand();
                InvUtils.dropHand();
                sentMsg = false;
            }
        }

        // Checking if needed to eat
        if (mc.player.getOffHandStack().getItem().isFood()) {
            if (eating) {
                if (shouldEat()) {
                    startEating();
                } else {
                    stopEating();
                }
            } else if (shouldEat()) {
                startEating();
            }
        }
    }

    // Variables
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        if (mc.player.getHealth() <= healthThreshold.get() || fallDamage.get() && !EntityUtils.isAboveWater(mc.player) && mc.player.fallDistance > 3) currentItem = Item.Totem;
        else if (autoFireRes.get() && !mc.player.getActiveStatusEffects().containsKey(StatusEffects.FIRE_RESISTANCE)) currentItem = Item.EGap;
        else if (mc.player.getHungerManager().getFoodLevel() < hungerThreshold.get() + 1) currentItem = Item.EGap;
        else currentItem = Item.Totem;
    }

    @EventHandler
    public void sendPacket(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerInteractBlockC2SPacket) stopEating();
    }

    private boolean shouldEat() {
        return (mc.player.getHungerManager().getFoodLevel() <= hungerThreshold.get() || autoFireRes.get() && !mc.player.getActiveStatusEffects().containsKey(StatusEffects.FIRE_RESISTANCE)) && !Modules.get().get(NFScaffold.class).hasWorked();
    }

    private void startEating() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        mc.options.useKey.setPressed(true);
        if (mc.player.isUsingItem()) return;
        mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
        eating = true;
    }

    private void stopEating() {
        if (mc.player == null || mc.world == null) return;

        mc.options.useKey.setPressed(false);
        mc.player.stopUsingItem();
        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, mc.player.getBlockPos(), Direction.DOWN));
        eating = false;
    }

    public boolean isEating() {
        return eating;
    }

    public enum Item {
        EGap(Items.ENCHANTED_GOLDEN_APPLE),
        Totem(Items.TOTEM_OF_UNDYING);

        final net.minecraft.item.Item item;

        Item(net.minecraft.item.Item item) {
            this.item = item;
        }
    }
}
