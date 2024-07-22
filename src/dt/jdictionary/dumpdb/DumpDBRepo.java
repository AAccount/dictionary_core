package dt.jdictionary.dumpdb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.sqlite.raw.IDbRepo;
import dt.jdictionary.sqlite.raw.PastHit;
import dt.jdictionary.sqlite.raw.RawDictionaryRow;
import dt.jdictionary.sqlite.raw.RawMeasureWordRow;
import dt.jdictionary.sqlite.raw.RawSimplifiedRow;
import dt.jdictionary.sqlite.raw.RawSubstringRow;
import dt.jdictionary.sqlite.raw.RelatedChar;
import dt.util.ChineseText;
import dt.util.J9Shorthand;

public class DumpDBRepo implements IDbRepo
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
	
	private final Map<String, List<RawDictionaryRow>> indexByChinese = new HashMap<>();
	private final Map<String, List<RawDictionaryRow>> indexByPinyinNorm = new HashMap<>();
	private final Map<String, List<RawDictionaryRow>> indexByFirstChar = new HashMap<>();
	private final Map<String, List<RawDictionaryRow>> indexByLastChar = new HashMap<>();
	private final Map<String, String> simplifiedMap = new HashMap<>();
	private final Map<String, List<String>> substringMap = new HashMap<>();
	private final Map<String, List<String>> measureMap = new HashMap<>();
	private final Map<String, List<String>> englishMap = new HashMap<>();
	private final Map<String, List<Date>> pastHitsMap = new HashMap<>();
	
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
			final RawDictionaryRow row = new RawDictionaryRow(
					parts[0], 
					parts[1], 
					parts[2], 
					parts[3].replace(DELIM_ESC, DELIM), 
					parts[4].equals(NULL) ? null : parts[4], 
					parts[5].equals(NULL) ? null : parts[5], 
					Double.parseDouble(parts[6])
			);
			addToListMap(indexByChinese, row.getZh(), row);
			addToListMap(indexByPinyinNorm, row.getPinyinNormalized(), row);
			addToListMap(indexByFirstChar, row.getFirstChar(), row);
			addToListMap(indexByLastChar, row.getLastChar(), row);
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
			addToListMap(target, parts[0], parts[1]);
			line = reader.readLine();
		}
		reader.close();
	}
	
	private <K,V> void addToListMap(Map<K,List<V>> target, K key, V value)
	{
		if(key == null)
		{
			return;
		}
		
		if(!target.containsKey(key))
		{
			target.put(key, new ArrayList<>());
		}
		target.get(key).add(value);
	}
	
	private void loadPastHits() throws IOException, ParseException
	{
		final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(DUMP_PAST), StandardCharsets.UTF_8));
		String line = reader.readLine();
		while(line != null)
		{
			final String[] parts = line.split(DELIM);
			addToListMap(pastHitsMap, parts[0], dateFormatter.parse(parts[1]));
			line = reader.readLine();
		}
		reader.close();
	}
	
	@Override
	public void init() throws SQLException
	{
		// Nothing to initialize. Instantiating already creates the dump files as needed.
	}

	@Override
	public void wipe() throws SQLException, IOException
	{
		final List<String> toWipe = J9Shorthand.list(DUMP_CHINESE, DUMP_ENGLISH, DUMP_MEASURE, DUMP_SIMPLIFIED, DUMP_SUBSTRING);
		for(final String wipeMe : toWipe)
		{
			final File wipable = new File(wipeMe);
			wipable.delete();
			wipable.createNewFile();
		}
		
		J9Shorthand.list(indexByChinese, indexByPinyinNorm, indexByFirstChar, indexByLastChar, simplifiedMap, substringMap, englishMap).forEach(Map::clear);
	}

	@Override
	public void saveHits(List<String> hits) throws SQLException
	{
		for(final String hit : hits)
		{
			final Date now = new Date();
			pastHitsWriter.println(String.format("%s%s%s", hit, DELIM, dateFormatter.format(now)));
			pastHitsWriter.flush();
			addToListMap(pastHitsMap, hit, now);
		}
	}

	@Override
	public List<PastHit> lookupPastHits(List<String> candidates) throws SQLException, ParseException
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

	@Override
	public List<RawDictionaryRow> lookupChinese(List<String> zhStrings) throws SQLException
	{
		final List<RawDictionaryRow> result = new ArrayList<>();
		for(final String zhString : zhStrings)
		{
			result.addAll(indexByChinese.getOrDefault(zhString, new ArrayList<>()));
		}
		return result;
	}

	@Override
	public String lookupSimplified(String zh) throws SQLException
	{
		return simplifiedMap.getOrDefault(zh, "");
	}

	@Override
	public List<String> lookupMeasureWords(String zh) throws SQLException
	{
		return measureMap.getOrDefault(zh, new ArrayList<>());
	}

	@Override
	public List<RawDictionaryRow> lookupRelatedWord(String zh, RelatedChar similarity) throws SQLException
	{
		return  similarity == RelatedChar.SAME_FRONT ? indexByFirstChar.getOrDefault(zh, new ArrayList<>()) : indexByLastChar.getOrDefault(zh, new ArrayList<>());
	}

	@Override
	public List<RawDictionaryRow> lookupEnglish(String en) throws SQLException
	{
		final List<String> chineseMatches = englishMap.get(en);
		final List<RawDictionaryRow> result = new ArrayList<>();
		for(final String match : chineseMatches)
		{
			result.addAll(indexByChinese.getOrDefault(match, new ArrayList<>()));
		}
		return result;
	}

	@Override
	public List<String> trySubstring(String compoundWord) throws SQLException
	{
		return substringMap.getOrDefault(compoundWord, new ArrayList<>());
	}

	@Override
	public List<RawDictionaryRow> findByNormalizedPinyin(List<String> normalizedPinyins) throws SQLException
	{
		final List<RawDictionaryRow> result = new ArrayList<>();
		for(final String zhString : normalizedPinyins)
		{
			result.addAll(indexByPinyinNorm.getOrDefault(zhString, new ArrayList<>()));
		}
		return result;	
	}

	@Override
	public void fillDictionary(List<SimpleLookup> allEntries) throws SQLException, IOException
	{
		final Map<String, List<String>> newEnglishMap = new HashMap<>();
		final PrintWriter chineseDumpWriter = new PrintWriter(new FileWriter(DUMP_CHINESE, false));
		for(final SimpleLookup entry : allEntries)
		{
			final List<String> trueChars = ChineseText.trueChars(entry.getZh());
			final String firstChar = trueChars.size() > 1 ? trueChars.get(0) : NULL;
			final String lastChar = trueChars.size() > 1 ? trueChars.get(trueChars.size()-1) : NULL;
			final String definition = String.join(", ", entry.getDefinitions()).toLowerCase().replace(DELIM, DELIM_ESC);
			chineseDumpWriter.println(String.format("%s%s%s%s%s%s%s%s%s%s%s%s%f", 
					entry.getZh(), DELIM, 
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
	
	private void addToNewEnglishMap(Map<String, List<String>> newEnglishMap, SimpleLookup entry)
	{
		final List<String> words = new ArrayList<>();
		for(final String def : entry.getDefinitions())
		{
			final String nonHyphenated = def.replaceAll("\\-", " ");
			final String[] defWords = nonHyphenated.split(" ");
			for(final String defWord : defWords)
			{
				final String cleaned = defWord.replaceAll("[^a-zA-Z]", "");
				if(!cleaned.isEmpty())
				{
					words.add(cleaned);
				}
			}
		}
		
		for(final String word : words)
		{
			addToListMap(newEnglishMap, word, entry.getZh());
		}
	}

	@Override
	public void fillMeasureWords(List<RawMeasureWordRow> allRows) throws SQLException, IOException
	{
		final PrintWriter measureWriter = new PrintWriter(new FileWriter(DUMP_MEASURE, false));
		for(final RawMeasureWordRow row : allRows)
		{
			measureWriter.println(String.format("%s%s%s%s%s", row.getZh(), DELIM, row.getMeasure(), DELIM, row.getMeasurePinyin()));
		}
		measureWriter.close();		
		loadKeyListOfValues(DUMP_MEASURE, measureMap);
	}

	@Override
	public void fillSimplified(List<RawSimplifiedRow> allRows) throws SQLException, IOException
	{
		final PrintWriter simplifiedWriter = new PrintWriter(new FileWriter(DUMP_SIMPLIFIED, false));
		for(final RawSimplifiedRow row : allRows)
		{
			simplifiedWriter.println(String.format("%s%s%s", row.getSimplified(), DELIM, row.getOriginal()));
		}
		simplifiedWriter.close();			
		loadSimplified();
	}

	@Override
	public void fillSubstrings(List<RawSubstringRow> allRows) throws SQLException, IOException
	{
		final PrintWriter simplifiedWriter = new PrintWriter(new FileWriter(DUMP_SUBSTRING, false));
		for(final RawSubstringRow row : allRows)
		{
			simplifiedWriter.println(String.format("%s%s%s", row.getSubstring(), DELIM, row.getFullString()));
		}
		simplifiedWriter.close();		
		loadKeyListOfValues(DUMP_SUBSTRING, substringMap);
	}

}
