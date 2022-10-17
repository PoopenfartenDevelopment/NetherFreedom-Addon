package netherfreedom.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import netherfreedom.modules.kmain.*;
import netherfreedom.modules.main.*;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoLog;
import meteordevelopment.meteorclient.systems.modules.movement.SafeWalk;
import meteordevelopment.meteorclient.systems.modules.player.Rotation;
import meteordevelopment.meteorclient.systems.modules.render.FreeLook;
import meteordevelopment.meteorclient.systems.modules.world.LiquidFiller;
import net.minecraft.command.CommandSource;

import java.util.ArrayList;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class ToggleModules extends Command {
    public ToggleModules() {
        super("toggle-modules", "Disables all modules.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("netherfreedom").executes(ctx -> {
            Modules modules = Modules.get();

            if (modules.get(AutoEatPlus.class).isActive())
                modules.get(AutoEatPlus.class).toggle();
            if (modules.get(AutoLog.class).isActive())
                modules.get(AutoLog.class).toggle();
            if (modules.get(FreeLook.class).isActive())
                modules.get(FreeLook.class).toggle();
            if (modules.get(HandManager.class).isActive())
                modules.get(HandManager.class).toggle();
            if (modules.get(InvManager.class).isActive())
                modules.get(InvManager.class).toggle();
            if (modules.get(LiquidFiller.class).isActive())
                modules.get(LiquidFiller.class).toggle();
            if (modules.get(NFNuker.class).isActive())
                modules.get(NFNuker.class).toggle();
            if (modules.get(Rotation.class).isActive())
                modules.get(Rotation.class).toggle();
            if (modules.get(SafeWalk.class).isActive())
                modules.get(SafeWalk.class).toggle();
            if (modules.get(ScaffoldPlus.class).isActive())
                modules.get(ScaffoldPlus.class).toggle();

            return SINGLE_SUCCESS;
        }));

        builder.then(literal("all").executes(ctx -> {
            new ArrayList<>(Modules.get().getActive()).forEach(Module::toggle);

            return SINGLE_SUCCESS;
        }));
    }
}
