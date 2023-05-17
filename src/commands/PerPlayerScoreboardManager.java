package commands;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
* @author EvModder/EvDoc (evdoc at altcraft.net)
*/
public final class PerPlayerScoreboardManager{
	private final Plugin pl;
	private final Scoreboard mainBoard;

	private final HashMap<Objective, Scoreboard> perObjScoreboards;
	private final HashMap<UUID, Scoreboard> activeTempScoreboards;
	private final HashMap<UUID, BukkitTask> activeTimers;
	
//	private final Object nmsMainBoard;
//	private final RefMethod methodGetObjective, methodGetScore;
//
//	//Set<Entry<String, Map<Objective, Score>>>
//	@SuppressWarnings("rawtypes")
//	private Set playerScores;//EDIT: Nvm this all all BS, it will be horribly expensive regardless; it appears Minecraft internally does not sort.

	public PerPlayerScoreboardManager(JavaPlugin p){
		pl = p;
		mainBoard = pl.getServer().getScoreboardManager().getMainScoreboard();
		perObjScoreboards = new HashMap<>();
		activeTempScoreboards = new HashMap<>();
		activeTimers = new HashMap<>();


//		RefMethod methodGetHandle = ReflectionUtils.getRefClass("{cb}.scoreboard.CraftScoreboard").getMethod("getHandle");
//		RefClass classScoreboard = ReflectionUtils.getRefClass("{nm}.world.scores.Scoreboard");
//		methodGetObjective = classScoreboard.findMethod(/*isStatic=*/false,
//				ReflectionUtils.getRefClass("{nm}.world.scores.ScoreboardObjective"), String.class);
//		methodGetScore = ReflectionUtils.getRefClass("{nm}.world.scores.ScoreboardScore").findMethod(/*isStatic=*/false, int.class);
//		nmsMainBoard = methodGetHandle.of(mainBoard).call();
//		for(Field f : classScoreboard.getRealClass().getDeclaredFields()){
//			if(!f.getType().equals(Map.class)) continue;
//			f.setAccessible(true);
//			try{
//				@SuppressWarnings("rawtypes")
//				Map map = (Map)f.get(nmsMainBoard);
//				if(map.isEmpty()) continue;
////				System.out.println("map is not empty");
//				if(map.keySet().iterator().next() instanceof String == false) continue;
////				System.out.println("map key is a String");
//				Object value = map.values().iterator().next();
//				if(value instanceof Map == false) continue;
////				System.out.println("map value is a map");
//				@SuppressWarnings("rawtypes")
//				Map subMap = (Map)value;
//				if(subMap.isEmpty()) continue;
////				System.out.println("map value is not empty");
//				if(!ReflectionUtils.getRefClass("{nm}.world.scores.ScoreboardObjective").getRealClass()
//						.equals(subMap.keySet().iterator().next().getClass())) continue;
////				System.out.println("map value key is ScoreboardObjective");
//				if(!ReflectionUtils.getRefClass("{nm}.world.scores.ScoreboardScore").getRealClass()
//						.equals(subMap.values().iterator().next().getClass())) continue;
////				System.out.println("map value value is ScoreboardScore");
//				playerScores = map.entrySet();
//			}
//			catch(IllegalArgumentException | IllegalAccessException e){e.printStackTrace();}
//		}
	}

	private void copyVisibleObjectives(Scoreboard sb){
		final Objective mainBelowName = mainBoard.getObjective(DisplaySlot.BELOW_NAME);
		if(mainBelowName == null) sb.clearSlot(DisplaySlot.BELOW_NAME);
		else{
			if(sb.getObjective(mainBelowName.getName()) == null){
				sb.registerNewObjective(mainBelowName.getName(), mainBelowName.getTrackedCriteria(), mainBelowName.getDisplayName());
			}
			final Objective tempBelowMain = sb.getObjective(mainBelowName.getName());
			tempBelowMain.setDisplaySlot(DisplaySlot.BELOW_NAME);
			for(Player player : pl.getServer().getOnlinePlayers()){
				final Score score = mainBelowName.getScore(player.getName());
				if(score.isScoreSet()) tempBelowMain.getScore(player.getName()).setScore(score.getScore());
			}
		}
		final Objective mainPlayerList = mainBoard.getObjective(DisplaySlot.PLAYER_LIST);
		if(mainPlayerList == null) sb.clearSlot(DisplaySlot.PLAYER_LIST);
		else{
			if(sb.getObjective(mainPlayerList.getName()) == null){
				sb.registerNewObjective(mainPlayerList.getName(), mainPlayerList.getTrackedCriteria(), mainPlayerList.getDisplayName());
			}
			final Objective tempPlayerList = sb.getObjective(mainPlayerList.getName());
			tempPlayerList.setDisplaySlot(DisplaySlot.PLAYER_LIST);
			for(Player player : pl.getServer().getOnlinePlayers()){
				final Score score = mainPlayerList.getScore(player.getName());
				if(score.isScoreSet()) tempPlayerList.getScore(player.getName()).setScore(score.getScore());
			}
		}
	}

