package com.johny.baseline.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.WhitespaceTokenizer;

/**
 * 
 * @author jms5
 *
 */
public class POSTagger extends Pipe{
	
	private POSTaggerME tagger;
	
	/*
	 * TOKENIZE AND POSTAG WITH OPEN NLP
	 */
	public POSTagger() {
		
		InputStream input;
		
		try {
			
			input = new FileInputStream(Constants.POSTAGGER_DETECTOR);
			POSModel model = new POSModel(input);
			POSTaggerME tagger = new POSTaggerME(model);
		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	@Override
	public Instance pipe(Instance inst) {
		
		String newData = "";
		
		Object data = inst.getData();
		Object target = inst.getTarget();
		
		System.out.println(">>> DATA: " + data);
		
		if(data instanceof String) {
		
			WhitespaceTokenizer tokenizer = WhitespaceTokenizer.INSTANCE;
	
			String[] tokens = tokenizer.tokenize((String) data);
	
			String[] tags = tagger.tag(tokens);
			
			String label = "";
			if(label instanceof String)
				label = (String) target;
			
			newData = tokens + " POS=" + tags + " " + label;
		}
		
		System.out.println(">>> NEWDATA: " + newData);
		
		inst.setData(newData);
		
		return inst;
	}

	public POSTaggerME getTagger() {
		return tagger;
	}

	public void setTagger(POSTaggerME tagger) {
		this.tagger = tagger;
	}
	
}
