package paliy.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class MKItem {
    /*
* CREATE TABLE `Mikrotik` (
	`id`	INTEGER NOT NULL,
	`MAC`	TEXT,
	`name`	TEXT,
	`ros`	TEXT,
	`descripton`	TEXT,
	PRIMARY KEY(id)
);
* */

    private IntegerProperty id;

    private StringProperty ip;
    private StringProperty MAC;
    private StringProperty name;
    private StringProperty ros;
    private StringProperty description;

    public MKItem() {
        this.id = new SimpleIntegerProperty();
        this.ip = new SimpleStringProperty();
        this.MAC = new SimpleStringProperty();
        this.name = new SimpleStringProperty();
        this.ros = new SimpleStringProperty();
        this.description = new SimpleStringProperty();
    }

    public int getId() {
        return id.get();
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public void setId(int id) {
        this.id.set(id);
    }

    public String getIp() {
        return ip.get();
    }

    public StringProperty ipProperty() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip.set(ip);
    }

    public String getMAC() {
        return MAC.get();
    }

    public StringProperty MACProperty() {
        return MAC;
    }

    public void setMAC(String MAC) {
        this.MAC.set(MAC);
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public String getRos() {
        return ros.get();
    }

    public StringProperty rosProperty() {
        return ros;
    }

    public void setRos(String ros) {
        this.ros.set(ros);
    }

    public String getDescription() {
        return description.get();
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public void setDescription(String description) {
        this.description.set(description);
    }
    /*    private String ip;
    private String mac;
    private String name;
    private String ros;

    public MKItem(String ip, String mac, String name, String ros) {
        this.ip = ip;
        this.mac = mac;
        this.name = name;
        this.ros = ros;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRos() {
        return ros;
    }

    public void setRos(String ros) {
        this.ros = ros;
    }*/
}
