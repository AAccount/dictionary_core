package dt.jdictionary.dbservice.alternative;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import dt.jdictionary.dbrepo.DbRepo;
import dt.jdictionary.dbrepo.raw.RawDictionaryRow;
import dt.jdictionary.dbservice.DbService;
import dt.util.ChineseText;

public class TypoSearch implements AlternateSearch
{
	private static final Logger logger = Logger.getLogger(TypoSearch.class.getName());

	private final String zh;
	private final DbRepo db;
	
	public TypoSearch(String zh, DbRepo db)
	{
		this.zh = zh;
		this.db = db;
	}

	@Override
	public List<RawDictionaryRow> trySearch() throws SQLException
	{
		final List<String> chars = ChineseText.charsByCodepoint(this.zh);
		final List<List<String>> normalizedPinyins = findPinyinForZh(chars);
		if(chars.size() != normalizedPinyins.size())
		{
			logger.info("pinyins per char does not match the amount of chars, chars: " + zh + " got pinyins for " + normalizedPinyins.size() + " of them");
			return new ArrayList<>();
		}

		final List<String> permutations = pinyinPermutations(normalizedPinyins);
		final List<RawDictionaryRow> candidates = this.db.findByNormalizedPinyin(permutations);

		return candidates.stream()
				.map(candidate -> new RawDictionaryRow(candidate, pinyinLookupSimilarity(candidate, chars)))
				.filter(candidate -> candidate.getRank() >0 && candidate.getRank() < this.zh.length())
				.collect(Collectors.toCollection(ArrayList::new));
	}

	private int pinyinLookupSimilarity(RawDictionaryRow candidate, List<String> targetChars)
	{
		int similarity = 0;
		final List<String> chars = ChineseText.charsByCodepoint(candidate.getZh());
		final Set<String> candidateSet = new HashSet<>();
		chars.stream().forEach(candidateChar -> candidateSet.add(candidateChar));
		for(final String targetChar : targetChars)
		{
			if(candidateSet.contains(targetChar))
			{
				similarity++;
			}
		}
		return similarity;
	}

	private List<List<String>> findPinyinForZh(List<String> chars) throws SQLException
	{
		final HashMap<String, Set<String>> pinyinMap = new HashMap<>();
		final List<RawDictionaryRow> dictionaryEntries = this.db.lookupChinese(chars);
		for(final RawDictionaryRow entry : dictionaryEntries)
		{
			if(!pinyinMap.containsKey(entry.getZh()))
			{
				pinyinMap.put(entry.getZh(), new HashSet<>());
			}
			pinyinMap.get(entry.getZh()).add(ChineseText.normalizePinyin(entry.getPinyin()));
		}
		
		final List<List<String>> result = new ArrayList<>();
		for(final String singleChar : chars)
		{
			result.add(new ArrayList<String>(pinyinMap.getOrDefault(singleChar, Set.of())));
		}
		return result;
	}

	private List<String> pinyinPermutations(List<List<String>> individualPinyins)
	{
		if(individualPinyins.size() == 0)
		{
			return new ArrayList<>();
		}
		else if(individualPinyins.size() == 1)
		{
			return individualPinyins.get(0);
		}
		else if(individualPinyins.size() == 2)
		{
			final List<String> result = new ArrayList<>();
			for(final String first : individualPinyins.get(0))
			{
				for(final String second : individualPinyins.get(1))
				{
					result.add(first + " " + second);
				}
			}
			return result;
		}
		else
		{
			final List<String> subresult = pinyinPermutations(individualPinyins.subList(1, individualPinyins.size()));
			return pinyinPermutations(List.of(individualPinyins.get(0), subresult));
		}
	}

	@Override
	public String LOOKUP_NAME()
	{
		return "Typo";
	}
}
