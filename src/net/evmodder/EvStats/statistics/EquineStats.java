package net.evmodder.EvStats.statistics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import net.evmodder.EvLib.extras.TellrawUtils.TranslationComponent;
import net.evmodder.EvStats.EvStatsMain;
import net.evmodder.HorseOwners.HorseUtils;
import net.evmodder.HorseOwners.api.events.HorseClaimEvent;
import net.evmodder.HorseOwners.api.events.HorseDeathEvent;
import net.evmodder.HorseOwners.api.events.HorseRenameEvent;

/**
* @author EvModder/EvDoc (evdoc at altcraft.net)
*/
public class EquineStats{
	private final EvStatsMain pl;
	private final ArrayList<Objective> horseObjectives;
	final String PREFIX;
	
	private void updateHorseScoreboard(AbstractHorse horse, String name){
		pl.getLogger().fine("Adding horse scoareboard statistics for '"+name+"'");
		final Scoreboard board = pl.getServer().getScoreboardManager().getMainScoreboard();
		switch(horse.getType()){
			case HORSE:
			case DONKEY:
			case MULE:
			case LLAMA:
				final String horseTypeL = horse.getType().name().toLowerCase();
				board.getObjective(PREFIX+horseTypeL+"-speed").getScore(name).setScore((int)(100*HorseUtils.getNormalSpeed(horse)));
				board.getObjective(PREFIX+horseTypeL+"-jump").getScore(name).setScore((int)(100*HorseUtils.getNormalJump(horse)));
				board.getObjective(PREFIX+horseTypeL+"-health").getScore(name).setScore(HorseUtils.getNormalMaxHealth(horse));
				return;
			case TRADER_LLAMA:
				board.getObjective(PREFIX+"trader_llama-h").getScore(name).setScore(HorseUtils.getNormalMaxHealth(horse));
				return;
			case SKELETON_HORSE:
				board.getObjective(PREFIX+"skeleton_horse-j").getScore(name).setScore((int)(100*HorseUtils.getNormalJump(horse)));
				return;
			default:
		}
	}

	private void renameHorseScoreboard(String oldName, String newName){
		pl.getLogger().fine("Updating horse scoareboard statistics from '"+oldName+"' to '"+newName+"'");
		final HashMap<String, Integer> scoresToKeep = new HashMap<>();
		final HashMap<String, Integer> scoresToMove = new HashMap<>();
		final Scoreboard board = pl.getServer().getScoreboardManager().getMainScoreboard();
		for(Objective objective : board.getObjectives()){
			final Score score = objective.getScore(oldName);
			if(!score.isScoreSet()) continue;
			scoresToKeep.put(objective.getName(), score.getScore());
		}
		for(Objective objective : horseObjectives){
			final Score score = objective.getScore(oldName);
			if(!score.isScoreSet()) continue;
			scoresToKeep.remove(objective.getName());
			if(newName != null) scoresToMove.put(objective.getName(), score.getScore());
		}
		board.resetScores(oldName);
		for(Entry<String, Integer> entry : scoresToKeep.entrySet()){
			board.getObjective(entry.getKey()).getScore(oldName).setScore(entry.getValue());
		}
		for(Entry<String, Integer> entry : scoresToMove.entrySet()){
			board.getObjective(entry.getKey()).getScore(newName).setScore(entry.getValue());
		}
	}

	public EquineStats(EvStatsMain pl){
		this.pl = pl;
		PREFIX = pl.getConfig().getString("horse-scoreboard-prefix", "");
		horseObjectives = new ArrayList<>();
		final Scoreboard board = pl.getServer().getScoreboardManager().getMainScoreboard();
		for(String eNameL : Arrays.asList("horse", "donkey", "mule", "llama")){// TODO: Camels
			final TranslationComponent eNameComp = new TranslationComponent("entity.minecraft."+eNameL);
			pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+eNameL+"-speed", Criteria.DUMMY, pl.loadTranslationComp("equine-statistics.speed", eNameComp));
			pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+eNameL+"-jump", Criteria.DUMMY, pl.loadTranslationComp("equine-statistics.jump", eNameComp));
			pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+eNameL+"-health", Criteria.DUMMY, pl.loadTranslationComp("equine-statistics.health", eNameComp));
			horseObjectives.add(board.getObjective(PREFIX+eNameL+"-speed"));
			horseObjectives.add(board.getObjective(PREFIX+eNameL+"-jump"));
			horseObjectives.add(board.getObjective(PREFIX+eNameL+"-health"));
		}
		// These equines only have a single stat which can vary
		pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"skeleton_horse-jump", Criteria.DUMMY,
				pl.loadTranslationComp("equine-statistics.jump", new TranslationComponent("entity.minecraft.skeleton_horse")));
		pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"trader_llama-health", Criteria.DUMMY,
				pl.loadTranslationComp("equine-statistics.health", new TranslationComponent("entity.minecraft.trader_llama")));
		horseObjectives.add(board.getObjective(PREFIX+"skeleton_horse-jump"));
		horseObjectives.add(board.getObjective(PREFIX+"trader_llama-health"));

		// Register claim/rename/death listeners
		pl.getServer().getPluginManager().registerEvents(new Listener(){
			@EventHandler(ignoreCancelled=true) public void onHorseClaim(HorseClaimEvent evt){
				if(evt.getEntity() instanceof AbstractHorse) updateHorseScoreboard((AbstractHorse) evt.getEntity(), evt.getClaimName());
			}
			@EventHandler(ignoreCancelled=true) public void onHorseRename(HorseRenameEvent evt){
				renameHorseScoreboard(evt.getOldFullName(), evt.getNewFullName());
			}
			@EventHandler(ignoreCancelled=true) public void onHorseDeath(HorseDeathEvent evt){
				renameHorseScoreboard(evt.getEntity().getCustomName(), null);
			}
		}, pl);
	}
}