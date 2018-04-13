package paliy;

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

    static class MKHelper{
        private ApiConnection con = null; // connect to router

        private static String CMD_GET_RESOURCES_DATA = "/system/resource/print";
        private static String CMD_GET_INTERFACES_DATA = "/interface/print";

        public void collectData(CollectionMK forCollection) {

            List<MKItem> temp = new ArrayList<>(forCollection.getMKList());
            forCollection.getMKList().clear();
            StringBuffer etherBuff;
            for (MKItem item : temp) {
                try {
                    con = ApiConnection.connect(item.getIp());
                    con.login("admin", "");

                    List<Map<String, String>> resourcesMap = con.execute(CMD_GET_RESOURCES_DATA);
                    System.out.println(resourcesMap);

                    for (Map<String, String> map : resourcesMap) {
                        String fullName =  map.get("platform") + " " + map.get("board-name") + " " +  map.get("architecture-name");
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
                    Logger.printLog(log);                }
            }
        }

        public void setOnBootScript(CollectionMK forCollection){

            for (MKItem item : forCollection.getMKList()) {
                try {
                    con = ApiConnection.connect(item.getIp());
                    con.login("admin", "");
                    Date date = Calendar.getInstance().getTime();
                    date = new Date(date.getTime() + 15000);

                    String time = new SimpleDateFormat("HH:mm:ss").format(date);
                    System.out.println(new SimpleDateFormat("HH:mm:ss").format(date));

                    //con.execute("/import file-name=flash/bootLoader.rsc");
                    String log = "/system/script/add NAME=test1 source='{/system reset-configuration run-after-reset=flash/MKHotspotConfigScript.rsc}'";
                    Logger.printLog(log);
                    con.execute("/system/script/add name=test1 source='{/system reset-configuration run-after-reset=flash/MKHotspotConfigScript.rsc}'");

                    log = "/system/scheduler/add start-time=" + time + " NAME=sch_reset on-event=test1";
                    Logger.printLog(log);
                    con.execute("/system/scheduler/add start-time=" + time + " name=sch_reset on-event=test1");

                } catch (MikrotikApiException e) {
                    String log = "ERROR ---> " + e.getMessage();
                    Logger.printLog(log);
                }
            }
        }

    }

    static class FTPHelper {
        private static void showServerReply(FTPClient ftpClient) {
            String[] replies = ftpClient.getReplyStrings();
            if (replies != null && replies.length > 0) {
                for (String aReply : replies) {
                    String log = "SERVER: " + aReply;
                    Logger.printLog(log);
                }
            }
        }

        public static void upload(String ip) {
            //String server = "router.lan";
            int port = 21;
            String user = "admin";
            String pass = "";
            FTPClient ftpClient = new FTPClient();
            try {
                String log = "Start connect process to " + ip;
                Logger.printLog(log);
                ftpClient.connect(ip, port);
                // ftpClient.connect(ip, port);
                showServerReply(ftpClient);
                int replyCode = ftpClient.getReplyCode();
                if (!FTPReply.isPositiveCompletion(replyCode)) {
                    log ="Operation failed. Server reply code: " + replyCode;
                    Logger.printLog(log);
                    return;
                }
                boolean success = ftpClient.login(user, pass);
                showServerReply(ftpClient);
                if (!success) {
                    log = "Could not login to the server";
                    Logger.printLog(log);
                    return;
                } else {
                    log = "LOGGED IN SERVER";
                    Logger.printLog(log);
                }

                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

                // APPROACH #1: uploads first file using an InputStream
                //File firstLocalFile = new File("D:/MKHotspotConfigScript.rsc");
                File firstLocalFile = new File("D:/Micro/MKHotSpotScriptCreator/sources/configFile");

                String remoteFilePath = "flash/bootLoader.rsc";
                InputStream inputStream = new FileInputStream(firstLocalFile);

                log = "Start uploading file";
                Logger.printLog(log);
                boolean done = ftpClient.storeFile(remoteFilePath, inputStream);
                inputStream.close();
                if (done) {
                    log = "The config file is uploaded successfully - " + ip;
                    Logger.printLog(log);
                }

           /* // APPROACH #2: uploads second file using an OutputStream
            File secondLocalFile = new File("E:/Test/Report.doc");
            String secondRemoteFile = "test/Report.doc";
            inputStream = new FileInputStream(secondLocalFile);

            System.out.println("Start uploading second file");
            OutputStream outputStream = ftpClient.storeFileStream(secondRemoteFile);
            byte[] bytesIn = new byte[4096];
            int read = 0;

            while ((read = inputStream.read(bytesIn)) != -1) {
                outputStream.write(bytesIn, 0, read);
            }
            inputStream.close();
            outputStream.close();

            boolean completed = ftpClient.completePendingCommand();
            if (completed) {
                System.out.println("The second file is uploaded successfully.");
            }

*/

            } catch (IOException ex) {
                String log = "Oops! Something wrong happened during connection to " + ip;
                Logger.printLog(log);
                log = "ERROR ---> " + ex.getMessage();
                Logger.printLog(log);
            } finally {
                try {
                    if (ftpClient.isConnected()) {
                        ftpClient.logout();
                        ftpClient.disconnect();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                    String log = "ERROR ---> " + ex.getMessage();
                    Logger.printLog(log);
                }
            }
            Logger.printLog("****************************************\n");
        }
    }
}