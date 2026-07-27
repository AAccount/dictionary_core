package dt.jdictionary.dbrepo.raw;

import dt.util.ChineseText;

public class RawDictionaryRow 
{
	private final String zh;
	private final String pinyin;
	private final String pinyinNormalized;
	private final String singleDefinition;
	private final String firstChar;
	private final String lastChar;
	private final double rank;

	public RawDictionaryRow(String zh, String pinyin, double rank) 
	{
		this.zh = zh;
		this.pinyin = pinyin;
		this.pinyinNormalized = ChineseText.normalizePinyin(pinyin);
		this.singleDefinition = null;
		this.rank = rank;

		final int[] codepoints = zh.codePoints().toArray();
		this.firstChar = codepoints.length > 1 ? Character.toString(codepoints[0]) : null;
		this.lastChar = codepoints.length > 1 ? Character.toString(codepoints[codepoints.length-1]) : null;
	}

	public RawDictionaryRow(String zh, String pinyin, String pinyinNormalized, String singleDefinition, String firstChar, String lastChar, double rank) 
	{
		this.zh = zh;
		this.pinyin = pinyin;
		this.pinyinNormalized = pinyinNormalized;
		this.singleDefinition = singleDefinition;
		this.firstChar = firstChar;
		this.lastChar = lastChar;
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

	public String getSingleDefinition() 
	{
		return singleDefinition;
	}
	
	public String getFirstChar() 
	{
		return firstChar;
	}

	public String getLastChar() 
	{
		return lastChar;
	}

	public String getPinyinNormalized() 
	{
		return pinyinNormalized;
	}

	public double getRank()
	{
		return rank;
	}

	@Override
	public String toString()
	{
		return "RawDictionaryRow [zh=" + zh + ", pinyin=" + pinyin + ", singleDefinition=" + singleDefinition + ", rank=" + rank + "]";
	}

	@Override
	public int hashCode() 
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((zh == null) ? 0 : zh.hashCode());
		result = prime * result + ((pinyin == null) ? 0 : pinyin.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) 
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RawDictionaryRow other = (RawDictionaryRow) obj;
		if (zh == null) 
		{
			if (other.zh != null)
				return false;
		} 
		else if (!zh.equals(other.zh))
			return false;
		
		if (pinyin == null) 
		{
			if (other.pinyin != null)
				return false;
		} 
		else if (!pinyin.equals(other.pinyin))
			return false;
		return true;
	}

	
}
