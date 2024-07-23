package dt.jdictionary.dumpdb;

public interface DbFillListener
{
	public void onDiskWrite(DumpFile dumpFile, int writes);
}
