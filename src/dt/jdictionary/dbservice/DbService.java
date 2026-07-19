package dt.jdictionary.dbservice;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import dt.cedict.CedictDump;
import dt.jdictionary.ChineseDefinitionLookup;
import dt.jdictionary.ChineseSummaryLookup;
import dt.jdictionary.ExceptionPile;
import dt.jdictionary.ExhaustiveChineseLookup;
import dt.jdictionary.ProgressListener;
import dt.jdictionary.dbrepo.DbRepo;
import dt.jdictionary.dbrepo.raw.RawDictionaryRow;
import dt.jdictionary.dbservice.alternative.AlternateSearch;
import dt.jdictionary.dbservice.alternative.DeinterlaceSearch;
import dt.jdictionary.dbservice.alternative.SameBackSearch;
import dt.jdictionary.dbservice.alternative.SameFrontSearch;
import dt.jdictionary.dbservice.alternative.SubstringOfSearch;
import dt.jdictionary.dbservice.alternative.SubstringSearch;
import dt.jdictionary.dbservice.alternative.TypoSearch;
import dt.jdictionary.util.GenerateCombinations;
import dt.util.ChineseText;
import dt.util.Debug;

public class DbService 
{
	private final DbRepo db;
	public DbService() throws IOException, ParseException, ClassNotFoundException, SQLException
	{
		db = new DbRepo();
	}

	public ExhaustiveChineseLookup lookupChinese(String chinese, boolean shouldSave) throws ExceptionPile
	{
		Debug.logTimestamp("definition start");
		final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
		final CompletableFuture<ChineseDefinitionLookup> directResults = CompletableFuture.supplyAsync(() -> {return this.lookupChineseDefinition(chinese, exceptions);});
		
		final List<AlternateSearch> alts = List.of(
			new SameFrontSearch(chinese, db), 
			new SameBackSearch(chinese, db), 
			new SubstringSearch(chinese, db), 
			new SubstringOfSearch(chinese, db), 
			new DeinterlaceSearch(chinese, db), 
			new TypoSearch(chinese, db)
		);
		
		Debug.logTimestamp("start exhaustive Chinese search");
		final Map<String, CompletableFuture<List<ChineseSummaryLookup>>> supplementaryFutures = new LinkedHashMap<>(); 
			alts.forEach(alt -> supplementaryFutures.put(alt.LOOKUP_NAME(), CompletableFuture.supplyAsync(() -> {
				try
				{
					return alt.trySearch();
				}
				catch(Exception e)
				{
					exceptions.add(e);
					return new ArrayList<>();
				}
			})));
		
		final Map<String, List<ChineseSummaryLookup>> supplementaries = new LinkedHashMap<>(); // linked hash map for predictable iteration order
		supplementaryFutures.keySet().forEach(altName -> {
			try
			{
				supplementaries.put(altName, rerankAlternates(altName, supplementaryFutures.get(altName).join()));
			}
			catch(Exception e)
			{
				exceptions.add(e);
				supplementaries.put(altName, supplementaryFutures.get(altName).join());
			}
		});
		Debug.logTimestamp("finish exhaustive Chinese search");
		
		if(shouldSave)
		{
			try
			{
				saveChineseSearchHits(directResults.join(), supplementaries.get(SubstringSearch.LOOKUP_NAME));
			}
			catch(Exception e)
			{
				exceptions.add(e);
			}
		}
		
		if(!exceptions.isEmpty())
		{
			throw new ExceptionPile("lookupChinese", exceptions);
		}

		return new ExhaustiveChineseLookup(directResults.join(), supplementaries);
	}
	
	private List<ChineseSummaryLookup> rerankAlternates(String alternate, List<ChineseSummaryLookup> results) throws Exception
	{
		if(alternate.equals(SubstringSearch.LOOKUP_NAME))
		{
			return results;
		}
		
		final List<String> candidates = results.stream().map(ChineseSummaryLookup::getChinese).toList();
		final Map<String, Long> pastHits = db.lookupPastHits(candidates);
		return DbServiceUtils.rerank(results, pastHits);
	}
	
	private ChineseDefinitionLookup lookupChineseDefinition(String zh, List<Exception> pile)
	{
		try
		{
			final List<RawDictionaryRow> rawResults = db.lookupChinese(List.of(zh));
			final Map<String, List<String>> resultsByPinyin = new HashMap<>();
			for(final RawDictionaryRow rawResult : rawResults)
			{
				final String pinyin = rawResult.getPinyin();
				if(!resultsByPinyin.keySet().contains(pinyin))
				{
					resultsByPinyin.put(pinyin, new ArrayList<>());
				}
				resultsByPinyin.get(pinyin).add(rawResult.getSingleDefinition());
			}
	
			final String simplified = db.lookupSimplified(zh);
			final List<String> measureWords = db.lookupMeasureWords(zh);
			final ChineseDefinitionLookup result = new ChineseDefinitionLookup(zh, resultsByPinyin, simplified, measureWords);
			return result;
		}
		catch(Exception e)
		{
			pile.add(e);
			e.printStackTrace();
			return new ChineseDefinitionLookup(zh, new HashMap<>(), "", new ArrayList<>());
		}
	}
	
