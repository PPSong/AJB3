package com.penn.ajb3;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.penn.ajb3.databinding.ClickToLoadMoreBinding;
import com.penn.ajb3.databinding.FragmentMyMomentsBinding;
import com.penn.ajb3.databinding.LoadingMoreBinding;
import com.penn.ajb3.databinding.MyMomentCellBinding;
import com.penn.ajb3.databinding.NoMoreBinding;
import com.penn.ajb3.realm.RMMyMoment;
import com.penn.ajb3.util.PPRetrofit;
import com.squareup.picasso.Picasso;

import java.util.HashSet;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

import static com.penn.ajb3.AllUsersActivity.RelatedUserListAdapter.LOAD_ALL;
import static com.penn.ajb3.MyMomentsFragment.MyMomentListAdapter.LOADING_NOT_START;
import static com.penn.ajb3.MyMomentsFragment.MyMomentListAdapter.LOAD_FAILED;
import static com.penn.ajb3.PPApplication.ppFromString;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MyMomentsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MyMomentsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MyMomentsFragment extends Fragment {

    private Set<String> objWaiting = new HashSet<String>();

    private boolean destroyed;

    private static final int pageSize = 10;

    private long startTime;

    private LinearLayoutManager limyLayoutManager;

    public class MyMomentListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

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
                    MyMomentCellBinding myMomentCellBinding = MyMomentCellBinding.inflate(layoutInflater, parent, false);
                    return new MyMomentVH(myMomentCellBinding);
                case LOADING_MORE:
                    LoadingMoreBinding loadingMoreBinding = LoadingMoreBinding.inflate(layoutInflater, parent, false);
                    return new LoadingMoreVH(loadingMoreBinding);
                case CLICK_TO_LOAD_MORE:
                    ClickToLoadMoreBinding clickToLoadMoreBinding = ClickToLoadMoreBinding.inflate(layoutInflater, parent, false);
                    clickToLoadMoreBinding.clickToLoadMoreTv.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            binding.mainRv.post(new Runnable() {
                                @Override
                                public void run() {
                                    loadMore();
                                }
                            });
                        }
                    });
                    return new ClickToLoadMoreVH(clickToLoadMoreBinding);
                case NO_MORE:
                    NoMoreBinding noMoreBinding = NoMoreBinding.inflate(layoutInflater, parent, false);
                    return new NoMoreVH(noMoreBinding);
                default:
                    throw new Error("onCreateViewHolder 类型错误");
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            Log.v("ppLog", "size:" + data.size());
            if (holder instanceof MyMomentVH) {
                ((MyMomentVH) holder).bind(data.get(position));
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position < data.size()) {
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
                return data.size();
            } else {
                return data.size() + 1;
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

        public class MyMomentVH extends RecyclerView.ViewHolder {
            private MyMomentCellBinding binding;
            private RMMyMoment rmMyMoment;

            public MyMomentVH(MyMomentCellBinding binding) {
                super(binding.getRoot());
                this.binding = binding;

                Picasso.with(getContext())
                        .load("https://ss0.bdstatic.com/5aV1bjqh_Q23odCf/static/superman/img/logo/bd_logo1_31bdc765.png")
                        .placeholder(android.R.drawable.ic_menu_myplaces)
                        .error(android.R.drawable.stat_notify_error)
                        .into(binding.avatarIv);

                binding.mainIv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        Intent intent = new Intent(getContext(), MomentDetailActivity.class);
                        intent.putExtra("momentId", rmMyMoment._id);

                        getContext().startActivity(intent);
                    }
                });

                binding.likeTv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        //如果在此momentId在objWaiting中, curMomentId
                        Log.v("ppLog", "check:" + rmMyMoment._id);
                        if (objWaiting.contains(rmMyMoment._id)) {
                            Log.v("ppLog", "稍等下, 不要重复点击");
                            return;
                        }

                        objWaiting.add(rmMyMoment._id);
                        Log.v("ppLog", "added:" + rmMyMoment._id);
                        int index = -1;
                        for (int i = 0; i < data.size(); i++) {
                            RMMyMoment rmMyMoment = data.get(i);
                            if (rmMyMoment._id.equals(rmMyMoment._id)) {
                                index = i;
                                break;
                            }
                        }

                        if (index > -1) {
                            rvAdapter.notifyItemChanged(index);
                        }

                        Observable<String> result = rmMyMoment.toggleLike();

                        Action callFinal = new Action() {
                            @Override
                            public void run() throws Exception {
                                objWaiting.remove(rmMyMoment._id);
                                Log.v("ppLog", "removed:" + rmMyMoment._id);
                                int index = -1;
                                for (int i = 0; i < data.size(); i++) {
                                    RMMyMoment rmMyMoment = data.get(i);
                                    if (rmMyMoment._id.equals(rmMyMoment._id)) {
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

            public void bind(RMMyMoment tmpRmMyMoment) {
                Log.v("ppLog", "bind");
                //一定要加下面这句, 把记录从realm中copy出来成为unmanaged object, 以防止在setData过程中原来的记录被删除而导致程序崩溃
                rmMyMoment = realm.copyFromRealm(tmpRmMyMoment);
                binding.setData(rmMyMoment);

                if (objWaiting.contains(rmMyMoment._id)) {
                    Log.v("ppLog", "waiting");
                    binding.pb.setVisibility(View.VISIBLE);
                } else {
                    Log.v("ppLog", "unwaiting");
                    binding.pb.setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    private Realm realm;
    private RealmResults<RMMyMoment> data;
    private MyMomentListAdapter rvAdapter;
    private FragmentMyMomentsBinding binding;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public MyMomentsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FollowsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MyMomentsFragment newInstance(String param1, String param2) {
        MyMomentsFragment fragment = new MyMomentsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v("ppLog", "follow onCreate");
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        destroyed = false;

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_my_moments, container, false);
        View view = binding.getRoot();

        setup();

        return view;
    }

    @Override
    public void onDestroyView() {
        destroyed = true;
        Log.v("ppLog", "onDestroyView follows");
        //removeAllChangeListeners, 防止多次触发
        data.removeAllChangeListeners();
        realm.close();
        super.onDestroyView();
    }

    private void setup() {
        realm = Realm.getDefaultInstance();
        data = realm.where(RMMyMoment.class).findAllSorted("createTime", Sort.DESCENDING);

        data.addChangeListener(new OrderedRealmCollectionChangeListener<RealmResults<RMMyMoment>>() {
            @Override
            public void onChange(RealmResults<RMMyMoment> rmMyMoments, OrderedCollectionChangeSet changeSet) {
                Log.v("ppLog", "addChangeListener");
//                rvAdapter.notifyDataSetChanged();
                // `null`  means the async query returns the first time.
                if (changeSet == null) {
                    rvAdapter.notifyDataSetChanged();
                    return;
                }
                // For deletions, the adapter has to be notified in reverse order.
                OrderedCollectionChangeSet.Range[] deletions = changeSet.getDeletionRanges();
                for (int i = deletions.length - 1; i >= 0; i--) {
                    OrderedCollectionChangeSet.Range range = deletions[i];
                    rvAdapter.notifyItemRangeRemoved(range.startIndex, range.length);
                }

                OrderedCollectionChangeSet.Range[] insertions = changeSet.getInsertionRanges();
                for (OrderedCollectionChangeSet.Range range : insertions) {
                    rvAdapter.notifyItemRangeInserted(range.startIndex, range.length);
                }

                OrderedCollectionChangeSet.Range[] modifications = changeSet.getChangeRanges();
                for (OrderedCollectionChangeSet.Range range : modifications) {
                    rvAdapter.notifyItemRangeChanged(range.startIndex, range.length);
                }
            }
        });

        rvAdapter = new MyMomentListAdapter();
        limyLayoutManager = new LinearLayoutManager(getContext());
        binding.mainRv.setLayoutManager(limyLayoutManager);
        binding.mainRv.setAdapter(rvAdapter);

        binding.mainRv.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int totalItemCount = limyLayoutManager.getItemCount();
                int lastVisibleItem = limyLayoutManager.findLastVisibleItemPosition();

                Log.v("ppLog", "totalItemCount:" + totalItemCount + ",lastVisibleItem:" + lastVisibleItem);

                if (rvAdapter.loadingStatus == LOADING_NOT_START && totalItemCount <= (lastVisibleItem + 1)) {
                    Log.v("ppLog", "LOAD_MORE");

                    loadMore();
                }
            }
        });

        binding.refreshBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refresh();
            }
        });

        //如果当前local数据库中MyMoment为空, 则自动刷新
        if (data.size() == 0) {
            refresh();
        }
    }

    private void refresh() {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    realm.where(RMMyMoment.class).findAll().deleteAllFromRealm();
                }
            });
        }

        startTime = System.currentTimeMillis();
        getMoment();
    }

    private void loadMore() {
        if (data.size() == 0) {
            startTime = System.currentTimeMillis();
        } else {
            startTime = data.get(data.size() - 1).createTime;
        }
        getMoment();
    }

    private void getMoment() {
        Observable<String> result = PPRetrofit.getInstance().getPPService().getMyMoment(startTime);

        Consumer<Object> callSuccess = new Consumer<Object>() {
            @Override
            public void accept(@NonNull final Object sObj) throws Exception {
                String s = sObj.toString();
                final JsonArray moments = ppFromString(s, null).getAsJsonArray();

                int startPosition = data.size();
                int size = data.size();

                try (Realm realm = Realm.getDefaultInstance()) {
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {

                            for (JsonElement item : moments) {

                                String itemStr = item.toString();

                                RMMyMoment obj = new RMMyMoment();
                                Log.v("ppLog", "t1");
                                obj._id = ppFromString(itemStr, "_id").getAsString();
                                Log.v("ppLog", "t2");
                                obj.userId = ppFromString(itemStr, "userId._id").getAsString();
                                Log.v("ppLog", "t3");
                                obj.nickname = ppFromString(itemStr, "userId.nickname").getAsString();
                                Log.v("ppLog", "t4");
                                obj.avatar = ppFromString(itemStr, "userId.avatar").getAsString();
                                Log.v("ppLog", "t5");
                                obj.body = ppFromString(itemStr, "body").getAsString();
                                Log.v("ppLog", "t6");
                                obj.image = ppFromString(itemStr, "image").getAsString();
                                Log.v("ppLog", "t7");
                                obj.like = ppFromString(itemStr, "like").getAsBoolean();
                                Log.v("ppLog", "t8");
                                obj.createTime = ppFromString(itemStr, "createTime").getAsLong();

                                realm.copyToRealmOrUpdate(obj);
                            }
                        }
                    });
                }

                if (moments.size() == pageSize) {
                    rvAdapter.loadingStatus = AllUsersActivity.RelatedUserListAdapter.LOADING_NOT_START;
                    rvAdapter.notifyItemRemoved(data.size());
                } else {
                    rvAdapter.loadingStatus = LOAD_ALL;
                    rvAdapter.notifyItemRemoved(data.size());
                    rvAdapter.notifyItemInserted(data.size());
                }

                rvAdapter.notifyItemRangeInserted(startPosition, size);

                //屏幕滚动到新加载的第一条
                binding.mainRv.scrollToPosition(startPosition);
            }
        };

        PPApplication.DoOnCallFailure doOnCallFailure = new PPApplication.DoOnCallFailure() {
            @Override
            public void needToDo() {
                //一点要判断当前activity是否销毁, 否则在系统自动kill当前Activity的时候会报null exception崩溃
                if (destroyed) {
                    return;
                }
                rvAdapter.loadingStatus = LOAD_FAILED;
                rvAdapter.notifyItemRemoved(data.size());
                rvAdapter.notifyItemInserted(data.size());
            }
        };

        Consumer<Throwable> callFailure = new PPApplication.CallFailure(doOnCallFailure).getCallFailure();

        PPApplication.apiRequest(result, callSuccess, callFailure, null);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
//        if (context instanceof OnFragmentInteractionListener) {
//            mListener = (OnFragmentInteractionListener) context;
//        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
