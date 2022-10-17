package netherfreedom.modules.main;

import meteordevelopment.meteorclient.utils.misc.input.KeyBinds;
import net.minecraft.client.option.KeyBinding;
import netherfreedom.modules.NetherFreedom;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;

import org.lwjgl.glfw.GLFW;

public class RotationsPlus extends Module {
    private final SettingGroup sgYaw = settings.createGroup("Yaw");
    private final SettingGroup sgPitch = settings.createGroup("Pitch");

    // Yaw
    private final Setting<LockMode> yawLockMode = sgYaw.add(new EnumSetting.Builder<LockMode>()
        .name("yaw-lock-mode")
        .description("The way in which your yaw is locked.")
        .defaultValue(LockMode.Smart)
        .build()
    );

    private final Setting<Double> yawAngle = sgYaw.add(new DoubleSetting.Builder()
        .name("yaw-angle")
        .description("Yaw angle in degrees.")
        .defaultValue(0)
        .sliderMax(360)
        .max(360)
        .build()
    );

    // Pitch
    private final Setting<LockMode> pitchLockMode = sgPitch.add(new EnumSetting.Builder<LockMode>()
        .name("pitch-lock-mode")
        .description("The way in which your pitch is locked.")
        .defaultValue(LockMode.None)
        .build()
    );

    private final Setting<Double> pitchAngle = sgPitch.add(new DoubleSetting.Builder()
        .name("pitch-angle")
        .description("Pitch angle in degrees.")
        .defaultValue(0)
        .range(-90, 90)
        .sliderRange(-90, 90)
        .build()
    );

    private final Setting<Boolean> arrowSnap = sgYaw.add(new BoolSetting.Builder()
        .name("arrows-snap")
        .description("Snap your player at 45° steps with the arrow keys.")
        .build()
    );

    public RotationsPlus() {
        super(NetherFreedom.MAIN, "rotations+", "Changes/locks your yaw & pitch.");
    }

    private boolean lDown = false;
    private boolean rDown = false;

    @Override
    public void onActivate() {
        onTick(null);
        lDown = false;
        rDown = false;
    }

    @Override
    public void onDeactivate() {
        onTick(null);
        lDown = false;
        rDown = false;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        switch (yawLockMode.get()) {
            case Simple -> setYawAngle(yawAngle.get().floatValue());
            case Smart  -> setYawAngle(getSmartYawDirection());
        }

        switch (pitchLockMode.get()) {
            case Simple -> mc.player.setPitch(pitchAngle.get().floatValue());
            case Smart  -> mc.player.setPitch(getSmartPitchDirection());
        }

        if (arrowSnap.get()) {
            if (!Input.isKeyPressed(GLFW.GLFW_KEY_LEFT)) lDown = false;

            if (Input.isKeyPressed(GLFW.GLFW_KEY_LEFT) && !lDown) {
                mc.player.setYaw(mc.player.getYaw() - 45);
                lDown = true;
                return;
            }

            if (!Input.isKeyPressed(GLFW.GLFW_KEY_RIGHT)) rDown = false;

            if (Input.isKeyPressed(GLFW.GLFW_KEY_RIGHT) && !rDown) {
                mc.player.setYaw(mc.player.getYaw() + 45);
                rDown = true;
                return;
            }
        }
    }

    private float getSmartYawDirection() {
        return Math.round((mc.player.getYaw() + 1f) / 45f) * 45f;
    }

    private float getSmartPitchDirection() {
        return Math.round((mc.player.getPitch() + 1f) / 30f) * 30f;
    }

    private void setYawAngle(float yawAngle) {
        mc.player.setYaw(yawAngle);
        mc.player.headYaw = yawAngle;
        mc.player.bodyYaw = yawAngle;
    }

    public enum LockMode {
        Smart,
        Simple,
        None
    }
}
