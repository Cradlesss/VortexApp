package com.project_void.vortexapp.core.permissions;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.project_void.vortexapp.R;
import com.project_void.vortexapp.ui.dialogs.VortexDialogInfo;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Map;

public final class PermissionsGate {
    public interface DenialHandler {
        void onDenied(@NonNull Activity activity,
                      @NonNull String[] requested,
                      @NonNull Map<String, Boolean> result,
                      @NonNull Runnable retry,
                      @Nullable String message
        );
    }

    private final Activity activity;
    private final DenialHandler denialHandler;
    private final Deque<Request> queue = new ArrayDeque<>();
    private boolean inFlight = false;
    private final ActivityResultLauncher<String[]> launcher;
    private static final DenialHandler SILENT_DENY = (act, req, res, retry, msg) -> {};

    public PermissionsGate(@NonNull Activity activity, @NonNull ActivityResultCaller caller) {
        this(activity, caller, PermissionsGate::defaultDenialHandler);
    }

    public PermissionsGate(@NonNull Activity activity, @NonNull ActivityResultCaller caller, @NonNull DenialHandler handler) {
        this.activity = activity;
        this.denialHandler = handler;
        this.launcher = caller.registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                this::onResult
        );
    }

    public void runForScan(@NonNull Runnable onGranted) {
        runWhenGranted(activity, AppPermissions.requiredForScan(), onGranted, activity.getString(R.string.perm_location));
    }

    public void runForConnect(@NonNull Runnable onGranted) {
        runWhenGranted(activity, AppPermissions.requiredForConnect(), onGranted, activity.getString(R.string.perm_nearby_devices));
    }

    public void runForNotifications(@NonNull Runnable onGranted) {
        runWhenGranted(activity, AppPermissions.requiredForNotifications(), onGranted, activity.getString(R.string.perm_notification));
    }

    public void runForScanAndConnect(@NonNull Runnable onGranted) {
        String[] scan = AppPermissions.requiredForScan();
        String[] conn = AppPermissions.requiredForConnect();
        runWhenGranted(activity, concat(scan, conn), onGranted, activity.getString(R.string.perm_location_and_nearby));
    }

    public void runWhenGranted(@NonNull Context c, @NonNull String[] perms, @NonNull Runnable onGranted, @NonNull String message) {
        String[] missing = AppPermissions.missing(c, perms);

        if (missing.length == 0) {
            onGranted.run();
            return;
        }

        enqueue(new Request(missing, onGranted, message));
        maybeLaunchNext();
    }

    private void runWhenGrantedSilent(@NonNull Context c, @NonNull String[] perms, @NonNull Runnable onGranted) {
        String[] missing = AppPermissions.missing(c, perms);

        if (missing.length == 0) {
            onGranted.run();
            return;
        }

        enqueue(new Request(missing, onGranted, SILENT_DENY, ""));
        maybeLaunchNext();
    }

    public void requestAllSilent(boolean untilGranted) {
        final SharedPreferences sp = activity.getSharedPreferences("perm_state", Context.MODE_PRIVATE);
        final String KEY = "initial_perm_prompted";

        if (sp.getBoolean(KEY, false)) return;

        if (!untilGranted) {
            sp.edit().putBoolean(KEY, true).apply();
            String[] scan = AppPermissions.requiredForScan();
            String[] conn = AppPermissions.requiredForConnect();
            String[] notif = AppPermissions.requiredForNotifications();
            String[] all = concat(scan, conn, notif);
            runWhenGrantedSilent(activity, all, () -> {
            });
        } else {
            String[] scan = AppPermissions.requiredForScan();
            String[] conn = AppPermissions.requiredForConnect();
            String[] notif = AppPermissions.requiredForNotifications();
            String[] all = concat(scan, conn, notif);
            runWhenGrantedSilent(activity, all, () -> sp.edit().putBoolean(KEY, true).apply());
        }
    }

    private void maybeLaunchNext() {
        if (inFlight || queue.isEmpty()) return;
        inFlight = true;
        final Request next = queue.peek();
        if (next == null) return;
        launcher.launch(next.perms);
    }

    private void onResult(Map<String, Boolean> res) {
        final Request r = queue.poll();
        inFlight = false;

        markAsked(r.perms);

        boolean allGranted = !res.containsValue(Boolean.FALSE);

        if (allGranted) r.onGranted.run();
        else {
            DenialHandler h = (r.handler != null) ? r.handler : denialHandler;
            h.onDenied(activity, r.perms, res, () -> {
                enqueue(new Request(r.perms, r.onGranted, r.handler, r.message));
                maybeLaunchNext();
            }, r.message);
        }
        maybeLaunchNext();
    }

    private void enqueue(Request r) {
        queue.add(r);
    }

    private SharedPreferences permState() {
        return activity.getSharedPreferences("perm_state", Context.MODE_PRIVATE);
    }

    private void markAsked(String[] perms) {
        SharedPreferences.Editor e = permState().edit();
        for (String p : perms)
            e.putBoolean("asked_" + p, true);
        e.apply();
    }

    private static boolean hasAskedBefore(@NonNull Activity act, @NonNull String p) {
        SharedPreferences sp = act.getSharedPreferences("perm_state", Context.MODE_PRIVATE);
        return sp.getBoolean("asked_" + p, false);
    }

    private static final class Request {
        final String[] perms;
        final Runnable onGranted;
        final DenialHandler handler;
        final String message;

        Request(String[] p, Runnable g, String m) {
            this(p, g, null, m);
        }

        Request(String[] p, Runnable g, DenialHandler h, String m) {
            perms = p;
            onGranted = g;
            handler = h;
            message = m;
        }
    }

    private static void defaultDenialHandler(Activity act, String[] requested, Map<String, Boolean> result, Runnable retry, @Nullable String message) {
        boolean permanentlyDenied = false;
        for (String p : requested) {
            boolean granted = Boolean.TRUE.equals(result.get(p));
            if (!granted) {
                boolean asked = hasAskedBefore(act, p);
                boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(act, p);
                if (asked && !showRationale) {
                    permanentlyDenied = true;
                    break;
                }
            }
        }

        final String msg = (message != null && !message.isEmpty())
                ? act.getString(R.string.perm_need_permission, message)
                : act.getString(R.string.perm_need_this_permission);

        if (permanentlyDenied)
            VortexDialogInfo.show(
                    act,
                    act.getString(R.string.perm_required_title),
                    msg,
                    act.getString(R.string.perm_request_action),
                    () -> act.startActivity(
                            new Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .setData(Uri.fromParts("package", act.getPackageName(), null))
                    )
            );
        else
            VortexDialogInfo.show(
                    act,
                    act.getString(R.string.perm_needed_title),
                    msg,
                    act.getString(R.string.perm_allow_action),
                    retry::run
            );
    }

    private static String[] concat(String[]... lists) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String[] arr : lists)
            set.addAll(Arrays.asList(arr));
        return set.toArray(new String[0]);
    }
}
