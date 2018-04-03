package com.jms5.baseline.beans;

import java.util.List;
/**
 * POJO for infobox schema
 * @author jms5
 *
 */
public class InfoboxSchema {
	
	private String templateName;
	private List<InfoboxTuple> tuples;

	public InfoboxSchema() {
	}
	
	public InfoboxSchema(List<InfoboxTuple> tuples) {
		super();
		this.tuples = tuples;
	}
	
	public InfoboxSchema(String templateName, List<InfoboxTuple> tuples) {
		super();
		this.templateName = templateName;
		this.tuples = tuples;
	}

	public String getTemplateName() {
		return templateName;
	}

	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}

	public List<InfoboxTuple> getTuples() {
		return tuples;
	}

	public void setTuples(List<InfoboxTuple> tuples) {
		this.tuples = tuples;
	}
	
	@Override
	public String toString() {
		String properties = " ";
		
		for(InfoboxTuple tuple : tuples)
			properties = properties + tuple.getProperty().toString() + ", ";
		
		return properties;
	}
	
}
