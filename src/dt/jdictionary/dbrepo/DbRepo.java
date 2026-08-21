package dt.jdictionary.dbrepo;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import dt.cedict.SimpleLookup;
import dt.jdictionary.dbrepo.raw.Columns;
import dt.jdictionary.dbrepo.raw.DbRepoCache;
import dt.jdictionary.dbrepo.raw.RawMeasureWordRow;
import dt.jdictionary.dbrepo.raw.RawSimplifiedRow;
import dt.jdictionary.dbrepo.raw.RawSubstringRow;
import dt.jdictionary.dbrepo.raw.RelatedChar;
import dt.jdictionary.dbrepo.raw.Tables;
import dt.jdictionary.util.ChineseText;

public class DbRepo
{
	private static final Logger logger = Logger.getLogger(DbRepo.class.getName());

	private static final int MAXIMUM_RESULTS = 200; // nobody is going to check more than 20 pages of stuff
	private Connection db;
	private DbRepoCache cache = DbRepoCache.getInstance();

	private static final String dateTimeFormat = "yyyy-MM-dd HH:mm:ss.SSSS";
	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimeFormat);

	private static final String RANKED_SQL(String whereCondition)
	{
		// This SQL assumes only 0 or 1 match in the past hits column per chinese.
		// Multiple past hits rows for the same chinese cause the SQL to duplicate the definitions by as many rows for that 1 chinese.
		return String.format("""
			select %s.%s, %s, %s, group_concat(%s, ' / ') as %s, %s, %s, max(%s, coalesce(max(unixepoch(%s.%s)), 0)) as %s
			from %s 
				join %s on %s.%s = %s.%s
				left join %s on %s.%s = %s.%s
			where %s
			group by %s
			order by %s desc
			limit %s
		""", Tables.TABLE_CHINESE_BASE, Columns.COL_CHINESE, Columns.COL_PINYIN, Columns.COL_PINYIN_NORM, Columns.COL_DEF, Columns.COL_DEF, Columns.COL_FIRST_CHAR, Columns.COL_LAST_CHAR, Columns.COL_RANK, Tables.TABLE_PASTHITS, Columns.COL_TIMESTAMP, Columns.COL_RANK,
			Tables.TABLE_CHINESE_BASE,
			Tables.TABLE_ENGLISH, Tables.TABLE_CHINESE_BASE, Columns.COL_ID, Tables.TABLE_ENGLISH, Columns.COL_CHINESE_BASEID,
			Tables.TABLE_PASTHITS, Tables.TABLE_PASTHITS, Columns.COL_CHINESE, Tables.TABLE_CHINESE_BASE, Columns.COL_CHINESE,
			whereCondition,
			Columns.COL_ID,
			Columns.COL_RANK,
			MAXIMUM_RESULTS);
	}

	public DbRepo() throws SQLException, ClassNotFoundException
	{
		final Path fullPath = Path.of(System.getProperty("user.home"), "Programs", "mdbg2_3.sqlite");
		final File parentDir = fullPath.getParent().toFile();
		if(!parentDir.exists()) 
		{
			parentDir.mkdirs(); 
		}
		
		final String envSqlitePath = System.getenv("DATABASE_URL");
		logger.info("got database path from environment variabe: '" + envSqlitePath + "'");
		final String sqlitePath = (envSqlitePath != null && !envSqlitePath.isBlank()) ? envSqlitePath : fullPath.toString();
		logger.info("using database path: " + sqlitePath);

		Class.forName("org.sqlite.JDBC");
		this.db = DriverManager.getConnection("jdbc:sqlite:"+sqlitePath);
		db.setAutoCommit(false);
	}

	public void init() throws SQLException
	{
		final List<List<String>> indexes = new ArrayList<>();
		final String createChineseBase = String.format("""
				CREATE TABLE %s (
						%s	INTEGER NOT NULL, 
						%s	TEXT NOT NULL, 
						%s	TEXT NOT NULL, 
						%s TEXT NOT NULL, 
						%s TEXT, 
						%s TEXT, 
						%s REAL,
						PRIMARY KEY(%s AUTOINCREMENT)
					)
					"""
			, Tables.TABLE_CHINESE_BASE, Columns.COL_ID, Columns.COL_CHINESE, Columns.COL_PINYIN, Columns.COL_PINYIN_NORM, Columns.COL_FIRST_CHAR, Columns.COL_LAST_CHAR, Columns.COL_RANK, Columns.COL_ID);
		indexes.add(List.of(Tables.TABLE_CHINESE_BASE, Columns.COL_ID));
		indexes.add(List.of(Tables.TABLE_CHINESE_BASE, Columns.COL_CHINESE));
		indexes.add(List.of(Tables.TABLE_CHINESE_BASE, Columns.COL_FIRST_CHAR));
		indexes.add(List.of(Tables.TABLE_CHINESE_BASE, Columns.COL_LAST_CHAR));
		indexes.add(List.of(Tables.TABLE_CHINESE_BASE, Columns.COL_PINYIN_NORM));

		final String createEnglish = String.format("""
			CREATE TABLE %s (
				%s	INTEGER NOT NULL, 
				%s	TEXT NOT NULL
			);""", Tables.TABLE_ENGLISH, Columns.COL_CHINESE_BASEID, Columns.COL_DEF);
		final String createEnglishFTS5 = String.format("CREATE VIRTUAL TABLE %s using fts5(%s, %s)", Tables.TABLE_ENGLISH_FTS5, Columns.COL_DEF, Columns.COL_CHINESE_BASEID);
		indexes.add(List.of(Tables.TABLE_ENGLISH, Columns.COL_CHINESE_BASEID));

		final String createMeasureWords = String.format("""
			CREATE TABLE %s (
				%s	TEXT NOT NULL, 
				%s	TEXT NOT NULL, 
				%s	TEXT NOT NULL, 
				PRIMARY KEY(%s,%s)
				)""", Tables.TABLE_MEASUREWORD, Columns.COL_CHINESE, Columns.COL_MEASURE_WORD, Columns.COL_MEASURE_PINYIN, Columns.COL_CHINESE, Columns.COL_MEASURE_WORD);
		indexes.add(List.of(Tables.TABLE_MEASUREWORD, Columns.COL_CHINESE));

		final String createSimplified = String.format("""
			CREATE TABLE %s (
				%s	TEXT NOT NULL, 
				%s	TEXT NOT NULL,
				PRIMARY KEY(%s,%s)
			)""", Tables.TABLE_SIMPLIFIED, Columns.COL_OG, Columns.COL_SIMPLIFIED, Columns.COL_OG, Columns.COL_SIMPLIFIED);
		indexes.add(List.of(Tables.TABLE_SIMPLIFIED, Columns.COL_OG));
		indexes.add(List.of(Tables.TABLE_SIMPLIFIED, Columns.COL_SIMPLIFIED));

		final String createSubstrings = String.format("""
			CREATE TABLE %s (
				%s	TEXT NOT NULL, 
				%s	TEXT NOT NULL, 
				PRIMARY KEY(%s,%s)
			)""", Tables.TABLE_SUBSTRING, Columns.COL_SUBSTRING, Columns.COL_FULL_STRING, Columns.COL_SUBSTRING, Columns.COL_FULL_STRING);
		indexes.add(List.of(Tables.TABLE_SUBSTRING, Columns.COL_SUBSTRING));

		final String createPastHits = String.format("""
				CREATE TABLE IF NOT EXISTS %s (
					%s	TEXT NOT NULL,
					%s	TEXT NOT NULL,
					PRIMARY KEY(%s)
				);
				""", Tables.TABLE_PASTHITS, Columns.COL_CHINESE, Columns.COL_TIMESTAMP, Columns.COL_CHINESE);
		indexes.add(List.of(Tables.TABLE_PASTHITS, Columns.COL_CHINESE));
		indexes.add(List.of(Tables.TABLE_PASTHITS, Columns.COL_TIMESTAMP));
		
		final String[] tables = {
			createChineseBase,
			createEnglish, createEnglishFTS5,
			createMeasureWords,
			createSimplified,
			createSubstrings,
			createPastHits
		};


		for(final String table : tables)
		{
			try(final Statement stmt = db.createStatement())
			{
				stmt.execute(table);
			}
		}

		for(final List<String> index : indexes)
		{
			try(final Statement stmt = db.createStatement())
			{
				final String table = index.get(0);
				final String column = index.get(1);
				stmt.execute(String.format("CREATE INDEX IF NOT EXISTS %sSort%s ON %s (%s)", table, column, table, column));
			}
		}
	}

	
	public void wipe() throws SQLException
	{
		final List<String> tables = new ArrayList<>();
		try(final Statement findTables = db.createStatement();
		final ResultSet foundTables = findTables.executeQuery("SELECT name FROM sqlite_master WHERE type='table' and name not like 'sqlite_%' and name <> '" + Tables.TABLE_PASTHITS + "'"))
		{
			db.setAutoCommit(true);
			while (foundTables.next())
			{
				tables.add(foundTables.getString(1));
			}
		}
		
		try(final Statement rm = db.createStatement())
		{
			for (final String table : tables)
			{
				rm.execute("drop table if exists " + table + ";");
			}
		}

		try(final Statement vaccuum = db.createStatement())
		{
			vaccuum.execute("vacuum;");
			db.setAutoCommit(false);
		}
		cache.wipe();
	}
	
	public void saveHits(List<String> hits, boolean validateAgainstDictionary) throws SQLException
	{
		final List<String> entries = validateAgainstDictionary ? filterWordsToKnown(hits) : hits;
		final String sql = String.format("""
			INSERT INTO %s (%s, %s) 
			VALUES (?,?)
			ON CONFLICT(%s)
			DO UPDATE SET %s = EXCLUDED.%s
			""", Tables.TABLE_PASTHITS, 
			Columns.COL_CHINESE, Columns.COL_TIMESTAMP,
			Columns.COL_CHINESE,
			Columns.COL_TIMESTAMP, Columns.COL_TIMESTAMP);
		try(final PreparedStatement pst = db.prepareStatement(sql))
		{
			for (final String entry : entries)
			{
				pst.setString(1, entry);
				pst.setString(2, LocalDateTime.now().format(dateTimeFormatter));
				pst.addBatch();
			}
			pst.executeBatch();
		}
		db.commit();

		invalidateCacheAfterHits(hits);
	}

	private void invalidateCacheAfterHits(List<String> hits)
	{
		cache.invalidateTableColumnCache(hits);

		final List<String> fronts = new ArrayList<>();
		final List<String> backs = new ArrayList<>();
		for(final String hit : hits)
		{
			final List<String> chars = ChineseText.charsByCodepoint(hit);
			if(!chars.isEmpty())
			{
				fronts.add(chars.get(0));
				backs.add(chars.get(chars.size() - 1));
			}
		}
		
		cache.invalidateTableColumnCache(fronts);
		cache.invalidateTableColumnCache(backs);
	}

	private List<String> filterWordsToKnown(List<String> words) throws SQLException
	{
		final List<DictionaryEntry> entries = lookupChinese(words);
		final List<String> results = new ArrayList<>();
		for(final DictionaryEntry entry : entries)
		{
			results.add(entry.getChinese());
		}
		return results;
	}
	
	public List<DictionaryEntry> lookupChinese(List<String> strings) throws SQLException
	{
		return lookupChineseByColumn(Tables.TABLE_CHINESE_BASE + "." +Columns.COL_CHINESE, strings);
	}
	
	private List<DictionaryEntry> lookupChineseByColumn(String column, List<String> strings) throws SQLException
	{
		if(strings.isEmpty())
		{
			logger.info("did not get any strings to lookup for column " + column);
			return List.of();
		}
		
		final List<DictionaryEntry> cachedEntries = new ArrayList<>();
		final List<String> noCache = new ArrayList<>();
		for(final String str : strings)
		{
			final List<DictionaryEntry> inCache = cache.getTableColumnCache(Tables.TABLE_CHINESE_BASE, column, str);
			if(inCache.isEmpty())
			{
				noCache.add(str);
			}
			else
			{
				cachedEntries.addAll(inCache);
			}
		}
		final String logPrefix = "lookup chinese by column (" + column + ") strings " + strings + " ";
		logger.info(logPrefix +"cached entries " + cachedEntries.size() + " uncached entries " + noCache.size());
		if(noCache.isEmpty())
		{
			logger.info(logPrefix + "all entries for string are cached");
			return cachedEntries;
		}

		final String repeaterRawString = "?, ".repeat(noCache.size());
		final String repeaterString = repeaterRawString.substring(0, repeaterRawString.length() - 2);
		final String where = column + " in (" + repeaterString + ")";
		final String sql = RANKED_SQL(where);

		final List<DictionaryEntry> entries = new ArrayList<>();
		try(final PreparedStatement pst = db.prepareStatement(sql))
		{
			for (int i = 0; i < noCache.size(); i++)
			{
				pst.setString(i + 1, noCache.get(i));
			}

			try(final ResultSet results = pst.executeQuery())
			{
				entries.addAll(processRawDbRows(results));
			}
		}
		
		for(final DictionaryEntry newRow : entries)
		{
			cache.setResultsForTableColumn(Tables.TABLE_CHINESE_BASE, column, newRow.getChinese(), newRow);
		}
		entries.addAll(cachedEntries);
		return entries;
	}

	private List<DictionaryEntry> lookupDictionaryTable(String sql, String target) throws SQLException
	{
		final List<DictionaryEntry> entries = new ArrayList<>();
		try(final PreparedStatement pst = db.prepareStatement(sql))
		{
			pst.setString(1, target);
			try(final ResultSet results = pst.executeQuery())
			{
				entries.addAll(processRawDbRows(results));
			}
		}
		return entries;
	}
	
	private List<DictionaryEntry> processRawDbRows(ResultSet results) throws SQLException
	{
		final List<DictionaryEntry> entries = new ArrayList<>();
		while(results.next())
		{
			final DictionaryEntry row =  new DictionaryEntry(
				results.getString(Columns.COL_CHINESE), 
				results.getString(Columns.COL_PINYIN), 
				results.getString(Columns.COL_PINYIN_NORM),
				results.getString(Columns.COL_DEF), 
				results.getString(Columns.COL_FIRST_CHAR), 
				results.getString(Columns.COL_LAST_CHAR),
				results.getDouble(Columns.COL_RANK));
			entries.add(row);
		}
		return entries;
	}

	public Map<String, List<String>> lookupReverseSimplified(List<String> characters) throws SQLException
	{
		final Map<String, List<String>> reverseResults = new HashMap<>();
		final List<String> noCache = new ArrayList<>();
		for(final String character : characters)
		{
			final List<String> cached = cache.getReverseSimplified(character);
			if(cached != null)
			{
				reverseResults.put(character, cached);
			}
			else
			{
				noCache.add(character);
			}
		}
		logger.info("reverse simplified for " + String.join(", ", characters) + " in cache: " + reverseResults.size() + " NOT in cache: " + noCache.size());

		if(noCache.isEmpty())
		{
			logger.info("everything in the cache for reverse simplified: " + String.join(", ", characters));
			return reverseResults;
		}

		final String inQuestionMarks = "?, ".repeat(noCache.size());
		final String sql = String.format(
				"select * from %s where %s in (" + inQuestionMarks.substring(0, inQuestionMarks.length() - 2) + ")",
				Tables.TABLE_SIMPLIFIED, Columns.COL_SIMPLIFIED);	
		try(final PreparedStatement pst = db.prepareStatement(sql))
		{
			for(int i=1; i<=noCache.size(); i++)
			{
				pst.setString(i, noCache.get(i-1));
			}

			try(final ResultSet results = pst.executeQuery())
			{
				while(results.next())
				{
					final String simplified = results.getString(Columns.COL_SIMPLIFIED);
					final String og = results.getString(Columns.COL_OG);
					if(!reverseResults.containsKey(simplified))
					{
						reverseResults.put(simplified, new ArrayList<>());
					}
					reverseResults.get(simplified).add(og);
					cache.setReverseSimplified(simplified, og);
				}
			}
		}

		for(final String character : characters)
		{
			if(!reverseResults.containsKey(character))
			{
				// this character is the same simplified and traditional
				final List<String> nochange = List.of(character);
				reverseResults.put(character, nochange);
				cache.setReverseSimplified(character, character);
			}
		}
		return reverseResults;
	}

	public String lookupSimplified(String original) throws SQLException
	{
		final int[] codePoints = original.codePoints().toArray();
		final List<Integer> unCached = new ArrayList<>();
		final Map<Integer, String> simplifiedMapping = new HashMap<>();
		for(final int codepoint : codePoints)
		{
			final String cached = cache.getSimplifiedCache(codepoint);
			if(cached != null)
			{
				simplifiedMapping.put(codepoint, cached);
			}
			else
			{
				unCached.add(codepoint);
			}
		}
		logger.info("simplified cache for " + original + " in cache: " + simplifiedMapping.size() + " not in cache: " + unCached.size());

		if(!unCached.isEmpty())
		{
			final Map<Integer, String> dbLookups = simpliedFromDb(unCached);
			for(final Integer codepoint : dbLookups.keySet())
			{
				final String simplified = dbLookups.get(codepoint);
				simplifiedMapping.put(codepoint, simplified);
				cache.setSimplfiedCache(codepoint, simplified);
			}
		}
		
		final StringBuilder simplified = new StringBuilder();
		for (final int codepoint : codePoints)
		{
			final String resultchar = simplifiedMapping.getOrDefault(codepoint, Character.toString(codepoint));
			simplified.append(resultchar);

			// To avoid relooking up characters with no simplified form, cheat and set the simplified as itself.
			if(!simplifiedMapping.containsKey(codepoint))
			{
				cache.setSimplfiedCache(codepoint, Character.toString(codepoint));
			}
		}

		final String result = simplified.toString();
		return result;
	}

	private Map<Integer, String> simpliedFromDb(List<Integer> codepoints) throws SQLException
	{
		final String inQuestionMarks = "?, ".repeat(codepoints.size());
		final String sql = String.format(
				"select * from %s where %s in (" + inQuestionMarks.substring(0, inQuestionMarks.length() - 2) + ")",
				Tables.TABLE_SIMPLIFIED, Columns.COL_OG);
		
		final Map<Integer, String> charMapper = new HashMap<>();
		try(final PreparedStatement pst = db.prepareStatement(sql))
		{
			for(int pstIndex = 0; pstIndex < codepoints.size(); pstIndex++)
			{
				pst.setString(pstIndex + 1, Character.toString(codepoints.get(pstIndex)));
			}

			try(final ResultSet results = pst.executeQuery())
			{
				while(results.next())
				{
					final String simplified = results.getString(Columns.COL_SIMPLIFIED);
					final String og = results.getString(Columns.COL_OG);
					charMapper.put(og.codePointAt(0), simplified);
				}
			}
		}
		return charMapper;
	}

	public List<String> lookupMeasureWords(String chinese) throws SQLException
	{
		final List<String> cached = cache.getMeasureWordCache(chinese);
		if(cached != null)
		{
			return cached;
		}

		final List<String> measureWords = new ArrayList<>();
		final String sql = String.format("select %s from %s where %s = ?", Columns.COL_MEASURE_WORD, Tables.TABLE_MEASUREWORD, Columns.COL_CHINESE);
		
		try(final PreparedStatement pst = db.prepareStatement(sql))
		{
			pst.setString(1, chinese);

			try(final ResultSet results = pst.executeQuery())
			{	
				while (results.next())
				{
					measureWords.add(results.getString(Columns.COL_MEASURE_WORD));
				}
			}
		}
		cache.setMeasureWordCache(chinese, measureWords);
		return measureWords;
	}

	public List<DictionaryEntry> lookupRelatedWord(String baseWord, RelatedChar similarity) throws SQLException
	{
		final String column = similarity == RelatedChar.SAME_FRONT ? Columns.COL_FIRST_CHAR : Columns.COL_LAST_CHAR;
		final List<DictionaryEntry> cached = cache.getTableColumnCache(Tables.TABLE_CHINESE_BASE, column, baseWord);
		if(!cached.isEmpty())
		{
			logger.info("related word for " + baseWord + " was cached");
			return cached;
		}
		logger.info("related word for " + baseWord + " not in the cache");

		final String where = column + " = ?";
		final List<DictionaryEntry> result =  lookupDictionaryTable(RANKED_SQL(where), baseWord);
		for(final DictionaryEntry newRow : result)
		{
			cache.setResultsForTableColumn(Tables.TABLE_CHINESE_BASE, column, baseWord, newRow);
		}
		return result;
	}

	public List<DictionaryEntry> lookupEnglish(String en) throws SQLException
	{
		final List<DictionaryEntry> cached = cache.getTableColumnCache(Tables.TABLE_ENGLISH, Columns.COL_DEF, en);
		if(!cached.isEmpty())
		{
			logger.info("english search for " + en + " was cached");
			return cached;
		}
		logger.info("english search for " + en + " not in the cache");

		final String sql = String.format("""
			select %s.%s, %s, %s, English.%s, %s, %s, max(%s.%s, coalesce(max(unixepoch(%s.%s)), 0)) as %s 
			from %s 
				join %s on %s.%s = %s.%s 
				join %s on %s.%s = %s.%s
				left join %s on %s.%s = %s.%s
			where %s.%s match ?
			group by %s
			order by %s desc
			limit %s
		""",
			Tables.TABLE_CHINESE_BASE, Columns.COL_CHINESE, Columns.COL_PINYIN, Columns.COL_PINYIN_NORM, Columns.COL_DEF, Columns.COL_FIRST_CHAR, Columns.COL_LAST_CHAR, Tables.TABLE_CHINESE_BASE, Columns.COL_RANK, Tables.TABLE_PASTHITS, Columns.COL_TIMESTAMP, Columns.COL_RANK,
			Tables.TABLE_CHINESE_BASE,
			Tables.TABLE_ENGLISH_FTS5, Tables.TABLE_CHINESE_BASE, Columns.COL_ID, Tables.TABLE_ENGLISH_FTS5, Columns.COL_CHINESE_BASEID,
			Tables.TABLE_ENGLISH, Tables.TABLE_CHINESE_BASE, Columns.COL_ID, Tables.TABLE_ENGLISH, Columns.COL_CHINESE_BASEID,
			Tables.TABLE_PASTHITS, Tables.TABLE_PASTHITS, Columns.COL_CHINESE, Tables.TABLE_CHINESE_BASE, Columns.COL_CHINESE,
			Tables.TABLE_ENGLISH_FTS5, Columns.COL_DEF,
			Columns.COL_ID,
			Columns.COL_RANK,
			MAXIMUM_RESULTS);
		final List<DictionaryEntry> results = lookupDictionaryTable(sql, en);
		for(final DictionaryEntry result : results)
		{
			cache.setResultsForTableColumn(Tables.TABLE_ENGLISH, Columns.COL_DEF, en, result);
		}
		return results;
	}
	
	public List<DictionaryEntry> findByNormalizedPinyin(List<String> normalizedPinyins) throws SQLException
	{
		return lookupChineseByColumn(Columns.COL_PINYIN_NORM, normalizedPinyins);
	}

	public List<String> lookupSuperstrings(String chinese) throws SQLException
	{
		final String sql = String.format("select %s from %s where %s = ?", Columns.COL_FULL_STRING, Tables.TABLE_SUBSTRING, Columns.COL_SUBSTRING);
		final List<String> cached = cache.getSuperstrings(chinese);
		if(cached != null)
		{
			logger.info("superstrings for " + chinese + " in cache");
			return cached;
		}
		logger.info("superstrings for " + chinese + "  NOT in cache");

		final List<String> result = new ArrayList<>();
		try(final PreparedStatement pst = db.prepareStatement(sql))
		{
			pst.setString(1, chinese);
			try(final ResultSet results = pst.executeQuery())
			{
				while (results.next())
				{
					result.add(results.getString(Columns.COL_FULL_STRING));
				}
			}
		}
		cache.setSuperstrings(chinese, result);
		return result;
	}

	
	public void fillDictionary(List<SimpleLookup> allEntries) throws SQLException
	{
		final String sqlChineseBase = String.format("INSERT INTO %s (%s, %s, %s, %s, %s, %s) VALUES (?,?,?,?,?, ?)", Tables.TABLE_CHINESE_BASE, Columns.COL_CHINESE, Columns.COL_PINYIN, Columns.COL_PINYIN_NORM, Columns.COL_FIRST_CHAR, Columns.COL_LAST_CHAR, Columns.COL_RANK);
		final String sqlEnglish = String.format("INSERT INTO %s (%s, %s) VALUES (?,?)", Tables.TABLE_ENGLISH, Columns.COL_CHINESE_BASEID, Columns.COL_DEF);
		final String sqlEnglishFTS5 = String.format("INSERT INTO %s (%s, %s) VALUES (?,?)", Tables.TABLE_ENGLISH_FTS5,
				Columns.COL_CHINESE_BASEID, Columns.COL_DEF);

		try(final PreparedStatement pstChineseBase = db.prepareStatement(sqlChineseBase);
			final PreparedStatement pstEnglish = db.prepareStatement(sqlEnglish);
			final PreparedStatement pstEnglishFts5 = db.prepareStatement(sqlEnglishFTS5))
		{
			final PreparedStatement[] englishPsts = { pstEnglish, pstEnglishFts5 };

			for (final SimpleLookup entry : allEntries)
			{
				final DictionaryEntry chineseBase = new DictionaryEntry(entry.getChinese(), entry.getPinyin(), entry.getRank());
				pstChineseBase.setString(1, chineseBase.getChinese());
				pstChineseBase.setString(2, chineseBase.getPinyin());
				pstChineseBase.setString(3, chineseBase.getPinyinNormalized());
				pstChineseBase.setString(4, chineseBase.getFirstChar());
				pstChineseBase.setString(5, chineseBase.getLastChar());
				pstChineseBase.setDouble(6, chineseBase.getRank());
				pstChineseBase.execute();

				try(final PreparedStatement getId = db.prepareStatement("select last_insert_rowid() as id;");
				final ResultSet getIdResults = getId.executeQuery())
				{
					getIdResults.next();
					final int id = getIdResults.getInt("id");

					for (final PreparedStatement pstEn : englishPsts)
					{
						for (final String definition : entry.getDefinitions())
						{
							pstEn.setInt(1, id);
							pstEn.setString(2, definition);
							pstEn.addBatch();
						}
					}
				}
			}
			pstEnglish.executeBatch();
			pstEnglishFts5.executeBatch();
		}
		db.commit();
		cache.wipe();
	}

	public void fillMeasureWords(List<RawMeasureWordRow> allRows) throws SQLException
	{
		final String sql = String.format("INSERT INTO %s (%s, %s, %s) VALUES (?,?,?)", Tables.TABLE_MEASUREWORD, Columns.COL_CHINESE, Columns.COL_MEASURE_WORD, Columns.COL_MEASURE_PINYIN);
		
		try(final PreparedStatement pst = db.prepareStatement(sql))
		{
			for (final RawMeasureWordRow row : allRows)
			{
				pst.setString(1, row.getChinese());
				pst.setString(2, row.getMeasure());
				pst.setString(3, row.getMeasurePinyin());
				pst.addBatch();
			}
			pst.executeBatch();
		}
		db.commit();

	}
	
	public void fillSimplified(List<RawSimplifiedRow> allRows) throws SQLException
	{
		final String sql = String.format("INSERT INTO %s (%s, %s) VALUES (?,?)", Tables.TABLE_SIMPLIFIED, Columns.COL_OG, Columns.COL_SIMPLIFIED);
		
		try(final PreparedStatement pst = db.prepareStatement(sql))
		{
			for (final RawSimplifiedRow row : allRows)
			{
				pst.setString(1, row.getOriginal());
				pst.setString(2, row.getSimplified());
				pst.addBatch();
			}
			pst.executeBatch();
		}
		db.commit();
	}

	public void fillSubstrings(List<RawSubstringRow> allRows) throws SQLException
	{
		final String sql = String.format("INSERT INTO %s (%s, %s) VALUES (?,?) ON CONFLICT(%s, %s) DO NOTHING;", Tables.TABLE_SUBSTRING, Columns.COL_SUBSTRING, Columns.COL_FULL_STRING, Columns.COL_SUBSTRING, Columns.COL_FULL_STRING);
		try(final PreparedStatement pst = db.prepareStatement(sql))
		{
			for (final RawSubstringRow row : allRows)
			{
				pst.setString(1, row.getSubstring());
				pst.setString(2, row.getFullString());
				pst.addBatch();
			}
			pst.executeBatch();
		}
		db.commit();
	}
}

