# Dictionary Backend

## Overview
This is the heart of the English Chinese dictionary. This does all the actual database reads and writes, as well as alternate lookup strategies for a given Chinese string. It will save cedict parses from the cedict repo.

## Chinese to English
This lookup strategy is designed for someone who only has an English background. It does not assume any formal Chinese study. I am mostly self taught, which is why I recycle concepts like "compound word" and don't use whatever the official academic term for the Chinese equivalent is.

Just like the English compound word cannot = can + not, I use the same concept in Chinese as "雖然" = 雖 + 然.

When given a string of Chinese characters, it is automatically assumed to be traditional Chinese, It then attempts the following lookups:
- try to find a dictionary entry for the entire string.
- find compound words or sayings that start with the same first character. If given **沒** or **沒**有 it will return **沒**事, for example, and give the definition of 沒事.
- find compound words or sayings the end with the same last character. If given 當**然** or just **然**, it will return 雖**然**, for example and give the definition of 雖然.
- "deinterlace" search: if the string is 3 or 4 chars: 1234 or 123, it will search for 13 and 24. This is an annoying trend in some sketchy Chinese manga websites, where compound words get split up by another character or a 4 character saying is shuffled around a bit.
- Substring search: do a leetcode style enumeration of all substrings given a full string of Chinese characters. See if any of those have dictionary entries. This is intended for when you are unsure of where compound words are in a string of Chinese. This will find them for you.
- Superstring search: see if the string is part of a larger saying. For example: 沒有意 -> 沒有意義. This is intended for when you don't recognize a string of characters as part of a larger saying. This will complete it for you.
- Typo search: given a string of Chinese: 123, look up each character's possible pinyin Romanizations, normalize them without the accents, and do a leetcode style enumaration of every possible combinations. For example if char 1 has 2 pinyins a and b, it will look up pinyins of 1a23, 1b23. This is also intended for sketchy manga websites who lack proper proofreading.
- Simplified search: given a string of Chinese, assume it is simplified, reverse it to traditional, and see if any of those traditional strings have dictionary entries. Note: due to an n:1 process of traditonal:simplified (發髮 -> 发) this search will have to try both 發髮 if it gets 发.

## English to Chinese
English to Chinese is very primitive and not used much. It given a string of English "word1 word2 word3", it does a leetcode style enumeration of every possible combination "word1", "word2", "word3", "word1 word2" etc, and sees if any full text search 5 (fts5) entries of the definitions match.
