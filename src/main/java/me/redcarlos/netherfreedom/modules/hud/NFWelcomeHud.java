package me.redcarlos.netherfreedom.modules.hud;

import me.redcarlos.netherfreedom.NFAddon;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.elements.TextHud;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.NameProtect;

import java.util.Calendar;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class NFWelcomeHud extends HudElement {
    public static final HudElementInfo<NFWelcomeHud> INFO = new HudElementInfo<>(NFAddon.Hud, "welcome-hud", "Displays a friendly welcome.", NFWelcomeHud::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("What text to show for the greeting.")
        .defaultValue(Mode.Time)
        .build()
    );

    private String leftText;
    private String rightText;
    private double leftWidth;

    public NFWelcomeHud() {
        super(INFO);
    }

    @Override
    public void tick(HudRenderer renderer) {
        int localTime = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (mode.get() == Mode.NetherFreedom) leftText = "Welcome to NF Client, ";
        else if (localTime <= 12) leftText = "Good Morning, ";
        else if (localTime <= 16) leftText = "Good Afternoon, ";
        else leftText = "Good Evening, ";

        rightText = Modules.get().get(NameProtect.class).getName(mc.getSession().getUsername());

        leftWidth = renderer.textWidth(leftText);
        double rightWidth = renderer.textWidth(rightText);

        box.setSize((leftWidth + rightWidth), renderer.textHeight());
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = this.x;
        double y = this.y;

        if (isInEditor()) {
            renderer.text("NFWelcome", x, y, TextHud.getSectionColor(0), true);
            return;
        }

        renderer.text(leftText, x, y, TextHud.getSectionColor(0), true);
        renderer.text(rightText, x + leftWidth, y, TextHud.getSectionColor(1), true);
    }

    public enum Mode {
        NetherFreedom,
        Time
    }
}
