package dt.jdictionary.dbservice.alternative;

import java.sql.SQLException;
import java.util.List;

import dt.jdictionary.dbrepo.DbRepo;
import dt.jdictionary.dbrepo.DictionaryEntry;
import dt.jdictionary.dbrepo.raw.RelatedChar;

public class SameBackSearch implements AlternateSearch
{
	private final String chinese;
	private final DbRepo db;
	
	public SameBackSearch(String chinese, DbRepo db)
	{
		this.chinese = chinese;
		this.db = db;
	}

	@Override
	public List<DictionaryEntry> trySearch() throws SQLException
	{
		final int[] codepoints = this.chinese.codePoints().toArray();
		final String lastChar = Character.toString(codepoints[codepoints.length-1]);
		return this.db.lookupRelatedWord(lastChar, RelatedChar.SAME_BACK);
	}

	@Override
	public String LOOKUP_NAME()
	{
		return "Same Back";
	}
}
