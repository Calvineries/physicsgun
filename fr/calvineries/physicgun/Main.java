package fr.calvineries.physicgun;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main extends JavaPlugin implements Listener {

    private Map<Player, Player> targets = new HashMap<>();
    private FileConfiguration config;
    private String caught_type_message;
    private String caught_message;
    private String release_type_message;
    private String release_message;
    private List<Player> messageSent = new ArrayList<Player>();;


    @Override
    public void onEnable() {
    	messageSent.clear();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("physicsgun").setExecutor(this);
        getCommand("physgun").setExecutor(this);
        getCommand("physicsgunreload").setExecutor(this);
        getCommand("physgunreload").setExecutor(this);

        saveDefaultConfig();

        config = getConfig();
        caught_type_message = config.getString("caught_type_message");
        caught_message = config.getString("caught_message"); 
        release_type_message = config.getString("release_type_message"); 
        release_message = config.getString("release_message"); 
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("physicsgun") || command.getName().equalsIgnoreCase("physgun")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("You are not a player.");
                return true;
            }

            Player player = (Player) sender;
            if (!player.isOp() && !player.hasPermission("physicsgun.give")) {
                player.sendMessage("You don't have permission to use this command.");
                return true;
            }
            ItemStack physicsGun = new ItemStack(Material.STICK);
            ItemMeta meta = physicsGun.getItemMeta();
            meta.setDisplayName("§bPhysics Gun");
            meta.setCustomModelData(1412);
            physicsGun.setItemMeta(meta);
            player.getInventory().addItem(physicsGun);

            sender.sendMessage("You get a §bPhysics Gun§r!");
            return true;
            
        } else if (command.getName().equalsIgnoreCase("physgunreload") || command.getName().equalsIgnoreCase("physicsgunreload")) { 
            if (!sender.isOp()) {
                sender.sendMessage("You don't have permission to use this command.");
                return true;
            }

            reloadConfig(); 

            config = getConfig();
            caught_type_message = config.getString("caught_type_message");
            caught_message = config.getString("caught_message");
            release_type_message = config.getString("release_type_message");
            release_message = config.getString("release_message");

            sender.sendMessage("Config reloaded!");
            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player)) {
            return;
        }

        Player player = event.getPlayer();
        Player target = (Player) event.getRightClicked();

        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand == null || itemInHand.getItemMeta() == null || !itemInHand.getItemMeta().hasDisplayName() || !itemInHand.getItemMeta().getDisplayName().equals("§bPhysics Gun") || itemInHand.getItemMeta().getCustomModelData() != 1412) {
            return;
        }
        if (!player.isOp() && !player.hasPermission("physicsgun.use")) {
            return;
        }
        if (target.getGameMode() == GameMode.CREATIVE) {
            player.sendMessage("§cThis player is in creative mode, he cannot be taken.");
            return;
        }
        
        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0));
        
        if (targets.containsValue(target)) {
            return;
        }

        targets.put(player, target);
        Location targetLocation = target.getLocation();
        Location playerLocation = player.getLocation();
        int distance = (int) playerLocation.distance(targetLocation);
        if (caught_type_message.equals("chat")) {
            if (!messageSent.contains(target)) {
                target.sendMessage(caught_message.replace("%player%", player.getName()));
                messageSent.add(target);
            }
        }
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!targets.containsKey(player) || targets.get(player) != target) {
                    cancel();
                    return;
                }

                Location targetLocation = target.getLocation();
                Location playerLocation = player.getLocation();
                Location teleportLocation = playerLocation.clone().add(playerLocation.getDirection().normalize().multiply(distance));

                targetLocation.setX(teleportLocation.getX());
                targetLocation.setY(teleportLocation.getY()+1);
                targetLocation.setZ(teleportLocation.getZ());
                target.setHealth(20);
                target.teleport(targetLocation);
                if (caught_type_message.equals("title")) {
                   target.sendTitle(caught_message.replace("%player%", player.getName()), "",  0, 5, 0);
                }
                if (caught_type_message.equals("subtitle")) {
                    target.sendTitle("", caught_message.replace("%player%", player.getName()), 0, 5, 0);
                }
            }
        }.runTaskTimer(this, 0L, 1L);
    }
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player player = (Player) event.getDamager();
            Player target = (Player) event.getEntity();
            messageSent.remove(target);
            ItemStack itemInHand = player.getInventory().getItemInMainHand();

            if (itemInHand == null || itemInHand.getItemMeta() == null || !itemInHand.getItemMeta().hasDisplayName() || !itemInHand.getItemMeta().getDisplayName().equals("§bPhysics Gun") || itemInHand.getItemMeta().getCustomModelData() != 1412) {
                return;
            }
            if (!player.isOp() && !player.hasPermission("physicsgun.use")) {
                return;
            }

            target.removePotionEffect(PotionEffectType.GLOWING);
            if (release_type_message.equals("chat")) {
                target.sendMessage(release_message.replace("%player%", player.getName()));
             }
            if (release_type_message.equals("title")) {
                target.sendTitle(release_message.replace("%player%", player.getName()), "",  0, 30, 0);
             }
             if (release_type_message.equals("subtitle")) {
                 target.sendTitle("", release_message.replace("%player%", player.getName()), 0, 30, 0);
             }
            targets.remove(player);
            event.setCancelled(true);
        }
    }
}
