package Utils.EventsLog;

import java.io.IOException;

public class LogEntry {

    public String eventType;
    public String eventID;
    public String eventLog;
    public String eventSource;
    public String eventDescription;
    public boolean addedTimeToDescription;
    public String stampAdded;


    public LogEntry(String eventType, String eventID, String eventLog, String eventSource, String eventDescription, boolean addedTimeToDescription) {


        this.eventType = eventType;
        this.eventID = eventID;
        this.eventLog = eventLog;
        this.eventSource = eventSource;
        this.eventDescription = eventDescription;
        this.addedTimeToDescription = addedTimeToDescription;
        this.stampAdded = "";


    }

    public LogEntry(String eventType, String eventID, String eventLog, String eventSource, String eventDescription, String addedTimeToDescription) {

        this.addedTimeToDescription = false;
        if(addedTimeToDescription.trim().compareToIgnoreCase("Yes") == 0 )
            this.addedTimeToDescription= true;

        this.eventType = eventType;
        this.eventID = eventID;
        this.eventLog = eventLog;
        this.eventSource = eventSource;
        this.eventDescription = eventDescription;
        this.stampAdded = "";


    }

    public void AddTimeToDescription (String time){
        stampAdded = "Time Stamp: " + time;

        eventDescription += " " + stampAdded;
    }

}

