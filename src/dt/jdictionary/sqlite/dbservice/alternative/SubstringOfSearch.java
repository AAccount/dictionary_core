package dt.jdictionary.sqlite.dbservice.alternative;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.sqlite.dbservice.DbServiceUtils;
import dt.jdictionary.sqlite.raw.IDbRepo;

public class SubstringOfSearch implements AlternateSearch
{
	private final String zh;
	private final IDbRepo db;
	
	public SubstringOfSearch(String zh, IDbRepo db)
	{
		this.zh = zh;
		this.db = db;
	}

	@Override
 	public List<SimpleLookup> trySearch() throws Exception
	{
		final List<String> possibleMatches = this.db.trySubstring(this.zh);
		if(possibleMatches.size() == 0)
		{
			return new ArrayList<>();
		}
		
		return DbServiceUtils.convertRawToSimple(this.db.lookupChinese(possibleMatches)).stream().collect(Collectors.toCollection(ArrayList::new));
	}

	@Override
	public String LOOKUP_NAME()
	{
		return "Substring Of";
	}
}
