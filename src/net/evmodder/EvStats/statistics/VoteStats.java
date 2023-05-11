package net.evmodder.EvStats.statistics;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import com.vexsoftware.votifier.model.VotifierEvent;
import net.evmodder.EvStats.EvStatsMain;

/**
* @author EvModder/EvDoc (evdoc at altcraft.net)
*/
public class VoteStats{
	public VoteStats(EvStatsMain pl){
		final String PREFIX = pl.getConfig().getString("vote-scoreboard-prefix", "pstats-");
		final String OBJ_NAME = PREFIX + "votes";
		pl.registerObjectiveIfDoesNotExist5sDelay(OBJ_NAME, Criteria.DUMMY, pl.loadTranslationComp("vote-scoreboard-name"));

		// Vote listener
		pl.getServer().getPluginManager().registerEvents(new Listener(){
			final Objective voteObjective = pl.getServer().getScoreboardManager().getMainScoreboard().getObjective(OBJ_NAME);
			@EventHandler public void vote(VotifierEvent evt){
				if(evt.getVote().getUsername() == null || evt.getVote().getUsername().isEmpty()) {
					pl.getLogger().warning("No username given when voting from "+evt.getVote().getServiceName());
					return;
				}
//				@SuppressWarnings("deprecation")
//				OfflinePlayer voter = pl.getServer().getOfflinePlayer(evt.getVote().getUsername());
//				if(voter == null || !voter.hasPlayedBefore()){
//					pl.getLogger().warning("Unknown Voting Player: " + evt.getVote().getUsername());
//					return;
//				}
//				String voteSite = evt.getVote().getServiceName();
//				pl.getLogger().fine("Player "+voter.getName()+" voted for the server on "+voteSite);
//				if(evt.getVote().getAddress() == null || evt.getVote().getAddress().isEmpty()) {
//					pl.getLogger().info("An address was not given when voting from "+voteSite);
//				}
				final Score score = voteObjective.getScore(evt.getVote().getUsername());
				score.setScore((score.isScoreSet() ? score.getScore() : 0) + 1);
			}
		}, pl);
	}
}