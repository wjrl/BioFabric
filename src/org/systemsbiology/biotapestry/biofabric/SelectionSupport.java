/*
**    Copyright (C) 2003-2011 Institute for Systems Biology 
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

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import javax.swing.SwingUtilities;


/****************************************************************************
**
** Used for gaggle selection support
*/

public class SelectionSupport {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private Set pendingSelections_;
  private List inboundNetwork_;
  private NetworkForSpecies outboundNetwork_;
  private String species_;
  private BioFabricWindow bfw_;
  private ArrayList commands_;
  private ArrayList gooseList_;
  private boolean initGooseList_;
  
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

  public SelectionSupport(BioFabricWindow bfw, String species) {
    bfw_ = bfw;
    species_ = species;
    commands_ = new ArrayList();
    gooseList_ = new ArrayList();    
    pendingSelections_ = new HashSet();
    initGooseList_ = true;
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  
  /***************************************************************************
  **
  ** Set the species
  */
  
  public synchronized void setSpecies(String species) {
    species_ = species;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the species
  */
  
  public synchronized String getSpecies() {
    return (species_);
  }  
 
  /***************************************************************************
  **
  ** Get the pending command list
  */
  
  public synchronized List getPendingCommands() {
    ArrayList pending = commands_;
    commands_ = new ArrayList();
    return (pending);
  }   

  /***************************************************************************
  **
  ** Let us know what the current goose list is
  */
  
  public synchronized void registerNewGooseList(List gooseList) {
    gooseList_.clear();
    gooseList_.addAll(gooseList);
    if (initGooseList_) {
      initGooseList_ = false;
    } else {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {    
          bfw_.haveGaggleGooseChange();
        }
      });
    }    
    return;
  }
  
  /***************************************************************************
  **
  ** Get the current goose list
  */
  
  public synchronized List getGooseList() {
    return (new ArrayList(gooseList_));
  }  
  
  /***************************************************************************
  **
  ** Create a new network
  */
  
  public synchronized void createNewNetwork(String species, List links, List loneNodes) {
    InboundNetworkOp op = new InboundNetworkOp(species, links, loneNodes);
    commands_.add(op);
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {    
        bfw_.haveInboundGaggleCommands();
      }
    });
    
    return;
  } 
  
  /***************************************************************************
  **
  ** Register an unsupported command
  */
  
  public synchronized void addUnsupportedCommand() {
    InboundGaggleOp op = new InboundGaggleOp(bfw_);
    commands_.add(op);
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {    
        bfw_.haveInboundGaggleCommands();
      }
    });    
    return;
  }   
  
  /***************************************************************************
  **
  ** Register a selection command
  */
  
  public synchronized void addSelectionCommand(String species, String[] selections) {
    HashSet newSelections = new HashSet(Arrays.asList(selections));
    SelectionSupport.SelectionsForSpecies sfs = new SelectionsForSpecies(species, newSelections);
    InboundSelectionOp op = new InboundSelectionOp(bfw_, sfs);
    commands_.add(op);
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {    
        bfw_.haveInboundGaggleCommands();
      }
    });    
    return;
  }     
  
  /***************************************************************************
  **
  ** Register a selection command
  */
  
  public synchronized void changeConnectionStatus(boolean connected) {
    InboundConnectionOp op = new InboundConnectionOp(bfw_, connected);
    commands_.add(op);
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {    
        bfw_.haveInboundGaggleCommands();
      }
    });    
    return;
  }     

  /***************************************************************************
  **
  ** Clear the selections
  */
  
  public synchronized void clearSelections() { 
    SelectionSupport.SelectionsForSpecies sfs = new SelectionsForSpecies(species_, new HashSet());
    InboundSelectionOp op = new InboundSelectionOp(bfw_, sfs);
    commands_.add(op);
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {    
        bfw_.haveInboundGaggleCommands();
      }
    });
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the selections
  */
  
  public synchronized SelectionsForSpecies getSelections() {
    return (new SelectionsForSpecies(species_, new HashSet(pendingSelections_)));
  }
  
  /***************************************************************************
  **
  ** Set outbound network
  */
  
  public synchronized void setOutboundNetwork(NetworkForSpecies network) {
    outboundNetwork_ = (network == null) ? null : (NetworkForSpecies)network.clone();
    if (outboundNetwork_ != null) {
      outboundNetwork_.setSpecies(species_);
    }
    return;
  }    

  /***************************************************************************
  **
  ** Get the network
  */
  
  public synchronized NetworkForSpecies getOutboundNetwork() {
    return ((outboundNetwork_ == null) ? null : (NetworkForSpecies)outboundNetwork_.clone());
  }
  
  /***************************************************************************
  **
  ** Set the selections
  */
  
  public synchronized void setSelections(List selections) { 
    pendingSelections_.clear();
    pendingSelections_.addAll(selections);
    return;
  }
  
  /***************************************************************************
  **
  ** Get the selection count
  */
  
  public synchronized int getSelectionCount() {
    return (pendingSelections_.size());
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** returns info all at once for concurrency protection
  */  
  
  public static class SelectionsForSpecies {
    public String species;
    public Set selections;
    
    SelectionsForSpecies(String species, Set selections) {
      this.species = species;
      this.selections = selections;
    }
  }
  
  /***************************************************************************
  **
  ** Returns a defined network
  */  
  
  public static class NetworkForSpecies implements Cloneable {
    private String species_;
    private List links_;
    private List singletons_;
    
    private NetworkForSpecies(String species, List reqLinks, List singletons) {
      species_ = species;
      links_ = reqLinks;
      singletons_ = singletons;
    }
    
    public NetworkForSpecies() {
      this(null, new ArrayList(), new ArrayList());
    }      
        
    public NetworkForSpecies(List reqLinks, List singletons) {
      this(null, reqLinks, singletons);
    }
    
    public Object clone() {
      try {
        NetworkForSpecies retval = (NetworkForSpecies)super.clone();
        retval.links_ = new ArrayList();
        int numLinks = this.links_.size();
        for (int i = 0; i < numLinks; i++) {
          retval.links_.add(((FabricLink)this.links_.get(i)).clone());
        } 
        retval.singletons_ = new ArrayList(singletons_);
        return (retval);
      } catch (CloneNotSupportedException cnse) {
        throw new IllegalStateException();
      }
      
    }   
    
    void setSpecies(String species) { 
      species_ = species;
      return;
    }   
    
    public String getSpecies() { 
      return (species_);
    }  
    
    public List getLinks() { 
      return (links_);
    }
    
    public List getSingletons() { 
      return (singletons_);
    }    
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
}
