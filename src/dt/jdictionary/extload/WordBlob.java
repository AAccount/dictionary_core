package dt.jdictionary.extload;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import dt.jdictionary.ProgressListener;
import dt.util.ChineseText;
import dt.util.StringUtil;

public class WordBlob
{
	private final ProgressListener progressListener;

	public WordBlob(ProgressListener progressListener)
	{
		this.progressListener = progressListener;
	}

	public List<String> parse(File file) throws IOException
	{
		final List<String> sentences = new ArrayList<>();
		long bytesProcessed = 0;

		final BufferedReader fileReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
		String line = fileReader.readLine();
		while (line != null)
		{
			final String cleaned = StringUtil.j11strip(line);
			if (cleaned.length() < 1)
			{
				line = fileReader.readLine();
			}

			final String[] lineSentences = line.split("\\.\\?\\,。？，");
			for (final String sentence : lineSentences)
			{
				sentences.add(ChineseText.stripNonChinese(sentence));
			}

			bytesProcessed = bytesProcessed + line.length();
			this.progressListener.onFractionalProgress("Parsing text blob", bytesProcessed, file.length());
			line = fileReader.readLine();
		}
		fileReader.close();

		return sentences;
	}
}
