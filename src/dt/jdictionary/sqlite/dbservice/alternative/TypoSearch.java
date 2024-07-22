package dt.jdictionary.sqlite.dbservice.alternative;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import dt.jdictionary.SimpleLookup;
import dt.jdictionary.sqlite.dbservice.DbServiceUtils;
import dt.jdictionary.sqlite.raw.IDbRepo;
import dt.util.ChineseText;
import dt.util.J9Shorthand;

public class TypoSearch implements AlternateSearch
{
	private final String zh;
	private final IDbRepo db;
	
	public TypoSearch(String zh, IDbRepo db)
	{
		this.zh = zh;
		this.db = db;
	}

	@Override
	public List<SimpleLookup> trySearch() throws Exception
	{
		final List<String> trueChars = ChineseText.trueChars(this.zh);
		final List<List<String>> normalizedPinyins = findPinyinForZh(trueChars);
		if(this.zh.length() != normalizedPinyins.size())
		{
			return new ArrayList<>();
		}

		final List<String> permutations = pinyinPermutations(normalizedPinyins);
		final List<SimpleLookup> candidates = DbServiceUtils.convertRawToSimple(this.db.findByNormalizedPinyin(permutations));

		return candidates.stream()
				.map(candidate -> new SimpleLookup(candidate.getZh(), candidate.getPinyin(), candidate.getDefinitions(), pinyinLookupSimilarity(candidate, trueChars)))
				.filter(candidate -> candidate.getRank() >0 && candidate.getRank() < this.zh.length())
				.collect(Collectors.toCollection(ArrayList::new));
	}

	private int pinyinLookupSimilarity(SimpleLookup candidate, List<String> targetChars)
	{
		int similarity = 0;
		final List<String> candidateTrueChars = ChineseText.trueChars(candidate.getZh());
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

	private List<List<String>> findPinyinForZh(List<String> chars) throws Exception
	{
		final HashMap<String, Set<String>> pinyinMap = new HashMap<>();
		final List<SimpleLookup> dictionaryEntries = DbServiceUtils.convertRawToSimple(this.db.lookupChinese(chars));
		for(final SimpleLookup entry : dictionaryEntries)
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
			return pinyinPermutations(J9Shorthand.list(individualPinyins.get(0), subresult));
		}
	}

	@Override
	public String LOOKUP_NAME()
	{
		return "Typo";
	}
}
