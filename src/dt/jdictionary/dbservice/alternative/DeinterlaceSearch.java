package dt.jdictionary.dbservice.alternative;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import dt.jdictionary.ChineseSummaryLookup;
import dt.jdictionary.dbrepo.DbRepo;
import dt.jdictionary.dbservice.DbServiceUtils;
import dt.util.ChineseText;

public class DeinterlaceSearch implements AlternateSearch
{	
	private final String zh;
	private final DbRepo db;
	
	public DeinterlaceSearch(String zh, DbRepo db)
	{
		this.zh = zh;
		this.db = db;
	}

	/**
	 * Attempt to "deinterlace" an entry: chars 123 --> lookup 13; chars 1234 --> lookup 13 and 24
	 * @throws SQLException 
	 */
	@Override
	public List<ChineseSummaryLookup> trySearch() throws SQLException
	{
		final int MIN_DEINTERLACE = 3;
		final int MAX_DEINTERLACE = 4;
		if(this.zh.length() < MIN_DEINTERLACE || this.zh.length() > MAX_DEINTERLACE)
		{
			return new ArrayList<>();
		}

		final List<String> chars = ChineseText.charsByCodepoint(zh);
		final List<String> candidates = new ArrayList<String>();
		candidates.add(chars.get(0) + chars.get(2));
		if(chars.size() == MAX_DEINTERLACE)
		{
			candidates.add(chars.get(1) + chars.get(3));
		}
		return DbServiceUtils.convertRawToSimple(this.db.lookupChinese(candidates));
	}

	@Override
	public String LOOKUP_NAME()
	{
		return "Deinterlace";
	}
}
