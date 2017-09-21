package edu.ucsd.XQuery.visitor;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static edu.ucsd.XQuery.visitor.MyVisitorHelper.makeEle;
import static edu.ucsd.XQuery.visitor.MyVisitorHelper.changeNodeToString;


public class MyXQueryVisitor extends XQueryBaseVisitor<List<Node>>{
	private Document xmlDoc = null;
	private List<Node> currNodeList = new ArrayList<>();
	private Map<String, List<Node>> allVars = new HashMap<>();
	
	@Override public List<Node> visitRpParent(@NotNull XQueryParser.RpParentContext ctx) {
		Set<Node> set = new HashSet<Node>();
		for(Node node : currNodeList){
			Node parent = null;
			if(node.getNodeType() == Node.ATTRIBUTE_NODE)
				parent = ((Attr)node).getOwnerElement();
			else
				parent = node.getParentNode();
			if(!set.contains(parent))
				set.add(parent);
		}
		List<Node> res = new ArrayList<>(set);
		currNodeList = res;
		return res;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visit} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitFilterOr(@NotNull XQueryParser.FilterOrContext ctx) {
		List<Node> copy = new ArrayList<>(currNodeList);
		Set<Node> set = new HashSet<>(visit(ctx.f(0)));
		currNodeList = copy;
		set.addAll((visit(ctx.f(1))));
		List<Node> res = new ArrayList<Node>(set);
		currNodeList = res;
		return res;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visit} on {@code ctx}.</p>
	 */

	@Override public List<Node> visitDoc(@NotNull XQueryParser.DocContext ctx) { 
		List<Node> res = new ArrayList<>();
		File file = new File(ctx.fileName().getText());
		if(!file.exists()){
			System.out.println("The input xml file doesn't exist!");
		}
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			xmlDoc = dBuilder.parse(file);
	        xmlDoc.getDocumentElement().normalize();
		} catch (ParserConfigurationException | SAXException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		res.add((Node)xmlDoc);
		currNodeList = res;
		return res;
	}
	
