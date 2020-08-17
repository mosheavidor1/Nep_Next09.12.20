package Utils.Data;

import java.util.ArrayList;

class XmlRow {
    ArrayList<String> cellList = new ArrayList<>();

    @Override
    public String toString() {
        return cellList.toString();
    }
}
