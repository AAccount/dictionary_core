package dt.jdictionary.dbservice.alternative;

import java.util.List;

import dt.jdictionary.ChineseSummaryLookup;
import dt.jdictionary.dbservice.DbServiceUtils;
import dt.jdictionary.dumpdb.DumpDBRepo;
import dt.jdictionary.dumpdb.RelatedChar;
import dt.jdictionary.dumpdb.line.DictionaryLine;

public class SameBackSearch implements AlternateSearch
{
	private final String zh;
	private final DumpDBRepo db;
	
	public SameBackSearch(String zh, DumpDBRepo db)
	{
		this.zh = zh;
		this.db = db;
	}

	@Override
	public List<ChineseSummaryLookup> trySearch()
	{
		final String lastChar = Character.toString(this.zh.charAt(this.zh.length()-1));
		final List<DictionaryLine> rawResults = this.db.lookupRelatedWord(lastChar, RelatedChar.SAME_BACK);
		return DbServiceUtils.convertRawToSimple(rawResults);
	}

	@Override
	public String LOOKUP_NAME()
	{
		return "Same Back";
	}
}
