/*
**    Copyright (C) 2003-2017 Institute for Systems Biology 
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
import org.systemsbiology.biofabric.model.AnnotationSet;
import org.systemsbiology.biofabric.model.BioFabricNetwork;
import org.systemsbiology.biofabric.model.FabricLink;
import org.systemsbiology.biofabric.util.AsynchExitRequestException;
import org.systemsbiology.biofabric.util.BTProgressMonitor;
import org.systemsbiology.biofabric.util.DataUtil;
import org.systemsbiology.biofabric.util.NID;
import org.systemsbiology.biofabric.util.ResourceManager;
import org.systemsbiology.biofabric.util.TrueObjChoiceContent;
import org.systemsbiology.biofabric.util.UiUtil;
import org.systemsbiology.biofabric.util.UniqueLabeller;

/****************************************************************************
**
** This does node clustering layout
*/

public class NodeClusterLayout extends NodeLayout {
  
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
  
   public List<NID.WithName> doNodeLayout(BioFabricNetwork.RelayoutBuildData rbd, 
							                            Params crParams,
							                            BTProgressMonitor monitor) throws AsynchExitRequestException { 
     
    //
    // Go through all the links. If a link source and target are both in the same cluster, we add the link to the cluster
    //
  	 
  	ClusterParams params = (ClusterParams)crParams;
    
    TreeMap<String, BioFabricNetwork.RelayoutBuildData> perClust = new TreeMap<String, BioFabricNetwork.RelayoutBuildData>();
    HashMap<Tuple, List<FabricLink>> interClust = new HashMap<Tuple, List<FabricLink>>();
    HashSet<NID.WithName> interNodesOnly = new HashSet<NID.WithName>();
    
    HashMap<NID.WithName, Integer> fullNodeDegree = null;
    if (params.cLay == ClusterParams.ClustLayout.BREADTH_CONN_FIRST) {
      fullNodeDegree = new HashMap<NID.WithName, Integer>();
    } 	
    
    BioFabricNetwork.BuildMode intraLay = (params.cLay == ClusterParams.ClustLayout.SIMILAR) ? BioFabricNetwork.BuildMode.CLUSTERED_LAYOUT
            																																								 : BioFabricNetwork.BuildMode.DEFAULT_LAYOUT;
      
    Iterator<FabricLink> flit = rbd.allLinks.iterator();
    while (flit.hasNext()) {
      FabricLink fl = flit.next();
      NID.WithName source = fl.getSrcID();
      NID.WithName target = fl.getTrgID();
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
          rbdpc = new BioFabricNetwork.RelayoutBuildData(new UniqueLabeller(),
    		                                                 new HashSet<FabricLink>(), new HashSet<NID.WithName>(),
    		                                                 new HashMap<NID.WithName, String>(), rbd.colGen, intraLay);
          rbdpc.allNodeIDs = new HashSet<NID.WithName>();
          perClust.put(srcClust, rbdpc);
        }
        rbdpc.allLinks.add(fl);
        addClusterNode(rbdpc, source); 
        addClusterNode(rbdpc, target);
      } else {
        Tuple intup = new Tuple(srcClust, trgClust); // Tuples reordered so val1 < val2!
        List<FabricLink> icfl = interClust.get(intup);
        if (icfl == null) {
          icfl = new ArrayList<FabricLink>();
          interClust.put(intup, icfl);
        }
        icfl.add(fl);
        interNodesOnly.add(source);
        interNodesOnly.add(target);
      }
    }
    
    //
    // Need to deal with "clusters" of nodes that have no internal links!
    //  
    
    for (NID.WithName node : rbd.allNodeIDs) {
    	String clust = params.getClusterForNode(node);
    	BioFabricNetwork.RelayoutBuildData rbdpc = perClust.get(clust);
    	if (rbdpc == null) {
    		rbdpc = new BioFabricNetwork.RelayoutBuildData(new UniqueLabeller(),
    		                                               new HashSet<FabricLink>(), new HashSet<NID.WithName>(),
    		                                               new HashMap<NID.WithName, String>(), rbd.colGen, intraLay);
        rbdpc.allNodeIDs = new HashSet<NID.WithName>();
        perClust.put(clust, rbdpc);
    	}  		
      addClusterNode(rbdpc, node); 
    }
    
    String startClust = (params.startNode != null) ? params.getClusterForNode(params.startNode) : null;
    
    List<String> bfc;
    switch (params.order) {
    	case BREADTH:
        bfc = breadthFirstClustering(params, interClust.keySet(), startClust, monitor);
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
    
    Map<String, List<NID.WithName>> hubs = null;
    if (params.cLay == ClusterParams.ClustLayout.BREADTH_CONN_FIRST) {
      hubs = rankInterClustHubs(interClust, params, fullNodeDegree);	
    }

    ArrayList<NID.WithName> allTargets = new ArrayList<NID.WithName>();

    Iterator<String> pcit = bfc.iterator(); 
    while (pcit.hasNext()) {
    	String clustName = pcit.next();
      BioFabricNetwork.RelayoutBuildData pcrbd = perClust.get(clustName);
      if (pcrbd == null) {
      	continue;
      }
      List<NID.WithName> targets;
      if (intraLay == BioFabricNetwork.BuildMode.CLUSTERED_LAYOUT) {
        NodeSimilarityLayout.ClusterParams crp = new NodeSimilarityLayout.ClusterParams();
    	  NodeSimilarityLayout nslLayout = new NodeSimilarityLayout();
    	  pcrbd.existingIDOrder = new ArrayList<NID.WithName>(pcrbd.allNodeIDs);
    	 // FAIL
    	  UiUtil.fixMePrintout("What am I trying to do here?");
        //targets = nslLayout.doClusteredLayoutOrder(pcrbd, crp, monitor, startFrac, endFrac);
    	  targets = new ArrayList<NID.WithName>();
      } else {
        DefaultLayout dl = new DefaultLayout();
        List<NID.WithName> starts = (hubs == null) ? null : hubs.get(clustName);
        targets = dl.defaultNodeOrder(pcrbd.allLinks, pcrbd.loneNodeIDs, starts, monitor);  
      }
      allTargets.addAll(targets);
    }
    interNodesOnly.removeAll(allTargets);
    allTargets.addAll(interNodesOnly);
    
    installNodeOrder(allTargets, rbd, monitor);
    
    (new DefaultEdgeLayout()).layoutEdges(rbd, monitor);
    
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
    		String srcClust = params.getClusterForNode(daLink.getSrcID());
        String trgClust = params.getClusterForNode(daLink.getTrgID());
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
    
    AnnotationSet nAnnots = generateNodeAnnotations(rbd, params);
    rbd.setNodeAnnotations(nAnnots);
      
    Map<Boolean, AnnotationSet> lAnnots = generateLinkAnnotations();
    rbd.setLinkAnnotations(lAnnots);

    return (allTargets);
  }
   
   
  /***************************************************************************
  **
  ** Generate node annotations to tag each cluster
  */
    
  private AnnotationSet generateNodeAnnotations(BioFabricNetwork.RelayoutBuildData rbd, ClusterParams params) {
    
    AnnotationSet retval = new AnnotationSet();
  
    
    TreeMap<Integer, NID.WithName> invert = new TreeMap<Integer, NID.WithName>();
    
    for (NID.WithName node : rbd.nodeOrder.keySet()) {
      invert.put(rbd.nodeOrder.get(node), node);
    }
     
    String currClust = null;
    Integer startRow = null;
    Integer lastKey = invert.lastKey();
    for (Integer row : invert.keySet()) {
      NID.WithName node = invert.get(row);
      String clust = params.getClusterForNode(node);
      System.out.println("node " + node + " clust " + clust);
      if (currClust == null) {
        currClust = clust;
        startRow = row;
        if (row.equals(lastKey)) {
          AnnotationSet.Annot annot = new AnnotationSet.Annot(currClust, startRow.intValue(), row.intValue(), 0);
          retval.addAnnot(annot);
          break;
        }
        continue;
      }
      if (currClust.equals(clust)) {
        if (row.equals(lastKey)) {
          AnnotationSet.Annot annot = new AnnotationSet.Annot(currClust, startRow.intValue(), row.intValue(), 0);
          retval.addAnnot(annot);
          break;
        }
        continue;
      } else { 
        // We have just entered a new cluster
        AnnotationSet.Annot annot = new AnnotationSet.Annot(currClust, startRow.intValue(), row.intValue() - 1, 0);
        retval.addAnnot(annot);
        startRow = row;
        currClust = clust;
        if (row.equals(lastKey)) {
          annot = new AnnotationSet.Annot(currClust, startRow.intValue(), row.intValue(), 0);
          retval.addAnnot(annot);
          break;
        }
      }
    }
    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Generate link annotations to tag each cluster and intercluster links
  */
    
  private Map<Boolean, AnnotationSet> generateLinkAnnotations() { 
  	HashMap<Boolean, AnnotationSet> retval = new HashMap<Boolean, AnnotationSet>();
  	retval.put(Boolean.TRUE, new AnnotationSet());
  	retval.put(Boolean.FALSE, new AnnotationSet());
    return (retval);
  }
   
  /***************************************************************************
  **
  ** Helper
  */
  
  private void addClusterNode(BioFabricNetwork.RelayoutBuildData rbd, NID.WithName nid) { 
    if (!rbd.allNodeIDs.contains(nid)) {
    	boolean ok = rbd.idGen.addExistingLabel(nid.getNID().getInternal());
    	if (!ok) {
    		throw new IllegalStateException();
    	}
      rbd.allNodeIDs.add(nid);
    }
    return;
  }

  /***************************************************************************
  **
  ** Count map maintenance
  */
  
  private void addToCount(Map<String, Map<NID.WithName, Integer>> countMap, String clust, NID.WithName node) { 
 	
    Map<NID.WithName, Integer> perClust = countMap.get(clust);
    if (perClust == null) {
    	perClust = new HashMap<NID.WithName, Integer>();
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
  
  private Map<String, List<NID.WithName>> rankInterClustHubs(Map<Tuple, List<FabricLink>> interLinks, 
  		                                                       ClusterParams params, 
  		                                                       Map<NID.WithName, Integer> fullNodeDegree) { 
 	
  	// Map(ClusterID -> Map(NodeID, InterClustDegree)):
  	Map<String, Map<NID.WithName, Integer>> preRetval = new HashMap<String, Map<NID.WithName, Integer>>();
  	for (Tuple tup : interLinks.keySet()) {
  	  List<FabricLink> forTup = interLinks.get(tup);
  	  for (FabricLink link : forTup) {
  	  	// Tup order is NOT src, trg, but lo, hi:
  	    addToCount(preRetval, params.getClusterForNode(link.getSrcID()), link.getSrcID());
  	    addToCount(preRetval, params.getClusterForNode(link.getTrgID()), link.getTrgID());
  	  }
  	}
  	
  	// Map(ClusterID -> List(NodeID)):
    Map<String, List<NID.WithName>> retval = new HashMap<String, List<NID.WithName>>();  
    for (String clust: preRetval.keySet()) {
    	Map<NID.WithName, Integer> degMap = preRetval.get(clust);
    	// Map(SortedInterClustDegree->Map(SortedFullNodeDegree, Set(SortedNodeName))):
    	TreeMap<Integer, SortedMap<Integer, SortedSet<NID.WithName>>> invert = 
      	new TreeMap<Integer, SortedMap<Integer, SortedSet<NID.WithName>>>(Collections.reverseOrder());
    	for (NID.WithName node : degMap.keySet()) {
    		Integer interClustDeg = degMap.get(node);
    		SortedMap<Integer, SortedSet<NID.WithName>> perInterDeg = invert.get(interClustDeg);
    		if (perInterDeg == null) {
    			perInterDeg = new TreeMap<Integer, SortedSet<NID.WithName>>(Collections.reverseOrder());
    			invert.put(interClustDeg, perInterDeg);
    		}
    		Integer fnDeg = fullNodeDegree.get(node);
    		SortedSet<NID.WithName> perFullDeg = perInterDeg.get(fnDeg);
    		if (perFullDeg == null) {
    			perFullDeg = new TreeSet<NID.WithName>();
    			perInterDeg.put(fnDeg, perFullDeg);
    		}
    		perFullDeg.add(node);
    	}
    	List<NID.WithName> flat = new ArrayList<NID.WithName>();
      for (SortedMap<Integer, SortedSet<NID.WithName>> perInterDeg : invert.values()) {
      	for (SortedSet<NID.WithName> perFull : perInterDeg.values()) {
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
  
  private List<String> clusterSizeOrder(TreeMap<String, BioFabricNetwork.RelayoutBuildData> perClust, 
  		                                  boolean nodeFirst, String startClust) { 
  	
  	TreeMap<Integer, SortedMap<Integer, SortedSet<String>>> preRet = 
  		new TreeMap<Integer, SortedMap<Integer, SortedSet<String>>>(Collections.reverseOrder());
  	for (String clustName : perClust.keySet()) {
  	  BioFabricNetwork.RelayoutBuildData rbdpc = perClust.get(clustName);
  	  Integer size1 = Integer.valueOf((nodeFirst) ? rbdpc.allNodeIDs.size() : rbdpc.allLinks.size());
  	  SortedMap<Integer, SortedSet<String>> forSize1 = preRet.get(size1);
  	  if (forSize1 == null) {
  	  	forSize1 = new TreeMap<Integer, SortedSet<String>>(Collections.reverseOrder());
  	  	preRet.put(size1, forSize1);
  	  }
  	  Integer size2 = Integer.valueOf((nodeFirst) ? rbdpc.allLinks.size() : rbdpc.allNodeIDs.size());
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
  
  private List<String> breadthFirstClustering(ClusterParams params, Set<Tuple> iclinks, String startClust,
                                              BTProgressMonitor monitor) throws AsynchExitRequestException {
    
  	UniqueLabeller uLab = new UniqueLabeller();
  	Map<String, NID.WithName> fakeNodes = new HashMap<String, NID.WithName>();
  	
  	for (String clu : params.getClusters()) {
  	  NID pnid = uLab.getNextOID();
  	  fakeNodes.put(clu, new NID.WithName(pnid, clu));	
  	}
  	
  	Set<FabricLink> links = new HashSet<FabricLink>();
  	for (Tuple tup : iclinks) {
  		FabricLink pseudo = new FabricLink(fakeNodes.get(tup.getVal1()), fakeNodes.get(tup.getVal2()), "ic", Boolean.valueOf(false), Boolean.valueOf(false));
  		links.add(pseudo);
  		pseudo = new FabricLink(fakeNodes.get(tup.getVal2()), fakeNodes.get(tup.getVal1()), "ic", Boolean.valueOf(false), Boolean.valueOf(false));
  		links.add(pseudo);
  	}
  	
  	List<NID.WithName> useRoots = null;
  	if (startClust != null) {
  		useRoots = new ArrayList<NID.WithName>();
  		useRoots.add(fakeNodes.get(startClust));
  	}
    GraphSearcher gs = new GraphSearcher(new HashSet<NID.WithName>(fakeNodes.values()), links);
    List<GraphSearcher.QueueEntry> qes = gs.breadthSearch(useRoots, monitor);
    ArrayList<String> retval = new ArrayList<String>(); 
    for (GraphSearcher.QueueEntry aqe : qes) {
      retval.add(aqe.name.getName());
  	}
    return (retval);  
  }
   
  /***************************************************************************
  **
  ** For passing around layout params
  */  
  
  public static class ClusterParams implements NodeLayout.Params {
        
    public enum Source {STORED, FILE, PLUGIN};
    
    public enum Order {NAME, NODE_SIZE, LINK_SIZE, BREADTH};
    
    public enum InterLink {INLINE, BETWEEN};
    
    public enum ClustLayout {BREADTH_CONN_FIRST, BREADTH, SIMILAR};
    
          
    public Source source;
    public Order order;
    public InterLink iLink;
    public ClustLayout cLay;
    public NID.WithName startNode;
    public boolean saveAssign;
    private Map<NID.WithName, String> nodeClusters_;

    public ClusterParams(Source source, Order order, InterLink iLink, ClustLayout cLay, String startNode, 
    		                 Map<AttributeLoader.AttributeKey, String> nodeClusterAttributes, 
    		                 Map<String, NID.WithName> nodes, boolean saveAssign) {
    	
    	if (nodeClusterAttributes != null) {
    	  nodeClusters_ = new HashMap<NID.WithName, String>();
        for (AttributeLoader.AttributeKey key : nodeClusterAttributes.keySet()) {
          NID.WithName nodeID = nodes.get(DataUtil.normKey(((AttributeLoader.StringKey)key).key));
          nodeClusters_.put(nodeID, nodeClusterAttributes.get(key));
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

    public void install(Map<AttributeLoader.AttributeKey, String> nodeClusterAttributes, Map<String, NID.WithName> nodes) {   	
    	if (nodeClusterAttributes != null) {
    	  nodeClusters_ = new HashMap<NID.WithName, String>();
        for (AttributeLoader.AttributeKey key : nodeClusterAttributes.keySet()) {
        	NID.WithName nodeID = nodes.get(DataUtil.normKey(((AttributeLoader.StringKey)key).key));    	
          nodeClusters_.put(nodeID, nodeClusterAttributes.get(key));
        }
      }
    }
    
    public void assign(Map<NID.WithName, String> nodeClusterAssign) {   	
    	nodeClusters_ = nodeClusterAssign;
      return;
    }
 
    public String getClusterForNode(NID.WithName nodeID) {   	
    	return (nodeClusters_.get(nodeID));
    }  
    
    public Map<NID.WithName, String> getClusterAssign() {   	
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

