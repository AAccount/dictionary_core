package dt.jdictionary.dbrepo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dt.cedict.SimpleLookup;
import dt.jdictionary.dbrepo.raw.Columns;
import dt.jdictionary.dbrepo.raw.DbRepoCache;
import dt.jdictionary.dbrepo.raw.RawDictionaryRow;
import dt.jdictionary.dbrepo.raw.RawMeasureWordRow;
import dt.jdictionary.dbrepo.raw.RawSimplifiedRow;
import dt.jdictionary.dbrepo.raw.RawSubstringRow;
import dt.jdictionary.dbrepo.raw.RelatedChar;
import dt.jdictionary.dbrepo.raw.Tables;
import dt.util.ListUtils;

public class DbRepo
{
	private Connection db;

	private static final String dateTimeFormat = "yyyy-MM-dd HH:mm:ss.SSSS";
	private static final DateFormat dateFormatter = new SimpleDateFormat(dateTimeFormat);
	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimeFormat);
	private final String DictionaryBaseSql = String.format(
		"select %s, %s, %s, %s, %s, %s, %s " + 
		"from %s join %s on %s.%s = %s.%s where"
		, Columns.COL_ZH, Columns.COL_PINYIN, Columns.COL_PINYIN_NORM, Columns.COL_DEF, Columns.COL_FIRST_CHAR, Columns.COL_LAST_CHAR, Columns.COL_RANK,
		Tables.TABLE_ZHBASE, Tables.TABLE_ENGLISH, Tables.TABLE_ZHBASE, Columns.COL_ID, Tables.TABLE_ENGLISH, Columns.COL_ZHBASEID);

	public DbRepo() throws SQLException, ClassNotFoundException
	{
		final String sqlitePath = System.getProperty("user.home") + "/Programs/mdbg2_1.sqlite";
		Class.forName("org.sqlite.JDBC");
		this.db = DriverManager.getConnection("jdbc:sqlite:"+sqlitePath);
		db.setAutoCommit(false);
	}

	public void init() throws SQLException
	{
		final List<List<String>> indexes = new ArrayList<>();
		final String createZhBase = String.format("""
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
			, Tables.TABLE_ZHBASE, Columns.COL_ID, Columns.COL_ZH, Columns.COL_PINYIN, Columns.COL_PINYIN_NORM, Columns.COL_FIRST_CHAR, Columns.COL_LAST_CHAR, Columns.COL_RANK, Columns.COL_ID);
		indexes.add(List.of(Tables.TABLE_ZHBASE, Columns.COL_ID));
		indexes.add(List.of(Tables.TABLE_ZHBASE, Columns.COL_ZH));
		indexes.add(List.of(Tables.TABLE_ZHBASE, Columns.COL_FIRST_CHAR));
		indexes.add(List.of(Tables.TABLE_ZHBASE, Columns.COL_LAST_CHAR));
		indexes.add(List.of(Tables.TABLE_ZHBASE, Columns.COL_PINYIN_NORM));

		final String createEnglish = String.format("""
			CREATE TABLE %s (
				%s	INTEGER NOT NULL, 
				%s	TEXT NOT NULL
			);""", Tables.TABLE_ENGLISH, Columns.COL_ZHBASEID, Columns.COL_DEF);
		final String createEnglishFTS5 = String.format("CREATE VIRTUAL TABLE %s using fts5(%s, %s)", Tables.TABLE_ENGLISH_FTS5, Columns.COL_DEF, Columns.COL_ZHBASEID);
		indexes.add(List.of(Tables.TABLE_ENGLISH, Columns.COL_ZHBASEID));

		final String createMeasureWords = String.format("""
			CREATE TABLE %s (
				%s	TEXT NOT NULL, 
				%s	TEXT NOT NULL, 
				%s	TEXT NOT NULL, 
				PRIMARY KEY(%s,%s)
				)""", Tables.TABLE_MEASUREWORD, Columns.COL_ZH, Columns.COL_MEASURE_WORD, Columns.COL_MEASURE_PINYIN, Columns.COL_ZH, Columns.COL_MEASURE_WORD);
		indexes.add(List.of(Tables.TABLE_MEASUREWORD, Columns.COL_ZH));

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
					%s	INTEGER NOT NULL,
					"timestamp"	TEXT NOT NULL
				);
				""", Tables.TABLE_PASTHITS, Columns.COL_ZH, Columns.COL_TIMESTAMP);
		indexes.add(List.of(Tables.TABLE_PASTHITS, Columns.COL_ZH));
		indexes.add(List.of(Tables.TABLE_PASTHITS, Columns.COL_TIMESTAMP));
		
		final String[] tables = {
			createZhBase,
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
		DbRepoCache.getInstance().wipe();
	}
	
	public void saveHits(List<String> hits) throws SQLException
	{
		final String sql = String.format("INSERT INTO %s (%s, %s) VALUES (?,?)", Tables.TABLE_PASTHITS, Columns.COL_ZH, Columns.COL_TIMESTAMP);
		try(final PreparedStatement pst = db.prepareStatement(sql))
		{
			for (final String hit : hits)
			{
				pst.setString(1, hit);
				pst.setString(2, dateFormatter.format(new Date()));
				pst.addBatch();
			}
			pst.executeBatch();
		}
		db.commit();

	}
	
	public Map<String, Long> lookupPastHits(List<String> candidates) throws SQLException, ParseException
	{
		if(candidates.isEmpty())
		{
			return Map.of();
		}
		
		final String repeaterRawString = "?, ".repeat(candidates.size());
		final String repeaterString = repeaterRawString.substring(0, repeaterRawString.length() - 2);
		final String sql = String.format("select * from %s where %s in (%s) order by %s desc", Tables.TABLE_PASTHITS, Columns.COL_ZH, repeaterString, Columns.COL_TIMESTAMP);

		try(final PreparedStatement pst = db.prepareStatement(sql))
		{
			for (int i = 0; i < candidates.size(); i++)
			{
				pst.setString(i + 1, candidates.get(i));
			}

			try(final ResultSet results = pst.executeQuery())
			{
				return processRawPastHits(results);
			}
		}
	}
	
	private Map<String, Long> processRawPastHits(ResultSet results) throws SQLException, ParseException
	{
		final Map<String, Long> pastHits = new HashMap<>();
		while(results.next())
		{
			final String chinese = results.getString(Columns.COL_ZH);
			final long timestamp = LocalDateTime.parse(results.getString(Columns.COL_TIMESTAMP), dateTimeFormatter)
				.toEpochSecond(ZoneOffset.UTC);

			pastHits.put(chinese, timestamp);
		}
		return pastHits;
	}

	
	public List<RawDictionaryRow> lookupChinese(List<String> zhStrings) throws SQLException
	{
		return lookupChineseByColumn(Columns.COL_ZH, zhStrings);
	}
	
	private List<RawDictionaryRow> lookupChineseByColumn(String column, List<String> zhStrings) throws SQLException
	{
		if(zhStrings.isEmpty())
		{
			return List.of();
		}
		
		final String zhsStringsKeyString = String.join(" ", zhStrings);		
		final String repeaterRawString = "?, ".repeat(zhStrings.size());
		final String repeaterString = repeaterRawString.substring(0, repeaterRawString.length() - 2);
		final String sql = DictionaryBaseSql + " " + column + " in (" + repeaterString + ")";
		final Optional<List<RawDictionaryRow>> cached = DbRepoCache.getInstance().getTableCache(sql, zhsStringsKeyString);
		if(cached.isPresent())
		{
			return cached.get();
		}

		final List<RawDictionaryRow> rawDbRows = new ArrayList<>();
		try(final PreparedStatement pst = db.prepareStatement(sql))
		{
			for (int i = 0; i < zhStrings.size(); i++)
			{
				pst.setString(i + 1, zhStrings.get(i));
			}

			try(final ResultSet results = pst.executeQuery())
			{
				rawDbRows.addAll(processRawDbRows(results));
			}
		}
		
		DbRepoCache.getInstance().setTableCache(sql, zhsStringsKeyString, rawDbRows);
		return rawDbRows;
	}

	private List<RawDictionaryRow> lookupDictionaryTable(String sql, String target) throws SQLException
	{
		final Optional<List<RawDictionaryRow>> cached = DbRepoCache.getInstance().getTableCache(sql, target);
		if(cached.isPresent())
		{
			return cached.get();
		}

		final List<RawDictionaryRow> rawDbRows = new ArrayList<>();
		try(final PreparedStatement pst = db.prepareStatement(sql))
		{
			pst.setString(1, target);
			try(final ResultSet results = pst.executeQuery())
			{
				rawDbRows.addAll(processRawDbRows(results));
			}
		}
		DbRepoCache.getInstance().setTableCache(sql, target, rawDbRows);
		return rawDbRows;
	}
	
	private List<RawDictionaryRow> processRawDbRows(ResultSet results) throws SQLException
	{
		final List<RawDictionaryRow> rawDbRows = new ArrayList<>();
		while(results.next())
		{
			final RawDictionaryRow row =  new RawDictionaryRow(
				results.getString(Columns.COL_ZH), 
				results.getString(Columns.COL_PINYIN), 
				results.getString(Columns.COL_PINYIN_NORM),
				results.getString(Columns.COL_DEF), 
				results.getString(Columns.COL_FIRST_CHAR), 
				results.getString(Columns.COL_LAST_CHAR),
				results.getDouble(Columns.COL_RANK));
			rawDbRows.add(row);
		}
		return rawDbRows;
	}

	public String lookupSimplified(String zh) throws SQLException
	{
		final Optional<String> cached = DbRepoCache.getInstance().getSimplifiedCache(zh);
		if(cached.isPresent())
		{
			return cached.get();
		}

		String zhSimplified = "";
		final String inQuestionMarks = "?, ".repeat(zh.length());
		final String sql = String.format(
				"select * from %s where %s in (" + inQuestionMarks.substring(0, inQuestionMarks.length() - 2) + ")",
				Tables.TABLE_SIMPLIFIED, Columns.COL_OG);
		
		final Map<String, String> charMapper = new HashMap<>();
		try(final PreparedStatement pst = db.prepareStatement(sql))
		{
			for (int pstIndex = 0; pstIndex < zh.length(); pstIndex++)
			{
				pst.setString(pstIndex + 1, Character.toString(zh.charAt(pstIndex)));
			}

			try(final ResultSet results = pst.executeQuery())
			{
				while (results.next())
				{
					final String simplified = results.getString(Columns.COL_SIMPLIFIED);
					final String og = results.getString(Columns.COL_OG);
					charMapper.put(og, simplified);
				}
			}
		}

		for (final char stringChar : zh.toCharArray())
		{
			final String charAsString = Character.toString(stringChar);
			final String resultchar = charMapper.keySet().contains(charAsString) ? charMapper.get(charAsString)
						: charAsString;
				zhSimplified = zhSimplified + resultchar;
		}
		DbRepoCache.getInstance().setSimplfiedCache(zh, zhSimplified);
		return zhSimplified;
	}

	public List<String> lookupMeasureWords(String zh) throws SQLException
	{
		final Optional<List<String>> cached = DbRepoCache.getInstance().getMeasureWordCache(zh);
		if(cached.isPresent())
		{
			return cached.get();
		}

		final List<String> measureWords = new ArrayList<>();
		final String sql = String.format("select %s from %s where %s = ?", Columns.COL_MEASURE_WORD, Tables.TABLE_MEASUREWORD, Columns.COL_ZH);
		
		try(final PreparedStatement pst = db.prepareStatement(sql))
		{
			pst.setString(1, zh);

			try(final ResultSet results = pst.executeQuery())
			{	
				while (results.next())
				{
					measureWords.add(results.getString(Columns.COL_MEASURE_WORD));
				}
			}
		}
		DbRepoCache.getInstance().setMeasureWordCache(zh, measureWords);
		return measureWords;
	}

	
	public List<RawDictionaryRow> lookupRelatedWord(String zh, RelatedChar similarity) throws SQLException
	{
		final String column = similarity == RelatedChar.SAME_FRONT ? Columns.COL_FIRST_CHAR : Columns.COL_LAST_CHAR;
		final String sql = DictionaryBaseSql + " " + column + " = ?";
		return lookupDictionaryTable(sql, zh);
	}

	
	public List<RawDictionaryRow> lookupEnglish(String en) throws SQLException
	{
		final String sql = String.format("""
			select %s, %s, %s, English.%s, %s, %s, %s.%s 
			from %s 
				join %s on %s.%s = %s.%s 
				join %s on %s.%s = %s.%s
			where %s.%s match ?""",
			Columns.COL_ZH, Columns.COL_PINYIN, Columns.COL_PINYIN_NORM, Columns.COL_DEF, Columns.COL_FIRST_CHAR, Columns.COL_LAST_CHAR, Tables.TABLE_ZHBASE, Columns.COL_RANK,
			Tables.TABLE_ZHBASE,
			Tables.TABLE_ENGLISH_FTS5, Tables.TABLE_ZHBASE, Columns.COL_ID, Tables.TABLE_ENGLISH_FTS5, Columns.COL_ZHBASEID,
			Tables.TABLE_ENGLISH, Tables.TABLE_ZHBASE, Columns.COL_ID, Tables.TABLE_ENGLISH, Columns.COL_ZHBASEID,
			Tables.TABLE_ENGLISH_FTS5, Columns.COL_DEF);
		return lookupDictionaryTable(sql, en);
	}

	
	public List<String> trySubstring(String compoundWord) throws SQLException
	{
		final String sql = String.format("select %s from %s where %s = ?", Columns.COL_FULL_STRING, Tables.TABLE_SUBSTRING, Columns.COL_SUBSTRING);
		return getListOfString(sql, compoundWord, Columns.COL_FULL_STRING);
	}
	
	public List<RawDictionaryRow> findByNormalizedPinyin(List<String> normalizedPinyins) throws SQLException
	{
		return lookupChineseByColumn(Columns.COL_PINYIN_NORM, normalizedPinyins);
	}

	private List<String> getListOfString(String sql, String search, String column) throws SQLException
	{
		final Optional<List<String>> cached = DbRepoCache.getInstance().getListOfStringsCache(sql, search, column);
		if(cached.isPresent())
		{
			return cached.get();
		}

		final List<String> result = new ArrayList<>();

		try(final PreparedStatement pst = db.prepareStatement(sql))
		{
			pst.setString(1, search);
			try(final ResultSet results = pst.executeQuery())
			{
				while (results.next())
				{
					result.add(results.getString(column));
				}
			}
		}
		DbRepoCache.getInstance().setListOfStringsCache(sql, search, column, result);
		return result;
	}

	
	public void fillDictionary(List<SimpleLookup> allEntries) throws SQLException
	{
		final String sqlZhBase = String.format("INSERT INTO %s (%s, %s, %s, %s, %s, %s) VALUES (?,?,?,?,?, ?)", Tables.TABLE_ZHBASE, Columns.COL_ZH, Columns.COL_PINYIN, Columns.COL_PINYIN_NORM, Columns.COL_FIRST_CHAR, Columns.COL_LAST_CHAR, Columns.COL_RANK);
		final String sqlEnglish = String.format("INSERT INTO %s (%s, %s) VALUES (?,?)", Tables.TABLE_ENGLISH, Columns.COL_ZHBASEID, Columns.COL_DEF);
		final String sqlEnglishFTS5 = String.format("INSERT INTO %s (%s, %s) VALUES (?,?)", Tables.TABLE_ENGLISH_FTS5,
				Columns.COL_ZHBASEID, Columns.COL_DEF);

		try(final PreparedStatement pstZhBase = db.prepareStatement(sqlZhBase);
			final PreparedStatement pstEnglish = db.prepareStatement(sqlEnglish);
			final PreparedStatement pstEnglishFts5 = db.prepareStatement(sqlEnglishFTS5))
		{
			final PreparedStatement[] englishPsts = { pstEnglish, pstEnglishFts5 };

			for (final SimpleLookup entry : allEntries)
			{
				final RawDictionaryRow zhBase = new RawDictionaryRow(entry.getZh(), entry.getPinyin(), entry.getRank());
				pstZhBase.setString(1, zhBase.getZh());
				pstZhBase.setString(2, zhBase.getPinyin());
				pstZhBase.setString(3, zhBase.getPinyinNormalized());
				pstZhBase.setString(4, zhBase.getFirstChar());
				pstZhBase.setString(5, zhBase.getLastChar());
				pstZhBase.setDouble(6, zhBase.getRank());
				pstZhBase.execute();

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
		DbRepoCache.getInstance().wipe();
	}

	public void fillMeasureWords(List<RawMeasureWordRow> allRows) throws SQLException
	{
		final String sql = String.format("INSERT INTO %s (%s, %s, %s) VALUES (?,?,?)", Tables.TABLE_MEASUREWORD, Columns.COL_ZH, Columns.COL_MEASURE_WORD, Columns.COL_MEASURE_PINYIN);
		
		try(final PreparedStatement pst = db.prepareStatement(sql))
		{
			for (final RawMeasureWordRow row : allRows)
			{
				pst.setString(1, row.getZh());
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

