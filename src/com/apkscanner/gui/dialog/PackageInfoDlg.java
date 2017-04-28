package com.apkscanner.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.border.MatteBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import com.apkscanner.Launcher;
import com.apkscanner.core.installer.ApkInstaller;
import com.apkscanner.core.installer.ApkInstaller.ApkInstallerListener;
import com.apkscanner.core.signer.Signature;
import com.apkscanner.gui.messagebox.ArrowTraversalPane;
import com.apkscanner.gui.messagebox.JTextOptionPane;
import com.apkscanner.gui.theme.TabbedPaneUIManager;
import com.apkscanner.gui.util.ApkFileChooser;
import com.apkscanner.gui.util.JHtmlEditorPane;
import com.apkscanner.gui.util.JHtmlEditorPane.HyperlinkClickListener;
import com.apkscanner.resource.Resource;
import com.apkscanner.tool.adb.AdbDeviceHelper.SimpleOutputReceiver;
import com.apkscanner.tool.adb.PackageInfo;
import com.apkscanner.util.FileUtil;
import com.apkscanner.util.FileUtil.FSStyle;
import com.apkscanner.util.SystemUtil;

public class PackageInfoDlg extends JDialog implements ActionListener, HyperlinkClickListener, ChangeListener {
	private static final long serialVersionUID = 7654892270063010429L;

	private static final String ACT_CMD_OPEN_PACKAGE = "ACT_CMD_OPEN_PACKAGE";
	private static final String ACT_CMD_SAVE_PACKAGE = "ACT_CMD_SAVE_PACKAGE";
	private static final String ACT_CMD_LAUCH_PACKAGE = "ACT_CMD_LAUCH_PACKAGE";
	private static final String ACT_CMD_UNINSTALL_PACKAGE = "ACT_CMD_UNINSTALL_PACKAGE";

	JTabbedPane tabbedPane;
	JHtmlEditorPane infoPanel;
	JHtmlEditorPane sysPackInfoPanel;
	JTextArea dumpsysTextArea;
	JTextArea signatureTextArea;
	JTextField txtApkPath;

	PackageInfo packageInfo;
	boolean hasSysPack;
	String apkPath;
	String hiddenApkPath;

	public PackageInfoDlg(Window owner) {
		super(owner);
		initialize(owner);
	}

	private void initialize(Window window)
	{
		setTitle("Package Info");
		setIconImage(Resource.IMG_APP_ICON.getImageIcon().getImage());
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setResizable(true);
		setModal(false);
		setLayout(new BorderLayout());
		setSize(new Dimension(500, 400));
		setLocationRelativeTo(window);

		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		toolBar.setLayout(new FlowLayout(FlowLayout.LEFT, 1, 1));
		toolBar.setBorder(new MatteBorder(0,0,1,0,Color.LIGHT_GRAY));

		JSeparator separator = new JSeparator(JSeparator.VERTICAL);
		separator.setPreferredSize(new Dimension(1,30));

		toolBar.add(getToolbarButton("open", Resource.IMG_APP_ICON.getImageIcon(24, 24), null, ACT_CMD_OPEN_PACKAGE));
		toolBar.add(getToolbarButton("save", Resource.IMG_RESOURCE_TEXTVIEWER_TOOLBAR_SAVE.getImageIcon(24, 24), null, ACT_CMD_SAVE_PACKAGE));
		toolBar.add(separator);
		toolBar.add(getToolbarButton(null, Resource.IMG_TOOLBAR_LAUNCH.getImageIcon(24, 24), null, ACT_CMD_LAUCH_PACKAGE));
		toolBar.add(getToolbarButton("uninstall", Resource.IMG_TOOLBAR_UNINSTALL.getImageIcon(24, 24), null, ACT_CMD_UNINSTALL_PACKAGE));

		add(toolBar, BorderLayout.NORTH);

		String tabbedStyle = (String) Resource.PROP_TABBED_UI_THEME.getData();
		tabbedPane = new JTabbedPane();
		TabbedPaneUIManager.setUI(tabbedPane, tabbedStyle);
		tabbedPane.addChangeListener(this);

		infoPanel = new JHtmlEditorPane();
		infoPanel.setEditable(false);
		infoPanel.setOpaque(true);
		infoPanel.setBackground(Color.white);
		infoPanel.setHyperlinkClickListener(this);

		sysPackInfoPanel = new JHtmlEditorPane();
		sysPackInfoPanel.setEditable(false);
		sysPackInfoPanel.setOpaque(true);
		sysPackInfoPanel.setBackground(Color.white);
		sysPackInfoPanel.setHyperlinkClickListener(this);

		//Font font = new Font("helvitica", Font.BOLD, 15);
		JLabel label = new JLabel();
		Font font = label.getFont();

		// create some css from the label's font
		StringBuilder style = new StringBuilder("#basic-info, #perm-group {");
		style.append("font-family:" + font.getFamily() + ";");
		style.append("font-weight:" + (font.isBold() ? "bold" : "normal") + ";");
		style.append("font-size:" + font.getSize() + "pt;}");
		style.append("#basic-info a {text-decoration:none; color:black;}");
		style.append("#perm-group a {text-decoration:none; color:#"+Integer.toHexString(label.getBackground().getRGB() & 0xFFFFFF)+";}");
		style.append(".danger-perm {text-decoration:none; color:red;}");
		style.append("#about {");
		style.append("font-family:" + font.getFamily() + ";");
		style.append("font-weight:" + (font.isBold() ? "bold" : "normal") + ";");
		style.append("font-size:" + font.getSize() + "pt;}");
		style.append("#about a {text-decoration:none;}");
		style.append("#div-button { background-color: #e7e7e7; border: none; color: white; margin:1px; padding: 5px; text-align: center; text-decoration: none; display: inline-block;");
		style.append("font-family:" + font.getFamily() + ";");
		style.append("font-weight:" + (font.isBold() ? "bold" : "normal") + ";");
		style.append("font-size:" + font.getSize() + "pt;}");
		style.append("#div-button a {text-decoration:none; color:black;}");

		infoPanel.setStyle(style.toString());
		sysPackInfoPanel.setStyle(style.toString());

		dumpsysTextArea = new JTextArea();
		dumpsysTextArea.setEditable(false);

		signatureTextArea = new JTextArea();
		signatureTextArea.setEditable(false);

		add(tabbedPane, BorderLayout.CENTER);

		txtApkPath = new JTextField();
		txtApkPath.setOpaque(true);
		txtApkPath.setEditable(false);

		add(txtApkPath, BorderLayout.SOUTH);
	}

