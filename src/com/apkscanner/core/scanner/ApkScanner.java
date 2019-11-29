package com.apkscanner.core.scanner;

import java.io.File;
import java.util.ArrayList;

import com.apkscanner.core.signer.SignatureReport;
import com.apkscanner.data.apkinfo.ApkInfo;
import com.apkscanner.resource.RConst;
import com.apkscanner.resource.RProp;
import com.apkscanner.tool.aapt.AaptNativeWrapper;
import com.apkscanner.tool.aapt.AaptXmlTreePath;
import com.apkscanner.tool.adb.AdbWrapper;
import com.apkscanner.util.ConsolCmd.ConsoleOutputObserver;
import com.apkscanner.util.FileUtil;
import com.apkscanner.util.Log;
import com.apkscanner.util.ZipFileUtil;

abstract public class ApkScanner
{
	public static final String APKSCANNER_TYPE_AAPT = "AAPT";
	public static final String APKSCANNER_TYPE_AAPTLIGHT = "AAPTLIGHT";
	public static final String APKSCANNER_TYPE_APKTOOL = "APKTOOL";

	public static final int NO_ERR = 0;
	public static final int ERR_NO_SUCH_FILE = -1;
	public static final int ERR_NO_SUCH_MANIFEST = -2;
	public static final int ERR_FAILURE_PULL_APK = -3;
	public static final int ERR_CAN_NOT_ACCESS_ASSET = -4;
	public static final int ERR_WRONG_MANIFEST = -5;
	public static final int ERR_FAILURE_VERIFY_CERT = -6;

	// @deprecated
	public static final int ERR_UNAVAIlABLE_PARAM = -99;
	// @deprecated
	public static final int ERR_UNKNOWN = -100;

	protected ApkInfo apkInfo = null;

	protected StatusListener statusListener = null;
	protected int scanningStatus;
	protected int lastErrorCode;

	public enum Status {
		STANBY(0x00),
		BASIC_INFO_COMPLETED(0x01),
		WIDGET_COMPLETED(0x02),
		LIB_COMPLETED(0x04),
		RESOURCE_COMPLETED(0x08),
		RES_DUMP_COMPLETED(0x10),
		ACTIVITY_COMPLETED(0x20),
		CERT_COMPLETED(0x40),
		ALL_COMPLETED(0x7F);

		int value;
		Status(int value) { this.value = value; }
		public int value() { return value; }
		public boolean isCompleted(int statusFlag) {
			return (statusFlag & value) == value;
		}
	}

	public interface StatusListener
	{
		public void onStart(long estimatedTime);
		public void onSuccess();
		public void onError(int error);
		public void onCompleted();
		public void onProgress(int step, String msg);
		public void onStateChanged(Status status);
	}

	public ApkScanner(StatusListener statusListener)
	{
		setStatusListener(statusListener);
	}

	public StatusListener getStatusListener() {
		return this.statusListener;
	}

	public void setStatusListener(StatusListener statusListener) {
		setStatusListener(statusListener, true);
	}

	public void setStatusListener(StatusListener statusListener, boolean evokeCompleted)
	{
		synchronized(this) {
			this.statusListener = statusListener;
			if(statusListener == null || !evokeCompleted) return;

			if(lastErrorCode != NO_ERR) {
				statusListener.onError(lastErrorCode);
			} else if(scanningStatus != 0) {
				for(Status state: Status.values()) {
					if(!isCompleted(state)) continue;
					Log.v(state + " is compleated sooner than register listener");
					statusListener.onStateChanged(state);
					if(Status.ALL_COMPLETED.equals(state)) {
						statusListener.onSuccess();
						statusListener.onCompleted();
					}
				}
			}
		}
	}

	public void openApk(final String apkFilePath)
	{
		openApk(apkFilePath, RProp.S.FRAMEWORK_RES.get());
	}

	abstract public void openApk(final String apkFilePath, String frameworkRes);

	abstract public void clear(boolean sync);

