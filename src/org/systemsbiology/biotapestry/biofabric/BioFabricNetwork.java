/*
**    Copyright (C) 2003-2012 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.biofabric;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.analysis.Link;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.MinMax;


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
  
  public static final int DEFAULT_LAYOUT           = 0;
  public static final int REORDER_LAYOUT           = 1;
  public static final int CLUSTERED_LAYOUT         = 2;
  public static final int SHADOW_LINK_CHANGE       = 3;
  public static final int GROUP_PER_NODE_CHANGE    = 4;
  public static final int BUILD_FOR_SUBMODEL       = 5;
  public static final int BUILD_FROM_XML           = 6;
  public static final int BUILD_FROM_SIF           = 7;
  public static final int BUILD_FROM_GAGGLE        = 8;
  public static final int NODE_ATTRIB_LAYOUT       = 9;
  public static final int LINK_ATTRIB_LAYOUT       = 10;
  public static final int GROUP_PER_NETWORK_CHANGE = 11;

  public enum LayoutMode {
    UNINITIALIZED_MODE,
    PER_NODE_MODE,
    PER_NETWORK_MODE
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  //
  // For mapping of selections:
  //
  
  private HashMap rowToTarg_;
  private int rowCount_;
  
  //
  // Link and node definitions:
  //
  
  private TreeMap fullLinkDefs_;
  private TreeMap nonShadowedLinkMap_;
  private HashMap nodeDefs_;
  
  //
  // Grouping for links:
  //
  
  private List linkGrouping_;
  
  //
  // Columns assignments, shadow and non-shadow states:
  //

  private ColumnAssign normalCols_;
  private ColumnAssign shadowCols_;
  
  private FabricColorGenerator colGen_;

  //
  // Current Link Layout Mode, default is UNINITIALIZED_MODE
  //

  private LayoutMode layoutMode_ = LayoutMode.UNINITIALIZED_MODE;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public BioFabricNetwork(BuildData bd) {
    int mode = bd.getMode();

    switch (mode) {
      case DEFAULT_LAYOUT:
      case REORDER_LAYOUT:
      case CLUSTERED_LAYOUT:
      case GROUP_PER_NODE_CHANGE:
      case GROUP_PER_NETWORK_CHANGE:
      case NODE_ATTRIB_LAYOUT:
      case LINK_ATTRIB_LAYOUT:
        RelayoutBuildData rbd = (RelayoutBuildData)bd;
        normalCols_ = new ColumnAssign();
        shadowCols_ = new ColumnAssign();
        rowToTarg_ = new HashMap();
        fullLinkDefs_ = new TreeMap();
        nonShadowedLinkMap_ = new TreeMap();
        nodeDefs_ = new HashMap();
        linkGrouping_ = new ArrayList();
        colGen_ = rbd.colGen;
        layoutMode_ = rbd.layoutMode;
        relayoutNetwork(rbd);
        break;
      case BUILD_FOR_SUBMODEL:
        SelectBuildData sbd = (SelectBuildData)bd;
        colGen_ = sbd.fullNet.colGen_;
        this.linkGrouping_ = new ArrayList(sbd.fullNet.linkGrouping_);
        fillSubModel(sbd.fullNet, sbd.subNodes, sbd.subLinks);
        break;
      case BUILD_FROM_XML:
      case SHADOW_LINK_CHANGE:
        BioFabricNetwork built = ((PreBuiltBuildData)bd).bfn;
        this.normalCols_ = built.normalCols_;
        this.shadowCols_ = built.shadowCols_;
        this.rowToTarg_ = built.rowToTarg_;
        this.fullLinkDefs_ = built.fullLinkDefs_;
        this.nonShadowedLinkMap_ = built.nonShadowedLinkMap_;
        this.nodeDefs_ = built.nodeDefs_;
        this.colGen_ = built.colGen_;
        this.rowCount_ = built.rowCount_;
        this.linkGrouping_ = built.linkGrouping_;
        break;
      case BUILD_FROM_SIF:
      case BUILD_FROM_GAGGLE:
        OrigBuildData obd = (OrigBuildData)bd;
        normalCols_ = new ColumnAssign();
        shadowCols_ = new ColumnAssign();
        rowToTarg_ = new HashMap();
        fullLinkDefs_ = new TreeMap();
        nonShadowedLinkMap_ = new TreeMap();
        nodeDefs_ = new HashMap();
        linkGrouping_ = new ArrayList();
        colGen_ = obd.colGen;
        processLinks(obd.allLinks, obd.loneNodes, obd.colGen);
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
  ** Install link grouping
  */

  public void installLinkGroups(List linkTagList) {
    linkGrouping_ = new ArrayList(linkTagList);
    return;
  }
  
  /***************************************************************************
  **
  ** Get link grouping
  */

  public List getLinkGroups() {
    return (linkGrouping_);
  }
  
  /***************************************************************************
  **
  ** Given an attribute list giving node order, confirm it is valid:
  */

  public boolean checkNewNodeOrder(Map nodeRows) {
    
    //
    // All existing targets must have a row, and all existing
    // rows need a target assigned!
    //
    
    HashSet asUpper = new HashSet();
    Iterator rttvit = rowToTarg_.values().iterator();
    while (rttvit.hasNext()) {
      asUpper.add(((String)rttvit.next()).toUpperCase());
    }
    if (!asUpper.equals(new HashSet(nodeRows.keySet()))) {
      return (false);
    }
    
    TreeSet asInts = new TreeSet();
    Iterator nrvit = nodeRows.values().iterator();
    while (nrvit.hasNext()) {
      String asStr = (String)nrvit.next();
      try {
        asInts.add(Integer.valueOf(asStr));
      } catch (NumberFormatException nfex) {
        return (false);
      }
    }
    if (!asInts.equals(new TreeSet(rowToTarg_.keySet()))) {
      return (false);
    }
    
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

  public SortedMap checkNewLinkOrder(Map linkRows) {
    
    //
    // Recover the mapping that tells us what link relationships are
    // directed:
    //
    
    HashMap relDir = new HashMap();
    Set allLinks = getAllLinks(true);
    Iterator alit = allLinks.iterator();
    while (alit.hasNext()) {
      FabricLink link = (FabricLink)alit.next();
      FabricLink.AugRelation rel = link.getAugRelation();
      boolean isDir = link.isDirected();
      Boolean myVal = new Boolean(isDir);
      Boolean currVal = (Boolean)relDir.get(rel);
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
    
    TreeMap dirMap = new TreeMap();
    Iterator lrit = linkRows.keySet().iterator();
    while (lrit.hasNext()) {
      FabricLink link = (FabricLink)lrit.next();
      String colNumStr = (String)linkRows.get(link);
      FabricLink dirCopy = (FabricLink)link.clone();
      Boolean isDirected = (Boolean)relDir.get(dirCopy.getAugRelation());
      dirCopy.installDirection(isDirected);
      try {
        dirMap.put(Integer.valueOf(colNumStr), dirCopy);
      } catch (NumberFormatException nfex) {
        return (null);
      }
    }
    
    // Ordered set of all our existing links:
    TreeSet alks = new TreeSet(allLinks);
    // Ordered set of guys we have been handed:
    TreeSet dmvs = new TreeSet(dirMap.values());
    
    // Has to be the case that the link definitions are 1:1 and
    // onto, or we have an error:
    if (!alks.equals(dmvs)) {
      return (null);
    }

    //
    // Has to be the case that all columns are also 1:1 and onto:
    //
    
    TreeSet ldks = new TreeSet(fullLinkDefs_.keySet());
    TreeSet dmks = new TreeSet(dirMap.keySet());
    
    if (!ldks.equals(dmks)) {
      return (null);
    }
    
    return (dirMap);
  }
  
  /***************************************************************************
  **
  ** Get all links
  */
  
  public Set getAllLinks(boolean withShadows) {
    HashSet allLinks = new HashSet();
    Iterator ldit = fullLinkDefs_.keySet().iterator();
    while (ldit.hasNext()) {
      Integer col = (Integer)ldit.next();
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
  
  public Iterator getOrderedLinkInfo(boolean withShadows) {
    return ((withShadows) ? fullLinkDefs_.keySet().iterator() : nonShadowedLinkMap_.keySet().iterator());
  }
  
  /***************************************************************************
  **
  ** Process a link set
  */

  private void processLinks(Set allLinks, Set loneNodes, FabricColorGenerator colGen) {
    //
    // Note the allLinks Set has pruned out duplicates and synonymous non-directional links
    //
    
    List targets = defaultNodeOrder(allLinks, loneNodes);
    
    //
    // Now have the ordered list of targets we are going to display.
    // Build target->row maps and the inverse:
    //
    
    fillNodesFromOrder(targets, colGen);

    //
    // Now each link is given a vertical extent.
    //
    
    TreeMap rankedLinks = new TreeMap();
    Map relsForPair = generateLinkExtents(allLinks, rankedLinks);
    
    //
    // This now assigns the link to its column.  Note that we order them
    // so that the shortest vertical link is drawn first!
    //
    
    defaultLinkToColumn(rankedLinks, relsForPair, colGen);

    //
    // Determine the start & end of each target row needed to handle the incoming
    // and outgoing links:
    //

    trimTargetRows();

    //
    // For the lone nodes, they are assigned into the last column:
    //
    
    loneNodesToLastColumn(loneNodes);
    return;
  }
  
  /***************************************************************************
  **
  ** Relayout the network!
  */
  
  private void relayoutNetwork(RelayoutBuildData rbd) {
    int mode = rbd.getMode();
    FabricColorGenerator colGen = rbd.colGen;
    SortedMap linkOrder = rbd.linkOrder;
    List linkGroups = (mode == GROUP_PER_NODE_CHANGE || mode == GROUP_PER_NETWORK_CHANGE)
            ? rbd.newLinkGroups : rbd.existingLinkGroups;
    installLinkGroups(linkGroups);
    boolean installNonStandardNodeOrder = (mode == NODE_ATTRIB_LAYOUT) ||
                                          (mode == CLUSTERED_LAYOUT) ||
                                          (mode == REORDER_LAYOUT);
    boolean installDefaultNodeOrder = (mode == DEFAULT_LAYOUT);

    List targets;
    if (installNonStandardNodeOrder) {
      targets = specifiedOrder(rbd.allNodes, rbd.nodeOrder);
    } else if (installDefaultNodeOrder) {
      targets = defaultNodeOrder(rbd.allLinks, rbd.loneNodes);
    } else {
      targets = rbd.existingOrder;
    }

    //
    // Now have the ordered list of targets we are going to display.
    // Build target->row maps and the inverse:
    //
    
    fillNodesFromOrder(targets, colGen);

    //
    // Now each link is given a vertical extent.
    //
    
    TreeMap rankedLinks = new TreeMap();
    Map relsForPair = generateLinkExtents(rbd.allLinks, rankedLinks);
    
    //
    // Ordering of links:
    //
    
    if (linkOrder == null) {
      //
      // This now assigns the link to its column.  Note that we order them
      // so that the shortest vertical link is drawn first!
      // 
      defaultLinkToColumn(rankedLinks, relsForPair, colGen);
    } else {
      //
      // This now assigns the link to its column, based on user specification
      // For now, the GROUP_PER_NETWORK_CHANGE is piggy-backing on the LINK_ATTRIB_LAYOUT
      //
      specifiedLinkToColumn(colGen, linkOrder, (mode == LINK_ATTRIB_LAYOUT || mode == GROUP_PER_NETWORK_CHANGE));
    }

    //
    // Determine the start & end of each target row needed to handle the incoming
    // and outgoing links:
    //

    trimTargetRows();

    //
    // For the lone nodes, they are assigned into the last column:
    //

    loneNodesToLastColumn(rbd.loneNodes);
    return;
  }
  
  /***************************************************************************
  **
  ** Get specified node order list from attribute map
  */
  
  private List specifiedOrder(Set allNodes, Map newOrder) {

    TreeMap forRetval = new TreeMap();
    Iterator rttvit = allNodes.iterator();
    while (rttvit.hasNext()) {
      String key = (String)rttvit.next();
      String asUpperKey = key.toUpperCase();
      String valAsStr = (String)newOrder.get(asUpperKey);
      try {
        Integer newRow = Integer.valueOf(valAsStr);
        forRetval.put(newRow, key);
      } catch (NumberFormatException nfex) {
        throw new IllegalStateException();
      }
    }
    return (new ArrayList(forRetval.values()));
  }

  /***************************************************************************
  **
  ** Get existing order
  */
  
  public List existingOrder() {
    ArrayList retval = new ArrayList();
    Iterator rtit = new TreeSet(rowToTarg_.keySet()).iterator();
    while (rtit.hasNext()) {
      Integer row = (Integer)rtit.next();
      String node = (String)rowToTarg_.get(row);
      retval.add(node);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get existing link order
  */
  
  public SortedMap getExistingLinkOrder() {
    TreeMap retval = new TreeMap();
    Iterator ldit = fullLinkDefs_.keySet().iterator();
    while (ldit.hasNext()) {
      Integer col = (Integer)ldit.next();
      LinkInfo li = (LinkInfo)fullLinkDefs_.get(col);
      FabricLink link = li.getLink();
      retval.put(col, link);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get existing order
  */
  
  public Iterator getRows() {
    return (rowToTarg_.keySet().iterator());
  }

  /***************************************************************************
  **
  ** Process a link set
  */

  private void specifiedLinkToColumn(FabricColorGenerator colGen, SortedMap linkOrder, boolean userSpec) {

    normalCols_.columnCount = 0;
    shadowCols_.columnCount = 0;
    int numColors = colGen.getNumColors();
    Iterator frkit = linkOrder.keySet().iterator();
    while (frkit.hasNext()) {
      Integer nextCol = (Integer)frkit.next();
      FabricLink nextLink = (FabricLink)linkOrder.get(nextCol);
      Integer useForSDef = new Integer(shadowCols_.columnCount);
      Integer[] colCounts = addLinkDef(nextLink, numColors, normalCols_.columnCount, shadowCols_.columnCount, colGen);
      shadowCols_.columnCount = colCounts[0].intValue();
      if (colCounts[1] != null) {
        normalCols_.columnCount = colCounts[1].intValue();
      }
      
      LinkInfo linf = getLinkDefinition(useForSDef, true);
      String topNodeName = (String)rowToTarg_.get(new Integer(linf.topRow()));
      String botNodeName = (String)rowToTarg_.get(new Integer(linf.bottomRow()));
      
      NodeInfo nit = (NodeInfo)nodeDefs_.get(topNodeName);
      NodeInfo nib = (NodeInfo)nodeDefs_.get(botNodeName);
      
      //
      // When displaying shadows, drain zone of top node is only mod 
      // for non-shadow link values, and drain zone of bottom node is 
      // only mod with shadow link 
      //
      // For non-shadow display, top node drain is mod with non-shadow values
      //
      // Stated another way, non-shadows always affect top node drains.  Shadow
      // links only affect bottom node shadow drains:
      //

      if (!userSpec) {
        if (!linf.isShadow()) {
          MinMax dzst = nit.getDrainZone(true);
          if (dzst == null) {
            dzst = (new MinMax()).init();
            nit.setDrainZone(dzst, true);
          }
          dzst.update(linf.getUseColumn(true));

          MinMax dznt = nit.getDrainZone(false);
          if (dznt == null) {
            dznt = (new MinMax()).init();
            nit.setDrainZone(dznt, false);
          }
          dznt.update(linf.getUseColumn(false));
        } else {
          MinMax dzsb = nib.getDrainZone(true);
          if (dzsb == null) {
            dzsb = (new MinMax()).init();
            nib.setDrainZone(dzsb, true);
          }
          dzsb.update(linf.getUseColumn(true));
        }
      }
    }
    
    if (userSpec) {
      setDrainZonesByContig(true);
      setDrainZonesByContig(false);
    }

    return;
  }
  
  /***************************************************************************
  **
  ** Helper
  */

  private void updateContigs(String nodeName, HashMap runsPerNode, Integer lastCol, Integer col) {
    int colVal = col.intValue();
    TreeMap runs = (TreeMap)runsPerNode.get(nodeName);
    if (runs == null) {
      runs = new TreeMap();
      runsPerNode.put(nodeName, runs);
    }
    MinMax currRun = (MinMax)runs.get(lastCol);
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

  private void runsToDrain(HashMap runsPerNode, boolean forShadow) {
    Iterator rpnit = runsPerNode.keySet().iterator();
    while (rpnit.hasNext()) {
      String nodeName = (String)rpnit.next();
      TreeMap runs = (TreeMap)runsPerNode.get(nodeName);
      MinMax maxRun = null;
      int maxSize = Integer.MIN_VALUE;
      Iterator rit = runs.values().iterator();
      while (rit.hasNext()) {
        MinMax aRun = (MinMax)rit.next();
        int runLen = aRun.max - aRun.min + 1;
        if (runLen > maxSize) {
          maxSize = runLen;
          maxRun = aRun;
        } else if ((runLen == 1) && (maxSize == 1)) {
          maxRun = aRun;  // move this to the end if no contig run
        }
      }
      if (maxRun != null) {
        NodeInfo nit = (NodeInfo)nodeDefs_.get(nodeName);
        nit.setDrainZone((MinMax)maxRun.clone(), forShadow);
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
    
    HashMap runsPerNode = new HashMap();

    Iterator olit = getOrderedLinkInfo(withShadows);
    Integer lastCol = new Integer(-1);
    while (olit.hasNext()) {
      Integer col = (Integer)olit.next();
      LinkInfo linf = getLinkDefinition(col, withShadows);
      String topNodeName = (String)rowToTarg_.get(new Integer(linf.topRow()));
      updateContigs(topNodeName, runsPerNode, lastCol, col);
      if (withShadows && linf.isShadow()) {
        String botNodeName = (String)rowToTarg_.get(new Integer(linf.bottomRow()));
        updateContigs(botNodeName, runsPerNode, lastCol, col);
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
    
    Iterator r2tit = (new TreeSet(rowToTarg_.keySet())).iterator();
    ind.indent();
    out.println("<nodes>");
    ind.up();
    while (r2tit.hasNext()) {
      Integer row = (Integer)r2tit.next();
      String targ = (String)rowToTarg_.get(row);
      NodeInfo ni = getNodeDefinition(targ);
      ind.indent();
      out.print("<node name=\"");
      out.print(CharacterEntityMapper.mapEntities(targ, false));
      out.print("\" row=\"");
      out.print(row);
      MinMax nsCols = ni.getColRange(false);
      out.print("\" minCol=\"");
      out.print(nsCols.min);
      out.print("\" maxCol=\"");
      out.print(nsCols.max);
      MinMax sCols = ni.getColRange(true);
      out.print("\" minColSha=\"");
      out.print(sCols.min);
      out.print("\" maxColSha=\"");
      out.print(sCols.max);
      MinMax nDrain = ni.getDrainZone(false);
      if (nDrain != null) {
        out.print("\" drainMin=\"");
        out.print(nDrain.min);
        out.print("\" drainMax=\"");
        out.print(nDrain.max);
      }
      MinMax sDrain = ni.getDrainZone(true);
      if (sDrain != null) {
        out.print("\" drainMinSha=\"");
        out.print(sDrain.min);
        out.print("\" drainMaxSha=\"");
        out.print(sDrain.max);
      }
      out.print("\" color=\"");
      out.print(ni.colorKey);
      out.println("\" />");
    }
    ind.down().indent();
    out.println("</nodes>");

    if (!linkGrouping_.isEmpty()) {
      Iterator lgit = linkGrouping_.iterator();
      ind.indent();
      out.println("<linkGroups>");
      out.println("<layout = \"" + layoutMode_ + "\" />");   // is this correct?
      ind.up();
      while (lgit.hasNext()) {
        String grpTag = (String)lgit.next();
        ind.indent();
        out.print("<linkGroup tag=\"");
        out.print(grpTag);
        out.println("\" />");
      }
      ind.down().indent();
      out.println("</linkGroups>");
    }

    HashMap inverse = new HashMap();
    Iterator nsit = nonShadowedLinkMap_.keySet().iterator();
    while (nsit.hasNext()) {
      Integer key = (Integer)nsit.next();
      inverse.put(nonShadowedLinkMap_.get(key), key);
    }
    
    
    Iterator ldit = fullLinkDefs_.keySet().iterator();
    ind.indent();
    out.println("<links>");
    ind.up();
    while (ldit.hasNext()) {
      Integer col = (Integer)ldit.next();
      LinkInfo li = getLinkDefinition(col, true);
      FabricLink link = li.getLink();
      ind.indent();
      out.print("<link src=\"");
      out.print(CharacterEntityMapper.mapEntities(link.getSrc(), false));
      out.print("\" trg=\"");
      out.print(CharacterEntityMapper.mapEntities(link.getTrg(), false));
      out.print("\" rel=\"");
      FabricLink.AugRelation augr = link.getAugRelation();
      out.print(CharacterEntityMapper.mapEntities(augr.relation, false));
      out.print("\" directed=\"");
      out.print(link.isDirected());
      out.print("\" shadow=\"");
      out.print(augr.isShadow);
      if (!augr.isShadow) {
        Integer nsCol = (Integer)inverse.get(col);
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
    Iterator r2tit = new TreeSet(rowToTarg_.keySet()).iterator();
    while (r2tit.hasNext()) {
      Integer row = (Integer)r2tit.next();
      String targ = (String)rowToTarg_.get(row);
      out.print(targ);
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
    Iterator ldit = fullLinkDefs_.keySet().iterator();
    while (ldit.hasNext()) {
      Integer col = (Integer)ldit.next();
      LinkInfo li = getLinkDefinition(col, true);
      FabricLink link = li.getLink();
      out.print(link.toEOAString());
      out.print(" = ");
      out.println(col);
    }
    return;
  }

  /***************************************************************************
  **
  ** Get Node Definition
  */

  public NodeInfo getNodeDefinition(String targ) {
    NodeInfo node = (NodeInfo)nodeDefs_.get(targ);
    return (node);
  }
  
  /***************************************************************************
  **
  ** Link definition for column.  If no link in column (may happen for selected networks),
  ** returns null
  */

  public LinkInfo getLinkDefinition(Integer colObj, boolean forShadow) {
    if (forShadow) {
      return ((LinkInfo)fullLinkDefs_.get(colObj));
    } else {
      Integer mapped = (Integer)nonShadowedLinkMap_.get(colObj);
      if (mapped != null) {
        return ((LinkInfo)fullLinkDefs_.get(mapped));
      } else {
        return (null);
      }
    }
  }
  
  /***************************************************************************
  **
  ** Get Target For Column
  */

  public String getTargetForColumn(Integer colVal, boolean forShadow) {
    ColumnAssign useCA = (forShadow) ? shadowCols_ : normalCols_;
    String target = (String)useCA.columnToTarget.get(colVal);
    return (target);
  }
  
  /***************************************************************************
  **
  ** Get Drain zone For Column
  */

  public String getDrainForColumn(Integer colVal, boolean forShadow) {
    int col = colVal.intValue();
    ColumnAssign useCA = (forShadow) ? shadowCols_ : normalCols_;
    String target = (String)useCA.columnToTarget.get(colVal);
    String source = (String)useCA.columnToSource.get(colVal);
    if (target != null) {
      NodeInfo nit = (NodeInfo)nodeDefs_.get(target);
      MinMax tdz = nit.getDrainZone(forShadow);
      if (tdz != null) {
        if ((col >= tdz.min) && (col <= tdz.max)) {
          return (target);
        }
      }
    }
    if (source != null) {
      NodeInfo nis = (NodeInfo)nodeDefs_.get(source);
      MinMax sdz = nis.getDrainZone(forShadow);
      if (sdz != null) {
        if ((col >= sdz.min) && (col <= sdz.max)) {
          return (source);
        }
      }
    }
    return (null);
  }

  /***************************************************************************
  **
  ** Get Source For Column
  */

  public String getSourceForColumn(Integer colVal, boolean forShadow) {
    ColumnAssign useCA = (forShadow) ? shadowCols_ : normalCols_;
    String source = (String)useCA.columnToSource.get(colVal);
    return (source);
  }
  
  /***************************************************************************
  **
  ** Get node for row
  */

  public String getNodeForRow(Integer rowObj) {
    String node = (String)rowToTarg_.get(rowObj);
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

  public List getNodeDefList() {
    return (new ArrayList(nodeDefs_.values()));
  }
  
  /***************************************************************************
  **
  ** Get all node names
  */

  public Set getNodeSet() {
    return (new HashSet(nodeDefs_.keySet()));
  }
  
  /***************************************************************************
  **
  ** Get link defs
  */

  public List getLinkDefList(boolean forShadow) {
    if (forShadow) {
      return (new ArrayList(fullLinkDefs_.values()));
    } else {
      ArrayList retval = new ArrayList();
      Iterator nsit = nonShadowedLinkMap_.keySet().iterator();
      while (nsit.hasNext()) {
        Integer linkID = (Integer)nsit.next();
        Integer mappedID = (Integer)nonShadowedLinkMap_.get(linkID);
        retval.add(fullLinkDefs_.get(mappedID));
      }
      return (retval);
    }
  }

  /***************************************************************************
   **
   ** Set Layout Mode
   */

  public void setLayoutMode(LayoutMode mode) {
    layoutMode_ = mode;
  }

  /***************************************************************************
   **
   ** Get Layout Mode
   */

  public LayoutMode getLayoutMode() {
    return layoutMode_;
  }
  
  /***************************************************************************
  **
  ** Get node matches
  */

  public Set nodeMatches(boolean fullMatch, String searchString) {
    HashSet retval = new HashSet();
    Iterator nkit = nodeDefs_.keySet().iterator();
    while (nkit.hasNext()) {
      String nodeName = (String)nkit.next();
      if (matches(searchString, nodeName, fullMatch)) {
        retval.add(nodeName);
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get first neighbors of node, along with info blocks
  */

  public void getFirstNeighbors(String nodeName, Set nodeSet, List nodes, List links) {
    Iterator ldit = fullLinkDefs_.values().iterator();
    while (ldit.hasNext()) {
      LinkInfo linf = (LinkInfo)ldit.next();
      if (linf.getSource().equals(nodeName)) {
        nodeSet.add(linf.getTarget());
        links.add(linf);
      } else if (linf.getTarget().equals(nodeName)) {
        nodeSet.add(linf.getSource());
        links.add(linf);
      }
    }
    Iterator nsit = nodeSet.iterator();
    while (nsit.hasNext()) {
      String name = (String)nsit.next();
      nodes.add(nodeDefs_.get(name));
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Get first neighbors of node
  */

  public Set getFirstNeighbors(String nodeName) {
    HashSet nodeSet = new HashSet();
    Iterator ldit = fullLinkDefs_.values().iterator();
    while (ldit.hasNext()) {
      LinkInfo linf = (LinkInfo)ldit.next();
      if (linf.getSource().equals(nodeName)) {
        nodeSet.add(linf.getTarget());
      } else if (linf.getTarget().equals(nodeName)) {
        nodeSet.add(linf.getSource());
      }
    }
    return (nodeSet);
  }

  /***************************************************************************
  **
  ** Add first neighbors of node set
  */

  public void addFirstNeighbors(Set nodeSet, Set columnSet, Set linkSet, boolean forShadow) {
    HashSet newNodes = new HashSet();
    Iterator ldit = fullLinkDefs_.values().iterator();
    while (ldit.hasNext()) {
      LinkInfo linf = (LinkInfo)ldit.next();
      if (!forShadow && linf.isShadow()) {
        continue;
      }
      if (nodeSet.contains(linf.getSource())) {
        newNodes.add(linf.getTarget());
        columnSet.add(new Integer(linf.getUseColumn(forShadow)));
        linkSet.add(linf.getLink());
      }
      if (nodeSet.contains(linf.getTarget())) {
        newNodes.add(linf.getSource());
        columnSet.add(new Integer(linf.getUseColumn(forShadow)));
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

  private Map linkCountPerTarg(Set targets, List linkList) {
    HashMap retval = new HashMap();
    Iterator lcit = linkList.iterator();
    while (lcit.hasNext()) {
      LinkInfo linf = (LinkInfo)lcit.next();
      String src = linf.getSource();
      String trg = linf.getTarget();
      if (targets.contains(src)) {
        Integer count = (Integer)retval.get(src);
        if (count == null) {
          retval.put(src, new Integer(1));
        } else {
          retval.put(src, new Integer(count.intValue() + 1));
        }
      }
      if (!src.equals(trg) && targets.contains(trg)) {
        Integer count = (Integer)retval.get(trg);
        if (count == null) {
          retval.put(trg, new Integer(1));
        } else {
          retval.put(trg, new Integer(count.intValue() + 1));
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

  private List pruneToMinSubModel(BioFabricNetwork bfn, List targetList, List linkList) {

    HashSet targSet = new HashSet();
    Iterator tlit = targetList.iterator();
    while (tlit.hasNext()) {
      NodeInfo ninf = (NodeInfo)tlit.next();
      targSet.add(ninf.nodeName);
    }

    Map subCounts = linkCountPerTarg(targSet, linkList);
    Map fullCounts = linkCountPerTarg(bfn.getNodeSet(), bfn.getLinkDefList(true));
    
    HashSet skipThem = new HashSet();
    HashSet ditchThem = new HashSet();
    Iterator lcit = linkList.iterator();
    while (lcit.hasNext()) {
      LinkInfo linf = (LinkInfo)lcit.next();
      String topNode = (String)bfn.getNodeForRow(new Integer(linf.topRow()));
      String botNode = (String)bfn.getNodeForRow(new Integer(linf.bottomRow()));
      boolean topStays = subCounts.get(topNode).equals(fullCounts.get(topNode));
      boolean botStays = subCounts.get(botNode).equals(fullCounts.get(botNode));
      if ((topStays && botStays) || (!topStays && !botStays)) {  // Nobody gets ditched!
        continue;
      }
      FabricLink link1 = linf.getLink();
      int col1 = linf.getUseColumn(true);
      skipThem.add(new Integer(col1));
      Iterator lc2it = linkList.iterator();
      while (lc2it.hasNext()) {
        LinkInfo linf2 = (LinkInfo)lc2it.next();
        int col2 = linf2.getUseColumn(true);
        if (skipThem.contains(new Integer(col2))) {
          continue;
        }
        if (linf2.getLink().shadowPair(link1)) { // got a shadow pair! 
          int maxLink = Math.max(col1, col2);
          int minLink = Math.min(col1, col2);
          ditchThem.add(new Integer((topStays) ? maxLink : minLink));
          break;
        }
      }
    }
    
    ArrayList retval = new ArrayList();
    Iterator lcit3 = linkList.iterator();
    while (lcit3.hasNext()) {
      LinkInfo linf = (LinkInfo)lcit3.next();
      int col = linf.getUseColumn(true);
      if (!ditchThem.contains(new Integer(col))) {
        retval.add(linf);
      }
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Fill out model with submodel data
  */

  private void fillSubModel(BioFabricNetwork bfn, List targetList, List linkList) {
    
    boolean doPrune = FabricDisplayOptionsManager.getMgr().getDisplayOptions().getMinShadowSubmodelLinks();
    if (doPrune) {
      linkList = pruneToMinSubModel(bfn, targetList, linkList);
    }

    HashMap lastColForNode = new HashMap();
    HashMap lastShadColForNode = new HashMap();
    Iterator lcit = linkList.iterator();
    while (lcit.hasNext()) {
      LinkInfo linf = (LinkInfo)lcit.next();
      if (!linf.isShadow()) {
        Integer lastCol = (Integer)lastColForNode.get(linf.getTarget());
        if ((lastCol == null) || (lastCol.intValue() < linf.getUseColumn(false))) {
          lastColForNode.put(linf.getTarget(), new Integer(linf.getUseColumn(false)));
        }
        lastCol = (Integer)lastColForNode.get(linf.getSource());
        if ((lastCol == null) || (lastCol.intValue() < linf.getUseColumn(false))) {
          lastColForNode.put(linf.getSource(), new Integer(linf.getUseColumn(false)));
        }
      }
      Integer lastColShad = (Integer)lastShadColForNode.get(linf.getTarget());
      if ((lastColShad == null) || (lastColShad.intValue() < linf.getUseColumn(true))) {
        lastShadColForNode.put(linf.getTarget(), new Integer(linf.getUseColumn(true)));
      }
      lastColShad = (Integer)lastShadColForNode.get(linf.getSource());
      if ((lastColShad == null) || (lastColShad.intValue() < linf.getUseColumn(true))) {
        lastShadColForNode.put(linf.getSource(), new Integer(linf.getUseColumn(true)));
      }
    }

    //
    // Need to compress the rows and columns, throwing away all empty slots
    // First record the "full scale" entries:
    //

    TreeSet needRows = new TreeSet();
    TreeSet needColumns = new TreeSet();
    TreeSet needColumnsShad = new TreeSet();

    Iterator tgit = targetList.iterator();
    while (tgit.hasNext()) {
      NodeInfo targetInf = (NodeInfo)tgit.next();
      needRows.add(new Integer(targetInf.nodeRow));
      needColumns.add(new Integer(targetInf.getColRange(false).min));
      needColumnsShad.add(new Integer(targetInf.getColRange(true).min));
    }

    Iterator cit = linkList.iterator();
    while (cit.hasNext()) {
      LinkInfo linf = (LinkInfo)cit.next();
      needRows.add(new Integer(linf.getStartRow()));
      needRows.add(new Integer(linf.getEndRow()));
      if (!linf.isShadow()) {
        needColumns.add(new Integer(linf.getUseColumn(false)));
      }
      needColumnsShad.add(new Integer(linf.getUseColumn(true)));
    }

    //
    // Create full-scale to mini-scale mappings:
    //

    TreeMap rowMap = new TreeMap();
    TreeMap columnMap = new TreeMap();
    TreeMap shadColumnMap = new TreeMap();

    int rowCount = 0;
    Iterator mrit = needRows.iterator();
    while (mrit.hasNext()) {
      Integer fullRow = (Integer)mrit.next();
      rowMap.put(fullRow, new Integer(rowCount++));
    }

    int colCount = 0;
    Iterator mcit = needColumns.iterator();
    while (mcit.hasNext()) {
      Integer fullCol = (Integer)mcit.next();
      columnMap.put(fullCol, new Integer(colCount++));
    }
    
    int shadColCount = 0;
    Iterator ncsit = needColumnsShad.iterator();
    while (ncsit.hasNext()) {
      Integer fullCol = (Integer)ncsit.next();
      shadColumnMap.put(fullCol, new Integer(shadColCount++));
    }

    //
    // Create modified copies of the node and link info with
    // compressed rows and columns:
    //

    ArrayList modTargetList = new ArrayList();
    ArrayList modLinkList = new ArrayList();

    int maxShadLinkCol = Integer.MIN_VALUE;
    int maxLinkCol = Integer.MIN_VALUE;
    cit = linkList.iterator();
    while (cit.hasNext()) {
      BioFabricNetwork.LinkInfo linf = (BioFabricNetwork.LinkInfo)cit.next();
      Integer startRowObj = (Integer)rowMap.get(new Integer(linf.getStartRow()));
      Integer endRowObj = (Integer)rowMap.get(new Integer(linf.getEndRow()));
      Integer miniColObj = (linf.isShadow()) ? new Integer(Integer.MIN_VALUE) : (Integer)columnMap.get(new Integer(linf.getUseColumn(false)));
      Integer miniColShadObj = (Integer)shadColumnMap.get(new Integer(linf.getUseColumn(true)));
      int miniColVal = miniColObj.intValue();
      if (miniColVal > maxLinkCol) {
        maxLinkCol = miniColVal;
      }
      int miniColShadVal = miniColShadObj.intValue();
      if (miniColShadVal > maxShadLinkCol) {
        maxShadLinkCol = miniColShadVal;
      }
      
      BioFabricNetwork.LinkInfo miniLinf = new BioFabricNetwork.LinkInfo(linf.getLink(), startRowObj.intValue(), endRowObj.intValue(),
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
      BioFabricNetwork.NodeInfo infoFull = (BioFabricNetwork.NodeInfo)tgit.next();
      Integer miniRowObj = (Integer)rowMap.get(new Integer(infoFull.nodeRow));
      BioFabricNetwork.NodeInfo infoMini = new BioFabricNetwork.NodeInfo(infoFull.nodeName, miniRowObj.intValue(), infoFull.colorKey);

      Integer minCol = (Integer)columnMap.get(new Integer(infoFull.getColRange(false).min));
      infoMini.updateMinMaxCol(minCol.intValue(), false);
      int miniColVal = minCol.intValue();
      if (miniColVal < minTrgCol) {
        minTrgCol = miniColVal;
      }
      
      Integer minShadCol = (Integer)shadColumnMap.get(new Integer(infoFull.getColRange(true).min));
      infoMini.updateMinMaxCol(minShadCol.intValue(), true);
      int miniShadColVal = minShadCol.intValue();
      if (miniShadColVal < minShadTrgCol) {
        minShadTrgCol = miniShadColVal;
      }

      int maxCol;
      Integer lastCol = (Integer)lastColForNode.get(infoFull.nodeName);
      if (lastCol == null) {
        maxCol = maxLinkCol;
      } else {
        Integer maxColObj = (Integer)columnMap.get(lastCol);
        maxCol = maxColObj.intValue();
      }
      infoMini.updateMinMaxCol(maxCol, false);
      
      int maxShadCol;
      Integer lastShadCol = (Integer)lastShadColForNode.get(infoFull.nodeName);
      if (lastShadCol == null) {
        maxShadCol = maxShadLinkCol;
      } else {
        Integer maxShadColObj = (Integer)shadColumnMap.get(lastShadCol);
        maxShadCol = maxShadColObj.intValue();
      }
      infoMini.updateMinMaxCol(maxShadCol, true);

      modTargetList.add(infoMini);
    }
    if (minTrgCol == Integer.MAX_VALUE) {
      minTrgCol = 0;
    }
    
    rowToTarg_ = new HashMap();
    nodeDefs_ = new HashMap();
    rowCount_ = modTargetList.size();
    for (int i = 0; i < rowCount_; i++) {
      BioFabricNetwork.NodeInfo infoMini = (BioFabricNetwork.NodeInfo)modTargetList.get(i);
      rowToTarg_.put(new Integer(infoMini.nodeRow), infoMini.nodeName);
      nodeDefs_.put(infoMini.nodeName, infoMini);
    }
    
    normalCols_ = new ColumnAssign();
    shadowCols_ = new ColumnAssign();
    fullLinkDefs_ = new TreeMap();
    nonShadowedLinkMap_ = new TreeMap();
    
    int numMll = modLinkList.size();
    for (int i = 0; i < numMll; i++) {
      BioFabricNetwork.LinkInfo infoMini = (BioFabricNetwork.LinkInfo)modLinkList.get(i);
      
      Integer useShadCol = new Integer(infoMini.getUseColumn(true));
      fullLinkDefs_.put(useShadCol, infoMini);
      shadowCols_.columnToSource.put(useShadCol, infoMini.getSource());
      shadowCols_.columnToTarget.put(useShadCol, infoMini.getTarget());

      if (!infoMini.isShadow()) {
        Integer useCol = new Integer(infoMini.getUseColumn(false));
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
    
    Iterator ndkit = nodeDefs_.keySet().iterator();
    while (ndkit.hasNext()) {
      String node = (String)ndkit.next();
      NodeInfo srcNI = (NodeInfo)nodeDefs_.get(node);
      
      //
      // Non-shadow calcs:
      //
      MinMax srcDrain = null;
      MinMax range = srcNI.getColRange(false);
      int startCol = range.max;
      int endCol = range.min;
      for (int i = startCol; i >= endCol; i--) {
        LinkInfo linf = (LinkInfo)getLinkDefinition(new Integer(i), false);
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
        srcNI.setDrainZone(srcDrain, false);
      }
      
      //
      // Shadow calcs:
      //
      
      MinMax shadowSrcDrain = null;
      MinMax shadowRange = srcNI.getColRange(true);
      int startColShad = shadowRange.min;
      int endColShad = shadowRange.max;
      for (int i = startColShad; i <= endColShad; i++) {
        LinkInfo linf = (LinkInfo)getLinkDefinition(new Integer(i), true);
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
        srcNI.setDrainZone(shadowSrcDrain, true);
      }
    }

    return;
  }

  /***************************************************************************
  **
  ** Calculate default node order
  */

  private List defaultNodeOrder(Set allLinks, Set loneNodes) {
    //
    // Note the allLinks Set has pruned out duplicates and synonymous non-directional links
    //
    //
    // Build a target list, top to bottom, that adds the node with the most
    // links first, and adds those link targets ASAP:
    // 
    
    HashMap linkCounts = new HashMap();
    HashMap targsPerSource = new HashMap();
    ArrayList targets = new ArrayList();

    HashSet targsToGo = new HashSet();
    Iterator alit = allLinks.iterator();
    while (alit.hasNext()) {
      FabricLink nextLink = (FabricLink)alit.next();
      String source = nextLink.getSrc();
      String target = nextLink.getTrg();
      HashSet targs = (HashSet)targsPerSource.get(source);
      if (targs == null) {
        targs = new HashSet();
        targsPerSource.put(source, targs);
      }
      targs.add(target);
      targs = (HashSet)targsPerSource.get(target);
      if (targs == null) {
        targs = new HashSet();
        targsPerSource.put(target, targs);
      }
      targs.add(source);
      targsToGo.add(source);
      targsToGo.add(target);
      Integer srcCount = (Integer)linkCounts.get(source);
      linkCounts.put(source, (srcCount == null) ? new Integer(1) : new Integer(srcCount.intValue() + 1));
      Integer trgCount = (Integer)linkCounts.get(target);
      linkCounts.put(target, (trgCount == null) ? new Integer(1) : new Integer(trgCount.intValue() + 1));
    }
    
    //
    // Rank the nodes by link count:
    //
    
    TreeMap countRank = new TreeMap(Collections.reverseOrder());
    Iterator lcit = linkCounts.keySet().iterator();
    while (lcit.hasNext()) {
      String src = (String)lcit.next();
      Integer count = (Integer)linkCounts.get(src);
      TreeSet perCount = (TreeSet)countRank.get(count);
      if (perCount == null) {
        perCount = new TreeSet();
        countRank.put(count, perCount);
      }
      perCount.add(src);
    }
    
    //
    // Get all kids added in.  Now doing this without recursion; seeing blown
    // stacks for huge networks!
    //

    while (!targsToGo.isEmpty()) {
      Iterator crit = countRank.keySet().iterator();
      while (crit.hasNext()) {
        Integer key = (Integer)crit.next();
        TreeSet perCount = (TreeSet)countRank.get(key);
        Iterator pcit = perCount.iterator();
        while (pcit.hasNext()) {
          String node = (String)pcit.next();
          if (targsToGo.contains(node)) {
            ArrayList queue = new ArrayList();
            targsToGo.remove(node);
            targets.add(node);
            addMyKidsNR(targets, targsPerSource, linkCounts, targsToGo, node, queue);
          }
        }
      }
    }
    
    //
    //
    // Tag on lone nodes.  If a node is by itself, but also shows up in the links,
    // we drop it:
    //
    
    HashSet remains = new HashSet(loneNodes);
    remains.removeAll(targets);
    targets.addAll(new TreeSet(remains));
    return (targets);
  }
  

  /***************************************************************************
  **
  ** Process a link set
  */

  private void defaultLinkToColumn(TreeMap rankedLinks, Map relsForPair, FabricColorGenerator colGen) {
    int numColors = colGen.getNumColors();

    //
    // This now assigns the link to its column.  Note that we order them
    // so that the shortest vertical link is drawn first!
    //

    ArrayList rels = new ArrayList(linkGrouping_);
    if (rels.isEmpty()) {
      rels.add("");
    }
    int numRel = rels.size();
    
    normalCols_.columnCount = 0;
    shadowCols_.columnCount = 0;
    // For each top row...
    for (int k = 0; k < rowCount_; k++) {
      Integer topRow = new Integer(k);
      int currMin = normalCols_.columnCount;
      int currShadMin = shadowCols_.columnCount;
      for (int i = 0; i < numRel; i++) {
        String relOnly = (String)rels.get(i);
        if (relOnly.equals("")) {
          relOnly = null;
        }
        shadowLinkToColumn(topRow.intValue(), rankedLinks, relsForPair, colGen, relOnly);
        TreeSet perSrc = (TreeSet)rankedLinks.get(topRow);
        if (perSrc == null) {
          continue;
        }
        Iterator psit = perSrc.iterator();

        // Drain the bottoms rows, in increasing order...
        while (psit.hasNext()) {
          Integer botRow = (Integer)psit.next();
          String topNode = (String)rowToTarg_.get(topRow);
          String botNode = (String)rowToTarg_.get(botRow);
          // Dumping links in order of the relation sort (alphabetical)...
          SortedMap forPair1 = (SortedMap)relsForPair.get(new Link(topNode, botNode));
          if (forPair1 != null) {
            Iterator fp1it = forPair1.values().iterator();
            while (fp1it.hasNext()) {
              FabricLink nextLink = (FabricLink)fp1it.next();
              if (!nextLink.isShadow()) {
                String augR = nextLink.getAugRelation().relation;
                if ((relOnly == null) || (augR.indexOf(relOnly) == (augR.length() - relOnly.length()))) {
                  Integer[] colAssign = addLinkDef(nextLink, numColors, normalCols_.columnCount, shadowCols_.columnCount, colGen);
                  if (colAssign[1] == null) {
                    throw new IllegalStateException();
                  }
                  shadowCols_.columnCount = colAssign[0].intValue();
                  normalCols_.columnCount = colAssign[1].intValue();
                }
              }
            }
          }
          // With directed links from above coming before directed links from below...       
          if (!topNode.equals(botNode)) { // DO NOT DUPLICATE FEEDBACK LINKS!
            SortedMap forPair2 = (SortedMap)relsForPair.get(new Link(botNode, topNode));
            if (forPair2 != null) {
              Iterator fp2it = forPair2.values().iterator();
              while (fp2it.hasNext()) {
                FabricLink nextLink = (FabricLink)fp2it.next();
                if (!nextLink.isShadow()) {
                  String augR = nextLink.getAugRelation().relation;
                  if ((relOnly == null) || (augR.indexOf(relOnly) == (augR.length() - relOnly.length()))) {
                    Integer[] colAssign = addLinkDef(nextLink, numColors, normalCols_.columnCount, shadowCols_.columnCount, colGen);
                    if (colAssign[1] == null) {
                      throw new IllegalStateException();
                    }
                    shadowCols_.columnCount = colAssign[0].intValue();
                    normalCols_.columnCount = colAssign[1].intValue();
                  }
                }
              }
            }
          }
        }
      }
      int currMaxNorm = normalCols_.columnCount - 1;
      int currMaxShad = shadowCols_.columnCount - 1;
      
      //
      // We only have drain zones if the maximum column we are working with actually
      // hosts a link to/from the row we are working with:
      //
      
      NodeInfo srcNI = (NodeInfo)nodeDefs_.get(rowToTarg_.get(topRow));
      int topRowVal = topRow.intValue();
      LinkInfo linf = getLinkDefinition(new Integer(currMaxNorm), false);
      
      if ((linf != null) && (linf.topRow() == topRowVal)) {
        srcNI.setDrainZone(new MinMax(currMin, currMaxNorm), false);
      }
      linf = getLinkDefinition(new Integer(currMaxShad), true);
      if ((linf != null) && ((linf.topRow() == topRowVal) || (linf.bottomRow() == topRowVal))) {
        srcNI.setDrainZone(new MinMax(currShadMin, currMaxShad), true);
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Get shadow links into their columns:
  */

  private void shadowLinkToColumn(int currDrainRow, TreeMap rankedLinks,
                                  Map relsForPair, FabricColorGenerator colGen, String relOnly) {
    int numColors = colGen.getNumColors();
    
    Iterator rlit = rankedLinks.keySet().iterator();
    // For each top row...
    while (rlit.hasNext()) {
      Integer topRow = (Integer)rlit.next();
      TreeSet perSrc = (TreeSet)rankedLinks.get(topRow);
      Iterator psit = perSrc.iterator();
      // Drain ONLY the bottom row that ends on our row...
      while (psit.hasNext()) {
        Integer botRow = (Integer)psit.next();
        if (botRow.intValue() != currDrainRow) {
          continue;
        }
        // We NEVER create shadow feedback links.  They are insanely redundant.
        if (topRow.equals(botRow)) {
          continue;
        }
        
        String topNode = (String)rowToTarg_.get(topRow);
        String botNode = (String)rowToTarg_.get(botRow);
        // Dumping links in order of the relation sort (alphabetical)...
        SortedMap forPair1 = (SortedMap)relsForPair.get(new Link(topNode, botNode));
        if (forPair1 != null) {
          Iterator fp1it = forPair1.values().iterator();
          while (fp1it.hasNext()) {
            // But ONLY if they are shadow links:
            FabricLink nextLink = (FabricLink)fp1it.next();
            if (nextLink.isShadow()) {
              String augR = nextLink.getAugRelation().relation;
              if ((relOnly == null) || (augR.indexOf(relOnly) == (augR.length() - relOnly.length()))) {
                Integer[] colAssign = addLinkDef(nextLink, numColors, Integer.MIN_VALUE, shadowCols_.columnCount, colGen);
                if (colAssign[1] != null) {
                  throw new IllegalStateException();
                }
                shadowCols_.columnCount = colAssign[0].intValue();
              }
            }
          }
        }
        // With directed links from above coming before directed links from below... 
        // This test should now always be true, given we are never doing feedback....
        if (!topNode.equals(botNode)) { // DO NOT DUPLICATE FEEDBACK LINKS!
          SortedMap forPair2 = (SortedMap)relsForPair.get(new Link(botNode, topNode));
          if (forPair2 != null) {
            Iterator fp2it = forPair2.values().iterator();
            while (fp2it.hasNext()) {
              FabricLink nextLink = (FabricLink)fp2it.next();
              if (nextLink.isShadow()) {
                String augR = nextLink.getAugRelation().relation;
                if ((relOnly == null) || (augR.indexOf(relOnly) == (augR.length() - relOnly.length()))) {
                  Integer[] colAssign = addLinkDef(nextLink, numColors, Integer.MIN_VALUE, shadowCols_.columnCount, colGen);
                  if (colAssign[1] != null) {
                    throw new IllegalStateException();
                  }
                  shadowCols_.columnCount = colAssign[0].intValue();
                }
              }
            }
          }
        } else {
          throw new IllegalStateException();  // Now should never get here for shadow links...
        }
      }
    }
    return;
  }

  /***************************************************************************
  **
  ** Fill out node info from order
  */

  private void fillNodesFromOrder(List targets, FabricColorGenerator colGen) {
    //
    // Now have the ordered list of targets we are going to display.
    // Build target->row maps and the inverse:
    //
    
    int numColors = colGen.getNumColors();

    int currRow = 0;
    Iterator trit = targets.iterator();
    while (trit.hasNext()) {
      String target = (String)trit.next();
      Integer rowObj = new Integer(currRow);
      rowToTarg_.put(rowObj, target);
      String colorKey = colGen.getGeneColor(currRow % numColors);
      nodeDefs_.put(target, new NodeInfo(target, currRow++, colorKey));
    }
    rowCount_ = targets.size();
    return;
  }
  
  /***************************************************************************
  **
  ** Add a node def
  */
  
  void addNodeInfoForIO(NodeInfo nif) {
    nodeDefs_.put(nif.nodeName, nif);
    rowCount_ = nodeDefs_.size();
    rowToTarg_.put(new Integer(nif.nodeRow), nif.nodeName);
    return;
  }
  
  /***************************************************************************
  **
  ** Generate vertical extents for links:
  **
  ** Returns Map relsForPair:
  **  Maps each simple source/target "Link" to sorted map
  **  Sorted map maps each link relation to the associated FabricLink, ordered by
  **  how the AugRelation comparator works
  **
  ** Fills in empty SortedMap rankedLinks:
  **  Maps each top link row to an ordered set of link maximums
  */

  private Map generateLinkExtents(Set allLinks, TreeMap rankedLinks) {
    //
    // Now each link is given a vertical extent.
    //
    
    HashMap relsForPair = new HashMap();
  //  HashMap directedMap = new HashMap();
    Iterator alit = allLinks.iterator();
    while (alit.hasNext()) {
      FabricLink nextLink = (FabricLink)alit.next();
      String source = nextLink.getSrc();
      String target = nextLink.getTrg();
   //   boolean directed = nextLink.isDirected();
  //    directedMap.put(new Link(source, target), new Boolean(directed));
      Link key = new Link(source, target);
      TreeMap rels = (TreeMap)relsForPair.get(key);
      if (rels == null) {
        rels = new TreeMap();
        relsForPair.put(key, rels);
      }
      rels.put(nextLink.getAugRelation(), nextLink);
      int[] link = new int[2];
      NodeInfo srcInfo = (NodeInfo)nodeDefs_.get(source);
      if (srcInfo == null) {
        System.err.println("Bad source: " + source + " from link " + nextLink + " key " + key);
        throw new IllegalStateException();
      }
      NodeInfo trgInfo = (NodeInfo)nodeDefs_.get(target);
      if (trgInfo == null) {
        System.err.println("Bad target: " + target + " from link " + nextLink + " key " + key);
        throw new IllegalStateException();
      }
      link[0] = srcInfo.nodeRow;
      link[1] = trgInfo.nodeRow;
      int min;
      int max;
      if (link[0] < link[1]) {
        min = link[0];
        max = link[1];
      } else {
        min = link[1];
        max = link[0];
      }
      Integer minObj = new Integer(min);
      TreeSet perSrc = (TreeSet)rankedLinks.get(minObj);  // min == node row!
      if (perSrc == null) {
        perSrc = new TreeSet();
        rankedLinks.put(minObj, perSrc);
      }
      perSrc.add(new Integer(max));  // Have the ordered set of link maxes
    }
    return (relsForPair);
  }
  
  /***************************************************************************
  **
  ** Determine the start & end of each target row needed to handle the incoming
  ** and outgoing links:
  */

  private void trimTargetRows() {
    Iterator fldit = fullLinkDefs_.keySet().iterator();
    while (fldit.hasNext()) {
      Integer colNum = (Integer)fldit.next();
      LinkInfo li = (LinkInfo)fullLinkDefs_.get(colNum);
      shadowCols_.columnToSource.put(colNum, li.getSource());
      shadowCols_.columnToTarget.put(colNum, li.getTarget());
      NodeInfo srcNI = (NodeInfo)nodeDefs_.get(li.getSource());
      NodeInfo trgNI = (NodeInfo)nodeDefs_.get(li.getTarget());
      srcNI.updateMinMaxCol(colNum.intValue(), true);
      trgNI.updateMinMaxCol(colNum.intValue(), true);
    }
    Iterator nslit = nonShadowedLinkMap_.keySet().iterator();
    while (nslit.hasNext()) {
      Integer colNum = (Integer)nslit.next();
      Integer mappedCol = (Integer)nonShadowedLinkMap_.get(colNum);
      LinkInfo li = (LinkInfo)fullLinkDefs_.get(mappedCol);
      normalCols_.columnToSource.put(colNum, li.getSource());
      normalCols_.columnToTarget.put(colNum, li.getTarget());
      NodeInfo srcNI = (NodeInfo)nodeDefs_.get(li.getSource());
      NodeInfo trgNI = (NodeInfo)nodeDefs_.get(li.getTarget());
      srcNI.updateMinMaxCol(colNum.intValue(), false);
      trgNI.updateMinMaxCol(colNum.intValue(), false);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** For the lone nodes, they are assigned into the last column:
  */

  private void loneNodesToLastColumn(Set loneNodes) {
    Iterator lnit = loneNodes.iterator();
    while (lnit.hasNext()) {
      String lone = (String)lnit.next();
      NodeInfo loneNI = (NodeInfo)nodeDefs_.get(lone);
      loneNI.updateMinMaxCol(normalCols_.columnCount - 1, false);
      loneNI.updateMinMaxCol(shadowCols_.columnCount - 1, true);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Pretty icky hack:
  */

  private Set getLoneNodes() {
    HashSet retval = new HashSet();
    Iterator lnit = nodeDefs_.keySet().iterator();
    boolean checkDone = false;
    while (lnit.hasNext()) {
      String lone = (String)lnit.next();
      NodeInfo loneNI = (NodeInfo)nodeDefs_.get(lone);
      int min = loneNI.getColRange(true).min;
      int max = loneNI.getColRange(true).max;
      
      if ((min == max) && (min == (shadowCols_.columnCount - 1))) {
        if (!checkDone) {
          if (shadowCols_.columnToSource.get(new Integer(min)) != null) {
            return (retval);
          } else {
            checkDone = true;
          }
        }
        retval.add(loneNI.nodeName);
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
    int srcRow = ((NodeInfo)nodeDefs_.get(nextLink.getSrc())).nodeRow;
    int trgRow = ((NodeInfo)nodeDefs_.get(nextLink.getTrg())).nodeRow;
    LinkInfo linf = new LinkInfo(nextLink, srcRow, trgRow, noShadowCol, shadowCol, key);
    Integer shadowKey = new Integer(shadowCol);
    fullLinkDefs_.put(shadowKey, linf);
    retval[0] = new Integer(shadowCol + 1);
    if (!linf.isShadow()) {
      nonShadowedLinkMap_.put(new Integer(noShadowCol), shadowKey);
      retval[1] = new Integer(noShadowCol + 1);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Add a link def
  */
  
  void addLinkInfoForIO(LinkInfo linf) {
    int useColVal = linf.getUseColumn(true);
    Integer useCol = new Integer(useColVal);
    fullLinkDefs_.put(useCol, linf);
    if (useColVal > shadowCols_.columnCount) {
      shadowCols_.columnCount = useColVal;
    }
    shadowCols_.columnToSource.put(useCol, linf.getSource());
    shadowCols_.columnToTarget.put(useCol, linf.getTarget());
    
    if (!linf.isShadow()) {
      int useNColVal = linf.getUseColumn(false);
      Integer useNCol = new Integer(useNColVal);
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
  ** Node ordering
  */
  
  private ArrayList orderMyKids(Map targsPerSource, Map linkCounts, HashSet targsToGo, String node) {
    HashSet targs = (HashSet)targsPerSource.get(node);
    TreeMap kidMap = new TreeMap(Collections.reverseOrder());
    Iterator tait = targs.iterator();
    while (tait.hasNext()) {
      String nextTarg = (String)tait.next();
      Integer count = (Integer)linkCounts.get(nextTarg);
      TreeSet perCount = (TreeSet)kidMap.get(count);
      if (perCount == null) {
        perCount = new TreeSet();
        kidMap.put(count, perCount);
      }
      perCount.add(nextTarg);
    }
    
    ArrayList myKidsToProc = new ArrayList();
    Iterator kmit = kidMap.values().iterator();
    while (kmit.hasNext()) {
      TreeSet perCount = (TreeSet)kmit.next();
      Iterator pcit = perCount.iterator();
      while (pcit.hasNext()) {
        String kid = (String)pcit.next();
        if (targsToGo.contains(kid)) {
          myKidsToProc.add(kid);
        }
      }
    }
    return (myKidsToProc);
  }
  
  /***************************************************************************
  **
  ** Node ordering, non-recursive:
  */
  
  private void addMyKidsNR(ArrayList targets, Map targsPerSource, Map linkCounts, HashSet targsToGo, String node, ArrayList queue) {
    queue.add(node);
    while (!queue.isEmpty()) {
      node = (String)queue.remove(0);
      ArrayList myKids = orderMyKids(targsPerSource, linkCounts, targsToGo, node);
      Iterator ktpit = myKids.iterator();
      while (ktpit.hasNext()) {
        String kid = (String)ktpit.next();
        if (targsToGo.contains(kid)) {
          targsToGo.remove(kid);
          targets.add(kid);
          queue.add(kid);
        }
      }
    }
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
  ** Constructor for I/O only
  */

  BioFabricNetwork() {
    normalCols_ = new ColumnAssign();
    shadowCols_ = new ColumnAssign();
    rowToTarg_ = new HashMap();
    fullLinkDefs_ = new TreeMap();
    nonShadowedLinkMap_ = new TreeMap();
    nodeDefs_ = new HashMap();
    linkGrouping_ = new ArrayList();
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
    
    LinkInfo(FabricLink flink, int startRow, int endRow, int noShadowColumn, int shadowColumn, String colorKey) {
      myLink_ = (FabricLink)flink.clone();
      startRow_ = startRow;
      endRow_ = endRow;
      noShadowColumn_ = noShadowColumn;
      shadowColumn_ = shadowColumn;
      colorKey_ = colorKey;
    }
    
    int getStartRow() {
      return (startRow_);
    }
    
    int getEndRow() {
      return (endRow_);
    }
    
    int getUseColumn(boolean shadowEnabled) {
      if (!shadowEnabled && myLink_.isShadow()) {
        // FIX ME: Seen this case with submodel creation?
        throw new IllegalStateException();
      }
      return ((shadowEnabled) ? shadowColumn_ : noShadowColumn_);
    }
    
    String getColorKey() {
      return (colorKey_);
    }

    FabricLink getLink() {
      return (myLink_);
    }
    
    String getSource() {
      return (myLink_.getSrc());
    }
    
    String getTarget() {
      return (myLink_.getTrg());
    }
    
    FabricLink.AugRelation getAugRelation() {
      return (myLink_.getAugRelation());
    }
    
    boolean isShadow() {
      return (myLink_.isShadow());
    }
    
    boolean isDirected() {
      return (myLink_.isDirected());
    }

    int bottomRow() {
      return (Math.max(startRow_, endRow_));
    }
    
    int topRow() {
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
    public String nodeName;
    public int nodeRow;
    public String colorKey;

    private MinMax colRangeSha_;
    private MinMax colRangePln_;
    private MinMax plainDrainZone_;
    private MinMax shadowDrainZone_;
    
    NodeInfo(String nodeName, int nodeRow, String colorKey) {
      this.nodeName = nodeName;
      this.nodeRow = nodeRow;
      this.colorKey = colorKey;
      colRangeSha_ = new MinMax();
      colRangeSha_.init();
      colRangePln_ = new MinMax();
      colRangePln_.init();
      plainDrainZone_ = null;
      shadowDrainZone_ = null;
    }
    
    MinMax getDrainZone(boolean forShadow) {
      return (forShadow) ? shadowDrainZone_ : plainDrainZone_;
    }
    
    void setDrainZone(MinMax zone, boolean forShadow) {
      if (forShadow) {
        shadowDrainZone_ = zone;
      } else {
        plainDrainZone_ = zone;
      }
      return;
    }

    MinMax getColRange(boolean forShadow) {
      return (forShadow) ? colRangeSha_ : colRangePln_;
    }

    void updateMinMaxCol(int i, boolean forShadow) {
      MinMax useMM = (forShadow) ? colRangeSha_ : colRangePln_;
      useMM.update(i);
      return;
    }
  }
  
  /***************************************************************************
  **
  ** For passing around build data
  */
  
  public static abstract class BuildData {
    protected int mode;
    
    public BuildData(int mode) {
      this.mode = mode;
    }

    public int getMode() {
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

    public PreBuiltBuildData(BioFabricNetwork bfn, int mode) {
      super(mode);
      this.bfn = bfn;
    }
  }
  
  /***************************************************************************
  **
  ** For passing around build data
  */
  
  public static class OrigBuildData extends BuildData {
    Set allLinks;
    Set loneNodes;
    FabricColorGenerator colGen;

    public OrigBuildData(Set allLinks, Set loneNodes, FabricColorGenerator colGen, int mode) {
      super(mode);
      this.allLinks = allLinks;
      this.loneNodes = loneNodes;
      this.colGen = colGen;
    }
  }
  
  /***************************************************************************
  **
  ** For passing around build data
  */
  
  public static class RelayoutBuildData extends BuildData {
    BioFabricNetwork bfn;
    Set allLinks;
    Set loneNodes;
    FabricColorGenerator colGen;
    Map nodeOrder;
    List existingOrder;
    SortedMap linkOrder;
    List existingLinkGroups;
    List newLinkGroups;
    Set allNodes;
    LayoutMode layoutMode;
    
    public RelayoutBuildData(BioFabricNetwork fullNet, int mode) {
      super(mode);
      this.bfn = fullNet;
      this.allLinks = fullNet.getAllLinks(true);
      this.colGen = fullNet.colGen_;
      this.nodeOrder = null;
      this.existingOrder = fullNet.existingOrder();
      this.linkOrder = null;
      this.existingLinkGroups = fullNet.linkGrouping_;
      this.loneNodes = fullNet.getLoneNodes();
      this.allNodes = fullNet.nodeDefs_.keySet();
      this.layoutMode = fullNet.getLayoutMode();
    }
    
    public void setNodeOrder(Map nodeOrder) {
      this.nodeOrder = nodeOrder;
      return;
    }
    
    public void setLinkOrder(SortedMap linkOrder) {
      this.linkOrder = linkOrder;
      return;
    }

    public void setLinkGroups(List linkGroups) {
      this.newLinkGroups = linkGroups;
      return;
    }
  }

  /***************************************************************************
  **
  ** For passing around build data
  */
  
  public static class SelectBuildData extends BuildData {
     BioFabricNetwork fullNet;
     List subNodes;
     List subLinks;

    public SelectBuildData(BioFabricNetwork fullNet, List subNodes, List subLinks) {
      super(BUILD_FOR_SUBMODEL);
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
  
  static private class ColumnAssign  {
    HashMap columnToSource;
    HashMap columnToTarget;
    int columnCount;

    ColumnAssign() {
      this.columnToSource = new HashMap();
      this.columnToTarget = new HashMap();
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

  public static SortedMap extractRelations(List allLinks) {
    HashSet flipSet = new HashSet();
    HashSet flipRels = new HashSet();
    HashSet rels = new HashSet();
    Iterator alit = allLinks.iterator();
    while (alit.hasNext()) {
      FabricLink nextLink = (FabricLink)alit.next();
      FabricLink.AugRelation relation = nextLink.getAugRelation();
      if (!nextLink.isFeedback()) {  // Autofeedback not flippable
        FabricLink flipLink = nextLink.flipped();
        if (flipSet.contains(flipLink)) {
          flipRels.add(relation);
        } else {
          flipSet.add(flipLink);
        }
      }
      rels.add(relation);
    }
    
    //
    // We have a hint that something is signed if there are two
    // separate links running in opposite directions!
    //

    TreeMap relMap = new TreeMap();
    Boolean noDir = new Boolean(false);
    Boolean haveDir = new Boolean(true);
    Iterator rit = rels.iterator();
    while (rit.hasNext()) {
      FabricLink.AugRelation rel = (FabricLink.AugRelation)rit.next();
      relMap.put(rel, (flipRels.contains(rel)) ? haveDir : noDir);
    }
    return (relMap);
  }
  
  /***************************************************************************
  **
  ** Process a link set that has not had directionality established
  */

  public static void assignDirections(List allLinks, Map relMap) {
    Iterator alit = allLinks.iterator();
    while (alit.hasNext()) {
      FabricLink nextLink = (FabricLink)alit.next();
      FabricLink.AugRelation rel = nextLink.getAugRelation();
      Boolean isDir = (Boolean)relMap.get(rel);
      nextLink.installDirection(isDir);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** This culls a set of links to remove non-directional synonymous and
  ** duplicate links.  Note that shadow links have already been created
  ** and added to the allLinks list.
  */

  public static void preprocessLinks(List allLinks, Set retval, Set culled) {
    Iterator alit = allLinks.iterator();
    while (alit.hasNext()) {
      FabricLink nextLink = (FabricLink)alit.next();
      if (retval.contains(nextLink)) {
        culled.add(nextLink);
      } else if (!nextLink.isDirected()) {
        if (!nextLink.isFeedback()) {
          FabricLink flipLink = nextLink.flipped();
          if (retval.contains(flipLink)) {
            // Make the order consistent for a given src & pair!
            if (nextLink.compareTo(flipLink) < 0) {
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
      installWorker(new LinkGroupTagWorker(whiteboard), new MyGroupGlue());
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
      board.linkInfo = buildFromXML(elemName, attrs);
      retval = board.linkInfo;
      return (retval);
    }
    
    private LinkInfo buildFromXML(String elemName, Attributes attrs) throws IOException {
      String src = AttributeExtractor.extractAttribute(elemName, attrs, "link", "src", true);
      src = CharacterEntityMapper.unmapEntities(src, false);
      String trg = AttributeExtractor.extractAttribute(elemName, attrs, "link", "trg", true);
      trg = CharacterEntityMapper.unmapEntities(trg, false);
      String rel = AttributeExtractor.extractAttribute(elemName, attrs, "link", "rel", true);
      rel = CharacterEntityMapper.unmapEntities(rel, false);
      String directed = AttributeExtractor.extractAttribute(elemName, attrs, "link", "directed", true);
      Boolean dirObj = Boolean.valueOf(directed);
      String shadow = AttributeExtractor.extractAttribute(elemName, attrs, "link", "shadow", true);
      Boolean shadObj = Boolean.valueOf(shadow);
      FabricLink flink = new FabricLink(src, trg, rel, shadObj.booleanValue(), dirObj);
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
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      FabricFactory.FactoryWhiteboard board = (FabricFactory.FactoryWhiteboard)this.sharedWhiteboard_;
      board.nodeInfo = buildFromXML(elemName, attrs);
      retval = board.nodeInfo;
      return (retval);
    }
    
    private NodeInfo buildFromXML(String elemName, Attributes attrs) throws IOException {
      String name = AttributeExtractor.extractAttribute(elemName, attrs, "node", "name", true);
      name = CharacterEntityMapper.unmapEntities(name, false);
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
      NodeInfo retval;
      try {
        int nodeRow = Integer.valueOf(row).intValue();
        retval = new NodeInfo(name, nodeRow, color);
        
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
          retval.setDrainZone(new MinMax(minDrVal, maxDrVal), false);
        }
        if (minDrainSha != null) {
          int minDrValSha = Integer.valueOf(minDrainSha).intValue();
          int maxDrValSha = Integer.valueOf(maxDrainSha).intValue();
          retval.setDrainZone(new MinMax(minDrValSha, maxDrValSha), true);
        }
      } catch (NumberFormatException nfex) {
        throw new IOException();
      }
      return (retval);
    }
  }
}
