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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.systemsbiology.biofabric.analysis.Link;
import org.systemsbiology.biofabric.io.AttributeLoader;
import org.systemsbiology.biofabric.layouts.ControlTopLayout;
import org.systemsbiology.biofabric.layouts.DefaultEdgeLayout;
import org.systemsbiology.biofabric.layouts.DefaultLayout;
import org.systemsbiology.biofabric.layouts.EdgeLayout;
import org.systemsbiology.biofabric.layouts.HierDAGLayout;
import org.systemsbiology.biofabric.layouts.NodeClusterLayout;
import org.systemsbiology.biofabric.layouts.NodeLayout;
import org.systemsbiology.biofabric.layouts.NodeSimilarityLayout;
import org.systemsbiology.biofabric.layouts.SetLayout;
import org.systemsbiology.biofabric.layouts.WorldBankLayout;
import org.systemsbiology.biofabric.ui.FabricColorGenerator;
import org.systemsbiology.biofabric.util.AsynchExitRequestException;
import org.systemsbiology.biofabric.util.BTProgressMonitor;
import org.systemsbiology.biofabric.util.DataUtil;
import org.systemsbiology.biofabric.util.NID;
import org.systemsbiology.biofabric.util.UiUtil;
import org.systemsbiology.biofabric.util.UniqueLabeller;

/****************************************************************************
**
** This is the Network model.
*/

