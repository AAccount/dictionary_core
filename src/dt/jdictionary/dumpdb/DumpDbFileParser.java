package dt.jdictionary.dumpdb;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dt.jdictionary.InitListener;
import dt.jdictionary.dumpdb.line.DictionaryLine;
import dt.util.ChineseText;
import dt.util.ListUtils;
import dt.util.MapUtil;

public class DumpDbFileParser
{	
	// This strategy produces piles upon piles of strings which each require a pointer and actual memory for the string.
	// Attempt to keep a master set of strings to avoid gratuitous duplicates. Example: 10 "我" strings.
	// If a string is not in the master pool, it will be added. If it is, the master pool version will be used and the "original" GCed.
	private final Map<String, String> masterStringPool = new HashMap<>();
	private final InitListener listener;
	private final Map<String, List<DictionaryLine>> indexByChineseRw = new HashMap<>();
	private final Map<String, List<DictionaryLine>> indexByPinyinNormRw = new HashMap<>();
	private final Map<String, List<DictionaryLine>> indexByFirstCharRw = new HashMap<>();
	private final Map<String, List<DictionaryLine>> indexByLastCharRw = new HashMap<>();
	private final Map<String, String> simplifiedMap = new HashMap<>();
	private final Map<String, List<String>> substringMapRw = new HashMap<>();
	private final Map<String, List<String>> measureMapRw = new HashMap<>();
	private final Map<String, List<String>> englishMapRw = new HashMap<>();
	private final Map<String, Integer> pastHitsMap = new HashMap<>();
	
	public DumpDbFileParser(InitListener listener)
	{
		super();
		this.listener = listener;
	}
	
	public DumpDbParseResult loadAll() throws IOException, ParseException
	{
		this.loadIndices();
		this.loadKeyListOfValues(DumpFile.ENGLISH.getPath(), englishMapRw);
		this.loadKeyListOfValues(DumpFile.SUBSTRING.getPath(), substringMapRw);
		this.loadKeyListOfValues(DumpFile.MEASURE_WORDS.getPath(), measureMapRw);
		this.loadSimplified();
		this.loadPastHits();
		this.initListenerWrapper(DumpDbConstants.LOADED_ALL_DUMPS, 100);
		
		return new DumpDbParseResult(
				this.createRoDicitionaryLine(this.indexByChineseRw),
				this.createRoDicitionaryLine(this.indexByPinyinNormRw),
				this.createRoDicitionaryLine(this.indexByFirstCharRw),
				this.createRoDicitionaryLine(this.indexByLastCharRw),
				this.simplifiedMap,
				this.createRoString(this.substringMapRw),
				this.createRoString(this.measureMapRw),
				this.createRoString(this.englishMapRw),
				pastHitsMap);
	}
	
	private Map<String, DictionaryLine[]> createRoDicitionaryLine(Map<String, List<DictionaryLine>> rwmap)
	{
		final Map<String, DictionaryLine[]> romap = new HashMap<>();
		for(final String key : rwmap.keySet())
		{
			final List<DictionaryLine> list = rwmap.get(key);
			final DictionaryLine[] array = new DictionaryLine[list.size()];
			for(int i=0; i<list.size(); i++)
			{
				array[i] = list.get(i);
			}
			romap.put(key, array);
		}
		return romap;
	}
	
	private Map<String, String[]> createRoString(Map<String, List<String>> rwmap)
	{
		final Map<String, String[]> romap = new HashMap<>();
		for(final String key : rwmap.keySet())
		{
			final List<String> list = rwmap.get(key);
			final String[] array = new String[list.size()];
			for(int i=0; i<list.size(); i++)
			{
				array[i] = list.get(i);
			}
			romap.put(key, array);		
		}
		return romap;
	}
	
	private void initListenerWrapper(String desc, int amount)
	{
		if(this.listener != null && (amount % 1000 == 0 || desc.equals(DumpDbConstants.LOADED_ALL_DUMPS))) // printing every update dramatically slows down the loading time
		{
			this.listener.onAnyProgress(desc, amount);
		}
	}
	
	private void loadIndices() throws IOException
	{
		final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(DumpFile.CHINESE.getPath()), StandardCharsets.UTF_8));
		String line = reader.readLine();
		int linesParsed = 0;
		while(line != null)
		{
			final String[] parts = line.split(DumpDbConstants.DELIM);
			final String chinese = parts[0];
			final String pinyin = parts[1];
			final String pinyinNormalized = parts[2];
			final String def = parts[3].replace(DumpDbConstants.DELIM_ESC, DumpDbConstants.DELIM);
			final double rank = Double.parseDouble(parts[6]);
			
			final DictionaryLine row = new DictionaryLine(this.masterStringsWrapper(chinese), this.masterStringsWrapper(pinyin), def, rank);
			MapUtil.addToListMap(this.indexByChineseRw, this.masterStringsWrapper(chinese), row);
			MapUtil.addToListMap(this.indexByPinyinNormRw, this.masterStringsWrapper(pinyinNormalized), row);
			
			final List<String> trueChars = ChineseText.trueChars(chinese);
			if(trueChars.size() > 1)
			{
				MapUtil.addToListMap(this.indexByFirstCharRw, this.masterStringsWrapper(trueChars.get(0)), row);
				MapUtil.addToListMap(this.indexByLastCharRw, this.masterStringsWrapper(ListUtils.last(trueChars)), row);
			}
			linesParsed++;
			initListenerWrapper("Load Dictionary Indicies", linesParsed);
			line = reader.readLine();
		}
		reader.close();
	}
	
	private String masterStringsWrapper(String target)
	{

		if(!this.masterStringPool.containsKey(target))
		{
			this.masterStringPool.put(target, target);
		}
		return this.masterStringPool.get(target);
	}
	
	private void loadSimplified() throws IOException
	{
		final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(DumpFile.SIMPLIFIED.getPath()), StandardCharsets.UTF_8));
		String line = reader.readLine();
		int linesParsed = 0;
		while(line != null)
		{
			final String[] parts = line.split(DumpDbConstants.DELIM);
			this.simplifiedMap.put(this.masterStringsWrapper(parts[0]), this.masterStringsWrapper(parts[1]));
			linesParsed++;
			initListenerWrapper("Parsed simplified", linesParsed);
			line = reader.readLine();
		}
		reader.close();
	}
	
	private void loadKeyListOfValues(String dumpFile, Map<String, List<String>> target) throws IOException
	{
		final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(dumpFile), StandardCharsets.UTF_8));
		String line = reader.readLine();
		int linesParsed = 0;
		final String[] filePath = dumpFile.split("/");
		final String fileName = filePath[filePath.length-1];
		while(line != null)
		{
			final String[] parts = line.split(DumpDbConstants.DELIM);
			MapUtil.addToListMap(target, this.masterStringsWrapper(parts[0]), this.masterStringsWrapper(parts[1]));
			linesParsed++;
			initListenerWrapper("Parsing file " + fileName, linesParsed);
			line = reader.readLine();
		}
		reader.close();
	}
	
	private void loadPastHits() throws IOException, ParseException
	{
		final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(DumpFile.PAST.getPath()), StandardCharsets.UTF_8));
		String line = reader.readLine();
		int linesParsed = 0;
		while(line != null)
		{
			final String[] parts = line.split(DumpDbConstants.DELIM);
			MapUtil.incrementCounterMap(this.pastHitsMap, this.masterStringsWrapper(parts[0]));
			linesParsed++;
			initListenerWrapper("Parsing past hits", linesParsed);
			line = reader.readLine();
		}
		reader.close();
	}
}
