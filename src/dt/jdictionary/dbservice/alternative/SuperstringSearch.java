package dt.jdictionary.dbservice.alternative;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import dt.jdictionary.dbrepo.DbRepo;
import dt.jdictionary.dbrepo.DictionaryEntry;

public class SuperstringSearch implements AlternateSearch
{
	private final String chinese;
	private final DbRepo db;
	
	public SuperstringSearch(String chinese, DbRepo db)
	{
		this.chinese = chinese;
		this.db = db;
	}

	@Override
 	public List<DictionaryEntry> trySearch() throws SQLException
	{
		final List<String> possibleMatches = this.db.lookupSuperstrings(this.chinese);
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
