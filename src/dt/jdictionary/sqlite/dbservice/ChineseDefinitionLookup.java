package dt.jdictionary.sqlite.dbservice;
import java.util.List;
import java.util.Map;

public class ChineseDefinitionLookup 
{
	private final String zh;
	private final Map<String, List<String>> results;
	private final String simplified;
	private final List<String> measureWords;

	public ChineseDefinitionLookup(String zh, Map<String, List<String>> results, String simplified, List<String> measureWords)
	{
		this.zh = zh;
		this.results = results;
		this.simplified = simplified;
		this.measureWords = measureWords;
	}

	public String getZh() 
	{
		return zh;
	}

	public Map<String, List<String>> getResults() 
	{
		return results;
	}

	public String getSimplified() 
	{
		return simplified;
	}

	public List<String> getMeasureWords() 
	{
		return measureWords;
	}

}
