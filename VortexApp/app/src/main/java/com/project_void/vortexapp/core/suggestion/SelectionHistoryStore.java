package com.project_void.vortexapp.core.suggestion;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SelectionHistoryStore {
    private static final String PREF = "selection_history";
    private static final String KEY  = "entries";
    private static final int MAX = 30;
    private final SharedPreferences p;

    public SelectionHistoryStore(Context ctx) {
        this.p = ctx.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public void record(SelectionEntry entry){
        List<SelectionEntry> list = load();
        list.add(0, entry);
        if (list.size() > MAX) list = list.subList(0, MAX);
        save(list);
    }

    public List<SelectionEntry> load(){
        Set<String> raw = p.getStringSet(KEY, new HashSet<>());
        List<SelectionEntry> list = new ArrayList<>();

        for (String s : raw){
            SelectionEntry e = SelectionEntry.deserialize(s);
            if (e != null) list.add(e);
        }
        list.sort(Comparator.comparingLong((SelectionEntry e) -> e.timestamp).reversed());
        return list;
    }

    private void save(List<SelectionEntry> list){
        Set<String> raw = new HashSet<>();
        for (SelectionEntry e : list) raw.add(e.serialize());
        p.edit().putStringSet(KEY, raw).apply();
    }
}
