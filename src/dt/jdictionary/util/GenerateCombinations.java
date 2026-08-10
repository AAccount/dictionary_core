package dt.jdictionary.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GenerateCombinations
{
	private GenerateCombinations() {}

	public static <T> List<List<T>> generateCombinations(List<T> elements)
	{
		final int MINIMUM_USEABLE_STRING = 1;
		if(elements.size() <= MINIMUM_USEABLE_STRING)
		{
			final List<List<T>> result = new ArrayList<List<T>>();
			result.add(elements);
			return result;
		}
	
		/**
		 * Given [1,2,3]: [1],[2][3],[1,2],[1,3],[1,2,3], every possible combination with "1" in it.
		 * Then calls itself with [2,3] for the remaining combinations without 1: [2,3]
		 */
		final Set<List<T>> result = new HashSet<>();
		for(int groupSize = 0; groupSize<=elements.size(); groupSize++)
		{
			for(int additional = groupSize; additional<elements.size(); additional++)
			{
				final List<T> combination = new ArrayList<T>();
				combination.addAll(elements.subList(0, groupSize));
				combination.add(elements.get(additional));
				result.add(combination);
			}
		}
		result.addAll(generateCombinations(elements.subList(1, elements.size())));
		return new ArrayList<List<T>>(result);
	}
}
