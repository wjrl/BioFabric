/*
**    Copyright (C) 2003-2008 Institute for Systems Biology 
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


/****************************************************************************
**
** Used as a stub for applications not using the gaggle
*/

public class DeadFabricGoose implements FabricGooseInterface {
  
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
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public DeadFabricGoose() {
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Activate the goose - called on AWT thread
  */

  public void activate() {
    return;
  }
  
  /***************************************************************************
  **
  ** Set goose parameters - called on AWT thread
  */

  public void setParameters(BioFabricWindow topWindow, String species) {
    return;
  }
  
  /***************************************************************************
  **
  ** A function to find out if goose is active - called on AWT thread
  */
  
  public boolean isActivated() {
    return (false);
  }
  
  /***************************************************************************
  **
  ** A function to find out if goose is connected - called on AWT thread
  */
  
  public boolean isConnected() {
    return (false);
  }
    
  /***************************************************************************
  **
  ** A function to connect to gaggle - called on AWT thread
  */
    
  public void connect() {
    throw new IllegalStateException();
  }
                        
  /***************************************************************************
  **
  ** Disconnect from gaggle - called on AWT thread
  */ 

  public void disconnect() {
    throw new IllegalStateException();
  }
  
  /***************************************************************************
  **
  ** A function to get selection support - called on AWT thread
  */
  
  public SelectionSupport getSelectionSupport() {
    throw new IllegalStateException();
  }
  
  /***************************************************************************
  **
  ** A function to set the current gaggle target - called on AWT thread
  */
     
  public void setCurrentGaggleTarget(String gooseName) {
    throw new IllegalStateException();
  }
 
  /***************************************************************************
  **
  ** A function to transmit selections to the gaggle boss. - called on AWT thread
  */
  
  public void transmitSelections() {
    throw new IllegalStateException();
  }
  
  /***************************************************************************
  **
  ** A function to transmit a network - called on AWT thread
  */
  
  public void transmitNetwork(SelectionSupport.NetworkForSpecies net) {
    throw new IllegalStateException();
  }
  
  /***************************************************************************
  **
  ** A function to show the current target - called on AWT thread
  */
  
  public void raiseCurrentTarget() {
    throw new IllegalStateException();
  }
  
  /***************************************************************************
  **
  ** A function to hide the current target - called on AWT thread
  */
  
  public void hideCurrentTarget() {
    throw new IllegalStateException();
  }
  
  /***************************************************************************
  **
  ** A function to shut down the gaggle infrastructure - called on AWT thread
  */
  
  public void closeDown() {
    throw new IllegalStateException();
  }
  
  /***************************************************************************
  **
  ** Connect to the boss (Goose req'd - called on AWT thread)
  */
  
  public void connectToGaggle() {
    throw new IllegalStateException();
  }
}
  