package edu.ucsd.XQuery.visitor.optimize;

import java.util.*;

import edu.ucsd.XQuery.visitor.XQueryParser;
import org.antlr.v4.runtime.tree.ParseTree;

public class TwoWayJoin {

    /**
     * rewrite original XQuery into XQuery with join-clause
     * @param xq
     * @return when can't rewrite, return null
     */
	public static String rewrite(ParseTree xq) {	
		int flwrSize = xq.getChildCount();
		if (!canRewrite(xq, flwrSize))	return null;
		
		Map<String, Integer> var2table = new HashMap<>();
		List<List<String>> tables = new ArrayList<>();
		if (!divideVarsIntoTables(xq.getChild(0), var2table, tables))
			return null; // because there is only one table joined, can't rewrite to join-clause in this case.
		
		Map<Integer, List<String[]>> joinOnConst = new HashMap<>();
		Map<String, List<String[]>> joinBetweenTables = new HashMap<>();
        int[] isolatedTables = new int[tables.size()];

		if(!divideConditionsIntoTables(xq.getChild(1), var2table, joinOnConst, joinBetweenTables, isolatedTables)
				|| joinBetweenTables.size() == 0) // can't or needn't rewrite
			return null;
		
		Map<Integer, String> rewrittenTables = rewriteTables(joinOnConst, tables);

		StringBuffer sbuf = new StringBuffer("for $tuple in ");
        sbuf.append(createJoin(joinBetweenTables, rewrittenTables, isolatedTables, tables.size()));
        sbuf.append("\nreturn\n").append(createReturn(xq.getChild(2).getChild(1)));
        sbuf.setLength(sbuf.length()-2); // remove tail , and \n
		return sbuf.toString();
	}

	private static String createReturn(ParseTree tree) {
		System.out.println(tree.getText());
		XQueryParser.XqResultContext cx = (XQueryParser.XqResultContext)tree;
        List<ParseTree> children = getAllXq(cx.xq());

		StringBuffer sbuf = new StringBuffer();
        String tagName = cx.NAME(0).getText();
        String[] vars = cx.xq().getText().split(",");
        sbuf.append("<").append(tagName).append("> {");

        for (ParseTree child: children) {
			System.out.println(child.getText());
			if (child.getText().startsWith("$")) {
				int index = child.getText().indexOf("/");
				if (index != -1) {
					sbuf.append("$tuple/")
							.append(child.getText().substring(1, index))
							.append("/*")
							.append(child.getText().substring(index))
							.append(",\n");
				} else {
					sbuf.append("$tuple/")
							.append(child.getText().substring(1))
							.append("/*,\n");
				}
			} else {
        		sbuf.append(createReturn(child));
			}
		}
        sbuf.setLength(sbuf.length()-2);
        sbuf.append("} </").append(tagName).append(">,\n");
        return sbuf.toString();
    }

    private static List<ParseTree> getAllXq(ParseTree tree) {
		int nChildren = tree.getChildCount();
		List<ParseTree> xqList = new ArrayList<>();
		if (nChildren == 3 && tree.getChild(1).getText().equals(",")) { // xqComma
			xqList.addAll(getAllXq(tree.getChild(0)));
			xqList.add(tree.getChild(2));
		}
		else // xqVar or xqResult
			xqList.add(tree);
		return xqList;
	}

	/**
	 * determine whether the XQuery can be rewritten. XQuery can't be rewritten if it is not required FLWR
	 * @param xq
	 * @param size
	 * @return
	 */
	private static boolean canRewrite(ParseTree xq, int size) {
		if (size != 3)	return false;
		if (size > 0 && !xq.getChild(0).getText().contains("for"))	return false;
		if (size > 1 && !xq.getChild(1).getText().contains("where"))	return false;
		if (size > 1 && !xq.getChild(2).getText().contains("return"))	return false;
		return true;
	}
	
	/**
	 * rewrite the query for every table and facilitate the following creation of join query
	 * @param joinOnConst
	 * @param tables
	 * @return
	 */
	private static Map<Integer, String> rewriteTables(Map<Integer, List<String[]>> joinOnConst, List<List<String>> tables) {
		Map<Integer, String> res = new HashMap<>();
		int size = tables.size();
		for (int i = 0; i < size; i++) {
			StringBuffer sb = new StringBuffer("for ");
			for (String str: tables.get(i)) {
				sb.append(str + ",\n");
			}
			sb.setLength(sb.length()-2);
			if (joinOnConst.containsKey(i)) {
				sb.append("\nwhere ");
				for (String[] pair: joinOnConst.get(i)) {
					sb.append(pair[0]).append(" eq ").append(pair[1]).append(",");
				}
				sb.setLength(sb.length()-1);
			}
			sb.append("\nreturn <tuple>{\n");
			for (String str: tables.get(i)) {
				String var = str.split(" ")[0]; //starts with $
				sb.append("<").append(var.substring(1, var.length())).append(">");
				sb.append("{").append(var).append("}");
				sb.append("</").append(var.substring(1, var.length())).append(">,\n");
			}
			sb.setLength(sb.length()-2);
			sb.append("\n}</tuple>");
			res.put(i, sb.toString());
		}
		return res;
	}

