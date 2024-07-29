package dt.jdictionary.dumpdb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dt.jdictionary.ChineseSummaryLookup;
import dt.jdictionary.InitListener;
import dt.jdictionary.dumpdb.line.DictionaryLine;
import dt.jdictionary.dumpdb.line.MeasureWordLine;
import dt.jdictionary.dumpdb.line.SimplifiedLine;
import dt.jdictionary.dumpdb.line.SubstringLine;
import dt.util.ChineseText;
import dt.util.J9Shorthand;
import dt.util.ListUtils;
import dt.util.MapUtil;

public class DumpDBRepo
{
	public static final String LOADED_ALL_DUMPS = "LOADED_ALL_DUMPS";
	private static final String NULL = "(NULL)";
	private static final String DELIM = "Ↄ"; // The discontinued Claudian C. Should never show up in normal cases.
	private static final String DELIM_ESC = "DELIM_ESC_CLAUDIAN_C";
	
	private static final DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS");
	
	// This strategy produces piles upon piles of strings which each require a pointer and actual memory for the string.
	// Attempt to keep a master set of strings to avoid gratuitous duplicates. Example: 10 "我" strings.
	// If a string is not in the master pool, it will be added. If it is, the master pool version will be used and the "original" GCed.
	private final Map<String, String> masterStringPool = new HashMap<>();
	private final Map<String, List<DictionaryLine>> indexByChineseRw = new HashMap<>();
	private final Map<String, List<DictionaryLine>> indexByPinyinNormRw = new HashMap<>();
	private final Map<String, List<DictionaryLine>> indexByFirstCharRw = new HashMap<>();
	private final Map<String, List<DictionaryLine>> indexByLastCharRw = new HashMap<>();
	private final Map<String, String> simplifiedMap = new HashMap<>();
	private final Map<String, List<String>> substringMapRw = new HashMap<>();
	private final Map<String, List<String>> measureMapRw = new HashMap<>();
	private final Map<String, List<String>> englishMapRw = new HashMap<>();
	private final Map<String, Integer> pastHitsMap = new HashMap<>();
	private final Map<String, String> simplifiedCache = new HashMap<>();
	
	private final Map<String, DictionaryLine[]> indexByChineseRo = new HashMap<>();
	private final Map<String, DictionaryLine[]> indexByPinyinNormRo = new HashMap<>();
	private final Map<String, DictionaryLine[]> indexByFirstCharRo = new HashMap<>();
	private final Map<String, DictionaryLine[]> indexByLastCharRo = new HashMap<>();
	private final Map<String, String[]> substringMapRo = new HashMap<>();
	private final Map<String, String[]> measureMapRo = new HashMap<>();
	private final Map<String, String[]> englishMapRo = new HashMap<>();
	
	private final PrintWriter pastHitsWriter;
	
	public DumpDBRepo(InitListener initListener) throws IOException, ParseException
	{
		final List<String> dumpFiles = J9Shorthand.list(DumpFile.CHINESE.getPath(), DumpFile.ENGLISH.getPath(), DumpFile.MEASURE_WORDS.getPath(), DumpFile.PAST.getPath(), DumpFile.SIMPLIFIED.getPath(), DumpFile.SUBSTRING.getPath());
		for(final String dump : dumpFiles)
		{
			final File file = new File(dump);
			file.getParentFile().mkdirs();
			file.createNewFile();
		}
		
		this.pastHitsWriter = new PrintWriter(new FileWriter(DumpFile.PAST.getPath(), true));
		this.loadAll(initListener);
	}

