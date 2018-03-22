/*
**    File created by Rishi Desai
**
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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.Label;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.systemsbiology.biofabric.cmd.CommandSet;
import org.systemsbiology.biofabric.ui.dialogs.utils.BTStashResultsDialog;
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
  
  public NetworkAlignmentDialog(JFrame parent, boolean forOrphanEdges) {
    super(parent, ResourceManager.getManager().getString("networkAlignment.title"), new Dimension(600, 300), 3);
    this.parent_ = parent;
    this.forOrphanEdge_ = forOrphanEdges;
    
    final ResourceManager rMan = ResourceManager.getManager();
//    setSize(600, 300);
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
  
//    JLabel graph1FileName_, graph2FileName_, alignFileName_, perfectFileName_;
//    graph1FileName_ = new JLabel(rMan.getString("networkAlignment.graph1"));
//    graph2FileName_ = new JLabel(rMan.getString("networkAlignment.graph2"));
//    alignFileName_ = new JLabel(rMan.getString("networkAlignment.alignment"));
//    perfectFileName_ = new JLabel(rMan.getString("networkAlignment.perfect"));
    
    graph1Field_ = new JTextField(25);
    graph2Field_= new JTextField(25);
    alignField_ = new JTextField(25);
    perfectField_ = new JTextField(25);
  
    graph1Field_.setEnabled(false);
    graph2Field_.setEnabled(false);
    alignField_.setEnabled(false);
    perfectField_.setEnabled(false);
  
//    MatchingJLabel graph1FileMatch, graph2FileMatch, alignFileMatch, perfectFileMatch;
//    graph1FileMatch = new MatchingJLabel(graph1FileName_.getText(), perfectFileName_);
//    graph2FileMatch = new MatchingJLabel(graph2FileName_.getText(), perfectFileName_);
//    alignFileMatch = new MatchingJLabel(alignFileName_.getText(), perfectFileName_);
//    perfectFileMatch = new MatchingJLabel(perfectFileName_.getText(), perfectFileName_);
    
    MatchingJLabel graph1FileMatch, graph2FileMatch, alignFileMatch, perfectFileMatch;
    JLabel perfectFileName = new JLabel(rMan.getString("networkAlignment.perfect")); // only to use as a reference, not in dialog
    perfectFileMatch = new MatchingJLabel(rMan.getString("networkAlignment.perfect"), perfectFileName);
    graph1FileMatch = new MatchingJLabel(rMan.getString("networkAlignment.graph1"), perfectFileName);
    graph2FileMatch = new MatchingJLabel(rMan.getString("networkAlignment.graph2"), perfectFileName);
    alignFileMatch = new MatchingJLabel(rMan.getString("networkAlignment.alignment"), perfectFileName);

//    graph1FileMatch = new MatchingJLabel(graph1FileName_.getText(), graph1FileName_);
//    graph2FileMatch = new MatchingJLabel(graph2FileName_.getText(), graph2FileName_);
//    alignFileMatch = new MatchingJLabel(alignFileName_.getText(), alignFileName_);
//    perfectFileMatch = new MatchingJLabel(perfectFileName_.getText(), perfectFileName_);
//    perfectFileMatch.setPreferredSize(new Dimension(30, perfectFileName_.getHeight()));
//    perfectFileMatch.getPreferredSize();
//    alignFileMatch.getPreferredSize();
//
//    graph1FileMatch.getPreferredSize();
//    graph2FileMatch.getPreferredSize();
    
    
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
//    panG1.add(graph1FileName_);
    panG1.add(graph1Field_);
    panG1.add(graph1Browse);
  
    JPanel panG2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
    panG2.add(graph2FileMatch);
//    panG2.add(graph2FileName_);
    panG2.add(graph2Field_);
    panG2.add(graph2Browse);
  
    JPanel panAlign = new JPanel(new FlowLayout(FlowLayout.LEFT));
    panAlign.add(alignFileMatch);
//    panAlign.add(alignFileName_);
    panAlign.add(alignField_);
    panAlign.add(alignmentBrowse);
  
    JPanel panPerfect = new JPanel(new FlowLayout(FlowLayout.LEFT));
    panPerfect.add(perfectFileMatch);
//    panPerfect.add(perfectFileName_);
    panPerfect.add(perfectField_);
    panPerfect.add(perfectBrowse);
   
//    Box funcButtonBox = Box.createVerticalBox();
//    funcButtonBox.add(Box.createVerticalGlue());
//    funcButtonBox.add(panGraphInfo);
//    funcButtonBox.add(panG1);
//    funcButtonBox.add(panG2);
//    funcButtonBox.add(panAlign);
    
    addWidgetFullRow(panGraphInfo, true);
    addWidgetFullRow(panG1, true);
    addWidgetFullRow(panG2, true);
    addWidgetFullRow(panAlign, true);
  
    //
    // Special layout buttons :: Cliques
    //
    
    if (!forOrphanEdges) { // add perfect alignment button
//      funcButtonBox.add(panPerfect);
      addWidgetFullRow(panPerfect, true);
    }
    
//    GridBagConstraints gbc = new GridBagConstraints();
//    UiUtil.gbcSet(gbc, 0, 0, 5, 1, UiUtil.HOR, 0, 0,
//            5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
//    cp.add(funcButtonBox, gbc);
    
    //
    // OK and Cancel buttons
    //
    
//    buttonO_ = new FixedJButton(rMan.getString("networkAlignment.ok"));
    
    buttonOK_ = finishConstruction().okButton;
    buttonOK_.setEnabled(false);
    buttonOK_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) { // button enabled iff files extracted
        try {
          NetworkAlignmentDialog.this.setVisible(false);
          NetworkAlignmentDialog.this.dispose();
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
    
//    FixedJButton buttonC = new FixedJButton(rMan.getString("networkAlignment.cancel"));
//    buttonC.addActionListener(new ActionListener() {
//      public void actionPerformed(ActionEvent ev) {
//        try {
//          nullifyFiles();
//          NetworkAlignmentDialog.this.setVisible(false);
//          NetworkAlignmentDialog.this.dispose();
//        } catch (Exception ex) {
//          ExceptionHandler.getHandler().displayException(ex);
//        }
//      }
//    });
//
//    List<JButton> list = new ArrayList<JButton>();
//    list.add(buttonC);
//    list.add(buttonO_);
    setLocationRelativeTo(parent);
  
//    finishConstruction();
  
//    JPanel panOptions = new JPanel(new FlowLayout(FlowLayout.LEFT));
//
//    panOptions.add(buttonO_);
//    panOptions.add(Box.createHorizontalStrut(10));
//    panOptions.add(buttonC);
//
//    Box optButtonBox = Box.createHorizontalBox();
//    optButtonBox.add(panOptions);
//
//    UiUtil.gbcSet(gbc, 0, 1, 5, 1, UiUtil.HOR, 0, 0,
//            5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
//    cp.add(optButtonBox, gbc);
    
  }
  
  @Override
  public void okAction() {
    if (filesAreExtracted()) {
      try {
        NetworkAlignmentDialog.this.setVisible(false);
        NetworkAlignmentDialog.this.dispose();
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
    } else { // should never happen because OK button is disabled.
      ResourceManager rMan = ResourceManager.getManager();
      JOptionPane.showMessageDialog(parent_, rMan.getString("networkAlignment.missingFiles"),
                                  rMan.getString("networkAlignment.missingFilesTitle"),
                                  JOptionPane.WARNING_MESSAGE);
    }
  }
  
  @Override
  public void closeAction() {
    try {
      nullifyFiles();
      NetworkAlignmentDialog.this.setVisible(false);
      NetworkAlignmentDialog.this.dispose();
    } catch (Exception ex) {
      ExceptionHandler.getHandler().displayException(ex);
    }
  }
  
  @Override
  protected boolean stashForOK() {
    return false;
  }
  
  /**
   *
   */
  
  private void nullifyFiles() {
    graph1File_ = null;
    graph2File_ = null;
    alignmentFile_ = null;
    perfectAlignFile_ = null;
    return;
  }
  
  /**
   *
   */
  
  private boolean filesAreExtracted() {
    return (graph1File_ != null) && (graph2File_ != null) && (alignmentFile_ != null);
  }
  
  /**
   *
   */
  
  public boolean hasFiles() {
    return (filesAreExtracted());
  }
  
  /**
   *
   */
  
  public NetworkAlignmentDialogInfo getNAInfo() {
    if (graph1File_ == null || graph2File_ == null || alignmentFile_ == null) { // perfect alignment file is optional
      throw new IllegalArgumentException("Graph file(s) or alignment file missing. Perfect alignment is optional.");
    }
    return (new NetworkAlignmentDialogInfo(graph1File_, graph2File_, alignmentFile_, perfectAlignFile_, forOrphanEdge_));
  }
  
  /**
   * * Loads the file
   */
  
  private void loadFromFile(FileIndex mode) {
    CommandSet cmd = CommandSet.getCmds("mainWindow");
    
    File file;
    
    switch (mode) {
      case GRAPH_ONE_FILE:
        file = cmd.getTheFile(".gw", ".sif", "LoadDirectory", "Graph Files (*.gw, *.sif)");
        break;
      case GRAPH_TWO_FILE:
        file = cmd.getTheFile(".gw", ".sif", "LoadDirectory", "Graph Files (*.gw, *.sif)");
        break;
      case ALIGNMENT_FILE:
        file = cmd.getTheFile(".align", null, "LoadDirectory", "filterName.align");
        break;
      case PERFECT_FILE:
        file = cmd.getTheFile(".align", null, "LoadDirectory", "filterName.align");
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
        break;
      default:
        throw new IllegalArgumentException();
    }
    
    if (filesAreExtracted()) { // enable OK button
      buttonOK_.setEnabled(true);
    }
    return;
  }
  
  /**
   * The unread files for G1, G2, and the Alignment
   */
  
  public static class NetworkAlignmentDialogInfo {
    
    public final File graphA, graphB, align, perfect; // graph1 and graph2 can be out of order (size), hence graphA and graphB
    public final boolean forOrphanEdge;
    
    public NetworkAlignmentDialogInfo(File graph1, File graph2, File align, File perfect, boolean forOrphanEdge) {
      this.graphA = graph1;
      this.graphB = graph2;
      this.align = align;
      this.perfect = perfect;
      this.forOrphanEdge = forOrphanEdge;
    }
    
  }
  
}

