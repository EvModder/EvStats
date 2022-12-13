package net.evmodder.EvStats;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import com.google.common.collect.ImmutableList;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.ChatColor;
import net.evmodder.EvLib.EvCommand;
import net.evmodder.EvLib.extras.ReflectionUtils;
import net.evmodder.EvLib.extras.ReflectionUtils.RefClass;
import net.evmodder.EvLib.extras.ReflectionUtils.RefMethod;

/**
* @author EvModder/EvDoc (evdoc at altcraft.net)
*/
public class CommandStat extends EvCommand{
	private final Plugin pl;
	private final ScoreboardManager boardManager;
	private final HashMap<Objective, Scoreboard> perObjScoreboards;
	private final HashMap<UUID, BukkitRunnable> activeTempDisplays;
	private final Object nmsMainBoard;
	private final RefMethod methodGetObjective, methodGetScore;
	@SuppressWarnings("rawtypes")
	private Set playerScores;//EDIT: Nvm this all all BS, it will be horribly expensive regardless; it appears Minecraft internally does not sort.

	private final Listener joinListener = new Listener(){
		//TODO: Instead of listening to Join/Quit, we should just listen to ScoreboardUpdateEvent (if/when it gets added)
		@EventHandler public void onPlayerJoin(PlayerJoinEvent evt){
			final Objective mainBelowName = boardManager.getMainScoreboard().getObjective(DisplaySlot.BELOW_NAME);
			if(mainBelowName != null && !mainBelowName.getScore(evt.getPlayer().getName()).isScoreSet()){
				final int score = mainBelowName.getScore(evt.getPlayer().getName()).getScore();
				for(final Scoreboard sb : perObjScoreboards.values()){
					final Objective belowName = sb.getObjective(DisplaySlot.BELOW_NAME);
					if(belowName == null){pl.getLogger().warning("Missing objective on temp scoreboard: "+mainBelowName.getName()); continue;}
					belowName.getScore(evt.getPlayer().getName()).setScore(score);
				}
			}
			final Objective mainPlayerList = boardManager.getMainScoreboard().getObjective(DisplaySlot.PLAYER_LIST);
			if(mainPlayerList != null && !mainPlayerList.getScore(evt.getPlayer().getName()).isScoreSet()){
				final int score = mainPlayerList.getScore(evt.getPlayer().getName()).getScore();
				for(final Scoreboard sb : perObjScoreboards.values()){
					final Objective playerList = sb.getObjective(DisplaySlot.BELOW_NAME);
					if(playerList == null){pl.getLogger().warning("Missing objective on temp scoreboard: "+mainBelowName.getName()); continue;}
					playerList.getScore(evt.getPlayer().getName()).setScore(score);
				}
			}
		}
		// Actually, we don't need this
		//@EventHandler public void onPlayerQuit(PlayerQuitEvent evt){}
	};
	private void registerVisibleObjectivesListener(Scoreboard sb){
		final Objective mainBelowName = boardManager.getMainScoreboard().getObjective(DisplaySlot.BELOW_NAME);
		final Objective mainPlayerList = boardManager.getMainScoreboard().getObjective(DisplaySlot.PLAYER_LIST);
		if(mainBelowName == null && mainPlayerList == null) return;
		if(mainBelowName != null && sb.getObjective(DisplaySlot.BELOW_NAME) == null){
			final Objective belowName = sb.registerNewObjective(
					mainBelowName.getName(), mainBelowName.getTrackedCriteria(), mainBelowName.getDisplayName());
			for(Player player : pl.getServer().getOnlinePlayers()){
				final Score score = mainBelowName.getScore(player.getName());
				if(score.isScoreSet()) belowName.getScore(player.getName()).setScore(score.getScore());
			}
		}
		if(mainPlayerList != null && sb.getObjective(DisplaySlot.PLAYER_LIST) == null){
			final Objective playerList = sb.registerNewObjective(
					mainPlayerList.getName(), mainPlayerList.getTrackedCriteria(), mainPlayerList.getDisplayName());
			for(Player player : pl.getServer().getOnlinePlayers()){
				final Score score = mainPlayerList.getScore(player.getName());
				if(score.isScoreSet()) playerList.getScore(player.getName()).setScore(score.getScore());
			}
		}
		// If the listener isn't already registered, register it.
		if(activeTempDisplays.isEmpty()) pl.getServer().getPluginManager().registerEvents(joinListener, pl);
	}

