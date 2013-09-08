package regalowl.autoprotect;

import java.util.ArrayList;




public class AdminMode {

	private ArrayList<String> adminMode = new ArrayList<String>();
	private boolean active;

	AdminMode() {
		active = false;
	}
	public void enableAdminMode(String player) {
		if (!adminMode.contains(player)) {
			adminMode.add(player);
			active = true;
		}
	}
	public void disableAdminMode(String player) {
		if (adminMode.contains(player)) {
			adminMode.remove(player);
		}
		if (adminMode.size() == 0) {
			active = false;
		}
	}	
	public boolean isAdminMode(String player) {
		if (adminMode.contains(player)) {
			return true;
		}
		return false;
	}
	public boolean isActive() {
		return active;
	}	
}
