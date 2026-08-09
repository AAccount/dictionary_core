package dt.jdictionary.dbservice;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import dt.cedict.CedictDump;
import dt.jdictionary.ChineseDefinitionLookup;
import dt.jdictionary.ChineseSummaryLookup;
import dt.jdictionary.ExhaustiveChineseLookup;
import dt.jdictionary.ProgressListener;
import dt.jdictionary.dbrepo.DbRepo;
import dt.jdictionary.dbrepo.raw.RawDictionaryRow;
import dt.jdictionary.dbservice.alternative.AlternateSearch;
import dt.jdictionary.dbservice.alternative.DeinterlaceSearch;
import dt.jdictionary.dbservice.alternative.SameBackSearch;
import dt.jdictionary.dbservice.alternative.SameFrontSearch;
import dt.jdictionary.dbservice.alternative.SimplifiedSearch;
import dt.jdictionary.dbservice.alternative.SuperstringSearch;
import dt.jdictionary.dbservice.alternative.SubstringSearch;
import dt.jdictionary.dbservice.alternative.TypoSearch;
import dt.jdictionary.util.GenerateCombinations;
import dt.util.LogUtils;

public class DbService 
{
	private static final Logger logger = Logger.getLogger(DbService.class.getName());

	private final DbRepo db;
	private final ExecutorService readExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	private final ExecutorService writeExecutor = Executors.newSingleThreadExecutor();

	public DbService() throws IOException, ParseException, ClassNotFoundException, SQLException
	{
		db = new DbRepo();
	}

