package com.penn.ajb3;

import android.databinding.DataBindingUtil;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.penn.ajb3.databinding.ActivityMainBinding;
import com.penn.ajb3.realm.RMRelatedUser;
import com.penn.ajb3.util.PPRetrofit;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.Socket;
import java.net.URISyntaxException;

import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;

import static android.R.attr.type;
import static com.github.nkzawa.socketio.client.Socket.EVENT_CONNECT;
import static com.github.nkzawa.socketio.client.Socket.EVENT_DISCONNECT;
import static com.penn.ajb3.PPApplication.AUTH_BODY;
import static com.penn.ajb3.PPApplication.ppFromString;

public class MainActivity extends AppCompatActivity {

    private Realm realm;

    private ActivityMainBinding binding;

    private com.github.nkzawa.socketio.client.Socket socket;

    private RealmResults<RMRelatedUser> relatedUsers;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        setSupportActionBar(binding.toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = binding.container;
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(3);

        binding.tabs.setupWithViewPager(mViewPager);

        try {
            socket = IO.socket(PPRetrofit.SOCKET_URL);
        } catch (URISyntaxException e) {
            Log.v("ppLog", "socket exception:" + e.toString());
        }

        socket.on("pushEvent", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                String typeString = args[0].toString();
                PPApplication.getPush(ppFromString(typeString, "type").getAsString());
            }
        });

        socket.on("needYourUserId", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                socket.emit("giveMyUserId", PPApplication.getPrefStringValue("MY_ID", "NONE"));
            }
        });

        socket.on(EVENT_CONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Log.v("ppLog", "EVENT_CONNECT");
                PPApplication.reconnectToServer();
            }

        });

        //todo 为什么没有触发
        socket.on(EVENT_DISCONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Log.v("ppLog", "EVENT_DISCONNECT");
            }

        });

        socket.connect();

        realm = Realm.getDefaultInstance();
        relatedUsers = realm.where(RMRelatedUser.class).findAll();
        relatedUsers.addChangeListener(new OrderedRealmCollectionChangeListener<RealmResults<RMRelatedUser>>() {
            @Override
            public void onChange(RealmResults<RMRelatedUser> rmRelatedUsers, OrderedCollectionChangeSet changeSet) {
                updateRelatedUserCount();
            }
        });

        updateRelatedUserCount();
        Log.v("ppLog", "onCreate on MainActivity end");
    }

    @Override
    protected void onDestroy() {
        realm.close();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            PPApplication.showDB();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            textView.setText(getString(R.string.section_format, getArguments().getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            if (position == 0) {
                return FollowsFragment.newInstance(null, null);
            } else if (position == 1) {
                return FansFragment.newInstance(null, null);
            } else if (position == 2) {
                return FriendsFragment.newInstance(null, null);
            } else {
                return PlaceholderFragment.newInstance(position + 1);
            }
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "FOLLOWS";
                case 1:
                    return "FANS";
                case 2:
                    return "FRIENDS";
            }
            return null;
        }
    }

    private void updateRelatedUserCount() {
        Log.v("ppLog", "updateRelatedUserCount");
        binding.relatedUserCount.setText(
                realm.where(RMRelatedUser.class).equalTo("isFollows", true).count()
                        + ","
                        + realm.where(RMRelatedUser.class).equalTo("isFans", true).count()
                        + ","
                        + realm.where(RMRelatedUser.class).equalTo("isFriends", true).count()
        );
    }
}
