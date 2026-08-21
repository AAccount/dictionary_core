package dt.jdictionary.dbservice.alternative;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dt.jdictionary.dbrepo.DbRepo;
import dt.jdictionary.dbrepo.DictionaryEntry;
import dt.jdictionary.util.ChineseText;

public class SimplifiedSearch implements AlternateSearch
{
	private final String chinese;
	private final DbRepo db;

	public SimplifiedSearch(String chinese, DbRepo db) 
	{
		this.chinese = chinese;
		this.db = db;
	}

	@Override
	public List<DictionaryEntry> trySearch() throws SQLException 
	{
		final List<String> characters = ChineseText.charsByCodepoint(chinese);
		final Map<String, List<String>> reverseMapping = db.lookupReverseSimplified(characters);
		final List<String> allCombinations = explodeCombinations(characters, reverseMapping);
		
		// Include the raw conversion of simplified to its possible traditionals in case it is useful.
		final List<DictionaryEntry> results = allCombinations
			.stream()
			.map(reversed -> new DictionaryEntry(reversed, "", 10000))
			.collect(Collectors.toCollection(ArrayList::new));
		final List<DictionaryEntry> dbMatches =  db.lookupChinese(allCombinations);
		results.addAll(dbMatches);
		
		if(results.size() == 1 && results.get(0).getChinese().equals(this.chinese))
		{
			return List.of(); // The only entry is the original search query that was already traditional.
		}
		return results;
	}

	private List<String> explodeCombinations(List<String> characters, Map<String, List<String>> reverseMapping)
	{
		List<String> partialResult = new ArrayList<>();
		partialResult.add("");

		for(final String character : characters)
		{
			final List<String> nextRow = new ArrayList<>();
			for(final String match : reverseMapping.get(character))
			{
				for(final String partial : partialResult)
				{
					nextRow.add(partial + match);
				}
			}
			partialResult = nextRow;
		}

		// For characters with no simplification, dbrepo stores a char=List.of(itself) entry
		// to prevent always having to go to the db only to find out there isn't one.
		// This means explode combinations may accidentally reproduce the same string as this.chinese.
		// Filter out that result.
		if(partialResult.size() == 1 && partialResult.get(0).equals(chinese))
		{
			return List.of();
		}
		return partialResult;
	}

	@Override
	public String LOOKUP_NAME() 
	{
		return "Was Simplified";
	}

}