    /**
     * for every element in <code>joinBetweenTables</code>, rewrite join-clause. After it is done, what remains in
     * <code>rewrittenTables</code> is tables that join with no one. Then rewrite join-clause in form of Cartesian product,
     * namely, leaving join condition empty.
     * @param joinBetweenTables
     * @param rewrittenTables
     * @param nTables number of tables
     * @param isolatedTables if i-th element is 1, indicates i-th table is isolated table. If 0, then not isolated table.
     *
     * @return
     */
	private static String createJoin(Map<String, List<String[]>> joinBetweenTables, Map<Integer, String> rewrittenTables,
                                     int[] isolatedTables, int nTables) {
		int[] clusters = new int[nTables];
		int nClusters = identifyClusters(clusters, joinBetweenTables);
        String[] joinForClusters = new String[nClusters]; // the results after join for tables in the same cluster
        for(int i = 0; i < nTables; i++) { //since isolated tables don't join, the join result should be itself in rewrittenTables.
            if(isolatedTables[i] == 1) {
                joinForClusters[clusters[i]] = rewrittenTables.get(i);
            }
        }

        List<Set<Integer>> list = new ArrayList<>();

        for(String joinId: joinBetweenTables.keySet()) {
            List<String[]> vars = joinBetweenTables.get(joinId);
            String[] parts = joinId.split("-");
            int id1 = Integer.parseInt(parts[0]);
            int id2 = Integer.parseInt(parts[1]);

            String join = createSubJoin(rewrittenTables.get(id1), rewrittenTables.get(id2), vars);
            int flag1 = -1, flag2 = -1;
            for(int i = 0; i < list.size(); i++) {
                Set<Integer> set = list.get(i);
                if(set.contains(id1)) flag1 = i;
                if(set.contains(id2)) flag2 = i;
                if(flag1 != -1 && flag2 != -1) break;
            }
            if (flag1 == -1 && flag2 == -1) {
                Set<Integer> newSet = new HashSet<>();
                newSet.add(id1);
                newSet.add(id2);
                list.add(newSet);
                flag1 = list.size()-1;
            }
            else if (flag1 == -1 && flag2 != -1) {
                Set<Integer> set = list.get(flag2);
                set.add(id1);
                flag1 = flag2;
            }
            else if (flag1 != -1 && flag2 == -1) {
                Set<Integer> set = list.get(flag1);
                set.add(id2);
            }
            else {
                Set<Integer> set = list.get(flag1);
                set.addAll(list.get(flag2));
                list.remove(flag2);
            }
            Set<Integer> set = list.get(flag1);
            for (int id : set) {
                rewrittenTables.put(id, join);
            }
            joinForClusters[clusters[id1]] = join;
        }

        if (nClusters > 1) { //multiple non-overlapped clusters, join them on empty condition, namely, Cartesian product
            String join = joinForClusters[0];
            for (int i = 1; i < nClusters; i++) {
                join = createCartesianProduct(join, joinForClusters[i]);
            }
            return join;
        }
        return joinForClusters[0];
	}

	private static int identifyClusters(int[] clusters, Map<String, List<String[]>> joinBetweenTables) {
	    int[] parent = new int[clusters.length];
        int count = parent.length;
	    for(int i = 0; i < parent.length; i++)
            parent[i] = i;
        for(String key: joinBetweenTables.keySet()) {
            String[] parts = key.split("-");
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            parent[first] = find(parent, second);
            count--;
        }

        Map<Integer, Integer> map = new HashMap<>();
        for(int i = 0; i < parent.length; i++) {
            int pa = find(parent, i);
            if(!map.containsKey(pa))
                map.put(pa, map.size());
            clusters[i] = map.get(pa);
        }
        return count;
    }

    private static int find(int[] parent, int i) {
	    if(parent[i] != i) {
	        parent[i] = find(parent, parent[i]);
        }
        return parent[i];
    }

    /**
     * rewrite into a join-clause with input two strings
     * @param sub1
     * @param sub2
     * @param vars variables on which two tables join
     * @return
     */
	private static String createSubJoin(String sub1, String sub2, List<String[]> vars) {
		StringBuffer sb = new StringBuffer("join(");
		sb.append(sub1).append(",\n").append(sub2).append(",");
		sb.append("[");
		for(String[] temp: vars)
			sb.append(temp[0].substring(1)).append(",");
		sb.setLength(sb.length()-1);
		sb.append("],[");
		for(String[] temp: vars)
			sb.append(temp[1].substring(1)).append(",");
		sb.setLength(sb.length()-1);
		sb.append("])");
		return sb.toString();
	}

	private static String createCartesianProduct(String sub1, String sub2) {
        StringBuffer sb = new StringBuffer("join(");
        sb.append(sub1).append(",\n").append(sub2).append(",");
        sb.append("[],[])");
        return sb.toString();
    }

