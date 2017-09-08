package com.penn.ajb3;

import android.content.Context;
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

import com.penn.ajb3.databinding.FriendsUserCellBinding;
import com.penn.ajb3.databinding.FragmentFriendsBinding;
import com.penn.ajb3.realm.RMRelatedUser;
import com.penn.ajb3.util.PPRetrofit;

import java.util.HashSet;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import retrofit2.HttpException;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FriendsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FriendsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FriendsFragment extends Fragment {
    private Set<String> objWaiting = new HashSet<String>();

    public class RelatedUserListAdapter extends RecyclerView.Adapter<RelatedUserListAdapter.RelatedUserVH> {

        @Override
        public RelatedUserVH onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

            FriendsUserCellBinding friendsUserCellBinding = FriendsUserCellBinding.inflate(layoutInflater, parent, false);

            return new RelatedUserVH(friendsUserCellBinding);
        }

        @Override
        public void onBindViewHolder(RelatedUserVH holder, int position) {
            holder.bind(data.get(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        public class RelatedUserVH extends RecyclerView.ViewHolder {
            private FriendsUserCellBinding binding;
            private String userId;

            public RelatedUserVH(FriendsUserCellBinding binding) {
                super(binding.getRoot());
                this.binding = binding;

                binding.unFriendBt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //保存点击时的userId
                        final String curUserId = userId;

                        //如果在此用户在objWaiting中, 则点击无效
                        Log.v("ppLog", "check:" + curUserId);
                        if (objWaiting.contains(curUserId)) {
                            Log.v("ppLog", "稍等下, 不要重复点击");
                            return;
                        }

                        objWaiting.add(curUserId);
                        Log.v("ppLog", "added:" + curUserId);
                        int index = -1;
                        for (int i = 0; i < data.size(); i++) {
                            RMRelatedUser rmRelatedUser = data.get(i);
                            if (rmRelatedUser.get_id().equals(curUserId)) {
                                index = i;
                                break;
                            }
                        }

                        if (index > -1) {
                            rvAdapter.notifyItemChanged(index);
                        }

                        Observable<String> result = PPRetrofit.getInstance().getPPService().unFriend(userId);

                        Action callFinal = new Action() {
                            @Override
                            public void run() throws Exception {
                                objWaiting.remove(curUserId);
                                Log.v("ppLog", "removed:" + curUserId);
                                int index = -1;
                                for (int i = 0; i < data.size(); i++) {
                                    RMRelatedUser rmRelatedUser = data.get(i);
                                    if (rmRelatedUser.get_id().equals(curUserId)) {
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

            public void bind(RMRelatedUser rmRelatedUser) {
                Log.v("ppLog", "bind");
                //一定要加下面这句, 把记录从realm中copy出来成为unmanaged object, 以防止在setData过程中原来的记录被删除而导致程序崩溃
                RMRelatedUser tmp = realm.copyFromRealm(rmRelatedUser);
                binding.setData(tmp);
                userId = tmp._id;
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

    private Realm realm;
    private RealmResults<RMRelatedUser> data;
    private RelatedUserListAdapter rvAdapter;
    private FragmentFriendsBinding binding;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public FriendsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FriendsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static FriendsFragment newInstance(String param1, String param2) {
        FriendsFragment fragment = new FriendsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_friends, container, false);
        View view = binding.getRoot();

        setup();

        return view;
    }

    @Override
    public void onDestroyView() {
        realm.close();
        super.onDestroyView();
    }

    private void setup() {
        realm = Realm.getDefaultInstance();
        data = realm.where(RMRelatedUser.class).equalTo("isFriends", true).findAllSorted("updateTime", Sort.DESCENDING);

        data.addChangeListener(new OrderedRealmCollectionChangeListener<RealmResults<RMRelatedUser>>() {
            @Override
            public void onChange(RealmResults<RMRelatedUser> rmRelatedUsers, OrderedCollectionChangeSet changeSet) {
//                rvAdapter.notifyDataSetChanged();
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

        rvAdapter = new RelatedUserListAdapter();
        binding.mainRv.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.mainRv.setAdapter(rvAdapter);
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
