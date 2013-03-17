/*
 * search query using reversed index with Document At A Time Query Processing and BM25
 */

package benjamin.search;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.TreeMap;

public class QuerySearch {
	private static final int BUFFERSIZE = 20*1000*1000;
	private HashMap<String, long[]> beginEndOfwordInIndex;
	private ArrayList<String> docIDToUrl;
	private ArrayList<Integer> docIDToTermSize;
	private RandomAccessFile indexFile;
	private double Daverage = 0;
	
	QuerySearch(){
		initialize();
		try{
			this.indexFile = new RandomAccessFile("index", "r");
		}catch(Exception e){
			e.printStackTrace();
		}
		//compute Daverage
		for(int i = 0; i < docIDToTermSize.size(); i++) Daverage += docIDToTermSize.get(i);
		Daverage = Daverage / docIDToTermSize.size();
	}
	
	public TreeMap<Double, String> getTop10PagesInBM25(String[] queries){
		if(queries == null) return null;
		
		TreeMap<Double, String> result = new TreeMap<>();
		PriorityQueue<BM25_URL_pair> heap= new PriorityQueue<>(10,
				new Comparator<BM25_URL_pair>(){
			public int compare(BM25_URL_pair a, BM25_URL_pair b){
				return Double.valueOf(a.getBM25()).compareTo(Double.valueOf(b.getBM25()));
			}
		});
		IndexNode[] indexNodes = new IndexNode[queries.length];
		for(int i = 0; i < queries.length; i++){
			indexNodes[i] = new IndexNode();
			indexNodes[i].setIndex(getBM25InfoOfWordIndex(queries[i]), this.docIDToUrl.size());
		}
		
		//System.out.println(indexNodes[0].getDocFrequency());
		//System.out.println(Arrays.toString(indexNodes[0].getDocIDs()));
		//System.out.println(docIDToUrl.size());
		int currentDocID = 0;
		while(currentDocID < docIDToUrl.size()){
			//get next docID in first index
			currentDocID = indexNodes[0].nextBiggerOrEqualInt(currentDocID);
			if(currentDocID >= docIDToUrl.size()) break;
			//System.out.println(currentDocID);
			
			//see if you find the same docID in other indexes
			int now = currentDocID;
			for(int i = 1; i < indexNodes.length && 
					(now = indexNodes[i].nextBiggerOrEqualInt(currentDocID)) == currentDocID;
					i++);
			if(now > currentDocID) currentDocID = now;
			else if(now == currentDocID){
				//currentDocID is in intersection
				//compute BM25
				
				//get docFrequency
				double[] Ft = new double[indexNodes.length];
				for(int i = 0; i < indexNodes.length; i++) Ft[i] = 
						indexNodes[i].getDocFrequency();
				
				//get word frequency in currentDocID
				double[]Fdt = new double[indexNodes.length];
				for(int i = 0; i < indexNodes.length; i++) Fdt[i] = 
						indexNodes[i].getCurrentFrequencies();
				
				double BM25Sum = 0;
				double k1 = 1.2;
				double b = 0.75;
				
				for(int i = 0; i< indexNodes.length; i++){
					double log = Math.log(((double) docIDToUrl.size() - Ft[i] + 0.5) /
							(Ft[i] + 0.5));
					double k = (k1 * ((1 - b) + b * 
							(double) docIDToTermSize.get(currentDocID) / Daverage));
					BM25Sum += log * ((k1 + 1) * Fdt[i]) / (k + Fdt[i]);
				}
				
				if(heap.size() >= 10) heap.poll();
				heap.offer(new BM25_URL_pair(BM25Sum, docIDToUrl.get(currentDocID)));
				
				currentDocID++;
			}
		}
		
		BM25_URL_pair pair;
		while((pair = heap.poll()) != null){
			result.put(pair.getBM25(), pair.getUrl());
		}
		return result;
	}
	
