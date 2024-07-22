package dt.jdictionary.sqlite.dbservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dt.jdictionary.sqlite.raw.PastHit;

public class PastHitUtils
{
	private static int MAX_COUNT = 100;
	
	public static Map<String, Integer> countHits(List<PastHit> pastHits)
	{
		final Map<String, Integer> counter = new HashMap<String, Integer>();
		for(final PastHit pastHit : pastHits)
		{
			final String key = pastHit.getChinese();
			if(!counter.containsKey(key))
			{
				counter.put(pastHit.getChinese(), 0);
			}
			
			if(counter.containsKey(key) && counter.get(key) < MAX_COUNT)
			{
				counter.put(key, counter.get(key)+1);
			}
		}
		return counter;
	}
}
