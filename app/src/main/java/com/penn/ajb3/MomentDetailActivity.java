package com.penn.ajb3;

import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

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
        public String momentId;
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
        binding.deleteBt.setVisibility(View.INVISIBLE);

        comments = new ArrayList();
        rvAdapter = new CommentListAdapter();
        linearLayoutManager = new LinearLayoutManager(this);
        binding.mainRv.setLayoutManager(linearLayoutManager);
        binding.mainRv.setAdapter(rvAdapter);
        binding.mainRv.setHasFixedSize(true);

        momentId = getIntent().getStringExtra("momentId");

        PPApplication.setupUI(this, binding.getRoot());

        //get Moment
        getMoment();

        //get Comment
        getComment();

        binding.deleteBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //todo 删除moment后, nearMoment中此条记录要清除么?
            }
        });

        binding.sendBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                binding.commentInputEt.clearFocus();

                //插入本地data
                long now = System.currentTimeMillis();
                String body = binding.commentInputEt.getText().toString();

                if (TextUtils.isEmpty(body)) {
                    return;
                }

                Comment obj = new Comment();
                obj._id = PPApplication.uuid + "_" + System.currentTimeMillis();
                obj.momentId = momentId;
                obj.userId = PPApplication.getPrefStringValue(PPApplication.MY_ID, "");
                obj.nickname = PPApplication.getPrefStringValue(PPApplication.NICKNAME, "");
                obj.avatar = PPApplication.getPrefStringValue(PPApplication.AVATAR, "");
                obj.body = body;
                obj.createTime = now;

                comments.add(obj);

                //todo 改进成自动滚动到最后一条, 看着记录被添加
//                rvAdapter.notifyItemInserted(comments.size() - 1);
                rvAdapter.notifyDataSetChanged();

                binding.mainRv.scrollToPosition(comments.size() - 1);

                //清空输入框
                binding.commentInputEt.setText("");

                //发送comment到服务器
                Observable<String> result = PPRetrofit.getInstance().getPPService().sendComment(obj._id, obj.momentId, obj.body, obj.createTime);

                PPApplication.apiRequest(result, PPApplication.callSuccess, PPApplication.callFailure, null);
            }
        });
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
                //如本地数据库有这个用户, 顺便更新ta的头像
                PPApplication.updateAvatar(obj.userId, obj.avatar);
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
                    Log.v("ppLog", "itemStr:" + itemStr);

                    final Comment obj = new Comment();
                    Log.v("ppLog", "t1");
                    obj._id = ppFromString(itemStr, "_id").getAsString();
                    Log.v("ppLog", "t2");
                    obj.body = ppFromString(itemStr, "body").getAsString();
                    Log.v("ppLog", "t3");
                    obj.userId = ppFromString(itemStr, "userId._id").getAsString();
                    Log.v("ppLog", "t4");
                    obj.nickname = ppFromString(itemStr, "userId.nickname").getAsString();
                    Log.v("ppLog", "t5");
                    obj.avatar = ppFromString(itemStr, "userId.avatar").getAsString();
                    //如本地数据库有这个用户, 顺便更新ta的头像
                    PPApplication.updateAvatar(obj.userId, obj.avatar);
                    Log.v("ppLog", "t6");
                    obj.createTime = ppFromString(itemStr, "createTime").getAsLong();

                    Log.v("ppLog", "t7");
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
            private String commentId;

            public CommentVH(CommentCellBinding binding) {
                super(binding.getRoot());
                this.binding = binding;

                binding.deleteBt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final String curCommentId = commentId;
                        deleteComment(curCommentId);
                    }
                });
            }

            private void bind(Comment comment) {
                binding.setData(comment);
                commentId = comment._id;
            }
        }
    }

    private void deleteComment(String commentId) {
        Observable<String> result = PPRetrofit.getInstance().getPPService().deleteComment(commentId);

        for (int i = 0; i < comments.size(); i++) {
            if (comments.get(i)._id.equals(commentId)) {
                comments.remove(i);
                rvAdapter.notifyItemRemoved(i);
            }
        }

        PPApplication.apiRequest(result, PPApplication.callSuccess, PPApplication.callFailure, null);
    }
}
