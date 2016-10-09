/*
**    Copyright (C) 2003-2014 Institute for Systems Biology 
**                            Seattle, Washington, USA. 
**
**    This library is free software; you can redistribute it and/or
**    modify it under the terms of the GNU Lesser General Public
**    License as published by the Free Software Foundation; either
**    version 2.1 of the License, or (at your option) any later version.
**
**    This library is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
**    Lesser General Public License for more details.
**
**    You should have received a copy of the GNU Lesser General Public
**    License along with this library; if not, write to the Free Software
**    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.systemsbiology.biofabric.layouts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import org.systemsbiology.biofabric.analysis.GraphSearcher;
import org.systemsbiology.biofabric.io.AttributeLoader;
import org.systemsbiology.biofabric.model.BioFabricNetwork;
import org.systemsbiology.biofabric.model.FabricLink;
import org.systemsbiology.biofabric.util.AsynchExitRequestException;
import org.systemsbiology.biofabric.util.BTProgressMonitor;
import org.systemsbiology.biofabric.util.ResourceManager;
import org.systemsbiology.biofabric.util.TrueObjChoiceContent;
import org.systemsbiology.biofabric.util.UiUtil;

/****************************************************************************
**
** This does node clustering layout
*/

public class NodeClusterLayout {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public NodeClusterLayout() { 
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  */
  
