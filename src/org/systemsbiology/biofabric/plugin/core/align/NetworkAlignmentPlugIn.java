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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.systemsbiology.biofabric.io.AttributeLoader;
import org.systemsbiology.biofabric.io.FabricFactory;
import org.systemsbiology.biofabric.io.FileLoadFlows;
import org.systemsbiology.biofabric.io.GWImportLoader;
import org.systemsbiology.biofabric.model.BioFabricNetwork;
import org.systemsbiology.biofabric.model.FabricLink;
import org.systemsbiology.biofabric.parser.AbstractFactoryClient;
import org.systemsbiology.biofabric.parser.GlueStick;
import org.systemsbiology.biofabric.plugin.BioFabricToolPlugIn;
import org.systemsbiology.biofabric.plugin.BioFabricToolPlugInCmd;
import org.systemsbiology.biofabric.plugin.BioFabricToolPlugInData;
import org.systemsbiology.biofabric.plugin.PlugInNetworkModelAPI;
import org.systemsbiology.biofabric.ui.FabricDisplayOptions;
import org.systemsbiology.biofabric.ui.FabricDisplayOptionsManager;
import org.systemsbiology.biofabric.ui.dialogs.RelationDirectionDialog;
import org.systemsbiology.biofabric.util.AsynchExitRequestException;
import org.systemsbiology.biofabric.util.AttributeExtractor;
import org.systemsbiology.biofabric.util.BackgroundWorker;
import org.systemsbiology.biofabric.util.BackgroundWorkerClient;
import org.systemsbiology.biofabric.util.BackgroundWorkerOwner;
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
  private NetworkAlignmentScorer.NetAlignStats netAlignStats_;
  private FileLoadFlows flf_;
  private JFrame topWindow_;
  
  
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
    myCmds_.add(new NetAlignScoresCmd()); 
    netAlignStats_ = new NetworkAlignmentScorer.NetAlignStats();
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
  
  public void newNetworkInstalled(PlugInNetworkModelAPI api) {
    flf_ = api.getFileUtilities();
    topWindow_ = api.getTopWindow();
    for (BioFabricToolPlugInCmd cmd : myCmds_) {
      //nalc.setNewNetwork(api);
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
    /*
    ind.up();
    ind.indent();
    
    String name = getClass().getName();
    
    out.print("<");
    out.print(name);
    out.println(">");
    ind.up();
    ind.indent();
    out.print("<netStats nodes=\"");
    out.print(myData_.nodeCount);
    out.print("\" links=\"");
    out.print(myData_.linkCount);
    out.print("\" fullShadowLinks=\"");
    out.print(myData_.fullShadowLinkCount);
    out.println("\" />");
    ind.down();
    ind.indent();
    out.print("</");
    out.print(name);
    out.println(">");
    ind.down();
    */
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
    netAlignStats_ = (NetworkAlignmentScorer.NetAlignStats)data;
    return;   
  }
  
  /***************************************************************************
   **
   ** Create individual networks from two files (.gw or .sif) and one .align file
   */
  
  private boolean networkAlignmentFromSources(NetworkAlignmentDialog.NetworkAlignmentDialogInfo nadi) {
    
    UniqueLabeller idGen = new UniqueLabeller();
    
    //
    // create the individual networks (links + lone nodes)
    //
  
    ArrayList<FabricLink> linksGraphA = new ArrayList<FabricLink>();
    HashSet<NID.WithName> lonersGraphA = new HashSet<NID.WithName>();
    
    if (GWImportLoader.isGWFile(nadi.graphA)) {
      flf_.loadFromGWSource(nadi.graphA, linksGraphA, lonersGraphA, null, idGen, true);
    } else {
      flf_.loadFromSifSource(nadi.graphA, linksGraphA, lonersGraphA, null, idGen, true);
    } // assume it's sif if it's not gw
    
    
    ArrayList<FabricLink> linksGraphB = new ArrayList<FabricLink>();
    HashSet<NID.WithName> lonersGraphB = new HashSet<NID.WithName>();
    
    if (GWImportLoader.isGWFile(nadi.graphB)) {
      flf_.loadFromGWSource(nadi.graphB, linksGraphB, lonersGraphB, null, idGen, true);
    } else {
      flf_.loadFromSifSource(nadi.graphB, linksGraphB, lonersGraphB, null, idGen, true);
    }
    
    return (networkAlignmentStepTwo(nadi, linksGraphA, lonersGraphA, linksGraphB, lonersGraphB, idGen));
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
                                          UniqueLabeller idGen) {
    //
    // Decide which graph has more nodes - graph 1 is smaller (#nodes) than graph 2 from here on
    //
  
    // small graph
    ArrayList<FabricLink> linksSmall = new ArrayList<FabricLink>();
    HashSet<NID.WithName> lonersSmall = new HashSet<NID.WithName>();
  
    // large graph
    ArrayList<FabricLink> linksLarge = new ArrayList<FabricLink>();
    HashSet<NID.WithName> lonersLarge = new HashSet<NID.WithName>();
  
    try {
      int numNodesA = BioFabricNetwork.extractNodes(linksGraphA, loneNodeIDsGraphA, null).size();
      int numNodesB = BioFabricNetwork.extractNodes(linksGraphB, loneNodeIDsGraphB, null).size();
      
      if (numNodesA > numNodesB) { // We compare #nodes
        linksLarge = linksGraphA;
        lonersLarge = loneNodeIDsGraphA;
        linksSmall = linksGraphB;
        lonersSmall = loneNodeIDsGraphB;
      } else if (numNodesA < numNodesB) {
        linksLarge = linksGraphB;
        lonersLarge = loneNodeIDsGraphB;
        linksSmall = linksGraphA;
        lonersSmall = loneNodeIDsGraphA;
      } else { // now we compare #links

        UiUtil.fixMePrintout("figure out G1/G2 from .align if networks have same # of nodes");
        int numLinksA = linksGraphA.size();
        int numLinksB = linksGraphB.size();

        if (numLinksA >= numLinksB) { // if #links are still equal, we do choose graphA as larger
          linksLarge = linksGraphA;   // We don't take the alignment file into consideration. . .
          lonersLarge = loneNodeIDsGraphA;
          linksSmall = linksGraphB;
          lonersSmall = loneNodeIDsGraphB;
        } else {
          linksLarge = linksGraphB;
          lonersLarge = loneNodeIDsGraphB;
          linksSmall = linksGraphA;
          lonersSmall = loneNodeIDsGraphA;
        }
      }
    } catch (AsynchExitRequestException aere) {
      // should never happen
      return (false);
    }
  
    //
    // read alignment and process
    //
  
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
    Map<NID.WithName, Boolean> mergedToCorrect = null, isAlignedNode = new HashMap<NID.WithName, Boolean>();
    if (perfectG1toG2 != null) {
      mergedToCorrect = new HashMap<NID.WithName, Boolean>();
    }
  
    boolean finished = nab.processNetAlign(mergedLinks, mergedLoneNodeIDs, mapG1toG2, perfectG1toG2, mergedToCorrect,
            isAlignedNode, linksSmall, lonersSmall, linksLarge, lonersLarge, relMap, nadi.forOrphanEdge, idGen, holdIt);
  
    //
    // Second process the perfect alignment (if given)
    //
    
    nab = null;
    ArrayList<FabricLink> mergedLinksPerfect = null;
    Set<NID.WithName> mergedLoneNodeIDsPerfect = null;
    SortedMap<FabricLink.AugRelation, Boolean> relMapPerfect = null;
    Set<FabricLink> reducedLinksPerfect = null;
    Map<NID.WithName, Boolean> isAlignedNodePerfect = null;
    
    if (finished && perfectG1toG2 != null) {
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
              isAlignedNodePerfect, linksSmall, lonersSmall, linksLarge, lonersLarge, relMapPerfect, false, idGen, holdIt);
    }
  
    if (finished) { // for main alignment
      finished = networkAlignmentStepThree(mergedLinks, reducedLinks, mergedLoneNodeIDs, relMap, idGen, holdIt);
    }
    
    if (finished && perfectG1toG2 != null) { // for perfect alignment
      finished = networkAlignmentStepThree(mergedLinksPerfect, reducedLinksPerfect, mergedLoneNodeIDsPerfect, relMapPerfect, idGen, holdIt);
    }
  
    NetworkAlignmentScorer.NetAlignStats netAlignStats = new NetworkAlignmentScorer.NetAlignStats();
    if (finished) { // Score Report
      finished = networkAlignmentStepFour(reducedLinks, mergedLoneNodeIDs, isAlignedNode, mergedToCorrect,
              reducedLinksPerfect, mergedLoneNodeIDsPerfect, isAlignedNodePerfect, netAlignStats,
              linksSmall, lonersSmall, linksLarge, lonersLarge, mapG1toG2, perfectG1toG2);
    }
    
    if (finished) { // Load the alignments
      networkAlignmentStepFive(reducedLinks, mergedLoneNodeIDs, mergedToCorrect, isAlignedNode, netAlignStats,
              nadi.forOrphanEdge, idGen, nadi.align, holdIt);
    }
    return (true);
  }
  
  /***************************************************************************
   **
   ** Directivity and Relations
   */
  
  private boolean networkAlignmentStepThree(List<FabricLink> links, Set<FabricLink> reducedLinks,
                                            Set<NID.WithName> loneNodeIDs,
                                            SortedMap<FabricLink.AugRelation, Boolean> relMap,
                                            UniqueLabeller idGen, File holdIt) {
  
    try {
      ResourceManager rMan = ResourceManager.getManager();
  
      RelationDirectionDialog rdd = new RelationDirectionDialog(topWindow_, relMap);
      rdd.setVisible(true);
      if (!rdd.haveResult()) {
        return (false);
      }
      if (rdd.getFromFile()) {
        File fileEda = flf_.getTheFile(".rda", ".txt", "AttribDirectory", "filterName.rda");
        if (fileEda == null) {
          return (true);
        }
        Map<AttributeLoader.AttributeKey, String> relAttributes = flf_.loadTheFile(fileEda, null, true);
        // Use the simple a = b format of node attributes
        if (relAttributes == null) {
          return (true);
        }
    
        HashSet<FabricLink.AugRelation> needed = new HashSet<FabricLink.AugRelation>(relMap.keySet());
    
        boolean tooMany = false;
        Iterator<AttributeLoader.AttributeKey> rit = relAttributes.keySet().iterator();
        while (rit.hasNext()) {
          AttributeLoader.StringKey sKey = (AttributeLoader.StringKey) rit.next();
          String key = sKey.key;
          String val = relAttributes.get(sKey);
          Boolean dirVal = Boolean.valueOf(val);
          FabricLink.AugRelation forNorm = new FabricLink.AugRelation(key, false);
          FabricLink.AugRelation forShad = new FabricLink.AugRelation(key, true);
          boolean matched = false;
          if (needed.contains(forNorm)) {
            matched = true;
            relMap.put(forNorm, dirVal);
            needed.remove(forNorm);
          }
          if (needed.contains(forShad)) {
            matched = true;
            relMap.put(forShad, dirVal);
            needed.remove(forShad);
          }
          if (! matched) {
            tooMany = true;
            break;
          }
        }
        if (!needed.isEmpty() || tooMany) {
          JOptionPane.showMessageDialog(topWindow_, rMan.getString("fabricRead.directionMapLoadFailure"),
                  rMan.getString("fabricRead.directionMapLoadFailureTitle"),
                  JOptionPane.ERROR_MESSAGE);
          return (false);
        }
      } else {
        relMap = rdd.getRelationMap();
      }
  
      HashSet<FabricLink> culledLinks = new HashSet<FabricLink>();
      FileLoadFlows.PreprocessNetwork pn = new FileLoadFlows.PreprocessNetwork();
      boolean didFinish = pn.doNetworkPreprocess(links, relMap, reducedLinks, culledLinks, holdIt);
      if (!didFinish) {
        return (false);
      }
  
      if (! culledLinks.isEmpty()) {
        String dupLinkFormat = rMan.getString("fabricRead.dupLinkFormat");
        // Ignore shadow link culls: / 2
        String dupLinkMsg = MessageFormat.format(dupLinkFormat, new Object[]{Integer.valueOf(culledLinks.size() / 2)});
        JOptionPane.showMessageDialog(topWindow_, dupLinkMsg,
                rMan.getString("fabricRead.dupLinkTitle"),
                JOptionPane.WARNING_MESSAGE);
      }
  
      //
      // For big files, user may want to specify layout options before the default layout with no
      // shadows. Let them set this here:
      //
  
      if (reducedLinks.size() > FileLoadFlows.SIZE_TO_ASK_ABOUT_SHADOWS) {
        String shadowMessage = rMan.getString("fabricRead.askAboutShadows");
        int doShadow =
                JOptionPane.showConfirmDialog(topWindow_, shadowMessage,
                        rMan.getString("fabricRead.askAboutShadowsTitle"),
                        JOptionPane.YES_NO_CANCEL_OPTION);
        if (doShadow == JOptionPane.CANCEL_OPTION) {
          return (false);
        }
        FabricDisplayOptions dops = FabricDisplayOptionsManager.getMgr().getDisplayOptions();
        dops.setDisplayShadows((doShadow == JOptionPane.YES_OPTION));
      }
    } catch (OutOfMemoryError oom) {
      ExceptionHandler.getHandler().displayOutOfMemory(oom);
      return (false);
    }
    return (true);
  }
  
  /***************************************************************************
   **
   ** Process NetAlign Score Reports
   */
  
  private boolean networkAlignmentStepFour(Set<FabricLink> reducedLinks, Set<NID.WithName> loneNodeIDs, Map<NID.WithName, Boolean> isAlignedNode,
                                           Map<NID.WithName, Boolean> mergedToCorrect, Set<FabricLink> reducedLinksPerfect,
                                           Set<NID.WithName> loneNodeIDsPerfect, Map<NID.WithName, Boolean> isAlignedNodePerfect,
                                           NetworkAlignmentScorer.NetAlignStats report,
                                           ArrayList<FabricLink> linksSmall, HashSet<NID.WithName> lonersSmall,
                                           ArrayList<FabricLink> linksLarge, HashSet<NID.WithName> lonersLarge,
                                           Map<NID.WithName, NID.WithName> mapG1toG2, Map<NID.WithName, NID.WithName> perfectG1toG2) {
  
    NetworkAlignmentScorer scorer = new NetworkAlignmentScorer(reducedLinks, loneNodeIDs, mergedToCorrect,
            isAlignedNode, isAlignedNodePerfect, reducedLinksPerfect, loneNodeIDsPerfect,
            linksSmall, lonersSmall, linksLarge, lonersLarge, mapG1toG2, perfectG1toG2, null);

    report.replaceValuesTo(scorer.getNetAlignStats());
    return (true);
  }
  
  /***************************************************************************
   **
   ** Build the network alignment
   */
  
  private boolean networkAlignmentStepFive(Set<FabricLink> reducedLinks, Set<NID.WithName> loneNodeIDs,
                                           Map<NID.WithName, Boolean> mergedToCorrect, Map<NID.WithName, Boolean> isAlignedNode,
                                           NetworkAlignmentScorer.NetAlignStats report,
                                           boolean forOrphanEdge, UniqueLabeller idGen, File align, File holdIt) {
    try {
      NetworkBuilder nb = new FileLoadFlows.NetworkBuilder(true, holdIt);
      nb.setForNetAlignBuild(idGen, reducedLinks, loneNodeIDs, mergedToCorrect, isAlignedNode, report, forOrphanEdge,
                             BuildData.BuildMode.BUILD_NETWORK_ALIGNMENT);
      nb.doNetworkBuild();
    } catch (OutOfMemoryError oom) {
      ExceptionHandler.getHandler().displayOutOfMemory(oom);
      return (false);
    }
    currentFile_ = null;
    FabricCommands.setPreference("LoadDirectory", align.getAbsoluteFile().getParent());
    manageWindowTitle(align.getName());
    return true;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
   **
   ** Command
   */
  
  private class NetAlignScoresCmd implements BioFabricToolPlugInCmd {
   
    private boolean enabled_;

    public String getCommandName() {
      ResourceManager rMan = ResourceManager.getManager();  // DOES NOT BELONG HERE
      return (rMan.getString("command.netAlignScores"));  
    }   
    
    public boolean performOperation(JFrame topFrame) {
      if (!enabled_) {
        return (false);
      }    
    
      // just temporary until I install PluginData stuff
      NetAlignScoreDialog scoreDialog = new NetAlignScoreDialog(topFrame, netAlignStats_);
      scoreDialog.setVisible(true);
      return (true);
    }
  
    public boolean isEnabled() {
      return (enabled_);    
    } 
  }
 
  /***************************************************************************
  **
  ** Command
  */
  
  private class LoadNetAlignGroupsCmd implements BioFabricToolPlugInCmd {
  
    private boolean enabled_;

    public String getCommandName() {
      ResourceManager rMan = ResourceManager.getManager();  // DOES NOT BELONG HERE
      return (rMan.getString("command.netAlignGroupLayout"));  
    }
    
    public boolean performOperation(JFrame topFrame) {
      if (!enabled_) {
        return (false);
      }    
    
      NetworkAlignmentDialog nad = new NetworkAlignmentDialog(topFrame, false);
      nad.setVisible(true);
      
      if(!nad.haveResult()) {
        return (false);
      }
  
      NetworkAlignmentDialog.NetworkAlignmentDialogInfo nai = nad.getNAInfo();
      
      boolean filesNotOkay =
              !standardFileChecks(nai.graphA, FILE_MUST_EXIST, FILE_CAN_CREATE_DONT_CARE,
                      FILE_DONT_CHECK_OVERWRITE, FILE_MUST_BE_FILE,
                      FILE_CAN_WRITE_DONT_CARE, FILE_CAN_READ) ||
              !standardFileChecks(nai.graphB, FILE_MUST_EXIST, FILE_CAN_CREATE_DONT_CARE,
                      FILE_DONT_CHECK_OVERWRITE, FILE_MUST_BE_FILE,
                      FILE_CAN_WRITE_DONT_CARE, FILE_CAN_READ) ||
              !standardFileChecks(nai.align, FILE_MUST_EXIST, FILE_CAN_CREATE_DONT_CARE,
                      FILE_DONT_CHECK_OVERWRITE, FILE_MUST_BE_FILE,
                      FILE_CAN_WRITE_DONT_CARE, FILE_CAN_READ);
      if (nai.perfect != null) {
        filesNotOkay = !standardFileChecks(nai.perfect, FILE_MUST_EXIST, FILE_CAN_CREATE_DONT_CARE,
                      FILE_DONT_CHECK_OVERWRITE, FILE_MUST_BE_FILE,
                      FILE_CAN_WRITE_DONT_CARE, FILE_CAN_READ);
      }
      
      if (filesNotOkay) {
        return (false);
      }
      return (networkAlignmentFromSources(nai));
    }
    
    public boolean isEnabled() {
      return (enabled_);    
    }
  
  }
  
  /***************************************************************************
   **
   ** Command
   */
  
  private class LoadNetAlignOrphanCmd implements BioFabricToolPlugInCmd {
 
    private boolean enabled_;
    
    public String getCommandName() {
      ResourceManager rMan = ResourceManager.getManager();  // DOES NOT BELONG HERE
      return (rMan.getString("command.orphanLayout"));  
    }
    
    public boolean performOperation(JFrame topFrame) {
      if (!enabled_) {
        return (false);
      }
      
      NetworkAlignmentDialog nad = new NetworkAlignmentDialog(topFrame, true);
      nad.setVisible(true);
      
      if(!nad.haveResult()) {
        return (false);
      }
      
      NetworkAlignmentDialog.NetworkAlignmentDialogInfo nai = nad.getNAInfo();
      
      boolean filesNotOkay =
              !standardFileChecks(nai.graphA, FILE_MUST_EXIST, FILE_CAN_CREATE_DONT_CARE,
                      FILE_DONT_CHECK_OVERWRITE, FILE_MUST_BE_FILE,
                      FILE_CAN_WRITE_DONT_CARE, FILE_CAN_READ) ||
              !standardFileChecks(nai.graphB, FILE_MUST_EXIST, FILE_CAN_CREATE_DONT_CARE,
                              FILE_DONT_CHECK_OVERWRITE, FILE_MUST_BE_FILE,
                              FILE_CAN_WRITE_DONT_CARE, FILE_CAN_READ) ||
              !standardFileChecks(nai.align, FILE_MUST_EXIST, FILE_CAN_CREATE_DONT_CARE,
                              FILE_DONT_CHECK_OVERWRITE, FILE_MUST_BE_FILE,
                              FILE_CAN_WRITE_DONT_CARE, FILE_CAN_READ);
      
      if (filesNotOkay) {
        return (false);
      }
      return (networkAlignmentFromSources(nai));
    }
    
    public boolean isEnabled() {
      return (enabled_);    
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
                                   boolean forClique, UniqueLabeller idGen, File holdIt) {
      finished_= true;
      holdIt_ = holdIt;
      try {
        NetworkAlignmentRunner runner = new NetworkAlignmentRunner(mergedLinks, mergedLoneNodeIDs, mapG1toG2, perfectG1toG2,
                mergedToCorrect, isAlignedNode, linksG1, lonersG1, linksG2, lonersG2, relMap, forClique, idGen);
        
        BackgroundWorkerClient bwc = new BackgroundWorkerClient(this, runner, topWindow_, bfw_,
                "fileLoad.waitTitle", "fileLoad.wait", true);
        
        runner.setClient(bwc);
        bwc.launchWorker();
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return (finished_);
    }
    
    // NOT SURE IF ALL OF THE METHODS BELOW ARE CORRECT
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
    private boolean forOrphanEdge_;
    private UniqueLabeller idGen_;
    
    public NetworkAlignmentRunner(ArrayList<FabricLink> mergedLinks, Set<NID.WithName> mergedLoners,
                                  Map<NID.WithName, NID.WithName> mapG1toG2,
                                  Map<NID.WithName, NID.WithName> perfectG1toG2,
                                  Map<NID.WithName, Boolean> mergedToCorrect,
                                  Map<NID.WithName, Boolean> isAlignedNode,
                                  ArrayList<FabricLink> linksG1, HashSet<NID.WithName> lonersG1,
                                  ArrayList<FabricLink> linksG2, HashSet<NID.WithName> lonersG2,
                                  SortedMap<FabricLink.AugRelation, Boolean> relMap,
                                  boolean forClique, UniqueLabeller idGen) {
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
      this.forOrphanEdge_ = forClique;
      this.idGen_ = idGen;
    }
    
    public Object runCore() throws AsynchExitRequestException {
      
      NetworkAlignment netAlign = new NetworkAlignment(mergedLinks_, mergedLoneNodeIDs_, mapG1toG2_, perfectG1toG2_,
              linksG1_, lonersG1_, linksG2_, lonersG2_, mergedToCorrect_, isAlignedNode_, forOrphanEdge_, idGen_, this);
      
      netAlign.mergeNetworks();
      BioFabricNetwork.extractRelations(mergedLinks_, relMap_, this);
      return (new Boolean(true));
    }
    
    public Object postRunCore() {
      return null;
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
      installWorker(new NetStatsWorker(board), new StatsGlue());
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
  
  public static class NetStatsWorker extends AbstractFactoryClient {
        
    public NetStatsWorker(FabricFactory.FactoryWhiteboard board) {
      super(board);
      myKeys_.add("netStats");
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      FabricFactory.FactoryWhiteboard board = (FabricFactory.FactoryWhiteboard) this.sharedWhiteboard_;
      board.currPlugInData = buildFromXML(elemName, attrs);
      retval = board.currPlugInData;
      return (retval);
    }
    
    private StatData buildFromXML(String elemName, Attributes attrs) throws IOException {
      String nodeStr = AttributeExtractor.extractAttribute(elemName, attrs, "netStats", "nodes", true);
      String linkStr = AttributeExtractor.extractAttribute(elemName, attrs, "netStats", "links", true);
      String shadStr = AttributeExtractor.extractAttribute(elemName, attrs, "netStats", "fullShadowLinks", true);
  
      StatData retval;
      try {
        int nodeCount = Integer.valueOf(nodeStr).intValue();
        int linkCount = Integer.valueOf(linkStr).intValue();
        int fullCount = Integer.valueOf(shadStr).intValue();
        retval = new StatData(nodeCount, linkCount, fullCount);
      } catch (NumberFormatException nfex) {
        throw new IOException();
      }
      return (retval);
    }  
  }
  
  public static class StatsGlue implements GlueStick {    
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, Object optionalArgs) throws IOException {
      FabricFactory.FactoryWhiteboard board = (FabricFactory.FactoryWhiteboard) optionalArgs;
      board.currPlugIn.attachXMLData(board.currPlugInData);
      return null;
    }
  }  
  
}