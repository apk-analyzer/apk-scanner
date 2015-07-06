package com.ApkInfo.UI;

import java.awt.EventQueue;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import java.awt.BorderLayout;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;

import com.ApkInfo.Core.*;
import com.ApkInfo.Core.ApkManager.ApkInfo;
import com.ApkInfo.Core.ApkManager.ProcessCmd;
import com.ApkInfo.Core.ApkManager.SolveType;
import com.ApkInfo.Core.ApkManager.StatusListener;
import com.ApkInfo.Resource.Resource;


public class MainUI extends JFrame implements WindowListener
{
	private static final long serialVersionUID = 1L;
	
	private static JFrame frame;
		
	static MainUI window;
	static private MyTabUI mMyTabUI;
	static private MyToolBarUI mMyToolBarUI;
	
	//window position
	static public int nPositionX,nPositionY;
	
	//for waiting
	static public JFrame WaitingDlg;
	static public MyProgressBarDemo ProgressBarDlg;

	static private ApkManager mApkManager;
	
	public static ApkInfo GetMyApkInfo(){
		return mApkManager.getApkInfo();
	}
		
	public static void openApk(final String apkPath) {
		//System.out.println("target file :" + apkPath);
		mApkManager = new ApkManager(apkPath, new StatusListener() {
			@Override
			public void OnStart(ProcessCmd cmd) {
				System.out.println("ApkCore.OnStart()");
				switch(cmd) {
				case SOLVE_RESOURCE:
					frame.setVisible(false);
					ProgressBarDlg = new MyProgressBarDemo();
					WaitingDlg = MyProgressBarDemo.createAndShowGUI(ProgressBarDlg);
					break;
				default:
					break;
				}
			}

			@Override
			public void OnComplete(ProcessCmd cmd) {
				System.out.println("ApkCore.OnComplete()");
				switch(cmd) {
				case SOLVE_RESOURCE:
					String title = "APK Scanner - " + apkPath.substring(apkPath.lastIndexOf(File.separator)+1);
					frame.setTitle(title);
					frame.setVisible(true);
					WaitingDlg.setVisible(false);
					
					mMyTabUI.setData(mApkManager.getApkInfo());
					break;
				default:
					break;
				}
			}

			@Override
			public void OnProgress(int step, String msg) {
				System.out.println("ApkCore.OnProgress() " + step + ",  " + msg);
				ProgressBarDlg.addProgress(step, msg);
			}

			@Override
			public void OnStateChange() {
				System.out.println("ApkCore.OnStateChange()");
			}
			
		});
		mApkManager.solve(SolveType.RESOURCE);
	}

	/**
	 * Launch the application.
	 */
	public static void main(final String[] args) {
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {	
				window = new MainUI();
				window.initialize("APK Scanner");
				
				String Osname = System.getProperty("os.name");
				System.out.println("OS : " + Osname);
				//System.out.println("java.io.tmpdir : " + System.getProperty("java.io.tmpdir"));
				//System.out.println("user.dir : " + System.getProperty("user.dir"));
				
				String apkPath = null;
				if(args.length > 0) {
					apkPath = args[0];
					System.out.println("Target APK : " + args[0]);
					frame.setVisible(false);
					openApk(apkPath);
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public MainUI() {
		//initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize(String title) {
		frame = new JFrame();
		frame.addWindowListener(this);
		frame.setBounds(100, 100, 600, 550);
		frame.setTitle(title);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);		
		
		mMyTabUI = new MyTabUI();
		mMyToolBarUI = new MyToolBarUI();

		frame.add(mMyTabUI, BorderLayout.CENTER);
		frame.add(mMyToolBarUI, BorderLayout.NORTH);
		
		//frame.getContentPane().add(new MyButtonPanel(), BorderLayout.SOUTH);
		frame.setResizable( true );
		
		//frame.add(new MyButtonPanel(), BorderLayout.NORTH);
		
        
        ImageIcon Appicon = Resource.IMG_APP_ICON.getImageIcon();
        
        frame.setIconImage(Appicon.getImage());
		
		frame.setVisible(true);
		
		nPositionX = frame.getLocationOnScreen().x;
		nPositionY = frame.getLocationOnScreen().y;
        
		frame.getContentPane().addHierarchyBoundsListener(new HierarchyBoundsListener(){
			@Override
			public void ancestorMoved(HierarchyEvent e) {
				nPositionX = frame.getLocationOnScreen().x;
				nPositionY = frame.getLocationOnScreen().y;
			}

			@Override
			public void ancestorResized(HierarchyEvent e) {
				
			}
		});
	}
	
	
	@Override
	public void windowOpened(WindowEvent e) {
		
	}

	@Override
	public void windowClosing(WindowEvent e) {
		frame.setVisible(false);
		if(DeviceUIManager.dlgDialog != null && DeviceUIManager.dlgDialog.isVisible()) {
			DeviceUIManager.dlgDialog.setVisible(false);
		}
		if(mApkManager != null)
			mApkManager.clear(true);
	}

	@Override
	public void windowClosed(WindowEvent e) {

	}

	@Override
	public void windowIconified(WindowEvent e) {
		
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		
	}

	@Override
	public void windowActivated(WindowEvent e) {
		
	}

	@Override
	public void windowDeactivated(WindowEvent e) {

	}

}
