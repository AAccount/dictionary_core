package dt.jdictionary.dbservice.alternative;

import java.sql.SQLException;
import java.util.List;

import dt.jdictionary.ChineseSummaryLookup;
import dt.jdictionary.dbrepo.DbRepo;
import dt.jdictionary.dbrepo.raw.RawDictionaryRow;
import dt.jdictionary.dbrepo.raw.RelatedChar;
import dt.jdictionary.dbservice.DbServiceUtils;

public class SameBackSearch implements AlternateSearch
{
	private final String zh;
	private final DbRepo db;
	
	public SameBackSearch(String zh, DbRepo db)
	{
		this.zh = zh;
		this.db = db;
	}

	@Override
	public List<ChineseSummaryLookup> trySearch() throws SQLException
	{
		final int[] codepoints = this.zh.codePoints().toArray();
		final String lastChar = Character.toString(codepoints[codepoints.length-1]);
		final List<RawDictionaryRow> rawResults = this.db.lookupRelatedWord(lastChar, RelatedChar.SAME_BACK);
		return DbServiceUtils.convertRawToSimple(rawResults);
	}

	@Override
	public String LOOKUP_NAME()
	{
		return "Same Back";
	}
}
