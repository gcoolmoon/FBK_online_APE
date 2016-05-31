package main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

public class Filter {

	// The development set contains the k segments after the training set
	public static List<List<Document>> luceneScore(TopDocs topDocs, double score) throws Exception{		

		List<Document> train = new ArrayList<Document>();
		List<Document> dev = new ArrayList<Document>();
		IndexSearcher searcher = OnlineAPE.searcherManager.acquire();

		for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
			if (((double) scoreDoc.score) >= score) {				
				train.add(searcher.doc(scoreDoc.doc));					
			} else {
				dev.add(searcher.doc(scoreDoc.doc));
				if(dev.size() == 10){
					break;
				}
			}
		}
		OnlineAPE.searcherManager.release(searcher);

		List<List<Document>> result = new ArrayList<List<Document>>(2);
		result.add(train);

		// Minimum criteria for the development set
		if(dev.size() < 10){
			dev = new ArrayList<Document>();
		}
		result.add(dev);

		return result;
	}

	// The development set contains the top-k segments of the training set
	public static ArrayList<ArrayList<Document>> luceneScore_top(TopDocs topDocs, double score, int topk) throws Exception{		

		ArrayList<Document> train = new ArrayList<Document>();
		ArrayList<Document> dev = new ArrayList<Document>();
		IndexSearcher searcher = OnlineAPE.searcherManager.acquire();

		for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
			if (((double) scoreDoc.score) >= score) {				
				train.add(searcher.doc(scoreDoc.doc));					
			}else{
				break;
			}
		}
		OnlineAPE.searcherManager.release(searcher);

		int count = 0;
		for(Document d : train){
			count += 1;
			if(count <= topk){
				dev.add(d);
			}else{
				break;
			}
		}

		ArrayList<ArrayList<Document>> result = new ArrayList<ArrayList<Document>>(2);
		result.add(train);
		result.add(dev);

		return result;
	}

	// The development set contains the k segments after the training set
	public static List<List<Document>> luceneScore(TopDocs topDocs, double threshold, boolean devFromThresh, double devSize, int devMin, int devMax) throws Exception{		
		List<Document> filtered = new ArrayList<Document>();
		List<Document> train = new ArrayList<Document>();
		List<Document> dev = new ArrayList<Document>();
		IndexSearcher searcher = OnlineAPE.searcherManager.acquire();

		for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
			if (((double) scoreDoc.score) >= threshold) {				
				filtered.add(searcher.doc(scoreDoc.doc));					
			} else if(!devFromThresh && dev.size() < devMax && dev.size() < (int)(devSize * filtered.size())) {
				dev.add(searcher.doc(scoreDoc.doc));					
			} else{
				break;
			}
		}

		if(devFromThresh){
			int size = (int)(devSize * filtered.size());
			size = size < devMax ? size : devMax;
			
			if(size >= devMin){
				Collections.shuffle(filtered, new Random(1234));
				dev = filtered.subList(0, size);
				train = filtered.subList(size, filtered.size());
			}else{
				train = filtered;
			}
		} else{
			if(dev.size() < devMin){
				dev = new ArrayList<Document>();
			}
		}

		OnlineAPE.searcherManager.release(searcher);

		List<List<Document>> result = new ArrayList<List<Document>>(2);
		result.add(train);
		result.add(dev);

		return result;
	}
}
