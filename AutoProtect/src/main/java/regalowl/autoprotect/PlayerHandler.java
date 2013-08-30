package regalowl.autoprotect;


import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

public class PlayerHandler implements Listener {
	
	private BukkitTask timerTask;
	private BukkitTask claimCleaner;
	private AutoProtect ap;
	private long lastMinutes;
	private long serverUptime;
	private FileConfiguration players;
	private FileConfiguration config;
	
	PlayerHandler() {
		lastMinutes = getCurrentMinutes();
		ap = AutoProtect.ap;
		players = ap.yh().getFileConfiguration("players");
		config = ap.yh().getFileConfiguration("config");
		serverUptime = config.getLong("server_uptime");
		startTimer();
		startClaimCleaner();
		ap.getServer().getPluginManager().registerEvents(this, ap);
	}
	public void disable() {
		timerTask.cancel();
		claimCleaner.cancel();
		for (Player p:Bukkit.getOnlinePlayers()) {
			players.set(p.getName() + ".last_seen", serverUptime);
		}
	}
	
	
	public void startTimer() {
		timerTask = ap.getServer().getScheduler().runTaskTimer(ap, new Runnable() {
		    public void run() {
		    	long minutes = getCurrentMinutes();
		    	serverUptime += (minutes - lastMinutes);
		    	players.set("server_uptime", serverUptime);
		    	lastMinutes = minutes;
		    }
		}, 1200L, 1200L);
	}
	
	
	public void startClaimCleaner() {
		claimCleaner = ap.getServer().getScheduler().runTaskTimer(ap, new Runnable() {
		    public void run() {
				Iterator<String> iterat = players.getKeys(false).iterator();
				while (iterat.hasNext()) {
					String player = iterat.next().toString();
					long lastSeen = players.getLong(player + ".last_seen");
					long maxMinutes = config.getLong("days_before_removal") * 24 * 60;
					if ((serverUptime - lastSeen) >= maxMinutes) {
						ap.getClaimHandler().unclaimAll(player);
						players.set(player, null);
						Bukkit.broadcast(ChatColor.RED + "[AutoProtect]" + player + "'s claims have been removed due to disuse.", "autoprotect.admin");
					}
				}
		    }
		}, 0L, 72000L);
	}

	
	@EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
		players.set(event.getPlayer().getName() + ".last_seen", serverUptime);
	}
	
	public int getCurrentMinutes() {return Math.round(System.currentTimeMillis()/60000);}

	public void setServerUptime(int value) {
		config.set("server_uptime", value);
		serverUptime = value;
		Iterator<String> iterat = players.getKeys(false).iterator();
		while (iterat.hasNext()) {
			String player = iterat.next().toString();
			players.set(player + ".last_seen", serverUptime);
		}
	}
	
}
