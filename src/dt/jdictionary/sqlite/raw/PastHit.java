package dt.jdictionary.sqlite.raw;

import java.util.Date;
import java.util.Objects;

public class PastHit
{
	private final String chinese;
	private final Date timestamp;
	
	public PastHit(String chinese, Date timestamp)
	{
		super();
		this.chinese = chinese;
		this.timestamp = timestamp;
	}

	public String getChinese()
	{
		return chinese;
	}
	
	public Date getTimestamp()
	{
		return timestamp;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(chinese, timestamp);
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
		PastHit other = (PastHit) obj;
		return Objects.equals(chinese, other.chinese) && Objects.equals(timestamp, other.timestamp);
	}
}
