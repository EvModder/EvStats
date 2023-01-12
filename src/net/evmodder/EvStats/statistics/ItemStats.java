package net.evmodder.EvStats.statistics;

import org.bukkit.World.Environment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import net.evmodder.EvLib.extras.NBTTagUtils;
import net.evmodder.EvStats.EvStatsMain;

/**
* @author EvModder/EvDoc (evdoc at altcraft.net)
*/
public class ItemStats{
	final String PREFIX;
	
	public ItemStats(EvStatsMain pl){
		Scoreboard board = pl.getServer().getScoreboardManager().getMainScoreboard();
		PREFIX = pl.getConfig().getString("items-scoreboard-prefix", "istats-");
		pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"fire", Criteria.DUMMY, pl.loadTranslationComp("item-statistics.fire"));
		pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"lava", Criteria.DUMMY, pl.loadTranslationComp("item-statistics.lava"));
		pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"void", Criteria.DUMMY, pl.loadTranslationComp("item-statistics.void"));
		pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"cactus", Criteria.DUMMY, pl.loadTranslationComp("item-statistics.cactus"));
		pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"despawn", Criteria.DUMMY, pl.loadTranslationComp("item-statistics.despawn"));
		pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"explosion", Criteria.DUMMY, pl.loadTranslationComp("item-statistics.explosion"));
		pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"lightning", Criteria.DUMMY, pl.loadTranslationComp("item-statistics.lightning"));
		pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"anvil", Criteria.DUMMY, pl.loadTranslationComp("item-statistics.anvil"));
//		pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"unknown", Criteria.DUMMY, pl.loadTranslationComp("item-statistics.unknown"));

		
		pl.getServer().getPluginManager().registerEvents(new Listener(){
			void incrDeathScore(String statName, ItemStack item){
				final String matNameL = item.getType().name().toLowerCase();
				Score newScoreObject = board.getObjective(statName).getScore(matNameL);
				newScoreObject.setScore((newScoreObject.isScoreSet() ? newScoreObject.getScore() : 0) + item.getAmount());
			}
			String getObjectiveNameFromDamageCause(DamageCause cause){
				switch(cause){
					case CONTACT: return "cactus";
					//NOTE: Sadly, items < y -63 instantly get deleted without any damage event
					//https://www.spigotmc.org/threads/detection-of-item-falls-into-the-void.282496/
					case VOID: return "void";
					case LAVA: return "lava";
					case FIRE: case FIRE_TICK: return "fire";
					case ENTITY_EXPLOSION: case BLOCK_EXPLOSION: return "explosion";
					case LIGHTNING: return "lightning";
					case FALLING_BLOCK: case ENTITY_ATTACK/*1.19+*/: return "anvil";
					default: {
						pl.getLogger().severe("Unexpected damage cause for item entity: "+cause.name());
						return "unknown";
					}
				}
			}
			{//"constructor"
				try{
					@SuppressWarnings("unchecked")
					Class<? extends Event> clazz = (Class<? extends Event>)
						Class.forName("com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent");
					pl.getServer().getPluginManager().registerEvent(clazz, this, EventPriority.MONITOR, new EventExecutor(){
						@Override public void execute(Listener listener, Event event){
							//pl.getLogger().info("entity remove from world event");
							Entity entity = ((EntityEvent)event).getEntity();
							if(entity instanceof Item && entity.getLocation().getY() <
									 (entity.getWorld().getEnvironment() == Environment.NORMAL ? -127 : -63)){
								//TODO: && !event.isCancelled()?
								//pl.getLogger().info("item < critical y lvl: "+((Item)entity).getItemStack().getType());
								incrDeathScore(PREFIX+"void", ((Item)entity).getItemStack());
							}
						}
					}, pl);
				}
				catch(ClassNotFoundException e){}
				catch(IllegalStateException e){pl.getLogger().warning("reload issue?: "); e.printStackTrace();}
				
			}
			@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
			public void itemDespawnEvent(ItemDespawnEvent evt){
//				pl.getLogger().info("Item Despawn: "+TextUtils.locationToString(evt.getEntity().getLocation())+", type: "+evt.getEntity().getItemStack().getType().name().toLowerCase());
				incrDeathScore(PREFIX+"despawn", evt.getEntity().getItemStack());
				evt.getEntity().remove();
			}
			@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
			public void onItemMiscDamage(EntityDamageEvent evt){
				if(evt.getEntity() instanceof Item){
					//pl.getLogger().info("Item damage event: "+evt.getCause().name()/+" (item cur health: "+NBTTagUtils.getTag(evt.getEntity()).getShort("Health")+")");
					if(NBTTagUtils.getTag(evt.getEntity()).getShort("Health") <= evt.getFinalDamage()){
						incrDeathScore(PREFIX+getObjectiveNameFromDamageCause(evt.getCause()), ((Item)evt.getEntity()).getItemStack());
						evt.getEntity().remove();
					}
				}
			}
		}, pl);
	}
}
