package dt.jdictionary.dbrepo.raw;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Need to wrap all cache responses in a "response" object because sometimes null is the answer.
public class DbRepoCache 
{
	private final Map<String, List<RawDictionaryRow>> tableCache = new HashMap<>();
	private final Map<String, String> simplifiedCache = new HashMap<>();
	private final Map<String, List<String>> measureWordCache = new HashMap<>();
	private final Map<String, List<String>> listOfStringsCache = new HashMap<>();
	private final Map<String, List<String>> reverseSimplified = new HashMap<>();

	private final static DbRepoCache instance = new DbRepoCache();

	public static DbRepoCache getInstance()
	{
		return instance;
	}

	private DbRepoCache() {}

	public List<RawDictionaryRow> getTableCache(String sql, String target)
	{
		final String key = stringMergedKey(new String[]{sql, target});
		return tableCache.getOrDefault(key, null);
	}

	public void setTableCache(String sql, String target, List<RawDictionaryRow> result)
	{
		final String key = stringMergedKey(new String[]{sql, target});
		tableCache.put(key, result);
	}

	public String getSimplifiedCache(String zh)
	{
		return simplifiedCache.getOrDefault(zh, null);
	}

	public void setSimplfiedCache(String zh, String simplified)
	{
		simplifiedCache.put(zh, simplified);
	}

	public List<String> getMeasureWordCache(String zh)
	{
		return measureWordCache.getOrDefault(zh, null);
	}

	public void setMeasureWordCache(String zh, List<String> measureWords)
	{
		measureWordCache.put(zh, measureWords);
	}

	public List<String> getListOfStringsCache(String sql, String search, String column)
	{
		final String key = stringMergedKey(new String[]{sql, search, column});
		return listOfStringsCache.getOrDefault(key, null);
	}

	public void setListOfStringsCache(String sql, String search, String column, List<String> results)
	{
		final String key = stringMergedKey(new String[]{sql, search, column});
		listOfStringsCache.put(key, results);
	}

	public List<String> getReverseSimplified(String simplified)
	{
		return reverseSimplified.getOrDefault(simplified, null);
	}

	public void setReverseSimplified(String simplified, List<String> traditionals)
	{
		reverseSimplified.put(simplified, traditionals);
	}

	public void wipe()
	{
		tableCache.clear();
		simplifiedCache.clear();
		measureWordCache.clear();
		listOfStringsCache.clear();
		reverseSimplified.clear();
	}

	private String stringMergedKey(String[] strings)
	{
		final String STRING_DELIM = "‱";
		final StringBuilder sb = new StringBuilder();
		for(final String string : strings)
		{
			sb.append(string).append(STRING_DELIM);
		}
		return sb.toString();
	}
}
