package dt.jdictionary.dumpdb;


public enum DumpFile
{
	CHINESE(Constants.DUMP_PREFIX + "dump_chinese"),
	ENGLISH(Constants.DUMP_PREFIX + "dump_english"),
	MEASURE_WORDS(Constants.DUMP_PREFIX + "dump_measure_words"),
	SIMPLIFIED(Constants.DUMP_PREFIX + "dump_simplified"),
	SUBSTRING(Constants.DUMP_PREFIX + "dump_substring"),
	PAST(Constants.DUMP_PREFIX + "dump_past_hits");
	
	private final String path;
	private DumpFile(String path)
	{
		this.path = path;
	}
	
	public String getPath()
	{
		return path;
	}

	private static class Constants
	{
		private static final String DUMP_PREFIX = System.getProperty("user.home") + "/Programs/JDictionary/";
	}
}
