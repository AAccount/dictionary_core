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
import dt.jdictionary.dumpdb.line.DictionaryLine;
import dt.jdictionary.dumpdb.line.MeasureWordLine;
import dt.jdictionary.dumpdb.line.PastHit;
import dt.jdictionary.dumpdb.line.SimplifiedLine;
import dt.jdictionary.dumpdb.line.SubstringLine;
import dt.util.ChineseText;
import dt.util.J9Shorthand;
import dt.util.MapUtil;

public class DumpDBRepo
{

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
	
	public DumpDBRepo() throws IOException, ParseException
	{
		final List<String> dumpFiles = J9Shorthand.list(DUMP_CHINESE, DUMP_ENGLISH, DUMP_MEASURE, DUMP_PAST, DUMP_SIMPLIFIED, DUMP_SUBSTRING);
		for(final String dump : dumpFiles)
		{
			final File file = new File(dump);
			file.getParentFile().mkdirs();
			file.createNewFile();
		}
		
		pastHitsWriter = new PrintWriter(new FileWriter(DUMP_PAST, true));
		loadIndices();
		loadKeyListOfValues(DUMP_ENGLISH, englishMap);
		loadKeyListOfValues(DUMP_SUBSTRING, substringMap);
		loadKeyListOfValues(DUMP_MEASURE, measureMap);
		loadSimplified();
		loadPastHits();
	}
	
	private void loadIndices() throws IOException
	{
		final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(DUMP_CHINESE), StandardCharsets.UTF_8));
		String line = reader.readLine();
		while(line != null)
		{
			final String[] parts = line.split(DELIM);
			final DictionaryLine row = new DictionaryLine(
					parts[0], 
					parts[1], 
					parts[2], 
					parts[3].replace(DELIM_ESC, DELIM), 
					parts[4].equals(NULL) ? null : parts[4], 
					parts[5].equals(NULL) ? null : parts[5], 
					Double.parseDouble(parts[6])
			);
			MapUtil.addToListMap(indexByChinese, row.getZh(), row);
			MapUtil.addToListMap(indexByPinyinNorm, row.getPinyinNormalized(), row);
			MapUtil.addToListMap(indexByFirstChar, row.getFirstChar(), row);
			MapUtil.addToListMap(indexByLastChar, row.getLastChar(), row);
			line = reader.readLine();
		}
		reader.close();
	}
	
	private void loadSimplified() throws IOException
	{
		final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(DUMP_SIMPLIFIED), StandardCharsets.UTF_8));
		String line = reader.readLine();
		while(line != null)
		{
			final String[] parts = line.split(DELIM);
			simplifiedMap.put(parts[0], parts[1]);
			line = reader.readLine();
		}
		reader.close();
	}
	
	private void loadKeyListOfValues(String dumpFile, Map<String, List<String>> target) throws IOException
	{
		final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(dumpFile), StandardCharsets.UTF_8));
		String line = reader.readLine();
		while(line != null)
		{
			final String[] parts = line.split(DELIM);
			MapUtil.addToListMap(target, parts[0], parts[1]);
			line = reader.readLine();
		}
		reader.close();
	}
	
	private void loadPastHits() throws IOException, ParseException
	{
		final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(DUMP_PAST), StandardCharsets.UTF_8));
		String line = reader.readLine();
		while(line != null)
		{
			final String[] parts = line.split(DELIM);
			MapUtil.addToListMap(pastHitsMap, parts[0], dateFormatter.parse(parts[1]));
			line = reader.readLine();
		}
		reader.close();
	}
	
	public void init()
	{
		// Nothing to initialize. Instantiating already creates the dump files as needed.
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
			MapUtil.addToListMap(pastHitsMap, hit, now);
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
		simplifiedCache.put(zh, zhSimplified);
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

	public void fillDictionary(List<ChineseSummaryLookup> allEntries) throws IOException
	{
		final Map<String, List<String>> newEnglishMap = new HashMap<>();
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
			addToNewEnglishMap(newEnglishMap, entry);
		}
		chineseDumpWriter.close();
		
		final PrintWriter englishDumpWriter = new PrintWriter(new FileWriter(DUMP_ENGLISH, false));
		for(final String word : newEnglishMap.keySet())
		{
			final List<String> potentials = newEnglishMap.get(word);
			for(final String potential : potentials)
			{
				englishDumpWriter.println(String.format("%s%s%s", word, DELIM, potential));
			}
		}
		englishDumpWriter.close();
		
		loadIndices();
		loadKeyListOfValues(DUMP_ENGLISH, englishMap);
	}
	
	private void addToNewEnglishMap(Map<String, List<String>> newEnglishMap, ChineseSummaryLookup entry)
	{
		final List<String> words = new ArrayList<>();

		final String nonHyphenated = entry.getDefinition().replaceAll("\\-", " ");
		final String[] defWords = nonHyphenated.split(" ");
		for(final String defWord : defWords)
		{
			final String cleaned = defWord.replaceAll("[^a-zA-Z]", "");
			if(!cleaned.isEmpty())
			{
				words.add(cleaned);
			}
		}
				
		for(final String word : words)
		{
			MapUtil.addToListMap(newEnglishMap, word, entry.getChinese());
		}
	}

	public void fillMeasureWords(List<MeasureWordLine> allRows) throws IOException
	{
		final PrintWriter measureWriter = new PrintWriter(new FileWriter(DUMP_MEASURE, false));
		for(final MeasureWordLine row : allRows)
		{
			measureWriter.println(String.format("%s%s%s%s%s", row.getZh(), DELIM, row.getMeasure(), DELIM, row.getMeasurePinyin()));
		}
		measureWriter.close();		
		loadKeyListOfValues(DUMP_MEASURE, measureMap);
	}

	public void fillSimplified(List<SimplifiedLine> allRows) throws IOException
	{
		final PrintWriter simplifiedWriter = new PrintWriter(new FileWriter(DUMP_SIMPLIFIED, false));
		for(final SimplifiedLine row : allRows)
		{
			simplifiedWriter.println(String.format("%s%s%s", row.getOriginal(), DELIM, row.getSimplified()));
		}
		simplifiedWriter.close();			
		loadSimplified();
	}

	public void fillSubstrings(List<SubstringLine> allRows) throws IOException
	{
		final PrintWriter simplifiedWriter = new PrintWriter(new FileWriter(DUMP_SUBSTRING, false));
		for(final SubstringLine row : allRows)
		{
			simplifiedWriter.println(String.format("%s%s%s", row.getSubstring(), DELIM, row.getFullString()));
		}
		simplifiedWriter.close();		
		loadKeyListOfValues(DUMP_SUBSTRING, substringMap);
	}

}
