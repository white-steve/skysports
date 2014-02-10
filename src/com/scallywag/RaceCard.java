/*
 * Copyright (c) 2014. This project contains secret material that may not be exposed without the written consent of its owners.
 *
 * Steve White.
 */

package com.scallywag;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by steve on 19/12/13.
 */
public class RaceCard {
    private final String skyUrl = "http://www1.skysports.com/";
    private final String racingUkUrl = "http://www.racinguk.com/racecard/";
    private final String driverName = "com.mysql.jdbc.Driver";
    private final String connectString = "jdbc:mysql://localhost/steve?user=steve&password=steve";
    private final String entrantInsert = "INSERT INTO steve.entrant (name,number,form,Jockey,trainer,weight,age,date,time,course,lastrace) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
    private final String historyInsert = "INSERT INTO steve.result (name,date,course,going,distance,class,winner,winprice,position,weight,jockey,trainer,sp,db,racetime,horsetime,runtype,mph )VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    private final String historyUpdate = "Update steve.result set course=?,going=?,distance=?,class=?,winner=?,winprice=?,position=?,weight=?,jockey=?,trainer=?,sp=?,db=?,racetime=?,horsetime=?,runtype=?,mph=? where date=? and name=?";
    private final String meetingUpdate = "Update steve.meeting set title=?,going=?,surface=?,class=?,distance=?,runners=? where course=? and time=? and date=?";
    private final String meetingInsert = "INSERT INTO steve.meeting (course,title,time,date,going,surface,class,distance,runners) VALUES (?,?,?,?,?,?,?,?,?)";
    private final String horseShortInsert = "INSERT INTO steve.horse (name,breeding,age) VALUES (?,?,?)";
    private final String horseFullInsert = "INSERT INTO steve.horse (name,breeding,age,yof,sex,sire,dam,trainer,rating_flat,rating_awt,rating_chase,rating_hurdle) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
    private final String statsInsert = "INSERT INTO steve.statistics (name,racetype,starts,percentfirst,wins,seconds,thirds) VALUES (?,?,?,?,?,?,?)";
    private final String selectCourse = "SELECT name from steve.courseofinterest where name=?";
    private final String updateStats = "update steve.statistics set starts=?,percentfirst=?,wins=?,seconds=?,thirds=? where name=? and racetype=?";
    private Date cardDate;
    private String date;
    private Map<String, Course> m_courses;
    private Map<String, xrefObject> m_xref;
    private PreparedStatement m_entrantPreparedInsert = null;
    private PreparedStatement m_historyPreparedInsert = null;
    private PreparedStatement m_historyPreparedUpdate = null;
    private PreparedStatement m_meetingPreparedInsert = null;
    private PreparedStatement m_updateMeeting = null;
    private PreparedStatement m_horsePreparedInsert = null;
    private PreparedStatement m_horsePreparedFullInsert = null;
    private PreparedStatement m_horseStatsisticsInsert = null;
    private PreparedStatement m_courseSelect = null;
    private PreparedStatement m_updateStats = null;

    public RaceCard(String date) {
        this.setDate(date);
        m_courses = new HashMap<String, Course>();
        setXref(new HashMap<String, xrefObject>());
    }

