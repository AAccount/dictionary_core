package dt.jdictionary.sqlite.dbservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.sqlite.raw.RawDictionaryRow;

public class DbServiceUtils 
{

	public static List<SimpleLookup> convertRawToSimple(List<RawDictionaryRow> rawResults)
	{
		final List<SimpleLookup> result = new ArrayList<>();
		final Map<String, SimpleLookup> mapper = new HashMap<>();

		for(final RawDictionaryRow rawResult : rawResults)
		{
			final String key = rawResult.getZh() + ":" + rawResult.getPinyin();
			if(!mapper.keySet().contains(key))
			{
				final SimpleLookup simpleLookup = new SimpleLookup(rawResult.getZh(), rawResult.getPinyin(), new ArrayList<>(), rawResult.getRank());
				result.add(simpleLookup);
				mapper.put(key, simpleLookup);
			}
			mapper.get(key).getDefinitions().add(rawResult.getSingleDefinition());
		}

		return result;
	}
	
	public static List<SimpleLookup> rerank(List<SimpleLookup> results, Map<String, Integer> pastHits)
	{
		return results.stream().map(lookup -> rerankSingle(lookup, pastHits)).collect(Collectors.toCollection(ArrayList::new));
	}
	
	private static SimpleLookup rerankSingle(SimpleLookup lookup, Map<String, Integer> pastHits)
	{
		final int HISTORY_RELEVANCE_MULTIPLIER = 10000; // Arbitrarily a 萬.
		if(lookup.getRank() > 0 && pastHits.containsKey(lookup.getZh())) // Blacklisted place names should stay that, way even if the place was seen in a text blob.
		{
			return new SimpleLookup(lookup, pastHits.get(lookup.getZh()) * HISTORY_RELEVANCE_MULTIPLIER);
		}
		return lookup;
	}
}
