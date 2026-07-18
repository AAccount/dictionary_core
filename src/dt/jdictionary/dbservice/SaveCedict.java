package dt.jdictionary.dbservice;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import dt.cedict.CedictDump;
import dt.cedict.MeasureWords;
import dt.cedict.SimpleLookup;
import dt.cedict.ZhPinyin;
import dt.jdictionary.ChineseSummaryLookup;
import dt.jdictionary.ProgressListener;
import dt.jdictionary.dbrepo.DbRepo;
import dt.jdictionary.dbrepo.raw.RawMeasureWordRow;
import dt.jdictionary.dbrepo.raw.RawSimplifiedRow;
import dt.jdictionary.dbrepo.raw.RawSubstringRow;
import dt.jdictionary.util.GenerateSubstrings;
import dt.util.ChineseText;
import dt.util.Debug;
import dt.util.MapUtil;

public class SaveCedict
{
	private static final String PROGRESS_DESC = "Saving to disk";
	
	private final DbRepo db;
	private final List<SimpleLookup> dictionary;
	private final List<RawSubstringRow> substringLines;
	private final List<RawMeasureWordRow> measureWordLines;
	private final List<RawSimplifiedRow> simplifiedLines;
	private final ProgressListener externalListener;
	private final int totalExpectedWrites;
	
	public SaveCedict(DbRepo db, CedictDump dump, ProgressListener listener)
	{
		this.db = db;
		this.dictionary = dump.getDictionary();
		this.substringLines = fillSubstrings(dictionary);
		this.measureWordLines = fillMeasureWords(dump.getMeasureWords());
		this.simplifiedLines = fillSimplified(dump.getSimplifiedChars());
		this.externalListener = listener;
		
		this.totalExpectedWrites = 
				dictionary.size() +
				measureWordLines.size() +
				simplifiedLines.size() +
				substringLines.size();
	}
	
	public void save() throws SQLException
	{
		if(this.dictionary.size() == 0)
		{
			Debug.logTimestamp("Empty dump. Don't wipe!");
			this.externalListener.onFractionalProgress(PROGRESS_DESC, 1, 1);
			return;
		}
				
		db.wipe();
		db.init();
		db.fillDictionary(dictionary);
		db.fillMeasureWords(measureWordLines);
		db.fillSimplified(simplifiedLines);
		db.fillSubstrings(substringLines);
	}

	private List<RawSubstringRow> fillSubstrings(List<SimpleLookup> dictionary)
	{
		final List<SimpleLookup> substringEntries = dictionary.stream()
			.filter(entry -> entry.getZh().length() > 1 && ChineseText.allChinese(entry.getZh()))
			.toList();

		final Set<RawSubstringRow> result = new HashSet<>();
		for(final SimpleLookup simpleLookup : substringEntries)
		{
			final List<String> substrings = GenerateSubstrings.generateSubstrings(simpleLookup.getZh());
			for(final String substring : substrings)
			{
				result.add(new RawSubstringRow(substring, simpleLookup.getZh()));
			}
		}
		return new ArrayList<>(result); 
	}

	private List<RawMeasureWordRow> fillMeasureWords(List<MeasureWords> measureWords)
	{
		final List<RawMeasureWordRow> result = new ArrayList<>();
		for(final MeasureWords mw : measureWords)
		{
			final String noun = mw.getZh();
			for(final ZhPinyin entry : mw.getMeasures())
			{
				result.add(new RawMeasureWordRow(noun, entry.getZh(), entry.getPinyin()));
			}
		}
		return result;
	}

	private List<RawSimplifiedRow> fillSimplified(Map<String, String> simplifiedChars)
	{
		final List<RawSimplifiedRow> simplifieds = new ArrayList<>();
		for(final String original : simplifiedChars.keySet())
		{
			simplifieds.add(new RawSimplifiedRow(original, simplifiedChars.get(original)));
		}
		return simplifieds;
	}
	
	private Map<String, List<String>> englishDefWordsToChineseMap(List<ChineseSummaryLookup> allEntries)
	{
		final Map<String, List<String>> englishMap = new HashMap<>();
		for(final ChineseSummaryLookup entry : allEntries)
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
				MapUtil.addToListMap(englishMap, word, entry.getChinese());
			}
		}
		return englishMap;
	}

	// @Override
	// public void onDiskWrite(DumpFile dumpFile, int writes)
	// {
	// 	int previousWrites = 0;
	// 	switch(dumpFile)
	// 	{
	// 		case ENGLISH:
	// 			previousWrites = this.dictionary.size();
	// 			break;
	// 		case MEASURE_WORDS:
	// 			previousWrites = this.dictionary.size();
	// 			break;
	// 		case SIMPLIFIED:
	// 			previousWrites = this.dictionary.size() + this.measureWordLines.size();
	// 			break;
	// 		case SUBSTRING:
	// 			previousWrites = this.dictionary.size() + this.measureWordLines.size() + this.simplifiedLines.size();
	// 			break;
	// 		case CHINESE: // This is the first dump to be written. There are no previous writes.
	// 			break;
	// 		default:
	// 			break;
	// 	}
	// 	this.externalListener.onFractionalProgress(PROGRESS_DESC, previousWrites + writes, totalExpectedWrites);
	// }
}
