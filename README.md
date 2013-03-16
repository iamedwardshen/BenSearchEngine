BenSearchEngine
===============

Building a search engine from scratch.

===============

benjamin.index

This benjamin.index part is basically an index builder using merge sorting and compression tech. It has been tested in NZ2 and NZ.
I will explain it in 5 parts.

1. statistics
	a. for NZ2 (800MB)
		index file : 90.4MB
		using time : 120 seconds
		average time to find a word's index from index file: 5ms
	b. for NZ (5.6G)
		index file : 4.6G
		using time : 6396 seconds
		average time to find a word's index from index file: 80ms

2. run configuration
	a. put data in NZ_data/ with relative path to the code
	b. it will generate mergeSortTemp/ forder to store temp data
	c. the out put files are: index, beginEndOfWordInIndex, docIDToUrl

3. First part: generate intermediate blocks
	a. pare pages iterately, generate [wordID, DocID, Postion, Context] pairs
	b. when memory has 20000 pages info, sort its pairs and merge pairs to [WordID, DocFrequency, [DocIDs], [Frequencies], [positions], [contexts]]
	c. persist this partial merge block to disk, then continue to parse pages until all done
	
	IndexGenerator.java: main part of this project, contro the whole process
	PageGenerator.java: generate pages iterately
	TempIndexGenerator.java: parse words from pages, sort and partially merge them, then generate temp blocks for mergeSort

4. Second Part: merge sort
	use heap to merge sort partially merged block to final index

	MergeSortMachine.java: merge sort blocks to generate final index
	MergeSortNode.java: node in heap

5. Third Part: compress
	use var-byte compression to generate index file(in IndexGenerator.java)

6. find index from index file (search)
	IndexFinder.java: give a word and it will return the [DocFrequency, [DocIDs], [Frequencies], [positions], [contexts]] of that word

===============

benjamin.search


