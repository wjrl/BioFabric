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

import org.systemsbiology.biofabric.cmd.CommandSet;
import org.systemsbiology.biofabric.util.ExceptionHandler;
import org.systemsbiology.biofabric.util.FixedJButton;
import org.systemsbiology.biofabric.util.ResourceManager;
import org.systemsbiology.biofabric.util.UiUtil;
import org.systemsbiology.biotapestry.biofabric.FabricCommands;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class NetworkAlignmentDialog extends JDialog {
  
  private enum FileIndex {
    GRAPH_ONE_FILE, GRAPH_TWO_FILE, ALIGNMENT_FILE, PERFECT_FILE
  }
  
  private final boolean forOrphanEdge_;
  private JFrame parent_;
  private JLabel graph1FileName_, graph2FileName_, alignFileName_, perfectFileName_;
  private File graph1File_, graph2File_, alignmentFile_, perfectAlignFile_; // perfect Alignment is optional
  
  public NetworkAlignmentDialog(JFrame parent, boolean forOrphanEdges) {
    super(parent, ResourceManager.getManager().getString("networkAlignment.title"), true);
    this.parent_ = parent;
    this.forOrphanEdge_ = forOrphanEdges;
    
    final ResourceManager rMan = ResourceManager.getManager();
    setSize(450, 400);
    JPanel cp = (JPanel) getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    
    //
    // File Buttons and File Labels
    //
    
    JButton graph1Button = new JButton(rMan.getString("networkAlignment.graph1"));
    JButton graph2Button = new JButton(rMan.getString("networkAlignment.graph2"));
    JButton alignmentButton = new JButton(rMan.getString("networkAlignment.alignment"));
    
    graph1FileName_ = new JLabel();
    graph2FileName_ = new JLabel();
    alignFileName_ = new JLabel();
    perfectFileName_ = new JLabel();
    
    graph1Button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          loadFromFile(FileIndex.GRAPH_ONE_FILE);
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
    
    graph2Button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          loadFromFile(FileIndex.GRAPH_TWO_FILE);
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
    
    alignmentButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          loadFromFile(FileIndex.ALIGNMENT_FILE);
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
    
    Box funcButtonBox = Box.createVerticalBox();
    funcButtonBox.add(Box.createVerticalGlue());
    
    JPanel panGraphInfo = new JPanel(new FlowLayout(FlowLayout.LEFT));
    panGraphInfo.add(new Label(rMan.getString("networkAlignment.graphsEitherOrder"))); // ADD TO PROPS
    
    JPanel panG1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
    panG1.add(graph1Button);
    panG1.add(graph1FileName_);
  
    JPanel panG2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
    panG2.add(graph2Button);
    panG2.add(graph2FileName_);
  
    JPanel panAlign = new JPanel(new FlowLayout(FlowLayout.LEFT));
    panAlign.add(alignmentButton);
    panAlign.add(alignFileName_);
    
    funcButtonBox.add(panGraphInfo);
    funcButtonBox.add(panG1);
    funcButtonBox.add(panG2);
    funcButtonBox.add(panAlign);
  
    //
    // Special layout buttons :: Cliques
    //
    
    if (!forOrphanEdges) { // add perfect alignment button
      JButton perfectButton = new JButton(rMan.getString("networkAlignment.perfect"));
      perfectButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          try {
            loadFromFile(FileIndex.PERFECT_FILE);
          } catch (Exception ex) {
            ExceptionHandler.getHandler().displayException(ex);
          }
        }
      });
      JPanel panPerfect = new JPanel(new FlowLayout(FlowLayout.LEFT));
      panPerfect.add(perfectButton);
      panPerfect.add(perfectFileName_);
      funcButtonBox.add(panPerfect);
    }
  
    UiUtil.gbcSet(gbc, 0, 0, 5, 1, UiUtil.HOR, 0, 0,
            5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    cp.add(funcButtonBox, gbc);
    
    //
    // OK and Cancel buttons
    //
    
    FixedJButton buttonO = new FixedJButton(rMan.getString("networkAlignment.ok"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          if (filesAreExtracted()) {
            NetworkAlignmentDialog.this.setVisible(false);
            NetworkAlignmentDialog.this.dispose();
          } else {
            JOptionPane.showMessageDialog(parent_, rMan.getString("networkAlignment.missingFiles"),
                    rMan.getString("networkAlignment.missingFilesTitle"),
                    JOptionPane.WARNING_MESSAGE);
          }
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
    
    FixedJButton buttonC = new FixedJButton(rMan.getString("networkAlignment.cancel"));
    buttonC.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          nullifyFiles();
          NetworkAlignmentDialog.this.setVisible(false);
          NetworkAlignmentDialog.this.dispose();
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
  
    JPanel panOptions = new JPanel(new FlowLayout(FlowLayout.LEFT));
    
    panOptions.add(buttonO);
    panOptions.add(Box.createHorizontalStrut(10));
    panOptions.add(buttonC);
    
    Box optButtonBox = Box.createHorizontalBox();
    optButtonBox.add(panOptions);
    
    UiUtil.gbcSet(gbc, 0, 1, 5, 1, UiUtil.HOR, 0, 0,
            5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    cp.add(optButtonBox, gbc);
    
    setLocationRelativeTo(parent);
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
        file = cmd.getTheFile(".gw", ".sif", "AttribDirectory", "Graph Files (*.gw, *.sif)");
        break;
      case GRAPH_TWO_FILE:
        file = cmd.getTheFile(".gw", ".sif", "AttribDirectory", "Graph Files (*.gw, *.sif)");
        break;
      case ALIGNMENT_FILE:
        file = cmd.getTheFile(".align", null, "AttribDirectory", "filterName.align");
        break;
      case PERFECT_FILE:
        file = cmd.getTheFile(".align", null, "AttribDirectory", "filterName.align");
        break;
      default:
        throw new IllegalArgumentException();
    }
    
    if (file == null) {
      return;
    }
    
    FabricCommands.setPreference("LoadDirectory", file.getAbsoluteFile().getParent());
    
    switch (mode) { // CAN I COMBINE THIS WITH THE TOP SWITCH CLAUSE???
      case GRAPH_ONE_FILE:
        graph1FileName_.setText(file.getName());
        graph1File_ = file;
        break;
      case GRAPH_TWO_FILE:
        graph2FileName_.setText(file.getName());
        graph2File_ = file;
        break;
      case ALIGNMENT_FILE:
        alignFileName_.setText(file.getName());
        alignmentFile_ = file;
        break;
      case PERFECT_FILE:
        perfectFileName_.setText(file.getName());
        perfectAlignFile_ = file;
        break;
      default:
        throw new IllegalArgumentException();
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
