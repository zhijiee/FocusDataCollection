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
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
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

        // TODO: Check Bluetooth permission
        // TODO: Turn on Bluetooth
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

        ListView lv = findViewById(R.id.museList);
        listviewAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        lv.setAdapter(listviewAdapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                manager.stopListening(); // Stop listening
                List<Muse> availableMuse = manager.getMuses();
                Intent returnIntent = new Intent();

                if (availableMuse.size() < 1) {
                    Log.d(TAG, "No available muse to connect to!");
                    setResult(RESULT_CANCELED);
                } else {
                    returnIntent.putExtra("pos", position);
                    Log.d(TAG, "pos:" + position);
                    setResult(RESULT_OK, returnIntent);

                }
                finish(); //return to previous activity with selected muse device
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
