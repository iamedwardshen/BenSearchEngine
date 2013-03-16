/*
 * merge sort the intermediate blocks
 * output the final index with buffer "MAX_WORDINDEXS_INMEMORY"
 */
package benjamin.index;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

public class MergeSortMachine {
	//buffer of output file cache
	private static final int BUFFERSIZE = 1*1000*1000;
	//buffer of output words cache
	private static final int MAX_WORDINDEXS_INMEMORY = 500;
	//merge sort heap
	private PriorityQueue<MergeSortNode> heap;
	//words' index cache
	private ArrayList<WordIndex> finalWordsIndex;
	//global index of each word index' begin position in final index file
	private int posCount;
	
	MergeSortMachine(){
		//generate heap using intermediate blocks
		generateHeap();
		finalWordsIndex = new ArrayList<>();
		posCount = 0;
	}
	
	public void merge(){
		System.out.println("Merge Sort ...");
		MergeSortNode node = heap.poll();
		ArrayList<Integer> thisPartWordIndex = node.peak();
		WordIndex lastWord = new WordIndex(thisPartWordIndex.get(0));
		lastWord.add(thisPartWordIndex);
		if(node.next()) heap.offer(node);
		
		MergeSortNode thisNode;
		int countOutputWords = 0;
		//as each block's index has been sorted according word alphabetic order and docIDs
		//we only need to merge the smallest one every time
		while((thisNode = heap.poll()) != null){
			thisPartWordIndex = thisNode.peak();
			if(thisPartWordIndex.get(0) == lastWord.getWordID()) lastWord.add(thisPartWordIndex);
			else{				
				finalWordsIndex.add(lastWord.deepClone());
				lastWord = new WordIndex(thisPartWordIndex.get(0));
				lastWord.add(thisPartWordIndex);
				//persist word indexes
				if(finalWordsIndex.size() >= MAX_WORDINDEXS_INMEMORY){
					persistIndex(finalWordsIndex);
					finalWordsIndex.clear();
					
					countOutputWords += MAX_WORDINDEXS_INMEMORY;
					System.out.println("outputed word indexes " + countOutputWords + "/" + GlobalInfo.getWordsNum());
				}
			}
			if(thisNode.next()) heap.offer(thisNode);
		}
		finalWordsIndex.add(lastWord.deepClone());
		persistIndex(finalWordsIndex);
		
		//persist doc info
		persistDocToUrl();
	}

	//persist docID, url, number of cleaned words in this doc
	private void persistDocToUrl(){
		System.out.println("persistDocToUrl ...");
		try{
			BufferedWriter outDocIDToUrl = 
					new BufferedWriter(new FileWriter("docIDToUrl"), BUFFERSIZE);
			for(int i = 0; i < GlobalInfo.getGlobalDocs().size(); i++){
				outDocIDToUrl.write(i + " " + GlobalInfo.getGlobalDocs().get(i) + " "
						+ GlobalInfo.getGlobalSizes().get(i) + "\n");
			}
			outDocIDToUrl.close();
		}catch(Exception e){
			e.printStackTrace();
		}

	}
	
