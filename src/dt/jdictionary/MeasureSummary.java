package dt.jdictionary;

import java.util.Objects;

public class MeasureSummary
{
	private final String measureWord;
	private final String measurePinyin;
	
	public MeasureSummary(String measureWord, String measurePinyin)
	{
		super();
		this.measureWord = measureWord;
		this.measurePinyin = measurePinyin;
	}

	public String getMeasureWord()
	{
		return measureWord;
	}

	public String getMeasurePinyin()
	{
		return measurePinyin;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(measurePinyin, measureWord);
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
		MeasureSummary other = (MeasureSummary) obj;
		return Objects.equals(measurePinyin, other.measurePinyin) && Objects.equals(measureWord, other.measureWord);
	}
	
	
}
