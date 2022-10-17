package netherfreedom.modules.main;

import net.minecraft.text.MutableText;
import netherfreedom.modules.NetherFreedom;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

public class ChatTweaks extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> prefix = sgGeneral.add(new StringSetting.Builder()
            .name("prefix")
            .description("What to be displayed as HIG Tools Prefix")
            .defaultValue("NF Client")
            .build()
    );

    private final Setting<SettingColor> prefixColors = sgGeneral.add(new ColorSetting.Builder()
            .name("prefix-color")
            .description("Color display for the prefix")
            .defaultValue(new SettingColor(145, 61, 226, 255))
            .build()
    );

    public ChatTweaks() {
        super(NetherFreedom.MAIN, "chat-tweaks", "Various chat tweaks.");
    }


    @Override
    public void onActivate() {
        ChatUtils.registerCustomPrefix("netherfreedom.modules", this::getPrefix);
    }

    public Text getPrefix() {
        MutableText logo = Text.literal(prefix.get());
        MutableText prefix = Text.literal("");
        logo.setStyle(logo.getStyle().withColor(TextColor.fromRgb(prefixColors.get().getPacked())));
        prefix.setStyle(prefix.getStyle().withFormatting(Formatting.GRAY));
        prefix.append("[");
        prefix.append(logo);
        prefix.append("] ");
        return prefix;
    }
}
