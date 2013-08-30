package regalowl.autoprotect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;



public class ClaimHandler implements Listener {

	private AutoProtect ap;
	private FileConfiguration clf;
	private FileConfiguration plf;
	private FileConfiguration cof;
	private Debug d;
	private ConcurrentHashMap<String, Claim> claims = new ConcurrentHashMap<String, Claim>();
	private ConcurrentHashMap<String, Claim> potentialClaims = new ConcurrentHashMap<String, Claim>();
	private ArrayList<Material> skipBlocks = new ArrayList<Material>();
	
	private ArrayList<Material> protectedMaterials = new ArrayList<Material>();
	private ArrayList<String> autoClaim = new ArrayList<String>();
	
	ClaimHandler() {
		ap = AutoProtect.ap;
		clf = ap.yh().getFileConfiguration("claims");
		plf = ap.yh().getFileConfiguration("players");
		cof = ap.yh().getFileConfiguration("config");
		d = ap.getDebug();
		ArrayList<String> skipNames = ap.getStringFunctions().explode(cof.getString("ignored_materials"), ",");
		for (String name:skipNames) {
			Material m = Material.matchMaterial(name);
			if (m == null) {continue;}
			skipBlocks.add(m);
		}
		protectedMaterials.add(Material.CROPS);
		protectedMaterials.add(Material.SOIL);
		protectedMaterials.add(Material.CARROT);
		protectedMaterials.add(Material.POTATO);
		Iterator<String> iterat = clf.getKeys(false).iterator();
		while (iterat.hasNext()) {
			String claimKey = iterat.next().toString();
			String owner = clf.getString(claimKey + ".owner");
			String[] values = claimKey.split("\\|");
			try {
				int x = Integer.parseInt(values[0]);
				int z = Integer.parseInt(values[1]);
				String world = values[2];
				claims.put(claimKey, new Claim(x, z, world, owner));
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
		}
		iterat = plf.getKeys(false).iterator();
		while (iterat.hasNext()) {
			String player = iterat.next().toString();
			if (plf.isSet(player + ".autoclaim")) {
				if (plf.getBoolean(player + ".autoclaim") == true) {
					autoClaim.add(player);
				}
			}
		}
		ap.getServer().getPluginManager().registerEvents(this, ap);
	}
	
	public void enableAutoClaim(Player p) {
		if (!autoClaim.contains(p.getName())) {
			autoClaim.add(p.getName());
			plf.set(p.getName() + ".autoclaim", true);
		}
	}
	public void disableAutoClaim(Player p) {
		if (autoClaim.contains(p.getName())) {
			autoClaim.remove(p.getName());
			plf.set(p.getName() + ".autoclaim", false);
		}
	}
	public boolean isAutoClaiming(Player p) {
		if (autoClaim.contains(p.getName())) {
			return true;
		} else {
			return false;
		}
	}
	
	public Claim getClaim(Location l) {
		Chunk c = l.getWorld().getChunkAt(l);
		String key = c.getX() + "|" + c.getZ() + "|" + c.getWorld().getName();
		if (claims.containsKey(key)) {
			return claims.get(key);
		}
		return null;
	}
	public Claim getClaim(Chunk c) {
		String key = c.getX() + "|" + c.getZ() + "|" + c.getWorld().getName();
		if (claims.containsKey(key)) {
			return claims.get(key);
		}
		return null;
	}
	public Claim getClaim(String key) {
		if (claims.containsKey(key)) {
			return claims.get(key);
		}
		return null;
	}
	
	public AdjacentStatus adjacentStatus(Location l, Player p) {
		AdjacentStatus status = AdjacentStatus.NOT_OWNED;
		Chunk chunk = l.getChunk();
		int x = chunk.getX();
		int z = chunk.getZ();
		ArrayList<Claim> adjacentClaims = new ArrayList<Claim>();
		adjacentClaims.add(getClaim(l.getWorld().getChunkAt(x - 1, z)));
		adjacentClaims.add(getClaim(l.getWorld().getChunkAt(x - 1, z - 1)));
		adjacentClaims.add(getClaim(l.getWorld().getChunkAt(x, z - 1)));
		adjacentClaims.add(getClaim(l.getWorld().getChunkAt(x + 1, z)));
		adjacentClaims.add(getClaim(l.getWorld().getChunkAt(x + 1, z + 1)));
		adjacentClaims.add(getClaim(l.getWorld().getChunkAt(x, z + 1)));
		adjacentClaims.add(getClaim(l.getWorld().getChunkAt(x - 1, z + 1)));
		adjacentClaims.add(getClaim(l.getWorld().getChunkAt(x + 1, z - 1)));
		
		for (Claim c : adjacentClaims) {
			if (c != null) {
				if (c.getOwner().equalsIgnoreCase(p.getName())) {
					status = AdjacentStatus.SAME_OWNER;
				} else {
					if (c.allowed(p.getName())) {
						status = AdjacentStatus.ALLOWED;
					} else {
						return AdjacentStatus.NOT_ALLOWED;
					}
				}
			}
		}
		return status;
	}
	
	public int getClaimCount(Player p) {
		int count = 0;
		for (Claim c:claims.values()) {
			if (c.getOwner().equals(p.getName())) {
				count++;
			}
		}
		return count;
	}
	public void claimChunk(Player p) {
		Location l = p.getLocation();
		Chunk c = l.getWorld().getChunkAt(l);
		String key = c.getX() + "|" + c.getZ() + "|" + c.getWorld().getName();
		if (!claims.containsKey(key)) {
			claims.put(key, new Claim(c.getX(), c.getZ(), l.getWorld().getName(), p.getName()));
		}
	}
	
	public void unclaimChunk(Player p) {
		Location l = p.getLocation();
		Chunk c = l.getWorld().getChunkAt(l);
		String key = c.getX() + "|" + c.getZ() + "|" + c.getWorld().getName();
		if (claims.containsKey(key)) {
			Claim claim = claims.get(key);
			claim.delete();
			claims.remove(key);
		}
	}
	public void unclaimAll(String player) {
		for (Claim c:claims.values()) {
			if (c.getOwner().equals(player)) {
				c.delete();
				claims.remove(c.getKey());
			}
		}
	}
	
	public Collection<Claim> getClaims() {
		return claims.values();
	}
	
	
	public void unclaimNear(Player p) {
		Chunk cc = p.getLocation().getChunk();
		String world = p.getLocation().getWorld().getName();
		int cx = cc.getX();
		int cz = cc.getZ();
		for (int z = (cz-5); z <= (cz+5); z++) {
			for (int x = (cx-5); x <= (cx+5); x++) {
				Claim claim = getClaim(x + "|" + z + "|" + world);
				if (claim == null) {continue;}
				if (claim.getOwner().equals(p.getName())) {
					claim.delete();
					claims.remove(claim.getKey());
				}
			}
		}
	}
	
	
	
	
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (event.isCancelled()) {return;}
		if (d.isActive()) {
			d.sendMessage("[BlockPlace] Block:" + event.getBlock().getType().name());
		}
		Block b = event.getBlock();
		Location l = b.getLocation();
		Player p = event.getPlayer();
		Chunk c = l.getWorld().getChunkAt(l);
		String key = c.getX() + "|" + c.getZ() + "|" + c.getWorld().getName();	
		if (claims.containsKey(key)) {
			Claim claim = claims.get(key);
			if (!claim.allowed(p.getName())) {
				event.setCancelled(true);
				p.sendMessage(ChatColor.RED + "You are not allowed to place blocks here.");
			}
			return;
		} else {
			if (skipBlocks.contains(b.getType())) {return;}
			if (!autoClaim.contains(p.getName())) {return;}
			if (getClaimCount(p) >= cof.getInt("max_claims")) {return;}
			if (potentialClaims.containsKey(key)) {
				Claim claim = potentialClaims.get(key);
				if (claim.getBlockCount() >= cof.getInt("blocks_to_trigger_claim")) {
					claims.put(key, claim);
					potentialClaims.remove(key);
					p.sendMessage(ChatColor.GREEN + "Chunk claimed. Type '/ap' for more information.");
				}
				claim.addBlockCount();
			} else {
				AdjacentStatus status = adjacentStatus(l, p);
				if (status == AdjacentStatus.SAME_OWNER) {
					claims.put(key, new Claim(c.getX(), c.getZ(), c.getWorld().getName(), p.getName()));
					p.sendMessage(ChatColor.GREEN + "Chunk claimed. Type '/ap' for more information.");
				} else if (status == AdjacentStatus.NOT_OWNED) {
					potentialClaims.put(key, new Claim(c.getX(), c.getZ(), c.getWorld().getName(), p.getName()));
				}
			}	
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
    	if (event.isCancelled()) {return;}
		if (d.isActive()) {
			d.sendMessage("[HangingBreakByEntity] Broken:" + event.getEntity().getType().getName());
		}
    	Claim c = getClaim(event.getEntity().getLocation());
    	if (c == null) {return;}
    	if (event.getRemover() instanceof Player) {
    		Player p = (Player)event.getRemover();
    		if (!c.allowed(p.getName())) {
        		event.setCancelled(true);
        	}
    	} else {
    		event.setCancelled(true);
    	}
    	
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerEmptyBucket(PlayerBucketEmptyEvent event) {
		if (event.isCancelled()) {return;}
		if (d.isActive()) {
			d.sendMessage("[PlayerEmptyBucket] Player:" + event.getPlayer().getName());
		}
		Block cb = event.getBlockClicked();
    	BlockFace face = event.getBlockFace();
    	Block b = cb.getRelative(face);
    	Claim c = getClaim(b.getLocation());
    	if (adjacentStatus(b.getLocation(), event.getPlayer()) == AdjacentStatus.NOT_ALLOWED && c == null) {
    		event.setCancelled(true);
    		event.getPlayer().sendMessage(ChatColor.RED + "You are too close to claimed territory to empty a bucket here.");
    		return;
    	}
    	if (c == null) {return;}
    	if (!c.allowed(event.getPlayer().getName())) {
        	event.setCancelled(true);
        	return;
        }
    }
	
    @EventHandler (priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
    	if (event.hasBlock()) {
        	if (event.isCancelled()) {return;}
    		if (d.isActive()) {
    			d.sendMessage("[PlayerInteract] Block:" + event.getClickedBlock().getType().name() + " Player: " + event.getPlayer().getName());
    		}
        	Claim c = getClaim(event.getClickedBlock().getLocation());
        	if (c == null) {return;}
        	if (!c.allowed(event.getPlayer().getName())) {
            	event.setCancelled(true);
            }
    	}
    }
	
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityInteract(EntityInteractEvent event) {
    	if (event.isCancelled()) {return;}
		if (d.isActive()) {
			d.sendMessage("[EntityInteract] Block:" + event.getBlock().getType().name());
		}
    	Claim c = getClaim(event.getBlock().getLocation());
    	if (c == null) {return;}
    	if (event.getEntity() instanceof Player) {
    		Player p = (Player)event.getEntity();
    		if (!c.allowed(p.getName())) {
        		event.setCancelled(true);
        	}
    	}
    }
	
    /*
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent event) {
    	if (event.isCancelled()) {return;}
    	ap.getDebug().sendMessage("[BlockBreak] Block:" + event.getBlock().getType().name());
    	Claim c = getClaim(event.getBlock().getLocation());
    	if (c == null) {return;}
    	if (!c.allowed(event.getPlayer().getName())) {
        	event.setCancelled(true);
        }
    }
	*/
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
    	if (event.isCancelled()) {return;}
		if (d.isActive()) {
			d.sendMessage("[BlockPistonExtend] Block:" + event.getBlock().getType().name());
		}
		BlockFace direction = event.getDirection();
		ArrayList<Block> allBlocks = new ArrayList<Block>();
    	allBlocks.add(event.getBlock());
    	for (Block b:event.getBlocks()) {
    		if (!allBlocks.contains(b)) {
    			allBlocks.add(b);
    		}
    		if (!allBlocks.contains(b.getRelative(direction))) {
    			allBlocks.add(b.getRelative(direction));
    		}
    	}
    	String owner = null;
    	String priorOwner = null;
    	for (Block b:allBlocks) {
    		Claim c = getClaim(b.getLocation());
        	if (c == null) {
        		owner = "none";
        	} else {
        		owner = c.getOwner();
        	}
        	if (priorOwner == null) {priorOwner = owner;}
        	if (!priorOwner.equals(owner)) {
        		event.setCancelled(true);
        		return;
        	}
        	priorOwner = owner;
    	}
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
    	if (event.isCancelled()) {return;}
		if (d.isActive()) {
			d.sendMessage("[BlockPistonRetract] Block:" + event.getBlock().getType().name());
		}
    	Claim to = getClaim(event.getRetractLocation());
    	Claim from = getClaim(event.getBlock().getLocation());
    	String toOwner = "none";
    	String fromOwner = "none";
    	if (to != null) {toOwner = to.getOwner();}
    	if (from != null) {fromOwner = from.getOwner();}
    	if (!toOwner.equals(fromOwner)) {
    		event.setCancelled(true);
    	}
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockIgnite(BlockIgniteEvent event) {
    	if (event.isCancelled()) {return;}
		if (d.isActive()) {
			d.sendMessage("[BlockIgnite] Block:" + event.getBlock().getType().name());
		}
    	Claim c = getClaim(event.getBlock().getLocation());
    	if (c == null) {return;}
    	Player p = event.getPlayer();
    	if (p != null) {
    		if (c.allowed(p.getName())) {
    			return;
    		}
    	}
        event.setCancelled(true); 
    }
    
    /*
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockFromTo(BlockFromToEvent event) {
    	if (event.isCancelled()) {return;}
    	Bukkit.broadcastMessage("BlockFromTo: " + event.getBlock().getType().name());
    	Claim to = getClaim(event.getToBlock().getLocation());
    	Claim from = getClaim(event.getBlock().getLocation());
    	String toOwner = "none";
    	String fromOwner = "none";
    	if (to != null) {toOwner = to.getOwner();}
    	if (from != null) {fromOwner = from.getOwner();}
    	if (!toOwner.equals(fromOwner)) {
    		event.setCancelled(true);
    	}
    }
    */
    /*
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockSpread(BlockSpreadEvent event) {
    	if (event.isCancelled()) {return;}
    	ap.getDebug().sendMessage("[BlockSpread] Block:" + event.getBlock().getType().name());
    	Claim c = getClaim(event.getBlock().getLocation());
    	if (c == null) {return;}
    	event.setCancelled(true);
    }
     */
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityExplode(EntityExplodeEvent event) {
    	if (event.isCancelled()) {return;}
		if (d.isActive()) {
			d.sendMessage("[EntityExplode] Entity:" + event.getEntityType().getName());
		}
    	List<Block> blocks = event.blockList();
    	for (Block b:blocks) {
        	Claim c = getClaim(b.getLocation());
        	if (c == null) {continue;}
        	event.setCancelled(true);	
    	}
    }
    
    /*
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityBreakDoor(EntityBreakDoorEvent event) {
    	if (event.isCancelled()) {return;}
    	ap.getDebug().sendMessage("[EntityBreakDoor] Entity:" + event.getEntityType().getName());
    	Claim c = getClaim(event.getBlock().getLocation());
    	if (c == null) {return;}
    	if (event.getEntity() instanceof Player) {
    		Player p = (Player)event.getEntity();
    		if (!c.allowed(p.getName())) {
        		event.setCancelled(true);
        	}
    	} else {
    		event.setCancelled(true);
    	}
    }
    */

    

    
/*
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBurn(BlockBurnEvent event) {
    	if (event.isCancelled()) {return;}
		if (d.isActive()) {
			d.sendMessage("[BlockBurn] Block:" + event.getBlock().getType().name());
		}
    	Claim c = getClaim(event.getBlock().getLocation());
    	if (c == null) {return;}
    	event.setCancelled(true);
    }
    */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
    	if (event.isCancelled()) {return;}
		if (d.isActive()) {
			d.sendMessage("[EntityChangeBlock] Entity:" + event.getEntityType().getName() + " Block:" + event.getBlock().getType().name());
		}
    	Claim c = getClaim(event.getBlock().getLocation());
    	if (c == null) {return;}
    	if (event.getEntity() instanceof Monster) {
    		event.setCancelled(true);
    	} else if (event.getEntity() instanceof Player) {
    		Player p = (Player)event.getEntity();
    		if (!c.allowed(p.getName())) {
    			event.setCancelled(true);
    		}
    	}
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
    	if (event.isCancelled()) {return;}
		if (d.isActive()) {
			d.sendMessage("[PlayerTeleport] Player:" + event.getPlayer().getName());
		}
    	if (event.getPlayer().isOp() || event.getPlayer().hasPermission("autoprotect.admin")) {return;}
    	String toOwner = "none";
    	String fromOwner = "none";
    	Claim to = getClaim(event.getTo());
    	if (to == null) {return;}
    	Claim from = getClaim(event.getFrom());
    	if (from != null) {fromOwner = from.getOwner();}
    	if (to != null) {toOwner = to.getOwner();}
    	if (!fromOwner.equals(toOwner) && !to.equals(event.getPlayer().getName())) {
    		event.setCancelled(true);
    	}
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
    	if (event.isCancelled()) {return;}
		if (d.isActive()) {
			d.sendMessage("[EntityDamageByEntity] Entity:" + event.getEntityType().getName() + " Damager:" + event.getDamager().getType().name());
		}
    	Claim c = getClaim(event.getEntity().getLocation());
    	if (c == null) {return;}
    	if (event.getDamager() instanceof Player) {
    		Player p = (Player)event.getDamager();
    		if (!c.allowed(p.getName())) {
        		event.setCancelled(true);
        	}
    	}
    }
   
    
}
