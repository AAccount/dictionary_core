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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dt.jdictionary.ChineseSummaryLookup;
import dt.jdictionary.InitListener;
import dt.jdictionary.dumpdb.line.DictionaryLine;
import dt.jdictionary.dumpdb.line.MeasureWordLine;
import dt.jdictionary.dumpdb.line.PastHit;
import dt.jdictionary.dumpdb.line.SimplifiedLine;
import dt.jdictionary.dumpdb.line.SubstringLine;
import dt.util.ChineseText;
import dt.util.J9Shorthand;
import dt.util.ListUtils;
import dt.util.MapUtil;

public class DumpDBRepo
{
	public static final String LOADED_ALL_DUMPS = "LOADED_ALL_DUMPS";
	
	private static final String DUMP_PREFIX = System.getProperty("user.home") + "/Programs/JDictionary/";
	private static final String DUMP_ENGLISH = DUMP_PREFIX + "dump_english";
	private static final String DUMP_CHINESE = DUMP_PREFIX + "dump_chinese";
	private static final String DUMP_MEASURE = DUMP_PREFIX + "dump_measure_words";
	private static final String DUMP_SIMPLIFIED = DUMP_PREFIX + "dump_simplified";
	private static final String DUMP_SUBSTRING = DUMP_PREFIX + "dump_substring";
	private static final String DUMP_PAST = DUMP_PREFIX + "dump_past_hits";
	
	private static final String NULL = "(NULL)";
	private static final String DELIM = "Ↄ"; // The discontinued Claudian C. Should never show up in normal cases.
	private static final String DELIM_ESC = "DELIM_ESC_CLAUDIAN_C";
	
	private static final DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS");
	
	// This strategy produces piles upon piles of strings which each require a pointer and actual memory for the string.
	// Attempt to keep a master set of strings to avoid gratuitous duplicates. Example: 10 "我" strings.
	// If a string is not in the master pool, it will be added. If it is, the master pool version will be used and the "original" GCed.
	private final Map<String, String> masterStringPool = new HashMap<>();
	private final Map<String, List<DictionaryLine>> indexByChinese = new HashMap<>();
	private final Map<String, List<DictionaryLine>> indexByPinyinNorm = new HashMap<>();
	private final Map<String, List<DictionaryLine>> indexByFirstChar = new HashMap<>();
	private final Map<String, List<DictionaryLine>> indexByLastChar = new HashMap<>();
	private final Map<String, String> simplifiedMap = new HashMap<>();
	private final Map<String, List<String>> substringMap = new HashMap<>();
	private final Map<String, List<String>> measureMap = new HashMap<>();
	private final Map<String, List<String>> englishMap = new HashMap<>();
	private final Map<String, List<Date>> pastHitsMap = new HashMap<>();
	private final Map<String, String> simplifiedCache = new HashMap<>();
	
	private final PrintWriter pastHitsWriter;
	
	public DumpDBRepo(InitListener initListener) throws IOException, ParseException
	{
		final List<String> dumpFiles = J9Shorthand.list(DUMP_CHINESE, DUMP_ENGLISH, DUMP_MEASURE, DUMP_PAST, DUMP_SIMPLIFIED, DUMP_SUBSTRING);
		for(final String dump : dumpFiles)
		{
			final File file = new File(dump);
			file.getParentFile().mkdirs();
			file.createNewFile();
		}
		
		pastHitsWriter = new PrintWriter(new FileWriter(DUMP_PAST, true));
		loadIndices(initListener);
		loadKeyListOfValues(DUMP_ENGLISH, englishMap, initListener);
		loadKeyListOfValues(DUMP_SUBSTRING, substringMap, initListener);
		loadKeyListOfValues(DUMP_MEASURE, measureMap, initListener);
		loadSimplified(initListener);
		loadPastHits(initListener);
		initListener.onAnyProgress(LOADED_ALL_DUMPS, 100);
		
		// The main string dedup has been done. Clear the giant 450000 entry hash map.
		masterStringPool.clear();
	}
	
	private void initListenerWrapper(InitListener initListener, String desc, int amount)
	{
		if(initListener != null && amount % 1000 == 0) // printing every update dramatically slows down the loading time
		{
			initListener.onAnyProgress(desc, amount);
		}
	}
	
