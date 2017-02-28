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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.systemsbiology.biotapestry.ui.dialogs.utils.EditableTable;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTStashResultsDialog;
import org.systemsbiology.biotapestry.util.ExceptionHandler;
import org.systemsbiology.biotapestry.util.FixedJButton;

/****************************************************************************
**
** Dialog box for editing relation directionality
*/

public class RelationDirectionDialog extends BTStashResultsDialog {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private EditableTable est_;
  private SortedMap returnMap_;
  private boolean getFromFile_;  
  private HashSet returnKeys_;  
  private SortedMap reducedMap_;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public RelationDirectionDialog(JFrame parent, SortedMap relationMap) {
    super(parent, "relDir.title", new Dimension(800, 400), 1);
    
    getFromFile_ = false;
    returnMap_ = null;
    returnKeys_ = new HashSet(relationMap.keySet());
    reducedMap_ = new TreeMap();
    HashSet seenRels = new HashSet();
    Iterator ceit = relationMap.keySet().iterator();
    while (ceit.hasNext()) {
      FabricLink.AugRelation aug = (FabricLink.AugRelation)ceit.next();
      if (seenRels.contains(aug.relation)) {
        continue;
      }
      seenRels.add(aug.relation);
      Boolean isDir = (Boolean)relationMap.get(aug);
      reducedMap_.put(new FabricLink.AugRelation(aug.relation, false), isDir); 
    }   
 
    est_ = new EditableTable(new RelationDirTableModel(), parent_);
    EditableTable.TableParams etp = new EditableTable.TableParams();
    etp.addAlwaysAtEnd = true;
    etp.singleSelectOnly = true;
    etp.cancelEditOnDisable = true;
    etp.tableIsUnselectable = true;
    etp.perColumnEnums = null;
    etp.colWidths = new ArrayList();  
    etp.colWidths.add(new EditableTable.ColumnWidths(RelationDirTableModel.RELATION, 50, 300, 5000));   
    etp.colWidths.add(new EditableTable.ColumnWidths(RelationDirTableModel.DIRECTION, 50, 100, 200));
   
    JPanel tablePan = est_.buildEditableTable(etp);
    addTable(tablePan, 8);  returnKeys_ = new HashSet(relationMap.keySet());
 
    ArrayList xtraButtonList = new ArrayList();
    FixedJButton buttonSA = new FixedJButton(rMan_.getString("dialogs.selectAll"));
    buttonSA.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          doMassUpdate(true);
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
    xtraButtonList.add(buttonSA);
    
    FixedJButton buttonSN = new FixedJButton(rMan_.getString("dialogs.selectNone"));
    buttonSN.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          doMassUpdate(false);
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
    xtraButtonList.add(buttonSN);
    
    FixedJButton buttonFile = new FixedJButton(rMan_.getString("relDir.useFile"));
    buttonFile.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          getFromFile_ = true;
          okAction();
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
    xtraButtonList.add(buttonFile);

    finishConstructionWithMultiExtraLeftButtons(xtraButtonList);
    displayProperties();
  }
  
  /***************************************************************************
  **
  ** Mass check/uncheck
  ** 
  */
  
  void doMassUpdate(boolean select) {
    Boolean isDir = new Boolean(select);
    RelationDirTableModel ecdtm = (RelationDirTableModel)est_.getModel(); 
    List vals = ecdtm.getValuesFromTable();
    int numVals = vals.size();
    ArrayList upVals = new ArrayList();
    for (int i = 0; i < numVals; i++) {
      RelationDirTableModel.TableRow tr = (RelationDirTableModel.TableRow)vals.get(i);
      RelationDirTableModel.TableRow trc = ecdtm.new TableRow();
      trc.relation = tr.relation;
      trc.isDir = isDir;
      upVals.add(trc);
    }
    est_.updateTable(true, upVals);
    return;
  }
  
  /***************************************************************************
  **
  ** Get the result
  ** 
  */
  
  public boolean getFromFile() {
    return (getFromFile_);
  }   
  
  /***************************************************************************
  **
  ** Get the result
  ** 
  */
  
  public SortedMap getRelationMap() {
    if (getFromFile_) {
      return (null);
    }
    returnMap_ = new TreeMap();
    Iterator ceit = returnKeys_.iterator();
    while (ceit.hasNext()) {
      FabricLink.AugRelation aug = (FabricLink.AugRelation)ceit.next();
      Boolean isDir = (Boolean)reducedMap_.get(new FabricLink.AugRelation(aug.relation, false));
      returnMap_.put(aug, isDir);
    }   
    return (returnMap_);
  }   
     
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Used for the data table
  */
  
  class RelationDirTableModel extends EditableTable.TableModel {
    
    final static int RELATION         = 0;
    final static int DIRECTION        = 1;
    private final static int NUM_COL_ = 2;
    
    class TableRow {
      String relation;
      Boolean isDir;
      
      TableRow() {   
      }
     
      TableRow(int i) {
        relation = (String)columns_[RELATION].get(i);
        isDir = (Boolean)columns_[DIRECTION].get(i);
      }
      
      void toCols() {
        columns_[RELATION].add(relation);  
        columns_[DIRECTION].add(isDir);
        return;
      }
      
      void replaceCols(int row) {
        columns_[RELATION].set(row, relation);  
        columns_[DIRECTION].set(row, isDir);
        return;
      }  
    }
 
    RelationDirTableModel() {
      super(NUM_COL_);
      colNames_ = new String[] {"relDir.relation",
                                "relDir.direction"};
      colClasses_ = new Class[] {String.class,
                                 Boolean.class};
      canEdit_ = new boolean[] {false,
                                true};
    }
 
    public List getValuesFromTable() {
      ArrayList retval = new ArrayList();
      for (int i = 0; i < rowCount_; i++) {
        TableRow ent = new TableRow(i);
        retval.add(ent);
      }
      return (retval); 
    }
  
    public void extractValues(List prsList) {
      super.extractValues(prsList);
      Iterator rit = prsList.iterator();
      while (rit.hasNext()) { 
        TableRow ent = (TableRow)rit.next();
        ent.toCols();
      }
      return;
    }
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Apply the data values to our property table UI component
  ** 
  */
  
  private void displayProperties() {
    List tableRows = initTableRows();
    ((RelationDirTableModel)est_.getModel()).extractValues(tableRows);
    return;
  }
  
  /***************************************************************************
  **
  ** Get the list of table rows
  ** 
  */

  private List initTableRows() {
    ArrayList retval = new ArrayList();
    RelationDirTableModel ecdtm = (RelationDirTableModel)est_.getModel(); 
    Iterator ceit = reducedMap_.keySet().iterator();
    while (ceit.hasNext()) {
      FabricLink.AugRelation aug = (FabricLink.AugRelation)ceit.next();
      RelationDirTableModel.TableRow tr = ecdtm.new TableRow();
      tr.relation = aug.relation;
      tr.isDir = (Boolean)reducedMap_.get(aug);
      retval.add(tr);
    }
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Stash our results for later interrogation.
  ** 
  */
  
  protected boolean stashForOK() {
    if (getFromFile_) {
      reducedMap_.clear();
      return (true);
    }
    RelationDirTableModel ecdtm = (RelationDirTableModel)est_.getModel(); 
    List vals = ecdtm.getValuesFromTable();
    int numVals = vals.size();
    for (int i = 0; i < numVals; i++) {
      RelationDirTableModel.TableRow tr = (RelationDirTableModel.TableRow)vals.get(i);
      reducedMap_.put(new FabricLink.AugRelation(tr.relation, false), tr.isDir);
    }
    return (true);
  }  
}
