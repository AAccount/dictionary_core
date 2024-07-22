package dt.jdictionary.dbservice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import dt.jdictionary.ChineseSummaryLookup;
import dt.jdictionary.MeasureSummary;
import dt.jdictionary.dumpdb.DumpDBRepo;
import dt.jdictionary.dumpdb.line.MeasureWordLine;
import dt.jdictionary.dumpdb.line.SimplifiedLine;
import dt.jdictionary.dumpdb.line.SubstringLine;
import dt.jdictionary.util.GenerateSubstrings;
import dt.util.ChineseText;
import dt.util.Debug;

public class SaveCedict 
{
	public void save(List<ChineseSummaryLookup> dictionary, Map<String, List<MeasureSummary>> measureWords, Map<String, String> simplifiedChars, DumpDBRepo db) throws IOException
	{
		if(dictionary.size() == 0)
		{
			Debug.logTimestamp("Empty dump. Don't wipe!");
			return;
		}

//		final int dictionarySize = dump.getDictionary().size();
//		final int uptoDictTrxes = DbRepo.INIT_TRX_COUNT + dictionarySize + DbRepo.DICT_EN_TRX;
//		final int totalTrxes = uptoDictTrxes + DbRepo.POST_DICT_TRX;
//		progressListener.onProgress(0, totalTrxes);
		db.wipe();
//		progressListener.onProgress(1, totalTrxes);
		db.init();
//		progressListener.onProgress(2, totalTrxes);

		db.fillDictionary(dictionary);
		fillMeasureWords(measureWords, db);
//		progressListener.onProgress(uptoDictTrxes + 1, totalTrxes);
		fillSimplified(simplifiedChars, db);
//		progressListener.onProgress(uptoDictTrxes + 2, totalTrxes);
		fillSubstrings(dictionary, db);
//		progressListener.onProgress(uptoDictTrxes + 3, totalTrxes);
	}

	private void fillSubstrings(List<ChineseSummaryLookup> dictionary, DumpDBRepo db) throws IOException
	{
		final List<ChineseSummaryLookup> substringEntries = dictionary.stream()
			.filter(unrankedlookup -> unrankedlookup.getChinese().length() > 1 && ChineseText.allChinese(unrankedlookup.getChinese())).collect(Collectors.toCollection(ArrayList::new));

		final Set<SubstringLine> result = new HashSet<>();
		for(final ChineseSummaryLookup simpleLookup : substringEntries)
		{
			final List<String> substrings = GenerateSubstrings.generateSubstrings(simpleLookup.getChinese());
			for(final String substring : substrings)
			{
				result.add(new SubstringLine(substring, simpleLookup.getChinese()));
			}
		}
		db.fillSubstrings(new ArrayList<>(result));
	}

	private void fillMeasureWords(Map<String, List<MeasureSummary>> measureWords, DumpDBRepo db) throws IOException
	{
		final Set<MeasureWordLine> mwTracker = new HashSet<>();
		for(final String noun : measureWords.keySet())
		{
			for(final MeasureSummary summary : measureWords.get(noun))
			{
				mwTracker.add(new MeasureWordLine(noun, summary.getMeasureWord(), summary.getMeasurePinyin()));
			}
		}
		db.fillMeasureWords(new ArrayList<>(mwTracker));
	}

	private void fillSimplified(Map<String, String> simplifiedChars, DumpDBRepo db) throws IOException
	{
		final List<SimplifiedLine> simplifieds = new ArrayList<>();
		for(final String original : simplifiedChars.keySet())
		{
			simplifieds.add(new SimplifiedLine(original, simplifiedChars.get(original)));
		}
		db.fillSimplified(simplifieds);
	}
}
