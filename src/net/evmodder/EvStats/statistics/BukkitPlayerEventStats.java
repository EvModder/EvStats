package net.evmodder.EvStats.statistics;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import net.evmodder.EvStats.EvStatsMain;

/**
* @author EvModder/EvDoc (evdoc at altcraft.net)
*/
public class BukkitPlayerEventStats{
	private void incrementScore(String who, Objective obj){
		final Score score = obj.getScore(who);
		score.setScore((score.isScoreSet() ? score.getScore() : 0) + 1);
	}

	public BukkitPlayerEventStats(EvStatsMain pl){
		final String PREFIX = pl.getConfig().getString("bukkit-player-events-scoreboard-prefix", "pstats-");
		pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"interact_entity", Criteria.DUMMY, pl.loadTranslationComp("bukkit-player-events.interact_entity"));
		pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"interact_block", Criteria.DUMMY, pl.loadTranslationComp("bukkit-player-events.interact_block"));
		pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"mined", Criteria.DUMMY, pl.loadTranslationComp("bukkit-player-events.mined"));
		pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"placed", Criteria.DUMMY, pl.loadTranslationComp("bukkit-player-events.placed"));
		pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"broken", Criteria.DUMMY, pl.loadTranslationComp("bukkit-player-events.broken"));
		// Note: "Dropped Items" aggregate already exists in vanilla stats, but not pick(ed)_up.
		pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"pick_up", Criteria.DUMMY, pl.loadTranslationComp("bukkit-player-events.picked_up"));
		pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"consumed", Criteria.DUMMY, pl.loadTranslationComp("bukkit-player-events.consumed"));
		pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"crafted", Criteria.DUMMY, pl.loadTranslationComp("bukkit-player-events.crafted"));
		pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"harvested", Criteria.DUMMY, pl.loadTranslationComp("bukkit-player-events.harvested"));
		pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"loot_generated", Criteria.DUMMY, pl.loadTranslationComp("bukkit-player-events.loot_generated"));
		pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"brushed", Criteria.DUMMY, pl.loadTranslationComp("bukkit-player-events.brushed"));

		// Register listener for various PlayerEvents
		pl.getServer().getPluginManager().registerEvents(new Listener(){
			final Objective interact_entityObj = pl.getServer().getScoreboardManager().getMainScoreboard().getObjective(PREFIX+"interact_entity");
			final Objective interact_blockObj = pl.getServer().getScoreboardManager().getMainScoreboard().getObjective(PREFIX+"interact_block");
			final Objective minedObj = pl.getServer().getScoreboardManager().getMainScoreboard().getObjective(PREFIX+"mined");
			final Objective placedObj = pl.getServer().getScoreboardManager().getMainScoreboard().getObjective(PREFIX+"placed");
			final Objective brokenObj = pl.getServer().getScoreboardManager().getMainScoreboard().getObjective(PREFIX+"broken");
			final Objective pick_upObj = pl.getServer().getScoreboardManager().getMainScoreboard().getObjective(PREFIX+"pick_up");
			final Objective consumedObj = pl.getServer().getScoreboardManager().getMainScoreboard().getObjective(PREFIX+"consumed");
			final Objective craftedObj = pl.getServer().getScoreboardManager().getMainScoreboard().getObjective(PREFIX+"crafted");
			final Objective harvestedObj = pl.getServer().getScoreboardManager().getMainScoreboard().getObjective(PREFIX+"harvested");
			final Objective loot_generatedObj = pl.getServer().getScoreboardManager().getMainScoreboard().getObjective(PREFIX+"loot_generated");
			final Objective brushedObj = pl.getServer().getScoreboardManager().getMainScoreboard().getObjective(PREFIX+"brushed");
			@EventHandler(ignoreCancelled = true) public void onEntityRightClick(PlayerInteractEntityEvent evt){
				incrementScore(evt.getPlayer().getName(), interact_entityObj);
			}
			@EventHandler(ignoreCancelled = true) public void onBlockRightClick(PlayerInteractEvent evt){
				if(evt.getAction() == Action.RIGHT_CLICK_BLOCK) incrementScore(evt.getPlayer().getName(), interact_blockObj);
			}
			@EventHandler(ignoreCancelled = true) public void onBlockMine(BlockBreakEvent evt){
				incrementScore(evt.getPlayer().getName(), minedObj);
			}
			@EventHandler(ignoreCancelled = true) public void onBlockPlace(BlockPlaceEvent evt){
				incrementScore(evt.getPlayer().getName(), placedObj);
			}
			@EventHandler(ignoreCancelled = true) public void onToolBreak(PlayerItemBreakEvent evt){
				incrementScore(evt.getPlayer().getName(), brokenObj);
			}
			@EventHandler(ignoreCancelled = true) public void onItemPickup(EntityPickupItemEvent evt){
				if(evt.getEntityType() == EntityType.PLAYER) incrementScore(evt.getEntity().getName(), pick_upObj);
			}
			@EventHandler(ignoreCancelled = true) public void onItemConsume(PlayerItemConsumeEvent evt){
				incrementScore(evt.getPlayer().getName(), consumedObj);
			}
			@EventHandler(ignoreCancelled = true) public void onItemCraft(CraftItemEvent evt){
				incrementScore(evt.getWhoClicked().getName(), craftedObj);
			}
			@EventHandler(ignoreCancelled = true) public void onBlockHarvest(PlayerHarvestBlockEvent evt){
				incrementScore(evt.getPlayer().getName(), harvestedObj);
			}
			@EventHandler(ignoreCancelled = true) public void onLootGenerated(LootGenerateEvent evt){
				incrementScore(evt.getEntity().getName(), loot_generatedObj);
			}
		}, pl);
	}
}