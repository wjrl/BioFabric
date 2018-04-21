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
import java.awt.Label;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
  
  private final boolean forOrphanEdge_;
  private JFrame parent_;
  private JTextField graph1Field_, graph2Field_, alignField_, perfectField_;
  private File graph1File_, graph2File_, alignmentFile_, perfectAlignFile_; // perfect Alignment is optional
  private FixedJButton buttonOK_;
  private JCheckBox buttonPerfectNG_;
  
  public NetworkAlignmentDialog(JFrame parent, boolean forOrphanEdges) {
    super(parent, ResourceManager.getManager().getString("networkAlignment.title"), new Dimension(700, 400), 3);
    this.parent_ = parent;
    this.forOrphanEdge_ = forOrphanEdges;
    
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
    panGraphInfo.add(new Label(rMan.getString("networkAlignment.message")));
    
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
    addWidgetFullRow(panG1, true);
    addWidgetFullRow(panG2, true);
    addWidgetFullRow(panAlign, true);
  
    buttonPerfectNG_ = new JCheckBox(rMan.getString("networkAlignment.perfectNodeGroups"));
    buttonPerfectNG_.setVisible(false);
  
    //
    // No Perfect Alignment for Orphan Layout
    //
    // 'Correct' node groups enabling
    //
    
    if (!forOrphanEdges) { // add perfect alignment button
      addWidgetFullRow(panPerfect, true);
      addWidgetFullRow(buttonPerfectNG_, true);
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
    return (graph1File_ != null) && (graph2File_ != null) && (alignmentFile_ != null);
  }
  
  public NetworkAlignmentDialogInfo getNAInfo() {
    if (! hasRequiredFiles()) {
      // should never happen
      throw new IllegalStateException("Graph file(s) or alignment file missing.");
    }
    return (new NetworkAlignmentDialogInfo(graph1File_, graph2File_, alignmentFile_, perfectAlignFile_, forOrphanEdge_, buttonPerfectNG_.isSelected()));
  }
  
  /**
   * * Loads the file
   */
  
  private void loadFromFile(FileIndex mode) {
    CommandSet cmd = CommandSet.getCmds("selectionWindow");
    
    File file;
    
    switch (mode) {
      case GRAPH_ONE_FILE:
        file = cmd.getFileLoader().getTheFile(".gw", ".sif", "LoadDirectory", "Graph Files (*.gw, *.sif)");
        break;
      case GRAPH_TWO_FILE:
        file = cmd.getFileLoader().getTheFile(".gw", ".sif", "LoadDirectory", "Graph Files (*.gw, *.sif)");
        break;
      case ALIGNMENT_FILE:
        file = cmd.getFileLoader().getTheFile(".align", null, "LoadDirectory", "filterName.align");
        break;
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
        buttonPerfectNG_.setVisible(true);  // enable check box
        buttonPerfectNG_.setSelected(true);
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
    public final boolean forOrphanEdge, forPerfectNG;
    
    public NetworkAlignmentDialogInfo(File graph1, File graph2, File align, File perfect, boolean forOrphanEdge, boolean forPerfectNG) {
      this.graphA = graph1;
      this.graphB = graph2;
      this.align = align;
      this.perfect = perfect;
      this.forOrphanEdge = forOrphanEdge;
      this.forPerfectNG = forPerfectNG;
    }
    
  }
  
}
