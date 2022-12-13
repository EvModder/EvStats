package net.evmodder.EvStats.statistics;

import java.lang.reflect.InvocationTargetException;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import net.evmodder.EvLib.extras.TellrawUtils.TranslationComponent;
import net.evmodder.EvStats.EvStatsMain;

/**
* @author EvModder/EvDoc (evdoc at altcraft.net)
*/
public class BeheadStats{
	final String PREFIX;
	final private EvStatsMain pl;

	private void incrementScore(String who, String objectiveName){
		final Objective obj = pl.getServer().getScoreboardManager().getMainScoreboard().getObjective(objectiveName);
		final Score score = obj.getScore(who);
		score.setScore((score.isScoreSet() ? score.getScore() : 0) + 1);
	}
	private void incrementBeheadScore(Entity killer, Entity victim){
		if(killer instanceof Player){
			incrementScore(killer.getName(), PREFIX+"beheaded_"+victim.getType().name().toLowerCase());
			incrementScore(killer.getName(), PREFIX+"beheaded");
		}
	}

	public BeheadStats(EvStatsMain pl){
		this.pl = pl;
		PREFIX = pl.getConfig().getString("behead-scoreboard-prefix", "dstats-");
		// Register behead scoreboards
		if(pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"beheaded", Criteria.DUMMY, pl.loadTranslationComp("behead-scoreboard-name"))){
			pl.getLogger().info("Registering all beheading scoreboards (may take a minute)...");
			for(EntityType e : EntityType.values()){
				final String entNameL = e.name().toLowerCase();
				final TranslationComponent entNameComp = new TranslationComponent("entity.minecraft."+entNameL);
				pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"beheaded_"+entNameL, Criteria.DUMMY,
						pl.loadTranslationComp("behead-scoreboard-name.entity", entNameComp));
			}
		}

		// Determine whether to listen for DH or PH event.
		try{
			@SuppressWarnings("unchecked")
			Class<? extends Event> clazz = (Class<? extends Event>) Class.forName("net.evmodder.DropHeads.events.EntityBeheadEvent");
			pl.getServer().getPluginManager().registerEvent(clazz, new Listener(){}, EventPriority.MONITOR, new EventExecutor(){
				@Override public void execute(Listener listener, Event evt){
					try{
						final Entity killer = (Entity)clazz.getMethod("getKiller").invoke(evt);
						incrementBeheadScore(killer, ((EntityEvent)evt).getEntity());
					}
					catch(IllegalAccessException | InvocationTargetException | NoSuchMethodException | SecurityException e){e.printStackTrace();}
				}
			}, pl);
		}
		catch(ClassNotFoundException e){
			try{
				@SuppressWarnings("unchecked")
				Class<? extends Event> clazz = (Class<? extends Event>) Class.forName("org.shininet.bukkit.playerheads.events.LivingEntityDropHeadEvent");
				pl.getServer().getPluginManager().registerEvent(clazz, new Listener(){}, EventPriority.MONITOR, new EventExecutor(){
					@Override public void execute(Listener listener, Event evt){
						final Entity victim = ((EntityEvent)evt).getEntity();
						if(victim.getLastDamageCause() != null && victim.getLastDamageCause() instanceof EntityDamageByEntityEvent){
							final Entity killer = ((EntityDamageByEntityEvent)victim.getLastDamageCause()).getDamager();
							incrementBeheadScore(killer, victim);
						}
					}
				}, pl);
			}
			catch(ClassNotFoundException e2){}
		}
	}
}