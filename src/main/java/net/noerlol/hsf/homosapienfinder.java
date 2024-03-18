package net.noerlol.hsf;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.io.BufferedWriter;
import java.io.FileWriter;

public final class homosapienfinder extends JavaPlugin implements CommandExecutor {
    private final Map<UUID, UUID> compassTargets = new HashMap<>();

    FileConfiguration config;
    File configFile = new File(getDataFolder(), "config.yml");
    private boolean enabled = true; // Tracks HSF state
    String[] hunters = new String[300];
    int huntersCount = 0;

    private Player CurrentSelectedPlayer;

    String[] players = new String[20];
    int playersCount = 0;
    public class RandomNumberExample {
        public static void main(String[] args) {
            int min = 1;
            int max = 250;

            // Generate a random double between 0.0 and 1.0
            double randomDouble = Math.random();

            // Scale the random double to be within the desired range (min to max)
            int randomNumber = (int) Math.floor(randomDouble * (max - min + 1)) + min;

            System.out.println("Random number between " + min + " and " + max + ": " + randomNumber);
        }
    }

    private class PlayerMoveListener implements Listener {
        @EventHandler
        public void onPlayerMove(PlayerMoveEvent event) {
            Player player = event.getPlayer();
            UUID targetUUID = compassTargets.get(player.getUniqueId());
            if (targetUUID != null) {
                Player target = Bukkit.getPlayer(targetUUID);
                if (target != null && target.isOnline()) {
                    updateCompass(player, target);
                } else {
                    // Clear the compass target if the target player is no longer online
                    compassTargets.remove(player.getUniqueId());
                    player.sendMessage("Compass target player is no longer online.");
                }
            }
        }
    }

    public static boolean folderExists(String folderPath) throws IOException {
        Path folder = Paths.get(folderPath);
        return Files.exists(folder);
    }

    public static void createFolder(String path) throws IOException {
        Path folder = Paths.get(path);
        Files.createDirectories(folder);
    }

    public static void createFile(String path) throws IOException {
        Path file = Paths.get(path);
        Files.createFile(file);
    }

