package dt.jdictionary.dbservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dt.jdictionary.ChineseSummaryLookup;
import dt.jdictionary.dbrepo.raw.RawDictionaryRow;

public class DbServiceUtils 
{

	public static List<ChineseSummaryLookup> convertRawToSimple(List<RawDictionaryRow> rawResults)
	{
		final Map<RawDictionaryRow, StringBuilder> rawToSb = new HashMap<>();
		final Map<RawDictionaryRow, Double> rawToRank = new HashMap<>();
		for(final RawDictionaryRow raw : rawResults)
		{
			if(!rawToSb.containsKey(raw))
			{
				rawToSb.put(raw, new StringBuilder());
			}
			rawToSb.get(raw).append(raw.getSingleDefinition()).append("/ ");

			final double rank = rawToRank.getOrDefault(raw, 0.0);
			rawToRank.put(raw, Math.max(rank, raw.getRank()));
		}

		final List<ChineseSummaryLookup> result = new ArrayList<>();
		for(final RawDictionaryRow raw : rawToSb.keySet())
		{
			final StringBuilder sb = rawToSb.get(raw);
			sb.setLength(sb.length()-1);
			result.add(new ChineseSummaryLookup(raw.getZh(), raw.getPinyin(), sb.toString(), rawToRank.get(raw)));
		}
		return result;
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
