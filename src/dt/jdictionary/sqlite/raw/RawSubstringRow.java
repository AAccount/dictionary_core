package dt.jdictionary.sqlite.raw;

public class RawSubstringRow 
{
	final String substring;
	final String fullString;

	public RawSubstringRow(String substring, String fullString) 
	{
		this.substring = substring;
		this.fullString = fullString;
	}

	public String getSubstring() 
	{
		return substring;
	}

	public String getFullString() 
	{
		return fullString;
	}

	@Override
	public String toString() 
	{
		return "RawSubstringRow [substring=" + substring + ", fullString=" + fullString + "]";
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

		final RawSubstringRow casted = (RawSubstringRow)obj;
		return
			casted.substring.equals(this.substring) &&
			casted.fullString.equals(this.fullString);
	}
}
