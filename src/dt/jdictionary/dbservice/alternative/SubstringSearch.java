package dt.jdictionary.dbservice.alternative;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dt.jdictionary.dbrepo.DbRepo;
import dt.jdictionary.dbrepo.DictionaryEntry;
import dt.jdictionary.util.GenerateSubstrings;
import dt.util.ChineseText;

public class SubstringSearch implements AlternateSearch
{
	private final String chinese;
	private final DbRepo db;
	private final Map<String, Double> frontToBackRanking;
	public static final String LOOKUP_NAME = "Substring";
	
	public SubstringSearch(String chinese, DbRepo db)
	{
		this.chinese = chinese;
		this.db = db;
		this.frontToBackRanking = this.generateFrontToBackRanking(chinese);
	}

	@Override
	public List<DictionaryEntry> trySearch() throws SQLException
	{
		final List<String> allSubstrings = GenerateSubstrings.generateSubstrings(this.chinese);
		return this.db.lookupChinese(allSubstrings)
				.stream().map(simpleLookup -> new DictionaryEntry(simpleLookup, this.rankBasedOnOriginalFrontToBack(simpleLookup.getChinese())))
				.collect(Collectors.toCollection(ArrayList::new));
	}

	@Override
	public String LOOKUP_NAME()
	{
		return LOOKUP_NAME;
	}
	
	private Map<String, Double> generateFrontToBackRanking(String input)
	{
		final Map<String, Double> result = new HashMap<>();
		final List<String> chars = ChineseText.charsByCodepoint(input);
		for(int i=0; i<chars.size(); i++)
		{
			final String singleChar = chars.get(i);
			result.put(singleChar, 1.0*(chars.size() - i));
		}
		return result;
	}
	
	private double rankBasedOnOriginalFrontToBack(String chinese)
	{
		return ChineseText.charsByCodepoint(chinese).stream().reduce(0.0, (acc, singleChar) -> acc + this.frontToBackRanking.get(singleChar), Double::sum);
	}
}
