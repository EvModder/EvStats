package net.evmodder.EvStats.statistics;

import java.util.HashSet;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;
import net.evmodder.EvLib.EvUtils;
import net.evmodder.EvStats.EvStatsMain;

/**
* @author EvModder/EvDoc (evdoc at altcraft.net)
*/
public class AdvancementStats{
	final String PREFIX;
	private final HashSet<String> ADVANCEMENTS_COUNTED;
	
	private boolean isCountedAdvancement(Advancement adv){
		final int i = adv.getKey().getKey().indexOf('/');
		return i != -1 && adv.getKey().getNamespace().equals(NamespacedKey.MINECRAFT) 
				&& ADVANCEMENTS_COUNTED.contains(adv.getKey().getKey().substring(0, i));
	}
	public AdvancementStats(EvStatsMain pl){
		PREFIX = pl.getConfig().getString("advancements-scoreboard-prefix", "pstats-");
		ADVANCEMENTS_COUNTED = new HashSet<>(pl.getConfig().getStringList("advancements-counted"));
		pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"advancements", Criteria.DUMMY, pl.loadTranslationComp("advancement-scoreboard-name"));

		// Advancement listener
		pl.getServer().getPluginManager().registerEvents(new Listener(){
			final Objective advObjective = pl.getServer().getScoreboardManager().getMainScoreboard().getObjective(PREFIX+"advancements");
			@EventHandler public void onAdvancementGet(PlayerAdvancementDoneEvent evt){
				if(!isCountedAdvancement(evt.getAdvancement())/* || evt.getPlayer().getGameMode() == GameMode.SPECTATOR*/) return;
				final int advancements = EvUtils.getVanillaAdvancements(evt.getPlayer(), ADVANCEMENTS_COUNTED).size();
				advObjective.getScore(evt.getPlayer().getName()).setScore(advancements);
			}
		}, pl);
	}
}