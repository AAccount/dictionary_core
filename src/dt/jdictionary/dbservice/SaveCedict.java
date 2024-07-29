package dt.jdictionary.dbservice;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import dt.jdictionary.ChineseSummaryLookup;
import dt.jdictionary.MeasureSummary;
import dt.jdictionary.ProgressListener;
import dt.jdictionary.dumpdb.DbFillListener;
import dt.jdictionary.dumpdb.DumpDBRepo;
import dt.jdictionary.dumpdb.DumpFile;
import dt.jdictionary.dumpdb.line.MeasureWordLine;
import dt.jdictionary.dumpdb.line.SimplifiedLine;
import dt.jdictionary.dumpdb.line.SubstringLine;
import dt.jdictionary.util.GenerateSubstrings;
import dt.util.ChineseText;
import dt.util.Debug;
import dt.util.MapUtil;

public class SaveCedict implements DbFillListener
{
	private static final String PROGRESS_DESC = "Saving to disk";
	
	private final DumpDBRepo db;
	private final List<ChineseSummaryLookup> dictionary;
	private final Map<String, List<String>> englishDefinitionWordToChinese;
	private final int totalEnglishLines;
	private final List<SubstringLine> substringLines;
	private final List<MeasureWordLine> measureWordLines;
	private final List<SimplifiedLine> simplifiedLines;
	private final ProgressListener externalListener;
	private final int totalExpectedWrites;
	
	public SaveCedict(DumpDBRepo db, List<ChineseSummaryLookup> dictionary, Map<String, List<MeasureSummary>> measureWords, Map<String, String> simplifiedChars,ProgressListener listener)
	{
		this.db = db;
		this.dictionary = dictionary;
		this.englishDefinitionWordToChinese = englishDefWordsToChineseMap(dictionary);
		this.totalEnglishLines = englishDefinitionWordToChinese.values().stream().reduce(0, (total, chineseList) -> total + chineseList.size(), Integer::sum);
		this.substringLines = fillSubstrings(dictionary);
		this.measureWordLines = fillMeasureWords(measureWords);
		this.simplifiedLines = fillSimplified(simplifiedChars);
		this.externalListener = listener;
		
		this.totalExpectedWrites = 
				dictionary.size() +
				this.totalEnglishLines +
				measureWordLines.size() +
				simplifiedLines.size() +
				substringLines.size();
	}
	
	public void save() throws IOException, ParseException
	{
		if(this.dictionary.size() == 0)
		{
			Debug.logTimestamp("Empty dump. Don't wipe!");
			this.externalListener.onFractionalProgress(PROGRESS_DESC, 1, 1);
			return;
		}
				
		db.wipe();
		db.fillDictionary(dictionary, this);
		db.fillEnglishMap(englishDefinitionWordToChinese, this);
		db.fillMeasureWords(measureWordLines, this);
		db.fillSimplified(simplifiedLines, this);
		db.fillSubstrings(substringLines, this);
		db.loadFromDisk(false, null);
	}

	private List<SubstringLine> fillSubstrings(List<ChineseSummaryLookup> dictionary)
	{
		final List<ChineseSummaryLookup> substringEntries = dictionary.stream()
			.filter(summary -> summary.getChinese().length() > 1 && ChineseText.allChinese(summary.getChinese()))
			.collect(Collectors.toCollection(ArrayList::new));

		final Set<SubstringLine> result = new HashSet<>();
		for(final ChineseSummaryLookup simpleLookup : substringEntries)
		{
			final List<String> substrings = GenerateSubstrings.generateSubstrings(simpleLookup.getChinese());
			for(final String substring : substrings)
			{
				result.add(new SubstringLine(substring, simpleLookup.getChinese()));
			}
		}
		return new ArrayList<>(result); 
	}

	private List<MeasureWordLine> fillMeasureWords(Map<String, List<MeasureSummary>> measureWords)
	{
		final Set<MeasureWordLine> mwTracker = new HashSet<>();
		for(final String noun : measureWords.keySet())
		{
			for(final MeasureSummary summary : measureWords.get(noun))
			{
				mwTracker.add(new MeasureWordLine(noun, summary.getMeasureWord(), summary.getMeasurePinyin()));
			}
		}
		return new ArrayList<>(mwTracker);
	}

	private List<SimplifiedLine> fillSimplified(Map<String, String> simplifiedChars)
	{
		final List<SimplifiedLine> simplifieds = new ArrayList<>();
		for(final String original : simplifiedChars.keySet())
		{
			simplifieds.add(new SimplifiedLine(original, simplifiedChars.get(original)));
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

	@Override
	public void onDiskWrite(DumpFile dumpFile, int writes)
	{
		int previousWrites = 0;
		switch(dumpFile)
		{
			case ENGLISH:
				previousWrites = this.dictionary.size();
				break;
			case MEASURE_WORDS:
				previousWrites = this.dictionary.size() + this.totalEnglishLines;
				break;
			case SIMPLIFIED:
				previousWrites = this.dictionary.size() + this.totalEnglishLines + this.measureWordLines.size();
				break;
			case SUBSTRING:
				previousWrites = this.dictionary.size() + this.totalEnglishLines + this.measureWordLines.size() + this.simplifiedLines.size();
				break;
			case CHINESE: // This is the first dump to be written. There are no previous writes.
				break;
			default:
				break;
		}
		this.externalListener.onFractionalProgress(PROGRESS_DESC, previousWrites + writes, totalExpectedWrites);
	}
}
