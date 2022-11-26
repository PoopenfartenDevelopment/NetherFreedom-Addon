package netherfreedom.utils;

import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.starscript.compiler.Compiler;
import meteordevelopment.starscript.compiler.Parser;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class NFUtils {

    // Armor Notify
    public static boolean checkThreshold(ItemStack i, double threshold) {
        return getDamage(i) <= threshold;
    }

    public static double getDamage(ItemStack i) {
        return (((double) (i.getMaxDamage() - i.getDamage()) / i.getMaxDamage()) * 100);
    }

    public static boolean isHelm(ItemStack itemStack) {
        if (itemStack == null) return false;
        Item i = itemStack.getItem();
        return i == Items.NETHERITE_HELMET || i == Items.DIAMOND_HELMET || i == Items.GOLDEN_HELMET || i == Items.IRON_HELMET || i == Items.CHAINMAIL_HELMET || i == Items.LEATHER_HELMET;
    }

    public static boolean isChest(ItemStack itemStack) {
        if (itemStack == null) return false;
        Item i = itemStack.getItem();
        return i == Items.NETHERITE_CHESTPLATE || i == Items.DIAMOND_CHESTPLATE || i == Items.GOLDEN_CHESTPLATE || i == Items.IRON_CHESTPLATE || i == Items.CHAINMAIL_CHESTPLATE || i == Items.LEATHER_CHESTPLATE;
    }

    public static boolean isLegs(ItemStack itemStack) {
        if (itemStack == null) return false;
        Item i = itemStack.getItem();
        return i == Items.NETHERITE_LEGGINGS || i == Items.DIAMOND_LEGGINGS || i == Items.GOLDEN_LEGGINGS || i == Items.IRON_LEGGINGS || i == Items.CHAINMAIL_LEGGINGS || i == Items.LEATHER_LEGGINGS;
    }

    public static boolean isBoots(ItemStack itemStack) {
        if (itemStack == null) return false;
        Item i = itemStack.getItem();
        return i == Items.NETHERITE_BOOTS || i == Items.DIAMOND_BOOTS || i == Items.GOLDEN_BOOTS || i == Items.IRON_BOOTS || i == Items.CHAINMAIL_BOOTS || i == Items.LEATHER_BOOTS;
    }

    public static int haveItem(Item item) {
        int count = 0;
        for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                count++;
            }
        }
        return count;
    }

    //honestly this is kinda embarrassing
    public static ArrayList<Item> shulkers = new ArrayList<>(){{
        add(Items.SHULKER_BOX);
        add(Items.WHITE_SHULKER_BOX);
        add(Items.ORANGE_SHULKER_BOX);
        add(Items.MAGENTA_SHULKER_BOX);
        add(Items.LIGHT_BLUE_SHULKER_BOX);
        add(Items.YELLOW_SHULKER_BOX);
        add(Items.LIME_SHULKER_BOX);
        add(Items.PINK_SHULKER_BOX);
        add(Items.GRAY_SHULKER_BOX);
        add(Items.LIGHT_GRAY_SHULKER_BOX);
        add(Items.CYAN_SHULKER_BOX);
        add(Items.PURPLE_SHULKER_BOX);
        add(Items.BLUE_SHULKER_BOX);
        add(Items.BROWN_SHULKER_BOX);
        add(Items.GREEN_SHULKER_BOX);
        add(Items.RED_SHULKER_BOX);
        add(Items.BLACK_SHULKER_BOX);
    }};

    public static int getNetherrack(){
        //couldn't be bothered making getting stats myself
        Parser.Result result = Parser.parse("{player.get_stat(\"netherrack\",\"mined\")}");
        return Integer.parseInt(String.valueOf(MeteorStarscript.ss.run(Compiler.compile(result))));
    }

    public static int getPickaxesBroken(){
        Parser.Result result = Parser.parse("{player.get_stat(\"netherite_pickaxe\",\"broken\")}");
        return Integer.parseInt(String.valueOf(MeteorStarscript.ss.run(Compiler.compile(result))));
    }
}