   public void orderByClusterAssignment(BioFabricNetwork.RelayoutBuildData rbd, 
                                        NodeSimilarityLayout.CRParams crParams,
                                        BTProgressMonitor monitor, 
                                        double startFrac, double endFrac) throws AsynchExitRequestException { 
     
    //
    // Go through all the links. If a link source and target are both in the same cluster, we add the link to the cluster
    //
  	 
  	ClusterParams params = (ClusterParams)crParams;
    
    TreeMap<String, BioFabricNetwork.RelayoutBuildData> perClust = new TreeMap<String, BioFabricNetwork.RelayoutBuildData>();
    HashMap<Tuple, List<FabricLink>> interClust = new HashMap<Tuple, List<FabricLink>>();
    HashSet<String> interNodesOnly = new HashSet<String>();
    
    HashMap<String, Integer> fullNodeDegree = null;
    if (params.cLay == ClusterParams.ClustLayout.BREADTH_CONN_FIRST) {
      fullNodeDegree = new HashMap<String, Integer>();
    } 	
	
    
    BioFabricNetwork.BuildMode intraLay = (params.cLay == ClusterParams.ClustLayout.SIMILAR) ? BioFabricNetwork.BuildMode.CLUSTERED_LAYOUT
            																																								 : BioFabricNetwork.BuildMode.DEFAULT_LAYOUT;
      
    Iterator<FabricLink> flit = rbd.allLinks.iterator();
    while (flit.hasNext()) {
      FabricLink fl = flit.next();
      String source = fl.getSrc().toUpperCase();
      String target = fl.getTrg().toUpperCase();
      if (fullNodeDegree != null) {
      	Integer count = fullNodeDegree.get(source);
        count = (count == null) ? Integer.valueOf(1) : Integer.valueOf(count.intValue() + 1);
        fullNodeDegree.put(source, count);   
        count = fullNodeDegree.get(target);
        count = (count == null) ? Integer.valueOf(1) : Integer.valueOf(count.intValue() + 1);
        fullNodeDegree.put(target, count);      	       
      }    
      
      String srcClust = params.getClusterForNode(source);
      String trgClust = params.getClusterForNode(target);
      if (srcClust.equals(trgClust)) {
        BioFabricNetwork.RelayoutBuildData rbdpc = perClust.get(srcClust);
        if (rbdpc == null) {
          rbdpc = new BioFabricNetwork.RelayoutBuildData(new HashSet<FabricLink>(), new HashSet<String>(), rbd.colGen, intraLay);
          rbdpc.allNodes = new HashSet<String>();
          perClust.put(srcClust, rbdpc);
        }
        rbdpc.allLinks.add(fl);
        rbdpc.allNodes.add(fl.getSrc());
        rbdpc.allNodes.add(fl.getTrg());
      } else {
        Tuple intup = new Tuple(srcClust, trgClust); // Tuples reordered so val1 < val2!
        List<FabricLink> icfl = interClust.get(intup);
        if (icfl == null) {
          icfl = new ArrayList<FabricLink>();
          interClust.put(intup, icfl);
        }
        if (fl.getTrg().equals("Valjean")) {
//        	System.out.println("Link " + fl + " for " + intup + " with " + trgClust + " " + target);
        }
        icfl.add(fl);
        interNodesOnly.add(fl.getSrc());
        interNodesOnly.add(fl.getTrg());
      }
    }
    
    //
    // Need to deal with "clusters" of nodes that have no internal links!
    //  
    
    for (String node : rbd.allNodes) {
    	String clust = params.getClusterForNode(node.toUpperCase());
    	BioFabricNetwork.RelayoutBuildData rbdpc = perClust.get(clust);
    	if (rbdpc == null) {
    	  rbdpc  = new BioFabricNetwork.RelayoutBuildData(new HashSet<FabricLink>(), new HashSet<String>(), rbd.colGen, intraLay);
        rbdpc.allNodes = new HashSet<String>();
        perClust.put(clust, rbdpc);
    	}  		
      rbdpc.allNodes.add(node);	
    }
    
    String startClust = (params.startNode != null) ? params.getClusterForNode(params.startNode.toUpperCase()) : null;
    
    List<String> bfc;
    switch (params.order) {
    	case BREADTH:
        bfc = breadthFirstClustering(params, interClust.keySet(), startClust);
        break;
    	case LINK_SIZE:
    		bfc = clusterSizeOrder(perClust, false, startClust);
    		break;  		
    	case NODE_SIZE:
    	  bfc = clusterSizeOrder(perClust, true, startClust);
    		break;
    	case NAME:
    	  bfc = new ArrayList<String>(perClust.keySet());
    	  if (startClust != null) {
    	    bfc.remove(startClust);
    	    bfc.add(0, startClust);
        }
    	  break;
    	default:
    		throw new IllegalStateException();
    }
    
    Map<String, List<String>> hubs = null;
    if (params.cLay == ClusterParams.ClustLayout.BREADTH_CONN_FIRST) {
      hubs = rankInterClustHubs(interClust, params, fullNodeDegree);	
    }

    ArrayList<String> allTargets = new ArrayList<String>();

    Iterator<String> pcit = bfc.iterator(); 
    while (pcit.hasNext()) {
    	String clustName = pcit.next();
      BioFabricNetwork.RelayoutBuildData pcrbd = perClust.get(clustName);
      if (pcrbd == null) {
      	continue;
      }
      List<String> targets;
      if (intraLay == BioFabricNetwork.BuildMode.CLUSTERED_LAYOUT) {
        NodeSimilarityLayout.CRParams crp = new NodeSimilarityLayout.ClusterParams();
    	  NodeSimilarityLayout nslLayout = new NodeSimilarityLayout();
    	  pcrbd.existingOrder = new ArrayList<String>(pcrbd.allNodes);
    	 // FAIL
        targets = nslLayout.doClusteredLayoutOrder(pcrbd, crp, monitor, startFrac, endFrac);
      } else {
        DefaultLayout dl = new DefaultLayout();
        List<String> starts = (hubs == null) ? null : hubs.get(clustName);
        targets = dl.defaultNodeOrder(pcrbd.allLinks, pcrbd.loneNodes, starts);  
      }
      allTargets.addAll(targets);
    }
    interNodesOnly.removeAll(allTargets);
    allTargets.addAll(interNodesOnly);
    
    (new DefaultLayout()).installNodeOrder(allTargets, rbd);
    
    (new DefaultEdgeLayout()).layoutEdges(rbd);
    
    if (params.iLink == ClusterParams.InterLink.BETWEEN) {
    	int origNum = rbd.linkOrder.size();
    	TreeMap<Integer, FabricLink> newOrder = new TreeMap<Integer, FabricLink>();
    	HashMap<String, List<FabricLink>> holdEm = new HashMap<String, List<FabricLink>>();
    	Iterator<FabricLink> ksit = rbd.linkOrder.values().iterator();
    	int colCount = 0;
    	String currClust = null;
    	boolean drainTime = false;
    	boolean interClustLink = false;
    	while (ksit.hasNext()) {
    		FabricLink daLink = ksit.next();
    		String srcClust = params.getClusterForNode(daLink.getSrc().toUpperCase());
        String trgClust = params.getClusterForNode(daLink.getTrg().toUpperCase());
        if (srcClust.equals(trgClust)) {
        	if ((currClust != null) && !currClust.equals(srcClust)) {
        		drainTime = true;
        	}
        	currClust = srcClust;
        	interClustLink = false;
        } else {
        	interClustLink = true;
        }
        
        if (drainTime) {
        	List<FabricLink> toDrain = holdEm.get(currClust);
        	if (toDrain != null) {
	        	for (FabricLink ihe : toDrain) {
	    	  		newOrder.put(Integer.valueOf(colCount++), ihe);   	  		
	    	  	}
        	}
        	holdEm.remove(currClust);
    	  	drainTime = false; 
    	  	newOrder.put(Integer.valueOf(colCount++), daLink);
        } else if (!interClustLink) {
        	currClust = srcClust;
    		  newOrder.put(Integer.valueOf(colCount++), daLink);
    	  } else {
    	  	String otherClust = srcClust.equals(currClust) ? trgClust : srcClust;
    	  	List<FabricLink> toDefer = holdEm.get(otherClust);
    	  	if (toDefer == null) {
    	  		toDefer = new ArrayList<FabricLink>();
    	  		holdEm.put(otherClust, toDefer);
    	  	}
    	  	toDefer.add(daLink);  	  	
    	  }
    	}
    	
    	 UiUtil.fixMePrintout("Cluster drain order is not correct");
    	for (String daClust : holdEm.keySet()) {
	      List<FabricLink> toDrain = holdEm.get(daClust);
	    	if (toDrain != null) {
	      	for (FabricLink ihe : toDrain) {
	  	  		newOrder.put(Integer.valueOf(colCount++), ihe);   	  		
	  	  	}
	    	}
    	}
    	if (newOrder.size() != origNum) {
    		throw new IllegalStateException();
    	}
    	rbd.setLinkOrder(newOrder);
    }
    if (params.saveAssign) {
    	rbd.clustAssign = params.getClusterAssign(); 
    }
    return;
  }
 
