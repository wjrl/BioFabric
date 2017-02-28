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

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.util.ExceptionHandler;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ResourceManager;


/****************************************************************************
**
** This is the navigator
*/

public class FabricNavTool extends JPanel {
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private BioFabricPanel bfp_;
  private FixedJButton startAtCurrent_;    
  private FixedJButton chooseAStart_;      
  private JLabel nodeName_;  
  private JLabel linkName_;  
  private FixedJButton buttonUp_;    
  private FixedJButton buttonRight_;    
  private FixedJButton buttonFarRight_;      
  private FixedJButton buttonLeft_;
  private FixedJButton buttonFarLeft_; 
  private FixedJButton buttonDown_; 
  private FixedJButton buttonZoom_;
  private FixedJButton buttonLinkZone_;
  private FixedJButton buttonClear_;  
  private JCheckBox selectedOnly_;  
  private BioFabricPanel.TourStatus currTstat_;
  private boolean haveSelection_;
  private boolean haveAModel_;
  private boolean controlsEnabled_;
  private JFrame topWindow_;
  private boolean doSOReset_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public FabricNavTool(JFrame topWindow) {
    ResourceManager rMan = ResourceManager.getManager();    

    setBorder(new EmptyBorder(20, 20, 20, 20));
    setLayout(new GridLayout(1, 1));
    setBackground(Color.WHITE);
    nodeName_ = new JLabel("", JLabel.CENTER);
    linkName_ = new JLabel("", JLabel.CENTER);
    controlsEnabled_ = true;
    topWindow_ = topWindow;
    doSOReset_ = false;
    
    startAtCurrent_ = new FixedJButton(rMan.getString("navTool.startAtCurrent"));
    startAtCurrent_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          BioFabricPanel.TourStatus tstat = bfp_.startTourFromSelection(calcSelectedOnly());
          installNames(tstat);
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
    startAtCurrent_.setEnabled(false);
        
    chooseAStart_ = new FixedJButton(rMan.getString("navTool.chooseAStart"));
    chooseAStart_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          bfp_.setToCollectTourStart(calcSelectedOnly());
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
    chooseAStart_.setEnabled(false);
    haveAModel_ = false;
 
    buttonUp_ = new FixedJButton(rMan.getString("navTool.up"));
    buttonUp_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          BioFabricPanel.TourStatus tstat = bfp_.goUp(calcSelectedOnly());
          installNames(tstat);
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
    
    buttonRight_ = new FixedJButton(rMan.getString("navTool.right"));
    buttonRight_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          BioFabricPanel.TourStatus tstat = bfp_.goRight(calcSelectedOnly());
          installNames(tstat);
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
    
    buttonFarRight_ = new FixedJButton(">>");
    buttonFarRight_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          BioFabricPanel.TourStatus tstat = bfp_.goFarRight(calcSelectedOnly());
          installNames(tstat);
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
      
    buttonLeft_ = new FixedJButton(rMan.getString("navTool.left"));
    buttonLeft_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          BioFabricPanel.TourStatus tstat = bfp_.goLeft(calcSelectedOnly());
          installNames(tstat);
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
    
    buttonFarLeft_ = new FixedJButton("<<");
    buttonFarLeft_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          BioFabricPanel.TourStatus tstat = bfp_.goFarLeft(calcSelectedOnly());
          installNames(tstat);
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
           
    buttonDown_ = new FixedJButton(rMan.getString("navTool.down"));
    buttonDown_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          BioFabricPanel.TourStatus tstat = bfp_.goDown(calcSelectedOnly());
          installNames(tstat);
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });    
    
    buttonClear_ = new FixedJButton(rMan.getString("navTool.clear"));
    buttonClear_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          bfp_.clearTour();
          clearTour();
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });    
    
    buttonZoom_ = new FixedJButton(rMan.getString("navTool.zoom"));
    buttonZoom_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          bfp_.zoomToTourStop();
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
    
    buttonLinkZone_ = new FixedJButton(rMan.getString("navTool.dropZone"));
    buttonLinkZone_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          BioFabricPanel.TourStatus tstat = bfp_.tourToDrainZone(calcSelectedOnly());
          installNames(tstat);
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
    
    selectedOnly_ = new JCheckBox(rMan.getString("navTool.skip"));
    selectedOnly_.setOpaque(true);
    selectedOnly_.setBackground(Color.white);
    selectedOnly_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          if (doSOReset_) {
            return;
          }
          if ((currTstat_ != null) && (currTstat_.currStopUnselected) && selectedOnly_.isSelected()) {
            ResourceManager rMan = ResourceManager.getManager();    
            JOptionPane.showMessageDialog(topWindow_, rMan.getString("navTool.currLocUnselected"),
                                          rMan.getString("navTool.currLocUnselectedTitle"),
                                          JOptionPane.ERROR_MESSAGE);
            doSOReset_ = true;
            selectedOnly_.setSelected(false);
            doSOReset_ = false;
            return;
          }
          BioFabricPanel.TourStatus tstat = bfp_.getTourDirections(selectedOnly_.isSelected());
          installNames(tstat);
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
    haveSelection_ = false;
    selectedOnly_.setEnabled(false);
    
      
    Box cbuttonPanel = Box.createHorizontalBox();
    cbuttonPanel.add(Box.createHorizontalGlue()); 
    cbuttonPanel.add(startAtCurrent_);        
    cbuttonPanel.add(Box.createHorizontalStrut(10));
    cbuttonPanel.add(chooseAStart_);        
    cbuttonPanel.add(Box.createHorizontalGlue());
       
    Box nodePanel = Box.createHorizontalBox();
    nodePanel.add(Box.createHorizontalStrut(10));
    nodePanel.add(nodeName_);
    nodePanel.add(Box.createHorizontalStrut(10));

    Box linkPanel = Box.createHorizontalBox();
    linkPanel.add(Box.createHorizontalStrut(10));
    linkPanel.add(linkName_);
    linkPanel.add(Box.createHorizontalStrut(10));
    
    Box dbuttonPanel = Box.createHorizontalBox();
    dbuttonPanel.add(Box.createHorizontalGlue()); 
    dbuttonPanel.add(buttonUp_);
    dbuttonPanel.add(Box.createHorizontalGlue());
    
    Box ebuttonPanel = Box.createHorizontalBox();
    ebuttonPanel.add(Box.createHorizontalGlue());
    ebuttonPanel.add(buttonFarLeft_);
    ebuttonPanel.add(Box.createHorizontalStrut(10));
    ebuttonPanel.add(buttonLeft_);
    ebuttonPanel.add(Box.createHorizontalStrut(10));
    ebuttonPanel.add(buttonRight_);
    ebuttonPanel.add(Box.createHorizontalStrut(10));
    ebuttonPanel.add(buttonFarRight_);
    ebuttonPanel.add(Box.createHorizontalGlue());

    Box fbuttonPanel = Box.createHorizontalBox();
    fbuttonPanel.add(Box.createHorizontalGlue()); 
    fbuttonPanel.add(buttonDown_);
    fbuttonPanel.add(Box.createHorizontalGlue());
    
    Box hbuttonPanel = Box.createHorizontalBox();
    hbuttonPanel.add(Box.createHorizontalGlue()); 
    hbuttonPanel.add(buttonClear_);        
    hbuttonPanel.add(Box.createHorizontalStrut(10));
    hbuttonPanel.add(buttonZoom_);  
    hbuttonPanel.add(Box.createHorizontalStrut(10));
    hbuttonPanel.add(buttonLinkZone_);  
    hbuttonPanel.add(Box.createHorizontalGlue());
 
    Box ibuttonPanel = Box.createHorizontalBox();
    ibuttonPanel.add(Box.createHorizontalGlue()); 
    ibuttonPanel.add(selectedOnly_);
    ibuttonPanel.add(Box.createHorizontalGlue());    
    
    Box gbuttonPanel = Box.createVerticalBox();
    gbuttonPanel.add(Box.createVerticalGlue());
    gbuttonPanel.add(cbuttonPanel);    
    gbuttonPanel.add(Box.createVerticalStrut(10));
    gbuttonPanel.add(nodePanel);    
    gbuttonPanel.add(Box.createVerticalStrut(10));
    gbuttonPanel.add(linkPanel);
    gbuttonPanel.add(Box.createVerticalStrut(10));
    gbuttonPanel.add(dbuttonPanel);
    gbuttonPanel.add(Box.createVerticalStrut(10));
    gbuttonPanel.add(dbuttonPanel);
    gbuttonPanel.add(Box.createVerticalStrut(10));
    gbuttonPanel.add(ebuttonPanel);
    gbuttonPanel.add(Box.createVerticalStrut(10));
    gbuttonPanel.add(fbuttonPanel);
    gbuttonPanel.add(Box.createVerticalStrut(10));
    gbuttonPanel.add(hbuttonPanel);
    gbuttonPanel.add(Box.createVerticalStrut(10));
    gbuttonPanel.add(ibuttonPanel);
    gbuttonPanel.add(Box.createVerticalGlue());
      
    add(gbuttonPanel);
    clearTour();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Reset the skip selections
  */

  public void resetSkipSelections() {
    selectedOnly_.setSelected(false);
    return;
  }    
  
  /***************************************************************************
  **
  ** Enable or disable the panel
  */

  public void enableControls(boolean enabled) {
    controlsEnabled_ = enabled;
    if (!enabled) {
      disableControls();
    } else {
      syncToState();
    }
    return;
  }    
         
  /***************************************************************************
  **
  ** Let us know we have a model
  */

  public void haveAModel(boolean haveIt) {
    haveAModel_ = haveIt;    
    chooseAStart_.setEnabled(controlsEnabled_ && haveAModel_);
    if (!haveAModel_) {
      currTstat_ = null;
      disableControls();
    }
    repaint();
    return;
  }
  /***************************************************************************
  **
  ** Let us know we have a selection
  */

  public void haveASelection(boolean haveIt) {
    startAtCurrent_.setEnabled(haveIt);
    selectedOnly_.setEnabled(haveIt);    
    if (currTstat_ != null) {
      currTstat_.currStopUnselected = bfp_.tourStopNowUnselected();
    }
    haveSelection_ = haveIt;
    repaint();
    return;
  }
  
  /***************************************************************************
  **
  ** Enable/disable tour
  */

  private void clearTour() {
    nodeName_.setText("");
    nodeName_.invalidate();
    linkName_.setText("");
    linkName_.invalidate();
    revalidate();
    disableControls();
    return;
  } 
  
  /***************************************************************************
  **
  ** Give valid answer to use selected only
  */

  private boolean calcSelectedOnly() {
    return (selectedOnly_.isEnabled() && selectedOnly_.isSelected());
  } 

  /***************************************************************************
  **
  ** Drawing routine
  */

  public void setFabricPanel(BioFabricPanel bfp) {
    bfp_ = bfp;
    return;
  }
  
  /***************************************************************************
  **
  ** install Names
  */

  public void installNames(BioFabricPanel.TourStatus tstat) {
    if (tstat == null) {
      return;       
    } 
    currTstat_ = (BioFabricPanel.TourStatus)tstat.clone();
    syncToState();
    return;
  } 
  
  /***************************************************************************
  **
  ** Enable/disable tour
  */

  private void disableControls() {
    buttonUp_.setEnabled(false); 
    buttonRight_.setEnabled(false); 
    buttonFarRight_.setEnabled(false); 
    buttonLeft_.setEnabled(false); 
    buttonFarLeft_.setEnabled(false); 
    buttonDown_.setEnabled(false);
    buttonClear_.setEnabled(false); 
    buttonZoom_.setEnabled(false);
    buttonLinkZone_.setEnabled(false);
    nodeName_.setEnabled(false);
    linkName_.setEnabled(false);
    selectedOnly_.setEnabled(false);
    repaint();
    return;
  }   

  /***************************************************************************
  **
  ** Sync to state!
  */

  private void syncToState() {
    chooseAStart_.setEnabled(haveAModel_);
    if (currTstat_ == null) {
      disableControls();
      return;
    }    
    buttonZoom_.setEnabled(true);
    buttonLinkZone_.setEnabled(true);
    buttonClear_.setEnabled(true);
    nodeName_.setEnabled(true);
    linkName_.setEnabled(true);
    nodeName_.setText(currTstat_.nodeName);
    linkName_.setText(currTstat_.linkName);
    nodeName_.invalidate();
    linkName_.invalidate();
    buttonUp_.setEnabled(currTstat_.upEnabled);   
    buttonRight_.setEnabled(currTstat_.rightEnabled);    
    buttonFarRight_.setEnabled(currTstat_.farRightEnabled);   
    buttonLeft_.setEnabled(currTstat_.leftEnabled);   
    buttonFarLeft_.setEnabled(currTstat_.farLeftEnabled);   
    buttonDown_.setEnabled(currTstat_.downEnabled);
    selectedOnly_.setEnabled(haveSelection_);
    startAtCurrent_.setEnabled(haveSelection_);
    revalidate();
    repaint();
    return;
  }   
}