	/**
	 * group variables into multiple tables to facilitate the following operation of creating join
	 * @param forCx context of for clause
	 * @param var2table map to subject every variable to its table id
	 * @param tables every element of it will form a table
	 */
	private static boolean divideVarsIntoTables(ParseTree forCx, Map<String, Integer> var2table, List<List<String>> tables) {
		int size = forCx.getChildCount();
		for(int i = 1; i < size; i++) {
			String var = forCx.getChild(i).getText() + forCx.getChild(i+1).getText();
			String path = forCx.getChild(i+3).getText();
			String content = var + " " + forCx.getChild(i+2).getText() + " " + forCx.getChild(i+3).getText();
			if (path.startsWith("doc")) {
				var2table.put(var, tables.size());
				List<String> temp = new ArrayList<>();
				temp.add(content);
				tables.add(temp);
			}
			else if (path.startsWith("$")){
				int index = path.indexOf("/");
				String var2 = (index != -1)? path.substring(0, index): path;
				if (var2table.containsKey(var2)) {
					int groupId = var2table.get(var2);
					var2table.put(var, groupId);
					tables.get(groupId).add(content);
				}
				else {
					System.out.println(var2 + " is used before it is defined");
				}
			}
			i += 4;
		}
		if (tables.size() < 2)
			return false;
		return true;
	}
	
	/**
	 *
     *Some tables may do a selection (joinOnConst).
     *Tables are divided into two separated types, those join with another table(joinBetweenTables),
     *those join with no one (<code> tables </code> minus those in joinBetweenTables)
     *
	 * @param whereCx
	 * @param var2table
	 * @param joinOnConst  key indicates which table needs a selection, ex. "1". value is list of variable and constant string pairs
	 * @param joinBetweenTables key indicates which two tables are joining, ex. "1-2". value is the variables which join on
     * @param isolatedTables id of tables that join with no one
	 * @return
	 */
	private static boolean divideConditionsIntoTables(ParseTree whereCx, Map<String, Integer> var2table,
                    Map<Integer, List<String[]>> joinOnConst, Map<String, List<String[]>> joinBetweenTables,
                    int[] isolatedTables) {
	    for (int i = 0; i < isolatedTables.length; i++)
	        isolatedTables[i] = 1;

	    whereCx = whereCx.getChild(1);
		int size = whereCx.getChildCount();

		List<ParseTree> conditions = new ArrayList<>();
		while(whereCx.getText().contains("and") && whereCx.getChildCount() == 3) {
			conditions.add(whereCx.getChild(2));
			if (!whereCx.getChild(1).getText().equals("and")) return false;
			whereCx = whereCx.getChild(0);
		}
		conditions.add(whereCx);
		
		for(int j = conditions.size()-1; j >= 0; j--) {
			whereCx = conditions.get(j);
			if(whereCx.getChild(0).getText().startsWith("$")) {
				String var1 = whereCx.getChild(0).getText();
				int table1 = var2table.get(var1);

                if (!whereCx.getChild(1).getText().equals("eq")
						&& !whereCx.getChild(1).getText().equals("=")) return false;
				if (whereCx.getChild(2).getText().startsWith("$")) { //$var1 eq $var2
					String var2 = whereCx.getChild(2).getText();
					int table2 = var2table.get(var2);
					isolatedTables[table1] = 0;
					isolatedTables[table2] = 0;

					String key = (table1 < table2)? table1+"-"+table2: table2+"-"+table1;
					String[] val = (table1 < table2)? new String[]{var1, var2}: new String[]{var2, var1};
					if (!joinBetweenTables.containsKey(key))
						joinBetweenTables.put(key, new ArrayList<>());
					joinBetweenTables.get(key).add(val);
				}
				else if (whereCx.getChild(2).getText().startsWith("\"")) { //$var1 eq constant
					String cons = whereCx.getChild(2).getText();
					if (!joinOnConst.containsKey(table1))
						joinOnConst.put(table1, new ArrayList<>());
					joinOnConst.get(table1).add(new String[]{var1, cons});
				}
				else return false;
			}
			else if(whereCx.getChild(0).getText().startsWith("\"")) {
				String cons1 = whereCx.getChild(0).getText();
				if (!whereCx.getChild(1).getText().equals("eq")) return false;
				if (whereCx.getChild(2).getText().startsWith("$")) { //constant eq $var2
					String var2 = whereCx.getChild(2).getText();
					int table2 = var2table.get(var2);

                    if (!joinOnConst.containsKey(table2))
						joinOnConst.put(table2, new ArrayList<>());
					joinOnConst.get(table2).add(new String[]{var2, cons1});
				}
				else if (whereCx.getChild(2).getText().startsWith("\"")) { //constant1 eq constant2
					String cons2 = whereCx.getChild(2).getText();
					if (!cons1.equals(cons2)) {
						joinOnConst.clear();
						joinBetweenTables.clear();
						return true;
					}
					for(String tempVar : var2table.keySet()) { //regard it as selecting a variable on certain constant
						if (!joinOnConst.containsKey(var2table.get(tempVar)))
							joinOnConst.put(var2table.get(tempVar), new ArrayList<>());
						joinOnConst.get(tempVar).add(new String[]{cons1, cons2});
						break;
					}
				}
				else return false;
			}
		}
		return true;
	}
}
