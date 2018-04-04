package com.example.bartek.followyou.History;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.bartek.followyou.Database.AppDatabase;
import com.example.bartek.followyou.Database.LocDao;
import com.example.bartek.followyou.Database.Way;
import com.example.bartek.followyou.Database.WayDao;
import com.example.bartek.followyou.DetailActivities.DetailsActivity;
import com.example.bartek.followyou.MainActivity;
import com.example.bartek.followyou.R;

import java.util.Collections;
import java.util.List;

import static com.example.bartek.followyou.MainActivity.NAME_DATABASE;

public class HistoryActivity extends AppCompatActivity {
    private ListView listView;
    private AppDatabase database;
    private WayDao wayDao;
    private LocDao locDao;
    private HistoryAdapter historyAdapter;
    private Context context;
    private List<Way> wayList;
    private List<Long> wayTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        context = getApplicationContext();
        database = Room.databaseBuilder(context, AppDatabase.class, NAME_DATABASE)
                .fallbackToDestructiveMigration().build();
        wayDao = database.wayDao();
        locDao = database.locDao();

        listView = (ListView) findViewById(R.id.listView);

        new AsyncTask<Void, Void, List<Way>>(){
            @Override
            protected List<Way> doInBackground(Void... voids) {
                return wayList = wayDao.getAll();
            }

            @Override
            protected void onPostExecute(List<Way> ways) {
                super.onPostExecute(ways);
                Collections.reverse(wayList);
                setList(wayList);
            }
        }.execute();
    }

    private void setList(final List<Way> ways){
        historyAdapter = new HistoryAdapter(context, R.layout.history_adapter, ways);
        listView.setAdapter(historyAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent detailsActivity = new Intent(context, DetailsActivity.class);
                detailsActivity.putExtra(MainActivity.DetailsIntentTag, ways.get(i).getId());
                startActivity(detailsActivity);
            }
        });
    }
}
