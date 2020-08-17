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
                int len = attributes.getLength();
                String currentSheetName="";
                for (int i=0; i<len;i++ ){
                    String attName = attributes.getQName(0);
                    if(attName.equals("ss:Name")) {
                        currentSheetName = attributes.getValue(0);
                        break;
                    }
                }
                if (currentSheetName.compareToIgnoreCase(worksheet) == 0) {
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
