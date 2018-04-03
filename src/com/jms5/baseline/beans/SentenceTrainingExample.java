package com.jms5.baseline.beans;

public class SentenceTrainingExample {
	private String attributeName;
	private String attributeValue;
	private String sentence;
	private String label;
	
	public SentenceTrainingExample() {
		super();
	}
	
	public SentenceTrainingExample(String attributeName, String attributeValue, String sentence, String label) {
		super();
		this.attributeName = attributeName;
		this.attributeValue = attributeValue;
		this.sentence = sentence;
		this.label = label;
	}
	
	public String getAttributeName() {
		return attributeName;
	}
	
	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}
	
	public String getAttributeValue() {
		return attributeValue;
	}
	
	public void setAttributeValue(String attributeValue) {
		this.attributeValue = attributeValue;
	}
	
	public String getSentence() {
		return sentence;
	}
	
	public void setSentence(String sentence) {
		this.sentence = sentence;
	}
	public String getLabel() {
		return label;
	}
	
	public void setLabel(String target) {
		this.label = target;
	}
	
	@Override
	public String toString() {
		return "> NAME: " + getAttributeName() + 
				"\n> VALUE: " + getAttributeValue() + 
				"\n> SENTENCE:" + getSentence() +
				"\n> TARGET: " + getLabel();
	}
}
