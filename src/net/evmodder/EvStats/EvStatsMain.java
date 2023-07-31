package net.evmodder.EvStats;

import java.util.ArrayList;
import org.bukkit.configuration.Configuration;
import org.bukkit.event.Event;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;
import commands.CommandStat;
import net.evmodder.EvLib.EvPlugin;
import net.evmodder.EvLib.FileIO;
import net.evmodder.EvLib.extras.TextUtils;
import net.evmodder.EvLib.extras.TellrawUtils.Component;
import net.evmodder.EvLib.extras.TellrawUtils.TranslationComponent;
import net.evmodder.EvStats.statistics.*;

/**
* @author EvModder/EvDoc (evdoc at altcraft.net)
*/
public class EvStatsMain extends EvPlugin {
	private Configuration translationsFile;

	private boolean isValidConfigPathChar(char c){return c == '-' || c == '.' || (c >= 'a' && c <= 'z');}

	// Loads config.getString(key), replacing '${abc-xyz}' with % in the key and config.getString('abc-xyz') in withComps.
	public TranslationComponent loadTranslationComp(String key, Component... with){
		//if(!translationsFile.isString(key)) getLogger().severe("Undefined key in translations file: "+key);
		final String msg = TextUtils.translateAlternateColorCodes('&', translationsFile.getString(key, key));
		if(msg.indexOf('$') == -1) return new TranslationComponent(msg, with);

		ArrayList<Component> finalWithComps = new ArrayList<>();
		StringBuilder builder = new StringBuilder();
		int withIdx = 0;
		for(int i=0; i<msg.length(); ++i){
			switch(msg.charAt(i)){
				case '$':
					final int start = i;
					if(++i < msg.length() && msg.charAt(i) == '{'){
						while(++i < msg.length() && isValidConfigPathChar(msg.charAt(i)));
						if(i < msg.length() && msg.charAt(i) == '}'){
							// Special case:
							if(start == 0 && i+1 == msg.length()) return new TranslationComponent(msg.substring(2, i), with);
							finalWithComps.add(loadTranslationComp(msg.substring(start+2, i)));
							builder.append("%s");
							continue;
						}
					}
					builder.append(msg.substring(start, i+1));
					continue;
				case '%':
					if(i+1 == msg.length() || msg.charAt(i+1) != '%') finalWithComps.add(with[withIdx++]);
					else{builder.append('%'); ++i; continue;} // %% is escape of %
				default:
					builder.append(msg.charAt(i));
			}
		}
		if(with != null && withIdx < with.length){
			getLogger().severe("Translation key does not have enough '%' for the number of 'with' arguments!: "+key);
		}
		return new TranslationComponent(builder.toString(), finalWithComps.toArray(new Component[0]));
	}

	private boolean checkExists(String className){
		try{Class.forName(className); return true;}
		catch(ClassNotFoundException e){return false;}
	}
	@SuppressWarnings("unchecked")
	private Class<? extends Event> getEntityRemoveFromWorldEvent(){
		try{return (Class<? extends Event>)Class.forName("com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent");}
		catch(ClassNotFoundException e){return null;}
	}

