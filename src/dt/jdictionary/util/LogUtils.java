package dt.jdictionary.util;

import java.util.Arrays;
import java.util.stream.Collectors;

public class LogUtils 
{
	private LogUtils(){}

	public static String printStackTrace(Throwable t)
	{
		if(t == null)
		{
			return "";
		}

		final String header = t.toString();
		final String relevant = Arrays.stream(t.getStackTrace())
			.filter(line -> line.getClassName().startsWith("dt."))
			.map(element -> element.toString())
			.collect(Collectors.joining("\n"));
		return relevant.isEmpty() ? header : header + "\n" + relevant;
	}
}
