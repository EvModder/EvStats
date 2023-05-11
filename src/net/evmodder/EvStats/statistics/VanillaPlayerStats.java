package net.evmodder.EvStats.statistics;

import java.util.Arrays;
import java.util.HashSet;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.scoreboard.Criteria;
import net.evmodder.EvLib.extras.TellrawUtils.RawTextComponent;
import net.evmodder.EvLib.extras.TellrawUtils.TranslationComponent;
import net.evmodder.EvStats.EvStatsMain;

/**
* @author EvModder/EvDoc (evdoc at altcraft.net)
*/
public class VanillaPlayerStats{
	public VanillaPlayerStats(EvStatsMain pl){
		final String PREFIX = pl.getConfig().getString("vanilla-statistics-scoreboard-prefix", "pstats-");
		if(pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"dummy", Criteria.DUMMY, new RawTextComponent("Test"))){
			pl.getLogger().info("Registering all vanilla statistics as scoreboards (may take a minute)...");

			// Simple stats (excluding trigger & dummy)
			// NOTE: "deathCount" and "deaths" are just duplicate stats for the same thing so why register both.
			// Same for "playerKillCount" and "player_kills"
			for(String simpleStat : Arrays.asList("air", "armor",/* "deathCount",*/ "food", "health", "level",/* "playerKillCount",*/ "totalKillCount", "xp")){
				pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+simpleStat, simpleStat, pl.loadTranslationComp("vanilla-statistics."+simpleStat));
			}
			// "minecraft.custom:minecraft.<custom_stat>"
			for(String customStat : Arrays.asList(
					"animals_bred", "aviate_one_cm", "bell_ring", "boat_one_cm", "clean_armor", "clean_banner", "clean_shulker_box",
					"climb_one_cm", "crouch_one_cm", "damage_absorbed", "damage_blocked_by_shield", "damage_dealt", "damage_dealt_absorbed",
					"damage_dealt_resisted", "damage_resisted", "damage_taken", "deaths", "drop", "eat_cake_slice", "enchant_item",
					"fall_one_cm", "fill_cauldron", "fish_caught", "fly_one_cm", "horse_one_cm", "inspect_dispenser", "inspect_dropper",
					"inspect_hoppers", "interact_with_anvil", "interact_with_beacon", "interact_with_blast_furnace", "interact_with_brewingstand",
					"interact_with_campfire", "interact_with_cartography_table", "interact_with_crafting_table", "interact_with_furnace",
					"interact_with_grindstone", "interact_with_lectern", "interact_with_loom", "interact_with_smithing_table", "interact_with_smoker",
					"interact_with_stonecutter", "jump", "leave_game", "minecart_one_cm", "mob_kills", "open_barrel", "open_chest", "open_enderchest",
					"open_shulker_box", "pig_one_cm", "play_noteblock", "play_record", "play_time", "player_kills", "pot_flower", "raid_trigger",
					"raid_win", "sleep_in_bed", "sneak_time", "sprint_one_cm", "strider_one_cm", "swim_one_cm", "talked_to_villager", "target_hit",
					"time_since_death", "time_since_rest", "total_world_time", "traded_with_villager", "trigger_trapped_chest", "tune_noteblock",
					"use_cauldron", "walk_on_water_one_cm", "walk_one_cm", "walk_under_water_one_cm"
			)){
				pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+customStat, "minecraft.custom:minecraft."+customStat,
						new TranslationComponent("stat.minecraft."+customStat));
			}
			// "killedByTeam.<color>", "teamkill.<color>"
			for(ChatColor color : ChatColor.values()){
				if(!color.isColor()) continue;
				final String criteria1 = "killedByTeam."+color.name().toLowerCase();
				pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+criteria1, criteria1, pl.loadTranslationComp("vanilla-statistics."+criteria1));
				final String criteria2 = "teamkill."+color.name().toLowerCase();
				pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+criteria2, criteria2, pl.loadTranslationComp("vanilla-statistics."+criteria2));
			}
			for(EntityType e : EntityType.values()){
				final String entNameL = e.name().toLowerCase();
				final TranslationComponent entNameComp = new TranslationComponent("entity.minecraft."+entNameL);
				
				pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"killed_"+entNameL, "minecraft.killed:minecraft."+entNameL,
						pl.loadTranslationComp("vanilla-statistics.killed", entNameComp));
				pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"killed_by_"+entNameL, "minecraft.killed_by:minecraft."+entNameL,
						pl.loadTranslationComp("vanilla-statistics.killed_by", entNameComp));
			}
			for(Material mat : Material.values()){
				final String matNameL = mat.name().toLowerCase();
				final TranslationComponent matNameComp = new TranslationComponent((mat.isBlock() ? "block" : "item")+".minecraft."+matNameL);
				if(mat.getMaxDurability() > 0){
					final String criteria = "minecraft.broken:minecraft."+matNameL; // stat_type.minecraft.broken
					pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+criteria, criteria, pl.loadTranslationComp("vanilla-statistics.broken", matNameComp));
				}
				pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"dropped_"+matNameL, "minecraft.dropped:minecraft."+matNameL,
						pl.loadTranslationComp("vanilla-statistics.dropped", matNameComp));
				pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"picked_up_"+matNameL, "minecraft.picked_up:minecraft."+matNameL,
						pl.loadTranslationComp("vanilla-statistics.picked_up", matNameComp));
				pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"used_"+matNameL, "minecraft.used:minecraft."+matNameL,
						pl.loadTranslationComp("vanilla-statistics.used", matNameComp));
				if(mat.isBlock()){
					pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"mined_"+matNameL, "minecraft.mined:minecraft."+matNameL,
							pl.loadTranslationComp("vanilla-statistics.mined", matNameComp));
				}
			}
			final HashSet<Material> alreadyAdded = new HashSet<>();
			pl.getServer().recipeIterator().forEachRemaining(r -> {
				if(alreadyAdded.add(r.getResult().getType())){
					final String matNameL = r.getResult().getType().name().toLowerCase();
					final TranslationComponent matNameComp = new TranslationComponent(
							(r.getResult().getType().isBlock() ? "block" : "item")+".minecraft."+matNameL);
					pl.registerObjectiveIfDoesNotExist5sDelay(PREFIX+"crafted_"+matNameL, "minecraft.crafted:minecraft."+matNameL,
							pl.loadTranslationComp("vanilla-statistics.crafted", matNameComp));
				}
			});
			pl.getLogger().info("Vanilla scoreboards registered");
		}//</end> if dummy register succeeded
	}
}