public abstract class BuildData {
  
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
                         BUILD_FROM_XML,
                         BUILD_FROM_SIF,
                         NODE_ATTRIB_LAYOUT,
                         LINK_ATTRIB_LAYOUT,
                         NODE_CLUSTER_LAYOUT,
                         CONTROL_TOP_LAYOUT,
                         HIER_DAG_LAYOUT,
                         MULTI_MODE_DAG_LAYOUT,
                         WORLD_BANK_LAYOUT,
                         SET_LAYOUT,
                         GROUP_PER_NETWORK_CHANGE,
                         BUILD_FROM_PLUGIN
                        };
                                                  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
                         
  protected BuildMode mode;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////   
        
  public BuildData(BuildMode mode) {
    this.mode = mode;
  }
     
  public BuildMode getMode() {
    return (mode);
  }  
  
  public boolean canRestore() {
    return (true);
  } 
  
  public void processSpecialtyBuildData() {
    return;
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
    public BioFabricNetwork.LayoutMode layoutMode;
    public UniqueLabeller idGen;
    
    public ControlTopLayout.CtrlMode cMode; 
    public ControlTopLayout.TargMode tMode;
    public List<String> fixedOrder; 
    public Map<String, Set<NID.WithName>> normNameToIDs;
    public Boolean pointUp;
    
    public AnnotationSet nodeAnnotForLayout;
    public Map<Boolean, AnnotationSet> linkAnnotsForLayout;
    
    public RelayoutBuildData(BioFabricNetwork fullNet, BuildMode mode, BTProgressMonitor monitor) throws AsynchExitRequestException {
      super(mode);
      this.bfn = fullNet;
      this.allLinks = fullNet.getAllLinks(true);
      this.colGen = fullNet.getColorGenerator();
      this.nodeOrder = null;
      this.existingIDOrder = fullNet.existingIDOrder();
      this.linkOrder = null;
      this.linkGroups = fullNet.getLinkGrouping();
      this.loneNodeIDs = fullNet.getLoneNodes(monitor);
      this.allNodeIDs = fullNet.getAllNodeDefinitions().keySet();
      this.clustAssign = (fullNet.nodeClustersAssigned()) ? fullNet.nodeClusterAssigment() : null;
      this.layoutMode = fullNet.getLayoutMode();
      this.idGen = fullNet.getGenerator();
      this.nodeAnnotForLayout = null;
      this.linkAnnotsForLayout = null;
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
      this.layoutMode = BioFabricNetwork.LayoutMode.PER_NODE_MODE;
      this.idGen = idGen;
      this.nodeAnnotForLayout = null;
      this.linkAnnotsForLayout = null;
    } 

    //
    // Allow for late binding of color generator:
    //
    
    public void setColorGen(FabricColorGenerator colGen) {
      this.colGen = colGen;
      return;
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
    
    public void setNodeAnnotations(AnnotationSet annots) {
      this.nodeAnnotForLayout = annots;
      return;
    }
    
    public void setLinkAnnotations(Map<Boolean, AnnotationSet> annots) {
      this.linkAnnotsForLayout = annots;
      return;
    }
      
    public void setGroupOrderAndMode(List<String> groupOrder, BioFabricNetwork.LayoutMode mode) {
      this.linkGroups = groupOrder;
      this.layoutMode = mode;
      return;
    }
    
    public void setCTL(ControlTopLayout.CtrlMode cMode, ControlTopLayout.TargMode tMode, List<String> fixedOrder, BioFabricNetwork bfn) {
      this.normNameToIDs = (fixedOrder == null) ? null : bfn.getNormNameToIDs();
      this.cMode = cMode;
      this.tMode = tMode;
      this.fixedOrder = fixedOrder;
      return;
    }
    
    public void setPointUp(Boolean pointUp) {
      this.pointUp = pointUp;
      return;
    }

    public boolean needsLayoutForRelayout() {
      switch (mode) {
        case DEFAULT_LAYOUT:
        case WORLD_BANK_LAYOUT:
        case CONTROL_TOP_LAYOUT:
        case HIER_DAG_LAYOUT:
        case SET_LAYOUT:      
        case REORDER_LAYOUT:
        case CLUSTERED_LAYOUT:      
        case NODE_CLUSTER_LAYOUT:
        	return (true);
        case NODE_ATTRIB_LAYOUT:
        case LINK_ATTRIB_LAYOUT:
        case GROUP_PER_NODE_CHANGE:
        case GROUP_PER_NETWORK_CHANGE:
        	// Already installed!
          return (false);
        case SHADOW_LINK_CHANGE:
        case BUILD_FOR_SUBMODEL:
        case BUILD_FROM_XML:
        case BUILD_FROM_SIF:
        case BUILD_FROM_PLUGIN:
        default:
        	// Not legal!
          throw new IllegalStateException();
      }
    }

    public NodeLayout getNodeLayout() {
    	switch (mode) {
    	  case DEFAULT_LAYOUT:
    	  case BUILD_FROM_SIF:
    	  	return (new DefaultLayout());
    	  case WORLD_BANK_LAYOUT:
    	  	return (new WorldBankLayout());
    	  case REORDER_LAYOUT:
        case CLUSTERED_LAYOUT:
          return (new NodeSimilarityLayout()); 	
        case NODE_CLUSTER_LAYOUT:
          return (new NodeClusterLayout());
        case CONTROL_TOP_LAYOUT:
          return (new ControlTopLayout(cMode, tMode, fixedOrder, normNameToIDs));
        case HIER_DAG_LAYOUT:  
          return (new HierDAGLayout(pointUp.booleanValue())); 
        case SET_LAYOUT:   
          UiUtil.fixMePrintout("Get customized set dialog");
          FabricLink link = allLinks.iterator().next();
          System.out.print(link + " means what?");            
          return (new SetLayout(pointUp.booleanValue() ? SetLayout.LinkMeans.BELONGS_TO : SetLayout.LinkMeans.CONTAINS)); 
    	  default:
    	  	System.err.println("Mode = " + mode);
    	  	UiUtil.fixMePrintout("Should throw exception");
    	  	return (new DefaultLayout());
    	} 	
    }

    public EdgeLayout getEdgeLayout() {
    	switch (mode) {
    	  case REORDER_LAYOUT:
        case CLUSTERED_LAYOUT:  
        case NODE_CLUSTER_LAYOUT:
           // The above layouts do edge layout as part of node layout:
          return (null);	
    	  default:
    	  	return (new DefaultEdgeLayout());	
    	}
    }

  }
  
  /***************************************************************************
  **
  ** For passing around build data
  */  
  
  public static class SelectBuildData extends BuildData {
     BioFabricNetwork fullNet;
     List<BioFabricNetwork.NodeInfo> subNodes;
     List<BioFabricNetwork.LinkInfo> subLinks;

    public SelectBuildData(BioFabricNetwork fullNet, List<BioFabricNetwork.NodeInfo> subNodes, List<BioFabricNetwork.LinkInfo> subLinks) {
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
}
