package com.johny.baseline.modules;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import com.johny.baseline.beans.Article;
import com.johny.baseline.parsers.KeywordsStaXParserCallable;
import com.johny.baseline.util.Constants;

public class Classifier {
	
	public List<Article> documentClassifier(String[] keywords) 
			throws IOException, InterruptedException, ExecutionException {
		
		System.out.println("START DOCUMENT CLASSIFIER!!");
		
		ExecutorService executor = Executors.newFixedThreadPool(Constants.THREADS_NUMBER);
		
		// get references to all chunk files
		List<Path> files = Files.walk(Paths.get(Constants.WIKIPEDIA_CHUNKS)).filter(Files::isRegularFile)
				.collect(Collectors.toList());
		
		List<Callable<List<Article>>> dumpTasks = new ArrayList<Callable<List<Article>>>();
		
		for (Path file : files) {
			dumpTasks.add(new KeywordsStaXParserCallable(file, keywords));
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
}
