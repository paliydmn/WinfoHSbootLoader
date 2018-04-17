package paliy;

import javafx.scene.control.TextArea;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MyLogger {
    private static TextArea txtAreaLog;
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

    void setTxtAreaLog(TextArea txtAreaLog) {
        MyLogger.txtAreaLog = txtAreaLog;
    }

    static void printLog(String log){
        String time = sdf.format(Calendar.getInstance().getTime());
        txtAreaLog.appendText(time + "     " + log);
        txtAreaLog.appendText("\n");
        System.out.println(time + "     " + log);
    }
}
