package com.johny.baseline.parsers;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.johny.baseline.util.Constants;

public class KeywordsStaXParserCallable implements Callable<List<Article>>{

	private String file;
	private String[] keywords;
	
	public KeywordsStaXParserCallable(Path file, String[] keywords) throws IOException {
		this.file = file.toAbsolutePath().toString();
		this.keywords = keywords;
	}
	
	@Override
	public List<Article> call() throws Exception {
		System.out.println(">>>> File: " + this.file);
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
					
					if (event.asStartElement().getName().getLocalPart().equals(Constants.TITLE)) {
						
						event = eventReader.nextEvent();
						
						String title = event.asCharacters().getData();
						
						for (String keyword : keywords) {
							if(title.contains(keyword)) {
								article.setArticleTitle(title.replaceAll(" ", "_"));
							}
						}
						
						continue;
					}
					
					if(event.asStartElement().getName().getLocalPart().equals(Constants.REDIRECT)) {
						// Discard redirect article
						article = new Article();
						continue;
					}

					if (event.asStartElement().getName().getLocalPart().equals(Constants.TEXT) 
							&& article.getArticleTitle() != null) {
						
						text = eventReader.getElementText().toLowerCase();
						
						Pattern pattern = Pattern.compile(Constants.REGEX_CATEGORY);
						Matcher matcher = pattern.matcher(text);
						
						List<String> categories = new ArrayList<String>();
						
						while(matcher.find()) {
							
							String category = matcher.group(0)
										.replaceAll("Category:", "")
										.replaceAll("\\[", "")
										.replaceAll("]", "");
							
							categories.add(category);
							
							for (String keyword : keywords) {
								if(category.contains(keyword)) {
									article.setMember(true);
									System.out.println(article.getArticleTitle() +" -> " + article.isMember());
								}
							}
							
						}
						
						if(categories.size()!=0)
							System.out.println(article.getArticleTitle() +" -> " + categories);
						else
							System.out.println(article.getArticleTitle() +" -> NO CATEGORY");
						
						article.setText(text.replaceAll(Constants.REGEX_INFOBOX_MAPPING, "")
										.replaceAll("\\n", " ").replaceAll("\\t", ""));
						
						continue;
					
					}
				}

				if (event.isEndElement() 
						&& article.getArticleTitle() != null) {

					EndElement endElement = event.asEndElement();

					if (endElement.getName().getLocalPart().equals(Constants.PAGE) 
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

}