  /***************************************************************************
  **
  ** Count map maintenance
  */
  
  private void addToCount(Map<String, Map<String, Integer>> countMap, String clust, String node) { 
 	
    Map<String, Integer> perClust = countMap.get(clust);
    if (perClust == null) {
    	perClust = new HashMap<String, Integer>();
    	countMap.put(clust, perClust);
    }
    Integer count = perClust.get(node);
    count = (count == null) ? Integer.valueOf(1) : Integer.valueOf(count.intValue() + 1);
    perClust.put(node, count);
    return;
  }
   
   
  /***************************************************************************
  **
  ** The nodes that are responsible for inter-cluster links may be almost no-shows
  ** in the intra-cluster link competition. Find those guys so we can put them first
  ** in the pile.
  */
  
  private Map<String, List<String>> rankInterClustHubs(Map<Tuple, List<FabricLink>> interLinks, ClusterParams params, 
  		                                                 Map<String, Integer> fullNodeDegree) { 
 	
  	// Map(ClusterID -> Map(NodeID, InterClustDegree)):
  	Map<String, Map<String, Integer>> preRetval = new HashMap<String, Map<String, Integer>>();
  	for (Tuple tup : interLinks.keySet()) {
  	  List<FabricLink> forTup = interLinks.get(tup);
  	  for (FabricLink link : forTup) {
  	  	// Tup order is NOT src, trg, but lo, hi:
  	    addToCount(preRetval, params.getClusterForNode(link.getSrc().toUpperCase()), link.getSrc());
  	    addToCount(preRetval, params.getClusterForNode(link.getTrg().toUpperCase()), link.getTrg());
  	  }
  	}
  	
  	// Map(ClusterID -> List(NodeID)):
    Map<String, List<String>> retval = new HashMap<String, List<String>>();  
    for (String clust: preRetval.keySet()) {
    	Map<String, Integer> degMap = preRetval.get(clust);
    	// Map(SortedInterClustDegree->Map(SortedFullNodeDegree, Set(SortedNodeName))):
    	TreeMap<Integer, SortedMap<Integer, SortedSet<String>>> invert = 
      	new TreeMap<Integer, SortedMap<Integer, SortedSet<String>>>(Collections.reverseOrder());
    	for (String node : degMap.keySet()) {
    		Integer InterClustDeg = degMap.get(node);
    		SortedMap<Integer, SortedSet<String>> perInterDeg = invert.get(InterClustDeg);
    		if (perInterDeg == null) {
    			perInterDeg = new TreeMap<Integer, SortedSet<String>>(Collections.reverseOrder());
    			invert.put(InterClustDeg, perInterDeg);
    		}
    		Integer fnDeg = fullNodeDegree.get(node.toUpperCase());
    		SortedSet<String> perFullDeg = perInterDeg.get(fnDeg);
    		if (perFullDeg == null) {
    			perFullDeg = new TreeSet<String>();
    			perInterDeg.put(fnDeg, perFullDeg);
    		}
    		perFullDeg.add(node);
    	}
    	List<String> flat = new ArrayList<String>();
      for (SortedMap<Integer, SortedSet<String>> perInterDeg : invert.values()) {
      	for (SortedSet<String> perFull : perInterDeg.values()) {
      	  flat.addAll(perFull);
        }
      }
    	retval.put(clust, flat);
    }
  	return (retval);
  }

  
  /***************************************************************************
  **
  */
  
