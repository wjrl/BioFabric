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

package org.systemsbiology.biofabric.model;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.xml.sax.Attributes;

import org.systemsbiology.biofabric.analysis.Link;
import org.systemsbiology.biofabric.io.AttributeLoader;
import org.systemsbiology.biofabric.io.FabricFactory;
import org.systemsbiology.biofabric.layouts.DefaultEdgeLayout;
import org.systemsbiology.biofabric.layouts.DefaultLayout;
import org.systemsbiology.biofabric.parser.AbstractFactoryClient;
import org.systemsbiology.biofabric.parser.GlueStick;
import org.systemsbiology.biofabric.ui.FabricColorGenerator;
import org.systemsbiology.biofabric.ui.FabricDisplayOptions;
import org.systemsbiology.biofabric.ui.FabricDisplayOptionsManager;
import org.systemsbiology.biofabric.util.AsynchExitRequestException;
import org.systemsbiology.biofabric.util.AttributeExtractor;
import org.systemsbiology.biofabric.util.BTProgressMonitor;
import org.systemsbiology.biofabric.util.CharacterEntityMapper;
import org.systemsbiology.biofabric.util.DataUtil;
import org.systemsbiology.biofabric.util.Indenter;
import org.systemsbiology.biofabric.util.MinMax;
import org.systemsbiology.biofabric.util.NID;
import org.systemsbiology.biofabric.util.UiUtil;
import org.systemsbiology.biofabric.util.UniqueLabeller;

/****************************************************************************
**
** This is the Network model
*/

public class BioFabricNetwork {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  public enum BuildMode {DEFAULT_LAYOUT,
                         REORDER_LAYOUT ,
                         CLUSTERED_LAYOUT ,
                         SHADOW_LINK_CHANGE,
                         GROUP_PER_NODE_CHANGE,
                         BUILD_FOR_SUBMODEL,
                         BUILD_FROM_XML ,
                         BUILD_FROM_SIF  ,
                         BUILD_FROM_GAGGLE ,
                         NODE_ATTRIB_LAYOUT ,
                         LINK_ATTRIB_LAYOUT,
                         NODE_CLUSTER_LAYOUT,
                         CONTROL_TOP_LAYOUT,
                         HIER_DAG_LAYOUT,
                         WORLD_BANK_LAYOUT,
                         GROUP_PER_NETWORK_CHANGE,
                        };
                                                
  public enum LayoutMode {
    UNINITIALIZED_MODE("notSet"),
    PER_NODE_MODE("perNode"),
    PER_NETWORK_MODE("perNetwork");

    private String text;

    LayoutMode(String text) {
      this.text = text;
    }

    public String getText() {
      return (text);
    }

	  public static LayoutMode fromString(String text)  throws IOException {
	    if (text != null) {
	      for (LayoutMode lm : LayoutMode.values()) {
	        if (text.equalsIgnoreCase(lm.text)) {
	          return (lm);
	        }
	      }
	    }
	    throw new IOException();
	  }
  }
                        

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
   
  //
  // For mapping of selections:
  //
  
  private HashMap<Integer, NID.WithName> rowToTargID_;
  private int rowCount_;
  
  //
  // Link and node definitions:
  //
  
  private TreeMap<Integer, LinkInfo> fullLinkDefs_;
  private TreeMap<Integer, Integer> nonShadowedLinkMap_;
  private HashMap<NID.WithName, NodeInfo> nodeDefs_;
  
  //
  // Grouping for links:
  //
  
  private List<String> linkGrouping_;
  
  //
  // Columns assignments, shadow and non-shadow states:
  //
 
  private ColumnAssign normalCols_;
  private ColumnAssign shadowCols_;
  
  private FabricColorGenerator colGen_;
  
  
  //
  // Current Link Layout Mode, either PER_NODE or PER_NETWORK
  // Default value is PER_NODE
  //

  private LayoutMode layoutMode_;
  
