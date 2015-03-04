package com.github.jerdeb.htmlgen;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class Generator {

	private static Model ontology = ModelFactory.createDefaultModel();
	private static String htmlTemplate = "";
	private static Document htmlTemplateDoc;
	
	private static String namespace = "";

	
	private static void ontologyDescription(){		
		String queryNoAuthors = "SELECT * WHERE {"+
					"?namespace a <" + OWL.Ontology + "> ." +
					//"?namespace <"+ RDFS.label + "> ?label . " + 
					"?namespace <"+ DCTerms.title + "> ?label . " +
					//"?namespace <"+ RDFS.comment + "> ?abstract . " + 
					"?namespace <"+ DCTerms.description + "> ?abstract . " + 
					"OPTIONAL { ?namespace <"+ DCTerms.modified + "> ?modified . }" + 
					"OPTIONAL { ?namespace <"+ OWL.versionInfo + "> ?version . }" + 
					"}";
		
		Query qry = QueryFactory.create(queryNoAuthors);
	    QueryExecution qe = QueryExecutionFactory.create(qry, ontology);
	    ResultSet rs = qe.execSelect();

	    while (rs.hasNext()){
	    	QuerySolution sol = rs.next();
	    	namespace = sol.get("namespace").asResource().toString();
	    	htmlTemplate = htmlTemplate.replace("${ontology.label}", sol.get("label").asLiteral().getValue().toString());
	    	htmlTemplate = htmlTemplate.replace("${ontology.namespace}", sol.get("namespace").asResource().toString());
	    	htmlTemplate = htmlTemplate.replace("${ontology.abstract}", sol.get("abstract").asLiteral().getValue().toString());
	    	if (sol.get("modified") != null) htmlTemplate = htmlTemplate.replace("${ontology.modified}", sol.get("modified").asLiteral().getValue().toString());
	    	if (sol.get("versionInfo") != null) htmlTemplate = htmlTemplate.replace("${ontology.versionInfo}", sol.get("version").asLiteral().getValue().toString());
	    }
	    
	    String queryAuthors = "SELECT ?name ?page ?email WHERE {"+
				"?namespace a <" + OWL.Ontology + "> ." +
				"?namespace <"+ DCTerms.creator + "> ?atr ." +
				"?atr <" + FOAF.name + "> ?name ."+
				"OPTIONAL {?atr <" + FOAF.homepage + "> ?page . }"+
				"OPTIONAL {?atr <" + FOAF.mbox + "> ?email . }"+
				"}";
	   
	    qry = QueryFactory.create(queryAuthors);
	    qe = QueryExecutionFactory.create(qry, ontology);
	    rs = qe.execSelect();
	    
	    StringBuilder sb = new StringBuilder();

	    while (rs.hasNext()){
	    	QuerySolution sol = rs.next();
	    	String name = sol.get("name").asLiteral().toString();
	    	String page = (sol.get("page") != null) ? sol.get("page").asResource().toString() : "";
	    	String email = (sol.get("email") != null) ? sol.get("email").asResource().toString() : "";
	    	
	    	sb.append("<li>");
	    	if (!(page.equals(""))){
	    		sb.append("<a href='");
	    		sb.append(page);
	    		sb.append("'>");
	    		sb.append(name);
	    		sb.append("</a>");
	    	} else {
	    		sb.append(name);
	    	}
	    	
	    	if (!(email.equals(""))){
	    		sb.append("&nbsp;");
	    		sb.append("<a href='");
	    		sb.append(email);
	    		sb.append("'>");
	    		sb.append("Email");
	    		sb.append("</a>");
	    	}
	    	sb.append("</li>");
	    	sb.append(System.getProperty("line.separator"));
	    }
	    htmlTemplate = htmlTemplate.replace("${ontology.authors}", sb.toString());
	    
	}
	
	private static void classDescription(){
		String queryClassCount = "SELECT (COUNT(DISTINCT ?class) as ?count) WHERE {"+
				"{ ?class a <" + RDFS.Class + "> . } UNION " +
				"{ ?class a <" + OWL.Class + "> . } " +
				"}";
		
		Query qry = QueryFactory.create(queryClassCount);
	    QueryExecution qe = QueryExecutionFactory.create(qry, ontology);
	    ResultSet rs = qe.execSelect();

	    while (rs.hasNext()){
	    	QuerySolution sol = rs.next();
	    	int cnt = sol.get("count").asLiteral().getInt();
	    	htmlTemplate = htmlTemplate.replace("${class.count}", String.valueOf(cnt));
	    }
	    
	    
	    
	    StringBuilder classBuilder = new StringBuilder();
		String classTemplate = htmlTemplateDoc.getElementById("classTemplate").outerHtml();
		classBuilder.append(classTemplate);
	    
	    //TODO make all fields optional
		//TODO list all OWL Deprecated classes in a different color?
		String queryClassDescription = "SELECT DISTINCT ?class ?label ?description WHERE {"+
				"{ ?class a <" + RDFS.Class + "> . } UNION " +
				"{ ?class a <" + OWL.Class + "> . } " +
				"?class <" + RDFS.label + "> ?label.  " +
				"?class <" + RDFS.comment + "> ?description.  " +
				"}";
		
		qry = QueryFactory.create(queryClassDescription);
	    qe = QueryExecutionFactory.create(qry, ontology);
	    rs = qe.execSelect();
	    
	    StringBuilder classTOC = new StringBuilder();
	    StringBuilder allClasses = new StringBuilder();
	    

	    while (rs.hasNext()){
	    	String _cb = classBuilder.toString();
	    	
	    	QuerySolution sol = rs.next();
	    	classTOC.append("<a data-toggle=\"tooltip\" data-original-title=\"${class.shorttip}\" href=\"#"+sol.get("class").asResource().toString().replace(namespace, "")+"\">"+sol.get("class").asResource().toString().replace(namespace, "")+"</a>");
	    	classTOC.append(", ");
	    	
	    	_cb = _cb.replace("${class.uri}", sol.get("class").asResource().toString());
	    	_cb = _cb.replace("${class.anchor}",sol.get("class").asResource().toString().replace(namespace, ""));
	    	_cb = _cb.replace("${class.label}", sol.get("label").asLiteral().getValue().toString());
	    	_cb = _cb.replace("${class.description}", sol.get("description").asLiteral().toString());
	    	_cb = _cb.replace("${class.propertydomain}", createClassPropertyDomain(sol.get("class").asResource().toString()));
	    	_cb = _cb.replace("${class.propertyrange}", createClassPropertyRange(sol.get("class").asResource().toString()));
	    	_cb = _cb.replace("${class.subclassof}", createClassSubClassOf(sol.get("class").asResource().toString()));
	    	_cb = _cb.replace("${class.subclassedby}", createClassSubClassedBy(sol.get("class").asResource().toString()));

	    	_cb = _cb.replaceAll("(.)+(<del>)\n", "");
	    	
	    	_cb = _cb.replaceAll("(<br />)(?:\\s+(<br />))+", "<br />");
	    	
	    	allClasses.append(_cb);
	    }
	    classTOC.deleteCharAt(classTOC.lastIndexOf(","));
	    htmlTemplate = htmlTemplate.replace("${classes.label}", classTOC.toString());
	    htmlTemplate = htmlTemplate.replace(classTemplate, allClasses.toString());
	}
	
	
	private static String createClassPropertyDomain(String classURI){
		Resource cls = ontology.createResource(classURI);
		Property dom = ontology.createProperty(RDFS.domain.getURI());
		
		List<Resource> lst = ontology.listSubjectsWithProperty(dom, cls).toList();
		StringBuilder ret = new StringBuilder();
		
		for(Resource r : lst){
	    	ret.append(createHTMLResource(r.toString()));
	    	ret.append(", ");
		}
		if (ret.length() > 0) {
			ret.deleteCharAt(ret.lastIndexOf(","));
		} else {
			ret.append("<del>");
		}
		
		return ret.toString();
	}
	
	private static String createClassPropertyRange(String classURI){
		Resource cls = ontology.createResource(classURI);
		Property dom = ontology.createProperty(RDFS.range.getURI());
		
		List<Resource> lst = ontology.listSubjectsWithProperty(dom, cls).toList();
		StringBuilder ret = new StringBuilder();
		
		for(Resource r : lst){
	    	ret.append(createHTMLResource(r.toString()));
	    	ret.append(", ");
		}
		if (ret.length() > 0){
			ret.deleteCharAt(ret.lastIndexOf(","));
		}else {
			ret.append("<del>");
		}
		
		return ret.toString();
	}
		
	private static String createClassSubClassOf(String classURI){
		Resource cls = ontology.createResource(classURI);
		Property dom = ontology.createProperty(RDFS.subClassOf.getURI());
		
		List<RDFNode> lst = ontology.listObjectsOfProperty(cls, dom).toList();
		StringBuilder ret = new StringBuilder();
		
		for(RDFNode r : lst){
			if (r.isAnon()){
				continue;
				//TODO: Fix
			}
	    	ret.append(createHTMLResource(r.toString()));
	    	ret.append(", ");
		}
		if (ret.length() > 0) {
			ret.deleteCharAt(ret.lastIndexOf(","));
		} else {
			ret.append("<del>");
		}
		
		return ret.toString();
	}
	
	private static String createClassSubClassedBy(String classURI){
		Resource cls = ontology.createResource(classURI);
		Property dom = ontology.createProperty(RDFS.subClassOf.getURI());
		
		List<Resource> lst = ontology.listSubjectsWithProperty(dom, cls).toList();
		StringBuilder ret = new StringBuilder();
		
		for(Resource r : lst){
	    	ret.append(createHTMLResource(r.toString()));
	    	ret.append(", ");
		}
		if (ret.length() > 0){
			ret.deleteCharAt(ret.lastIndexOf(","));
		} else {
			ret.append("<del>");
		}
		
		return ret.toString();
	}
	
	private static String createHTMLResource(String resource){
		String ret = "";
		if (resource.contains(namespace)) ret = "<a href=\"#"+resource.replace(namespace, "")+"\">"+ontology.qnameFor(resource)+"</a>";
		else ret = "<a target=\"_blank\" class=\"text-warning\" href=\""+resource+"\">"+ontology.qnameFor(resource)+"</a>";

		return ret;
	}
	
	private static void propertyDescription(){
		String queryClassCount = "SELECT (COUNT(DISTINCT ?property) as ?count) WHERE {"+
				"?property a <" + RDF.Property + "> . " +
				"}";
		
		Query qry = QueryFactory.create(queryClassCount);
	    QueryExecution qe = QueryExecutionFactory.create(qry, ontology);
	    ResultSet rs = qe.execSelect();

	    while (rs.hasNext()){
	    	QuerySolution sol = rs.next();
	    	int cnt = sol.get("count").asLiteral().getInt();
	    	htmlTemplate = htmlTemplate.replace("${property.count}", String.valueOf(cnt));
	    }
	    
	    StringBuilder propertyBuilder = new StringBuilder();
		String propertyTemplate = htmlTemplateDoc.getElementById("propertyTemplate").outerHtml();
	    propertyBuilder.append(propertyTemplate);
	    
		//TODO list all OWL properties classes in a different color?

		String queryClassDescription = "SELECT DISTINCT *  WHERE {"+
				"{ ?class a <" + RDF.Property + "> . } UNION " +
				"{ ?class a <" + OWL.DatatypeProperty + "> . } UNION " +
				"{ ?class a <" + OWL.ObjectProperty + "> . } " +
				"OPTIONAL {?class <" + RDFS.label + "> ?label.}  " +
				"OPTIONAL {?class <" + RDFS.comment + "> ?description.}  " +
				"OPTIONAL {?class <" + RDFS.domain + "> ?domain.}  " +
				"OPTIONAL {?class <" + RDFS.range + "> ?range.}  " +
				"OPTIONAL {?class <" + OWL.minCardinality + "> ?mincardinality.  }" +
				"OPTIONAL {?class <" + OWL.maxCardinality + "> ?maxcardinality.  }" +
				"OPTIONAL {?class <" + OWL.inverseOf + "> ?inverse.  }" +
				"OPTIONAL {?class <" + RDFS.subPropertyOf + "> ?subpropertyof.  }" +
				"}";
		
		qry = QueryFactory.create(queryClassDescription);
	    qe = QueryExecutionFactory.create(qry, ontology);
	    rs = qe.execSelect();
	    
	    StringBuilder propertiesTOC = new StringBuilder();
	    StringBuilder allProp = new StringBuilder();

	    while (rs.hasNext()){
	    	String _cb = propertyBuilder.toString();
	    	
	    	QuerySolution sol = rs.next();
	    	
	    	if (!(sol.get("class").asResource().toString().startsWith(namespace)))
	    		continue; // we do not want to republish stuff which is already published somewhere else
	    	
	    	propertiesTOC.append("<a href=\"#"+sol.get("class").asResource().toString().replace(namespace, "")+"\">"+sol.get("class").asResource().toString().replace(namespace, "")+"</a>");
	    	propertiesTOC.append(", ");
	    	
	    	_cb = _cb.replace("${property.uri}", sol.get("class").asResource().toString());
	    	if (sol.get("label") != null) _cb = _cb.replace("${property.label}", sol.get("label").asLiteral().getValue().toString());
	    	else _cb = _cb.replace("${property.label}",sol.get("class").asResource().toString().replace(namespace, ""));
	    		
	    	_cb = _cb.replace("${property.types}", createPropertyTypes(sol.get("class").asResource().toString()));
	    	
	    	if (sol.get("description") != null) _cb = _cb.replace("${property.description}", sol.get("description").asLiteral().getValue().toString());
	    	else _cb = _cb.replace("${property.description}","");

	    	if (sol.get("domain") != null) _cb = _cb.replace("${property.classdomain}", createHTMLResource(sol.get("domain").asResource().toString()));
	    	else _cb = _cb.replace("${property.classdomain}", "-");

	    	
	    	if (sol.get("range") != null) _cb = _cb.replace("${property.classrange}", createHTMLResource(sol.get("range").asResource().toString()));
	    	else _cb = _cb.replace("${property.classrange}", "-");
	    		
	    	if (sol.get("mincardinality") != null) _cb = _cb.replace("${property.mincardinality}", String.valueOf(sol.get("mincardinality").asLiteral().getInt()));
	    	else _cb = _cb.replace("${property.mincardinality}","<del>");
	    	
	    	if (sol.get("maxcardinality") != null) _cb = _cb.replace("${property.maxcardinality}", String.valueOf(sol.get("maxcardinality").asLiteral().getInt()));
	    	else _cb = _cb.replace("${property.maxcardinality}","<del>");
	    	
	    	if (sol.get("inverse") != null) _cb = _cb.replace("${property.inverse}", createHTMLResource(sol.get("inverse").asResource().toString()));
	    	else _cb = _cb.replace("${property.inverse}","<del>");
	    	
	    	if (sol.get("subpropertyof") != null) _cb = _cb.replace("${property.subpropertyof}", createHTMLResource(sol.get("subpropertyof").asResource().toString()));
	    	else _cb = _cb.replace("${property.subpropertyof}","<del>");
	    	
	    	_cb = _cb.replace("${property.subpropertyby}", createSubPropertyBy(sol.get("class").asResource().toString()));
	    	
	    	_cb = _cb.replaceAll("(.)+(<del>)\n", "");
	    	
	    	_cb = _cb.replaceAll("(<br />)(?:\\s+(<br />))+", "<br />");
	    	
	    	allProp.append(_cb);
	    }
	    propertiesTOC.deleteCharAt(propertiesTOC.lastIndexOf(","));
	    htmlTemplate = htmlTemplate.replace("${properties.label}", propertiesTOC.toString());
	    htmlTemplate = htmlTemplate.replace(propertyTemplate, allProp.toString());
	}
	
	private static String createPropertyTypes(String classURI){
		StringBuilder sb = new StringBuilder();
		
		Resource cls = ontology.createResource(classURI);
		
		List<RDFNode> lst = ontology.listObjectsOfProperty(cls, RDF.type).toList();
		
		for(RDFNode r : lst){
	    	sb.append("<a target=\"_blank\" class=\"label label-success\" href=\""+r.asResource().toString()+"\">"+ontology.qnameFor(r.asResource().toString())+"</a>&nbsp;");
		}
		
		return sb.toString();
	}
	
	private static String createSubPropertyBy(String propertyURI){
		StringBuilder sb = new StringBuilder();
		
		Resource prop = ontology.createResource(propertyURI);
		
		List<Resource> lst = ontology.listSubjectsWithProperty(RDFS.subPropertyOf, prop).toList();
		
		for(Resource r : lst){
			sb.append(createHTMLResource(r.toString()));
	    	sb.append(", ");
		}
		
		if (sb.length() > 0){
			sb.deleteCharAt(sb.lastIndexOf(","));
		} else {
			sb.append("<del>");
		}
		

		
		
		return sb.toString();
	}
	
	private static void instanceDescriptions(){
		String query = "SELECT DISTINCT ?res WHERE {"
				+ "?res a ?type . "
				+ "MINUS { ?res a <" + RDF.Property + "> . }  " 
				+ "MINUS { ?res a <" + OWL.DatatypeProperty + "> . }  " 
				+ "MINUS { ?res a <" + OWL.ObjectProperty + "> . }"
				+ "MINUS { ?res a <" + RDFS.Class + "> . }  " 
				+ "MINUS { ?res a <" + OWL.Class + "> . } " 
				+ "MINUS { ?res a <" + OWL.Ontology + "> } ." 
				+ "MINUS { ?res a <" + OWL.DeprecatedClass + "> } ." 
				+ "}";
		
		
		Query qry = QueryFactory.create(query);
	    QueryExecution qe = QueryExecutionFactory.create(qry, ontology);
	    ResultSet rs = qe.execSelect();
		int count = 0;
		StringBuilder sb = new StringBuilder();


	    while (rs.hasNext()){
	    	QuerySolution sol = rs.next();
	    	if (sol.get("res").isAnon()){
	    		continue;
	    		//TODO: fix when blank node
	    	}
	    	sb.append(createHTMLResource(sol.get("res").asResource().toString()));
			sb.append(", ");
			count++;
	    }
		if (sb.length() > 0){
			sb.deleteCharAt(sb.lastIndexOf(","));
			sb.append("<br/>");
		}
		
    	htmlTemplate = htmlTemplate.replace("${instances.count}", String.valueOf(count));
    	htmlTemplate = htmlTemplate.replace("${instances.label}", sb.toString());
		
		
	}
	
	public static void main(String [] args) throws IOException{
		
		String sparqlQuerystring = "SELECT ?s where {?s ?p ?o} LIMIT 1";
		Query query = QueryFactory.create(sparqlQuerystring);

		QueryExecution qexec = QueryExecutionFactory.sparqlService("http://example.org", query);

		ResultSet results = qexec.execSelect();

		if (results.hasNext())
		{}		else
		{
		}
		qexec.close();
		
//		ontology.read(args[0]); 
		ontology.read("/Users/jeremy/Downloads/ns-2");
		
		File f = new File("/Users/jeremy/Downloads/ns-2");
		String asciidoc = f.getName();
		
		int pos = asciidoc.lastIndexOf(".");
		if (pos > 0) {
			asciidoc = asciidoc.substring(0, pos)+".adoc";
		}
		
		
		String tmp = "/Users/jeremy/Sites/ontologies/template.html";//Generator.class.getResource("template.html").getFile();
//		htmlTemplate = com.google.common.io.Files.toString(new File(tmp), Charset.defaultCharset());
		
		
		htmlTemplateDoc = Jsoup.parse(new File(tmp), Charset.defaultCharset().toString());
		htmlTemplateDoc.outputSettings().prettyPrint(true).indentAmount(0);

		
		htmlTemplate = htmlTemplateDoc.html();
		htmlTemplate =  htmlTemplate.replace("${asciidoc.path}", asciidoc);
		
		ontologyDescription();
		classDescription();
		propertyDescription();
		instanceDescriptions();
		
		Document cleaned = Jsoup.parse(htmlTemplate);
		cleaned.outputSettings().prettyPrint(true).indentAmount(3).outline(true);
		
		File outputFile = new File("/Users/jeremy/Sites/ontologies/sioc.html");
	    FileUtils.writeStringToFile(outputFile, cleaned.outerHtml(), "UTF-8");
		
	    //System.out.println(cleaned.html());

	}
}