	private JButton getToolbarButton(String text, Icon icon, String tooltip, String actCommand) {
		JButton button = new JButton(icon);
		button.setToolTipText(tooltip);
		button.addActionListener(this);
		button.setBorderPainted(false);
		button.setOpaque(false);
		button.setFocusable(false);
		button.setActionCommand(actCommand);
		button.setVerticalTextPosition(JLabel.BOTTOM);
		button.setHorizontalTextPosition(JLabel.CENTER);
		//button.setPreferredSize(new Dimension(43,45));
		return button;
	}

	private void alignTabbedPanel(boolean hasSysPackPanel) {
		tabbedPane.removeAll();
		tabbedPane.addTab(Resource.STR_TAB_PACAKGE_INFO.getString(), null, infoPanel, null);
		if(hasSysPackPanel) {
			tabbedPane.addTab(Resource.STR_TAB_SYS_PACAKGE_INFO.getString(), null, sysPackInfoPanel, null);
		}
		tabbedPane.addTab(Resource.STR_TAB_DUMPSYS.getString(), null, new JScrollPane(dumpsysTextArea), null);
		tabbedPane.addTab(Resource.STR_TAB_CERT.getString(), null, new JScrollPane(signatureTextArea), null);
	}

	public void setPackageInfo(PackageInfo info) {
		packageInfo = info;

		hasSysPack = info.getHiddenSystemPackageValue("pkg") != null;
		alignTabbedPanel(hasSysPack);

		info.getLauncherActivityList(true);

		txtApkPath.setText(info.apkPath);

		infoPanel.setBody(getSummaryText(info, false));
		if(hasSysPack) {
			sysPackInfoPanel.setBody(getSummaryText(info, true));
		}

		setDumpsysText(info.dumpsys);

		String publicKey = null;
		if(info.signature != null && !info.signature.isEmpty() 
				&& !info.signature.startsWith("Permission denied")) {
			Signature signature = new Signature(info.signature);
			try {
				CertificateFactory cf = CertificateFactory.getInstance("X509");
				X509Certificate X509Certificatecertificate = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(signature.toByteArray()));
				publicKey = X509Certificatecertificate.toString();
			} catch (CertificateException e1) {
				e1.printStackTrace();
			}
		} else {
			publicKey = info.signature;
		}
		signatureTextArea.setText(publicKey);
		signatureTextArea.setCaretPosition(0);
	}

	private String getSummaryText(PackageInfo info, boolean isSystemPackage)
	{
		//String appName = "App Name";
		String infoBlock = !isSystemPackage ? "Packages" : "Hidden system packages";

		String apkPath = !isSystemPackage ? info.apkPath : info.getHiddenSystemPackageValue("codePath");
		
		long apkSize = 0;
		SimpleOutputReceiver outputReceiver = new SimpleOutputReceiver();
		try {
			info.device.executeShellCommand("ls -l " + apkPath, outputReceiver);
		} catch (TimeoutException | AdbCommandRejectedException | ShellCommandUnresponsiveException | IOException e) {
			e.printStackTrace();
		}
		for(String line: outputReceiver.getOutput()) {
			if(line.isEmpty()) continue;
			String size = line.replaceAll(".*\\s+(\\d+)\\s+\\d+.*", "$1");
			if(!line.equals(size)) {
				apkSize = Integer.parseInt(size);
				if(!apkPath.endsWith(".apk")) {
					String tmp = line.replaceAll(".*\\s(\\S*\\.apk)", "/$1");
					if(!line.equals(tmp)) {
						apkPath += tmp;
					}
				}
			}
		}
		if(!isSystemPackage) {
			this.apkPath = apkPath;
		} else {
			this.hiddenApkPath = apkPath;
		}

		String versionName = info.getValue(infoBlock, "versionName");
		String versionCode = info.getValue(infoBlock, "versionCode");

		String temp = null;
		String sdkVersion = "";
		temp = info.getValue(infoBlock, "minSdk");
		if(temp != null) {
			sdkVersion += makeHyperLink("@event", temp +" (Min)", "Min SDK version", "minSdk", null);
		}
		temp = info.getValue(infoBlock, "targetSdk");
		if(temp != null) {
			if(!sdkVersion.isEmpty()) {
				sdkVersion += ", ";
			}
			sdkVersion += makeHyperLink("@event", temp + " (Target)", "Targer SDK version", "targetSdk", null);
		}
		temp = info.getValue(infoBlock, "maxSdk");
		if(temp != null) {
			if(!sdkVersion.isEmpty()) {
				sdkVersion += ", ";
			}
			sdkVersion += makeHyperLink("@event", temp + " (Max)", "Max SDK version", "maxSdk", null);
		}
		if(sdkVersion.isEmpty()) {
			sdkVersion += "Unspecified";
		}

		//String feature = "LAUNCHER, FLAGS<br/>";

		StringBuilder feature = new StringBuilder();
		if(isSystemPackage) {
			feature.append(makeHyperLink("@event", Resource.STR_FEATURE_HIDDEN_SYS_PACK_LAB.getString(), Resource.STR_FEATURE_HIDDEN_SYS_PACK_DESC.getString(), "feature-hidden-pack", null));
		} else if(info.hasLauncher()) {
			feature.append(makeHyperLink("@event", Resource.STR_FEATURE_LAUNCHER_LAB.getString(), Resource.STR_FEATURE_LAUNCHER_DESC.getString(), "feature-launcher", null));
		} else {
			feature.append(makeHyperLink("@event", Resource.STR_FEATURE_HIDDEN_LAB.getString(), Resource.STR_FEATURE_HIDDEN_DESC.getString(), "feature-hidden", null));
		}
		feature.append(", " + makeHyperLink("@event", Resource.STR_FEATURE_FLAG_LAB.getString(), Resource.STR_FEATURE_FLAG_DESC.getString(), "feature-flags", null));

		String installer = info.getValue(infoBlock, "installerPackageName");
		if(installer == null || installer.isEmpty()) {
			installer = "N/A";
		}
		installer += ", " + info.getValue(infoBlock, "timeStamp");
		installer = makeHyperLink("@event", installer, "TimeStamp", "feature-timeStamp", null);

		StringBuilder strTabInfo = new StringBuilder("");
		strTabInfo.append("<table>");
		strTabInfo.append("  <tr>");
		strTabInfo.append("    <td height=200>");
		strTabInfo.append("      <div id=\"basic-info\">");
		//strTabInfo.append("        <font style=\"font-size:20px; color:#548235; font-weight:bold\">");
		//strTabInfo.append("          " + appName);
		//strTabInfo.append("        </font><br/>");
		strTabInfo.append("        <font style=\"font-size:15px; color:#4472C4\">");
		strTabInfo.append("          [" + info.pkgName +"]");
		strTabInfo.append("        </font><br/>");
		strTabInfo.append("        <font style=\"font-size:15px; color:#ED7E31\">");
		strTabInfo.append("          " + makeHyperLink("@event", "Ver. " + versionName +" / " + versionCode, "VersionName : " + versionName + "\n" + "VersionCode : " + versionCode, "app-version", null));
		strTabInfo.append("        </font><br/>");
		strTabInfo.append("        <br/>");
		strTabInfo.append("        <font style=\"font-size:12px\">");
		strTabInfo.append("          @SDK Ver. " + sdkVersion + "<br/>");
		strTabInfo.append("          " + FileUtil.getFileSize(apkSize, FSStyle.FULL));
		strTabInfo.append("        </font>");
		strTabInfo.append("        <br/><br/>");
		strTabInfo.append("        <font style=\"font-size:12px\">");
		strTabInfo.append("          [" + Resource.STR_FEATURE_LAB.getString() + "] ");
		strTabInfo.append("          " + feature.toString() + "<br/>");
		strTabInfo.append("          [" + Resource.STR_FEATURE_INSTALLER_LAB.getString() + "] ");
		strTabInfo.append("          " + installer);
		strTabInfo.append("        </font><br/>");
		strTabInfo.append("      </div>");
		strTabInfo.append("    </td>");
		strTabInfo.append("  </tr>");
		strTabInfo.append("</table>");
		strTabInfo.append("<div id=\"perm-group\" style=\"text-align:left; width:300px; padding-top:5px; border-top:1px; border-left:0px; border-right:0px; border-bottom:0px; border-style:solid;\">");
		strTabInfo.append("  <font style=\"font-size:12px;color:black;\">");
		/*
		if(allPermissionsList != null && !allPermissionsList.isEmpty()) {
			strTabInfo.append("    [" + Resource.STR_BASIC_PERMISSIONS.getString() + "] - ");
			strTabInfo.append("    " + makeHyperLink("@event","<u>" + Resource.STR_BASIC_PERMLAB_DISPLAY.getString() + "</u>",Resource.STR_BASIC_PERMDESC_DISPLAY.getString(),"display-list", null));
		} else {
			strTabInfo.append("    " + Resource.STR_LABEL_NO_PERMISSION.getString());
		}
		 */
		strTabInfo.append("  </font><br/>");
		strTabInfo.append("  <font style=\"font-size:5px\"><br/></font>");
		//strTabInfo.append("  " + permGorupImg);
		strTabInfo.append("</div>");
		strTabInfo.append("<div height=10000 width=10000></div>");

		return strTabInfo.toString();
	}

	private void setDumpsysText(String[] dumpsys) {
		StringBuilder dumpSys = new StringBuilder();
		StringBuilder packageInfo = new StringBuilder();

		String blockRegex = "^(\\s*)(Hidden system )?[pP]ackages:\\s*$";
		String blockEndRegex = "";
		boolean startInfoBlock = false;
		for(String line: dumpsys) {
			if(startInfoBlock && line.matches(blockEndRegex)) {
				startInfoBlock = false;
			}

			if(!startInfoBlock) {
				if(line.matches(blockRegex)) {
					startInfoBlock = true;
					blockEndRegex = "^" + line.replaceAll(blockRegex, "$1") + "\\S.*";
				}
			}

			if(startInfoBlock) {
				packageInfo.append(line);
				packageInfo.append("\n");
			} else {
				dumpSys.append(line);
				dumpSys.append("\n");
			}
		}
		packageInfo.append("\n");
		packageInfo.append(dumpSys.toString());

		dumpsysTextArea.setText(packageInfo.toString());
		dumpsysTextArea.setCaretPosition(0);
	}

	private String makeHyperLink(String href, String text, String title, String id, String style)
	{
		return JHtmlEditorPane.makeHyperLink(href, text, title, id, style);
	}

	private void showDialog(String content, String title, Dimension size, Icon icon)
	{
		JTextOptionPane.showTextDialog(null, content, title, JOptionPane.INFORMATION_MESSAGE, icon, size);
	}

	public void showFeatureInfo(String id)
	{
		String feature = null;
		Dimension size = new Dimension(400, 100);

		if("feature-hidden".equals(id)) {
			feature = Resource.STR_FEATURE_HIDDEN_DESC.getString();
		} else if("feature-launcher".equals(id)) {
			feature = Resource.STR_FEATURE_LAUNCHER_DESC.getString();
		} else if("feature-flags".equals(id)) {
			StringBuilder sb = new StringBuilder();
			String temp = packageInfo.getValue("flags"); 
			if(temp != null && !temp.isEmpty()) {
				sb.append("flags: ");
				sb.append(temp);
			}
			temp = packageInfo.getValue("privateFlags"); 
			if(temp != null && !temp.isEmpty()) {
				sb.append("\nprivateFlags: ");
				sb.append(temp);
			}
			temp = packageInfo.getValue("pkgFlags"); 
			if(temp != null && !temp.isEmpty()) {
				sb.append("\npkgFlags: ");
				sb.append(temp);
			}
			feature = sb.toString();
		} else if("feature-timeStamp".equals(id)) {
			StringBuilder sb = new StringBuilder();

			String temp = packageInfo.getValue("installerPackageName");
			if(temp == null || temp.isEmpty()) {
				temp = "N/A";
			}
			sb.append("Installer: ");
			sb.append(temp);
			sb.append("\n");
			temp = packageInfo.getValue("timeStamp");
			if(temp != null && !temp.isEmpty()) {
				sb.append("\ntimeStamp: ");
				sb.append(temp);
			}
			temp = packageInfo.getValue("firstInstallTime"); 
			if(temp != null && !temp.isEmpty()) {
				sb.append("\nfirstInstallTime: ");
				sb.append(temp);
			}
			temp = packageInfo.getValue("lastUpdateTime"); 
			if(temp != null && !temp.isEmpty()) {
				sb.append("\nlastUpdateTime: ");
				sb.append(temp);
			}
			feature = sb.toString();
		}

		showDialog(feature, "Feature info", size, null);
	}

	@Override
	public void stateChanged(ChangeEvent arg0) {
		if(!hasSysPack) return;
		
		JTabbedPane tabSource = (JTabbedPane) arg0.getSource();
		if(tabSource.getSelectedComponent().equals(sysPackInfoPanel)) {
			txtApkPath.setText(hiddenApkPath);
		} else {
			txtApkPath.setText(apkPath);
		}
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		String actCmd = arg0.getActionCommand();

		if(ACT_CMD_OPEN_PACKAGE.equals(actCmd)) {
			Launcher.run(packageInfo.device.getSerialNumber(), txtApkPath.getText(), null);
		} else if(ACT_CMD_SAVE_PACKAGE.equals(actCmd)) {
			final String apkPath = txtApkPath.getText();
			if(apkPath == null) return;

			String saveFileName;
			if(apkPath.endsWith("base.apk")) {
				saveFileName = apkPath.replaceAll(".*/(.*)/base.apk", "$1.apk");
			} else {
				saveFileName = apkPath.replaceAll(".*/", "");
			}

			final File destFile = ApkFileChooser.saveApkFile(this, saveFileName);
			if(destFile == null) return;

			ApkInstaller apkInstaller = new ApkInstaller(packageInfo.device.getSerialNumber(), new ApkInstallerListener() {
				StringBuilder sb = new StringBuilder();
				@Override
				public void OnError(int cmdType, String device) {
					JTextOptionPane.showTextDialog(null, Resource.STR_MSG_FAILURE_PULLED.getString() + "\n\nConsol output", sb.toString(),  Resource.STR_LABEL_ERROR.getString(), JTextOptionPane.ERROR_MESSAGE,
							null, new Dimension(400, 100));
				}

				@Override
				public void OnSuccess(int cmdType, String device) {
					int n = ArrowTraversalPane.showOptionDialog(null,
							Resource.STR_MSG_SUCCESS_PULL_APK.getString() + "\n" + destFile.getAbsolutePath(),
							Resource.STR_LABEL_QUESTION.getString(),
							JOptionPane.YES_NO_CANCEL_OPTION,
							JOptionPane.INFORMATION_MESSAGE,
							null,
							new String[] {Resource.STR_BTN_EXPLORER.getString(), Resource.STR_BTN_OPEN.getString(), Resource.STR_BTN_OK.getString()},
							Resource.STR_BTN_OK.getString());
					switch(n) {
					case 0: // explorer
						SystemUtil.openFileExplorer(destFile);
						break;
					case 1: // open
						Launcher.run(destFile.getAbsolutePath());
						break;
					default:
						break;
					}
				}

				@Override public void OnCompleted(int cmdType, String device) { }
				@Override public void OnMessage(String msg) { sb.append(msg); }
			});		
			apkInstaller.pullApk(apkPath, destFile.getAbsolutePath());
		}
	}

	@Override
	public void hyperlinkClick(String id) {
		if(id.endsWith("Sdk")){
			String sdkVer = packageInfo.getValue(id);
			SdkVersionInfoDlg sdkDlg = new SdkVersionInfoDlg(null, Resource.STR_SDK_INFO_FILE_PATH.getString(), Integer.parseInt(sdkVer));
			sdkDlg.setLocationRelativeTo(this);
			sdkDlg.setVisible(true);
		} else if("app-version".equals(id)) {
			String ver = "versionName : " + packageInfo.versionName + "\n" + "versionCode : " + packageInfo.versionCode;
			showDialog(ver, "App version info", new Dimension(300, 50), null);
		} else if(id.startsWith("feature-")) {
			showFeatureInfo(id);
		}
	}
}
