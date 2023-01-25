package net.evmodder.EvStats;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;

public class ObjectivesExpansion extends PlaceholderExpansion{
	// Resource Page:
	// https://www.spigotmc.org/resources/placeholderapi-scoreboard-objectives-placeholder.48236/
	// eCloud: https://api.extendedclip.com/manage/

	// Previous Author (before rewrite): LethalBunny

//	@Override public boolean canRegister(){return true;}//Default behavior is return true
	@Override public String getAuthor(){return "EvModder & LethalBunny";}
	@Override public String getName(){return "ScoreboardObjectives";}
	@Override public String getIdentifier(){return "objective";}
	@Override public String getVersion(){return "5.0";}

	private String plc(String str){return "%" + getIdentifier() + "_" + str + "%";}
	@Override public List<String> getPlaceholders(){
		return Arrays.asList(
				// Deprecated placeholders (still supported, but not shown in TAB-complete)
//				plc("displayname_<obj-name>"),
//				plc("score_<obj-name>"),
//				plc("score_<obj-name>_[otherEntry]"),
//				plc("scorep_<obj-name>"),
//				plc("scorep_<obj-name>_[otherEntry]"),
//				plc("scorep_{<obj-name>}"),
//				plc("scorep_{<obj-name>}_{[otherPlayer]}"),
//				plc("scoreof_{<obj-name>}_for_<placeholder>"),
//				plc("entryposhigh_{<obj-name>}_{<#>}"),
//				plc("scoreposhigh_{<obj-name>}_{<#>}"),
//				plc("entryposlow_{<obj-name>}_{<#>}"),
//				plc("scoreposlow_{<obj-name>}_{<#>}"),

				// Supported placeholders
				plc("displayname_{<obj>}"),
				plc("score_{<obj>}%"),
				plc("score_{<obj>}_{[otherEntry]}"),
				plc("entrypos_{<obj>}_{<#>}"),
				plc("scorepos_{<obj>}_{<#>}"),
				plc("score_{<obj>}_for_entrypos_{<obj>}_{<#>}"),
				plc("score_{<obj>}_for_entrypos_{<obj>}_{<#>}_{[otherEntry]}")
		);
	}

	private String getScore(String objName, String entry){
		final Objective obj = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(objName);
		return obj == null ? null : ""+obj.getScore(entry).getScore();
	}
	private String getDisplayName(String objName){
		final Objective obj = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(objName);
		return obj == null ? null : obj.getDisplayName();
	}

	private int numEntries = 0;
	private final HashMap<Objective, List<Score>> sortedScores = new HashMap<>();
//	private final long cacheExpirationTicks = 20;
//	private BukkitRunnable cacheExpirationTask;
	private Score getScoreAtRank(String objName, int rank){
		final Objective obj = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(objName);
		if(obj == null){
			Bukkit.getLogger().warning("Unknown objective: "+objName);
			return null;
		}
		final Set<String> entries = Bukkit.getScoreboardManager().getMainScoreboard().getEntries();
		List<Score> objScores = sortedScores.get(obj);
		if(objScores == null || entries.size() > numEntries){
			numEntries = entries.size();
			objScores = new ArrayList<>();
			for(String entry : entries){
				final Score score = obj.getScore(entry);
				if(score.isScoreSet()) objScores.add(score);
			}
			sortedScores.put(obj, objScores);
		}
		/*if(!cacheExpirationTask) */objScores.sort(Comparator.comparingInt(e -> e.getScore()));
		rank = (rank > 0 ? objScores.size() : -1) - rank; // Currently we sort ascending 
		//rank += (rank > 0 ? -1 : objScores.size()); // If sorted descending, use this
		return (rank >= 0 && rank < objScores.size()) ? objScores.get(rank) : objScores.get(objScores.size()-1);
	}

	private static class Pair<T, R>{
		public final T a; public final R b;
		public Pair(T t, R r){a=t; b=r;}
	}
	private final Pair<String, Integer> parseNextExpansion(final OfflinePlayer player, final String identifier, final int start){
		if(start + 1 > identifier.length() || identifier.charAt(start) != '{') return null;//invalid input
		if(identifier.charAt(start + 1) != '%'){
			final int end = identifier.indexOf('}', start + 1);
			if(end == -1) return null;//invalid input
			return new Pair<>(identifier.substring(start+1, end), end + 1);
		}
		int depth = 1;
		for(int i=start + 2; i<identifier.length(); ++i){
			switch(identifier.charAt(i)){
				case '}':
					if(--depth == 0){
						if(identifier.charAt(i - 1) != '%') return null;//invalid input
						return new Pair<>(PlaceholderAPI.setPlaceholders(player, identifier.substring(start + 1, i)), i + 1);
					}
					break;
				case '{':
					++depth;
			}
		}
		return null;//invalid input
	}

