package regalowl.autoprotect;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import regalowl.databukkit.DataBukkit;
import regalowl.databukkit.DataBukkitPlugin;
import regalowl.databukkit.YamlHandler;

public class AutoProtect extends JavaPlugin {
	public static AutoProtect ap;
	private DataBukkit db;
	private YamlHandler yh;
	private FileConfiguration config;
	private PlayerHandler ph;
	private ClaimHandler ch;
	private StringFunctions sf;
	private OutlineHandler oh;
	private AutoClaim ac;
	private Debug d;
	private AdminMode am;
	

	@Override
	public void onEnable() {
		if (DataBukkitPlugin.dataBukkit == null) {
			Logger log = Logger.getLogger("Minecraft");
			log.severe("[AutoProtect]DataBukkit connection failed, shutting down.");
			return;
		}
		db = DataBukkitPlugin.dataBukkit.getDataBukkit(this);
		yh = db.getYamlHandler();
		yh.registerFileConfiguration("config");
		yh.registerFileConfiguration("claims");
		yh.registerFileConfiguration("players");
		yh.copyFromJar("config");
		yh.setSaveInterval(6000L);
		config = yh.getFileConfiguration("config");
		ap = this;
		d = new Debug();
		am = new AdminMode();
		sf = new StringFunctions();
		ph = new PlayerHandler();
		ch = new ClaimHandler();
		oh = new OutlineHandler();
		ac = new AutoClaim();
	}

