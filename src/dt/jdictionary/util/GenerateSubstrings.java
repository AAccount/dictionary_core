package dt.jdictionary.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GenerateSubstrings
{

	public static List<String> generateSubstrings(String saying)
	{
		// To generate all possible substrings, you will get the original string itself. Don't return that entry.
		final List<String> results = GenerateSubstrings.generateSubstringsReal(saying);
		return results.stream().filter(substring -> !substring.equals(saying)).collect(Collectors.toCollection(ArrayList::new));
	}

	private static List<String> generateSubstringsReal(String saying)
	{
		final int MINIMUM_USEABLE_STRING = 1;
		if(saying.length() < MINIMUM_USEABLE_STRING)
		{
			return new ArrayList<>();
		}
	
		final List<String> result = new ArrayList<>();
		for(int i = 1; i <= saying.length(); i++)
		{
			result.add(saying.substring(0, i));
		}
		result.addAll(generateSubstringsReal(saying.substring(1)));
		return result;
	}

}
