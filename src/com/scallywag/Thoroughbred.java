/*
 * Copyright (c) 2014. This project contains secret material that may not be exposed without the written consent of its owners.
 *
 * Steve White.
 */

package com.scallywag;

import com.jaunt.*;
import com.jaunt.component.Table;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.Vector;

/**
 * Created by steve on 20/12/13.
 */
public class Thoroughbred {

    private GeneticParent m_geneticParent;
    private UserAgent m_userAgent;
    private int[] pairs = {1, 2, 4, 8, 16, 32, 32};
    private int[] damDist = {16, 8, 4, 2, 1, 0};

    public Thoroughbred() {
        m_userAgent = new UserAgent();               /* create new userAgent (headless browser). */
    }

    public String getPedigreeInfo(String horseName) {
        Logger logger = Logger.getLogger("Loader");
        logger.debug("getting details for horseName " + horseName);
        org.jsoup.nodes.Document doc = null;
        //
        // jsoup stuff, had to drop int jsoup as jaunt couldn,t handle this
        //
        int offsetOfBracket = horseName.indexOf("(");
        if (offsetOfBracket > 0)
            horseName = horseName.substring(0, offsetOfBracket - 1).trim();
        try {
            doc = Jsoup.connect("http://www.pedigreequery.com/" + horseName).get();
        } catch (org.jsoup.HttpStatusException ee) {
            if (ee.getStatusCode() == 404) {
                String xx = new String("NO Data for " + horseName);
                return xx;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
        org.jsoup.select.Elements links = doc.select("td");
        org.jsoup.nodes.Element link = links.get(20);
        String xxx = link.text();
        return xxx;
        // end jsoup
    }

    public Boolean getPedigreeTable(String horseName) {
        Logger logger = Logger.getLogger("Loader");
        logger.debug("getting details for horseName " + horseName);
        String URL = null;


        Elements infoElements = null;
        Element tblElement = null;
        Elements thElements;


        try {
            m_userAgent.visit("http://www.pedigreequery.com");
        } catch (ResponseException e) {
            e.printStackTrace();
        }
        try {
            m_userAgent.doc.fillout("Horse:", horseName);
        } catch (NotFound notFound) {
            notFound.printStackTrace();
        } catch (MultipleFound multipleFound) {
            multipleFound.printStackTrace();
        }
        try {
            m_userAgent.doc.submit("Horse Search");
        } catch (SearchException e) {
            e.printStackTrace();
        } catch (ResponseException e) {
            e.printStackTrace();
        }

        try {
            tblElement = m_userAgent.doc.findFirst("<table class=\"tablesorter\">");
        } catch (NotFound notFound) {
            logger.debug("no info found for horse " + horseName);
            return false;
        }
        thElements = tblElement.findEvery("<td>");

        for (int i = 0; i < thElements.size(); i++) {
            String xyz;
            try {
                xyz = thElements.getElement(i).innerText();
                // TODO get correct name
                if (xyz.contains("(GB)")) {
                    // find href.
                    String xx;
                    xx = thElements.getElement(i).outerHTML();

                    int offset = xx.indexOf("http");
                    String rest = xx.substring(offset);

                    offset = rest.indexOf("'>");

                    URL = rest.substring(0, offset);

                    break;
                }
            } catch (NodeNotFound nodeNotFound) {
                nodeNotFound.printStackTrace();
            }
        }

        Table tableElement = null;

        if (URL == null)
            return false;

        try {
            m_userAgent.visit(URL); //visit a webpage
        } catch (JauntException e) {
            System.err.println(e);
            return false;
        }
        try {
            m_userAgent.doc.saveAs("ïnfo.html");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            tableElement = m_userAgent.doc.getTable("<table class=pedigreetable>");
        } catch (NotFound notFound) {
            notFound.printStackTrace();
            return false;
        }

        Elements tbRow = tableElement.getRow(0);
        int tblWidth = tbRow.size();
        GeneticParent myParents = new GeneticParent(horseName);
        Elements tblCol = tableElement.getCol(0);
        int tblDepth = tblCol.size();

        Vector<Vector<GeneticParent>> myVector = new Vector<Vector<GeneticParent>>();
        Vector<GeneticParent> newVector = new Vector<GeneticParent>();

        newVector.add(myParents);
        myVector.add(0, newVector);

        for (int v = 0; v <= (tblWidth / 2); v++) {
            Vector<GeneticParent> filler = new Vector<GeneticParent>();
            for (int i = 0; i < pairs[v]; i++) {
                filler.add(new GeneticParent("?"));
            }
            myVector.add(v, filler);
        }

        myVector.elementAt(0).elementAt(0).setHorseName(horseName);
        // TODO Put this back
        // myVector.elementAt(0).elementAt(0).setForm(this.getPedigreeInfo(horseName));
        myVector.elementAt(0).elementAt(0).setDam(myVector.elementAt(1).elementAt(0));
        myVector.elementAt(0).elementAt(0).setSire(myVector.elementAt(1).elementAt(1));

        for (int column = 0; column <= (tblWidth / 2); column++) {

            Elements tbCol = tableElement.getCol(column * 2);
            String tmpStr = null, tmpStr2 = null;

            for (int pair = 0; pair < pairs[column]; pair++) {

                int sireOffset = (tblDepth / pairs[column]) * pair;
                int damOffset = sireOffset + damDist[column];

                try {
                    tmpStr = tbCol.getElement(sireOffset).innerText();
                } catch (NodeNotFound nodeNotFound) {
                    nodeNotFound.printStackTrace();
                }
                try {
                    tmpStr2 = tbCol.getElement(damOffset).innerText();
                } catch (NodeNotFound nodeNotFound) {
                    nodeNotFound.printStackTrace();
                }
                if (column < (tblWidth / 2)) {
                    myVector.elementAt(column).elementAt(pair).setSire(myVector.elementAt(column + 1).elementAt(pair * 2));
                    myVector.elementAt(column).elementAt(pair).setDam(myVector.elementAt(column + 1).elementAt((pair * 2) + 1));
                    myVector.elementAt(column + 1).elementAt(pair * 2).setHorseName(tmpStr);
                    myVector.elementAt(column + 1).elementAt((pair * 2) + 1).setHorseName(tmpStr2);
                    // TODO
                    // myVector.elementAt(column + 1).elementAt(pair * 2).setForm(this.getPedigreeInfo(tmpStr));
                    // myVector.elementAt(column + 1).elementAt((pair * 2) + 1).setForm(this.getPedigreeInfo(tmpStr2));
                }
            }
        }

        m_geneticParent = myVector.elementAt(0).elementAt(0);
        return true;
    }

    public GeneticParent getHorseGeneology() {
        return m_geneticParent;
    }

}
