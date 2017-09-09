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
import com.penn.ajb3.databinding.ClickToLoadMoreBinding;
import com.penn.ajb3.databinding.LoadingMoreBinding;
import com.penn.ajb3.databinding.NoMoreBinding;
import com.penn.ajb3.messageEvent.RelatedUserChanged;
import com.penn.ajb3.realm.RMRelatedUser;
import com.penn.ajb3.util.PPRetrofit;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import retrofit2.HttpException;

import static com.penn.ajb3.AllUsersActivity.RelatedUserListAdapter.LOADING;
import static com.penn.ajb3.AllUsersActivity.RelatedUserListAdapter.LOADING_NOT_START;
import static com.penn.ajb3.AllUsersActivity.RelatedUserListAdapter.LOAD_ALL;
import static com.penn.ajb3.AllUsersActivity.RelatedUserListAdapter.LOAD_FAILED;
import static com.penn.ajb3.PPApplication.ppFromString;

public class AllUsersActivity extends AppCompatActivity {

    private Set<String> objWaiting = new HashSet<String>();
    private ArrayList<AllUsersActivity.OtherUser> otherUsers;
    private LinearLayoutManager linearLayoutManager;

    private static final int pageSize = 20;

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

    public class RelatedUserListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        //type
        public static final int NORMAL = 0;
        public static final int LOADING_MORE = 1;
        public static final int CLICK_TO_LOAD_MORE = 2;
        public static final int NO_MORE = 3;

        //status
        public static final int LOADING_NOT_START = 1;
        public static final int LOADING = 2;
        public static final int LOAD_FAILED = 3;
        public static final int LOAD_ALL = 4;

        public int loadingStatus = LOADING_NOT_START;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

