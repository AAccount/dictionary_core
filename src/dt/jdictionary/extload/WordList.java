package dt.jdictionary.extload;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dt.jdictionary.ProgressListener;
import dt.util.ChineseText;

public class WordList
{	
	private final ProgressListener progressListener;

	public WordList(ProgressListener progressListener)
	{
		this.progressListener = progressListener;
	}

	public List<String> parse(File file) throws IOException
	{
		final Set<String> hashSet = new HashSet<>();
		long bytesProcessed = 0;

			final BufferedReader fileReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
			String line = fileReader.readLine();
			while (line != null)
			{
				final String cleaned = line.strip();
				if(cleaned.length() > 1 && ChineseText.allChinese(cleaned)) // Past hits is intended to help prioritize compound words.
				{
					hashSet.add(cleaned);
				}
				bytesProcessed = bytesProcessed + line.length();
				this.progressListener.onFractionalProgress("Parsing word list", bytesProcessed, file.length());
				line = fileReader.readLine();
			}
			fileReader.close();

		return new ArrayList<String>(hashSet);
	}
}
