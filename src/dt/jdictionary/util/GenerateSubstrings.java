package dt.jdictionary.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GenerateSubstrings
{

	public static List<String> generateSubstrings(String saying)
	{
		// To generate all possible substrings, you will get the original string itself. Don't return that entry.
		final int[] codepoints = saying.codePoints().toArray();
		final List<String> results = GenerateSubstrings.generateSubstringsReal(codepoints).stream()
			.map(cps -> new String(cps, 0, cps.length))
			.toList();
		return results.stream().filter(substring -> !substring.equals(saying)).toList();
	}

	private static List<int[]> generateSubstringsReal(int[] codepoints)
	{
		final int MINIMUM_USEABLE_STRING = 1;
		if(codepoints.length == MINIMUM_USEABLE_STRING)
		{
			return List.of(codepoints);
		}
	
		final List<int[]> result = new ArrayList<>();
		for(int i = 1; i <= codepoints.length; i++)
		{
			result.add(Arrays.copyOfRange(codepoints, 0, i));
		}

		result.addAll(generateSubstringsReal(Arrays.copyOfRange(codepoints, 1, codepoints.length)));
		return result;
	}

}