  private List<String> clusterSizeOrder(TreeMap<String, BioFabricNetwork.RelayoutBuildData> perClust, boolean nodeFirst, String startClust) { 
  	
  	TreeMap<Integer, SortedMap<Integer, SortedSet<String>>> preRet = 
  		new TreeMap<Integer, SortedMap<Integer, SortedSet<String>>>(Collections.reverseOrder());
  	for (String clustName : perClust.keySet()) {
  	  BioFabricNetwork.RelayoutBuildData rbdpc = perClust.get(clustName);
  	  Integer size1 = Integer.valueOf((nodeFirst) ? rbdpc.allNodes.size() : rbdpc.allLinks.size());
  	  SortedMap<Integer, SortedSet<String>> forSize1 = preRet.get(size1);
  	  if (forSize1 == null) {
  	  	forSize1 = new TreeMap<Integer, SortedSet<String>>(Collections.reverseOrder());
  	  	preRet.put(size1, forSize1);
  	  }
  	  Integer size2 = Integer.valueOf((nodeFirst) ? rbdpc.allLinks.size() : rbdpc.allNodes.size());
  	  SortedSet<String> forSize2 = forSize1.get(size2);
  	  if (forSize2 == null) {
  	  	forSize2 = new TreeSet<String>();
  	  	forSize1.put(size2, forSize2);
  	  }
  	  forSize2.add(clustName); 	
  	}
  	
  	List<String> flat = new ArrayList<String>();
    for (SortedMap<Integer, SortedSet<String>> perSize1 : preRet.values()) {
    	for (SortedSet<String> perSize2 : perSize1.values()) {
    	  flat.addAll(perSize2);
      }
    }
    
    if (startClust != null) {
    	flat.remove(startClust);
    	flat.add(0, startClust);
    }
    
    return (flat);
  }

  /***************************************************************************
  **
  */
  