    private boolean initPreparedStatements(Connection conn) {

        Logger logger = Logger.getLogger("Loader");
        try {
            Class.forName(driverName).newInstance();
        } catch (Exception ex) {
            logger.debug("VendorError: " + ex.getMessage());
        }

        try {
            conn = DriverManager.getConnection(connectString);
        } catch (SQLException ex) {
            logger.debug("SQLException: " + ex.getMessage());
            logger.debug("SQLState: " + ex.getSQLState());
            logger.debug("VendorError: " + ex.getErrorCode());
            return false;
        }

        try {
            m_entrantPreparedInsert = conn.prepareStatement(entrantInsert);
            m_historyPreparedInsert = conn.prepareStatement(historyInsert);
            m_historyPreparedUpdate = conn.prepareStatement(historyUpdate);
            m_meetingPreparedInsert = conn.prepareStatement(meetingInsert);
            m_horsePreparedInsert = conn.prepareStatement(horseShortInsert);
            m_horsePreparedFullInsert = conn.prepareStatement(horseFullInsert);
            m_horseStatsisticsInsert = conn.prepareStatement(statsInsert);
            m_courseSelect = conn.prepareStatement(selectCourse);
            m_updateStats = conn.prepareStatement(updateStats);
            m_updateMeeting = conn.prepareStatement(meetingUpdate);
        } catch (SQLException e) {
            logger.debug(e.getMessage());
            return false;
        }
        return true;
    }

    public RaceCard createRaceCard(String dateOf) {
        return new RaceCard(dateOf);
    }

    public Date getCardDate() {
        return cardDate;
    }

    public void setCardDate(Date cardDate) {
        this.cardDate = cardDate;
    }

    public Map<String, Course> getCourses() {
        return m_courses;
    }

    public void setCourses(Map<String, Course> courses) {
        this.m_courses = courses;
    }

