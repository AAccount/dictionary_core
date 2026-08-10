package dt.jdictionary;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import dt.jdictionary.dbrepo.DictionaryEntry;

public class ExhaustiveChineseLookup 
{
	private final ChineseDefinitionLookup definition;
	private final Map<String, List<DictionaryEntry>> supplementaries;
	
	public ExhaustiveChineseLookup(ChineseDefinitionLookup definition, Map<String, List<DictionaryEntry>> supplementaries) 
	{
		super();
		this.definition = definition;
		this.supplementaries = supplementaries;
	}

	public ChineseDefinitionLookup getDefinition() 
	{
		return definition;
	}
	
	public Map<String, List<DictionaryEntry>> getSupplementaries() 
	{
		return supplementaries;
	}

	@Override
	public String toString()
	{
		return "ExhaustiveChineseLookup [definition=" + definition + ", supplementaries=" + supplementaries + "]";
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(definition, supplementaries);
	}

	@Override
	public boolean equals(Object obj)
	{
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		final ExhaustiveChineseLookup other = (ExhaustiveChineseLookup) obj;
		return Objects.equals(definition, other.definition) && Objects.equals(supplementaries, other.supplementaries);
	}
	
	
}
