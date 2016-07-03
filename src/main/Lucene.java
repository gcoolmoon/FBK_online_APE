package main;

import java.io.StringReader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

public class Lucene {
	
	public static TopDocs queryMTSrcSentence(String querySrcMTText, int max) throws Exception{
		
		TopDocs topDocs = null;
		IndexSearcher searcher = null;
		
		try {
			searcher = OnlineAPE.searcherManager.acquire();
			BooleanQuery bQuery = getBooleanQuery(OnlineAPE.analyzer, querySrcMTText, "MTSrcSentence");
			topDocs = searcher.search(bQuery, max);

		} catch (Exception e) {			
			e.printStackTrace();
		} finally{
			OnlineAPE.searcherManager.release(searcher);
		}

		return topDocs;
	}
	
	private static BooleanQuery getBooleanQuery(Analyzer analyzer, String qs,
			String fieldToSearch) throws Exception {

		BooleanQuery q = new BooleanQuery();
		TokenStream ts = analyzer.tokenStream(fieldToSearch, new StringReader(
				qs));
		CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
		ts.reset();

		while (ts.incrementToken()) {
			String termText = termAtt.toString();
			q.add(new TermQuery(new Term(fieldToSearch, termText)),
					BooleanClause.Occur.SHOULD);
		}
		ts.close();
		return q;
	}
	
	public static void add(int id, String src, String mt, String pe, String mtsrc, String pesrc, String mtpeAlign, String pepeAlign) throws Exception{		
		
		Document doc = new Document();
	    doc.add(new IntField("Id", id, Field.Store.YES));
	    doc.add(new TextField("SourceSentence", src, Field.Store.YES));
	    doc.add(new TextField("MTSentence", mt, Field.Store.YES));
	    doc.add(new TextField("PESentence", pe, Field.Store.YES));
	    doc.add(new TextField("MTSrcSentence", mtsrc, Field.Store.YES));
	    doc.add(new TextField("PESrcSentence", mtsrc, Field.Store.YES));
	    doc.add(new TextField("MTPEAlignment", mtpeAlign, Field.Store.YES));	    
	    doc.add(new TextField("PEPEAlignment", pepeAlign, Field.Store.YES));
	    OnlineAPE.index.addDocument(doc);
	    OnlineAPE.index.commit();
	    OnlineAPE.searcherManager.maybeRefresh();
	}
}