  private UniqueLabeller nodeIDGenerator_;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public BioFabricNetwork(BuildData bd, 
  		                    BTProgressMonitor monitor, 
                          double startFrac, 
                          double endFrac) throws AsynchExitRequestException {
  	nodeIDGenerator_ = new UniqueLabeller();
  	layoutMode_ = LayoutMode.UNINITIALIZED_MODE;
    BuildMode mode = bd.getMode();
    
    switch (mode) {
      case DEFAULT_LAYOUT:  
      case REORDER_LAYOUT:
      case CLUSTERED_LAYOUT:
      case GROUP_PER_NODE_CHANGE:
      case GROUP_PER_NETWORK_CHANGE:
      case NODE_ATTRIB_LAYOUT:
      case LINK_ATTRIB_LAYOUT:
      case NODE_CLUSTER_LAYOUT:
      case CONTROL_TOP_LAYOUT: 
      case HIER_DAG_LAYOUT:
      case WORLD_BANK_LAYOUT:
        RelayoutBuildData rbd = (RelayoutBuildData)bd;
        normalCols_ = new ColumnAssign();
        shadowCols_ = new ColumnAssign();
        rowToTargID_ = new HashMap<Integer, NID.WithName>(); 
        fullLinkDefs_ = new TreeMap<Integer, LinkInfo>();
        nonShadowedLinkMap_ = new TreeMap<Integer, Integer>();
        nodeDefs_ = new HashMap<NID.WithName, NodeInfo>();
        linkGrouping_ = new ArrayList<String>(rbd.linkGroups);
        colGen_ = rbd.colGen;
        layoutMode_ = rbd.layoutMode;
        System.out.println("BFNCO");
        relayoutNetwork(rbd, monitor, startFrac, endFrac);
        System.out.println("BFNCO2");
        break;
      case BUILD_FOR_SUBMODEL:
        SelectBuildData sbd = (SelectBuildData)bd;
        colGen_ = sbd.fullNet.colGen_;
        this.linkGrouping_ = new ArrayList<String>(sbd.fullNet.linkGrouping_);
        this.layoutMode_ = sbd.fullNet.layoutMode_;
        fillSubModel(sbd.fullNet, sbd.subNodes, sbd.subLinks);
        break;
      case BUILD_FROM_XML:
      case SHADOW_LINK_CHANGE:
        BioFabricNetwork built = ((PreBuiltBuildData)bd).bfn;
        this.normalCols_ = built.normalCols_;
        this.shadowCols_ = built.shadowCols_;
        this.rowToTargID_ = built.rowToTargID_; 
        this.fullLinkDefs_ = built.fullLinkDefs_;
        this.nonShadowedLinkMap_ = built.nonShadowedLinkMap_;
        this.nodeDefs_ = built.nodeDefs_;
        this.colGen_ = built.colGen_;
        this.rowCount_ = built.rowCount_;
        this.linkGrouping_ = built.linkGrouping_;
        this.layoutMode_ = built.layoutMode_;
        break;
      case BUILD_FROM_SIF:
      case BUILD_FROM_GAGGLE:
        RelayoutBuildData obd = (RelayoutBuildData)bd;    
        normalCols_ = new ColumnAssign();
        shadowCols_ = new ColumnAssign();
        rowToTargID_ = new HashMap<Integer, NID.WithName>(); 
        fullLinkDefs_ = new TreeMap<Integer, LinkInfo>();
        nonShadowedLinkMap_ = new TreeMap<Integer, Integer>();
        nodeDefs_ = new HashMap<NID.WithName, NodeInfo>();
        linkGrouping_ = new ArrayList<String>();
        layoutMode_ = LayoutMode.UNINITIALIZED_MODE;
        colGen_ = obd.colGen;
        processLinks(obd, monitor, startFrac, endFrac);
        break;
      default:
        throw new IllegalArgumentException();
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Get map from normalized name to IDs (Moving to Cytoscape SUIDs, there
  *  can be multiple nodes for one name)
  */

  public Map<String, Set<NID.WithName>> getNormNameToIDs() {
  	HashMap<String, Set<NID.WithName>> retval = new HashMap<String, Set<NID.WithName>>();
  	Iterator<NID.WithName> kit = nodeDefs_.keySet().iterator();
  	while (kit.hasNext()) {
  		NID.WithName key = kit.next();
  		NodeInfo forKey = nodeDefs_.get(key);
  		String nameNorm = DataUtil.normKey(forKey.getNodeName());
  		Set<NID.WithName> forName = retval.get(nameNorm);
  		if (forName == null) {
  			forName = new HashSet<NID.WithName>();
  			retval.put(nameNorm, forName);
  		}
  		forName.add(key);
  	}
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Install link grouping
  */

  public void installLinkGroups(List<String> linkTagList) {    
    linkGrouping_ = new ArrayList<String>(linkTagList);
    return;
  }
  
  /***************************************************************************
  ** 
  ** Get link grouping
  */

  public List<String> getLinkGroups() {    
    return (linkGrouping_);
  }
  
  /***************************************************************************
  ** 
  ** Given an attribute list giving node order, confirm it is valid:
  */

  public boolean checkNewNodeOrder(Map<AttributeLoader.AttributeKey, String> nodeAttributes) { 
    
    //
    // All existing targets must have a row, and all existing
    // rows need a target assigned!
    //
    
    HashSet<String> normedNames = new HashSet<String>();
    Iterator<NID.WithName> rttvit = rowToTargID_.values().iterator();
    while (rttvit.hasNext()) {
    	NID.WithName key = rttvit.next();
    	NodeInfo ni = nodeDefs_.get(key);
      normedNames.add(DataUtil.normKey(ni.getNodeName()));
    }
     
    HashSet<String> normedKeys = new HashSet<String>();
    Iterator<AttributeLoader.AttributeKey> akit = nodeAttributes.keySet().iterator();
    while (akit.hasNext()) {
    	AttributeLoader.AttributeKey key = akit.next();
      normedKeys.add(DataUtil.normKey(key.toString()));
    }
  
    if (!normedNames.equals(normedKeys)) {
      return (false);
    }
    
    TreeSet<Integer> asInts = new TreeSet<Integer>();
    Iterator<String> nrvit = nodeAttributes.values().iterator();
    while (nrvit.hasNext()) {
      String asStr = nrvit.next();
      try {
        asInts.add(Integer.valueOf(asStr));
      } catch (NumberFormatException nfex) {
        return (false);
      }
    }
    
    if (!asInts.equals(new TreeSet<Integer>(rowToTargID_.keySet()))) {
      return (false);
    }
    
    return (true);
  }
  
  /***************************************************************************
  ** 
  ** Given an attribute list giving node order, confirm it is valid:
  */

  public boolean checkNodeClusterAssignment(Map<AttributeLoader.AttributeKey, String> nodeClusters) { 
    
    //
    // All nodes must be assigned to a cluster
    //
    
    HashSet<AttributeLoader.AttributeKey> asUpper = new HashSet<AttributeLoader.AttributeKey>();
    Iterator<NID.WithName> rttvit = rowToTargID_.values().iterator();
    /*
    while (rttvit.hasNext()) { 	
    	
      asUpper.add(new AttributeLoader.StringKey(rttvit.next().toUpperCase()));
      
      
      this.nodeOrder = new HashMap<NID, Integer>();
      Map<String, Set<NID>> nameToID = genNormNameToID();
      for (AttributeLoader.AttributeKey key : nodeOrder.keySet()) {
        try {
          Integer newRow = Integer.valueOf(nodeOrder.get(key));
          String normName = DataUtil.normKey(((AttributeLoader.StringKey)key).key);
          Set<NID> ids = nameToID.get(normName);
          if (ids.size() != 1) {
          	throw new IllegalStateException();
          }
          NID id = ids.iterator().next();
          this.nodeOrder.put(id, newRow);
        } catch (NumberFormatException nfex) {
          throw new IllegalStateException();
        }
      }
    }*/
    UiUtil.fixMePrintout("Actually do something");
 //   if (!asUpper.equals(new HashSet<AttributeLoader.AttributeKey>(nodeClusters.keySet()))) {
  //    return (false);
  //  }
   
    return (true);
  }
  
  /***************************************************************************
  ** 
  ** Given an attribute list giving link order, confirm it is valid, get back
  ** map to integer values with directed tag filled in!
  **
  ** FIX ME: A few problems need to be addressed.  First, there is no
  ** decent error messaging about what is missing or duplicated.  Second,
  ** we require shadow links to ALWAYS be specified, even if the user does
  ** not care.... 
  */

  public SortedMap<Integer, FabricLink> checkNewLinkOrder(Map<AttributeLoader.AttributeKey, String> linkRows) { 
    
    //
    // Recover the mapping that tells us what link relationships are
    // directed:
    //
    
    HashMap<FabricLink.AugRelation, Boolean> relDir = new HashMap<FabricLink.AugRelation, Boolean>();
    Set<FabricLink> allLinks = getAllLinks(true);
    Iterator<FabricLink> alit = allLinks.iterator();
    while (alit.hasNext()) {
      FabricLink link = alit.next();
      FabricLink.AugRelation rel = link.getAugRelation();
      boolean isDir = link.isDirected();
      Boolean myVal = new Boolean(isDir);
      Boolean currVal = relDir.get(rel);
      if (currVal != null) {
        if (!currVal.equals(myVal)) {
          throw new IllegalStateException();
        }
      } else {
        relDir.put(rel, myVal);
      }
    }
    
    //
    // Create a map that takes column Integer to a correctly
    // directed copy of the Fabric link:
    //
    
    TreeMap<Integer, FabricLink> dirMap = new TreeMap<Integer, FabricLink>();
    Iterator<AttributeLoader.AttributeKey> lrit = linkRows.keySet().iterator();
    while (lrit.hasNext()) {
      FabricLink link = (FabricLink)lrit.next();
      String colNumStr = linkRows.get(link);
      FabricLink dirCopy = link.clone();
      Boolean isDirected = relDir.get(dirCopy.getAugRelation());
      dirCopy.installDirection(isDirected);
      try {
        dirMap.put(Integer.valueOf(colNumStr), dirCopy);
      } catch (NumberFormatException nfex) {
        return (null);
      }
    }
    
    // Ordered set of all our existing links:
    TreeSet<FabricLink> alks = new TreeSet<FabricLink>(allLinks);
    // Ordered set of guys we have been handed:
    TreeSet<FabricLink> dmvs = new TreeSet<FabricLink>(dirMap.values());
    
    // Has to be the case that the link definitions are 1:1 and
    // onto, or we have an error:
    if (!alks.equals(dmvs)) {
      return (null);
    }
  
    //
    // Has to be the case that all columns are also 1:1 and onto:
    //
    
    TreeSet<Integer> ldks = new TreeSet<Integer>(fullLinkDefs_.keySet());
    TreeSet<Integer> dmks = new TreeSet<Integer>(dirMap.keySet());
    
    if (!ldks.equals(dmks)) {
      return (null);
    }
    
    return (dirMap);
  }
  
  /***************************************************************************
  **
  ** Get all links
  */
  
  public Set<FabricLink> getAllLinks(boolean withShadows) {  
    HashSet<FabricLink> allLinks = new HashSet<FabricLink>();
    Iterator<Integer> ldit = fullLinkDefs_.keySet().iterator();
    while (ldit.hasNext()) {
      Integer col = ldit.next();
      LinkInfo li = getLinkDefinition(col, true);  // just get everybody...
      FabricLink link = li.getLink();
      if (withShadows || !link.isShadow()) {
        allLinks.add(link);
      }
    }
    return (allLinks);
  }
 
  /***************************************************************************
  **
  ** Get ordered linkInfo iterator
  */
  
  public Iterator<Integer> getOrderedLinkInfo(boolean withShadows) {  
    return ((withShadows) ? fullLinkDefs_.keySet().iterator() : nonShadowedLinkMap_.keySet().iterator());
  }
  
  /***************************************************************************
  ** 
  ** Process a link set
  */

  private void processLinks(RelayoutBuildData rbd, 
  		                      BTProgressMonitor monitor, 
                            double startFrac, 
                            double endFrac) throws AsynchExitRequestException {
    //
    // Note the allLinks Set has pruned out duplicates and synonymous non-directional links
    //
       
    List<NID.WithName> targetIDs =  (new DefaultLayout()).doNodeLayout(rbd, null);
    
    //
    // Now have the ordered list of targets we are going to display.
    //
    
    fillNodesFromOrder(targetIDs, rbd.colGen, rbd.clustAssign);

    //
    // This now assigns the link to its column.  Note that we order them
    // so that the shortest vertical link is drawn first!
    //
    
    (new DefaultEdgeLayout()).layoutEdges(rbd, monitor, startFrac, endFrac);
    specifiedLinkToColumn(rbd.colGen, rbd.linkOrder, false, monitor, startFrac, endFrac);

    //
    // Determine the start & end of each target row needed to handle the incoming
    // and outgoing links:
    //

    trimTargetRows();
        
    //
    // For the lone nodes, they are assigned into the last column:
    //
    
    loneNodesToLastColumn(rbd.loneNodeIDs);
    return;
  }
  
  /***************************************************************************
  **
  ** Relayout the network!
  */
  
  private void relayoutNetwork(RelayoutBuildData rbd, BTProgressMonitor monitor, 
                               double startFrac, 
                               double endFrac) throws AsynchExitRequestException {
    BuildMode mode = rbd.getMode();
    installLinkGroups(rbd.linkGroups);
    setLayoutMode(rbd.layoutMode);
    boolean specifiedNodeOrder = (mode == BuildMode.NODE_ATTRIB_LAYOUT) || 
                                 (mode == BuildMode.DEFAULT_LAYOUT) ||
                                 (mode == BuildMode.CONTROL_TOP_LAYOUT) ||
                                 (mode == BuildMode.HIER_DAG_LAYOUT) ||
                                 (mode == BuildMode.WORLD_BANK_LAYOUT) ||
                                 (mode == BuildMode.NODE_CLUSTER_LAYOUT) || 
                                 (mode == BuildMode.CLUSTERED_LAYOUT) || 
                                 (mode == BuildMode.REORDER_LAYOUT);          
    List<NID.WithName> targetIDs;
    if (specifiedNodeOrder) {
      targetIDs = specifiedIDOrder(rbd.allNodeIDs, rbd.nodeOrder);
    } else {       
      targetIDs = rbd.existingIDOrder;
    }
   
    if (monitor != null) {
      if (!monitor.updateProgress((int)(startFrac * 100.0))) {
        throw new AsynchExitRequestException();
      }
    }
   
    //
    // Now have the ordered list of targets we are going to display.
    // Build target->row maps and the inverse:
    //

    fillNodesFromOrder(targetIDs, rbd.colGen, rbd.clustAssign);

    //
    // Ordering of links:
    //
  
    if ((rbd.linkOrder == null) || rbd.linkOrder.isEmpty() || (mode == BuildMode.GROUP_PER_NODE_CHANGE) || (mode == BuildMode.GROUP_PER_NETWORK_CHANGE)) {
      if ((rbd.nodeOrder == null) || rbd.nodeOrder.isEmpty()) {
        rbd.nodeOrder = new HashMap<NID.WithName, Integer>();
        int numT = targetIDs.size();
        for (int i = 0; i < numT; i++) {
          NID.WithName targID = targetIDs.get(i);
          rbd.nodeOrder.put(targID, Integer.valueOf(i));
        }
      }
      (new DefaultEdgeLayout()).layoutEdges(rbd, monitor, startFrac, endFrac);
    }

    //
    // This now assigns the link to its column, based on user specification
    //
    
    if (monitor != null) {
      if (!monitor.updateProgress((int)(((endFrac + startFrac) / 2.0) * 100.0))) {
        throw new AsynchExitRequestException();
      }
    }
    
    specifiedLinkToColumn(rbd.colGen, rbd.linkOrder, ((mode == BuildMode.LINK_ATTRIB_LAYOUT) || 
    		                                              (mode == BuildMode.NODE_CLUSTER_LAYOUT) ||
    		                                              (mode == BuildMode.GROUP_PER_NODE_CHANGE) ||
    		                                              (mode == BuildMode.GROUP_PER_NETWORK_CHANGE)),
    		                  monitor, (endFrac + startFrac) / 2.0, endFrac);
      
    //
    // Determine the start & end of each target row needed to handle the incoming
    // and outgoing links:
    //

    trimTargetRows();
        
    //
    // For the lone nodes, they are assigned into the last column:
    //

    loneNodesToLastColumn(rbd.loneNodeIDs);

    if (monitor != null) {
      if (!monitor.updateProgress((int)(endFrac * 100.0))) {
        throw new AsynchExitRequestException();
      }
    }

    return;
  }
  
  /***************************************************************************
  **
  ** Get specified node ID order list from attribute map
  */
  
  private List<NID.WithName> specifiedIDOrder(Set<NID.WithName> allNodeIDs, Map<NID.WithName, Integer> newOrder) { 
    TreeMap<Integer, NID.WithName> forRetval = new TreeMap<Integer, NID.WithName>();
    Iterator<NID.WithName> rttvit = allNodeIDs.iterator();
    while (rttvit.hasNext()) {
      NID.WithName key = rttvit.next();
      Integer val = newOrder.get(key);
      forRetval.put(val, key);
    }
    return (new ArrayList<NID.WithName>(forRetval.values()));
  }
 
  /***************************************************************************
  **
  ** Get existing order
  */
  
  public List<NID.WithName> existingIDOrder() {  
    ArrayList<NID.WithName> retval = new ArrayList<NID.WithName>();
    Iterator<Integer> rtit = new TreeSet<Integer>(rowToTargID_.keySet()).iterator();
    while (rtit.hasNext()) {
      Integer row = rtit.next();
      NID.WithName nodeID = rowToTargID_.get(row);
      retval.add(nodeID);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get existing link order
  */
  
  public SortedMap<Integer, FabricLink> getExistingLinkOrder() {  
    TreeMap<Integer, FabricLink> retval = new TreeMap<Integer, FabricLink>();
    Iterator<Integer> ldit = fullLinkDefs_.keySet().iterator();
    while (ldit.hasNext()) {
      Integer col = ldit.next();
      LinkInfo li = fullLinkDefs_.get(col);
      FabricLink link = li.getLink();
      retval.put(col, link);
    }
    return (retval);
  }

  /***************************************************************************
  ** 
  ** Process a link set
  */

  private void specifiedLinkToColumn(FabricColorGenerator colGen, 
  		                               SortedMap<Integer, FabricLink> linkOrder, 
  		                               boolean userSpec, BTProgressMonitor monitor, 
                                     double startFrac, 
                                     double endFrac) throws AsynchExitRequestException {
     
    normalCols_.columnCount = 0;
    shadowCols_.columnCount = 0;
    int numColors = colGen.getNumColors();
    Iterator<Integer> frkit = linkOrder.keySet().iterator();
    
    double inc = (endFrac - startFrac) / ((linkOrder.size() == 0) ? 1 : linkOrder.size());
    double currProg = startFrac;
    
    while (frkit.hasNext()) {
      Integer nextCol = frkit.next();
      FabricLink nextLink = linkOrder.get(nextCol);
      Integer[] colCounts = addLinkDef(nextLink, numColors, normalCols_.columnCount, shadowCols_.columnCount, colGen);
      shadowCols_.columnCount = colCounts[0].intValue();
      if (colCounts[1] != null) {
        normalCols_.columnCount = colCounts[1].intValue();
      }
      
	    if (monitor != null) {
	    	currProg += inc;
	      if (!monitor.updateProgress((int)(currProg * 100.0))) {
	        throw new AsynchExitRequestException();
	      }
	    }    
    }
    
    // WJRL 3/7/17 Commented out temporarily so I can check efficiency of other code....
   
    System.out.println("This operation is O(e^2)");
    setDrainZonesWithMultipleLabels(true);
    System.out.println("SDZML 0.5");
    setDrainZonesWithMultipleLabels(false);
    System.out.println("SDZML done");
 
    if (monitor != null) {
      if (!monitor.updateProgress((int)(endFrac * 100.0))) {
        throw new AsynchExitRequestException();
      }
      System.out.println(currProg);
    }           
    return;
  }
  
  /***************************************************************************
   ** Calculates each MinMax region for every node's zero or more drain zones
  ** 
   */
  
  private void setDrainZonesWithMultipleLabels(boolean forShadow) {
    
    List<LinkInfo> links = getLinkDefList(forShadow);
    Map<NID.WithName, List<DrainZone>> nodeToZones = new TreeMap<NID.WithName, List<DrainZone>>();

    LinkInfo current = links.get(0);  // these keep track of start of zone and zone's node
    int startIdx = 0;
    
    for (int index = 0; index <= links.size(); index++) {
      
      if (index == links.size()) {
        
        int endIdx = links.size() - 1;
  
        MinMax mm = new MinMax(startIdx, endIdx);
        NID.WithName name = findZoneNode(mm, forShadow, links);
  
        if (nodeToZones.get(name) == null) {
          nodeToZones.put(name, new ArrayList<DrainZone>());
        }
        nodeToZones.get(name).add(new DrainZone(mm, forShadow));
        
      } else if (! isContiguous(links.get(index), current)) {
        
        int last = index - 1;  // backtrack one position
  
        MinMax mm = new MinMax(startIdx, last);
        NID.WithName name = findZoneNode(mm, forShadow, links);
  
        if (nodeToZones.get(name) == null) {
          nodeToZones.put(name, new ArrayList<DrainZone>());
        }
        nodeToZones.get(name).add(new DrainZone(mm, forShadow));
        
        startIdx = index;           // update the start index
        current = links.get(index); // update the current node whose zone we're calculating
      }
        
    }

    for (Map.Entry<NID.WithName, List<DrainZone>> entry : nodeToZones.entrySet()) {
      
      NodeInfo ni = getNodeDefinition(entry.getKey());
      ni.setDrainZones(entry.getValue(), forShadow);
    }
    
  }
  
  /***************************************************************************
   ** Returns true if two links are part of the same drain zone
   **
   */
  
  private boolean isContiguous(LinkInfo A, LinkInfo B) {
    
    NID.WithName mainA;
    if (A.isShadow()) {
      mainA = getNodeIDForRow(A.bottomRow());
    } else {
      mainA = getNodeIDForRow(A.topRow());
    }
    
    NID.WithName mainB;
    if (B.isShadow()) {
      mainB = getNodeIDForRow(B.bottomRow());
    } else {
      mainB = getNodeIDForRow(B.topRow());
    }
    
    return mainA.equals(mainB);
  }
  
  /***************************************************************************
   * finds the node of the drain zone with bounds = MinMax([A,B])
   */
  
  private NID.WithName findZoneNode(MinMax mm, boolean forShadow, List<LinkInfo> links) {
    
    LinkInfo li = links.get(mm.min);  // checking the minimum of the minmax is enough...?

    if (forShadow) {
      return getNodeIDForRow(li.bottomRow());
    } else {
      return getNodeIDForRow(li.topRow());
    }
  }
  
  /***************************************************************************
   **
  ** Helper
  */

  private void updateContigs(NID.WithName nodeID, HashMap<NID.WithName, SortedMap<Integer, MinMax>> runsPerNode, 
                             Integer lastCol, Integer col) {
    int colVal = col.intValue();
    SortedMap<Integer, MinMax> runs = runsPerNode.get(nodeID);
    if (runs == null) {
      runs = new TreeMap<Integer, MinMax>();
      runsPerNode.put(nodeID, runs);        
    }
    MinMax currRun = runs.get(lastCol);
    if (currRun == null) {
      currRun = new MinMax(colVal);
      runs.put(col, currRun);        
    } else {
      currRun.update(colVal);
      runs.remove(lastCol);
      runs.put(col, currRun);
    }
    return;
  }
 
  /***************************************************************************
  ** 
  ** When user specified link order, things could get totally wild.  Set drain zones
  ** by leftmost largest contiguous set of links.
  */

  private void runsToDrain(HashMap<NID.WithName, SortedMap<Integer, MinMax>> runsPerNode, boolean forShadow) {  
    Iterator<NID.WithName> rpnit = runsPerNode.keySet().iterator();
    while (rpnit.hasNext()) {  
      NID.WithName nodeID = rpnit.next();      
      SortedMap<Integer, MinMax> runs = runsPerNode.get(nodeID);
      MinMax maxRun = null;
      int maxSize = Integer.MIN_VALUE;
      Iterator<MinMax> rit = runs.values().iterator();
      while (rit.hasNext()) {  
        MinMax aRun = rit.next();
        int runLen = aRun.max - aRun.min + 1;
        if (runLen > maxSize) {
          maxSize = runLen;
          maxRun = aRun;
        } else if ((runLen == 1) && (maxSize == 1)) {
          maxRun = aRun;  // move this to the end if no contig run
        }
      }
      if (maxRun != null) {
        NodeInfo nit = nodeDefs_.get(nodeID);
        nit.addDrainZone(new DrainZone(maxRun.clone(), forShadow));
      }
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** When user specified link order, things could get totally wild.  Set drain zones
  ** by leftmost largest contiguous set of links.
  */

  private void setDrainZonesByContig(boolean withShadows) {
    
    HashMap<NID.WithName, SortedMap<Integer, MinMax>> runsPerNode = new HashMap<NID.WithName, SortedMap<Integer, MinMax>>();
         
    Iterator<Integer> olit = getOrderedLinkInfo(withShadows);
    Integer lastCol = Integer.valueOf(-1);
    while (olit.hasNext()) {
      Integer col = olit.next();
      LinkInfo linf = getLinkDefinition(col, withShadows);
      NID.WithName topNodeID = rowToTargID_.get(Integer.valueOf(linf.topRow()));
      updateContigs(topNodeID, runsPerNode, lastCol, col);
      if (withShadows && linf.isShadow()) {
        NID.WithName botNodeID = rowToTargID_.get(Integer.valueOf(linf.bottomRow()));     
        updateContigs(botNodeID, runsPerNode, lastCol, col);
      }
      lastCol = col;
    } 
    runsToDrain(runsPerNode, withShadows);
    return;
  }
   
  /***************************************************************************
  **
  ** Dump the network using XML
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {    
    ind.indent();
    out.println("<BioFabric>");
    ind.up();
    
    colGen_.writeXML(out, ind);
    
    //
    // Display options:
    //
    
    FabricDisplayOptionsManager.getMgr().writeXML(out, ind);
       
    //
    // Dump the nodes, then the links:
    //  
    
    Iterator<Integer> r2tit = (new TreeSet<Integer>(rowToTargID_.keySet())).iterator();
    ind.indent();
    out.println("<nodes>");
    ind.up();
    while (r2tit.hasNext()) {
      Integer row = r2tit.next();
      NID.WithName nodeID = rowToTargID_.get(row);
      NodeInfo ni = getNodeDefinition(nodeID);
      ni.writeXML(out, ind, row.intValue());
    }
    ind.down().indent();
    out.println("</nodes>");
  
    if (!linkGrouping_.isEmpty()) {
      Iterator<String> lgit = linkGrouping_.iterator();
      ind.indent();
      out.print("<linkGroups mode=\"");
      out.print(layoutMode_.getText());
      out.println("\">");
      ind.up();
      while (lgit.hasNext()) {
        String grpTag = lgit.next();
        ind.indent();
        out.print("<linkGroup tag=\"");
        out.print(grpTag);
        out.println("\" />");
      }
      ind.down().indent();
      out.println("</linkGroups>");
    }
  
    HashMap<Integer, Integer> inverse = new HashMap<Integer, Integer>();
    Iterator<Integer> nsit = nonShadowedLinkMap_.keySet().iterator();
    while (nsit.hasNext()) {
      Integer key = nsit.next();
      inverse.put(nonShadowedLinkMap_.get(key), key);
    }
    
    
    Iterator<Integer> ldit = fullLinkDefs_.keySet().iterator();
    ind.indent();
    out.println("<links>");
    ind.up();
    while (ldit.hasNext()) {
      Integer col = ldit.next();
      LinkInfo li = getLinkDefinition(col, true);
      FabricLink link = li.getLink();
      ind.indent();
      out.print("<link srcID=\"");
      out.print(link.getSrcID().getNID().getInternal());
      out.print("\" trgID=\"");
      out.print(link.getTrgID().getNID().getInternal());
      out.print("\" rel=\"");
      FabricLink.AugRelation augr = link.getAugRelation();
      out.print(CharacterEntityMapper.mapEntities(augr.relation, false));
      out.print("\" directed=\"");
      out.print(link.isDirected());
      out.print("\" shadow=\"");
      out.print(augr.isShadow);
      if (!augr.isShadow) {
        Integer nsCol = inverse.get(col); 
        out.print("\" column=\"");
        out.print(nsCol);
      }
      out.print("\" shadowCol=\"");
      out.print(col);     
      out.print("\" srcRow=\"");
      out.print(li.getStartRow());
      out.print("\" trgRow=\"");
      out.print(li.getEndRow());
      out.print("\" color=\"");
      out.print(li.getColorKey());
      out.println("\" />");
    }
    ind.down().indent();
    out.println("</links>");
    ind.down().indent();
    out.println("</BioFabric>");
    return;
  }
  
  /***************************************************************************
  **
  ** Dump a node attribute file with row assignments:
  */
  
  public void writeNOA(PrintWriter out) {    
    out.println("Node Row");
    Iterator<Integer> r2tit = new TreeSet<Integer>(rowToTargID_.keySet()).iterator();
    while (r2tit.hasNext()) {
      Integer row = r2tit.next();
      NID.WithName nodeID = rowToTargID_.get(row);
      NodeInfo ni = getNodeDefinition(nodeID);
      out.print(ni.getNodeName());
      out.print(" = ");
      out.println(row);
    } 
    return;
  }
  
  /***************************************************************************
  **
  ** Dump an edge attribute file with column assignments:
  */
  
  public void writeEDA(PrintWriter out) {    
    out.println("Link Column");
    Iterator<Integer> ldit = fullLinkDefs_.keySet().iterator();
    while (ldit.hasNext()) {
      Integer col = ldit.next();
      LinkInfo li = getLinkDefinition(col, true);
      FabricLink link = li.getLink();
      out.print(link.toEOAString(nodeDefs_));
      out.print(" = ");
      out.println(col);
    }
    return;
  }
    
  /***************************************************************************
  ** 
  ** Get Node Definition
  */

  public NodeInfo getNodeDefinition(NID.WithName targID) {
    NodeInfo node = nodeDefs_.get(targID);
    return (node);
  }
  
  /***************************************************************************
  ** 
  ** Link definition for column.  If no link in column (may happen for selected networks),
  ** returns null
  */

  public LinkInfo getLinkDefinition(Integer colObj, boolean forShadow) {
    if (forShadow) {
      return (fullLinkDefs_.get(colObj));
    } else {
      Integer mapped = nonShadowedLinkMap_.get(colObj);
      if (mapped != null) {
        return (fullLinkDefs_.get(mapped));
      } else {
        return (null);
      }     
    }
  }
  
  /***************************************************************************
  ** 
  ** Get Target For Column
  */

  public NID.WithName getTargetIDForColumn(Integer colVal, boolean forShadow) {
    ColumnAssign useCA = (forShadow) ? shadowCols_ : normalCols_;
    NID.WithName target = useCA.columnToTarget.get(colVal);
    return (target);
  }
  
  /***************************************************************************
  ** 
  ** Get Drain zone For Column
  */

  public NID.WithName getDrainForColumn(Integer colVal, boolean forShadow) {
  
    int col = colVal.intValue();
    ColumnAssign useCA = (forShadow) ? shadowCols_ : normalCols_;
    NID.WithName targetID = useCA.columnToTarget.get(colVal);
    NID.WithName sourceID = useCA.columnToSource.get(colVal);
    if (targetID != null) {
      NodeInfo nit = nodeDefs_.get(targetID);
      List<DrainZone> tdzs = nit.getDrainZones(forShadow);
      if (tdzs != null) {
        for (DrainZone tdz : tdzs)
          if ((col >= tdz.getMinMax().min) && (col <= tdz.getMinMax().max)) {
            return (targetID);
          }
      }
    }
    if (sourceID != null) {
      NodeInfo nis = nodeDefs_.get(sourceID);
      List<DrainZone> sdzs = nis.getDrainZones(forShadow);
      if (sdzs != null) {
        for (DrainZone sdz : sdzs)
          if ((col >= sdz.getMinMax().min) && (col <= sdz.getMinMax().max)) {
            return (sourceID);
          }
      }
    }
  
    return (null);
  }
  
  /***************************************************************************
  ** 
  ** Get Source For Column
  */

  public NID.WithName getSourceIDForColumn(Integer colVal, boolean forShadow) {
    ColumnAssign useCA = (forShadow) ? shadowCols_ : normalCols_;
    NID.WithName source = useCA.columnToSource.get(colVal);
    return (source);
  }
  
  /***************************************************************************
  ** 
  ** Get node for row
  */

  public NID.WithName getNodeIDForRow(Integer rowObj) {
    NID.WithName node = rowToTargID_.get(rowObj);
    return (node);
  }
  
  /***************************************************************************
  ** 
  ** Get link count
  */

  public int getLinkCount(boolean forShadow) {
    return ((forShadow) ? fullLinkDefs_.size() : nonShadowedLinkMap_.size());
  } 
  
  /***************************************************************************
  ** 
  ** Get column count
  */

  public int getColumnCount(boolean forShadow) {
    ColumnAssign useCA = (forShadow) ? shadowCols_ : normalCols_;
    return (useCA.columnCount);
  } 
  
  /***************************************************************************
  ** 
  ** Get Row Count
  */

  public int getRowCount() {
    return (rowCount_);
  } 
   
  /***************************************************************************
  ** 
  ** Get node defs
  */

  public List<NodeInfo> getNodeDefList() {
    return (new ArrayList<NodeInfo>(nodeDefs_.values()));
  } 
  
  /***************************************************************************
  ** 
  ** Get all node names
  */

  public Set<NID.WithName> getNodeSetIDs() {
  	HashSet<NID.WithName> retval = new HashSet<NID.WithName>();
  	Iterator<NodeInfo> nsit = nodeDefs_.values().iterator();
    while (nsit.hasNext()) {
    	NodeInfo ni = nsit.next();
      retval.add(new NID.WithName(ni.getNodeID(), ni.getNodeName()));
    }
    return (retval);
  }  
  
  /***************************************************************************
  ** 
  ** Get link defs
  */

  public List<LinkInfo> getLinkDefList(boolean forShadow) {
    if (forShadow) {
      return (new ArrayList<LinkInfo>(fullLinkDefs_.values()));
    } else {
      ArrayList<LinkInfo> retval = new ArrayList<LinkInfo>();
      Iterator<Integer> nsit = nonShadowedLinkMap_.keySet().iterator();
      while (nsit.hasNext()) {
        Integer linkID = nsit.next();
        Integer mappedID = nonShadowedLinkMap_.get(linkID);
        retval.add(fullLinkDefs_.get(mappedID));
      }
      return (retval);
    }
  } 
  
  /***************************************************************************
   **
   ** Set Layout Mode (PER_NODE or PER_NETWORK)
   */

  public void setLayoutMode(LayoutMode mode) {
    layoutMode_ = mode;
  }

  /***************************************************************************
   **
   ** Get Layout Mode (PER_NODE or PER_NETWORK)
   */

  public LayoutMode getLayoutMode() {
    return layoutMode_;
  }
 
  /***************************************************************************
  ** 
  ** Get node matches
  */

  public Set<NID.WithName> nodeMatches(boolean fullMatch, String searchString) {
    HashSet<NID.WithName> retval = new HashSet<NID.WithName>();
    Iterator<NID.WithName> nkit = nodeDefs_.keySet().iterator();
    while (nkit.hasNext()) {
      NID.WithName nodeID = nkit.next();
      String nodeName = nodeDefs_.get(nodeID).getNodeName();
      if (matches(searchString, nodeName, fullMatch)) {
        retval.add(nodeID);
      }
    }    
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Get first neighbors of node, along with info blocks
  */

  public void getFirstNeighbors(NID.WithName nodeID, Set<NID.WithName> nodeSet, List<NodeInfo> nodes, List<LinkInfo> links) {
    Iterator<LinkInfo> ldit = fullLinkDefs_.values().iterator();
    while (ldit.hasNext()) {
      LinkInfo linf = ldit.next();
      if (linf.getSource().equals(nodeID)) {
        nodeSet.add(linf.getTarget());
        links.add(linf);
      } else if (linf.getTarget().equals(nodeID)) {
        nodeSet.add(linf.getSource());
        links.add(linf);
      }
    }
    Iterator<NID.WithName> nsit = nodeSet.iterator();
    while (nsit.hasNext()) {
      NID.WithName nextID = nsit.next();
      nodes.add(nodeDefs_.get(nextID));
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** Get first neighbors of node
  */

  public Set<NID.WithName> getFirstNeighbors(NID.WithName nodeID) {
    HashSet<NID.WithName> nodeSet = new HashSet<NID.WithName>();
    Iterator<LinkInfo> ldit = fullLinkDefs_.values().iterator();
    while (ldit.hasNext()) {
      LinkInfo linf = ldit.next();
      if (linf.getSource().equals(nodeID)) {
        nodeSet.add(linf.getTarget());
      } else if (linf.getTarget().equals(nodeID)) {
        nodeSet.add(linf.getSource());
      }
    }
    return (nodeSet);
  }
   
  /***************************************************************************
  ** 
  ** Add first neighbors of node set
  */

  public void addFirstNeighbors(Set<NID.WithName> nodeSet, Set<Integer> columnSet, Set<FabricLink> linkSet, boolean forShadow) {
    HashSet<NID.WithName> newNodes = new HashSet<NID.WithName>();
    Iterator<LinkInfo> ldit = fullLinkDefs_.values().iterator();
    while (ldit.hasNext()) {
      LinkInfo linf = ldit.next();
      if (!forShadow && linf.isShadow()) {
        continue;
      }
      if (nodeSet.contains(linf.getSource())) {
        newNodes.add(linf.getTarget());
        columnSet.add(Integer.valueOf(linf.getUseColumn(forShadow)));
        linkSet.add(linf.getLink());
      }
      if (nodeSet.contains(linf.getTarget())) {
        newNodes.add(linf.getSource());
        columnSet.add(Integer.valueOf(linf.getUseColumn(forShadow)));
        linkSet.add(linf.getLink());
      }
    }
    nodeSet.addAll(newNodes);
    return;
  }

  /***************************************************************************
  **
  ** Do the match
  ** 
  */
 
  private boolean matches(String searchString, String nodeName, boolean isFull) {
    String canonicalNodeName = DataUtil.normKey(nodeName);
    if (isFull) {
      return (canonicalNodeName.equals(searchString));
    } else {
      return (canonicalNodeName.indexOf(searchString) != -1);
    }
  }

  /***************************************************************************
  **
  ** Count of links per targ:
  */

  private Map<NID.WithName, Integer> linkCountPerTarg(Set<NID.WithName> targets, List<LinkInfo> linkList) {
    HashMap<NID.WithName, Integer> retval = new HashMap<NID.WithName, Integer>();
    Iterator<LinkInfo> lcit = linkList.iterator();
    while (lcit.hasNext()) {
      LinkInfo linf = lcit.next();
      NID.WithName src = linf.getSource();
      NID.WithName trg = linf.getTarget();      
      if (targets.contains(src)) {
        Integer count = retval.get(src);
        if (count == null) {
          retval.put(src, Integer.valueOf(1));
        } else {
          retval.put(src, Integer.valueOf(count.intValue() + 1));
        }
      }
      if (!src.equals(trg) && targets.contains(trg)) {
        Integer count = retval.get(trg);
        if (count == null) {
          retval.put(trg, Integer.valueOf(1));
        } else {
          retval.put(trg, Integer.valueOf(count.intValue() + 1));
        }
      }
    }
    return (retval);
  }
   
  /***************************************************************************
  **
  ** When creating submodel and shadow links are being displayed, we can choose
  ** to only show one copy of a link: either the main link, or the shadow link, unless
  ** both endpoint drains are on display, then both will be shown.
  */

  private List<LinkInfo> pruneToMinSubModel(BioFabricNetwork bfn, List<NodeInfo> targetList, List<LinkInfo> linkList) {
          
    HashSet<NID.WithName> targSet = new HashSet<NID.WithName>();
    Iterator<NodeInfo> tlit = targetList.iterator();
    while (tlit.hasNext()) {
      NodeInfo ninf = tlit.next();
      targSet.add(new NID.WithName(ninf.getNodeID(), ninf.getNodeName()));
    }   

    Map<NID.WithName, Integer> subCounts = linkCountPerTarg(targSet, linkList);
    Map<NID.WithName, Integer> fullCounts = linkCountPerTarg(bfn.getNodeSetIDs(), bfn.getLinkDefList(true));
    
    HashSet<Integer> skipThem = new HashSet<Integer>();
    HashSet<Integer> ditchThem = new HashSet<Integer>();
    Iterator<LinkInfo> lcit = linkList.iterator();
    while (lcit.hasNext()) {
      LinkInfo linf = lcit.next();
      NID.WithName topNode = bfn.getNodeIDForRow(Integer.valueOf(linf.topRow()));
      NID.WithName botNode = bfn.getNodeIDForRow(Integer.valueOf(linf.bottomRow()));
      boolean topStays = subCounts.get(topNode).equals(fullCounts.get(topNode));
      boolean botStays = subCounts.get(botNode).equals(fullCounts.get(botNode));
      if ((topStays && botStays) || (!topStays && !botStays)) {  // Nobody gets ditched!
        continue;
      }
      FabricLink link1 = linf.getLink();
      int col1 = linf.getUseColumn(true);
      skipThem.add(Integer.valueOf(col1));
      Iterator<LinkInfo> lc2it = linkList.iterator();
      while (lc2it.hasNext()) {
        LinkInfo linf2 = lc2it.next();
        int col2 = linf2.getUseColumn(true);
        if (skipThem.contains(Integer.valueOf(col2))) {
          continue;
        }
        if (linf2.getLink().shadowPair(link1)) { // got a shadow pair! 
          int maxLink = Math.max(col1, col2);
          int minLink = Math.min(col1, col2);
          ditchThem.add(Integer.valueOf((topStays) ? maxLink : minLink));
          break;
        }
      }
    } 
    
    ArrayList<LinkInfo> retval = new ArrayList<LinkInfo>();
    Iterator<LinkInfo> lcit3 = linkList.iterator();
    while (lcit3.hasNext()) {
      LinkInfo linf = lcit3.next();
      int col = linf.getUseColumn(true);
      if (!ditchThem.contains(Integer.valueOf(col))) {
        retval.add(linf);
      }
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Fill out model with submodel data
  */

  private void fillSubModel(BioFabricNetwork bfn, List<NodeInfo> targetList, List<LinkInfo> linkList) {
    
    boolean doPrune = FabricDisplayOptionsManager.getMgr().getDisplayOptions().getMinShadowSubmodelLinks();
    if (doPrune) {
      linkList = pruneToMinSubModel(bfn, targetList, linkList);
    }
      
    HashMap<NID.WithName, Integer> lastColForNode = new HashMap<NID.WithName, Integer>();
    HashMap<NID.WithName, Integer> lastShadColForNode = new HashMap<NID.WithName, Integer>();
    Iterator<LinkInfo> lcit = linkList.iterator();
    while (lcit.hasNext()) {
      LinkInfo linf = lcit.next();
      if (!linf.isShadow()) {
        Integer lastCol = lastColForNode.get(linf.getTarget());
        if ((lastCol == null) || (lastCol.intValue() < linf.getUseColumn(false))) {
          lastColForNode.put(linf.getTarget(), Integer.valueOf(linf.getUseColumn(false)));
        }
        lastCol = lastColForNode.get(linf.getSource());
        if ((lastCol == null) || (lastCol.intValue() < linf.getUseColumn(false))) {
          lastColForNode.put(linf.getSource(), Integer.valueOf(linf.getUseColumn(false)));
        }
      }
      Integer lastColShad = lastShadColForNode.get(linf.getTarget());
      if ((lastColShad == null) || (lastColShad.intValue() < linf.getUseColumn(true))) {
        lastShadColForNode.put(linf.getTarget(), Integer.valueOf(linf.getUseColumn(true)));
      }
      lastColShad = lastShadColForNode.get(linf.getSource());
      if ((lastColShad == null) || (lastColShad.intValue() < linf.getUseColumn(true))) {
        lastShadColForNode.put(linf.getSource(), Integer.valueOf(linf.getUseColumn(true)));
      }
    }

    //
    // Need to compress the rows and columns, throwing away all empty slots
    // First record the "full scale" entries:
    //

    TreeSet<Integer> needRows = new TreeSet<Integer>();
    TreeSet<Integer> needColumns = new TreeSet<Integer>();   
    TreeSet<Integer> needColumnsShad = new TreeSet<Integer>();   

    Iterator<NodeInfo> tgit = targetList.iterator();
    while (tgit.hasNext()) {
      NodeInfo targetInf = tgit.next();
      needRows.add(Integer.valueOf(targetInf.nodeRow));
      needColumns.add(Integer.valueOf(targetInf.getColRange(false).min));
      needColumnsShad.add(Integer.valueOf(targetInf.getColRange(true).min));
    }

    Iterator<LinkInfo> cit = linkList.iterator();
    while (cit.hasNext()) {
      LinkInfo linf = cit.next();
      needRows.add(Integer.valueOf(linf.getStartRow()));
      needRows.add(Integer.valueOf(linf.getEndRow()));
      if (!linf.isShadow()) {
        needColumns.add(Integer.valueOf(linf.getUseColumn(false)));
      }
      needColumnsShad.add(Integer.valueOf(linf.getUseColumn(true)));
    }

    //
    // Create full-scale to mini-scale mappings:
    //

    TreeMap<Integer, Integer> rowMap = new TreeMap<Integer, Integer>();
    TreeMap<Integer, Integer> columnMap = new TreeMap<Integer, Integer>();
    TreeMap<Integer, Integer> shadColumnMap = new TreeMap<Integer, Integer>();

    int rowCount = 0;
    Iterator<Integer> mrit = needRows.iterator();
    while (mrit.hasNext()) {
      Integer fullRow = mrit.next();
      rowMap.put(fullRow, Integer.valueOf(rowCount++));
    }

    int colCount = 0;
    Iterator<Integer> mcit = needColumns.iterator();
    while (mcit.hasNext()) {
      Integer fullCol = mcit.next();
      columnMap.put(fullCol, Integer.valueOf(colCount++));
    }
    
    int shadColCount = 0;
    Iterator<Integer> ncsit = needColumnsShad.iterator();
    while (ncsit.hasNext()) {
      Integer fullCol = ncsit.next();
      shadColumnMap.put(fullCol, Integer.valueOf(shadColCount++));
    }
  
    //
    // Create modified copies of the node and link info with
    // compressed rows and columns:
    //

    ArrayList<NodeInfo> modTargetList = new ArrayList<NodeInfo>();
    ArrayList<LinkInfo> modLinkList = new ArrayList<LinkInfo>();

    int maxShadLinkCol = Integer.MIN_VALUE;
    int maxLinkCol = Integer.MIN_VALUE;
    cit = linkList.iterator();
    while (cit.hasNext()) {
      BioFabricNetwork.LinkInfo linf = cit.next();
      Integer startRowObj = rowMap.get(Integer.valueOf(linf.getStartRow()));
      Integer endRowObj = rowMap.get(Integer.valueOf(linf.getEndRow()));
      Integer miniColObj = (linf.isShadow()) ? Integer.valueOf(Integer.MIN_VALUE) : columnMap.get(Integer.valueOf(linf.getUseColumn(false)));
      Integer miniColShadObj = shadColumnMap.get(Integer.valueOf(linf.getUseColumn(true)));
      int miniColVal = miniColObj.intValue();
      if (miniColVal > maxLinkCol) {
        maxLinkCol = miniColVal;
      }
      int miniColShadVal = miniColShadObj.intValue();
      if (miniColShadVal > maxShadLinkCol) {
        maxShadLinkCol = miniColShadVal;
      }
      
      LinkInfo miniLinf = new LinkInfo(linf.getLink(), startRowObj.intValue(), endRowObj.intValue(), 
                                       miniColObj.intValue(), miniColShadObj.intValue(), linf.getColorKey());
      modLinkList.add(miniLinf);
    }
    if (maxLinkCol == Integer.MIN_VALUE) {
      maxLinkCol = 1;
    } else {
      maxLinkCol++;
    }
    if (maxShadLinkCol == Integer.MIN_VALUE) {
      maxShadLinkCol = 1;
    } else {
      maxShadLinkCol++;
    }
 
    int minTrgCol = Integer.MAX_VALUE;
    int minShadTrgCol = Integer.MAX_VALUE;
    tgit = targetList.iterator();
    while (tgit.hasNext()) {
      NodeInfo infoFull = tgit.next();
      Integer miniRowObj = rowMap.get(Integer.valueOf(infoFull.nodeRow));
      NodeInfo infoMini = new NodeInfo(infoFull.getNodeID(), infoFull.getNodeName(), miniRowObj.intValue(), infoFull.colorKey);

      Integer minCol = columnMap.get(Integer.valueOf(infoFull.getColRange(false).min));
      infoMini.updateMinMaxCol(minCol.intValue(), false);
      int miniColVal = minCol.intValue();
      if (miniColVal < minTrgCol) {
        minTrgCol = miniColVal;
      }
      
      Integer minShadCol = shadColumnMap.get(Integer.valueOf(infoFull.getColRange(true).min));
      infoMini.updateMinMaxCol(minShadCol.intValue(), true);
      int miniShadColVal = minShadCol.intValue();
      if (miniShadColVal < minShadTrgCol) {
        minShadTrgCol = miniShadColVal;
      }
   
      int maxCol;        
      Integer lastCol = lastColForNode.get(infoFull.getNodeID());
      if (lastCol == null) {
        maxCol = maxLinkCol;
      } else {
        Integer maxColObj = columnMap.get(lastCol);     
        maxCol = maxColObj.intValue();
      }
      infoMini.updateMinMaxCol(maxCol, false);
      
      int maxShadCol;        
      Integer lastShadCol = lastShadColForNode.get(infoFull.getNodeID());
      if (lastShadCol == null) {
        maxShadCol = maxShadLinkCol;
      } else {
        Integer maxShadColObj = shadColumnMap.get(lastShadCol);     
        maxShadCol = maxShadColObj.intValue();
      }
      infoMini.updateMinMaxCol(maxShadCol, true);
  
      modTargetList.add(infoMini);
    }
    if (minTrgCol == Integer.MAX_VALUE) {
      minTrgCol = 0;
    }
    
    rowToTargID_ = new HashMap<Integer, NID.WithName>();
    nodeDefs_ = new HashMap<NID.WithName, NodeInfo>();
    rowCount_ = modTargetList.size();
    for (int i = 0; i < rowCount_; i++) {
      NodeInfo infoMini = modTargetList.get(i);
      NID.WithName nwn = new NID.WithName(infoMini.getNodeID(), infoMini.getNodeName());
      rowToTargID_.put(Integer.valueOf(infoMini.nodeRow), nwn);
      nodeDefs_.put(nwn, infoMini);
    }
    
    normalCols_ = new ColumnAssign();
    shadowCols_ = new ColumnAssign();
    fullLinkDefs_ = new TreeMap<Integer, LinkInfo>();
    nonShadowedLinkMap_ = new TreeMap<Integer, Integer>();
    
    int numMll = modLinkList.size();
    for (int i = 0; i < numMll; i++) {
      BioFabricNetwork.LinkInfo infoMini = modLinkList.get(i);
      
      Integer useShadCol = Integer.valueOf(infoMini.getUseColumn(true));
      fullLinkDefs_.put(useShadCol, infoMini);      
      shadowCols_.columnToSource.put(useShadCol, infoMini.getSource());   
      shadowCols_.columnToTarget.put(useShadCol, infoMini.getTarget());
           
      if (!infoMini.isShadow()) {
        Integer useCol = Integer.valueOf(infoMini.getUseColumn(false));
        nonShadowedLinkMap_.put(useCol, useShadCol);      
        normalCols_.columnToSource.put(useCol, infoMini.getSource());   
        normalCols_.columnToTarget.put(useCol, infoMini.getTarget());
      }
    }
    
    normalCols_.columnCount = maxLinkCol - minTrgCol;
    shadowCols_.columnCount = maxShadLinkCol - minShadTrgCol;
    
    //
    // Need to build drain zones.  For each node, start at max col and back up.
    // Drain starts at max IF the source is the top node.  Continues until
    // we stop having nodes in the next slot that are the top nodes.
    //
    // We only keep a drain zone when it is at the top of the display.  We can
    // check this by seeing if the last link for the node has the node at the top
    // (keep drain) or the bottom (drop drain) of the link.
    //
    // That logic only applies with non-shadow presentation.  For shadowed, everybody 
    // with a link has a drain zone
    //
    
    Iterator<NID.WithName> ndkit = nodeDefs_.keySet().iterator();
    while (ndkit.hasNext()) {
      NID.WithName node = ndkit.next();
      NodeInfo srcNI = nodeDefs_.get(node);
      
      //
      // Non-shadow calcs:
      //
      MinMax srcDrain = null;
      MinMax range = srcNI.getColRange(false);
      int startCol = range.max;
      int endCol = range.min;
      for (int i = startCol; i >= endCol; i--) {
        LinkInfo linf = getLinkDefinition(Integer.valueOf(i), false);
        // done when no longer contiguous:
        if ((linf == null) || (!linf.getSource().equals(node) && !linf.getTarget().equals(node))) {
          break;
        }
        // Second case allows feedback:
        if ((linf.bottomRow() == srcNI.nodeRow) && (linf.topRow() != srcNI.nodeRow)) {
          break;
        }
        if (linf.topRow() == srcNI.nodeRow) {
          if (srcDrain == null) {
            srcDrain = new MinMax();
            srcDrain.init();
          }
          srcDrain.update(i); 
        }        
      }
      if (srcDrain != null) {
        srcNI.addDrainZone(new DrainZone(srcDrain, false));
      }
      
      //
      // Shadow calcs:
      //
      
      MinMax shadowSrcDrain = null;
      MinMax shadowRange = srcNI.getColRange(true);
      int startColShad = shadowRange.min;
      int endColShad = shadowRange.max;
      for (int i = startColShad; i <= endColShad; i++) {
        LinkInfo linf = getLinkDefinition(Integer.valueOf(i), true);
        if (linf == null) {
          continue;
        }
        boolean isShadow = ((linf != null) && linf.isShadow());
   //     if (!isShadow && (shadowSrcDrain == null)) {
    //      continue;
    //    }
        // done when no longer contiguous:
        if (shadowSrcDrain != null) {
          if ((linf == null) || (!linf.getSource().equals(node) && !linf.getTarget().equals(node))) {
            break;
          }
        }
        if (((linf.topRow() == srcNI.nodeRow) && !isShadow) || 
             (linf.bottomRow() == srcNI.nodeRow) && isShadow) {
          if (shadowSrcDrain == null) {
            shadowSrcDrain = new MinMax();
            shadowSrcDrain.init();
          }
          shadowSrcDrain.update(i); 
        }        
      }
      if (shadowSrcDrain != null) {
        srcNI.addDrainZone(new DrainZone(shadowSrcDrain, true));
      }
    }
 
    return;
  }


  /***************************************************************************
  ** 
  ** Fill out node info from order
  */

  private void fillNodesFromOrder(List<NID.WithName> targetIDs, 
  		                            FabricColorGenerator colGen, Map<NID.WithName, String> clustAssign) {
    //
    // Now have the ordered list of targets we are going to display.
    // Build target->row maps and the inverse:
    //
    
    int numColors = colGen.getNumColors();

    int currRow = 0;
    Iterator<NID.WithName> trit = targetIDs.iterator();
    while (trit.hasNext()) {
      NID.WithName targetID = trit.next();
      Integer rowObj = Integer.valueOf(currRow);
      rowToTargID_.put(rowObj, targetID);
      String colorKey = colGen.getGeneColor(currRow % numColors);
      NodeInfo nextNI = new NodeInfo(targetID.getNID(), targetID.getName(), currRow++, colorKey);
      if (clustAssign != null) {
      	nextNI.setCluster(clustAssign.get(targetID));
      }
      nodeDefs_.put(targetID, nextNI);
    }
    rowCount_ = targetIDs.size();
    return;
  }
  
  /***************************************************************************
  **
  ** Add a node def
  */
  
  void addNodeInfoForIO(NodeInfo nif) {
  	NID.WithName nwn = new NID.WithName(nif.getNodeID(), nif.getNodeName());
    nodeDefs_.put(nwn, nif);
    rowCount_ = nodeDefs_.size();   
    rowToTargID_.put(Integer.valueOf(nif.nodeRow), nwn);
    return;
  }
  
  /***************************************************************************
  ** 
  ** Determine the start & end of each target row needed to handle the incoming
  ** and outgoing links:
  */

  private void trimTargetRows() {
    Iterator<Integer> fldit = fullLinkDefs_.keySet().iterator();
    while (fldit.hasNext()) {
      Integer colNum = fldit.next();
      LinkInfo li = fullLinkDefs_.get(colNum);
      shadowCols_.columnToSource.put(colNum, li.getSource());
      shadowCols_.columnToTarget.put(colNum, li.getTarget());
      NodeInfo srcNI = nodeDefs_.get(li.getSource());    
      NodeInfo trgNI = nodeDefs_.get(li.getTarget()); 
      srcNI.updateMinMaxCol(colNum.intValue(), true);
      trgNI.updateMinMaxCol(colNum.intValue(), true);
    }
    Iterator<Integer> nslit = nonShadowedLinkMap_.keySet().iterator();
    while (nslit.hasNext()) {
      Integer colNum = nslit.next();
      Integer mappedCol = nonShadowedLinkMap_.get(colNum);
      LinkInfo li = fullLinkDefs_.get(mappedCol);
      normalCols_.columnToSource.put(colNum, li.getSource());
      normalCols_.columnToTarget.put(colNum, li.getTarget());
      NodeInfo srcNI = nodeDefs_.get(li.getSource());    
      NodeInfo trgNI = nodeDefs_.get(li.getTarget());    
      srcNI.updateMinMaxCol(colNum.intValue(), false);
      trgNI.updateMinMaxCol(colNum.intValue(), false);
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** For the lone nodes, they are assigned into the last column:
  */

  private void loneNodesToLastColumn(Set<NID.WithName> loneNodeIDs) {
    Iterator<NID.WithName> lnit = loneNodeIDs.iterator();
    while (lnit.hasNext()) {
      NID.WithName loneID = lnit.next();
      NodeInfo loneNI = nodeDefs_.get(loneID);     
      loneNI.updateMinMaxCol(normalCols_.columnCount - 1, false);
      loneNI.updateMinMaxCol(shadowCols_.columnCount - 1, true);
    }    
    return;
  }
  
  /***************************************************************************
  ** 
  ** Pretty icky hack:
  */

  private Set<NID.WithName> getLoneNodes() {
    HashSet<NID.WithName> retval = new HashSet<NID.WithName>();
    Iterator<NID.WithName> lnit = nodeDefs_.keySet().iterator();
    boolean checkDone = false;
    while (lnit.hasNext()) {
      NID.WithName loneID = lnit.next();
      NodeInfo loneNI = nodeDefs_.get(loneID);
      int min = loneNI.getColRange(true).min;
      int max = loneNI.getColRange(true).max;
      
      if ((min == max) && (min == (shadowCols_.columnCount - 1))) {
        if (!checkDone) {
          if (shadowCols_.columnToSource.get(Integer.valueOf(min)) != null) {
            return (retval);
          } else {
            checkDone = true;
          }
        }
        retval.add(loneID);
      }    
    }    
    return (retval);
  }
 
 
 
  /***************************************************************************
  **
  ** Add a link def
  */
  
  private Integer[] addLinkDef(FabricLink nextLink, int numColors, int noShadowCol, int shadowCol, FabricColorGenerator colGen) {
    Integer[] retval = new Integer[2]; 
    String key = colGen.getGeneColor(shadowCol % numColors);
    int srcRow = nodeDefs_.get(nextLink.getSrcID()).nodeRow;
    int trgRow = nodeDefs_.get(nextLink.getTrgID()).nodeRow;
    LinkInfo linf = new LinkInfo(nextLink, srcRow, trgRow, noShadowCol, shadowCol, key);
    Integer shadowKey = Integer.valueOf(shadowCol);
    fullLinkDefs_.put(shadowKey, linf);
    retval[0] = Integer.valueOf(shadowCol + 1); 
    if (!linf.isShadow()) {
      nonShadowedLinkMap_.put(Integer.valueOf(noShadowCol), shadowKey);
      retval[1] = Integer.valueOf(noShadowCol + 1); 
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Add a link def
  */
  
  void addLinkInfoForIO(LinkInfo linf) {
    int useColVal = linf.getUseColumn(true);
    Integer useCol = Integer.valueOf(useColVal);
    fullLinkDefs_.put(useCol, linf);    
    if (useColVal > shadowCols_.columnCount) {
      shadowCols_.columnCount = useColVal;
    }
    shadowCols_.columnToSource.put(useCol, linf.getSource());   
    shadowCols_.columnToTarget.put(useCol, linf.getTarget());
    
    if (!linf.isShadow()) {
      int useNColVal = linf.getUseColumn(false);
      Integer useNCol = Integer.valueOf(useNColVal);
      nonShadowedLinkMap_.put(useNCol, useCol);    
      if (useNColVal > normalCols_.columnCount) {
        normalCols_.columnCount = useNColVal;
      }
      normalCols_.columnToSource.put(useNCol, linf.getSource());   
      normalCols_.columnToTarget.put(useNCol, linf.getTarget());
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Add a link group tag
  */
  
  void addLinkGroupForIO(String tag) {
    this.linkGrouping_.add(tag);
    return;
  }
 
  /***************************************************************************
  **
  ** Set color generator (I/0)
  */
  
  void setColorGenerator(FabricColorGenerator fcg) {
    colGen_ = fcg;
    return;
  } 
  
  /***************************************************************************
  **
  ** Answer if we have node cluster assignments
  */
  
  public boolean nodeClustersAssigned() {
  	if (nodeDefs_.isEmpty()) {
  		return (false);
  	}
    NodeInfo ni = nodeDefs_.values().iterator().next();
    return (ni.getCluster() != null);
  } 
  
  /***************************************************************************
  **
  ** Return node cluster assignment
  */
  
  public Map<NID.WithName, String> nodeClusterAssigment() {
  	HashMap<NID.WithName, String> retval = new HashMap<NID.WithName, String>();
  	if (nodeDefs_.isEmpty()) {
  		return (retval);
  	}
    for (NodeInfo ni : nodeDefs_.values()) {
    	String cluster = ni.getCluster();
    	if (cluster == null) {
    		throw new IllegalStateException();
    	}
    	retval.put(new NID.WithName(ni.getNodeID(), ni.getNodeName()), cluster);
    }
    return (retval);    
  } 
  
  /***************************************************************************
  **
  ** Set node cluster assignment
  */
  
  public void setNodeClusterAssigment(Map<NID, String> assign) {
  	if (nodeDefs_.isEmpty()) {
  		throw new IllegalStateException();
  	}
    for (NodeInfo ni : nodeDefs_.values()) {
    	String cluster = assign.get(ni.getNodeID());
    	if (cluster == null) {
    		throw new IllegalArgumentException();
    	}
    	ni.setCluster(cluster);
    }
    return;    
  } 
  
  /***************************************************************************
  **
  ** Constructor for I/O only
  */

  BioFabricNetwork() { 
    normalCols_ = new ColumnAssign();
    shadowCols_ = new ColumnAssign();
    rowToTargID_ = new HashMap<Integer, NID.WithName>(); 
    fullLinkDefs_ = new TreeMap<Integer, LinkInfo>();
    nonShadowedLinkMap_ = new TreeMap<Integer, Integer>();
    nodeDefs_ = new HashMap<NID.WithName, NodeInfo>();
    linkGrouping_ = new ArrayList<String>();
    colGen_ = null;
  }
  
  /***************************************************************************
  **
  ** Info for a link
  */  
  
  public static class LinkInfo {
    private FabricLink myLink_;
    private int startRow_;
    private int endRow_;
    private int noShadowColumn_;
    private int shadowColumn_;
    private String colorKey_;
    
    public LinkInfo(FabricLink flink, int startRow, int endRow, int noShadowColumn, int shadowColumn, String colorKey) {
      myLink_ = flink.clone();
      startRow_ = startRow;
      endRow_ = endRow;
      noShadowColumn_ = noShadowColumn;
      shadowColumn_ = shadowColumn;
      colorKey_ = colorKey;
    }
    
    public int getStartRow() {
      return (startRow_);
    }  
    
    public int getEndRow() {
      return (endRow_);
    }  
    
    public int getUseColumn(boolean shadowEnabled) {
      if (!shadowEnabled && myLink_.isShadow()) {
        // FIX ME: Seen this case with submodel creation?
        throw new IllegalStateException();
      }
      return ((shadowEnabled) ? shadowColumn_ : noShadowColumn_);
    }  
    
    public String getColorKey() {
      return (colorKey_);
    }  
       
    public FabricLink getLink() {
      return (myLink_);
    }    
    
    public NID.WithName getSource() {
      return (myLink_.getSrcID());
    }
    
    public NID.WithName getTarget() {
      return (myLink_.getTrgID());
    }
    
    public FabricLink.AugRelation getAugRelation() {
      return (myLink_.getAugRelation());
    }
    
    public boolean isShadow() {
      return (myLink_.isShadow());
    }
    
    public boolean isDirected() {
      return (myLink_.isDirected());
    }
        
    public int bottomRow() {
      return (Math.max(startRow_, endRow_));
    } 
    
    public int topRow() {
      return (Math.min(startRow_, endRow_));
    } 
    
    public boolean inLinkRowRange(int row) {
      return ((row >= topRow()) && (row <= bottomRow()));
    } 
  }  
  
  /***************************************************************************
  **
  ** Node info
  */  
  
  public static class NodeInfo {
  	private NID nodeID_;
    private String nodeName_;
    public int nodeRow;
    public String colorKey;
    private String cluster_;
        
    private MinMax colRangeSha_;
    private MinMax colRangePln_;
    
    private List<DrainZone> shadowDrainZones_;
    private List<DrainZone> plainDrainZones_;
    
    NodeInfo(NID nodeID, String nodeName, int nodeRow, String colorKey) {
    	nodeID_ = nodeID;
      nodeName_ = nodeName;
      this.nodeRow = nodeRow;
      this.colorKey = colorKey;
      colRangeSha_ = new MinMax();
      colRangeSha_.init();
      colRangePln_ = new MinMax();
      colRangePln_.init();
      shadowDrainZones_ = new ArrayList<DrainZone>();
      cluster_ = null;
      plainDrainZones_ = new ArrayList<DrainZone>();
    }
      
    public String getNodeName() { 
      return (nodeName_);
    }
    
    public NID getNodeID() { 
      return (nodeID_);
    } 
    
    public NID.WithName getNodeIDWithName() { 
      return (new NID.WithName(nodeID_, nodeName_));
    } 
 
    public List<DrainZone> getDrainZones(boolean forShadow) {
      return (forShadow) ? new ArrayList<DrainZone>(shadowDrainZones_) : new ArrayList<DrainZone>(plainDrainZones_);
    }
    
    public void addDrainZone(DrainZone dz) {
      if (dz.isShadow()) {
        shadowDrainZones_.add(dz);
      } else {
        plainDrainZones_.add(dz);
      }
      return;
    }
  
    public void setDrainZones(List<DrainZone> zones, boolean forShadow) {
      if (forShadow) {
        shadowDrainZones_ = new ArrayList<DrainZone>(zones);
      } else {
        plainDrainZones_ = new ArrayList<DrainZone>(zones);
      }
    }
    
    public MinMax getColRange(boolean forShadow) { 
      return (forShadow) ? colRangeSha_ : colRangePln_;
    }
      
    void updateMinMaxCol(int i, boolean forShadow) {
      MinMax useMM = (forShadow) ? colRangeSha_ : colRangePln_;
      useMM.update(i);
      return;
    }
    
    public void setCluster(String cluster) {
      cluster_ = cluster;
      return;
    }
    
    public String getCluster() {
      return (cluster_);
    }
    
    /***************************************************************************
    **
     ** Dump the node using XML
    */
  
    public void writeXML(PrintWriter out, Indenter ind, int row) {
      ind.indent();
      out.print("<node name=\"");
      out.print(CharacterEntityMapper.mapEntities(nodeName_, false));
      out.print("\" nid=\"");
      out.print(nodeID_.getInternal());
      out.print("\" row=\"");
      out.print(row);
      MinMax nsCols = getColRange(false);
      out.print("\" minCol=\"");
      out.print(nsCols.min);
      out.print("\" maxCol=\"");
      out.print(nsCols.max);
      MinMax sCols = getColRange(true);
      out.print("\" minColSha=\"");
      out.print(sCols.min);
      out.print("\" maxColSha=\"");
      out.print(sCols.max);
      out.print("\" color=\"");
      out.print(colorKey);
      String clust = getCluster();
      if (clust != null) {
        out.print("\" cluster=\"");
        out.print(CharacterEntityMapper.mapEntities(clust, false));
      }

      out.println("\">");
      
      //
      // DRAIN ZONES XML
      //
      
      ind.up();
      ind.indent();
      if (this.plainDrainZones_.size() > 0) {
        out.println("<drainZones>");
        ind.up();
        for (DrainZone dz : this.plainDrainZones_) {
          dz.writeXML(out, ind);
        }
        ind.down();
        ind.indent();
        out.println("</drainZones>");
      } else {
        out.println("<drainZones/>");
      }
      
      ind.indent();
      if (this.shadowDrainZones_.size() > 0) {
        out.println("<drainZonesShadow>");
        ind.up();
        for (DrainZone dzSha : this.shadowDrainZones_) {
          dzSha.writeXML(out, ind);
        }
        ind.down();
        ind.indent();
        out.println("</drainZonesShadow>");
      } else {
        out.println("<drainZonesShadow/>");
      }
      
      ind.down();
      ind.indent();
      out.println("</node>");
    }
  }
  
  /***************************************************************************
   **
   ** Drain Zone
   */
  
  public static class DrainZone {
    
    private MinMax dzmm;
    private boolean isShadow;
    
    public DrainZone(MinMax dzmm, boolean isShadow){
      this.dzmm = dzmm.clone();
      this.isShadow = isShadow;
    }
    
    public void writeXML(PrintWriter out, Indenter ind) {
      if (isShadow) {
        ind.indent();
        out.print("<drainZoneShadow minCol=\"");
        out.print(dzmm.min);
        out.print("\" maxCol=\"");
        out.print(dzmm.max);
        out.println("\" />");
      } else {
        ind.indent();
        out.print("<drainZone minCol=\"");
        out.print(dzmm.min);
        out.print("\" maxCol=\"");
        out.print(dzmm.max);
        out.println("\" />");
      }
    }
    
    public boolean isShadow() {
      return isShadow;
    }
    
    public MinMax getMinMax() {
      return dzmm;
    }
  }
  
  /***************************************************************************
  **
  ** For passing around build data
  */  
  
  public static abstract class BuildData {
    protected BuildMode mode;
    
    public BuildData(BuildMode mode) {
      this.mode = mode;
    }
       
    public BuildMode getMode() {
      return (mode);
    }  
    
    public boolean canRestore() {
      return (true);
    }  
  }

  /***************************************************************************
  **
  ** For passing around build data
  */  
  
  public static class PreBuiltBuildData extends BuildData {
    BioFabricNetwork bfn;
  
    public PreBuiltBuildData(BioFabricNetwork bfn, BuildMode mode) {
      super(mode);
      this.bfn = bfn;
    } 
  }
  
  /***************************************************************************
  **
  ** For passing around build data
  */  
  
  public static class RelayoutBuildData extends BuildData {
    public BioFabricNetwork bfn;
    public Set<FabricLink> allLinks;
    public Set<NID.WithName> loneNodeIDs;
    public FabricColorGenerator colGen;
    public Map<NID.WithName, Integer> nodeOrder;
    public List<NID.WithName> existingIDOrder;
    public SortedMap<Integer, FabricLink> linkOrder;
    public List<String> linkGroups;
    public Set<NID.WithName> allNodeIDs;
    public Map<NID.WithName, String> clustAssign;
    public LayoutMode layoutMode;
    public UniqueLabeller idGen; 
    
    public RelayoutBuildData(BioFabricNetwork fullNet, BuildMode mode) {
      super(mode);
      this.bfn = fullNet;
      this.allLinks = fullNet.getAllLinks(true);
      this.colGen = fullNet.colGen_;
      this.nodeOrder = null;
      this.existingIDOrder = fullNet.existingIDOrder();
      this.linkOrder = null;
      this.linkGroups = fullNet.linkGrouping_;
      this.loneNodeIDs = fullNet.getLoneNodes();
      this.allNodeIDs = fullNet.nodeDefs_.keySet();
      this.clustAssign = (fullNet.nodeClustersAssigned()) ? fullNet.nodeClusterAssigment() : null;
      this.layoutMode = fullNet.getLayoutMode();
      this.idGen = fullNet.nodeIDGenerator_;
    }
    
    public RelayoutBuildData(UniqueLabeller idGen,
    		                     Set<FabricLink> allLinks, Set<NID.WithName> loneNodeIDs, 
    		                     Map<NID.WithName, String> clustAssign, 
    		                     FabricColorGenerator colGen, BuildMode mode) {
      super(mode);
      this.bfn = null;
      this.allLinks = allLinks;
      this.colGen = colGen;
      this.nodeOrder = null;
      this.existingIDOrder = null;
      this.linkOrder = null;
      this.linkGroups = new ArrayList<String>();
      this.clustAssign = clustAssign;
      this.loneNodeIDs = loneNodeIDs;
      this.allNodeIDs = null;
      this.layoutMode = LayoutMode.PER_NODE_MODE;   
      this.idGen = idGen; 
    } 

    public Map<String, Set<NID.WithName>> genNormNameToID() {
    	HashMap<String, Set<NID.WithName>> retval = new HashMap<String, Set<NID.WithName>>();
    	Iterator<NID.WithName> nit = this.allNodeIDs.iterator();
    	while (nit.hasNext()) {
    		NID.WithName nodeID = nit.next();
    		String name = nodeID.getName();
  		  String nameNorm = DataUtil.normKey(name);
  	   	Set<NID.WithName> forName = retval.get(nameNorm);
  		  if (forName == null) {
  			  forName = new HashSet<NID.WithName>();
  			  retval.put(nameNorm, forName);
  		  }
  		  forName.add(nodeID);
  	  }
      return (retval);	
    }
    
    public void setNodeOrderFromAttrib(Map<AttributeLoader.AttributeKey, String> nodeOrderIn) {
      this.nodeOrder = new HashMap<NID.WithName, Integer>();
      Map<String, Set<NID.WithName>> nameToID = genNormNameToID();
      for (AttributeLoader.AttributeKey key : nodeOrderIn.keySet()) {
        try {
          Integer newRow = Integer.valueOf(nodeOrderIn.get(key));
          String keyName = ((AttributeLoader.StringKey)key).key;
          String normName = DataUtil.normKey(keyName);        
          Set<NID.WithName> ids = nameToID.get(normName);
          if (ids.size() != 1) {
          	throw new IllegalStateException();
          }
          NID.WithName id = ids.iterator().next();
          this.nodeOrder.put(id, newRow);
        } catch (NumberFormatException nfex) {
          throw new IllegalStateException();
        }
      }
      return;
    }
    
    public void setNodeOrder(Map<NID.WithName, Integer> nodeOrder) {
      this.nodeOrder = nodeOrder;
      return;
    }

    public void setLinkOrder(SortedMap<Integer, FabricLink> linkOrder) {
      this.linkOrder = linkOrder;
      return;
    }
    
    public void setGroupOrderAndMode(List<String> groupOrder, LayoutMode mode) {
      this.linkGroups = groupOrder;
      this.layoutMode = mode;
      return;
    }
  }
 
  /***************************************************************************
  **
  ** For passing around build data
  */  
  
  public static class SelectBuildData extends BuildData {
     BioFabricNetwork fullNet;
     List<NodeInfo> subNodes;
     List<LinkInfo> subLinks;

    public SelectBuildData(BioFabricNetwork fullNet, List<NodeInfo> subNodes, List<LinkInfo> subLinks) {
      super(BuildMode.BUILD_FOR_SUBMODEL);
      this.fullNet = fullNet;
      this.subNodes = subNodes;
      this.subLinks = subLinks;
    } 
  }
  
  /***************************************************************************
  **
  ** For passing around ranked nodes
  */  
  
  static class DoubleRanked  {
     double rank;
     String id;
     Link byLink;

    DoubleRanked(double rank, String id, Link byLink) {
      this.rank = rank;
      this.id = id;
      this.byLink = byLink;
    } 
  }
  
  /***************************************************************************
  **
  ** For storing column assignments
  */  
  
  public static class ColumnAssign  {
    public HashMap<Integer, NID.WithName> columnToSource;
    public HashMap<Integer, NID.WithName> columnToTarget;
    public int columnCount;

    ColumnAssign() {
      this.columnToSource = new HashMap<Integer, NID.WithName>();
      this.columnToTarget = new HashMap<Integer, NID.WithName>();
      this.columnCount = 0;
    } 
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  ** 
  ** Extract relations
  */

  public static SortedMap<FabricLink.AugRelation, Boolean> extractRelations(List<FabricLink> allLinks) {
    HashSet<FabricLink> flipSet = new HashSet<FabricLink>();
    HashSet<FabricLink.AugRelation> flipRels = new HashSet<FabricLink.AugRelation>();
    HashSet<FabricLink.AugRelation> rels = new HashSet<FabricLink.AugRelation>(); 
    Iterator<FabricLink> alit = allLinks.iterator();
    while (alit.hasNext()) {
      FabricLink nextLink = alit.next();
      FabricLink.AugRelation relation = nextLink.getAugRelation();
      if (!nextLink.isFeedback()) {  // Autofeedback not flippable
        FabricLink flipLink = nextLink.flipped();
        if (flipSet.contains(flipLink)) {
          flipRels.add(relation);
        } else {
          flipSet.add(nextLink);
        }
      }
      rels.add(relation);
    } 
    
    //
    // We have a hint that something is signed if there are two
    // separate links running in opposite directions!
    //
        
    TreeMap<FabricLink.AugRelation, Boolean> relMap = new TreeMap<FabricLink.AugRelation, Boolean>();
    Boolean noDir = new Boolean(false);
    Boolean haveDir = new Boolean(true);
    Iterator<FabricLink.AugRelation> rit = rels.iterator();
    while (rit.hasNext()) {
      FabricLink.AugRelation rel = rit.next();
      relMap.put(rel, (flipRels.contains(rel)) ? haveDir : noDir);
    }    
    return (relMap);
  }
  
  /***************************************************************************
  ** 
  ** Process a link set that has not had directionality established
  */

  public static void assignDirections(List<FabricLink> allLinks, Map<FabricLink.AugRelation, Boolean> relMap) {    
    Iterator<FabricLink> alit = allLinks.iterator();
    while (alit.hasNext()) {
      FabricLink nextLink = alit.next();
      FabricLink.AugRelation rel = nextLink.getAugRelation();
      Boolean isDir = relMap.get(rel);
      nextLink.installDirection(isDir);
    }
    return;
  }  
  
  /***************************************************************************
  ** 
  ** Helper to drop to map to single name: useful
  */

  public static Map<String, NID.WithName> reduceNameSetToOne(Map<String, Set<NID.WithName>> mapsToSets) { 
  	HashMap<String, NID.WithName> retval = new HashMap<String, NID.WithName>();
    Iterator<String> alit = mapsToSets.keySet().iterator();
    while (alit.hasNext()) {
      String nextName = alit.next();
      Set<NID.WithName> forName = mapsToSets.get(nextName);
      if (forName.size() != 1) {
      	throw new IllegalStateException();
      }
      retval.put(nextName, forName.iterator().next());
    }
    return (retval);
  }  
  
  /***************************************************************************
  ** 
  ** This culls a set of links to remove non-directional synonymous and
  ** duplicate links.  Note that shadow links have already been created
  ** and added to the allLinks list. 
  */

  public static void preprocessLinks(List<FabricLink> allLinks, Set<FabricLink> retval, Set<FabricLink> culled) {
  	FabricLink.FabLinkComparator flc = new FabricLink.FabLinkComparator();
    Iterator<FabricLink> alit = allLinks.iterator();
    while (alit.hasNext()) {
      FabricLink nextLink = alit.next();
      if (retval.contains(nextLink)) {
        culled.add(nextLink);
      } else if (!nextLink.isDirected()) {
        if (!nextLink.isFeedback()) {
          FabricLink flipLink = nextLink.flipped();
          if (retval.contains(flipLink)) {
            // Make the order consistent for a given src & pair!
            if (flc.compare(nextLink, flipLink) < 0) {
              retval.remove(flipLink);
              culled.add(flipLink);
              retval.add(nextLink);
            } else {
              culled.add(nextLink);              
            }  
          } else {
            retval.add(nextLink);
          }
        } else {
          retval.add(nextLink);
        }
      } else {
        retval.add(nextLink);
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class NetworkDataWorker extends AbstractFactoryClient {
    
    public NetworkDataWorker(FabricFactory.FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("BioFabric");
      installWorker(new FabricColorGenerator.ColorSetWorker(whiteboard), new MyColorSetGlue());
      installWorker(new FabricDisplayOptions.FabricDisplayOptionsWorker(whiteboard), null);
      installWorker(new NodeInfoWorker(whiteboard), new MyNodeGlue());
      installWorker(new LinkInfoWorker(whiteboard), new MyLinkGlue());
      installWorker(new LinkGroupWorker(whiteboard), null);
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("BioFabric")) {
        FabricFactory.FactoryWhiteboard board = (FabricFactory.FactoryWhiteboard)this.sharedWhiteboard_;
        board.bfn = new BioFabricNetwork();
        retval = board.bfn;
      }
      return (retval);     
    }
  }
  
  public static class MyColorSetGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FabricFactory.FactoryWhiteboard board = (FabricFactory.FactoryWhiteboard)optionalArgs;
      board.bfn.setColorGenerator(board.fcg);
      return (null);
    }
  }  
  
  public static class MyNodeGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FabricFactory.FactoryWhiteboard board = (FabricFactory.FactoryWhiteboard)optionalArgs;
      board.bfn.addNodeInfoForIO(board.nodeInfo);
      return (null);
    }
  }  
  
  public static class MyLinkGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FabricFactory.FactoryWhiteboard board = (FabricFactory.FactoryWhiteboard)optionalArgs;
      board.bfn.addLinkInfoForIO(board.linkInfo);
      return (null);
    }
  } 
  
  public static class MyGroupGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FabricFactory.FactoryWhiteboard board = (FabricFactory.FactoryWhiteboard)optionalArgs;
      board.bfn.addLinkGroupForIO(board.groupTag);
      return (null);
    }
  }
  
  public static class DrainZoneGlue implements GlueStick {
    
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, Object optionalArgs) throws IOException {
      FabricFactory.FactoryWhiteboard board = (FabricFactory.FactoryWhiteboard) optionalArgs;
      board.nodeInfo.addDrainZone(board.drainZone);
      return null;
    }
  }
  
  /***************************************************************************
   **
   ** For XML I/O
   */
  
  public static class DrainZoneWorker extends AbstractFactoryClient {
    
    private boolean isShadow;
    
    public DrainZoneWorker(FabricFactory.FactoryWhiteboard board, boolean isShadow) {
      super(board);
      this.isShadow = isShadow;
      
      if (this.isShadow) {
        myKeys_.add("drainZoneShadow");
      } else {
        myKeys_.add("drainZone");
      }
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      FabricFactory.FactoryWhiteboard board = (FabricFactory.FactoryWhiteboard) this.sharedWhiteboard_;
      board.drainZone = buildFromXML(elemName, attrs);
      retval = board.drainZone;
      return (retval);
    }
    
    private DrainZone buildFromXML(String elemName, Attributes attrs) throws IOException {
      
      String minCol, maxCol;
      
      if (this.isShadow) {
        minCol = AttributeExtractor.extractAttribute(elemName, attrs, "drainZoneShadow", "minCol", true);
        maxCol = AttributeExtractor.extractAttribute(elemName, attrs, "drainZoneShadow", "maxCol", true);
      } else {
        minCol = AttributeExtractor.extractAttribute(elemName, attrs, "drainZone", "minCol", true);
        maxCol = AttributeExtractor.extractAttribute(elemName, attrs, "drainZone", "maxCol", true);
      }
  
      int min = Integer.valueOf(minCol).intValue();
      int max = Integer.valueOf(maxCol).intValue();
      MinMax dzmm = new MinMax(min, max);
      
      return (new DrainZone(dzmm, isShadow));
    }
    
  }
  
  public static class LinkGroupWorker extends AbstractFactoryClient {
    
    public LinkGroupWorker(FabricFactory.FactoryWhiteboard board) {
      super(board);
      FabricFactory.FactoryWhiteboard whiteboard = (FabricFactory.FactoryWhiteboard)sharedWhiteboard_;   
      myKeys_.add("linkGroups");
      installWorker(new LinkGroupTagWorker(whiteboard), new MyGroupGlue());
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      LayoutMode retval = null;
      if (elemName.equals("linkGroups")) {
        String target = AttributeExtractor.extractAttribute(elemName, attrs, "linkGroups", "mode", false);
        if (target == null) {
        	target = LayoutMode.PER_NODE_MODE.getText();   	
        }
        FabricFactory.FactoryWhiteboard board = (FabricFactory.FactoryWhiteboard)this.sharedWhiteboard_;
        retval = LayoutMode.fromString(target);
        board.bfn.setLayoutMode(retval);

      }
      return (retval);     
    }  
  }

  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class LinkInfoWorker extends AbstractFactoryClient {
    
    public LinkInfoWorker(FabricFactory.FactoryWhiteboard board) {
      super(board);
      myKeys_.add("link");
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      FabricFactory.FactoryWhiteboard board = (FabricFactory.FactoryWhiteboard)this.sharedWhiteboard_;    
      board.linkInfo = buildFromXML(elemName, attrs, board);
      retval = board.linkInfo;
      return (retval);     
    }  
    
    private LinkInfo buildFromXML(String elemName, Attributes attrs, FabricFactory.FactoryWhiteboard board) throws IOException { 
      String src = AttributeExtractor.extractAttribute(elemName, attrs, "link", "src", false);
      src = CharacterEntityMapper.unmapEntities(src, false);
      String trg = AttributeExtractor.extractAttribute(elemName, attrs, "link", "trg", false);
      trg = CharacterEntityMapper.unmapEntities(trg, false);
        
      String srcID = AttributeExtractor.extractAttribute(elemName, attrs, "link", "srcID", false);
      
      String trgID = AttributeExtractor.extractAttribute(elemName, attrs, "link", "trgID", false);
      
      NID.WithName srcNID;
      if (src != null) {
      	// Previously used DataUtil.normKey(src) as argument, but as issue #41 shows,
      	// that is wrong. Need the original string:
      	srcNID = board.legacyMap.get(src);	
      } else if (srcID != null) {
      	srcNID = board.wnMap.get(new NID(srcID));
      } else {
      	throw new IOException();
      }
      
      NID.WithName trgNID;
      if (trg != null) {
      	// Previously used DataUtil.normKey(trg) as argument, but as issue #41 shows,
      	// that is wrong. Need the original string:
      	trgNID = board.legacyMap.get(trg);
      } else if (trgID != null) {
      	trgNID = board.wnMap.get(new NID(trgID));
      } else {
      	throw new IOException();
      }
      
      String rel = AttributeExtractor.extractAttribute(elemName, attrs, "link", "rel", true);
      rel = CharacterEntityMapper.unmapEntities(rel, false);
      String directed = AttributeExtractor.extractAttribute(elemName, attrs, "link", "directed", true);
      Boolean dirObj = Boolean.valueOf(directed);
      String shadow = AttributeExtractor.extractAttribute(elemName, attrs, "link", "shadow", true);
      Boolean shadObj = Boolean.valueOf(shadow);
      FabricLink flink = new FabricLink(srcNID, trgNID, rel, shadObj.booleanValue(), dirObj);
      String col = AttributeExtractor.extractAttribute(elemName, attrs, "link", "column", false);
      String shadowCol = AttributeExtractor.extractAttribute(elemName, attrs, "link", "shadowCol", true);
      String srcRow = AttributeExtractor.extractAttribute(elemName, attrs, "link", "srcRow", true);
      String trgRow = AttributeExtractor.extractAttribute(elemName, attrs, "link", "trgRow", true);
      String color = AttributeExtractor.extractAttribute(elemName, attrs, "link", "color", true);
      
      if (!shadObj.booleanValue() && (col == null)) {
        throw new IOException();
      }
         
      LinkInfo retval;
      try {
        int useColumn = (col == null) ? Integer.MIN_VALUE : Integer.valueOf(col).intValue();
        int shadowColumn = Integer.valueOf(shadowCol).intValue();
        int srcRowVal = Integer.valueOf(srcRow).intValue();
        int trgRowVal = Integer.valueOf(trgRow).intValue();
        retval = new LinkInfo(flink, srcRowVal, trgRowVal, useColumn, shadowColumn, color);
      } catch (NumberFormatException nfex) {
        throw new IOException();
      }
      return (retval);
    }
  }
  
   /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class LinkGroupTagWorker extends AbstractFactoryClient {
    
    public LinkGroupTagWorker(FabricFactory.FactoryWhiteboard board) {
      super(board);
      myKeys_.add("linkGroup");
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      FabricFactory.FactoryWhiteboard board = (FabricFactory.FactoryWhiteboard)this.sharedWhiteboard_;
      board.groupTag = buildFromXML(elemName, attrs);
      retval = board.groupTag;
      return (retval);     
    }  
    
    private String buildFromXML(String elemName, Attributes attrs) throws IOException { 
      String tag = AttributeExtractor.extractAttribute(elemName, attrs, "linkGroup", "tag", true);
      return (tag);
    }
  }
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class NodeInfoWorker extends AbstractFactoryClient {
    
  	
    public NodeInfoWorker(FabricFactory.FactoryWhiteboard board) {
      super(board);
      myKeys_.add("node");
      installWorker(new DrainZoneWorker(board, false), new DrainZoneGlue());
      installWorker(new DrainZoneWorker(board, true), new DrainZoneGlue());
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      FabricFactory.FactoryWhiteboard board = (FabricFactory.FactoryWhiteboard)this.sharedWhiteboard_;
      if (myKeys_.contains(elemName)) {
        board.nodeInfo = buildFromXML(elemName, attrs, board);
        retval = board.nodeInfo;
      }
      return (retval);     
    }  
    
    private NodeInfo buildFromXML(String elemName, Attributes attrs, FabricFactory.FactoryWhiteboard board) throws IOException {
      String name = AttributeExtractor.extractAttribute(elemName, attrs, "node", "name", true);
      name = CharacterEntityMapper.unmapEntities(name, false);
      String nidStr = AttributeExtractor.extractAttribute(elemName, attrs, "node", "nid", false);

      NID nid;
      NID.WithName nwn;
      if (nidStr != null) {
      	boolean ok = board.ulb.addExistingLabel(nidStr);
      	if (!ok) {
      		throw new IOException();
      	}
      	nid = new NID(nidStr);
      	nwn = new NID.WithName(nid, name);
      } else {
      	nid = board.ulb.getNextOID();
      	nwn = new NID.WithName(nid, name);

      	// Addresses Issue 41. Used DataUtil.normKey(name), but if a node made it
      	// as a separate entity in the past, we should keep them unique. Note that with
      	// NIDs now, we can support "identically" named nodes anyway:
      	board.legacyMap.put(name, nwn);
      }
      board.wnMap.put(nid, nwn);
 
      String row = AttributeExtractor.extractAttribute(elemName, attrs, "node", "row", true);
      String minCol = AttributeExtractor.extractAttribute(elemName, attrs, "node", "minCol", true);
      String maxCol = AttributeExtractor.extractAttribute(elemName, attrs, "node", "maxCol", true);
      String minColSha = AttributeExtractor.extractAttribute(elemName, attrs, "node", "minColSha", true);
      String maxColSha = AttributeExtractor.extractAttribute(elemName, attrs, "node", "maxColSha", true);
      String minDrain = AttributeExtractor.extractAttribute(elemName, attrs, "node", "drainMin", false);
      String maxDrain = AttributeExtractor.extractAttribute(elemName, attrs, "node", "drainMax", false);
      String minDrainSha = AttributeExtractor.extractAttribute(elemName, attrs, "node", "drainMinSha", false);
      String maxDrainSha = AttributeExtractor.extractAttribute(elemName, attrs, "node", "drainMaxSha", false);
      String color = AttributeExtractor.extractAttribute(elemName, attrs, "node", "color", true);
      String cluster = AttributeExtractor.extractAttribute(elemName, attrs, "node", "cluster", false);
      cluster = CharacterEntityMapper.unmapEntities(cluster, false);
      
      NodeInfo retval;
      try {
        int nodeRow = Integer.valueOf(row).intValue();
        retval = new NodeInfo(nid, name, nodeRow, color);
        if (cluster != null) {
        	UiUtil.fixMePrintout("Make cluster assign a list");
        	retval.setCluster(cluster);
        }
        
        int min = Integer.valueOf(minCol).intValue();
        int max = Integer.valueOf(maxCol).intValue();
        retval.updateMinMaxCol(min, false);
        retval.updateMinMaxCol(max, false);
        
        int minSha = Integer.valueOf(minColSha).intValue();
        int maxSha = Integer.valueOf(maxColSha).intValue();
        retval.updateMinMaxCol(minSha, true);
        retval.updateMinMaxCol(maxSha, true);
  
        if (minDrain != null) {
          int minDrVal = Integer.valueOf(minDrain).intValue();
          int maxDrVal = Integer.valueOf(maxDrain).intValue();
          DrainZone dz = new DrainZone(new MinMax(minDrVal, maxDrVal), false);
          retval.addDrainZone(dz);
        }
        if (minDrainSha != null) {
          int minDrValSha = Integer.valueOf(minDrainSha).intValue();
          int maxDrValSha = Integer.valueOf(maxDrainSha).intValue();
          DrainZone dz = new DrainZone(new MinMax(minDrValSha, maxDrValSha), true);
          retval.addDrainZone(dz);
        }
      } catch (NumberFormatException nfex) {
        throw new IOException();
      }
      return (retval);
    }
  }
}
