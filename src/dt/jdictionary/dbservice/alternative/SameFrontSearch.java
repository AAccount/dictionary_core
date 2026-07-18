package dt.jdictionary.dbservice.alternative;

import java.sql.SQLException;
import java.util.List;

import dt.jdictionary.ChineseSummaryLookup;
import dt.jdictionary.dbrepo.DbRepo;
import dt.jdictionary.dbrepo.raw.RawDictionaryRow;
import dt.jdictionary.dbrepo.raw.RelatedChar;
import dt.jdictionary.dbservice.DbServiceUtils;

public class SameFrontSearch implements AlternateSearch
{
	private final String zh;
	private final DbRepo db;
	
	public SameFrontSearch(String zh, DbRepo db)
	{
		this.zh = zh;
		this.db = db;
	}

	@Override
	public List<ChineseSummaryLookup> trySearch() throws SQLException
	{
		final String firstChar = Character.toString(this.zh.charAt(0));
		final List<RawDictionaryRow> rawResults = this.db.lookupRelatedWord(firstChar, RelatedChar.SAME_FRONT);
		return DbServiceUtils.convertRawToSimple(rawResults);
	}

	@Override
	public String LOOKUP_NAME()
	{
		return "Same Front";
	}
}