    public void LoadSkyRaceCard(String Url) {
        Logger logger = Logger.getLogger("Loader");
        org.jsoup.select.Elements tables = null;
        org.jsoup.select.Elements results;
        Boolean done = false;
        Boolean normalPage = true;

        Connection conn = null;

        if (!initPreparedStatements(conn)) {
            logger.debug("Failed to connect to database");
            return;
        }

        // Parse str into a Document
        org.jsoup.nodes.Document doc = null;
        while (!done) {
            try {
                doc = Jsoup.connect(Url).get();
                if (Url.contains("early")) {
                    normalPage = false;
                }
                done = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (normalPage)
            processNormalPage(logger, tables, doc);
        else
            processEarlyPage(logger, tables, doc);
    }

    //
    // Used by Special Cheltenham Code
    //
    //

    private void processEarlyPage(Logger logger, Elements tables, Document doc) {


        if (doc != null) {
            tables = doc.getElementsByAttributeValue("class", "v5-tbl-t1");
            int stop = 1;
        }

        String Going = "Splendid";
        String Surface = "Gold";

        for (Element myElement : tables) {


            Elements childrenOfTable = myElement.children();


            //Elements tableElements = childrenOfTable.getElementsByAttribute("caption");

            Element tableElement = myElement.nextElementSibling();
            Element caption = childrenOfTable.get(0);
            String captionStr = caption.text();

            int pos = captionStr.indexOf("Racecard");
            String courseName = captionStr.substring(0, pos - 1);

            try {
                m_courseSelect.setString(1, courseName);
            } catch (SQLException e) {
                logger.trace(e.getMessage());
            }

            ResultSet rs = null;
            try {
                rs = m_courseSelect.executeQuery();
            } catch (SQLException e) {
                logger.trace(e.getMessage());
            }

            try {
                if (!rs.next()) {
                    logger.debug("Ïgnoring " + courseName);
                    continue;
                }
            } catch (SQLException e) {
                logger.trace(e.getMessage());
            }
            // Create the Race Course
            Course raceCourse = Course.createCourse(captionStr);
            raceCourse.setGoing(Going);
            raceCourse.setSurface(Surface);

            m_courses.put(captionStr, raceCourse);

            logger.debug("Processing " + captionStr);

            Elements children = tableElement.children();


            Element body = childrenOfTable.get(3);
            Elements bodyChildren = body.children();
            int stop = 0;
            for (Element hrefChild : bodyChildren) {
                if (hrefChild.tagName() == "tr") {
                    String meetingName = hrefChild.text();
                    Elements href = hrefChild.getElementsByAttribute("href");

                    Element hrefElement;
                    try {
                        hrefElement = href.get(0);
                    } catch (Exception e) {
                        continue;
                    }

                    String time = hrefElement.text();
                    hrefElement = href.get(0);
                    Attributes hrefT = hrefElement.attributes();
                    String hrefAttribute = hrefT.get("href");
                    String fullUrl = skyUrl + hrefAttribute;
                    String fullracingUkUrl = racingUkUrl;

                    logger.debug("Processing " + time + " " + meetingName);


                    Meeting theMeeting = Meeting.createMeeting(courseName, meetingName, "01-01-2014", "00:00", fullUrl, fullracingUkUrl, 0, 0, 0);

                    theMeeting.populateEarly();

                    raceCourse.getMeetings().put(meetingName, theMeeting);

                    if (!saveToMySql(captionStr, Going, Surface, meetingName, theMeeting)) {
                        logger.debug("Failed saving " + time + " " + meetingName);
                    } else {
                        logger.debug("Meeting saved " + time + " " + meetingName);
                    }

                }
            }
        }
    }

    private void processNormalPage(Logger logger, Elements tables, Document doc) {
        if (doc != null) {
            tables = doc.getElementsByAttributeValueContaining("class", "v5-list-t3");
            int stop = 1;
        }

        String Going = null;
        String Surface = null;

        for (Element myElement : tables) {

            for (Element condElement : myElement.children()) {
                if (condElement.tagName() == "li") {

                    String x = condElement.childNode(0).toString();
                    String y = condElement.childNode(1).toString();

                    if (x.contains("Going:"))
                        Going = y;
                    else
                        Surface = y;
                }

            }

            Element tableElement = myElement.nextElementSibling();

            Attributes caption = tableElement.attributes();
            String captionStr = caption.get("summary");

            int pos = captionStr.indexOf("Racecard");
            String courseName = captionStr.substring(0, pos - 1);

            try {
                m_courseSelect.setString(1, courseName);
            } catch (SQLException e) {
                logger.trace(e.getMessage());
            }

            ResultSet rs = null;
            try {
                rs = m_courseSelect.executeQuery();
            } catch (SQLException e) {
                logger.trace(e.getMessage());
            }

            try {
                if (!rs.next()) {
                    logger.debug("Ïgnoring " + courseName);
                    continue;
                }
            } catch (SQLException e) {
                logger.trace(e.getMessage());
            }
            // Create the Race Course
            Course raceCourse = Course.createCourse(captionStr);
            raceCourse.setGoing(Going);
            raceCourse.setSurface(Surface);

            m_courses.put(captionStr, raceCourse);

            logger.debug("Processing " + captionStr);

            Elements children = tableElement.children();

            for (Element bdy : children) {

                String res = bdy.tag().getName();

                if (res == "tbody") {

                    Elements bodyChildren = bdy.children();

                    for (Element hrefChild : bodyChildren) {
                        if (hrefChild.tagName() == "tr") {
                            String meetingName = hrefChild.text();

                            if (meetingName.contains(") result"))
                                continue;

                            Elements href = hrefChild.getElementsByAttribute("href");

                            Element hrefElement;
                            try {
                                hrefElement = href.get(0);
                            } catch (Exception e) {
                                continue;
                            }

                            String time = hrefElement.text();
                            hrefElement = href.get(1);
                            Attributes hrefT = hrefElement.attributes();
                            String hrefAttribute = hrefT.get("href");
                            String fullUrl = skyUrl + hrefAttribute;
                            String fullracingUkUrl = racingUkUrl;

                            logger.debug("Processing " + time + " " + meetingName);

                            ExtractDetailsFromTitle extractDetailsFromTitle = new ExtractDetailsFromTitle(logger, meetingName);

                            try {
                                extractDetailsFromTitle.invoke();
                            } catch (Exception e) {
                                logger.debug("Caught critical error in parsing meetingName " + meetingName);
                                e.printStackTrace();
                            }

                            String shortMeetingName = meetingName;

                            int iClass = extractDetailsFromTitle.getiClass();
                            int iDistance = extractDetailsFromTitle.getiDistance();
                            int runners = extractDetailsFromTitle.getRunners();

                            Meeting theMeeting = Meeting.createMeeting(courseName, shortMeetingName, getDate(), time, fullUrl, fullracingUkUrl, iClass, iDistance, runners);

                            theMeeting.populate();

                            raceCourse.getMeetings().put(shortMeetingName, theMeeting);

                            if (!saveToMySql(captionStr, Going, Surface, shortMeetingName, theMeeting)) {
                                logger.debug("Failed saving " + time + " " + shortMeetingName);
                            } else {
                                logger.debug("Meeting saved " + time + " " + shortMeetingName);
                            }

                        }
                    }
                }
            }
        }
    }

    private boolean saveToMySql(String courseName, String Going, String Surface, String mtgName, Meeting mtg) {

        Logger logger = Logger.getLogger("Loader");
        logger.setLevel(Level.DEBUG);

        java.sql.Date sqlDate = null;


        if (getCardDate() == null) {    // card date not set FIXME
            cardDate = new Date();
        }

        logger.debug("Storing data for the meeting " + mtgName);
        int offset = courseName.indexOf(" Racecards");
        String shortName = courseName.substring(0, offset).trim();
        try {
            m_meetingPreparedInsert.setString(1, shortName);
            m_meetingPreparedInsert.setString(2, mtg.getDescription());
            m_meetingPreparedInsert.setTime(3, Time.valueOf(mtg.getTimeOfRace() + ":00"));
            sqlDate = new java.sql.Date(mtg.getDateTime().getTime());
            m_meetingPreparedInsert.setDate(4, sqlDate);
            m_meetingPreparedInsert.setString(5, Going);
            m_meetingPreparedInsert.setString(6, Surface);
            m_meetingPreparedInsert.setString(7, Integer.toString(mtg.get_iClass()));
            m_meetingPreparedInsert.setString(8, mtg.get_iDistance() + "f");
            m_meetingPreparedInsert.setInt(9, mtg.getM_iRunners());
        } catch (SQLException e) {
            //logger.debug("SQLException: Insert " + e.getMessage());
        }
        int modified = 0;
        try {
            modified = m_meetingPreparedInsert.executeUpdate();
            int stop = 1;
        } catch (SQLException e) {
            //logger.error("SQLException: Meeting Insert " + e.getMessage());
        }
        if (modified == 0) {
            try {

                sqlDate = new java.sql.Date(mtg.getDateTime().getTime());
                m_updateMeeting.setString(1, mtg.getDescription());
                m_updateMeeting.setString(2, Going);
                m_updateMeeting.setString(3, Surface);
                m_updateMeeting.setString(4, Integer.toString(mtg.get_iClass()));
                m_updateMeeting.setString(5, mtg.get_iDistance() + "f");
                m_updateMeeting.setInt(6, mtg.getM_iRunners());
                m_updateMeeting.setString(7, shortName);
                m_updateMeeting.setString(8, mtg.getDescription());
                m_updateMeeting.setTime(9, Time.valueOf(mtg.getTimeOfRace() + ":00"));
                try {
                    modified = m_updateMeeting.executeUpdate();
                    int stop = 1;
                } catch (SQLException se) {
                    logger.error("SQLException: Meeting Update " + se.getMessage());
                }
            } catch (SQLException e1) {
                logger.error("SQLException: Update " + e1.getMessage());
                e1.printStackTrace();
            }
        }

        logger.debug("Stored Meeting " + mtg.getDescription() + " " + mtg.getDateTime());

        for (Map.Entry<String, Entry> entrantEntry : mtg.getEntrants().entrySet()) {
            String entrantKey = entrantEntry.getKey();
            Entry entrantValue = entrantEntry.getValue();

            try {
                m_entrantPreparedInsert.setString(1, entrantValue.getHorseName());
                m_entrantPreparedInsert.setString(2, cleanClothNumber(entrantValue));
                m_entrantPreparedInsert.setString(3, entrantValue.getForm());
                m_entrantPreparedInsert.setString(4, entrantValue.getJockey());
                m_entrantPreparedInsert.setString(5, entrantValue.getTrainer());
                m_entrantPreparedInsert.setString(6, entrantValue.getWeight());
                m_entrantPreparedInsert.setString(7, entrantValue.getAge());
                m_entrantPreparedInsert.setDate(8, sqlDate);
                m_entrantPreparedInsert.setTime(9, Time.valueOf(mtg.getTimeOfRace() + ":00"));
                m_entrantPreparedInsert.setString(10, entrantValue.getCourse());
                m_entrantPreparedInsert.setString(11, entrantValue.getDaysSinceLast());

            } catch (SQLException e) {
                logger.debug("SQLException: " + e.getMessage());
            }
            try {
                modified = m_entrantPreparedInsert.executeUpdate();
            } catch (SQLException e) {
                //logger.error("SQLException: " + e.getMessage());
            }


            if (entrantValue.getHorse() == null)
                continue;

            xrefObject myX;
            myX = getXref().get(entrantValue.getHorse().getHorseName());

            if (myX == null) {

                try {
                    m_horsePreparedInsert.setString(1, entrantValue.getHorseName());
                    m_horsePreparedInsert.setString(2, entrantValue.getHorse().getBreeding());
                    m_horsePreparedInsert.setString(3, entrantValue.getHorse().getAge());
                } catch (SQLException e) {
                    logger.debug("SQLException: " + e.getMessage());
                }
                try {
                    modified = m_horsePreparedInsert.executeUpdate();
                } catch (SQLException e) {
                    //logger.error("SQLException: " + e.getMessage());
                }
            } else {
                if (myX != null) {
                    try {
                        m_horsePreparedFullInsert.setString(1, entrantValue.getHorseName());
                        m_horsePreparedFullInsert.setString(2, entrantValue.getHorse().getBreeding());
                        m_horsePreparedFullInsert.setString(3, entrantValue.getHorse().getAge());
                        m_horsePreparedFullInsert.setString(4, myX.getYof());
                        m_horsePreparedFullInsert.setString(5, myX.getSex());
                        m_horsePreparedFullInsert.setString(6, myX.getSire());
                        m_horsePreparedFullInsert.setString(7, myX.getDam());
                        m_horsePreparedFullInsert.setString(8, myX.getTrainer());
                        m_horsePreparedFullInsert.setString(9, myX.getFlatrating());
                        m_horsePreparedFullInsert.setString(10, myX.getAwtrating());
                        m_horsePreparedFullInsert.setString(11, myX.getChaserating());
                        m_horsePreparedFullInsert.setString(12, myX.getHurdlerating());
                    } catch (SQLException e) {
                        logger.debug("SQLException: " + e.getMessage());
                    }
                    try {
                        modified = m_horsePreparedFullInsert.executeUpdate();
                    } catch (SQLException e) {
                        //logger.error("SQLException: " + e.getMessage());
                    }
                }
            }

            for (Result horseResult : entrantValue.getHorse().getHistory()) {

                DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
                Date date = null;
                try {
                    date = formatter.parse(horseResult.getDate());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                modified = 0;
                java.sql.Date esqlDate = new java.sql.Date(date.getTime());
                try {
                    m_historyPreparedInsert.setString(1, entrantValue.getHorseName());
                    m_historyPreparedInsert.setDate(2, esqlDate);
                    m_historyPreparedInsert.setString(3, horseResult.getCourse());
                    m_historyPreparedInsert.setString(4, horseResult.getGoing());
                    m_historyPreparedInsert.setString(5, horseResult.getDistance());
                    m_historyPreparedInsert.setString(6, horseResult.getRaceClass());
                    m_historyPreparedInsert.setString(7, horseResult.getRaceWinner());
                    m_historyPreparedInsert.setString(8, horseResult.getPrice());
                    m_historyPreparedInsert.setString(9, horseResult.getPosition());
                    m_historyPreparedInsert.setString(10, horseResult.getWeight());
                    m_historyPreparedInsert.setString(11, horseResult.getJockey());
                    m_historyPreparedInsert.setString(12, horseResult.getTrainer());
                    m_historyPreparedInsert.setString(13, horseResult.getSP());
                    m_historyPreparedInsert.setString(14, horseResult.getDb());
                    m_historyPreparedInsert.setString(15, horseResult.getRaceTime());
                    m_historyPreparedInsert.setString(16, horseResult.getHorseTime());
                    m_historyPreparedInsert.setString(17, horseResult.getRaceType());
                    m_historyPreparedInsert.setString(18, horseResult.getHorseMph());
                } catch (SQLException e) {
                    logger.debug("SQLException: " + e.getMessage());
                }
                try {
                    modified = m_historyPreparedInsert.executeUpdate();
                } catch (SQLException e) {
                    //logger.debug("SQLException: Insert " + e.getMessage());
                }
                if (modified == 0) {
                    try {

                        m_historyPreparedUpdate.setString(1, horseResult.getCourse());
                        m_historyPreparedUpdate.setString(2, horseResult.getGoing());
                        m_historyPreparedUpdate.setString(3, horseResult.getDistance());
                        m_historyPreparedUpdate.setString(4, horseResult.getRaceClass());
                        m_historyPreparedUpdate.setString(5, horseResult.getRaceWinner());
                        m_historyPreparedUpdate.setString(6, horseResult.getPrice());
                        m_historyPreparedUpdate.setString(7, horseResult.getPosition());
                        m_historyPreparedUpdate.setString(8, horseResult.getWeight());
                        m_historyPreparedUpdate.setString(9, horseResult.getJockey());
                        m_historyPreparedUpdate.setString(10, horseResult.getTrainer());
                        m_historyPreparedUpdate.setString(11, horseResult.getSP());
                        m_historyPreparedUpdate.setString(12, horseResult.getDb());
                        m_historyPreparedUpdate.setString(13, horseResult.getRaceTime());
                        m_historyPreparedUpdate.setString(14, horseResult.getHorseTime());
                        m_historyPreparedUpdate.setString(15, horseResult.getRaceType());
                        m_historyPreparedUpdate.setString(16, horseResult.getHorseMph());
                        m_historyPreparedUpdate.setString(17, entrantValue.getHorseName());
                        m_historyPreparedUpdate.setDate(18, esqlDate);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    try {
                        modified = m_historyPreparedUpdate.executeUpdate();
                    } catch (SQLException se) {
                        logger.debug("SQLException: Update " + se.getMessage());
                    }
                }
            }

            // Now lets cycle the stats
            modified = 0;
            for (Statistics horseStats : entrantValue.getHorse().getStats()) {

                try {
                    m_horseStatsisticsInsert.setString(1, entrantValue.getHorseName());
                    m_horseStatsisticsInsert.setString(2, horseStats.getRaceType());
                    m_horseStatsisticsInsert.setString(3, horseStats.getStarts().toString());
                    m_horseStatsisticsInsert.setString(4, horseStats.getPercentFirst());
                    m_horseStatsisticsInsert.setInt(5, horseStats.getWins());
                    m_horseStatsisticsInsert.setInt(6, horseStats.getSeconds());
                    m_horseStatsisticsInsert.setInt(7, horseStats.getThirds());
                } catch (SQLException e) {
                    logger.debug("SQLException: " + e.getMessage());
                }
                try {
                    modified = m_horseStatsisticsInsert.executeUpdate();

                } catch (SQLException e) {
                    //logger.error("SQLException: " + e.getMessage());
                }
                if (modified == 0) {
                    try {
                        m_updateStats.setString(6, entrantValue.getHorseName());
                        m_updateStats.setString(7, horseStats.getRaceType());
                        m_updateStats.setString(1, horseStats.getStarts().toString());
                        m_updateStats.setString(2, horseStats.getPercentFirst());
                        m_updateStats.setInt(3, horseStats.getWins());
                        m_updateStats.setInt(4, horseStats.getSeconds());
                        m_updateStats.setInt(5, horseStats.getThirds());
                    } catch (SQLException e) {
                        logger.debug("SQLException: " + e.getMessage());
                    }
                    try {
                        modified = m_updateStats.executeUpdate();

                    } catch (SQLException e) {
                        //logger.error("SQLException: " + e.getMessage());
                    }
                }
            }
        }

        return true;
    }

    private String cleanClothNumber(Entry entrantValue) {
        String clothStr = entrantValue.getClothNumber();
        int offBkt = clothStr.indexOf(" (");
        if (offBkt >= 0)
            clothStr = clothStr.substring(0, offBkt);
        return clothStr;
    }

    public Map<String, xrefObject> getXref() {
        return m_xref;
    }


//    private void storeTree(GeneticParent topLevel, Node newParent) {
//
//        // recursive call this until there are no more parents
//        if (topLevel == null) {
//            Node newNode = getGraphDb().createNode(new Label() {
//                @Override
//                public String name() {
//                    return "No Geneology for this horse";
//                }
//            });
//            newNode.setProperty("name", "nomark");
//            Relationship newRelationShip = newNode.createRelationshipTo(newParent, RelTypes.PARENT);
//            newRelationShip.setProperty("message", "no daddy");
//            return;
//        }
//
//        GeneticParent sire = topLevel.getSire();
//        if (sire == null)
//            return;
//
//        Node newNode = getGraphDb().createNode(new Label() {
//            @Override
//            public String name() {
//                return "Parent Sire";
//            }
//        });
//        newNode.setProperty("name", sire.getHorseName());
//        Relationship newRelationShip = newNode.createRelationshipTo(newParent, RelTypes.PARENT);
//        newRelationShip.setProperty("message", "ny daddy");
//        storeTree(sire, newNode);
//
//        GeneticParent dam = topLevel.getDam();
//        if (dam == null)
//            return;
//        newNode = getGraphDb().createNode(new Label() {
//            @Override
//            public String name() {
//                return "Parent Dam";
//            }
//        });
//        newNode.setProperty("name", dam.getHorseName());
//        newRelationShip = newNode.createRelationshipTo(newParent, RelTypes.PARENT);
//        newRelationShip.setProperty("message", "my mummy");
//        storeTree(sire, newNode);
//    }

    public void setXref(Map<String, xrefObject> pxref) {
        this.m_xref = pxref;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    private class ExtractDetailsFromTitle {

        private Logger logger;
        private String meetingName;
        private String shortMeetingName;

        private int iClass;
        private int iDistance;
        private int iRunners;

        public ExtractDetailsFromTitle(Logger logger, String meetingName) {
            this.iClass = 0;
            this.iDistance = 0;
            this.iRunners = 0;
            this.logger = logger;
            this.meetingName = meetingName;
        }

        public String getShortMeetingName() {
            return shortMeetingName;
        }

        public int getiClass() {
            return iClass;
        }

        public int getiDistance() {
            return iDistance;
        }

        public int getRunners() {
            return iRunners;
        }

        public ExtractDetailsFromTitle invoke() throws Exception {

            shortMeetingName = "";
            int len = meetingName.length();
            String[] splitData = {};

            int noOfBraces = 0;
            int offBrace = -1;
            int secBrace = -1;
            String distStr = new String();
            boolean done = false;


            secBrace = meetingName.lastIndexOf('(');
            String tmp = meetingName.substring(0, secBrace - 1);

            if (secBrace >= 0) {
                String ltmp = meetingName.substring(0, secBrace - 1);
                int off = ltmp.lastIndexOf('(');
                if (off >= 0) {
                    offBrace = off;
                    noOfBraces = 2;
                } else {
                    noOfBraces = 1;
                }
            }


            int posAge = 0;
            int posClass = 1;
            int posDist = 2;
            int posRunners = 3;
            int endBrace = meetingName.lastIndexOf(')');
            if (endBrace < 0) {
                throw new Exception("Unmatched braces in input " + meetingName);
            }
            splitData = getFields(meetingName.substring(secBrace + 1, endBrace));


            if (noOfBraces == 2) {
                // Could be the new format  (len) (age,class,runners
                distStr = meetingName.substring(offBrace + 1, secBrace - 2);
                getDistanceFurlongs(distStr);
                posDist = -1;
            }

            for (int i = 1; i < splitData.length; i++) {
                if (splitData[i].contains("Class"))
                    posClass = i;
                else if (splitData[i].contains("runner"))
                    posRunners = i;
                else if (posDist != -1) {
                    posDist = i;
                }
            }

            ///////////////////////////////////////////////////////////
            //   Class Information
            ///////////////////////////////////////////////////////////
            if (posClass >= 0) {

                getClassNumber(splitData[posClass].trim());
            }
            ///////////////////////////////////////////////////////////
            //   Distance Information
            ///////////////////////////////////////////////////////////
            if (posDist >= 0) {
                getDistanceFurlongs(splitData[posDist].trim());
            }
            ///////////////////////////////////////////////////////////
            //   Runners Information
            ///////////////////////////////////////////////////////////
            if (posRunners >= 0) {
                // If we have the number of runners
                getNoOfRunners(splitData[posRunners].trim());
            }

            return this;
        }


        private String[] getFields(String input) {
            String[] result = {};

            result = input.trim().split(",", 4);

            return result;
        }

        private void getClassNumber(String classStr) {
            int num = classStr.indexOf("Class ");
            if (num >= 0 && classStr.length() > 6) {
                iClass = Integer.parseInt(classStr.substring(6));
            }
        }

        private void getDistanceFurlongs(String distanceStr) throws Exception {
            String yardsStr;// carve out the furlongs and yards
            int posM = distanceStr.indexOf("m");
            if (posM > 0) {
                String milesStr = distanceStr.substring(0, posM);
                iDistance = Integer.parseInt(milesStr) * 8;
                posM++; // move it along
            } else {
                posM = 0;
            }
            int posF = distanceStr.indexOf("f");
            if (posF >= 0 && posF > posM) {
                String xxS = distanceStr.substring(posM, posF);
                try {
                    iDistance += Integer.parseInt(xxS.trim());
                } catch (Exception e) {
                    logger.debug("Unable to parse distance string of " + distanceStr);
                    throw (e);
                }
            }
            try {
                if (posF == -1)
                    posF = posM;
                if (distanceStr.indexOf("y") != -1) {
                    yardsStr = distanceStr.substring(posF + 2, distanceStr.length() - 1);
                    if (Integer.parseInt(yardsStr.trim()) >= 150) {
                        iDistance++;
                    }
                }
            } catch (Exception se) {
                throw new Exception("Unable to parse " + distanceStr);

            }

            if (iDistance == 0) {
                throw new Exception("zero distance " + distanceStr);
            }
        }

        private void getNoOfRunners(String runnerStr) throws Exception {

            try {

                int spOff = runnerStr.indexOf(' ');
                if (spOff >= 0) {
                    runnerStr = runnerStr.substring(0, spOff);
                    if (runnerStr != null && runnerStr.length() > 0) {
                        iRunners = Integer.parseInt(runnerStr);
                    }
                }
            } catch (Exception e) {
                throw (e);
            }
        }
    }
}