	public CommandStat(JavaPlugin p){
		super(p); pl = p;
		boardManager = p.getServer().getScoreboardManager();
		perObjScoreboards = new HashMap<>();
		activeTempDisplays = new HashMap<>();
		RefMethod methodGetHandle = ReflectionUtils.getRefClass("{cb}.scoreboard.CraftScoreboard").getMethod("getHandle");
		RefClass classScoreboard = ReflectionUtils.getRefClass("{nm}.world.scores.Scoreboard");
		methodGetObjective = classScoreboard.findMethod(/*isStatic=*/false,
				ReflectionUtils.getRefClass("{nm}.world.scores.ScoreboardObjective"), String.class);
		methodGetScore = ReflectionUtils.getRefClass("{nm}.world.scores.ScoreboardScore").findMethod(/*isStatic=*/false, int.class);
		nmsMainBoard = methodGetHandle.of(boardManager.getMainScoreboard()).call();
		for(Field f : classScoreboard.getRealClass().getDeclaredFields()){
			if(!f.getType().equals(Map.class)) continue;
			f.setAccessible(true);
			try{
				@SuppressWarnings("rawtypes")
				Map map = (Map)f.get(nmsMainBoard);
				if(map.isEmpty()) continue;
//				System.out.println("map is not empty");
				Object value = map.values().iterator().next();
				if(value instanceof Map == false) continue;
//				System.out.println("map value is a map");
				@SuppressWarnings("rawtypes")
				Map subMap = (Map)value;
				if(subMap.isEmpty()) continue;
//				System.out.println("map value is not empty");
				if(!ReflectionUtils.getRefClass("{nm}.world.scores.ScoreboardObjective").getRealClass()
						.equals(subMap.keySet().iterator().next().getClass())) continue;
//				System.out.println("map value key is ScoreboardObjective");
				if(!ReflectionUtils.getRefClass("{nm}.world.scores.ScoreboardScore").getRealClass()
						.equals(subMap.values().iterator().next().getClass())) continue;
//				System.out.println("map value value is ScoreboardScore");
				playerScores = map.entrySet();
			}
			catch(IllegalArgumentException | IllegalAccessException e){e.printStackTrace();}
		}
	}

