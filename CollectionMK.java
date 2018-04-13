package paliy;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import paliy.model.MKItem;

public class CollectionMK {
    private ObservableList<MKItem> mkList = FXCollections.observableArrayList();

    public void add(MKItem mk) {
        mkList.add(mk);
    }
    public void clear() {
        mkList.clear();

    }

    public ObservableList<MKItem> getMKList() {
        return mkList;
    }
}