//  public NetworkAlignmentDialog(JFrame parent, boolean forOrphanEdges) {
//    super(parent, ResourceManager.getManager().getString("networkAlignment.title"), true);
//    this.parent_ = parent;
//    this.forOrphanEdge_ = forOrphanEdges;
//
//    final ResourceManager rMan = ResourceManager.getManager();
//    setSize(600, 400);
//    JPanel cp = (JPanel) getContentPane();
//    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
//    cp.setLayout(new GridBagLayout());
//    GridBagConstraints gbc = new GridBagConstraints();
//
//    //
//    // File Buttons and File Labels
//    //
//
//    JButton graph1Button = new JButton(rMan.getString("networkAlignment.graph1"));
//    JButton graph2Button = new JButton(rMan.getString("networkAlignment.graph2"));
//    JButton alignmentButton = new JButton(rMan.getString("networkAlignment.alignment"));
//
//    graph1FileName_ = new JLabel();
//    graph2FileName_ = new JLabel();
//    alignFileName_ = new JLabel();
//    perfectFileName_ = new JLabel();
//
//    graph1Button.addActionListener(new ActionListener() {
//      public void actionPerformed(ActionEvent e) {
//        try {
//          loadFromFile(FileIndex.GRAPH_ONE_FILE);
//        } catch (Exception ex) {
//          ExceptionHandler.getHandler().displayException(ex);
//        }
//      }
//    });
//
//    graph2Button.addActionListener(new ActionListener() {
//      public void actionPerformed(ActionEvent e) {
//        try {
//          loadFromFile(FileIndex.GRAPH_TWO_FILE);
//        } catch (Exception ex) {
//          ExceptionHandler.getHandler().displayException(ex);
//        }
//      }
//    });
//
//    alignmentButton.addActionListener(new ActionListener() {
//      public void actionPerformed(ActionEvent e) {
//        try {
//          loadFromFile(FileIndex.ALIGNMENT_FILE);
//        } catch (Exception ex) {
//          ExceptionHandler.getHandler().displayException(ex);
//        }
//      }
//    });
//
//    Box funcButtonBox = Box.createVerticalBox();
//    funcButtonBox.add(Box.createVerticalGlue());
//
//    JPanel panGraphInfo = new JPanel(new FlowLayout(FlowLayout.LEFT));
//    panGraphInfo.add(new Label(rMan.getString("networkAlignment.graphsEitherOrder"))); // ADD TO PROPS
//
//    JPanel panG1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
//    panG1.add(graph1Button);
//    panG1.add(graph1FileName_);
//
//    JPanel panG2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
//    panG2.add(graph2Button);
//    panG2.add(graph2FileName_);
//
//    JPanel panAlign = new JPanel(new FlowLayout(FlowLayout.LEFT));
//    panAlign.add(alignmentButton);
//    panAlign.add(alignFileName_);
//
//    funcButtonBox.add(panGraphInfo);
//    funcButtonBox.add(panG1);
//    funcButtonBox.add(panG2);
//    funcButtonBox.add(panAlign);
//
//    //
//    // Special layout buttons :: Cliques
//    //
//
//    if (!forOrphanEdges) { // add perfect alignment button
//      JButton perfectButton = new JButton(rMan.getString("networkAlignment.perfect"));
//      perfectButton.addActionListener(new ActionListener() {
//        public void actionPerformed(ActionEvent e) {
//          try {
//            loadFromFile(FileIndex.PERFECT_FILE);
//          } catch (Exception ex) {
//            ExceptionHandler.getHandler().displayException(ex);
//          }
//        }
//      });
//      JPanel panPerfect = new JPanel(new FlowLayout(FlowLayout.LEFT));
//      panPerfect.add(perfectButton);
//      panPerfect.add(perfectFileName_);
//      funcButtonBox.add(panPerfect);
//    }
//
//    UiUtil.gbcSet(gbc, 0, 0, 5, 1, UiUtil.HOR, 0, 0,
//            5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
//    cp.add(funcButtonBox, gbc);
//
//    //
//    // OK and Cancel buttons
//    //
//
//    buttonO_ = new FixedJButton(rMan.getString("networkAlignment.ok"));
//    buttonO_.setEnabled(false);
//    buttonO_.addActionListener(new ActionListener() {
//      public void actionPerformed(ActionEvent ev) { // button enabled iff files extracted
//        try {
//          NetworkAlignmentDialog.this.setVisible(false);
//          NetworkAlignmentDialog.this.dispose();
//        } catch (Exception ex) {
//          ExceptionHandler.getHandler().displayException(ex);
//        }
//      }
//    });
//
//    FixedJButton buttonC = new FixedJButton(rMan.getString("networkAlignment.cancel"));
//    buttonC.addActionListener(new ActionListener() {
//      public void actionPerformed(ActionEvent ev) {
//        try {
//          nullifyFiles();
//          NetworkAlignmentDialog.this.setVisible(false);
//          NetworkAlignmentDialog.this.dispose();
//        } catch (Exception ex) {
//          ExceptionHandler.getHandler().displayException(ex);
//        }
//      }
//    });
//
//    JPanel panOptions = new JPanel(new FlowLayout(FlowLayout.LEFT));
//
//    panOptions.add(buttonO_);
//    panOptions.add(Box.createHorizontalStrut(10));
//    panOptions.add(buttonC);
//
//    Box optButtonBox = Box.createHorizontalBox();
//    optButtonBox.add(panOptions);
//
//    UiUtil.gbcSet(gbc, 0, 1, 5, 1, UiUtil.HOR, 0, 0,
//            5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
//    cp.add(optButtonBox, gbc);
//
//    setLocationRelativeTo(parent);
//  }