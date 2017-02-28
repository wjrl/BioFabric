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

import static org.systemsbiology.biofabric.analysis.NetworkAlignment.*;

public class NetworkAlignmentDialog extends JDialog {
  
  private final int GRAPH_ONE_FILE = 1;
  private final int GRAPH_TWO_FILE = 2;
  private final int ALIGNMENT_FILE = 3;
  
  private JFrame parent_;

  private GraphNA graph1_, graph2_;
  private AlignmentNA alignment_;
  
  public NetworkAlignmentDialog(final JFrame parent) {
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
          loadFromFile(GRAPH_ONE_FILE);
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
    
    graph2Button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          loadFromFile(GRAPH_TWO_FILE);
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
    
    alignmentButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          loadFromFile(ALIGNMENT_FILE);
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
    
    UiUtil.gbcSet(gbc, 0, 0, 5, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
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
    
    UiUtil.gbcSet(gbc, 0, 1, 5, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
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
      throw new IllegalArgumentException();
    }
    return new NetworkAlignInfo(graph1_, graph2_, alignment_);
  }
  
  /**
   * * Loads the file
   */
  
  private void loadFromFile(int mode) {
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
    
    if (! check(file)) {
      
    }
    
    switch (mode) {
      case GRAPH_ONE_FILE:
        graph1_ = GraphNA.readGraphGWFile(file);
        break;
      case GRAPH_TWO_FILE:
        graph2_ = GraphNA.readGraphGWFile(file);
        break;
      case ALIGNMENT_FILE:
        alignment_ = AlignmentNA.readAlignFile(file);
        break;
      default:
        throw new IllegalArgumentException();
    }
    
    return;
  }
  
  private boolean check(File file) { //  TO CHECK FILE
    return true;
  }
  
}


///*
//**    Copyright (C) 2003-2014 Institute for Systems Biology
//**                            Seattle, Washington, USA.
//**
//**    This library is free software; you can redistribute it and/or
//**    modify it under the terms of the GNU Lesser General Public
//**    License as published by the Free Software Foundation; either
//**    version 2.1 of the License, or (at your option) any later version.
//**
//**    This library is distributed in the hope that it will be useful,
//**    but WITHOUT ANY WARRANTY; without even the implied warranty of
//**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//**    Lesser General Public License for more details.
//**
//**    You should have received a copy of the GNU Lesser General Public
//**    License along with this library; if not, write to the Free Software
//**    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//*/
//
//package org.systemsbiology.biofabric.ui.dialogs;
//
//        import org.systemsbiology.biofabric.analysis.NetworkAlignment;
//        import org.systemsbiology.biofabric.cmd.CommandSet;
//        import org.systemsbiology.biofabric.util.ExceptionHandler;
//        import org.systemsbiology.biofabric.util.FixedJButton;
//        import org.systemsbiology.biofabric.util.ResourceManager;
//        import org.systemsbiology.biofabric.util.UiUtil;
//
//        import javax.swing.*;
//        import javax.swing.border.EmptyBorder;
//        import java.awt.*;
//        import java.awt.event.ActionEvent;
//        import java.awt.event.ActionListener;
//        import java.io.File;
//
//        import static org.systemsbiology.biofabric.analysis.NetworkAlignment.GraphNA;
//        import static org.systemsbiology.biofabric.analysis.NetworkAlignment.AlignmentNA;
//        import static org.systemsbiology.biofabric.analysis.NetworkAlignment.NetworkAlignInfo;
//
//public class NetworkAlignmentDialog extends JDialog {
//
//  private final int GRAPH_ONE_FILE = 1;
//  private final int GRAPH_TWO_FILE = 2;
//  private final int ALIGNMENT_FILE = 3;
//
//  private JFrame parent_;
//
//  private GraphNA graph1_, graph2_;
//  private AlignmentNA alignment_;
//
//  public NetworkAlignmentDialog(final JFrame parent) {
//    super(parent, ResourceManager.getManager().getString("networkAlignment.title"), true);
//    this.parent_ = parent;
//
//    ResourceManager rMan = ResourceManager.getManager();
//    setSize(450, 400);
//    JPanel cp = (JPanel) getContentPane();
//    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
//    cp.setLayout(new GridBagLayout());
//    GridBagConstraints gbc = new GridBagConstraints();
//
////    JLabel graph_1 = new JLabel(rMan.getString("networkAlignment.graph1"));
////    JLabel graph_2 = new JLabel(rMan.getString("networkAlignment.graph2"));
////    JLabel alignment = new JLabel(rMan.getString("networkAlignment.alignment"));
//
//    JButton graph1Button = new JButton(rMan.getString("networkAlignment.graph1"));
//    JButton graph2Button = new JButton(rMan.getString("networkAlignment.graph2"));
//    JButton alignmentButton = new JButton(rMan.getString("networkAlignment.alignment"));
//
//    graph1Button.addActionListener(new ActionListener() {
//      public void actionPerformed(ActionEvent e) {
//        try {
//          loadFromFile(GRAPH_ONE_FILE);
//        } catch (Exception ex) {
//          ExceptionHandler.getHandler().displayException(ex);
//        }
//      }
//    });
//
//    graph2Button.addActionListener(new ActionListener() {
//      public void actionPerformed(ActionEvent e) {
//        try {
//          loadFromFile(GRAPH_TWO_FILE);
//        } catch (Exception ex) {
//          ExceptionHandler.getHandler().displayException(ex);
//        }
//      }
//    });
//
//    alignmentButton.addActionListener(new ActionListener() {
//      public void actionPerformed(ActionEvent e) {
//        try {
//          loadFromFile(ALIGNMENT_FILE);
//        } catch (Exception ex) {
//          ExceptionHandler.getHandler().displayException(ex);
//        }
//      }
//    });
//
//    Box funcButtonBox = Box.createVerticalBox();
//    funcButtonBox.add(Box.createVerticalGlue());
//    funcButtonBox.add(graph1Button);
//    funcButtonBox.add(Box.createVerticalStrut(10));
//    funcButtonBox.add(graph2Button);
//    funcButtonBox.add(Box.createVerticalStrut(10));
//    funcButtonBox.add(alignmentButton);
//
//    UiUtil.gbcSet(gbc, 0, 0, 5, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
//    cp.add(funcButtonBox, gbc);
//
//    FixedJButton buttonO = new FixedJButton(rMan.getString("networkAlignment.ok"));
//    buttonO.addActionListener(new ActionListener() {
//      public void actionPerformed(ActionEvent ev) {
//        try {
//          if (filesAreExtracted()) {
//            NetworkAlignmentDialog.this.setVisible(false);
//            NetworkAlignmentDialog.this.dispose();
//          } else {
//            JOptionPane.showMessageDialog(parent_, "Invalid");
//          }
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
//          NetworkAlignmentDialog.this.setVisible(false);
//          NetworkAlignmentDialog.this.dispose();
//        } catch (Exception ex) {
//          ExceptionHandler.getHandler().displayException(ex);
//        }
//      }
//    });
//
//    Box optButtonBox = Box.createHorizontalBox();
//    optButtonBox.add(buttonO);
//    optButtonBox.add(Box.createHorizontalStrut(10));
//    optButtonBox.add(buttonC);
//
//    UiUtil.gbcSet(gbc, 0, 1, 5, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
//    cp.add(optButtonBox, gbc);
//
//
//    setLocationRelativeTo(parent);
//  }
//
//  /**
//   *
//   */
//
//  private boolean filesAreExtracted() {
//    return (graph1_ != null) && (graph2_ != null) && (alignment_ != null);
//  }
//
//  /**
//   *
//   */
//
//  public NetworkAlignInfo getNAInfo() {
//    return new NetworkAlignInfo(graph1_, graph2_, alignment_);
//  }
//
////  /**
////   *
////   */
////
////  public File getGraph1() {
////    return graph1_;
////  }
////
////  public File getGraph2() {
////    return graph2_;
////  }
////
////  public File getAlignment() {
////    return alignment_;
////  }
//
//  /**
//   * * Loads the file
//   */
//
//  private void loadFromFile(int mode) {
//    CommandSet cmd = CommandSet.getCmds("mainWindow");
//
//    File file;
//
//    switch (mode) {
//      case GRAPH_ONE_FILE:
//        file = cmd.getTheFile(".gw", null, "AttribDirectory", "filterName.gw");
//        break;
//      case GRAPH_TWO_FILE:
//        file = cmd.getTheFile(".gw", null, "AttribDirectory", "filterName.gw");
//      case ALIGNMENT_FILE:
//        file = cmd.getTheFile(".align", null, "AttribDirectory", "filterName.align");
//        break;
//      default:
//        throw new IllegalArgumentException();
//    }
//
//    if (file == null) {
//      return;
//    }
//
//    if (!check(file)) {
//
//    }
//
//    switch (mode) {
//      case GRAPH_ONE_FILE:
//        graph1_ = new GraphNA(file);
//        break;
//      case GRAPH_TWO_FILE:
//        graph2_ = new GraphNA(file);
//      case ALIGNMENT_FILE:
//        alignment_ = new AlignmentNA(file);
//        break;
//      default:
//        throw new IllegalArgumentException();
//    }
//
//    return;
//  }
//
//  private boolean check(File file) { //  TO CHECK FILE
//    return true;
//  }
//
////  /**
////   * Loads the .gw graph (2) file
////   */
////
////  private void loadGraphTwoFile() {
////    CommandSet cmd = CommandSet.getCmds("mainWindow");
////    File fileEda = cmd.getTheFile(".gw", null, "AttribDirectory", "filterName.gw");
//////    if (fileEda == null) { // NEED TO FIX THIS
//////      return;
//////    }
////
////    graph2_ = fileEda;
////  }
////
////  /**
////   * Loads the .align alignment file
////   */
////
////  private void loadAlignmentFile() {
////    CommandSet cmd = CommandSet.getCmds("mainWindow");
////    File fileEda = cmd.getTheFile(".align", null, "AttribDirectory", "filterName.align");
//////    if (fileEda == null) { // NEED TO FIX THIS
//////      return;
//////    }
////
////    alignment_ = fileEda;
////  }
//
//}

