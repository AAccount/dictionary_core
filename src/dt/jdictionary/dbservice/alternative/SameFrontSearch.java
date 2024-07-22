package dt.jdictionary.dbservice.alternative;

import java.util.List;

import dt.jdictionary.ChineseSummaryLookup;
import dt.jdictionary.dbservice.DbServiceUtils;
import dt.jdictionary.dumpdb.DumpDBRepo;
import dt.jdictionary.dumpdb.RelatedChar;
import dt.jdictionary.dumpdb.line.DictionaryLine;

public class SameFrontSearch implements AlternateSearch
{
	private final String zh;
	private final DumpDBRepo db;
	
	public SameFrontSearch(String zh, DumpDBRepo db)
	{
		this.zh = zh;
		this.db = db;
	}

	@Override
	public List<ChineseSummaryLookup> trySearch()
	{
		final String firstChar = Character.toString(this.zh.charAt(0));
		final List<DictionaryLine> rawResults = this.db.lookupRelatedWord(firstChar, RelatedChar.SAME_FRONT);
		return DbServiceUtils.convertRawToSimple(rawResults);
	}

	@Override
	public String LOOKUP_NAME()
	{
		return "Same Front";
	}
}
