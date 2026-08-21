package dt.jdictionary.dbrepo.raw;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import dt.jdictionary.dbrepo.DictionaryEntry;

public class DbRepoCache 
{
	private static final Logger logger = Logger.getLogger(DbRepoCache.class.getName());

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

	// This cache table should be "write once only". The dictionary entries are fixed.
	// Without the write once only check the synchronized is useless.
	// It allows multiple concurrent alternate search strategies to write duplicates upon duplicates into the cache.
	// This sequence seems to trigger this most noticeably: 瞎練, 有幾條名, 有幾條命, 曾經
	// (This is from Shadows House. The 3rd lookup is a "typo correction" for the 2nd one.)
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
			tableColumnEntryCache.get(table).get(column).get(columnValue).add(result);
		}
		else
		{
			logger.info("table " + table + " column " + column + " column value " + columnValue + " cache entry already exists");
		}
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

	public synchronized void invalidateTableColumnCache(List<String> chineseWords)
	{
		for(final String table : tableColumnEntryCache.keySet())
		{
			for(final String column : tableColumnEntryCache.get(table).keySet())
			{
				for(final String chineseWord : chineseWords)
				{
					tableColumnEntryCache.get(table).get(column).remove(chineseWord);
				}
			}
		}
	}

	public synchronized String getSimplifiedCache(Integer codepoint)
	{
		return simplifiedCache.getOrDefault(codepoint, null);
	}

	public synchronized void setSimplfiedCache(Integer codepoint, String simplified)
	{
		simplifiedCache.put(codepoint, simplified);
	}

	public synchronized List<String> getMeasureWordCache(String chinese)
	{
		return measureWordCache.getOrDefault(chinese, null);
	}

	public synchronized void setMeasureWordCache(String chinese, List<String> measureWords)
	{
		measureWordCache.put(chinese, measureWords);
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
