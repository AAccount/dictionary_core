package dt.jdictionary.sqlite.raw;

import java.util.List;

import dt.jdictionary.SimpleLookup;

public interface IDbRepo
{

	void init() throws Exception;

	void wipe() throws Exception;

	void saveHits(List<String> hits) throws Exception;

	List<PastHit> lookupPastHits(List<String> candidates) throws Exception;

	List<RawDictionaryRow> lookupChinese(List<String> zhStrings) throws Exception;

	String lookupSimplified(String zh) throws Exception;

	List<String> lookupMeasureWords(String zh) throws Exception;

	List<RawDictionaryRow> lookupRelatedWord(String zh, RelatedChar similarity) throws Exception;

	List<RawDictionaryRow> lookupEnglish(String en) throws Exception;

	List<String> trySubstring(String compoundWord) throws Exception;

	List<RawDictionaryRow> findByNormalizedPinyin(List<String> normalizedPinyins) throws Exception;

	void fillDictionary(List<SimpleLookup> allEntries) throws Exception;

	void fillMeasureWords(List<RawMeasureWordRow> allRows) throws Exception;

	void fillSimplified(List<RawSimplifiedRow> allRows) throws Exception;

	void fillSubstrings(List<RawSubstringRow> allRows) throws Exception;

}