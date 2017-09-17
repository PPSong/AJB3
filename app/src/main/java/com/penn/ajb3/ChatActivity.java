package com.penn.ajb3;

import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.penn.ajb3.databinding.ActivityChatBinding;
import com.penn.ajb3.databinding.MessageMyCellBinding;
import com.penn.ajb3.databinding.MessageOtherCellBinding;
import com.penn.ajb3.realm.RMBlockUser;
import com.penn.ajb3.realm.RMMessage;
import com.penn.ajb3.realm.RMMyProfile;
import com.penn.ajb3.util.PPRetrofit;

import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

import static com.penn.ajb3.AllUsersActivity.RelatedUserListAdapter.LOAD_FAILED;
import static com.penn.ajb3.PPApplication.ppFromString;

public class ChatActivity extends AppCompatActivity {
    private long startTime;

    public class MessageListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        public static final int MESSAGE_OTHER = 0;
        public static final int MESSAGE_MY = 1;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

            if (viewType == MESSAGE_OTHER) {
                MessageOtherCellBinding messageOtherCellBinding = MessageOtherCellBinding.inflate(layoutInflater, parent, false);

                return new MessageOtherVH(messageOtherCellBinding);
            } else {
                MessageMyCellBinding messageMyCellBinding = MessageMyCellBinding.inflate(layoutInflater, parent, false);

                return new MessageMyVH(messageMyCellBinding);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof MessageOtherVH) {
                ((MessageOtherVH) holder).bind(data.get(position));
            } else {
                ((MessageMyVH) holder).bind(data.get(position));
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (data.get(position).userId.equals(PPApplication.getPrefStringValue(PPApplication.MY_ID, ""))) {
                return MESSAGE_OTHER;
            } else {
                return MESSAGE_MY;
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        public class MessageOtherVH extends RecyclerView.ViewHolder {
            private MessageOtherCellBinding binding;

            public MessageOtherVH(MessageOtherCellBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            public void bind(RMMessage rmMessage) {
                binding.setData(rmMessage);
            }
        }

        public class MessageMyVH extends RecyclerView.ViewHolder {
            private MessageMyCellBinding binding;

            public MessageMyVH(MessageMyCellBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            public void bind(RMMessage rmMessage) {
                binding.setData(rmMessage);
            }
        }
    }

    private Realm realm;
    private RealmResults<RMMessage> data;
    private MessageListAdapter rvAdapter;
    private ActivityChatBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat);

        setup();
    }

    private void setup() {
        //todo 先清空
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    realm.where(RMMessage.class).findAll().deleteAllFromRealm();
                }
            });
        }

        realm = Realm.getDefaultInstance();
        data = realm.where(RMMessage.class).findAllSorted("createTime", Sort.DESCENDING);

        data.addChangeListener(new OrderedRealmCollectionChangeListener<RealmResults<RMMessage>>() {
            @Override
            public void onChange(RealmResults<RMMessage> rmRelatedUsers, OrderedCollectionChangeSet changeSet) {
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

        rvAdapter = new MessageListAdapter();
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        binding.mainRv.setLayoutManager(linearLayoutManager);
        binding.mainRv.setAdapter(rvAdapter);


        binding.sendBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

        // 再每隔一秒获取一次
        //Declare the timer
        Timer t = new Timer();
        //Set the schedule function and rate
        t.scheduleAtFixedRate(new TimerTask() {
                                  @Override
                                  public void run() {
                                      //Called each time when 1000 milliseconds (1 second) (the period parameter)
                                      getMessage();
                                  }
                              },
                //Set how long before to start calling the TimerTask (in milliseconds)
                0,
                //Set the amount of time between each execution (in milliseconds)
                1000);
    }

    @Override
    protected void onDestroy() {
        realm.close();
        super.onDestroy();
    }

    private void getMessage() {
        String geo[] = PPApplication.getCurGeo();
        Observable<String> result = PPRetrofit.getInstance().getPPService().getMessage(geo[0], geo[1], startTime);

        Consumer<Object> callSuccess = new Consumer<Object>() {

            @Override
            public void accept(@NonNull Object sObj) throws Exception {
                String s = sObj.toString();
                Log.v("ppLog", s);
                final JsonArray messages = ppFromString(s, null).getAsJsonArray();

                try (Realm realm = Realm.getDefaultInstance()) {
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            for (JsonElement item : messages) {
                                String itemStr = item.toString();

                                RMMessage obj = new RMMessage();
                                obj._id = ppFromString(itemStr, "_id").getAsString();
                                obj.userId = ppFromString(itemStr, "userId._id").getAsString();
                                obj.nickname = ppFromString(itemStr, "userId.nickname").getAsString();
                                obj.avatar = ppFromString(itemStr, "userId.avatar").getAsString();
                                obj.body = ppFromString(itemStr, "body").getAsString();
                                obj.createTime = ppFromString(itemStr, "createTime").getAsLong();
                                obj.status = "net";

                                if (obj.createTime > startTime) {
                                    startTime = obj.createTime;
                                }

                                realm.copyToRealmOrUpdate(obj);
                            }
                        }
                    });
                }
            }
        };

        PPApplication.apiRequest(result, callSuccess, PPApplication.callFailure, null);
    }

    private void sendMessage() {
        final long now = System.currentTimeMillis();
        final String _id = PPApplication.uuid + "_" + now;
        final String body = binding.messageEt.getText().toString();
        String[] geo = PPApplication.getCurGeo();
        String lnt = geo[0];
        String lat = geo[1];

        binding.messageEt.setText("");
        binding.mainRv.scrollToPosition(0);

        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    RMMyProfile me = realm.where(RMMyProfile.class).findFirst();

                    RMMessage obj = new RMMessage();
                    obj._id = _id;
                    obj.userId = PPApplication.getPrefStringValue(PPApplication.MY_ID, "");
                    obj.nickname = me.nickname;
                    obj.avatar = me.avatar;
                    obj.body = body;
                    obj.createTime = now;
                    obj.status = "local";

                    realm.copyToRealmOrUpdate(obj);
                }
            });
        }

        Observable<String> result = PPRetrofit.getInstance().getPPService().sendMessage(_id, body, now, lnt, lat);

        Consumer<Object> callSuccess = new Consumer<Object>() {

            @Override
            public void accept(@NonNull Object sObj) throws Exception {
                try (Realm realm = Realm.getDefaultInstance()) {
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            RMMessage rmMessage = realm.where(RMMessage.class).equalTo("_id", _id).findFirst();

                            rmMessage.status = "net";
                        }
                    });
                }
            }
        };

        PPApplication.DoOnCallFailure doOnCallFailure = new PPApplication.DoOnCallFailure() {
            @Override
            public void needToDo() {
                try (Realm realm = Realm.getDefaultInstance()) {
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            RMMessage rmMessage = realm.where(RMMessage.class).equalTo("_id", _id).findFirst();

                            rmMessage.status = "fail";
                        }
                    });
                }
            }
        };

        Consumer<Throwable> callFailure = new PPApplication.CallFailure(doOnCallFailure).getCallFailure();

        PPApplication.apiRequest(result, callSuccess, callFailure, null);
    }
}