	private void loadIndices(InitListener initListener) throws IOException
	{
		final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(DUMP_CHINESE), StandardCharsets.UTF_8));
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
			MapUtil.addToListMap(indexByChinese, this.masterStringsWrapper(chinese), row);
			MapUtil.addToListMap(indexByPinyinNorm, this.masterStringsWrapper(pinyinNormalized), row);
			
			final List<String> trueChars = ChineseText.trueChars(chinese);
			if(trueChars.size() > 1)
			{
				MapUtil.addToListMap(indexByFirstChar, this.masterStringsWrapper(trueChars.get(0)), row);
				MapUtil.addToListMap(indexByLastChar, this.masterStringsWrapper(ListUtils.last(trueChars)), row);
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
			masterStringPool.put(target, target);
		}
		return masterStringPool.get(target);
	}
	
	private void loadSimplified(InitListener initListener) throws IOException
	{
		final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(DUMP_SIMPLIFIED), StandardCharsets.UTF_8));
		String line = reader.readLine();
		int linesParsed = 0;
		while(line != null)
		{
			final String[] parts = line.split(DELIM);
			simplifiedMap.put(this.masterStringsWrapper(parts[0]), this.masterStringsWrapper(parts[1]));
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
		final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(DUMP_PAST), StandardCharsets.UTF_8));
		String line = reader.readLine();
		int linesParsed = 0;
		while(line != null)
		{
			final String[] parts = line.split(DELIM);
			MapUtil.addToListMap(pastHitsMap, this.masterStringsWrapper(parts[0]), dateFormatter.parse(parts[1]));
			linesParsed++;
			initListenerWrapper(initListener, "Parsing past hits", linesParsed);
			line = reader.readLine();
		}
		reader.close();
	}

	public void wipe() throws IOException
	{
		final List<String> toWipe = J9Shorthand.list(DUMP_CHINESE, DUMP_ENGLISH, DUMP_MEASURE, DUMP_SIMPLIFIED, DUMP_SUBSTRING);
		for(final String wipeMe : toWipe)
		{
			final File wipable = new File(wipeMe);
			wipable.delete();
			wipable.createNewFile();
		}
		
		J9Shorthand.list(indexByChinese, indexByPinyinNorm, indexByFirstChar, indexByLastChar, simplifiedMap, substringMap, englishMap, simplifiedCache).forEach(Map::clear);
	}

	public void saveHits(List<String> hits)
	{
		for(final String hit : hits)
		{
			final Date now = new Date();
			pastHitsWriter.println(String.format("%s%s%s", hit, DELIM, dateFormatter.format(now)));
			pastHitsWriter.flush();
			MapUtil.addToListMap(pastHitsMap, this.masterStringsWrapper(hit), now);
//			MapUtil.addToListMap(pastHitsMap, hit, now);
		}
	}

	public List<PastHit> lookupPastHits(List<String> candidates) throws ParseException
	{
		final List<PastHit> result = new ArrayList<>();
		for(final String candidate : candidates)
		{
			final List<Date> timestamps = pastHitsMap.getOrDefault(candidate, new ArrayList<>());
			for(final Date timestamp : timestamps)
			{
				result.add(new PastHit(candidate, timestamp));
			}
		}
		return result;
	}

	public List<DictionaryLine> lookupChinese(List<String> zhStrings)
	{
		final List<DictionaryLine> result = new ArrayList<>();
		for(final String zhString : zhStrings)
		{
			result.addAll(indexByChinese.getOrDefault(zhString, new ArrayList<>()));
		}
		return result;
	}

	public String lookupSimplified(String zh)
	{
		if(simplifiedCache.containsKey(zh))
		{
			return simplifiedCache.get(zh);
		}
		
		String zhSimplified = "";
		final List<String> chars = ChineseText.trueChars(zh);
		for(final String singleChar : chars)
		{
			final String singleSimplified = simplifiedMap.get(singleChar);
			if(singleSimplified == null)
			{
				zhSimplified = zhSimplified + singleChar;
			}
			else
			{
				zhSimplified = zhSimplified + singleSimplified;
			}
		}
		simplifiedCache.put(this.masterStringsWrapper(zh), this.masterStringsWrapper(zhSimplified));
//		simplifiedCache.put(zh, zhSimplified);
		return zhSimplified;
	}

	public List<String> lookupMeasureWords(String zh)
	{
		return measureMap.getOrDefault(zh, new ArrayList<>());
	}

	public List<DictionaryLine> lookupRelatedWord(String zh, RelatedChar similarity)
	{
		return  similarity == RelatedChar.SAME_FRONT ? indexByFirstChar.getOrDefault(zh, new ArrayList<>()) : indexByLastChar.getOrDefault(zh, new ArrayList<>());
	}

	public List<DictionaryLine> lookupEnglish(String en)
	{
		final List<String> chineseMatches = englishMap.get(en);
		final List<DictionaryLine> result = new ArrayList<>();
		for(final String match : chineseMatches)
		{
			result.addAll(indexByChinese.getOrDefault(match, new ArrayList<>()));
		}
		return result;
	}

	public List<String> trySubstring(String compoundWord)
	{
		return substringMap.getOrDefault(compoundWord, new ArrayList<>());
	}

	public List<DictionaryLine> findByNormalizedPinyin(List<String> normalizedPinyins)
	{
		final List<DictionaryLine> result = new ArrayList<>();
		for(final String zhString : normalizedPinyins)
		{
			result.addAll(indexByPinyinNorm.getOrDefault(zhString, new ArrayList<>()));
		}
		return result;	
	}

	public void fillDictionary(List<ChineseSummaryLookup> allEntries, DbFillListener fillListener) throws IOException
	{
		int writes = 0;
		final PrintWriter chineseDumpWriter = new PrintWriter(new FileWriter(DUMP_CHINESE, false));
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
		
		loadIndices(null);
	}
	
	public void fillEnglishMap(Map<String, List<String>> enToPossibleChinese, DbFillListener fillListener) throws IOException
	{
		int writes = 0;
		final PrintWriter englishDumpWriter = new PrintWriter(new FileWriter(DUMP_ENGLISH, false));
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
		loadKeyListOfValues(DUMP_ENGLISH, englishMap, null);
	}


	public void fillMeasureWords(List<MeasureWordLine> allRows, DbFillListener fillListener) throws IOException
	{
		int writes = 0;
		final PrintWriter measureWriter = new PrintWriter(new FileWriter(DUMP_MEASURE, false));
		for(final MeasureWordLine row : allRows)
		{
			measureWriter.println(String.format("%s%s%s%s%s", row.getZh(), DELIM, row.getMeasure(), DELIM, row.getMeasurePinyin()));
			writes++;
			fillListener.onDiskWrite(DumpFile.MEASURE_WORDS, writes);
		}
		measureWriter.close();		
		loadKeyListOfValues(DUMP_MEASURE, measureMap, null);
	}

	public void fillSimplified(List<SimplifiedLine> allRows, DbFillListener fillListener) throws IOException
	{
		int writes = 0;
		final PrintWriter simplifiedWriter = new PrintWriter(new FileWriter(DUMP_SIMPLIFIED, false));
		for(final SimplifiedLine row : allRows)
		{
			simplifiedWriter.println(String.format("%s%s%s", row.getOriginal(), DELIM, row.getSimplified()));
			writes++;
			fillListener.onDiskWrite(DumpFile.SIMPLIFIED, writes);
		}
		simplifiedWriter.close();			
		loadSimplified(null);
	}

	public void fillSubstrings(List<SubstringLine> allRows, DbFillListener fillListener) throws IOException
	{
		int writes = 0;
		final PrintWriter simplifiedWriter = new PrintWriter(new FileWriter(DUMP_SUBSTRING, false));
		for(final SubstringLine row : allRows)
		{
			simplifiedWriter.println(String.format("%s%s%s", row.getSubstring(), DELIM, row.getFullString()));
			writes++;
			fillListener.onDiskWrite(DumpFile.SUBSTRING, writes);
		}
		simplifiedWriter.close();		
		loadKeyListOfValues(DUMP_SUBSTRING, substringMap, null);
	}
}
