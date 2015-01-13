package massim.eismassim;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import eis.iilang.Action;
import eis.iilang.IILElement;
import eis.iilang.Identifier;
import eis.iilang.Numeral;
import eis.iilang.Percept;

/**
 * This class represents a connector to the 
 * 2011-scenario.
 * 
 * @author tristanbehrens
 *
 */
class Mars2011Entity extends Entity {

	/**
	 * Instantiates an entity. Supposed to be only 
	 * called by a factory.
	 */
	protected Mars2011Entity() {
		super();
	}	
		
	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return "mars2011entity";
	}

	@Override
	protected Document actionToXML(Action action) {

		String actionType = null;
		actionType = action.getName();
		
		String actionParameter = null;
		if ( action.getParameters().size() != 0)
			actionParameter = action.getParameters().element().toProlog();
		
		String actionId = null;
		actionId = "" + getCurrentActionId();
		
			// create document
		Document doc = null;
		try {
			
			doc = documentbuilderfactory.newDocumentBuilder().newDocument();
			Element root = doc.createElement("message");
			root.setAttribute("type","action");
			doc.appendChild(root);
			
			Element auth = doc.createElement("action");
			auth.setAttribute("type",actionType);
			if ( actionParameter != null )
				auth.setAttribute("param",actionParameter);
			auth.setAttribute("id",actionId);
			root.appendChild(auth);
			
		} catch (ParserConfigurationException e) {
			System.err.println("unable to create new document");
			return null;
		}
		
		//System.out.println(action.toProlog());
		//System.out.println(XMLToString(doc));
		
		return doc;
	
	}

	@Override
	protected Collection<Percept> byeToIIL(Document document) {
		assert false : XMLToString(document);
		return null;
	}

	@Override
	protected Collection<Percept> requestActionToIIL(Document document) {

/*		
<?xml version="1.0" encoding="UTF-8" standalone="no"?><message timestamp="1296227560433" type="request-action">
<perception deadline="1296227562433" id="160">
<simulation step="159"/>
<self areaScore="11" energy="9" health="9" lastAction="skip" lastActionResult="" maxEnergy="9" maxEnergyDisabled="4" maxHealth="9" position="vertex14" strength="5" visRange="2"/>
<team achievementPoints="0" lastStepScore="11" name="A" score="638" usedAchievementPoints="0" zonesScore="11"/>
<visibleVertices>
<visibleVertex name="vertex6"/>
<visibleVertex name="vertex14"/>
</visibleVertices>
<visibleEdges>
<visibleEdge node1="vertex6" node2="vertex11"/>
</visibleEdges>
<visibleEntities>
<visibleEntity name="b8" node="vertex18" team="B"/>
</visibleEntities>
</perception>
</message>
 */
		
		HashSet<Percept> ret = new HashSet<Percept>();
		
		String str;
		
		// parse the <message>-tag
		Element root = document.getDocumentElement();
		assert root != null;
		assert root.getAttribute("type").equalsIgnoreCase("request-action");
		str = root.getAttribute("timestamp");
		assert str != null && str.equals("") == false : XMLToString(document);
		long timestamp = new Long(str).longValue();
		ret.add(new Percept("timestamp",new Numeral(timestamp)));

		// parse the <perception>-tag
		Element perception = (Element)root.getElementsByTagName("perception").item(0);
		assert perception.getNodeName().equalsIgnoreCase("perception");
		str = perception.getAttribute("deadline");
		assert str != null && str.equals("") == false : XMLToString(document);
		long deadline = new Long(str).longValue();
		ret.add(new Percept("deadline",new Numeral(deadline)));
		
		// used later
		NodeList tags;
		Element tag;
		int step;
		
		// <simulation>-tag
		tags = perception.getElementsByTagName("simulation");
		assert tags.getLength() == 1 : XMLToString(document);
		tag = (Element) tags.item(0);
		str = tag.getAttribute("step");
		assert str != null && str.equals("") == false : XMLToString(document);
		step = new Integer(str).intValue();
		ret.add(new Percept("step",new Numeral(step)));
		
		// <self>-tag
		// <self areaScore="11" energy="9" health="9" lastAction="skip" lastActionResult="" maxEnergy="9" maxEnergyDisabled="4" maxHealth="9" position="vertex14" strength="5" visRange="2"/>

//		int areasScore,energy,health,lastAction,lastActionResult,maxEnergy,maxEnergyDisabled,
		//maxHealth,position,strength,visRange;
		tags = perception.getElementsByTagName("self");
		assert tags.getLength() == 1;
		tag = (Element) tags.item(0);
		str = tag.getAttribute("zoneScore");
		assert str != null && str.equals("") == false : XMLToString(document);
		ret.add(new Percept("zoneScore",new Numeral(new Integer(str).intValue())));
		str = tag.getAttribute("energy");
		assert str != null && str.equals("") == false : XMLToString(document);
		ret.add(new Percept("energy",new Numeral(new Integer(str).intValue())));
		str = tag.getAttribute("health");
		assert str != null && str.equals("") == false : XMLToString(document);
		ret.add(new Percept("health",new Numeral(new Integer(str).intValue())));
		str = tag.getAttribute("lastAction");
		assert str != null && str.equals("") == false : XMLToString(document);
		ret.add(new Percept("lastAction",new Identifier(str)));
		str = tag.getAttribute("lastActionResult");
		assert str != null && str.equals("") == false : XMLToString(document);
		ret.add(new Percept("lastActionResult",new Identifier(str)));
		str = tag.getAttribute("maxEnergy");
		assert str != null && str.equals("") == false : XMLToString(document);
		ret.add(new Percept("maxEnergy",new Numeral(new Integer(str).intValue())));
		str = tag.getAttribute("maxEnergyDisabled");
		assert str != null && str.equals("") == false : XMLToString(document);
		ret.add(new Percept("maxEnergyDisabled",new Numeral(new Integer(str).intValue())));
		str = tag.getAttribute("maxHealth");
		assert str != null && str.equals("") == false : XMLToString(document);
		ret.add(new Percept("maxHealth",new Numeral(new Integer(str).intValue())));
		str = tag.getAttribute("position");
		assert str != null && str.equals("") == false : XMLToString(document);
		ret.add(new Percept("position",new Identifier(str)));
		str = tag.getAttribute("strength");
		assert str != null && str.equals("") == false : XMLToString(document);
		ret.add(new Percept("strength",new Numeral(new Integer(str).intValue())));
		str = tag.getAttribute("visRange");
		assert str != null && str.equals("") == false : XMLToString(document);
		ret.add(new Percept("visRange",new Numeral(new Integer(str).intValue())));

		// <team achievementPoints="0" lastStepScore="16" name="A" score="18935" usedAchievementPoints="0" zonesScore="16"/>
		tags = perception.getElementsByTagName("team");
		assert tags.getLength() == 1;
		tag = (Element) tags.item(0);
		str = tag.getAttribute("money");
		assert str != null && str.equals("") == false : XMLToString(document);
		ret.add(new Percept("money",new Numeral(new Integer(str).intValue())));
		str = tag.getAttribute("zonesScore");
		assert str != null && str.equals("") == false : XMLToString(document);
		ret.add(new Percept("zonesScore",new Numeral(new Integer(str).intValue())));
		str = tag.getAttribute("lastStepScore");
		assert str != null && str.equals("") == false : XMLToString(document);
		ret.add(new Percept("lastStepScore",new Numeral(new Integer(str).intValue())));
		str = tag.getAttribute("score");
		assert str != null && str.equals("") == false : XMLToString(document);
		ret.add(new Percept("score",new Numeral(new Integer(str).intValue())));
		//str = tag.getAttribute("usedAchievementPoints");
		//assert str != null && str.equals("") == false : XMLToString(document);
		//ret.add(new Percept("usedAchievementPoints",new Numeral(new Integer(str).intValue())));
		//str = tag.getAttribute("zonesScore");
		//assert str != null && str.equals("") == false : XMLToString(document);
		//ret.add(new Percept("zonesScore",new Numeral(new Integer(str).intValue())));

		// <visibleVertices>-tag and <visibleVertex>-tag
		tags = perception.getElementsByTagName("visibleVertices");
		assert tags.getLength() == 1;
		tag = (Element)tags.item(0);
		tags = tag.getElementsByTagName("visibleVertex");
		for ( int a = 0 ; a < tags.getLength() ; a ++ ) {
			tag = (Element)tags.item(a);
			str = tag.getAttribute("name");
			assert str != null && str.equals("") == false : XMLToString(document);
			Identifier name = new Identifier(str);
			str = tag.getAttribute("team");
			assert str != null && str.equals("") == false : XMLToString(document);
			Identifier team = new Identifier(str);
			ret.add(new Percept("visibleVertex",name,team));
		}

		// <visibleEdges>-tag and <visibleEdge>-tag
		tags = perception.getElementsByTagName("visibleEdges");
		assert tags.getLength() == 1;
		tag = (Element)tags.item(0);
		tags = tag.getElementsByTagName("visibleEdge");
		for ( int a = 0 ; a < tags.getLength() ; a ++ ) {
			tag = (Element)tags.item(a);
			str = tag.getAttribute("node1");
			assert str != null && str.equals("") == false : XMLToString(tag);
			Identifier node1 = new Identifier(str);
			str = tag.getAttribute("node2");
			assert str != null && str.equals("") == false : XMLToString(tag);
			Identifier node2 = new Identifier(str);
			ret.add(new Percept("visibleEdge",node1,node2));
		}
		
		// <visibleEntities>-tag and <visibleEntity>-tag
		tags = perception.getElementsByTagName("visibleEntities");
		if ( tags.getLength() != 0 ) {
			tag = (Element)tags.item(0);
			tags = tag.getElementsByTagName("visibleEntity");
			for ( int a = 0 ; a < tags.getLength() ; a ++ ) {
				tag = (Element)tags.item(a);
				str = tag.getAttribute("name");
				assert str != null && str.equals("") == false : XMLToString(tag);
				Identifier name = new Identifier(str);
				str = tag.getAttribute("node");
				assert str != null && str.equals("") == false : XMLToString(tag);
				Identifier node = new Identifier(str);
				str = tag.getAttribute("team");
				assert str != null && str.equals("") == false : XMLToString(tag);
				Identifier team = new Identifier(str);
				str = tag.getAttribute("status");
				assert str != null && str.equals("") == false : XMLToString(tag);
				Identifier status = new Identifier(str);
				ret.add(new Percept("visibleEntity",name,node,team,status));
			}
		}
		
		// <probedVertices>-tag and <probedVertex>-tag
		// <probedVertex name="vertex12" weight="8"/>
		tags = perception.getElementsByTagName("probedVertices");
		if ( tags.getLength() != 0 ) {
			tag = (Element)tags.item(0);
			tags = tag.getElementsByTagName("probedVertex");
			for ( int a = 0 ; a < tags.getLength() ; a ++ ) {
				tag = (Element)tags.item(a);
				str = tag.getAttribute("name");
				assert str != null && str.equals("") == false : XMLToString(tag);
				Identifier name = new Identifier(str);
				str = tag.getAttribute("value");
				assert str != null && str.equals("") == false : XMLToString(tag);
				Numeral value = new Numeral(new Integer(str).intValue());
				ret.add(new Percept("probedVertex",name,value));
			}
		}
		
		// <surveyedEdge node1="vertex12" node2="vertex19" weight="8"/>
		tags = perception.getElementsByTagName("surveyedEdges");
		if ( tags.getLength() != 0 ) {
			tag = (Element)tags.item(0);
			tags = tag.getElementsByTagName("surveyedEdge");
			for ( int a = 0 ; a < tags.getLength() ; a ++ ) {
				tag = (Element)tags.item(a);
				str = tag.getAttribute("node1");
				assert str != null && str.equals("") == false : XMLToString(tag);
				Identifier node1 = new Identifier(str);
				str = tag.getAttribute("node2");
				assert str != null && str.equals("") == false : XMLToString(tag);
				Identifier node2 = new Identifier(str);
				str = tag.getAttribute("weight");
				assert str != null && str.equals("") == false : XMLToString(tag);
				Numeral weight = new Numeral(new Integer(str).intValue());
				ret.add(new Percept("surveyedEdge",node1,node2,weight));
			}
		}
		
		// <inspectedEntity energy="8" health="9" maxEnergy="8" maxHealth="9" name="b4" node="vertex9" strength="6" team="B" visRange="2"/>
		tags = perception.getElementsByTagName("inspectedEntities");
		if ( tags.getLength() != 0 ) {
			tag = (Element)tags.item(0);
			tags = tag.getElementsByTagName("inspectedEntity");
			for ( int a = 0 ; a < tags.getLength() ; a ++ ) {
				tag = (Element)tags.item(a);
			
				str = tag.getAttribute("name");
				assert str != null && str.equals("") == false : XMLToString(tag);
				Identifier name = new Identifier(str);
				str = tag.getAttribute("team");
				assert str != null && str.equals("") == false : XMLToString(tag);
				Identifier team = new Identifier(str);
				str = tag.getAttribute("role");
				assert str != null && str.equals("") == false : XMLToString(tag);
				Identifier role = new Identifier(str);
				str = tag.getAttribute("node");
				assert str != null && str.equals("") == false : XMLToString(tag);
				Identifier node = new Identifier(str);
				str = tag.getAttribute("energy");
				assert str != null && str.equals("") == false : XMLToString(tag);
				Numeral energy = new Numeral(new Integer(str).intValue());
				str = tag.getAttribute("maxEnergy");
				assert str != null && str.equals("") == false : XMLToString(tag);
				Numeral maxEnergy = new Numeral(new Integer(str).intValue());
				str = tag.getAttribute("health");
				assert str != null && str.equals("") == false : XMLToString(tag);
				Numeral health = new Numeral(new Integer(str).intValue());
				str = tag.getAttribute("maxHealth");
				assert str != null && str.equals("") == false : XMLToString(tag);
				Numeral maxHealth = new Numeral(new Integer(str).intValue());
				str = tag.getAttribute("strength");
				assert str != null && str.equals("") == false : XMLToString(tag);
				Numeral strength = new Numeral(new Integer(str).intValue());
				str = tag.getAttribute("visRange");
				assert str != null && str.equals("") == false : XMLToString(tag);
				Numeral visRange = new Numeral(new Integer(str).intValue());

				ret.add(new Percept("inspectedEntity",name,team,role,node,energy,maxEnergy,health,maxHealth,strength,visRange));
			}
		}

		// achievements
		tags = perception.getElementsByTagName("team");
		assert tags.getLength() == 1;
		tag = (Element) tags.item(0);
		tags = tag.getElementsByTagName("achievements");
		if ( tags.getLength() != 0 ) {
			tag = (Element)tags.item(0);
			tags = tag.getElementsByTagName("achievement");
			for ( int a = 0 ; a < tags.getLength() ; a ++ ) {
				tag = (Element)tags.item(a);
				str = tag.getAttribute("name");
				assert str != null && str.equals("") == false : XMLToString(tag);
				ret.add(new Percept("achievement",new Identifier(str)));
			}
		}
		
		return ret;
	
	}

	@Override
	protected Collection<Percept> simEndToIIL(Document document) {
	
		// <?xml version="1.0" encoding="UTF-8" standalone="no"?><message timestamp="1296546945647" type="sim-end">
		// <sim-result ranking="2" score="35"/>
		// </message>

		HashSet<Percept> ret = new HashSet<Percept>();

		String str;
		
		// parse the <message>-tag
		Element root = document.getDocumentElement();
		assert root != null;
		assert root.getAttribute("type").equalsIgnoreCase("sim-end");

		// parse the <simulation>-tag
		Element simResult = (Element)root.getElementsByTagName("sim-result").item(0);
		assert simResult.getNodeName().equalsIgnoreCase("sim-result");
		str = simResult.getAttribute("ranking");
		assert str != null && str.equals("") == false;
		ret.add(new Percept("ranking",new Numeral(new Integer(str).intValue())));
		str = simResult.getAttribute("score");
		assert str != null && str.equals("") == false;
		ret.add(new Percept("score",new Numeral(new Integer(str).intValue())));
		
		return ret;
	
	}

	@Override
	protected Collection<Percept> simStartToIIL(Document document) {

		HashSet<Percept> ret = new HashSet<Percept>();

		//<?xml version="1.0" encoding="UTF-8" standalone="no"?><message timestamp="1296226870974" type="sim-start">
		//<simulation edges="46" id="0" steps="1000" vertices="20"/>

		String str;
		
		// parse the <message>-tag
		Element root = document.getDocumentElement();
		assert root != null;
		assert root.getAttribute("type").equalsIgnoreCase("sim-start");

		// parse the <simulation>-tag
		Element simulation = (Element)root.getElementsByTagName("simulation").item(0);
		assert simulation.getNodeName().equalsIgnoreCase("simulation");
		str = simulation.getAttribute("id");
		assert str != null && str.equals("") == false;
		ret.add(new Percept("id",new Identifier(str)));
		str = simulation.getAttribute("steps");
		assert str != null && str.equals("") == false;
		ret.add(new Percept("steps",new Numeral(new Integer(str).intValue())));
		str = simulation.getAttribute("vertices");
		assert str != null && str.equals("") == false;
		ret.add(new Percept("vertices",new Numeral(new Integer(str).intValue())));
		str = simulation.getAttribute("edges");
		assert str != null && str.equals("") == false;
		ret.add(new Percept("edges",new Numeral(new Integer(str).intValue())));
		str = simulation.getAttribute("role");
		assert str != null && str.equals("") == false;
		ret.add(new Percept("role",new Identifier(str)));

		// used later
		/*NodeList tags;
		Element tag;
		int step;

		// parse the <self>-tag
		Element self = (Element)simulation.getElementsByTagName("self").item(0);
		assert self.getNodeName().equalsIgnoreCase("self");
		str = self.getAttribute("maxEnergy");
		assert str != null && str.equals("") == false;
		ret.add(new Percept("maxEnergy",new Numeral(new Integer(str).intValue())));
		str = self.getAttribute("name");
		assert str != null && str.equals("") == false;
		ret.add(new Percept("name",new Identifier(str)));
		str = self.getAttribute("strength");
		assert str != null && str.equals("") == false;
		ret.add(new Percept("strength",new Numeral(new Integer(str).intValue())));
		str = self.getAttribute("team");
		assert str != null && str.equals("") == false;
		ret.add(new Percept("team",new Identifier(str)));
		str = self.getAttribute("visRange");
		assert str != null && str.equals("") == false;
		ret.add(new Percept("visRange",new Numeral(new Integer(str).intValue())));
		println("role is missing");
		println("get rid off strength?");
		println("get rid off visrange?");

		// teammmembers
		Element teamMembers = (Element)simulation.getElementsByTagName("teamMembers").item(0);
		tags = teamMembers.getElementsByTagName("teamMember");
		for ( int a = 0 ; a < tags.getLength() ; a++ ) {
			
			tag = (Element)tags.item(a);
			str = self.getAttribute("maxEnergy");
			assert str != null && str.equals("") == false;
			Numeral maxEnergy = new Numeral(new Integer(str).intValue());
			str = self.getAttribute("name");
			assert str != null && str.equals("") == false;
			Identifier name = new Identifier(str);
			str = self.getAttribute("strength");
			assert str != null && str.equals("") == false;
			Numeral strength = new Numeral(new Integer(str).intValue());
			str = self.getAttribute("team");
			assert str != null && str.equals("") == false;
			Identifier team = new Identifier(str);
			str = self.getAttribute("visRange");
			assert str != null && str.equals("") == false;
			Numeral visRange = new Numeral(new Integer(str).intValue());
			
			ret.add(new Percept("teamMember",name,team,maxEnergy,strength,visRange));
			
		}
		
		println("role is missing");
		println("get rid off team?");
		println("get rid off strength?");
		println("get rid off visrange?");*/
		
		//assert false : XMLToString(document) + "\n" + ret;
		
		return ret;

	}

}