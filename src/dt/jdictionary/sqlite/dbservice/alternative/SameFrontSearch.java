package dt.jdictionary.sqlite.dbservice.alternative;

import java.sql.SQLException;
import java.util.List;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.sqlite.raw.IDbRepo;
import dt.jdictionary.sqlite.dbservice.DbServiceUtils;
import dt.jdictionary.sqlite.raw.RawDictionaryRow;
import dt.jdictionary.sqlite.raw.RelatedChar;

public class SameFrontSearch implements AlternateSearch
{
	private final String zh;
	private final IDbRepo db;
	
	public SameFrontSearch(String zh, IDbRepo db)
	{
		this.zh = zh;
		this.db = db;
	}

	@Override
	public List<SimpleLookup> trySearch() throws Exception
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
