package com.RadioMSG;

/**
 * Created by jdouyere on 13/02/17, based on code from "carlosefonseca" on GitHubGist
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class GPXWriter {

    public static void writeGpxFile(String fileName, String n, List<RMsgObject> objectList) {

        String fullFileName = RMsgProcessor.HomePath + RMsgProcessor.Dirprefix + fileName;
        File mFile = new File(fullFileName);

        String header = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?><gpx xmlns=\"http://www.topografix.com/GPX/1/1\" creator=\"MapSource 6.15.5\" version=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"  xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\"><trk>\n";
        String name = "<name>" + n + "</name><trkseg>\n";

        String segments = "";
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
        Date gpsDate;
        String dateStr = "";
        for (RMsgObject msg : objectList) {
            if (msg.position != null) {
                //segments += "<trkpt lat=\"" + msg.position.getLatitude() + "\" lon=\"" + msg.position.getLongitude() +
                //        "\"><time>" + df.format(new Date(msg.position.getTime())) + "</time></trkpt>\n";
                //Copied from PC Java version
                //2021-02-15_173632.txt
                try {
                    dateStr = msg.fileName.substring(0, msg.fileName.indexOf("."));
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'_'HHmmss", Locale.US);
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    gpsDate = sdf.parse(dateStr);
                } catch (Exception e) {
                    gpsDate = new Date();
                }
                segments += "<trkpt lat=\"" + msg.position.getLatitude() + "\" lon=\"" + msg.position.getLongitude() +
                        "\"><time>" + df.format(gpsDate) + "</time></trkpt>\n";
                //oldstring = "2011-01-18 00:00:00.0";
            }
        }

        String footer = "</trkseg></trk></gpx>";

        try {
            FileWriter writer = new FileWriter(mFile, false);
            writer.append(header);
            writer.append(name);
            writer.append(segments);
            writer.append(footer);
            writer.flush();
            writer.close();
            //if (Config.isDEBUG())
            //Log.i(TAG, "Saved " + points.size() + " points.");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            //Log.e(TAG, "Error Writting Path",e);
        }
    }
}