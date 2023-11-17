package redcarlos.netherfreedomaddon.modules.main;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.item.ItemStack;
import redcarlos.netherfreedomaddon.NFAddon;
import redcarlos.netherfreedomaddon.utils.NFUtils;

public class ArmorNotify extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> threshold = sgGeneral.add(new DoubleSetting.Builder()
        .name("durability")
        .description("How low an armor piece needs to be to alert you (in %).")
        .defaultValue(20)
        .range(1, 100)
        .sliderRange(1, 100)
        .build()
    );

    public ArmorNotify() {
        super(NFAddon.Main, "armor-notify", "Notifies you when your armor pieces are low.");
    }

    private boolean alertedHelmet;
    private boolean alertedChestplate;
    private boolean alertedLeggings;
    private boolean alertedBoots;

    @Override
    public void onActivate() {
        alertedHelmet = false;
        alertedChestplate = false;
        alertedLeggings = false;
        alertedBoots = false;
    }

    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        Iterable<ItemStack> armorPieces = mc.player.getArmorItems();
        for (ItemStack armorPiece : armorPieces) {

            if (NFUtils.checkNotifyThreshold(armorPiece, threshold.get())) {
                if (NFUtils.isHelmetArmor(armorPiece) && !alertedHelmet) {
                    warning("Your helmet durability is low.");
                    alertedHelmet = true;
                } else if (NFUtils.isChestplateArmor(armorPiece) && !alertedChestplate) {
                    warning("Your chestplate durability is low.");
                    alertedChestplate = true;
                } else if (NFUtils.isLeggingsArmor(armorPiece) && !alertedLeggings) {
                    warning("Your leggings durability is low.");
                    alertedLeggings = true;
                } else if (NFUtils.isBootsArmor(armorPiece) && !alertedBoots) {
                    warning("Your boots durability is low.");
                    alertedBoots = true;
                }
            } else if (!NFUtils.checkNotifyThreshold(armorPiece, threshold.get())) {
                if (NFUtils.isHelmetArmor(armorPiece) && alertedHelmet) alertedHelmet = false;
                else if (NFUtils.isChestplateArmor(armorPiece) && alertedChestplate) alertedChestplate = false;
                else if (NFUtils.isLeggingsArmor(armorPiece) && alertedLeggings) alertedLeggings = false;
                else if (NFUtils.isBootsArmor(armorPiece) && alertedBoots) alertedBoots = false;
            }
        }
    }
}
