package commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import net.evmodder.EvLib.bukkit.EvCommand;

/**
* @author EvModder/EvDoc (evdoc at altcraft.net)
*/
public class CommandStat extends EvCommand{
	private final PerPlayerScoreboardManager ppMan;
	private final Scoreboard mainBoard;

	public CommandStat(JavaPlugin pl){
		super(pl);
		ppMan = new PerPlayerScoreboardManager(pl);
		mainBoard = pl.getServer().getScoreboardManager().getMainScoreboard();
	}

	private static List<String> statNames = null;
	public List<String> getStatNames(){
		if(statNames == null || statNames.size() != mainBoard.getObjectives().size()){
			statNames = mainBoard.getObjectives().stream().map(obj -> obj.getName()).toList();
		}
		return statNames;
	}

	@Override public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args){
		if(args.length == 2) return Stream.concat(
				Stream.of("5", "10", "60", "300", "3600"),
				Bukkit.getServer().getOnlinePlayers().stream().map(p -> p.getName())
			).filter(n -> n.startsWith(args[1])).toList();

		if(args.length != 1) return ImmutableList.of(); //TODO: 2nd arg for display time in seconds (or toggle?)

		//return statNames.stream().filter(name -> name.startsWith(args[0])).toList();
		// TODO: Prefix tree would be better
		int sharedPrefixLen = args[0].length();
		while(true){
			HashMap<String, String> tabCompletes = new HashMap<String, String>();
			for(String statName : getStatNames()){
				if(!statName.startsWith(args[0])) continue;
				final int currPartEnd = statName.indexOf('_', sharedPrefixLen + 1);
//				pl.getLogger().info("currPartEnd: "+currPartEnd);
				final String tabComplete = statName.substring(0, currPartEnd == -1 ? statName.length() : currPartEnd+1);
//				pl.getLogger().info("obj: "+statName+", tabc: "+tabComplete);
				if(tabCompletes.put(tabComplete, statName) != null) tabCompletes.put(tabComplete, tabComplete);
			}
			if(tabCompletes.size() != 1 || !tabCompletes.values().iterator().next().endsWith("_")) return tabCompletes.values().stream().toList();
			sharedPrefixLen += tabCompletes.values().iterator().next().length();
		}
	}

	@Override public boolean onCommand(CommandSender sender, Command command, String label, String args[]){
		if(sender instanceof Player == false){
			sender.sendMessage(ChatColor.RED+"This command can only be run by in-game players!");
			return true;
		}
		if(args.length == 0){
			ppMan.returnToMainScoreboard((Player)sender);
			return true;
		}
		if(args.length > 3){
//			sender.sendMessage(ChatColor.RED+"Invalid arguments");
			return false;
		}
		Objective obj = mainBoard.getObjective(args[0]);
//		if(obj == null) obj = mainBoard.getObjective("pstats-"+args[0]);
//		if(obj == null) obj = mainBoard.getObjective("istats-"+args[0]);
		if(obj == null){
			sender.sendMessage(ChatColor.RED+"Unknown statistic: "+args[0]);
			ppMan.returnToMainScoreboard((Player)sender);
			return true;
		}
		if(args.length == 1) ppMan.showTempScoreboard((Player)sender, obj, null, null);
		else if(args.length == 2){
			if(args[1].matches("\\d+")) ppMan.showTempScoreboard((Player)sender, obj, Long.parseLong(args[1]), /*entry=*/null);
			else ppMan.showTempScoreboard((Player)sender, obj, /*seconds=*/null, args[1]);
		}
		else if(args[1].matches("\\d+")) ppMan.showTempScoreboard((Player)sender, obj, Long.parseLong(args[1]), args[2]);
		else if(args[2].matches("\\d+")) ppMan.showTempScoreboard((Player)sender, obj, Long.parseLong(args[2]), args[1]);
		else{
//			sender.sendMessage(ChatColor.RED+"Invalid arguments");
			return false;
		}
		return true;
	}
}