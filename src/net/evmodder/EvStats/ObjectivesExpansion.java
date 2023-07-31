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
	@Override public String getVersion(){return "5.3";}

	private String plc(String str){return "%" + getIdentifier() + "_" + str + "%";}
	@Override public List<String> getPlaceholders(){
		return Arrays.asList(
				// Deprecated placeholders (still supported, but not shown in TAB-complete)
//				plc("displayname_<obj>"),
//				plc("score_<obj>"),
//				plc("score_<obj>_[otherEntry]"),
//				plc("scorep_<obj>"),
//				plc("scorep_<obj>_[otherEntry]"),
//				plc("scorep_{<obj>}"),
//				plc("scorep_{<obj>}_{[otherPlayer]}"),
//				plc("scoreof_{<obj>}_for_<placeholder>"),
//				plc("entryposhigh_{<obj>}_{<#>}"),
//				plc("scoreposhigh_{<obj>}_{<#>}"),
//				plc("entryposlow_{<obj>}_{<#>}"),
//				plc("scoreposlow_{<obj>}_{<#>}"),

				// Supported placeholders
				plc("displayname_{<obj>}"),
				plc("score_{<obj>}%"),
				plc("score_{<obj>}_{[otherEntry]}"),
				plc("entrypos_{<obj>}_{<#>}"),
				plc("scorepos_{<obj>}_{<#>}"),
				plc("score_{<obj>}_for_entrypos_{<obj>}_{<#>}"),
				plc("score_{<obj>}_for_entrypos_{<obj>}_{<#>}_{[otherEntry]}"),
				plc("entries_{<obj>},<all/any/size>"),
				plc("entries_{<obj>}_for_score_{<#>},<all/any/size>")
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

	private enum ListType{ALL, ANY, SIZE}
	private ListType parseListType(final String name){
		if(name.equals("all") || name.equals("list")) return ListType.ALL;
		else if(name.equals("size") || name.equals("amount")) return ListType.SIZE;
		else if(name.equals("first") || name.equals("any")) return ListType.ANY;
		Bukkit.getLogger().warning("Unknown output option: "+name+", defaulting to 'any'");
		return ListType.ANY;
	}
	private String getEntries(String objName, Integer integer, ListType type){
		final Objective obj = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(objName);
		if(obj == null) return null;
		int size = 0;
		ArrayList<String> entries = new ArrayList<>();
		for(final String entry : Bukkit.getScoreboardManager().getMainScoreboard().getEntries()){
			final Score score = obj.getScore(entry);
			if(score.isScoreSet() && (integer == null || score.getScore() == integer)){
				switch(type){
					case ANY:
						return entry;
					case SIZE:
						++size;
						break;
					case ALL:
						entries.add(entry);
				}
			}
		}
		return type == ListType.ALL ? String.join(",", entries) : ""+size;
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
		List<Score> objScores;
		if(entries.size() != numEntries || (objScores=sortedScores.get(obj)) == null){
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
	private int getRank(String objName, String entry){
		final Objective obj = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(objName);
		if(obj == null){
			Bukkit.getLogger().warning("Unknown objective: "+objName);
			return -1;
		}
		final Score score = obj.getScore(entry);
		if(!score.isScoreSet()) return -1;
		final int target = score.getScore();
		getScoreAtRank(objName, -1);
		int low = 1, high = numEntries+1;
		//int r = numEntries/2, s = getScoreAtRank(objName, r+1).getScore();
		while(true){
			final int r = (low+high)/2, s = getScoreAtRank(objName, r).getScore();
			if(s < target) high = r;
			else if(s > target) low = r;
			else return r;
		}
	}

	private static class Pair<T, R>{
		public final T a; public final R b;
		public Pair(T t, R r){a=t; b=r;}
	}
	private final Pair<String, Integer> parseNextExpansion(final OfflinePlayer player, final String identifier, final int start){
		if(start + 1 > identifier.length() || identifier.charAt(start) != '{') return null;//invalid input
//		if(identifier.charAt(start + 1) != '%'){
//			final int end = identifier.indexOf('}', start + 1);
//			if(end == -1) return null;//invalid input
//			return new Pair<>(identifier.substring(start+1, end), end + 1);
//		}
		int depth = 1;
		for(int i=start + 2; i<identifier.length(); ++i){
			switch(identifier.charAt(i)){
				case '}':
					if(--depth == 0){
//						if(identifier.charAt(i - 1) != '%') return null;//invalid input
						final String innerRequest = identifier.substring(start + 1, i);
						final int firstUnderscore = innerRequest.indexOf('_');
						if(firstUnderscore == -1 || !PlaceholderAPI.isRegistered(innerRequest.substring(0, firstUnderscore))){
							return new Pair<>(innerRequest, i + 1);//not a placeholder
						}
						final String result = PlaceholderAPI.setPlaceholders(player, "%"+innerRequest+"%");
						return new Pair<>(result == null || result.isEmpty() ? innerRequest : result, i + 1);
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
			final boolean NO_POS, LOW_POS = identifier.startsWith("low", objNameStart);
			if(LOW_POS){objNameStart += 3; NO_POS = true;}
			else if(NO_POS=identifier.startsWith("high", objNameStart)) objNameStart += 4;
			if(!identifier.startsWith("_{", objNameStart)) return null;//invalid input
			
			Pair<String, Integer> textAndIdx = parseNextExpansion(player, identifier, objNameStart + 1);
			if(textAndIdx == null) return null;//invalid input
			final String objName = textAndIdx.a;
			final int objNameEnd = textAndIdx.b;
			
			if(objNameEnd == identifier.length()) return NO_POS
					? ""+getRank(objName, player.getName())//%objective_entrypos_{objName}%
					: getScoreAtRank(objName, LOW_POS ? -1 : 1).getEntry();//%objective_entrypos(low|high)_{objName}%
			if(!identifier.startsWith("_{", objNameEnd)) return null;//invalid input
			textAndIdx = parseNextExpansion(player, identifier, objNameEnd + 1);
			if(textAndIdx == null) return null;//invalid input
			final int rank;
			try{rank = Integer.parseInt(textAndIdx.a);}
			catch(IllegalArgumentException e){return null;}//invalid input
			if(rank == 0) return null;//invalid input
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
			textAndIdx = parseNextExpansion(player, identifier, objNameEnd + 1);
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
		else if(identifier.startsWith("entries_")){
			if(identifier.charAt(8) != '{'){
				final int objNameEnd = identifier.lastIndexOf(',');
				return getEntries(identifier.substring(8, objNameEnd), /*score=*/null, parseListType(identifier.substring(objNameEnd+1)));
			}
			Pair<String, Integer> textAndIdx = parseNextExpansion(player, identifier, 8);
			if(textAndIdx == null) return null;//invalid input
			final String objName = textAndIdx.a;
			final int objNameEnd = textAndIdx.b;
			if(identifier.startsWith(",", objNameEnd)) return getEntries(objName, /*score=*/null, parseListType(identifier.substring(objNameEnd+1)));
			else if(!identifier.startsWith("_for_score_", objNameEnd)) return null;//invalid input
			else if(identifier.charAt(objNameEnd+/*_for_score_.size=*/11) != '{'){
				int scoreEnd = identifier.lastIndexOf(',');
				ListType type = ListType.ANY;
				if(scoreEnd == -1) scoreEnd = identifier.length();
				else type = parseListType(identifier.substring(scoreEnd+1));
				final String scoreStr = identifier.substring(objNameEnd+/*_for_score_.size=*/11, scoreEnd);
				int score = 1;
				try{score = Integer.parseInt(scoreStr);}
				catch(IllegalArgumentException e){return null;}//invalid input
				return getEntries(objName, score, type);
			}
			else{
				textAndIdx = parseNextExpansion(player, identifier, objNameEnd+/*_for_score_.size=*/11);
				if(textAndIdx == null) return null;//invalid input
				int score = 1;
				try{score = Integer.parseInt(textAndIdx.a);}
				catch(IllegalArgumentException e){return null;}//invalid input
				if(identifier.charAt(textAndIdx.b) != ',') return getEntries(objName, score, ListType.ANY);
				return getEntries(objName, score, parseListType(identifier.substring(textAndIdx.b+1)));
			}
		}
		else if(identifier.startsWith("rank_")){
			//%objective_rank_{objName}%
			if(identifier.charAt(5) != '{'){
				return ""+getRank(identifier.substring(5), player.getName());//%objective_rank_objName%
			}
			Pair<String, Integer> textAndIdx = parseNextExpansion(player, identifier, 5);
			if(textAndIdx == null) return null;//invalid input
			final String objName = textAndIdx.a;
			final int objNameEnd = textAndIdx.b;
			
			if(objNameEnd == identifier.length()) return ""+getRank(objName, player.getName());//%objective_rank_{objName}%
			if(identifier.charAt(objNameEnd) != '_') return null;//invalid input
			if(identifier.charAt(objNameEnd+1) != '{'){
				return ""+getRank(objName, identifier.substring(objNameEnd+1));//%objective_rank_{objName}_otherEntry%
			}
			if(identifier.charAt(objNameEnd+1) != '{' || identifier.charAt(identifier.length()-1) != '}') return null;//invalid input
			return ""+getRank(objName, parseNextExpansion(player, identifier, objNameEnd+1).a);//%objective_rank_{objName}_{otherEntry}%
		}
		return null;//placeholder not found
	}
	@Override public String onPlaceholderRequest(Player player, String identifier){return onRequest(player, identifier);}
}