	//get [docFrequency, docIDs, frequencies]
	private int[] getBM25InfoOfWordIndex(String word){
		if(!beginEndOfwordInIndex.containsKey(word)) return null;
		
		long[] beginEnd = beginEndOfwordInIndex.get(word);
		int docFrequency = (int) beginEnd[2];
		int[] result = new int[1 + docFrequency + docFrequency];
		
		//read [docIDs, frequencies]
		byte[] buf = new byte[(docFrequency + docFrequency) * 4];
		try{
			indexFile.seek(beginEnd[0]);
			int len = 0;
			while(len < buf.length){
				len += indexFile.read(buf, len, buf.length - len);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		//build ReadBuff
		ReadBuff readBuff = new ReadBuff(buf);
		
		//add docFrequency
		result[0] = docFrequency;
		
		//add docIDs
		result[1] = readBuff.readNextInt();
		for(int i = 0; i < docFrequency; i++)
			result[2 + i] = readBuff.readNextInt() + result[2 + i - 1];
		
		//add frequencies
		for(int i = 0; i < docFrequency; i++)
			result[1 + docFrequency + i] = readBuff.readNextInt();
		
		return result;
	}
	
	//read and return:
	//[docFrequency, wordID, docIDs, frequencies, positions, contexts]
	private ArrayList<Integer> getAllInfoOfWordIndex(String word){
		if(!beginEndOfwordInIndex.containsKey(word)) return null;
		ArrayList<Integer> result = new ArrayList<>();
		
		long[] beginEnd = beginEndOfwordInIndex.get(word);
		int docFrequency = (int) beginEnd[2];
		
		//add docFrequency
		result.add(docFrequency);
		
		//read [docFrequency, docIDs, frequencies, positions, contexts]
		byte[] buf = new byte[(int) (beginEnd[1] - beginEnd[0])];
		try{
			indexFile.seek(beginEnd[0]);
			int len = 0;
			while(len < buf.length){
				len += indexFile.read(buf, len, buf.length - len);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		//build ReadBuff
		ReadBuff readBuff = new ReadBuff(buf);
		
		
		//get docIDs
		int lastInt = 0;
		for(int i = 0; i < docFrequency; i++){
			int thisInt = readBuff.readNextInt() + lastInt;
			lastInt = thisInt;
			result.add(thisInt);
			//System.out.println(thisInt);
		}
		
		//get frequencies
		ArrayList<Integer> frequencies = new ArrayList<>();
		for(int i = 0; i < docFrequency; i++){			
			int thisFrequency = readBuff.readNextInt();
			result.add(thisFrequency);
			frequencies.add(thisFrequency);
			//System.out.println(thisFrequency);
		}
		
		//get positions
		for(int i = 0; i < frequencies.size(); i++){
			lastInt = 0;
			for(int j = 0; j < frequencies.get(i); j++){
				int thisInt = readBuff.readNextInt() + lastInt;
				lastInt = thisInt;
				result.add(thisInt);
			}
		}
		
		//get contexts
		for(int i = 0; i < frequencies.size(); i++){
			for(int j = 0; j < frequencies.get(i); j++){
				int thisInt = readBuff.readNextByte();
				result.add(thisInt);
			}
		}
		
		return result;
	}
	
	private void initialize(){
		System.out.println("initialize IndexFinder");
		this.beginEndOfwordInIndex = new HashMap<>();
		this.docIDToUrl = new ArrayList<>();
		this.docIDToTermSize = new ArrayList<>();
		try{
			//beginEndOfWordInIndex
			BufferedReader brBeginEndOfwordInIndex = 
					new BufferedReader(new FileReader("beginEndOfWordInIndex"), BUFFERSIZE);
			String line;
			while((line = brBeginEndOfwordInIndex.readLine()) != null){
				String[] splits = line.split(" ");
				String word = splits[0];
				long[] value = new long[3];
				//begin pos
				value[0] = Long.valueOf(splits[1]);
				//end pod
				value[1] = Long.valueOf(splits[2]);
				//docFrequency
				value[2] = Long.valueOf(splits[3]);
				this.beginEndOfwordInIndex.put(word, value);
			}
			brBeginEndOfwordInIndex.close();
			
			//docIDToUrl
			//docIDToTermSize
			BufferedReader brDocIDToUrl = 
					new BufferedReader(new FileReader("docIDToUrl"), BUFFERSIZE);
			while((line = brDocIDToUrl.readLine()) != null){
				String[] splits = line.split(" ");
				String url = splits[1];
				int termSize = Integer.valueOf(splits[2]);
				this.docIDToUrl.add(url);
				this.docIDToTermSize.add(termSize);
			}
			brDocIDToUrl.close();
		}catch(Exception e){
			
		}
	}
	
	public static void main(String[] args){
		
		QuerySearch indexFinder = new QuerySearch();
		//[docFrequency, docIDs, frequencies, positions, contexts]
		Date begin = new Date();
		//int[] a = indexFinder.getBM25InfoOfWordIndex("car");
		//System.out.println(a.size());
		
		String[] queary = {"porn", "girl", "sex"};
		TreeMap<Double, String> result = indexFinder.getTop10PagesInBM25(queary);
		for(Entry<Double, String> entry : result.entrySet()){
			System.out.println(entry);
		}
		
		Date end = new Date();
		System.out.println("used " + (end.getTime() - begin.getTime()) + " milliseconds");

	}
}

class ReadBuff{
	private byte[] buf;
	private int pos;
	
	ReadBuff(byte[] buf){
		this.buf = buf;
		pos = 0;
	}
	
	public int getPos(){
		return pos;
	}
	
	public int readNextInt(){
		int result = 0;
		while((buf[pos] & 0x80) == 0x80){
			result = result * 128 + (buf[pos++] & 0x7F);
		}
		result = result * 128 + (buf[pos++] & 0x7F);
		return result;
	}
	
	public int readNextByte(){
		return (int) buf[pos++];
	}
}

//document at a time query processing node of each word index
class IndexNode{
	//[docFrequency, docIDs, frequencies]
	private int docFrequency;
	private int[] docIDs;
	private int[] frequencies;
	private int pos;
	private int MAXDOCID;
	
	public void setIndex(int[] index, int max){
		this.docFrequency = index[0];
		
		this.docIDs = new int[this.docFrequency];
		for(int i = 0; i < this.docFrequency; i++) this.docIDs[i] = index[i + 1];
		
		this.frequencies = new int[this.docFrequency];
		for(int i = 0; i < this.docFrequency; i++)
			this.frequencies[i] = index[i + this.docFrequency + 1];
		
		this.pos = -1;
		
		this.MAXDOCID = max;
	}
	
	public int[] getDocIDs(){
		return docIDs;
	}
	
	public int getCurrentFrequencies(){
		if(pos >= docFrequency) return this.MAXDOCID;
		return frequencies[pos];
	}
	
	public int getCurrentDocID(){
		if(pos >= docFrequency) return this.MAXDOCID;
		return docIDs[pos];
	}
	
	public int getDocFrequency(){
		return this.docFrequency;
	}
	
	public int nextBiggerOrEqualInt(int input){
		if(++pos >= docFrequency) return this.MAXDOCID;
		while(pos < docFrequency){
			if(docIDs[pos] >= input) return docIDs[pos];
			pos++;
		}
		return this.MAXDOCID;
	}
}

class BM25_URL_pair{
	private double BM25;
	private String url;
	
	BM25_URL_pair(double BM25, String url){
		this.BM25 = BM25;
		this.url = url;
	}
	
	public double getBM25(){
		return this.BM25;
	}
	public String getUrl(){
		return this.url;
	}
}


