package dt.jdictionary.dbservice.alternative;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import dt.jdictionary.ChineseSummaryLookup;
import dt.jdictionary.dbrepo.DbRepo;
import dt.jdictionary.dbservice.DbServiceUtils;

public class SubstringOfSearch implements AlternateSearch
{
	private final String zh;
	private final DbRepo db;
	
	public SubstringOfSearch(String zh, DbRepo db)
	{
		this.zh = zh;
		this.db = db;
	}

	@Override
 	public List<ChineseSummaryLookup> trySearch() throws SQLException
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
