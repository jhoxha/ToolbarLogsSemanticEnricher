package triple.parser;

import metadata.annotator.Resource;

import org.apache.log4j.Logger;

public class Quad {
	final static Logger logger = Logger.getLogger(Quad.class);
	public String subject;
	public String predicate;
	public String object;
	public String context;
	
	public Quad(String asubject, String apredicate, String aobject, String acontext){
		subject=asubject;
		predicate=apredicate;
		object=aobject;
		context=acontext;
	}
	
	public Quad(Triple triple, String acontext){
		subject=triple.subject;
		predicate=triple.predicate;
		object=triple.object;
		context=acontext;
	}	
	
	public String getObject() {
		return object.toString();
	}

	public void setObject(String object) {
		this.object = object;
	}

	public String print(){
		return subject.toString()+"\t"+predicate.toString() + "\t"+object.toString() + "\t"+context.toString();
	}

}
