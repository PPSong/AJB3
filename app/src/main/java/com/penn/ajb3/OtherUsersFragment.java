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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.penn.ajb3.databinding.OtherUsersUserCellBinding;
import com.penn.ajb3.databinding.FragmentOtherUsersBinding;
import com.penn.ajb3.realm.RMMyProfile;
import com.penn.ajb3.realm.RMRelatedUser;
import com.penn.ajb3.util.PPRetrofit;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import retrofit2.HttpException;

import static com.penn.ajb3.PPApplication.getNewFans;
import static com.penn.ajb3.PPApplication.getNewFollows;
import static com.penn.ajb3.PPApplication.getNewFriends;
import static com.penn.ajb3.PPApplication.ppFromString;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OtherUsersFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link OtherUsersFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OtherUsersFragment extends Fragment {
    private ArrayList<OtherUser> otherUsers;

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

    public class RelatedUserListAdapter extends RecyclerView.Adapter<RelatedUserListAdapter.OtherUserVH> {

        @Override
        public OtherUserVH onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

            OtherUsersUserCellBinding otherUsersUserCellBinding = OtherUsersUserCellBinding.inflate(layoutInflater, parent, false);

            return new OtherUserVH(otherUsersUserCellBinding);
        }

        @Override
        public void onBindViewHolder(OtherUserVH holder, int position) {
            holder.bind(otherUsers.get(position));
        }

        @Override
        public int getItemCount() {
            return otherUsers.size();
        }

        public class OtherUserVH extends RecyclerView.ViewHolder {
            private OtherUsersUserCellBinding binding;
            private String userId;

            public OtherUserVH(OtherUsersUserCellBinding binding) {
                super(binding.getRoot());
                this.binding = binding;

                binding.followBt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Observable<String> result = PPRetrofit.getInstance().getPPService().follow(userId);

                        result.subscribeOn(Schedulers.newThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(
                                        new Consumer<String>() {
                                            @Override
                                            public void accept(@NonNull final String s) throws Exception {
                                                if (s.equals("ok")) {

                                                } else {
                                                    Log.v("ppLog", "follow failed:" + s);
                                                }
                                            }
                                        },
                                        new Consumer<Throwable>() {
                                            @Override
                                            public void accept(@NonNull Throwable throwable) {
                                                try {
                                                    if (throwable instanceof HttpException) {
                                                        HttpException exception = (HttpException) throwable;
                                                        Log.v("ppLog", "http exception:" + exception.response().errorBody().string());
                                                    } else {
                                                        Log.v("ppLog", throwable.toString());
                                                    }
                                                } catch (Exception e) {
                                                    Log.v("ppLog", e.toString());
                                                }
                                            }
                                        });
                    }
                });
            }

            public void bind(OtherUser otherUser) {
                binding.setData(otherUser);
                userId = otherUser._id;
            }
        }
    }

    private Realm realm;
    private RealmResults<RMRelatedUser> data;
    private RelatedUserListAdapter rvAdapter;
    private FragmentOtherUsersBinding binding;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public OtherUsersFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment OtherUsersFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static OtherUsersFragment newInstance(String param1, String param2) {
        OtherUsersFragment fragment = new OtherUsersFragment();
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
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_other_users, container, false);
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
        otherUsers = new ArrayList<OtherUser>();

        Observable<String> result = PPRetrofit.getInstance().getPPService().getOtherUsers();

        result.subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            @Override
                            public void accept(@NonNull final String s) throws Exception {
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
                                    Log.v("ppLog", "length:" + otherUsers.size());
                                }
                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) {
                                try {
                                    if (throwable instanceof HttpException) {
                                        HttpException exception = (HttpException) throwable;
                                        Log.v("ppLog", "http exception:" + exception.response().errorBody().string());
                                    } else {
                                        Log.v("ppLog", throwable.toString());
                                    }
                                } catch (Exception e) {
                                    Log.v("ppLog", e.toString());
                                }
                            }
                        });

        realm = Realm.getDefaultInstance();
        data = realm.where(RMRelatedUser.class).findAll();

        data.addChangeListener(new OrderedRealmCollectionChangeListener<RealmResults<RMRelatedUser>>() {
            @Override
            public void onChange(RealmResults<RMRelatedUser> rmRelatedUsers, OrderedCollectionChangeSet changeSet) {
                rvAdapter.notifyDataSetChanged();
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
