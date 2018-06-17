/*
**    File created by Rishi Desai
**
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

package org.systemsbiology.biofabric.plugin.core.align;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import org.systemsbiology.biofabric.cmd.CommandSet;
import org.systemsbiology.biofabric.ui.dialogs.utils.BTStashResultsDialog;
import org.systemsbiology.biofabric.ui.dialogs.utils.DialogSupport;
import org.systemsbiology.biofabric.util.ExceptionHandler;
import org.systemsbiology.biofabric.util.FixedJButton;
import org.systemsbiology.biofabric.util.MatchingJLabel;
import org.systemsbiology.biofabric.util.ResourceManager;
import org.systemsbiology.biotapestry.biofabric.FabricCommands;

import javax.swing.border.EmptyBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class NetworkAlignmentDialog extends BTStashResultsDialog {
  
  private enum FileIndex {
    GRAPH_ONE_FILE, GRAPH_TWO_FILE, ALIGNMENT_FILE, PERFECT_FILE
  }
  
  private final NetworkAlignmentBuildData.ViewType analysisType_;
  private JFrame parent_;
  private JTextField graph1Field_, graph2Field_, alignField_, perfectField_;
  private File graph1File_, graph2File_, alignmentFile_, perfectAlignFile_; // perfect Alignment is optional
  private FixedJButton buttonOK_;
  private JCheckBox undirectedConfirm_;
  private static final long serialVersionUID = 1L;
  private JComboBox perfectNGsCombo_;
  
  private final int NONE_INDEX = 0, NC_INDEX = 1, JS_INDEX = 2; // indices on combo box

  
  public NetworkAlignmentDialog(JFrame parent, NetworkAlignmentBuildData.ViewType analysisType) {
    super(parent, ResourceManager.getManager().getString("networkAlignment.title"), new Dimension(700, 450), 3);
    this.parent_ = parent;
    this.analysisType_ = analysisType;
    
    final ResourceManager rMan = ResourceManager.getManager();
    JPanel cp = (JPanel) getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    
    //
    // File Buttons and File Labels
    //
    
    JButton graph1Browse = new JButton(rMan.getString("networkAlignment.browse"));
    JButton graph2Browse = new JButton(rMan.getString("networkAlignment.browse"));
    JButton alignmentBrowse = new JButton(rMan.getString("networkAlignment.browse"));
    JButton perfectBrowse = new JButton(rMan.getString("networkAlignment.browse"));
  
    graph1Field_ = new JTextField(30);
    graph2Field_= new JTextField(30);
    alignField_ = new JTextField(30);
    perfectField_ = new JTextField(30);
    undirectedConfirm_ = new JCheckBox(rMan.getString("networkAlignment.confirmUndirected"));

    MatchingJLabel graph1FileMatch, graph2FileMatch, alignFileMatch, perfectFileMatch;
    JLabel perfectFileName = new JLabel(rMan.getString("networkAlignment.perfect")); // only to use as a reference, not in dialog
    perfectFileMatch = new MatchingJLabel(rMan.getString("networkAlignment.perfect"), perfectFileName);
    graph1FileMatch = new MatchingJLabel(rMan.getString("networkAlignment.graph1"), perfectFileName);
    graph2FileMatch = new MatchingJLabel(rMan.getString("networkAlignment.graph2"), perfectFileName);
    alignFileMatch = new MatchingJLabel(rMan.getString("networkAlignment.alignment"), perfectFileName);
    
    graph1FileMatch.setHorizontalAlignment(SwingConstants.CENTER);
    graph2FileMatch.setHorizontalAlignment(SwingConstants.CENTER);
    alignFileMatch.setHorizontalAlignment(SwingConstants.CENTER);
    perfectFileMatch.setHorizontalAlignment(SwingConstants.CENTER);
    
    undirectedConfirm_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          if (hasRequiredFiles()) { // enable OK button
            buttonOK_.setEnabled(true);
          }
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
    
    graph1Browse.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          loadFromFile(FileIndex.GRAPH_ONE_FILE);
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
    
    graph2Browse.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          loadFromFile(FileIndex.GRAPH_TWO_FILE);
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
    
    alignmentBrowse.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          loadFromFile(FileIndex.ALIGNMENT_FILE);
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
  
    perfectBrowse.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          loadFromFile(FileIndex.PERFECT_FILE);
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
    
    JPanel panGraphInfo = new JPanel(new FlowLayout(FlowLayout.LEFT));
    JPanel panGraphInfoTwo = null;
    switch (analysisType_) {
      case ORPHAN:
        panGraphInfo.add(new JLabel(rMan.getString("networkAlignment.messageNonGroup")));
        break;
      case CYCLE:
        panGraphInfo.add(new JLabel(rMan.getString("networkAlignment.messageNonGroup")));
        panGraphInfoTwo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panGraphInfoTwo.add(new JLabel(rMan.getString("networkAlignment.messageCycleTwo")));
        break;
      case GROUP:
        panGraphInfo.add(new JLabel(rMan.getString("networkAlignment.message")));
        break;
      default:
        throw new IllegalStateException();
    }
   
    JPanel panGraphConfirm = new JPanel(new FlowLayout(FlowLayout.LEFT));
    panGraphConfirm.add(undirectedConfirm_);
    
    JPanel panG1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
    panG1.add(graph1FileMatch);
    panG1.add(graph1Field_);
    panG1.add(graph1Browse);
  
    JPanel panG2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
    panG2.add(graph2FileMatch);
    panG2.add(graph2Field_);
    panG2.add(graph2Browse);
  
    JPanel panAlign = new JPanel(new FlowLayout(FlowLayout.LEFT));
    panAlign.add(alignFileMatch);
    panAlign.add(alignField_);
    panAlign.add(alignmentBrowse);
  
    JPanel panPerfect = new JPanel(new FlowLayout(FlowLayout.LEFT));
    panPerfect.add(perfectFileMatch);
    panPerfect.add(perfectField_);
    panPerfect.add(perfectBrowse);
   
    addWidgetFullRow(panGraphInfo, true);
    if (panGraphInfoTwo != null) {
      addWidgetFullRow(panGraphInfoTwo, true);
    }
    addWidgetFullRow(panGraphConfirm, true);
    addWidgetFullRow(panG1, true);
    addWidgetFullRow(panG2, true);
    addWidgetFullRow(panAlign, true);
  
    JLabel perfectNGLabel = new JLabel(rMan.getString("networkAlignment.perfectNodeGroups"));
    String[] choices = new String[3];
    choices[NONE_INDEX] = rMan.getString("networkAlignment.none");
    choices[NC_INDEX] = rMan.getString("networkAlignment.nodeCorrectness");
    choices[JS_INDEX] = rMan.getString("networkAlignment.jaccardSimilarity");
    
    perfectNGsCombo_ = new JComboBox(choices); // have to use unchecked for v1.6
    perfectNGsCombo_.setEnabled(false);
    perfectNGsCombo_.setSelectedIndex(NONE_INDEX);
  
    //
    // No Perfect Alignment for Orphan Layout
    //
    // 'Correct' node groups enabling
    //
    
    if (analysisType_ != NetworkAlignmentBuildData.ViewType.ORPHAN) { // add perfect alignment button
      addWidgetFullRow(panPerfect, true);
      addLabeledWidget(perfectNGLabel, perfectNGsCombo_, true, true);
    }
    
    //
    // OK button
    //
    
    DialogSupport.Buttons buttons = finishConstruction();
    
    buttonOK_ = buttons.okButton;
    buttonOK_.setEnabled(false);
    
    setLocationRelativeTo(parent);
  }
  
  @Override
  public void okAction() {
    try {
      super.okAction();
    } catch (Exception ex) {
      // should never happen because OK button is disabled without correct files.
      ExceptionHandler.getHandler().displayException(ex);
    }
    return;
  }
  
  @Override
  public void closeAction() {
    try {
      super.closeAction();
    } catch (Exception ex) {
      ExceptionHandler.getHandler().displayException(ex);
    }
  }
  
  @Override
  protected boolean stashForOK() {
    return (hasRequiredFiles());
  }
  
  /**
   * If user entered in minimum required files
   */
  
  private boolean hasRequiredFiles() {
    return (graph1File_ != null) && (graph2File_ != null) && (alignmentFile_ != null) && undirectedConfirm_.isSelected();
  }
  
  public NetworkAlignmentDialogInfo getNAInfo() {
    if (! hasRequiredFiles()) {
      // should never happen
      throw new IllegalStateException("Graph file(s) or alignment file missing.");
    }
    
    NodeGroupMap.PerfectNGMode mode;
    switch (perfectNGsCombo_.getSelectedIndex()) {
      case NONE_INDEX:
        mode = NodeGroupMap.PerfectNGMode.NONE;
        break;
      case NC_INDEX:
        mode = NodeGroupMap.PerfectNGMode.NODE_CORRECTNESS;
        break;
      case JS_INDEX:
        mode = NodeGroupMap.PerfectNGMode.JACCARD_SIMILARITY;
        break;
      default:
        // should never happen
        throw (new IllegalStateException("Illegal perfect NG mode"));
    }
  
    return (new NetworkAlignmentDialogInfo(graph1File_, graph2File_, alignmentFile_, perfectAlignFile_, analysisType_, mode));
  }
  
  /**
   * * Loads the file
   */
  
  private void loadFromFile(FileIndex mode) {
    CommandSet cmd = CommandSet.getCmds("selectionWindow");
    
    File file;
    
    switch (mode) {
      case GRAPH_ONE_FILE:
      case GRAPH_TWO_FILE:
        file = cmd.getFileLoader().getTheFile(".gw", ".sif", "LoadDirectory", "filterName.graph");
        break;
      case ALIGNMENT_FILE:
      case PERFECT_FILE:
        file = cmd.getFileLoader().getTheFile(".align", null, "LoadDirectory", "filterName.align");
        break;
      default:
        throw new IllegalArgumentException();
    }
    
    if (file == null) {
      return;
    }
    
    FabricCommands.setPreference("LoadDirectory", file.getAbsoluteFile().getParent());
    
    switch (mode) {
      case GRAPH_ONE_FILE:
        graph1Field_.setText(file.getAbsolutePath());
        graph1File_ = file;
        break;
      case GRAPH_TWO_FILE:
        graph2Field_.setText(file.getAbsolutePath());
        graph2File_ = file;
        break;
      case ALIGNMENT_FILE:
        alignField_.setText(file.getAbsolutePath());
        alignmentFile_ = file;
        break;
      case PERFECT_FILE:
        perfectField_.setText(file.getAbsolutePath());
        perfectAlignFile_ = file;
        perfectNGsCombo_.setEnabled(true);
        break;
      default:
        throw new IllegalArgumentException();
    }
    
    if (hasRequiredFiles()) { // enable OK button
      buttonOK_.setEnabled(true);
    }
    return;
  }
  
  /**
   * The unread files for G1, G2, and the Alignment
   */
  
  public static class NetworkAlignmentDialogInfo {
    
    public final File graphA, graphB, align, perfect; // graph1 and graph2 can be out of order (size), hence graphA and graphB

    public final NetworkAlignmentBuildData.ViewType analysisType;
    public final NodeGroupMap.PerfectNGMode mode;
    
    public NetworkAlignmentDialogInfo(File graph1, File graph2, File align, File perfect,
                                      NetworkAlignmentBuildData.ViewType analysisType, NodeGroupMap.PerfectNGMode mode) {
      this.graphA = graph1;
      this.graphB = graph2;
      this.align = align;
      this.perfect = perfect;
      this.analysisType = analysisType;
      this.mode = mode;
    }
    
  }
  
}