	@Override public List<Node> visitApDescendants(@NotNull XQueryParser.ApDescendantsContext ctx) {
		visit(ctx.doc());
		currNodeList.addAll(MyVisitorHelper.getDescendant(currNodeList));  //including current node, namely root node
		return visit(ctx.rp());
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visit} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitRpChildren(@NotNull XQueryParser.RpChildrenContext ctx) {
		List<Node> res = new ArrayList<>();
		for(Node node : currNodeList){
			res.addAll(MyVisitorHelper.getAllChildren(node));
		}
		currNodeList = res;
		return res;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visit} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitRpCurrent(@NotNull XQueryParser.RpCurrentContext ctx) {
		return currNodeList; 
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visit} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitFilterRp(@NotNull XQueryParser.FilterRpContext ctx) {
		List<Node> temp = new ArrayList<>(currNodeList);
		List<Node> res = new ArrayList<>();
		for(Node node : temp){
			currNodeList = new ArrayList<>();
			currNodeList.add(node);
			if(!visit(ctx.rp()).isEmpty())
				res.add(node);
		}
		currNodeList = res;
		return res;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visit} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitRpDoubleSlash(@NotNull XQueryParser.RpDoubleSlashContext ctx) {
		visit(ctx.rp(0));
		currNodeList.addAll(MyVisitorHelper.getDescendant(currNodeList)); //currNodeList should include current nodes and all descendants of the current nodes 
		Set<Node> set = new HashSet<Node>(visit(ctx.rp(1)));
		currNodeList = new ArrayList<>(set);
		return currNodeList;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visit} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitRpConcatenation(@NotNull XQueryParser.RpConcatenationContext ctx) {
		List<Node> copy = new ArrayList<Node>(currNodeList);
		List<Node> res = new ArrayList<Node>(visit(ctx.rp(0)));
		currNodeList = copy;
		res.addAll(visit(ctx.rp(1)));
		currNodeList = res;
		return res;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visit} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitRpBracket(@NotNull XQueryParser.RpBracketContext ctx) { 
		return visit(ctx.rp());
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visit} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitFilterNot(@NotNull XQueryParser.FilterNotContext ctx) {
		List<Node> copy = new ArrayList<>(currNodeList);
		Set<Node> set = new HashSet<>(visit(ctx.f()));
		List<Node> res = new ArrayList<>();
		for(Node node : copy){
			if(!set.contains(node))
				res.add(node);
		}
		currNodeList = res;
		return res;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visit} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitFilterBracket(@NotNull XQueryParser.FilterBracketContext ctx) {
		return visit(ctx.f());
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visit} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitRpText(@NotNull XQueryParser.RpTextContext ctx) {
		List<Node> res = new ArrayList<>();
		for(Node node : currNodeList){
			res.addAll(MyVisitorHelper.getTextChildren(node));
		}
		currNodeList = res;
		return res;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visit} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitApChildren(@NotNull XQueryParser.ApChildrenContext ctx) {
		List<Node> res = new ArrayList<Node>();
		visit(ctx.doc());
		res.addAll(visit(ctx.rp()));
		currNodeList = res;
        return res;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visit} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitRpTagName(@NotNull XQueryParser.RpTagNameContext ctx) {
		List<Node> res = new ArrayList<>();
		String tagName = ctx.NAME().getText();
		for(Node node : currNodeList){
			for(Node child : MyVisitorHelper.getElementChildren(node)){  //only element node has tag name
				if(child.getNodeName().equals(tagName))
					res.add(child);
			}
		}
		currNodeList = res;
		return res; 
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visit} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitFilterAnd(@NotNull XQueryParser.FilterAndContext ctx) {
		visit(ctx.f(0));
		return visit(ctx.f(1));
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visit} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitFilterEq(@NotNull XQueryParser.FilterEqContext ctx) {
		List<Node> copy = new ArrayList<>(currNodeList);
		List<Node> res = new ArrayList<>();
		for(Node node : copy){
			currNodeList = new ArrayList<>();
			currNodeList.add(node);
			List<Node> rp0Result = new ArrayList<>(visit(ctx.rp(0)));
			
			currNodeList = new ArrayList<>();
			currNodeList.add(node);
			List<Node> rp1Result = new ArrayList<>(visit(ctx.rp(1)));
			
			boolean flag = false;
			for(Node n1 : rp0Result){
				for(Node n2 : rp1Result){
					if(n1.isEqualNode(n2)){
						flag = true;
						res.add(node);
						break;
					}
				}
				if(flag)	break;
			}
		}
		currNodeList = res;
		return res;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visit} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitFilterIs(@NotNull XQueryParser.FilterIsContext ctx) {
		List<Node> copy = new ArrayList<>(currNodeList);
		List<Node> res = new ArrayList<>();
		for(Node node : copy){
			currNodeList = new ArrayList<>();
			currNodeList.add(node);
			List<Node> rp0Result = new ArrayList<>(visit(ctx.rp(0)));
			
			currNodeList = new ArrayList<>();
			currNodeList.add(node);
			List<Node> rp1Result = new ArrayList<>(visit(ctx.rp(1)));
			
			boolean flag = false;
			for(Node n1 : rp0Result){
				for(Node n2 : rp1Result){
					if(n1.isSameNode(n2)){
						flag = true;
						res.add(node);
						break;
					}
				}
				if(flag)	break;
			}
		}
		currNodeList = res;
		return res;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visit} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitRpSlash(@NotNull XQueryParser.RpSlashContext ctx) {
		visit(ctx.rp(0));
		Set<Node> set = new HashSet<Node>(visit(ctx.rp(1))); //will there be duplicate nodes after searching rp(1)?
		currNodeList = new ArrayList<>(set);
		return currNodeList; 
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visit} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitRpFilter(@NotNull XQueryParser.RpFilterContext ctx) {
		visit(ctx.rp());
		return visit(ctx.f());
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visit} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitRpAttribute(@NotNull XQueryParser.RpAttributeContext ctx) {
		List<Node> res = new ArrayList<>();
		String attrName = ctx.NAME().getText();
		for(Node node : currNodeList){
			if(node.getNodeType() == Node.ELEMENT_NODE){
				Element ele = (Element)node;
				if(ele.hasAttribute(attrName))
					res.add(ele.getAttributeNode(attrName));
			}
		}
		currNodeList = res;
		return res;
	}
	
