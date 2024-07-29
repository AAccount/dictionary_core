package dt.jdictionary.dumpdb.line;

import java.nio.charset.StandardCharsets;

public class DictionaryLine 
{
	private final String zh;
	private final byte[] pinyin;
	private final byte[] definition;
	private final double rank;

	public DictionaryLine(String zh, byte[] pinyin, String definition, double rank) 
	{
		this.zh = zh;
		this.pinyin = pinyin;
		this.definition = definition.getBytes(StandardCharsets.UTF_8);
		this.rank = rank;
	}
	
	public String getZh() 
	{
		return zh;
	}

	public String getPinyin() 
	{
		return new String(this.pinyin, StandardCharsets.UTF_8);
	}

	public String getdefinition() 
	{
		return new String(this.definition, StandardCharsets.UTF_8);
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
		return "RawDictionaryRow [zh=" + zh + ", pinyin=" + pinyin + ", singleDefinition=" + this.getdefinition() + ", rank=" + rank + "]";
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
