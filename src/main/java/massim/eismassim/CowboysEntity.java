package massim.eismassim;

import java.util.Collection;
import java.util.LinkedList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import eis.iilang.Action;
import eis.iilang.IILElement;
import eis.iilang.Percept;

public class CowboysEntity extends Entity {

	@Override
	public String getType() {
		return "cowboy";
	}

	@Override
	protected Document actionToXML(Action action) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Collection<Percept> byeToIIL(Document document) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Collection<Percept> requestActionToIIL(Document document) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Collection<Percept> simEndToIIL(Document document) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Collection<Percept> simStartToIIL(Document document) {
		// TODO Auto-generated method stub
		return null;
	}


}