            switch (viewType) {
                case NORMAL:
                    AllUsersUserCellBinding allUsersUserCellBinding = AllUsersUserCellBinding.inflate(layoutInflater, parent, false);
                    return new AllUsersActivity.RelatedUserListAdapter.OtherUserVH(allUsersUserCellBinding);
                case LOADING_MORE:
                    LoadingMoreBinding loadingMoreBinding = LoadingMoreBinding.inflate(layoutInflater, parent, false);
                    return new AllUsersActivity.RelatedUserListAdapter.LoadingMoreVH(loadingMoreBinding);
                case CLICK_TO_LOAD_MORE:
                    ClickToLoadMoreBinding clickToLoadMoreBinding = ClickToLoadMoreBinding.inflate(layoutInflater, parent, false);
                    clickToLoadMoreBinding.clickToLoadMoreTv.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            rvAdapter.loadingStatus = LOADING;
                            final int curSize = otherUsers.size();

                            binding.mainRv.post(new Runnable() {
                                @Override
                                public void run() {
                                    rvAdapter.notifyItemChanged(curSize);

                                    binding.mainRv.scrollToPosition(curSize);

                                    loadMore();
                                }
                            });
                        }
                    });
                    return new AllUsersActivity.RelatedUserListAdapter.ClickToLoadMoreVH(clickToLoadMoreBinding);
                case NO_MORE:
                    NoMoreBinding noMoreBinding = NoMoreBinding.inflate(layoutInflater, parent, false);
                    return new AllUsersActivity.RelatedUserListAdapter.NoMoreVH(noMoreBinding);
                default:
                    throw new Error("onCreateViewHolder 类型错误");
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof OtherUserVH) {
                ((OtherUserVH) holder).bind(otherUsers.get(position));
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position < otherUsers.size()) {
                return NORMAL;
            } else {
                switch (loadingStatus) {
                    case LOADING:
                        return LOADING_MORE;
                    case LOAD_FAILED:
                        return CLICK_TO_LOAD_MORE;
                    case LOAD_ALL:
                        return NO_MORE;
                    default:
                        throw new Error("getItemViewType 类型错误");
                }
            }
        }

        @Override
        public int getItemCount() {
            if (loadingStatus == LOADING_NOT_START) {
                return otherUsers.size();
            } else {
                return otherUsers.size() + 1;
            }
        }

        public class NoMoreVH extends RecyclerView.ViewHolder {
            private NoMoreBinding binding;

            public NoMoreVH(NoMoreBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }

        public class LoadingMoreVH extends RecyclerView.ViewHolder {
            private LoadingMoreBinding binding;

            public LoadingMoreVH(LoadingMoreBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }

        public class ClickToLoadMoreVH extends RecyclerView.ViewHolder {
            private ClickToLoadMoreBinding binding;

            public ClickToLoadMoreVH(ClickToLoadMoreBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
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

        //todo 需要使用一个绝对最小的字符串代替"0"
        Observable<String> result = PPRetrofit.getInstance().getPPService().getOtherUsers("0");

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

                if (otherUsers.size() == pageSize) {
                    rvAdapter.loadingStatus = LOADING_NOT_START;
                } else {
                    rvAdapter.loadingStatus = LOAD_ALL;
                }
            }
        };

        Consumer<Throwable> callFailure = new Consumer<Throwable>() {
            @Override
            public void accept(@NonNull Throwable throwable) {
                rvAdapter.loadingStatus = LOAD_FAILED;
                try {
                    if (throwable instanceof HttpException) {
                        //http非200返回code错误
                        HttpException exception = (HttpException) throwable;
                        String errorBodyString = exception.response().errorBody().string();
                        Log.v("ppLog", errorBodyString);
                        int code = PPApplication.ppFromString(errorBodyString, "code", PPApplication.PPValueType.INT).getAsInt();
                        if (code < 0) {
                            //用户自定义错误
                            String error = PPApplication.ppFromString(errorBodyString, "error").getAsString();
                            Log.v("ppLog", "http exception:" + error);
                            PPApplication.showError("http exception:" + error);
                            if (code == -1000) {
                                PPApplication.logout();
                            }
                        } else {
                            //http常规错误
                            Log.v("ppLog", "http exception:" + errorBodyString);
                            PPApplication.showError("http exception:" + errorBodyString);
                        }
                    } else {
                        //执行callSuccess过程中错误
                        Log.v("ppLog", throwable.toString());
                        PPApplication.showError(throwable.toString());
                    }
                } catch (Exception e) {
                    //执行callFailure过程中错误
                    Log.v("ppLog", e.toString());
                    PPApplication.showError(e.toString());
                }
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

        PPApplication.apiRequest(result, callSuccess, callFailure, callFinal);

        rvAdapter = new RelatedUserListAdapter();
        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        binding.mainRv.setLayoutManager(linearLayoutManager);
        binding.mainRv.setAdapter(rvAdapter);
        binding.mainRv.setHasFixedSize(true);

        binding.mainRv.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int totalItemCount = linearLayoutManager.getItemCount();
                int lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();

                if (rvAdapter.loadingStatus == LOADING_NOT_START && totalItemCount <= (lastVisibleItem + 1)) {
                    rvAdapter.loadingStatus = LOADING;
                    rvAdapter.notifyItemInserted(otherUsers.size());

                    loadMore();
                }
            }
        });
    }

    private void loadMore() {
        String lastUsername = otherUsers.get(otherUsers.size() - 1).username;
        Observable<String> result = PPRetrofit.getInstance().getPPService().getOtherUsers(lastUsername);

        Consumer<Object> callSuccess = new Consumer<Object>() {
            @Override
            public void accept(@NonNull final Object sObj) throws Exception {
                String s = sObj.toString();
                JsonArray users = ppFromString(s, null).getAsJsonArray();
                int startPosition = otherUsers.size();
                int size = users.size();
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

                if (users.size() == pageSize) {
                    rvAdapter.loadingStatus = LOADING_NOT_START;
                    rvAdapter.notifyItemRemoved(otherUsers.size());
                } else {
                    rvAdapter.loadingStatus = LOAD_ALL;
                    rvAdapter.notifyItemChanged(otherUsers.size());
                }

                rvAdapter.notifyItemRangeInserted(startPosition, size);

                //屏幕滚动到新加载的第一条
                binding.mainRv.scrollToPosition(startPosition);
            }
        };

        Consumer<Throwable> callFailure = new Consumer<Throwable>() {
            @Override
            public void accept(@NonNull Throwable throwable) {
                rvAdapter.loadingStatus = LOAD_FAILED;
                rvAdapter.notifyItemRemoved(otherUsers.size());
                try {
                    if (throwable instanceof HttpException) {
                        //http非200返回code错误
                        HttpException exception = (HttpException) throwable;
                        String errorBodyString = exception.response().errorBody().string();
                        Log.v("ppLog", errorBodyString);
                        int code = PPApplication.ppFromString(errorBodyString, "code", PPApplication.PPValueType.INT).getAsInt();
                        if (code < 0) {
                            //用户自定义错误
                            String error = PPApplication.ppFromString(errorBodyString, "error").getAsString();
                            Log.v("ppLog", "http exception:" + error);
                            PPApplication.showError("http exception:" + error);
                            if (code == -1000) {
                                PPApplication.logout();
                            }
                        } else {
                            //http常规错误
                            Log.v("ppLog", "http exception:" + errorBodyString);
                            PPApplication.showError("http exception:" + errorBodyString);
                        }
                    } else {
                        //执行callSuccess过程中错误
                        Log.v("ppLog", throwable.toString());
                        PPApplication.showError(throwable.toString());
                    }
                } catch (Exception e) {
                    //执行callFailure过程中错误
                    Log.v("ppLog", e.toString());
                    PPApplication.showError(e.toString());
                }
            }
        };

        PPApplication.apiRequest(result, callSuccess, callFailure, null);
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