	public void loadAll(InitListener initListener) throws IOException, ParseException
	{
		this.loadIndices(initListener);
		this.createRoDicitionaryLine(this.indexByChineseRw, this.indexByChineseRo);
		this.createRoDicitionaryLine(this.indexByPinyinNormRw, this.indexByPinyinNormRo);
		this.createRoDicitionaryLine(this.indexByFirstCharRw, this.indexByFirstCharRo);
		this.createRoDicitionaryLine(this.indexByLastCharRw, this.indexByLastCharRo);
		this.loadKeyListOfValues(DumpFile.ENGLISH.getPath(), englishMapRw, initListener);
		this.createRoString(this.englishMapRw, this.englishMapRo);
		this.loadKeyListOfValues(DumpFile.SUBSTRING.getPath(), substringMapRw, initListener);
		this.createRoString(this.substringMapRw, this.substringMapRo);
		this.loadKeyListOfValues(DumpFile.MEASURE_WORDS.getPath(), measureMapRw, initListener);
		this.createRoString(this.measureMapRw, this.measureMapRo);
		this.loadSimplified(initListener);
		this.loadPastHits(initListener);
		this.initListenerWrapper(initListener, LOADED_ALL_DUMPS, 100);
		
		// The main string dedup has been done. Clear the giant 450000 entry hash map.
		this.masterStringPool.clear();
		J9Shorthand.list(this.indexByChineseRw, this.indexByFirstCharRw, this.indexByLastCharRw, this.indexByPinyinNormRw, this.englishMapRw, this.substringMapRw, this.measureMapRw).forEach(Map::clear);
	}
	
	private void createRoDicitionaryLine(Map<String, List<DictionaryLine>> rwmap, Map<String, DictionaryLine[]> romap)
	{
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
		rwmap.clear();
	}
	
	private void createRoString(Map<String, List<String>> rwmap, Map<String, String[]> romap)
	{
		for(final String key : rwmap.keySet())
		{
			final List<String> list = rwmap.get(key);
			final String[] array = new String[list.size()];
			for(int i=0; i<list.size(); i++)
			{
				array[i] = list.get(i);
			}
			romap.put(key, array);		}
		rwmap.clear();
	}
	
	private void initListenerWrapper(InitListener initListener, String desc, int amount)
	{
		if(initListener != null && (amount % 1000 == 0 || desc.equals(LOADED_ALL_DUMPS))) // printing every update dramatically slows down the loading time
		{
			initListener.onAnyProgress(desc, amount);
		}
	}
	
