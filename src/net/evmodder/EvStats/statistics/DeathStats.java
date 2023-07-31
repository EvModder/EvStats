package net.evmodder.EvStats.statistics;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import net.evmodder.EvStats.EvStatsMain;

/**
* @author EvModder/EvDoc (evdoc at altcraft.net)
*/
public class DeathStats{
	final String PREFIX;

	private Method findMethod(Class<?> clazz, String name){
		for(Method m : clazz.getDeclaredMethods()) if(m.getName().equals(name)) return m;
		return null;
	}

	public DeathStats(EvStatsMain pl){
		PREFIX = pl.getConfig().getString("death-scoreboard-prefix", "dstats-");
		pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"deaths", Criteria.DUMMY, pl.loadTranslationComp("item-statistics.fire"));

		final Objective deathObj = pl.getServer().getScoreboardManager().getMainScoreboard().getObjective(PREFIX+"deaths");
		try{
			pl.getLogger().info("looking for TranslatableComponent class");
			Class<?> clazz = Class.forName("net.kyori.adventure.text.TranslatableComponent");
			pl.getLogger().info("looking for TranslatableComponent.key() method");
			Method keyMethod = findMethod(clazz, "key");
			pl.getLogger().info("looking for PlayerDeathEvent.deathMessage() method");
			Method deathMessageMethod = findMethod(PlayerDeathEvent.class, "deathMessage");
			pl.getLogger().info("registering listener");
			pl.getServer().getPluginManager().registerEvents(new Listener(){
				@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
				public void onPlayerDeath(PlayerDeathEvent evt){
					try{
						String deathMsgKey = (String)keyMethod.invoke(deathMessageMethod.invoke(evt));
						pl.getLogger().info("death msg key: "+deathMsgKey);
						final Score newScoreObject = deathObj.getScore(deathMsgKey);
						newScoreObject.setScore((newScoreObject.isScoreSet() ? newScoreObject.getScore() : 0) + 1);
					}
					catch(IllegalAccessException | InvocationTargetException e){e.printStackTrace();}
				}
			}, pl);
		}
		catch(ClassNotFoundException e){}
	}
}
