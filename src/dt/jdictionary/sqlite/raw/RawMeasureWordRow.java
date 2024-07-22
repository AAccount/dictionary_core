package dt.jdictionary.sqlite.raw;

public class RawMeasureWordRow 
{
	private final String zh;
	private final String measure;
	private final String measurePinyin;

	public RawMeasureWordRow(String zh, String measure, String measurePinyin) 
	{
		this.zh = zh;
		this.measure = measure;
		this.measurePinyin = measurePinyin;
	}

	public String getZh() 
	{
		return zh;
	}

	public String getMeasure() 
	{
		return measure;
	}

	public String getMeasurePinyin() 
	{
		return measurePinyin;
	}

	@Override
	public int hashCode() 
	{
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object obj) 
	{
		if(obj == null || !obj.getClass().equals(this.getClass()))
		{
			return false;
		}

		final RawMeasureWordRow casted = (RawMeasureWordRow)obj;
		return
			casted.zh.equals(this.zh) &&
			casted.measure.equals(this.measure) &&
			casted.measurePinyin.equals(this.measurePinyin);
	}

	@Override
	public String toString()
	{
		return "RawMeasureWordRow [zh=" + zh + ", measure=" + measure + ", measurePinyin=" + measurePinyin + "]";
	}	
}
