/*
**    Copyright (C) 2003-2018 Institute for Systems Biology 
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

package org.systemsbiology.biofabric.plugin.core.align;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.systemsbiology.biofabric.io.FabricFactory;
import org.systemsbiology.biofabric.io.FileLoadFlows;
import org.systemsbiology.biofabric.io.GWImportLoader;
import org.systemsbiology.biofabric.model.BioFabricNetwork;
import org.systemsbiology.biofabric.model.BuildExtractor;
import org.systemsbiology.biofabric.model.FabricLink;
import org.systemsbiology.biofabric.parser.AbstractFactoryClient;
import org.systemsbiology.biofabric.parser.GlueStick;
import org.systemsbiology.biofabric.plugin.BioFabricToolPlugIn;
import org.systemsbiology.biofabric.plugin.BioFabricToolPlugInCmd;
import org.systemsbiology.biofabric.plugin.BioFabricToolPlugInData;
import org.systemsbiology.biofabric.plugin.PlugInNetworkModelAPI;
import org.systemsbiology.biofabric.util.AsynchExitRequestException;
import org.systemsbiology.biofabric.util.AttributeExtractor;
import org.systemsbiology.biofabric.util.BackgroundWorker;
import org.systemsbiology.biofabric.util.BackgroundWorkerClient;
import org.systemsbiology.biofabric.util.BackgroundWorkerControlManager;
import org.systemsbiology.biofabric.util.BackgroundWorkerOwner;
import org.systemsbiology.biofabric.util.CharacterEntityMapper;
import org.systemsbiology.biofabric.util.ExceptionHandler;
import org.systemsbiology.biofabric.util.Indenter;
import org.systemsbiology.biofabric.util.NID;
import org.systemsbiology.biofabric.util.ResourceManager;
import org.systemsbiology.biofabric.util.UiUtil;
import org.systemsbiology.biofabric.util.UniqueLabeller;
import org.systemsbiology.biotapestry.biofabric.FabricCommands;

import org.xml.sax.Attributes;

/****************************************************************************
**
** Class for doing network alignment
*/

public class NetworkAlignmentPlugIn implements BioFabricToolPlugIn {
  
  private ArrayList<BioFabricToolPlugInCmd> myCmds_;
  private String myTag_;
  private NetAlignStats netAlignStats_;
  private FileLoadFlows flf_;
  private JFrame topWindow_;
  private BackgroundWorkerControlManager bwcm_;
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Null constructor (required)
  */
  
