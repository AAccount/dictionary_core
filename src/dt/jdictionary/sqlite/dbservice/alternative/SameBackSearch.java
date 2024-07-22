package dt.jdictionary.sqlite.dbservice.alternative;

import java.sql.SQLException;
import java.util.List;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.sqlite.raw.IDbRepo;
import dt.jdictionary.sqlite.dbservice.DbServiceUtils;
import dt.jdictionary.sqlite.raw.RawDictionaryRow;
import dt.jdictionary.sqlite.raw.RelatedChar;

public class SameBackSearch implements AlternateSearch
{
	private final String zh;
	private final IDbRepo db;
	
	public SameBackSearch(String zh, IDbRepo db)
	{
		this.zh = zh;
		this.db = db;
	}

	@Override
	public List<SimpleLookup> trySearch() throws Exception
	{
		final String lastChar = Character.toString(this.zh.charAt(this.zh.length()-1));
		final List<RawDictionaryRow> rawResults = this.db.lookupRelatedWord(lastChar, RelatedChar.SAME_BACK);
		return DbServiceUtils.convertRawToSimple(rawResults);
	}

	@Override
	public String LOOKUP_NAME()
	{
		return "Same Back";
	}
}
