package com.apkscanner.gui.dialog.install;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.apkscanner.core.AdbWrapper;
import com.apkscanner.gui.ApkInstaller;
import com.apkscanner.gui.ApkInstaller.InstallDlgFuncListener;
import com.apkscanner.gui.dialog.install.*;
import com.apkscanner.gui.util.ArrowTraversalPane;
import com.apkscanner.resource.Resource;
import com.apkscanner.test.ProgressBarTest;
import com.apkscanner.util.Log;

public class InstallDlg extends JDialog implements ActionListener{
	
	InstallCheckTable TestTable;
	DeviceListPanel deviceListDig;
	JScrollPane scrollPane;
	JPanel framelayout;
	JFrame f;
	JTextArea taskOutput;
	static private InstallDlgFuncListener CoreInstallLitener; 
	InstallDlg dlg;
	JPanel MessageBox;
	static JOptionPane newOption;
	
	final String[] checkPackDelOptions = {Resource.STR_BTN_OPEN.getString(), Resource.STR_BTN_INSTALL.getString(), Resource.STR_BTN_DEL.getString(), Resource.STR_BTN_CANCEL.getString()};
	
	public InstallDlgFuncListener getInstallDlgFuncListener() {
		return this.CoreInstallLitener;
	}
	public InstallDlg() {
		createAndShowGUI();
		
		CoreInstallLitener = new InstallDlgFuncListener() {
			Runnable runThread;
			int QuestionResult;			
			@Override
			public void AddLog(String str) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void AddCheckList(String name,String t) {
				// TODO Auto-generated method stub
				TestTable.addTableModel(name,t);
			}

			@Override
			public void Complete(String str) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void ShowDeviceList() {
				
			}
			
			@Override
			public int ShowQuestion(Runnable runthread, Object message, String title, int optionType, int messageType, Icon icon, Object[] options, Object initialValue) {
				// TODO Auto-generated method stub
				this.runThread = runthread;
				Log.d("ShowQuestion");				
				
				JButton[] btn = new JButton[options.length];
				
				for( int i=0; options.length > i; i++ ) {
					btn[i] = new JButton(options[i].toString());
					btn[i].addActionListener(new AlertButtonListener());

				}
				
				newOption = ArrowTraversalPane.makeOptionPane(message, title, optionType, messageType, icon, btn, initialValue);
				
				MessageBox.removeAll();
				MessageBox.add(newOption);
				dlg.pack();
				return 0;
			}

			@Override
			public int getResult() {
				// TODO Auto-generated method stub
				//this.runThread.notify();
				return this.QuestionResult;
			}
			public void SetResult(int result) {				
				this.QuestionResult = result;				
				synchronized (runThread) {
					this.runThread.notify();
				}
			}			
		};		
		dlg = this;
	}

	class AlertButtonListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
            JButton b = (JButton) e.getSource();
            Log.d("click : " + b.getText());
            Log.d("Resource : " + Resource.STR_BTN_CANCEL.getString());
            
            if (b.getText().equals(Resource.STR_BTN_OPEN.getString())) {
            	
            } else if (b.getText().equals(Resource.STR_BTN_INSTALL.getString())) {
            	
            } else if (b.getText().equals(Resource.STR_BTN_DEL.getString())) {
            	
            } else if (b.getText().equals(Resource.STR_BTN_CANCEL.getString())) {
            	CoreInstallLitener.SetResult(1);
            } else if (b.getText().equals(Resource.STR_BTN_PUSH.getString())) {
            	
            } else if (b.getText().equals(Resource.STR_BTN_INSTALL.getString())) {
            	
            } else if (b.getText().equals(Resource.STR_BTN_NO.getString())) {
            	
            } else if (b.getText().equals(Resource.STR_BTN_YES.getString())) {
            	
            } else if (b.getText().equals(Resource.STR_BTN_REFRESH.getString())) {
            	CoreInstallLitener.SetResult(0);
            }
		}
		
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if ("showLogBox".equals(e.getActionCommand())) {
			if(scrollPane.isVisible()) {
				newOption.removeAll();
				scrollPane.setVisible(false);
				this.pack();
				
			} else {
				scrollPane.setVisible(true);
				
				this.pack();
			}
		} else if("Refresh".equals(e.getActionCommand())) {
			
		}
	}
	
    private void createAndShowGUI() {
        //Create and set up the window.
        
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e1) {
			e1.printStackTrace();
		}
    	
        f = new JFrame();
        
        TestTable = new InstallCheckTable();
        deviceListDig = new DeviceListPanel();
        
        //TestTable.createAndShowGUI();
        
        this.setTitle(Resource.STR_APP_NAME.getString());
        this.setIconImage(Resource.IMG_TOOLBAR_INSTALL.getImageIcon().getImage());
        //this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //this.setBounds(0, 0, 700, 400);
        this.setPreferredSize(new Dimension(700,400));
        
         this.setMinimumSize(new Dimension(700, 400));
        this.setLocationRelativeTo(null);
        //this.setResizable(false);
        this.pack();
        this.setVisible(true);
        //f.getContentPane().setLayout(new BorderLayout());
        //f.setLayout(new BorderLayout());
        JButton btnExit = new JButton("btnExit");
        JButton btnshowLogBox = new JButton("showLogBox");
        
        btnshowLogBox.addActionListener(this);
        
        
        JPanel framelayout = new JPanel(new BorderLayout());
        JPanel parent = new JPanel(new GridLayout(1,2));
        JPanel CheckListBox = new JPanel(new BorderLayout());
        MessageBox = new JPanel(new BorderLayout());
        JPanel ButtonBox = new JPanel(new BorderLayout());
        JPanel LogBox= new JPanel(new BorderLayout());
        
		taskOutput = new JTextArea();
		taskOutput.setText(Log.getLog());
		taskOutput.setEditable(false);
		taskOutput.setCaretPosition(0);
		scrollPane = new JScrollPane(taskOutput);
		//scrollPane.setPreferredSize(new Dimension(600, 400));
        
        ButtonBox.setBackground(Color.PINK);
        
        CheckListBox.add(TestTable);
        //MessageBox.add(deviceListDig);
        
        parent.add(CheckListBox, BorderLayout.WEST);
        parent.add(MessageBox, BorderLayout.EAST);
        
        LogBox.add(scrollPane);
        
        ButtonBox.add(btnExit,BorderLayout.EAST );
        ButtonBox.add(LogBox, BorderLayout.SOUTH);
        ButtonBox.add(btnshowLogBox, BorderLayout.WEST);
                
        framelayout.add(parent,BorderLayout.CENTER);
        framelayout.add(ButtonBox,BorderLayout.SOUTH);
                
        scrollPane.setVisible(false);
        
        this.add(framelayout);        
    }
	
    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	InstallDlg dlg = new InstallDlg();                
            }
        });
    }
}