	final int identifierLen = "objective_".length();
	final int scorePrefixLen = "score".length();
	final int displaynamePrefixLen = "displayname_".length();
	final int entryposPrefixLen = "entrypos".length();
	final int scoreposPrefixLen = "scorepos".length();
	// Note: r`^%objective_` and r`%$` are not included in `identifier`
	@Override public String onRequest(OfflinePlayer player, String identifier){
		// Note: This entire function could be waayyy simpler if using Regex, but I wanted to optimize for performance.
		if(identifier.startsWith("entrypos")){
			//%objective_entrypos(low|high)?_{objName}_{[#]}%
			if(identifier.charAt(identifier.length()-1) != '}') return null;//invalid input
			int objNameStart = entryposPrefixLen;
			final boolean LOW_POS = identifier.startsWith("low", objNameStart);
			if(LOW_POS) objNameStart += 3;
			else if(identifier.startsWith("high", objNameStart)) objNameStart += 4;
			if(!identifier.startsWith("_{", objNameStart)) return null;//invalid input
			
			Pair<String, Integer> textAndIdx = parseNextExpansion(player, identifier, objNameStart + 1);
			if(textAndIdx == null) return null;//invalid input
			final String objName = textAndIdx.a;
			final int objNameEnd = textAndIdx.b;
			
			if(objNameEnd == identifier.length()) return getScoreAtRank(objName, LOW_POS ? -1 : 1).getEntry();//%objective_entrypos_{objName}%
			if(!identifier.startsWith("_{", objNameEnd)) return null;//invalid input
			textAndIdx = parseNextExpansion(player, identifier, objNameEnd + 2);
			if(textAndIdx == null) return null;//invalid input
			int rank = 1;
			try{rank = Integer.parseInt(textAndIdx.a);}
			catch(IllegalArgumentException e){return null;}//invalid input
			return getScoreAtRank(objName, (LOW_POS ? -rank : rank)).getEntry();//%objective_entrypos_{objName}_{#}%
		}
		else if(identifier.startsWith("scorepos")){
			//%objective_scorepos(low|high)?_{objName}_{[#]}%
			if(identifier.charAt(identifier.length()-1) != '}') return null;//invalid input
			int objNameStart = scoreposPrefixLen;
			final boolean LOW_POS = identifier.startsWith("low", objNameStart);
			if(LOW_POS) objNameStart += 3;
			else if(identifier.startsWith("high", objNameStart)) objNameStart += 4;
			if(!identifier.startsWith("_{", objNameStart)) return null;//invalid input
			
			Pair<String, Integer> textAndIdx = parseNextExpansion(player, identifier, objNameStart + 1);
			if(textAndIdx == null) return null;//invalid input
			final String objName = textAndIdx.a;
			final int objNameEnd = textAndIdx.b;
			
			if(objNameEnd == identifier.length()) return ""+getScoreAtRank(objName, LOW_POS ? -1 : 1).getScore();//%objective_scorepos_{objName}%
			if(!identifier.startsWith("_{", objNameEnd)) return null;//invalid input
			textAndIdx = parseNextExpansion(player, identifier, objNameEnd + 2);
			if(textAndIdx == null) return null;//invalid input
			int rank = 1;
			try{rank = Integer.parseInt(textAndIdx.a);}
			catch(IllegalArgumentException e){return null;}//invalid input
			return ""+getScoreAtRank(objName, (LOW_POS ? -rank : rank)).getScore();//%objective_scorepos_{objName}_{#}%
		}
		else if(identifier.startsWith("score")){
			//%objective_score(p|of)?_{?objName}?%
			//%objective_score_{objName}_{[otherEntry]}%
			//%objective_score_{objName1}_for_entrypos_{objName2}_{#}%
			int objNameStart = scorePrefixLen;
			if(identifier.charAt(objNameStart) == 'p') ++objNameStart;
			else if(identifier.charAt(objNameStart) == 'o') objNameStart += 2;
			if(identifier.charAt(objNameStart) == '_') ++objNameStart;
			else return null;//invalid input
			if(identifier.charAt(objNameStart) != '{'){
				final int entryStart = identifier.lastIndexOf('_');
				if(entryStart > objNameStart){
					final String entry = identifier.substring(entryStart+1);
					final String objName = identifier.substring(objNameStart, entryStart);
					return getScore(objName, entry);//%objective_score(p|of)?_objName_otherEntry%
				}
				final String objName = identifier.substring(objNameStart);
				return getScore(objName, player.getName());//%objective_score(p|of)?_objName%
			}
			Pair<String, Integer> textAndIdx = parseNextExpansion(player, identifier, objNameStart);
			if(textAndIdx == null) return null;//invalid input
			final String objName = textAndIdx.a;
			final int objNameEnd = textAndIdx.b;
			
			if(objNameEnd == identifier.length()) return getScore(objName, player.getName());//%objective_score(p|of)?_{objName}%
			if(identifier.startsWith("_for_", objNameEnd)){
				final String subRequest = identifier.substring(objNameEnd+5 +
						(identifier.startsWith("objective_", objNameEnd+5) ? identifierLen : 0));
				final String entry = onRequest(player, subRequest);//...entrypos(low|high)?_{objName}_{[#]}
				return entry == null ? null : getScore(objName, entry);//%objective_score(p|of)?_{objName1}_for_...%
			}
			if(identifier.charAt(objNameEnd) != '_') return null;//invalid input
			if(identifier.charAt(objNameEnd+1) != '{'){
				return getScore(objName, identifier.substring(objNameEnd+1));//%objective_score(p|of)?_{objName}_otherEntry%
			}
			if(identifier.charAt(identifier.length()-1) != '}') return null;//invalid input
			return getScore(objName, parseNextExpansion(player, identifier, objNameEnd+1).a);//%objective_score(p|of)?_{objName}_{otherEntry}%
		}
		else if(identifier.startsWith("displayname_")){
			//%objective_displayname_{objName}%
			if(identifier.charAt(displaynamePrefixLen) != '{'){
				return getDisplayName(identifier.substring(displaynamePrefixLen));//%objective_displayname_objName%
			}
			if(identifier.charAt(identifier.length()-1) != '}') return null;//invalid input
			return getDisplayName(parseNextExpansion(player, identifier, displaynamePrefixLen).a);//%objective_displayname_{objName}%
		}
		return null;//placeholder not found
	}
	@Override public String onPlaceholderRequest(Player player, String identifier){return onRequest(player, identifier);}
}