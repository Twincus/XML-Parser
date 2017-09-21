package edu.ucsd.XQuery.main;

import java.io.FileInputStream;
import java.util.List;

import edu.ucsd.XQuery.visitor.XQueryLexer;
import edu.ucsd.XQuery.visitor.XQueryParser;
import edu.ucsd.XQuery.visitor.optimize.TwoWayJoin;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.w3c.dom.Node;

import edu.ucsd.XQuery.util.Tools;
import edu.ucsd.XQuery.visitor.MyXQueryVisitor;

public class Main {
	public static void main(String[] args){
		try {
			String desPath = "output.xml";
			String newXQueryPath = "new_XQuery.txt";

			ANTLRInputStream inputStream = new ANTLRInputStream(new FileInputStream("query.txt"));
			XQueryLexer lexer = new XQueryLexer(inputStream);
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			XQueryParser parser = new XQueryParser(tokens);
			parser.removeErrorListeners();
			ParseTree tree = parser.xq();
			
			String newXQuery = TwoWayJoin.rewrite(tree);
			if (newXQuery != null) {
				System.out.println("rewriting done...");
				Tools.outputRewrittenXquey(newXQuery, newXQueryPath);
				inputStream = new ANTLRInputStream(new FileInputStream(newXQueryPath));
				lexer = new XQueryLexer(inputStream);
				tokens = new CommonTokenStream(lexer);
				parser = new XQueryParser(tokens);
				parser.removeErrorListeners();
				tree = parser.xq();
			}

			MyXQueryVisitor visitor = new MyXQueryVisitor();
			List<Node> res = visitor.visit(tree);
			Tools.writeToXML(res, desPath);
			System.out.println("done!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
