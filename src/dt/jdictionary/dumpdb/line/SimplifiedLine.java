package dt.jdictionary.dumpdb.line;

public class SimplifiedLine 
{
	private final String original;
	private final String simplified;

	public SimplifiedLine(String original, String simplified) 
	{
		this.original = original;
		this.simplified = simplified;
	}

	public String getOriginal() 
	{
		return original;
	}

	public String getSimplified() 
	{
		return simplified;
	}

	@Override
	public String toString() 
	{
		return "RawSimplifiedRow [original=" + original + ", simplified=" + simplified + "]";
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

		final SimplifiedLine casted = (SimplifiedLine)obj;
		return
			casted.original.equals(this.original) &&
			casted.simplified.equals(this.simplified);
	}
}
