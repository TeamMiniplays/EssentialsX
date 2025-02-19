package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.MetaItemStack;
import com.earth2me.essentials.User;
import com.earth2me.essentials.craftbukkit.Inventories;
import com.earth2me.essentials.utils.NumberUtil;
import com.earth2me.essentials.utils.VersionUtil;
import com.google.common.collect.Lists;
import net.ess3.api.TranslatableException;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Commandgive extends EssentialsLoopCommand {
    public Commandgive() {
        super("give");
    }

    @Override
    public void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 2) {
            throw new NotEnoughArgumentsException();
        }

        ItemStack stack = ess.getItemDb().get(args[1]);
        final String itemname = stack.getType().toString().toLowerCase(Locale.ENGLISH).replace("_", "");

        if (sender.isPlayer() && !ess.getUser(sender.getPlayer()).canSpawnItem(stack.getType())) {
            throw new TranslatableException("cantSpawnItem", itemname);
        }

        try {
            if (args.length > 3 && VersionUtil.PRE_FLATTENING && NumberUtil.isInt(args[2]) && NumberUtil.isInt(args[3])) {
                stack.setAmount(Integer.parseInt(args[2]));
                stack.setDurability(Short.parseShort(args[3]));
            } else if (args.length > 2 && Integer.parseInt(args[2]) > 0) {
                stack.setAmount(Integer.parseInt(args[2]));
            } else if (ess.getSettings().getDefaultStackSize() > 0) {
                stack.setAmount(ess.getSettings().getDefaultStackSize());
            } else if (ess.getSettings().getOversizedStackSize() > 0 && sender.isAuthorized("essentials.oversizedstacks")) {
                stack.setAmount(ess.getSettings().getOversizedStackSize());
            }
        } catch (final NumberFormatException e) {
            throw new NotEnoughArgumentsException();
        }

        final MetaItemStack metaStack = new MetaItemStack(stack);
        if (!metaStack.canSpawn(ess)) {
            throw new TranslatableException("unableToSpawnItem", itemname);
        }

        if (args.length > 3) {
            boolean allowUnsafe = ess.getSettings().allowUnsafeEnchantments();
            if (allowUnsafe && sender.isPlayer() && !ess.getUser(sender.getPlayer()).isAuthorized("essentials.enchantments.allowunsafe")) {
                allowUnsafe = false;
            }

            final int metaStart = NumberUtil.isInt(args[3]) ? 4 : 3;

            if (args.length > metaStart) {
                metaStack.parseStringMeta(sender, allowUnsafe, args, metaStart, ess);
            }

            stack = metaStack.getItemStack();
        }

        if (stack.getType() == Material.AIR) {
            throw new TranslatableException("cantSpawnItem", "Air");
        }

        final String itemName = stack.getType().toString().toLowerCase(Locale.ENGLISH).replace('_', ' ');
        final boolean isDropItemsIfFull = ess.getSettings().isDropItemsIfFull();
        final ItemStack finalStack = stack;
        loopOnlinePlayersConsumer(server, sender, false, true, args[0], player -> {
            sender.sendTl("giveSpawn", finalStack.getAmount(), itemName, player.getName());

            final Map<Integer, ItemStack> leftovers = Inventories.addItem(player.getBase(), player.isAuthorized("essentials.oversizedstacks") ? ess.getSettings().getOversizedStackSize() : 0, finalStack);

            for (final ItemStack item : leftovers.values()) {
                if (isDropItemsIfFull) {
                    final World w = player.getWorld();
                    w.dropItemNaturally(player.getLocation(), item);
                } else {
                    sender.sendTl("giveSpawnFailure", item.getAmount(), itemName, player.getName());
                }
            }

            player.getBase().updateInventory();
        });
    }

    @Override
    protected List<String> getTabCompleteOptions(final Server server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return getPlayers(server, sender);
        } else if (args.length == 2) {
            return getItems();
        } else if (args.length == 3) {
            return Lists.newArrayList("1", "64"); // TODO: get actual max size
        } else if (args.length == 4) {
            return Lists.newArrayList("0");
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    protected void updatePlayer(final Server server, final CommandSource sender, final User user, final String[] args) {

    }
}