	private void saveChineseSearchHits(ChineseDefinitionLookup definitionLookup, List<ChineseSummaryLookup> substrings) throws Exception
	{
		final List<String> hits = new ArrayList<>();
		if(!definitionLookup.getResults().isEmpty())
		{
			hits.add(definitionLookup.getZh());
		}
		
		if(!substrings.isEmpty())
		{
			final List<String> substringHits = substrings.stream()
				.filter(substringEntry -> substringEntry.getChinese().codePointCount(0, substringEntry.getChinese().length()) > 1)
				.map(ChineseSummaryLookup::getChinese)
				.toList();
			hits.addAll(substringHits);
		}
		db.saveHits(hits);
	}
	
	public Map<String, List<ChineseSummaryLookup>> lookupEnglish(String en) throws ExceptionPile
	{
		Debug.logTimestamp("english start");
		final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

		final Map<String, CompletableFuture<List<ChineseSummaryLookup>>> wordFutures= new HashMap<>();
		final String[] individualWords = en.split(" ");
		for(final String individualWord : individualWords)
		{
			wordFutures.put(individualWord, CompletableFuture.supplyAsync(() -> {
				try
				{
					return this.lookupSingleEnglishWord(individualWord);
				}
				catch(Exception e)
				{
					exceptions.add(e);
					return new ArrayList<>();
				}
			}));
		}
		
		final Map<String, List<ChineseSummaryLookup>> result= new HashMap<>();
		for(final String word : wordFutures.keySet())
		{
			final List<ChineseSummaryLookup> singleResult = wordFutures.get(word).join();
			result.put(word, singleResult);
		}
		Debug.logTimestamp("english end");

		if(!exceptions.isEmpty())
		{
			throw new ExceptionPile("lookupEnglish", exceptions);
		}
		return findUseableCombinations(result);
	}
	
	private Map<String, List<ChineseSummaryLookup>> findUseableCombinations(Map<String, List<ChineseSummaryLookup>> individualDefinitions)
	{
		final List<List<String>> combinations = GenerateCombinations.generateCombinations(new ArrayList<>(individualDefinitions.keySet()));
		final Map<String, List<ChineseSummaryLookup>> result = new HashMap<String, List<ChineseSummaryLookup>>();
		for(final List<String> combination : combinations)
		{
			final List<ChineseSummaryLookup> combinedLookup = getQualifyingEntries(individualDefinitions, combination);
			if(!combinedLookup.isEmpty())
			{
				result.put(combination.toString(), combinedLookup);
			}
		}
		return result;
	}
	
	private List<ChineseSummaryLookup> getQualifyingEntries(Map<String, List<ChineseSummaryLookup>> individualDefinitions, List<String> combination)
	{
		if(combination.size() == 1)
		{
			return individualDefinitions.get(combination.get(0));
		}
		
		final List<ChineseSummaryLookup> result = new ArrayList<>(individualDefinitions.get(combination.get(0)));
		for(final String word : combination.subList(1, combination.size()))
		{
			final List<ChineseSummaryLookup> wordEntries = individualDefinitions.get(word);
			result.retainAll(wordEntries);
		}
		return result;
	}
	
	private List<ChineseSummaryLookup> lookupSingleEnglishWord(String singleWord) throws Exception
	{
		final List<ChineseSummaryLookup> rawResults =  DbServiceUtils.convertRawToSimple(db.lookupEnglish(singleWord));
		final List<String> candidates = rawResults.stream().map(ChineseSummaryLookup::getChinese).toList();
		final Map<String, Long> pastHits = db.lookupPastHits(candidates);
		return DbServiceUtils.rerank(rawResults, pastHits);
	}

	public void saveCedictDump(CedictDump dump, ProgressListener listener) throws Exception
	{
		new SaveCedict(db, dump, listener).save();
	}
	
	public void savePastHits(List<String> words, boolean verifyInDictionary) throws Exception
	{
		final List<String> useable = verifyInDictionary ? checkChineseInDictionary(words) : words;
		db.saveHits(useable);
	}
	
	private List<String> checkChineseInDictionary(List<String> words) throws Exception
	{
		final List<RawDictionaryRow> rawDictionaryRows = db.lookupChinese(words);
		final Set<String> inDictionary = rawDictionaryRows.stream().map(RawDictionaryRow::getZh).collect(Collectors.toCollection(HashSet::new));
		return words.stream().filter(word -> inDictionary.contains(word)).toList();
	}
	
	public List<String> extractCompoundWords(List<String> manySentences) throws Exception
	{
		final int cpus = Runtime.getRuntime().availableProcessors();
		final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(cpus);

		final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
		final ConcurrentHashMap.KeySetView<String, Boolean> compoundWordSet = ConcurrentHashMap.newKeySet();
		for(final String sentence : manySentences)
		{
			executor.submit(() -> {
				List<ChineseSummaryLookup> compounds;
				try
				{
					compounds = new SubstringSearch(sentence, db).trySearch();
					compounds.forEach(simpleLookup -> compoundWordSet.add(simpleLookup.getChinese()));
				}
				catch(Exception e)
				{
					exceptions.add(e);
				}
			});

		}
		executor.shutdown();
		try
		{
			executor.awaitTermination(1, TimeUnit.DAYS);
		}
		catch(InterruptedException e)
		{
			exceptions.add(e);
		}
		
		if(!exceptions.isEmpty())
		{
			throw new ExceptionPile("extractCompoundWords", exceptions);
		}
		return new ArrayList<String>(compoundWordSet);
	}
}
