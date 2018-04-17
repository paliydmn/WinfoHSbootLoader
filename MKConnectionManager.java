package paliy;

import javafx.concurrent.Task;
import me.legrange.mikrotik.ApiConnection;
import me.legrange.mikrotik.MikrotikApiException;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import paliy.model.MKItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class MKConnectionManager {
    private static final String REBOOT_COMMAND = "/system/reboot";
    private static final String MK_USER_NAME = "admin";
    private static final String MK_PASSWORD = "";
    private static String CMD_GET_RESOURCES_DATA = "/system/resource/print";
    private static String CMD_GET_INTERFACES_DATA = "/interface/print";

    void uploadRosFiles() {
        UploadRosTask uploadRosTask = new UploadRosTask();
        Thread t = new Thread(uploadRosTask);
        t.start();
    }

    void uploadScript() {
        UploadScriptTask uploadScriptTask = new UploadScriptTask();
        Thread t = new Thread(uploadScriptTask);
        t.start();
    }

    static class MKHelper {

        private ApiConnection con = null; // connect to router

        public void collectData(CollectionMK forCollection) {

            List<MKItem> temp = new ArrayList<>(CollectionMKSingleton.getInstance().getMKList());
            //List<MKItem> temp = new ArrayList<>(forCollection.getMKList()); //replaced with singleton
            forCollection.getMKList().clear();
            StringBuffer etherBuff;
            for (MKItem item : temp) {
                try {
                    con = ApiConnection.connect(item.getIp());
                    con.login(MK_USER_NAME, "");

                    List<Map<String, String>> resourcesMap = con.execute(CMD_GET_RESOURCES_DATA);
                    System.out.println(resourcesMap);

                    for (Map<String, String> map : resourcesMap) {
                        String fullName = map.get("platform") + " " + map.get("board-name") + " " + map.get("architecture-name");
                        item.setName(fullName);
                        item.setRos(map.get("version"));
                    }
                    forCollection.getMKList().add(item);

                    List<Map<String, String>> interfacedMap = con.execute(CMD_GET_INTERFACES_DATA);
                    etherBuff = new StringBuffer();
                    for (Map<String, String> map : interfacedMap) {
                        etherBuff.append(map.get("name"))
                                .append(" ")
                                .append(map.get("mac-address"))
                                .append("\n");
                        System.out.println(etherBuff.toString());
                    }
                    item.setDescription(etherBuff.toString());

                } catch (MikrotikApiException e) {
                    String log = "ERROR ---> " + e.getMessage();
                    MyLogger.printLog(log);
                }
            }
        }

        void rebootMk() {
            for (MKItem item : CollectionMKSingleton.getInstance().getMKList()) {
                try {
                    con = ApiConnection.connect(item.getIp());
                    con.login(MK_USER_NAME, MK_PASSWORD);
                    con.execute(REBOOT_COMMAND);
                    String log = "Reboot for " + item.getIp();
                    MyLogger.printLog(log);
                } catch (MikrotikApiException e) {
                    String log = "ERROR ---> " + e.getMessage();
                    MyLogger.printLog(log);
                }
            }
        }
    }

    private static class FTPHelper {
        private static void showServerReply(FTPClient ftpClient) {
            String[] replies = ftpClient.getReplyStrings();
            if (replies != null && replies.length > 0) {
                for (String aReply : replies) {
                    String log = "SERVER: " + aReply;
                     MyLogger.printLog(log);
                }
            }
        }
    }

    private class UploadRosTask extends Task<String> {
        @Override
        protected String call() throws Exception {
            for (MKItem mk : CollectionMKSingleton.getInstance().getMKList()) {
                upload(mk);
            }
            return "UPLOADED";
        }

        void upload(MKItem mk) {
            int port = 21;
            FTPClient ftpClient = new FTPClient();
            try {
                String log = "Start connect process to " + mk.getIp();
                System.out.println(log);
                //  MyLogger.printLog(log);
                ftpClient.connect(mk.getIp(), port);
                FTPHelper.showServerReply(ftpClient);
                int replyCode = ftpClient.getReplyCode();
                if (!FTPReply.isPositiveCompletion(replyCode)) {
                    log = "Operation failed. Server reply code: " + replyCode;
                    MyLogger.printLog(log);
                    return;
                }
                boolean success = ftpClient.login(MK_USER_NAME, MK_PASSWORD);
                FTPHelper.showServerReply(ftpClient);
                if (!success) {
                    log = "Could not login to the server";
                    MyLogger.printLog(log);
                    return;
                } else {
                    log = "LOGGED IN SERVER";
                    //MyLogger.printLog(log);
                }

                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

                // APPROACH #1: uploads first file using an InputStream
                File folder = new File("src/paliy/assets/ros4/");
                String to = "";
                File[] listRosFiles = folder.listFiles();
                Map<String, String> fromToRosMap = new HashMap<>();

                for (int i = 0; i < listRosFiles.length; i++) {
                    if (listRosFiles[i].isFile()) {
                        fromToRosMap.put(folder.getPath() + "/" + listRosFiles[i].getName(), to + listRosFiles[i].getName());
                        System.out.println("File " + listRosFiles[i].getName());
                    } else if (listRosFiles[i].isDirectory()) {
                        System.out.println("Directory " + listRosFiles[i].getName());
                    }
                }

              /*  //add Script to uploads
                String remoteFilePath = "flash/bootLoader.rsc";
                fromToRosMap.put("src/paliy/assets/bootLoader.rsc", remoteFilePath);*/

                InputStream is;
                for (Map.Entry<String, String> map : fromToRosMap.entrySet()) {
                    String file = map.getKey();
                    is = new FileInputStream(file);
                    boolean done = ftpClient.storeFile(map.getValue(), is);
                    is.close();
                    if (done) {
                        log = file + " file is uploaded successfully to - " + mk.getIp();
                        System.out.println(log);
                        ;
                        //   MyLogger.printLog(log);
                    }
                }
                String status = mk.getStatus() == null ? "ROS files uploaded" : mk.getStatus() +"\nROS files uploaded";
                mk.setStatus(status);
            } catch (IOException ex) {
                String log = "Oops! Something wrong happened during connection to " + mk.getIp();
                MyLogger.printLog(log);
                log = "ERROR ---> " + ex.getMessage();
                MyLogger.printLog(log);
            } finally {
                try {
                    if (ftpClient.isConnected()) {
                        ftpClient.logout();
                        ftpClient.disconnect();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                    String log = "ERROR ---> " + ex.getMessage();
                    MyLogger.printLog(log);
                }
            }
            MyLogger.printLog("****************************************\n");
        }
    }

    private class UploadScriptTask extends Task<String> {
        private ApiConnection con = null; // connect to router

        @Override
        protected String call() throws Exception {
            for (MKItem mk : CollectionMKSingleton.getInstance().getMKList()) {
                upload(mk);
                setOnBootScript(mk);
            }
            return "UPLOADED";
        }

        void upload(MKItem mk) {
            int port = 21;
            FTPClient ftpClient = new FTPClient();
            try {
                String log = "Start connect process to " + mk;
                MyLogger.printLog(log);
                ftpClient.connect(mk.getIp(), port);
                FTPHelper.showServerReply(ftpClient);
                int replyCode = ftpClient.getReplyCode();
                if (!FTPReply.isPositiveCompletion(replyCode)) {
                    log = "Operation failed. Server reply code: " + replyCode;
                    MyLogger.printLog(log);
                    return;
                }
                boolean success = ftpClient.login(MK_USER_NAME, MK_PASSWORD);
                FTPHelper.showServerReply(ftpClient);
                if (!success) {
                    log = "Could not login to the server";
                    MyLogger.printLog(log);
                    return;
                } else {
                    log = "LOGGED IN SERVER";
                    //    MyLogger.printLog(log);
                }

                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                String remoteFilePath = "flash/bootLoader.rsc";
                File file = new File("src/paliy/assets/bootLoader.rsc");

                InputStream is = new FileInputStream(file);
                boolean done = ftpClient.storeFile(remoteFilePath, is);
                is.close();
                if (done) {
                    log = file + " Script is uploaded successfully to - " + mk.getIp();
                    MyLogger.printLog(log);
                }
                String status = mk.getStatus() == null ? "Boot Script uploaded" : mk.getStatus() + "\nBoot Script uploaded ";
                mk.setStatus(status);
            } catch (IOException ex) {
                String log = "Oops! Something wrong happened during connection to " + mk.getIp();
                MyLogger.printLog(log);
                log = "ERROR ---> " + ex.getMessage();
                MyLogger.printLog(log);
            } finally {
                try {
                    if (ftpClient.isConnected()) {
                        ftpClient.logout();
                        ftpClient.disconnect();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                    String log = "ERROR ---> " + ex.getMessage();
                    MyLogger.printLog(log);
                }
            }
            MyLogger.printLog("****************************************\n");
        }

        void setOnBootScript(MKItem mk) {
            try {
                con = ApiConnection.connect(mk.getIp());
                con.login(MK_USER_NAME, MK_PASSWORD);
                Date date = Calendar.getInstance().getTime();
                date = new Date(date.getTime() + 15000);

                String time = new SimpleDateFormat("HH:mm:ss").format(date);
                System.out.println(new SimpleDateFormat("HH:mm:ss").format(date));

                //con.execute("/import file-name=flash/bootLoader.rsc");
                con.execute("/system/script/add name=test1 source='{/system reset-configuration run-after-reset=flash/MKHotspotConfigScript.rsc}'");
                String log = "EXECUTED - /system/script/add NAME=test1 source='{/system reset-configuration run-after-reset=flash/MKHotspotConfigScript.rsc}'";
                MyLogger.printLog(log);

                con.execute("/system/scheduler/add start-time=" + time + " name=sch_reset on-event=test1");
                log = "EXECUTED - /system/scheduler/add start-time=" + time + " NAME=sch_reset on-event=test1";
                MyLogger.printLog(log);

                String status = mk.getStatus() == null ? "Schedules created" : mk.getStatus() + "\nSchedules created ";
                mk.setStatus(status);
            } catch (MikrotikApiException e) {
                String log = "ERROR ---> " + e.getMessage();
                MyLogger.printLog(log);
            }
        }
    }

}