package dt.jdictionary.dumpdb;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
import dt.util.MapUtil;

public class DumpDBRepo
{
	private static final String NULL = "(NULL)";
	private static final DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS");
	
	private final Map<String, String> masterStringPool = new HashMap<>();
	private final Map<String, String> simplifiedMap = new HashMap<>();
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
		this.loadFromDisk(true, initListener);
	}
	
	public void loadFromDisk(boolean includePastHits, InitListener initListener) throws IOException, ParseException
	{
		final DumpDbParseResult parseResult = new DumpDbFileParser(initListener).loadAll();
		this.indexByChineseRo.putAll(parseResult.getIndexByChinese());
		this.indexByPinyinNormRo.putAll(parseResult.getIndexByPinyinNorm());
		this.indexByFirstCharRo.putAll(parseResult.getIndexByFirstChar());
		this.indexByLastCharRo.putAll(parseResult.getIndexByLastChar());
		this.simplifiedMap.putAll(parseResult.getSimplifiedMap());
		this.substringMapRo.putAll(parseResult.getSubstringMap());
		this.measureMapRo.putAll(parseResult.getMeasureMap());
		this.englishMapRo.putAll(parseResult.getEnglishMap());
		if(includePastHits)
		{
			this.pastHitsMap.putAll(parseResult.getPastHitsMap());
		}
	}
	
	private String masterStringsWrapper(String target)
	{

		if(!this.masterStringPool.containsKey(target))
		{
			this.masterStringPool.put(target, target);
		}
		return this.masterStringPool.get(target);
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
			this.pastHitsWriter.println(String.format("%s%s%s", hit, DumpDbConstants.DELIM, dateFormatter.format(now)));
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
			final String definition = entry.getDefinition().toLowerCase().replace(DumpDbConstants.DELIM, DumpDbConstants.DELIM_ESC);
			chineseDumpWriter.println(String.format("%s%s%s%s%s%s%s%s%s%s%s%s%f", 
					entry.getChinese(), DumpDbConstants.DELIM, 
					entry.getPinyin(), DumpDbConstants.DELIM, 
					ChineseText.normalizePinyin(entry.getPinyin()), DumpDbConstants.DELIM,
					definition, DumpDbConstants.DELIM,
					firstChar, DumpDbConstants.DELIM,
					lastChar, DumpDbConstants.DELIM,
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
				englishDumpWriter.println(String.format("%s%s%s", word, DumpDbConstants.DELIM, potential));
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
			measureWriter.println(String.format("%s%s%s%s%s", row.getZh(), DumpDbConstants.DELIM, row.getMeasure(), DumpDbConstants.DELIM, row.getMeasurePinyin()));
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
			simplifiedWriter.println(String.format("%s%s%s", row.getOriginal(), DumpDbConstants.DELIM, row.getSimplified()));
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
			simplifiedWriter.println(String.format("%s%s%s", row.getSubstring(), DumpDbConstants.DELIM, row.getFullString()));
			writes++;
			fillListener.onDiskWrite(DumpFile.SUBSTRING, writes);
		}
		simplifiedWriter.close();		
	}
}
