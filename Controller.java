package paliy;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import paliy.model.MKItem;
import paliy.model.MikrotikDAO;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Controller {

    private static final String ARP_GET_IP_HW = "arp -a";
    private static final String TRACERT_GET_HOST_HW = "tracert -h 5 -w 20";
    private static final String[] MIKROTIK_MACS = {"E4-8D-8C", "6C-3B-6B", "CC-2D-E0", "4C-5E-0C", "D4-CA-6D", "00-0C-42", "64-D1-54"};
    private static Logger logger;
    private final CollectionMK collectionMK = new CollectionMK();
    public Button btnRescan;
    public Button btnUpload;
    public Button btnSelectFile;
    public Button btnGetFullData;
    public TextField txtFieldScriptPath;
    public TableView<MKItem> tblMainTable;
    public TableColumn<String, String> clmnIp;
    public TableColumn<String, String> clmnMac;
    public TableColumn<String, String> clmnName;
    public TableColumn<String, String> clmnROS;
    public TableColumn<String, String> clmnDescrip;

    public TextArea txtAreaLog;
    public ProgressBar progress;
    public Label statusLabel;
    private String NET = "";
    private ScanMK scanner = new ScanMK();

    @FXML
    public void initialize() {
        logger = new Logger();
        logger.setTxtAreaLog(txtAreaLog);

        tblMainTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tblMainTable.setEditable(true);
        tblMainTable.getSelectionModel().setCellSelectionEnabled(true);

        clmnIp.setCellValueFactory(new PropertyValueFactory<String, String>("ip"));
        clmnMac.setCellValueFactory(new PropertyValueFactory<String, String>("MAC"));
        clmnName.setCellValueFactory(new PropertyValueFactory<String, String>("name"));
        clmnROS.setCellValueFactory(new PropertyValueFactory<String, String>("ros"));
        clmnDescrip.setCellValueFactory(new PropertyValueFactory<String, String>("description"));

        scanner.scan();//findMK();
        tblMainTable.setItems(collectionMK.getMKList());


        ObservableList<TablePosition> selectedCells = tblMainTable.getSelectionModel().getSelectedCells();
        selectedCells.addListener((ListChangeListener.Change<? extends TablePosition> change) -> {
            if (selectedCells.size() > 0) {
                TablePosition selectedCell = selectedCells.get(0);
                TableColumn column = selectedCell.getTableColumn();
                int rowIndex = selectedCell.getRow();
//                Object data = column.getCellObservableValue(rowIndex).getValue();
            }
        });
       /* txtFieldScriptPath.textProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("textfield changed from " + oldValue + " to " + newValue);
        });*/

        logger.printLog("Init completed!");
    }

    public void onGetFullData(ActionEvent actionEvent) {
        MKConnectionManager.MKHelper mk = new MKConnectionManager.MKHelper();
        mk.collectData(collectionMK);

        tblMainTable.setItems(collectionMK.getMKList());

    }

    private boolean isMikrotik(String mac) {
        for (String str : MIKROTIK_MACS) {
            if (str.equals(mac.substring(0, 8)))
                return true;
        }
        return false;
    }

    private List<String> getARPTable() throws IOException {
        Scanner s = new Scanner(Runtime.getRuntime().exec(Controller.ARP_GET_IP_HW).getInputStream()).useDelimiter("\\A");
        List<String> arpList = new ArrayList<>();
        Map<String, String> ipMacMap = new HashMap<>();

        while (s.hasNext()) {
            arpList.add(s.nextLine());
            /* String tmp =s.nextLine();
            String ip;
            String mac;
            if(!((ip = getIp(tmp)) == null)){
                arpList.add(ip);
                if(!((mac = getMAC(tmp)) == null)) {
                    arpList.add(mac);
                    ipMacMap.put(ip,mac);
                }
            }*/
        }
        s.close();
        return arpList;
    }

    public List<String> getHostName(String cmd, List<String> ipList) {

        List<String> hostList = new ArrayList<>();
        for (String ip : ipList) {
            Scanner s = null;
            try {
                s = new Scanner(Runtime.getRuntime().exec(cmd + " " + ip).getInputStream()).useDelimiter("\\A");
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (s != null && s.hasNext()) {
                String tmp = s.nextLine();
                if (tmp.contains("MikroTik"))
                    System.out.println("MikroTik - " + ip);
            }
            if (s != null) {
                s.close();
            }
        }
        return hostList;
    }

    public void onUpload() {

        for (MKItem mk : collectionMK.getMKList()) {
            //new Thread(() -> new MKConnectionManager().upload(mk.getIp())).start();
            try {
                Thread.sleep(250);
                MKConnectionManager.FTPHelper.upload(mk.getIp());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //write on boot script to routers
            new MKConnectionManager.MKHelper().setOnBootScript(collectionMK);
        }
    }

    public void onRescan() {
        if (scanner.isRunning()) {
            scanner.scan();
        } else {
            scanner = new ScanMK();
            scanner.scan();
        }

    }

    private String getIp(String ipStr) {
        String IPADDRESS_PATTERN =
                "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";

        Pattern pattern = Pattern.compile(IPADDRESS_PATTERN);
        Matcher matcher = pattern.matcher(ipStr);
        if (matcher.find()) {
            return matcher.group();
        } else {
            return null;
        }
    }

    private String getMAC(String macStr) {
        String MACADDRESS_PATTERN =
                "(?:[0-9a-fA-F][-:]?){12}";

        Pattern pattern = Pattern.compile(MACADDRESS_PATTERN);
        Matcher matcher = pattern.matcher(macStr.toUpperCase());
        if (matcher.find()) {
            return matcher.group();
        } else {
            return null;
        }
    }

    public void onFileSelect() {
        FileChooser fileChooser = new FileChooser();
        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            txtFieldScriptPath.setText(selectedFile.getAbsolutePath());
        }
    }

    private class PingTask extends Task<Integer> {

        @Override
        protected Integer call() throws Exception {
            Integer counter = 0;
            InetAddress inet;
            if (!NET.equals("")) {
                String ipAddress = NET.substring(0, NET.lastIndexOf(".") + 1);
                for (int i = 1; i <= 254; i++) {
                    if (!isCancelled()) {
                        try {
                            inet = InetAddress.getByName(ipAddress + i);
                            boolean reachable = inet.isReachable(20);
                            System.out.println(i + " = " + reachable);
                            if (reachable) {
                                String log = ipAddress + i + " reachable";
                                Logger.printLog(log);
                            }
                            counter = i;
                            this.updateProgress(i, 254);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    this.updateMessage(String.valueOf(counter));
                }
            }
            return counter;
        }
    }
    // PingTask pingTask;
    private class ScanMK {

        final PingTask pingTask = new PingTask();
        boolean isRunning() {
            return pingTask.isRunning();
        }

        void scan() {
            List<String> arpList = null;
            try {
                arpList = getARPTable();
            } catch (IOException e) {
                e.printStackTrace();
            }

            NET = getIp(arpList.get(1));
            if (collectionMK.getMKList() != null && !collectionMK.getMKList().isEmpty()) {
                collectionMK.getMKList().clear();
            }

            for (String arpLine : arpList) {
                String ip;
                String mac;
                if (!((ip = getIp(arpLine)) == null) && !((mac = getMAC(arpLine)) == null)) {
                    if (ip.startsWith(NET.substring(0, 3))) {
                        if (isMikrotik(mac)) {
                            MKItem mk = new MKItem();

                            mk.setId(1);
                            mk.setIp(ip);
                            mk.setMAC(mac);
                            mk.setName("");
                            mk.setRos("");
                            mk.setDescription("");

                            //collectionMK.add(new MKItem(ip, mac, "", ""));
                            collectionMK.add(mk);
                            String log = "found: " + ip + " - " + mac;
                            logger.printLog(log);
                        }
                    }
                }
            }

            final ProgressIndicator progressIndicator = new ProgressIndicator(0);

            if (!pingTask.isRunning()) {
                btnRescan.setText("Stop");

                progress.progressProperty().unbind();
                progress.setProgress(0);
                progressIndicator.setProgress(0);

                progress.progressProperty().unbind();
                progress.progressProperty().bind(pingTask.progressProperty());
                progressIndicator.progressProperty().unbind();
                progressIndicator.progressProperty().bind(pingTask.progressProperty());

                statusLabel.textProperty().unbind();
                statusLabel.textProperty().bind(pingTask.messageProperty());

                pingTask.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED,
                        t -> {
                            statusLabel.textProperty().unbind();
                            statusLabel.setText(String.valueOf(pingTask.getValue()));
                        });

                new Thread(pingTask).start();
            } else {
                btnRescan.setText("Rescan");

                pingTask.cancel(true);
                pingTask.cancel();
                progress.progressProperty().unbind();
                progressIndicator.progressProperty().unbind();
                statusLabel.textProperty().unbind();
                //
                progress.setProgress(0);
                progressIndicator.setProgress(0);
            }

            //System.out.println(getARPTable(ARP_GET_IP_HW ));
            //Get host by ip
            // System.out.println(getHostName(TRACERT_GET_HOST_HW, currentNetIpList));
            System.out.println("Done");
        }
    }
    public void OnWriteToDB(ActionEvent actionEvent) {
        for(MKItem mk : collectionMK.getMKList()){

            try {
                Logger.printLog("Write to db ->>> ");
                if(MikrotikDAO.insertBook(
                        mk.getIp(),
                        mk.getMAC(),
                        mk.getName(),
                        mk.getRos(),
                        mk.getDescription())){
                    Logger.printLog("Entry is added successfully <<<-");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

    }
}