	private void loadIndices(InitListener initListener) throws IOException
	{
		final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(DumpFile.CHINESE.getPath()), StandardCharsets.UTF_8));
		String line = reader.readLine();
		int linesParsed = 0;
		while(line != null)
		{
			final String[] parts = line.split(DELIM);
			final String chinese = parts[0];
			final String pinyin = parts[1];
			final String pinyinNormalized = parts[2];
			final String def = parts[3].replace(DELIM_ESC, DELIM);
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
			initListenerWrapper(initListener, "Load Dictionary Indicies", linesParsed);
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
	
	private void loadSimplified(InitListener initListener) throws IOException
	{
		final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(DumpFile.SIMPLIFIED.getPath()), StandardCharsets.UTF_8));
		String line = reader.readLine();
		int linesParsed = 0;
		while(line != null)
		{
			final String[] parts = line.split(DELIM);
			this.simplifiedMap.put(this.masterStringsWrapper(parts[0]), this.masterStringsWrapper(parts[1]));
			linesParsed++;
			initListenerWrapper(initListener, "Parsed simplified", linesParsed);
			line = reader.readLine();
		}
		reader.close();
	}
	
	private void loadKeyListOfValues(String dumpFile, Map<String, List<String>> target, InitListener initListener) throws IOException
	{
		final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(dumpFile), StandardCharsets.UTF_8));
		String line = reader.readLine();
		int linesParsed = 0;
		final String[] filePath = dumpFile.split("/");
		final String fileName = filePath[filePath.length-1];
		while(line != null)
		{
			final String[] parts = line.split(DELIM);
			MapUtil.addToListMap(target, this.masterStringsWrapper(parts[0]), this.masterStringsWrapper(parts[1]));
			linesParsed++;
			initListenerWrapper(initListener, "Parsing file " + fileName, linesParsed);
			line = reader.readLine();
		}
		reader.close();
	}
	
	private void loadPastHits(InitListener initListener) throws IOException, ParseException
	{
		final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(DumpFile.PAST.getPath()), StandardCharsets.UTF_8));
		String line = reader.readLine();
		int linesParsed = 0;
		while(line != null)
		{
			final String[] parts = line.split(DELIM);
			MapUtil.incrementCounterMap(this.pastHitsMap, this.masterStringsWrapper(parts[0]));
			linesParsed++;
			initListenerWrapper(initListener, "Parsing past hits", linesParsed);
			line = reader.readLine();
		}
		reader.close();
	}

	public void wipe() throws IOException
	{
		final List<String> toWipe = J9Shorthand.list(DumpFile.CHINESE.getPath(), DumpFile.ENGLISH.getPath(), DumpFile.MEASURE_WORDS.getPath(), DumpFile.SIMPLIFIED.getPath(), DumpFile.SUBSTRING.getPath());
		for(final String wipeMe : toWipe)
		{
			final File wipable = new File(wipeMe);
			wipable.delete();
			wipable.createNewFile();
		}
		
		J9Shorthand.list(indexByChineseRo, indexByPinyinNormRo, indexByFirstCharRo, indexByLastCharRo, simplifiedMap, substringMapRo, englishMapRo, simplifiedCache).forEach(Map::clear);
	}

	public void saveHits(List<String> hits)
	{
		for(final String hit : hits)
		{
			final Date now = new Date();
			this.pastHitsWriter.println(String.format("%s%s%s", hit, DELIM, dateFormatter.format(now)));
			this.pastHitsWriter.flush();
			MapUtil.incrementCounterMap(this.pastHitsMap, this.masterStringsWrapper(hit));
		}
	}

	public Map<String, Integer> lookupPastHits(List<String> candidates) throws ParseException
	{
		final Map<String, Integer> result = new HashMap<>();
		for(final String candidate : candidates)
		{
			if(pastHitsMap.containsKey(candidate))
			{
				result.put(candidate, pastHitsMap.get(candidate));
			}
		}
		return result;
	}

	public List<DictionaryLine> lookupChinese(List<String> zhStrings)
	{
		final List<DictionaryLine> result = new ArrayList<>();
		for(final String zhString : zhStrings)
		{
			final DictionaryLine[] stringResult = this.indexByChineseRo.getOrDefault(zhString, new DictionaryLine[] {});
			result.addAll(Arrays.asList(stringResult));
		}
		return result;
	}

	public String lookupSimplified(String zh)
	{
		if(this.simplifiedCache.containsKey(zh))
		{
			return this.simplifiedCache.get(zh);
		}
		
		String zhSimplified = "";
		final List<String> chars = ChineseText.trueChars(zh);
		for(final String singleChar : chars)
		{
			final String singleSimplified = this.simplifiedMap.get(singleChar);
			if(singleSimplified == null)
			{
				zhSimplified = zhSimplified + singleChar;
			}
			else
			{
				zhSimplified = zhSimplified + singleSimplified;
			}
		}
		this.simplifiedCache.put(this.masterStringsWrapper(zh), this.masterStringsWrapper(zhSimplified));
		return zhSimplified;
	}

	public List<String> lookupMeasureWords(String zh)
	{
		return Arrays.asList(this.measureMapRo.getOrDefault(zh, new String[] {}));
	}

	public List<DictionaryLine> lookupRelatedWord(String zh, RelatedChar similarity)
	{
		return  similarity == RelatedChar.SAME_FRONT ? 
				Arrays.asList(this.indexByFirstCharRo.getOrDefault(zh, new DictionaryLine[] {})):
				Arrays.asList(this.indexByLastCharRo.getOrDefault(zh, new DictionaryLine[] {}));
	}

	public List<DictionaryLine> lookupEnglish(String en)
	{
		final String[] chineseMatches = englishMapRo.get(en);
		final List<DictionaryLine> result = new ArrayList<>();
		for(final String match : chineseMatches)
		{
			result.addAll(Arrays.asList(this.indexByChineseRo.getOrDefault(match, new DictionaryLine[] {})));
		}
		return result;
	}

	public List<String> trySubstring(String compoundWord)
	{
		return Arrays.asList(this.substringMapRo.getOrDefault(compoundWord, new String[] {}));
	}

	public List<DictionaryLine> findByNormalizedPinyin(List<String> normalizedPinyins)
	{
		final List<DictionaryLine> result = new ArrayList<>();
		for(final String zhString : normalizedPinyins)
		{
			result.addAll(Arrays.asList(this.indexByPinyinNormRo.getOrDefault(zhString, new DictionaryLine[] {})));
		}
		return result;	
	}

	public void fillDictionary(List<ChineseSummaryLookup> allEntries, DbFillListener fillListener) throws IOException
	{
		int writes = 0;
		final PrintWriter chineseDumpWriter = new PrintWriter(new FileWriter(DumpFile.CHINESE.getPath(), false));
		for(final ChineseSummaryLookup entry : allEntries)
		{
			final List<String> trueChars = ChineseText.trueChars(entry.getChinese());
			final String firstChar = trueChars.size() > 1 ? trueChars.get(0) : NULL;
			final String lastChar = trueChars.size() > 1 ? trueChars.get(trueChars.size()-1) : NULL;
			final String definition = entry.getDefinition().toLowerCase().replace(DELIM, DELIM_ESC);
			chineseDumpWriter.println(String.format("%s%s%s%s%s%s%s%s%s%s%s%s%f", 
					entry.getChinese(), DELIM, 
					entry.getPinyin(), DELIM, 
					ChineseText.normalizePinyin(entry.getPinyin()), DELIM,
					definition, DELIM,
					firstChar, DELIM,
					lastChar, DELIM,
					entry.getRank()));
			writes++;
			fillListener.onDiskWrite(DumpFile.CHINESE, writes);
		}
		chineseDumpWriter.close();
	}
	
	public void fillEnglishMap(Map<String, List<String>> enToPossibleChinese, DbFillListener fillListener) throws IOException
	{
		int writes = 0;
		final PrintWriter englishDumpWriter = new PrintWriter(new FileWriter(DumpFile.ENGLISH.getPath(), false));
		for(final String word : enToPossibleChinese.keySet())
		{
			final List<String> potentials = enToPossibleChinese.get(word);
			for(final String potential : potentials)
			{
				englishDumpWriter.println(String.format("%s%s%s", word, DELIM, potential));
				writes++;
				fillListener.onDiskWrite(DumpFile.ENGLISH, writes);
			}
		}
		englishDumpWriter.close();
	}


	public void fillMeasureWords(List<MeasureWordLine> allRows, DbFillListener fillListener) throws IOException
	{
		int writes = 0;
		final PrintWriter measureWriter = new PrintWriter(new FileWriter(DumpFile.MEASURE_WORDS.getPath(), false));
		for(final MeasureWordLine row : allRows)
		{
			measureWriter.println(String.format("%s%s%s%s%s", row.getZh(), DELIM, row.getMeasure(), DELIM, row.getMeasurePinyin()));
			writes++;
			fillListener.onDiskWrite(DumpFile.MEASURE_WORDS, writes);
		}
		measureWriter.close();		
	}

	public void fillSimplified(List<SimplifiedLine> allRows, DbFillListener fillListener) throws IOException
	{
		int writes = 0;
		final PrintWriter simplifiedWriter = new PrintWriter(new FileWriter(DumpFile.SIMPLIFIED.getPath(), false));
		for(final SimplifiedLine row : allRows)
		{
			simplifiedWriter.println(String.format("%s%s%s", row.getOriginal(), DELIM, row.getSimplified()));
			writes++;
			fillListener.onDiskWrite(DumpFile.SIMPLIFIED, writes);
		}
		simplifiedWriter.close();			
	}

	public void fillSubstrings(List<SubstringLine> allRows, DbFillListener fillListener) throws IOException
	{
		int writes = 0;
		final PrintWriter simplifiedWriter = new PrintWriter(new FileWriter(DumpFile.SUBSTRING.getPath(), false));
		for(final SubstringLine row : allRows)
		{
			simplifiedWriter.println(String.format("%s%s%s", row.getSubstring(), DELIM, row.getFullString()));
			writes++;
			fillListener.onDiskWrite(DumpFile.SUBSTRING, writes);
		}
		simplifiedWriter.close();		
	}
}
