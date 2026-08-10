package dt.jdictionary.dbrepo.raw;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dt.jdictionary.dbrepo.DictionaryEntry;

public class DbRepoCache 
{
	private final Map<Integer, String> simplifiedCache = new HashMap<>();
	private final Map<String, List<String>> measureWordCache = new HashMap<>();
	private final Map<String, List<String>> superStringCache = new HashMap<>();
	private final Map<String, List<String>> reverseSimplified = new HashMap<>();
	private final Map<String, Map<String, Map<String, List<DictionaryEntry>>>> tableColumnEntryCache = new HashMap<>();

	private final static DbRepoCache instance = new DbRepoCache();

	public static DbRepoCache getInstance()
	{
		return instance;
	}

	private DbRepoCache() {}

	public synchronized void setResultsForTableColumn(String table, String column, String columnValue, DictionaryEntry result)
	{
		if(!tableColumnEntryCache.containsKey(table))
		{
			tableColumnEntryCache.put(table, new HashMap<>());
		}

		if(!tableColumnEntryCache.get(table).containsKey(column))
		{
			tableColumnEntryCache.get(table).put(column, new HashMap<>());
		}

		if(!tableColumnEntryCache.get(table).get(column).containsKey(columnValue))
		{
			tableColumnEntryCache.get(table).get(column).put(columnValue, new ArrayList<>());
		}
		tableColumnEntryCache.get(table).get(column).get(columnValue).add(result);
	}

	public synchronized List<DictionaryEntry> getTableColumnCache(String table, String column, String columnValue)
	{
		if(!tableColumnEntryCache.containsKey(table))
		{
			return List.of();
		}

		if(!tableColumnEntryCache.get(table).containsKey(column))
		{
			return List.of();
		}

		return tableColumnEntryCache.get(table).get(column).getOrDefault(columnValue, List.of());
	}

	public synchronized String getSimplifiedCache(Integer codepoint)
	{
		return simplifiedCache.getOrDefault(codepoint, null);
	}

	public synchronized void setSimplfiedCache(Integer codepoint, String simplified)
	{
		simplifiedCache.put(codepoint, simplified);
	}

	public synchronized List<String> getMeasureWordCache(String zh)
	{
		return measureWordCache.getOrDefault(zh, null);
	}

	public synchronized void setMeasureWordCache(String zh, List<String> measureWords)
	{
		measureWordCache.put(zh, measureWords);
	}

	public synchronized List<String> getSuperstrings(String chinese)
	{
		return superStringCache.getOrDefault(chinese, null);
	}

	public synchronized void setSuperstrings(String chinese, List<String> results)
	{
		superStringCache.put(chinese, results);
	}

	public synchronized List<String> getReverseSimplified(String simplified)
	{
		return reverseSimplified.getOrDefault(simplified, null);
	}

	public synchronized void setReverseSimplified(String simplified, String traditional)
	{
		if(!reverseSimplified.containsKey(simplified))
		{
			reverseSimplified.put(simplified, new ArrayList<>());
		}
		reverseSimplified.get(simplified).add(traditional);
	}

	public void wipe()
	{
		tableColumnEntryCache.clear();
		simplifiedCache.clear();
		measureWordCache.clear();
		superStringCache.clear();
		reverseSimplified.clear();
	}
}
