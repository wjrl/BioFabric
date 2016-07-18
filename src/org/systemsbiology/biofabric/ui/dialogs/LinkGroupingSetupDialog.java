/*
**    Copyright (C) 2003-2014 Institute for Systems Biology 
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

package org.systemsbiology.biofabric.ui.dialogs;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.border.EmptyBorder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.systemsbiology.biofabric.model.FabricLink;
import org.systemsbiology.biofabric.ui.dialogs.utils.DialogSupport;
import org.systemsbiology.biofabric.ui.dialogs.utils.EditableTable;
import org.systemsbiology.biofabric.util.DataUtil;
import org.systemsbiology.biofabric.util.ResourceManager;
import org.systemsbiology.biofabric.util.UiUtil;

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
  private List<String> linkGroupResult_;
  private EditableTable<LinkGroupingTableModel.TableRow> est_;
  private JFrame parent_;
  private Set<FabricLink.AugRelation> allRelations_;
  private static final long serialVersionUID = 1L;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 

  public LinkGroupingSetupDialog(JFrame parent, List<String> currentTags, Set<FabricLink.AugRelation> allRelations) {     
    super(parent, ResourceManager.getManager().getString("linkGroupEdit.title"), true);
    parent_ = parent;
    allRelations_ = allRelations;
        
    setSize(650, 300);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();

    est_ = new EditableTable<LinkGroupingTableModel.TableRow>(new LinkGroupingTableModel(), parent_);
    EditableTable.TableParams etp = new EditableTable.TableParams();
    etp.addAlwaysAtEnd = false;
    etp.buttons = EditableTable.ALL_BUT_EDIT_BUTTONS;
    etp.singleSelectOnly = true;
    JPanel tablePan = est_.buildEditableTable(etp);
                                        
    UiUtil.gbcSet(gbc, 0, 0, 10, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    cp.add(tablePan, gbc);
     
    DialogSupport ds = new DialogSupport(this, ResourceManager.getManager(), gbc);
    ds.buildAndInstallButtonBox(cp, 10, 10, false, true);
    est_.getModel().extractValues(initTableRows((currentTags == null) ? new ArrayList<String>() : currentTags)) ;  
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
  
  public List<String> getGroups() {
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
  
  /***************************************************************************
  **
  ** Get the list of table rows
  ** 
  */

  private List<LinkGroupingTableModel.TableRow> initTableRows(List<String> inTags) {
    ArrayList<LinkGroupingTableModel.TableRow> retval = new ArrayList<LinkGroupingTableModel.TableRow>();
    EditableTable.TableModel<LinkGroupingTableModel.TableRow> ecdtm = est_.getModel(); 
    Iterator<String> ceit = inTags.iterator();
    while (ceit.hasNext()) {
      String tag = ceit.next();
      LinkGroupingTableModel.TableRow tr = ecdtm.constructARow();
      tr.groupTag = tag;
      retval.add(tr);
    }
    return (retval);
  } 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** The table
  */

  class LinkGroupingTableModel extends EditableTable.TableModel<LinkGroupingTableModel.TableRow> {

    private final static int GROUP_TAG_  = 0;
    private final static int NUM_COL_    = 1;
    
    private static final long serialVersionUID = 1L;
    
    LinkGroupingTableModel() {
      super(NUM_COL_);
      colNames_ = new String[] {"linkGroupEdit.suffix"};
      colClasses_ = new Class[] {String.class};
    }    
   
    public class TableRow implements EditableTable.ATableRow {
      public String groupTag;
      
      public TableRow() {
      }
      
      TableRow(int i) {
        groupTag = (String)columns_.get(GROUP_TAG_).get(i);
      }
      
      public void toCols() {
        columns_.get(GROUP_TAG_).add(groupTag);  
        return;
      }
    }
  
    protected TableRow constructARow(int i) {
      return (new TableRow(i));     
    }
    public TableRow constructARow() {
      return (new TableRow());     
    }   
    
    List<String> applyValues() {
      List<TableRow> vals = getValuesFromTable();
      
      //
      // Make sure the groups are OK. Names must be unique, non-blank, present as suffixes in the
      // provided set of link relations, and they must cover the set. Also, issue #1
      //
      
      ResourceManager rMan = ResourceManager.getManager();
      ArrayList<String> seenTags = new ArrayList<String>();
      int size = vals.size();
      if (size == 0) {
        return (seenTags);
      }
      
      //
      // Blanks not allowed, duplicates not allowed:
      //
      
      for (int i = 0; i < size; i++) {
        TableRow row = vals.get(i);
        String tag = row.groupTag;
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
      
      //
      // This tests that all the provided tags show up somewhere in the set of relations,
      // plus that all relations are matched by one and only one tag. The latter case should
      // deal with issue #8
      // 
      
      //
      // Note that all group suffixes must apply to at least one AugRelation; each group suffix is allowed 
      // to apply to multiple AugRelations. However, all AugRelations must have exactly one suffix that applies, 
      //
      
      int numST = seenTags.size();
      int numAugs = 0;
      HashSet<FabricLink.AugRelation> augsWithTags = new HashSet<FabricLink.AugRelation>();
      HashSet<String> tagsWithAug = new HashSet<String>();
      Iterator<FabricLink.AugRelation> arit = allRelations_.iterator();
      while (arit.hasNext()) {
        FabricLink.AugRelation relation = arit.next();
        numAugs++;
        for (int i = 0; i < numST; i++) {
          String checkTag = seenTags.get(i);
          if (relation.relation.indexOf(checkTag) == (relation.relation.length() - checkTag.length())) {
            // We have already had a match with another tag:
            if (augsWithTags.contains(relation)) {
              JOptionPane.showMessageDialog(parent_, rMan.getString("rsedit.ambig"),
                                            rMan.getString("rsedit.ambigTitle"),
                                            JOptionPane.ERROR_MESSAGE);            
              return (null);
            }
            augsWithTags.add(relation);
            tagsWithAug.add(checkTag);
          }
        }
      }
      
      if ((tagsWithAug.size() != numST) || (augsWithTags.size() != numAugs)) {
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
