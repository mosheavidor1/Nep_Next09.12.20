package Utils.Data;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import java.util.ArrayList;
import java.util.List;

class SAXHandler extends DefaultHandler {

    public List<XmlRow> xmlRowList;
    private XmlRow xmlRow;
    private StringBuilder content;
    private String worksheet;
    private boolean parse;
    private boolean sheetWasParsed;
    private String currentCellContent;

    public SAXHandler(String workSheetName){
        worksheet =workSheetName;
        parse =false;
        sheetWasParsed = false;
        xmlRowList = new ArrayList<>();
        xmlRow = null;
        content = new StringBuilder();
        currentCellContent ="";

    }

    @Override
    //Triggered when the start of tag is found.
    public void startElement(String uri, String localName, String qName, Attributes attributes)  {
        if(! sheetWasParsed) {
            if (!parse && qName.equals("Worksheet")) {
                String currentSheetName="";
                currentSheetName=attributes.getValue("ss:Name");
                if (currentSheetName!= null && currentSheetName.compareToIgnoreCase(worksheet) == 0) {
                    parse = true;
                }

            }
            else {
                switch (qName) {
                    //Create a new Row object when the start tag is found
                    case "Row":
                        xmlRow = new XmlRow();
                        break;
                     //if it is a new cell delete current value of content
                    case "Cell":
                        String index = null;
                        index = attributes.getValue("ss:Index");
                        //if cell contains ss:Index attribute that means there empty cells before it - creating a loop to fill it
                        if(index!=null){
                            int num = Integer.parseInt(index);
                            for(int i=xmlRow.cellList.size(); i<num-1;i++){
                                xmlRow.cellList.add("");
                            }
                        }
                        content.setLength(0);
                        break;
                }
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName)  {
        if (parse) {
            switch (qName) {
                case "Row":
                    xmlRowList.add(xmlRow);
                    break;
                case "Data":
                    currentCellContent = content.toString();
                    break;
                case "Cell":
                    xmlRow.cellList.add(currentCellContent);
                    currentCellContent="";
                    break;
                case "Worksheet":
                    parse = false;
                    sheetWasParsed = true;
                    break;
            }
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (parse) {
            content.append(ch, start, length);
        }

    }
}
