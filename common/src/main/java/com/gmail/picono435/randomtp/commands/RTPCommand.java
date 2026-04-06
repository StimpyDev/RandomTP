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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class RTPCommand {
	
	private static Map<String, Long> cooldowns = new HashMap<String, Long>();
	
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("rtp").requires(source -> RandomTPAPI.hasPermission(source, "randomtp.command.basic"))
				.executes(context -> runCommand(context.getSource().getPlayerOrException())
				));
		dispatcher.register(Commands.literal("randomtp").requires(source -> RandomTPAPI.hasPermission(source, "randomtp.command.basic"))
				.executes(context -> runCommand(context.getSource().getPlayerOrException())
				));
	}
	
	private static int runCommand(ServerPlayer p) {
    try {
        if(!RandomTPAPI.checkCooldown(p, cooldowns) && !RandomTPAPI.hasPermission(p, "randomtp.cooldown.exempt")) {
            long secondsLeft = RandomTPAPI.getCooldownLeft(p, cooldowns);
            Component cooldownmes = Component.literal(Messages.getCooldown().replaceAll("\\{secondsLeft\\}", Long.toString(secondsLeft)).replaceAll("\\{playerName\\}", p.getName().getString()).replaceAll("&", "§"));
            p.sendSystemMessage(cooldownmes, false);
            return 1;
        } else {
            cooldowns.remove(p.getName().getString());
            if(Config.useOriginal()) {
                Component finding = Component.literal(Messages.getFinding().replaceAll("\\{playerName\\}", p.getName().getString()).replaceAll("\\{blockX\\}", "" + (int)p.position().x).replaceAll("\\{blockY\\}", "" + (int)p.position().y).replaceAll("\\{blockZ\\}", "" + (int)p.position().z).replaceAll("&", "§"));
                p.sendSystemMessage(finding, false);
                if(!Config.getDefaultWorld().equals("playerworld")) {
                    RandomTPAPI.randomTeleport(p, RandomTPAPI.getWorld(Config.getDefaultWorld(), p.getServer()));
                } else {
                    RandomTPAPI.randomTeleport(p, p.serverLevel());
                }
                cooldowns.put(p.getName().getString(), System.currentTimeMillis());
                return 1;
            }

            double cal = p.serverLevel().getWorldBorder().getSize()/2;
            BigDecimal num = new BigDecimal(cal);
            String maxDistance = num.toPlainString();
            
            CommandSourceStack silentSource = p.getServer().createCommandSourceStack()
                .withPermission(4)
                .withSuppressedOutput();

            String distance = (Config.getMaxDistance() == 0) ? maxDistance : String.valueOf(Config.getMaxDistance());
            String command = "spreadplayers " + p.serverLevel().getWorldBorder().getCenterX() + " " + p.serverLevel().getWorldBorder().getCenterZ() + " " + Config.getMinDistance() + " " + distance + " false " + p.getScoreboardName();

            p.getServer().getCommands().performCommand(silentSource, command);

            Component successful = Component.literal(Messages.getSuccessful().replaceAll("\\{playerName\\}", p.getName().getString()).replaceAll("\\{blockX\\}", "" + (int)p.position().x).replaceAll("\\{blockY\\}", "" + (int)p.position().y).replaceAll("\\{blockZ\\}", "" + (int)p.position().z).replaceAll("&", "§"));
            p.sendSystemMessage(successful, false);
            
            cooldowns.put(p.getName().getString(), System.currentTimeMillis());
        }
    } catch (Exception ex) {
        ex.printStackTrace();
    }
    return 1;
}
