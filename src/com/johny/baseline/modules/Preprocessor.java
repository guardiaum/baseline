package com.johny.baseline.modules;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import com.johny.baseline.beans.Article;
import com.johny.baseline.beans.InfoboxTuple;
import com.johny.baseline.util.StaXParserCallable;
import com.johny.baseline.util.Util;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

public class Preprocessor {
	
	public static final String WIKIPEDIA_CHUNKS = "/home/jms5/kobe/infoboxes-extractor/wikipedia-dump/wikipedia-xml-chunks";
	public static final String TRAINING_DATASET = "/home/jms5/baseline/training-dataset.csv";
	public static final String SENTENCE_DETECTOR = "/home/jms5/baseline/open-nlp-models/en-sent.bin";
	public static final int THREADS_NUMBER = 8;
	
	/**
	 * Scans the Wikipedia corpus and selects all articles containing the exact name
	 * of the given infobox template name.
	 * 
	 * @param templateName
	 *            String
	 * @return articles List<Article>
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws ExecutionException 
	 */
	public List<Article> selectArticles(String templateName) 
			throws IOException, InterruptedException, ExecutionException {
		
		System.out.println("-> SEARCHING FOR PAGES USING " + templateName.toUpperCase() + " INFOBOX");
		
		ExecutorService executor = Executors.newFixedThreadPool(THREADS_NUMBER);

		// get references to all chunk files
		List<Path> files = Files.walk(Paths.get(WIKIPEDIA_CHUNKS)).filter(Files::isRegularFile)
				.collect(Collectors.toList());

		List<Callable<List<Article>>> dumpTasks = new ArrayList<Callable<List<Article>>>();

		for (Path file : files) {
			dumpTasks.add(new StaXParserCallable(file, templateName));
		}
		
		System.out.println("START EXTRACTING DATA FROM DUMP...");
		
		List<Future<List<Article>>> listFutures = executor.invokeAll(dumpTasks);

		List<Article> articles = new ArrayList<Article>();
		
		for (Future<List<Article>> future : listFutures) {
			
			if(future.isDone() && future != null) {
				
				List<Article> returnedArticles = future.get();
				
				if(returnedArticles.size() > 0)
					articles.addAll(returnedArticles);
				
			}
			
		}
		
		// closes ExecutorService
		executor.shutdown();
		
		return articles;
	}
	
