package dt.jdictionary.dbservice.alternative;

import java.util.List;

import dt.jdictionary.ChineseSummaryLookup;

public interface AlternateSearch 
{
	public abstract List<ChineseSummaryLookup> trySearch();
	public abstract String LOOKUP_NAME();
}