    private void LoadDefaultConfigIntoConfigFile() throws IOException {
        File cff = new File(getDataFolder(), "config.yml");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(cff))) {

            writer.write("#\n");
            writer.write("#\n");
            writer.write("#\n");
            writer.write("#\n");
            writer.write("#\n");
            writer.write("#    HomoSapienFinder - The only Manhunt plugin you need.\n");
            writer.write("#\n");
            writer.write("#\n");
            writer.write("#\n");
            writer.write("#\n");
            writer.write("#\n");
            writer.write("\n");
            writer.write("HuntersCageTime: 30\n");
            writer.write("# Hunters will be in a bedrock box for time length in seconds (Aka headstart to the speedrunners)\n");
            writer.write("# To make it so no headstart is given, set this value to 1\n");
            writer.write("\n");
            writer.write("compass: true");
            writer.write("\n");
            writer.write("# | DO NOT TOUCH THIS |\n");
            writer.write("ConfigVersion: A0.2\n");
            writer.write("# | DO NOT TOUCH THIS |\n");
            getLogger().info("Config written successfully, ConfigVersion A0.2");
        } catch (IOException e) {
            getLogger().warning("Oh no! I couldn't write the config due to an IOException! Anyways, here's the full error:\n\n " + e);
        }
    }
    @Override
    public void onEnable() {
        reloadConfig();
        getLogger().info("homosapienfinder plugin is enabled!");

        try {
            if (!folderExists("./plugins/HomoSapienFinder")) {
                createFolder("./plugins/HomoSapienFinder");
                createFile("./plugins/HomoSapienFinder/config.yml");
            } if (!folderExists("./plugins/HomoSapienFinder/config.yml")) { // folder exists works for files too :3
                createFile("./plugins/HomoSapienFinder/config.yml");
                LoadDefaultConfigIntoConfigFile();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        configFile = new File(getDataFolder(), "config.yml");
        config = YamlConfiguration.loadConfiguration(configFile);

        // Commands Initialization
        Objects.requireNonNull(getCommand("hsfenable")).setExecutor(this);
        Objects.requireNonNull(getCommand("hsfdisable")).setExecutor(this);
        Objects.requireNonNull(getCommand("hsf")).setExecutor(this);
        Objects.requireNonNull(getCommand("addHunter")).setExecutor(this);
        Objects.requireNonNull(getCommand("addPlayer")).setExecutor(this);
        Objects.requireNonNull(getCommand("listhunters")).setExecutor(this);
        Objects.requireNonNull(getCommand("listplayers")).setExecutor(this);
        Objects.requireNonNull(getCommand("start")).setExecutor(this);
    }
    public class TeleportTask implements Runnable {
        private final Player targetPlayer;
        private final Location startLocation;
        private final CommandSender sender;

        public TeleportTask(Player targetPlayer, Location startLocation, CommandSender sender) {
            this.targetPlayer = targetPlayer;
            this.startLocation = startLocation;
            this.sender = sender;
        }

        @Override
        public void run() {
            if (targetPlayer.isOnline()) {
                targetPlayer.teleport(startLocation);
                Bukkit.broadcastMessage("Hunters have been teleported back");
            } else {
                sender.sendMessage(targetPlayer.getName() + " is no longer online.");
            }
        }
    }
    private void updateCompass(Player player, Player target) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("Compass tracking " + target.getName());
            compass.setItemMeta(meta);
        }
        player.setCompassTarget(target.getLocation());
        player.getInventory().addItem(compass);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,@NotNull Command command,@NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return false;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("hsfenable")) {
            enabled = true;
            player.sendMessage("Plugin enabled!");
            return true;
        } else if (command.getName().equalsIgnoreCase("hsfdisable")) {
            enabled = false;
            player.sendMessage("Plugin disabled!");
            return true;
        } else if (command.getName().equalsIgnoreCase("hsf")) {
            if (enabled) {
                player.sendMessage("HSF is enabled!");
            } else {
                player.sendMessage("HSF is disabled! Enable using /hsfenable");
            }
        } else if (command.getName().equalsIgnoreCase("addHunter")) {
            if (!enabled) {
                player.sendMessage("HSF is disabled! Enable using /hsfenable");
            }
            if (args.length == 0) {
                player.sendMessage("Please provide a player to add as hunter: /addHunter ExamplePlayer");
            } else {
                String targetUsername = args[0];
                Player target = Bukkit.getPlayer(targetUsername);
                if (target != null) {
                    player.sendMessage("Added as hunter!");
                    hunters[huntersCount] = targetUsername;
                    huntersCount++;
                } else {
                    player.sendMessage("That player is not on the server!");
                }
            }
        } else if (command.getName().equalsIgnoreCase("addPlayer")) {
            if (!enabled) {
                player.sendMessage("HSF is disabled! Enable using /hsfenable");
            }
            if (args.length == 0) {
                player.sendMessage("Please provide a player to add as hunter: /addPlayer ExamplePlayer");
            } else {
                String targetUsername = args[0];
                Player target = Bukkit.getPlayer(targetUsername);
                if (target != null) {
                    player.sendMessage("Added as player");
                    players[playersCount] = targetUsername;
                    playersCount++;
                } else {
                    player.sendMessage("That player is not on the server!");
                }
            }
        } else if (command.getName().equalsIgnoreCase("listhunters")) {
            if (!enabled) {
                player.sendMessage("HSF is disabled! Enable using /hsfenable");
            }
            String msg = "Hunters: ";
            for (int i = 0; i < hunters.length; i++) {
                if (hunters[i] != null) {
                    msg += hunters[i] + " ";
                } else {
                    continue;
                }
            }
            player.sendMessage(msg);
        } else if (command.getName().equalsIgnoreCase("listplayers")) {
            if (!enabled) {
                player.sendMessage("HSF is disabled! Enable using /hsfenable");
            }
            String msg = "Players: ";
            for (int i = 0; i < players.length; i++) {
                if (players[i] != null) {
                    msg += players[i] + " ";
                } else {
                    continue;
                }
            }
            player.sendMessage(msg);
        } else if (command.getName().equalsIgnoreCase("start")) {
            if (!enabled) {
                player.sendMessage("HSF is disabled! Enable using /hsfenable");
            }

            for (int i = 0; i < huntersCount; i++) {
                if (hunters[i] == null) {
                    continue;
                }
                String hunterUsername = hunters[i];
                Player hunter = Bukkit.getPlayer(hunterUsername);
                Bukkit.broadcastMessage("Hunters will be teleported to 0, 0 in a bedrock cage!");
                World world = player.getWorld();

                int bw = 10;
                int bh = 5;

                assert hunter != null;
                Location playerLocation = hunter.getLocation();

                int centerX = 0;
                int centerY = playerLocation.getBlockY();
                int centerZ = 0;
                int startY = 256 - bh;

                for (int x = centerX - bw / 2; x <= centerX + bw / 2; x++) {
                    for (int y = startY; y < startY + bh; y++) {
                        for (int z = centerZ - bw / 2; z <= centerZ + bw / 2; z++) {
                            // Check if the position is on the edge of the box
                            if (x == centerX - bw / 2 || x == centerX + bw / 2 ||
                                    y == startY || y == startY + bh - 1 ||
                                    z == centerZ - bw / 2 || z == centerZ + bw / 2) {
                                Location blockLocation = new Location(playerLocation.getWorld(), x, y, z);
                                blockLocation.getBlock().setType(Material.BEDROCK);
                            }
                        }
                    }
                }
                Location BedrockCage = new Location(world, 0, 252, 0);
                hunter.teleport(BedrockCage);
                TeleportTask teleportTask = new TeleportTask(hunter, playerLocation, sender);
                int HuntersCageTime = config.getInt("HuntersCageTime", 30);
                Bukkit.getScheduler().scheduleSyncDelayedTask(this, teleportTask, 20L * HuntersCageTime);
                Bukkit.broadcastMessage("Teleporting hunters in " + HuntersCageTime + "seconds.");
            }
        } else if (command.getName().equalsIgnoreCase("setcompass")) {
            boolean isEnabled = config.getBoolean("compass", true);
            if (!isEnabled) {
                player.sendMessage("Compass is turned off in the config.");
                return false;
            }
            if (args.length != 1) {
                player.sendMessage("Usage: /setcompass <player>");
                return false;
            }

            Player target = getServer().getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                player.sendMessage("That player is not online!");
                return false;
            }

            // Search the "players" array for all players and check whether user provided username is in that array or not
            // Only passthrough if there:
            boolean PlayerInPlayersArray = false;
            for (int i = 0; i < playersCount; i++) {
                if (players[i] == null) {
                    continue;
                } else {
                    Player CurrentSelectedPlayer = Bukkit.getPlayer(players[i]);
                    if (CurrentSelectedPlayer != null) {
                        PlayerInPlayersArray = true;
                        i = playersCount;
                    } else {
                        if (PlayerInPlayersArray) {
                            PlayerInPlayersArray = false;
                        }
                    }
                }
            }

            if (!PlayerInPlayersArray) {
                player.sendMessage("That player is not in the players (speedrunners)! Please do /addplayer <username> to add a player as a speedrunner!");
            }

            compassTargets.put(player.getUniqueId(), target.getUniqueId()); // bukkit kys
            player.sendMessage("Compass target set to " + target.getName() + ".");

            // Update the compass immediately for the player
            updateCompass(player, target);
            return true;
        }

        return false;
    }
}
