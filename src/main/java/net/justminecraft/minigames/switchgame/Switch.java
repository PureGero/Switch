package net.justminecraft.minigames.switchgame;

import com.sk89q.jnbt.*;
import net.justminecraft.minigames.minigamecore.Game;
import net.justminecraft.minigames.minigamecore.MG;
import net.justminecraft.minigames.minigamecore.Minigame;
import net.justminecraft.minigames.minigamecore.worldbuffer.Chunk;
import net.justminecraft.minigames.minigamecore.worldbuffer.Section;
import net.justminecraft.minigames.minigamecore.worldbuffer.WorldBuffer;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class Switch extends Minigame implements Listener {

    public static float HUNGER_LOSS_PER_SECOND = 0.15f;

    File chunksDir;

    /**
     * Get child tag of a NBT structure.
     *
     * @param items    The parent tag map
     * @param key      The name of the tag to get
     * @param expected The expected type of the tag
     * @return child tag casted to the expected type
     * @throws IOException if the tag does not exist or the tag is not of the expected type
     */
    private static <T extends Tag> T getChildTag(Map<String, Tag> items, String key, Class<T> expected) throws IOException {
        if (!items.containsKey(key)) {
            throw new IOException("Schematic file is missing a \"" + key + "\" tag");
        }
        Tag tag = items.get(key);
        if (!expected.isInstance(tag)) {
            throw new IOException(key + " tag is not of tag type " + expected.getName());
        }
        return expected.cast(tag);
    }

    public void onEnable() {
        chunksDir = new File(this.getDataFolder(), "chunks");
        if (!chunksDir.isDirectory())
            chunksDir.mkdirs();
        MG.core().registerMinigame(this);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Switch enabled");
    }

    public void onDisable() {
        getLogger().info("Switch disabled");
    }

    @Override
    public int getMaxPlayers() {
        return 9;
    }

    @Override
    public int getMinPlayers() {
        return 2;
    }

    @Override
    public String getMinigameName() {
        return "Switch";
    }

    private ArrayList<File> randomSchems() {
        File[] d = chunksDir.listFiles();
        ArrayList<File> schems = new ArrayList<>();

        for (File f : d) {
            if (f.getName().endsWith(".schematic")) {
                schems.add(f);
            }
        }

        if (schems.size() < 9)
            throw new RuntimeException("Not enough chunk schematic files in " + chunksDir);

        Collections.shuffle(schems);

        return schems;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        if (!e.getTo().getChunk().equals(e.getFrom().getChunk()) && (e.getTo().getChunk().getX() % 2) != 0 || (e.getTo().getChunk().getZ() % 2) != 0) {
            Game g = MG.core().getGame(e.getPlayer());
            if (g != null && g.minigame == this) {
                e.setCancelled(true);
                e.getPlayer().teleport(e.getFrom());
                //e.getPlayer().setVelocity(e.getFrom().subtract(e.getTo()).toVector().normalize().multiply(1));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDeath(PlayerDeathEvent e) {
        Game g = MG.core().getGame(e.getEntity());
        if (g != null && g.minigame == this)
            g.broadcastRaw(e.getDeathMessage());
    }

    @EventHandler
    public void onLiquidFlow(BlockFromToEvent e) {
        if (e.getBlock().getWorld() == MG.core().world)
            if ((e.getBlock().getType() == Material.WATER || e.getBlock().getType() == Material.STATIONARY_WATER ||
                    e.getBlock().getType() == Material.LAVA || e.getBlock().getType() == Material.STATIONARY_LAVA)
                    && !e.getToBlock().getChunk().equals(e.getBlock().getChunk())) {
                for (Game g : MG.core().getGames(this)) {
                    if (g.world.equals(e.getBlock().getWorld()))
                        e.setCancelled(true);
                }
            }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Game g = MG.core().getGame(e.getPlayer());
        if (g != null && g.minigame == this) {
            if (!e.getBlock().getChunk().equals(e.getBlockAgainst().getChunk()))
                e.setCancelled(false);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Game g = MG.core().getGame(e.getPlayer());
        if (g != null && g.minigame == this) {
            if (e.getItem() != null && e.getAction() != Action.PHYSICAL && e.getItem().getType() == Material.LEVER
                    && e.getItem().getItemMeta().getDisplayName().equals("Force a Switch")) {
                if (e.getItem().getAmount() > 1)
                    e.getItem().setAmount(e.getItem().getAmount() - 1);
                else {
                    e.getItem().setType(Material.STICK);
                    ItemMeta m = e.getItem().getItemMeta();
                    m.setDisplayName(null);
                    e.getItem().setItemMeta(m);
                }
                e.setCancelled(true);
                ((SwitchGame) g).switchNow();
            }
        }
    }

    public ItemStack chestItem() {
        int i = (int) (Math.random() * 9);
        switch (i) {
            case 0:
                if (Math.random() < 0.4) // Halve chance of lava bucket, increase chance of iron ingot
                    return new ItemStack(Material.LAVA_BUCKET, 1);
            case 1:
                return new ItemStack(Material.IRON_INGOT, 1);
            case 2:
                return new ItemStack(Material.DIAMOND, 1);
            case 3:
                return new ItemStack(Material.COOKED_BEEF, (int) (Math.random() * 2) + 1);
            case 4:
                return new ItemStack(Material.WOOD, (int) (Math.random() * 4) + 1, (short) (Math.random() * 6));
            case 5:
                return new ItemStack(Material.POTATO_ITEM, 1);
            case 6:
                return new ItemStack(Material.APPLE, (int) (Math.random() * 4) + 1);
            case 7:
                return new ItemStack(Material.RAW_FISH, (int) (Math.random() * 2) + 1);
            case 8:
                ItemStack s = new ItemStack(Material.LEVER, 1);
                ItemMeta m = s.getItemMeta();
                m.setDisplayName("Force a Switch");
                s.setItemMeta(m);
                return s;
        }
        return new ItemStack(Material.COOKED_CHICKEN);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (e.getBlock().getType() == Material.STONE) {
            Game g = MG.core().getGame(e.getPlayer());
            if (g != null && g.minigame == this) {
                SwitchGame s = (SwitchGame) g;
                if (!s.rands.containsKey(e.getPlayer().getUniqueId()))
                    s.rands.put(e.getPlayer().getUniqueId(), new Random('S' + 'w' + 'i' + 't' + 'c' + 'h'));
                if (!s.chest.containsKey(e.getPlayer().getUniqueId()))
                    s.chest.put(e.getPlayer().getUniqueId(), 5);
                int i = s.chest.get(e.getPlayer().getUniqueId()) - 1;
                s.chest.put(e.getPlayer().getUniqueId(), i);
                if (i == 0) {
                    e.getBlock().setType(Material.CHEST);
                    e.setCancelled(true);
                    BlockState b = e.getBlock().getState();
                    if (b instanceof Chest) { // I dunno why this would ever be false?
                        Chest c = (Chest) b;
                        do {
                            c.getBlockInventory().setItem((int) (Math.random() * 27), chestItem());
                        } while (Math.random() < 0.5);
                    }
                    s.chest.put(e.getPlayer().getUniqueId(), s.rands.get(e.getPlayer().getUniqueId()).nextInt(10) + 10);
                }
            }
        }
    }

    private void placeChunk(File schem, WorldBuffer b, int x, int z) {
        try {
            // Based off https://github.com/sk89q/WorldEdit/blob/master/worldedit-core/src/main/java/com/sk89q/worldedit/schematic/MCEditSchematicFormat.java
            NBTInputStream nbtStream = new NBTInputStream(new GZIPInputStream(new FileInputStream(schem)));
            NamedTag rootTag = nbtStream.readNamedTag();
            nbtStream.close();
            if (!rootTag.getName().equals("Schematic")) {
                throw new IOException("Tag \"Schematic\" does not exist or is not first");
            }
            CompoundTag schematicTag = (CompoundTag) rootTag.getTag();

            Map<String, Tag> schematic = schematicTag.getValue();
            if (!schematic.containsKey("Blocks")) {
                throw new IOException("Schematic file is missing a \"Blocks\" tag");
            }
            short width = getChildTag(schematic, "Width", ShortTag.class).getValue();
            short length = getChildTag(schematic, "Length", ShortTag.class).getValue();
            short height = getChildTag(schematic, "Height", ShortTag.class).getValue();
            if (width != 16 || length != 16 || height != 256)
                throw new IOException("Invalid dimension of " + width + "," + height + "," + length);

            Chunk c = b.getChunkAt(x, z);
            byte[] blockId = getChildTag(schematic, "Blocks", ByteArrayTag.class).getValue();
            byte[] blockData = getChildTag(schematic, "Data", ByteArrayTag.class).getValue();
            for (int y = 0; y < 16; y++) {
                boolean empty = true;
                for (int i = y * 4096; i < y * 4096 + 4096; i++) {
                    if (blockId[i] != 0) {
                        empty = false;
                        break;
                    }
                }
                if (empty) continue;
                Section s = c.createSection(y);
                int a = y * 4096;
                System.arraycopy(blockId, a, s.blocks, 0, 4096);
                for (int i = 0; i < 4096; i += 2) {
                    s.data[i / 2] = (byte) ((blockData[i + 1 + a] << 4) | blockData[i + a]);
                }
            }

            // TODO ADD TILE ENTITIES

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Game newGame() {
        return new SwitchGame(this);
    }

    @Override
    public void startGame(Game game) {
        SwitchGame g = (SwitchGame) game;

        g.world.setDifficulty(Difficulty.HARD);

        for (int x = -5; x < 5; x++)
            for (int z = -5; z < 5; z++)
                g.world.getChunkAt(x, z).load();

        List<org.bukkit.Chunk> chunks = new ArrayList<>();

        for (int x = -2; x <= 2; x+=2) {
            for (int z = -2; z <= 2; z+=2) {
                chunks.add(g.world.getChunkAt(x, z));
            }
        }

        for (Player p : g.players) {
            org.bukkit.Chunk chunk = chunks.remove(0);
            Block highest;
            do {
                highest = g.world.getHighestBlockAt((int) (Math.random() * 16) | (chunk.getX() << 4), (int) (Math.random() * 16) | (chunk.getZ() << 4));
            } while (highest.getType() == Material.LAVA);
            p.teleport(highest.getLocation().add(0.5, 1, 0.5));
            MG.resetPlayer(p);
            p.playSound(p.getLocation(), Sound.LEVEL_UP, 2, 1);
            g.minigame.message(p, "Game has started!");
            p.sendMessage("Every once in a while you will switch places with another player!");
            p.sendMessage("Set a trap to kill them when they switch with you!");
        }
        // Fix players not seeing eachother bug
        Bukkit.getScheduler().scheduleSyncDelayedTask(Switch.this, () -> {
            for (Player p : g.players) {
                p.teleport(p.getLocation());
            }
        }, 1);
        g.switchCountdown();
        g.hungerTicker();
    }

    @Override
    public void generateWorld(Game g, WorldBuffer w) {
        g.moneyPerDeath = 5;
        g.moneyPerWin = 30;
        g.disableBlockBreaking = false;
        g.disableBlockPlacing = false;
        g.disableHunger = false;

        ArrayList<File> schems = randomSchems();

        long t = System.currentTimeMillis();
        for (int x = -10; x < 10; x++)
            for (int z = -10; z < 10; z++)
                w.blankChunk(x, z);

        byte[] barrier = new byte[4096];
        for (int i = 0; i < 4096; i++) barrier[i] = (byte) 166;
        for (int x = -3; x <= 3; x++)
            for (int z = -3; z <= 3; z++) {
                if (x % 2 == 0 && z % 2 == 0) {
                    File s = schems.remove(0);
                    placeChunk(s, w, x, z);
                } else {
                    for (int i = 0; i < 16; i++)
                        w.getChunkAt(x, z).createSection(i).blocks = barrier;
                }
            }

        getLogger().info("Generated map in " + (System.currentTimeMillis() - t) + "ms");
    }


}
