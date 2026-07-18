package dt.jdictionary.dbrepo.raw;

import java.util.List;

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
		final List<String> trueChars = ChineseText.trueChars(zh);
		this.firstChar = trueChars.size() > 1 ? trueChars.get(0) : null;
		this.lastChar = trueChars.size() > 1 ? trueChars.get(trueChars.size()-1) : null;
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
	public int hashCode()
	{
		return toString().hashCode();
	}

	@Override
	public String toString()
	{
		return "RawDictionaryRow [zh=" + zh + ", pinyin=" + pinyin + ", singleDefinition=" + singleDefinition + ", rank=" + rank + "]";
	}

	@Override
	public boolean equals(Object obj)
	{
		if(obj == null || !obj.getClass().equals(this.getClass()))
		{
			return false;
		}
		
		final RawDictionaryRow casted = (RawDictionaryRow)obj;
		return 
			casted.zh.equals(this.zh) && 
			casted.pinyin.equals(this.pinyin) && 
			casted.singleDefinition.equals(this.singleDefinition);
	}
}
