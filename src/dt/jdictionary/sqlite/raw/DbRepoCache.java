package dt.jdictionary.sqlite.raw;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Need to wrap all cache responses in a "response" object because sometimes null is the answer.
public class DbRepoCache 
{
	//  List<RawDictionaryRow> lookupDictionaryTable(String sql, String target)
	private final Map<String, List<RawDictionaryRow>> tableCache = new HashMap<>();

	// String lookupSimplified(String zh)
	private final Map<String, String> simplifiedCache = new HashMap<>();

	// List<String> lookupMeasureWords(String zh)
	private final Map<String, List<String>> measureWordCache = new HashMap<>();

	// List<String>getListOfString(String sql, String search, String column)	
	private final Map<String, List<String>> listOfStringsCache = new HashMap<>();

	private final static DbRepoCache instance = new DbRepoCache();

	public static DbRepoCache getInstance()
	{
		return instance;
	}

	private DbRepoCache() {}

	public Optional<List<RawDictionaryRow>> getTableCache(String sql, String target)
	{
		final String key = stringMergedKey(new String[]{sql, target});
		if(!tableCache.containsKey(key))
		{
			return Optional.empty();
		}

		return Optional.of(tableCache.get(key));
	}

	public void setTableCache(String sql, String target, List<RawDictionaryRow> result)
	{
		final String key = stringMergedKey(new String[]{sql, target});
		tableCache.put(key, result);
	}

	public Optional<String> getSimplifiedCache(String zh)
	{
		if(!simplifiedCache.containsKey(zh))
		{
			return Optional.empty();
		}
		return Optional.of(simplifiedCache.get(zh));
	}

	public void setSimplfiedCache(String zh, String simplified)
	{
		simplifiedCache.put(zh, simplified);
	}

	public Optional<List<String>> getMeasureWordCache(String zh)
	{
		if(!measureWordCache.containsKey(zh))
		{
			return Optional.empty();
		}

		return Optional.of(measureWordCache.get(zh));
	}

	public void setMeasureWordCache(String zh, List<String> measureWords)
	{
		measureWordCache.put(zh, measureWords);
	}

	public Optional<List<String>> getListOfStringsCache(String sql, String search, String column)
	{
		final String key = stringMergedKey(new String[]{sql, search, column});
		if(!listOfStringsCache.containsKey(key))
		{
			return Optional.empty();
		}
		return Optional.of(listOfStringsCache.get(key));
	}

	public void setListOfStringsCache(String sql, String search, String column, List<String> results)
	{
		final String key = stringMergedKey(new String[]{sql, search, column});
		listOfStringsCache.put(key, results);
	}

	public void wipe()
	{
		tableCache.clear();
		simplifiedCache.clear();
		measureWordCache.clear();
		listOfStringsCache.clear();
	}

	private String stringMergedKey(String[] strings)
	{
		final String STRING_DELIM = "‱";
		String result = "";
		for(final String string : strings)
		{
			result = result + string + STRING_DELIM;
		}
		return result;
	}
}
