package dt.jdictionary.dumpdb.line;

public class DictionaryLine 
{
	private final String zh;
	private final String pinyin;
	private final String definition;
	private final double rank;



	public DictionaryLine(String zh, String pinyin, String singleDefinition, double rank) 
	{
		this.zh = zh;
		this.pinyin = pinyin;
		this.definition = singleDefinition;
		this.rank = rank;
	}
	
	public String getZh() 
	{
		return zh;
	}

	public String getPinyin() 
	{
		return pinyin;
	}

	public String getdefinition() 
	{
		return definition;
	}

	public double getRank()
	{
		return rank;
	}
	
	@Override
	public int hashCode()
	{
		return toString().hashCode();
	}

	@Override
	public String toString()
	{
		return "RawDictionaryRow [zh=" + zh + ", pinyin=" + pinyin + ", singleDefinition=" + definition + ", rank=" + rank + "]";
	}

	@Override
	public boolean equals(Object obj)
	{
		if(obj == null || !obj.getClass().equals(this.getClass()))
		{
			return false;
		}
		
		final DictionaryLine casted = (DictionaryLine)obj;
		return 
			casted.zh.equals(this.zh) && 
			casted.pinyin.equals(this.pinyin) && 
			casted.definition.equals(this.definition);
	}
}