	@Override public List<Node> visitXqComma(XQueryParser.XqCommaContext ctx) { 
		List<Node> res = new ArrayList<>();
		List<Node> copy = new ArrayList<>(currNodeList);
		System.out.println(ctx.xq(0).getText());
		System.err.println(ctx.xq(1).getText());
		res.addAll(visit(ctx.xq(0)));
		currNodeList = copy;
		res.addAll(visit(ctx.xq(1)));
		currNodeList = res;
		return res; 
	
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitXqAp(XQueryParser.XqApContext ctx) {
		return visit(ctx.ap());
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitXqVar(XQueryParser.XqVarContext ctx) {
		return visit(ctx.var());
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitXqClause(XQueryParser.XqClauseContext ctx) {
		List<Node> res = new ArrayList<>();
		Map<String, List<Node>> varsCopy = new HashMap<>(allVars);
		recurse(ctx, ctx.forClause().var().size(), 0, res);
		allVars = varsCopy;
		return res;
	}
	
	private void recurse(XQueryParser.XqClauseContext ctx, int forSize, int i, List<Node> res){
		if(i < forSize) { //iterate over vars in for-clause
			Map<String, List<Node>> varsBackUp = new HashMap<>(allVars);
//			List<Node> currNodeListBackUp = new ArrayList<>(currNodeList);
			System.out.println(ctx.forClause().xq(i).getText());
			List<Node> nodes = visit(ctx.forClause().xq(i));
			for(Node node : nodes) {
				List<Node> temp = new ArrayList<>();
				allVars = new HashMap<>(varsBackUp);
//				currNodeList = new ArrayList<>(currNodeListBackUp);
				temp.add(node);
				allVars.put(ctx.forClause().var(i).NAME().getText(), temp);
				recurse(ctx, forSize, i+1, res);
			}
		}
		else { //deal with let-clause and where-clause
			if(ctx.letClause() != null) 
				visit(ctx.letClause());
			if(ctx.whereClause() != null && visit(ctx.whereClause()) == null)
				return;
			res.addAll(visit(ctx.returnClause()));
		}
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitXqSlashRp(XQueryParser.XqSlashRpContext ctx) { 
		System.out.println(ctx.getText());
		
		List<Node> temp = new ArrayList<>(visit(ctx.xq()));
		
		Set<Node> set = new HashSet<>();
		for(Node t: temp) {
			currNodeList = new ArrayList<>();
			currNodeList.add(t);
			set.addAll(visit(ctx.rp()));
		}
		currNodeList = new ArrayList<>(set);
		System.out.println(ctx.rp().getText());
		return currNodeList;
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitXqBracket(XQueryParser.XqBracketContext ctx) {
		return visit(ctx.xq());
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitXqResult(XQueryParser.XqResultContext ctx) { 
		List<Node> temp = new ArrayList<>(visit(ctx.xq()));
		List<Node> res = new ArrayList<>();
		res.add(MyVisitorHelper.makeEle(xmlDoc, ctx.NAME(0).getText(), temp));
		return res;
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitXqDoubleSlashRp(XQueryParser.XqDoubleSlashRpContext ctx) {
		System.out.println(ctx.getText());
		System.out.println(ctx.xq().getText());
		List<Node> aa = visit(ctx.xq());
		List<Node> temp = new ArrayList<>(aa);
		Set<Node> set = new HashSet<>();
		for(Node t: temp) {
			currNodeList = new ArrayList<>();
			currNodeList.add(t);
			currNodeList.addAll(MyVisitorHelper.getDescendant(currNodeList));
			set.addAll(visit(ctx.rp()));
		}
		currNodeList = new ArrayList<>(set);
		return currNodeList;
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitXqStrConst(XQueryParser.XqStrConstContext ctx) {
		List<Node> res = new ArrayList<>();
		String text = ctx.STR_CONST().getText();
		res.add(MyVisitorHelper.makeText(xmlDoc, text.substring(1, text.length()-1)));
		return res;
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitXqlet(XQueryParser.XqletContext ctx) { 
		Map<String, List<Node>> copy = new HashMap<>(allVars);
		visit(ctx.letClause());
		List<Node> res = visit(ctx.xq());
		allVars = copy;
		return res;
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitForClause(XQueryParser.ForClauseContext ctx) { 
		return null;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitLetClause(XQueryParser.LetClauseContext ctx) { 
		int size = ctx.var().size();
		for(int i = 0; i < size; i++) {
			List<Node> currNodeListCopy = new ArrayList<>(currNodeList);
			Map<String, List<Node>> copy = new HashMap<>(allVars);
			copy.put(ctx.var(i).NAME().getText(), new ArrayList<>(visit(ctx.xq(i))));
			allVars = copy;
			currNodeList = currNodeListCopy;
		}
		return new ArrayList<>(0);
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitWhereClause(XQueryParser.WhereClauseContext ctx) { 
		return visit(ctx.cond());
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitReturnClause(XQueryParser.ReturnClauseContext ctx) { 
		return visit(ctx.xq());
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitCondOr(XQueryParser.CondOrContext ctx) { 
		List<Node> copy = new ArrayList<>(currNodeList);
		
		List<Node> first = visit(ctx.cond(0));
		currNodeList = copy;
		if(first != null)
			return new ArrayList<>(0);
		List<Node> copy2 = new ArrayList<>(currNodeList);
		List<Node> second = visit(ctx.cond(1));
		currNodeList = copy2;
		if (second == null)
			return null;
		return new ArrayList<>(0);
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitCondAnd(XQueryParser.CondAndContext ctx) {
		List<Node> copy = new ArrayList<>(currNodeList);
		System.out.println(ctx.getText());
		List<Node> first = visit(ctx.cond(0));
		currNodeList = copy;
		if(first == null)
			return null;
		List<Node> copy2 = new ArrayList<>(currNodeList);
		List<Node> second = visit(ctx.cond(1));
		currNodeList = copy2;
		if (second == null)
			return null;
		return new ArrayList<>(0);
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitCondEmpty(XQueryParser.CondEmptyContext ctx) { 
//		List<Node> copy = new ArrayList<>(currNodeList);
		List<Node> temp = visit(ctx.xq());
//		currNodeList = copy;
		if(temp.isEmpty())
			return new ArrayList<>(0);
		return null;
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitCondSome(XQueryParser.CondSomeContext ctx) { 
		int varSize = ctx.var().size();
		System.out.println(ctx.getText());
		Map<String, List<Node>> allVarsBackUp = new HashMap<>(allVars);
		for(int i = 0; i < varSize; i++) {
			List<Node> copy = new ArrayList<>(currNodeList);
			Map<String, List<Node>> copyMap = new HashMap<>(allVars);
			copyMap.put(ctx.var(i).NAME().getText(), visit(ctx.xq(i)));
			currNodeList = copy;
			allVars = copyMap;
		}
		List<Node> copy = new ArrayList<>(currNodeList);
		List<Node> res = visit(ctx.cond());
		currNodeList = copy;
		allVars = allVarsBackUp;
		return res;
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitCondIs(XQueryParser.CondIsContext ctx) {
		List<Node> copy = new ArrayList<>(currNodeList);
		List<Node> copy2 = new ArrayList<>(currNodeList);
		
		List<Node> first = new ArrayList<>(visit(ctx.xq(0)));
		currNodeList = copy;
		List<Node> second = new ArrayList<>(visit(ctx.xq(1)));
		for(Node n1: first) {
			for(Node n2: second) {
				if(n1.isSameNode(n2))
					return new ArrayList<>(0);
			}
		}
		currNodeList = copy2;
		return null;
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitCondNot(XQueryParser.CondNotContext ctx) { 
		List<Node> copy = new ArrayList<>(currNodeList);
		List<Node> temp = visit(ctx.cond());
		currNodeList = copy;
		if(temp == null)
			return new ArrayList<>(0);
		else
			return null;
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitCondEq(XQueryParser.CondEqContext ctx) { 
		List<Node> copy = new ArrayList<>(currNodeList);
		List<Node> copy2 = new ArrayList<>(currNodeList);
		
		List<Node> first = new ArrayList<>(visit(ctx.xq(0)));
		currNodeList = copy;
		List<Node> second = new ArrayList<>(visit(ctx.xq(1)));
		for(Node n1: first) {
			for(Node n2: second) {
				if(n1.isEqualNode(n2))
					return new ArrayList<>(0);
			}
		}
		currNodeList = copy2;
		return null;
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitCondParenthetsis(XQueryParser.CondParenthetsisContext ctx) { 
		return visit(ctx.cond());
	}
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public List<Node> visitVar(XQueryParser.VarContext ctx) {
		System.out.println(ctx.NAME().getText());
		this.currNodeList = new ArrayList<>(allVars.get(ctx.NAME().getText()));
		return currNodeList;
	}

	@Override public List<Node> visitXqJoin(XQueryParser.XqJoinContext ctx) {
        List<TerminalNode> leftVarList = ctx.varList(0).NAME();
        List<TerminalNode> rightVarList = ctx.varList(1).NAME();

        List<Node> res = new ArrayList<Node>();
        List<Node> copy = new ArrayList<>(currNodeList);

        List<Node> left = visit(ctx.xq(0));
        this.currNodeList = copy;
        List<Node> right = visit(ctx.xq(1));

        if (leftVarList.size() == 0 && rightVarList.size() == 0) {
            for (Node leftNode : left) {
                List<Node> leftChildren = MyVisitorHelper.getAllChildren(leftNode);
                for (Node rightNode : right) {
                    List<Node> rightChildren = MyVisitorHelper.getAllChildren(rightNode);
                    List<Node> all = new ArrayList<Node>();
                    all.addAll(leftChildren);
                    all.addAll(rightChildren);
                    res.add(makeEle(xmlDoc, "tuple", all));
                }
            }
        } else {
            // use the shorter list to build hashmap
            if (right.size() < left.size()) {
                List<Node> tmp = left;
                left = right;
                right = tmp;
                List<TerminalNode> tmp2 = leftVarList;
                leftVarList = rightVarList;
                rightVarList = tmp2;
            }

            HashMap<Integer, ArrayList<Node>> varMap = new HashMap<>();

            // fill in the hashmap using the shorter list of nodes
            for (Node node : left) {
                StringBuilder keyString = new StringBuilder();
                List<Node> children = MyVisitorHelper.getAllChildren(node);

                //loop over listed column names in [], following this order
                for (TerminalNode colVal : leftVarList) {
                    //for each of the columns of this record, search for the column name
                    for (Node child : children) {
                        System.out.println(child.getNodeName());
                        System.out.println(colVal.toString());
                        if (colVal.toString().equals(child.getNodeName())) {
                            keyString.append(changeNodeToString(child));
                        }
                    }
                }

                int key = keyString.toString().hashCode();

                if (varMap.containsKey(key)) {
                    varMap.get(key).add(node);
                } else {
                    ArrayList<Node> newList = new ArrayList<Node>();
                    newList.add(node);
                    varMap.put(key, newList);
                }
            }

            // loop over the other list of nodes
            for (Node node : right) {
                StringBuilder keyString = new StringBuilder();
                List<Node> children = MyVisitorHelper.getAllChildren(node);

                //loop over listed column names in [], following this order
                for (TerminalNode colVal : rightVarList) {
                    //for each of the columns of this record, search for the column name
                    for (Node child : children) {
                        System.out.println(child.getNodeName());
                        System.out.println(colVal.toString());
                        if (colVal.toString().equals(child.getNodeName())) {
                            keyString.append(changeNodeToString(child));
                        }
                    }
                }

                int key = keyString.toString().hashCode();

                // check if this key exists in hashMap, if so create a joined record
                if (varMap.containsKey(key)) {
                    List<Node> leftNodeList = varMap.get(key);

                    for (Node leftNode : leftNodeList) {
                        List<Node> leftNodeChildren = MyVisitorHelper.getAllChildren(leftNode);
                        List<Node> all = new ArrayList<Node>();
                        all.addAll(leftNodeChildren);
                        all.addAll(children);
                        res.add(makeEle(xmlDoc, "tuple", all));
                    }
                }
            }
            for (Node n : res) {
                System.out.println(n.getTextContent());
            }
        }

        return res;
    }

	@Override public List<Node> visitVarList(XQueryParser.VarListContext ctx) {
		return visitChildren(ctx);
	}
}
