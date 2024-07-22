package dt.jdictionary;

import java.util.Objects;

public class ChineseSummaryLookup implements Comparable<ChineseSummaryLookup>
{
	private final String chinese;
	private final String pinyin;
	private final String definition;
	private final double rank;
	
	public ChineseSummaryLookup(String chinese, String pinyin, String definition, double rank)
	{
		this.chinese = chinese;
		this.pinyin = pinyin;
		this.definition = definition;
		this.rank = rank;
	}
	
	public ChineseSummaryLookup(ChineseSummaryLookup existing, double rank)
	{
		this.chinese = existing.getChinese();
		this.pinyin = existing.getPinyin();
		this.definition = existing.getDefinition();
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
	
	public double getRank()
	{
		return rank;
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(chinese, pinyin);
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
		ChineseSummaryLookup other = (ChineseSummaryLookup) obj;
		return Objects.equals(chinese, other.chinese) && Objects.equals(pinyin, other.pinyin);
	}
	
	@Override
	public String toString()
	{
		return "ChineseSummaryLookup [chinese=" + chinese + ", pinyin=" + pinyin + ", definition=" + definition + ", rank=" + rank + "]";
	}

	@Override
	public int compareTo(ChineseSummaryLookup other)
	{
		final double difference = this.rank - other.rank;
		return difference == 0 ? 0 : difference > 0 ? 1 : -1;
	}
}
