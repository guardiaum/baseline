package com.johny.baseline.execution;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.johny.baseline.beans.Article;
import com.johny.baseline.modules.Classifier;
import com.johny.baseline.modules.Preprocessor;
import com.johny.baseline.util.CSVUtil;
import com.johny.baseline.util.Constants;
import com.johny.baseline.util.POSTagger;

import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.classify.MaxEnt;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.pipe.FeatureSequence2FeatureVector;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.PrintInputAndTarget;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.SimpleTaggerSentence2TokenSequence;
import cc.mallet.pipe.Target2Label;
import cc.mallet.pipe.Target2LabelSequence;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequence2FeatureVectorSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;

public class Run {

	public static void main(String[] args) {

		try {

			// Preprocessor execution
			// returns training examples
			// structure: attribute, sentence, label
			List<String[]> trainingExamples = runPreprocessor(args);

			// Classifier execution
			// Classifing documents using heuristics
			List<Article> articlesMember = runDocumentClassifier(args);

			// Classifing sentences Mallet's Maximum Entropy Model
			List<ClassifierTrainer<MaxEnt>> sentencesClassifier = runSentenceClassifier();

			System.out.println("Returned Classifiers: " + sentencesClassifier.size());
			// Extraction

			System.out.println("FINISHED!");

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

	}

	private static List<ClassifierTrainer<MaxEnt>> runSentenceClassifier() throws IOException {

		List<ClassifierTrainer<MaxEnt>> classifiers = new ArrayList<ClassifierTrainer<MaxEnt>>();

		Pipe pipe = buildPipe();

		List<Path> datasets = Files.walk(Paths.get(Constants.DATASETS_DIR)).filter(Files::isRegularFile)
				.collect(Collectors.toList());

		for (Path datasetPath : datasets) {

			InstanceList instanceList = new InstanceList(pipe);

			List<String[]> trainingExamples = getTrainingExamples(datasetPath);

			for (String[] example : trainingExamples) {

				String name = example[0];

				String target = example[1];

				String data = example[2];
				
				Instance inst = new Instance(data, target, name, null);

				instanceList.addThruPipe(inst);
			}

			ClassifierTrainer<MaxEnt> maxEnt = new MaxEntTrainer();

			maxEnt.train(instanceList);

			classifiers.add(maxEnt);
		}

		return classifiers;
	}

	/**
	 * Converts passed dataset to a list of string arrays adding as a training
	 * example only instances labeled as "t"
	 * 
	 * @param datasetPath
	 * @return
	 * @throws IOException
	 */
	private static List<String[]> getTrainingExamples(Path datasetPath) throws IOException {

		List<String[]> trainingExamples = new ArrayList<String[]>();

		BufferedReader reader = Files.newBufferedReader(datasetPath);

		String line = reader.readLine();
		while (line != null) {
			List<String> elements = CSVUtil.readLine(line);

			if (elements.get(3).equals("t"))
				trainingExamples.add(new String[] { elements.get(0), elements.get(1), elements.get(2) });

			line = reader.readLine();
		}

		return trainingExamples;
	}

	private static Pipe buildPipe() {
		ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

		pipeList.add(new POSTagger());
		pipeList.add(new FeatureSequence2FeatureVector());
		
		/* um ou outro */
		pipeList.add(new Target2Label());
		//pipeList.add(new Target2LabelSequence());
		
		pipeList.add(new PrintInputAndTarget());

		return new SerialPipes(pipeList);
	}

	private static List<Article> runDocumentClassifier(String[] args)
			throws IOException, InterruptedException, ExecutionException {

		Classifier c = new Classifier();

		// Classifing Documents (Articles)
		String[] keywords = args[0].replace("infobox ", "").split(" ");

		System.out.println("KEYWORDS: " + Arrays.toString(keywords));

		List<Article> labeledArticles = c.documentClassifier(keywords);

		List<Article> articlesMember = labeledArticles.stream().filter(article -> article.isMember() == true)
				.collect(Collectors.toList());

		System.out.println("Articles not member: #" + (labeledArticles.size() - articlesMember.size()));
		System.out.println("Articles member: #" + articlesMember.size());

		return articlesMember;
	}

	private static List<String[]> runPreprocessor(String[] args)
			throws IOException, InterruptedException, ExecutionException, FileNotFoundException {

		Preprocessor p = new Preprocessor();

		List<Article> articles = p.selectArticles(args[0]);

		System.out.println("Found Articles: #" + articles.size());

		// Selecting attributes
		Map<String, Double> selectedAttributes = p.selectAttributes(articles, 0.15);

		// Print selected attributes and respective frequency
		for (Map.Entry<String, Double> attribute : selectedAttributes.entrySet()) {
			System.out.println(attribute.getKey() + " -> " + attribute.getValue());
		}

		List<String> listSelectedAttributes = new ArrayList<String>(selectedAttributes.keySet());

		// Segments document into sentences and constructs training examples
		// returns a list of training examples
		// structure: attribute-name, sentence, label
		List<String[]> trainingExamples = p.documentSegmentation(articles, listSelectedAttributes);

		// Save training set to file
		p.constructsTrainingDatasets(trainingExamples, listSelectedAttributes);

		return trainingExamples;
	}

}
