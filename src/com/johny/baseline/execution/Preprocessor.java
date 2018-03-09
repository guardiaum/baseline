package com.johny.baseline.execution;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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

public class Preprocessor {
	
	public static final String WIKIPEDIA_CHUNKS = "/home/jms5/kobe/infoboxes-extractor/wikipedia-dump/wikipedia-xml-chunks";
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
		
		Map<String, Integer> attributesFrequency = new HashMap<String, Integer>();
		
		for (Article article : articles) {
			
			for (InfoboxTuple tuple : article.getInfobox().getTuples()) {
				
				if(attributesFrequency.containsKey(tuple.getProperty())) {
					Integer currentCount = attributesFrequency.get(tuple.getProperty());
					attributesFrequency.replace(tuple.getProperty(), currentCount, currentCount + 1);
				}else {
					attributesFrequency.put(tuple.getProperty(), 1);
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

}