	public void openPackage(String devSerialNumber, String devApkFilePath, String framework)
	{
		if(statusListener != null) statusListener.onProgress(1, "I: Open package\n");
		if(statusListener != null) statusListener.onProgress(1, "I: apk path in device : " + devApkFilePath + "\n");

		String tempApkFilePath = "/" + devSerialNumber + devApkFilePath;
		tempApkFilePath = tempApkFilePath.replaceAll("/", File.separator+File.separator).replaceAll("//", "/");
		tempApkFilePath = FileUtil.makeTempPath(tempApkFilePath)+".apk";

		if(framework == null) {
			framework = RProp.S.FRAMEWORK_RES.get();
		}

		String frameworkRes = "";
		if(framework != null && !framework.isEmpty()) {
			for(String s: framework.split(";")) {
				if(s.startsWith("@")) {
					String devNum = s.replaceAll("^@([^/]*)/.*", "$1");
					String path = s.replaceAll("^@[^/]*", "");
					String dest = (new File(tempApkFilePath).getParent()) + File.separator + path.replaceAll(".*/", "");
					if(statusListener != null) statusListener.onProgress(1, "I: start to pull resource apk " + path + "\n");
					AdbWrapper.pullApk(devNum, path, dest, null);
					frameworkRes += dest + ";";
				} else {
					frameworkRes += s + ";";
				}
			}
		}

		if(statusListener != null) statusListener.onProgress(1, "I: start to pull apk " + devApkFilePath + "\n");
		boolean ret = AdbWrapper.pullApk(devSerialNumber, devApkFilePath, tempApkFilePath, new ConsoleOutputObserver() {
			String prePercent = null;
			@Override
			public boolean ConsolOutput(String output) {
				if(statusListener != null) {
					String percent = output;//.replaceAll("\\[\\s*([0-9]*)%\\] .*", "$1");
					//if(!percent.equals(output)) {
					if(!percent.equals(prePercent)) {
						prePercent = percent;
						statusListener.onProgress(0, percent);
					}
					//}
				}
				return true;
			}
		});

		if(!ret || !(new File(tempApkFilePath)).exists()) {
			Log.e("openPackage() failure : apk pull - " + tempApkFilePath);
			if(statusListener != null) {
				statusListener.onError(ERR_FAILURE_PULL_APK);
				statusListener.onCompleted();
			}
			return;
		}

		openApk(tempApkFilePath, frameworkRes);
	}

	public ApkInfo getApkInfo()
	{
		return apkInfo;
	}

	protected void solveCert()
	{
		if(!(new File(apkInfo.filePath)).exists()) {
			Log.e("No such apk file");
			return;
		}

		ArrayList<String> certList = new ArrayList<String>();
		ArrayList<String> certFiles = new ArrayList<String>();

		SignatureReport sr = new SignatureReport(new File(apkInfo.filePath));
		for(int i = 0; i < sr.getSize(); i ++) {
			certList.add(sr.getReport(i));
		}

		apkInfo.certificates = null;
		if(!certList.isEmpty()) {
			apkInfo.certificates = certList.toArray(new String[certList.size()]);
		}

		apkInfo.signatureScheme = sr.getSignatureScheme();

		if(sr.contains("MD5", RConst.SAMSUNG_KEY_MD5)) {
			apkInfo.featureFlags |= ApkInfo.APP_FEATURE_SAMSUNG_SIGN;
		}
		if(sr.contains("MD5", RConst.SS_TEST_KEY_MD5)) {
			apkInfo.featureFlags |= ApkInfo.APP_FEATURE_PLATFORM_SIGN;
		}

		String[] certFilePaths = ZipFileUtil.findFiles(apkInfo.filePath, null, "^META-INF/.*");

		if(certList.isEmpty() && certFilePaths == null) {
			Log.e("No such folder : META-INFO/");
			return;
		}

		for (String s : certFilePaths) {
			String ext = s.toUpperCase();
			if(!ext.endsWith(".RSA") && !ext.endsWith(".DSA") && !ext.endsWith(".EC") ) {
				certFiles.add(s);
			}
		}

		apkInfo.certFiles = null;
		if(!certFiles.isEmpty()) {
			apkInfo.certFiles = certFiles.toArray(new String[certFiles.size()]);
		}
	}

	protected void setType() {
		setType(apkInfo.filePath);
	}

	protected void setType(String fileName) {
		if(fileName == null) return;
		fileName = fileName.toLowerCase();
		if(fileName.endsWith(".apk")) {
			apkInfo.type = ApkInfo.PACKAGE_TYPE_APK;
		} else if(fileName.endsWith(".apex")) {
			apkInfo.type = ApkInfo.PACKAGE_TYPE_APEX;
		} else {
			apkInfo.type = ApkInfo.PACKAGE_TYPE_UNKNOWN;
		}
	}

