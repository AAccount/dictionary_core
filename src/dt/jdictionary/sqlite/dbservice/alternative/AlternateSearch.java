package dt.jdictionary.sqlite.dbservice.alternative;

import java.sql.SQLException;
import java.util.List;

import dt.jdictionary.SimpleLookup;

public interface AlternateSearch 
{
	public abstract List<SimpleLookup> trySearch() throws Exception;
	public abstract String LOOKUP_NAME();
}
