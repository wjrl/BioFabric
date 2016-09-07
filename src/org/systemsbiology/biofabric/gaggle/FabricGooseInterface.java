/*
**    Copyright (C) 2003-2007 Institute for Systems Biology 
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

package org.systemsbiology.biofabric.gaggle;

import org.systemsbiology.biofabric.app.BioFabricWindow;
import org.systemsbiology.biofabric.gaggle.SelectionSupport.NetworkForSpecies;


/****************************************************************************
**
** Defines goose application interface
*/

public interface FabricGooseInterface {
  
  public static final String BOSS_NAME = "boss";  
  
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Activate the goose - called on AWT thread
  */

  public void activate();
  
  /***************************************************************************
  **
  ** Set goose parameters - called on AWT thread
  */

  public void setParameters(BioFabricWindow topWindow, String species);
  
  /***************************************************************************
  **
  ** A function to find out if goose is active - called on AWT thread
  */
  
  public boolean isActivated();
  
  
  /***************************************************************************
  **
  ** A function to find out if goose is connected - called on AWT thread
  */
  
  public boolean isConnected();
    
  /***************************************************************************
  **
  ** A function to connect to gaggle - called on AWT thread
  */
    
  public void connect(); 
                        
  /***************************************************************************
  **
  ** Disconnect from gaggle - called on AWT thread
  */ 

  public void disconnect();
  
  /***************************************************************************
  **
  ** A function to get selection support - called on AWT thread
  */
  
  public SelectionSupport getSelectionSupport();
  
  /***************************************************************************
  **
  ** A function to set the current gaggle target - called on AWT thread
  */
     
  public void setCurrentGaggleTarget(String gooseName);  
  
  /***************************************************************************
  **
  ** A function to transmit selections to the gaggle boss. - called on AWT thread
  */
  
  public void transmitSelections();
  
  /***************************************************************************
  **
  ** A function to transmit a network - called on AWT thread
  */
  
  public void transmitNetwork(SelectionSupport.NetworkForSpecies net); 
  
  /***************************************************************************
  **
  ** A function to show the current target - called on AWT thread
  */
  
  public void raiseCurrentTarget();
  
  /***************************************************************************
  **
  ** A function to hide the current target - called on AWT thread
  */
  
  public void hideCurrentTarget();
  
  /***************************************************************************
  **
  ** A function to shut down the gaggle infrastructure - called on AWT thread
  */
  
  public void closeDown();
  
  /***************************************************************************
  **
  ** Connect to the boss (Goose req'd - called on AWT thread)
  */
  
  public void connectToGaggle();
  
}