	public ExhaustiveChineseLookup lookupChinese(String chinese, boolean shouldSave)
	{
		logger.info("start chinese lookup of " + chinese);
		final List<CompletableFuture> allFutures = new ArrayList<>();
		final CompletableFuture<ChineseDefinitionLookup> directResults = CompletableFuture
			.supplyAsync(() -> {
				try 
				{
					return this.lookupChineseDefinition(chinese);
				} 
				catch (Exception e) 
				{
					logger.severe("problems looking up the chinese definition\n" + LogUtils.printStackTrace(e));
					throw new RuntimeException(e.getLocalizedMessage(), e);
				}}, readExecutor)
			.exceptionally(ex -> {
				logger.severe("problems looking up the chinese definition\n" + LogUtils.printStackTrace(ex.getCause()));
				return new ChineseDefinitionLookup(chinese, Map.of(), "", List.of());			
			});
		allFutures.add(directResults);

		final List<AlternateSearch> alts = List.of(
			new SameFrontSearch(chinese, db), 
			new SameBackSearch(chinese, db), 
			new SubstringSearch(chinese, db), 
			new SuperstringSearch(chinese, db), 
			new DeinterlaceSearch(chinese, db), 
			new TypoSearch(chinese, db),
			new SimplifiedSearch(chinese, db)
		);
		final List<CompletableFuture<List<ChineseSummaryLookup>>> altFutures = new ArrayList<>();
		for(final AlternateSearch alt : alts)
		{
			final CompletableFuture<List<ChineseSummaryLookup>> altFuture = CompletableFuture
				.supplyAsync(() -> {
					try 
					{
						return alt.trySearch();
					} 
					catch (Exception e) 
					{
						logger.severe("problems with alternate search " + alt.LOOKUP_NAME() + "\n" + LogUtils.printStackTrace(e));
						throw new RuntimeException(e.getLocalizedMessage(), e);
					}}, readExecutor)
				.exceptionally(ex -> {
					logger.severe("problems with alternate search " + alt.LOOKUP_NAME() + "\n" + LogUtils.printStackTrace(ex.getCause()));
					return List.of();			
				});
			altFutures.add(altFuture);
			allFutures.add(altFuture);
		}

		final CompletableFuture<Void> allFinished = CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0]));
		final CompletableFuture<ExhaustiveChineseLookup> assembleResults = allFinished.thenApplyAsync(v -> {
			if(shouldSave)
			{
				saveChineseSearchHits(directResults.join());
			}
			final Map<String, List<ChineseSummaryLookup>> altMap = new LinkedHashMap<>();
			for(int i=0; i<altFutures.size(); i++)
			{
				final AlternateSearch searchObj = alts.get(i);
				final List<ChineseSummaryLookup> altResult = altFutures.get(i).join();
				if(altResult.isEmpty())
				{
					continue;
				}
				altMap.put(searchObj.LOOKUP_NAME(), altResult);
			}
			return new ExhaustiveChineseLookup(directResults.join(), altMap);

		}, writeExecutor);
		final ExhaustiveChineseLookup exhaustiveLookup = assembleResults.join();
		logger.info("done chinese lookup for " + chinese);
		return exhaustiveLookup;
	}
	
	private ChineseDefinitionLookup lookupChineseDefinition(String zh) throws SQLException
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
	
	private void saveChineseSearchHits(ChineseDefinitionLookup definitionLookup)
	{
		final List<String> hits = new ArrayList<>();
		if(!definitionLookup.getResults().isEmpty())
		{
			hits.add(definitionLookup.getZh());
		}
		try 
		{
			db.saveHits(hits, false);
		} 
		catch (Exception e) 
		{
			logger.severe("couldn't save search hits\n" + LogUtils.printStackTrace(e));
		}
	}
	
	public Map<String, List<ChineseSummaryLookup>> lookupEnglish(String en)
	{
		logger.info("english start " + en);

		final Map<String, CompletableFuture<List<ChineseSummaryLookup>>> wordFutures= new HashMap<>();
		final List<CompletableFuture<List<ChineseSummaryLookup>>> futures = new ArrayList<>();
		final String[] individualWords = en.split(" ");
		for(final String individualWord : individualWords)
		{
			final CompletableFuture<List<ChineseSummaryLookup>> wordFuture= CompletableFuture.supplyAsync(() -> {
				try 
				{
					return this.lookupSingleEnglishWord(individualWord);
				} 
				catch (Exception e) 
				{
					logger.severe("problems looking up english word " + individualWord + "\n" + LogUtils.printStackTrace(e));
					return List.of();
				}
			}, readExecutor);
			wordFutures.put(individualWord, wordFuture);
			futures.add(wordFuture);
		}

		final CompletableFuture<Void> allFinished = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
		allFinished.join();
		final Map<String, List<ChineseSummaryLookup>> result= new HashMap<>();
		for(final String word : wordFutures.keySet())
		{
			final List<ChineseSummaryLookup> singleResult = wordFutures.get(word).join();
			result.put(word, singleResult);
		}
		logger.info("english end " + en);

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
		final List<String> candidates = new ArrayList<>();
		for(final ChineseSummaryLookup summary : rawResults)
		{
			candidates.add(summary.getChinese());
		}
		return rawResults;
	}

	public void saveCedictDump(CedictDump dump, ProgressListener listener) throws Exception
	{
		CompletableFuture
			.runAsync(() -> {
			try 
			{
				new SaveCedict(db, dump, listener).save();
			} 
			catch (Exception e) 
			{
				logger.severe("problems saving cedict parse\n" + LogUtils.printStackTrace(e));
				throw new RuntimeException(e.getLocalizedMessage(), e);
			}
			}, writeExecutor)
			.exceptionally(ex -> {
					logger.severe("problems saving cedict " + LogUtils.printStackTrace(ex.getCause()));
					return null;
			})
			.join();
	}
	
	public void savePastHits(List<String> words, boolean verifyInDictionary)
	{
		CompletableFuture
			.runAsync(() -> {
				try 
				{
					db.saveHits(words, verifyInDictionary);
				} 
				catch (Exception e) 
				{
					logger.severe("problems saving a series of past hits\n" + LogUtils.printStackTrace(e));
					throw new RuntimeException(e.getLocalizedMessage(), e);
				}
			}, writeExecutor)
			.exceptionally(x -> {
						logger.severe("could not import past hits " + LogUtils.printStackTrace(x.getCause()));
						return null;
			})
			.join();
	}
	
	
	
	public List<String> extractCompoundWords(List<String> manySentences) throws Exception
	{
		final List<CompletableFuture<List<ChineseSummaryLookup>>> futures = new ArrayList<>();
		for(final String sentence : manySentences)
		{
			final CompletableFuture<List<ChineseSummaryLookup>> future = CompletableFuture
				.supplyAsync(() -> {
					try 
					{
						return new SubstringSearch(sentence, db).trySearch();
					} 
					catch (Exception e) 
					{
						logger.severe("problems searching compound words in sentence " + sentence + "\n" + LogUtils.printStackTrace(e));
						throw new RuntimeException(e.getLocalizedMessage(), e);
					}}, readExecutor)
				.exceptionally(ex -> {
					logger.severe("other problems extracting compound words\n" + LogUtils.printStackTrace(ex.getCause()));
					return List.of();			
				});
				futures.add(future);	
		}

		final CompletableFuture<Void> allFinished = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
		allFinished.join();		

		final List<String> results = new ArrayList<>();
		for(final CompletableFuture<List<ChineseSummaryLookup>> future : futures)
		{
			final List<ChineseSummaryLookup> summaries = future.join();
			for(final ChineseSummaryLookup summary : summaries)
			{
				results.add(summary.getChinese());
			}
		}
		return results;
	}
}
