package net.justminecraft.minigames.switchgame;

import net.justminecraft.minigames.minigamecore.Game;
import net.justminecraft.minigames.minigamecore.Minigame;
import net.justminecraft.minigames.minigamecore.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class SwitchGame extends Game {

    public int taskId = 0;
    public HashMap<UUID, Integer> chest = new HashMap<UUID, Integer>();
    public HashMap<UUID, Random> rands = new HashMap<UUID, Random>();

    public SwitchGame(Minigame mg) {
        super(mg);
    }

    public void switchNow() {
        Bukkit.getScheduler().cancelTask(taskId);
        doSwitch();
    }

    private void doSwitch() {
        broadcastRaw("Switch in 2 seconds!");
        taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(minigame, new Runnable() {
            public void run() {
                if (players.size() > 0) {
                    ArrayList<Location> l = new ArrayList<Location>();
                    for (Player p : players)
                        l.add(p.getLocation());
                    l.add(l.get(0));
                    l.remove(0);
                    for (Player p : players) {
                        p.teleport(l.get(0));
                        l.remove(0);
                        p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 60, 255));
                        PlayerData.get(p.getUniqueId()).incrementStat(minigame.getMinigameName(), "switches");
                    }
                    broadcastRaw("A switch has occured!");
                    switchCountdown();
                }
            }
        }, 40);
    }

    public void switchCountdown() {
        taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(minigame, new Runnable() {
            public void run() {
                doSwitch();
            }
        }, 20 * 20 * (int) (Math.random() * 6 + 1) - 2);
    }

    public void hungerTicker() {
        Bukkit.getScheduler().scheduleSyncDelayedTask(minigame, new Runnable() {
            public void run() {
                for (Player p : players) {
                    if (p.getSaturation() > 1)
                        p.setSaturation(p.getSaturation() - 1);
                    else if (p.getSaturation() > 0)
                        p.setSaturation(0);
                    else if (p.getFoodLevel() > 0)
                        p.setFoodLevel(p.getFoodLevel() - 1);
                }
                if (players.size() > 0)
                    hungerTicker();
            }
        }, (long) ((1 / Switch.HUNGER_LOSS_PER_SECOND) * 20));
    }

}
