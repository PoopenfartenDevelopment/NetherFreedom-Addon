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

public class WelcomeHudNF extends HudElement {
    public static final HudElementInfo<WelcomeHudNF> INFO = new HudElementInfo<>(NFAddon.Hud, "welcome-hud-NF", "Displays a friendly welcome.", WelcomeHudNF::new);

    private String leftText;
    private String rightText;
    private double leftWidth;

    public WelcomeHudNF() {
        super(INFO);
    }

    @Override
    public void tick(HudRenderer renderer) {
        leftText = "Welcome to NetherFreedom, ";
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
            renderer.text("WelcomeHUD-NF", x, y, TextHud.getSectionColor(0), true);
            return;
        }

        renderer.text(leftText, x, y, TextHud.getSectionColor(0), true);
        renderer.text(rightText, x + leftWidth, y, TextHud.getSectionColor(1), true);
    }
}
