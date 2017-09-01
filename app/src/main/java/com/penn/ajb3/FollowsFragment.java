package com.penn.ajb3;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.penn.ajb3.databinding.FollowsUserCellBinding;
import com.penn.ajb3.databinding.FragmentFollowsBinding;
import com.penn.ajb3.realm.RMRelatedUser;

import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FollowsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FollowsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FollowsFragment extends Fragment {
    public class RelatedUserListAdapter extends RecyclerView.Adapter<RelatedUserListAdapter.RelatedUserVH> {

        @Override
        public RelatedUserVH onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

            FollowsUserCellBinding followsUserCellBinding = FollowsUserCellBinding.inflate(layoutInflater, parent, false);

            return new RelatedUserVH(followsUserCellBinding);
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
            private FollowsUserCellBinding binding;

            public RelatedUserVH(FollowsUserCellBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            public void bind(RMRelatedUser rmRelatedUser) {
                binding.setData(rmRelatedUser);
            }
        }
    }

    private Realm realm;
    private RealmResults<RMRelatedUser> data;
    private RelatedUserListAdapter rvAdapter;
    private FragmentFollowsBinding binding;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public FollowsFragment() {
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
    public static FollowsFragment newInstance(String param1, String param2) {
        FollowsFragment fragment = new FollowsFragment();
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
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_follows, container, false);
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
        data = realm.where(RMRelatedUser.class).equalTo("isFollows", true).findAllSorted("updateTime", Sort.DESCENDING);

        data.addChangeListener(new OrderedRealmCollectionChangeListener<RealmResults<RMRelatedUser>>() {
            @Override
            public void onChange(RealmResults<RMRelatedUser> rmRelatedUsers, OrderedCollectionChangeSet changeSet) {
                rvAdapter.notifyDataSetChanged();
            }
        });

        rvAdapter = new RelatedUserListAdapter();
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
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
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
