package dt.jdictionary.dbservice;

import java.util.ArrayList;
import java.util.List;
import dt.jdictionary.ChineseSummaryLookup;
import dt.jdictionary.dbrepo.raw.RawDictionaryRow;

public class DbServiceUtils 
{
	public static List<ChineseSummaryLookup> convertRawToSimple(List<RawDictionaryRow> rawResults)
	{
		final List<ChineseSummaryLookup> result = new ArrayList<>();
		for(final RawDictionaryRow raw : rawResults)
		{
			result.add(new ChineseSummaryLookup(raw.getZh(), raw.getPinyin(), raw.getSingleDefinition(), raw.getRank()));
		}
		return result;
	}
}