	// These things only get displayed for online players, so we don't need to bother adding them until players join
	private final Listener joinQuitListener = new Listener(){
		@EventHandler public void onPlayerJoin(PlayerJoinEvent evt){
			// Copy visible objectives
			final Objective mainBelowName = mainBoard.getObjective(DisplaySlot.BELOW_NAME);
			if(mainBelowName != null && !mainBelowName.getScore(evt.getPlayer().getName()).isScoreSet()){
				final int score = mainBelowName.getScore(evt.getPlayer().getName()).getScore();
				for(final Scoreboard sb : activeTempScoreboards.values()){
					final Objective belowName = sb.getObjective(mainBelowName.getName());
					if(belowName == null){pl.getLogger().warning("Missing objective on temp scoreboard: "+mainBelowName.getName()); continue;}
					belowName.getScore(evt.getPlayer().getName()).setScore(score);
				}
			}
			final Objective mainPlayerList = mainBoard.getObjective(DisplaySlot.PLAYER_LIST);
			if(mainPlayerList != null && !mainPlayerList.getScore(evt.getPlayer().getName()).isScoreSet()){
				final int score = mainPlayerList.getScore(evt.getPlayer().getName()).getScore();
				for(final Scoreboard sb : activeTempScoreboards.values()){
					final Objective playerList = sb.getObjective(mainPlayerList.getName());
					if(playerList == null){pl.getLogger().warning("Missing objective on temp scoreboard: "+mainBelowName.getName()); continue;}
					playerList.getScore(evt.getPlayer().getName()).setScore(score);
				}
			}
		}
		@EventHandler public void onPlayerQuit(PlayerQuitEvent evt){
			returnToMainScoreboard(evt.getPlayer());
		}
	};

	private Scoreboard getTempScoreboard(Objective obj){
		Scoreboard tempSB = perObjScoreboards.get(obj);
		if(tempSB == null){
			perObjScoreboards.put(obj, tempSB = pl.getServer().getScoreboardManager().getNewScoreboard());
			tempSB.registerNewTeam("is_obj_sb");
			final Objective objCopy = tempSB.registerNewObjective(obj.getName(), obj.getTrackedCriteria(), obj.getDisplayName());
			objCopy.setDisplaySlot(DisplaySlot.SIDEBAR);
		}

		// NMS method:
//		Object nmsObj = methodGetObjective.of(nmsMainBoard).call(obj.getName());
//	//	int i=0;
//		for(Object entry : playerScores){
//			@SuppressWarnings("rawtypes") String entryName = (String)((java.util.Map.Entry)entry).getKey();
//			@SuppressWarnings("rawtypes") Map nmsObjToScore = (Map)((java.util.Map.Entry)entry).getValue();
//			Object nmsScore = nmsObjToScore.get(nmsObj);
//			if(nmsScore != null){
//				objCopy.getScore(entryName).setScore((int)methodGetScore.of(nmsScore).call());
//	//			System.out.println("added score for "+entryName+": "+(int)methodGetScore.of(nmsScore).call());
//			}
//	//		if(++i == 10) break;
//		}
		// Bukkit method:
		final Objective objCopy = tempSB.getObjective(obj.getName());
		for(final String entry : mainBoard.getEntries()){ // Lagfest
			final Score score = obj.getScore(entry);
			if(score.isScoreSet()) objCopy.getScore(entry).setScore(score.getScore());
		}
		return tempSB;
	}
	
