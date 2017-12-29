package com.zhijie.focus;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseListener;
import com.choosemuse.libmuse.MuseManagerAndroid;

import java.lang.ref.WeakReference;
import java.util.List;

public class Activity_Connect_Muse extends Activity {

    MuseManagerAndroid manager;

    private ArrayAdapter<String> listviewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity__connect__muse);

        manager = MuseManagerAndroid.getInstance();
        manager.setContext(this);
        WeakReference<Activity_Connect_Muse> weakActivity =
                new WeakReference<>(this);
        manager.setMuseListener(new Activity_Connect_Muse.MuseL(weakActivity));

        manager.stopListening();
        manager.startListening();

        initUI();


    }

    private void initUI() {
        Button refresh = findViewById(R.id.refresh);
        refresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                manager.stopListening();
                manager.startListening();
            }
        });

        Button select_muse = findViewById(R.id.select_muse);
        select_muse.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //todo: Return back
                finish();
            }
        });

        ListView lv = findViewById(R.id.museList);
        listviewAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

        lv.setAdapter(listviewAdapter);


    }

    public void museListChanged() {
        final List<Muse> list = manager.getMuses();
        listviewAdapter.clear();
        for (Muse m : list) {
            listviewAdapter.add(m.getName() + " - " + m.getMacAddress());
        }
    }


    class MuseL extends MuseListener {
        final WeakReference<Activity_Connect_Muse> activityRef;

        MuseL(final WeakReference<Activity_Connect_Muse> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void museListChanged() {
            activityRef.get().museListChanged();
        }

    }

}
