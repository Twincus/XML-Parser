package edu.ucsd.XQuery.visitor;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MyVisitorHelper {
	private static Document newDoc;
	
	static {
		DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = fac.newDocumentBuilder();
			newDoc = builder.newDocument();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public static List<Node> getAllChildren(Node parent){
		List<Node> res = new ArrayList<>();
		NodeList children = parent.getChildNodes();
		for(int i = 0; i < children.getLength(); i++){
			res.add(children.item(i));
		}
		return res;
	}
	
	public static List<Node> getElementChildren(Node parent){
		List<Node> res = new ArrayList<>();
		NodeList children = parent.getChildNodes();
		for(int i = 0; i < children.getLength(); i++){
			if(children.item(i).getNodeType() == Node.ELEMENT_NODE){
				res.add(children.item(i));
			}
		}
		return res;
	}
	
	public static List<Node> getTextChildren(Node parent){
		List<Node> res = new ArrayList<>();
		NodeList children = parent.getChildNodes();
		for(int i = 0; i < children.getLength(); i++){
			Node child = children.item(i);
			if(child.getNodeType() == Node.TEXT_NODE 
					&& !child.getTextContent().equals("\n")){
				res.add(child);
			}
		}
		return res;
	}
	
	public static List<Node> getDescendant(List<Node> parent){
		List<Node> temp = new ArrayList<Node>();
		Queue<Node> que = new LinkedList<Node>();
		que.addAll(parent);
		while(!que.isEmpty()){
			Node curr = que.poll();
			temp.addAll(MyVisitorHelper.getAllChildren(curr));
			que.addAll(MyVisitorHelper.getAllChildren(curr));
		}
		return temp;
	}
	
	public static Node makeText(Document doc, String text){
		Node res = doc.createTextNode(text);
		return res;
	}
	
	public static Node makeEle(Document doc, String tagName, List<Node> nodeList) {
		Node ele = newDoc.createElement(tagName);
		for (Node node : nodeList) {
			if (node != null)
				ele.appendChild(newDoc.importNode(node, true));
		}
		return ele;
	}

	public static String changeNodeToString(Node node) {
		String name = node.getNodeName();
		StringWriter writer = new StringWriter();
		Transformer transformer = null;

		try {
			transformer = TransformerFactory.newInstance().newTransformer();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		}

		try {
			transformer.transform(new DOMSource(node), new StreamResult(writer));
		} catch (TransformerException e) {
			e.printStackTrace();
		}

		String xml = writer.toString();
		return xml.substring(xml.indexOf("<" + name + ">") + 2 + name.length(), xml.length() - 3 - name.length());
	}
}
