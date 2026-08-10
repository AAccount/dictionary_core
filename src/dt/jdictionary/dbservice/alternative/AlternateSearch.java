package dt.jdictionary.dbservice.alternative;

import java.sql.SQLException;
import java.util.List;

import dt.jdictionary.dbrepo.DictionaryEntry;

public interface AlternateSearch 
{
	public abstract List<DictionaryEntry> trySearch() throws SQLException;
	public abstract String LOOKUP_NAME();
}
