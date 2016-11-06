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

import org.systemsbiology.biofabric.analysis.NetworkAlignment;
import org.systemsbiology.biofabric.cmd.CommandSet;
import org.systemsbiology.biofabric.ui.dialogs.utils.DialogSupport;
import org.systemsbiology.biofabric.util.ExceptionHandler;
import org.systemsbiology.biofabric.util.FixedJButton;
import org.systemsbiology.biofabric.util.ResourceManager;
import org.systemsbiology.biofabric.util.UiUtil;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class NetworkAlignmentDialog extends JDialog implements DialogSupport.DialogSupportClient {
  
  private File graph1, graph2, alignment;
  
  public NetworkAlignmentDialog(JFrame parent) {
    super(parent, ResourceManager.getManager().getString("networkAlignment.title"), true);
    
    ResourceManager rMan = ResourceManager.getManager();
    setSize(450, 400);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    
//    JLabel graph_1 = new JLabel(rMan.getString("networkAlignment.graph1"));
//    JLabel graph_2 = new JLabel(rMan.getString("networkAlignment.graph2"));
//    JLabel alignment = new JLabel(rMan.getString("networkAlignment.alignment"));
    
    JButton graph_1 = new JButton(rMan.getString("networkAlignment.graph1"));
    JButton graph_2 = new JButton(rMan.getString("networkAlignment.graph2"));
    JButton alignment = new JButton(rMan.getString("networkAlignment.alignment"));
  
    graph_1.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          loadGraphOneFile();
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
  
    graph_2.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          loadGraphTwoFile();
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
  
    alignment.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          loadAlignmentFile();
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
    
    Box buttonBox = Box.createVerticalBox();
    buttonBox.add(Box.createVerticalGlue());
    buttonBox.add(graph_1);
    buttonBox.add(Box.createVerticalStrut(10));
    buttonBox.add(graph_2);
    buttonBox.add(Box.createVerticalStrut(10));
    buttonBox.add(alignment);
  
    UiUtil.gbcSet(gbc, 0, 8, 10, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    cp.add(buttonBox, gbc);
  
    FixedJButton buttonO = new FixedJButton(rMan.getString("exportDialog.ok"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          if (extractFiles()) { //  NEED TO FIX THIS
            NetworkAlignmentDialog.this.setVisible(false);
            NetworkAlignmentDialog.this.dispose();
          }
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
    
    cp.add(buttonO);
    
  
    setLocationRelativeTo(parent);
  }
  
  @Override
  public void applyAction() {
    
  }
  
  @Override
  public void okAction() {
    
  }
  
  @Override
  public void closeAction() {
    
  }
  
  /**
   *
   */
  
  private boolean extractFiles() {
    return (graph1 != null) && (graph2 != null) && (alignment != null);
  }
  
  /**
   *
   */
  
  public File getGraph1() {
    return graph1;
  }
  
  public File getGraph2() {
    return graph2;
  }
  
  public File getAlignment() {
    return alignment;
  }
  
  /**
   * Loads the .gw graph (1) file
   */
  
  private void loadGraphOneFile() {
    CommandSet cmd = CommandSet.getCmds("mainWindow");
    File fileEda = cmd.getTheFile(".gw", null, "AttribDirectory", "filterName.gw");
//    if (fileEda == null) { // NEED TO FIX THIS
//      return;
//    }
    
    graph1 = fileEda;
  }
  
  /**
   * Loads the .gw graph (2) file
   */
  
  private void loadGraphTwoFile() {
    CommandSet cmd = CommandSet.getCmds("mainWindow");
    File fileEda = cmd.getTheFile(".gw", null, "AttribDirectory", "filterName.gw");
//    if (fileEda == null) { // NEED TO FIX THIS
//      return;
//    }
    
    graph2 = fileEda;
  }
  
  /**
   * Loads the .align alignment file
   */
  
  private void loadAlignmentFile() {
    CommandSet cmd = CommandSet.getCmds("mainWindow");
    File fileEda = cmd.getTheFile(".align", null, "AttribDirectory", "filterName.align");
//    if (fileEda == null) { // NEED TO FIX THIS
//      return;
//    }
    
    alignment = fileEda;
  }
  
}
