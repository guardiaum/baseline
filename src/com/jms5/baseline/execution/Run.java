package com.jms5.baseline.execution;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.jms5.baseline.beans.Article;
import com.jms5.baseline.beans.SentenceTrainingExample;
import com.jms5.baseline.data.DatasetsUtil;
import com.jms5.baseline.modules.ClassifierMod;
import com.jms5.baseline.modules.Preprocessor;
import com.jms5.baseline.util.Constants;

import cc.mallet.classify.BaggingClassifier;
import cc.mallet.classify.Classification;
import cc.mallet.types.Instance;
import cc.mallet.types.Label;

public class Run {
	
	private static DatasetsUtil data;
	private static ClassifierMod classifier;
	private static Preprocessor preprocessor;
	
	public static void main(String[] args) {
		
		data = new DatasetsUtil();
		classifier = new ClassifierMod();
		preprocessor = new Preprocessor();
		
		try {

			// Preprocessor execution
			// returns training examples
			// structure: attribute, sentence, label
			// List<String[]> trainingExamples = runPreprocessor(args);

			// Classifier execution
			// Classifing documents using heuristics
			// List<Article> articlesMember = runDocumentClassifier(args);

			// Classifing sentences Mallet's Maximum Entropy Model
			List<SentenceTrainingExample> trainingExamples = data.getTrainingExamples(Paths.get(Constants.TRAINING_DATASET));

			Map<String, BaggingClassifier> sentencesClassifier = runSentenceClassifier(trainingExamples);

			System.out.println("Returned Classifiers: " + sentencesClassifier.size());
			// Extraction
			
			List<SentenceTrainingExample> relabeledTrainingExamples = classifier.relabel(trainingExamples, sentencesClassifier);

			System.out.println("FINISHED!");

		} catch (IOException e) {
			e.printStackTrace();
		}
		/*
		 * catch (InterruptedException e) { e.printStackTrace(); } catch
		 * (ExecutionException e) { e.printStackTrace(); }
		 */

	}
	

	private static Map<String, BaggingClassifier> runSentenceClassifier(
			List<SentenceTrainingExample> trainingExamples) throws IOException {
		
		return classifier.sentenceClassifier(trainingExamples);
	}
	
	private static List<Article> runDocumentClassifier(String[] args)
			throws IOException, InterruptedException, ExecutionException {

		// Classifing Documents (Articles)
		String[] keywords = args[0].replace("infobox ", "").split(" ");

		//System.out.println("KEYWORDS: " + Arrays.toString(keywords));

		List<Article> labeledArticles = classifier.documentClassifier(keywords);

		List<Article> articlesMember = labeledArticles.stream().filter(article -> article.isMember() == true)
				.collect(Collectors.toList());

		//System.out.println("Articles not member: #" + (labeledArticles.size() - articlesMember.size()));
		//System.out.println("Articles member: #" + articlesMember.size());

		return articlesMember;
	}

	private static List<String[]> runPreprocessor(String[] args)
			throws IOException, InterruptedException, ExecutionException, FileNotFoundException {

		List<Article> articles = preprocessor.selectArticles(args[0]);

		System.out.println("Found Articles: #" + articles.size());

		// Selecting attributes
		Map<String, Double> selectedAttributes = preprocessor.selectAttributes(articles, 0.15);

		// Print selected attributes and respective frequency
		for (Map.Entry<String, Double> attribute : selectedAttributes.entrySet()) {
			System.out.println(attribute.getKey() + " -> " + attribute.getValue());
		}

		List<String> listSelectedAttributes = new ArrayList<String>(selectedAttributes.keySet());

		// Segments document into sentences and constructs training examples
		// returns a list of training examples
		// structure: attribute-name, sentence, label
		List<String[]> trainingExamples = preprocessor.documentSegmentation(articles, listSelectedAttributes);

		// Save training set to file
		data.constructsTrainingDataset(trainingExamples, listSelectedAttributes);

		return trainingExamples;
	}
	
}
