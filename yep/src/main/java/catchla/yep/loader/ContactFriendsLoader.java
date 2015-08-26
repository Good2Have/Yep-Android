package catchla.yep.loader;

import android.accounts.Account;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.v4.content.AsyncTaskLoader;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

import java.util.List;
import java.util.Locale;

import catchla.yep.model.ContactUpload;
import catchla.yep.model.TaskResponse;
import catchla.yep.model.User;
import catchla.yep.util.YepAPI;
import catchla.yep.util.YepAPIFactory;
import catchla.yep.util.YepException;

/**
 * Created by mariotaku on 15/5/27.
 */
public class ContactFriendsLoader extends AsyncTaskLoader<TaskResponse<List<User>>> {

    private final Account mAccount;

    public ContactFriendsLoader(Context context, Account account) {
        super(context);
        mAccount = account;
    }

    @Override
    public TaskResponse<List<User>> loadInBackground() {
        final YepAPI yep = YepAPIFactory.getInstance(getContext(), mAccount);
        try {
            final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
            final String country = Locale.getDefault().getCountry();
            final ContactUpload contact = new ContactUpload();
            final String[] cols = {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER};
            final String selection = ContactsContract.CommonDataKinds.Phone.NUMBER + " NOT NULL";
            final Cursor c = getContext().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, cols, selection, null, null);
            c.moveToFirst();
            while (!c.isAfterLast()) {
                final String phoneNumber = c.getString(1);
                try {
                    contact.add(c.getString(0), String.valueOf(phoneNumberUtil.parse(phoneNumber, country).getNationalNumber()));
                } catch (NumberParseException e) {
                    // Ignore
                }
                c.moveToNext();
            }
            c.moveToNext();
            c.close();
            final List<User> user = yep.uploadContact(contact);
            return TaskResponse.getInstance(user);
        } catch (YepException e) {
            return TaskResponse.getInstance(e);
        }
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }
}