	private Scoreboard getTempScoreboard(UUID viewer, Objective obj, String targetEntry){
		Scoreboard tempSB = activeTempScoreboards.get(viewer);
		if(tempSB == null || tempSB.getTeam("is_obj_sb") != null || tempSB.getObjective(obj.getName()) == null){
			tempSB = pl.getServer().getScoreboardManager().getNewScoreboard();
			final Objective objCopy = tempSB.registerNewObjective(obj.getName(), obj.getTrackedCriteria(), obj.getDisplayName());
			objCopy.setDisplaySlot(DisplaySlot.SIDEBAR);
		}
		// We might need to knock off another entry to make space on the sidebar display
		final Objective objCopy = tempSB.getObjective(obj.getName());
		final List<String> objEntries = tempSB.getEntries().stream().filter(e -> objCopy.getScore(e).isScoreSet()).toList();
		if(objEntries.size() == 15){ // same as >= 15 since it only incrs by 1
			final String minEntry = objEntries.stream().min((a, b) -> Integer.compare(objCopy.getScore(a).getScore(), objCopy.getScore(b).getScore())).get();
			tempSB.resetScores(minEntry);
		}
		final Score score = obj.getScore(targetEntry);
		if(score.isScoreSet()) objCopy.getScore(targetEntry).setScore(score.getScore());
		return tempSB;
	}

	// Runs every 2s when any temp display is active
	private BukkitTask scoreboardUpdateLoopTask;
	private void runScoreboardUpdateLoop(){
		scoreboardUpdateLoopTask = new BukkitRunnable(){
			@Override public void run(){
				for(final Scoreboard sb : activeTempScoreboards.values()){
					final boolean isObjScoreboard = sb.getTeam("is_obj_sb") != null;
					final Objective tempObj = sb.getObjective(DisplaySlot.SIDEBAR);
					final Objective mainObj =  mainBoard.getObjective(tempObj.getName());

					// Copy all scores for obj from main SB to temp SB
					if(isObjScoreboard) for(final String entry : mainBoard.getEntries()){ // Lagfest
						final Score score = mainObj.getScore(entry);
						if(score.isScoreSet()) tempObj.getScore(entry).setScore(score.getScore());
					}
					// Prevent new scores for obj from being copied to temp SB
					else for(final String entry : sb.getEntries()){ // Lagfest
						final Score score = mainObj.getScore(entry);
						final Score scoreCopy = tempObj.getScore(entry);
						if(score.isScoreSet() && scoreCopy.isScoreSet()) scoreCopy.setScore(score.getScore());
					}
					copyVisibleObjectives(sb);
				}
			}
		}.runTaskTimer(pl, 0l, 40l);
	}

	public void returnToMainScoreboard(Player player){
		player.setScoreboard(mainBoard);
		final UUID uuid = player.getUniqueId();
		activeTempScoreboards.remove(uuid);
		final boolean wasEmpty = activeTimers.isEmpty();
		final BukkitTask oldTask = activeTimers.remove(uuid);
		if(oldTask != null) oldTask.cancel();
		if(!wasEmpty && activeTimers.isEmpty()){
			HandlerList.unregisterAll(joinQuitListener);
			scoreboardUpdateLoopTask.cancel();
		}
	}

	// If seconds=null, show indefinitely. If entry=null, show all entries
	public void showTempScoreboard(Player player, Objective obj, Long seconds, String entry){
		if(entry == null) player.setScoreboard(getTempScoreboard(obj));
		else player.setScoreboard(getTempScoreboard(player.getUniqueId(), obj, entry));
		final UUID uuid = player.getUniqueId();
		activeTempScoreboards.put(uuid, player.getScoreboard());  // Also overwrites any previously active obj
		final BukkitTask oldTask = activeTimers.get(uuid);
		if(oldTask != null) oldTask.cancel();

		final BukkitTask newTask = seconds == null ? null : new BukkitRunnable(){@Override public void run(){
			final Player player = pl.getServer().getPlayer(uuid);
			if(player != null) returnToMainScoreboard(player);
		}}.runTaskLater(pl, seconds*20l);

		if(activeTimers.isEmpty()){
			pl.getServer().getPluginManager().registerEvents(joinQuitListener, pl);
			runScoreboardUpdateLoop();
		}
		activeTimers.put(uuid, newTask); // Effectively, an infinite timer
	}
}