  private List<String> breadthFirstClustering(ClusterParams params, Set<Tuple> iclinks, String startClust) { 
    
  	Set<String> clusters = new HashSet<String>(params.getClusters());
  	Set<FabricLink> links = new HashSet<FabricLink>();
  	for (Tuple tup : iclinks) {
  		FabricLink pseudo = new FabricLink(tup.getVal1(), tup.getVal2(), "ic", Boolean.valueOf(false), Boolean.valueOf(false));
  		links.add(pseudo);
  		pseudo = new FabricLink(tup.getVal2(), tup.getVal1(), "ic", Boolean.valueOf(false), Boolean.valueOf(false));
  		links.add(pseudo);
  	}
  	
  	List<String> useRoots = null;
  	if (startClust != null) {
  		useRoots = new ArrayList<String>();
  		useRoots.add(startClust);
  	}
    GraphSearcher gs = new GraphSearcher(clusters, links);
    List<GraphSearcher.QueueEntry> qes = gs.breadthSearch(useRoots);
    ArrayList<String> retval = new ArrayList<String>(); 
    for (GraphSearcher.QueueEntry aqe : qes) {
      retval.add(aqe.name);
  	}
    return (retval);  
  }
   
  /***************************************************************************
  **
  ** For passing around layout params
  */  
  
  public static class ClusterParams implements NodeSimilarityLayout.CRParams {
        
    public enum Source {STORED, FILE, PLUGIN};
    
    public enum Order {NAME, NODE_SIZE, LINK_SIZE, BREADTH};
    
    public enum InterLink {INLINE, BETWEEN};
    
    public enum ClustLayout {BREADTH_CONN_FIRST, BREADTH, SIMILAR};
    
          
    public Source source;
    public Order order;
    public InterLink iLink;
    public ClustLayout cLay;
    public String startNode;
    public boolean saveAssign;
    private Map<String, String> nodeClusters_;

    public ClusterParams(Source source, Order order, InterLink iLink, ClustLayout cLay, String startNode, 
    		                 Map<AttributeLoader.AttributeKey, String> nodeClusterAttributes, boolean saveAssign) {
    	
    	if (nodeClusterAttributes != null) {
    	  nodeClusters_ = new HashMap<String, String>();
        for (AttributeLoader.AttributeKey key : nodeClusterAttributes.keySet()) {
          nodeClusters_.put(((AttributeLoader.StringKey)key).key, nodeClusterAttributes.get(key));
        }
      }
      this.source = source;
      this.order = order;
      this.saveAssign = saveAssign;
    }
 
    public ClusterParams(boolean stored) {
      this.source = (stored) ? Source.STORED : Source.FILE;
      this.order = Order.BREADTH;
      this.iLink = InterLink.BETWEEN;
      this.cLay = ClustLayout.BREADTH_CONN_FIRST;
      this.startNode = null;
      this.saveAssign = true;
    }
    
    public boolean needsFile() {
    	return (source.equals(Source.FILE));
    }

    public void install(Map<AttributeLoader.AttributeKey, String> nodeClusterAttributes) {   	
    	if (nodeClusterAttributes != null) {
    	  nodeClusters_ = new HashMap<String, String>();
        for (AttributeLoader.AttributeKey key : nodeClusterAttributes.keySet()) {
          nodeClusters_.put(((AttributeLoader.StringKey)key).key, nodeClusterAttributes.get(key));
        }
      }
    }
    
    public void assign(Map<String, String> nodeClusterAssign) {   	
    	nodeClusters_ = nodeClusterAssign;
      return;
    }
 
    public String getClusterForNode(String nodeID) {   	
    	return (nodeClusters_.get(nodeID));
    }  
    
    public Map<String, String> getClusterAssign() {   	
    	return (nodeClusters_);
    }  
    
    
    public Collection<String> getClusters() {   	
    	return (nodeClusters_.values());
    }    
    
    public static Vector<TrueObjChoiceContent<Source>> getSourceChoices(boolean stored) {
      ResourceManager rMan = ResourceManager.getManager();
      Vector<TrueObjChoiceContent<Source>> retval = new Vector<TrueObjChoiceContent<Source>>();
      if (stored) {
      	retval.add(new TrueObjChoiceContent<Source>(rMan.getString("nodeClusterParams.stored"), Source.STORED));
      }
      retval.add(new TrueObjChoiceContent<Source>(rMan.getString("nodeClusterParams.file"), Source.FILE));
      retval.add(new TrueObjChoiceContent<Source>(rMan.getString("nodeClusterParams.plugin"), Source.PLUGIN));   
      return (retval);
    }
    
