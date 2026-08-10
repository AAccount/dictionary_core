package dt.jdictionary.util;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.List;

public class ChineseText 
{
	private ChineseText() {}

	public static String normalizePinyin(String pinyin)
	{
		return Normalizer.normalize(pinyin.toLowerCase().strip(), Form.NFD).replaceAll("\\p{M}", "");
	}

	public static List<String> charsByCodepoint(String chinese)
	{
		return chinese.codePoints()
			.mapToObj(Character::toString)
			.toList();
	}

	public static boolean allChinese(String string)
	{
		return string.codePoints().allMatch(codepoint -> Character.UnicodeScript.of(codepoint) == Character.UnicodeScript.HAN);
	}
}
