package com.johny.baseline.util;

public class Constants {
	
	// concurrency
	public static final int THREADS_NUMBER = 8;
	
	// dump xml nodes
	public static final String PAGE = "page";
	public static final String TITLE = "title";
	public static final String REDIRECT = "redirect";
	public static final String REVISION = "revision";
	public static final String TEXT = "text";
	
	// regex for text extraction/cleaning
	public static final String REGEX_CATEGORY = "\\[\\[category:.*\\]\\]";
	public static final String REGEX_INFOBOX_MAPPING = "\\{\\{\\s?infobox.*\\n(|.*\\n)*\\}\\}";
	public static final String REGEX_INFOBOX_TEMPLATE = "infobox(\\s\\w*){1,2}";
	
	// path references
	public static final String USER_DIR = System.getProperty("user.home");
	public static final String PROJECT_DIR = System.getProperty("user.dir");
	public static final String WIKIPEDIA_CHUNKS = USER_DIR + "/workspace/kobe/infoboxes-extractor/wikipedia-dump/wikipedia-xml-chunks";
	public static final String TRAINING_DATASET = PROJECT_DIR + "/dataset/training-set.csv";
	
	// OpenNLP models
	public static final String SENTENCE_DETECTOR = PROJECT_DIR + "/open-nlp-models/en-sent.bin";
	public static final String POSTAGGER_DETECTOR = PROJECT_DIR + "/open-nlp-models/en-pos-maxent.bin";
	
}
