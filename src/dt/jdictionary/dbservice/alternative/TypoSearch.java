package dt.jdictionary.dbservice.alternative;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import dt.jdictionary.ChineseSummaryLookup;
import dt.jdictionary.dbrepo.DbRepo;
import dt.jdictionary.dbservice.DbServiceUtils;
import dt.util.ChineseText;

public class TypoSearch implements AlternateSearch
{
	private final String zh;
	private final DbRepo db;
	
	public TypoSearch(String zh, DbRepo db)
	{
		this.zh = zh;
		this.db = db;
	}

	@Override
	public List<ChineseSummaryLookup> trySearch() throws SQLException
	{
		final List<String> trueChars = ChineseText.trueChars(this.zh);
		final List<List<String>> normalizedPinyins = findPinyinForZh(trueChars);
		if(this.zh.length() != normalizedPinyins.size())
		{
			return new ArrayList<>();
		}

		final List<String> permutations = pinyinPermutations(normalizedPinyins);
		final List<ChineseSummaryLookup> candidates = DbServiceUtils.convertRawToSimple(this.db.findByNormalizedPinyin(permutations));

		return candidates.stream()
				.map(candidate -> new ChineseSummaryLookup(candidate, pinyinLookupSimilarity(candidate, trueChars)))
				.filter(candidate -> candidate.getRank() >0 && candidate.getRank() < this.zh.length())
				.collect(Collectors.toCollection(ArrayList::new));
	}

	private int pinyinLookupSimilarity(ChineseSummaryLookup candidate, List<String> targetChars)
	{
		int similarity = 0;
		final List<String> candidateTrueChars = ChineseText.trueChars(candidate.getChinese());
		final Set<String> candidateSet = new HashSet<>();
		candidateTrueChars.stream().forEach(candidateChar -> candidateSet.add(candidateChar));
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
		final List<ChineseSummaryLookup> dictionaryEntries = DbServiceUtils.convertRawToSimple(this.db.lookupChinese(chars));
		for(final ChineseSummaryLookup entry : dictionaryEntries)
		{
			if(!pinyinMap.containsKey(entry.getChinese()))
			{
				pinyinMap.put(entry.getChinese(), new HashSet<>());
			}
			pinyinMap.get(entry.getChinese()).add(ChineseText.normalizePinyin(entry.getPinyin()));
		}
		
		final List<List<String>> result = new ArrayList<>();
		for(final String singleChar : chars)
		{
			result.add(new ArrayList<String>(pinyinMap.get(singleChar)));
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
