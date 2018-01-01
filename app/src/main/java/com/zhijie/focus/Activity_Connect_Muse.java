package com.zhijie.focus;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseListener;
import com.choosemuse.libmuse.MuseManagerAndroid;
import com.google.gson.Gson;

import java.lang.ref.WeakReference;
import java.util.List;

public class Activity_Connect_Muse extends Activity {

    String TAG = "Activity_Connect_Muse";

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
                listviewAdapter.clear();
                manager.stopListening();
                manager.startListening();
            }
        });

//        Button select_muse = findViewById(R.id.select_muse);
//        select_muse.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                //Return back
//                finish();
//            }
//        });

        ListView lv = findViewById(R.id.museList);
        listviewAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        lv.setAdapter(listviewAdapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {


                // Todo: Return back to the original Activity.
                List<Muse> availableMuse = manager.getMuses();
                if (availableMuse.size() < 1) {
                    Log.d(TAG, "No available muse to connect to!");
                } else {

                    Toast.makeText(getApplicationContext(),
                            "Click ListItem Number " + position + "Muse: " + availableMuse.get(position), Toast.LENGTH_LONG)
                            .show();

                    Gson gson = new Gson();

                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("muse", availableMuse.get(position));
                }
            }
        });

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
