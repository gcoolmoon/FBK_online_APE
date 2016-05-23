package main;

import java.util.ArrayList;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

public class Filter {
	
	public static ArrayList<ArrayList<Document>> luceneScore(TopDocs topDocs, double score) throws Exception{		
		
		ArrayList<Document> train = new ArrayList<Document>();
		ArrayList<Document> dev = new ArrayList<Document>();
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
		
		ArrayList<ArrayList<Document>> result = new ArrayList<ArrayList<Document>>(2);
		result.add(train);
		result.add(dev);
		
		return result;
	}
}
