#BenSearchEngine#
Building a search engine from scratch: `benjamin.index`, `benjamin.search`

###1. statistics ###
	data set: 2,500,000 urls, 5.6G raw HTML pages
	generated index file(var-byte compressed): 4.6G
	time used to generate the index file: 6396 seconds
	average time used to search queries: 80 ms

###2. run configuration###
a. start with `benjamin.index.Main.java`

b. put raw HTML pages data(gz compressed) in `NZ_data/`, with each page block's name as `*_data`, `*_index`

c. it will generate `mergeSortTemp/` forder to store temp data

d. it will cost maximum 3G memory

e. output files are: `index` in binary, `beginEndOfWordInIndex` in ASCII, `docIDToUrl` in ASCII

#benjamin.index##
benjamin.index part is basically a reversed index builder using merge sorting and compression tech.

###1. generate intermediate blocks for disk merge sort ###
a. pare pages iterately, generate `[wordID, DocID, Postion, Context]` pairs

b. when memory has 20000 pages info, sort these pairs and merge pairs to `[WordID, DocFrequency, [DocIDs], [Frequencies], [positions], [contexts]]`

c. persist this partially indexes blocks to disk, then continue to parse pages until all done
	
	IndexGenerator.java: main part of this project, contro the whole process
	PageGenerator.java: generate pages iterately
	TempIndexGenerator.java: parse words, sort and merge words info pairs, then store temp blocks in disk for mergeSort

###2. merge sort###
use heap to merge sort partially merged block to final index

	MergeSortMachine.java: merge sort blocks to generate final index
	MergeSortNode.java: temp block node used in heap

###3. compression###
use var-byte compression to generate index file(in IndexGenerator.java)

##benjamin.search##
benjamin.search part is basically a queries search engine, generate results(urls) for each queries using some ranking algorithms

###1. queries search engine###
Using Document At A Time Query Processing and BM25 ranking algorithm.

	QuerySearch.java: give queries and it will return the top K BM25 pages for that queries.

