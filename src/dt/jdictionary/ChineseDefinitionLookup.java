package dt.jdictionary;
import java.util.List;
import java.util.Map;

public class ChineseDefinitionLookup 
{
	private final String chinese;
	private final Map<String, List<String>> results;
	private final String simplified;
	private final List<String> measureWords;

	public ChineseDefinitionLookup(String chinese, Map<String, List<String>> results, String simplified, List<String> measureWords)
	{
		this.chinese = chinese;
		this.results = results;
		this.simplified = simplified;
		this.measureWords = measureWords;
	}

	public String getChinese() 
	{
		return chinese;
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