    public static Vector<TrueObjChoiceContent<InterLink>> getILinkChoices() {
      ResourceManager rMan = ResourceManager.getManager();
      Vector<TrueObjChoiceContent<InterLink>> retval = new Vector<TrueObjChoiceContent<InterLink>>();
      retval.add(new TrueObjChoiceContent<InterLink>(rMan.getString("nodeClusterParams.between"), InterLink.BETWEEN));
      retval.add(new TrueObjChoiceContent<InterLink>(rMan.getString("nodeClusterParams.inline"), InterLink.INLINE));   
      return (retval);
    }
    
    public static Vector<TrueObjChoiceContent<ClustLayout>> getClustLayoutChoices() {
      ResourceManager rMan = ResourceManager.getManager();
      Vector<TrueObjChoiceContent<ClustLayout>> retval = new Vector<TrueObjChoiceContent<ClustLayout>>();
      retval.add(new TrueObjChoiceContent<ClustLayout>(rMan.getString("nodeClusterParams.breadthConnFirst"), ClustLayout.BREADTH_CONN_FIRST));
      retval.add(new TrueObjChoiceContent<ClustLayout>(rMan.getString("nodeClusterParams.breadth"), ClustLayout.BREADTH));
      retval.add(new TrueObjChoiceContent<ClustLayout>(rMan.getString("nodeClusterParams.similar"), ClustLayout.SIMILAR));   
      return (retval);
    }
        
    public static Vector<TrueObjChoiceContent<Order>> getOrderChoices() {
      ResourceManager rMan = ResourceManager.getManager();
      Vector<TrueObjChoiceContent<Order>> retval = new Vector<TrueObjChoiceContent<Order>>();
      retval.add(new TrueObjChoiceContent<Order>(rMan.getString("nodeClusterParams.breadthOrder"), Order.BREADTH));   
      retval.add(new TrueObjChoiceContent<Order>(rMan.getString("nodeClusterParams.name"), Order.NAME));
      retval.add(new TrueObjChoiceContent<Order>(rMan.getString("nodeClusterParams.nodeSize"), Order.NODE_SIZE));   
      retval.add(new TrueObjChoiceContent<Order>(rMan.getString("nodeClusterParams.linkSize"), Order.LINK_SIZE));   
      return (retval);
    }     
  }    

  /***************************************************************************
  **
  ** Class for representing a tuple
  */ 
   
  public static class Tuple implements Cloneable, Comparable<Tuple> {
  
    private String val1_;
    private String val2_;
  
    public Tuple(String val1, String val2) {
      if ((val1 == null) || (val2 == null)) {
        throw new IllegalArgumentException();
      }
      if (val1.compareTo(val2) < 0) {
        val1_ = val1;
        val2_ = val2;
      } else {
        val1_ = val2;
        val2_ = val1;
      }
    }  
   
    @Override
    public Tuple clone() {
      try {
        Tuple newTup = (Tuple)super.clone();
        return (newTup);
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }    
    }
    
    @Override
    public int hashCode() {
      return (val1_.hashCode() + val2_.hashCode());
    }
    
    public String getVal1() {
      return (val1_);
    }
    
    public String getVal2() {
      return (val2_);
    }  
    
    @Override
    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof Tuple)) {
        return (false);
      }
      Tuple otherT = (Tuple)other;
      return ((this.val1_.equals(otherT.val1_)) && (this.val2_.equals(otherT.val2_)));
    }
 
    @Override
    public String toString() {
      return ("Tuple: (" + val1_ + ", " + val2_ + ")");
    }
    
    public int compareTo(Tuple otherTup) {
      int val1Diff = this.val1_.compareTo(otherTup.val1_);
      if (val1Diff != 0) {
        return (val1Diff);
      }      
      return (this.val2_.compareTo(otherTup.val2_));
    }   
  }
}

