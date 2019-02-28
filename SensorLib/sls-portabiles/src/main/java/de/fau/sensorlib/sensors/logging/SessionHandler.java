/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */

package de.fau.sensorlib.sensors.logging;

import java.util.ArrayList;
import java.util.List;

public class SessionHandler {

    private boolean mFirstPacketRead;

    private int mSessionCount;

    private List<Session> mSessionList;


    public SessionHandler() {
        mFirstPacketRead = false;
        mSessionList = new ArrayList<>();
    }

    public void setSessionCount(int sessionCount) {
        mSessionCount = sessionCount;
        mFirstPacketRead = true;
    }

    public boolean firstPacketRead() {
        return mFirstPacketRead;
    }

    public void addSession(Session session) {
        session.setSessionNumber(mSessionList.size());
        mSessionList.add(session);
    }


    public List<Session> getSessionList() {
        return mSessionList;
    }

    public int getSessionCount() {
        return mSessionCount;
    }

    public boolean allSessionsRead() {
        return mSessionList.size() == getSessionCount();
    }

}
