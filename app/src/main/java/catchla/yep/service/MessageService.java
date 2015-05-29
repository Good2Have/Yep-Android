package catchla.yep.service;

import android.accounts.Account;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.desmond.asyncmanager.AsyncManager;
import com.desmond.asyncmanager.TaskRunnable;

import catchla.yep.Constants;
import catchla.yep.model.Conversation;
import catchla.yep.model.Message;
import catchla.yep.model.PagedMessages;
import catchla.yep.model.Paging;
import catchla.yep.model.TaskResponse;
import catchla.yep.util.Utils;
import catchla.yep.util.YepAPI;
import catchla.yep.util.YepAPIFactory;
import catchla.yep.util.YepException;
import io.realm.Realm;
import io.realm.RealmQuery;

/**
 * Created by mariotaku on 15/5/29.
 */
public class MessageService extends Service implements Constants {

    public static final String ACTION_REFRESH_MESSAGES = "REFRESH_MESSAGES";

    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        final String action = intent.getAction();
        if (action == null) return START_NOT_STICKY;
        switch (action) {
            case ACTION_REFRESH_MESSAGES: {
                refreshMessages();
                break;
            }
        }
        return START_STICKY;
    }

    private void refreshMessages() {
        final Account account = Utils.getCurrentAccount(this);
        if (account == null) return;
        final TaskRunnable<Account, TaskResponse<Boolean>, MessageService>
                task = new TaskRunnable<Account, TaskResponse<Boolean>, MessageService>() {
            @Override
            public TaskResponse<Boolean> doLongOperation(final Account account) throws InterruptedException {
                final Realm realm = Utils.getRealmForAccount(getApplication(), account);
                final YepAPI yep = YepAPIFactory.getInstance(getApplication(), account);
                realm.beginTransaction();
                try {
                    PagedMessages messages;
                    int page = 1;
                    final Paging paging = new Paging();
                    while ((messages = yep.getUnreadMessages(paging)).size() > 0) {
                        for (Message message : messages) {
                            final RealmQuery<Conversation> query = realm.where(Conversation.class);
                            final String recipientType = message.getRecipientType();
                            final String conversationId;
                            if (Message.RecipientType.USER.equalsIgnoreCase(recipientType)) {
                                conversationId = message.getSender().getId();
                            } else if (Message.RecipientType.CIRCLE.equalsIgnoreCase(recipientType)) {
                                conversationId = message.getCircle().getId();
                            } else {
                                throw new UnsupportedOperationException();
                            }
                            query.equalTo("id", conversationId);
                            Conversation conversation = query.findFirst();
                            if (conversation == null) {
                                conversation = new Conversation();
                                conversation.setCircle(message.getCircle());
                                conversation.setSender(message.getSender());
                                conversation.setRecipientType(recipientType);
                                conversation.setId(conversationId);
                            }
                            conversation.setCreatedAt(message.getCreatedAt());
                            conversation.setTextContent(message.getTextContent());
                            realm.copyToRealmOrUpdate(conversation);
                        }
                        realm.copyToRealmOrUpdate(messages);
                        paging.page(++page);
                        if (messages.getCount() < messages.getPerPage()) break;
                    }
                    realm.commitTransaction();
                    return TaskResponse.getInstance(true);
                } catch (YepException e) {
                    Log.w(LOGTAG, e);
                    realm.cancelTransaction();
                    return TaskResponse.getInstance(e);
                } catch (Exception e) {
                    Log.e(LOGTAG, "Error getting messages", e);
                    return TaskResponse.getInstance(e);
                } finally {
                    realm.close();
                }
            }

            @Override
            public void callback(final MessageService handler, final TaskResponse<Boolean> result) {
                super.callback(handler, result);
            }
        };
        task.setParams(account);
        task.setResultHandler(this);
        AsyncManager.runBackgroundTask(task);
    }
}