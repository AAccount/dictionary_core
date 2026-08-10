package dt.jdictionary.dbrepo.raw;

public class RawMeasureWordRow 
{
	private final String chinese;
	private final String measure;
	private final String measurePinyin;

	public RawMeasureWordRow(String chinese, String measure, String measurePinyin) 
	{
		this.chinese = chinese;
		this.measure = measure;
		this.measurePinyin = measurePinyin;
	}

	public String getChinese() 
	{
		return chinese;
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
			casted.chinese.equals(this.chinese) &&
			casted.measure.equals(this.measure) &&
			casted.measurePinyin.equals(this.measurePinyin);
	}

	@Override
	public String toString()
	{
		return "RawMeasureWordRow [chinese=" + chinese + ", measure=" + measure + ", measurePinyin=" + measurePinyin + "]";
	}	
}