	protected void scanningStarted() {
		synchronized (this) {
			lastErrorCode = 0;

			if(statusListener != null) {
				statusListener.onStart(0);
			}
		}
	}

	protected void errorOccurred(int errorCode) {
		synchronized (this) {
			lastErrorCode = errorCode;
			if(statusListener != null) {
				statusListener.onError(errorCode);
				statusListener.onCompleted();
			}
		}
	}

	protected void stateChanged(Status status) {
		synchronized (this) {
			if(status == Status.STANBY) {
				scanningStatus = 0;
			} else {
				scanningStatus |= status.value();
			}

			if(statusListener != null) {
				statusListener.onStateChanged(status);

				if(scanningStatus == Status.ALL_COMPLETED.value()) {
					Log.i("I: completed... ");
					statusListener.onStateChanged(Status.ALL_COMPLETED);
					statusListener.onSuccess();
					statusListener.onCompleted();
				}
			}
		}
	}

	public int getStatus() {
		return scanningStatus;
	}

	public boolean isScanning() {
		synchronized (this) {
			return (lastErrorCode == NO_ERR && scanningStatus != 0 && scanningStatus != Status.ALL_COMPLETED.value());
		}
	}

	public boolean isCompleted(Status status) {
		synchronized (this) {
			return (status.value() != 0 && (scanningStatus & status.value()) == status.value());
		}
	}

	public int getLastErrorCode() {
		synchronized (this) {
			return lastErrorCode;
		}
	}

	public String getLastErrorMessage() {
		synchronized (this) {
			return getErrorMessage(lastErrorCode);
		}
	}

	public String getErrorMessage(int errCode) {
		switch(errCode) {
		case NO_ERR: return null;
		case ERR_NO_SUCH_FILE: return "No such apk file.";
		case ERR_NO_SUCH_MANIFEST: return "No such manifest file in the APK.";
		case ERR_FAILURE_PULL_APK: return "Can not pull apk from device.";
		case ERR_CAN_NOT_ACCESS_ASSET: return "Access fail for asset of APK.";
		case ERR_WRONG_MANIFEST: return "Manifest was wrong format.";
		case ERR_FAILURE_VERIFY_CERT: return "Bad signed.";
		case ERR_UNKNOWN: default: return "Unknown Error: " + errCode;
		}
	}

	public static ApkScanner getInstance() {
		return ApkScanner.getInstance(RProp.B.USE_EASY_UI.get()
				? ApkScanner.APKSCANNER_TYPE_AAPTLIGHT : ApkScanner.APKSCANNER_TYPE_AAPT);
	}

	public static ApkScanner getInstance(String name) {
		if(name == null || APKSCANNER_TYPE_AAPT.equalsIgnoreCase(name)) {
			return new AaptLightScanner(null, false);
		} else if(APKSCANNER_TYPE_AAPTLIGHT.equalsIgnoreCase(name)) {
			return new AaptLightScanner(null);
		}
		return new AaptScanner(null);
	}

	public String getScannerType() {
		if(this.getClass().equals(AaptScanner.class)) {
			return APKSCANNER_TYPE_AAPT;
		} else if(this.getClass().equals(AaptLightScanner.class)) {
			return APKSCANNER_TYPE_AAPTLIGHT;
		}
		return APKSCANNER_TYPE_AAPT;
	}

	public static String getPackageName(final String apkFilePath) {
		if(apkFilePath == null || apkFilePath.isEmpty()
				|| !new File(apkFilePath).isFile()) {
			Log.e("No such file " + apkFilePath);
			return null;
		}

		Log.i("getDump AndroidManifest...");
		String[] androidManifest = AaptNativeWrapper.Dump.getXmltree(apkFilePath, new String[] { "AndroidManifest.xml" });
		if(androidManifest == null || androidManifest.length == 0) {
			Log.e("Failure : Can't read the AndroidManifest.xml");
			return null;
		}

		Log.i("createAaptXmlTree...");
		AaptXmlTreePath manifestPath = new AaptXmlTreePath();
		manifestPath.createAaptXmlTree(androidManifest);
		if(manifestPath.getNode("/manifest") == null || manifestPath.getNode("/manifest/application") == null) {
			Log.e("Failure : Wrong format. Don't have '<manifest>' or '<application>' tag");
			return null;
		}

		return manifestPath.getNode("/manifest").getAttribute("package");
	}
}
