package com.example.bartek.followyou.DetailActivities;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import com.example.bartek.followyou.History.HistoryActivity;
import com.example.bartek.followyou.History.HistoryAdapter;
import com.example.bartek.followyou.MainActivity;
import com.example.bartek.followyou.R;

public class DetailsActivity extends AppCompatActivity {
    private FragmentPagerAdapter fragmentPagerAdapter;
    private static int wayId;
    private Intent historyIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wayId = getIntent().getIntExtra(MainActivity.DetailsIntentTag, 0);
        setContentView(R.layout.activity_details);
        historyIntent = new Intent(this, HistoryActivity.class);
        ViewPager viewPager = (ViewPager) findViewById(R.id.vpPager);
        fragmentPagerAdapter = new detailsPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(fragmentPagerAdapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private static class detailsPagerAdapter extends FragmentPagerAdapter{
        private static int NUM_ITEMS = 2;

        public detailsPagerAdapter(android.support.v4.app.FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position){
                case 0:
                    return DetailsInfoActivity.newInstance(wayId);
                case 1:
                    return DetailsMapActivity.newInstance(wayId);
                default: return null;
            }
        }

        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position){
                case 0:
                    return "Info";
                case 1:
                    return "Map";
                default: return "Info";
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        finishAffinity();
        startActivity(historyIntent);
    }
}
