package edu.ucsd.XQuery.util;

import java.io.*;
import java.util.List;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

public class Tools {
	public static void writeToXML(List<Node> res, String filepath) throws IOException, TransformerException {
        File output = new File(filepath);
        if(res.isEmpty()) System.out.println("Empty result!");
        if(output.exists()) output.delete();
        try {
            FileOutputStream outputStream = new FileOutputStream(filepath,true);
            for(Node n : res) {
            	if(n.getNodeType() == Node.TEXT_NODE && n.getTextContent().equals("\n")) continue;
                if(n.getNodeType() == Node.ATTRIBUTE_NODE){
                    outputStream.write((n.getNodeName() + "=" + n.getNodeValue() + "\n").getBytes());
                    continue;
                }
                else if(n.getNodeType() == Node.TEXT_NODE){
                    outputStream.write((n.getTextContent() + "\n").getBytes());
                    continue;
                }
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.transform(new DOMSource(n), new StreamResult(outputStream));
            }
        } catch (FileNotFoundException e) {
            System.out.println("Cannot find output path!");
        }
    }

    public static void outputRewrittenXquey(String str, String filePath) throws IOException {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), "UTF-8"));
        bw.write(str);
        bw.close();
    }
}
