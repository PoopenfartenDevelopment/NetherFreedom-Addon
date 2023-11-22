/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 * Enhanced by RedCarlos26
 */

package me.redcarlos.netherfreedom.modules.main;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import org.lwjgl.glfw.GLFW;
import me.redcarlos.netherfreedom.NFAddon;

public class NFRotation extends Module {
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
        .visible(() -> yawLockMode.get() == LockMode.Simple)
        .build()
    );

    private final Setting<Boolean> arrowSnap = sgYaw.add(new BoolSetting.Builder()
        .name("arrows-snap")
        .description("Snap your player at 45° steps with the arrow keys.")
        .visible(() -> yawLockMode.get() != LockMode.None)
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
        .visible(() -> pitchLockMode.get() == LockMode.Simple)
        .build()
    );

    private boolean lDown, rDown;

    public NFRotation() {
        super(NFAddon.Main, "NF-rotation", "Changes/locks your yaw and pitch.");
    }

    @Override
    public void onActivate() {
        lDown = false;
        rDown = false;
    }

    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (event.screen instanceof DisconnectedScreen) {
            toggle();
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        toggle();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        switch (yawLockMode.get()) {
            case Simple -> setYawAngle(yawAngle.get().floatValue());
            case Smart  -> setYawAngle(Math.round((mc.player.getYaw() + 1f) / 45f) * 45f);
        }

        switch (pitchLockMode.get()) {
            case Simple -> mc.player.setPitch(pitchAngle.get().floatValue());
            case Smart  -> mc.player.setPitch(Math.round(((mc.player.getPitch() + 1f) / 30f) * 30f));
        }

        if (arrowSnap.get() && yawLockMode.get() != LockMode.None) {
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
            }
        }
    }

    private void setYawAngle(float yawAngle) {
        if (mc.player == null || mc.world == null) return;

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
