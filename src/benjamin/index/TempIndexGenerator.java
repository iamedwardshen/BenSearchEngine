/*
 * generate intermediate blocks for merge sorting
 * first generate [WordID, DocID, Position, Context] pairs
 * then merge them to a sorted temp index
 * as [WordID, DocFrequency, [DocIDs], [Frequencies], [Positions], [Contexts]]
 * and output them to disk using compressed binary file
 */
package benjamin.index;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.zip.GZIPOutputStream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class TempIndexGenerator{
	private static final int BUFFERSIZE = 20*1000*1000;
	//WordID, DocID, Position, Context
	private ArrayList<RawInfoOfWord> rawInfoOfEachWordsInThisBlock;
	private int pageCount;
	private int persistenceCount;
	
	TempIndexGenerator(){		
		this.rawInfoOfEachWordsInThisBlock = new ArrayList<>();
		this.pageCount = 0;
		this.persistenceCount = 0;
		File mergeSortTempFile = new File("mergeSortTemp/");
		if(!mergeSortTempFile.exists()) mergeSortTempFile.mkdir();
	}
	
	public int getPageCount(){
		return this.pageCount;
	}
	
	public int getPersistenceCount(){
		return this.persistenceCount;
	}
	
	public void persist(){
		//[WordID : [DocID : [Frequency, Position, Context]]]
		LinkedHashMap<Integer, LinkedHashMap<Integer, PositionContextPair>> map = 
				combineRawInfoOfEachWordsInThisBlock(this.rawInfoOfEachWordsInThisBlock);
		
		//write [WordID, DocFrequency, [DocIDs], [Frequencies], [Positions], [Contexts]]
		buildTempWordIndexAsMerSortBlock(map);

		this.persistenceCount++;
		this.rawInfoOfEachWordsInThisBlock.clear();
		this.pageCount = 0;
	}
	
	private void buildTempWordIndexAsMerSortBlock(
			LinkedHashMap<Integer, LinkedHashMap<Integer, PositionContextPair>> map){
		System.out.println("output partially merged block ...");
		
		try{
			GZIPOutputStream gout = new GZIPOutputStream(
					new FileOutputStream(
							new File("mergeSortTemp/" + this.persistenceCount)), BUFFERSIZE);
			
			//[WordID : [DocID : [Frequency, Position, Context]]]
			Iterator<Entry<Integer, LinkedHashMap<Integer, PositionContextPair>>> mapI =
					map.entrySet().iterator();
			//iterator words
			while(mapI.hasNext()){
				//[DocFrequency, [DocIDs], [Frequencies], [Positions], [Contexts]
				ArrayList<Integer> docIDs = new ArrayList<>();
				ArrayList<Integer> frequencies = new ArrayList<>();
				ArrayList<Integer> positions = new ArrayList<>();
				ArrayList<Byte> contexts = new ArrayList<>();
				
				Entry<Integer, LinkedHashMap<Integer, PositionContextPair>> mapIEntry =
						mapI.next();
				Iterator<Entry<Integer, PositionContextPair>> littleMapI =
						mapIEntry.getValue().entrySet().iterator();
				//iterator docs
				while(littleMapI.hasNext()){
					Entry<Integer, PositionContextPair> littleMapIEntry= littleMapI.next();
					PositionContextPair thisPositionContextPair = littleMapIEntry.getValue();
					//add DocIDX
					docIDs.add(littleMapIEntry.getKey());
					//add WordsFrequencyInThisDocX
					frequencies.add(thisPositionContextPair.frequency);
					positions.addAll(thisPositionContextPair.positions);
					contexts.addAll(thisPositionContextPair.contexts);
				}

				//[WordID, DocFrequency, [DocIDs], [Frequencies], [Positions], [Contexts]]
				//convert to binary
				ByteBuffer byteBuffer = ByteBuffer.allocate(
						(1 + 1 + docIDs.size() + frequencies.size() + positions.size()) * 4
						+ contexts.size());        
		        IntBuffer intBuffer = byteBuffer.asIntBuffer();
		        //add wordID
		        intBuffer.put(mapIEntry.getKey());
		        //System.out.println(mapIEntry.getKey());
		        //add docFrequency
		        intBuffer.put(docIDs.size());
		        //System.out.println(docIDs.size());
		        //add DocIDs
		        for(int i = 0; i< docIDs.size(); i++){
		        	intBuffer.put(docIDs.get(i));
		        }
		        //System.out.println(docIDs);
		        //add frequencies
		        for(int i = 0; i< frequencies.size(); i++){
		        	intBuffer.put(frequencies.get(i));
		        }
		        //System.out.println(frequencies);
		        //add positions
		        for(int i = 0; i< positions.size(); i++){
		        	intBuffer.put(positions.get(i));
		        }
		        //System.out.println(positions);
		        //add contexts
		        for(int i = 0; i< contexts.size(); i++){
		        	byteBuffer.array()[byteBuffer.array().length - contexts.size() + i] = contexts.get(i);
		        }
		        //System.out.println(contexts);
		        //System.out.println();
		        gout.write(byteBuffer.array());
		        //System.out.println(Arrays.toString(byteBuffer.array()));
			}
			gout.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		/*
		for(Integer word : tempWordIndexAsMerSortBlock.keySet()){
			System.out.println(GlobalInfo.getWordByID(word) + " " + tempWordIndexAsMerSortBlock.get(word));
		}
		*/
	}
	
	//sort indexes
	private LinkedHashMap<Integer, LinkedHashMap<Integer, PositionContextPair>> 
		combineRawInfoOfEachWordsInThisBlock(ArrayList<RawInfoOfWord> rawInfoOfEachWordsInThisBlock){
		System.out.println("partially merge ...");
		//[WordID : [DocID : [Frequency, Position, Context]]]
		LinkedHashMap<Integer, LinkedHashMap<Integer, PositionContextPair>> map = new LinkedHashMap<>();
		RawInfoOfWord[] array = rawInfoOfEachWordsInThisBlock.toArray(new RawInfoOfWord[0]);
		Arrays.sort(array, new Comparator<RawInfoOfWord>(){
			public int compare(RawInfoOfWord a, RawInfoOfWord b){
				String as = GlobalInfo.getWordByID(a.wordID);
				String bs = GlobalInfo.getWordByID(b.wordID);
				if(as.compareTo(bs) != 0) return as.compareTo(bs);
				else if(a.docID != b.docID) 
					return Integer.valueOf(a.docID).compareTo(Integer.valueOf(b.docID));
				else if(a.position != b.position)
					return Integer.valueOf(a.position).compareTo(Integer.valueOf(b.position));
				else return 0;
			}
		});
		for(int i = 0; i < array.length; i++){
			RawInfoOfWord thisInfo = array[i];
			if(!map.containsKey(thisInfo.wordID)){
				LinkedHashMap<Integer, PositionContextPair> littleMap = new LinkedHashMap<>();
				littleMap.put(thisInfo.docID, new PositionContextPair(thisInfo.position, thisInfo.context));
				map.put(thisInfo.wordID, littleMap);
			}
			else if(!map.get(thisInfo.wordID).containsKey(thisInfo.docID)){
				map.get(thisInfo.wordID).put(thisInfo.docID, new PositionContextPair(thisInfo.position, thisInfo.context));
			}
			else{
				map.get(thisInfo.wordID).get(thisInfo.docID).add(thisInfo.position, thisInfo.context);
			}
		}
		return map;
	}
	
	//clean page and generate words and contexts
	public void clean(String page, String url){
		if(page == null || url == null || 
				!(page.length() >= 20) || !page.substring(0, 20).contains("200 OK")) return;
		int pageBegin = page.indexOf("<");
		if(pageBegin == -1) return;
		String thisPage = page.substring(pageBegin);
		
		WordContextPair uncleaned = parseWordsFromPage(thisPage);
		WordContextPair cleaned = cleanWords(uncleaned);
		int docID = GlobalInfo.putDoc(url, cleaned.size);
		
		makeRawInfoOfEachWordsInThisBlock(cleaned, docID);

		this.pageCount++;
	}
	
	private void makeRawInfoOfEachWordsInThisBlock(WordContextPair cleaned, int docID){
		for(int i = 0; i < cleaned.size; i++){
			int worldID = GlobalInfo.putWord(cleaned.words[i]);
			this.rawInfoOfEachWordsInThisBlock.add(
					new RawInfoOfWord(worldID, docID, i, cleaned.contexts[i]));
		}
	}
	
	private WordContextPair cleanWords(WordContextPair uncleaned){
		String[] words = uncleaned.words;
		byte[] contexts = uncleaned.contexts;
		ArrayList<String> cleanedWords = new ArrayList<>();
		ArrayList<Byte> cleanedContexts = new ArrayList<>();
		for(int i = 0; i < words.length; i++){
			char[] wordChar = words[i].toCharArray();
			StringBuffer sb = new StringBuffer();
			for(int j = 0; j < wordChar.length; j++){
				if(Character.isAlphabetic(wordChar[j]) || Character.isDigit((wordChar[j]))) sb.append(wordChar[j]);
				else{
					if(sb.length() > 0){
						cleanedWords.add(sb.toString().toLowerCase());
						cleanedContexts.add(contexts[i]);
					}
					sb = new StringBuffer();
				}
			}
			if(sb.length() > 0){
				cleanedWords.add(sb.toString().toLowerCase());
				cleanedContexts.add(contexts[i]);
			}
		}
		byte[] convertedCleanedContexts = new byte[cleanedContexts.size()];
		for(int i = 0; i < cleanedContexts.size(); i++) convertedCleanedContexts[i] = cleanedContexts.get(i).byteValue();
		return new WordContextPair(cleanedWords.toArray(new String[0]), convertedCleanedContexts);
	}
	
	//parse HTML with jsoup
	private WordContextPair parseWordsFromPage(String page){
		Document doc = Jsoup.parse(page);
		String[] words = doc.text().split(" ");
		byte[] contexts = new byte[words.length];
		return new WordContextPair(words, contexts);
	}

}

//the frequency, positions and contexts of a specific word in a specific doc
class PositionContextPair{
	ArrayList<Integer> positions = null;
	ArrayList<Byte> contexts = null;
	int frequency = 0;
	PositionContextPair(int firstPosition, byte firstContext){
		this.positions = new ArrayList<>();
		this.positions.add(firstPosition);
		this.contexts = new ArrayList<>();
		this.contexts.add(firstContext);
		this.frequency = 1;
	}
	
	void add(int position, byte context){
		this.positions.add(position);
		this.contexts.add(context);
		this.frequency++;
	}
}

//quadruple tuple for every word in every position of every doc
class RawInfoOfWord{
	int wordID;
	int docID;
	int position;
	byte context;
	RawInfoOfWord(int wordID, int docID, int position, byte context){
		this.wordID = wordID;
		this.docID = docID;
		this.position = position;
		this.context = context;
	}
}

//words and their according contexts
class WordContextPair{
	String[] words = null;
	byte[] contexts = null;
	int size = 0;
	WordContextPair(String[] words, byte[] contexts){
		if(words.length == contexts.length){
			this.words = words;
			this.contexts = contexts;
			this.size = words.length;
		}
	}
}
