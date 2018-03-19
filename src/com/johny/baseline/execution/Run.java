package com.johny.baseline.execution;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.johny.baseline.beans.Article;
import com.johny.baseline.modules.Classifier;
import com.johny.baseline.modules.Preprocessor;

public class Run {

	public static void main(String[] args) {
		
		Preprocessor p = new Preprocessor();
		Classifier c = new Classifier();
		
		try {
			/*
			List<Article> articles = p.selectArticles(args[0]);
			
			System.out.println("Found Articles: #" + articles.size());
			
			Map<String, Double> selectedAttributes = p.selectAttributes(articles, 0.15);
			
			for (Map.Entry<String, Double> attribute : selectedAttributes.entrySet()) {
				System.out.println(attribute.getKey() + " -> " + attribute.getValue());
			}
			
			List<String> listSelectedAttributes = new ArrayList<String>(selectedAttributes.keySet());
			
			List<String[]> trainingExamples = p.documentSegmentation(articles, listSelectedAttributes);
			
			p.constructsTrainingDataset(trainingExamples);
			
			*/
			
			String[] keywords = args[0].replace("infobox", "").split(" ");
			
			System.out.println("KEYWORDS: "+ Arrays.toString(keywords));
			
			List<Article> labeledArticles = c.documentClassifier(keywords);
			
			List<Article> articlesMember = labeledArticles
					.stream().filter(article -> article.isMember()==true)
					.collect(Collectors.toList());
			
			System.out.println("Articles not member: #" + (labeledArticles.size() - articlesMember.size()));
			System.out.println("Articles member: #" + articlesMember.size());
			
			System.out.println("FINISHED!");
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		
	}

}
