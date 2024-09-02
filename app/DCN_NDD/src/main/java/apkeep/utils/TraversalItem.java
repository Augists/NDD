package apkeep.utils;

import java.util.HashMap;
import java.util.HashSet;

public class TraversalItem {
	public String nodeName;
	public HashMap<String,HashSet<Integer>> sourceNode_aps;
	public HashSet<Integer> atoms;
	public TraversalItem(String name, HashSet<Integer> aps) {
		// TODO Auto-generated constructor stub
		nodeName = name;
		sourceNode_aps = new HashMap<String,HashSet<Integer>>();
		atoms = new HashSet<Integer>(aps);
	}

	@Override
	public String toString() {
		return nodeName+" "+atoms.toString();
	}
	@Override
	public boolean equals(Object o) {
		if(o instanceof TraversalItem) {
			TraversalItem another = (TraversalItem) o;
			if(!another.nodeName.equals(nodeName)) return false;
			if(!another.sourceNode_aps.equals(sourceNode_aps)) return false;
			if(!another.atoms.equals(atoms)) return false;
			return true;
		}
		return false;
	}
	@Override
	public int hashCode() {
		return nodeName.hashCode()+sourceNode_aps.hashCode()+atoms.hashCode();
	}

	public void updateSourceMap(TraversalItem last_hop, HashSet<Integer> aps) {
		// TODO Auto-generated method stub
		for(String src : last_hop.sourceNode_aps.keySet()) {
			HashSet<Integer> src_aps = new HashSet<Integer>(last_hop.sourceNode_aps.get(src));
			src_aps.retainAll(aps);
			if(src_aps.isEmpty()) continue;
			HashSet<Integer> src_atoms = sourceNode_aps.get(src);
			if(src_atoms == null) {
				src_atoms = new HashSet<Integer>();
			}
			src_atoms.addAll(src_aps);
			sourceNode_aps.put(src, src_atoms);
		}
	}

	public void setSourceMap(String node_name, HashSet<Integer> aps) {
		// TODO Auto-generated method stub
		sourceNode_aps.put(node_name, new HashSet<Integer>(aps));
	}
}
