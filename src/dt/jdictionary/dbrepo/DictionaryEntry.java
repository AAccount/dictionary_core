package dt.jdictionary.dbrepo;

import dt.jdictionary.util.ChineseText;

public class DictionaryEntry implements Comparable<DictionaryEntry>
{
	private final String chinese;
	private final String pinyin;
	private final String pinyinNormalized;
	private final String definition;
	private final String firstChar;
	private final String lastChar;
	private final double rank;

	public DictionaryEntry(String chinese, String pinyin, double rank) 
	{
		this.chinese = chinese;
		this.pinyin = pinyin;
		this.pinyinNormalized = ChineseText.normalizePinyin(pinyin);
		this.definition = "";
		this.rank = rank;

		final int[] codepoints = chinese.codePoints().toArray();
		this.firstChar = codepoints.length > 1 ? Character.toString(codepoints[0]) : null;
		this.lastChar = codepoints.length > 1 ? Character.toString(codepoints[codepoints.length-1]) : null;
	}

	public DictionaryEntry(DictionaryEntry other, double rank)
	{
		this.chinese = other.chinese;
		this.pinyin = other.pinyin;
		this.pinyinNormalized = other.pinyinNormalized;
		this.definition = other.definition;
		this.rank = rank;
		this.firstChar = other.firstChar;
		this.lastChar = other.lastChar;
	}

	public DictionaryEntry(String chinese, String pinyin, String pinyinNormalized, String definition, String firstChar, String lastChar, double rank) 
	{
		this.chinese = chinese;
		this.pinyin = pinyin;
		this.pinyinNormalized = pinyinNormalized;
		this.definition = definition;
		this.firstChar = firstChar;
		this.lastChar = lastChar;
		this.rank = rank;
	}
	
	public String getChinese() 
	{
		return chinese;
	}

	public String getPinyin() 
	{
		return pinyin;
	}

	public String getDefinition() 
	{
		return definition;
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
		return "DictionaryEntry [chinese=" + chinese + ", pinyin=" + pinyin + ", definition=" + definition + ", rank=" + rank + "]";
	}

	@Override
	public int hashCode() 
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((chinese == null) ? 0 : chinese.hashCode());
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
		DictionaryEntry other = (DictionaryEntry) obj;
		if (chinese == null) 
		{
			if (other.chinese != null)
				return false;
		} 
		else if (!chinese.equals(other.chinese))
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

	@Override
	public int compareTo(DictionaryEntry other)
	{
		final double difference = this.rank - other.rank;
		return difference == 0 ? 0 : difference > 0 ? 1 : -1;
	}
}
