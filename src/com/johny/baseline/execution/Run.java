package com.johny.baseline.execution;

import java.io.FileNotFoundException;
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

import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.classify.MaxEnt;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.fst.SimpleTagger;
import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.FeatureSequence2FeatureVector;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.PrintInputAndTarget;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.Target2Label;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.tsf.TokenText;
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
			ClassifierTrainer sentencesClassifier = runSentenceClassifier(trainingExamples);
			
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

	private static ClassifierTrainer<MaxEnt> runSentenceClassifier(List<String[]> trainingExamples) 
			throws IOException {
		
		// bag of words
		// part of speech

		/* TOKENIZE AND POSTAG (WITH MALLET??) */
		Pipe pipe = buildPipe();
		InstanceList instanceList = new InstanceList(pipe);
		
		for (String[] example : trainingExamples) {
			// sequence: sentence, attribute, attribute
			Instance instance = new Instance(example[1], example[0], example[0], null);
			
			// Now process each instance provided by the iterator.
			instanceList.add(instance);
		}
		
		ClassifierTrainer<MaxEnt> maxEnt = new MaxEntTrainer();
		maxEnt.train(instanceList);
		
		/* TOKENIZE AND POSTAG WITH OPEN NLP
		
		List<String[]> taggedSentences = new ArrayList<String[]>(); 
		
		InputStream input = new FileInputStream(Constants.POSTAGGER_DETECTOR);
		POSModel model = new POSModel(input);
		POSTaggerME tagger = new POSTaggerME(model);
		
		WhitespaceTokenizer tokenizer = WhitespaceTokenizer.INSTANCE;
		
		for (String[] trainExample : trainingExamples) {
			
			String[] tokens = tokenizer.tokenize(trainExample[1]);
			
			String[] tags = tagger.tag(tokens);
			
			taggedSentences.add(new String[]{trainExample[0], 
					Arrays.toString(tokens), Arrays.toString(tags), trainExample[2]});
		}*/
		
		return maxEnt;
	}

	private static Pipe buildPipe() {
		ArrayList<Pipe> pipeList = new ArrayList<Pipe>();
		
		//pipeList.add(new CharSequence2TokenSequence());
		pipeList.add(new SimpleTagger.SimpleTaggerSentence2FeatureVectorSequence());
		/*pipeList.add(new TokenSequence2FeatureSequence());
		pipeList.add(new Target2Label());
		pipeList.add(new FeatureSequence2FeatureVector());*/
        pipeList.add(new PrintInputAndTarget());
        
		return new SerialPipes(pipeList);
	}

	private static List<Article> runDocumentClassifier(String[] args)
			throws IOException, InterruptedException, ExecutionException {
		
		Classifier c = new Classifier();
		
		// Classifing Documents (Articles)
		String[] keywords = args[0].replace("infobox ", "").split(" ");
		
		System.out.println("KEYWORDS: "+ Arrays.toString(keywords));
		
		List<Article> labeledArticles = c.documentClassifier(keywords);
		
		List<Article> articlesMember = labeledArticles
				.stream().filter(article -> article.isMember()==true)
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
		p.constructsTrainingDataset(trainingExamples);
		
		return trainingExamples;
	}

}
