package dt.jdictionary.dbservice.alternative;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import dt.jdictionary.ChineseSummaryLookup;
import dt.jdictionary.dbservice.DbServiceUtils;
import dt.jdictionary.dumpdb.DumpDBRepo;

public class SubstringOfSearch implements AlternateSearch
{
	private final String zh;
	private final DumpDBRepo db;
	
	public SubstringOfSearch(String zh, DumpDBRepo db)
	{
		this.zh = zh;
		this.db = db;
	}

	@Override
 	public List<ChineseSummaryLookup> trySearch()
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
