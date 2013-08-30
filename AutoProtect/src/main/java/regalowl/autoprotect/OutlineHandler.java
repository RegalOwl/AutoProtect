package regalowl.autoprotect;

import java.util.ArrayList;
import java.util.HashMap;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class OutlineHandler implements Listener {
	
	private HashMap<String,Outline> outlines = new HashMap<String,Outline>();
	
	OutlineHandler() {
		AutoProtect.ap.getServer().getPluginManager().registerEvents(this, AutoProtect.ap);
	}
	
	
	
	public boolean makeOutline(ArrayList<Block> blocks, String owner) {
		if (!outlines.containsKey(owner)) {
			outlines.put(owner, new Outline(blocks, owner));
			return true;
		}
		return false;
	}
	
	public void finished(String owner) {
		if (outlines.containsKey(owner)) {
			outlines.remove(owner);
		}
	}
	
	public void shutDown() {
		for (Outline o:outlines.values()) {
			o.cancel();
		}
	}
	
	
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
    	if (outlines.containsKey(event.getPlayer().getName())) {
    		event.setCancelled(true);
    	}
    }
    
	@EventHandler(priority = EventPriority.HIGH)
	public void onBlockPlace(BlockPlaceEvent event) {
    	if (outlines.containsKey(event.getPlayer().getName())) {
    		event.setCancelled(true);
    	}
	}
	
}
