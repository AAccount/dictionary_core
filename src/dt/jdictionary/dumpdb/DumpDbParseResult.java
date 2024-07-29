package dt.jdictionary.dumpdb;

import java.util.Map;

import dt.jdictionary.dumpdb.line.DictionaryLine;

public class DumpDbParseResult
{
	private final Map<String, DictionaryLine[]> indexByChinese;
	private final Map<String, DictionaryLine[]> indexByPinyinNorm;
	private final Map<String, DictionaryLine[]> indexByFirstChar;
	private final Map<String, DictionaryLine[]> indexByLastChar;
	private final Map<String, String> simplifiedMap;
	private final Map<String, String[]> substringMap;
	private final Map<String, String[]> measureMap;
	private final Map<String, String[]> englishMap;
	private final Map<String, Integer> pastHitsMap;
	
	public DumpDbParseResult(
			Map<String, DictionaryLine[]> indexByChinese,
			Map<String, DictionaryLine[]> indexByPinyinNorm, 
			Map<String, DictionaryLine[]> indexByFirstChar,
			Map<String, DictionaryLine[]> indexByLastChar, 
			Map<String, String> simplifiedMap,
			Map<String, String[]> substringMap, 
			Map<String, String[]> measureMap, 
			Map<String, String[]> englishMap,
			Map<String, Integer> pastHitsMap)
	{
		super();
		this.indexByChinese = indexByChinese;
		this.indexByPinyinNorm = indexByPinyinNorm;
		this.indexByFirstChar = indexByFirstChar;
		this.indexByLastChar = indexByLastChar;
		this.simplifiedMap = simplifiedMap;
		this.substringMap = substringMap;
		this.measureMap = measureMap;
		this.englishMap = englishMap;
		this.pastHitsMap = pastHitsMap;
	}

	public Map<String, DictionaryLine[]> getIndexByChinese()
	{
		return indexByChinese;
	}

	public Map<String, DictionaryLine[]> getIndexByPinyinNorm()
	{
		return indexByPinyinNorm;
	}

	public Map<String, DictionaryLine[]> getIndexByFirstChar()
	{
		return indexByFirstChar;
	}

	public Map<String, DictionaryLine[]> getIndexByLastChar()
	{
		return indexByLastChar;
	}

	public Map<String, String> getSimplifiedMap()
	{
		return simplifiedMap;
	}

	public Map<String, String[]> getSubstringMap()
	{
		return substringMap;
	}

	public Map<String, String[]> getMeasureMap()
	{
		return measureMap;
	}

	public Map<String, String[]> getEnglishMap()
	{
		return englishMap;
	}

	public Map<String, Integer> getPastHitsMap()
	{
		return pastHitsMap;
	}
}
