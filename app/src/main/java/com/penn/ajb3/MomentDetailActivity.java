package com.penn.ajb3;

import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.penn.ajb3.databinding.ActivityMomentDetailBinding;
import com.penn.ajb3.databinding.CommentCellBinding;
import com.penn.ajb3.util.PPRetrofit;

import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;

import static com.penn.ajb3.PPApplication.ppFromString;

public class MomentDetailActivity extends AppCompatActivity {

    private ActivityMomentDetailBinding binding;
    private ArrayList<Comment> comments;

    private CommentListAdapter rvAdapter;
    private LinearLayoutManager linearLayoutManager;
    private String momentId;

    public class MomentDetail {
        public String _id;
        public String userId;
        public String nickname;
        public String avatar;
        public String body;
        public String image;
        public long createTime;

        public int deletable() {
            if (userId.equals(PPApplication.getPrefStringValue(PPApplication.MY_ID, ""))) {
                return View.VISIBLE;
            } else {
                return View.INVISIBLE;
            }
        }
    }

    public class Comment {
        public String _id;
        public String userId;
        public String nickname;
        public String avatar;
        public String body;
        public long createTime;

        public int deletable() {
            if (userId.equals(PPApplication.getPrefStringValue(PPApplication.MY_ID, ""))) {
                return View.VISIBLE;
            } else {
                return View.INVISIBLE;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_moment_detail);

        setup();
    }

    private void setup() {
        comments = new ArrayList();
        rvAdapter = new CommentListAdapter();
        linearLayoutManager = new LinearLayoutManager(this);
        binding.mainRv.setLayoutManager(linearLayoutManager);
        binding.mainRv.setAdapter(rvAdapter);
        binding.mainRv.setHasFixedSize(true);

        momentId = getIntent().getStringExtra("momentId");

        //get Moment
        getMoment();

        //get Comment
        getComment();
    }

    private void getMoment() {
        Observable<String> result = PPRetrofit.getInstance().getPPService().getMomentDetail(momentId);

        Consumer<Object> callSuccess = new Consumer<Object>() {
            @Override
            public void accept(@NonNull final Object sObj) throws Exception {
                String s = sObj.toString();

                MomentDetail obj = new MomentDetail();
                obj._id = ppFromString(s, "_id").getAsString();
                obj.userId = ppFromString(s, "userId._id").getAsString();
                obj.nickname = ppFromString(s, "userId.nickname").getAsString();
                obj.avatar = ppFromString(s, "userId.avatar").getAsString();
                obj.body = ppFromString(s, "body").getAsString();
                obj.image = ppFromString(s, "image").getAsString();
                obj.createTime = ppFromString(s, "createTime").getAsLong();

                binding.setData(obj);
            }
        };

        PPApplication.apiRequest(result, callSuccess, PPApplication.callFailure, null);
    }

    private void getComment() {
        Observable<String> result = PPRetrofit.getInstance().getPPService().getComments(momentId);

        Consumer<Object> callSuccess = new Consumer<Object>() {
            @Override
            public void accept(@NonNull final Object sObj) throws Exception {
                String s = sObj.toString();
                JsonArray commentsArr = ppFromString(s, null).getAsJsonArray();
                for (JsonElement item : commentsArr) {

                    String itemStr = item.toString();

                    final Comment obj = new Comment();
                    obj._id = ppFromString(itemStr, "_id").getAsString();
                    obj.body = ppFromString(itemStr, "body").getAsString();
                    obj.userId = ppFromString(itemStr, "userId._id").getAsString();
                    obj.nickname = ppFromString(itemStr, "userId.nickname").getAsString();
                    obj.avatar = ppFromString(itemStr, "userId.avatar").getAsString();
                    obj.createTime = ppFromString(itemStr, "createTime").getAsLong();

                    comments.add(obj);
                }

                rvAdapter.notifyDataSetChanged();
            }
        };

        PPApplication.apiRequest(result, callSuccess, PPApplication.callFailure, null);
    }

    public class CommentListAdapter extends RecyclerView.Adapter<CommentListAdapter.CommentVH> {
        @Override
        public CommentVH onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

            CommentCellBinding commentCellBinding = CommentCellBinding.inflate(layoutInflater, parent, false);

            return new CommentVH(commentCellBinding);
        }

        @Override
        public void onBindViewHolder(CommentVH holder, int position) {
            holder.bind(comments.get(position));
        }

        @Override
        public int getItemCount() {
            return comments.size();
        }

        public class CommentVH extends RecyclerView.ViewHolder {
            private CommentCellBinding binding;

            public CommentVH(CommentCellBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            private void bind(Comment comment) {
                binding.setData(comment);
            }
        }
    }
}
