package dt.jdictionary.dbservice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dt.jdictionary.ChineseSummaryLookup;
import dt.jdictionary.dbrepo.raw.RawDictionaryRow;

public class DbServiceUtils 
{

	public static List<ChineseSummaryLookup> convertRawToSimple(List<RawDictionaryRow> rawResults)
	{
		return rawResults.stream()
				.map(line -> new ChineseSummaryLookup(line.getZh(), line.getPinyin(), line.getSingleDefinition(), line.getRank()))
				.toList();
	}
	
	public static List<ChineseSummaryLookup> rerank(List<ChineseSummaryLookup> results, Map<String, Long> pastHits)
	{
		return results.stream().map(lookup -> rerankSingle(lookup, pastHits)).toList();
	}
	
	private static ChineseSummaryLookup rerankSingle(ChineseSummaryLookup lookup, Map<String, Long> pastHits)
	{
		final int HISTORY_RELEVANCE_MULTIPLIER = 10000; // Arbitrarily a 萬.
		if(lookup.getRank() > 0 && pastHits.containsKey(lookup.getChinese())) // Blacklisted place names should stay that, way even if the place was seen in a text blob.
		{
			return new ChineseSummaryLookup(lookup, pastHits.get(lookup.getChinese()) * HISTORY_RELEVANCE_MULTIPLIER);
		}
		return lookup;
	}
}
