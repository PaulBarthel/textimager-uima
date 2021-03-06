package org.hucompute.textimager.uima.wiki;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import de.tudarmstadt.ukp.dkpro.core.io.jwpl.type.WikipediaLink;

public class WikidataHyponyms extends JCasAnnotator_ImplBase {

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		for (WikipediaLink wikipediaLink : JCasUtil.select(aJCas, WikipediaLink.class)) {
			if(wikipediaLink.getLinkType().equals("internal")){
				org.hucompute.textimager.uima.type.wikipedia.WikipediaLink wikilink = new org.hucompute.textimager.uima.type.wikipedia.WikipediaLink(aJCas,wikipediaLink.getBegin(),wikipediaLink.getEnd());
				wikilink.setLinkType(wikipediaLink.getLinkType());
				wikilink.setTarget(wikipediaLink.getTarget());
				wikilink.setAnchor(wikipediaLink.getAnchor());
				try {
					wikilink.setWikiData(wikiDataFromWikipediaLink(wikilink.getTarget()));
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {
					List<String> wikidatas = wikidataHyponyms(wikilink.getWikiData());
					wikilink.setWikiDataHyponyms(new StringArray(aJCas, wikidatas.size()));
					for (int i = 0; i < wikidatas.size(); i++) {
						wikilink.setWikiDataHyponyms(i, wikidatas.get(i));
					}
				} catch (JSONException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				wikilink.addToIndexes();
				wikipediaLink.removeFromIndexes();
			}
		}
	}
	
	public void addWikidataHyponyms(WikipediaLink wikipediaLink){
		
	}
	

	public List<String>wikidataHyponyms(String wikidataId) throws JSONException, IOException{
		String query = 
				"SELECT ?item{"+
						"wd:"+ wikidataId +" wdt:P31|wdt:P279*|wdt:P31/wdt:P279 ?item"+
						"}";
		String url = "https://query.wikidata.org/sparql?query="+URLEncoder.encode(query, "utf-8")+"&format=json";
		return getWikidataInstanceOfByJson(new JSONObject(Jsoup.connect(url).ignoreContentType(true).execute().body()).getJSONObject("results").getJSONArray("bindings"));
	}

	public String wikiDataFromWikipediaLink(String wikiTitle,String wikipediaLanguage) throws IOException{
		String url = "https://" + wikipediaLanguage + ".wikipedia.org/w/api.php?action=query&prop=pageprops&ppprop=wikibase_item&redirects=1&format=xml&titles="+wikiTitle;
		Document doc;
		doc = Jsoup.connect(url).ignoreContentType(true).execute().parse();
		return (doc.select("query > pages pageprops").get(0).attr("wikibase_item"));
	}

	public String wikiDataFromWikipediaLink(String wikipediaLink) throws IOException{
		String language = wikipediaLink.replaceAll(".*?//(.*?)\\..*", "$1");
		String title = wikipediaLink.replaceAll(".*?//.*?/.*?/(.*)", "$1");
		return wikiDataFromWikipediaLink(title, language);
	}

	public List<String>getWikidataInstanceOfByJson(JSONArray json){
		List<String>output = new ArrayList<String>();
		for(int i = 0; i < json.length(); i++){
			if(json.getJSONObject(i).get("item") instanceof JSONObject)
				output.add(json.getJSONObject(i).getJSONObject("item").getString("value").replace("http://www.wikidata.org/entity/", ""));
			else
				output.add(json.getJSONObject(i).getString("item").replace("http://www.wikidata.org/entity/", ""));
		}
		return output;
	}
}
