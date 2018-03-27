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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.systemsbiology.biofabric.util.ResourceManager;
import org.systemsbiology.biofabric.util.UiUtil;

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
    myCmds_ = new  ArrayList<BioFabricToolPlugInCmd>();
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
  
  public void writePluginData(String keyword, BioFabricToolPlugInData data) {
    
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
    private int nodeCount_;
    private int linkCount_;
    private int fullShadowLinkCount_;
    private boolean enabled_;
    
    /***************************************************************************
    **
    ** Set new network. In this case, process the network too
    */
    
    public void setNewNetwork(PlugInNetworkModelAPI api) {
      if (api != null) {
        nodeCount_ = api.getNodeCount();
        linkCount_ = api.getLinkCount(false);
        fullShadowLinkCount_ = api.getLinkCount(true);
        enabled_ = true;
      } else {
        nodeCount_ = 0;
        linkCount_ = 0;
        fullShadowLinkCount_ = 0;
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
                                         new Object[] {new Integer(nodeCount_), 
                                                       new Integer(linkCount_), 
                                                       new Integer(fullShadowLinkCount_)});  
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
}