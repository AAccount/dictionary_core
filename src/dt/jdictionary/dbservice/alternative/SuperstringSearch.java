package dt.jdictionary.dbservice.alternative;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import dt.jdictionary.dbrepo.DbRepo;
import dt.jdictionary.dbrepo.raw.RawDictionaryRow;

public class SuperstringSearch implements AlternateSearch
{
	private final String zh;
	private final DbRepo db;
	
	public SuperstringSearch(String zh, DbRepo db)
	{
		this.zh = zh;
		this.db = db;
	}

	@Override
 	public List<RawDictionaryRow> trySearch() throws SQLException
	{
		final List<String> possibleMatches = this.db.lookupSuperstrings(this.zh);
		if(possibleMatches.size() == 0)
		{
			return new ArrayList<>();
		}
		
		return this.db.lookupChinese(possibleMatches).stream().collect(Collectors.toCollection(ArrayList::new));
	}

	@Override
	public String LOOKUP_NAME()
	{
		return "Superstring";
	}
}
