package net.evmodder.EvStats.statistics;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import net.evmodder.EvStats.EvStatsMain;

/**
* @author EvModder/EvDoc (evdoc at altcraft.net)
*/
public class CommandStats{
	public CommandStats(EvStatsMain pl){
		final String PREFIX = pl.getConfig().getString("command-scoreboard-prefix", "zstats-");
		final String OBJ_NAME = PREFIX + "commands";
		pl.registerObjectiveIfDoesNotExist5sDelay(OBJ_NAME, Criteria.DUMMY, pl.loadTranslationComp("command-scoreboard-name"));

		// Command listener
		pl.getServer().getPluginManager().registerEvents(new Listener(){
			final Objective cmdObjective = pl.getServer().getScoreboardManager().getMainScoreboard().getObjective(OBJ_NAME);
			@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
			public void onPlayerSendCommand(PlayerCommandPreprocessEvent evt){
				final Score score = cmdObjective.getScore(evt.getPlayer().getName());
				score.setScore((score.isScoreSet() ? score.getScore() : 0) + 1);
			}
		}, pl);
	}
}