package dt.jdictionary.dbservice.alternative;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dt.jdictionary.ChineseSummaryLookup;
import dt.jdictionary.dbrepo.DbRepo;
import dt.jdictionary.dbservice.DbServiceUtils;
import dt.util.ChineseText;

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
	public List<ChineseSummaryLookup> trySearch() throws SQLException 
	{
		final List<String> characters = ChineseText.charsByCodepoint(chinese);
		final Map<String, List<String>> reverseMapping = db.lookupReverseSimplified(characters);
		final List<String> allCombinations = explodeCombinations(characters, reverseMapping);
		return DbServiceUtils.convertRawToSimple(db.lookupChinese(allCombinations));
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