	private void persistIndex(ArrayList<WordIndex> finalWordsIndex){
		System.out.println("persist index to file ...");
		try{
			BufferedOutputStream outIndex = 
					new BufferedOutputStream(new FileOutputStream("index", true), BUFFERSIZE);
			BufferedWriter outBeginEndOfWordInIndex = 
					new BufferedWriter(new FileWriter("beginEndOfWordInIndex", true), BUFFERSIZE);
			
			//write [WordID, [DocIDs], [Frequencies], [Positions], [Contexts]]
			for(WordIndex wordIndex: finalWordsIndex){
				int beginPosCount = posCount;
				
				//output compressed wordID(int)
				byte[] out = getCompressedBytes(wordIndex.getWordID());
				outIndex.write(out);
				posCount += out.length;
				
				//output compressed docIDs(ints)
				int lastInt = 0;
				for(int i = 0; i < wordIndex.getDocIDs().size(); i++){
					int thisInt = wordIndex.getDocIDs().get(i) - lastInt;
					lastInt = wordIndex.getDocIDs().get(i);
					out = getCompressedBytes(thisInt);
					outIndex.write(out);
					posCount += out.length;
				}
				
				//output compressed frequencies
				for(int i = 0; i < wordIndex.getFrequencies().size(); i++){
					out = getCompressedBytes(wordIndex.getFrequencies().get(i));
					outIndex.write(out);
					posCount += out.length;
				}
				
				//output compressed positions
				int positionsCount = 0;
				for(int i = 0; i < wordIndex.getFrequencies().size(); i++){
					lastInt = 0;
					for(int j = 0; j < wordIndex.getFrequencies().get(i); j++){
						int thisInt = wordIndex.getPositions().get(positionsCount) - lastInt;
						lastInt = wordIndex.getPositions().get(positionsCount++);
						out = getCompressedBytes(thisInt);
						outIndex.write(out);
						posCount += out.length;
					}
				}
				
				//output contexts
				positionsCount = 0;
				for(int i = 0; i < wordIndex.getFrequencies().size(); i++){
					for(int j = 0; j < wordIndex.getFrequencies().get(i); j++){
						outIndex.write(wordIndex.getContexts().get(positionsCount++));
						posCount += 1;
					}
				}
				
				//output word index position and docFrequency info
				//[word, beginIndex, engIndex, docFrequency]
				outBeginEndOfWordInIndex.write(GlobalInfo.getWordByID(wordIndex.getWordID())
						+ " " + beginPosCount + " " + posCount + " " + 
						wordIndex.getDocFrequency() + "\n");
			}
			
			outIndex.close();
			outBeginEndOfWordInIndex.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	//using var-byte compression
	private byte[] getCompressedBytes(int number){
		//int 5 [0000 0101] => byte [0 000 0101]
		//int 200 [0000 0001] [1111 0100] => byte[1 000 0011] [0 111 0100]
		ArrayList<Byte> list = new ArrayList<>();
		do{
			byte thisByte = (byte) (number % 128);
			thisByte = (byte)  (thisByte | 0x80);
			list.add(thisByte);
			number = number / 128;
		}while(number > 0);
		byte result[] = new byte[list.size()];
		for(int i = 0; i < list.size(); i++){
			result[i] = list.get(list.size() - 1 - i);
		}
		result[list.size() - 1] = (byte) (result[list.size() - 1] & 0x7F);
		
		return result;
	}
	
	private void generateHeap(){
		File folder = new File("mergeSortTemp/");
		String[] files = folder.list();
		this.heap = new PriorityQueue<MergeSortNode>(files.length, new Comparator<MergeSortNode>(){
			//compare word alphabetic order and first docID
			public int compare(MergeSortNode a, MergeSortNode b){
				//[WordID, DocFrequency, [DocIDs], [Frequencies], [Positions], [Contexts]]
				ArrayList<Integer> tempA = a.peak();
				ArrayList<Integer> tempB = b.peak();
				String wordA = GlobalInfo.getWordByID(tempA.get(0));
				String wordB = GlobalInfo.getWordByID(tempB.get(0));
				int FIRSTDOCID = 2;
				
				if(!wordA.equals(wordB)) return wordA.compareTo(wordB);
				else if(tempA.get(FIRSTDOCID) != tempB.get(FIRSTDOCID))
					return Integer.valueOf(tempA.get(FIRSTDOCID)).compareTo(
							Integer.valueOf(tempB.get(FIRSTDOCID)));
				else return -1;
			}
		});
		for(int i = 0; i < files.length; i++){
			System.out.println("generator MergeSortNode " + i);
			this.heap.offer(new MergeSortNode(new File(folder, files[i])));
		}
	}

}

class WordIndex{
	private int wordID;
	//how many doc contents this wrod
	private int docFrequency;
	private ArrayList<Integer> docIDs;
	//frequency of this word in each docID
	private ArrayList<Integer> frequencies;
	private ArrayList<Integer> positions;
	private ArrayList<Byte> contexts;
	
	WordIndex(int wordID){
		this.wordID = wordID;
		docFrequency = 0;
		docIDs = new ArrayList<>();
		frequencies = new ArrayList<>();
		positions = new ArrayList<>();
		contexts = new ArrayList<>();
	}
	public int getDocFrequency(){
		return this.docFrequency;
	}
	public ArrayList<Integer> getDocIDs(){
		return this.docIDs;
	}
	public ArrayList<Integer> getFrequencies(){
		return this.frequencies;
	}
	public ArrayList<Integer> getPositions(){
		return this.positions;
	}
	public ArrayList<Byte> getContexts(){
		return this.contexts;
	}
	
	//return a new WordIndex with same content
	public WordIndex deepClone(){
		WordIndex clone = new WordIndex(this.wordID);
		clone.docFrequency = this.docFrequency;
		//clone docIDs
		ArrayList<Integer> cloneDocIDs = new ArrayList<>();
		cloneDocIDs.addAll(this.docIDs);
		clone.docIDs = cloneDocIDs;
		//clone frequencies
		ArrayList<Integer> cloneFrequencies = new ArrayList<>();
		cloneFrequencies.addAll(this.frequencies);
		clone.frequencies = cloneFrequencies;
		//clone positions
		ArrayList<Integer> clonePositions = new ArrayList<>();
		clonePositions.addAll(this.positions);
		clone.positions = clonePositions;
		//clone contexts
		ArrayList<Byte> cloneContexts = new ArrayList<>();
		cloneContexts.addAll(this.contexts);
		clone.contexts = cloneContexts;
		return clone;
	}
	
	@Override
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append(GlobalInfo.getWordByID(wordID));
		sb.append("(");
		sb.append(wordID);
		sb.append(") ");
		sb.append(docFrequency);
		sb.append(", ");
		sb.append(docIDs);
		sb.append(", ");
		sb.append(frequencies);
		sb.append(", ");
		sb.append(positions);
		sb.append(", ");
		sb.append(contexts);
		return sb.toString();
		
	}
	
	public int getWordID(){
		return this.wordID;
	}
	
	//merge an partial index to this one
	public void add(ArrayList<Integer> thisPartWordIndex){
		//[WordID, DocFrequency, [DocIDs], [Frequencies], [Positions], [Contexts]]
		//add docFrequency
		int docFrequency = thisPartWordIndex.get(1);
		this.docFrequency += docFrequency;
		//add docIDs
		int pos = 2;
		this.docIDs.addAll(thisPartWordIndex.subList(pos, pos + docFrequency));
		pos += docFrequency;
		//add frequencies
		int count = 0;
		this.frequencies.addAll(thisPartWordIndex.subList(pos, pos + docFrequency));
		for(int i = pos; i < pos + docFrequency; i++) count += thisPartWordIndex.get(i);
		pos += docFrequency;
		//add positions
		this.positions.addAll(thisPartWordIndex.subList(pos, pos + count));
		pos += count;
		//add context
		for(int i = pos; i < pos + count; i++) 
			this.contexts.add((byte) thisPartWordIndex.get(i).intValue());
	}
}

