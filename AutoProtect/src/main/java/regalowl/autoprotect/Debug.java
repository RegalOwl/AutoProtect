package regalowl.autoprotect;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;


public class Debug {

	private ArrayList<String> debug = new ArrayList<String>();
	private boolean active;

	Debug() {
		active = false;
	}

	public void addDebug(String player) {
		if (!debug.contains(player)) {
			debug.add(player);
			active = true;
		}
	}
	public void removeDebug(String player) {
		if (debug.contains(player)) {
			debug.remove(player);
		}
		if (debug.size() == 0) {
			active = false;
		}
	}
	
	public boolean isDebugging(String player) {
		if (debug.contains(player)) {
			return true;
		}
		return false;
	}
	
	public boolean isActive() {
		return active;
	}


	public void sendMessage(String message) {
		for (String player:debug) {
			Player p = Bukkit.getPlayer(player);
			if (p != null) {
				p.sendMessage(message);
			}
		}
	}
	
	
}
