package net.evmodder.EvStats.statistics;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import net.evmodder.EvStats.EvStatsMain;

/**
* @author EvModder/EvDoc (evdoc at altcraft.net)
*/
public class ChatStats{
	public ChatStats(EvStatsMain pl){
		final String PREFIX = pl.getConfig().getString("chat-scoreboard-prefix", "pstats-");
		final String OBJ_NAME = PREFIX + "chats";
		pl.registerObjectiveIfDoesNotExist5sDelay(OBJ_NAME, Criteria.DUMMY, pl.loadTranslationComp("chat-scoreboard-name"));

		// Chat listener
		pl.getServer().getPluginManager().registerEvents(new Listener(){
			final Objective chatObjective = pl.getServer().getScoreboardManager().getMainScoreboard().getObjective(OBJ_NAME);
			@EventHandler(ignoreCancelled = true) public void onPlayerChat(AsyncPlayerChatEvent evt){
				final Score score = chatObjective.getScore(evt.getPlayer().getName());
				score.setScore((score.isScoreSet() ? score.getScore() : 0) + 1);
			}
		}, pl);
	}
}
