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

import org.systemsbiology.biotapestry.ui.dialogs.utils.BTStashResultsDialog;
import org.systemsbiology.biotapestry.ui.dialogs.utils.DialogSupport;
import org.systemsbiology.biotapestry.ui.dialogs.utils.EditableTable;
import org.systemsbiology.biotapestry.util.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/****************************************************************************
 * *
 * * Dialog comboBox for specifying link groupings
 */

public class LinkGroupingSetupDialog extends BTStashResultsDialog {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  public final int CHOICE_PER_NODE    = 0;
  public final int CHOICE_PER_NETWORK = 1;

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
  private JComboBox comboBox;
  private int chosenMode;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
   * *
   * * Constructor
   */

  public LinkGroupingSetupDialog(JFrame parent, List currentTags, Set allRelations, BioFabricNetwork bfn) {
    super(parent, ResourceManager.getManager().getString("linkGroupEdit.title"),new Dimension(650, 450), 2);
    parent_ = parent;
    allRelations_ = allRelations;

    installJComboBox(bfn);

    FixedJButton fileButton =
            new FixedJButton(rMan_.getString("linkGroupEdit.loadFromFile"));

    fileButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          loadFromFile();
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });

    est_ = new EditableTable(new LinkGroupingTableModel(), parent_);
    EditableTable.TableParams etp = new EditableTable.TableParams();
    etp.addAlwaysAtEnd = false;
    etp.buttons = EditableTable.ALL_BUT_EDIT_BUTTONS;
    etp.singleSelectOnly = true;
    JPanel tablePan = est_.buildEditableTable(etp);

    addTable(tablePan, 4);

    est_.getModel().extractValues((currentTags == null) ? new ArrayList() : currentTags);
    setLocationRelativeTo(parent);

    finishConstructionWithExtraLeftButton(fileButton);
  }

  /***************************************************************************
   * * Install JComboBox with existing mode pre-selected from BioFabric Network's
   * * current layoutMode
   */

  private void installJComboBox(BioFabricNetwork bfn) {
    JLabel boxLabel = new JLabel(rMan_.getString("linkGroupEdit.mode"));

    String[] choices = new String[2];
    choices[CHOICE_PER_NODE] = rMan_.getString("linkGroupEdit.orderPerNode");
    choices[CHOICE_PER_NETWORK] = rMan_.getString("linkGroupEdit.orderNetwork");

    comboBox = new JComboBox(choices);

    int mode = bfn.getLayoutMode();
    if (mode == BioFabricNetwork.UNINITIALIZED_MODE || mode == BioFabricNetwork.PER_NODE_MODE) {
      comboBox.setSelectedIndex(CHOICE_PER_NODE);
    } else if (mode == BioFabricNetwork.PER_NETWORK_MODE) {
      comboBox.setSelectedIndex(CHOICE_PER_NETWORK);
    }

    addLabeledWidget(boxLabel, comboBox, true, true);
  }

  /***************************************************************************
   * *
   * * Answer if we have a result
   * *
   */
  
  public boolean haveResult() {
    return (haveResult_);
  }
  
  /***************************************************************************
   * *
   * * Return results
   * *
   */
  
  public List getGroups() {
    return (linkGroupResult_);
  }

  /***************************************************************************
   * *
   * * Return chosen mode of comboBox
   * *
   */

  public int getSelectedMode() {
    return chosenMode;
  }

  /***************************************************************************
   * *
   * * Standard apply
   * *
   */

  public void applyAction() {
    throw new UnsupportedOperationException();
  }
  
  /***************************************************************************
   * *
   * * Standard ok
   * *
   */

  public void okAction() {
    if (stashResults(true)) {
      setVisible(false);
      dispose();
    }
    return;
  }

  /***************************************************************************
   * *
   * * Standard close
   * *
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
   * *
   * * The table
   */

  class LinkGroupingTableModel extends EditableTable.TableModel {

    private final static int GROUP_TAG_ = 0;
    private final static int NUM_COL_ = 1;
    
    LinkGroupingTableModel() {
      super(NUM_COL_);
      colNames_ = new String[]{"linkGroupEdit.suffix"};
      colClasses_ = new Class[]{String.class};
    }

    public List getValuesFromTable() {
      ArrayList retval = new ArrayList();
      for (int i = 0; i < this.rowCount_; i++) {
        retval.add((String) columns_[GROUP_TAG_].get(i));
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
        String tag = (String) vals.get(i);
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
        FabricLink.AugRelation relation = (FabricLink.AugRelation) arit.next();
        boolean gotIt = false;
        for (int i = 0; i < numST; i++) {
          if (relation.relation.indexOf((String) seenTags.get(i)) != - 1) {
            gotIt = true;
            break;
          }
        }
        if (! gotIt) {
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
   * *
   * * Stash our results for later interrogation.  If they have an error, pop
   * * up a warning dialog and return false, else return true.
   * *
   */

  @Override
  protected boolean stashForOK() {
    linkGroupResult_ = ((LinkGroupingTableModel) est_.getModel()).applyValues();
    if (linkGroupResult_ == null) {
      haveResult_ = false;
      return (false);
    }
    haveResult_ = true;

    int mode = comboBox.getSelectedIndex();

    if (mode == CHOICE_PER_NODE) {
      chosenMode =  BioFabricNetwork.PER_NODE_MODE;
    } else if (mode == CHOICE_PER_NETWORK){
      chosenMode = BioFabricNetwork.PER_NETWORK_MODE;
    } else {
      ExceptionHandler.getHandler()
              .displayException(new IllegalArgumentException("Illegal Link-Group-Dialog-JComboBox Selected Index"));
    }
    return (true);
  }

  /**
   * loads relation order from file and updates table with new order.
   * error checking done when user presses "ok"
   */

  private void loadFromFile() {
    FabricCommands cmd = FabricCommands.getCmds("mainWindow");
    File fileEda = cmd.getTheFile(".txt", null, "AttribDirectory", "filterName.txt");
    if (fileEda == null) {
      return;
    }
    List nodes = UiUtil.simpleFileRead(fileEda);
    if (nodes == null) {
      return;
    }

    est_.updateTable(true, nodes);  // update table
    return;
  }

}
