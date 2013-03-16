package benjamin.search;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class IndexFinder {
	private static final int BUFFERSIZE = 20*1000*1000;
	private HashMap<String, int[]> beginEndOfwordInIndex;
	private ArrayList<String> docIDToUrl;
	private ArrayList<Integer> docIDToTermSize;
	private RandomAccessFile indexFile;
	
	IndexFinder(){
		initialize();
		try{
			this.indexFile = new RandomAccessFile("index", "r");
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public ArrayList<Integer> getWordIndex(String word){
		if(!beginEndOfwordInIndex.containsKey(word)) return null;
		//[docFrequency, wordID, docIDs, frequencies, positions, contexts]
		ArrayList<Integer> result = new ArrayList<>();
		
		int[] beginEnd = beginEndOfwordInIndex.get(word);
		int docFrequency = beginEnd[2];
		result.add(docFrequency);
		
		//ReadBuff
		byte[] buf = new byte[beginEnd[1] - beginEnd[0]];
		try{
			indexFile.seek(beginEnd[0]);
			int len = 0;
			while(len < buf.length){
				len += indexFile.read(buf, len, buf.length - len);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		ReadBuff readBuff = new ReadBuff(buf);
		//get wordID
		int wordID = readBuff.readNextInt();
		result.add(wordID);
		//System.out.println(wordID);
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
				int[] value = new int[3];
				//begin pos
				value[0] = Integer.valueOf(splits[1]);
				//end pod
				value[1] = Integer.valueOf(splits[2]);
				//docFrequency
				value[2] = Integer.valueOf(splits[3]);
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
		
		IndexFinder indexFinder = new IndexFinder();
		//[docFrequency, wordID, docIDs, frequencies, positions, contexts]
		Date begin = new Date();
		ArrayList<Integer> a = indexFinder.getWordIndex("car");
		//System.out.println(a.size());
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






