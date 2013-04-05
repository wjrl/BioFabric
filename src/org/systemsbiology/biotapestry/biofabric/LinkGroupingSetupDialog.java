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

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.border.EmptyBorder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.ui.dialogs.utils.DialogSupport;
import org.systemsbiology.biotapestry.ui.dialogs.utils.EditableTable;

/****************************************************************************
**
** Dialog box for specifying link groupings
*/

public class LinkGroupingSetupDialog extends JDialog implements DialogSupport.DialogSupportClient {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private boolean haveResult_;
  private List linkGroupResult_;
  private EditableTable est_;
  private JFrame parent_;
  private Set allRelations_;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 

  public LinkGroupingSetupDialog(JFrame parent, List currentTags, Set allRelations) {     
    super(parent, ResourceManager.getManager().getString("linkGroupEdit.title"), true);
    parent_ = parent;
    allRelations_ = allRelations;
        
    setSize(650, 300);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();

    est_ = new EditableTable(new LinkGroupingTableModel(), parent_);
    EditableTable.TableParams etp = new EditableTable.TableParams();
    etp.addAlwaysAtEnd = false;
    etp.buttons = EditableTable.ALL_BUT_EDIT_BUTTONS;
    etp.singleSelectOnly = true;
    JPanel tablePan = est_.buildEditableTable(etp);
                                        
    UiUtil.gbcSet(gbc, 0, 0, 10, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    cp.add(tablePan, gbc);
     
    DialogSupport ds = new DialogSupport(this, ResourceManager.getManager(), gbc);
    ds.buildAndInstallButtonBox(cp, 10, 10, false, true);
    est_.getModel().extractValues((currentTags == null) ? new ArrayList() : currentTags) ;  
    setLocationRelativeTo(parent);
  }
  
  /***************************************************************************
  **
  ** Answer if we have a result
  ** 
  */
  
  public boolean haveResult() {
    return (haveResult_);
  }
  
  /***************************************************************************
  **
  ** Return results
  ** 
  */
  
  public List getGroups() {
    return (linkGroupResult_);
  }  

  /***************************************************************************
  **
  ** Standard apply
  ** 
  */
 
  public void applyAction() {
    throw new UnsupportedOperationException();
  }
  
  /***************************************************************************
  **
  ** Standard ok
  ** 
  */
 
  public void okAction() {
    if (stashResults(true)) {
      setVisible(false);
      dispose();
    }
    return;   
  }
   
  /***************************************************************************
  **
  ** Standard close
  ** 
  */
  
  public void closeAction() {
    if (stashResults(false)) {
      setVisible(false);
      dispose();
    }
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** The table
  */

  class LinkGroupingTableModel extends EditableTable.TableModel {

    private final static int GROUP_TAG_  = 0;
    private final static int NUM_COL_    = 1;   
    
    LinkGroupingTableModel() {
      super(NUM_COL_);
      colNames_ = new String[] {"linkGroupEdit.suffix"};
      colClasses_ = new Class[] {String.class};
    }    
   
    public List getValuesFromTable() {
      ArrayList retval = new ArrayList();
      for (int i = 0; i < this.rowCount_; i++) {
        retval.add((String)columns_[GROUP_TAG_].get(i));
      }
      return (retval);
    }
     
    public void extractValues(List prsList) {
      super.extractValues(prsList);
      Iterator rit = prsList.iterator();
      while (rit.hasNext()) {
        columns_[GROUP_TAG_].add(rit.next());
      }
      return;
    }
    
    List applyValues() {
      List vals = getValuesFromTable();
      
      //
      // Make sure the groups are OK. Names must be unique, non-blank, present as suffixes in the
      // provided set of link relations, and they must cover the set.
      //
      
      ResourceManager rMan = ResourceManager.getManager();
      ArrayList seenTags = new ArrayList();
      int size = vals.size();
      if (size == 0) {
        return (seenTags);
      }
      
      for (int i = 0; i < size; i++) {
        String tag = (String)vals.get(i);
        if ((tag == null) || (tag.trim().equals(""))) {
          JOptionPane.showMessageDialog(parent_, rMan.getString("rsedit.badRegion"),
                                        rMan.getString("rsedit.badRegionTitle"),
                                        JOptionPane.ERROR_MESSAGE);
          return (null);
        }
        
        tag = tag.trim();
        
        if (DataUtil.containsKey(seenTags, tag)) {
          JOptionPane.showMessageDialog(parent_, rMan.getString("rsedit.dupRegion"),
                                        rMan.getString("rsedit.badRegionTitle"),
                                        JOptionPane.ERROR_MESSAGE);           
            
          return (null);
        }
        
        seenTags.add(tag);
      }
      
      
      boolean fail = false;
      int numST = seenTags.size();
      Iterator arit = allRelations_.iterator();
      while (arit.hasNext()) {
        FabricLink.AugRelation relation = (FabricLink.AugRelation)arit.next();
        boolean gotIt = false;
        for (int i = 0; i < numST; i++) {
          if (relation.relation.indexOf((String)seenTags.get(i)) != -1) {
            gotIt = true;
            break;
          }
        }
        if (!gotIt) {
          fail = true;
          break;
        }
      }
      
      if (fail) {
        JOptionPane.showMessageDialog(parent_, rMan.getString("rsedit.notCovering"),
                                      rMan.getString("rsedit.badCoverageTitle"),
                                      JOptionPane.ERROR_MESSAGE);           
            
        return (null);
      }
      return (seenTags);
    }
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Stash our results for later interrogation.  If they have an error, pop
  ** up a warning dialog and return false, else return true.
  ** 
  */
  
  private boolean stashResults(boolean ok) {
    if (ok) {
      linkGroupResult_ = ((LinkGroupingTableModel)est_.getModel()).applyValues();
      if (linkGroupResult_ == null) {
        haveResult_ = false;
        return (false);
      }
      haveResult_ = true;
      return (true);
    } else {
      haveResult_ = false;
      return (true);
    }
  } 
}
