package dt.jdictionary.dbservice.alternative;

import java.sql.SQLException;
import java.util.List;

import dt.jdictionary.dbrepo.raw.RawDictionaryRow;

public interface AlternateSearch 
{
	public abstract List<RawDictionaryRow> trySearch() throws SQLException;
	public abstract String LOOKUP_NAME();
}
