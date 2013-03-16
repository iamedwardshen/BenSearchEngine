/*
 * node in the merge sort big heap, represent each blocks
 */
package benjamin.index;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

public class MergeSortNode {
	private int currentPointToTempIndex = -1;
	private ArrayList<ArrayList<Integer>> tempIndex;
	private Block block;
	
	MergeSortNode(File tempIndexFile){
		//[WordID, DocFrequency, [DocIDs], [Frequencies], [Positions], [Contexts]]
		this.tempIndex = new ArrayList<ArrayList<Integer>>();
		this.block = new Block(tempIndexFile);
		next();
	}
	
	//get next word index in this block
	//if the cached word indexed are used up, then read more form disk
	public boolean next(){
		if(++currentPointToTempIndex >= tempIndex.size()){
			tempIndex = this.block.readNextBlock();
			currentPointToTempIndex = 0;
			if(tempIndex == null) return false;
		}
		return true;
	}

	//see the current word index
	public ArrayList<Integer> peak(){
		return tempIndex.get(currentPointToTempIndex);
	}
}

//gradually read and pare data from disk
class Block{
	private final int BUFFERSIZE = 1*1000*1000;
	private GZIPInputStream gin;
	private byte[] tempData;
	private int pos;
	private int len;
	
	Block(File tempIndexFile){
		try{
			this.gin = new GZIPInputStream(new FileInputStream(tempIndexFile), BUFFERSIZE);
			tempData = new byte[1000];
			readMore();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	//parse more indexes from disk
	public ArrayList<ArrayList<Integer>> readNextBlock(){
		if(len == -1) return null;
		
		ArrayList<ArrayList<Integer>> result = new ArrayList<ArrayList<Integer>>();
		int count = 0;
		int thisLen = this.len - this.pos;
		while(count < thisLen){
			ArrayList<Integer> thisIndex= readNextIndex();
			result.add(thisIndex);
			count += thisIndex.size() * 4;
		}
		return result;
		
	}
	
	//parse one index from disk
	private ArrayList<Integer> readNextIndex(){
		ArrayList<Integer> thisResult = new ArrayList<Integer>();
		int remain = 0;
		//add wordID
		thisResult.add(getNextInt());
		//add docFrequency
		int docFrequency = getNextInt();
		thisResult.add(docFrequency);
		//add docIDs
		for(int i = 1; i <= docFrequency; i++) thisResult.add(getNextInt());
		//add frequencies
		for(int i = 1; i <= docFrequency; i++){
			int thisFre = getNextInt();
			thisResult.add(thisFre);
			remain += thisFre;
		}
		//add positions
		for(int i = 1; i <= remain; i++) thisResult.add(getNextInt());
		//add contexts
		for(int i = 1; i <= remain; i++) thisResult.add((int) getNextByte());
		
		return thisResult;
	}
	
	//uncompress an integer from binary file
	private int getNextInt(){
		int result = (getNextByte() & 0xFF) << 24 | (getNextByte() & 0xFF) << 16 |
					(getNextByte() & 0xFF) << 8 | (getNextByte() & 0xFF);
		return result;
	}
	
	//get one byte from disk
	//if cached data is used up, then read more from disk
	private byte getNextByte(){
		byte temp = tempData[pos++];
		if(len != -1 && this.pos >= this.len) readMore();
		return temp;
	}

	//read BUFFERSIZE data from disk
	private void readMore(){
		try{
			this.len = this.gin.read(this.tempData);
			this.pos = 0;
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
