package dt.jdictionary.dbservice.alternative;

import java.sql.SQLException;
import java.util.List;

import dt.jdictionary.ChineseSummaryLookup;

public interface AlternateSearch 
{
	public abstract List<ChineseSummaryLookup> trySearch() throws SQLException;
	public abstract String LOOKUP_NAME();
}
