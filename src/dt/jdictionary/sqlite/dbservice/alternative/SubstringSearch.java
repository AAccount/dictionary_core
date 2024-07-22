package dt.jdictionary.sqlite.dbservice.alternative;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.sqlite.dbservice.DbServiceUtils;
import dt.jdictionary.sqlite.raw.IDbRepo;
import dt.jdictionary.util.GenerateSubstrings;
import dt.util.ChineseText;

public class SubstringSearch implements AlternateSearch
{
	private final String zh;
	private final IDbRepo db;
	private final Map<String, Double> frontToBackRanking;
	public static final String LOOKUP_NAME = "Substring";
	
	public SubstringSearch(String zh, IDbRepo db)
	{
		this.zh = zh;
		this.db = db;
		this.frontToBackRanking = this.generateFrontToBackRanking(zh);
	}

	@Override
	public List<SimpleLookup> trySearch() throws Exception
	{
		final List<String> allSubstrings = GenerateSubstrings.generateSubstrings(this.zh);
		return DbServiceUtils
				.convertRawToSimple(this.db.lookupChinese(allSubstrings))
				.stream().map(simpleLookup -> new SimpleLookup(simpleLookup, this.rankBasedOnOriginalFrontToBack(simpleLookup.getZh())))
				.collect(Collectors.toCollection(ArrayList::new));
		
//		if(UiConstants.getFlag(UiConstants.FLAG_ALWAYS_SINGLE_SUBSTRING))
//		{
//			return allResults;
//		}
//		
//		final List<SimpleLookup> nonSingle = allResults.stream().filter(result -> ChineseText.trueChars(result.getZh()).size() > 1).collect(Collectors.toCollection(ArrayList::new));
//		return nonSingle.isEmpty() ? allResults : nonSingle;
	}

	@Override
	public String LOOKUP_NAME()
	{
		return LOOKUP_NAME;
	}
	
	private Map<String, Double> generateFrontToBackRanking(String input)
	{
		final Map<String, Double> result = new HashMap<>();
		final List<String> trueChars = ChineseText.trueChars(input);
		for(int i=0; i<trueChars.size(); i++)
		{
			final String singleChar = trueChars.get(i);
			result.put(singleChar, 1.0*(trueChars.size() - i));
		}
		return result;
	}
	
	private double rankBasedOnOriginalFrontToBack(String resultZh)
	{
		return ChineseText.trueChars(resultZh).stream().reduce(0.0, (acc, singleChar) -> acc + this.frontToBackRanking.get(singleChar), Double::sum);
	}
}