	private static List<String> statNames = null;
	@Override public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args){
		if(args.length != 1) return ImmutableList.of(); //TODO: 2nd arg for display time in seconds (or toggle?)
		if(args[0].startsWith("zstats-")/* || args[0].startsWith("istats-")*/) args[0] = args[0].substring(7);
		if(statNames == null){
			statNames = boardManager.getMainScoreboard().getObjectives().stream()
					.map(obj -> {
						String objName = obj.getName();
						if(objName.startsWith("zstats-")/* || objName.startsWith("istats-")*/) objName = objName.substring(7);
						return objName;
					})
					.toList();
		}
		// TODO: Prefix tree would be more optimal
		int sharedPrefixLen = args[0].length();
		while(true){
			HashMap<String, String> tabCompletes = new HashMap<String, String>();
			for(String statName : statNames){
				if(!statName.startsWith(args[0])) continue;
				final int currPartEnd = statName.indexOf('_', sharedPrefixLen + 1);
//				pl.getLogger().info("currPartEnd: "+currPartEnd);
				final String tabComplete = statName.substring(0, currPartEnd == -1 ? statName.length() : currPartEnd+1);
//				pl.getLogger().info("obj: "+statName+", tabc: "+tabComplete);
				if(tabCompletes.put(tabComplete, statName) != null) tabCompletes.put(tabComplete, tabComplete);
			}
			if(tabCompletes.size() != 1 || !tabCompletes.values().iterator().next().endsWith("_")) return tabCompletes.values().stream().toList();
			sharedPrefixLen += tabCompletes.values().iterator().next().length();
		}
	}

	private boolean showObjective(Player player, Objective obj, long seconds){
		Scoreboard sb = perObjScoreboards.get(obj);
		if(sb == null){
			perObjScoreboards.put(obj, sb = boardManager.getNewScoreboard());
			sb.registerNewObjective(obj.getName(), obj.getTrackedCriteria(), obj.getDisplayName());
			Objective objCopy = sb.getObjective(obj.getName());
			Object nmsObj = methodGetObjective.of(nmsMainBoard).call(obj.getName());
			System.out.println("hmmmm: "+playerScores.size());/////////////////////////////////////
			int i=0;
			for(Object entry : playerScores){
				@SuppressWarnings("rawtypes") String entryName = (String)((java.util.Map.Entry)entry).getKey();
				@SuppressWarnings("rawtypes") Map nmsObjToScore = (Map)((java.util.Map.Entry)entry).getValue();
				Object nmsScore = nmsObjToScore.get(nmsObj);
				if(nmsScore != null){
					objCopy.getScore(entryName).setScore((int)methodGetScore.of(nmsScore).call());
//					System.out.println("added score for "+entryName+": "+(int)methodGetScore.of(nmsScore).call());
				}
				if(++i == 10) break;
			}
			objCopy.setDisplaySlot(DisplaySlot.SIDEBAR);
			pl.getLogger().info("all scores of caller on sb: "+sb.getScores(player.getName()).size());
		}

		player.setScoreboard(perObjScoreboards.get(obj));
		final UUID uuid = player.getUniqueId();
		final BukkitRunnable oldTask = activeTempDisplays.get(uuid);
		if(oldTask != null) oldTask.cancel();
		final BukkitRunnable newTask = new BukkitRunnable(){@Override public void run(){
			Player player = pl.getServer().getPlayer(uuid);
			if(player != null) player.setScoreboard(boardManager.getMainScoreboard());
			activeTempDisplays.remove(uuid);
			if(activeTempDisplays.isEmpty()) HandlerList.unregisterAll(joinListener);
		}};
		newTask.runTaskLater(pl, seconds*20L);
		registerVisibleObjectivesListener(sb);
		activeTempDisplays.put(uuid, newTask);
		return true;
	}

	@Override public boolean onCommand(CommandSender sender, Command command, String label, String args[]){
		if(sender instanceof Player == false){
			sender.sendMessage(ChatColor.RED+"This command can only be run by in-game players!");
			return true;
		}
		if(args.length == 0){
			// Clear currently displayed statistic
			final BukkitRunnable oldTask = activeTempDisplays.remove(((Player)sender).getUniqueId());
			if(oldTask != null){
				oldTask.cancel();
				((Player)sender).setScoreboard(boardManager.getMainScoreboard());
				return true;
			}
			else{
				return false;
			}
		}
		Objective obj = boardManager.getMainScoreboard().getObjective(args[0]);
		if(obj == null) obj = boardManager.getMainScoreboard().getObjective("zstats-"+args[0]);
		if(obj == null) obj = boardManager.getMainScoreboard().getObjective("istats-"+args[0]);
		if(obj == null){
			sender.sendMessage(ChatColor.RED+"Unknown statistic: "+args[0]);
			return true;
		}
		final long seconds = args.length > 1 && args[1].matches("\\d+") ? Long.parseLong(args[1]) : 8;
		showObjective((Player)sender, obj, seconds);
		return true;
	}
}