	/**
	 * Catalogs all attributes mentioned and selects the
	 * most common. Our current implementation restricts attention to
	 * attributes used in at least 15% of the articles
	 * 
	 * @param articles
	 * @param frequencyThreshold
	 * @return
	 */
	public Map<String, Double> selectAttributes(List<Article> articles, double frequencyThreshold){
		
		System.out.println("SELECTING ATTRIBUTES");
		
		Map<String, Integer> attributesFrequency = new HashMap<String, Integer>();
		
		for (Article article : articles) {
			
			if(article.getInfobox() != null) {
				
				for (InfoboxTuple tuple : article.getInfobox().getTuples()) {
					
					if(attributesFrequency.containsKey(tuple.getProperty())) {
						Integer currentCount = attributesFrequency.get(tuple.getProperty());
						attributesFrequency.replace(tuple.getProperty(), currentCount, currentCount + 1);
					}else {
						attributesFrequency.put(tuple.getProperty(), 1);
					}
				}
			}
		}
		
		Map<String, Double> attributesByFrequency = 
				Util.calculateFrequency(attributesFrequency, (double) articles.size());
		
		Map<String, Double> selectedAttributes =  attributesByFrequency.entrySet().stream()
				.filter(map -> map.getValue() >= frequencyThreshold)
				.collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));
		
		selectedAttributes = Util.sortCategoriesDesc(selectedAttributes);
		
		return selectedAttributes;
		
	}
	
	/**
	 * Constructs training datasets for use when 
	 * learning classifiers and extractors
	 */
	public void constructsTrainingDataset(List<String[]> trainingExamples) {
		System.out.println("CONSTRUCTING TRAINING DATASET");
		System.out.println("TRAINING EXAMPLES SIZE: " + trainingExamples.size());
		
		Path path = Paths.get(TRAINING_DATASET);
		try (BufferedWriter writer = Files.newBufferedWriter(path)){
			for (String[] trainingExample : trainingExamples) {
				String line = trainingExample[0] + "," 
							+ trainingExample[1] + "," 
							+ trainingExample[2] + "\n";
				writer.write(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	/***
	 * Iterates through the articles. 
	 * For each article with an infobox mentioning one 
	 * or more target attributes, KYLIN segments the document into 
	 * sentences, using the OpenNLP library [1]. 
	 * 
	 * @param articles
	 * @param selectedAttributes
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public List<String[]> documentSegmentation(List<Article> articles, List<String> selectedAttributes) throws FileNotFoundException, IOException {
		
		System.out.println("DOCUMENT SEGMENTATION");
		
		List<String[]> trainingExamples = new ArrayList<String[]>();
		
		for (Article article : articles) {
			
			if(article.getText() != null) {
				
				String text = article.getText();
				String[] sentences = null;
				
				try (InputStream modelIn = new FileInputStream(SENTENCE_DETECTOR)){
					SentenceModel model = new SentenceModel(modelIn);
					
					SentenceDetectorME sentenceDetector = new SentenceDetectorME(model);
					
					sentences = sentenceDetector.sentDetect(text);
				}
				
				List<String[]> labeledMatches = matchAttributesToSentences(article, sentences, selectedAttributes);
				trainingExamples.addAll(labeledMatches);
			}
		}
		
		return trainingExamples;
	}
	
	/***
	 * For each target attribute, KYLIN tries to find a unique, corresponding sentence in the article. 
	 * The resulting labelled sentences form positive training examples for each attribute. 
	 * Other sentences form negative training examples
	 * 
	 * @param article
	 * @param sentences
	 * @return
	 */
	private List<String[]> matchAttributesToSentences(Article article,
			String[] sentences, List<String> selectedAttributes) {
		
		List<String[]> labeledMatches = new ArrayList<String[]>();
		
		if(sentences != null) {
			
			if(article.getInfobox() != null) {
				
				for (InfoboxTuple tuple : article.getInfobox().getTuples()) {
					
					if(selectedAttributes.contains(tuple.getProperty())) {
						
						List<String[]> trainingExamples = findMatchings(tuple, sentences);
						
						if(trainingExamples != null && 
								trainingExamples.size() > 0)
							labeledMatches.addAll(trainingExamples);
					}
				}
			}			
		}
		
		return labeledMatches;
	}

	/***
	 * For each target attribute, KYLIN tries to find a unique, corresponding sentence in the article. 
	 * If the attribute value is mentioned by exactly one sentence
	 * in the article, use that sentence and the matching token as a 
	 * training example
	 * 
	 * @param tuple
	 * @param sentences
	 * @return
	 */
	private List<String[]> findMatchings(InfoboxTuple tuple, String[] sentences) {
		
		List<String> allMatches = new ArrayList<String>();
		
		for (String sentence : sentences) {
			
			if(sentence.contains(tuple.getValue()))
				allMatches.add(sentence);
		
		}
		
		List<String[]> trainingExamples = new ArrayList<String[]>();
		
		if(allMatches.size() == 1)
			trainingExamples.add(new String[]{tuple.getProperty(), allMatches.get(0), "t"});
		else if(allMatches.size() > 1)
			return labelSentences(tuple, allMatches);
		
		return trainingExamples;
	}

	/**
	 * If the value is mentioned by several sentences, 
	 * KYLIN determines what percentage of the tokens in 
	 * the attribute’s name are in each sentence. 
	 * If the sentence matching the highest 
	 * percentage of tokens has at least 60% of these keywords,
	 * then it is selected as a positive training example
	 * 
	 * @param tuple
	 * @param allMatches
	 * @return
	 */
	private List<String[]> labelSentences(InfoboxTuple tuple, List<String> allMatches) {

		List<String> propTokens = Arrays.asList(tuple.getProperty().replaceAll("_", " ").split(" "));
		List<String> valueTokens = Arrays.asList(tuple.getValue().replaceAll("_", " ").split(" "));
		
		List<String> tokens = new ArrayList<String>();
		tokens.addAll(propTokens);
		tokens.addAll(valueTokens);
		
		List<String[]> trainingExamples = new ArrayList<String[]>();
		
		for (String sentenceMatch : allMatches) {
			int count = 0;
			
			for (String token : tokens) {
				
				if(sentenceMatch.contains(token))
					count++;
			
			}
			
			double matchPercentage = count / (double) tokens.size();
			
			if(matchPercentage >= 0.6)
				trainingExamples.add(new String[] {tuple.getProperty(), sentenceMatch, "t"});
			else
				trainingExamples.add(new String[] {tuple.getProperty(), sentenceMatch, "f"});
			
		}
		
		return trainingExamples;
	}

}
