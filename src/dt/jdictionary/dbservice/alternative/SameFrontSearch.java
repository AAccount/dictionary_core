package dt.jdictionary.dbservice.alternative;

import java.sql.SQLException;
import java.util.List;

import dt.jdictionary.dbrepo.DbRepo;
import dt.jdictionary.dbrepo.DictionaryEntry;
import dt.jdictionary.dbrepo.raw.RelatedChar;

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
	public List<DictionaryEntry> trySearch() throws SQLException
	{
		final int[] codepoints = this.zh.codePoints().toArray();
		final String firstChar = Character.toString(codepoints[0]);
		return this.db.lookupRelatedWord(firstChar, RelatedChar.SAME_FRONT);
	}

	@Override
	public String LOOKUP_NAME()
	{
		return "Same Front";
	}
}
