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

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class NetworkAlignmentDialog extends JDialog {
  
  private enum FileIndex {
    GRAPH_ONE_FILE, GRAPH_TWO_FILE, ALIGNMENT_FILE
  }
  
  private JFrame parent_;
  private File graph1_, graph2_, alignment_;
  private boolean forClique_;
  
  public NetworkAlignmentDialog(JFrame parent) {
    super(parent, ResourceManager.getManager().getString("networkAlignment.title"), true);
    this.parent_ = parent;
    
    ResourceManager rMan = ResourceManager.getManager();
    setSize(450, 400);
    JPanel cp = (JPanel) getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    
    JButton graph1Button = new JButton(rMan.getString("networkAlignment.graph1"));
    JButton graph2Button = new JButton(rMan.getString("networkAlignment.graph2"));
    JButton alignmentButton = new JButton(rMan.getString("networkAlignment.alignment"));
    
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
    funcButtonBox.add(graph1Button);
    funcButtonBox.add(Box.createVerticalStrut(10));
    funcButtonBox.add(graph2Button);
    funcButtonBox.add(Box.createVerticalStrut(10));
    funcButtonBox.add(alignmentButton);
    
    final JCheckBox clique = new JCheckBox("Clique analysis mode", false);
    // NEED TO ADD RESOURCE MANAGER STRING TO PROPS
    funcButtonBox.add(clique);
    clique.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          forClique_ = clique.isSelected();
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
    
    UiUtil.gbcSet(gbc, 0, 0, 5, 1, UiUtil.HOR, 0, 0,
            5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    cp.add(funcButtonBox, gbc);
    
    FixedJButton buttonO = new FixedJButton(rMan.getString("networkAlignment.ok"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          if (filesAreExtracted()) {
            NetworkAlignmentDialog.this.setVisible(false);
            NetworkAlignmentDialog.this.dispose();
          } else {
            JOptionPane.showMessageDialog(parent_, "Invalid");
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
          NetworkAlignmentDialog.this.setVisible(false);
          NetworkAlignmentDialog.this.dispose();
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
    
    Box optButtonBox = Box.createHorizontalBox();
    optButtonBox.add(buttonO);
    optButtonBox.add(Box.createHorizontalStrut(10));
    optButtonBox.add(buttonC);
    
    UiUtil.gbcSet(gbc, 0, 1, 5, 1, UiUtil.HOR, 0, 0,
            5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    cp.add(optButtonBox, gbc);
    
    setLocationRelativeTo(parent);
  }
  
  /**
   *
   */
  
  private boolean filesAreExtracted() {
    return (graph1_ != null) && (graph2_ != null) && (alignment_ != null);
  }
  
  /**
   *
   */
  
  public NetworkAlignInfo getNAInfo() {
    if (graph1_ == null || graph2_ == null || alignment_ == null) {
      throw new IllegalArgumentException("One or more files missing");
    }
    return new NetworkAlignInfo(graph1_, graph2_, alignment_, forClique_);
  }
  
  /**
   * * Loads the file
   */
  
  private void loadFromFile(FileIndex mode) {
    CommandSet cmd = CommandSet.getCmds("mainWindow");
    
    File file;
    
    switch (mode) {
      case GRAPH_ONE_FILE:
        file = cmd.getTheFile(".gw", null, "AttribDirectory", "filterName.gw");
        break;
      case GRAPH_TWO_FILE:
        file = cmd.getTheFile(".gw", null, "AttribDirectory", "filterName.gw");
        break;
      case ALIGNMENT_FILE:
        file = cmd.getTheFile(".align", null, "AttribDirectory", "filterName.align");
        break;
      default:
        throw new IllegalArgumentException();
    }
    
    if (file == null) {
      return;
    }
    
    switch (mode) { // CAN I COMBINE THIS WITH THE TOP SWITCH CLAUSE???
      case GRAPH_ONE_FILE:
        graph1_ = file;
        break;
      case GRAPH_TWO_FILE:
        graph2_ = file;
        break;
      case ALIGNMENT_FILE:
        alignment_ = file;
        break;
      default:
        throw new IllegalArgumentException();
    }
    
    return;
  }
  
  /**
   * The unread files for G1, G2, and the Alignment
   */
  
  public static class NetworkAlignInfo {
    
    public final File graph1, graph2, align;
    public final boolean forClique;
    
    public NetworkAlignInfo(File graph1, File graph2, File align, boolean forClique) {
      this.graph1 = graph1;
      this.graph2 = graph2;
      this.align = align;
      this.forClique = forClique;
    }
    
  }
  
}
