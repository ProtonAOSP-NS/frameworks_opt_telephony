/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.telephony;

import static com.android.internal.telephony.RILConstants.RIL_UNSOL_DATA_CALL_LIST_CHANGED;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_KEEPALIVE_STATUS;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_PCO_DATA;
import static com.android.internal.telephony.RILConstants.RIL_UNSOL_UNTHROTTLE_APN;

import android.hardware.radio.data.IRadioDataIndication;
import android.os.AsyncResult;
import android.os.RemoteException;
import android.telephony.PcoData;
import android.telephony.data.DataCallResponse;

import com.android.internal.telephony.dataconnection.KeepaliveStatus;

import java.util.ArrayList;

/**
 * Interface declaring unsolicited radio indications for data APIs.
 */
public class DataIndication extends IRadioDataIndication.Stub {
    private final RIL mRil;

    public DataIndication(RIL ril) {
        mRil = ril;
    }

    /**
     * Indicates data call contexts have changed.
     * @param indicationType Type of radio indication
     * @param dcList List of SetupDataCallResult identical to that returned by getDataCallList.
     *        It is the complete list of current data contexts including new contexts that have
     *        been activated.
     */
    public void dataCallListChanged(int indicationType,
            android.hardware.radio.data.SetupDataCallResult[] dcList) {
        mRil.processIndication(RIL.DATA_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_DATA_CALL_LIST_CHANGED, dcList);
        ArrayList<DataCallResponse> response = RILUtils.convertHalDataCallResultList(dcList);
        mRil.mDataCallListChangedRegistrants.notifyRegistrants(
                new AsyncResult(null, response, null));
    }

    /**
     * Indicates a status update for an ongoing Keepalive session.
     * @param indicationType Type of radio indication
     * @param halStatus Status of the ongoing Keepalive session
     */
    public void keepaliveStatus(int indicationType,
            android.hardware.radio.data.KeepaliveStatus halStatus) {
        mRil.processIndication(RIL.DATA_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) {
            mRil.unsljLogRet(RIL_UNSOL_KEEPALIVE_STATUS, "handle=" + halStatus.sessionHandle
                    + " code=" +  halStatus.code);
        }

        KeepaliveStatus ks = new KeepaliveStatus(halStatus.sessionHandle, halStatus.code);
        mRil.mNattKeepaliveStatusRegistrants.notifyRegistrants(new AsyncResult(null, ks, null));
    }

    /**
     * Indicates when there is new Carrier PCO data received for a data call.
     * @param indicationType Type of radio indication
     * @param pco New PcoData
     */
    public void pcoData(int indicationType, android.hardware.radio.data.PcoDataInfo pco) {
        mRil.processIndication(RIL.DATA_SERVICE, indicationType);

        PcoData response = new PcoData(pco.cid, pco.bearerProto, pco.pcoId, pco.contents);

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_PCO_DATA, response);

        mRil.mPcoDataRegistrants.notifyRegistrants(new AsyncResult(null, response, null));
    }

    /**
     * Stop throttling calls to setupDataCall for the given APN.
     * @param indicationType Type of radio indication
     * @param apn APN to unthrottle
     * @throws RemoteException
     */
    public void unthrottleApn(int indicationType, String apn) throws RemoteException {
        mRil.processIndication(RIL.DATA_SERVICE, indicationType);

        if (RIL.RILJ_LOGD) mRil.unsljLogRet(RIL_UNSOL_UNTHROTTLE_APN, apn);

        mRil.mApnUnthrottledRegistrants.notifyRegistrants(new AsyncResult(null, apn, null));
    }
}
