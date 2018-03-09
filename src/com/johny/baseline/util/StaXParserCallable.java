package com.johny.baseline.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.XMLEvent;

import com.johny.baseline.beans.Article;
import com.johny.baseline.beans.InfoboxSchema;
import com.johny.baseline.beans.InfoboxTuple;

public class StaXParserCallable implements Callable<List<Article>>{

	private static final String PAGE = "page";
	private static final String TITLE = "title";
	private static final String REDIRECT = "redirect";
	private static final String REVISION = "revision";
	private static final String TEXT = "text";
	
	private static final String regexInfoboxMapping = "\\{\\{\\s?infobox.*\\n(|.*\\n)*\\}\\}";
	private static final String regexInfoboxTemplate = "infobox(\\s\\w*){1,2}";
	
	private String file;
	private String templateName;
	
	public StaXParserCallable(Path file, String templateName) throws IOException {
		this.file = file.toAbsolutePath().toString();
		this.templateName = templateName;
	}
	
	@Override
	public List<Article> call() throws Exception {
		System.out.println(">>>> File: " + this.file);
		System.out.println("-> SEARCHING FOR PAGES USING " + templateName.toUpperCase() + " INFOBOX");
		return readConfig(this.file);
	}
	
	public List<Article> readConfig(String configFile) throws IOException {
		List<Article> articles = new ArrayList<Article>();

		XMLInputFactory inputFactory = XMLInputFactory.newInstance();

		try {
			// set up event reader
			InputStream in = new FileInputStream(configFile);

			XMLEventReader eventReader = inputFactory.createXMLEventReader(in);

			Article article = new Article();
			while (eventReader.hasNext()) {

				XMLEvent event = eventReader.nextEvent();

				String text = "";
				if (event.isStartElement()) {
					
					if (event.asStartElement().getName().getLocalPart().equals(TITLE)) {
						
						event = eventReader.nextEvent();
						article.setArticleTitle(event.asCharacters().getData().replaceAll(" ", "_"));
						
						continue;
					
					}
					
					if(event.asStartElement().getName().getLocalPart().equals(REDIRECT)) {
						// Discard redirect article
						article = new Article();
						continue;
					}

					if (event.asStartElement().getName().getLocalPart().equals(TEXT)) {
						
						text = eventReader.getElementText().toLowerCase();
						
						if (text.contains(templateName.toLowerCase())) {
							article.setText(text);
							text = text.substring(0, 2000);
							InfoboxSchema infobox = getInfoboxSchemaFromText(text);
							article.setInfobox(infobox);
						}
						
						continue;
					
					}
				}

				if (event.isEndElement()) {

					EndElement endElement = event.asEndElement();

					if (endElement.getName().getLocalPart().equals(PAGE) 
							&& article.getArticleTitle()!=null) {
						
						if(!article.getArticleTitle().contains(":") && 
								article.getArticleTitle() != null &&
								article.getText() != null)
							articles.add(article);
						
						article = new Article();
					}
					
				}
			
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (XMLStreamException e) {
			System.err.println("Problem in file: "+ this.file + " - " + e.getMessage());
		}

		return articles;
	}
	
	/**
	 * Parse wiki page text to {@link InfoboxSchema}
	 * @param text
	 * @return {@link InfoboxSchema}
	 */
	private InfoboxSchema getInfoboxSchemaFromText(String text) {

		Pattern patternInfobox = Pattern.compile(regexInfoboxMapping);
		Pattern patternTemplateName = Pattern.compile(regexInfoboxTemplate);

		// extract infobox
		Matcher mInfobox = patternInfobox.matcher(text);
		
		if (mInfobox.find()) {
			InfoboxSchema infobox = new InfoboxSchema();
			
			String infoboxMatch = mInfobox.group(0);
			
			// extracts template name
			Matcher mTemplate = patternTemplateName.matcher(infoboxMatch);
			if (mTemplate.find()) {

				String templateName = mTemplate.group(0).replaceAll("\n", "")
						.trim().replaceAll(" ", "_");
				
				infobox.setTemplateName(templateName);
				
			}else
				infobox.setTemplateName(null);

			// removes template name from infobox matching
			String props = infoboxMatch.replaceAll(regexInfoboxTemplate, "")
					.replaceAll("\\<.*\\>", " ")
					.replaceAll("\n\\|", " | ").replaceAll("\\{", "")
					.replaceAll("\\}", "").replaceAll("\\[", "").replaceAll("\\]", "");
			
			// extracts for properties-values
			if (props != null)
				infobox.setTuples(getTuples(props));
			else
				infobox.setTuples(null);

			return infobox;
		}
		return null;
	}
	
	/**
	 * Parse props-value text from infobox mapping and returns a list of {@link InfoboxTuple}
	 * @param props - String
	 * @return tuples - List<{@link InfoboxTuple}>
	 */
	private List<InfoboxTuple> getTuples(String props) {
		
		List<InfoboxTuple> tuples = new ArrayList<InfoboxTuple>();
		
		String[] splitProps = props.split(" \\| ");
		
		if(splitProps.length > 0) {
			for (int i = 0; i < splitProps.length; i++) {
	
				if ((splitProps[i] != "") && (splitProps[i].contains("="))) {
	
					String[] tupleRaw = splitProps[i].trim().split(" \\= ");
					
					if (tupleRaw.length == 2) {
						
						if( !tupleRaw[0].equals("")
								&& !tupleRaw[0].equals(" ")
								&& !tupleRaw[1].equals("")
								&& !tupleRaw[1].equals(" ")) {
							
							InfoboxTuple tuple = 
									new InfoboxTuple(
											tupleRaw[0].replace("|", "").trim(), 
											tupleRaw[1].trim()
											.replaceAll("\n", ""));
							
							//System.out.println(tuple.toString());
							
							if (!tuples.contains(tuple))
								tuples.add(tuple);
						
						}
					}
				}
			}
		}
		
		return tuples;
	}

}
