package dt.jdictionary.dbrepo.raw;

public class RawSimplifiedRow 
{
	private final String original;
	private final String simplified;

	public RawSimplifiedRow(String original, String simplified) 
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

		final RawSimplifiedRow casted = (RawSimplifiedRow)obj;
		return
			casted.original.equals(this.original) &&
			casted.simplified.equals(this.simplified);
	}
}
