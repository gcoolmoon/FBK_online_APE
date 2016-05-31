package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class OnlineAPE {

	private static String srcFile;
	private static String mtFile;
	private static String peFile;
	private static String mtsrcFile;
	private static String mtpeAlignFile;
	private static String workDir;
	private static int max;
	private static float threshold;	
	private static String scriptDir;
	private static int loadPrevious;
	private static int numberOfUpdates;

	public static IndexWriter index;
	public static Analyzer analyzer;
	public static SearcherManager searcherManager;

	private static Map<String, String> defaultFeatureMap = new HashMap<String, String>();

	public static Logger log;

	public static void main(String[] args) {		


		// Initialize configuration parameters
		try {
			initConfigParams(args[0]);			
		} catch (Exception e) {
			System.out.println("Failed to initialize configuration parameters  (see trace below). The program will terminate !!");
			e.printStackTrace();
			System.exit(1);
		}
		//Initialize logger
		System.setProperty("logfile.name",workDir);		
		OnlineAPE.log = Logger.getLogger(OnlineAPE.class.getName());

		if(OnlineAPE.loadPrevious == 0){

			// Check if there is already a working directory
			log.info("Checking if working directory already exist");
			if(new File(workDir + "/global").exists() || new File(workDir + "/local").exists()){
				log.error("There already exist some files in '" + workDir + "'. \nPlease specify a different working directory or delete all files in it. The program will terminate !!");
				System.exit(1);						
			} 

			// Create a working environment
			log.info("Creating working environment");
			try {
				createDir(OnlineAPE.workDir);
				createDir(OnlineAPE.workDir + "/global");
				log.debug("Created directory " + OnlineAPE.workDir + "/global");
				createDir(OnlineAPE.workDir + "/global/lucene");
				log.debug("Created directory " + OnlineAPE.workDir + "/global/lucene");
				createDir(OnlineAPE.workDir + "/global/lm");
				log.debug("Created directory " + OnlineAPE.workDir + "/global/lm");
				createDir(OnlineAPE.workDir + "/local");
				log.debug("Created directory " + OnlineAPE.workDir + "/local");
				log.info("Created working environment");
			} catch (Exception e) {
				log.error("Failed to create working environment  (see trace below). The program will terminate !!");
				log.error(e);
				System.exit(1);
			}
		}

		// Initialize Lucene parameters
		log.info("Initializing Lucene parameters");
		try {
			initLuceneParams();
			log.info("Initialized Lucene parameters");
		} catch (Exception e) {
			log.error("Failed to initialize lucene parameters  (see trace below). The program will terminate !!");
			log.error(e);
			System.exit(1);
		}

		// Initialize a feature map with default weights 
		log.info("Initializing a feature map with default MOSES weight");
		initDefaultFeatureMap();

		// Load input documents
		log.info("Loading necessary documents");
		BufferedReader srcBr = null;
		BufferedReader mtBr = null;
		BufferedReader peBr = null;
		BufferedReader mtsrcBr = null;
		BufferedReader mtpeAlignBr = null;

		try {
			srcBr = new BufferedReader(new FileReader(srcFile));
			mtBr = new BufferedReader(new FileReader(mtFile));
			peBr = new BufferedReader(new FileReader(peFile));
			mtsrcBr = new BufferedReader(new FileReader(mtsrcFile));
			mtpeAlignBr = new BufferedReader(new FileReader(mtpeAlignFile));
			log.info("Loaded all the necessary documents");

		} catch (Exception e) {
			log.error("Failed to load input documents  (see trace below). The program will terminate !!");
			log.error(e);
			System.exit(1);
		}


		int sentID = 0;

		// Begin online automatic post-editing
		log.info("Starting online APE module");
		while (true) {

			long startTime = System.currentTimeMillis();

			sentID += 1;

			log.info("Processing sentence ID = " + sentID);

			String src = null;
			String mt = null;
			String pe = null;
			String mtsrc = null;
			String mtpeAlign = null;

			TopDocs topDocs = null;
			List<List<Document>> docs = null;
			String script = null;
			String lmPath = null;
			String pePath = null;
			String expPath = null;
			String prevExpPath = null;
			boolean validFeatureMap = false;
			Map<String, String> featureMap = null;

			expPath = OnlineAPE.workDir + "/local/" + String.valueOf(sentID);
			log.debug("Experiment directory = " + expPath);

			prevExpPath = OnlineAPE.workDir + "/local/" + String.valueOf(sentID-1);

			// Read  input documents
			log.info("Reading segments from all the input documents");
			try{
				src = srcBr.readLine();
				log.debug("Source sentence = " + src);
				mt = mtBr.readLine();
				log.debug("MT output = " + mt);
				pe = peBr.readLine();
				log.debug("Post-edition = " + pe);
				mtsrc = mtsrcBr.readLine(); // Assume we have it
				log.debug("Joint representation = " + mtsrc);
				mtpeAlign = mtpeAlignBr.readLine(); // Assume we have it
				log.debug("MT-PE alignment = " + mtpeAlign);
			}catch(Exception e){
				log.error("Failed to read input segment ID " + sentID + "  (see trace below). The program will terminate !!");
				log.error(e);
				System.exit(1);
			}			

			if (src == null || mt == null) {
				log.info("Either the source or the MT output (or both) is/are null. The program will stop."); 
				break;
			}

			if(new File(expPath).exists()){
				log.info("Skipping segment ID " + sentID);
				continue;
			}			

			// Built joint representation (Assume we already have it)
			// mtsrc = buildJointRep(mt, src);

			// Extract documents from Lucene index
			log.info("Extracting documents from Lucene index");
			try{
				topDocs = Lucene.queryMTSrcSentence(mtsrc, max);
				log.debug("Total " + topDocs.totalHits + " document/s retrieved");
			}catch(Exception e){
				log.error("Failed to extract documents from lucene index for segment ID " + sentID + "  (see trace below). The program will terminate !!");
				log.error(e);
				System.exit(1);
			}

			// Filter the extracted documents
			log.info("Filtering the list of extracted documents to create training and development set");
			try{
				//docs = Filter.luceneScore(topDocs, threshold);
				//docs = Filter.luceneScore_top(topDocs, threshold, 1);
				docs = Filter.luceneScore(topDocs, threshold, true, 0.2, 5, 5);
				log.debug("Training data size = " + docs.get(0).size());
				log.debug("Development data size = " + docs.get(1).size());
			}catch(Exception e){
				log.error("Failed to filter documents for segment ID " + sentID + "  (see trace below). The program will terminate !!");
				log.error(e);
				System.exit(1);
			}			

			// Set-up experiments
			log.info("Creating experimental set-up");
			try{
				createDir(expPath);
				log.debug("Created directory " + expPath);
				createDataset(src, mt, mtsrc, docs, expPath);
				log.debug("Saved training, development, and test data set in " + expPath);

				if(sentID == 1){
					featureMap = OnlineAPE.defaultFeatureMap;
					log.info("Using the default feature weights for segment ID " + sentID);
					validFeatureMap = true;
				}else{
					try{
						featureMap = loadFeatureMap(prevExpPath + "/tune/moses.ini");

						if(!isValidFeatureMap(featureMap)){
							validFeatureMap = false;
							log.warn("The feature weights from " + prevExpPath + "/tune/moses.ini are not valid. The program will use un-tuned feature weights");
						}else{
							validFeatureMap = true;
							String features = "";
							for(String key : featureMap.keySet()){
								features += "\t" + key + "= " + featureMap.get(key) + "\n";
							}
							log.debug("Feature weights are:\n" + features.trim());
						}
					}catch(IOException ioe){
						validFeatureMap = false;
						log.warn("Warning: Failed to access " + prevExpPath + "/tune/moses.ini for segment ID " + sentID + " (see trace below). The program will use un-tuned feature weights");
						log.warn(ioe);						
					}
				}

				if(!validFeatureMap){
					try{
						featureMap = loadFeatureMap(prevExpPath + "/moses.ini");
						String features = "";
						for(String key : featureMap.keySet()){
							features += "\t" + key + "= " + featureMap.get(key) + "\n";
						}
						log.debug("Feature weights are:\n" + features.trim());	
					}catch(Exception e){
						log.error("Failed to access feature weights from " + prevExpPath + "/moses.ini (see trace below). The program will terminate!!");
						log.error(e);
						System.exit(1);
					}
				}

				createMosesIni(expPath, featureMap);
				log.debug("Created moses.ini");
			}catch(Exception e){
				log.error("Failed to create experimental data set for segment ID " + sentID + "  (see trace below). The program will terminate !!");
				log.error(e);
				System.exit(1);
			}

			// Run APE pipeline
			log.info("Running APE pipeline");
			try{
				script = scriptDir + "/online_ape_pipeline.sh";
				runMoses(script, expPath);
				log.info("APE pipeline finished");
			}catch(Exception e){
				log.error("Failed to run online APE pipeline for segment ID " + sentID + "  (see trace below). The program will terminate !!");
				log.error(e);
				System.exit(1);
			}
			
			if(OnlineAPE.numberOfUpdates > -1 && sentID > OnlineAPE.numberOfUpdates){
				log.info("Sentence ID " + sentID + " took " + (System.currentTimeMillis() - startTime) + " milliseconds \n" );
				continue;
			}else{
				// Add the new entry to Lucene index
				log.info("Adding the new entry to Lucene index");
				try{
					Lucene.add(sentID, src, mt, pe, mtsrc, mtpeAlign);
				}catch(Exception e){
					log.error("Failed to add new entry in lucene index for segment ID " + sentID + "  (see trace below). The program will terminate !!");
					log.error(e);
					System.exit(1);
				}

				// Update global language model
				log.info("Updating the global language model");
				script = scriptDir + "/update_lm.sh";
				lmPath = OnlineAPE.workDir + "/global/lm";
				pePath = lmPath + "/test.pe";
				try {
					save(pe, pePath);
					updateLM(script, lmPath, pePath);
				} catch (Exception e) {
					log.error("Failed to update the global language model for segment ID " + sentID + "  (see trace below). The program will terminate !!");
					log.error(e);
					System.exit(1);
				}

				log.info("Sentence ID " + sentID + " took " + (System.currentTimeMillis() - startTime) + " milliseconds \n" );
			}			
		}		 
	}

	public static void createDir(String dir) throws Exception {
		new File(dir).mkdir();
	}

	public static void createDataset(String src, String mt, String mtsrc, List<List<Document>> docs, String expPath)
			throws Exception {
		BufferedWriter testSrcBw;
		BufferedWriter testMtBw;
		BufferedWriter testMtSrcBw;

		//BufferedWriter trainSrcBw;
		//BufferedWriter trainMtBw;
		BufferedWriter trainMtSrcBw;
		BufferedWriter trainPeBw;
		BufferedWriter trainMtPeAlignBw;

		BufferedWriter devMtSrcBw;
		BufferedWriter devPeBw;

		String dataDir = expPath + "/data";
		createDir(dataDir);

		// Create test files
		testSrcBw = new BufferedWriter(new FileWriter(dataDir + "/test.src"));
		testMtBw = new BufferedWriter(new FileWriter(dataDir + "/test.mt"));
		testMtSrcBw = new BufferedWriter(new FileWriter(dataDir + "/test.mtsrc"));
		testSrcBw.write(src + "\n");
		testMtBw.write(mt + "\n");
		testMtSrcBw.write(mtsrc + "\n");

		// Create training files
		//trainSrcBw = new BufferedWriter(new FileWriter(dataDir + "/train.src"));
		//trainMtBw = new BufferedWriter(new FileWriter(dataDir + "/train.mt"));
		trainMtSrcBw = new BufferedWriter(new FileWriter(dataDir + "/train.mtsrc"));
		trainPeBw = new BufferedWriter(new FileWriter(dataDir + "/train.pe"));
		trainMtPeAlignBw = new BufferedWriter(new FileWriter(dataDir + "/align.mt.pe"));
		for (Document doc : docs.get(0)) {
			//trainSrcBw.write(doc.get("SourceSentence") + "\n");
			//trainMtBw.write(doc.get("MTSentence") + "\n");
			trainMtSrcBw.write(doc.get("MTSrcSentence") + "\n");
			trainPeBw.write(doc.get("PESentence") + "\n");
			trainMtPeAlignBw.write(doc.get("Alignment") + "\n");
		}

		// Create development files
		devMtSrcBw = new BufferedWriter(new FileWriter(dataDir + "/dev.mtsrc"));
		devPeBw = new BufferedWriter(new FileWriter(dataDir + "/dev.pe"));
		
		for (Document doc : docs.get(1)) {			
			devMtSrcBw.write(doc.get("MTSrcSentence") + "\n");
			devPeBw.write(doc.get("PESentence") + "\n");
		}

		testSrcBw.close();
		testMtBw.close();
		testMtSrcBw.close();
		//trainSrcBw.close();
		//trainMtBw.close();
		trainMtSrcBw.close();
		trainPeBw.close();
		trainMtPeAlignBw.close();		
		devMtSrcBw.close();
		devPeBw.close();

	}

	public static void createMosesIni(String expPath, Map<String, String> featureMap) throws Exception {
		String modelPath = expPath + "/train/model";

		BufferedWriter mosesIniBw = new BufferedWriter(new FileWriter(expPath + "/moses.ini"));

		mosesIniBw.write("[input-factors]" + "\n");
		mosesIniBw.write("0" + "\n");
		mosesIniBw.write("" + "\n");
		mosesIniBw.write("[mapping]" + "\n");
		mosesIniBw.write("0 T 0" + "\n");
		mosesIniBw.write("1 T 1" + "\n");
		mosesIniBw.write("" + "\n");
		mosesIniBw.write("[distortion-limit]" + "\n");
		mosesIniBw.write("6" + "\n");
		mosesIniBw.write("" + "\n");
		mosesIniBw.write("[feature]" + "\n");
		mosesIniBw.write("UnknownWordPenalty" + "\n");
		mosesIniBw.write("WordPenalty" + "\n");
		mosesIniBw.write("PhrasePenalty" + "\n");
		mosesIniBw.write("PhraseDictionaryMemory name=TranslationModel0 num-features=4 path=" + modelPath
				+ "/phrase-table.gz input-factor=0 output-factor=0 table-limit=20" + "\n");
		mosesIniBw.write("PhraseDictionaryMemory name=TranslationModel1 num-features=4 path=" + modelPath
				+ "/phrase-table-backoff.gz input-factor=0 output-factor=0 table-limit=20" + "\n");
		mosesIniBw
		.write("LexicalReordering name=LexicalReordering0 num-features=6 type=wbe-msd-bidirectional-fe-allff input-factor=0 output-factor=0 path="
				+ modelPath + "/reordering-table.wbe-msd-bidirectional-fe.gz" + "\n");
		mosesIniBw.write("Distortion" + "\n");
		mosesIniBw.write("IRSTLM name=LM0 factor=0 path=" + OnlineAPE.workDir + "/global/lm/global.blm order=3" + "\n");
		if(OnlineAPE.defaultFeatureMap.containsKey("LM1")){
			mosesIniBw.write("IRSTLM name=LM1 factor=0 path=" + expPath + "/lm/local.blm order=3" + "\n");	
		}		
		mosesIniBw.write("" + "\n");
		mosesIniBw.write("[weight]" + "\n");

		for (String key : featureMap.keySet()) {
			mosesIniBw.write(key + "= " + featureMap.get(key) + "\n");
		}

		mosesIniBw.write("" + "\n");
		mosesIniBw.write("[decoding-graph-backoff]" + "\n");
		mosesIniBw.write("0" + "\n");
		mosesIniBw.write("1" + "\n");

		mosesIniBw.close();
	}

	public static void updateFeatureMaps(int sentID, Map<String, String> featureMap) {
		/*if (featureMap == null)
			featureMap = featureMaps.get(sentID - 1);
		featureMaps.put(sentID, featureMap);*/
	}

	public static void runMoses(String script, String expPath) throws Exception {
		String command;
		Process p;

		command = script + " --data_dir " + expPath + "/data " + "--work_dir " + expPath;
		log.debug("APE pipeline command is " + command);

		p = Runtime.getRuntime().exec(command);
		p.waitFor();
	}

	public static void initLuceneParams() throws Exception {
		String indexFolder = OnlineAPE.workDir + "/global/lucene";

		OnlineAPE.analyzer = new WhitespaceAnalyzer();

		Directory dir = FSDirectory.open(java.nio.file.Paths.get(indexFolder));
		IndexWriterConfig iwc = new IndexWriterConfig(OnlineAPE.analyzer);

		IndexWriterConfig.OpenMode mode = OpenMode.CREATE_OR_APPEND; 
		iwc.setOpenMode(mode);
		log.debug("IndexWriterConfig openMode is set to " + mode.name());
		OnlineAPE.index = new IndexWriter(dir, iwc);
		log.debug("Initialized indexWriter");

		OnlineAPE.searcherManager = new SearcherManager(index, true, null);
		log.debug("Initialized searcherManager");
	}

	public static void initConfigParams(String config) throws Exception {
		Properties prop = new Properties();
		prop.load(new FileInputStream(config));

		OnlineAPE.srcFile = prop.getProperty("srcFile");
		OnlineAPE.mtFile = prop.getProperty("mtFile");
		OnlineAPE.peFile = prop.getProperty("peFile");
		OnlineAPE.mtsrcFile = prop.getProperty("mtsrcFile");
		OnlineAPE.mtpeAlignFile = prop.getProperty("mtpeAlignFile");
		OnlineAPE.max = Integer.valueOf(prop.getProperty("max"));
		OnlineAPE.threshold = Float.valueOf(prop.getProperty("threshold"));
		OnlineAPE.workDir = prop.getProperty("workDir");
		OnlineAPE.scriptDir = prop.getProperty("scriptDir");
		OnlineAPE.loadPrevious = Integer.valueOf(prop.getProperty("loadPrevious"));
		OnlineAPE.numberOfUpdates = Integer.valueOf(prop.getProperty("numberOfUpdates"));
	}

	public static void updateLM(String script, String lmPath, String pePath) throws Exception {
		String command;
		Process p;

		command = script + " " + lmPath + " " + pePath;
		log.debug("Global language model update command is " + command);

		p = Runtime.getRuntime().exec(command);
		p.waitFor();
	}

	public static void save(String data, String filePath) throws Exception {
		BufferedWriter bw = new BufferedWriter(new FileWriter(filePath));
		bw.write(data + "\n");
		bw.close();
	}

	public static Map<String, String> loadFeatureMap(String mosesIni) throws Exception {
		Set<String> features = defaultFeatureMap.keySet();		

		BufferedReader br = new BufferedReader(new FileReader(mosesIni));

		String line;
		Map<String, String> featureMap = new HashMap<String, String>();

		while ((line = br.readLine()) != null) {
			line = line.trim();
			String[] toks = line.split(" ", 2);
			if (toks.length > 1) {
				String feature = toks[0].replace("=", "");
				String value = toks[1];
				if (features.contains(feature)) {
					featureMap.put(feature, value);
				}
			}
		}
		br.close();
		return featureMap;
	}

	public static void displayFeatureMaps(Map<Integer, Map<String, String>> featureMaps){
		log.debug("Printing all the saved feature weights in feature maps");
		for(int key : featureMaps.keySet()){
			log.debug("Key = " + key);
			Map<String, String> featureMap = featureMaps.get(key);
			for(String k : featureMap.keySet()){
				log.debug(k + " = " + featureMap.get(k));	
			}

		}
	}

	public static void initDefaultFeatureMap(){
		OnlineAPE.defaultFeatureMap.put("LexicalReordering0", "0.3 0.3 0.3 0.3 0.3 0.3");
		OnlineAPE.defaultFeatureMap.put("Distortion0", "0.3");
		OnlineAPE.defaultFeatureMap.put("LM0", "0.5");
		OnlineAPE.defaultFeatureMap.put("LM1", "0.5");
		OnlineAPE.defaultFeatureMap.put("WordPenalty0", "-1");
		OnlineAPE.defaultFeatureMap.put("PhrasePenalty0", "0.2");
		OnlineAPE.defaultFeatureMap.put("TranslationModel0", "0.2 0.2 0.2 0.2");
		OnlineAPE.defaultFeatureMap.put("TranslationModel1", "0.2 0.2 0.2 0.2");
		OnlineAPE.defaultFeatureMap.put("UnknownWordPenalty0", "1");
	}

	public static boolean isValidFeatureMap(Map<String, String> featureMap){
		boolean valid = true;
		String key = null;

		if(featureMap == null){
			valid = false;
			OnlineAPE.log.warn("Feature map is null");
		}else if (featureMap.keySet().size() != 9){
			valid = false;
			OnlineAPE.log.warn("Feature map key set size is " + featureMap.keySet().size() + " not equal to 9");
		} else if((key = containsWeight(featureMap, "0")) != null){
			valid = false;
			OnlineAPE.log.warn("Feature map contains a feature (" + key + ") with weight 0");
		}
		return valid;
	}

	public static String containsWeight(Map<String, String> featureMap, String weight){		

		for(String key : featureMap.keySet()){
			for(String val : featureMap.get(key).split(" ")){
				if(val.equalsIgnoreCase(weight)){
					return key;
				}
			}
		}
		return null;
	}
}
