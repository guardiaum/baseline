package com.jms5.baseline.beans;
/**
 * POJO for Article
 * @author jms5
 *
 */
public class Article {
	
	private String articleTitle;
	private InfoboxSchema infobox;
	private String text;
	private boolean isMember;

	public Article() {
		this.isMember = false;
	}
	
	public Article(String articleTitle) {
		this.articleTitle = articleTitle;
		this.isMember = false;
	}
	
	public Article(String articleTitle, InfoboxSchema infobox) {
		super();
		this.articleTitle = articleTitle;
		this.infobox = infobox;
		this.isMember = false;
	}

	public Article(String articleTitle, InfoboxSchema infobox, String text) {
		super();
		this.articleTitle = articleTitle;
		this.infobox = infobox;
		this.text = text;
		this.isMember = false;
	}

	public InfoboxSchema getInfobox() {
		return infobox;
	}

	public void setInfobox(InfoboxSchema infobox) {
		this.infobox = infobox;
	}

	public String getArticleTitle() {
		return articleTitle;
	}

	public void setArticleTitle(String article_title) {
		this.articleTitle = article_title;
	}
	
	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
	
	public boolean isMember() {
		return isMember;
	}

	public void setMember(boolean isMember) {
		this.isMember = isMember;
	}
	
	@Override
	public boolean equals(Object obj) {
		boolean same = false;
		
		if( obj!=null && obj instanceof Article){
			same = this.articleTitle.equals(((Article) obj).getArticleTitle());
		}
		
		return same;
	}
}
