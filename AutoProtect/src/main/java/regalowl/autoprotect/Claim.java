package regalowl.autoprotect;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;



public class Claim {
	
	private String owner;
	private ArrayList<String> allowed = new ArrayList<String>();
	private String world;
	private int x;
	private int z;
	private String key;
	
	private AutoProtect ap;
	private FileConfiguration cc;
	
	private int blockCount;
	private StringFunctions sf;
	
	
	

	Claim(int x, int z, String world, String owner) {
		this.x = x;
		this.z = z;
		this.world = world;
		this.owner = owner;
		this.key = x + "|" + z + "|" + world;
		ap = AutoProtect.ap;
		sf = ap.getStringFunctions();
		cc = ap.yh().getFileConfiguration("claims");
		cc.set(key + ".owner", owner);
		blockCount = 0;
		allowed = sf.explode(cc.getString(key + ".allowed"), ",");
	}
	
	
	public boolean contains(Location l) {
		Chunk lChunk = Bukkit.getWorld(world).getChunkAt(l);
		if (lChunk.getX() == x && lChunk.getZ() == z && lChunk.getWorld().getName().equals(world)) {
			return true;
		}
		return false;
	}
	
	public Chunk getChunk() {
		return Bukkit.getWorld(world).getChunkAt(x, z);
	}
	public String getKey() {
		return key;
	}
	
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
		cc.set(key + ".owner", owner);
	}
	
	public void delete() {
		cc.set(key, null);
	}
	public String getAllowed() {
		return sf.implode(allowed, ",");
	}
	
	public void addAllowed(String player) {
		if (!allowed.contains(player.toLowerCase())) {
			allowed.add(player);
		}
		cc.set(key + ".allowed", sf.implode(allowed, ","));
	}
	public void removeAllowed(String player) {
		if (allowed.contains(player)) {
			allowed.remove(player);
		}
		cc.set(key + ".allowed", sf.implode(allowed, ","));
	}
	public boolean allowed(String player) {
		if (owner.equalsIgnoreCase(player)) {
			return true;
		}
		for (String name:allowed) {
			if (player.equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}
	
	public int getBlockCount() {
		return blockCount;
	}
	public void addBlockCount() {
		blockCount++;
	}
	
	public ArrayList<Block> getEdgeBlocks() {
		ArrayList<Block> blocks = new ArrayList<Block>();
		for (int i = (x*16); i < (x*16+16); i++) {
			Block b = getAcceptableBlock(i, z*16);
			if (b != null) {blocks.add(b);}
		}
		for (int i = (x*16); i < (x*16+16); i++) {
			Block b = getAcceptableBlock(i, z*16+15);
			if (b != null) {blocks.add(b);}
		}
		for (int i = (z*16+1); i < (z*16+15); i++) {
			Block b = getAcceptableBlock(x*16, i);
			if (b != null) {blocks.add(b);}
		}
		for (int i = (z*16+1); i < (z*16+15); i++) {
			Block b = getAcceptableBlock(x*16+15, i);
			if (b != null) {blocks.add(b);}
		}
		return blocks;
	}
	
	public Block getAcceptableBlock(int x, int z) {
		World w = Bukkit.getWorld(world);
		Block b = getFirstSolidBlock(x, z);
		if (b == null) {return null;}
		while (freeOfObstructions(b) == false) {
			int y = b.getY() + 1;
			if (y >= 128) {return null;}
			b = w.getBlockAt(b.getX(), y, b.getZ());
		}
		return b;
	}
	
	public Block getFirstSolidBlock(int x, int z) {
		int y = 128;
		World w = Bukkit.getWorld(world);
		Block nb = w.getBlockAt(x, y, z);
		while (!(nb.getType() == Material.AIR)) {
			if (y == 0) {return null;}
			nb = w.getBlockAt(nb.getX(), y, nb.getZ());
			y--;
		}
		while (!nb.getType().isSolid()) {
			if (y == 0) {return null;}
			nb = w.getBlockAt(nb.getX(), y - 1, nb.getZ());
			y--;
		}
		return nb;
	}
	
	public boolean freeOfObstructions(Block b) {
		if (b.getType() != Material.AIR) {return false;}
		ArrayList<Block> nearBlocks = new ArrayList<Block>();
		nearBlocks.add(b.getRelative(BlockFace.EAST));
		nearBlocks.add(b.getRelative(BlockFace.WEST));
		nearBlocks.add(b.getRelative(BlockFace.NORTH));
		nearBlocks.add(b.getRelative(BlockFace.SOUTH));
		nearBlocks.add(b.getRelative(BlockFace.UP));
		nearBlocks.add(b.getRelative(BlockFace.DOWN));
		int showId = ap.getConfig().getInt("show_id");
		byte showData = (byte)ap.getConfig().getInt("show_data");
		for (Block block:nearBlocks) {
			if (block.getType() == Material.AIR) {continue;}
			if (block.getData() != showData || block.getTypeId() != showId) {
				return false;
			}
		}
		return true;
	}
	
	
}
