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

    private boolean sentMessage;
    private Item currentItem;

    public HandManager() {
        super(NetherFreedom.MAIN, "hand-manager", "Allows you to hold specified items in your offhand.");
    }

    @Override
    public void onActivate() {
        sentMessage = false;
        currentItem = item.get();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        currentItem = item.get();

        if (mc.player.getOffHandStack().getItem() != currentItem.item) {
            FindItemResult item = InvUtils.find(itemStack -> itemStack.getItem() == currentItem.item, hotbar.get() ? 0 : 9, 35);

            if (!item.found()) {
                if (!sentMessage) {
                    warning("Chosen item not found.");
                    sentMessage = true;
                }
            }
        }
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