  public NetworkAlignmentPlugIn() {
    myCmds_ = new ArrayList<BioFabricToolPlugInCmd>();
    myCmds_.add(new LoadNetAlignGroupsCmd());
    myCmds_.add(new LoadNetAlignOrphanCmd());
    myCmds_.add(new LoadNetAlignCaseIICmd());
    myCmds_.add(new NetAlignScoresCmd()); 
    netAlignStats_ = new NetAlignStats();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Set the unique tag
  */
  
  public void setUniquePlugInTag(String tag) {
    myTag_ = tag;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the unique tag
  */
  
  public String getUniquePlugInTag() {
    return (myTag_);
  }

  /***************************************************************************
  **
  ** Install a new network
  */
  
  public void newNetworkInstalled(BioFabricNetwork bfn) {
    UiUtil.fixMePrintout("Drop stats if new network is not an alignment");
    for (BioFabricToolPlugInCmd cmd : myCmds_) {
      ((Enabler)cmd).setEnabled(true);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** InstallAPI
  */
  
  public void installAPI(PlugInNetworkModelAPI api) {
    flf_ = api.getFileUtilities();
    topWindow_ = api.getTopWindow();
    bwcm_ = api.getBWCtrlMgr();
    for (BioFabricToolPlugInCmd cmd : myCmds_) {
      ((Enabler)cmd).setEnabled(true);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Get name of tool menu to display
  */
  
  public String getToolMenu() {
    ResourceManager rMan = ResourceManager.getManager();  // DOES NOT BELONG HERE
    return (rMan.getString("command.alignmentCommands"));
  }
  
  /***************************************************************************
  **
  ** Get count of commands
  */
  
  public int getCommandCount() {
    return (myCmds_.size());   
  }
  
  /***************************************************************************
  **
  ** Get the nth command
  */
  
  public BioFabricToolPlugInCmd getCommand(int index) {
    return (myCmds_.get(index));
  }
 
  /***************************************************************************
  **
  ** Write session data to given output
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    if (!netAlignStats_.hasStats()) {
      return;
    }
    ind.up();
    ind.indent();
    String name = getClass().getName();
    
    out.print("<");
    out.print(name);
    out.println(">");

    ind.up();
    netAlignStats_.writeXML(out, ind);
    ind.down();
    ind.indent();
    out.print("</");
    out.print(name);
    out.println(">");
    ind.down();
    return;
  }  
  
  /***************************************************************************
  **
  ** Get XML Reader
  */
 
  public AbstractFactoryClient getXMLWorker(FabricFactory.FactoryWhiteboard board) {
    return (new PlugInWorker(board, this));
  }
  
  /***************************************************************************
  **
  ** Attach session data read from XML
  */
 
  public void attachXMLData(BioFabricToolPlugInData data) {
    netAlignStats_ = (NetAlignStats)data;
    return;   
  }
 
  /***************************************************************************
   **
   ** Create individual networks from two files (.gw or .sif) and one .align file
   */
  
  private boolean networkAlignmentFromSources(NetworkAlignmentDialog.NetworkAlignmentDialogInfo nadi,
                                              NetworkAlignmentBuildData.ViewType outType) {
    
    UniqueLabeller idGen = new UniqueLabeller();
    
    //
    // create the individual networks (links + lone nodes)
    //
  
    ArrayList<FabricLink> linksGraphA = new ArrayList<FabricLink>();
    HashSet<NID.WithName> lonersGraphA = new HashSet<NID.WithName>();
    
    if (GWImportLoader.isGWFile(nadi.graphA)) {
      flf_.loadFromASource(nadi.graphA, linksGraphA, lonersGraphA, null, idGen, true, FileLoadFlows.FileLoadType.GW);
    } else {
      flf_.loadFromASource(nadi.graphA, linksGraphA, lonersGraphA, null, idGen, true, FileLoadFlows.FileLoadType.SIF);
    } // assume it's sif if it's not gw
    
    
    ArrayList<FabricLink> linksGraphB = new ArrayList<FabricLink>();
    HashSet<NID.WithName> lonersGraphB = new HashSet<NID.WithName>();
    
    if (GWImportLoader.isGWFile(nadi.graphB)) {
      flf_.loadFromASource(nadi.graphB, linksGraphB, lonersGraphB, null, idGen, true, FileLoadFlows.FileLoadType.GW);
    } else {
      flf_.loadFromASource(nadi.graphB, linksGraphB, lonersGraphB, null, idGen, true, FileLoadFlows.FileLoadType.SIF);
    }
    
    return (networkAlignmentStepTwo(nadi, linksGraphA, lonersGraphA, linksGraphB, lonersGraphB, idGen, outType));
  }
  
  /**************************************************************************
   **
   ** Load the alignment file
   */
  
  private Map<NID.WithName, NID.WithName> loadTheAlignmentFile(File file,
                                                              ArrayList<FabricLink> linksGraph1, HashSet<NID.WithName> loneNodesGraph1,
                                                              ArrayList<FabricLink> linksGraph2, HashSet<NID.WithName> loneNodesGraph2) {
    
    Map<NID.WithName, NID.WithName> mapG1toG2 = new HashMap<NID.WithName, NID.WithName>();
    try {
  
      AlignmentLoader.NetAlignFileStats stats = new AlignmentLoader.NetAlignFileStats();
      AlignmentLoader alod = new AlignmentLoader();
      
      alod.readAlignment(file, mapG1toG2, stats, linksGraph1, loneNodesGraph1, linksGraph2, loneNodesGraph2);
  
      if (!stats.badLines.isEmpty()) {
        ResourceManager rMan = ResourceManager.getManager();
        String badLineFormat = rMan.getString("netAlignRead.badLineFormat");
        String badLineMsg = MessageFormat.format(badLineFormat, new Object[] {Integer.valueOf(stats.badLines.size())});
        JOptionPane.showMessageDialog(topWindow_, badLineMsg,
                rMan.getString("netAlignRead.badLineTitle"),
                JOptionPane.WARNING_MESSAGE);
      }
      if (!stats.dupLines.isEmpty()) {
        ResourceManager rMan = ResourceManager.getManager();
        String dupLineFormat = rMan.getString("netAlignRead.dupLineFormat");
        String dupLineMsg = MessageFormat.format(dupLineFormat, new Object[] {Integer.valueOf(stats.dupLines.size())});
        JOptionPane.showMessageDialog(topWindow_, dupLineMsg,
                rMan.getString("netAlignRead.dupLineTitle"),
                JOptionPane.WARNING_MESSAGE);
      }
    } catch (IOException ioe) {
      flf_.displayFileInputError(ioe);
      return (null);
    }
    FabricCommands.setPreference("AttribDirectory", file.getAbsoluteFile().getParent());
    return (mapG1toG2);
  }
  
  /***************************************************************************
   **
   ** Merges individual networks using alignment
   */
  
  private boolean networkAlignmentStepTwo(NetworkAlignmentDialog.NetworkAlignmentDialogInfo nadi,
                                          ArrayList<FabricLink> linksGraphA, HashSet<NID.WithName> loneNodeIDsGraphA,
                                          ArrayList<FabricLink> linksGraphB, HashSet<NID.WithName> loneNodeIDsGraphB,
                                          UniqueLabeller idGen, NetworkAlignmentBuildData.ViewType outType) {
    //
    // Assign GraphA and GraphB to Graph1 and Graph2
    //
    
    NetAlignGraphStructure struct = new NetAlignGraphStructure();
    boolean worked = assignGraphs(linksGraphA, loneNodeIDsGraphA, linksGraphB, loneNodeIDsGraphB, nadi.align, struct);
    if (! worked) {
      return (true);
    }
  
    // small graph G1
    ArrayList<FabricLink> linksSmall = struct.linksSmall;
    HashSet<NID.WithName> lonersSmall = struct.lonersSmall;
  
    // large graph G2
    ArrayList<FabricLink> linksLarge = struct.linksLarge;
    HashSet<NID.WithName> lonersLarge = struct.lonersLarge;

    // Alignment processing
    Map<NID.WithName, NID.WithName> mapG1toG2 =
            loadTheAlignmentFile(nadi.align, linksSmall, lonersSmall, linksLarge, lonersLarge);
    if (mapG1toG2 == null) {
      return (true);
    }
    
    Map<NID.WithName, NID.WithName> perfectG1toG2;
    if (nadi.perfect != null) {
      perfectG1toG2 = loadTheAlignmentFile(nadi.perfect, linksSmall, lonersSmall, linksLarge, lonersLarge);
      if (perfectG1toG2 == null) {
        return (true);
      }
    } else {
      perfectG1toG2 = null;
    }
    
    //
    // The CaseII cycle alignment can use the perfect alignment file, so we need to be more
    // specific about when the perfect/Group analysis is being done:
    //
    
    boolean doingPerfectGroup = (outType == NetworkAlignmentBuildData.ViewType.GROUP) && 
                                (perfectG1toG2 != null);
       
    File holdIt;
    try {
      holdIt = File.createTempFile("BioFabricHold", ".zip");
      holdIt.deleteOnExit();
    } catch (IOException ioex) {
      holdIt = null;
    }
    
    //
    // First process the given (main) alignment
    //
    
    NetworkAlignmentBuilder nab = new NetworkAlignmentBuilder();
    
    ArrayList<FabricLink> mergedLinks = new ArrayList<FabricLink>();
    Set<NID.WithName> mergedLoneNodeIDs = new HashSet<NID.WithName>();
    SortedMap<FabricLink.AugRelation, Boolean> relMap = new TreeMap<FabricLink.AugRelation, Boolean>();
    Set<FabricLink> reducedLinks = new HashSet<FabricLink>();
    Map<NID.WithName, Boolean> mergedToCorrectNC = null, isAlignedNode = new HashMap<NID.WithName, Boolean>();
    if (doingPerfectGroup) {
      mergedToCorrectNC = new HashMap<NID.WithName, Boolean>();
    }
    
    boolean finished = nab.processNetAlign(mergedLinks, mergedLoneNodeIDs, mapG1toG2, perfectG1toG2, mergedToCorrectNC,
            isAlignedNode, linksSmall, lonersSmall, linksLarge, lonersLarge, relMap, outType, idGen, holdIt);
  
    //
    // Second process the perfect alignment (if given)
    //
    
    nab = null;
    ArrayList<FabricLink> mergedLinksPerfect = null;
    Set<NID.WithName> mergedLoneNodeIDsPerfect = null;
    SortedMap<FabricLink.AugRelation, Boolean> relMapPerfect = null;
    Set<FabricLink> reducedLinksPerfect = null;
    Map<NID.WithName, Boolean> isAlignedNodePerfect = null;
    
    if (finished && doingPerfectGroup) {
      //
      // We now have to process the Perfect alignment so we can compare the links/nodes (topology, etc)
      // between the given alignment and the perfect alignment. The added -'Perfect' on the variable names
      // signifies it.
      //
      nab = new NetworkAlignmentBuilder();
      mergedLinksPerfect = new ArrayList<FabricLink>();
      mergedLoneNodeIDsPerfect = new HashSet<NID.WithName>();
      relMapPerfect = new TreeMap<FabricLink.AugRelation, Boolean>();
      reducedLinksPerfect = new HashSet<FabricLink>();
      isAlignedNodePerfect = new HashMap<NID.WithName, Boolean>();
      
      finished = nab.processNetAlign(mergedLinksPerfect, mergedLoneNodeIDsPerfect, perfectG1toG2, null, null,
              isAlignedNodePerfect, linksSmall, lonersSmall, linksLarge, lonersLarge, relMapPerfect, 
              NetworkAlignmentBuildData.ViewType.GROUP, idGen, holdIt);
    }
  
    // i.e. same 2 graphs and perfect alignment yield an empty network
    if (mergedLinks.isEmpty()) {
      JOptionPane.showMessageDialog(topWindow_, (ResourceManager.getManager()).getString("networkAlignment.emptyNetwork"),
              (ResourceManager.getManager()).getString("networkAlignment.emptyNetworkTitle"),
              JOptionPane.WARNING_MESSAGE);
      return (false);
    }
  
    if (finished) { // for main alignment      
      finished = flf_.handleDirectionsDupsAndShadows(mergedLinks, mergedLoneNodeIDs, false, relMap, reducedLinks, holdIt, true);
    }
    
    if (finished && doingPerfectGroup) { // for perfect alignment
      finished = flf_.handleDirectionsDupsAndShadows(mergedLinksPerfect, mergedLoneNodeIDsPerfect, false, relMapPerfect, reducedLinksPerfect, holdIt, true);
    }
  
    if (finished) { // Score Report
      finished = networkAlignmentStepFour(reducedLinks, mergedLoneNodeIDs, isAlignedNode, mergedToCorrectNC,
              reducedLinksPerfect, mergedLoneNodeIDsPerfect, isAlignedNodePerfect, netAlignStats_,
              linksSmall, lonersSmall, linksLarge, lonersLarge, mapG1toG2, perfectG1toG2);
    }
   
    if (finished) { // Load the alignments
      
      //
      // If we are doing a CaseII Cycle layout, we want to have a full list of the nodes in both
      // networks:
      //
      
      Set<NID.WithName> allLargerNodes = new HashSet<NID.WithName>(lonersLarge);
      for (FabricLink ll: linksLarge) {
        allLargerNodes.add(ll.getSrcID());
        allLargerNodes.add(ll.getTrgID());
      }
      
      Set<NID.WithName> allSmallerNodes = new HashSet<NID.WithName>(lonersSmall);
      for (FabricLink ll: linksSmall) {
        allSmallerNodes.add(ll.getSrcID());
        allSmallerNodes.add(ll.getTrgID());
      }
  
      networkAlignmentStepFive(allLargerNodes, allSmallerNodes, reducedLinks, mergedLoneNodeIDs, 
                               mergedToCorrectNC, isAlignedNode,
                               mapG1toG2, perfectG1toG2, linksLarge, lonersLarge, netAlignStats_, outType,
                               nadi.mode, idGen, nadi.align, holdIt);
 
    }
    return (true);
  }
  

  
  /***************************************************************************
   **
   ** Process NetAlign Score Reports
   */
  
  private boolean networkAlignmentStepFour(Set<FabricLink> reducedLinks, Set<NID.WithName> loneNodeIDs, Map<NID.WithName, Boolean> isAlignedNode,
                                           Map<NID.WithName, Boolean> mergedToCorrectNC, Set<FabricLink> reducedLinksPerfect,
                                           Set<NID.WithName> loneNodeIDsPerfect, Map<NID.WithName, Boolean> isAlignedNodePerfect,
                                           NetAlignStats report,
                                           ArrayList<FabricLink> linksSmall, HashSet<NID.WithName> lonersSmall,
                                           ArrayList<FabricLink> linksLarge, HashSet<NID.WithName> lonersLarge,
                                           Map<NID.WithName, NID.WithName> mapG1toG2, Map<NID.WithName, NID.WithName> perfectG1toG2) {
    File holdIt;
    try {
      holdIt = File.createTempFile("BioFabricHold", ".zip");
      holdIt.deleteOnExit();
    } catch (IOException ioex) {
      holdIt = null;
    }
    NetAlignMeasureBuilder namb = new NetAlignMeasureBuilder();
    
    boolean finished = namb.processNetAlignMeasures(reducedLinks, loneNodeIDs, isAlignedNode, mergedToCorrectNC,
            reducedLinksPerfect, loneNodeIDsPerfect, isAlignedNodePerfect, report, linksSmall, lonersSmall,
            linksLarge, lonersLarge, mapG1toG2, perfectG1toG2, holdIt);
    
    return (finished);
  }
  
  /***************************************************************************
   **
   ** Build the network alignment
   */
  
  private boolean networkAlignmentStepFive(Set<NID.WithName> allLargerNodes,
                                           Set<NID.WithName> allSmallerNodes,
                                           Set<FabricLink> reducedLinks, Set<NID.WithName> loneNodeIDs,
                                           Map<NID.WithName, Boolean> mergedToCorrect, 
                                           Map<NID.WithName, Boolean> isAlignedNode,
                                           Map<NID.WithName, NID.WithName> mapG1toG2,
                                           Map<NID.WithName, NID.WithName> perfectMap,
                                           ArrayList<FabricLink> linksLarge, HashSet<NID.WithName> lonersLarge,
                                           NetAlignStats report, 
                                           NetworkAlignmentBuildData.ViewType viewType, 
                                           NodeGroupMap.PerfectNGMode mode,
                                           UniqueLabeller idGen, File align, File holdIt) {

    HashMap<NID.WithName, String> emptyClustMap = new HashMap<NID.WithName, String>();
    NetworkAlignmentBuildData nabd = 
      new NetworkAlignmentBuildData(idGen, allLargerNodes, allSmallerNodes, reducedLinks, loneNodeIDs, mergedToCorrect,
                                    isAlignedNode, report, emptyClustMap,
                                    viewType, mapG1toG2, perfectMap, linksLarge, lonersLarge, mode);

    try {
      flf_.buildNetworkForPlugIn(nabd, holdIt); 
    } catch (OutOfMemoryError oom) {
      ExceptionHandler.getHandler().displayOutOfMemory(oom);
      return (false);
    }
    flf_.setCurrentXMLFile(null);
    FabricCommands.setPreference("LoadDirectory", align.getAbsoluteFile().getParent());
    flf_.manageWindowTitle(align.getName());
    return true;
  }
  
  /***************************************************************************
   **
   ** Assign GraphA and GraphB to Graph1 and Graph2 using comparisons
   */
  
  private boolean assignGraphs(ArrayList<FabricLink> linksGraphA, HashSet<NID.WithName> loneNodeIDsGraphA,
                               ArrayList<FabricLink> linksGraphB, HashSet<NID.WithName> loneNodeIDsGraphB,
                               File align, NetAlignGraphStructure struct) {
  
    Set<NID.WithName> nodesA = null, nodesB = null;
    try {
      nodesA = BuildExtractor.extractNodes(linksGraphA, loneNodeIDsGraphA, null);
      nodesB = BuildExtractor.extractNodes(linksGraphB, loneNodeIDsGraphB, null);
    } catch (AsynchExitRequestException aere) {
      // should never happen
    }
    int numNodesA = nodesA.size();
    int numNodesB = nodesB.size();
    // First compare node number
    if (numNodesA > numNodesB) {
      // G1 = B, G2 = A
      struct.linksLarge = linksGraphA;
      struct.lonersLarge = loneNodeIDsGraphA;
      struct.linksSmall = linksGraphB;
      struct.lonersSmall = loneNodeIDsGraphB;
      return (true);
    } else if (numNodesA < numNodesB) {
      // G1 = A, G2 = B
      struct.linksLarge = linksGraphB;
      struct.lonersLarge = loneNodeIDsGraphB;
      struct.linksSmall = linksGraphA;
      struct.lonersSmall = loneNodeIDsGraphA;
      return (true);
    }
    
    //
    // Now use the columns from the alignment file for insight
    //
    // Generate necessary structures for comparison
    //
    
    AlignmentLoader alod = new AlignmentLoader();
    Map<String, String> mapG1ToG2Str;
    try {
      mapG1ToG2Str = alod.readAlignment(align, new AlignmentLoader.NetAlignFileStats());
    } catch (IOException ioe) {
      flf_.displayFileInputError(ioe);
      return (false);
    }
  
    Set<String> namesG1 = new HashSet<String>(), namesG2 = new HashSet<String>();
    for (Map.Entry<String, String> match : mapG1ToG2Str.entrySet()) {
      namesG1.add(match.getKey());
      namesG2.add(match.getValue());
    }
  
    Set<String> namesA = new HashSet<String>(), namesB = new HashSet<String>();
    for (NID.WithName node : nodesA) {
      namesA.add(node.getName());
    }
    for (NID.WithName node : nodesB) {
      namesB.add(node.getName());
    }
    
    //
    // Can only test cases of equality here because nearly all error checking
    // is done in the main read.
    //
  
    if (! namesA.equals(namesB)) { // if node sets are equal, cannot distinguish without link # comparison
      if (namesG1.equals(namesA) && namesG2.equals(namesB)) {
        // G1 = A, G2 = B
        struct.linksLarge = linksGraphB;
        struct.lonersLarge = loneNodeIDsGraphB;
        struct.linksSmall = linksGraphA;
        struct.lonersSmall = loneNodeIDsGraphA;
        return (true);
      } else if (namesG1.equals(namesB) && namesG2.equals(namesA)) {
        // G1 = B, G2 = A
        struct.linksLarge = linksGraphA;
        struct.lonersLarge = loneNodeIDsGraphA;
        struct.linksSmall = linksGraphB;
        struct.lonersSmall = loneNodeIDsGraphB;
        return (true);
      }
    }
    
    //
    // If both graphs still have same node sets, link size is the only option
    //
  
    int numLinksA = linksGraphA.size();
    int numLinksB = linksGraphB.size();
    // if #links are still equal, we choose graphA as smaller (G1) and graphB as larger (G2)
    if (numLinksA > numLinksB) {
      // G1 = B, G2 = A
      struct.linksLarge = linksGraphA;
      struct.lonersLarge = loneNodeIDsGraphA;
      struct.linksSmall = linksGraphB;
      struct.lonersSmall = loneNodeIDsGraphB;
      return (true);
    } else {
      // G1 = A, G2 = B
      struct.linksLarge = linksGraphB;
      struct.lonersLarge = loneNodeIDsGraphB;
      struct.linksSmall = linksGraphA;
      struct.lonersSmall = loneNodeIDsGraphA;
      return (true);
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  interface Enabler {
    public void setEnabled(boolean isEnabled);
  }
  
  
  /***************************************************************************
  **
  ** Command
  */
  
  private class NetAlignScoresCmd implements BioFabricToolPlugInCmd, Enabler {
   
    public void setEnabled(boolean isEnabled) {
      // Depends on if we have non-null scores.
      if (!isEnabled) {
        System.out.println("Turn off " + isEnabled);
      } else {
        System.out.println("Set Enabled? " + netAlignStats_.hasStats());
      } 
    }
      
    public String getCommandName() {
      ResourceManager rMan = ResourceManager.getManager();  // DOES NOT BELONG HERE
      return (rMan.getString("command.netAlignMeasures"));
    }   
    
    public boolean performOperation(JFrame topFrame) {
      if (!netAlignStats_.hasStats()) {
        return (false);
      }    
    
      NetAlignMeasureDialog scoreDialog = new NetAlignMeasureDialog(topFrame, netAlignStats_);
      scoreDialog.setVisible(true);
      return (true);
    }
  
    public boolean isEnabled() {
      System.out.println("Enabled? " + netAlignStats_.hasStats());
      return (netAlignStats_.hasStats());    
    } 
  }
 
  /***************************************************************************
  **
  ** Command
  */
  
  private class LoadNetAlignGroupsCmd implements BioFabricToolPlugInCmd, Enabler {
  
    private boolean enabled_;

    public void setEnabled(boolean isEnabled) {
      enabled_ = isEnabled;
    }
    
    public String getCommandName() {
      ResourceManager rMan = ResourceManager.getManager();  // DOES NOT BELONG HERE
      return (rMan.getString("command.netAlignGroupLayout"));  
    }
    
    public boolean performOperation(JFrame topFrame) {
      if (!enabled_) {
        return (false);
      }    
    
      NetworkAlignmentDialog nad = new NetworkAlignmentDialog(topFrame,  
                                                              NetworkAlignmentBuildData.ViewType.GROUP);
      nad.setVisible(true);
      
      if(!nad.haveResult()) {
        return (false);
      }
  
      NetworkAlignmentDialog.NetworkAlignmentDialogInfo nai = nad.getNAInfo();
      
      boolean filesNotOkay =
              !flf_.standardFileChecks(nai.graphA, FileLoadFlows.FILE_MUST_EXIST, FileLoadFlows.FILE_CAN_CREATE_DONT_CARE,
                                       FileLoadFlows.FILE_DONT_CHECK_OVERWRITE, FileLoadFlows.FILE_MUST_BE_FILE,
                                       FileLoadFlows.FILE_CAN_WRITE_DONT_CARE, FileLoadFlows.FILE_CAN_READ) ||
              !flf_.standardFileChecks(nai.graphB, FileLoadFlows.FILE_MUST_EXIST, FileLoadFlows.FILE_CAN_CREATE_DONT_CARE,
                                       FileLoadFlows.FILE_DONT_CHECK_OVERWRITE, FileLoadFlows.FILE_MUST_BE_FILE,
                                       FileLoadFlows.FILE_CAN_WRITE_DONT_CARE, FileLoadFlows.FILE_CAN_READ) ||
              !flf_.standardFileChecks(nai.align, FileLoadFlows.FILE_MUST_EXIST, FileLoadFlows.FILE_CAN_CREATE_DONT_CARE,
                  FileLoadFlows.FILE_DONT_CHECK_OVERWRITE, FileLoadFlows.FILE_MUST_BE_FILE,
                  FileLoadFlows.FILE_CAN_WRITE_DONT_CARE, FileLoadFlows.FILE_CAN_READ);
      if (nai.perfect != null) {
        filesNotOkay = !flf_.standardFileChecks(nai.perfect, FileLoadFlows.FILE_MUST_EXIST, FileLoadFlows.FILE_CAN_CREATE_DONT_CARE,
                                                FileLoadFlows.FILE_DONT_CHECK_OVERWRITE, FileLoadFlows.FILE_MUST_BE_FILE,
                                                FileLoadFlows.FILE_CAN_WRITE_DONT_CARE, FileLoadFlows.FILE_CAN_READ);
      }
      
      if (filesNotOkay) {
        return (false);
      }
      return (networkAlignmentFromSources(nai, NetworkAlignmentBuildData.ViewType.GROUP));
    }
    
    public boolean isEnabled() {
      return (enabled_);    
    }
  
  }
  
  /***************************************************************************
  **
  ** Command
  */
  
  private class LoadNetAlignCaseIICmd implements BioFabricToolPlugInCmd, Enabler {
  
    private boolean enabled_;

    public void setEnabled(boolean isEnabled) {
      enabled_ = isEnabled;
    }
    
    public String getCommandName() {
      ResourceManager rMan = ResourceManager.getManager();  // DOES NOT BELONG HERE
      return (rMan.getString("command.netAlignCaseIILayout"));  
    }
    
    public boolean performOperation(JFrame topFrame) {
      if (!enabled_) {
        return (false);
      }    
    
      NetworkAlignmentDialog nad = new NetworkAlignmentDialog(topFrame, NetworkAlignmentBuildData.ViewType.CYCLE);
      nad.setVisible(true);
      
      if(!nad.haveResult()) {
        return (false);
      }
  
      NetworkAlignmentDialog.NetworkAlignmentDialogInfo nai = nad.getNAInfo();
      
      boolean filesNotOkay =
              !flf_.standardFileChecks(nai.graphA, FileLoadFlows.FILE_MUST_EXIST, FileLoadFlows.FILE_CAN_CREATE_DONT_CARE,
                                       FileLoadFlows.FILE_DONT_CHECK_OVERWRITE, FileLoadFlows.FILE_MUST_BE_FILE,
                                       FileLoadFlows.FILE_CAN_WRITE_DONT_CARE, FileLoadFlows.FILE_CAN_READ) ||
              !flf_.standardFileChecks(nai.graphB, FileLoadFlows.FILE_MUST_EXIST, FileLoadFlows.FILE_CAN_CREATE_DONT_CARE,
                                       FileLoadFlows.FILE_DONT_CHECK_OVERWRITE, FileLoadFlows.FILE_MUST_BE_FILE,
                                       FileLoadFlows.FILE_CAN_WRITE_DONT_CARE, FileLoadFlows.FILE_CAN_READ) ||
              !flf_.standardFileChecks(nai.align, FileLoadFlows.FILE_MUST_EXIST, FileLoadFlows.FILE_CAN_CREATE_DONT_CARE,
                  FileLoadFlows.FILE_DONT_CHECK_OVERWRITE, FileLoadFlows.FILE_MUST_BE_FILE,
                  FileLoadFlows.FILE_CAN_WRITE_DONT_CARE, FileLoadFlows.FILE_CAN_READ);

      if (filesNotOkay) {
        return (false);
      }
      return (networkAlignmentFromSources(nai, NetworkAlignmentBuildData.ViewType.CYCLE));
    }
    
    public boolean isEnabled() {
      return (enabled_);    
    }
  
  }

  
  /***************************************************************************
   **
   ** Command
   */
  
  private class LoadNetAlignOrphanCmd implements BioFabricToolPlugInCmd, Enabler {
 
    private boolean enabled_;
    
    public void setEnabled(boolean isEnabled) {
      enabled_ = isEnabled;
    }
    
    public String getCommandName() {
      ResourceManager rMan = ResourceManager.getManager();  // DOES NOT BELONG HERE
      return (rMan.getString("command.orphanLayout"));  
    }
    
    public boolean performOperation(JFrame topFrame) {
      if (!enabled_) {
        return (false);
      }
      
      NetworkAlignmentDialog nad = new NetworkAlignmentDialog(topFrame, NetworkAlignmentBuildData.ViewType.ORPHAN);
      nad.setVisible(true);
      
      if(!nad.haveResult()) {
        return (false);
      }
      
      NetworkAlignmentDialog.NetworkAlignmentDialogInfo nai = nad.getNAInfo();
      
      boolean filesNotOkay =
              !flf_.standardFileChecks(nai.graphA, FileLoadFlows.FILE_MUST_EXIST, FileLoadFlows.FILE_CAN_CREATE_DONT_CARE,
                                       FileLoadFlows.FILE_DONT_CHECK_OVERWRITE, FileLoadFlows.FILE_MUST_BE_FILE,
                                       FileLoadFlows.FILE_CAN_WRITE_DONT_CARE, FileLoadFlows.FILE_CAN_READ) ||
              !flf_.standardFileChecks(nai.graphB, FileLoadFlows.FILE_MUST_EXIST, FileLoadFlows.FILE_CAN_CREATE_DONT_CARE,
                                       FileLoadFlows.FILE_DONT_CHECK_OVERWRITE, FileLoadFlows.FILE_MUST_BE_FILE,
                                       FileLoadFlows.FILE_CAN_WRITE_DONT_CARE, FileLoadFlows.FILE_CAN_READ) ||
              !flf_.standardFileChecks(nai.align, FileLoadFlows.FILE_MUST_EXIST, FileLoadFlows.FILE_CAN_CREATE_DONT_CARE,
                                       FileLoadFlows.FILE_DONT_CHECK_OVERWRITE, FileLoadFlows.FILE_MUST_BE_FILE,
                                       FileLoadFlows.FILE_CAN_WRITE_DONT_CARE, FileLoadFlows.FILE_CAN_READ);
      
      if (filesNotOkay) {
        return (false);
      }
      return (networkAlignmentFromSources(nai, NetworkAlignmentBuildData.ViewType.ORPHAN));
    }
    
    public boolean isEnabled() {
      return (enabled_);    
    }
  }
  
  /****************************************************************************
  **
  ** Contains common network alignment scores
  */
  
  public static class NetAlignStats implements BioFabricToolPlugInData {
    
    private List<NetAlignMeasure> measures_;
    
    public NetAlignStats() {
      measures_ = new ArrayList<NetAlignMeasure>();
    }
    
    public NetAlignStats(List<NetAlignMeasure> measures) {
      this.measures_ = measures;
    }
     
    public void addAMeasure(NetAlignMeasure measure) {
      measures_.add(measure);
      return;
    }
    
    public boolean hasStats() {
      return (!measures_.isEmpty());
    }
    
    public List<NetAlignMeasure> getMeasures() {
      return (measures_);
    }
     
    @Override
    public String toString() {
      StringBuilder ret = new StringBuilder("Measures");
      for (NetAlignMeasure msr : measures_) {
        ret.append('\n').append(msr.name).append(':').append(String.format("%4.4f", msr.val));
      }
      return (ret.toString());
    }
    
    public void replaceValuesTo(NetAlignStats other) {
      measures_ = new ArrayList<NetAlignMeasure>(other.measures_);
    }
    
    public void writeXML(PrintWriter out, Indenter ind) {
      ind.indent();  
      out.println("<NetAlignStats>");
      ind.up();
      for (NetAlignMeasure msr : measures_) {
        msr.writeXML(out, ind);
      }
      ind.down();
      ind.indent();
      out.println("</NetAlignStats>");
      return;
    }
  }
  
  public static class NetAlignMeasure {
    
    public final Double val;
    public final String name;
    
    public NetAlignMeasure(String name, Double val) {
      this.val = val;
      this.name = name;
    }
  
    public void writeXML(PrintWriter out, Indenter ind) {
      ind.indent(); 
      out.print("<NetAlignMeasure name=\"");
      out.print(CharacterEntityMapper.mapEntities(name, false));
      out.print("\" val=\"");
      out.print(val);
      out.println("\"/>");
      return;
    }
 
    @Override
    public String toString() {
      return ("NetAlignMeasure{" + "val=" + val + ", name='" + name + '\'' + '}');
    }
  }

  /***************************************************************************
   **
   ** Class for building network alignments
   */
  
  private class NetworkAlignmentBuilder implements BackgroundWorkerOwner {
    
    private File holdIt_;
    private boolean finished_;
    
    public boolean processNetAlign(ArrayList<FabricLink> mergedLinks, Set<NID.WithName> mergedLoneNodeIDs,
                                   Map<NID.WithName, NID.WithName> mapG1toG2,
                                   Map<NID.WithName, NID.WithName> perfectG1toG2,
                                   Map<NID.WithName, Boolean> mergedToCorrect,
                                   Map<NID.WithName, Boolean> isAlignedNode,
                                   ArrayList<FabricLink> linksG1, HashSet<NID.WithName> lonersG1,
                                   ArrayList<FabricLink> linksG2, HashSet<NID.WithName> lonersG2,
                                   SortedMap<FabricLink.AugRelation, Boolean> relMap,
                                   NetworkAlignmentBuildData.ViewType outType, UniqueLabeller idGen, File holdIt) {
      finished_= true;
      holdIt_ = holdIt;
      try {
        NetworkAlignmentRunner runner = new NetworkAlignmentRunner(mergedLinks, mergedLoneNodeIDs, mapG1toG2, perfectG1toG2,
                mergedToCorrect, isAlignedNode, linksG1, lonersG1, linksG2, lonersG2, relMap, outType, idGen);
        
        BackgroundWorkerClient bwc = new BackgroundWorkerClient(this, runner, topWindow_, bwcm_,
                "fileLoad.waitTitle", "fileLoad.wait", true);
        
        runner.setClient(bwc);
        bwc.launchWorker();
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return (finished_);
    }
    
    public boolean handleRemoteException(Exception remoteEx) {
      finished_ = false;
      return (false);
    }
    
    public void handleCancellation() {
      finished_ = false;
      flf_.cancelAndRestore(holdIt_);
    }
    
    public void cleanUpPreEnable(Object result) {
      return;
    }
    
    public void cleanUpPostRepaint(Object result) {
      return;
    }
  }  
  
  /***************************************************************************
   **
   ** Background network alignment processing
   */
  
  private class NetworkAlignmentRunner extends BackgroundWorker {
    
    private ArrayList<FabricLink> mergedLinks_;
    private Set<NID.WithName> mergedLoneNodeIDs_;
    private Map<NID.WithName, NID.WithName> mapG1toG2_, perfectG1toG2_;
    private Map<NID.WithName, Boolean> mergedToCorrect_, isAlignedNode_;
    private ArrayList<FabricLink> linksG1_, linksG2_;
    private HashSet<NID.WithName> lonersG1_, lonersG2_;
    private SortedMap<FabricLink.AugRelation, Boolean> relMap_;
    private NetworkAlignmentBuildData.ViewType outType_;
    private UniqueLabeller idGen_;
    
    public NetworkAlignmentRunner(ArrayList<FabricLink> mergedLinks, Set<NID.WithName> mergedLoners,
                                  Map<NID.WithName, NID.WithName> mapG1toG2,
                                  Map<NID.WithName, NID.WithName> perfectG1toG2,
                                  Map<NID.WithName, Boolean> mergedToCorrect,
                                  Map<NID.WithName, Boolean> isAlignedNode,
                                  ArrayList<FabricLink> linksG1, HashSet<NID.WithName> lonersG1,
                                  ArrayList<FabricLink> linksG2, HashSet<NID.WithName> lonersG2,
                                  SortedMap<FabricLink.AugRelation, Boolean> relMap,
                                  NetworkAlignmentBuildData.ViewType outType, UniqueLabeller idGen) {
      super(new Boolean(false));
      
      this.mergedLinks_ = mergedLinks;
      this.mergedLoneNodeIDs_ = mergedLoners;
      this.mapG1toG2_ = mapG1toG2;
      this.perfectG1toG2_ = perfectG1toG2;
      this.mergedToCorrect_ = mergedToCorrect;
      this.isAlignedNode_ = isAlignedNode;
      this.linksG1_ = linksG1;
      this.lonersG1_ = lonersG1;
      this.linksG2_ = linksG2;
      this.lonersG2_ = lonersG2;
      this.relMap_ = relMap;
      this.outType_ = outType;
      this.idGen_ = idGen;
    }
    
    public Object runCore() throws AsynchExitRequestException {
      
      NetworkAlignment netAlign = new NetworkAlignment(mergedLinks_, mergedLoneNodeIDs_, mapG1toG2_, perfectG1toG2_,
              linksG1_, lonersG1_, linksG2_, lonersG2_, mergedToCorrect_, isAlignedNode_, outType_, idGen_, this);
      
      netAlign.mergeNetworks();
      BuildExtractor.extractRelations(mergedLinks_, relMap_, this);
      return (new Boolean(true));
    }
    
    public Object postRunCore() {
      return null;
    }
  }
  
  /***************************************************************************
   **
   ** Class for calculating network alignment measures
   */
  
  private class NetAlignMeasureBuilder implements BackgroundWorkerOwner {
    
    private File holdIt_;
    private boolean finished_;
    
    public boolean processNetAlignMeasures(Set<FabricLink> reducedLinks, Set<NID.WithName> loneNodeIDs, Map<NID.WithName, Boolean> isAlignedNode,
                                           Map<NID.WithName, Boolean> mergedToCorrectNC, Set<FabricLink> reducedLinksPerfect,
                                           Set<NID.WithName> loneNodeIDsPerfect, Map<NID.WithName, Boolean> isAlignedNodePerfect,
                                           NetAlignStats report,
                                           ArrayList<FabricLink> linksSmall, HashSet<NID.WithName> lonersSmall,
                                           ArrayList<FabricLink> linksLarge, HashSet<NID.WithName> lonersLarge,
                                           Map<NID.WithName, NID.WithName> mapG1toG2, Map<NID.WithName, NID.WithName> perfectG1toG2, File holdIt) {
      finished_ = true;
      holdIt_ = holdIt;
      try {
        NetAlignMeasureRunner runner = new NetAlignMeasureRunner(reducedLinks, loneNodeIDs, isAlignedNode, mergedToCorrectNC, reducedLinksPerfect,
                loneNodeIDsPerfect, isAlignedNodePerfect, report, linksSmall, lonersSmall, linksLarge, lonersLarge, mapG1toG2, perfectG1toG2);
    
        BackgroundWorkerClient bwc = new BackgroundWorkerClient(this, runner, topWindow_, bwcm_,
                "fileLoad.waitTitle", "fileLoad.wait", true);
    
        runner.setClient(bwc);
        bwc.launchWorker();
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return (finished_);
    }
    
    @Override
    public boolean handleRemoteException(Exception remoteEx) {
      finished_ = false;
      return (false);
    }
  
    @Override
    public void handleCancellation() {
      finished_ = false;
      flf_.cancelAndRestore(holdIt_);
      return;
    }
  
    @Override
    public void cleanUpPreEnable(Object result) {
      return;
    }
  
    @Override
    public void cleanUpPostRepaint(Object result) {
      return;
    }
  }
  
  /***************************************************************************
   **
   ** Background network alignment measure processing
   */
  
  private class NetAlignMeasureRunner extends BackgroundWorker {
  
    private Map<NID.WithName, NID.WithName> mapG1toG2_;
    private Set<FabricLink> reducedLinks_;
    private Set<NID.WithName> loneNodeIDs_;
    private Map<NID.WithName, Boolean> isAlignedNode_;
    private Map<NID.WithName, Boolean> mergedToCorrectNC_;
    private Map<NID.WithName, NID.WithName> perfectG1toG2_;
    private Set<FabricLink> reducedLinksPerfect_;
    private Set<NID.WithName> loneNodeIDsPerfect_;
    private Map<NID.WithName, Boolean> isAlignedNodePerfect_;
  
    private ArrayList<FabricLink> linksSmall_, linksLarge_;
    private HashSet<NID.WithName> lonersSmall_, lonersLarge_;
    private NetAlignStats report_;
    
    
    public NetAlignMeasureRunner(Set<FabricLink> reducedLinks, Set<NID.WithName> loneNodeIDs, Map<NID.WithName, Boolean> isAlignedNode,
                                 Map<NID.WithName, Boolean> mergedToCorrectNC, Set<FabricLink> reducedLinksPerfect,
                                 Set<NID.WithName> loneNodeIDsPerfect, Map<NID.WithName, Boolean> isAlignedNodePerfect,
                                 NetAlignStats report,
                                 ArrayList<FabricLink> linksSmall, HashSet<NID.WithName> lonersSmall,
                                 ArrayList<FabricLink> linksLarge, HashSet<NID.WithName> lonersLarge,
                                 Map<NID.WithName, NID.WithName> mapG1toG2, Map<NID.WithName, NID.WithName> perfectG1toG2) {
      super(new Boolean(false));
      
      this.reducedLinks_ = reducedLinks;
      this.loneNodeIDs_ = loneNodeIDs;
      this.isAlignedNode_ = isAlignedNode;
      this.mergedToCorrectNC_ = mergedToCorrectNC;
      this.reducedLinksPerfect_ = reducedLinksPerfect;
      this.loneNodeIDsPerfect_ = loneNodeIDsPerfect;
      this.isAlignedNodePerfect_ = isAlignedNodePerfect;
      this.report_ = report;
      this.linksSmall_ = linksSmall;
      this.lonersSmall_ = lonersSmall;
      this.linksLarge_ = linksLarge;
      this.lonersLarge_ = lonersLarge;
      this.mapG1toG2_ = mapG1toG2;
      this.perfectG1toG2_ = perfectG1toG2;
    }
  
    @Override
    public Object runCore() throws AsynchExitRequestException {
  
      NetworkAlignmentScorer scorer = new NetworkAlignmentScorer(reducedLinks_, loneNodeIDs_, mergedToCorrectNC_,
              isAlignedNode_, isAlignedNodePerfect_, reducedLinksPerfect_, loneNodeIDsPerfect_,
              linksSmall_, lonersSmall_, linksLarge_, lonersLarge_, mapG1toG2_, perfectG1toG2_, this);
  
      this.report_.replaceValuesTo(scorer.getNetAlignStats());
      
      return (new Boolean(true));
    }
  
    @Override
    public Object postRunCore() {
      return (null);
    }
  }
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class PlugInWorker extends AbstractFactoryClient {
   
    private NetworkAlignmentPlugIn plugin_;
   
    public PlugInWorker(FabricFactory.FactoryWhiteboard board, NetworkAlignmentPlugIn plugin) {
      super(board);
      plugin_ = plugin;
      String name = plugin.getClass().getName();
      myKeys_.add(name);
      installWorker(new NetAlignStatsWorker(board), new NetAlignStatsGlue());
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      FabricFactory.FactoryWhiteboard board = (FabricFactory.FactoryWhiteboard)this.sharedWhiteboard_;
      if (myKeys_.contains(elemName)) {
        board.currPlugIn = plugin_;
        retval = board.currPlugIn;
      }
      return (retval);     
    }  
  }
  
  /***************************************************************************
  **
  ** For XML I/O
  */
  
  public static class NetAlignStatsWorker extends AbstractFactoryClient {
        
    public NetAlignStatsWorker(FabricFactory.FactoryWhiteboard board) {
      super(board);
      myKeys_.add("NetAlignStats");
      installWorker(new NetAlignMeasureWorker(board), null);
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      FabricFactory.FactoryWhiteboard board = (FabricFactory.FactoryWhiteboard) this.sharedWhiteboard_;
      board.currPlugInData = new NetAlignStats();
      retval = board.currPlugInData;
      return (retval);
    }
  }
  
  public static class NetAlignStatsGlue implements GlueStick {    
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, Object optionalArgs) throws IOException {
      FabricFactory.FactoryWhiteboard board = (FabricFactory.FactoryWhiteboard) optionalArgs;
      board.currPlugIn.attachXMLData(board.currPlugInData);
      return null;
    }
  } 
  
  /***************************************************************************
  **
  ** For XML I/O
  */
  
  public static class NetAlignMeasureWorker extends AbstractFactoryClient {
        
    public NetAlignMeasureWorker(FabricFactory.FactoryWhiteboard board) {
      super(board);
      myKeys_.add("NetAlignMeasure");
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      FabricFactory.FactoryWhiteboard board = (FabricFactory.FactoryWhiteboard) this.sharedWhiteboard_;    
      ((NetAlignStats)board.currPlugInData).addAMeasure(buildFromXML(elemName, attrs));
      return (retval);
    }
    
    private NetAlignMeasure buildFromXML(String elemName, Attributes attrs) throws IOException {
      String name = AttributeExtractor.extractAttribute(elemName, attrs, "NetAlignMeasure", "name", true);
      name = CharacterEntityMapper.unmapEntities(name, false);
      String valStr = AttributeExtractor.extractAttribute(elemName, attrs, "NetAlignMeasure", "val", true);
      
      NetAlignMeasure retval;
      try {
        Double value = Double.valueOf(valStr);
        retval = new NetAlignMeasure(name, value);
        System.out.println("NAM " + retval);
      } catch (NumberFormatException nfex) {
        throw new IOException();
      }
      return (retval);
    }  
  }
  
  /***************************************************************************
   **
   ** For transferring objects when assigning Graph1 and Graph2 ONLY
   */
  
  private static final class NetAlignGraphStructure {
    ArrayList<FabricLink> linksSmall;   // G1
    HashSet<NID.WithName> lonersSmall;
    ArrayList<FabricLink> linksLarge;   // G2
    HashSet<NID.WithName> lonersLarge;
  }
  
}