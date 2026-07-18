package dt.jdictionary.dbservice;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import dt.cedict.CedictDump;
import dt.cedict.MeasureWords;
import dt.cedict.SimpleLookup;
import dt.cedict.ZhPinyin;
import dt.jdictionary.ProgressListener;
import dt.jdictionary.dbrepo.DbRepo;
import dt.jdictionary.dbrepo.raw.RawMeasureWordRow;
import dt.jdictionary.dbrepo.raw.RawSimplifiedRow;
import dt.jdictionary.dbrepo.raw.RawSubstringRow;
import dt.jdictionary.util.GenerateSubstrings;
import dt.util.ChineseText;
import dt.util.Debug;

public class SaveCedict
{
	private static final String PROGRESS_DESC = "Saving to disk";
	
	private final DbRepo db;
	private final List<SimpleLookup> dictionary;
	private final List<RawSubstringRow> substringLines;
	private final List<RawMeasureWordRow> measureWordLines;
	private final List<RawSimplifiedRow> simplifiedLines;
	private final ProgressListener externalListener;
	
	public SaveCedict(DbRepo db, CedictDump dump, ProgressListener listener)
	{
		this.db = db;
		this.dictionary = dump.getDictionary();
		this.substringLines = fillSubstrings(dictionary);
		this.measureWordLines = fillMeasureWords(dump.getMeasureWords());
		this.simplifiedLines = fillSimplified(dump.getSimplifiedChars());
		this.externalListener = listener;
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
}
