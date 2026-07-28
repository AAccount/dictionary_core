package dt.jdictionary.dbservice.alternative;

import java.sql.SQLException;
import java.util.List;

import dt.jdictionary.ChineseSummaryLookup;
import dt.jdictionary.dbrepo.DbRepo;

public class SimplifiedSearch implements AlternateSearch
{
	private final String chinese;
	private final DbRepo db;

	public SimplifiedSearch(String chinese, DbRepo db) 
	{
		this.chinese = chinese;
		this.db = db;
	}

	@Override
	public List<ChineseSummaryLookup> trySearch() throws SQLException 
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'trySearch'");
	}

	@Override
	public String LOOKUP_NAME() 
	{
		return "Was Simplified";
	}

}
