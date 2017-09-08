package com.penn.ajb3;

import android.app.ActionBar;
import android.databinding.DataBindingUtil;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.penn.ajb3.databinding.ActivityAllUsersBinding;
import com.penn.ajb3.databinding.AllUsersUserCellBinding;
import com.penn.ajb3.messageEvent.RelatedUserChanged;
import com.penn.ajb3.realm.RMRelatedUser;
import com.penn.ajb3.util.PPRetrofit;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import retrofit2.HttpException;

import static com.penn.ajb3.PPApplication.ppFromString;

public class AllUsersActivity extends AppCompatActivity {

    private Set<String> objWaiting = new HashSet<String>();
    private ArrayList<AllUsersActivity.OtherUser> otherUsers;

    public class OtherUser {
        public String _id;
        public String username;
        public String nickname;
        public String sex;
        public String avatar;

        public int followable() {
            try (Realm realm = Realm.getDefaultInstance()) {
                RMRelatedUser rmRelatedUser = realm.where(RMRelatedUser.class).equalTo("_id", _id).findFirst();
                if (rmRelatedUser != null && (rmRelatedUser.isFollows || rmRelatedUser.isFriends)) {
                    return View.INVISIBLE;
                } else {
                    return View.VISIBLE;
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void relatedUserChanged(RelatedUserChanged event) {
        Log.v("ppLog", "relatedUserChanged");
        int index = -1;

        ArrayList<String> userIds = event.userIds;
        for (String _id : userIds) {
            for (int i = 0; i < otherUsers.size(); i++) {
                if (otherUsers.get(i)._id.equals(_id)) {
                    index = i;
                }
            }

            if (index > -1) {
                rvAdapter.notifyItemChanged(index);
            }
        }
    }

    public class RelatedUserListAdapter extends RecyclerView.Adapter<AllUsersActivity.RelatedUserListAdapter.OtherUserVH> {

        @Override
        public AllUsersActivity.RelatedUserListAdapter.OtherUserVH onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

            AllUsersUserCellBinding allUsersUserCellBinding = AllUsersUserCellBinding.inflate(layoutInflater, parent, false);

            return new AllUsersActivity.RelatedUserListAdapter.OtherUserVH(allUsersUserCellBinding);
        }

        @Override
        public void onBindViewHolder(AllUsersActivity.RelatedUserListAdapter.OtherUserVH holder, int position) {
            holder.bind(otherUsers.get(position));
        }

        @Override
        public int getItemCount() {
            return otherUsers.size();
        }

        public class OtherUserVH extends RecyclerView.ViewHolder {
            private AllUsersUserCellBinding binding;
            private String userId;

            public OtherUserVH(AllUsersUserCellBinding binding) {
                super(binding.getRoot());
                this.binding = binding;

                binding.followBt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //保存点击时的userId
                        final String curUserId = userId;

                        //如果在此用户在objWaiting中, 则点击无效
                        if (objWaiting.contains(curUserId)) {
                            Log.v("ppLog", "稍等下, 不要重复点击");
                            return;
                        }

                        objWaiting.add(curUserId);
                        int index = -1;
                        for (int i = 0; i < otherUsers.size(); i++) {
                            AllUsersActivity.OtherUser otherUser = otherUsers.get(i);
                            if (otherUser._id.equals(curUserId)) {
                                index = i;
                                break;
                            }
                        }

                        if (index > -1) {
                            rvAdapter.notifyItemChanged(index);
                        }

                        Observable<String> result = PPRetrofit.getInstance().getPPService().follow(userId);

                        Action callFinal = new Action() {
                            @Override
                            public void run() throws Exception {
                                objWaiting.remove(curUserId);

                                int index = -1;
                                for (int i = 0; i < otherUsers.size(); i++) {
                                    AllUsersActivity.OtherUser otherUser = otherUsers.get(i);
                                    if (otherUser._id.equals(curUserId)) {
                                        index = i;
                                        break;
                                    }
                                }

                                if (index > -1) {
                                    rvAdapter.notifyItemChanged(index);
                                }
                            }
                        };

                        PPApplication.apiRequest(result, PPApplication.callSuccess, PPApplication.callFailure, callFinal);
                    }
                });
            }

            public void bind(AllUsersActivity.OtherUser otherUser) {
                binding.setData(otherUser);
                userId = otherUser._id;
                if (objWaiting.contains(userId)) {
                    Log.v("ppLog", "waiting");
                    binding.pb.setVisibility(View.VISIBLE);
                } else {
                    Log.v("ppLog", "unwaiting");
                    binding.pb.setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    private AllUsersActivity.RelatedUserListAdapter rvAdapter;
    private ActivityAllUsersBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_all_users);

        setup();
    }

    private void setup() {
        EventBus.getDefault().register(this);

        otherUsers = new ArrayList<OtherUser>();

        Observable<String> result = PPRetrofit.getInstance().getPPService().getOtherUsers();

        Consumer<Object> callSuccess = new Consumer<Object>() {
            @Override
            public void accept(@NonNull final Object sObj) throws Exception {
                String s = sObj.toString();
                JsonArray users = ppFromString(s, null).getAsJsonArray();
                for (JsonElement item : users) {

                    String itemStr = item.toString();

                    OtherUser obj = new OtherUser();
                    obj._id = ppFromString(itemStr, "_id").getAsString();
                    obj.username = ppFromString(itemStr, "username").getAsString();
                    obj.nickname = ppFromString(itemStr, "nickname").getAsString();
                    obj.sex = ppFromString(itemStr, "sex").getAsString();
                    obj.avatar = ppFromString(itemStr, "avatar").getAsString();

                    otherUsers.add(obj);
                }
                rvAdapter.notifyDataSetChanged();
            }
        };

        Action callFinal = new Action() {
            @Override
            public void run() throws Exception {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        binding.mainPbView.setVisibility(View.INVISIBLE);
                    }

                }, 200); // 5000ms delay
            }
        };

        PPApplication.apiRequest(result, callSuccess, PPApplication.callFailure, callFinal);

        rvAdapter = new RelatedUserListAdapter();
        binding.mainRv.setLayoutManager(new LinearLayoutManager(this));
        binding.mainRv.setAdapter(rvAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }
}
