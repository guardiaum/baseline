package com.jms5.baseline.modules;

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

import com.jms5.baseline.beans.Article;
import com.jms5.baseline.beans.SentenceTrainingExample;
import com.jms5.baseline.parsers.KeywordsStaXParserCallable;
import com.jms5.baseline.util.Constants;
import com.jms5.baseline.util.POSTagger;

import cc.mallet.classify.BaggingClassifier;
import cc.mallet.classify.BaggingTrainer;
import cc.mallet.classify.Classification;
import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.classify.MaxEnt;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.pipe.FeatureSequence2FeatureVector;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.SimpleTaggerSentence2TokenSequence;
import cc.mallet.pipe.Target2Label;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Label;

public class ClassifierMod {
	
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
	
	public Map<String, BaggingClassifier> sentenceClassifier(
			List<SentenceTrainingExample> trainingExamples) throws IOException {

		Map<String, BaggingClassifier> classifiers = new HashMap<String, BaggingClassifier>();
		
		// split training examples according with attribute name
		Map<String, List<SentenceTrainingExample>> attributesTrainingExamples = trainingExamples.stream()
				.collect(Collectors.groupingBy(SentenceTrainingExample::getAttributeName));

		for (Map.Entry<String, List<SentenceTrainingExample>> attributeTrainingSet : attributesTrainingExamples
				.entrySet()) {

			System.out.println(">>> Training classifier for " + attributeTrainingSet.getKey() + " attribute.");

			BaggingClassifier classifier = trainClassifier4Attribute(attributeTrainingSet);

			classifiers.put(attributeTrainingSet.getKey(), classifier);

		}

		return classifiers;
	}
	
	private BaggingClassifier trainClassifier4Attribute(
			Map.Entry<String, List<SentenceTrainingExample>> attributeTrainingSet) {
		
		InstanceList instanceList = generateInstanceList(attributeTrainingSet);
		
		BaggingTrainer trainer = new BaggingTrainer(new ClassifierTrainer.Factory<ClassifierTrainer<MaxEnt>>() {

			@Override
			public ClassifierTrainer<MaxEnt> newClassifierTrainer(cc.mallet.classify.Classifier c) {
				return new MaxEntTrainer();
			}
		});

		BaggingClassifier classifier = trainer.train(instanceList);

		return classifier;
	}
	
	/***
	 * Generates a InstanceList of training examples.
	 * 
	 * data = sentence
	 * target = attribute value to be predicted
	 * name = attribute name
	 * 
	 * @param attributeTrainingSet
	 * @return
	 */
	private InstanceList generateInstanceList(
			Map.Entry<String, List<SentenceTrainingExample>> attributeTrainingSet) {
		
		Pipe pipe = buildPipe();

		InstanceList instanceList = new InstanceList(pipe);

		for (SentenceTrainingExample example : attributeTrainingSet.getValue()) {

			if (example.getSentence() != null & example.getAttributeValue() != null
					& example.getAttributeName() != null & example.getLabel().equals("t")) {

				Instance inst = new Instance(example.getSentence(), example.getAttributeValue(),
						example.getAttributeName(), null);

				instanceList.addThruPipe(inst);

			}
		}
		return instanceList;
	}
	
	/***
	 * Construct pipe for tokenization, pos tagging
	 * and feature sequence of instance list
	 * 
	 * @return
	 */
	private Pipe buildPipe() {
		ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

		pipeList.add(new POSTagger());
		pipeList.add(new SimpleTaggerSentence2TokenSequence());
		pipeList.add(new TokenSequence2FeatureSequence());
		pipeList.add(new FeatureSequence2FeatureVector());

		/* um ou outro */
		pipeList.add(new Target2Label());
		// pipeList.add(new Target2LabelSequence());

		// pipeList.add(new PrintInputAndTarget());

		return new SerialPipes(pipeList);
	}
	
	public List<SentenceTrainingExample> relabel(List<SentenceTrainingExample> trainingExamples,
			Map<String, BaggingClassifier> sentencesClassifiers) {
		
		Pipe pipe = buildPipe();

		for (SentenceTrainingExample trainingExample : trainingExamples) {
			
			if(trainingExample.getLabel().equals("f")) {
			
				BaggingClassifier attributeClassifier = 
						sentencesClassifiers.get(trainingExample.getAttributeName());
				
				Instance inst = new Instance(trainingExample.getSentence(), 
						trainingExample.getAttributeValue(), trainingExample.getAttributeName(), null);
				
				Classification c = attributeClassifier.classify(pipe.instanceFrom(inst));
				
				Label l = c.getLabeling().getBestLabel();
				
				System.out.println("CURRENT");
				System.out.println(trainingExample.toString());
				System.out.println(">>> PREDICTION: " + l);
			}
		}
		
		return null;
	}
}
