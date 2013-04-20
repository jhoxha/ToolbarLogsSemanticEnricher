package triple.parser;

public class Triple {

	public String subject;
	public String predicate;
	public String object;
	
	public Triple(String asubject, String apredicate, String aobject){
		subject=asubject.trim().replace("\n", " ");
		predicate=apredicate.trim().replace("\n", " ");
		object=aobject.trim().replace("\n", " ");
	}
	
	public String print(){
		return subject.toString()+"\t"+predicate.toString() + "\t"+object.toString();
	}

}
