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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.ui.dialogs.utils.BTStashResultsDialog;
import org.systemsbiology.biotapestry.ui.dialogs.utils.EditableTable;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.ExceptionHandler;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
 **
 ** Dialog box for setting up a node comparison
 */

public class CompareNodesSetupDialog extends BTStashResultsDialog {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private Set result_;
  private EditableTable est_;
  private Set allNodes_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
   **
   ** Constructor
   */

  public CompareNodesSetupDialog(JFrame parent, Set allNodes) {
    super(parent, "compareNodesSetup.title", new Dimension(600, 500), 2);
    result_ = null;
    allNodes_ = allNodes;

    //
    // Build extra button:
    //

    FixedJButton buttonR = new FixedJButton(rMan_.getString("compareNodesSetup.loadFromFile"));
    buttonR.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          loadFromFile();
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });

    est_ = new EditableTable(new NodeListTableModel(), parent_);
    EditableTable.TableParams etp = new EditableTable.TableParams();
    etp.addAlwaysAtEnd = false;
    etp.buttons = EditableTable.ALL_BUT_EDIT_BUTTONS;
    etp.singleSelectOnly = true;
    JPanel tablePan = est_.buildEditableTable(etp);

    addTable(tablePan, 5);
    finishConstructionWithExtraLeftButton(buttonR);

  }

  /***************************************************************************
   **
   ** Return results
   **
   */

  public Set getResults() {
    return (result_);
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

  class NodeListTableModel extends EditableTable.TableModel {

    private final static int NODE_NAME_  = 0;
    private final static int NUM_COL_    = 1;

    NodeListTableModel() {
      super(NUM_COL_);
      colNames_ = new String[] {"compareNodesSetup.nodeName"};
      colClasses_ = new Class[] {String.class};
    }

    public List getValuesFromTable() {
      ArrayList retval = new ArrayList();
      for (int i = 0; i < this.rowCount_; i++) {
        retval.add((String)columns_[NODE_NAME_].get(i));
      }
      return (retval);
    }

    public void extractValues(List prsList) {
      super.extractValues(prsList);
      Iterator rit = prsList.iterator();
      while (rit.hasNext()) {
        columns_[NODE_NAME_].add(rit.next());
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
          JOptionPane.showMessageDialog(parent_, rMan.getString("compareNodesSetup.badName"),
                  rMan.getString("compareNodesSetup.badNameTitle"),
                  JOptionPane.ERROR_MESSAGE);
          return (null);
        }

        tag = tag.trim();

        if (DataUtil.containsKey(seenTags, tag)) {
          JOptionPane.showMessageDialog(parent_, rMan.getString("compareNodesSetup.dupName"),
                  rMan.getString("compareNodesSetup.dupNameTitle"),
                  JOptionPane.ERROR_MESSAGE);

          return (null);
        }

        if (!DataUtil.containsKey(allNodes_, tag)) {
          JOptionPane.showMessageDialog(parent_, rMan.getString("compareNodesSetup.notANode"),
                  rMan.getString("compareNodesSetup.notANodeTitle"),
                  JOptionPane.ERROR_MESSAGE);

          return (null);
        }
        seenTags.add(tag);
      }

      return (seenTags);
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
   **
   ** Stash our results for later interrogation.
   **
   */

  protected boolean stashForOK() {
    List av = ((NodeListTableModel)est_.getModel()).applyValues();
    if (av == null) {
      result_ = null;
      return (false);
    }
    result_ = new HashSet(av);
    return (true);
  }

  /***************************************************************************
   **
   ** Load names from a file
   **
   */

  void loadFromFile() {
    FabricCommands cmd = FabricCommands.getCmds("mainWindow");
    File fileEda = cmd.getTheFile(".txt", null, "AttribDirectory", "filterName.txt");
    if (fileEda == null) {
      return;
    }
    List nodes = UiUtil.simpleFileRead(fileEda);
    if (nodes == null) {
      return;
    }
    ((NodeListTableModel)est_.getModel()).extractValues(nodes);
    est_.updateTable(true, nodes);
    cmd.setPreference("AttribDirectory", fileEda.getAbsoluteFile().getParent());
    return;
  }
}