	@Override public void onEvEnable(){
		//---------- <Load translations> ----------------------------------------------------------------------
		translationsFile = FileIO.loadConfig(this, "translations.yml",
				getClass().getResourceAsStream("/translations.yml"), /*notifyIfNew=*/false);
//		Configuration embeddedTranslationsFile = FileIO.loadConfig(this, "translations-temp-DELETE.yml",
//				getClass().getResourceAsStream("/translations.yml"), /*notifyIfNew=*/false);
//		translationsFile.setDefaults(embeddedTranslationsFile);
//		FileIO.deleteFile("translations-temp-DELETE.yml");

		final Class<? extends Event> clazz = getEntityRemoveFromWorldEvent();

		if(config.getBoolean("add-scoreboards-for-vanilla-statistics", false)) new VanillaPlayerStats(this);
		if(config.getBoolean("add-scoreboards-for-bukkit-player-events", false)) new BukkitPlayerEventStats(this);
		if(config.getBoolean("add-scoreboards-for-items-destroyed", false)) new ItemStats(this, clazz);
		if(config.getBoolean("add-scoreboard-for-death-types", false) && clazz != null) new DeathStats(this);
		if(config.getBoolean("add-scoreboard-for-chats", false)) new ChatStats(this);
		if(config.getBoolean("add-scoreboard-for-commands", false)) new CommandStats(this);
		if(config.getBoolean("add-scoreboard-for-advancements", false)) new AdvancementStats(this);
		if(config.getBoolean("add-scoreboards-for-horse-attributes", false)
				&& checkExists("net.evmodder.HorseOwners.api.events.HorseClaimEvent")) new EquineStats(this);
		if(config.getBoolean("add-scoreboards-for-mobs-beheaded", false) && (
			checkExists("net.evmodder.DropHeads.events.EntityBeheadEvent") ||
			checkExists("org.shininet.bukkit.playerheads.events.LivingEntityDropHeadEvent")
		)){
			new BeheadStats(this);
		}
		if(config.getBoolean("add-scoreboards-for-votes", false) && checkExists("com.vexsoftware.votifier.model.VotifierEvent")) new VoteStats(this);
		new CommandStat(this);

//		if(getServer().getPluginManager().getPlugin("PlaceholderAPI") != null){
//			new ObjectivesExpansion(this).register();
//		}
	}

	@Override public void onEvDisable(){}

	ArrayList<String> addObjectiveCmds = new ArrayList<>();
	// TODO: Make private
	public boolean registerObjectiveIfDoesNotExist5sDelay(String name, String criteria, Component displayName){
		Objective objective = getServer().getScoreboardManager().getMainScoreboard().getObjective(name);
		if(objective != null && objective.getDisplayName().equals(displayName.toPlainText())) return false;

		if(addObjectiveCmds.isEmpty()) new BukkitRunnable(){@Override public void run(){
			for(String cmd : addObjectiveCmds) getServer().dispatchCommand(getServer().getConsoleSender(), cmd);
			addObjectiveCmds.clear();
		}}.runTaskLater(this, 20*5);
		if(objective == null) addObjectiveCmds.add("scoreboard objectives add "+name+" "+criteria+" "+displayName.toString());
		else{
			getLogger().info("Modifying displayname for '"+name+"' from: '"+objective.getDisplayName()+"' to '"+displayName.toPlainText()
				+"', more formally to: "+displayName.toString());
			addObjectiveCmds.add("scoreboard objectives modify "+name+" displayname "+displayName.toString());
		}
		return true;
	}
	public boolean registerObjectiveIfDoesNotExist5sDelay(String name, Criteria criteria, Component displayName){
		return registerObjectiveIfDoesNotExist5sDelay(name, criteria.getName().toLowerCase(), displayName);
	}
}


//final Scoreboard board = pl.getServer().getScoreboardManager().getMainScoreboard();
//board.registerNewObjective("buildscore", "dummy", "§[■] Blocks Placed [■]");


//Display all different equine stats in a loop
/*new BukkitRunnable(){
	final Scoreboard board = pl.getServer().getScoreboardManager().getMainScoreboard();
	final String[] horseTypes = new String[]{"horse", "donkey", "mule", "llama"};
	final String[] statTypes = new String[]{"speed", "jump", "health"};
	int typeI = 0, statI = 0;
	@Override public void run(){
		if(typeI > 3){
			if(typeI == 4){board.getObjective("trader_llama-h").setDisplaySlot(DisplaySlot.SIDEBAR); typeI = 5;}
			else{board.getObjective("skeleton_horse-j").setDisplaySlot(DisplaySlot.SIDEBAR); typeI = 0;}
			return;
		}
		board.getObjective(horseTypes[typeI]+"-"+statTypes[statI]).setDisplaySlot(DisplaySlot.SIDEBAR);
		if((statI = ++statI % 3) == 0) typeI = ++typeI % 6;
	}
}.runTaskTimer(pl, 20*5, 20*5);*/