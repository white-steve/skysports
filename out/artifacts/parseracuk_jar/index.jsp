<%@ page import="java.text.*, java.sql.*, javax.sql.*, java.io.*, javax.naming.*" %>
<html>
    <head><title>Steve and Malcs Information Service</title></head>
    <body bgcolor="white" >
        <%
            InitialContext ctx;
            DataSource ds;
            Connection conn = null;
            Statement stmt;
            String entryStmnt = "SELECT * from steve.entrant where date=? and time=? and course=? order by abs(lastrace)";
            String horseStmnt = "Select * from steve.horse where name=?";
            String statsStmnt = "Select * from steve.statistics where name=?";
            String historyStmntAll = "Select * from steve.result where name=? order by date desc";
            String historyStmnt = "Select * from steve.result where name=? and horsetime > \"\" order by mph desc";
            String filtHistoryStmnt = "Select * from steve.result where name=? and class<=? order by date desc";
            String topStmnt = "Select * from steve.meeting where date=current_date() and time=? order by date desc";
			
			String bigQuery = "select meeting.title, result.name, result.mph , result.weight from meeting,entrant,result where entrant.date = current_date() and entrant.time = ? and meeting.date = current_date() 
and meeting.time = entrant.time and entrant.name = result.name and abs(substring(result.distance, 1, 2)) >= abs(substring(meeting.distance, 1, 2)) and result.mph > '' and abs(result.mph) <= 43 
and (result.going = 'Hvy' or result.going = 'Sft/Hvy') order by result.mph desc" ;


            PreparedStatement queryEntries = null;
            PreparedStatement queryHorses = null;
            PreparedStatement queryStats = null;
            PreparedStatement queryHistory = null;
            PreparedStatement queryHistoryAll = null;
            PreparedStatement filteredHistory = null;
            PreparedStatement queryTop = null;
			PreparedStatement queryBig = null;
            ResultSet rs = null;

            String p1 = request.getParameter("date");
            String p2 = request.getParameter("time");

            try {
                ctx = new InitialContext();
                ds = (DataSource) ctx.lookup("jdbc/MySQLDataSource");
                conn = ds.getConnection();
                stmt = conn.createStatement();
                queryEntries = conn.prepareStatement(entryStmnt);
                queryHorses = conn.prepareStatement(horseStmnt);
                queryStats = conn.prepareStatement(statsStmnt);
                queryHistory = conn.prepareStatement(historyStmnt);
                queryHistoryAll = conn.prepareStatement(historyStmntAll);
                filteredHistory = conn.prepareStatement(filtHistoryStmnt);
                queryTop = conn.prepareStatement(topStmnt);
				queryBig = conn.prepareStatement(bigQuery);

                try {
                    if (p1 != null && p2 != null) {
                        queryTop.setString(1, p1);
                        queryTop.setString(2, p2);
                    } else {
                        topStmnt = "Select * from steve.meeting where date=current_date() order by date desc";
                        queryTop = conn.prepareStatement(topStmnt);
                    }
                    rs = queryTop.executeQuery();
                } catch (SQLException se) {
        %>
        <%= se.getMessage()%>
        <%
            }

            while (rs.next()) {
                java.sql.Date myDate = rs.getDate("date");
                java.sql.Time myTime = rs.getTime("time");
                String course = rs.getString("course");
                DateFormat mdf = new SimpleDateFormat("dd-MM-yyyy");
                String newMeetingDate = mdf.format(myDate);
                String title = rs.getString("title");

                queryEntries.setDate(1, myDate);
                queryEntries.setTime(2, myTime);
                queryEntries.setString(3, course);

                int Runners = 0;
                try {
                    Runners = rs.getInt("runners");
                } catch (Exception ee) {
                }


        %>
        <%                    if (Runners >= 8 && title.contains("Handicap")) {
        %>
        <br><table bgcolor=#F0FFF0 border="1"  cellspacing="7" cellpadding="7" align="left">
            <%
            } else {
            %>
            <br><table border="1"  cellspacing="7" cellpadding="7" align="left">
                <%
                    }
                %>

                <caption><font face="Verdana" color="royalblue" size="20" >Meeting</font></caption>
                <tr>
					<td>
					<div class = "image">
						<img src = "skl.jpg" width="200" height="85" > 
					</div>
					</td>
                    <td><font face="Verdana" color="blue" size="20" ><%= rs.getString("course")%></font></td>
                    <td><font face="Verdana" color="blue" size="20" ><%= rs.getString("going")%></font></td>
                    <td><font face="Verdana" color="blue" size="20" ><%= newMeetingDate%></font></td>
                    <td><%= title%></td>
                    <td><font face="Verdana" color="blue" size="20" ><%= rs.getString("distance")%></font></td>
                    <td><font face="Verdana" color="blue" size="20" ><%= rs.getString("class")%></font></td>
                    <td><font face="Verdana" color="blue" size="20" ><%= rs.getString("runners")%></font></td>
                </tr>
            </table>
            <%
			
				//
				try {
					java.sql.Time myTime = rs.getTime("time");
                    queryBig.setTime(1, myTime);

                } catch (SQLException e) {
                    e.printStackTrace();
                }

                ResultSet bigRs = null;
                try {
                    bigRs = queryHorses.executeQuery();
                } catch (SQLException se) {
					%>
					<%= se.getMessage()%>
					<%
                }
				while (bigRs.next()) {
                    String title = bigRs.getString("title");
                    String name = bigRs.getString("name");
					String mph = bigRs.getString("mph");
					String weight = bigRs.getString("weight");
					if (title == null) {
						title = "";
					}
                    if (name == null) {
                        name = "";
                    }
					if (mph == null) {
                        mph = "";
                    }
					if (weight == null) {
                        weight = "";
                    }
            %>
            <br><table border="2"  cellspacing="7" cellpadding="7" align="left">
                <tr>
                    <td><font face="Verdana" color="blue" size="10" bgcolor="white"><%= title%></font>
                    </td>
                    <td><font face="Verdana" color="orange" size="10" bgcolor="white"><%= name%></td></font>
                    <td>
                        <font face="Verdana" size="10" color="red">
                        <%= mph%>
                        </font>
                    </td>
                    <td><%= weight%></td>
                </tr>
            </table>
            <%
			}
				//

                ResultSet entRs = queryEntries.executeQuery();
                while (entRs.next()) {
                    String horseName = entRs.getString("name");
                    String predict = entRs.getString("predict");
					String actual = entRs.getString("actual");
					String malpred = entRs.getString("pr1");
					if (actual == null) {
						actual = "";
					}
                    if (predict == null) {
                        predict = "";
                    }
					if (malpred == null) {
                        malpred = "";
                    }
                    String form = entRs.getString("form");
                    String Jockey = entRs.getString("jockey");
                    String trainer = entRs.getString("trainer");
                    String age = entRs.getString("age");
                    String weight = entRs.getString("weight");
                    String lastrace = entRs.getString("lastrace");
            %>
            <br><table border="2"  cellspacing="7" cellpadding="7" align="left">
                <tr>
                    <td><font face="Verdana" color="blue" size="10" bgcolor="white"><%= entRs.getString("name")%></font>
                    </td>
                    <td><font face="Verdana" color="orange" size="10" bgcolor="white"><%= lastrace%></td></font>
                    <td>
                        <font face="Verdana" size="10" color="red">
                        <%= predict%>
                        </font>
                    </td>
                    <td><%= form%></td>
                    <td><%= Jockey%></td>
                    <td><%= trainer%></td>
                    <td><%= age%></td>
                    <td><%= weight%></td>
					<td>
                        <font face="Verdana" size="10" color="blue">
                        <%= actual%>
                        </font>
                    </td>
					<td>
                        <font face="Verdana" size="10" color="orange">
                        <%= malpred%>
                        </font>
                    </td>
                </tr>
            </table>
            <%
                try {
                    queryHorses.setString(1, horseName);

                } catch (SQLException e) {
                    e.printStackTrace();
                }

                ResultSet horseRs = null;
                try {
                    horseRs = queryHorses.executeQuery();
                } catch (SQLException se) {
            %>
            <%= se.getMessage()%>
            <%
                }
         // Print horse information
            %>
            <br><table border="3" cellspacing="7" cellpadding="7" align="left">
                <%
                    try {
                        while (horseRs.next()) {
                            String sex = horseRs.getString("sex");
                            String dam = horseRs.getString("dam");
                            String sire = horseRs.getString("sire");
                            String yof = horseRs.getString("yof");
                            String breeding = horseRs.getString("breeding");
                            String sage = horseRs.getString("age");
                            if (sex != null && sex.length() > 0) {
                %>
                <tr>
                    <td><%= sex%></td>
                    <td><%= dam%></td>
                    <td><%= sire%></td>
                    <td><%= yof%></td>
                    <td><%= breeding%></td>
                    <td><%= sage%></td>
                </tr>
                <%
                } else {
                %>
                <tr><td><%= breeding%> </td></tr>
                <%
                        }
                    }
                } catch (SQLException se) {
                %>
                <%= se.getMessage()%>
                <%
                    }
                %>
            </table>

            <br><table border="4"  cellspacing="7" cellpadding="7" align="left">
                <%               // Print stats information
                    try {
                        queryStats.setString(1, horseName);

                    } catch (SQLException se) {
                %>
                <%= se.getMessage()%>
                <%
                    }

                    ResultSet statsRs = null;
                    try {
                        statsRs = queryStats.executeQuery();
                    } catch (SQLException se) {
                %>
                <%= se.getMessage()%>
                <%
                    }
                    if (statsRs.next() == false) {
                %>
                <tr>
                    <td>First time Out</td>
                </tr><br>
                <%
                    }

                    try {
                        while (statsRs.next()) {
                            String raceType;
                            String pFirst;
                            int starts, fst, sec, third;

                            raceType = statsRs.getString("racetype");
                            pFirst = statsRs.getString("percentfirst");

                            starts = statsRs.getInt("starts");
                            fst = statsRs.getInt("wins");
                            sec = statsRs.getInt("seconds");
                            third = statsRs.getInt("thirds");
                %>
                <tr>
                    <td><%= raceType%></td>
                    <td><%= pFirst%></td>
                    <td><%= starts%></td>
                    <td><%= fst%></td>
                    <td><%= sec%></td>
                    <td><%= third%></td>
                </tr>
                <%
                    }
                } catch (SQLException se) {
                %>
                <%= se.getMessage()%>
                <%
                    }
                %>
            </table>


            <%
            // History Table

            %>
            <br><table border="5" cellspacing="7" cellpadding="7" align="left">
                <%                try {
                        queryHistoryAll.setString(1, horseName);
                    } catch (SQLException se) {
                %>
                <%= se.getMessage()%>
                <%
                    }

                    ResultSet historyRs = null;
                    try {
                        historyRs = queryHistoryAll.executeQuery();
                    } catch (SQLException se) {
                %>
                <%= se.getMessage()%>
                <%
                    }
                    if (historyRs.next() == false) {
                %>
                <tr>
                    <td>no History</td>
                </tr><br>
                <%
                    }

                    try {
                        historyRs.beforeFirst();
                %>
                <caption>By All Details</caption>
                <%
                    int line = 0;
                    while (historyRs.next()) {
                        line++;
                        String courseName = historyRs.getString("course");
                        java.sql.Date courseDate = historyRs.getDate("date");

                        DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
                        String newDate = df.format(courseDate);

                        String going = historyRs.getString("going");
                        String distance = historyRs.getString("distance");

                        String classStr = historyRs.getString("class");
                        String position = historyRs.getString("position");
                        String raceTime = historyRs.getString("racetime");
                        String horseTime = historyRs.getString("horsetime");
                        if (raceTime == null) {
                            raceTime = "";
                        }
                        if (horseTime == null) {
                            horseTime = "";
                        }
                        String winner = historyRs.getString("winner");
                        String lweight = historyRs.getString("weight");
                        String sp = historyRs.getString("sp");
                        String db = historyRs.getString("db");
                        String rt = historyRs.getString("runtype");
                        String mph = historyRs.getString("mph");
                        if (mph == null) {
                            mph = "";
                        }
                        String rjockey = historyRs.getString("jockey");
                        String rtrainer = historyRs.getString("trainer");
                %>
                <%
                    if ((line % 2) == 0) {
                %>
                <tr bgcolor=#FFFFE6>
                    <%
                    } else {
                    %>
                <tr bgcolor=#D6F5F5>
                    <%
                        }
                    %>
                    <td><%= newDate%></td>
                    <td><%= courseName%></td>
                    <td><%= going%></td>
                    <td><%= distance%></td>
                    <td><%= classStr%></td>
                    <td><%= rt%></td>
                    <%
                        if (position.length() > 2 && position.substring(0, 2).equals("1/")) {
                    %>
                    <td><font face="Verdana" color="red" ><%= position%></font></td>
                        <%
                        } else {
                        %>
                    <td><%= position%></td>
                    <%
                        }
                    %>

                    <td><%= lweight%></td>
                    <td><font face="Verdana" color="blue" ><%= raceTime%></font></td>
                    <td><%= db%></td>
                    <td><font face="Verdana" color="red""><%= horseTime%></font></td>
                    <td><font face="Verdana" color="red""><%= mph%></font></td>
                    <td><%= rjockey%></td>
                    <td><%= rtrainer%></td>
                    <td><%= winner%></td>
                    <td><%= sp%></td>
                </tr>
                <%

                    }
                } catch (SQLException se) {
                %>
                <%= se.getMessage()%>
                <%
                    }
                %>
            </table>
            <%
                //
                // Insert new Java Table Code here
                //   |
                //   V

            %>
            <br><table border="5"  cellpadding="7" align="left">
                <%                                int distI = 0;
                    try {
                        String distanceStr = rs.getString("distance");

                        if (distanceStr.length() > 0) {
                            String tmp = distanceStr.substring(0, distanceStr.length() - 1);
                            distI = Integer.parseInt(tmp);

                        }
                        queryHistory.setString(1, horseName);
                    } catch (SQLException se) {
                %>
                <%= se.getMessage()%>
                <%
                    }

                    ResultSet dSet = null;
                    try {
                        dSet = queryHistory.executeQuery();
                    } catch (SQLException se) {
                %>
                <%= se.getMessage()%>
                <%
                    }
                    if (dSet.next() == false) {
                %>
                <tr>
                    <td>no History</td>
                </tr><br>
                <%
                    }
                    dSet.beforeFirst();
                    try {
                %>
                <caption>By Distance</caption>
                <%
                    int line = 0;
                    while (dSet.next()) {

                        String courseName = dSet.getString("course");
                        java.sql.Date courseDate = dSet.getDate("date");

                        DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
                        String newDate = df.format(courseDate);

                        String going = dSet.getString("going");
                        String distance = dSet.getString("distance");
                        int distToCheck = 0;
                        if (distance.length() > 0) {
                            String tmp = distance.substring(0, distance.length() - 1);
                            distToCheck = Integer.parseInt(tmp);
                        }
                        String classStr = dSet.getString("class");
                        String position = dSet.getString("position");
                        String raceTime = dSet.getString("racetime");
                        String horseTime = dSet.getString("horsetime");
                        if (raceTime == null) {
                            raceTime = "";
                        }
                        if (horseTime == null) {
                            horseTime = "";
                        }
                        String winner = dSet.getString("winner");
                        String lweight = dSet.getString("weight");
                        String sp = dSet.getString("sp");
                        String db = dSet.getString("db");
                        String rt = dSet.getString("runtype");
                        String mph = dSet.getString("mph");
                        if (mph == null) {
                            mph = "";
                        }
                        String rjockey = dSet.getString("jockey");
                        String rtrainer = dSet.getString("trainer");
                        boolean process = false;

                        if (distToCheck == distI) {

                            process = true;
                            line++;

                            if ((line % 2) == 0) {
                %>
                <tr bgcolor=#FFFFE6>
                    <%
                    } else {
                    %>
                <tr bgcolor=#D6F5F5>
                    <%
                        }
                    %>								
                    <td><%= newDate%></td>
                    <td><%= courseName%></td>
                    <td><%= going%></td>
                    <td><%= distance%></td>
                    <td><%= classStr%></td>
                    <td><%= rt%></td>
                    <%
                        if (position.length() > 2 && position.substring(0, 2).equals("1/")) {
                    %>
                    <td><font face="Verdana" color="red" ><%= position%></font></td>
                        <%
                        } else {
                        %>
                    <td><%= position%></td>
                    <%
                        }
                    %>

                    <td><%= lweight%></td>
                    <td><font face="Verdana" color="blue" ><%= raceTime%></font></td>
                    <td><%= db%></td>
                    <td><font face="Verdana" color="red""><%= horseTime%></font></td>
                    <td><font face="Verdana" color="red""><%= mph%></font></td>
                    <td><%= rjockey%></td>
                    <td><%= rtrainer%></td>
                    <td><%= winner%></td>
                    <td><%= sp%></td>
                </tr>
                <%
                        }
                    }
                } catch (SQLException se) {
                %>
                <%= se.getMessage()%>
                <%
                        }
                    }
//
                    // End of Java insert zone
                    //

                %>
            </table>












            }
            %>
        </table>
        <%        } // rs loop
        %>
    </table>
    <%
    } // outer most catch
    catch (SQLException se) {
    %>
    <%= se.getMessage()%>
    <%
    } catch (NamingException ne) {
    %>
    <%= ne.getMessage()%>
    <%

        }
        queryEntries.close();
        queryHorses.close();
        queryStats.close();
        queryHistory.close();
        queryHistoryAll.close();
        queryTop.close();
		queryBig.close();
        filteredHistory.close();
        conn.close();
    %>
</body>
</html>