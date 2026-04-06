package com.gmail.picono435.randomtp.commands;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import com.gmail.picono435.randomtp.api.RandomTPAPI;
import com.gmail.picono435.randomtp.config.Config;
import com.gmail.picono435.randomtp.config.Messages;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class RTPDCommand {
    
    private static Map<String, Long> cooldowns = new HashMap<String, Long>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rtpd")
                .requires(source -> RandomTPAPI.hasPermission(source, "randomtp.command.interdim"))
                .then(Commands.argument("dimension", DimensionArgument.dimension())
                        .executes(context -> 
                            runCommand(context.getSource().getPlayerOrException(), DimensionArgument.getDimension(context, "dimension"))
                        )
                ));
        
        dispatcher.register(Commands.literal("dimensionrtp")
                .requires(source -> RandomTPAPI.hasPermission(source, "randomtp.command.interdim"))
                .then(Commands.argument("dimension", DimensionArgument.dimension())
                        .executes(context ->
                            runCommand(context.getSource().getPlayerOrException(), DimensionArgument.getDimension(context, "dimension"))
                        )
                ));
    }
    
    private static int runCommand(ServerPlayer p, ServerLevel dim) {
        try {
            if(!RandomTPAPI.checkCooldown(p, cooldowns) && !RandomTPAPI.hasPermission(p, "randomtp.cooldown.exempt")) {
                long secondsLeft = RandomTPAPI.getCooldownLeft(p, cooldowns);
                Component cooldownmes = Component.literal(Messages.getCooldown()
                        .replaceAll("\\{secondsLeft\\}", Long.toString(secondsLeft))
                        .replaceAll("\\{playerName\\}", p.getName().getString())
                        .replaceAll("&", "§"));
                p.sendSystemMessage(cooldownmes, false);
                return 1;
            }
			
            String dimensionId = dim.dimension().location().toString();
            if(!inWhitelist(dimensionId)) {
                p.sendSystemMessage(Component.literal(Messages.getDimensionNotAllowed()
                        .replaceAll("\\{playerName\\}", p.getName().getString())
                        .replaceAll("\\{dimensionId\\}", dimensionId)
                        .replace('&', '§')), false);
                return 1;
            }

            cooldowns.remove(p.getName().getString());

            if(Config.useOriginal()) {
                Component finding = Component.literal(Messages.getFinding()
                        .replaceAll("\\{playerName\\}", p.getName().getString())
                        .replaceAll("&", "§"));
                p.sendSystemMessage(finding, false);
                
                RandomTPAPI.randomTeleport(p, dim);
                cooldowns.put(p.getName().getString(), System.currentTimeMillis());
                return 1;
            }

            p.handleInsidePortal(p.blockPosition());
            p.changeDimension(dim);

            double borderSize = dim.getWorldBorder().getSize() / 2;
            BigDecimal num = new BigDecimal(borderSize);
            String maxDistance = (Config.getMaxDistance() == 0) ? num.toPlainString() : String.valueOf(Config.getMaxDistance());

            CommandSourceStack silentSource = p.getServer().createCommandSourceStack()
                    .withLevel(dim)
                    .withPermission(4)
                    .withSuppressedOutput();

            String command = String.format("spreadplayers %f %f %d %s false %s", 
                    dim.getWorldBorder().getCenterX(), 
                    dim.getWorldBorder().getCenterZ(), 
                    Config.getMinDistance(), 
                    maxDistance, 
                    p.getScoreboardName());

            p.getServer().getCommands().performCommand(silentSource, command);

            Component successful = Component.literal(Messages.getSuccessful()
                    .replaceAll("\\{playerName\\}", p.getName().getString())
                    .replaceAll("\\{blockX\\}", "" + (int)p.position().x)
                    .replaceAll("\\{blockY\\}", "" + (int)p.position().y)
                    .replaceAll("\\{blockZ\\}", "" + (int)p.position().z)
                    .replaceAll("&", "§"));
            p.sendSystemMessage(successful, false);

            cooldowns.put(p.getName().getString(), System.currentTimeMillis());
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return 1;
    }
    
    private static boolean inWhitelist(String dimension) {
        if(Config.useWhitelist()) {
            return Config.getAllowedDimensions().contains(dimension);
        } else {
            return !Config.getAllowedDimensions().contains(dimension);
        }
    }
}
