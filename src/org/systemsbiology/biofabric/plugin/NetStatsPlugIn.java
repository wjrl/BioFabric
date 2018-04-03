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

package org.systemsbiology.biofabric.plugin;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.systemsbiology.biofabric.io.FabricFactory;
import org.systemsbiology.biofabric.parser.AbstractFactoryClient;
import org.systemsbiology.biofabric.util.AttributeExtractor;
import org.systemsbiology.biofabric.util.Indenter;
import org.systemsbiology.biofabric.util.ResourceManager;
import org.systemsbiology.biofabric.util.UiUtil;
import org.xml.sax.Attributes;

/****************************************************************************
**
** Class for the simple network statistics tool
*/

public class NetStatsPlugIn implements BioFabricToolPlugIn {
  
  private ArrayList<BioFabricToolPlugInCmd> myCmds_;
  private String myTag_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Null constructor (required)
  */
  
  public NetStatsPlugIn() {
    myCmds_ = new ArrayList<BioFabricToolPlugInCmd>();
    myCmds_.add(new NodeAndLinkCounterCmd());
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
    for (BioFabricToolPlugInCmd cmd : myCmds_) {
      NodeAndLinkCounterCmd nalc = (NodeAndLinkCounterCmd)cmd;
      nalc.setNewNetwork(api);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Get name of tool menu to display
  */
  
  public String getToolMenu() {
    ResourceManager rMan = ResourceManager.getManager();  // DOES NOT BELONG HERE
    return (rMan.getString("command.statsCommands"));
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
    ind.up();
    ind.indent();
    NodeAndLinkCounterCmd nlcc = (NodeAndLinkCounterCmd)myCmds_.get(0);
    out.print("<plugInData tag=\"");
    out.print(myTag_);
    out.println("\" >");
    ind.up();
    ind.indent();
    out.print("<netStats nodes=\"");
    out.print(nlcc.nodeCount);
    out.print("\" links=\"");
    out.print(nlcc.linkCount);
    out.print("\" fullShadowlinks=\"");
    out.print(nlcc.fullShadowLinkCount);
    out.println("\" />");
    ind.down();
    ind.indent();
    out.println("</plugInData>");
    ind.down();
    return;
  }  

  /***************************************************************************
  **
  ** Read session data
  */
  
  public void readPluginData() {
    
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  public static class NodeAndLinkCounterCmd implements BioFabricToolPlugInCmd {    
    int nodeCount;
    int linkCount;
    int fullShadowLinkCount;
    private boolean enabled_;
    
    /***************************************************************************
    **
    ** Set new network. In this case, process the network too
    */
    
    public void setNewNetwork(PlugInNetworkModelAPI api) {
      if (api != null) {
        nodeCount = api.getNodeCount();
        linkCount = api.getLinkCount(false);
        fullShadowLinkCount = api.getLinkCount(true);
        enabled_ = true;
      } else {
        nodeCount = 0;
        linkCount = 0;
        fullShadowLinkCount = 0;
        enabled_ = false;      
      }
      return;
    }

    /***************************************************************************
    **
    ** Get the name
    */
    
    public String getCommandName() {
      ResourceManager rMan = ResourceManager.getManager();  // DOES NOT BELONG HERE
      return (rMan.getString("command.statsCommands"));  
    }

    /***************************************************************************
    **
    ** Perform the operation
    */
    
    public boolean performOperation(JFrame topFrame) {
      if (!enabled_) {
        return (false);
      }
      
      ResourceManager rMan = ResourceManager.getManager();  // DOES NOT BELONG HERE   
      String desc = MessageFormat.format(rMan.getString("modelCounts.message"), 
                                         new Object[] {new Integer(nodeCount), 
                                                       new Integer(linkCount), 
                                                       new Integer(fullShadowLinkCount)});  
      desc = UiUtil.convertMessageToHtml(desc);
      JOptionPane.showMessageDialog(topFrame, desc,
                                    rMan.getString("modelCounts.modelCountTitle"),
                                    JOptionPane.INFORMATION_MESSAGE);        
      return (true);
    }
  
    /***************************************************************************
    **
    ** Answer if command is enabled
    */
    
    public boolean isEnabled() {
      return (enabled_);    
    }
  }
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class PlugInWorker extends AbstractFactoryClient {
    
    public PlugInWorker(FabricFactory.FactoryWhiteboard board) {
      super(board);
      myKeys_.add(my class name or something);
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