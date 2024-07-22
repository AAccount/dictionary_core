package dt.jdictionary;

import java.util.ArrayList;
import java.util.List;

public class ExceptionPile extends Exception
{
	private static final long serialVersionUID = 20240711200804L;
	private final List<Exception> exceptions;
	
	public ExceptionPile(String message, List<Exception> exceptions)
	{
		super(message, exceptions.get(0));
		this.exceptions = exceptions;
	}
	
	public ExceptionPile(String message, Exception exception)
	{
		super(message, exception);
		this.exceptions = new ArrayList<>();
		this.exceptions.add(exception);
	}
	
	public List<Exception> getExceptions()
	{
		return exceptions;
	}
	
}