	@Override
	public void onDisable() {
		db.shutDown();
		oh.shutDown();
		ph.disable();
	}


	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("You must be in game to use AutoProtect.");
			return true;
		}
		Player p = (Player)sender;
		if (args.length == 0) {
			sendHelpMessage(p);
			return true;
		}
		String command = args[0];
		
		if (command.equalsIgnoreCase("allowchunk")) {
			if (args.length > 1) {
				String name = args[1];
				Claim c = ch.getClaim(p.getLocation());
				if (c == null) {
					p.sendMessage(ChatColor.RED + "You do not own this chunk.");
					return true;
				}
				if (c.getOwner().equals(p.getName())) {
					c.addAllowed(name);
					p.sendMessage(ChatColor.GREEN + name + " has been given access to this chunk.");
				} else {
					p.sendMessage(ChatColor.RED + "You do not own this chunk.");
				}
			}
			return true;
		}
		
		if (command.equalsIgnoreCase("disallowchunk")) {
			if (args.length > 1) {
				String name = args[1];
				Claim c = ch.getClaim(p.getLocation());
				if (c == null) {
					p.sendMessage(ChatColor.RED + "You do not own this chunk.");
					return true;
				}
				if (c.getOwner().equals(p.getName())) {
					c.removeAllowed(name);
					p.sendMessage(ChatColor.GREEN + name + " no longer has access to this chunk.");
				} else {
					p.sendMessage(ChatColor.RED + "You do not own this chunk.");
				}
			}
			return true;
		}
		
		if (command.equalsIgnoreCase("allow")) {
			if (args.length > 1) {
				String name = args[1];
				for (Claim c:ch.getClaims()) {
					if (c.getOwner().equals(p.getName())) {
						c.addAllowed(name);
					}
				}
				p.sendMessage(ChatColor.GREEN + name + " now has access to all of your chunks.");
			}
			return true;
		}
		

		
		if (command.equalsIgnoreCase("disallow")) {
			if (args.length > 1) {
				String name = args[1];
				for (Claim c:ch.getClaims()) {
					if (c.getOwner().equals(p.getName())) {
						c.removeAllowed(name);
					}
				}
				p.sendMessage(ChatColor.GREEN + name + " no longer has access to any of your chunks.");
			}
			return true;
		}
		
		if (command.equalsIgnoreCase("claim") || command.equalsIgnoreCase("c")) {
			Claim claim = ch.getClaim(p.getLocation());
			if (claim == null) {
				if (!(ch.getClaimCount(p) > config.getInt("max_claims"))) {
					AdjacentStatus status = ch.adjacentStatus(p.getLocation(), p);
					if (status == AdjacentStatus.NOT_ALLOWED) {
						p.sendMessage(ChatColor.RED + "You cannot claim chunks next to another player's chunk.");
					} else {
						ch.claimChunk(p);
						p.sendMessage(ChatColor.GREEN + "Chunk claimed.");
					}
				} else {
					p.sendMessage(ChatColor.RED + "You have reached your chunk claim limit. You must first unclaim a chunk before claiming this one.");
				}
			} else {
				if (claim.getOwner().equalsIgnoreCase(p.getName())) {
					p.sendMessage(ChatColor.RED + "You have already claimed this chunk.");
				} else {
					p.sendMessage(ChatColor.RED + "This chunk is claimed by " + claim.getOwner() + ".");
				}
			}
			return true;
		}
		
		if (command.equalsIgnoreCase("autoclaim") || command.equalsIgnoreCase("ac")) {
			if (ac.isAutoClaiming(p.getName())) {
				ac.removeAutoClaim(p.getName());
				p.sendMessage(ChatColor.GREEN + "Autoclaim disabled.");
			} else {
				ac.addAutoClaim(p.getName());
				p.sendMessage(ChatColor.GREEN + "Autoclaim enabled.");
			}
		}
		
		if (command.equalsIgnoreCase("unclaim") || command.equalsIgnoreCase("u")) {
			if (args.length > 1) {
				if (args[1].equalsIgnoreCase("all")) {
					ch.unclaimAll(p.getName());
					p.sendMessage(ChatColor.GREEN + "All chunks unclaimed.");
					return true;
				} else if (args[1].equalsIgnoreCase("near")) {
					ch.unclaimNear(p);
					p.sendMessage(ChatColor.GREEN + "Nearby chunks have been unclaimed.  Use /ap map to verify.");
					return true;
				}
			}
			Claim claim = ch.getClaim(p.getLocation());
			if (claim == null) {
				p.sendMessage(ChatColor.RED + "You have not claimed this chunk.");
			} else {
				if (claim.getOwner().equalsIgnoreCase(p.getName())) {
					ch.unclaimChunk(p);
					p.sendMessage(ChatColor.GREEN + "Chunk unclaimed.");
				} else {
					p.sendMessage(ChatColor.RED + "This chunk is claimed by " + claim.getOwner() + ".");
				}
			}
			return true;
		}
		
		if (command.equalsIgnoreCase("autounclaim") || command.equalsIgnoreCase("auc")) {
			if (ac.isAutoUnclaiming(p.getName())) {
				ac.removeAutoUnclaim(p.getName());
				p.sendMessage(ChatColor.GREEN + "Autounclaim disabled.");
			} else {
				ac.addAutoUnclaim(p.getName());
				p.sendMessage(ChatColor.GREEN + "Autounclaim enabled.");
			}
		}
		
		if (command.equalsIgnoreCase("off")) {
			ch.disableAutoClaim(p);
			p.sendMessage(ChatColor.GREEN + "Autoclaim has been disabled.");
			return true;
		}
		
		if (command.equalsIgnoreCase("on")) {
			ch.enableAutoClaim(p);
			p.sendMessage(ChatColor.GREEN + "Autoclaim has been enabled.");
			return true;
		}
		
		if (command.equalsIgnoreCase("show") || command.equalsIgnoreCase("s")) {
			String name = p.getName();
			if (args.length > 1 && p.hasPermission("autoprotect.admin")) {
				name = args[1];
			}
			ArrayList<Block> edgeBlocks = new ArrayList<Block>();
			ArrayList<Claim> allClaims = new ArrayList<Claim>();
			Chunk cc = p.getLocation().getChunk();
			String world = p.getLocation().getWorld().getName();
			int cx = cc.getX();
			int cz = cc.getZ();
			for (int z = (cz-2); z <= (cz+2); z++) {
				for (int x = (cx-2); x <= (cx+2); x++) {
					Claim claim = ch.getClaim(x + "|" + z + "|" + world);
					if (claim != null) {
						if (claim.getOwner().equalsIgnoreCase(name)) {
							allClaims.add(claim);
						}
					}
				}
			}
			for (Claim claim:allClaims) {
				ArrayList<Block> edge = claim.getEdgeBlocks();
				for (Block b:edge) {
					edgeBlocks.add(b);
				}
			}
			boolean success = oh.makeOutline(edgeBlocks, p.getName());
			if (success) {
				p.sendMessage(ChatColor.GREEN + "Outline drawn for 10 seconds.");
			} else {
				p.sendMessage(ChatColor.RED + "You cannot use '/ap show' right now.");
			}
			return true;
		}
		
		if (command.equalsIgnoreCase("map") || command.equalsIgnoreCase("m")) {
			Chunk cc = p.getLocation().getChunk();
			String world = p.getLocation().getWorld().getName();
			int cx = cc.getX();
			int cz = cc.getZ();
			String[] symbols = new String[25];
			symbols[0] = ChatColor.AQUA + "+";
			symbols[1] = ChatColor.YELLOW + "=";
			symbols[2] = ChatColor.BLUE + "#";
			symbols[3] = ChatColor.DARK_AQUA + "$";
			symbols[4] = ChatColor.DARK_BLUE + "%";
			symbols[5] = ChatColor.DARK_GRAY + "^";
			symbols[6] = ChatColor.DARK_GREEN + "&";
			symbols[7] = ChatColor.DARK_PURPLE + "*";
			symbols[8] = ChatColor.DARK_RED + "(";
			symbols[9] = ChatColor.GOLD + ")";
			symbols[10] = ChatColor.GRAY + "~";
			symbols[11] = ChatColor.GREEN + "@";
			symbols[12] = ChatColor.LIGHT_PURPLE + "[";
			symbols[13] = ChatColor.RED + "]";
			symbols[14] = ChatColor.BLACK + "1";
			symbols[15] = ChatColor.WHITE + "2";
			symbols[16] = ChatColor.WHITE + "3";
			symbols[17] = ChatColor.WHITE + "4";
			symbols[18] = ChatColor.WHITE + "5";
			symbols[19] = ChatColor.WHITE + "6";
			symbols[20] = ChatColor.WHITE + "7";
			symbols[21] = ChatColor.WHITE + "8";
			symbols[22] = ChatColor.WHITE + "9";
			symbols[23] = ChatColor.WHITE + "0";
			symbols[24] = ChatColor.WHITE + "?";
			p.sendMessage(fM("&cAutoProtect Map &f(&eNorth is up.&f)"));
			HashMap<String, String> symbolMap = new HashMap<String, String>();
			int symbol = 0;
			String keys = "";
			String mapRow = "";
			for (int z = (cz-10); z <= (cz+10); z++) {
				mapRow = "";
				for (int x = (cx-20); x <= (cx+20); x++) {
					if (x == cx && z == cz) {
						mapRow += ChatColor.GOLD + "X";
						continue;
					}
					Claim claim = ch.getClaim(x + "|" + z + "|" + world);
					if (claim != null) {
						String owner = claim.getOwner();
						String cSymbol = "";
						if (symbolMap.containsKey(owner)) {
							cSymbol = symbolMap.get(owner);
						} else {
							cSymbol = symbols[symbol];
							symbolMap.put(owner, cSymbol);
							keys += ChatColor.WHITE + owner + " " + cSymbol + ChatColor.WHITE + ";";
							if (symbol < 24) {symbol++;}
						}
						mapRow += cSymbol;
					} else {
						mapRow += ChatColor.WHITE + "-";
					}
				}
				p.sendMessage(mapRow);
			}
			p.sendMessage(ChatColor.AQUA + keys);
			return true;
		}

		
		if (command.equalsIgnoreCase("info") || command.equalsIgnoreCase("i")) {
			Chunk c = p.getLocation().getChunk();
			Claim claim = ch.getClaim(p.getLocation());
			p.sendMessage(ChatColor.DARK_GRAY + "-----------------------------------------------------");
			p.sendMessage(ChatColor.GREEN + "Current chunk:" + c.getX() + " z:" + c.getZ());
			if (ch.isAutoClaiming(p)) {
				p.sendMessage(ChatColor.GREEN + "You have intelligent autoclaim enabled.");
			} else {
				p.sendMessage(ChatColor.RED + "You have intelligent autoclaim disabled.");
			}
			if (claim == null) {
				p.sendMessage(ChatColor.GREEN + "Owner: none.");
			} else {
				p.sendMessage(ChatColor.GREEN + "Owner: " + claim.getOwner());
				p.sendMessage(ChatColor.GREEN + "Allowed: " + claim.getAllowed());
			}
			p.sendMessage(ChatColor.GREEN + "You own " + ch.getClaimCount(p) + " chunks out of a maximum of " + config.getInt("max_claims") + " chunks.");
			p.sendMessage(ChatColor.DARK_GRAY + "-----------------------------------------------------");
			return true;
		}
		
		if (command.equalsIgnoreCase("setowner") && p.hasPermission("autoprotect.admin")) {
			Claim c = ch.getClaim(p.getLocation());
			if (c == null) {
				p.sendMessage(ChatColor.RED + "No claim exists here.");
				return true;
			}
			if (args.length == 1) {
				p.sendMessage(ChatColor.RED + "Use /ap setowner [owner]");
				return true;
			}
			c.setOwner(args[1]);
			p.sendMessage(ChatColor.GREEN + "Owner set.");
			return true;
		}
		
		if (command.equalsIgnoreCase("set") && p.hasPermission("autoprotect.admin")) {
			if (args.length == 3) {
				try {
					String setting = args[1];
					if (setting.equalsIgnoreCase("servertime")) {
						int value = Integer.parseInt(args[2]);
						ph.setServerUptime(value);
						p.sendMessage(ChatColor.GREEN + "Value set.");
					} else if (setting.equalsIgnoreCase("maxclaims")) {
						int value = Integer.parseInt(args[2]);
						config.set("max_claims", value);
						p.sendMessage(ChatColor.GREEN + "Value set.");
					} else if (setting.equalsIgnoreCase("daysbeforeremoval")) {
						int value = Integer.parseInt(args[2]);
						config.set("days_before_removal", value);
						p.sendMessage(ChatColor.GREEN + "Value set.");
					} else if (setting.equalsIgnoreCase("blocksforclaim")) {
						int value = Integer.parseInt(args[2]);
						config.set("blocks_to_trigger_claim", value);
						p.sendMessage(ChatColor.GREEN + "Value set.");
					} else if (setting.equalsIgnoreCase("showid")) {
						int value = Integer.parseInt(args[2]);
						config.set("show_id", value);
						p.sendMessage(ChatColor.GREEN + "Value set.");
					} else if (setting.equalsIgnoreCase("showdata")) {
						int value = Integer.parseInt(args[2]);
						config.set("show_data", value);
						p.sendMessage(ChatColor.GREEN + "Value set.");
					} else if (setting.equalsIgnoreCase("ignoredmaterials")) {
						config.set("ignored_materials", args[2]);
						p.sendMessage(ChatColor.GREEN + "Value set.");
					} else {
						p.sendMessage(ChatColor.RED + "Use /ap set [servertime/maxclaims/daysbeforeremoval/blocksforclaim/showid/showdata/ignoredmaterials] [value]");
					}
				} catch (Exception e) {
					p.sendMessage(ChatColor.RED + "Use /ap set [setting] [value]");
					return true;
				}
			} else {
				p.sendMessage(ChatColor.RED + "Use /ap set [setting] [value]");
				return true;
			}
		}
		
		if (command.equalsIgnoreCase("help")) {
			sendHelpMessage(p);
			return true;
		}
		
		if (command.equalsIgnoreCase("detail")) {
			sendDetailMessage(p);
			return true;
		}
		
		if (command.equalsIgnoreCase("debug") && p.hasPermission("autoprotect.admin")) {
			if (d.isDebugging(p.getName())) {
				d.removeDebug(p.getName());
				p.sendMessage(ChatColor.GREEN + "Debug disabled.");
			} else {
				d.addDebug(p.getName());
				p.sendMessage(ChatColor.GREEN + "Debug enabled.");
			}
			return true;
		}
		
		if (command.equalsIgnoreCase("admin") && p.hasPermission("autoprotect.admin")) {
			if (am.isAdminMode(p.getName())) {
				am.disableAdminMode(p.getName());
				p.sendMessage(ChatColor.GREEN + "Admin bypass mode disabled. AutoProtect will no longer ignore you.");
			} else {
				am.enableAdminMode(p.getName());
				p.sendMessage(ChatColor.GREEN + "Admin bypass mode enabled. AutoProtect will now ignore you.");
			}
			return true;
		}
		
		return true;
	}
	
	public void sendHelpMessage(Player p) {
		p.sendMessage(fM("&7------------------&5AutoProtect Help&7-------------------"));
		p.sendMessage(fM("&6AutoProtect will do its best to automatically protect your creations from griefers.  If you need to make changes, the following commands are available: "));
		p.sendMessage(fM("&c/ap claim &f: &eClaims the chunk you're standing in."));
		p.sendMessage(fM("&c/ap unclaim ('all' or 'near') &f: &eUnclaims the chunk you're standing in, all chunks if 'all' is specified, or nearby chunks for 'near'."));
		p.sendMessage(fM("&c/ap map &f: &eDisplays nearby chunks and their owners."));
		p.sendMessage(fM("&c/ap show &f: &eOutlines nearby chunks that you own."));
		p.sendMessage(fM("&c/ap allow &b[name] &f: &eAllows another player to use your chunks."));
		p.sendMessage(fM("&c/ap disallow &b[name] &f: &eDisallows an allowed player from using your chunks."));
		p.sendMessage(fM("&c/ap allowchunk &b[name] &f: &eAllows another player to use the chunk you're standing in."));
		p.sendMessage(fM("&c/ap disallowchunk &b[name] &f: &eDisallows a player from using the chunk you're standing in."));
		p.sendMessage(fM("&c/ap off &f: &eDisables intelligent auto claiming."));
		p.sendMessage(fM("&c/ap on &f: &eEnables intelligent auto claiming."));
		p.sendMessage(fM("&c/ap info &f: &eDisplays chunk information."));
		p.sendMessage(fM("&c/ap detail &f: &eDisplays detailed plugin information."));
		p.sendMessage(fM("&c/ap autoclaim &f: &eToggles chunk autoclaim."));
		p.sendMessage(fM("&c/ap autounclaim &f: &eToggles chunk autounclaim."));
	}
	
	public void sendDetailMessage(Player p) {
		p.sendMessage(fM("&7------------------&5AutoProtect Detail&7-------------------"));
		p.sendMessage(fM("&eYou may claim a maximum of: &a" + config.getInt("max_claims") + " chunks."));
		p.sendMessage(fM("&eBlock placements needed to claim a chunk: &a" + config.getInt("blocks_to_trigger_claim")));
		p.sendMessage(fM("&eBlock placements needed to claim a chunk adjacent to an owned chunk: &a" + 1));
		p.sendMessage(fM("&eServer uptime: &a" + config.getInt("server_uptime") + " minutes."));
		p.sendMessage(fM("&eClaims will be automatically removed after: &a" + config.getInt("days_before_removal") + " days without logging in."));
		p.sendMessage(fM("&eBlock placements of the following materials will be ignored: &a" + config.getString("ignored_materials")));
	}
	
	public YamlHandler yh() {
		return yh;
	}
	
	public StringFunctions getStringFunctions() {
		return sf;
	}
	public ClaimHandler getClaimHandler() {
		return ch;
	}
	public OutlineHandler getOutlineHandler() {
		return oh;
	}
	public Debug getDebug() {
		return d;
	}
	public AdminMode getAdminMode() {
		return am;
	}
	
	public String fM(String message) {
		message = message.replace("&0", ChatColor.BLACK+"");
		message = message.replace("&1", ChatColor.DARK_BLUE+"");
		message = message.replace("&2", ChatColor.DARK_GREEN+"");
		message = message.replace("&3", ChatColor.DARK_AQUA+"");
		message = message.replace("&4", ChatColor.DARK_RED+"");
		message = message.replace("&5", ChatColor.DARK_PURPLE+"");
		message = message.replace("&6", ChatColor.GOLD+"");
		message = message.replace("&7", ChatColor.GRAY+"");
		message = message.replace("&8", ChatColor.DARK_GRAY+"");
		message = message.replace("&9", ChatColor.BLUE+"");
		message = message.replace("&a", ChatColor.GREEN+"");
		message = message.replace("&b", ChatColor.AQUA+"");
		message = message.replace("&c", ChatColor.RED+"");
		message = message.replace("&d", ChatColor.LIGHT_PURPLE+"");
		message = message.replace("&e", ChatColor.YELLOW+"");
		message = message.replace("&f", ChatColor.WHITE+"");
		message = message.replace("&k", ChatColor.MAGIC+"");
		message = message.replace("&l", ChatColor.BOLD+"");
		message = message.replace("&m", ChatColor.STRIKETHROUGH+"");
		message = message.replace("&n", ChatColor.UNDERLINE+"");
		message = message.replace("&o", ChatColor.ITALIC+"");
		message = message.replace("&r", ChatColor.RESET+"");
		return message;
	}
	

}
