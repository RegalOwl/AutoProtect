package regalowl.autoprotect;

import java.util.ArrayList;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;


public class Outline {
	private AutoProtect ap;
	private BukkitTask restoreTask;
	private String name;
	ArrayList<Block> original;
	ArrayList<Block> changed = new ArrayList<Block>();
	ArrayList<Material> materials = new ArrayList<Material>();
	ArrayList<Byte> data = new ArrayList<Byte>();
	private long waitTicks;
	Outline(ArrayList<Block> blocks, String owner) {
		name = owner;
		original = blocks;
		waitTicks = 200L;
		ap = AutoProtect.ap;
		for (Block b : original) {
			materials.add(b.getType());
			data.add(b.getData());
			changed.add(b);
			b.setTypeId(ap.getConfig().getInt("show_id"));
			b.setData((byte)ap.getConfig().getInt("show_data"));
		}
		restoreTask = ap.getServer().getScheduler().runTaskLater(ap, new Runnable() {
			public void run() {
				for (int i = 0; i < changed.size(); i++) {
					Block b = changed.get(i);
					b.setType(materials.get(i));
					b.setData(data.get(i));
					ap.getOutlineHandler().finished(name);
				}
			}
		}, waitTicks);
	}
	
	public void cancel() {
		restoreTask.cancel();
		for (int i = 0; i < changed.size(); i++) {
			Block b = changed.get(i);
			b.setType(materials.get(i));
			b.setData(data.get(i));
		}
	}

}
