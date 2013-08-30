package regalowl.autoprotect;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

public class AutoClaim implements Listener {

	ArrayList<String> autoClaim = new ArrayList<String>();
	ArrayList<String> autoUnclaim = new ArrayList<String>();
	private AutoProtect ap;
	private BukkitTask autoClaimTask;
	private ClaimHandler ch;
	private boolean running;
	
	AutoClaim() {
		ap = AutoProtect.ap;
		ch = ap.getClaimHandler();
		running = false;
		ap.getServer().getPluginManager().registerEvents(this, ap);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		removeAutoClaim(player.getName());
		removeAutoUnclaim(player.getName());
	}
	
	
	public void start() {
		if (running) {return;}
		running = true;
		autoClaimTask = ap.getServer().getScheduler().runTaskTimer(ap, new Runnable() {
		    public void run() {
		    	if (autoClaim.isEmpty() && autoUnclaim.isEmpty()) {
		    		stop();
		    	}
		    	for (String player:autoClaim) {
		    		claim(player);
		    	}
		    	for (String player:autoUnclaim) {
		    		unclaim(player);
		    	}
		    }
		}, 5L, 5L);
	}
	
	public void stop() {
		autoClaimTask.cancel();
		running = false;
	}
	
	public void addAutoClaim(String player) {
		if (!autoClaim.contains(player)) {
			autoClaim.add(player);
			start();
		}
	}
	public void addAutoUnclaim(String player) {
		if (!autoUnclaim.contains(player)) {
			autoUnclaim.add(player);
			start();
		}
	}
	public void removeAutoClaim(String player) {
		if (autoClaim.contains(player)) {
			autoClaim.remove(player);
		}
	}
	public void removeAutoUnclaim(String player) {
		if (autoUnclaim.contains(player)) {
			autoUnclaim.remove(player);
		}
	}
	
	public boolean isAutoClaiming(String player) {
		if (autoClaim.contains(player)) {
			return true;
		}
		return false;
	}
	public boolean isAutoUnclaiming(String player) {
		if (autoUnclaim.contains(player)) {
			return true;
		}
		return false;
	}
	
	private void claim(String player) {
		Player p = Bukkit.getPlayer(player);
		if (p == null) {return;}
		Claim claim = ch.getClaim(p.getLocation());
		if (claim == null) {
			if (!(ch.getClaimCount(p) > ap.yh().getFileConfiguration("config").getInt("max_claims"))) {
				AdjacentStatus status = ch.adjacentStatus(p.getLocation(), p);
				if (status == AdjacentStatus.NOT_ALLOWED) {
					return;
				} else {
					ch.claimChunk(p);
					p.sendMessage(ChatColor.GREEN + "Chunk claimed.");
				}
			}
		}
	}
	
	private void unclaim(String player) {
		Player p = Bukkit.getPlayer(player);
		if (p == null) {return;}
		Claim claim = ch.getClaim(p.getLocation());
		if (claim != null) {
			if (claim.getOwner().equalsIgnoreCase(p.getName())) {
				ch.unclaimChunk(p);
				p.sendMessage(ChatColor.GREEN + "Chunk unclaimed.");
			}
		}
	}
	
	
}
