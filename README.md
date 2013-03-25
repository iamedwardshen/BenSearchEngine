#BenSearchEngine#
Building a search engine from scratch: `benjamin.index`, `benjamin.search`

#benjamin.index##
Benjamin.index part is basically a reversed index builder using merge sorting and compression tech.

###1. data set###
	using NZ HTML pages (5.6G), 2,500,000 urls
	index file : 4.6G
	using time : 6396 seconds
	average time to find a word's index from index file: 80ms

###2. run configuration###
a. start with `Main.java`

b. put data in `NZ_data/`, with names `*_data`, `*_index`

c. it will generate `mergeSortTemp/` forder to store temp data

d. it will cost 3G memory

e. the binary output files are: `index`, `beginEndOfWordInIndex`, `docIDToUrl`

###3. First part: generate intermediate blocks###
a. pare pages iterately, generate `[wordID, DocID, Postion, Context]` pairs

b. when memory has 20000 pages info, sort its pairs and merge pairs to `[WordID, DocFrequency, [DocIDs], [Frequencies], [positions], [contexts]]`

c. persist this partially indexes blocks to disk, then continue to parse pages until all done
	
	IndexGenerator.java: main part of this project, contro the whole process
	PageGenerator.java: generate pages iterately
	TempIndexGenerator.java: parse words, sort and merge words info pairs, then generate temp blocks for mergeSort

###4. Second Part: merge sort###
use heap to merge sort partially merged block to final index

	MergeSortMachine.java: merge sort blocks to generate final index
	MergeSortNode.java: node in heap

###5. Third Part: compress###
use var-byte compression to generate index file(in IndexGenerator.java)

##benjamin.search##

###1. find index from index file###
Using Document At A Time Query Processing and BM25.
	IndexFinder.java: give queries and it will return the top K BM25 pages for that queries.
