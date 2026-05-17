package com.project_void.vortexapp.ui.selectdevice.device_info;

import android.bluetooth.le.ScanRecord;
import android.util.Log;
import android.util.SparseArray;
import androidx.annotation.Nullable;
import com.project_void.vortexapp.BuildConfig;

public class DeviceSignature {
    public static final int CAPS_TYPE_MASK = 0x000F;
    public static final int TYPE_LED_STRIP = 0x0001;

    public enum Source {
        SERVICE_DATA,
        MANUFACTURER,
        NONE
    }
    public final int proto;
    public final int modelID;
    public final int verMajor;
    public final int verMinor;
    public final int verPatch;
    public final int mfgCompanyID;
    public final int mfgIconCode;
    public final int mfgProto;
    public final Source src;

    public DeviceSignature(int proto, int modelID, int verMajor, int verMinor, int verPatch, int mfgCompanyID, int mfgProto, int mfgIcCode, Source src) {
        this.proto = proto;
        this.modelID = modelID;
        this.verMajor = verMajor;
        this.verMinor = verMinor;
        this.verPatch = verPatch;
        this.mfgCompanyID = mfgCompanyID;
        this.mfgProto = mfgProto;
        this.mfgIconCode = mfgIcCode;
        this.src = src;
    }

    @Nullable
    public static DeviceSignature from(ScanRecord rec) {
        if (rec == null) return null;
        if (BuildConfig.DEBUG) Log.d("DeviceSignature", "from: " + rec);

        int proto = -1, model = -1, iconCode = 0;
        int maj = -1, min = -1, pat = -1;
        int mfgID = -1, mfgProto = -1;
        Source src = Source.NONE;

        SparseArray<byte[]> mfg = rec.getManufacturerSpecificData();
        if (mfg != null && mfg.size() > 0) {
            for (int i = 0; i < mfg.size(); i++) {
                int companyID = mfg.keyAt(i);
                byte[] md = mfg.valueAt(i);
                if (md == null) continue;

                if (md.length >= 5) {
                    mfgID = companyID;
                    mfgProto = md[0] & 0xFF;
                    model  = md[1] & 0xFF;

                    maj = md[2] & 0xFF;
                    min = md[3] & 0xFF;
                    pat = md[4] & 0xFF;

                    if (md.length >= 7)
                        iconCode = (md[5] & 0xFF) | ((md[6] & 0xFF) << 8);

                    proto = mfgProto;

                    src = Source.MANUFACTURER;
                    break;
                }
            }
        }

        if (src == Source.NONE) return null;

        return new DeviceSignature(proto, model, maj, min, pat, mfgID, mfgProto, iconCode, src);
    }
}
