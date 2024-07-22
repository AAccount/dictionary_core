package dt.jdictionary.sqlite.dbservice;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import dt.jdictionary.MeasureWords;
import dt.jdictionary.SimpleLookup;
import dt.jdictionary.ZhPinyin;
import dt.jdictionary.sqlite.raw.IDbRepo;
import dt.jdictionary.sqlite.raw.RawSubstringRow;
import dt.jdictionary.util.GenerateSubstrings;
import dt.util.ChineseText;
import dt.util.Debug;
import dt.jdictionary.sqlite.raw.RawMeasureWordRow;
import dt.jdictionary.sqlite.raw.RawSimplifiedRow;

public class SaveCedict 
{
	public void save(List<SimpleLookup> dictionary, List<MeasureWords> measureWords, Map<String, String> simplifiedChars, IDbRepo db) throws Exception
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

	private void fillSubstrings(List<SimpleLookup> dictionary, IDbRepo db) throws Exception
	{
		final List<SimpleLookup> substringEntries = dictionary.stream()
			.filter(unrankedlookup -> unrankedlookup.getZh().length() > 1 && ChineseText.allChinese(unrankedlookup.getZh())).collect(Collectors.toCollection(ArrayList::new));

		final Set<RawSubstringRow> result = new HashSet<>();
		for(final SimpleLookup simpleLookup : substringEntries)
		{
			final List<String> substrings = GenerateSubstrings.generateSubstrings(simpleLookup.getZh());
			for(final String substring : substrings)
			{
				result.add(new RawSubstringRow(substring, simpleLookup.getZh()));
			}
		}
		db.fillSubstrings(new ArrayList<>(result));
	}

	private void fillMeasureWords(List<MeasureWords> measureWords, IDbRepo db) throws Exception
	{
		final Set<RawMeasureWordRow> mwTracker = new HashSet<>();
		for(final MeasureWords measureListing : measureWords)
		{
			for(final ZhPinyin measure : measureListing.getMeasures())
			{
				mwTracker.add(new RawMeasureWordRow(measureListing.getZh(), measure.getZh(), measure.getPinyin()));
			}
		}
		db.fillMeasureWords(new ArrayList<>(mwTracker));
	}

	private void fillSimplified(Map<String, String> simplifiedChars, IDbRepo db) throws Exception
	{
		final List<RawSimplifiedRow> simplifieds = new ArrayList<>();
		for(final String original : simplifiedChars.keySet())
		{
			simplifieds.add(new RawSimplifiedRow(original, simplifiedChars.get(original)));
		}
		db.fillSimplified(simplifieds);
	}
}
